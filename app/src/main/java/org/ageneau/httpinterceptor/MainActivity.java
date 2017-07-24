package org.ageneau.httpinterceptor;

import android.content.Intent;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import java.net.MalformedURLException;
import java.net.URL;

public class MainActivity extends AppCompatActivity {

    private static final String TARGET_URL = "https://wowza-cloudfront.streamroot.io/liveorigin/stream4/playlist.m3u8";
    private static final String PLAYER = "org.videolan.vlc";
    private static final int PLAYER_REQUEST = 42;
    private static final int LOCAL_PORT = 8888;

    private String mTarget;
    private String mTargetPath;
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

        // Create and start HTTP forwarder
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

        String localUrl = "http://localhost:" + String.valueOf(LOCAL_PORT) + mTargetPath;

        // Launch player on local URL
        Intent playerIntent = new Intent(Intent.ACTION_VIEW);
        playerIntent.setData(Uri.parse(localUrl));
        playerIntent.setPackage(PLAYER);
        startActivityForResult(playerIntent, PLAYER_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PLAYER_REQUEST) {
            finish();
        }
    }
}
