package com.example.vocably.Onboarding;

import android.content.Intent;

import com.example.vocably.MainActivity;
import com.example.vocably.R;

public class Login {

    public void bind(Onboarding2 fragment) {
        fragment.getTvTitle().setText(R.string.btn_login);
        fragment.getBtnSubmit().setText(R.string.btn_login);
        fragment.getTvToggle().setText(R.string.toggle_to_register);

        fragment.getBtnSubmit().setOnClickListener(v -> {
            fragment.clearErrors();
            String email    = fragment.getEtEmail().getText() != null ? fragment.getEtEmail().getText().toString().trim() : "";
            String password = fragment.getEtPassword().getText() != null ? fragment.getEtPassword().getText().toString().trim() : "";

            boolean valid = true;
            if (email.isEmpty()) {
                fragment.getTilEmail().setError(fragment.getString(R.string.error_fill_fields));
                valid = false;
            }
            if (password.isEmpty()) {
                fragment.getTilPassword().setError(fragment.getString(R.string.error_fill_fields));
                valid = false;
            }
            if (!valid) return;

            fragment.getAuth().signInWithEmailAndPassword(email, password)
                    .addOnSuccessListener(result -> {
                        fragment.startActivity(new Intent(fragment.getActivity(), MainActivity.class));
                        if (fragment.getActivity() != null) fragment.getActivity().finish();
                    })
                    .addOnFailureListener(e ->
                            fragment.getTilPassword().setError(fragment.getString(R.string.error_login) + e.getMessage()));
        });

        fragment.getTvToggle().setOnClickListener(v -> fragment.switchToRegister());
    }
}