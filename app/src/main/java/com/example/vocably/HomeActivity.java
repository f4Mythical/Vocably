package com.example.vocably;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class HomeActivity extends AppCompatActivity {

    private final VersionTracker  versionTracker  = new VersionTracker();
    private final FirestoreHelper firestoreHelper = new FirestoreHelper();
    private final Handler         clockHandler    = new Handler(Looper.getMainLooper());

    private TextView     tvTime;
    private ImageView    btnSettings;
    private View         menuScrim;
    private LinearLayout settingsMenu;
    private LinearLayout recentContainer;

    private boolean menuOpen = false;

    private final String[] greetings = {
            "Witaj w Vocably", "Hej, miło Cię widzieć", "Cześć!",
            "Guten Morgen!", "Hello there!", "Hi! Gotowy na naukę?",
            "Bon jour!", "Ciao!", "¡Hola!"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        versionTracker.start(this);

        TextView tvGreeting = findViewById(R.id.tvGreeting);
        tvTime          = findViewById(R.id.tvTime);
        btnSettings     = findViewById(R.id.btnSettings);
        menuScrim       = findViewById(R.id.menuScrim);
        settingsMenu    = findViewById(R.id.settingsMenu);
        recentContainer = findViewById(R.id.recentContainer);

        TextView btnEnglish = findViewById(R.id.btnEnglish);
        TextView btnGerman  = findViewById(R.id.btnGerman);

        tvGreeting.setText(greetings[new Random().nextInt(greetings.length)]);
        startClock();

        btnEnglish.setOnClickListener(v -> startActivity(new Intent(this, EnglishChoice.class)));
        btnGerman.setOnClickListener(v  -> startActivity(new Intent(this, DeutschChoice.class)));

        btnSettings.setOnClickListener(v -> toggleMenu());
        menuScrim.setOnClickListener(v   -> closeMenu());

        settingsMenu.findViewById(R.id.menuItemProfile).setOnClickListener(v -> {
            startActivity(new Intent(this, ProfileActivity.class));
            closeMenu();

        });
        settingsMenu.findViewById(R.id.menuItemFeedback).setOnClickListener(v -> {
            new FeedbackSheet().show(getSupportFragmentManager(), "feedback");
            closeMenu();

        });
        settingsMenu.findViewById(R.id.menuItemInfo).setOnClickListener(v -> {

            startActivity(new Intent(this, InformationActivity.class));
            closeMenu();
        });

        createUserCollection();
    }



    @Override
    protected void onResume() {
        super.onResume();
        buildRecentSessions();
    }

    private void buildRecentSessions() {
        recentContainer.removeAllViews();
        List<SessionManager.Session> sessions = SessionManager.load(this);

        for (SessionManager.Session s : sessions) {
            recentContainer.addView(buildSessionRow(s));
        }
    }

    private View buildSessionRow(SessionManager.Session s) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setBackground(ContextCompat.getDrawable(this, R.drawable.bg_btn_language));
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dpToPx(14), dpToPx(14), dpToPx(14), dpToPx(14));

        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        rowParams.setMargins(0, 0, 0, dpToPx(8));
        row.setLayoutParams(rowParams);

        TextView tvLang = new TextView(this);
        tvLang.setText(s.languageLabel());
        tvLang.setTextSize(12f);
        tvLang.setTextColor(ContextCompat.getColor(this, R.color.accent_gold));
        tvLang.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        TextView tvDot1 = dot();
        TextView tvDot2 = dot();

        TextView tvInfo = new TextView(this);
        tvInfo.setText(s.bookName + "  ·  Unit " + s.unitNumber);
        tvInfo.setTextSize(12f);
        tvInfo.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        tvInfo.setSingleLine(true);
        LinearLayout.LayoutParams infoParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        infoParams.setMargins(dpToPx(6), 0, dpToPx(6), 0);
        tvInfo.setLayoutParams(infoParams);

        TextView tvMode = new TextView(this);
        tvMode.setText(s.mode);
        tvMode.setTextSize(12f);
        tvMode.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        tvMode.setSingleLine(true);
        tvMode.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        row.addView(tvLang);
        row.addView(tvDot1);
        row.addView(tvInfo);
        row.addView(tvDot2);
        row.addView(tvMode);

        row.setOnClickListener(v -> launchSession(s));
        return row;
    }

    private TextView dot() {
        TextView tv = new TextView(this);
        tv.setText("·");
        tv.setTextSize(12f);
        tv.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        tv.setPadding(dpToPx(4), 0, dpToPx(4), 0);
        tv.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        return tv;
    }

    private void launchSession(SessionManager.Session s) {
        if (s == null) return;
        String lang = s.language;
        Intent intent = null;

        switch (s.mode) {
            case "Fiszki":
                intent = new Intent(this, FlashcardActivity.class);
                intent.putExtra(FlashcardActivity.EXTRA_LANGUAGE, lang);
                intent.putExtra(FlashcardActivity.EXTRA_BOOK_FILE, s.bookFile);
                intent.putExtra(FlashcardActivity.EXTRA_UNIT_NUMBER, s.unitNumber);
                intent.putStringArrayListExtra(FlashcardActivity.EXTRA_SECTION_NUMBERS, new ArrayList<>(s.sections));
                break;
            case "Wpisz":
                intent = new Intent(this, TypingActivity.class);
                intent.putExtra(TypingActivity.EXTRA_LANGUAGE, lang);
                intent.putExtra(TypingActivity.EXTRA_BOOK_FILE, s.bookFile);
                intent.putExtra(TypingActivity.EXTRA_UNIT_NUMBER, s.unitNumber);
                intent.putStringArrayListExtra(TypingActivity.EXTRA_SECTION_NUMBERS, new ArrayList<>(s.sections));
                break;
            case "Wybór":
                intent = new Intent(this, ChoiceActivity.class);
                intent.putExtra(ChoiceActivity.EXTRA_LANGUAGE, lang);
                intent.putExtra(ChoiceActivity.EXTRA_BOOK_FILE, s.bookFile);
                intent.putExtra(ChoiceActivity.EXTRA_UNIT_NUMBER, s.unitNumber);
                intent.putStringArrayListExtra(ChoiceActivity.EXTRA_SECTION_NUMBERS, new ArrayList<>(s.sections));
                break;
            case "Szybkie fiszki":
                intent = new Intent(this, QuickFlashcardActivity.class);
                intent.putExtra(QuickFlashcardActivity.EXTRA_LANGUAGE, lang);
                intent.putExtra(QuickFlashcardActivity.EXTRA_BOOK_FILE, s.bookFile);
                intent.putExtra(QuickFlashcardActivity.EXTRA_UNIT_NUMBER, s.unitNumber);
                intent.putStringArrayListExtra(QuickFlashcardActivity.EXTRA_SECTION_NUMBERS, new ArrayList<>(s.sections));
                break;
            case "Szybka odpowiedź":
                intent = new Intent(this, SpeedAnswerActivity.class);
                intent.putExtra(SpeedAnswerActivity.EXTRA_LANGUAGE, lang);
                intent.putExtra(SpeedAnswerActivity.EXTRA_BOOK_FILE, s.bookFile);
                intent.putExtra(SpeedAnswerActivity.EXTRA_UNIT_NUMBER, s.unitNumber);
                intent.putStringArrayListExtra(SpeedAnswerActivity.EXTRA_SECTION_NUMBERS, new ArrayList<>(s.sections));
                break;
            case "Lista":
                intent = new Intent(this, WordListActivity.class);
                intent.putExtra(WordListActivity.EXTRA_LANGUAGE, lang);
                intent.putExtra(WordListActivity.EXTRA_BOOK_FILE, s.bookFile);
                intent.putExtra(WordListActivity.EXTRA_UNIT_NUMBER, s.unitNumber);
                intent.putStringArrayListExtra(WordListActivity.EXTRA_SECTION_NUMBERS, new ArrayList<>(s.sections));
                break;
            case "Litera po literze":
                intent = new Intent(this, LetterByLetterActivity.class);
                intent.putExtra(LetterByLetterActivity.EXTRA_LANGUAGE, lang);
                intent.putExtra(LetterByLetterActivity.EXTRA_BOOK_FILE, s.bookFile);
                intent.putExtra(LetterByLetterActivity.EXTRA_UNIT_NUMBER, s.unitNumber);
                intent.putStringArrayListExtra(LetterByLetterActivity.EXTRA_SECTION_NUMBERS, new ArrayList<>(s.sections));
                break;
            case "Prawda czy Fałsz":
                intent = new Intent(this, TrueFalseActivity.class);
                intent.putExtra(TrueFalseActivity.EXTRA_LANGUAGE, lang);
                intent.putExtra(TrueFalseActivity.EXTRA_BOOK_FILE, s.bookFile);
                intent.putExtra(TrueFalseActivity.EXTRA_UNIT_NUMBER, s.unitNumber);
                intent.putStringArrayListExtra(TrueFalseActivity.EXTRA_SECTION_NUMBERS, new ArrayList<>(s.sections));
                break;
            case "Ułóż słowo":
                intent = new Intent(this, ScrambleActivity.class);
                intent.putExtra(ScrambleActivity.EXTRA_LANGUAGE, lang);
                intent.putExtra(ScrambleActivity.EXTRA_BOOK_FILE, s.bookFile);
                intent.putExtra(ScrambleActivity.EXTRA_UNIT_NUMBER, s.unitNumber);
                intent.putStringArrayListExtra(ScrambleActivity.EXTRA_SECTION_NUMBERS, new ArrayList<>(s.sections));
                break;
            case "Memory":
                intent = new Intent(this, MemorySetupActivity.class);
                intent.putExtra(MemorySetupActivity.EXTRA_LANGUAGE, lang);
                intent.putExtra(MemorySetupActivity.EXTRA_BOOK_FILE, s.bookFile);
                intent.putExtra(MemorySetupActivity.EXTRA_UNIT_NUMBER, s.unitNumber);
                intent.putStringArrayListExtra(MemorySetupActivity.EXTRA_SECTION_NUMBERS, new ArrayList<>(s.sections));
                break;
        }

        if (intent != null) startActivity(intent);
    }

    private void toggleMenu() {
        if (menuOpen) closeMenu();
        else openMenu();
    }

    private void openMenu() {
        menuOpen = true;
        btnSettings.setRotation(0f);
        btnSettings.animate()
                .rotationBy(405).setDuration(300)
                .setInterpolator(new DecelerateInterpolator())
                .start();

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
    }

    private void closeMenu() {
        menuOpen = false;
        menuScrim.animate().alpha(0f).setDuration(150)
                .withEndAction(() -> menuScrim.setVisibility(View.GONE)).start();
        settingsMenu.animate()
                .alpha(0f).translationY(40f).scaleY(0.88f).setDuration(180)
                .setInterpolator(new DecelerateInterpolator())
                .withEndAction(() -> {
                    settingsMenu.setVisibility(View.INVISIBLE);
                    settingsMenu.setTranslationY(0f);
                    settingsMenu.setScaleY(1f);
                    btnSettings.animate().rotation(0f).setDuration(300)
                            .setInterpolator(new DecelerateInterpolator()).start();
                }).start();
    }

    private void startClock() {
        clockHandler.post(new Runnable() {
            @Override public void run() {
                tvTime.setText(new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date()));
                clockHandler.postDelayed(this, 30000);
            }
        });
    }

    private void createUserCollection() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) firestoreHelper.createUserIfNotExists(user.getUid(), user.getEmail());
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        clockHandler.removeCallbacksAndMessages(null);
        versionTracker.stop();
    }

    @Override public void onBackPressed() {
        if (menuOpen) closeMenu();
        else super.onBackPressed();
    }
}