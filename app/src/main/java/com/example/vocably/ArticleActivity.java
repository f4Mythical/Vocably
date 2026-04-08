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

public class ArticleActivity extends AppCompatActivity {

    public static final String EXTRA_LANGUAGE        = "language";
    public static final String EXTRA_BOOK_FILE       = "book_file";
    public static final String EXTRA_UNIT_NUMBER     = "unit_number";
    public static final String EXTRA_SECTION_NUMBERS = "section_numbers";

    private static class Word {
        String article;
        String noun;
        String polish;
        Word(String article, String noun, String polish) {
            this.article = article;
            this.noun    = noun;
            this.polish  = polish;
        }
    }

    private List<Word> allWords    = new ArrayList<>();
    private List<Word> queue       = new ArrayList<>();
    private List<Word> wrongPile   = new ArrayList<>();
    private int        currentIdx  = 0;
    private int        totalCount  = 0;
    private int        correctCount = 0;
    private int        wrongCount   = 0;

    private TextView     tvProgress;
    private TextView     tvNoun;
    private TextView     tvPolish;
    private View         translationPanel;
    private TextView     tvFeedback;
    private View         cardView;
    private LinearLayout btnDer, btnDie, btnDas;
    private View         studyLayout, summaryLayout;
    private TextView     tvCorrectCount, tvWrongCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_article);

        bindViews();
        loadWords();

        if (allWords.isEmpty()) {
            finish();
            return;
        }

        queue.addAll(allWords);
        Collections.shuffle(queue);
        totalCount = queue.size();
        showCard();

        btnDer.setOnClickListener(v -> answer("der"));
        btnDie.setOnClickListener(v -> answer("die"));
        btnDas.setOnClickListener(v -> answer("das"));
    }

    private void bindViews() {
        tvProgress       = findViewById(R.id.tvProgress);
        tvNoun           = findViewById(R.id.tvNoun);
        tvPolish         = findViewById(R.id.tvPolish);
        translationPanel = findViewById(R.id.translationPanel);
        tvFeedback       = findViewById(R.id.tvFeedback);
        cardView         = findViewById(R.id.cardView);
        btnDer           = findViewById(R.id.btnDer);
        btnDie           = findViewById(R.id.btnDie);
        btnDas           = findViewById(R.id.btnDas);
        studyLayout      = findViewById(R.id.studyLayout);
        summaryLayout    = findViewById(R.id.summaryLayout);
        tvCorrectCount   = findViewById(R.id.tvCorrectCount);
        tvWrongCount     = findViewById(R.id.tvWrongCount);
    }

    private void showCard() {
        if (currentIdx >= queue.size()) {
            showSummary();
            return;
        }

        tvFeedback.setVisibility(View.GONE);
        translationPanel.setVisibility(View.GONE);
        resetButtons();

        Word word = queue.get(currentIdx);
        tvProgress.setText((totalCount - queue.size() + currentIdx + 1) + " / " + totalCount);
        tvNoun.setText(word.noun);
        tvPolish.setText(word.polish);
    }

    private void answer(String chosen) {
        Word word = queue.get(currentIdx);

        disableButtons();

        if (chosen.equals(word.article)) {
            correctCount++;
            highlightButton(chosen, true);
            tvFeedback.setText("✓  " + word.article + " " + word.noun);
            tvFeedback.setTextColor(ContextCompat.getColor(this, R.color.correct_green));
            tvFeedback.setVisibility(View.VISIBLE);
            translationPanel.setVisibility(View.VISIBLE);

            btnDer.postDelayed(() -> {
                queue.remove(currentIdx);
                if (currentIdx >= queue.size()) currentIdx = 0;
                if (queue.isEmpty()) { showSummary(); return; }
                showCard();
            }, 600);
        } else {
            wrongCount++;
            highlightButton(chosen, false);
            highlightButton(word.article, true);
            tvFeedback.setText("✗  " + word.article + " " + word.noun);
            tvFeedback.setTextColor(ContextCompat.getColor(this, R.color.error_red));
            tvFeedback.setVisibility(View.VISIBLE);
            translationPanel.setVisibility(View.VISIBLE);

            Word wp = queue.remove(currentIdx);
            queue.add(wp);
            if (currentIdx >= queue.size()) currentIdx = 0;
            btnDer.postDelayed(this::showCard, 900);
        }
    }

    private void highlightButton(String article, boolean correct) {
        LinearLayout btn = getBtn(article);
        if (btn == null) return;
        btn.setBackgroundResource(correct
                ? R.drawable.bg_btn_correct
                : R.drawable.bg_btn_wrong);
    }

    private LinearLayout getBtn(String article) {
        switch (article) {
            case "der": return btnDer;
            case "die": return btnDie;
            case "das": return btnDas;
        }
        return null;
    }

    private void resetButtons() {
        btnDer.setBackgroundResource(R.drawable.bg_btn_language);
        btnDie.setBackgroundResource(R.drawable.bg_btn_language);
        btnDas.setBackgroundResource(R.drawable.bg_btn_language);
        btnDer.setEnabled(true);
        btnDie.setEnabled(true);
        btnDas.setEnabled(true);
    }

    private void disableButtons() {
        btnDer.setEnabled(false);
        btnDie.setEnabled(false);
        btnDas.setEnabled(false);
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
                                if (tr.optString("language").equals("de")) {
                                    foreign = tr.optString("value", "");
                                    break;
                                }
                            }
                        }
                        if (foreign.isEmpty() || polish.isEmpty()) continue;

                        String lowerForeign = foreign.toLowerCase();
                        String article = null;
                        String noun    = foreign;

                        if (lowerForeign.startsWith("der ")) {
                            article = "der";
                            noun    = foreign.substring(4);
                        } else if (lowerForeign.startsWith("die ")) {
                            article = "die";
                            noun    = foreign.substring(4);
                        } else if (lowerForeign.startsWith("das ")) {
                            article = "das";
                            noun    = foreign.substring(4);
                        }

                        if (article != null) {
                            allWords.add(new Word(article, noun, polish));
                        }
                    }
                }
                break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showSummary() {
        studyLayout.setVisibility(View.GONE);
        summaryLayout.setVisibility(View.VISIBLE);
        tvCorrectCount.setText(String.valueOf(correctCount));
        tvWrongCount.setText(String.valueOf(wrongCount));

        TextView btnRepeat = findViewById(R.id.btnRepeatUnknown);
        TextView btnFinish = findViewById(R.id.btnFinish);

        btnFinish.setOnClickListener(v -> finish());
        btnRepeat.setOnClickListener(v -> {
            queue.clear();
            queue.addAll(allWords);
            Collections.shuffle(queue);
            totalCount   = queue.size();
            currentIdx   = 0;
            correctCount = 0;
            wrongCount   = 0;
            summaryLayout.setVisibility(View.GONE);
            studyLayout.setVisibility(View.VISIBLE);
            showCard();
        });
    }
}