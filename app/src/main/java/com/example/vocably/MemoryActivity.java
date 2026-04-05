package com.example.vocably;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.View;
import android.widget.GridLayout;
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

public class MemoryActivity extends AppCompatActivity {

    private GridLayout gridLayout;
    private TextView   tvMoves, tvMatched;
    private View       summaryLayout, gameLayout;
    private TextView   tvFinalMoves;

    private int    gridCols, gridRows, pairCount;
    private String pairMode, language;

    private List<MemoryCard> cards   = new ArrayList<>();
    private MemoryCard firstFlipped  = null;
    private boolean    isChecking    = false;
    private int        moves         = 0;
    private int        matchedPairs  = 0;

    private static class MemoryCard {
        String displayText;
        int    pairId;
        boolean flipped  = false;
        boolean matched  = false;
        TextView view;

        MemoryCard(String displayText, int pairId, TextView view) {
            this.displayText = displayText;
            this.pairId      = pairId;
            this.view        = view;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_memory);

        gridCols  = getIntent().getIntExtra(MemorySetupActivity.EXTRA_GRID_COLS, 3);
        gridRows  = getIntent().getIntExtra(MemorySetupActivity.EXTRA_GRID_ROWS, 2);
        pairMode  = getIntent().getStringExtra(MemorySetupActivity.EXTRA_PAIR_MODE);
        language  = getIntent().getStringExtra(MemorySetupActivity.EXTRA_LANGUAGE);
        pairCount = (gridCols * gridRows) / 2;

        gridLayout  = findViewById(R.id.memoryGrid);
        tvMoves     = findViewById(R.id.tvMoves);
        tvMatched   = findViewById(R.id.tvMatched);
        summaryLayout = findViewById(R.id.summaryLayout);
        gameLayout    = findViewById(R.id.gameLayout);
        tvFinalMoves  = findViewById(R.id.tvFinalMoves);

        loadAndBuildGrid();

        findViewById(R.id.btnPlayAgain).setOnClickListener(v -> {
            summaryLayout.setVisibility(View.GONE);
            gameLayout.setVisibility(View.VISIBLE);
            resetGame();
        });
        findViewById(R.id.btnFinish).setOnClickListener(v -> finish());
    }

    private void loadAndBuildGrid() {
        List<String[]> wordPairs = loadWords();
        if (wordPairs.isEmpty()) { finish(); return; }

        Collections.shuffle(wordPairs);
        List<String[]> selected = wordPairs.subList(0, Math.min(pairCount, wordPairs.size()));
        pairCount = selected.size();

        List<String[]> cardDefs = new ArrayList<>();
        for (int i = 0; i < selected.size(); i++) {
            String[] pair = selected.get(i);
            if ("both".equals(pairMode)) {
                cardDefs.add(new String[]{pair[0], String.valueOf(i)});
                cardDefs.add(new String[]{pair[1], String.valueOf(i)});
            } else if ("lang".equals(pairMode)) {
                cardDefs.add(new String[]{pair[0], String.valueOf(i)});
                cardDefs.add(new String[]{pair[0], String.valueOf(i)});
            } else {
                cardDefs.add(new String[]{pair[1], String.valueOf(i)});
                cardDefs.add(new String[]{pair[1], String.valueOf(i)});
            }
        }
        Collections.shuffle(cardDefs);
        buildGrid(cardDefs);
        updateStats();
    }

    private void buildGrid(List<String[]> cardDefs) {
        gridLayout.removeAllViews();
        gridLayout.setColumnCount(gridCols);
        gridLayout.setRowCount(gridRows);
        cards.clear();
        firstFlipped = null;
        moves        = 0;
        matchedPairs = 0;

        for (int i = 0; i < cardDefs.size(); i++) {
            String text   = cardDefs.get(i)[0];
            int    pairId = Integer.parseInt(cardDefs.get(i)[1]);

            TextView cardView = new TextView(this);
            cardView.setGravity(Gravity.CENTER);
            cardView.setTextSize(13f);
            cardView.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
            cardView.setBackgroundResource(R.drawable.bg_memory_card_back);
            cardView.setPadding(8, 8, 8, 8);

            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width  = 0;
            params.height = 0;
            params.columnSpec = GridLayout.spec(i % gridCols, 1, 1f);
            params.rowSpec    = GridLayout.spec(i / gridCols, 1, 1f);
            params.setMargins(dpToPx(5), dpToPx(5), dpToPx(5), dpToPx(5));
            cardView.setLayoutParams(params);

            MemoryCard card = new MemoryCard(text, pairId, cardView);
            cards.add(card);

            cardView.setOnClickListener(v -> onCardClick(card));
            gridLayout.addView(cardView);
        }
    }

    private void resetGame() {
        List<String[]> cardDefs = new ArrayList<>();
        for (int i = 0; i < cards.size(); i += 2) {
            String[] def1 = {cards.get(i).displayText,   String.valueOf(cards.get(i).pairId)};
            String[] def2 = {cards.get(i+1).displayText, String.valueOf(cards.get(i+1).pairId)};
            cardDefs.add(def1);
            cardDefs.add(def2);
        }
        Collections.shuffle(cardDefs);
        buildGrid(cardDefs);
        updateStats();
    }

    private void onCardClick(MemoryCard card) {
        if (isChecking || card.flipped || card.matched) return;

        flipCard(card, true);

        if (firstFlipped == null) {
            firstFlipped = card;
        } else {
            moves++;
            updateStats();
            isChecking = true;

            MemoryCard second = card;
            new Handler().postDelayed(() -> {
                if (firstFlipped.pairId == second.pairId && firstFlipped != second) {
                    firstFlipped.matched = true;
                    second.matched       = true;
                    firstFlipped.view.setBackgroundResource(R.drawable.bg_memory_card_matched);
                    second.view.setBackgroundResource(R.drawable.bg_memory_card_matched);
                    matchedPairs++;
                    if (matchedPairs >= pairCount) showSummary();
                } else {
                    flipCard(firstFlipped, false);
                    flipCard(second, false);
                }
                firstFlipped = null;
                isChecking   = false;
                updateStats();
            }, 800);
        }
    }

    private void flipCard(MemoryCard card, boolean faceUp) {
        card.flipped = faceUp;
        if (faceUp) {
            card.view.setText(card.displayText);
            card.view.setTextSize(12f);
            card.view.setBackgroundResource(R.drawable.bg_memory_card_front);
        } else {
            card.view.setText("");
            card.view.setBackgroundResource(R.drawable.bg_memory_card_back);
        }
    }

    private void updateStats() {
        tvMoves.setText(String.valueOf(moves));
        tvMatched.setText(matchedPairs + " / " + pairCount);
    }

    private void showSummary() {
        gameLayout.setVisibility(View.GONE);
        summaryLayout.setVisibility(View.VISIBLE);
        tvFinalMoves.setText(String.valueOf(moves));
    }

    private List<String[]> loadWords() {
        String bookFile = getIntent().getStringExtra(MemorySetupActivity.EXTRA_BOOK_FILE);
        int unitNumber  = getIntent().getIntExtra(MemorySetupActivity.EXTRA_UNIT_NUMBER, -1);
        ArrayList<String> sectionNumbers = getIntent().getStringArrayListExtra(MemorySetupActivity.EXTRA_SECTION_NUMBERS);
        List<String[]> result = new ArrayList<>();

        if (bookFile == null || unitNumber == -1 || sectionNumbers == null) return result;

        try {
            JSONObject json = loadJson(bookFile);
            if (json == null) return result;
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
                            result.add(new String[]{foreign, polish});
                        }
                    }
                }
                break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
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

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
}