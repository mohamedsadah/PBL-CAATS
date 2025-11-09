package com.example.caats.ui.common;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.signature.ObjectKey;
import com.example.caats.MyAppGlideModule;
import com.example.caats.GlideApp;
import com.example.caats.R;
import com.example.caats.databinding.FragmentProfileCommonBinding;
import com.example.caats.auth.LoginActivity;
import com.example.caats.network.AuthService;
import com.example.caats.network.SupabaseClient;
import com.example.caats.utils.PreferenceManager;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.JsonObject;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.LazyHeaders;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileCommonFragment extends Fragment {

    private static final int READ_MEDIA_REQUEST_CODE = 1002;
    private FragmentProfileCommonBinding binding;
    private ActivityResultLauncher<String> galleryLauncher;
    private ActivityResultLauncher<String> requestPermissionLauncher;
    private JsonObject pendingSession;
    private int pendingPosition = -1;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        galleryLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        binding.profileImageView.setImageURI(uri);
                        uploadProfilePicture(uri);
                    }
                });

        requestPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            if (isGranted) {
                galleryLauncher.launch("image/*");
            } else {
                Toast.makeText(getContext(), "Permission denied. Cannot pick image.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentProfileCommonBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupClickListeners();

        String savedImageUrl = PreferenceManager.getImageUrl(requireContext());
        Log.d("ProfileCommonFragment", "Initial Image URL from Prefs: " + savedImageUrl);

        if(savedImageUrl != null) {
            if (savedImageUrl.startsWith("\"") && savedImageUrl.endsWith("\"")){
            savedImageUrl = savedImageUrl.substring(1, savedImageUrl.length() - 1);
           }
            displayProfileImage(savedImageUrl, null);
        }
    }

    private void setupClickListeners() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            binding.changePictureButton.setOnClickListener(v -> checkPermissionAndLaunchGallery());
        }
        binding.changePasswordButton.setOnClickListener(v -> showChangePasswordDialog());
        binding.logoutButton.setOnClickListener(v -> handleLogout());
        binding.goBack.setOnClickListener(v -> requireActivity().finish());
    }

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    private void checkPermissionAndLaunchGallery() {
        String permission = Manifest.permission.READ_MEDIA_IMAGES;
        if (ContextCompat.checkSelfPermission(requireContext(), permission) == PackageManager.PERMISSION_GRANTED) {
            galleryLauncher.launch("image/*");
        } else {
            requestPermissionLauncher.launch(permission);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == READ_MEDIA_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                galleryLauncher.launch("image/*");
            } else {
                Toast.makeText(getContext(), "Permission denied. Cannot pick image.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void showChangePasswordDialog() {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialogue_change_password, null);
        TextInputEditText newPasswordField = dialogView.findViewById(R.id.newPasswordField);
        TextInputEditText confirmPasswordField = dialogView.findViewById(R.id.confirmPasswordField);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Change Password")
                .setView(dialogView)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Update", (dialog, which) -> {
                    String newPassword = Objects.requireNonNull(newPasswordField.getText()).toString();
                    String confirmPassword = Objects.requireNonNull(confirmPasswordField.getText()).toString();

                    if (newPassword.isEmpty() || newPassword.length() < 6) {
                        Toast.makeText(getContext(), "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (!newPassword.equals(confirmPassword)) {
                        Toast.makeText(getContext(), "Passwords do not match", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    updateSupabasePassword(newPassword);
                })
                .show();
    }

    private void handleLogout() {
        PreferenceManager.clearAll(requireContext());

        Intent intent = new Intent(getActivity(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        requireActivity().finish();
    }

    private void updateSupabasePassword(String newPassword) {
        AuthService authService = SupabaseClient.getAuthClient().create(AuthService.class);
        String token = PreferenceManager.getToken(requireContext());
        if (token == null || token.isEmpty()) {
            Toast.makeText(getContext(), "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        JsonObject body = new JsonObject();
        body.addProperty("password", newPassword);

        authService.updateUser("Bearer " + token, body).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
                requireActivity().runOnUiThread(() -> {
                    if (response.isSuccessful()) {
                        Toast.makeText(getContext(), "Password updated successfully!", Toast.LENGTH_SHORT).show();
                    } else {
                        String errorMessage = "Error updating password.";
                        try {
                            if (response.errorBody() != null) {
                                errorMessage += " " + response.errorBody().string();
                            }
                        } catch (IOException e) { /* Ignore */ }
                        Log.e("ProfileCommonFragment", "Password Update Error: " + errorMessage);
                        Toast.makeText(getContext(), "Error updating password", Toast.LENGTH_LONG).show();
                    }
                });
            }

            @Override
            public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
                requireActivity().runOnUiThread(() -> {
                    Log.e("ProfileCommonFragment", "Password Update Failure", t);
                    Toast.makeText(getContext(), "Network Error: " + t.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void uploadProfilePicture(Uri imageUri) {
        OkHttpClient okHttpClient = SupabaseClient.getOkHttpClientInstance(requireContext());
        String userId = PreferenceManager.getUserId(requireContext());
        String token = PreferenceManager.getToken(requireContext());

        if (userId == null || userId.isEmpty() || token == null || token.isEmpty()) {
            Toast.makeText(getContext(), "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        String bucketName = "avatars";
        String folderPath = "public";
        String fileName = userId + ".jpg";
        String filePathInBucket = folderPath + "/" + fileName;
        String uploadUrl = SupabaseClient.SUPABASE_URL + "storage/v1/object/" + bucketName + "/" + filePathInBucket;

        try (InputStream inputStream = requireContext().getContentResolver().openInputStream(imageUri)) {
            if (inputStream == null) {
                Toast.makeText(getContext(), "Error opening image file", Toast.LENGTH_SHORT).show();
                return;
            }

            byte[] fileBytes = readBytes(inputStream);
            RequestBody requestBody = RequestBody.create(fileBytes, MediaType.parse("image/jpeg"));

            Request request = new Request.Builder()
                    .url(uploadUrl)
                    .addHeader("Authorization", "Bearer " + token)
                    .addHeader("apikey", SupabaseClient.API_KEY)
                    .put(requestBody)
                    .addHeader("X-Upsert", "true")
                    .build();

            okHttpClient.newCall(request).enqueue(new okhttp3.Callback() {
                @Override
                public void onFailure(@NonNull okhttp3.Call call, @NonNull IOException e) {
                    requireActivity().runOnUiThread(() -> {
                        Log.e("ProfileCommonFragment", "Upload Failure", e);
                        Toast.makeText(getContext(), "Upload failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
                }

                @Override
                public void onResponse(@NonNull okhttp3.Call call, @NonNull okhttp3.Response response) throws IOException {
                    final String responseBodyString = response.body().string();
                    if (response.isSuccessful()) {
                        requireActivity().runOnUiThread(() -> {
                            Toast.makeText(getContext(), "Picture updated successfully!", Toast.LENGTH_SHORT).show();
                            String publicUrl = SupabaseClient.SUPABASE_URL + "storage/v1/object/"
                                    + bucketName + "/" + filePathInBucket;
                            Log.d("ProfileCommonFragment", "Constructed Public URL: " + publicUrl);
                            saveProfileImageUrl(publicUrl);
                        });
                    } else {
                        final String errorMsg = "Upload failed: " + response.code() + " " + response.message() + " | Body: " + responseBodyString;
                        requireActivity().runOnUiThread(() -> {
                            Log.e("ProfileCommonFragment", "Upload Error: " + errorMsg);
                            Toast.makeText(getContext(), "Upload failed: " + response.code(), Toast.LENGTH_LONG).show();
                        });
                    }
                    response.close();
                }
            });

        } catch (IOException e) {
            Log.e("ProfileCommonFragment", "File Read Error", e);
            Toast.makeText(getContext(), "Error reading image file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
        /* Ignore */
    }

    private void saveProfileImageUrl(String url) {
        AuthService restService = SupabaseClient.getRestClient(requireContext()).create(AuthService.class);
        String authUid = PreferenceManager.getUserId(requireContext());

        if (authUid == null || authUid.isEmpty()) {
            Log.e("ProfileCommonFragment", "Cannot save profile URL: Auth UID not found.");
            return;
        }

        JsonObject body = new JsonObject();
        body.addProperty("image_url", url);

        String authUidFilter = "eq." + authUid;

        restService.updateProfile(authUidFilter, body).enqueue(new Callback<List<JsonObject>>() {
            @Override
            public void onResponse(@NonNull Call<List<JsonObject>> call, @NonNull Response<List<JsonObject>> response) {
                requireActivity().runOnUiThread(() -> {
                    if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                        JsonObject updatedProfile = response.body().get(0);
                        String savedUrl = null;
                        String updatedAt = null;

                        if (updatedProfile.has("image_url") && !updatedProfile.get("image_url").isJsonNull()) {
                            savedUrl = updatedProfile.get("image_url").getAsString();
                            if (savedUrl != null && savedUrl.length() > 1 && savedUrl.startsWith("\"") && savedUrl.endsWith("\"")) {
                                savedUrl = savedUrl.substring(1, savedUrl.length() - 1);
                            }
                        }

                        if (updatedProfile.has("updated_at") && !updatedProfile.get("updated_at").isJsonNull()) {
                            updatedAt = updatedProfile.get("updated_at").getAsString();
                        }

                        Log.d("ProfileCommonFragment", "Profile avatar URL updated. URL: " + savedUrl + ", UpdatedAt: " + updatedAt);
                        PreferenceManager.saveImageUrl(requireContext(), savedUrl); // Save the updated URL to Prefs
                        displayProfileImage(savedUrl, updatedAt);

                    } else {
                        String errorMessage = "Failed to save profile URL.";
                        try {
                            if (response.errorBody() != null) {
                                errorMessage += " " + response.errorBody().string();
                            }
                        } catch (IOException e) { /* Ignore */ }
                        Log.e("ProfileCommonFragment", "Save URL Error: " + errorMessage);
                        Toast.makeText(getContext(), "Failed to save profile picture link", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onFailure(@NonNull Call<List<JsonObject>> call, @NonNull Throwable t) {
                requireActivity().runOnUiThread(() -> {
                    Log.e("ProfileCommonFragment", "Save URL Network Failure", t);
                    Toast.makeText(getContext(), "Network Error saving profile link", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void displayProfileImage(String imageUrl, String updatedAtSignature) {
        if (imageUrl == null || imageUrl.isEmpty() || getContext() == null || binding == null) {
            if (binding != null) {
                binding.profileImageView.setImageResource(R.drawable.ic_profile_placeholder);
            }
            return;
        }

        String token = PreferenceManager.getToken(requireContext());
        if (token == null || token.isEmpty()) {
            if (binding != null) {
                binding.profileImageView.setImageResource(R.drawable.ic_profile_placeholder);
            }
            return;
        }

        GlideUrl glideUrl = new GlideUrl(imageUrl, new LazyHeaders.Builder()
                .addHeader("Authorization", "Bearer " + token)
                .addHeader("apikey", SupabaseClient.API_KEY)
                .build());

        String signatureKey = (updatedAtSignature != null && !updatedAtSignature.isEmpty())
                ? updatedAtSignature
                : String.valueOf(System.currentTimeMillis());

        GlideApp.with(this)
                .load(glideUrl)
                .signature(new ObjectKey(signatureKey))
                .placeholder(R.drawable.ic_profile_placeholder)
                .error(R.drawable.ic_profile_placeholder)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(true)
                .into(binding.profileImageView);
    }

    private byte[] readBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
        int bufferSize = 4096;
        byte[] buffer = new byte[bufferSize];
        int len;
        while ((len = inputStream.read(buffer)) != -1) {
            byteBuffer.write(buffer, 0, len);
        }
        return byteBuffer.toByteArray();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}