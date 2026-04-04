package com.example.vocably.Onboarding;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.example.vocably.R;

public class Onboarding1 extends Fragment {

    private static final int TYPING_SPEED_MS = 40;
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_onboarding1, container, false);

        TextView textViewGreetings = view.findViewById(R.id.textViewGreetings);
        Button btnNext             = view.findViewById(R.id.btnNext);

        btnNext.setVisibility(View.GONE);

        typeText(textViewGreetings, getString(R.string.greetings), 0, () ->
                btnNext.setVisibility(View.VISIBLE));

        btnNext.setOnClickListener(v -> {
            if (getActivity() instanceof OnboardingActivity) {
                ((OnboardingActivity) getActivity()).goToLogin();
            }
        });

        return view;
    }

    private void typeText(TextView tv, String text, int index, Runnable onFinished) {
        if (!isAdded()) return;
        if (index < text.length()) {
            tv.setText(text.substring(0, index + 1));
            handler.postDelayed(() -> typeText(tv, text, index + 1, onFinished), TYPING_SPEED_MS);
        } else {
            handler.post(onFinished);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        handler.removeCallbacksAndMessages(null);
    }
}