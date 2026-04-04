package com.example.vocably;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class FirestoreHelper {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public void createUserIfNotExists(String uid, String email) {
        db.collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(snap -> {
                    if (!snap.exists()) {
                        Map<String, Object> user = new HashMap<>();
                        user.put("email", email);
                        user.put("createdAt", Timestamp.now());
                        db.collection("users")
                                .document(uid)
                                .set(user)
                                .addOnFailureListener(e -> {});
                    }
                })
                .addOnFailureListener(e -> {});
    }

    public void getUser(String uid,
                        com.google.android.gms.tasks.OnSuccessListener<com.google.firebase.firestore.DocumentSnapshot> onSuccess) {
        db.collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(onSuccess)
                .addOnFailureListener(e -> {});
    }
}