package com.abbysimple.worldclock;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.provider.AlarmClock;
import android.widget.RemoteViews;

/**
 * The clocks are TextClocks (see layout/widget.xml), so the launcher keeps them ticking
 * itself; this provider only points them at the saved zones. Tapping opens the settings;
 * the Add alarm button opens the system clock's new-alarm screen.
 */
public class WorldClockWidget extends AppWidgetProvider {
    private static final int[] MAIN_CLOCKS = {
            R.id.hour_tens, R.id.hour_ones, R.id.minute_tens, R.id.minute_ones, R.id.main_date,
    };
    private static final int[] ROW_CLOCKS = {R.id.row_day, R.id.row_time, R.id.row_ampm};

    @Override
    public void onUpdate(Context context, AppWidgetManager manager, int[] ids) {
        RemoteViews views = build(context);
        for (int id : ids) manager.updateAppWidget(id, views);
    }

    /** Redraws every placed widget, e.g. after the clock list changes. */
    public static void updateAll(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        int[] ids = manager.getAppWidgetIds(new ComponentName(context, WorldClockWidget.class));
        if (ids.length > 0) manager.updateAppWidget(ids, build(context));
    }

    private static RemoteViews build(Context context) {
        Clocks clocks = Clocks.load(context);
        String pkg = context.getPackageName();
        RemoteViews views = new RemoteViews(pkg, R.layout.widget);

        for (int clock : MAIN_CLOCKS) views.setString(clock, "setTimeZone", clocks.main);

        views.removeAllViews(R.id.rows);
        for (Clocks.Zone zone : clocks.rows) {
            RemoteViews row = new RemoteViews(pkg, R.layout.widget_row);
            row.setTextViewText(R.id.row_label, zone.label);
            for (int clock : ROW_CLOCKS) row.setString(clock, "setTimeZone", zone.id);
            views.addView(R.id.rows, row);
        }

        Intent settings = new Intent(context, SettingsActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        views.setOnClickPendingIntent(R.id.root, PendingIntent.getActivity(context, 0, settings,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT));

        Intent alarm = new Intent(AlarmClock.ACTION_SET_ALARM)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        views.setOnClickPendingIntent(R.id.add_alarm, PendingIntent.getActivity(context, 1, alarm,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT));
        return views;
    }
}
