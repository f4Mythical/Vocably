package com.example.vocably;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class InformationActivity extends AppCompatActivity {

    private static class InfoEntry {
        String title   = "";
        String version = "";
        List<String> items = new ArrayList<>();
        String date    = "";
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_information);

        LinearLayout container = findViewById(R.id.infoContainer);
        LayoutInflater inflater = LayoutInflater.from(this);

        int index = 1;
        while (true) {
            InfoEntry entry = loadInfo("info" + index);
            if (entry == null) break;

            View card = inflater.inflate(R.layout.item_info_card, container, false);

            TextView tvTitle = card.findViewById(R.id.tvTitle);
            tvTitle.setText(entry.title);

            TextView tvVersion = card.findViewById(R.id.tvVersion);
            tvVersion.setText("Wersja " + entry.version);

            LinearLayout bulletsContainer = card.findViewById(R.id.bulletsContainer);
            for (String item : entry.items) {
                View bulletRow = inflater.inflate(R.layout.item_info_bullet, bulletsContainer, false);
                TextView tvBullet = bulletRow.findViewById(R.id.tvBulletText);
                tvBullet.setText(item);
                bulletsContainer.addView(bulletRow);
            }

            TextView tvDate = card.findViewById(R.id.tvDate);
            tvDate.setText(entry.date);

            container.addView(card);
            index++;
        }
    }

    private InfoEntry loadInfo(String name) {
        try {
            int resId = getResources().getIdentifier(name, "raw", getPackageName());
            if (resId == 0) return null;

            InputStream is = getResources().openRawResource(resId);
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            XmlPullParser parser = factory.newPullParser();
            parser.setInput(is, "UTF-8");

            InfoEntry entry = new InfoEntry();
            String tag = null;

            int event = parser.getEventType();
            while (event != XmlPullParser.END_DOCUMENT) {
                if (event == XmlPullParser.START_TAG) {
                    tag = parser.getName();
                } else if (event == XmlPullParser.TEXT) {
                    String text = parser.getText().trim();
                    if (text.isEmpty()) { event = parser.next(); continue; }
                    if ("title".equals(tag))       entry.title = text;
                    else if ("version".equals(tag)) entry.version = text;
                    else if ("item".equals(tag))    entry.items.add(text);
                    else if ("date".equals(tag))    entry.date = text;
                }
                event = parser.next();
            }
            is.close();
            return entry.version.isEmpty() ? null : entry;
        } catch (Exception e) {
            return null;
        }
    }
}