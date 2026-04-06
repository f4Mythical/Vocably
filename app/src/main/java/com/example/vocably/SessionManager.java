package com.example.vocably;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class SessionManager {

    private static final String PREFS   = "vocably_sessions";
    private static final String KEY     = "recent_sessions";
    private static final int    MAX     = 3;

    public static class Session {
        public String language;
        public String bookFile;
        public String bookName;
        public int    unitNumber;
        public List<String> sections;
        public String direction;
        public String mode;

        public String languageLabel() {
            return "en".equals(language) ? "Angielski" : "Niemiecki";
        }

        public String directionLabel() {
            if ("lang_to_polish".equals(direction))
                return "en".equals(language) ? "EN → PL" : "DE → PL";
            if ("polish_to_lang".equals(direction))
                return "en".equals(language) ? "PL → EN" : "PL → DE";
            return "Losowy";
        }

        public String sectionsLabel() {
            if (sections == null || sections.isEmpty()) return "—";
            if (sections.size() == 1) return sections.get(0);
            return sections.get(0) + " +" + (sections.size() - 1);
        }
    }

    public static void save(Context ctx, Session s) {
        SharedPreferences prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        List<Session> list = load(ctx);

        list.removeIf(e ->
                e.language.equals(s.language) &&
                        e.bookFile.equals(s.bookFile) &&
                        e.unitNumber == s.unitNumber &&
                        e.mode.equals(s.mode) &&
                        String.valueOf(e.sections).equals(String.valueOf(s.sections))
        );

        list.add(0, s);
        if (list.size() > MAX) list = list.subList(0, MAX);

        try {
            JSONArray arr = new JSONArray();
            for (Session se : list) {
                JSONObject o = new JSONObject();
                o.put("language",   se.language);
                o.put("bookFile",   se.bookFile);
                o.put("bookName",   se.bookName);
                o.put("unitNumber", se.unitNumber);
                o.put("direction",  se.direction);
                o.put("mode",       se.mode);
                JSONArray sec = new JSONArray();
                if (se.sections != null)
                    for (String sec1 : se.sections) sec.put(sec1);
                o.put("sections", sec);
                arr.put(o);
            }
            prefs.edit().putString(KEY, arr.toString()).apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static List<Session> load(Context ctx) {
        SharedPreferences prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        List<Session> list = new ArrayList<>();
        String raw = prefs.getString(KEY, null);
        if (raw == null) return list;
        try {
            JSONArray arr = new JSONArray(raw);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                Session s = new Session();
                s.language   = o.optString("language");
                s.bookFile   = o.optString("bookFile");
                s.bookName   = o.optString("bookName");
                s.unitNumber = o.optInt("unitNumber", -1);
                s.direction  = o.optString("direction");
                s.mode       = o.optString("mode");
                s.sections   = new ArrayList<>();
                JSONArray sec = o.optJSONArray("sections");
                if (sec != null)
                    for (int j = 0; j < sec.length(); j++)
                        s.sections.add(sec.getString(j));
                list.add(s);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }
}