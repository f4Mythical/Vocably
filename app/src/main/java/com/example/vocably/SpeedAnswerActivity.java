package com.example.vocably;

import android.app.AlertDialog;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.widget.ProgressBar;
import android.widget.SeekBar;
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

public class SpeedAnswerActivity extends AppCompatActivity {

    public static final String EXTRA_LANGUAGE        = "language";
    public static final String EXTRA_BOOK_FILE       = "book_file";
    public static final String EXTRA_UNIT_NUMBER     = "unit_number";
    public static final String EXTRA_SECTION_NUMBERS = "section_numbers";

    private TextView    tvProgress, tvPromptLang, tvPrompt;
    private View        translationPanel;
    private TextView    tvAnswerLang, tvAnswer;
    private View        cardView;
    private ProgressBar timerBar;
    private SeekBar     speedSlider;
    private TextView    tvSpeedLabel;
    private View        studyLayout;

    private List<Word> allWords   = new ArrayList<>();
    private int        currentIdx = 0;
    private boolean    currentLangToPolish = true;
    private String     direction;
    private String     language;

    private CountDownTimer cardTimer;
    private int            cardDurationSec = 3;

    private GestureDetector gestureDetector;

    private static class Word {
        String foreign;
        String polish;
        Word(String f, String p) { foreign = f; polish = p; }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_speed_answer);

        language = getIntent().getStringExtra(EXTRA_LANGUAGE);

        bindViews();
        loadDirection();
        loadWords();

        if (allWords.isEmpty()) { finish(); return; }

        Collections.shuffle(allWords);
        setupSlider();
        setupGesture();
        showCard();
    }

    private void bindViews() {
        tvProgress      = findViewById(R.id.tvProgress);
        tvPromptLang    = findViewById(R.id.tvPromptLang);
        tvPrompt        = findViewById(R.id.tvPrompt);
        translationPanel = findViewById(R.id.translationPanel);
        tvAnswerLang    = findViewById(R.id.tvAnswerLang);
        tvAnswer        = findViewById(R.id.tvAnswer);
        cardView        = findViewById(R.id.cardView);
        timerBar        = findViewById(R.id.timerBar);
        speedSlider     = findViewById(R.id.speedSlider);
        tvSpeedLabel    = findViewById(R.id.tvSpeedLabel);
        studyLayout     = findViewById(R.id.studyLayout);
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
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void setupSlider() {
        speedSlider.setMin(1);
        speedSlider.setMax(10);
        speedSlider.setProgress(cardDurationSec);
        tvSpeedLabel.setText(cardDurationSec + "s");

        speedSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int val, boolean fromUser) {
                cardDurationSec = val;
                tvSpeedLabel.setText(val + "s");
                if (fromUser) {
                    cancelTimer();
                    startCardTimer();
                }
            }
            @Override public void onStartTrackingTouch(SeekBar s) { cancelTimer(); }
            @Override public void onStopTrackingTouch(SeekBar s)  { startCardTimer(); }
        });
    }

    private void showCard() {
        if (currentIdx >= allWords.size()) {
            showFinishDialog();
            return;
        }

        translationPanel.setVisibility(View.GONE);
        translationPanel.setTranslationY(0);
        currentLangToPolish = resolveDirection();

        Word word = allWords.get(currentIdx);
        tvProgress.setText((currentIdx + 1) + " / " + allWords.size());

        if (currentLangToPolish) {
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

        startCardTimer();
    }

    private void startCardTimer() {
        cancelTimer();
        long durationMs = cardDurationSec * 1000L;
        timerBar.setMax((int) durationMs);
        timerBar.setProgress((int) durationMs);

        cardTimer = new CountDownTimer(durationMs, 16) {
            @Override public void onTick(long ms) {
                timerBar.setProgress((int) ms);
            }
            @Override public void onFinish() {
                timerBar.setProgress(0);
                revealAndAdvance();
            }
        }.start();
    }

    private void cancelTimer() {
        if (cardTimer != null) { cardTimer.cancel(); cardTimer = null; }
    }

    private void revealAndAdvance() {
        translationPanel.setVisibility(View.VISIBLE);
        translationPanel.post(() -> {
            float startY = translationPanel.getHeight() > 0 ? translationPanel.getHeight() : 200f;
            translationPanel.setTranslationY(startY);
            ObjectAnimator anim = ObjectAnimator.ofFloat(translationPanel, "translationY", startY, 0f);
            anim.setDuration(220);
            anim.setInterpolator(new DecelerateInterpolator());
            anim.start();
        });

        cardView.postDelayed(() -> {
            currentIdx++;
            cardView.setTranslationX(0f);
            cardView.setAlpha(1f);
            showCard();
        }, 700);
    }

    private void setupGesture() {
        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float vX, float vY) {
                if (e1 == null || e2 == null) return false;
                float dX = e2.getX() - e1.getX();
                float dY = e2.getY() - e1.getY();
                if (Math.abs(dX) > Math.abs(dY) && Math.abs(dX) > 80 && Math.abs(vX) > 80) {
                    cancelTimer();
                    currentIdx++;
                    cardView.animate()
                            .translationX(dX > 0 ? 1200f : -1200f)
                            .alpha(0f).setDuration(180)
                            .withEndAction(() -> {
                                cardView.setTranslationX(0f);
                                cardView.setAlpha(1f);
                                showCard();
                            }).start();
                    return true;
                }
                return false;
            }
            @Override public boolean onDown(MotionEvent e) { return true; }
        });

        cardView.setOnTouchListener((v, e) -> gestureDetector.onTouchEvent(e));
    }

    private void showFinishDialog() {
        cancelTimer();
        new AlertDialog.Builder(this)
                .setTitle("Koniec!")
                .setMessage("Przejrzałeś wszystkie słówka.")
                .setPositiveButton("Powtórz", (d, w) -> {
                    Collections.shuffle(allWords);
                    currentIdx = 0;
                    showCard();
                })
                .setNegativeButton("Zakończ", (d, w) -> finish())
                .setCancelable(false)
                .show();
    }

    private boolean resolveDirection() {
        if ("random".equals(direction)) return new Random().nextBoolean();
        return "lang_to_polish".equals(direction);
    }

    private String getForeignLangLabel() {
        return "de".equals(language) ? "Niemiecki" : "Angielski";
    }

    @Override protected void onDestroy() { super.onDestroy(); cancelTimer(); }

    @Override public boolean onTouchEvent(MotionEvent e) {
        return gestureDetector.onTouchEvent(e) || super.onTouchEvent(e);
    }

    private JSONObject loadJson(String fileName) {
        try {
            InputStream is = getAssets().open(fileName);
            byte[] buf = new byte[is.available()];
            is.read(buf);
            is.close();
            return new JSONObject(new String(buf, StandardCharsets.UTF_8));
        } catch (Exception e) { return null; }
    }
}