package com.example.vocably;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.SetOptions;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VersionTracker {

    private static final String STORE_URL =
            "https://play.google.com/store/apps/details?id=com.example.vocably";

    private ListenerRegistration listener;
    private android.app.Dialog activeDialog;

    public void start(@NonNull Activity activity) {
        String currentVersion = getCurrentVersion(activity);

        DocumentReference ref = FirebaseFirestore.getInstance()
                .collection("versions")
                .document("current");

        ref.get().addOnSuccessListener(snap -> {
            if (!snap.exists()) {
                Map<String, Object> defaults = new HashMap<>();
                defaults.put("version",    currentVersion);
                defaults.put("bypass_key", "");
                defaults.put("changelog",  Arrays.asList(activity.getString(R.string.version_tracker_new)));

                ref.set(defaults, SetOptions.merge())
                        .addOnSuccessListener(a -> startListener(activity, ref, currentVersion))
                        .addOnFailureListener(e -> {});
            } else {
                startListener(activity, ref, currentVersion);
            }
        }).addOnFailureListener(e -> startListener(activity, ref, currentVersion));
    }

    private void startListener(@NonNull Activity activity,
                               @NonNull DocumentReference ref,
                               @NonNull String currentVersion) {
        listener = ref.addSnapshotListener(
                com.google.firebase.firestore.MetadataChanges.INCLUDE, (doc, error) -> {
                    if (error != null || doc == null) return;
                    if (doc.getMetadata().isFromCache() && !doc.exists()) return;
                    if (!doc.exists()) return;
                    if (activity.isFinishing() || activity.isDestroyed()) return;

                    String requiredVersion = doc.getString("version");
                    if (requiredVersion == null || requiredVersion.isEmpty()) return;

                    if (!currentVersion.equals(requiredVersion)) {
                        String bypassKey = doc.getString("bypass_key");
                        List<String> changelog = (List<String>) doc.get("changelog");
                        activity.runOnUiThread(() ->
                                showUpdateDialog(activity, requiredVersion, changelog, bypassKey));
                    } else {
                        activity.runOnUiThread(this::dismissDialog);
                    }
                });
    }

    public void stop() {
        if (listener != null) {
            listener.remove();
            listener = null;
        }
        dismissDialog();
    }

    private void showUpdateDialog(@NonNull Activity activity,
                                  String requiredVersion,
                                  List<String> changelog,
                                  String bypassKey) {
        if (activeDialog != null && activeDialog.isShowing()) return;

        android.app.Dialog dialog = new android.app.Dialog(activity);
        View v = LayoutInflater.from(activity)
                .inflate(R.layout.dialog_version_update, null);

        ((TextView) v.findViewById(R.id.tvUpdateVersion))
                .setText(activity.getString(R.string.necesary_update));

        LinearLayout changelogContainer = v.findViewById(R.id.changelogContainer);
        changelogContainer.removeAllViews();
        if (changelog != null && !changelog.isEmpty()) {
            for (String item : changelog) {
                TextView tvItem = new TextView(activity);
                tvItem.setText(item);
                tvItem.setTextColor(androidx.core.content.ContextCompat
                        .getColor(activity, android.R.color.darker_gray));
                tvItem.setTextSize(13f);
                tvItem.setPadding(0, 4, 0, 4);
                changelogContainer.addView(tvItem);
            }
        }

        v.findViewById(R.id.tvUpdateBtn).setOnClickListener(btn ->
                activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(STORE_URL))));

        View bypassTrigger = v.findViewById(R.id.bypassTrigger);
        LinearLayout bypassPanel = v.findViewById(R.id.bypassPanel);
        com.google.android.material.textfield.TextInputEditText etBypass =
                v.findViewById(R.id.etBypassKey);
        TextView tvBypassConfirm = v.findViewById(R.id.tvBypassConfirm);

        if (bypassKey != null && !bypassKey.isEmpty()) {
            bypassTrigger.setOnClickListener(trigger -> {
                if (bypassPanel.getVisibility() == View.VISIBLE) {
                    bypassPanel.setVisibility(View.GONE);
                } else {
                    bypassPanel.setVisibility(View.VISIBLE);
                }
            });

            final String finalKey = bypassKey;
            tvBypassConfirm.setOnClickListener(btn -> {
                String entered = "";
                if (etBypass.getText() != null) {
                    entered = etBypass.getText().toString().trim();
                }
                if (entered.equals(finalKey)) {
                    dialog.dismiss();
                    activeDialog = null;
                } else {
                    etBypass.setError(activity.getString(R.string.version_update_bypass_error));
                }
            });
        } else {
            bypassTrigger.setClickable(false);
            bypassPanel.setVisibility(View.GONE);
        }

        dialog.setContentView(v);
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(
                    new android.graphics.drawable.ColorDrawable(
                            android.graphics.Color.TRANSPARENT));
            dialog.getWindow().setLayout(
                    android.view.WindowManager.LayoutParams.MATCH_PARENT,
                    android.view.WindowManager.LayoutParams.WRAP_CONTENT);
        }

        activeDialog = dialog;
        dialog.show();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(
                    android.view.WindowManager.LayoutParams.MATCH_PARENT,
                    android.view.WindowManager.LayoutParams.WRAP_CONTENT);
        }
    }

    private void dismissDialog() {
        if (activeDialog != null) {
            if (activeDialog.isShowing()) activeDialog.dismiss();
            activeDialog = null;
        }
    }

    private String getCurrentVersion(Context context) {
        try {
            String v = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0).versionName;
            if (v != null) {
                return v;
            }
            return "0";
        } catch (Exception e) {
            return "0";
        }
    }
}