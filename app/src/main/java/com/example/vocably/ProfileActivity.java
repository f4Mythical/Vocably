package com.example.vocably;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ProfileActivity extends AppCompatActivity {

    private static final String PRIVACY_URL = "https://example.com/privacy";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        TextView tvEmail        = findViewById(R.id.tvEmail);
        TextView tvCreatedAt    = findViewById(R.id.tvCreatedAt);
        LinearLayout btnPrivacy = findViewById(R.id.btnPrivacy);
        TextView btnLogout      = findViewById(R.id.btnLogout);
        ImageView btnDelete     = findViewById(R.id.btnDeleteAccount);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user != null) {
            tvEmail.setText(user.getEmail() != null ? user.getEmail() : "—");
        }

        new FirestoreHelper().getUser(user != null ? user.getUid() : "", snap -> {
            if (snap != null && snap.exists()) {
                Timestamp ts = snap.getTimestamp("createdAt");
                if (ts != null) {
                    String date = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
                            .format(new Date(ts.getSeconds() * 1000));
                    tvCreatedAt.setText(date);
                } else {
                    tvCreatedAt.setText("—");
                }
            } else {
                tvCreatedAt.setText("—");
            }
        });

        btnPrivacy.setOnClickListener(v ->
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(PRIVACY_URL))));

        btnLogout.setOnClickListener(v ->
                new AlertDialog.Builder(this)
                        .setTitle("Wyloguj się")
                        .setMessage("Na pewno chcesz się wylogować?")
                        .setPositiveButton("Wyloguj", (d, w) -> {
                            FirebaseAuth.getInstance().signOut();
                            Intent intent = new Intent(this, MainActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                        })
                        .setNegativeButton("Anuluj", null)
                        .show());

        btnDelete.setOnClickListener(v ->
                new AlertDialog.Builder(this)
                        .setTitle("Usuń konto")
                        .setMessage("Czy na pewno chcesz usunąć konto? Tej operacji nie można cofnąć.")
                        .setPositiveButton("Usuń", (d, w) -> deleteAccount())
                        .setNegativeButton("Anuluj", null)
                        .show());
    }

    private void deleteAccount() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        String uid = user.getUid();

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .delete()
                .addOnCompleteListener(task ->
                        user.delete().addOnCompleteListener(deleteTask -> {
                            FirebaseAuth.getInstance().signOut();
                            Intent intent = new Intent(this, MainActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                        }));
    }
}