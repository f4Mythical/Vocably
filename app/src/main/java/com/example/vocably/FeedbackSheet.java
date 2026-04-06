package com.example.vocably;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FeedbackSheet extends BottomSheetDialogFragment {

    private FrameLayout container;
    private String currentCategory = null;

    private final List<Uri> selectedPhotos = new ArrayList<>();
    private LinearLayout photosPreviewContainer;

    private ActivityResultLauncher<Intent> photoPickerLauncher;
    private ActivityResultLauncher<String> permissionLauncher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(STYLE_NORMAL, R.style.BottomSheetStyle);

        photoPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        selectedPhotos.clear();
                        if (result.getData().getClipData() != null) {
                            int count = result.getData().getClipData().getItemCount();
                            for (int i = 0; i < count; i++) {
                                selectedPhotos.add(result.getData().getClipData().getItemAt(i).getUri());
                            }
                        } else if (result.getData().getData() != null) {
                            selectedPhotos.add(result.getData().getData());
                        }
                        updatePhotoPreviews();
                    }
                }
        );

        permissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                granted -> {
                    if (granted) {
                        openPhotoPicker();
                    } else {
                        Toast.makeText(getContext(), "Brak uprawnień do zdjęć", Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup parent, @Nullable Bundle state) {
        return inflater.inflate(R.layout.fragment_feedback_sheet, parent, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        container = view.findViewById(R.id.feedbackContainer);
        showCategories();
    }

    private void requestPhotoPermissionOrOpen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            String permission = android.Manifest.permission.READ_MEDIA_IMAGES;
            if (ContextCompat.checkSelfPermission(requireContext(), permission) == PackageManager.PERMISSION_GRANTED) {
                openPhotoPicker();
            } else {
                permissionLauncher.launch(permission);
            }
        } else {
            String permission = android.Manifest.permission.READ_EXTERNAL_STORAGE;
            if (ContextCompat.checkSelfPermission(requireContext(), permission) == PackageManager.PERMISSION_GRANTED) {
                openPhotoPicker();
            } else {
                permissionLauncher.launch(permission);
            }
        }
    }

    private void openPhotoPicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        photoPickerLauncher.launch(Intent.createChooser(intent, "Wybierz zdjęcia"));
    }

    private void showCategories() {
        currentCategory = null;
        container.removeAllViews();
        View v = LayoutInflater.from(getContext()).inflate(R.layout.view_feedback_categories, container, false);

        v.findViewById(R.id.catWordError).setOnClickListener(x -> showWordError());
        v.findViewById(R.id.catSuggestion).setOnClickListener(x -> showSuggestion());
        v.findViewById(R.id.catNewBook).setOnClickListener(x -> showNewBook());
        v.findViewById(R.id.catTechProblem).setOnClickListener(x -> showTechProblem());
        v.findViewById(R.id.catRate).setOnClickListener(x -> showRate());

        container.addView(v);
    }

    private void showWordError() {
        currentCategory = "word_error";
        container.removeAllViews();
        View v = LayoutInflater.from(getContext()).inflate(R.layout.view_feedback_word_error, container, false);

        TextInputEditText etBook       = v.findViewById(R.id.etBook);
        TextInputEditText etClass      = v.findViewById(R.id.etClass);
        TextInputEditText etChapter    = v.findViewById(R.id.etChapter);
        TextInputEditText etSubChapter = v.findViewById(R.id.etSubChapter);
        TextInputEditText etContent    = v.findViewById(R.id.etContent);
        TextView btnSend               = v.findViewById(R.id.btnSend);

        btnSend.setOnClickListener(x -> {
            String content = getText(etContent);
            if (content.isEmpty()) { etContent.setError("Wypełnij treść"); return; }

            Map<String, Object> data = new HashMap<>();
            data.put("category",    "word_error");
            data.put("book",        getText(etBook));
            data.put("class",       getText(etClass));
            data.put("chapter",     getText(etChapter));
            data.put("subChapter",  getText(etSubChapter));
            data.put("content",     content);
            sendFeedback(data);
        });

        container.addView(v);
    }

    private void showSuggestion() {
        currentCategory = "suggestion";
        container.removeAllViews();
        View v = LayoutInflater.from(getContext()).inflate(R.layout.view_feedback_suggestion, container, false);

        TextInputEditText etContent = v.findViewById(R.id.etContent);
        TextView btnSend            = v.findViewById(R.id.btnSend);

        btnSend.setOnClickListener(x -> {
            String content = getText(etContent);
            if (content.isEmpty()) { etContent.setError("Wypełnij treść"); return; }

            Map<String, Object> data = new HashMap<>();
            data.put("category", "suggestion");
            data.put("content",  content);
            sendFeedback(data);
        });

        container.addView(v);
    }

    private void showNewBook() {
        currentCategory = "new_book";
        selectedPhotos.clear();
        container.removeAllViews();
        View v = LayoutInflater.from(getContext()).inflate(R.layout.view_feedback_new_book, container, false);

        TextInputEditText etLanguage = v.findViewById(R.id.etLanguage);
        TextInputEditText etBook     = v.findViewById(R.id.etBook);
        TextInputEditText etClass    = v.findViewById(R.id.etClass);
        TextInputEditText etContent  = v.findViewById(R.id.etContent);
        TextView btnAddPhotos        = v.findViewById(R.id.btnAddPhotos);
        photosPreviewContainer       = v.findViewById(R.id.photosPreviewContainer);
        TextView btnSend             = v.findViewById(R.id.btnSend);

        btnAddPhotos.setOnClickListener(x -> requestPhotoPermissionOrOpen());

        btnSend.setOnClickListener(x -> {
            Map<String, Object> data = new HashMap<>();
            data.put("category",    "new_book");
            data.put("language",    getText(etLanguage));
            data.put("book",        getText(etBook));
            data.put("class",       getText(etClass));
            data.put("content",     getText(etContent));
            data.put("photoCount",  selectedPhotos.size());

            List<String> photoUriStrings = new ArrayList<>();
            for (Uri uri : selectedPhotos) photoUriStrings.add(uri.toString());
            data.put("photoUris", photoUriStrings);

            sendFeedback(data);
        });

        container.addView(v);
    }

    private void updatePhotoPreviews() {
        if (photosPreviewContainer == null) return;
        photosPreviewContainer.removeAllViews();
        int sizeDp = 64;
        int sizePx = Math.round(sizeDp * getResources().getDisplayMetrics().density);
        int marginPx = Math.round(6 * getResources().getDisplayMetrics().density);

        for (Uri uri : selectedPhotos) {
            ImageView img = new ImageView(getContext());
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(sizePx, sizePx);
            params.setMargins(0, 0, marginPx, 0);
            img.setLayoutParams(params);
            img.setScaleType(ImageView.ScaleType.CENTER_CROP);
            img.setImageURI(uri);
            photosPreviewContainer.addView(img);
        }
    }

    private void showTechProblem() {
        currentCategory = "tech_problem";
        container.removeAllViews();
        View v = LayoutInflater.from(getContext()).inflate(R.layout.view_feedback_suggestion, container, false);

        TextInputEditText etContent = v.findViewById(R.id.etContent);
        TextView btnSend            = v.findViewById(R.id.btnSend);

        btnSend.setOnClickListener(x -> {
            String content = getText(etContent);
            if (content.isEmpty()) { etContent.setError("Wypełnij treść"); return; }

            Map<String, Object> data = new HashMap<>();
            data.put("category", "tech_problem");
            data.put("content",  content);
            sendFeedback(data);
        });

        container.addView(v);
    }

    private void showRate() {
        currentCategory = "rate";
        container.removeAllViews();
        View v = LayoutInflater.from(getContext()).inflate(R.layout.view_feedback_rate, container, false);

        int[] starIds = {R.id.star1, R.id.star2, R.id.star3, R.id.star4, R.id.star5};
        TextView[] stars = new TextView[5];
        for (int i = 0; i < 5; i++) stars[i] = v.findViewById(starIds[i]);

        final int[] rating = {0};

        for (int i = 0; i < 5; i++) {
            final int idx = i + 1;
            stars[i].setOnClickListener(x -> {
                rating[0] = idx;
                for (int j = 0; j < 5; j++) {
                    stars[j].setText(j < idx ? "★" : "☆");
                }
            });
        }

        TextInputEditText etContent = v.findViewById(R.id.etContent);
        TextView btnSend            = v.findViewById(R.id.btnSend);

        btnSend.setOnClickListener(x -> {
            if (rating[0] == 0) {
                Toast.makeText(getContext(), "Wybierz ocenę", Toast.LENGTH_SHORT).show();
                return;
            }
            Map<String, Object> data = new HashMap<>();
            data.put("category", "rate");
            data.put("rating",   rating[0]);
            data.put("content",  getText(etContent));
            sendFeedback(data);
        });

        container.addView(v);
    }

    private void sendFeedback(Map<String, Object> data) {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        data.put("uid",       uid);
        data.put("createdAt", Timestamp.now());

        FirebaseFirestore.getInstance()
                .collection("feedback")
                .add(data)
                .addOnSuccessListener(ref -> {
                    Toast.makeText(getContext(), "Dziękujemy za feedback!", Toast.LENGTH_SHORT).show();
                    dismiss();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Błąd wysyłania, spróbuj ponownie", Toast.LENGTH_SHORT).show());
    }

    private String getText(TextInputEditText et) {
        return et.getText() != null ? et.getText().toString().trim() : "";
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null) {
            com.google.android.material.bottomsheet.BottomSheetBehavior<FrameLayout> behavior =
                    com.google.android.material.bottomsheet.BottomSheetBehavior.from(
                            (FrameLayout) getDialog().findViewById(com.google.android.material.R.id.design_bottom_sheet));
            behavior.setState(com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED);
        }
    }
}