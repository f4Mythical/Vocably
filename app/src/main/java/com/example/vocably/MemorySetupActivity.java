package com.example.vocably;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

public class MemorySetupActivity extends AppCompatActivity {

    public static final String EXTRA_LANGUAGE        = "language";
    public static final String EXTRA_BOOK_FILE       = "book_file";
    public static final String EXTRA_UNIT_NUMBER     = "unit_number";
    public static final String EXTRA_SECTION_NUMBERS = "section_numbers";

    public static final String EXTRA_GRID_COLS = "grid_cols";
    public static final String EXTRA_GRID_ROWS = "grid_rows";
    public static final String EXTRA_PAIR_MODE = "pair_mode";

    private String selectedGrid = "3x2";
    private String pairMode;

    private TextView[] gridBtns = new TextView[4];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_memory_setup);

        String language  = getIntent().getStringExtra(EXTRA_LANGUAGE);
        String prefKey   = "en".equals(language) ? "direction_en" : "direction_de";
        String direction = getSharedPreferences("vocably_prefs", Context.MODE_PRIVATE)
                .getString(prefKey, "lang_to_polish");

        if ("random".equals(direction)) {
            new AlertDialog.Builder(this)
                    .setTitle("Tryb losowy niedostępny")
                    .setMessage("Memory wymaga stałego kierunku tłumaczenia. Zmień kierunek na Język → Polski lub Polski → Język i spróbuj ponownie.")
                    .setPositiveButton("OK", (d, w) -> finish())
                    .setCancelable(false)
                    .show();
            return;
        }

        pairMode = "both";

        gridBtns[0] = findViewById(R.id.btnGrid2x2);
        gridBtns[1] = findViewById(R.id.btnGrid3x2);
        gridBtns[2] = findViewById(R.id.btnGrid4x2);
        gridBtns[3] = findViewById(R.id.btnGrid4x4);

        gridBtns[0].setOnClickListener(v -> selectGrid("2x2"));
        gridBtns[1].setOnClickListener(v -> selectGrid("3x2"));
        gridBtns[2].setOnClickListener(v -> selectGrid("4x2"));
        gridBtns[3].setOnClickListener(v -> selectGrid("4x4"));

        selectGrid(selectedGrid);

        findViewById(R.id.btnStartMemory).setOnClickListener(v -> startGame());
    }

    private void selectGrid(String grid) {
        selectedGrid = grid;
        String[] keys = {"2x2", "3x2", "4x2", "4x4"};
        for (int i = 0; i < gridBtns.length; i++) {
            gridBtns[i].setBackgroundResource(
                    keys[i].equals(grid)
                            ? R.drawable.bg_btn_language_selected
                            : R.drawable.bg_btn_language
            );
        }
    }

    private void startGame() {
        int cols, rows;
        switch (selectedGrid) {
            case "2x2": cols = 2; rows = 2; break;
            case "4x2": cols = 4; rows = 2; break;
            case "4x4": cols = 4; rows = 4; break;
            default:    cols = 3; rows = 2; break;
        }

        Intent intent = new Intent(this, MemoryActivity.class);
        intent.putExtra(EXTRA_LANGUAGE,    getIntent().getStringExtra(EXTRA_LANGUAGE));
        intent.putExtra(EXTRA_BOOK_FILE,   getIntent().getStringExtra(EXTRA_BOOK_FILE));
        intent.putExtra(EXTRA_UNIT_NUMBER, getIntent().getIntExtra(EXTRA_UNIT_NUMBER, -1));
        intent.putStringArrayListExtra(EXTRA_SECTION_NUMBERS,
                getIntent().getStringArrayListExtra(EXTRA_SECTION_NUMBERS));
        intent.putExtra(EXTRA_GRID_COLS, cols);
        intent.putExtra(EXTRA_GRID_ROWS, rows);
        intent.putExtra(EXTRA_PAIR_MODE, pairMode);
        startActivity(intent);
    }
}