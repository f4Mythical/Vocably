package com.example.vocably;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
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

public class WordListActivity extends AppCompatActivity {

    public static final String EXTRA_LANGUAGE        = "language";
    public static final String EXTRA_BOOK_FILE       = "book_file";
    public static final String EXTRA_UNIT_NUMBER     = "unit_number";
    public static final String EXTRA_SECTION_NUMBERS = "section_numbers";

    private static final int   ACCENT_TEXT_COLOR = 0xFF412402;
    private static final int   BG_CARD_COLOR     = 0xFF252429;
    private static final int   ROW_HEIGHT_DP     = 52;
    private static final int   ROW_MARGIN_DP     = 8;
    private static final int   CORNER_RADIUS_DP  = 12;

    private int accentColor;

    private static class Word {
        String foreign, polish;
        Word(String f, String p) { foreign = f; polish = p; }
    }

    private static class WaveCardView extends View {
        private final Paint paintBg     = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint paintAccent = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final float seed;
        private final float cornerR;

        WaveCardView(Context ctx, int seed, float cornerR, int accentColor) {
            super(ctx);
            this.seed    = seed;
            this.cornerR = cornerR;
            paintBg.setColor(BG_CARD_COLOR);
            paintAccent.setColor(accentColor);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            float W = getWidth(), H = getHeight();
            float split = W * 0.5f;
            float amp   = 6f + (seed % 4) * 3f;
            int   bumps = 4;

            float[] px = new float[bumps + 1];
            float[] py = new float[bumps + 1];
            for (int i = 0; i <= bumps; i++) {
                py[i] = (i / (float) bumps) * H;
                px[i] = split + (i % 2 == 0 ? amp : -amp);
            }

            Path rightPath = new Path();
            rightPath.moveTo(W, 0);
            rightPath.lineTo(px[0], py[0]);
            for (int i = 1; i <= bumps; i++) {
                float mx = (px[i - 1] + px[i]) / 2f;
                float my = (py[i - 1] + py[i]) / 2f;
                rightPath.quadTo(px[i - 1], py[i - 1], mx, my);
            }
            rightPath.lineTo(px[bumps], H);
            rightPath.lineTo(W, H);
            rightPath.close();

            RectF rect = new RectF(0, 0, W, H);
            Path  clip = new Path();
            clip.addRoundRect(rect, cornerR, cornerR, Path.Direction.CW);
            canvas.clipPath(clip);

            canvas.drawRoundRect(rect, cornerR, cornerR, paintBg);
            canvas.drawPath(rightPath, paintAccent);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        accentColor = ContextCompat.getColor(this, R.color.accent_gold);

        String language = getIntent().getStringExtra(EXTRA_LANGUAGE);
        List<Word> words = loadWords(language);

        ScrollView scrollView = new ScrollView(this);
        scrollView.setBackgroundColor(ContextCompat.getColor(this, R.color.bg_primary));

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        int padH = dpToPx(16);
        int padTop = dpToPx(52);
        container.setPadding(padH, padTop, padH, dpToPx(24));

        TextView tvTitle = new TextView(this);
        tvTitle.setText("Lista słówek");
        tvTitle.setTextSize(20f);
        tvTitle.setTypeface(tvTitle.getTypeface(), android.graphics.Typeface.BOLD);
        tvTitle.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        container.addView(tvTitle);

        TextView tvCount = new TextView(this);
        tvCount.setText(words.size() + " słówek");
        tvCount.setTextSize(13f);
        tvCount.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        LinearLayout.LayoutParams countParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        countParams.setMargins(0, dpToPx(4), 0, dpToPx(20));
        tvCount.setLayoutParams(countParams);
        container.addView(tvCount);

        int rowH    = dpToPx(ROW_HEIGHT_DP);
        int margin  = dpToPx(ROW_MARGIN_DP);
        float cornerR = dpToPx(CORNER_RADIUS_DP);

        for (int i = 0; i < words.size(); i++) {
            Word word = words.get(i);

            FrameLayout frame = new FrameLayout(this);
            LinearLayout.LayoutParams frameParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, rowH);
            frameParams.setMargins(0, margin, 0, margin);
            frame.setLayoutParams(frameParams);

            WaveCardView card = new WaveCardView(this, i * 13 + 7, cornerR, accentColor);
            card.setLayoutParams(new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            frame.addView(card);

            LinearLayout splitLayout = new LinearLayout(this);
            splitLayout.setOrientation(LinearLayout.HORIZONTAL);
            splitLayout.setLayoutParams(new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

            TextView tvLeft = new TextView(this);
            tvLeft.setText(word.polish);
            tvLeft.setTextSize(13f);
            tvLeft.setTypeface(tvLeft.getTypeface(), android.graphics.Typeface.BOLD);
            tvLeft.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
            tvLeft.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams llLeft = new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.MATCH_PARENT, 1f);
            llLeft.setMargins(dpToPx(4), 0, dpToPx(4), 0);
            tvLeft.setLayoutParams(llLeft);

            TextView tvRight = new TextView(this);
            tvRight.setText(word.foreign);
            tvRight.setTextSize(13f);
            tvRight.setTypeface(tvRight.getTypeface(), android.graphics.Typeface.BOLD);
            tvRight.setTextColor(ACCENT_TEXT_COLOR);
            tvRight.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams llRight = new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.MATCH_PARENT, 1f);
            llRight.setMargins(dpToPx(4), 0, dpToPx(4), 0);
            tvRight.setLayoutParams(llRight);

            splitLayout.addView(tvLeft);
            splitLayout.addView(tvRight);
            frame.addView(splitLayout);
            container.addView(frame);
        }

        scrollView.addView(container);
        setContentView(scrollView);
    }

    private List<Word> loadWords(String language) {
        String bookFile = getIntent().getStringExtra(EXTRA_BOOK_FILE);
        int unitNumber  = getIntent().getIntExtra(EXTRA_UNIT_NUMBER, -1);
        ArrayList<String> sectionNumbers = getIntent().getStringArrayListExtra(EXTRA_SECTION_NUMBERS);
        List<Word> result = new ArrayList<>();

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
                            result.add(new Word(foreign, polish));
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