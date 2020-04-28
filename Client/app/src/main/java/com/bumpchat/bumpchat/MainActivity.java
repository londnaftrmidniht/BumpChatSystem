package com.bumpchat.bumpchat;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import java.io.File;

public class MainActivity extends Activity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent;

        // Go to setup on first launch
        File db = getDatabasePath("bumpChat");
        if (!db.exists()) {
            intent = new Intent(this, SetupActivity.class);
        } else {
            intent = new Intent(this, LoginActivity.class);
        }

        // Complete activity so user can't go back
        finish();

        startActivity(intent);
    }
}