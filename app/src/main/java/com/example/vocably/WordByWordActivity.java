package com.example.vocably;

import android.animation.ObjectAnimator;
import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class WordByWordActivity extends AppCompatActivity {

    public static final String EXTRA_LANGUAGE        = "language";
    public static final String EXTRA_BOOK_FILE       = "book_file";
    public static final String EXTRA_UNIT_NUMBER     = "unit_number";
    public static final String EXTRA_SECTION_NUMBERS = "section_numbers";

    private static class Word {
        String foreign;
        String polish;
        Word(String f, String p) { foreign = f; polish = p; }
    }

    private List<Word> allWords  = new ArrayList<>();
    private int        currentIdx = 0;
    private String     language;
    private String     direction;
    private boolean    translationVisible = false;

    private TextView tvProgress;
    private TextView tvPromptLang;
    private TextView tvPrompt;
    private TextView tvAnswerLang;
    private TextView tvAnswer;
    private View     cardView;
    private View     translationPanel;
    private TextView btnShowTranslation;

    private GestureDetector gestureDetector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_word_by_word);

        language = getIntent().getStringExtra(EXTRA_LANGUAGE);

        String prefKey = "en".equals(language) ? "direction_en" : "direction_de";
        direction = getSharedPreferences("vocably_prefs", Context.MODE_PRIVATE)
                .getString(prefKey, "lang_to_polish");

        if ("random".equals(direction)) {
            new AlertDialog.Builder(this)
                    .setTitle("Tryb losowy niedostępny")
                    .setMessage("Słowo po słowie wymaga stałego kierunku tłumaczenia. Zmień kierunek na Język → Polski lub Polski → Język i spróbuj ponownie.")
                    .setPositiveButton("OK", (d, w) -> finish())
                    .setCancelable(false)
                    .show();
            return;
        }

        tvProgress         = findViewById(R.id.tvProgress);
        tvPromptLang       = findViewById(R.id.tvPromptLang);
        tvPrompt           = findViewById(R.id.tvPrompt);
        tvAnswerLang       = findViewById(R.id.tvAnswerLang);
        tvAnswer           = findViewById(R.id.tvAnswer);
        cardView           = findViewById(R.id.cardView);
        translationPanel   = findViewById(R.id.translationPanel);
        btnShowTranslation = findViewById(R.id.btnShowTranslation);

        btnShowTranslation.setOnClickListener(v -> revealTranslation());

        loadWords();

        if (allWords.isEmpty()) { finish(); return; }

        showCard();
        setupGesture();
    }

    private void showCard() {
        translationVisible = false;
        translationPanel.setVisibility(View.GONE);
        translationPanel.setTranslationY(0);
        btnShowTranslation.setVisibility(View.VISIBLE);

        Word word = allWords.get(currentIdx);
        tvProgress.setText((currentIdx + 1) + " / " + allWords.size());

        if ("lang_to_polish".equals(direction)) {
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

    private void revealTranslation() {
        if (translationVisible) return;
        translationVisible = true;
        btnShowTranslation.setVisibility(View.GONE);
        translationPanel.setVisibility(View.VISIBLE);

        translationPanel.post(() -> {
            float startY = translationPanel.getHeight() > 0 ? translationPanel.getHeight() : 200f;
            translationPanel.setTranslationY(startY);
            ObjectAnimator anim = ObjectAnimator.ofFloat(translationPanel, "translationY", startY, 0f);
            anim.setDuration(250);
            anim.setInterpolator(new DecelerateInterpolator());
            anim.start();
        });
    }

    private void setupGesture() {
        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float vX, float vY) {
                if (e1 == null || e2 == null) return false;
                float dX = e2.getX() - e1.getX();
                float dY = e2.getY() - e1.getY();
                if (Math.abs(dX) > Math.abs(dY) && Math.abs(dX) > 80 && Math.abs(vX) > 80) {
                    if (dX > 0) navigatePrev();
                    else        navigateNext();
                    return true;
                }
                return false;
            }
            @Override public boolean onDown(MotionEvent e) { return true; }
        });

        cardView.setOnTouchListener((v, e) -> gestureDetector.onTouchEvent(e));
    }

    private void navigateNext() {
        if (currentIdx >= allWords.size() - 1) return;
        animateAndGo(true);
    }

    private void navigatePrev() {
        if (currentIdx <= 0) return;
        animateAndGo(false);
    }

    private void animateAndGo(boolean forward) {
        float targetX = forward
                ? -getResources().getDisplayMetrics().widthPixels * 1.2f
                :  getResources().getDisplayMetrics().widthPixels * 1.2f;

        cardView.animate()
                .translationX(targetX)
                .alpha(0f)
                .setDuration(180)
                .setInterpolator(new DecelerateInterpolator())
                .withEndAction(() -> {
                    currentIdx += forward ? 1 : -1;
                    cardView.setTranslationX(-targetX);
                    cardView.setAlpha(0f);
                    showCard();
                    cardView.animate()
                            .translationX(0f)
                            .alpha(1f)
                            .setDuration(180)
                            .setInterpolator(new DecelerateInterpolator())
                            .start();
                }).start();
    }

    private void loadWords() {
        String bookFile = getIntent().getStringExtra(EXTRA_BOOK_FILE);
        int unitNumber  = getIntent().getIntExtra(EXTRA_UNIT_NUMBER, -1);
        ArrayList<String> sectionNumbers = getIntent().getStringArrayListExtra(EXTRA_SECTION_NUMBERS);

        if (bookFile == null || unitNumber == -1 || sectionNumbers == null) return;

        try {
            InputStream is = getAssets().open(bookFile);
            byte[] buf = new byte[is.available()];
            is.read(buf);
            is.close();
            JSONObject json = new JSONObject(new String(buf, StandardCharsets.UTF_8));
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
                                if (tr.optString("language").equals(language)) {
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

    private String getForeignLangLabel() {
        return "de".equals(language) ? "Niemiecki" : "Angielski";
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        return gestureDetector != null && gestureDetector.onTouchEvent(e) || super.onTouchEvent(e);
    }
}