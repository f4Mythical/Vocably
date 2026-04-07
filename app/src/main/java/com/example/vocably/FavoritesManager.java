package com.example.vocably;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.LinkedHashSet;
import java.util.Set;

public class FavoritesManager {

    private static final String PREFS = "vocably_favorites";

    public static Set<String> get(Context ctx, String language) {
        SharedPreferences prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        return new LinkedHashSet<>(prefs.getStringSet("fav_" + language, new LinkedHashSet<>()));
    }

    public static void toggle(Context ctx, String language, String mode) {
        Set<String> favs = get(ctx, language);
        if (favs.contains(mode)) favs.remove(mode);
        else favs.add(mode);
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putStringSet("fav_" + language, favs)
                .apply();
    }

    public static boolean isFav(Context ctx, String language, String mode) {
        return get(ctx, language).contains(mode);
    }
}