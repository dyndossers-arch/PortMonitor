package com.portmonitor.app;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class Store {
    private static final String PREF = "port_monitor";
    private static final String ITEMS = "items";
    private static final String RUNNING = "running";

    public static List<Endpoint> all(Context c) {
        ArrayList<Endpoint> out = new ArrayList<>();
        String raw = prefs(c).getString(ITEMS, "[]");
        try {
            JSONArray a = new JSONArray(raw);
            for (int i=0; i<a.length(); i++) out.add(Endpoint.fromJson(a.getJSONObject(i)));
        } catch (Exception ignored) {}
        return out;
    }
    public static void save(Context c, List<Endpoint> items) {
        JSONArray a = new JSONArray();
        for (Endpoint e : items) a.put(e.toJson());
        prefs(c).edit().putString(ITEMS, a.toString()).apply();
    }
    public static void setRunning(Context c, boolean v) { prefs(c).edit().putBoolean(RUNNING, v).apply(); }
    public static boolean isRunning(Context c) { return prefs(c).getBoolean(RUNNING, false); }
    private static SharedPreferences prefs(Context c) { return c.getSharedPreferences(PREF, Context.MODE_PRIVATE); }
}
