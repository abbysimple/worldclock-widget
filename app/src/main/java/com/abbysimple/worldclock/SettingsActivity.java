package com.abbysimple.worldclock;

import android.app.Activity;
import android.app.AlertDialog;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.provider.AlarmClock;
import android.text.InputFilter;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextClock;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Choose the main (flip card) zone and manage the extra rows. Changes apply immediately. */
public class SettingsActivity extends Activity {
    private static final int PICK_MAIN = 1;
    private static final int PICK_ROW = 2;

    private Clocks clocks;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        clocks = Clocks.load(this);

        // Opened by the launcher to set up or reconfigure a widget: always accept it.
        int widgetId = getIntent().getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID);
        if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            setResult(RESULT_OK, new Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId));
            WorldClockWidget.updateAll(this);
        }

        findViewById(R.id.add_zone).setOnClickListener(v -> pick(PICK_ROW));
        findViewById(R.id.add_alarm).setOnClickListener(v ->
                startActivity(new Intent(AlarmClock.ACTION_SET_ALARM)));
        findViewById(R.id.pin_widget).setOnClickListener(v -> pinWidget());
        render();
    }

    private void render() {
        ViewGroup main = findViewById(R.id.main_item);
        main.removeAllViews();
        main.addView(zoneItem(main, clocks.main, Clocks.defaultLabel(clocks.main), v -> pick(PICK_MAIN)));

        ViewGroup rows = findViewById(R.id.rows_list);
        rows.removeAllViews();
        for (int i = 0; i < clocks.rows.size(); i++) {
            Clocks.Zone zone = clocks.rows.get(i);
            int index = i;
            View item = zoneItem(rows, zone.id, zone.label, v -> editRow(index));
            if (i > 0) ((LinearLayout.LayoutParams) item.getLayoutParams()).topMargin = dp(8);
            rows.addView(item);
        }
        findViewById(R.id.empty_rows).setVisibility(clocks.rows.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private View zoneItem(ViewGroup parent, String id, String label, View.OnClickListener onClick) {
        View item = getLayoutInflater().inflate(R.layout.item_zone, parent, false);
        ((TextView) item.findViewById(R.id.item_label)).setText(label);
        ((TextView) item.findViewById(R.id.item_place)).setText(describe(id));
        ((TextClock) item.findViewById(R.id.item_time)).setTimeZone(id);
        item.setOnClickListener(onClick);
        return item;
    }

    static String describe(String id) {
        String country = Clocks.country(id);
        String place = Clocks.city(id);
        if (!country.isEmpty() && !country.equals(place)) place += ", " + country;
        return place + " · " + Clocks.gmtOffset(id);
    }

    private void editRow(int index) {
        Clocks.Zone zone = clocks.rows.get(index);
        List<String> labels = new ArrayList<>();
        List<Runnable> actions = new ArrayList<>();
        labels.add("Rename");
        actions.add(() -> rename(zone));
        if (index > 0) {
            labels.add("Move up");
            actions.add(() -> Collections.swap(clocks.rows, index, index - 1));
        }
        if (index < clocks.rows.size() - 1) {
            labels.add("Move down");
            actions.add(() -> Collections.swap(clocks.rows, index, index + 1));
        }
        if (!zone.id.equals(clocks.main)) {
            labels.add("Use as main clock");
            actions.add(() -> clocks.main = zone.id);
        }
        labels.add("Remove");
        actions.add(() -> clocks.rows.remove(index));

        new AlertDialog.Builder(this)
                .setTitle(zone.label + " — " + Clocks.city(zone.id))
                .setItems(labels.toArray(new String[0]), (d, which) -> {
                    actions.get(which).run();
                    changed();
                })
                .show();
    }

    private void rename(Clocks.Zone zone) {
        EditText input = new EditText(this);
        input.setText(zone.label);
        input.setSelectAllOnFocus(true);
        input.setSingleLine();
        input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(10)});
        FrameLayout box = new FrameLayout(this);
        box.setPadding(dp(20), dp(8), dp(20), 0);
        box.addView(input);

        new AlertDialog.Builder(this)
                .setTitle("Label for " + Clocks.city(zone.id))
                .setMessage("Short names fit best, like LAX or NYC.")
                .setView(box)
                .setPositiveButton("Save", (d, w) -> {
                    String label = input.getText().toString().trim();
                    zone.label = label.isEmpty() ? Clocks.defaultLabel(zone.id) : label;
                    changed();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void pick(int request) {
        startActivityForResult(new Intent(this, ZonePickerActivity.class), request);
    }

    @Override
    protected void onActivityResult(int request, int result, Intent data) {
        if (result != RESULT_OK || data == null) return;
        String id = data.getStringExtra(ZonePickerActivity.EXTRA_ZONE);
        if (request == PICK_MAIN) {
            clocks.main = id;
        } else {
            clocks.rows.add(new Clocks.Zone(id, Clocks.defaultLabel(id)));
        }
        changed();
    }

    private void changed() {
        clocks.save(this);
        render();
    }

    private void pinWidget() {
        AppWidgetManager manager = AppWidgetManager.getInstance(this);
        if (manager.isRequestPinAppWidgetSupported()) {
            manager.requestPinAppWidget(new ComponentName(this, WorldClockWidget.class), null, null);
        } else {
            Toast.makeText(this, R.string.add_manually, Toast.LENGTH_LONG).show();
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
