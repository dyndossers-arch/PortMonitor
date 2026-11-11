package com.portmonitor.app;

import org.json.JSONObject;

public class Endpoint {
    public String id, name, host, state;
    public int port, timeout;
    public long checkedAt;

    public Endpoint(String id, String name, String host, int port, int timeout, String state, long checkedAt) {
        this.id=id; this.name=name; this.host=host; this.port=port; this.timeout=timeout;
        this.state=state; this.checkedAt=checkedAt;
    }
    public JSONObject toJson() {
        JSONObject o = new JSONObject();
        try {
            o.put("id", id); o.put("name", name); o.put("host", host); o.put("port", port);
            o.put("timeout", timeout); o.put("state", state); o.put("checkedAt", checkedAt);
        } catch (Exception ignored) {}
        return o;
    }
    public static Endpoint fromJson(JSONObject o) {
        return new Endpoint(o.optString("id"), o.optString("name"), o.optString("host"),
                o.optInt("port"), o.optInt("timeout", 3000), o.optString("state", "UNKNOWN"),
                o.optLong("checkedAt", 0));
    }
}
