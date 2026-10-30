package org.lineageos.zramstatus;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Handler;
import android.os.Looper;
import android.os.IBinder;
import android.os.UserHandle;
import android.os.SystemClock;
import android.os.PowerManager;
import android.os.DropBoxManager;
import android.view.Surface;
import org.json.*;
import java.io.*;
import java.security.MessageDigest;
import java.util.Locale;

/** User-started bounded session with media diagnostic excerpts and optional silent video. */
public final class FeedbackSessionService extends Service {
    static final String STOP = "org.lineageos.zramstatus.STOP_SESSION";
    static final String CANCEL = "org.lineageos.zramstatus.CANCEL_SESSION";
    static final long MAX_VIDEO = 20L * 1024 * 1024;
    private static final int NOTICE = 6202;
    private static final String CHANNEL = "bug_session";
    static volatile boolean active;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private long started, wallStart;
    private JSONObject report;
    private final JSONArray samples = new JSONArray();
    private MediaProjection projection;
    private VirtualDisplay display;
    private Encoder encoder;
    private File video;
    private PowerManager.WakeLock wakeLock;
    private boolean finishing;
    private final java.util.Set<Integer> selectedPids = new java.util.HashSet<>();
    private final Runnable tick = new Runnable() {
        @Override public void run() {
            if (finishing) return;
            try {
                ActivityManager manager = getSystemService(ActivityManager.class);
                java.util.List<ActivityManager.RunningAppProcessInfo> processes = manager.getRunningAppProcesses();
                if (processes != null) for (ActivityManager.RunningAppProcessInfo process : processes) {
                    if (selectedPids.size() < 64 && (process.processName.equals(report.optString("package"))
                            || process.processName.startsWith(report.optString("package") + ":"))) selectedPids.add(process.pid);
                }
                ActivityManager.MemoryInfo memory = new ActivityManager.MemoryInfo();
                getSystemService(ActivityManager.class).getMemoryInfo(memory);
                samples.put(new JSONObject().put("elapsed_seconds", Math.min(180,
                        (SystemClock.elapsedRealtime() - started) / 1000))
                        .put("available_memory_mb", memory.availMem / (1024 * 1024))
                        .put("low_memory", memory.lowMemory)
                        .put("interactive", getSystemService(PowerManager.class).isInteractive()));
            } catch (Exception ignored) { }
            if (samples.length() >= 37 || SystemClock.elapsedRealtime() - started >= 180000) finish(false);
            else handler.postDelayed(this, 5000);
        }
    };
    @Override public IBinder onBind(Intent i) { return null; }
    @Override public int onStartCommand(Intent intent, int flags, int id) {
        if (intent == null) { stopSelf(); return START_NOT_STICKY; }
        if (STOP.equals(intent.getAction())) { finish(false); return START_NOT_STICKY; }
        if (CANCEL.equals(intent.getAction())) { finish(true); return START_NOT_STICKY; }
        if (active || finishing) return START_NOT_STICKY;
        try {
            if (UserHandle.myUserId() != 0) throw new SecurityException();
            String pkg = intent.getStringExtra("package");
            if (pkg == null || !pkg.matches("[A-Za-z_$][A-Za-z0-9_$]*(\\.[A-Za-z_$][A-Za-z0-9_$]*)+"))
                throw new IllegalArgumentException();
            android.content.pm.PackageInfo app = getPackageManager().getPackageInfo(pkg, 0);
            long version = app.getLongVersionCode();
            NotificationManager nm = getSystemService(NotificationManager.class);
            NotificationChannel ch = new NotificationChannel(CHANNEL, getString(R.string.session_title), NotificationManager.IMPORTANCE_LOW);
            ch.setSound(null, null); ch.enableVibration(false); ch.setShowBadge(false); nm.createNotificationChannel(ch);
            boolean withVideo = intent.getBooleanExtra("video", false);
            startForeground(NOTICE, notice(true), withVideo ? ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
                    : ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
            started = SystemClock.elapsedRealtime(); wallStart = System.currentTimeMillis();
            report = FeedbackStore.base("session", wallStart);
            report.put("package", pkg).put("app_version_code", version)
                    .put("local_clear_generation", FeedbackStore.prefs(this).getLong("clear_generation", 0));
            report.put("application", app.applicationInfo.loadLabel(getPackageManager()).toString().substring(0,
                    Math.min(100, app.applicationInfo.loadLabel(getPackageManager()).length())));
            if (app.versionName != null) report.put("app_version_name", app.versionName.substring(0, Math.min(100, app.versionName.length())));
            String description = intent.getStringExtra("description");
            if (description != null && !description.trim().isEmpty()) report.put("description", description.substring(0, Math.min(4000, description.length())));
            wakeLock = getSystemService(PowerManager.class).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "ZramStatus:BugSession");
            wakeLock.acquire(185000);
            active = true;
            if (withVideo) {
                Intent consent = intent.getParcelableExtra("projection_data", Intent.class);
                if (intent.getIntExtra("projection_code", 0) != Activity.RESULT_OK || consent == null) throw new SecurityException();
                projection = getSystemService(MediaProjectionManager.class).getMediaProjection(Activity.RESULT_OK, consent);
                if (projection == null) throw new SecurityException();
                projection.registerCallback(new MediaProjection.Callback() {
                    @Override public void onStop() { finish(false); }
                }, handler);
                video = FeedbackStore.video(this, report.getString("id"));
                android.util.DisplayMetrics metrics = getResources().getDisplayMetrics();
                boolean landscape = metrics.widthPixels > metrics.heightPixels;
                int width = landscape ? 848 : 480, height = landscape ? 480 : 848;
                encoder = new Encoder(video, width, height, handler, () -> finish(false), () -> finish(true));
                display = projection.createVirtualDisplay("Bug recording", width, height, metrics.densityDpi,
                        DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, encoder.surface(), null, handler);
            }
            handler.post(tick);
        } catch (Exception ignored) {
            android.widget.Toast.makeText(this, R.string.feedback_unavailable, android.widget.Toast.LENGTH_LONG).show();
            finish(true);
        }
        return START_NOT_STICKY;
    }
    private Notification notice(boolean ongoing) {
        Intent open = new Intent(this, FeedbackActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        if (report != null) open.putExtra("report_id", report.optString("id"));
        PendingIntent view = PendingIntent.getActivity(this, NOTICE, open, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder b = new Notification.Builder(this, CHANNEL).setSmallIcon(R.drawable.ic_sm_feedback)
                .setContentTitle(getString(ongoing ? R.string.session_recording : R.string.session_ready))
                .setContentText(getString(ongoing ? R.string.session_notice : R.string.session_ready_hint))
                .setContentIntent(view).setOngoing(ongoing).setOnlyAlertOnce(true).setVisibility(Notification.VISIBILITY_SECRET);
        if (ongoing) {
            PendingIntent stop = PendingIntent.getService(this, 1, new Intent(this, FeedbackSessionService.class).setAction(STOP),
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            PendingIntent cancel = PendingIntent.getService(this, 2, new Intent(this, FeedbackSessionService.class).setAction(CANCEL),
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            b.addAction(new Notification.Action.Builder(null, getString(R.string.session_stop), stop).build());
            b.addAction(new Notification.Action.Builder(null, getString(R.string.session_discard), cancel).build());
        }
        return b.build();
    }
    private void finish(boolean discard) {
        if (finishing) return;
        finishing = true; handler.removeCallbacksAndMessages(null);
        if (wakeLock != null && wakeLock.isHeld()) wakeLock.release();
        if (display != null) { display.release(); display = null; }
        if (projection != null) { projection.stop(); projection = null; }
        final boolean cancelled = discard;
        final long wallEnd = System.currentTimeMillis();
        final java.util.Set<Integer> pids = new java.util.HashSet<>(selectedPids);
        final JSONObject finished = report;
        final long duration = Math.min(180, Math.max(0, (SystemClock.elapsedRealtime() - started) / 1000));
        // Keep the service alive until the bounded local write finishes.
        FeedbackStore.IO.execute(() -> {
            boolean saved = false;
            if (encoder != null) {
                encoder.close();
                if (!encoder.valid() && video != null) video.delete();
            }
            try {
                if (!cancelled && finished != null) {
                    JSONObject session = new JSONObject().put("duration_seconds", duration).put("samples", samples)
                            .put("events", events(finished.optString("package")))
                            .put("playback", PlaybackDiagnostics.collect(this, wallStart, wallEnd, pids));
                    if (video != null && video.isFile() && video.length() > 0 && video.length() <= MAX_VIDEO) {
                        MessageDigest digest = MessageDigest.getInstance("SHA-256");
                        try (InputStream in = new FileInputStream(video)) {
                            byte[] block = new byte[8192]; int n;
                            while ((n = in.read(block)) != -1) digest.update(block, 0, n);
                        }
                        StringBuilder hex = new StringBuilder();
                        for (byte b : digest.digest()) hex.append(String.format(Locale.US, "%02x", b & 255));
                        session.put("video", new JSONObject().put("bytes", video.length()).put("sha256", hex.toString()));
                    } else if (video != null) video.delete();
                    finished.put("session", session);
                    FeedbackStore.saveSession(this, finished); saved = true;
                }
            } catch (Exception ignored) { }
            if (!saved && video != null) video.delete();
            final boolean ready = saved;
            handler.post(() -> {
                active = false;
                stopForeground(STOP_FOREGROUND_REMOVE);
                if (ready) getSystemService(NotificationManager.class).notify(NOTICE, notice(false));
                stopSelf();
            });
        });
    }
    private JSONArray events(String pkg) {
        JSONArray events = new JSONArray();
        try {
            DropBoxManager db = getSystemService(DropBoxManager.class);
            int budget = 48;
            long end = wallStart + Math.min(180000, SystemClock.elapsedRealtime() - started);
            for (String tag : CrashSanitizer.TAGS) {
                long after = wallStart;
                while (budget-- > 0 && events.length() < 3) {
                    try (DropBoxManager.Entry e = db.getNextEntry(tag, after)) {
                        if (e == null || e.getTimeMillis() > end) break;
                        after = e.getTimeMillis();
                        CrashSanitizer.Result r = CrashSanitizer.parse(tag, e.getText(CrashSanitizer.MAX_INPUT), 0);
                        if (r == null || !pkg.equals(r.packageName)
                                || getPackageManager().getApplicationInfo(pkg, 0).uid != r.uid) continue;
                        JSONObject v = FeedbackStore.base(r.kind, after);
                        v.put("package", pkg).put("app_version_code", report.getLong("app_version_code"))
                                .put("exception_types", new JSONArray(r.exceptions)).put("code_frames", new JSONArray(r.frames.subList(0, Math.min(12, r.frames.size()))));
                        if (!r.signal.isEmpty()) v.put("signal", r.signal);
                        events.put(FeedbackStore.submissionPayload(v));
                    }
                }
            }
        } catch (Exception ignored) { }
        return events;
    }
    /** Encoder receives pixels; the muxer writes inside this process, never handing a private file to mediaserver. */
    private static final class Encoder implements AutoCloseable {
        private android.media.MediaCodec codec;
        private android.media.MediaMuxer muxer;
        private Surface input;
        private Thread thread;
        private volatile boolean stopping;
        private volatile boolean failed;
        private volatile long stopAt;
        private final java.util.concurrent.CountDownLatch done = new java.util.concurrent.CountDownLatch(1);
        private final Runnable limit, error;
        private final Handler main;
        Encoder(File file, int width, int height, Handler main, Runnable limit, Runnable error) throws Exception {
            this.main = main; this.limit = limit; this.error = error;
            try {
                android.media.MediaFormat format = android.media.MediaFormat.createVideoFormat("video/avc", width, height);
                format.setInteger(android.media.MediaFormat.KEY_COLOR_FORMAT,
                        android.media.MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
                format.setInteger(android.media.MediaFormat.KEY_BIT_RATE, 600000);
                format.setInteger(android.media.MediaFormat.KEY_FRAME_RATE, 20);
                format.setInteger(android.media.MediaFormat.KEY_I_FRAME_INTERVAL, 2);
                codec = android.media.MediaCodec.createEncoderByType("video/avc");
                codec.configure(format, null, null, android.media.MediaCodec.CONFIGURE_FLAG_ENCODE);
                input = codec.createInputSurface();
                muxer = new android.media.MediaMuxer(file.getAbsolutePath(), android.media.MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
                codec.start();
                thread = new Thread(this::drain, "BugVideoEncoder"); thread.start();
            } catch (Exception e) {
                failed = true;
                if (codec != null) try { codec.release(); } catch (Exception ignored) { }
                if (muxer != null) try { muxer.release(); } catch (Exception ignored) { }
                if (input != null) input.release();
                throw e;
            }
        }
        Surface surface() { return input; }
        private void drain() {
            boolean writing = false, eos = false;
            int track = -1; long bytes = 0, firstPts = -1, lastPts = -1;
            android.media.MediaCodec.BufferInfo info = new android.media.MediaCodec.BufferInfo();
            try {
                while (true) {
                    if (stopping && !eos) { codec.signalEndOfInputStream(); eos = true; }
                    if (stopping && SystemClock.elapsedRealtime() > stopAt) throw new java.io.IOException();
                    int index = codec.dequeueOutputBuffer(info, 10000);
                    if (index == android.media.MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        if (writing) throw new java.io.IOException();
                        track = muxer.addTrack(codec.getOutputFormat()); muxer.start(); writing = true;
                    } else if (index >= 0) {
                        try {
                            if ((info.flags & android.media.MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) info.size = 0;
                            if (info.size > 0) {
                                if (!writing) throw new java.io.IOException();
                                java.nio.ByteBuffer buffer = codec.getOutputBuffer(index);
                                if (buffer == null) throw new java.io.IOException();
                                buffer.position(info.offset); buffer.limit(info.offset + info.size);
                                if (firstPts < 0) firstPts = info.presentationTimeUs;
                                info.presentationTimeUs = Math.max(lastPts + 1, info.presentationTimeUs - firstPts);
                                lastPts = info.presentationTimeUs;
                                muxer.writeSampleData(track, buffer, info); bytes += info.size;
                                if (bytes >= MAX_VIDEO - 512 * 1024 && !stopping) {
                                    stopAt = SystemClock.elapsedRealtime() + 3000; stopping = true; main.post(limit);
                                }
                            }
                        } finally { codec.releaseOutputBuffer(index, false); }
                        if ((info.flags & android.media.MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) break;
                    }
                }
                if (!writing || bytes == 0) failed = true;
            } catch (Exception ignored) { failed = true; main.post(error); }
            finally {
                try { codec.stop(); } catch (Exception ignored) { failed = true; }
                try { codec.release(); } catch (Exception ignored) { }
                if (writing) try { muxer.stop(); } catch (Exception ignored) { failed = true; }
                try { muxer.release(); } catch (Exception ignored) { }
                input.release(); done.countDown();
            }
        }
        @Override public void close() {
            stopAt = SystemClock.elapsedRealtime() + 3000; stopping = true;
            try { if (!done.await(4, java.util.concurrent.TimeUnit.SECONDS)) failed = true; }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); failed = true; }
        }
        boolean valid() { return !failed && done.getCount() == 0; }
    }
    @Override public void onDestroy() {
        if (!finishing) finish(true);
        active = false;
        super.onDestroy();
    }
    @Override public void onTaskRemoved(Intent rootIntent) { finish(true); }
}
