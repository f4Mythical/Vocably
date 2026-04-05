package com.example.vocably;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
        tvTime              = findViewById(R.id.tvTime);
        TextView btnEnglish = findViewById(R.id.btnEnglish);
        TextView btnGerman  = findViewById(R.id.btnGerman);

        tvGreeting.setText(greetings[new Random().nextInt(greetings.length)]);
        startClock();

        btnEnglish.setOnClickListener(v ->
                startActivity(new Intent(this, EnglishChoice.class)));
        btnGerman.setOnClickListener(v ->
                startActivity(new Intent(this, DeutschChoice.class)));

        createUserCollection();
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
}