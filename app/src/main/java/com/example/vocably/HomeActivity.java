package com.example.vocably;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Random;

public class HomeActivity extends AppCompatActivity {

    private final VersionTracker versionTracker = new VersionTracker();
    private final FirestoreHelper firestoreHelper = new FirestoreHelper();
    private final Handler clockHandler = new Handler(Looper.getMainLooper());

    private TextView tvTime;
    private ImageView btnSettings;
    private View menuScrim;
    private LinearLayout settingsMenu;

    private boolean menuOpen = false;

    private final String[] greetings = {
            "Witaj w Vocably",
            "Hej, miło Cię widzieć",
            "Cześć!",
            "Guten Morgen!",
            "Hello there!",
            "Hi! Gotowy na naukę?",
            "Bon jour!",
            "Ciao!",
            "¡Hola!"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        versionTracker.start(this);

        TextView tvGreeting = findViewById(R.id.tvGreeting);
        tvTime       = findViewById(R.id.tvTime);
        btnSettings  = findViewById(R.id.btnSettings);
        menuScrim    = findViewById(R.id.menuScrim);
        settingsMenu = findViewById(R.id.settingsMenu);

        TextView btnEnglish = findViewById(R.id.btnEnglish);
        TextView btnGerman  = findViewById(R.id.btnGerman);

        tvGreeting.setText(greetings[new Random().nextInt(greetings.length)]);
        startClock();

        btnEnglish.setOnClickListener(v ->
                startActivity(new Intent(this, EnglishChoice.class)));
        btnGerman.setOnClickListener(v ->
                startActivity(new Intent(this, DeutschChoice.class)));

        btnSettings.setOnClickListener(v -> toggleMenu());
        menuScrim.setOnClickListener(v -> closeMenu());

        settingsMenu.findViewById(R.id.menuItemProfile).setOnClickListener(v -> {
            closeMenu();
            startActivity(new Intent(this, ProfileActivity.class));
        });

        settingsMenu.findViewById(R.id.menuItemFeedback).setOnClickListener(v -> {
            closeMenu();
            new FeedbackSheet().show(getSupportFragmentManager(), "feedback");
        });

        settingsMenu.findViewById(R.id.menuItemInfo).setOnClickListener(v -> closeMenu());

        createUserCollection();
    }

    private void toggleMenu() {
        if (menuOpen) closeMenu();
        else openMenu();
    }

    private void openMenu() {
        menuOpen = true;

        btnSettings.animate()
                .rotationBy(405f)
                .setDuration(650)
                .setInterpolator(new DecelerateInterpolator())
                .withEndAction(() -> {
                    menuScrim.setVisibility(View.VISIBLE);
                    menuScrim.setAlpha(0f);
                    menuScrim.animate().alpha(1f).setDuration(180).start();

                    settingsMenu.setVisibility(View.VISIBLE);
                    settingsMenu.setAlpha(0f);
                    settingsMenu.setTranslationY(50f);
                    settingsMenu.setScaleY(0.85f);
                    settingsMenu.setPivotY(settingsMenu.getHeight());

                    settingsMenu.animate()
                            .alpha(1f).translationY(0f).scaleY(1f)
                            .setDuration(280)
                            .setInterpolator(new OvershootInterpolator(1.1f))
                            .start();
                })
                .start();
    }

    private void closeMenu() {
        menuOpen = false;

        menuScrim.animate()
                .alpha(0f).setDuration(150)
                .withEndAction(() -> menuScrim.setVisibility(View.GONE))
                .start();

        settingsMenu.animate()
                .alpha(0f).translationY(40f).scaleY(0.88f)
                .setDuration(180)
                .setInterpolator(new DecelerateInterpolator())
                .withEndAction(() -> {
                    settingsMenu.setVisibility(View.INVISIBLE);
                    settingsMenu.setTranslationY(0f);
                    settingsMenu.setScaleY(1f);

                    btnSettings.animate()
                            .rotationBy(-405f).setDuration(650)
                            .setInterpolator(new DecelerateInterpolator())
                            .start();
                })
                .start();
    }

    private void startClock() {
        clockHandler.post(new Runnable() {
            @Override
            public void run() {
                tvTime.setText(new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date()));
                clockHandler.postDelayed(this, 30000);
            }
        });
    }

    private void createUserCollection() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            firestoreHelper.createUserIfNotExists(user.getUid(), user.getEmail());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        clockHandler.removeCallbacksAndMessages(null);
        versionTracker.stop();
    }

    @Override
    public void onBackPressed() {
        if (menuOpen) {
            closeMenu();
        } else {
            super.onBackPressed();
        }
    }
}