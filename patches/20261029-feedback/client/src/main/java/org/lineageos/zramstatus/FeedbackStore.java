package org.lineageos.zramstatus;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.os.Build;
import android.os.DropBoxManager;
import android.os.UserHandle;
import android.util.AtomicFile;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Local report store; reproduction sessions may include bounded media diagnostics. Upload is explicit. */
final class FeedbackStore {
    static final ExecutorService IO = Executors.newSingleThreadExecutor();
    static final long DAY = 24L * 60 * 60 * 1000;
    static final int MAX_REPORTS = 20;
    static final int MAX_REPORT_BYTES = 65536;
    private static final Object LOCK = new Object();
    private static final String PREFS = "system_feedback";

    static SharedPreferences prefs(Context c) { return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE); }
    static boolean enabled(Context c) { return prefs(c).getBoolean("enabled", false); }

    static boolean automaticSupported(Context c) {
        return new File("/system/etc/sysconfig/xz1c-bug-feedback.xml").isFile()
                && c.checkSelfPermission("android.permission.READ_DROPBOX_DATA")
                == android.content.pm.PackageManager.PERMISSION_GRANTED;
    }

    static void setEnabled(Context c, boolean enabled) {
        synchronized (LOCK) {
            SharedPreferences.Editor e = prefs(c).edit().putBoolean("enabled", enabled);
            if (enabled) e.putLong("enabled_since", System.currentTimeMillis())
                    .putLong("last_seen", System.currentTimeMillis()).putLong("last_notice", 0);
            e.commit();
            c.getPackageManager().setComponentEnabledSetting(
                    new android.content.ComponentName(c, FeedbackReceiver.class),
                    enabled ? android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                            : android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    android.content.pm.PackageManager.DONT_KILL_APP);
            if (!enabled) FeedbackReceiver.cancelNotification(c);
        }
    }

    private static File directory(Context c, String name) throws Exception {
        File f = new File(c.getFilesDir(), name);
        if (!f.isDirectory() && !f.mkdirs()) throw new java.io.IOException("Storage unavailable");
        return f;
    }

    static void collect(Context c, String tag, long eventTime) {
        if (!enabled(c) || UserHandle.myUserId() != 0 || !CrashSanitizer.TAGS.contains(tag)) return;
        long now = System.currentTimeMillis();
        if (eventTime < prefs(c).getLong("enabled_since", now) || eventTime > now + 60000) return;
        try {
            DropBoxManager manager = c.getSystemService(DropBoxManager.class);
            if (manager == null) return;
            try (DropBoxManager.Entry entry = manager.getNextEntry(tag, eventTime - 1)) {
                if (entry == null || entry.getTimeMillis() != eventTime) {
                    return;
                }
                CrashSanitizer.Result result = CrashSanitizer.parse(tag,
                        entry.getText(CrashSanitizer.MAX_INPUT), UserHandle.myUserId());
                if (result == null) {
                    return;
                }
                ApplicationInfo app = c.getPackageManager().getApplicationInfo(result.packageName, 0);
                if (app.uid != result.uid) {
                    return;
                }
                PackageInfo pkg = c.getPackageManager().getPackageInfo(result.packageName, 0);
                JSONObject report = base(result.kind, eventTime);
                report.put("package", result.packageName);
                report.put("app_version_code", pkg.getLongVersionCode());
                report.put("exception_types", new JSONArray(result.exceptions));
                report.put("code_frames", new JSONArray(result.frames));
                if (!result.signal.isEmpty()) report.put("signal", result.signal);
                synchronized (LOCK) {
                    if (!enabled(c)) return;
                    String signature = result.packageName + "|" + result.kind + "|"
                            + result.exceptions + "|" + result.frames + "|" + result.signal;
                    String key = digest(signature) + ":" + (eventTime / DAY);
                    for (JSONObject previous : listLocked(c)) {
                        if (key.equals(previous.optString("local_dedup_key"))) return;
                    }
                    report.put("local_dedup_key", key);
                    saveLocked(c, report);
                    prefs(c).edit().putLong("last_seen", Math.max(eventTime,
                            prefs(c).getLong("last_seen", 0))).apply();
                    FeedbackReceiver.notifyIfAllowed(c, report.getString("id"));
                }
            }
        } catch (Exception ignored) {
            // Never persist or log the exception message: it may itself contain private text.
        }
    }

    static void catchUp(Context c) {
        if (!enabled(c) || UserHandle.myUserId() != 0) return;
        try {
            DropBoxManager manager = c.getSystemService(DropBoxManager.class);
            long after = Math.max(prefs(c).getLong("last_seen", 0),
                    Math.max(prefs(c).getLong("enabled_since", System.currentTimeMillis()),
                            System.currentTimeMillis() - DAY));
            int budget = 12;
            for (String tag : CrashSanitizer.TAGS) {
                long cursor = after;
                while (budget > 0) {
                    long t;
                    try (DropBoxManager.Entry entry = manager.getNextEntry(tag, cursor)) {
                        if (entry == null) break;
                        t = entry.getTimeMillis();
                    }
                    cursor = t;
                    budget--;
                    collect(c, tag, t);
                }
            }
        } catch (Exception ignored) { }
    }

    static JSONObject manual(Context c, String symptom, String application, String description, String pkg) throws Exception {
        if (UserHandle.myUserId() != 0) throw new SecurityException();
        if (!java.util.Arrays.asList("crash", "slow", "black_screen", "playback", "other").contains(symptom))
            throw new IllegalArgumentException();
        JSONObject r = base("manual", System.currentTimeMillis());
        if (application == null || application.trim().isEmpty() || application.length() > 100
                || description == null || description.trim().isEmpty() || description.length() > 4000)
            throw new IllegalArgumentException();
        r.put("symptom", symptom).put("application", application.trim()).put("description", description.trim());
        if (pkg != null) {
            PackageInfo info = c.getPackageManager().getPackageInfo(pkg, 0);
            r.put("package", pkg).put("app_version_code", info.getLongVersionCode());
            if (info.versionName != null) r.put("app_version_name", info.versionName.substring(0, Math.min(100, info.versionName.length())));
        }
        synchronized (LOCK) { saveLocked(c, r); }
        return r;
    }

    static JSONObject base(String kind, long time) throws Exception {
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'", Locale.US);
        df.setTimeZone(TimeZone.getTimeZone("UTC"));
        JSONObject r = new JSONObject();
        r.put("schema", 1);
        r.put("id", UUID.randomUUID().toString()); // Incident identifier, not a device identifier.
        r.put("time_utc_minute", df.format(new Date(time)));
        r.put("kind", kind);
        r.put("device", "Sony Xperia XZ1 Compact / SO-02K");
        r.put("android_sdk", Build.VERSION.SDK_INT);
        r.put("system_build", safeBuild(Build.VERSION.INCREMENTAL));
        r.put("security_patch", Build.VERSION.SECURITY_PATCH.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}")
                ? Build.VERSION.SECURITY_PATCH : "unknown");
        return r;
    }

    static void saveSession(Context c, JSONObject r) throws Exception {
        synchronized (LOCK) {
            if (r.optLong("local_clear_generation", 0) != prefs(c).getLong("clear_generation", 0))
                throw new java.io.IOException("Discarded session");
            saveLocked(c, r);
        }
    }

    static File video(Context c, String id) throws Exception {
        if (!id.matches("[0-9a-f-]{36}")) throw new IllegalArgumentException();
        return new File(directory(c, "feedback_video"), id + ".mp4");
    }

    static void discard(Context c, JSONObject r) throws Exception {
        synchronized (LOCK) {
            new AtomicFile(new File(directory(c, "feedback_reports"), r.getString("id") + ".json")).delete();
            video(c, r.getString("id")).delete();
        }
    }

    static void removeVideo(Context c, JSONObject r) throws Exception {
        synchronized (LOCK) {
            video(c, r.getString("id")).delete();
            JSONObject session = r.getJSONObject("session");
            session.remove("video");
            saveLocked(c, r);
        }
    }

    static String safeBuild(String value) {
        return value != null && value.matches("[0-9]{1,20}") ? value : "unknown";
    }

    private static String digest(String input) throws Exception {
        StringBuilder s = new StringBuilder();
        for (byte b : MessageDigest.getInstance("SHA-256").digest(input.getBytes(StandardCharsets.UTF_8)))
            s.append(String.format(Locale.US, "%02x", b & 255));
        return s.toString();
    }

    static List<JSONObject> list(Context c) throws Exception {
        synchronized (LOCK) { pruneLocked(c); return listLocked(c); }
    }

    private static List<JSONObject> listLocked(Context c) throws Exception {
        List<JSONObject> result = new ArrayList<>();
        File[] files = directory(c, "feedback_reports").listFiles();
        if (files == null) return result;
        java.util.Arrays.sort(files, Comparator.comparingLong(File::lastModified).reversed());
        for (File file : files) {
            if (!file.getName().matches("[0-9a-f-]{36}\\.json") || file.length() > MAX_REPORT_BYTES) continue;
            try { result.add(new JSONObject(new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8))); }
            catch (Exception ignored) { }
            if (result.size() >= MAX_REPORTS) break;
        }
        return result;
    }

    private static void saveLocked(Context c, JSONObject report) throws Exception {
        byte[] data = report.toString(2).getBytes(StandardCharsets.UTF_8);
        if (data.length > MAX_REPORT_BYTES) throw new java.io.IOException("Report too large");
        AtomicFile f = new AtomicFile(new File(directory(c, "feedback_reports"), report.getString("id") + ".json"));
        FileOutputStream stream = null;
        try { stream = f.startWrite(); stream.write(data); f.finishWrite(stream); }
        catch (Exception e) { if (stream != null) f.failWrite(stream); throw e; }
        pruneLocked(c);
    }

    private static void pruneLocked(Context c) throws Exception {
        File[] files = directory(c, "feedback_reports").listFiles();
        if (files != null) {
            java.util.Arrays.sort(files, Comparator.comparingLong(File::lastModified).reversed());
            for (int i = 0; i < files.length; i++) {
                if (i >= MAX_REPORTS || files[i].lastModified() < System.currentTimeMillis() - 7 * DAY)
                    new AtomicFile(files[i]).delete();
            }
        }
        File[] videos = directory(c, "feedback_video").listFiles();
        if (videos != null) for (File video : videos) {
            String id = video.getName().replace(".mp4", "");
            // An in-progress recording has no report yet. Give it the bounded recording window.
            if (!new File(directory(c, "feedback_reports"), id + ".json").isFile()
                    && video.lastModified() < System.currentTimeMillis() - 5 * 60 * 1000L) video.delete();
        }
    }

    static JSONObject submissionPayload(JSONObject r) throws Exception {
        JSONObject payload = new JSONObject();
        payload.put("schema", 1);
        String id = r.optString("id");
        if (!id.matches("[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}"))
            throw new java.io.IOException("Invalid incident");
        payload.put("incident_id", id); // Random per incident, never a stable phone identifier.
        for (String key : new String[] {"time_utc_minute", "kind", "symptom", "device", "android_sdk",
                "system_build", "security_patch", "application", "description", "package", "app_version_code", "app_version_name", "signal",
                "exception_types", "code_frames"}) {
            if (r.has(key)) payload.put(key, r.get(key));
        }
        if ("session".equals(r.optString("kind"))) payload.put("session", sessionPayload(r.getJSONObject("session")));
        return payload;
    }

    private static JSONObject fields(JSONObject source, String... keys) throws Exception {
        JSONObject result = new JSONObject();
        for (String key : keys) if (source.has(key)) result.put(key, source.get(key));
        return result;
    }

    private static JSONObject sessionPayload(JSONObject source) throws Exception {
        JSONObject result = fields(source, "duration_seconds", "playback");
        JSONArray samples = new JSONArray(), events = new JSONArray();
        JSONArray original = source.getJSONArray("samples");
        for (int i = 0; i < Math.min(37, original.length()); i++) samples.put(fields(original.getJSONObject(i),
                "elapsed_seconds", "available_memory_mb", "low_memory", "interactive"));
        original = source.getJSONArray("events");
        for (int i = 0; i < Math.min(3, original.length()); i++) events.put(fields(original.getJSONObject(i),
                "schema", "incident_id", "time_utc_minute", "kind", "device", "android_sdk", "system_build",
                "security_patch", "package", "app_version_code", "signal", "exception_types", "code_frames"));
        result.put("samples", samples).put("events", events);
        if (source.has("video")) result.put("video", fields(source.getJSONObject("video"), "bytes", "sha256"));
        return result;
    }

    static String preview(JSONObject r, String recipient) throws Exception {
        return "XZ1C Bug report / Bug 报告\nRecipient / 接收方: " + recipient
                + "\n\n" + submissionPayload(r).toString(2) + "\n";
    }

    static void clear(Context c) {
        synchronized (LOCK) {
            for (String name : new String[] {"feedback_reports", "feedback_video"}) {
                File[] files = new File(c.getFilesDir(), name).listFiles();
                if (files != null) for (File f : files) f.delete();
            }
            prefs(c).edit().putLong("clear_generation", prefs(c).getLong("clear_generation", 0) + 1)
                    .remove("collector_status").remove("last_broadcast_received")
                    .putLong("last_seen", System.currentTimeMillis())
                    .putLong("enabled_since", System.currentTimeMillis()).apply();
            FeedbackReceiver.cancelNotification(c);
        }
    }

    private FeedbackStore() { }
}
