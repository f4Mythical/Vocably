package com.example.vocably;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.vocably.Onboarding.OnboardingActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {

    private final VersionTracker versionTracker = new VersionTracker();
    private final FirestoreHelper firestoreHelper = new FirestoreHelper();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        versionTracker.start(this);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || user.isAnonymous()) {
            startActivity(new Intent(this, OnboardingActivity.class));
            finish();
            return;
        }

        startActivity(new Intent(this, HomeActivity.class));
        finish();
    }

    private void createUserCollection() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseUser user = auth.getCurrentUser();

        if (user != null) {
            firestoreHelper.createUserIfNotExists(user.getUid(), user.getEmail());
        } else {
            auth.signInAnonymously().addOnSuccessListener(result -> {
                FirebaseUser newUser = result.getUser();
                if (newUser != null) {
                    firestoreHelper.createUserIfNotExists(newUser.getUid(), "");
                }
            }).addOnFailureListener(e -> {});
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        versionTracker.stop();
    }
}