package com.example.vocably;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class FlashcardActivity extends AppCompatActivity {

    public static final String EXTRA_LANGUAGE        = "language";
    public static final String EXTRA_BOOK_FILE       = "book_file";
    public static final String EXTRA_UNIT_NUMBER     = "unit_number";
    public static final String EXTRA_SECTION_NUMBERS = "section_numbers";

    private TextView tvProgress, tvPromptLang, tvPrompt;
    private View     translationPanel;
    private TextView tvAnswerLang, tvAnswer;
    private TextView btnShowTranslation;
    private View     cardView;
    private TextView tvKnownCount, tvUnknownCount;
    private View     summaryLayout, studyLayout;

    private List<Word> allWords    = new ArrayList<>();
    private List<Word> unknownPile = new ArrayList<>();
    private int        currentIdx  = 0;
    private boolean    translationVisible = false;
    private String     direction;
    private int        knownCount   = 0;
    private int        unknownCount = 0;

    private GestureDetector gestureDetector;

    private static class Word {
        String foreign;
        String polish;
        Word(String foreign, String polish) {
            this.foreign = foreign;
            this.polish  = polish;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_flashcard);

        bindViews();
        loadDirection();
        loadWords();

        if (allWords.isEmpty()) {
            finish();
            return;
        }

        Collections.shuffle(allWords);
        setupGesture();
        showCard();
    }

    private void bindViews() {
        tvProgress         = findViewById(R.id.tvProgress);
        tvPromptLang       = findViewById(R.id.tvPromptLang);
        tvPrompt           = findViewById(R.id.tvPrompt);
        translationPanel   = findViewById(R.id.translationPanel);
        tvAnswerLang       = findViewById(R.id.tvAnswerLang);
        tvAnswer           = findViewById(R.id.tvAnswer);
        btnShowTranslation = findViewById(R.id.btnShowTranslation);
        cardView           = findViewById(R.id.cardView);
        tvKnownCount       = findViewById(R.id.tvKnownCount);
        tvUnknownCount     = findViewById(R.id.tvUnknownCount);
        summaryLayout      = findViewById(R.id.summaryLayout);
        studyLayout        = findViewById(R.id.studyLayout);

        btnShowTranslation.setOnClickListener(v -> revealTranslation());
    }

    private void loadDirection() {
        String lang = getIntent().getStringExtra(EXTRA_LANGUAGE);
        String prefKey = "en".equals(lang) ? "direction_en" : "direction_de";
        direction = getSharedPreferences("vocably_prefs", Context.MODE_PRIVATE)
                .getString(prefKey, "lang_to_polish");
    }

    private void loadWords() {
        String bookFile = getIntent().getStringExtra(EXTRA_BOOK_FILE);
        int unitNumber  = getIntent().getIntExtra(EXTRA_UNIT_NUMBER, -1);
        ArrayList<String> sectionNumbers = getIntent().getStringArrayListExtra(EXTRA_SECTION_NUMBERS);
        String lang = getIntent().getStringExtra(EXTRA_LANGUAGE);

        if (bookFile == null || unitNumber == -1 || sectionNumbers == null) return;

        try {
            JSONObject json = loadJson(bookFile);
            if (json == null) return;
            JSONArray units = json.getJSONArray("books").getJSONObject(0).getJSONArray("units");

            for (int i = 0; i < units.length(); i++) {
                JSONObject unit = units.getJSONObject(i);
                if (unit.getInt("number") != unitNumber) continue;

                JSONArray sections = unit.getJSONArray("sections");
                for (int j = 0; j < sections.length(); j++) {
                    JSONObject section = sections.getJSONObject(j);
                    if (!section.has("number") || !section.has("words")) continue;
                    if (!sectionNumbers.contains(section.getString("number"))) continue;

                    JSONArray words = section.getJSONArray("words");
                    for (int k = 0; k < words.length(); k++) {
                        JSONObject w = words.getJSONObject(k);
                        String polish = w.optString("polish", "");
                        String foreign = "";
                        JSONArray translations = w.optJSONArray("translations");
                        if (translations != null) {
                            for (int t = 0; t < translations.length(); t++) {
                                JSONObject tr = translations.getJSONObject(t);
                                if (tr.optString("language").equals(lang)) {
                                    foreign = tr.optString("value", "");
                                    break;
                                }
                            }
                        }
                        if (!foreign.isEmpty() && !polish.isEmpty()) {
                            allWords.add(new Word(foreign, polish));
                        }
                    }
                }
                break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showCard() {
        if (currentIdx >= allWords.size()) {
            showSummary();
            return;
        }

        translationVisible = false;
        translationPanel.setVisibility(View.GONE);
        translationPanel.setTranslationY(0);
        btnShowTranslation.setVisibility(View.VISIBLE);

        Word word = allWords.get(currentIdx);
        boolean langToPolish = resolveDirection();

        tvProgress.setText((currentIdx + 1) + " / " + allWords.size());

        if (langToPolish) {
            tvPromptLang.setText(getForeignLangLabel());
            tvPrompt.setText(word.foreign);
            tvAnswerLang.setText("Polski");
            tvAnswer.setText(word.polish);
        } else {
            tvPromptLang.setText("Polski");
            tvPrompt.setText(word.polish);
            tvAnswerLang.setText(getForeignLangLabel());
            tvAnswer.setText(word.foreign);
        }

        cardView.setAlpha(1f);
        cardView.setTranslationX(0f);
    }

    private boolean resolveDirection() {
        if ("random".equals(direction)) return new Random().nextBoolean();
        return "lang_to_polish".equals(direction);
    }

    private String getForeignLangLabel() {
        return "de".equals(getIntent().getStringExtra(EXTRA_LANGUAGE)) ? "Niemiecki" : "Angielski";
    }

    private void revealTranslation() {
        if (translationVisible) return;
        translationVisible = true;
        btnShowTranslation.setVisibility(View.GONE);
        translationPanel.setVisibility(View.VISIBLE);

        translationPanel.post(() -> {
            float startY = translationPanel.getHeight() > 0 ? translationPanel.getHeight() : 300f;
            translationPanel.setTranslationY(startY);
            ObjectAnimator anim = ObjectAnimator.ofFloat(translationPanel, "translationY", startY, 0f);
            anim.setDuration(280);
            anim.setInterpolator(new DecelerateInterpolator());
            anim.start();
        });
    }

    private void setupGesture() {
        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            private static final int SWIPE_THRESHOLD = 100;
            private static final int SWIPE_VELOCITY  = 100;

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float vX, float vY) {
                if (e1 == null || e2 == null) return false;
                float dX = e2.getX() - e1.getX();
                float dY = e2.getY() - e1.getY();
                if (Math.abs(dX) > Math.abs(dY)
                        && Math.abs(dX) > SWIPE_THRESHOLD
                        && Math.abs(vX) > SWIPE_VELOCITY) {
                    if (!translationVisible) {
                        revealTranslation();
                        return true;
                    }
                    swipeCard(dX > 0);
                    return true;
                }
                return false;
            }

            @Override
            public boolean onDown(MotionEvent e) { return true; }
        });

        cardView.setOnTouchListener((v, event) -> gestureDetector.onTouchEvent(event));
    }

    private void swipeCard(boolean known) {
        float targetX = known
                ? getResources().getDisplayMetrics().widthPixels * 1.5f
                : -getResources().getDisplayMetrics().widthPixels * 1.5f;

        cardView.animate()
                .translationX(targetX)
                .alpha(0f)
                .setDuration(220)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .withEndAction(() -> {
                    if (known) {
                        knownCount++;
                    } else {
                        unknownCount++;
                        unknownPile.add(allWords.get(currentIdx));
                    }
                    currentIdx++;
                    cardView.setTranslationX(0f);
                    cardView.setAlpha(1f);
                    showCard();
                })
                .start();
    }

    private void showSummary() {
        studyLayout.setVisibility(View.GONE);
        summaryLayout.setVisibility(View.VISIBLE);
        tvKnownCount.setText(String.valueOf(knownCount));
        tvUnknownCount.setText(String.valueOf(unknownCount));

        TextView btnRepeatUnknown = findViewById(R.id.btnRepeatUnknown);
        TextView btnFinish        = findViewById(R.id.btnFinish);

        if (unknownPile.isEmpty()) {
            btnRepeatUnknown.setVisibility(View.GONE);
        } else {
            btnRepeatUnknown.setVisibility(View.VISIBLE);
            btnRepeatUnknown.setOnClickListener(v -> repeatUnknown());
        }

        btnFinish.setOnClickListener(v -> finish());
    }

    private void repeatUnknown() {
        allWords.clear();
        allWords.addAll(unknownPile);
        unknownPile.clear();
        Collections.shuffle(allWords);
        currentIdx   = 0;
        knownCount   = 0;
        unknownCount = 0;
        summaryLayout.setVisibility(View.GONE);
        studyLayout.setVisibility(View.VISIBLE);
        showCard();
    }

    private JSONObject loadJson(String fileName) {
        try {
            InputStream is = getAssets().open(fileName);
            byte[] buf = new byte[is.available()];
            is.read(buf);
            is.close();
            return new JSONObject(new String(buf, StandardCharsets.UTF_8));
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return gestureDetector.onTouchEvent(event) || super.onTouchEvent(event);
    }
}