package com.example.vocably;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
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

public class ChoiceActivity extends AppCompatActivity {

    public static final String EXTRA_LANGUAGE        = "language";
    public static final String EXTRA_BOOK_FILE       = "book_file";
    public static final String EXTRA_UNIT_NUMBER     = "unit_number";
    public static final String EXTRA_SECTION_NUMBERS = "section_numbers";

    private TextView tvProgress, tvPromptLang, tvPrompt;
    private TextView tvFeedback;
    private TextView btnNext;
    private TextView[] optionBtns = new TextView[4];
    private View studyLayout, summaryLayout;
    private TextView tvCorrectCount, tvWrongCount;

    private List<Word> allWords   = new ArrayList<>();
    private int        currentIdx = 0;
    private boolean    answered   = false;
    private boolean    currentLangToPolish = true;
    private String     direction;
    private String     language;
    private int        correctCount = 0;
    private int        wrongCount   = 0;

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
        setContentView(R.layout.activity_choice);

        language = getIntent().getStringExtra(EXTRA_LANGUAGE);

        bindViews();
        loadDirection();
        loadWords();

        if (allWords.isEmpty()) {
            finish();
            return;
        }

        Collections.shuffle(allWords);
        showCard();
    }

    private void bindViews() {
        tvProgress   = findViewById(R.id.tvProgress);
        tvPromptLang = findViewById(R.id.tvPromptLang);
        tvPrompt     = findViewById(R.id.tvPrompt);
        tvFeedback   = findViewById(R.id.tvFeedback);
        btnNext      = findViewById(R.id.btnNext);
        studyLayout  = findViewById(R.id.studyLayout);
        summaryLayout = findViewById(R.id.summaryLayout);
        tvCorrectCount = findViewById(R.id.tvCorrectCount);
        tvWrongCount   = findViewById(R.id.tvWrongCount);

        optionBtns[0] = findViewById(R.id.btnOption0);
        optionBtns[1] = findViewById(R.id.btnOption1);
        optionBtns[2] = findViewById(R.id.btnOption2);
        optionBtns[3] = findViewById(R.id.btnOption3);

        btnNext.setOnClickListener(v -> {
            currentIdx++;
            showCard();
        });
    }

    private void loadDirection() {
        String prefKey = "en".equals(language) ? "direction_en" : "direction_de";
        direction = getSharedPreferences("vocably_prefs", Context.MODE_PRIVATE)
                .getString(prefKey, "lang_to_polish");
    }

    private void loadWords() {
        String bookFile = getIntent().getStringExtra(EXTRA_BOOK_FILE);
        int unitNumber  = getIntent().getIntExtra(EXTRA_UNIT_NUMBER, -1);
        ArrayList<String> sectionNumbers = getIntent().getStringArrayListExtra(EXTRA_SECTION_NUMBERS);

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

    private void showCard() {
        if (currentIdx >= allWords.size()) {
            showSummary();
            return;
        }

        answered = false;
        currentLangToPolish = resolveDirection();
        tvFeedback.setVisibility(View.GONE);
        btnNext.setVisibility(View.GONE);

        for (TextView btn : optionBtns) {
            btn.setBackgroundResource(R.drawable.bg_btn_language);
            btn.setEnabled(true);
        }

        Word word = allWords.get(currentIdx);
        tvProgress.setText((currentIdx + 1) + " / " + allWords.size());

        if (currentLangToPolish) {
            tvPromptLang.setText(getForeignLangLabel());
            tvPrompt.setText(word.foreign);
        } else {
            tvPromptLang.setText("Polski");
            tvPrompt.setText(word.polish);
        }

        setupOptions(word);
    }

    private void setupOptions(Word correct) {
        String correctAnswer = currentLangToPolish ? correct.polish : correct.foreign;

        List<String> options = new ArrayList<>();
        options.add(correctAnswer);

        List<Word> pool = new ArrayList<>(allWords);
        pool.remove(correct);
        Collections.shuffle(pool);

        for (Word w : pool) {
            String candidate = currentLangToPolish ? w.polish : w.foreign;
            if (!candidate.equals(correctAnswer) && !options.contains(candidate)) {
                options.add(candidate);
            }
            if (options.size() == 4) break;
        }

        while (options.size() < 4) options.add("—");

        Collections.shuffle(options);

        for (int i = 0; i < 4; i++) {
            String opt = options.get(i);
            optionBtns[i].setText(opt);
            optionBtns[i].setOnClickListener(v -> onOptionSelected(optionBtns[indexOf(opt)], opt, correctAnswer));
        }
    }

    private int indexOf(String opt) {
        for (int i = 0; i < 4; i++) {
            if (optionBtns[i].getText().toString().equals(opt)) return i;
        }
        return 0;
    }

    private void onOptionSelected(TextView selected, String chosen, String correct) {
        if (answered) return;
        answered = true;

        for (TextView btn : optionBtns) btn.setEnabled(false);

        if (chosen.equals(correct)) {
            correctCount++;
            selected.setBackgroundResource(R.drawable.bg_btn_correct);
            tvFeedback.setVisibility(View.GONE);
        } else {
            wrongCount++;
            selected.setBackgroundResource(R.drawable.bg_btn_wrong);
            for (TextView btn : optionBtns) {
                if (btn.getText().toString().equals(correct)) {
                    btn.setBackgroundResource(R.drawable.bg_btn_correct);
                    break;
                }
            }
            tvFeedback.setText("Poprawna odpowiedź: " + correct);
            tvFeedback.setVisibility(View.VISIBLE);
        }

        btnNext.setVisibility(View.VISIBLE);
    }

    private boolean resolveDirection() {
        if ("random".equals(direction)) return new Random().nextBoolean();
        return "lang_to_polish".equals(direction);
    }

    private String getForeignLangLabel() {
        return "de".equals(language) ? "Niemiecki" : "Angielski";
    }

    private void showSummary() {
        studyLayout.setVisibility(View.GONE);
        summaryLayout.setVisibility(View.VISIBLE);
        tvCorrectCount.setText(String.valueOf(correctCount));
        tvWrongCount.setText(String.valueOf(wrongCount));
        findViewById(R.id.btnFinish).setOnClickListener(v -> finish());
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
}