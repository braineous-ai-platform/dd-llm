package io.braineous.dd.llm.cr.model;

import com.google.gson.*;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class CommitRequest {

    private String queryKind;          // which catalog entry / query kind this decision is for
    private String catalogVersion;
    private String actor;              // manual user / system (string)
    private List<String> notes;        // optional human notes
    private JsonObject payload;        // the thing being committed (manual loop can carry normalized output)

    public CommitRequest() {
    }

    public String getQueryKind() { return queryKind; }
    public void setQueryKind(String queryKind) { this.queryKind = queryKind; }

    public String getCatalogVersion() { return catalogVersion; }
    public void setCatalogVersion(String catalogVersion) { this.catalogVersion = catalogVersion; }


    public String getActor() { return actor; }
    public void setActor(String actor) { this.actor = actor; }

    public List<String> getNotes() { return notes; }
    public void setNotes(List<String> notes) { this.notes = notes; }

    public JsonObject getPayload() { return payload; }
    public void setPayload(JsonObject payload) { this.payload = payload; }

    // -------------------------
    // Hardening helpers
    // -------------------------

    public String safeQueryKind() {
        if (this.queryKind == null) { return null; }
        String t = this.queryKind.trim();
        if (t.isEmpty()) { return null; }
        return t;
    }

    public String safeCatalogVersion() {
        if (this.catalogVersion == null) { return null; }
        String t = this.catalogVersion.trim();
        if (t.isEmpty()) { return null; }
        return t;
    }

    public String safeActor() {
        if (this.actor == null) { return null; }
        String t = this.actor.trim();
        if (t.isEmpty()) { return null; }
        return t;
    }

    public List<String> safeNotes() {
        if (this.notes == null) { return Collections.emptyList(); }
        return this.notes;
    }

    // -------------------------
    // JSON serialization
    // -------------------------

    public JsonObject toJson() {
        JsonObject root = new JsonObject();

        String qk = safeQueryKind();
        if (qk != null) { root.addProperty("queryKind", qk); }

        String v = safeCatalogVersion();
        if (v != null) { root.addProperty("catalogVersion", v); }


        String a = safeActor();
        if (a != null) { root.addProperty("actor", a); }

        JsonArray arr = new JsonArray();
        List<String> list = safeNotes();
        for (int i = 0; i < list.size(); i++) {
            String s = list.get(i);
            if (s != null) {
                String t = s.trim();
                if (!t.isEmpty()) { arr.add(t); }
            }
        }
        root.add("notes", arr);

        if (this.payload != null) {
            root.add("payload", JsonParser.parseString(this.payload.toString()).getAsJsonObject());
        }

        return root;
    }

    public String toJsonString() {
        return toJson().toString();
    }

    public static CommitRequest fromJson(JsonObject json) {
        if (json == null) { return null; }

        CommitRequest r = new CommitRequest();

        if (json.has("queryKind") && !json.get("queryKind").isJsonNull()) {
            try { r.setQueryKind(json.get("queryKind").getAsString()); } catch (RuntimeException re) { }
        }

        if (json.has("catalogVersion") && !json.get("catalogVersion").isJsonNull()) {
            try { r.setCatalogVersion(json.get("catalogVersion").getAsString()); } catch (RuntimeException re) { }
        }


        if (json.has("actor") && !json.get("actor").isJsonNull()) {
            try { r.setActor(json.get("actor").getAsString()); } catch (RuntimeException re) { }
        }


        if (json.has("notes") && !json.get("notes").isJsonNull()) {
            try {
                if (json.get("notes").isJsonArray()) {
                    JsonArray a = json.get("notes").getAsJsonArray();
                    ArrayList<String> list = new ArrayList<String>();
                    for (int i = 0; i < a.size(); i++) {
                        JsonElement e = a.get(i);
                        if (e != null && !e.isJsonNull()) {
                            try {
                                String s = e.getAsString();
                                if (s != null) {
                                    String t = s.trim();
                                    if (!t.isEmpty()) { list.add(t); }
                                }
                            } catch (RuntimeException re2) { }
                        }
                    }
                    r.setNotes(list);
                }
            } catch (RuntimeException re) { }
        }

        if (json.has("payload") && !json.get("payload").isJsonNull()) {
            try {
                if (json.get("payload").isJsonObject()) {
                    JsonObject p = json.get("payload").getAsJsonObject();
                    r.setPayload(JsonParser.parseString(p.toString()).getAsJsonObject());
                }
            } catch (RuntimeException re) { }
        }

        return r;
    }

    public static CommitRequest fromJsonString(String json) {
        if (json == null) { return null; }
        String t = json.trim();
        if (t.isEmpty()) { return null; }

        try {
            JsonElement el = JsonParser.parseString(t);
            if (el == null || !el.isJsonObject()) { return null; }
            return fromJson(el.getAsJsonObject());
        } catch (RuntimeException re) {
            return null;
        }
    }

    //----------------------------------------
    private static String safeTrim(String s) {
        if (s == null) {
            return "";
        }
        String t = s.trim();
        if (t.isEmpty()) {
            return "";
        }
        return t;
    }


    public static String canonicalJsonString(JsonObject obj) {
        if (obj == null) {
            return "";
        }
        return canonicalElement(obj);
    }

    private static String canonicalElement(JsonElement el) {
        if (el == null || el.isJsonNull()) {
            return "null";
        }

        if (el.isJsonObject()) {
            JsonObject o = el.getAsJsonObject();

            List<String> keys = new ArrayList<String>();
            for (Map.Entry<String, JsonElement> me : o.entrySet()) {
                if (me != null && me.getKey() != null) {
                    keys.add(me.getKey());
                }
            }
            Collections.sort(keys);

            StringBuilder sb = new StringBuilder();
            sb.append("{");
            for (int i = 0; i < keys.size(); i++) {
                String k = keys.get(i);
                if (i > 0) {
                    sb.append(",");
                }
                sb.append(quote(k));
                sb.append(":");
                sb.append(canonicalElement(o.get(k)));
            }
            sb.append("}");
            return sb.toString();
        }

        if (el.isJsonArray()) {
            JsonArray a = el.getAsJsonArray();
            StringBuilder sb = new StringBuilder();
            sb.append("[");
            for (int i = 0; i < a.size(); i++) {
                if (i > 0) {
                    sb.append(",");
                }
                sb.append(canonicalElement(a.get(i)));
            }
            sb.append("]");
            return sb.toString();
        }

        if (el.isJsonPrimitive()) {
            JsonPrimitive p = el.getAsJsonPrimitive();
            if (p.isString()) {
                return quote(p.getAsString());
            }
            if (p.isBoolean()) {
                return p.getAsBoolean() ? "true" : "false";
            }
            if (p.isNumber()) {
                // Gson preserves numeric text reasonably; keep as string form.
                return p.getAsNumber().toString();
            }
            return quote(p.getAsString());
        }

        // Fallback (shouldn't happen)
        return el.toString();
    }

    private static String quote(String s) {
        if (s == null) {
            return "\"\"";
        }
        // minimal JSON escaping
        String t = s.replace("\\", "\\\\").replace("\"", "\\\"");
        return "\"" + t + "\"";
    }



    private static byte[] sha256Bytes(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(s != null ? s.getBytes(java.nio.charset.StandardCharsets.UTF_8)
                    : new byte[0]);
            return md.digest();
        } catch (NoSuchAlgorithmException e) {
            // should never happen on JVM; fail fast
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private static String toHex(byte[] bytes) {
        if (bytes == null) {
            return "";
        }
        char[] hex = "0123456789abcdef".toCharArray();
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (int i = 0; i < bytes.length; i++) {
            int v = bytes[i] & 0xFF;
            sb.append(hex[v >>> 4]);
            sb.append(hex[v & 0x0F]);
        }
        return sb.toString();
    }

}

