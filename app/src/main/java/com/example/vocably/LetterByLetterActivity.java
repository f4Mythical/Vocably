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

public class LetterByLetterActivity extends AppCompatActivity {

    public static final String EXTRA_LANGUAGE        = "language";
    public static final String EXTRA_BOOK_FILE       = "book_file";
    public static final String EXTRA_UNIT_NUMBER     = "unit_number";
    public static final String EXTRA_SECTION_NUMBERS = "section_numbers";

    private static class WordPair {
        String question;
        String answer;
        WordPair(String q, String a) { question = q; answer = a; }
    }

    private List<WordPair> queue = new ArrayList<>();
    private int currentIndex  = 0;
    private int revealedCount = 0;

    private TextView     tvQuestion;
    private TextView     tvLetters;
    private TextView     tvProgress;
    private LinearLayout btnReveal;
    private LinearLayout btnKnow;
    private LinearLayout btnDontKnow;
    private LinearLayout actionRow;

    private String direction;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_letter_by_letter);

        tvQuestion  = findViewById(R.id.tvQuestion);
        tvLetters   = findViewById(R.id.tvLetters);
        tvProgress  = findViewById(R.id.tvProgress);
        btnReveal   = findViewById(R.id.btnReveal);
        btnKnow     = findViewById(R.id.btnKnow);
        btnDontKnow = findViewById(R.id.btnDontKnow);
        actionRow   = findViewById(R.id.actionRow);

        String language  = getIntent().getStringExtra(EXTRA_LANGUAGE);
        String bookFile  = getIntent().getStringExtra(EXTRA_BOOK_FILE);
        int unitNumber   = getIntent().getIntExtra(EXTRA_UNIT_NUMBER, -1);
        ArrayList<String> sections = getIntent().getStringArrayListExtra(EXTRA_SECTION_NUMBERS);

        direction = getSharedPreferences("vocably_prefs", Context.MODE_PRIVATE)
                .getString("direction_" + language, "lang_to_polish");

        loadWords(bookFile, unitNumber, sections);
        Collections.shuffle(queue);
        showCurrent();

        btnReveal.setOnClickListener(v -> revealNextLetter());
        btnKnow.setOnClickListener(v -> next(true));
        btnDontKnow.setOnClickListener(v -> next(false));
    }

    private void loadWords(String bookFile, int unitNumber, List<String> sections) {
        String language = getIntent().getStringExtra(EXTRA_LANGUAGE);
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
                        if (foreign.isEmpty() || polish.isEmpty()) continue;
                        if (direction.equals("lang_to_polish")) {
                            queue.add(new WordPair(foreign, polish));
                        } else if (direction.equals("polish_to_lang")) {
                            queue.add(new WordPair(polish, foreign));
                        } else {
                            if (Math.random() < 0.5) queue.add(new WordPair(foreign, polish));
                            else                     queue.add(new WordPair(polish, foreign));
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
        revealedCount = 0;
        WordPair wp = queue.get(currentIndex);

        tvProgress.setText((currentIndex + 1) + " / " + queue.size());
        tvQuestion.setText(wp.question);
        tvLetters.setText(buildMask(wp.answer, 0));

        actionRow.setVisibility(View.GONE);
        btnReveal.setVisibility(View.VISIBLE);
    }

    private void revealNextLetter() {
        WordPair wp = queue.get(currentIndex);
        revealedCount++;
        tvLetters.setText(buildMask(wp.answer, revealedCount));

        if (revealedCount >= wp.answer.length()) {
            btnReveal.setVisibility(View.GONE);
            actionRow.setVisibility(View.VISIBLE);
        }
    }

    private String buildMask(String word, int revealed) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < word.length(); i++) {
            char c = word.charAt(i);
            if (c == ' ') {
                sb.append("  ");
            } else if (i < revealed) {
                sb.append(c);
            } else {
                sb.append('_');
            }
        }
        return sb.toString();
    }

    private void next(boolean knew) {
        if (!knew) {
            WordPair wp = queue.remove(currentIndex);
            queue.add(wp);
        } else {
            currentIndex++;
        }

        if (currentIndex >= queue.size()) {
            finish();
            return;
        }
        showCurrent();
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
}