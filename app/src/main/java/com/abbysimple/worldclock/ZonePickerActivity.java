package com.abbysimple.worldclock;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextClock;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/** Searchable list of every city/country time zone; returns the chosen zone id. */
public class ZonePickerActivity extends Activity {
    public static final String EXTRA_ZONE = "zone";

    private static final class Entry {
        final String id, city, place, search;

        Entry(String id) {
            this.id = id;
            city = Clocks.city(id);
            place = SettingsActivity.describe(id);
            TimeZone tz = TimeZone.getTimeZone(id);
            search = (id + " " + place + " "
                    + tz.getDisplayName(false, TimeZone.LONG) + " "
                    + tz.getDisplayName(true, TimeZone.LONG)).toLowerCase(Locale.ROOT);
        }
    }

    private final List<Entry> all = new ArrayList<>();
    private final List<Entry> shown = new ArrayList<>();
    private BaseAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_zone_picker);

        for (String id : Clocks.allZoneIds()) all.add(new Entry(id));
        all.sort((a, b) -> a.city.compareToIgnoreCase(b.city));
        shown.addAll(all);

        adapter = new BaseAdapter() {
            @Override public int getCount() { return shown.size(); }
            @Override public Object getItem(int i) { return shown.get(i); }
            @Override public long getItemId(int i) { return i; }

            @Override
            public View getView(int i, View view, ViewGroup parent) {
                if (view == null) {
                    view = getLayoutInflater().inflate(R.layout.item_zone_pick, parent, false);
                }
                Entry e = shown.get(i);
                ((TextView) view.findViewById(R.id.pick_city)).setText(e.city);
                ((TextView) view.findViewById(R.id.pick_place)).setText(e.place);
                ((TextClock) view.findViewById(R.id.pick_time)).setTimeZone(e.id);
                return view;
            }
        };

        ListView list = findViewById(R.id.zone_list);
        list.setAdapter(adapter);
        list.setOnItemClickListener((p, v, i, id) -> {
            setResult(RESULT_OK, new Intent().putExtra(EXTRA_ZONE, shown.get(i).id));
            finish();
        });

        EditText search = findViewById(R.id.search);
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}

            @Override
            public void afterTextChanged(Editable s) {
                String[] words = s.toString().toLowerCase(Locale.ROOT).trim().split("\\s+");
                shown.clear();
                for (Entry e : all) {
                    boolean match = true;
                    for (String w : words) match &= e.search.contains(w);
                    if (match) shown.add(e);
                }
                adapter.notifyDataSetChanged();
            }
        });
    }
}
