package com.spot.meet;

import android.content.Intent;
import android.os.Bundle;
import android.text.method.PasswordTransformationMethod;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ScrollView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class ResetPasswordActivity extends AppCompatActivity {

    private EditText etNewPassword, etConfirmNewPassword;
    private TextView btnToggleNewPass, btnToggleConfirmNewPass;
    private Button btnReset;
    private LinearLayout resetCard;

    private boolean passwordVisible = false;
    private boolean confirmPasswordVisible = false;
    private DatabaseHelper dbHelper;
    private String email;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password);

        // Apply Window Insets (Keyboard/IME aware)
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.activity_reset_password_root), (v, insets) -> {
            androidx.core.graphics.Insets systemBars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars());
            androidx.core.graphics.Insets imeInsets = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.ime());
            View content = findViewById(R.id.reset_scroll_content);
            if (content != null) {
                content.setPadding(content.getPaddingLeft(), content.getPaddingTop(), 
                                 content.getPaddingRight(), Math.max(40, imeInsets.bottom));
            }
            return insets;
        });

        email = getIntent().getStringExtra("email");
        dbHelper = new DatabaseHelper(this);

        initViews();
        startEntranceAnimations();
        setListeners();
        setupFocusEnhancements();
    }

    private void setupFocusEnhancements() {
        View.OnFocusChangeListener scrollFocusListener = (v, hasFocus) -> {
            if (hasFocus) {
                v.postDelayed(() -> {
                    ScrollView scrollView = findViewById(R.id.activity_reset_password_root);
                    int[] location = new int[2];
                    v.getLocationOnScreen(location);
                    int[] scrollLocation = new int[2];
                    scrollView.getLocationOnScreen(scrollLocation);

                    int y = location[1] - scrollLocation[1] + scrollView.getScrollY();
                    scrollView.smoothScrollTo(0, y - 100);
                }, 200);
            }
        };
        etNewPassword.setOnFocusChangeListener(scrollFocusListener);
        etConfirmNewPassword.setOnFocusChangeListener(scrollFocusListener);

        // Handle keyboard "Next" button specifically to ensure the next field is lifted
        etNewPassword.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_NEXT) {
                etConfirmNewPassword.requestFocus();
                return true;
            }
            return false;
        });
    }

    private void initViews() {
        etNewPassword = findViewById(R.id.et_new_password);
        etConfirmNewPassword = findViewById(R.id.et_confirm_new_password);
        btnToggleNewPass = findViewById(R.id.btn_toggle_new_pass);
        btnToggleConfirmNewPass = findViewById(R.id.btn_toggle_confirm_new_pass);
        btnReset = findViewById(R.id.btn_reset_password);
        resetCard = findViewById(R.id.reset_card);
    }

    private void startEntranceAnimations() {
        LinearLayout header = findViewById(R.id.reset_header);
        header.setAlpha(0f);
        resetCard.setAlpha(0f);
        resetCard.setTranslationY(40f);

        header.animate().alpha(1f).setDuration(500).setStartDelay(100).start();
        resetCard.animate().alpha(1f).translationY(0f).setDuration(550).setStartDelay(200).start();
    }

    private void setListeners() {
        // Password visibility toggle
        btnToggleNewPass.setOnClickListener(v -> {
            passwordVisible = !passwordVisible;
            updatePasswordVisibility(etNewPassword, btnToggleNewPass, passwordVisible);
        });

        // Confirm password visibility toggle
        btnToggleConfirmNewPass.setOnClickListener(v -> {
            confirmPasswordVisible = !confirmPasswordVisible;
            updatePasswordVisibility(etConfirmNewPassword, btnToggleConfirmNewPass, confirmPasswordVisible);
        });

        // Reset button
        btnReset.setOnClickListener(v -> attemptReset());
    }

    private void updatePasswordVisibility(EditText editText, TextView toggleBtn, boolean isVisible) {
        if (isVisible) {
            editText.setTransformationMethod(null);
            toggleBtn.setText("🙈");
        } else {
            editText.setTransformationMethod(new PasswordTransformationMethod());
            toggleBtn.setText("👁️");
        }
        editText.setSelection(editText.getText().length());
    }

    private void attemptReset() {
        String newPass = etNewPassword.getText().toString();
        String confirmPass = etConfirmNewPassword.getText().toString();

        if (newPass.isEmpty()) {
            shakeView(etNewPassword);
            etNewPassword.setError("Enter new password");
            return;
        }
        if (newPass.length() < 6) {
            shakeView(etNewPassword);
            etNewPassword.setError("Password must be at least 6 characters");
            return;
        }
        if (confirmPass.isEmpty()) {
            shakeView(etConfirmNewPassword);
            etConfirmNewPassword.setError("Confirm your new password");
            return;
        }
        if (!newPass.equals(confirmPass)) {
            shakeView(etConfirmNewPassword);
            etConfirmNewPassword.setError("Passwords do not match");
            return;
        }

        btnReset.setEnabled(false);
        btnReset.setText("Updating…");

        dbHelper.updatePassword(email, newPass, success -> {
            if (success) {
                // Success animation
                resetCard.animate().alpha(0f).scaleX(0.95f).scaleY(0.95f).setDuration(300)
                        .withEndAction(() -> {
                            Toast.makeText(this, "🎉 Password updated! Please sign in.", Toast.LENGTH_LONG).show();
                            Intent intent = new Intent(ResetPasswordActivity.this, LoginActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            overridePendingTransition(R.anim.fade_in, android.R.anim.fade_out);
                            finish();
                        }).start();
            } else {
                btnReset.setEnabled(true);
                btnReset.setText("RESET PASSWORD");
                Toast.makeText(this, "Failed to update password. Please try again.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void shakeView(View view) {
        view.animate()
                .translationX(12f).setDuration(60)
                .withEndAction(() -> view.animate()
                        .translationX(-12f).setDuration(60)
                        .withEndAction(() -> view.animate()
                                .translationX(8f).setDuration(50)
                                .withEndAction(() -> view.animate()
                                        .translationX(0f).setDuration(50).start())
                                .start())
                        .start())
                .start();
    }
}
