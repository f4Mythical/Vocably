package com.example.vocably;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class ScrambleActivity extends AppCompatActivity {

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
    private int     currentIndex = 0;
    private String  direction;

    private List<String> letterPool     = new ArrayList<>();
    private List<String> selectedLetters = new ArrayList<>();

    private TextView     tvProgress;
    private TextView     tvQuestion;
    private TextView     tvBuilt;
    private TextView     tvFeedback;
    private LinearLayout lettersContainer;
    private LinearLayout btnClear;
    private LinearLayout btnCheck;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scramble);

        tvProgress       = findViewById(R.id.tvProgress);
        tvQuestion       = findViewById(R.id.tvQuestion);
        tvBuilt          = findViewById(R.id.tvBuilt);
        tvFeedback       = findViewById(R.id.tvFeedback);
        lettersContainer = findViewById(R.id.lettersContainer);
        btnClear         = findViewById(R.id.btnClear);
        btnCheck         = findViewById(R.id.btnCheck);

        String language = getIntent().getStringExtra(EXTRA_LANGUAGE);
        String bookFile = getIntent().getStringExtra(EXTRA_BOOK_FILE);
        int unitNumber  = getIntent().getIntExtra(EXTRA_UNIT_NUMBER, -1);
        ArrayList<String> sections = getIntent().getStringArrayListExtra(EXTRA_SECTION_NUMBERS);

        direction = getSharedPreferences("vocably_prefs", Context.MODE_PRIVATE)
                .getString("direction_" + language, "lang_to_polish");

        loadWords(bookFile, unitNumber, sections, language);
        Collections.shuffle(queue);
        showCurrent();

        btnClear.setOnClickListener(v -> clearSelection());
        btnCheck.setOnClickListener(v -> checkAnswer());
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
                            if (direction.equals("lang_to_polish")) {
                                queue.add(new WordPair(foreign, polish));
                            } else if (direction.equals("polish_to_lang")) {
                                queue.add(new WordPair(polish, foreign));
                            } else {
                                if (new Random().nextBoolean()) queue.add(new WordPair(foreign, polish));
                                else                            queue.add(new WordPair(polish, foreign));
                            }
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
        selectedLetters.clear();
        letterPool.clear();

        WordPair wp = queue.get(currentIndex);
        tvProgress.setText((currentIndex + 1) + " / " + queue.size());
        tvQuestion.setText(wp.question);
        tvBuilt.setText("");

        String answer = wp.answer;
        List<String> letters = new ArrayList<>();
        for (int i = 0; i < answer.length(); i++) {
            letters.add(String.valueOf(answer.charAt(i)));
        }

        String shuffled;
        int tries = 0;
        do {
            Collections.shuffle(letters);
            shuffled = join(letters);
            tries++;
        } while (shuffled.equals(answer) && tries < 20);

        letterPool.addAll(letters);
        renderLetterButtons();
    }

    private void renderLetterButtons() {
        lettersContainer.removeAllViews();

        int cols = 6;
        LinearLayout currentRow = null;

        for (int i = 0; i < letterPool.size(); i++) {
            if (i % cols == 0) {
                currentRow = new LinearLayout(this);
                currentRow.setOrientation(LinearLayout.HORIZONTAL);
                currentRow.setGravity(android.view.Gravity.CENTER);
                LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                rp.setMargins(0, 0, 0, dpToPx(8));
                currentRow.setLayoutParams(rp);
                lettersContainer.addView(currentRow);
            }

            String letter = letterPool.get(i);
            final int idx = i;

            TextView btn = new TextView(this);
            btn.setText(letter.equals(" ") ? "·" : letter);
            btn.setGravity(android.view.Gravity.CENTER);
            btn.setTextSize(18f);
            btn.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
            btn.setBackground(ContextCompat.getDrawable(this, R.drawable.bg_btn_language));

            LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(dpToPx(44), dpToPx(44));
            bp.setMargins(dpToPx(4), 0, dpToPx(4), 0);
            btn.setLayoutParams(bp);

            btn.setOnClickListener(v -> {
                selectedLetters.add(letter);
                letterPool.remove(idx);
                updateBuilt();
                renderLetterButtons();
            });

            currentRow.addView(btn);
        }
    }

    private void updateBuilt() {
        tvBuilt.setText(join(selectedLetters));
    }

    private void clearSelection() {
        letterPool.addAll(selectedLetters);
        selectedLetters.clear();
        updateBuilt();
        renderLetterButtons();
    }

    private void checkAnswer() {
        String built  = join(selectedLetters);
        String answer = queue.get(currentIndex).answer;

        if (built.equalsIgnoreCase(answer)) {
            tvFeedback.setText("✓");
            tvFeedback.setTextColor(ContextCompat.getColor(this, R.color.correct_green));
            tvFeedback.setVisibility(View.VISIBLE);
            btnCheck.postDelayed(() -> {
                queue.remove(currentIndex);
                if (currentIndex >= queue.size()) currentIndex = 0;
                if (queue.isEmpty()) { finish(); return; }
                showCurrent();
            }, 500);
        } else {
            tvFeedback.setText("✗  " + answer);
            tvFeedback.setTextColor(ContextCompat.getColor(this, R.color.error_red));
            tvFeedback.setVisibility(View.VISIBLE);
            WordPair wp = queue.remove(currentIndex);
            queue.add(wp);
            if (currentIndex >= queue.size()) currentIndex = 0;
            btnCheck.postDelayed(this::showCurrent, 1000);
        }
    }

    private String join(List<String> list) {
        StringBuilder sb = new StringBuilder();
        for (String s : list) sb.append(s);
        return sb.toString();
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
}