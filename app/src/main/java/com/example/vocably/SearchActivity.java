package com.example.vocably;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class SearchActivity extends AppCompatActivity {

    public static final String EXTRA_LANGUAGE        = "language";
    public static final String EXTRA_BOOK_FILE       = "book_file";
    public static final String EXTRA_UNIT_NUMBER     = "unit_number";
    public static final String EXTRA_SECTION_NUMBERS = "section_numbers";

    private static class Word {
        String foreign;
        String polish;
        Word(String f, String p) { foreign = f; polish = p; }
    }

    private List<Word> allWords = new ArrayList<>();
    private LinearLayout resultsContainer;
    private TextView tvCount;
    private String language;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        language = getIntent().getStringExtra(EXTRA_LANGUAGE);

        resultsContainer = findViewById(R.id.resultsContainer);
        tvCount          = findViewById(R.id.tvCount);
        EditText etSearch = findViewById(R.id.etSearch);

        loadWords();

        tvCount.setText(allWords.size() + " słówek");
        showResults(allWords);

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {}
            @Override public void afterTextChanged(Editable s) {
                filter(s.toString().trim());
            }
        });
    }

    private void filter(String query) {
        if (query.isEmpty()) {
            tvCount.setText(allWords.size() + " słówek");
            showResults(allWords);
            return;
        }
        String lower = query.toLowerCase();
        List<Word> filtered = new ArrayList<>();
        for (Word w : allWords) {
            if (w.polish.toLowerCase().contains(lower) || w.foreign.toLowerCase().contains(lower)) {
                filtered.add(w);
            }
        }
        tvCount.setText(filtered.size() + " wyników");
        showResults(filtered);
    }

    private void showResults(List<Word> words) {
        resultsContainer.removeAllViews();

        if (words.isEmpty()) {
            TextView tvEmpty = new TextView(this);
            tvEmpty.setText("Brak wyników");
            tvEmpty.setTextSize(14f);
            tvEmpty.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
            tvEmpty.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(80));
            tvEmpty.setLayoutParams(lp);
            resultsContainer.addView(tvEmpty);
            return;
        }

        for (Word word : words) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setBackground(ContextCompat.getDrawable(this, R.drawable.bg_btn_language));
            row.setGravity(Gravity.CENTER_VERTICAL);

            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(52));
            rowParams.setMargins(0, 0, 0, dpToPx(8));
            row.setLayoutParams(rowParams);

            TextView tvPolish = new TextView(this);
            tvPolish.setText(word.polish);
            tvPolish.setTextSize(13f);
            tvPolish.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
            tvPolish.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams lpLeft = new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.MATCH_PARENT, 1f);
            lpLeft.setMargins(dpToPx(8), 0, dpToPx(4), 0);
            tvPolish.setLayoutParams(lpLeft);

            View divider = new View(this);
            divider.setBackgroundColor(ContextCompat.getColor(this, R.color.divider));
            LinearLayout.LayoutParams divParams = new LinearLayout.LayoutParams(dpToPx(1),
                    ViewGroup.LayoutParams.MATCH_PARENT);
            divParams.setMargins(0, dpToPx(10), 0, dpToPx(10));
            divider.setLayoutParams(divParams);

            TextView tvForeign = new TextView(this);
            tvForeign.setText(word.foreign);
            tvForeign.setTextSize(13f);
            tvForeign.setTextColor(ContextCompat.getColor(this, R.color.accent_gold));
            tvForeign.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams lpRight = new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.MATCH_PARENT, 1f);
            lpRight.setMargins(dpToPx(4), 0, dpToPx(8), 0);
            tvForeign.setLayoutParams(lpRight);

            row.addView(tvPolish);
            row.addView(divider);
            row.addView(tvForeign);
            resultsContainer.addView(row);
        }
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

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
}