package com.abbysimple.worldclock;

import android.content.Context;
import android.content.SharedPreferences;
import android.icu.text.TimeZoneNames;
import android.icu.util.TimeZone;
import android.icu.util.ULocale;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** The saved clock setup: one main zone for the flip cards plus the list of rows below. */
public final class Clocks {
    public static final class Zone {
        public final String id;
        public String label;

        public Zone(String id, String label) {
            this.id = id;
            this.label = label;
        }
    }

    public String main;
    public final List<Zone> rows = new ArrayList<>();

    private static final String PREFS = "clocks";

    public static Clocks load(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        Clocks clocks = new Clocks();
        clocks.main = prefs.getString("main", "Asia/Manila");
        String json = prefs.getString("rows", null);
        if (json == null) {
            clocks.rows.add(new Zone("America/Los_Angeles", "LAX"));
            clocks.rows.add(new Zone("Asia/Manila", "MNL"));
            return clocks;
        }
        try {
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                JSONObject o = array.getJSONObject(i);
                clocks.rows.add(new Zone(o.getString("id"), o.getString("label")));
            }
        } catch (JSONException ignored) {
            // Corrupt prefs: fall back to whatever parsed.
        }
        return clocks;
    }

    public void save(Context context) {
        JSONArray array = new JSONArray();
        try {
            for (Zone z : rows) array.put(new JSONObject().put("id", z.id).put("label", z.label));
        } catch (JSONException e) {
            throw new IllegalStateException(e);
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putString("main", main)
                .putString("rows", array.toString())
                .apply();
        WorldClockWidget.updateAll(context);
    }

    // ---- Time zone names ----

    /** Every location-based zone (one per city/region, no aliases), plus UTC. */
    public static List<String> allZoneIds() {
        List<String> ids = new ArrayList<>(TimeZone.getAvailableIDs(
                TimeZone.SystemTimeZoneType.CANONICAL_LOCATION, null, null));
        ids.add("UTC");
        return ids;
    }

    public static String city(String id) {
        if (id.equals("UTC")) return "UTC";
        String name = TimeZoneNames.getInstance(ULocale.getDefault()).getExemplarLocationName(id);
        if (name != null) return name;
        return id.substring(id.lastIndexOf('/') + 1).replace('_', ' ');
    }

    public static String country(String id) {
        if (id.equals("UTC")) return "Coordinated Universal Time";
        String region = TimeZone.getRegion(id);
        if (region == null || region.equals("001")) return "";
        return new Locale("", region).getDisplayCountry();
    }

    /** "GMT+8", "GMT-7", "GMT+5:30" for the zone's current offset. */
    public static String gmtOffset(String id) {
        int minutes = java.util.TimeZone.getTimeZone(id).getOffset(System.currentTimeMillis()) / 60000;
        String sign = minutes < 0 ? "-" : "+";
        minutes = Math.abs(minutes);
        String text = "GMT" + sign + (minutes / 60);
        return minutes % 60 == 0 ? text : text + String.format(Locale.US, ":%02d", minutes % 60);
    }

    /** Short row label: the city's airport-style code when known, else its first three letters. */
    public static String defaultLabel(String id) {
        String code = CODES.get(id);
        if (code != null) return code;
        String letters = city(id).replaceAll("[^\\p{L}]", "");
        return letters.substring(0, Math.min(3, letters.length())).toUpperCase(Locale.ROOT);
    }

    private static final Map<String, String> CODES = new HashMap<>();
    static {
        String[] pairs = {
                "America/Los_Angeles", "LAX", "America/New_York", "NYC", "America/Chicago", "CHI",
                "America/Denver", "DEN", "America/Phoenix", "PHX", "America/Anchorage", "ANC",
                "Pacific/Honolulu", "HNL", "America/Toronto", "YYZ", "America/Vancouver", "YVR",
                "America/Mexico_City", "MEX", "America/Sao_Paulo", "SAO",
                "America/Argentina/Buenos_Aires", "BUE", "Europe/London", "LON", "Europe/Paris", "PAR",
                "Europe/Berlin", "BER", "Europe/Madrid", "MAD", "Europe/Rome", "ROM",
                "Europe/Amsterdam", "AMS", "Europe/Moscow", "MOW", "Europe/Istanbul", "IST",
                "Asia/Dubai", "DXB", "Asia/Riyadh", "RUH", "Asia/Kolkata", "IND", "Asia/Karachi", "KHI",
                "Asia/Bangkok", "BKK", "Asia/Ho_Chi_Minh", "SGN", "Asia/Jakarta", "JKT",
                "Asia/Singapore", "SIN", "Asia/Kuala_Lumpur", "KUL", "Asia/Hong_Kong", "HKG",
                "Asia/Shanghai", "CHN", "Asia/Taipei", "TPE", "Asia/Tokyo", "TYO", "Asia/Seoul", "SEL",
                "Asia/Manila", "MNL", "Australia/Sydney", "SYD", "Australia/Melbourne", "MEL",
                "Australia/Brisbane", "BNE", "Australia/Perth", "PER", "Pacific/Auckland", "AKL",
                "Africa/Johannesburg", "JNB", "Africa/Cairo", "CAI", "Africa/Lagos", "LOS",
                "Africa/Nairobi", "NBO", "UTC", "UTC",
        };
        for (int i = 0; i < pairs.length; i += 2) CODES.put(pairs[i], pairs[i + 1]);
    }
}
