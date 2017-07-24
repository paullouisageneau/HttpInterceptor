package org.ageneau.httpinterceptor;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * HttpInterceptor main activity
 * This activity starts an instance of HttpForwarder acting as reverse proxy for target URL and
 * launches a media player activity on the local address
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private static final String TARGET_URL = "https://wowza-cloudfront.streamroot.io/liveorigin/stream4/playlist.m3u8";
    private static final String PLAYER = "org.videolan.vlc";    // VLC media player
    private static final int PLAYER_REQUEST = 42;
    private static final int LOCAL_PORT = 8888;

    private String mTarget;     // Base URL from target URL
    private String mTargetPath; // Path from target URL

    private HttpForwarder mForwarder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Parse target URL
        try {
            URL url = new URL(TARGET_URL);
            mTarget = url.getProtocol() + "://" + url.getHost();
            mTargetPath = url.getPath();
        }
        catch(MalformedURLException e) {
            e.printStackTrace();
            finish();
        }

        // Create and start the forwarder
        Log.d(TAG, "Starting HTTP forwarder...");
        mForwarder = new HttpForwarder(LOCAL_PORT, mTarget);
        mForwarder.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Stop HTTP forwarder
        mForwarder.stop();
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Local URL to hit the forwarder
        String localUrl = "http://localhost:" + String.valueOf(LOCAL_PORT) + mTargetPath;

        // Launch player on local URL
        Log.d(TAG, "Launching media player...");
        Intent playerIntent = new Intent(Intent.ACTION_VIEW);
        playerIntent.setData(Uri.parse(localUrl));
        playerIntent.setPackage(PLAYER);

        try {
            startActivityForResult(playerIntent, PLAYER_REQUEST);
        } catch (ActivityNotFoundException e) {
            Toast toast = Toast.makeText(getBaseContext(), "Specified player not found", Toast.LENGTH_LONG);
            toast.show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == PLAYER_REQUEST) {
            // New VLC versions return a result before closing so we shouldn't finish
            //finish();
        }
    }
}
