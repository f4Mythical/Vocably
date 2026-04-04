package com.example.vocably.Onboarding;

import android.content.Intent;

import com.example.vocably.MainActivity;
import com.example.vocably.R;

public class Register {

    public void bind(Onboarding2 fragment) {
        fragment.getTvTitle().setText(R.string.btn_register);
        fragment.getBtnSubmit().setText(R.string.btn_register);
        fragment.getTvToggle().setText(R.string.toggle_to_login);

        fragment.getBtnSubmit().setOnClickListener(v -> {
            fragment.clearErrors();
            String email          = fragment.getEtEmail().getText() != null ? fragment.getEtEmail().getText().toString().trim() : "";
            String password       = fragment.getEtPassword().getText() != null ? fragment.getEtPassword().getText().toString().trim() : "";
            String passwordRepeat = fragment.getEtPasswordRepeat().getText() != null ? fragment.getEtPasswordRepeat().getText().toString().trim() : "";

            boolean valid = true;
            if (email.isEmpty()) {
                fragment.getTilEmail().setError(fragment.getString(R.string.error_fill_fields));
                valid = false;
            }
            if (password.isEmpty()) {
                fragment.getTilPassword().setError(fragment.getString(R.string.error_fill_fields));
                valid = false;
            }
            if (passwordRepeat.isEmpty()) {
                fragment.getTilPasswordRepeat().setError(fragment.getString(R.string.error_fill_fields));
                valid = false;
            }
            if (!valid) return;

            if (!password.equals(passwordRepeat)) {
                fragment.getTilPasswordRepeat().setError(fragment.getString(R.string.error_passwords_not_match));
                return;
            }

            fragment.getAuth().createUserWithEmailAndPassword(email, password)
                    .addOnSuccessListener(result -> {
                        fragment.startActivity(new Intent(fragment.getActivity(), MainActivity.class));
                        if (fragment.getActivity() != null) fragment.getActivity().finish();
                    })
                    .addOnFailureListener(e ->
                            fragment.getTilEmail().setError(fragment.getString(R.string.error_register) + e.getMessage()));
        });

        fragment.getTvToggle().setOnClickListener(v -> fragment.switchToLogin());
    }
}