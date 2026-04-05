package com.example.vocably;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class TypingActivity extends AppCompatActivity {

    public static final String EXTRA_LANGUAGE        = "language";
    public static final String EXTRA_BOOK_FILE       = "book_file";
    public static final String EXTRA_UNIT_NUMBER     = "unit_number";
    public static final String EXTRA_SECTION_NUMBERS = "section_numbers";

    private TextView tvProgress, tvPromptLang, tvPrompt;
    private TextView tvInputDisplay, tvFeedback, btnCheck;
    private View     studyLayout, summaryLayout;
    private TextView tvCorrectCount, tvWrongCount;
    private View     keyboardEnWrapper, keyboardDeWrapper;

    private PopupWindow activePopup = null;

    private List<Word> allWords        = new ArrayList<>();
    private int        currentIdx      = 0;
    private String     inputBuffer     = "";
    private boolean    checked         = false;
    private boolean    currentLangToPolish = true;
    private String     direction;
    private String     language;
    private int        correctCount    = 0;
    private int        wrongCount      = 0;

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
        setContentView(R.layout.activity_typing);

        language = getIntent().getStringExtra(EXTRA_LANGUAGE);

        bindViews();
        loadDirection();
        loadWords();

        if (allWords.isEmpty()) {
            finish();
            return;
        }

        Collections.shuffle(allWords);
        setupKeyboard();
        showCard();
    }

    private void bindViews() {
        tvProgress      = findViewById(R.id.tvProgress);
        tvPromptLang    = findViewById(R.id.tvPromptLang);
        tvPrompt        = findViewById(R.id.tvPrompt);
        tvInputDisplay  = findViewById(R.id.tvInputDisplay);
        tvFeedback      = findViewById(R.id.tvFeedback);
        btnCheck        = findViewById(R.id.btnCheck);
        studyLayout     = findViewById(R.id.studyLayout);
        summaryLayout   = findViewById(R.id.summaryLayout);
        tvCorrectCount  = findViewById(R.id.tvCorrectCount);
        tvWrongCount    = findViewById(R.id.tvWrongCount);
        keyboardEnWrapper = findViewById(R.id.keyboardEnWrapper);
        keyboardDeWrapper = findViewById(R.id.keyboardDeWrapper);

        btnCheck.setOnClickListener(v -> onCheckOrNext());
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

        checked = false;
        inputBuffer = "";
        currentLangToPolish = resolveDirection();
        tvInputDisplay.setText("");
        tvInputDisplay.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        tvInputDisplay.setBackgroundResource(R.drawable.bg_btn_language);
        tvFeedback.setVisibility(View.GONE);
        btnCheck.setText("Sprawdź");

        Word word = allWords.get(currentIdx);
        tvProgress.setText((currentIdx + 1) + " / " + allWords.size());

        if (currentLangToPolish) {
            tvPromptLang.setText(getForeignLangLabel());
            tvPrompt.setText(word.foreign);
        } else {
            tvPromptLang.setText("Polski");
            tvPrompt.setText(word.polish);
        }
    }

    private void onCheckOrNext() {
        if (!checked) {
            check();
        } else {
            currentIdx++;
            showCard();
        }
    }

    private void check() {
        checked = true;

        Word word = allWords.get(currentIdx);
        String answer = currentLangToPolish ? word.polish : word.foreign;

        boolean correct = inputBuffer.trim().equalsIgnoreCase(answer.trim());

        if (correct) {
            correctCount++;
            tvInputDisplay.setTextColor(ContextCompat.getColor(this, R.color.correct_green));
            tvInputDisplay.setBackgroundResource(R.drawable.bg_btn_language_selected);
            tvFeedback.setVisibility(View.GONE);
        } else {
            wrongCount++;
            tvInputDisplay.setTextColor(ContextCompat.getColor(this, R.color.error_red));
            tvInputDisplay.setBackgroundResource(R.drawable.bg_btn_language);
            tvFeedback.setText("Poprawna odpowiedź: " + answer);
            tvFeedback.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
            tvFeedback.setVisibility(View.VISIBLE);
        }

        btnCheck.setText("Dalej →");
    }

    private void setupKeyboard() {
        if ("de".equals(language)) {
            keyboardDeWrapper.setVisibility(View.VISIBLE);
            keyboardEnWrapper.setVisibility(View.GONE);
            setupDeKeys();
        } else {
            keyboardEnWrapper.setVisibility(View.VISIBLE);
            keyboardDeWrapper.setVisibility(View.GONE);
            setupEnKeys();
        }
    }

    private void setupEnKeys() {
        String[] ids = {"en_q","en_w","en_e","en_r","en_t","en_y","en_u","en_i","en_o","en_p",
                "en_a","en_s","en_d","en_f","en_g","en_h","en_j","en_k","en_l",
                "en_z","en_x","en_c","en_v","en_b","en_n","en_m"};
        String[] chars = {"q","w","e","r","t","y","u","i","o","p",
                "a","s","d","f","g","h","j","k","l",
                "z","x","c","v","b","n","m"};

        for (int i = 0; i < ids.length; i++) {
            int resId = getResources().getIdentifier(ids[i], "id", getPackageName());
            TextView key = findViewById(resId);
            if (key == null) continue;
            String ch = chars[i];
            key.setOnClickListener(v -> typeChar(ch));
        }

        Map<String, String> enLongPress = new HashMap<>();
        enLongPress.put("en_a", "ą");
        enLongPress.put("en_e", "ę");
        enLongPress.put("en_o", "ó");
        enLongPress.put("en_s", "ś");
        enLongPress.put("en_z", "ź");
        enLongPress.put("en_x", "ż");
        enLongPress.put("en_c", "ć");
        enLongPress.put("en_n", "ń");
        enLongPress.put("en_l", "ł");

        for (Map.Entry<String, String> entry : enLongPress.entrySet()) {
            int resId = getResources().getIdentifier(entry.getKey(), "id", getPackageName());
            TextView key = findViewById(resId);
            if (key == null) continue;
            String ch = entry.getValue();
            key.setOnLongClickListener(v -> {
                showKeyPopup(v, ch);
                typeChar(ch);
                return true;
            });
        }

        findViewById(R.id.en_space).setOnClickListener(v -> typeChar(" "));
        findViewById(R.id.en_backspace).setOnClickListener(v -> backspace());
    }

    private void setupDeKeys() {
        String[] ids = {"de_q","de_w","de_e","de_r","de_t","de_z","de_u","de_i","de_o","de_p",
                "de_a","de_s","de_d","de_f","de_g","de_h","de_j","de_k","de_l",
                "de_y","de_x","de_c","de_v","de_b","de_n","de_m"};
        String[] chars = {"q","w","e","r","t","z","u","i","o","p",
                "a","s","d","f","g","h","j","k","l",
                "y","x","c","v","b","n","m"};

        for (int i = 0; i < ids.length; i++) {
            int resId = getResources().getIdentifier(ids[i], "id", getPackageName());
            TextView key = findViewById(resId);
            if (key == null) continue;
            String ch = chars[i];
            key.setOnClickListener(v -> typeChar(ch));
        }

        findViewById(R.id.de_ae).setOnClickListener(v -> typeChar("ä"));
        findViewById(R.id.de_oe).setOnClickListener(v -> typeChar("ö"));
        findViewById(R.id.de_ue).setOnClickListener(v -> typeChar("ü"));
        findViewById(R.id.de_sz).setOnClickListener(v -> typeChar("ß"));
        findViewById(R.id.de_space).setOnClickListener(v -> typeChar(" "));
        findViewById(R.id.de_backspace).setOnClickListener(v -> backspace());

        findViewById(R.id.de_der).setOnClickListener(v -> typeWord("der "));
        findViewById(R.id.de_die).setOnClickListener(v -> typeWord("die "));
        findViewById(R.id.de_das).setOnClickListener(v -> typeWord("das "));

        Map<String, String> deLongPress = new HashMap<>();
        deLongPress.put("de_a", "ą");
        deLongPress.put("de_e", "ę");
        deLongPress.put("de_o", "ó");
        deLongPress.put("de_s", "ś");
        deLongPress.put("de_z", "ź");
        deLongPress.put("de_x", "ż");
        deLongPress.put("de_c", "ć");
        deLongPress.put("de_n", "ń");
        deLongPress.put("de_l", "ł");

        for (Map.Entry<String, String> entry : deLongPress.entrySet()) {
            int resId = getResources().getIdentifier(entry.getKey(), "id", getPackageName());
            TextView key = findViewById(resId);
            if (key == null) continue;
            String ch = entry.getValue();
            key.setOnLongClickListener(v -> {
                showKeyPopup(v, ch);
                typeChar(ch);
                return true;
            });
        }
    }

    private void showKeyPopup(View anchor, String character) {
        if (activePopup != null && activePopup.isShowing()) {
            activePopup.dismiss();
        }

        TextView popupText = new TextView(this);
        popupText.setText(character);
        popupText.setTextSize(24f);
        popupText.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        popupText.setBackgroundResource(R.drawable.bg_btn_language_selected);
        popupText.setGravity(Gravity.CENTER);
        popupText.setPadding(0, 8, 0, 8);

        int size = (int) (anchor.getWidth() * 1.4f);

        PopupWindow popup = new PopupWindow(popupText, size, anchor.getHeight() + 20, false);
        popup.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        popup.setElevation(12f);

        int[] location = new int[2];
        anchor.getLocationInWindow(location);
        int x = location[0] + anchor.getWidth() / 2 - size / 2;
        int y = location[1] - anchor.getHeight() - 20;

        popup.showAtLocation(anchor, Gravity.NO_GRAVITY, x, y);
        activePopup = popup;

        anchor.postDelayed(() -> {
            if (popup.isShowing()) popup.dismiss();
        }, 600);
    }

    private void typeChar(String ch) {
        if (checked) return;
        inputBuffer += ch;
        tvInputDisplay.setText(inputBuffer);
    }

    private void typeWord(String word) {
        if (checked) return;
        inputBuffer = word;
        tvInputDisplay.setText(inputBuffer);
    }

    private void backspace() {
        if (checked || inputBuffer.isEmpty()) return;
        inputBuffer = inputBuffer.substring(0, inputBuffer.length() - 1);
        tvInputDisplay.setText(inputBuffer);
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