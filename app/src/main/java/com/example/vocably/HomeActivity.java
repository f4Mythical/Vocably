package com.example.vocably;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

public class HomeActivity extends AppCompatActivity {

    private final VersionTracker versionTracker = new VersionTracker();
    private final FirestoreHelper firestoreHelper = new FirestoreHelper();
    private final Handler clockHandler = new Handler(Looper.getMainLooper());

    private TextView tvTime;
    private View booksScrollView;
    private LinearLayout booksRow, directionRow, unitsSection, unitsContainer;
    private LinearLayout sectionsSection, sectionsContainer;
    private TextView btnDirLangToPolish, btnDirPolishToLang, btnDirRandom;
    private TextView btnStartQuiz;
    private TextView selectedUnitBtn = null;

    private String selectedLanguage = null;
    private String selectedBook = null;
    private JSONObject selectedBookJson = null;
    private String selectedDirection = "lang_to_polish";

    private final Map<Integer, List<String>> selectedSections = new HashMap<>();

    private final String[] greetings = {
            "Witaj w Vocably",
            "Hej, miło Cię widzieć",
            "Cześć!",
            "Guten Morgen!",
            "Hello there!",
            "Hi! Gotowy na naukę?",
            "Bon jour!",
            "Ciao!",
            "¡Hola!"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        versionTracker.start(this);

        TextView tvGreeting   = findViewById(R.id.tvGreeting);
        tvTime                = findViewById(R.id.tvTime);
        TextView btnEnglish   = findViewById(R.id.btnEnglish);
        TextView btnGerman    = findViewById(R.id.btnGerman);
        booksScrollView       = findViewById(R.id.booksScrollView);
        booksRow              = findViewById(R.id.booksRow);
        directionRow          = findViewById(R.id.directionRow);
        unitsSection          = findViewById(R.id.unitsSection);
        unitsContainer        = findViewById(R.id.unitsContainer);
        sectionsSection       = findViewById(R.id.sectionsSection);
        sectionsContainer     = findViewById(R.id.sectionsContainer);
        btnDirLangToPolish    = findViewById(R.id.btnDirLangToPolish);
        btnDirPolishToLang    = findViewById(R.id.btnDirPolishToLang);
        btnDirRandom          = findViewById(R.id.btnDirRandom);
        btnStartQuiz          = findViewById(R.id.btnStartQuiz);

        tvGreeting.setText(greetings[new Random().nextInt(greetings.length)]);
        startClock();

        btnEnglish.setOnClickListener(v -> selectLanguage("en", btnEnglish, btnGerman));
        btnGerman.setOnClickListener(v -> selectLanguage("de", btnGerman, btnEnglish));

        btnDirLangToPolish.setOnClickListener(v -> selectDirection("lang_to_polish"));
        btnDirPolishToLang.setOnClickListener(v -> selectDirection("polish_to_lang"));
        btnDirRandom.setOnClickListener(v -> selectDirection("random"));

        btnStartQuiz.setOnClickListener(v -> {});

        createUserCollection();
    }

    private void selectLanguage(String lang, TextView selected, TextView other) {
        selectedLanguage = lang;
        selectedBook = null;
        selectedBookJson = null;
        selectedSections.clear();
        selectedUnitBtn = null;
        unitsSection.setVisibility(View.GONE);
        unitsContainer.removeAllViews();
        sectionsSection.setVisibility(View.GONE);
        sectionsContainer.removeAllViews();
        btnStartQuiz.setVisibility(View.GONE);

        selected.setBackgroundResource(R.drawable.bg_btn_language_selected);
        other.setBackgroundResource(R.drawable.bg_btn_language);

        loadBooks(lang);

        String savedDir = getSharedPreferences("vocably_prefs", Context.MODE_PRIVATE)
                .getString("direction_" + lang, "lang_to_polish");
        selectedDirection = savedDir;

        String langName = lang.equals("en") ? "Angielski" : "Niemiecki";
        btnDirLangToPolish.setText(langName + " → Polski");
        btnDirPolishToLang.setText("Polski → " + langName);
        directionRow.setVisibility(View.VISIBLE);
        updateDirectionButtons();
    }

    private void loadBooks(String lang) {
        booksRow.removeAllViews();
        String[] bookFiles = lang.equals("en")
                ? new String[]{"focus1.json", "focus2.json", "focus3.json", "focus4.json"}
                : new String[]{"focus1_de.json", "focus2_de.json", "focus3_de.json", "focus4_de.json"};

        boolean anyFound = false;
        for (String file : bookFiles) {
            JSONObject json = loadJson(file);
            if (json == null) continue;
            anyFound = true;

            try {
                JSONArray books = json.getJSONArray("books");
                if (books.length() == 0) continue;
                JSONObject book = books.getJSONObject(0);
                String name  = book.getString("name");
                String level = book.getString("level");

                TextView btn = new TextView(this);
                btn.setText(name + "\nPoziom " + level);
                btn.setGravity(android.view.Gravity.CENTER);
                btn.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
                btn.setTextSize(13f);
                btn.setLineSpacing(0, 1.3f);
                btn.setBackground(ContextCompat.getDrawable(this, R.drawable.bg_btn_language));

                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        dpToPx(120), dpToPx(64));
                params.setMarginEnd(dpToPx(10));
                btn.setLayoutParams(params);

                final JSONObject bookJson = book;
                btn.setOnClickListener(v -> selectBook(file, bookJson, btn));
                booksRow.addView(btn);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        booksScrollView.setVisibility(anyFound ? View.VISIBLE : View.GONE);
    }

    private void selectBook(String fileName, JSONObject bookJson, TextView selectedBtn) {
        selectedBook = fileName;
        selectedBookJson = bookJson;
        selectedSections.clear();
        selectedUnitBtn = null;
        unitsContainer.removeAllViews();
        sectionsSection.setVisibility(View.GONE);
        sectionsContainer.removeAllViews();
        btnStartQuiz.setVisibility(View.GONE);

        for (int i = 0; i < booksRow.getChildCount(); i++) {
            booksRow.getChildAt(i).setBackgroundResource(R.drawable.bg_btn_language);
        }
        selectedBtn.setBackgroundResource(R.drawable.bg_btn_language_selected);

        loadUnits(bookJson);
    }

    private void loadUnits(JSONObject bookJson) {
        unitsContainer.removeAllViews();

        try {
            JSONArray units = bookJson.getJSONArray("units");
            int cols = 4;
            LinearLayout currentRow = null;

            for (int i = 0; i < units.length(); i++) {
                JSONObject unit = units.getJSONObject(i);
                int unitNumber = unit.getInt("number");

                if (i % cols == 0) {
                    currentRow = new LinearLayout(this);
                    currentRow.setOrientation(LinearLayout.HORIZONTAL);
                    LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT);
                    rowParams.setMargins(0, 0, 0, dpToPx(8));
                    currentRow.setLayoutParams(rowParams);
                    unitsContainer.addView(currentRow);
                }

                TextView unitBtn = new TextView(this);
                unitBtn.setText("Unit " + unitNumber);
                unitBtn.setGravity(android.view.Gravity.CENTER);
                unitBtn.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
                unitBtn.setTextSize(13f);
                unitBtn.setBackground(ContextCompat.getDrawable(this, R.drawable.bg_btn_language));

                LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(0, dpToPx(48), 1f);
                btnParams.setMargins(0, 0, i % cols == cols - 1 ? 0 : dpToPx(8), 0);
                unitBtn.setLayoutParams(btnParams);

                final JSONObject finalUnit = unit;
                final int finalUnitNumber = unitNumber;
                unitBtn.setOnClickListener(v -> selectUnit(unitBtn, finalUnit, finalUnitNumber));
                currentRow.addView(unitBtn);
            }

            if (units.length() % cols != 0) {
                int remaining = cols - (units.length() % cols);
                for (int i = 0; i < remaining; i++) {
                    View spacer = new View(this);
                    LinearLayout.LayoutParams spacerParams = new LinearLayout.LayoutParams(
                            0, dpToPx(48), 1f);
                    spacerParams.setMargins(0, 0, i == remaining - 1 ? 0 : dpToPx(8), 0);
                    spacer.setLayoutParams(spacerParams);
                    currentRow.addView(spacer);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        unitsSection.setVisibility(View.VISIBLE);
    }

    private void selectUnit(TextView unitBtn, JSONObject unit, int unitNumber) {
        if (selectedUnitBtn != null) {
            selectedUnitBtn.setBackgroundResource(R.drawable.bg_btn_language);
        }
        selectedUnitBtn = unitBtn;
        unitBtn.setBackgroundResource(R.drawable.bg_btn_language_selected);

        selectedSections.clear();
        sectionsContainer.removeAllViews();
        btnStartQuiz.setVisibility(View.GONE);

        loadSections(unit, unitNumber);
    }

    private void loadSections(JSONObject unit, int unitNumber) {
        sectionsContainer.removeAllViews();

        try {
            JSONArray sections = unit.getJSONArray("sections");
            List<String> defaultSelected = new ArrayList<>();

            for (int j = 0; j < sections.length(); j++) {
                JSONObject section = sections.getJSONObject(j);
                if (!section.has("number") || !section.has("words")) continue;

                String sectionNumber = section.getString("number");
                JSONArray words = section.getJSONArray("words");
                if (words.length() == 0) continue;

                String sectionTitle = section.optString("title", "");
                String label = sectionNumber + (sectionTitle.isEmpty() ? "" : "  " + sectionTitle);

                defaultSelected.add(sectionNumber);

                TextView sectionBtn = new TextView(this);
                sectionBtn.setText(label);
                sectionBtn.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
                sectionBtn.setTextSize(13f);
                sectionBtn.setBackground(ContextCompat.getDrawable(this, R.drawable.bg_btn_language_selected));

                LinearLayout.LayoutParams sParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(48));
                sParams.setMargins(0, 0, 0, dpToPx(8));
                sectionBtn.setLayoutParams(sParams);
                sectionBtn.setGravity(android.view.Gravity.CENTER_VERTICAL | android.view.Gravity.START);
                sectionBtn.setPadding(dpToPx(16), 0, dpToPx(16), 0);

                final String finalSectionNumber = sectionNumber;
                sectionBtn.setOnClickListener(v -> {
                    List<String> list = selectedSections.getOrDefault(unitNumber, new ArrayList<>());
                    if (list.contains(finalSectionNumber)) {
                        list.remove(finalSectionNumber);
                        sectionBtn.setBackgroundResource(R.drawable.bg_btn_language);
                    } else {
                        list.add(finalSectionNumber);
                        sectionBtn.setBackgroundResource(R.drawable.bg_btn_language_selected);
                    }
                    selectedSections.put(unitNumber, list);
                    updateStartButton();
                });

                sectionsContainer.addView(sectionBtn);
            }

            selectedSections.put(unitNumber, defaultSelected);
            sectionsSection.setVisibility(View.VISIBLE);
            updateStartButton();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateStartButton() {
        boolean any = false;
        for (List<String> list : selectedSections.values()) {
            if (!list.isEmpty()) { any = true; break; }
        }
        btnStartQuiz.setVisibility(any ? View.VISIBLE : View.GONE);
    }

    private void selectDirection(String dir) {
        selectedDirection = dir;
        getSharedPreferences("vocably_prefs", Context.MODE_PRIVATE)
                .edit()
                .putString("direction_" + selectedLanguage, dir)
                .apply();
        updateDirectionButtons();
    }

    private void updateDirectionButtons() {
        int active   = R.drawable.bg_btn_language_selected;
        int inactive = R.drawable.bg_btn_language;
        btnDirLangToPolish.setBackgroundResource(selectedDirection.equals("lang_to_polish") ? active : inactive);
        btnDirPolishToLang.setBackgroundResource(selectedDirection.equals("polish_to_lang") ? active : inactive);
        btnDirRandom.setBackgroundResource(selectedDirection.equals("random") ? active : inactive);
    }

    private JSONObject loadJson(String fileName) {
        try {
            InputStream is = getAssets().open(fileName);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            return new JSONObject(new String(buffer, StandardCharsets.UTF_8));
        } catch (Exception e) {
            return null;
        }
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    private void startClock() {
        clockHandler.post(new Runnable() {
            @Override
            public void run() {
                tvTime.setText(new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date()));
                clockHandler.postDelayed(this, 30000);
            }
        });
    }

    private void createUserCollection() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            firestoreHelper.createUserIfNotExists(user.getUid(), user.getEmail());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        clockHandler.removeCallbacksAndMessages(null);
        versionTracker.stop();
    }
}