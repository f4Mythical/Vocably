package com.example.vocably.Onboarding;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.example.vocably.MainActivity;
import com.example.vocably.R;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;

public class Register extends Fragment {

    private FirebaseAuth auth;
    private TextInputEditText etEmail, etPassword;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_onboarding2, container, false);

        auth = FirebaseAuth.getInstance();
        etEmail    = view.findViewById(R.id.etEmail);
        etPassword = view.findViewById(R.id.etPassword);
        Button btnSubmit    = view.findViewById(R.id.btnSubmit);
        TextView tvTitle    = view.findViewById(R.id.tvTitle);
        TextView tvToggle   = view.findViewById(R.id.tvToggle);

        tvTitle.setText("Zarejestruj się");
        btnSubmit.setText("Zarejestruj");
        tvToggle.setText("Masz już konto? Zaloguj się");

        btnSubmit.setOnClickListener(v -> {
            String email    = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
            String password = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(getContext(), "Uzupełnij email i hasło", Toast.LENGTH_SHORT).show();
                return;
            }
            auth.createUserWithEmailAndPassword(email, password)
                    .addOnSuccessListener(result -> {
                        startActivity(new Intent(getActivity(), MainActivity.class));
                        if (getActivity() != null) getActivity().finish();
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(getContext(), "Błąd rejestracji: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        });

        tvToggle.setOnClickListener(v -> {
            if (getActivity() instanceof OnboardingActivity) {
                ((OnboardingActivity) getActivity()).goToLogin();
            }
        });

        return view;
    }
}