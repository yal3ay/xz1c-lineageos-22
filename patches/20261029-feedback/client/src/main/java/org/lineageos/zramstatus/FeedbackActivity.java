package org.lineageos.zramstatus;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.content.res.ColorStateList;
import android.widget.ImageView;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.UserHandle;
import android.view.Gravity;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.util.List;

/** Visible opt-in, local inbox, exact report preview, then an explicit user-mediated share. */
public final class FeedbackActivity extends Activity {
    private LinearLayout root;
    private TextView homeCount;
    private boolean previewing;
    private long generation;
    private boolean submitting;
    private SessionPlayer previewPlayer;
    @Override protected void onPause() {
        if (previewPlayer != null) previewPlayer.pause();
        super.onPause();
    }
    private String sessionPackage;
    private String sessionDescription = "";
    private static final int PROJECTION_REQUEST = 42;
    private static final int NOTICE_REQUEST = 43;
    @Override protected void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        state.putString("session_package", sessionPackage);
        state.putString("session_description", sessionDescription);
    }
    @Override protected void onResume() {
        super.onResume();
        if (root != null && !previewing) showHome();
    }

    private static final java.util.concurrent.ExecutorService UPLOAD =
            java.util.concurrent.Executors.newSingleThreadExecutor();
    private static final int INK = 0xff13233a;
    private static final int MUTED = 0xff6b7a90;
    private static final int BACKGROUND = 0xfff4f7fb;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        if (state != null) { sessionPackage = state.getString("session_package"); sessionDescription = state.getString("session_description", ""); }
        showHome();
    }

    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        showHome();
    }

    @Override public void onBackPressed() {
        if (previewing) showHome(); else super.onBackPressed();
    }

    private void layout(int title) {
        if (previewPlayer != null) { previewPlayer.stopPlayback(); previewPlayer = null; }
        generation++;
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BACKGROUND);
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(root);
        setContentView(scroll);
        getWindow().setStatusBarColor(BACKGROUND);
        getWindow().setNavigationBarColor(BACKGROUND);
        WindowInsetsController bars = getWindow().getInsetsController();
        if (bars != null) bars.setSystemBarsAppearance(
                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                        | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS,
                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                        | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS);
        scroll.setOnApplyWindowInsetsListener((v, insets) -> {
            android.graphics.Insets system = insets.getInsets(WindowInsets.Type.systemBars());
            root.setPadding(dp(16), system.top + dp(20), dp(16), system.bottom + dp(24));
            return WindowInsets.CONSUMED;
        });
        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        ImageView back = new ImageView(this);
        back.setImageResource(R.drawable.ic_sm_back);
        back.setColorFilter(INK);
        back.setPadding(dp(10), dp(10), dp(10), dp(10));
        back.setBackground(round(0xffe6ecf4, 22));
        back.setContentDescription(getString(R.string.feedback_back));
        back.setOnClickListener(v -> onBackPressed());
        header.addView(back, new LinearLayout.LayoutParams(dp(44), dp(44)));
        TextView heading = text(getString(title), 24, INK, true);
        LinearLayout.LayoutParams headingParams = new LinearLayout.LayoutParams(0, -2, 1);
        headingParams.leftMargin = dp(14);
        header.addView(heading, headingParams);
        root.addView(header);
    }

    private void showHome() {
        previewing = false;
        layout(R.string.feedback_title);
        LinearLayout hero = card();
        hero.setPadding(dp(20), dp(18), dp(20), dp(20));
        GradientDrawable heroBackground = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[] {0xff2f80ed, 0xff3aa0e8, 0xff36c2c9});
        heroBackground.setCornerRadius(dp(22));
        hero.setBackground(heroBackground);
        hero.setElevation(dp(4));
        homeCount = text("0", 34, 0xffffffff, true);
        hero.addView(homeCount);
        hero.addView(text(getString(R.string.feedback_saved_title), 15, 0xffffffff, true));
        hero.addView(text(getString(R.string.feedback_hero_hint), 13, 0xebffffff, false));
        LinearLayout about = card();
        about.addView(text(getString(R.string.feedback_privacy_title), 19, INK, true));
        about.addView(text(getString(R.string.feedback_privacy_summary), 14, MUTED, false));
        TextView details = text(getString(R.string.feedback_data_details), 14, 0xff2f80ed, true);
        details.setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle(R.string.feedback_privacy_title).setMessage(R.string.feedback_privacy_body)
                .setPositiveButton(android.R.string.ok, null).show());
        about.addView(details);
        about.addView(text(getString(R.string.feedback_recipient_line,
                getString(R.string.feedback_recipient)), 14, INK, true));
        if (UserHandle.myUserId() != 0) {
            about.addView(text(getString(R.string.feedback_owner_only), 15, INK, false));
            return;
        }
        LinearLayout settings = card();
        boolean supported = FeedbackStore.automaticSupported(this);
        Switch enable = new Switch(this);
        enable.setText("");
        int[][] toggleStates = {{android.R.attr.state_checked}, {}};
        enable.setThumbTintList(new ColorStateList(toggleStates, new int[] {0xff2f80ed, 0xffffffff}));
        enable.setTrackTintList(new ColorStateList(toggleStates, new int[] {0xffa4c6f7, 0xffd3dbe6}));
        enable.setTextColor(INK);
        enable.setTextSize(16);
        enable.setPadding(0, dp(8), 0, dp(8));
        enable.setChecked(supported && FeedbackStore.enabled(this));
        if (!supported && FeedbackStore.enabled(this))
            FeedbackStore.IO.execute(() -> FeedbackStore.setEnabled(this, false));
        LinearLayout toggleRow = new LinearLayout(this);
        toggleRow.setGravity(Gravity.CENTER_VERTICAL);
        TextView toggleLabel = text(getString(R.string.feedback_enable), 16, INK, true);
        LinearLayout.LayoutParams toggleTextParams = new LinearLayout.LayoutParams(0, -2, 1);
        toggleTextParams.rightMargin = dp(12);
        toggleRow.addView(toggleLabel, toggleTextParams);
        toggleRow.addView(enable);
        settings.addView(toggleRow);
        enable.setEnabled(supported);
        settings.addView(text(getString(supported ? R.string.feedback_enable_summary
                : R.string.feedback_update_required), 14, MUTED, false));
        enable.setOnCheckedChangeListener((b, checked) -> {
            if (checked) {
                new AlertDialog.Builder(this).setTitle(R.string.feedback_enable)
                        .setMessage(getString(R.string.feedback_enable_consent) + "\n\n"
                                + getString(R.string.feedback_privacy_body))
                        .setNegativeButton(android.R.string.cancel, (d, w) -> showHome())
                        .setPositiveButton(R.string.feedback_enable_confirm, (d, w) -> {
                            FeedbackStore.IO.execute(() -> {
                                FeedbackStore.setEnabled(this, true);
                                runOnUiThread(() -> {
                                    if (isFinishing()) return;
                                    if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                                            != PackageManager.PERMISSION_GRANTED)
                                        requestPermissions(new String[] {Manifest.permission.POST_NOTIFICATIONS}, 31);
                                    showHome();
                                });
                            });
                        }).setOnCancelListener(d -> showHome()).show();
            } else {
                FeedbackStore.IO.execute(() -> {
                    FeedbackStore.setEnabled(this, false);
                    runOnUiThread(() -> { if (!isFinishing()) showHome(); });
                });
            }
        });
        LinearLayout sessionCard = card();
        sessionCard.addView(text(getString(R.string.session_title), 19, INK, true));
        sessionCard.addView(text(getString(R.string.session_summary), 14, MUTED, false));
        if (FeedbackSessionService.active) {
            sessionCard.addView(text(getString(R.string.session_recording), 15, INK, true));
            sessionCard.addView(button(R.string.session_stop, () -> {
                startService(new Intent(this, FeedbackSessionService.class).setAction(FeedbackSessionService.STOP));
                toast(R.string.session_finishing);
            }));
        } else sessionCard.addView(button(R.string.session_start, this::chooseSessionApp));
        root.addView(button(R.string.feedback_manual, this::manual));
        root.addView(text(getString(R.string.feedback_saved_title), 18, INK, true));
        TextView loading = text(getString(R.string.feedback_loading), 14, MUTED, false);
        root.addView(loading);
        LinearLayout inbox = new LinearLayout(this);
        inbox.setOrientation(LinearLayout.VERTICAL);
        root.addView(inbox);
        root.addView(button(R.string.feedback_delete_all, () -> new AlertDialog.Builder(this)
                .setTitle(R.string.feedback_delete_all).setMessage(R.string.feedback_delete_message)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.feedback_delete_confirm, (d, w) -> {
                    if (FeedbackSessionService.active) startService(new Intent(this, FeedbackSessionService.class).setAction(FeedbackSessionService.CANCEL));
                    FeedbackStore.IO.execute(() -> {
                    FeedbackStore.clear(this);
                    runOnUiThread(() -> { if (!isFinishing()) showHome(); });
                }); }).show()));
        long currentGeneration = generation;
        FeedbackStore.IO.execute(() -> {
            try {
                List<JSONObject> reports = FeedbackStore.list(this);
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed() || currentGeneration != generation) return;
                    homeCount.setText(Integer.toString(reports.size()));
                    loading.setText(reports.isEmpty() ? R.string.feedback_empty : R.string.feedback_saved_summary);
                    for (JSONObject report : reports) {
                        Button row = new Button(this);
                        styleButton(row, false);
                        row.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
                        row.setBackground(round(0xffffffff, 20));
                        row.setBackgroundTintList(null);
                        row.setTextColor(INK);
                        row.setText(report.optString("time_utc_minute") + " · "
                                + kind(report.optString("kind")) + "\n" + displayApp(report) + "  ›");
                        row.setOnClickListener(v -> showPreview(report));
                        inbox.addView(row, new LinearLayout.LayoutParams(-1, -2));
                    }
                    String wanted = getIntent().getStringExtra("report_id");
                    if (wanted != null) {
                        getIntent().removeExtra("report_id");
                        for (JSONObject report : reports) if (wanted.equals(report.optString("id"))) {
                            showPreview(report); break;
                        }
                    }
                });
            } catch (Exception e) { runOnUiThread(() -> { if (currentGeneration == generation && !isDestroyed())
                loading.setText(R.string.feedback_unavailable); }); }
        });
    }

    private String kind(String value) {
        if (value.equals("anr")) return getString(R.string.feedback_kind_anr);
        if (value.equals("native_crash")) return getString(R.string.feedback_kind_native);
        if (value.equals("session")) return getString(R.string.session_title);
        if (value.equals("manual")) return getString(R.string.feedback_kind_manual);
        return getString(R.string.feedback_kind_crash);
    }

    private void chooseSessionApp() {
        if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[] {Manifest.permission.POST_NOTIFICATIONS}, NOTICE_REQUEST);
            return;
        }
        android.app.NotificationManager notifications = getSystemService(android.app.NotificationManager.class);
        android.app.NotificationChannel channel = notifications.getNotificationChannel("bug_session");
        if (!notifications.areNotificationsEnabled() || (channel != null && channel.getImportance() == android.app.NotificationManager.IMPORTANCE_NONE)) {
            toast(R.string.session_notification_required); return;
        }
        java.util.List<android.content.pm.ResolveInfo> apps = getPackageManager().queryIntentActivities(
                new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0);
        java.util.Map<String, String> names = new java.util.TreeMap<>();
        for (android.content.pm.ResolveInfo app : apps) {
            if (app.activityInfo.applicationInfo.uid / 100000 != 0) continue;
            names.put(app.activityInfo.packageName, app.loadLabel(getPackageManager()).toString());
        }
        java.util.List<String> packages = new java.util.ArrayList<>(names.keySet());
        packages.sort((a, b) -> names.get(a).compareToIgnoreCase(names.get(b)));
        String[] labels = new String[packages.size()];
        for (int i = 0; i < labels.length; i++) labels[i] = names.get(packages.get(i));
        new AlertDialog.Builder(this).setTitle(R.string.session_choose_app).setItems(labels, (d, which) -> {
            sessionPackage = packages.get(which);
            LinearLayout choices = new LinearLayout(this);
            choices.setOrientation(LinearLayout.VERTICAL); choices.setPadding(dp(20), dp(8), dp(20), dp(8));
            choices.addView(text(getString(R.string.session_consent_body), 14, INK, false));
            android.widget.EditText explanation = new android.widget.EditText(this);
            explanation.setHint(R.string.feedback_details_hint);
            explanation.setMinLines(2); explanation.setMaxLines(4);
            explanation.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE);
            explanation.setFilters(new android.text.InputFilter[] {new android.text.InputFilter.LengthFilter(4000)});
            choices.addView(explanation);
            CheckBox technical = new CheckBox(this); technical.setText(R.string.session_technical_consent);
            CheckBox screen = new CheckBox(this); screen.setText(R.string.session_video_consent);
            technical.setChecked(false); screen.setChecked(false);
            choices.addView(technical); choices.addView(screen);
            ScrollView choiceScroll = new ScrollView(this); choiceScroll.addView(choices);
            AlertDialog dialog = new AlertDialog.Builder(this).setTitle(R.string.session_start).setView(choiceScroll)
                    .setNegativeButton(android.R.string.cancel, null).setPositiveButton(R.string.session_start, (a, b) -> {
                        if (!technical.isChecked()) return;
                        sessionDescription = explanation.getText().toString().trim();
                        if (screen.isChecked()) {
                            android.media.projection.MediaProjectionManager pm = getSystemService(android.media.projection.MediaProjectionManager.class);
                            startActivityForResult(pm.createScreenCaptureIntent(), PROJECTION_REQUEST);
                        } else startSession(false, null, 0);
                    }).create();
            dialog.setOnShowListener(v -> {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                technical.setOnCheckedChangeListener((b, checked) -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(checked));
            }); dialog.show();
        }).setNegativeButton(android.R.string.cancel, null).show();
    }
    @Override public void onRequestPermissionsResult(int request, String[] permissions, int[] grants) {
        super.onRequestPermissionsResult(request, permissions, grants);
        if (request == NOTICE_REQUEST && grants.length > 0 && grants[0] == PackageManager.PERMISSION_GRANTED) chooseSessionApp();
    }
    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (request == PROJECTION_REQUEST && result == RESULT_OK && data != null) startSession(true, data, result);
    }
    private void startSession(boolean video, Intent permission, int result) {
        if (sessionPackage == null || FeedbackSessionService.active || submitting) return;
        try {
            // Do not delete anyone's earlier recordings to make room.
            FeedbackStore.list(this);
            java.io.File[] stored = new java.io.File(getFilesDir(), "feedback_video").listFiles();
            long bytes = 0;
            if (stored != null) for (java.io.File f : stored) bytes += f.length();
            if (video && bytes >= 40L * 1024 * 1024) { toast(R.string.session_storage_full); return; }
            Intent start = new Intent(this, FeedbackSessionService.class).putExtra("package", sessionPackage)
                    .putExtra("description", sessionDescription).putExtra("video", video).putExtra("projection_data", permission).putExtra("projection_code", result);
            startForegroundService(start);
            toast(R.string.session_go_app);
            moveTaskToBack(true);
        } catch (Exception ignored) { toast(R.string.feedback_unavailable); }
    }

    private void manual() {
        String[] labels = getResources().getStringArray(R.array.feedback_symptoms);
        String[] codes = {"crash", "slow", "black_screen", "playback", "other"};
        new AlertDialog.Builder(this).setTitle(R.string.feedback_manual)
                .setItems(labels, (d, which) -> manualChooseApp(codes[which]))
                .setNegativeButton(android.R.string.cancel, null).show();
    }

    private void manualChooseApp(String symptom) {
        java.util.Map<String, String> apps = new java.util.TreeMap<>();
        for (android.content.pm.ResolveInfo app : getPackageManager().queryIntentActivities(
                new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0)) {
            apps.put(app.activityInfo.packageName, app.loadLabel(getPackageManager()).toString());
        }
        java.util.List<String> packages = new java.util.ArrayList<>(apps.keySet());
        packages.sort((a, b) -> apps.get(a).compareToIgnoreCase(apps.get(b)));
        String[] labels = new String[packages.size() + 1];
        labels[0] = getString(R.string.feedback_system_app);
        for (int i = 0; i < packages.size(); i++) labels[i + 1] = apps.get(packages.get(i));
        new AlertDialog.Builder(this).setTitle(R.string.session_choose_app).setItems(labels, (d, which) ->
                manualDetails(symptom, which == 0 ? null : packages.get(which - 1), labels[which]))
                .setNegativeButton(android.R.string.cancel, null).show();
    }

    private void manualDetails(String symptom, String pkg, String label) {
        LinearLayout fields = new LinearLayout(this);
        fields.setOrientation(LinearLayout.VERTICAL);
        fields.setPadding(dp(20), dp(8), dp(20), dp(8));
        android.widget.EditText app = new android.widget.EditText(this);
        app.setSingleLine(true);
        app.setHint(R.string.feedback_app_hint);
        app.setText(label);
        app.setFocusable(false);
        app.setFilters(new android.text.InputFilter[] {new android.text.InputFilter.LengthFilter(100)});
        android.widget.EditText details = new android.widget.EditText(this);
        details.setHint(R.string.feedback_details_hint);
        details.setMinLines(3); details.setMaxLines(6);
        details.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE | android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        details.setFilters(new android.text.InputFilter[] {new android.text.InputFilter.LengthFilter(4000)});
        fields.addView(app); fields.addView(details);
        ScrollView fieldScroll = new ScrollView(this); fieldScroll.addView(fields);
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle(R.string.feedback_details_title)
                .setView(fieldScroll).setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.feedback_details_continue, null).create();
        dialog.setOnShowListener(v -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(button -> {
            String application = app.getText().toString().trim();
            String description = details.getText().toString().trim();
            if (application.isEmpty()) { app.setError(getString(R.string.feedback_required)); return; }
            if (description.isEmpty()) { details.setError(getString(R.string.feedback_required)); return; }
            dialog.dismiss();
            FeedbackStore.IO.execute(() -> {
                try {
                    JSONObject report = FeedbackStore.manual(this, symptom, application, description, pkg);
                    runOnUiThread(() -> { if (!isFinishing()) showPreview(report); });
                } catch (Exception e) { runOnUiThread(() -> toast(R.string.feedback_unavailable)); }
            });
        }));
        dialog.show();
    }

    private void showPreview(JSONObject report) {
        previewing = true;
        layout(R.string.feedback_preview_title);
        FeedbackReceiver.cancelNotification(this);
        if (report.optString("kind").equals("session") && !FeedbackSessionService.active)
            getSystemService(android.app.NotificationManager.class).cancel(6202);
        LinearLayout intro = card();
        intro.addView(text(getString(R.string.feedback_recipient_line,
                getString(R.string.feedback_recipient)), 16, INK, true));
        intro.addView(text(getString(R.string.feedback_preview_summary), 14, MUTED, false));
        TextView content = text("", 12, INK, false);
        content.setTypeface(Typeface.MONOSPACE);
        content.setTextIsSelectable(true);
        try { content.setText(FeedbackStore.preview(report, getString(R.string.feedback_recipient))); }
        catch (Exception e) { content.setText(R.string.feedback_unavailable); }
        card().addView(content);
        boolean hasVideo = report.optJSONObject("session") != null && report.optJSONObject("session").has("video");
        CheckBox videoConsent = new CheckBox(this);
        if (hasVideo) {
            LinearLayout videoCard = card();
            videoCard.addView(text(getString(R.string.session_video_warning), 14, INK, true));
            SessionPlayer player = new SessionPlayer(this);
            previewPlayer = player;
            android.widget.FrameLayout videoFrame = new android.widget.FrameLayout(this);
            videoFrame.setBackground(round(0xff000000, 16));
            videoFrame.setClipToOutline(true);
            videoFrame.addView(player, new android.widget.FrameLayout.LayoutParams(-1, -1, Gravity.CENTER));
            try { player.setVideoPath(FeedbackStore.video(this, report.getString("id")).getAbsolutePath()); }
            catch (Exception ignored) { }
            android.widget.MediaController controls = new android.widget.MediaController(this);
            controls.setAnchorView(videoFrame); player.setMediaController(controls);
            videoCard.addView(videoFrame, new LinearLayout.LayoutParams(-1, dp(280)));
            videoCard.addView(button(R.string.session_play, player::start));
            videoCard.addView(button(R.string.session_remove_video, () -> {
                if (submitting) return;
                player.stopPlayback();
                FeedbackStore.IO.execute(() -> {
                    try { FeedbackStore.removeVideo(this, report); runOnUiThread(() -> showPreview(report)); }
                    catch (Exception ignored) { runOnUiThread(() -> toast(R.string.feedback_unavailable)); }
                });
            }));
            videoConsent.setText(getString(R.string.session_video_send_consent, getString(R.string.feedback_recipient)));
            videoConsent.setTextColor(INK); videoConsent.setChecked(false);
            root.addView(videoConsent);
        }
        if (report.optString("kind").equals("session")) root.addView(button(R.string.session_discard, () -> {
            if (submitting) return;
            new AlertDialog.Builder(this).setTitle(R.string.session_discard).setMessage(R.string.session_discard_confirm)
                    .setNegativeButton(android.R.string.cancel, null)
                    .setPositiveButton(R.string.feedback_delete_confirm, (d, w) -> FeedbackStore.IO.execute(() -> {
                        try { FeedbackStore.discard(this, report); runOnUiThread(this::showHome); }
                        catch (Exception ignored) { runOnUiThread(() -> toast(R.string.feedback_unavailable)); }
                    })).show();
        }));
        CheckBox consent = new CheckBox(this);
        consent.setText(getString(R.string.feedback_share_consent, getString(R.string.feedback_recipient)));
        consent.setTextColor(INK);
        consent.setTextSize(14);
        consent.setButtonTintList(ColorStateList.valueOf(0xff2f80ed));
        consent.setPadding(0, dp(12), 0, dp(12));
        consent.setChecked(false);
        root.addView(consent);
        Button share = button(R.string.feedback_share, () -> { });
        styleButton(share, true);
        String endpoint = getString(R.string.feedback_endpoint);
        boolean configured = !endpoint.isEmpty() && FeedbackProvider.uploadToken().length() >= 32;
        if (!configured) root.addView(text(getString(R.string.feedback_submission_unavailable), 14, MUTED, false));
        share.setEnabled(false);
        consent.setOnCheckedChangeListener((b, checked) -> share.setEnabled(checked && (!hasVideo || videoConsent.isChecked()) && configured && !submitting));
        videoConsent.setOnCheckedChangeListener((b, checked) -> share.setEnabled(consent.isChecked() && checked && configured && !submitting));
        share.setOnClickListener(v -> {
            if (!consent.isChecked() || (hasVideo && !videoConsent.isChecked()) || !configured || submitting) return;
            submitting = true;
            share.setEnabled(false);
            share.setText(R.string.feedback_submitting);
            UPLOAD.execute(() -> {
                boolean success = false;
                try {
                    JSONObject payload = FeedbackStore.submissionPayload(report);
                    if (payload.optString("kind").equals("session"))
                        Submission.sendSession(this, endpoint, payload);
                    else Submission.send(endpoint, payload);
                    success = true;
                } catch (Exception ignored) { /* Never log payloads, addresses or exception messages. */ }
                boolean submitted = success;
                runOnUiThread(() -> {
                    submitting = false;
                    if (isFinishing() || isDestroyed()) return;
                    if (submitted) {
                        share.setText(R.string.feedback_submitted);
                        share.setEnabled(false);
                        toast(R.string.feedback_submission_success);
                    } else {
                        share.setText(R.string.feedback_share);
                        share.setEnabled(consent.isChecked() && (!hasVideo || videoConsent.isChecked()) && configured);
                        toast(R.string.feedback_submission_failed);
                    }
                });
            });
        });
        root.addView(share);
        root.addView(button(R.string.feedback_return, this::showHome));
    }

    /** Gives the decoder bounded byte reads, without sharing a private-file descriptor or public URI. */
    private final class SessionPlayer extends android.view.SurfaceView
            implements android.view.SurfaceHolder.Callback, android.widget.MediaController.MediaPlayerControl {
        private android.media.MediaPlayer player;
        private android.media.MediaDataSource source;
        private android.widget.MediaController controls;
        private String path;
        private boolean prepared, wantPlay;
        private int videoWidth, videoHeight;
        SessionPlayer(android.content.Context context) {
            super(context); getHolder().addCallback(this);
            setOnClickListener(v -> { if (controls != null && prepared) controls.show(); });
        }
        void setVideoPath(String path) { this.path = path; }
        void setMediaController(android.widget.MediaController controller) {
            controls = controller; controls.setMediaPlayer(this); controls.setEnabled(false);
        }
        @Override public void surfaceCreated(android.view.SurfaceHolder holder) {
            if (path == null) return;
            try {
                final java.io.RandomAccessFile file = new java.io.RandomAccessFile(path, "r");
                final long length = file.length();
                source = new android.media.MediaDataSource() {
                    private boolean closed;
                    @Override public synchronized int readAt(long position, byte[] data, int offset, int size) throws java.io.IOException {
                        if (closed || position < 0 || position >= length) return -1;
                        if (size == 0) return 0;
                        file.seek(position);
                        return file.read(data, offset, (int) Math.min(Math.min(size, 65536), length - position));
                    }
                    @Override public long getSize() { return length; }
                    @Override public synchronized void close() throws java.io.IOException {
                        if (!closed) { closed = true; file.close(); }
                    }
                };
                player = new android.media.MediaPlayer();
                player.setDisplay(holder); player.setDataSource(source); player.setVolume(0, 0);
                player.setOnPreparedListener(p -> {
                    prepared = true;
                    updateVideoSize(p.getVideoWidth(), p.getVideoHeight());
                    p.setVideoScalingMode(android.media.MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT);
                    setContentDescription(getString(R.string.session_play) + " (" + (p.getDuration() / 1000) + "s)");
                    if (controls != null) controls.setEnabled(true);
                    if (wantPlay) start();
                });
                player.setOnVideoSizeChangedListener((p, width, height) -> updateVideoSize(width, height));
                player.setOnErrorListener((p, what, extra) -> {
                    stopPlayback(); toast(R.string.feedback_unavailable); return true;
                });
                player.setOnCompletionListener(p -> { if (controls != null) controls.show(); });
                player.prepareAsync();
            } catch (Exception ignored) { stopPlayback(); toast(R.string.feedback_unavailable); }
        }
        private void updateVideoSize(int width, int height) {
            if (width <= 0 || height <= 0) return;
            videoWidth = width; videoHeight = height;
            getHolder().setFixedSize(width, height);
            requestLayout();
        }
        @Override protected void onMeasure(int widthSpec, int heightSpec) {
            int width = android.view.View.MeasureSpec.getSize(widthSpec);
            int height = android.view.View.MeasureSpec.getSize(heightSpec);
            if (videoWidth > 0 && videoHeight > 0 && width > 0 && height > 0) {
                if ((long) videoWidth * height > (long) width * videoHeight)
                    height = Math.max(1, (int) ((long) width * videoHeight / videoWidth));
                else width = Math.max(1, (int) ((long) height * videoWidth / videoHeight));
            }
            setMeasuredDimension(width, height);
        }
        @Override public void surfaceChanged(android.view.SurfaceHolder holder, int format, int width, int height) { }
        @Override public void surfaceDestroyed(android.view.SurfaceHolder holder) { stopPlayback(); }
        void stopPlayback() {
            prepared = false; wantPlay = false;
            if (controls != null) controls.hide();
            if (player != null) { player.release(); player = null; }
            if (source != null) { try { source.close(); } catch (Exception ignored) { } source = null; }
        }
        @Override public void start() {
            wantPlay = true;
            if (prepared && player != null) { player.start(); if (controls != null) controls.show(); }
        }
        @Override public void pause() { wantPlay = false; if (prepared && player != null) player.pause(); }
        @Override public int getDuration() { return prepared && player != null ? player.getDuration() : 0; }
        @Override public int getCurrentPosition() { return prepared && player != null ? player.getCurrentPosition() : 0; }
        @Override public void seekTo(int position) { if (prepared && player != null) player.seekTo(Math.max(0,position)); }
        @Override public boolean isPlaying() { return prepared && player != null && player.isPlaying(); }
        @Override public int getBufferPercentage() { return 100; }
        @Override public boolean canPause() { return true; }
        @Override public boolean canSeekBackward() { return true; }
        @Override public boolean canSeekForward() { return true; }
        @Override public int getAudioSessionId() { return player == null ? 0 : player.getAudioSessionId(); }
    }

    /** Only this explicit user action has a network path; collection and notices never call it. */
    static final class Submission {
        static void sendSession(android.content.Context context, String endpoint, JSONObject report) throws Exception {
            if (!endpoint.endsWith("/api/reports")) throw new java.io.IOException();
            java.net.URI uri = new java.net.URI(endpoint.substring(0, endpoint.length() - 7) + "sessions");
            if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null || uri.getRawUserInfo() != null
                    || uri.getRawQuery() != null || uri.getRawFragment() != null) throw new java.io.IOException();
            byte[] json = report.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
            if (json.length > FeedbackStore.MAX_REPORT_BYTES) throw new java.io.IOException();
            JSONObject meta = report.getJSONObject("session").optJSONObject("video");
            java.io.File video = FeedbackStore.video(context, report.getString("incident_id"));
            long size = meta == null ? 0 : meta.getLong("bytes");
            if (size < 0 || size > FeedbackSessionService.MAX_VIDEO || (size > 0 && (!video.isFile() || video.length() != size)))
                throw new java.io.IOException();
            javax.net.ssl.HttpsURLConnection c = (javax.net.ssl.HttpsURLConnection) uri.toURL().openConnection();
            try {
                c.setInstanceFollowRedirects(false); c.setConnectTimeout(15000); c.setReadTimeout(30000);
                c.setUseCaches(false); c.setRequestMethod("POST"); c.setDoOutput(true);
                c.setRequestProperty("Content-Type", "application/vnd.xz1c.bug-session");
                c.setRequestProperty("Authorization", "Bearer " + FeedbackProvider.uploadToken());
                c.setFixedLengthStreamingMode(4L + json.length + size);
                try (java.io.DataOutputStream out = new java.io.DataOutputStream(c.getOutputStream())) {
                    out.writeInt(json.length); out.write(json);
                    if (size > 0) try (java.io.InputStream in = new java.io.FileInputStream(video)) {
                        byte[] block = new byte[8192]; int n;
                        while ((n = in.read(block)) != -1) out.write(block, 0, n);
                    }
                }
                if (c.getResponseCode() != 201) throw new java.io.IOException();
                java.io.ByteArrayOutputStream ack = new java.io.ByteArrayOutputStream();
                try (java.io.InputStream in = c.getInputStream()) {
                    byte[] block = new byte[512]; int n;
                    while ((n = in.read(block)) != -1) { if (ack.size() + n > 4096) throw new java.io.IOException(); ack.write(block, 0, n); }
                }
                if (!report.getString("incident_id").equals(new JSONObject(ack.toString("UTF-8")).optString("report_id")))
                    throw new java.io.IOException();
            } finally { c.disconnect(); }
        }
        static void send(String endpoint, JSONObject report) throws Exception {
            java.net.URI uri = new java.net.URI(endpoint);
            if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null
                    || uri.getRawUserInfo() != null || uri.getRawQuery() != null || uri.getRawFragment() != null)
                throw new java.io.IOException("Invalid receiving endpoint");
            byte[] body = report.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
            if (body.length > FeedbackStore.MAX_REPORT_BYTES) throw new java.io.IOException("Report too large");
            javax.net.ssl.HttpsURLConnection c = (javax.net.ssl.HttpsURLConnection) uri.toURL().openConnection();
            try {
                c.setInstanceFollowRedirects(false);
                c.setConnectTimeout(15000); c.setReadTimeout(15000);
                c.setUseCaches(false); c.setRequestMethod("POST"); c.setDoOutput(true);
                c.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                c.setRequestProperty("Accept", "application/json");
                c.setRequestProperty("Authorization", "Bearer " + FeedbackProvider.uploadToken());
                c.setRequestProperty("User-Agent", "XZ1C-Feedback/1");
                c.setFixedLengthStreamingMode(body.length);
                try (java.io.OutputStream out = c.getOutputStream()) { out.write(body); }
                if (c.getResponseCode() != 201) throw new java.io.IOException("Submission rejected");
                java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
                try (java.io.InputStream in = c.getInputStream()) {
                    byte[] block = new byte[512]; int n;
                    while ((n = in.read(block)) != -1) {
                        if (buffer.size() + n > 4096) throw new java.io.IOException("Invalid acknowledgement");
                        buffer.write(block, 0, n);
                    }
                }
                JSONObject response = new JSONObject(buffer.toString("UTF-8"));
                if (!report.getString("incident_id").equals(response.optString("report_id")))
                    throw new java.io.IOException("Invalid acknowledgement");
            } finally { c.disconnect(); }
        }
    }

    private LinearLayout card() {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(dp(18), dp(12), dp(18), dp(12));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xffffffff);
        bg.setCornerRadius(dp(22));
        c.setBackground(bg);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.topMargin = dp(14);
        params.bottomMargin = dp(10);
        root.addView(c, params);
        return c;
    }

    private TextView text(String text, int size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(text); t.setTextSize(size); t.setTextColor(color);
        t.setPadding(0, dp(6), 0, dp(6));
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }

    private Button button(int label, Runnable action) {
        Button b = new Button(this);
        b.setText(label);
        styleButton(b, false);
        b.setOnClickListener(v -> action.run());
        return b;
    }

    private void styleButton(Button b, boolean primary) {
        b.setAllCaps(false);
        b.setTextSize(15);
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setPadding(dp(16), dp(12), dp(16), dp(12));
        b.setMinHeight(dp(48));
        b.setStateListAnimator(null);
        b.setElevation(0);
        b.setBackground(round(primary ? 0xff2f80ed : 0xffe8f0fc, 16));
        int[][] states = {{android.R.attr.state_enabled}, {}};
        b.setBackgroundTintList(new ColorStateList(states,
                new int[] {primary ? 0xff2f80ed : 0xffe8f0fc, 0xffe9edf3}));
        b.setTextColor(new ColorStateList(states,
                new int[] {primary ? 0xffffffff : 0xff2f6fd6, 0xff8b99ab}));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = dp(10); lp.bottomMargin = dp(4);
        b.setLayoutParams(lp);
    }

    private GradientDrawable round(int color, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color); d.setCornerRadius(dp(radius));
        return d;
    }

    private String displayApp(JSONObject report) {
        String name = report.optString("package");
        if (name.isEmpty()) return report.optString("application", getString(R.string.feedback_kind_manual));
        try { return getPackageManager().getApplicationLabel(
                getPackageManager().getApplicationInfo(name, 0)).toString(); }
        catch (PackageManager.NameNotFoundException ignored) { return name; }
    }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    private void toast(int message) { if (!isFinishing()) Toast.makeText(this, message, Toast.LENGTH_LONG).show(); }
}
