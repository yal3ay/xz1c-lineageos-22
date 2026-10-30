package org.lineageos.zramstatus;

import android.content.Context;
import android.os.SystemProperties;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Bounded, read-only media diagnostics for a user-started session. Never changes log buffers. */
final class PlaybackDiagnostics {
    private static final Pattern LINE = Pattern.compile("^\\s*(\\d+\\.\\d+)\\s+(\\d+)\\s+(\\d+)\\s+[VDIWEF]\\s+([^:]+):\\s?(.*)$");
    private static final String[] TAGS = {"ACodec", "CCodec", "CCodecConfig", "MediaCodec", "OMXNodeInstance", "OMXClient", "OMX-VDEC-1080P", "OMX-VDEC-720P", "OMX-VDEC", "ColorUtils", "SurfaceUtils", "NuPlayerDecoder", "MediaCodecVideoRenderer"};
    static JSONObject collect(Context context, long start, long end, Set<Integer> selectedPids) {
        JSONObject result = new JSONObject();
        try {
            result.put("scope", "selected_app_and_shared_media");
            result.put("renderer", safe(SystemProperties.get("debug.hwui.renderer", "unknown"), 80));
            result.put("renderer_32bit", safe(SystemProperties.get("debug.hwui.renderer_32bit", "unknown"), 80));
            ArrayList<String> command = new ArrayList<>();
            java.util.Collections.addAll(command, "/system/bin/logcat", "-d", "-v", "epoch", "-T", String.format(Locale.US, "%.3f", start / 1000.0));
            for (String tag : TAGS) command.add(tag + ":V");
            command.add("*:S");
            Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            java.util.concurrent.atomic.AtomicBoolean limited = new java.util.concurrent.atomic.AtomicBoolean();
            Thread reader = new Thread(() -> {
                try (InputStream in = process.getInputStream()) {
                    byte[] block = new byte[4096]; int n;
                    while ((n = in.read(block)) != -1) {
                        if (output.size() + n > 262144) { limited.set(true); process.destroyForcibly(); break; }
                        output.write(block, 0, n);
                    }
                } catch (Exception ignored) { }
            }, "BugMediaLogReader");
            reader.setDaemon(true); reader.start();
            boolean done = process.waitFor(3, TimeUnit.SECONDS);
            if (!done) process.destroyForcibly();
            reader.join(1000);
            if (reader.isAlive()) { process.destroyForcibly(); result.put("status", "unavailable"); }
            else {
                JSONArray records = extract(output.toString("UTF-8"), start, end, selectedPids);
                result.put("records", records);
                result.put("status", !done || limited.get() ? "partial" : process.exitValue() != 0 ? "unavailable" : records.length() == 0 ? "no_matching_records" : "captured");
            }
            if (!result.has("records")) result.put("records", new JSONArray());
        } catch (Exception ignored) {
            try { result.put("status", "unavailable").put("records", new JSONArray()); } catch (Exception unused) { }
        }
        return result;
    }
    static JSONArray extract(String text, long start, long end, Set<Integer> pids) throws Exception {
        LinkedHashSet<String> records = new LinkedHashSet<>();
        int bytes = 0;
        for (String line : text.split("\\n")) {
            Matcher m = LINE.matcher(line);
            if (!m.matches()) continue;
            double seconds;
            int pid;
            try { seconds = Double.parseDouble(m.group(1)); pid = Integer.parseInt(m.group(2)); }
            catch (NumberFormatException ignored) { continue; }
            if (seconds * 1000 < start || seconds * 1000 > end) continue;
            String tag = m.group(4).trim();
            if (!java.util.Arrays.asList(TAGS).contains(tag)) continue;
            String message = safe(m.group(5), 180);
            String record = String.format(Locale.US, "+%.1fs %s %s: %s", (seconds * 1000 - start) / 1000.0,
                    pids.contains(pid) ? "selected_app" : "shared_media", tag, message);
            if (records.contains(record)) continue;
            int size = record.getBytes(StandardCharsets.UTF_8).length;
            while (!records.isEmpty() && (records.size() >= 48 || bytes + size > 8192)) {
                String oldest = records.iterator().next(); records.remove(oldest);
                bytes -= oldest.getBytes(StandardCharsets.UTF_8).length;
            }
            records.add(record); bytes += size;
        }
        return new JSONArray(records);
    }
    private static String safe(String value, int max) {
        value = value.replaceAll("(?i)(?:https?|file|content|rtsp)://[^\\s]+", "[address]")
                .replaceAll("[\\p{Cntrl}]", " ").trim();
        return value.length() > max ? value.substring(0, max) : value;
    }
    private PlaybackDiagnostics() { }
}
