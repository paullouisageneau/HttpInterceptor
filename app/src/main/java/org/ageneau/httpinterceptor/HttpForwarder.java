package org.ageneau.httpinterceptor;

import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.HttpURLConnection;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * HTTP forwarder implementation
 */
public class HttpForwarder implements Runnable {

    private static final String TAG = "HttpForwarder";
    private static final int BUFFER_SIZE = 2048;

    private final int mPort;
    private final String mTargetUrl;

    private ServerSocket mServerSocket;

    /**
     * Create server for specified port, requests will be forwarded to specified target URL
     */
    public HttpForwarder(int port, String targetUrl) {
        mPort = port;
        mTargetUrl = targetUrl.replaceAll("/$", "");
    }

    /**
     * Start the server
     */
    public void start() {
        try {
            // Open the server socket
            mServerSocket = new ServerSocket(mPort);

            Log.d(TAG, "Listening on port " + String.valueOf(mPort));

            Thread t = new Thread(this);
            t.start();
        }
        catch(IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Stop the server
     */
    public void stop() {
        try {
            if(mServerSocket != null) {
                mServerSocket.close();
                mServerSocket = null;
            }
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Server main loop
     */
    @Override
    public void run() {
        try {
            // Loop on incoming connections
            while(true) {
                // Accept
                final Socket socket = mServerSocket.accept();

                // Spawn a thread
                Thread t = new Thread() {
                    @Override
                    public void run() {
                        handle(socket);
                    }
                };

                t.start();
            }
        } catch(SocketException e) {
            // Stopped
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Receive an HTTP request, forward the request to target and forward back the response
     */
    private void handle(Socket socket) {
        BufferedReader reader = null;
        PrintStream output = null;
        try {
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            output = new PrintStream(socket.getOutputStream());

            // Read request line
            String requestLine = reader.readLine();

            // Parse request line
            String[] tokens = requestLine.split(" ", 3);
            if(tokens.length != 3) {
                sendResponse(output, "400 Bad Request", false);
                return;
            }
            String method = tokens[0].toUpperCase();
            String path = tokens[1];

            // We accept only GET requests
            if(!method.equals("GET")) {
                sendResponse(output, "405 Method not allowed", false);
                return;
            }

            // Read the headers
            Map<String, List<String>> headers = new HashMap<>();
            String line;
            while(!(line = reader.readLine()).isEmpty()) {
                String[] s = line.split(":", 2);
                String key = s[0].trim();
                if(!headers.containsKey(key))
                    headers.put(key, new ArrayList<String>());
                headers.get(key).add(s.length == 2 ? s[1].trim() : "");
            }

            String targetUrl = mTargetUrl + path;
            Log.i(TAG, method + " " + targetUrl);

            // Forge the corresponding request
            URL url = new URL(targetUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            // Populate request
            conn.setRequestMethod(method);
            for(Map.Entry<String, List<String>> entry : headers.entrySet()) {
                String key = entry.getKey();
                if(!key.equals("Host")) {   // Host will be set by HttpURLConnection
                    for(String value : entry.getValue()) {
                        conn.setRequestProperty(key, value);
                    }
                }
            }

            // Send request
            conn.setDoOutput(false);
            conn.connect();

            // Forward response
            String response = conn.getResponseCode() + " " + conn.getResponseMessage();
            sendResponse(output, response, true);

            // Forward response headers
            Map<String, List<String>> map = conn.getHeaderFields();
            for(Map.Entry<String, List<String>> entry : map.entrySet()) {
                String key = entry.getKey();
                if(key != null                          // status line is passed with a null key
                        && !key.equals("Connection")) { // sendResponse set it to close
                    for(String value : entry.getValue()) {
                        output.print(key + ": " + value + "\r\n");
                    }
                }
            }
            output.print("\r\n");
            output.flush();

            // Forward content
            try {
                InputStream in;
                if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    in = conn.getInputStream();
                }
                else {
                    in = conn.getErrorStream();
                }

                in = new BufferedInputStream(conn.getInputStream());
                byte[] buffer = new byte[BUFFER_SIZE];
                int size;
                while ((size = in.read(buffer)) != -1) {
                    output.write(buffer, 0, size);
                }
            } finally {
                conn.disconnect();
            }

        } catch(IOException e) {
            e.printStackTrace();
        }
        finally {
            try {
                if(output != null) output.close();
                if(reader != null) reader.close();
                socket.close();
            } catch(IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Send an HTTP response on stream
     */
    private void sendResponse(PrintStream output, String response, boolean otherHeaders) {
        output.print("HTTP/1.1 " + response + "\r\n");
        output.print("Connection: close\r\n");
        if(!otherHeaders) {
            output.print("\r\n");
            output.flush();
        }
    }
}
