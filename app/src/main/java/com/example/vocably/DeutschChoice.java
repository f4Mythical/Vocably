package com.example.vocably;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DeutschChoice extends AppCompatActivity {

    private LinearLayout booksRow, unitsContainer, sectionsSection, sectionsContainer;
    private LinearLayout directionRow, quizSection, quizContainer;
    private TextView btnDirLangToPolish, btnDirPolishToLang, btnDirRandom;
    private TextView selectedUnitBtn = null;
    private View booksScrollView;

    private String selectedDirection = "lang_to_polish";
    private final Map<Integer, List<String>> selectedSections = new HashMap<>();

    private String currentBookFile   = null;
    private String currentBookName   = "";
    private int    currentUnitNumber = -1;

    private static final String LANG = "de";

    private static final String[] BOOK_FILES = {
            "trends1.json", "trends2.json", "trends3.json", "trends4.json"
    };

    private static final String[] QUIZ_MODES = {
            "Wyszukiwanie", "Fiszki", "Wpisz", "Wybór", "Szybkie fiszki",
            "Losowa nauka", "Memory", "Szybka odpowiedź", "Lista", "Litera po literze", "Prawda czy Fałsz",
            "Ułóż słowo"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_language_choice);

        booksScrollView    = findViewById(R.id.booksScrollView);
        booksRow           = findViewById(R.id.booksRow);
        directionRow       = findViewById(R.id.directionRow);
        unitsContainer     = findViewById(R.id.unitsContainer);
        sectionsSection    = findViewById(R.id.sectionsSection);
        sectionsContainer  = findViewById(R.id.sectionsContainer);
        quizSection        = findViewById(R.id.quizSection);
        quizContainer      = findViewById(R.id.quizContainer);
        btnDirLangToPolish = findViewById(R.id.btnDirLangToPolish);
        btnDirPolishToLang = findViewById(R.id.btnDirPolishToLang);
        btnDirRandom       = findViewById(R.id.btnDirRandom);

        selectedDirection = getSharedPreferences("vocably_prefs", Context.MODE_PRIVATE)
                .getString("direction_de", "lang_to_polish");

        btnDirLangToPolish.setText("Niemiecki → Polski");
        btnDirPolishToLang.setText("Polski → Niemiecki");

        btnDirLangToPolish.setOnClickListener(v -> selectDirection("lang_to_polish"));
        btnDirPolishToLang.setOnClickListener(v -> selectDirection("polish_to_lang"));
        btnDirRandom.setOnClickListener(v -> selectDirection("random"));

        loadBooks();
    }

    private Set<String> getFavs() {
        return new LinkedHashSet<>(getSharedPreferences("vocably_favorites", Context.MODE_PRIVATE)
                .getStringSet("fav_" + LANG, new LinkedHashSet<>()));
    }

    private void toggleFav(String mode) {
        Set<String> favs = getFavs();
        if (favs.contains(mode)) favs.remove(mode);
        else favs.add(mode);
        getSharedPreferences("vocably_favorites", Context.MODE_PRIVATE)
                .edit().putStringSet("fav_" + LANG, favs).apply();
    }

    private void loadBooks() {
        booksRow.removeAllViews();
        boolean anyFound = false;

        for (String file : BOOK_FILES) {
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

                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dpToPx(120), dpToPx(64));
                params.setMarginEnd(dpToPx(10));
                btn.setLayoutParams(params);

                final JSONObject bookJson = book;
                final String bookFile = file;
                btn.setOnClickListener(v -> selectBook(bookJson, btn, bookFile));
                booksRow.addView(btn);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        booksScrollView.setVisibility(anyFound ? View.VISIBLE : View.GONE);
    }

    private void selectBook(JSONObject bookJson, TextView selectedBtn, String bookFile) {
        currentBookFile = bookFile;
        try { currentBookName = bookJson.getString("name"); } catch (Exception e) { currentBookName = ""; }
        selectedSections.clear();
        selectedUnitBtn = null;
        currentUnitNumber = -1;
        unitsContainer.removeAllViews();
        sectionsSection.setVisibility(View.GONE);
        sectionsContainer.removeAllViews();
        quizSection.setVisibility(View.GONE);
        directionRow.setVisibility(View.GONE);

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
                            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
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
                    LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(0, dpToPx(48), 1f);
                    sp.setMargins(0, 0, i == remaining - 1 ? 0 : dpToPx(8), 0);
                    spacer.setLayoutParams(sp);
                    currentRow.addView(spacer);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        findViewById(R.id.unitsSection).setVisibility(View.VISIBLE);
    }

    private void selectUnit(TextView unitBtn, JSONObject unit, int unitNumber) {
        if (selectedUnitBtn != null) {
            selectedUnitBtn.setBackgroundResource(R.drawable.bg_btn_language);
        }
        selectedUnitBtn = unitBtn;
        unitBtn.setBackgroundResource(R.drawable.bg_btn_language_selected);
        currentUnitNumber = unitNumber;

        selectedSections.clear();
        sectionsContainer.removeAllViews();
        quizSection.setVisibility(View.GONE);

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
                    updateQuiz();
                });

                sectionsContainer.addView(sectionBtn);
            }

            selectedSections.put(unitNumber, defaultSelected);
            sectionsSection.setVisibility(View.VISIBLE);
            updateQuiz();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateQuiz() {
        boolean any = false;
        for (List<String> list : selectedSections.values()) {
            if (!list.isEmpty()) { any = true; break; }
        }

        if (!any) {
            quizSection.setVisibility(View.GONE);
            directionRow.setVisibility(View.GONE);
            return;
        }

        directionRow.setVisibility(View.VISIBLE);
        updateDirectionButtons();
        showQuizModes();
    }

    private void showQuizModes() {
        quizContainer.removeAllViews();

        Set<String> favs = getFavs();
        List<String> ordered = new ArrayList<>();
        for (String m : QUIZ_MODES) { if (favs.contains(m)) ordered.add(m); }
        for (String m : QUIZ_MODES) { if (!favs.contains(m)) ordered.add(m); }

        for (String mode : ordered) {
            boolean isFav = favs.contains(mode);

            FrameLayout wrapper = new FrameLayout(this);
            LinearLayout.LayoutParams wParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(52));
            wParams.setMargins(0, 0, 0, dpToPx(8));
            wrapper.setLayoutParams(wParams);

            TextView modeBtn = new TextView(this);
            modeBtn.setText(mode);
            modeBtn.setGravity(android.view.Gravity.CENTER);
            modeBtn.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
            modeBtn.setTextSize(14f);
            modeBtn.setBackground(ContextCompat.getDrawable(this,
                    isFav ? R.drawable.bg_btn_language_selected : R.drawable.bg_btn_language));
            modeBtn.setLayoutParams(new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

            ImageView star = new ImageView(this);
            star.setImageResource(R.drawable.ic_star);
            star.setVisibility(isFav ? View.VISIBLE : View.GONE);
            FrameLayout.LayoutParams starParams = new FrameLayout.LayoutParams(dpToPx(14), dpToPx(14));
            starParams.gravity = android.view.Gravity.TOP | android.view.Gravity.END;
            starParams.setMargins(0, dpToPx(6), dpToPx(8), 0);
            star.setLayoutParams(starParams);
            star.setColorFilter(ContextCompat.getColor(this, R.color.accent_gold));

            wrapper.addView(modeBtn);
            wrapper.addView(star);

            final String finalMode = mode;
            modeBtn.setOnClickListener(v -> startQuiz(finalMode));
            modeBtn.setOnLongClickListener(v -> {
                toggleFav(finalMode);
                showQuizModes();
                return true;
            });

            quizContainer.addView(wrapper);
        }

        quizSection.setVisibility(View.VISIBLE);
    }

    private void saveSession(String mode, List<String> sections) {
        SessionManager.Session s = new SessionManager.Session();
        s.language   = LANG;
        s.bookFile   = currentBookFile;
        s.bookName   = currentBookName;
        s.unitNumber = currentUnitNumber;
        s.sections   = new ArrayList<>(sections);
        s.direction  = selectedDirection;
        s.mode       = mode;
        SessionManager.save(this, s);
    }

    private void startQuiz(String mode) {
        if (currentBookFile == null || currentUnitNumber == -1) return;
        List<String> sections = selectedSections.get(currentUnitNumber);
        if (sections == null || sections.isEmpty()) return;

        if (!mode.equals("Losowa nauka")) {
            saveSession(mode, sections);
        }

        if (mode.equals("Fiszki")) {
            Intent intent = new Intent(this, FlashcardActivity.class);
            intent.putExtra(FlashcardActivity.EXTRA_LANGUAGE, LANG);
            intent.putExtra(FlashcardActivity.EXTRA_BOOK_FILE, currentBookFile);
            intent.putExtra(FlashcardActivity.EXTRA_UNIT_NUMBER, currentUnitNumber);
            intent.putStringArrayListExtra(FlashcardActivity.EXTRA_SECTION_NUMBERS, new ArrayList<>(sections));
            startActivity(intent);
            return;
        }

        if (mode.equals("Wpisz")) {
            Intent intent = new Intent(this, TypingActivity.class);
            intent.putExtra(TypingActivity.EXTRA_LANGUAGE, LANG);
            intent.putExtra(TypingActivity.EXTRA_BOOK_FILE, currentBookFile);
            intent.putExtra(TypingActivity.EXTRA_UNIT_NUMBER, currentUnitNumber);
            intent.putStringArrayListExtra(TypingActivity.EXTRA_SECTION_NUMBERS, new ArrayList<>(sections));
            startActivity(intent);
            return;
        }

        if (mode.equals("Wybór")) {
            Intent intent = new Intent(this, ChoiceActivity.class);
            intent.putExtra(ChoiceActivity.EXTRA_LANGUAGE, LANG);
            intent.putExtra(ChoiceActivity.EXTRA_BOOK_FILE, currentBookFile);
            intent.putExtra(ChoiceActivity.EXTRA_UNIT_NUMBER, currentUnitNumber);
            intent.putStringArrayListExtra(ChoiceActivity.EXTRA_SECTION_NUMBERS, new ArrayList<>(sections));
            startActivity(intent);
            return;
        }

        if (mode.equals("Szybkie fiszki")) {
            Intent intent = new Intent(this, QuickFlashcardActivity.class);
            intent.putExtra(QuickFlashcardActivity.EXTRA_LANGUAGE, LANG);
            intent.putExtra(QuickFlashcardActivity.EXTRA_BOOK_FILE, currentBookFile);
            intent.putExtra(QuickFlashcardActivity.EXTRA_UNIT_NUMBER, currentUnitNumber);
            intent.putStringArrayListExtra(QuickFlashcardActivity.EXTRA_SECTION_NUMBERS, new ArrayList<>(sections));
            startActivity(intent);
            return;
        }

        if (mode.equals("Memory")) {
            Intent intent = new Intent(this, MemorySetupActivity.class);
            intent.putExtra(MemorySetupActivity.EXTRA_LANGUAGE, LANG);
            intent.putExtra(MemorySetupActivity.EXTRA_BOOK_FILE, currentBookFile);
            intent.putExtra(MemorySetupActivity.EXTRA_UNIT_NUMBER, currentUnitNumber);
            intent.putStringArrayListExtra(MemorySetupActivity.EXTRA_SECTION_NUMBERS, new ArrayList<>(sections));
            startActivity(intent);
            return;
        }

        if (mode.equals("Losowa nauka")) {
            String[] randomModes = {
                    "Fiszki", "Wpisz", "Wybór", "Szybkie fiszki", "Memory",
                    "Szybka odpowiedź", "Litera po literze", "Prawda czy Fałsz", "Ułóż słowo"
            };
            String randomMode = randomModes[(int) (Math.random() * randomModes.length)];
            startQuiz(randomMode);
            return;
        }

        if (mode.equals("Szybka odpowiedź")) {
            Intent intent = new Intent(this, SpeedAnswerActivity.class);
            intent.putExtra(SpeedAnswerActivity.EXTRA_LANGUAGE, LANG);
            intent.putExtra(SpeedAnswerActivity.EXTRA_BOOK_FILE, currentBookFile);
            intent.putExtra(SpeedAnswerActivity.EXTRA_UNIT_NUMBER, currentUnitNumber);
            intent.putStringArrayListExtra(SpeedAnswerActivity.EXTRA_SECTION_NUMBERS, new ArrayList<>(sections));
            startActivity(intent);
            return;
        }

        if (mode.equals("Lista")) {
            Intent intent = new Intent(this, WordListActivity.class);
            intent.putExtra(WordListActivity.EXTRA_LANGUAGE, LANG);
            intent.putExtra(WordListActivity.EXTRA_BOOK_FILE, currentBookFile);
            intent.putExtra(WordListActivity.EXTRA_UNIT_NUMBER, currentUnitNumber);
            intent.putStringArrayListExtra(WordListActivity.EXTRA_SECTION_NUMBERS, new ArrayList<>(sections));
            startActivity(intent);
            return;
        }

        if (mode.equals("Litera po literze")) {
            Intent intent = new Intent(this, LetterByLetterActivity.class);
            intent.putExtra(LetterByLetterActivity.EXTRA_LANGUAGE, LANG);
            intent.putExtra(LetterByLetterActivity.EXTRA_BOOK_FILE, currentBookFile);
            intent.putExtra(LetterByLetterActivity.EXTRA_UNIT_NUMBER, currentUnitNumber);
            intent.putStringArrayListExtra(LetterByLetterActivity.EXTRA_SECTION_NUMBERS, new ArrayList<>(sections));
            startActivity(intent);
            return;
        }

        if (mode.equals("Prawda czy Fałsz")) {
            Intent intent = new Intent(this, TrueFalseActivity.class);
            intent.putExtra(TrueFalseActivity.EXTRA_LANGUAGE, LANG);
            intent.putExtra(TrueFalseActivity.EXTRA_BOOK_FILE, currentBookFile);
            intent.putExtra(TrueFalseActivity.EXTRA_UNIT_NUMBER, currentUnitNumber);
            intent.putStringArrayListExtra(TrueFalseActivity.EXTRA_SECTION_NUMBERS, new ArrayList<>(sections));
            startActivity(intent);
            return;
        }

        if (mode.equals("Ułóż słowo")) {
            Intent intent = new Intent(this, ScrambleActivity.class);
            intent.putExtra(ScrambleActivity.EXTRA_LANGUAGE, LANG);
            intent.putExtra(ScrambleActivity.EXTRA_BOOK_FILE, currentBookFile);
            intent.putExtra(ScrambleActivity.EXTRA_UNIT_NUMBER, currentUnitNumber);
            intent.putStringArrayListExtra(ScrambleActivity.EXTRA_SECTION_NUMBERS, new ArrayList<>(sections));
            startActivity(intent);
        }

        if (mode.equals("Wyszukiwanie")) {
            Intent intent = new Intent(this, SearchActivity.class);
            intent.putExtra(SearchActivity.EXTRA_LANGUAGE, LANG);
            intent.putExtra(SearchActivity.EXTRA_BOOK_FILE, currentBookFile);
            intent.putExtra(SearchActivity.EXTRA_UNIT_NUMBER, currentUnitNumber);
            intent.putStringArrayListExtra(SearchActivity.EXTRA_SECTION_NUMBERS, new ArrayList<>(sections));
            startActivity(intent);
            return;
        }
    }

    private void selectDirection(String dir) {
        selectedDirection = dir;
        getSharedPreferences("vocably_prefs", Context.MODE_PRIVATE)
                .edit()
                .putString("direction_de", dir)
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
}