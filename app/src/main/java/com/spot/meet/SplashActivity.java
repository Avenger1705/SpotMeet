package com.spot.meet;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DURATION = 2000; // 2 seconds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Animate logo content
        LinearLayout content = findViewById(R.id.splash_content);
        Animation popIn = AnimationUtils.loadAnimation(this, R.anim.pop_in);
        content.startAnimation(popIn);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            // Check if user is already logged in
            SharedPreferences prefs = getSharedPreferences("spotmeet_prefs", MODE_PRIVATE);
            boolean loggedIn = prefs.getBoolean("is_logged_in", false);

            Intent intent;
            if (loggedIn) {
                intent = new Intent(SplashActivity.this, MainActivity.class);
            } else {
                intent = new Intent(SplashActivity.this, LoginActivity.class);
            }
            startActivity(intent);
            overridePendingTransition(R.anim.fade_in, android.R.anim.fade_out);
            finish();
        }, SPLASH_DURATION);
    }
}
