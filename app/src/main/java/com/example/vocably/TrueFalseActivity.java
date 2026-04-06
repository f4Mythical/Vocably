package com.example.vocably;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
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

public class TrueFalseActivity extends AppCompatActivity {

    public static final String EXTRA_LANGUAGE        = "language";
    public static final String EXTRA_BOOK_FILE       = "book_file";
    public static final String EXTRA_UNIT_NUMBER     = "unit_number";
    public static final String EXTRA_SECTION_NUMBERS = "section_numbers";

    private static class WordPair {
        String foreign;
        String polish;
        WordPair(String f, String p) { foreign = f; polish = p; }
    }

    private List<WordPair> queue    = new ArrayList<>();
    private List<WordPair> allWords = new ArrayList<>();
    private int     currentIndex = 0;
    private boolean currentIsTrue;
    private String  shownTranslation;
    private String  direction;

    private TextView     tvProgress;
    private TextView     tvQuestion;
    private TextView     tvTranslation;
    private TextView     tvFeedback;
    private LinearLayout btnTrue;
    private LinearLayout btnFalse;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_true_false);

        tvProgress    = findViewById(R.id.tvProgress);
        tvQuestion    = findViewById(R.id.tvQuestion);
        tvTranslation = findViewById(R.id.tvTranslation);
        tvFeedback    = findViewById(R.id.tvFeedback);
        btnTrue       = findViewById(R.id.btnTrue);
        btnFalse      = findViewById(R.id.btnFalse);

        String language = getIntent().getStringExtra(EXTRA_LANGUAGE);
        String bookFile = getIntent().getStringExtra(EXTRA_BOOK_FILE);
        int unitNumber  = getIntent().getIntExtra(EXTRA_UNIT_NUMBER, -1);
        ArrayList<String> sections = getIntent().getStringArrayListExtra(EXTRA_SECTION_NUMBERS);

        direction = getSharedPreferences("vocably_prefs", Context.MODE_PRIVATE)
                .getString("direction_" + language, "lang_to_polish");

        loadWords(bookFile, unitNumber, sections, language);
        Collections.shuffle(queue);
        showCurrent();

        btnTrue.setOnClickListener(v -> answer(true));
        btnFalse.setOnClickListener(v -> answer(false));
    }

    private void loadWords(String bookFile, int unitNumber, List<String> sections, String language) {
        try {
            InputStream is = getAssets().open(bookFile);
            byte[] buffer = new byte[is.available()];
            is.read(buffer);
            is.close();
            JSONObject json = new JSONObject(new String(buffer, StandardCharsets.UTF_8));
            JSONArray units = json.getJSONArray("books").getJSONObject(0).getJSONArray("units");

            for (int i = 0; i < units.length(); i++) {
                JSONObject unit = units.getJSONObject(i);
                if (unit.getInt("number") != unitNumber) continue;
                JSONArray unitSections = unit.getJSONArray("sections");
                for (int j = 0; j < unitSections.length(); j++) {
                    JSONObject section = unitSections.getJSONObject(j);
                    if (!section.has("number") || !section.has("words")) continue;
                    if (!sections.contains(section.getString("number"))) continue;
                    JSONArray words = section.getJSONArray("words");
                    for (int k = 0; k < words.length(); k++) {
                        JSONObject w = words.getJSONObject(k);
                        String polish  = w.optString("polish", "");
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
                            allWords.add(new WordPair(foreign, polish));
                            queue.add(new WordPair(foreign, polish));
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showCurrent() {
        if (queue.isEmpty()) { finish(); return; }

        tvFeedback.setVisibility(View.GONE);
        btnTrue.setAlpha(1f);
        btnFalse.setAlpha(1f);

        WordPair wp = queue.get(currentIndex);
        tvProgress.setText((currentIndex + 1) + " / " + queue.size());

        boolean langToPolish = direction.equals("lang_to_polish")
                || (direction.equals("random") && new Random().nextBoolean());

        currentIsTrue = new Random().nextBoolean();

        if (langToPolish) {
            tvQuestion.setText(wp.foreign);
            if (currentIsTrue) {
                shownTranslation = wp.polish;
            } else {
                shownTranslation = getRandomWrong(wp, false);
                if (shownTranslation == null) { currentIsTrue = true; shownTranslation = wp.polish; }
            }
        } else {
            tvQuestion.setText(wp.polish);
            if (currentIsTrue) {
                shownTranslation = wp.foreign;
            } else {
                shownTranslation = getRandomWrong(wp, true);
                if (shownTranslation == null) { currentIsTrue = true; shownTranslation = wp.foreign; }
            }
        }

        tvTranslation.setText(shownTranslation);
    }

    private String getRandomWrong(WordPair current, boolean getForeign) {
        if (allWords.size() < 2) return null;
        List<WordPair> pool = new ArrayList<>(allWords);
        pool.remove(current);
        Collections.shuffle(pool);
        for (WordPair wp : pool) {
            String candidate = getForeign ? wp.foreign : wp.polish;
            String correct   = getForeign ? current.foreign : current.polish;
            if (!candidate.equals(correct)) return candidate;
        }
        return null;
    }

    private void answer(boolean userSaidTrue) {
        if (userSaidTrue == currentIsTrue) {
            tvFeedback.setText("✓");
            tvFeedback.setTextColor(getResources().getColor(R.color.correct_green, getTheme()));
            tvFeedback.setVisibility(View.VISIBLE);
            btnTrue.postDelayed(() -> {
                queue.remove(currentIndex);
                if (currentIndex >= queue.size()) currentIndex = 0;
                if (queue.isEmpty()) { finish(); return; }
                showCurrent();
            }, 500);
        } else {
            tvFeedback.setText("✗  " + (currentIsTrue ? tvTranslation.getText() : "to nieprawda"));
            tvFeedback.setTextColor(getResources().getColor(R.color.error_red, getTheme()));
            tvFeedback.setVisibility(View.VISIBLE);
            WordPair wp = queue.remove(currentIndex);
            queue.add(wp);
            if (currentIndex >= queue.size()) currentIndex = 0;
            btnTrue.postDelayed(this::showCurrent, 800);
        }
    }
}