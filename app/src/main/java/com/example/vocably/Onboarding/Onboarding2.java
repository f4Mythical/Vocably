package com.example.vocably.Onboarding;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.example.vocably.R;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;

public class Onboarding2 extends Fragment {

    private FirebaseAuth auth;
    private TextInputEditText etEmail, etPassword, etPasswordRepeat;
    private TextInputLayout tilEmail, tilPassword, tilPasswordRepeat;
    private TextView btnSubmit, tvTitle, tvToggle;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_onboarding2, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        auth              = FirebaseAuth.getInstance();
        etEmail           = view.findViewById(R.id.etEmail);
        etPassword        = view.findViewById(R.id.etPassword);
        etPasswordRepeat  = view.findViewById(R.id.etPasswordRepeat);
        tilEmail          = view.findViewById(R.id.tilEmail);
        tilPassword       = view.findViewById(R.id.tilPassword);
        tilPasswordRepeat = view.findViewById(R.id.tilPasswordRepeat);
        btnSubmit         = view.findViewById(R.id.btnSubmit);
        tvTitle           = view.findViewById(R.id.tvTitle);
        tvToggle          = view.findViewById(R.id.tvToggle);

        new Login().bind(this);
    }

    public void clearErrors() {
        tilEmail.setError(null);
        tilPassword.setError(null);
        tilPasswordRepeat.setError(null);
    }

    public void switchToLogin() {
        clearErrors();
        tilPasswordRepeat.setVisibility(View.GONE);
        new Login().bind(this);
    }

    public void switchToRegister() {
        clearErrors();
        tilPasswordRepeat.setVisibility(View.VISIBLE);
        new Register().bind(this);
    }

    public FirebaseAuth getAuth()                        { return auth; }
    public TextInputEditText getEtEmail()                { return etEmail; }
    public TextInputEditText getEtPassword()             { return etPassword; }
    public TextInputEditText getEtPasswordRepeat()       { return etPasswordRepeat; }
    public TextInputLayout getTilEmail()                 { return tilEmail; }
    public TextInputLayout getTilPassword()              { return tilPassword; }
    public TextInputLayout getTilPasswordRepeat()        { return tilPasswordRepeat; }
    public TextView getBtnSubmit()                       { return btnSubmit; }
    public TextView getTvTitle()                         { return tvTitle; }
    public TextView getTvToggle()                        { return tvToggle; }
}