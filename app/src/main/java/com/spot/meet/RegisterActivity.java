package com.spot.meet;

import android.content.Intent;
import android.os.Bundle;
import android.text.method.PasswordTransformationMethod;
import android.util.Patterns;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class RegisterActivity extends AppCompatActivity {

    private EditText etUsername, etEmail, etPassword, etConfirmPassword;
    private Button btnRegister;
    private TextView btnTogglePassword, btnToggleConfirmPassword, btnGoLogin, btnBack;
    private boolean passwordVisible = false;
    private boolean confirmPasswordVisible = false;
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Apply Window Insets (Keyboard/IME aware)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.activity_register), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime());
            View content = findViewById(R.id.register_scroll_content);
            if (content != null) {
                content.setPadding(content.getPaddingLeft(), content.getPaddingTop(), 
                                 content.getPaddingRight(), Math.max(28, imeInsets.bottom));
            }
            return insets;
        });

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
                    ScrollView scrollView = findViewById(R.id.activity_register);
                    int[] location = new int[2];
                    v.getLocationOnScreen(location);
                    int[] scrollLocation = new int[2];
                    scrollView.getLocationOnScreen(scrollLocation);

                    int y = location[1] - scrollLocation[1] + scrollView.getScrollY();
                    scrollView.smoothScrollTo(0, y - 100);
                }, 200);
            }
        };
        etUsername.setOnFocusChangeListener(scrollFocusListener);
        etEmail.setOnFocusChangeListener(scrollFocusListener);
        etPassword.setOnFocusChangeListener(scrollFocusListener);
        etConfirmPassword.setOnFocusChangeListener(scrollFocusListener);

        // Handle keyboard "Next" button flow to ensure every field is lifted properly
        etUsername.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_NEXT) {
                etEmail.requestFocus();
                return true;
            }
            return false;
        });
        etEmail.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_NEXT) {
                etPassword.requestFocus();
                return true;
            }
            return false;
        });
        etPassword.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_NEXT) {
                etConfirmPassword.requestFocus();
                return true;
            }
            return false;
        });
    }

    private void initViews() {
        etUsername = findViewById(R.id.et_reg_username);
        etEmail = findViewById(R.id.et_reg_email);
        etPassword = findViewById(R.id.et_reg_password);
        etConfirmPassword = findViewById(R.id.et_reg_confirm_password);
        btnRegister = findViewById(R.id.btn_register);
        btnTogglePassword = findViewById(R.id.btn_toggle_reg_password);
        btnToggleConfirmPassword = findViewById(R.id.btn_toggle_reg_confirm_password);
        btnGoLogin = findViewById(R.id.btn_go_login);
        btnBack = findViewById(R.id.btn_back_register);
    }

    private void startEntranceAnimations() {
        LinearLayout header = findViewById(R.id.register_header);
        LinearLayout card = findViewById(R.id.register_card);

        header.setAlpha(0f);
        card.setAlpha(0f);
        card.setTranslationY(40f);

        header.animate().alpha(1f).setDuration(600).setStartDelay(100).start();
        card.animate().alpha(1f).translationY(0f).setDuration(600).setStartDelay(250).start();
    }

    private void setListeners() {
        btnBack.setOnClickListener(v -> {
            onBackPressed();
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });

        btnTogglePassword.setOnClickListener(v -> {
            passwordVisible = !passwordVisible;
            updatePasswordVisibility(etPassword, btnTogglePassword, passwordVisible);
        });

        btnToggleConfirmPassword.setOnClickListener(v -> {
            confirmPasswordVisible = !confirmPasswordVisible;
            updatePasswordVisibility(etConfirmPassword, btnToggleConfirmPassword, confirmPasswordVisible);
        });

        btnGoLogin.setOnClickListener(v -> {
            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        });

        btnRegister.setOnClickListener(v -> attemptRegister());
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

    private void attemptRegister() {
        String username = etUsername.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString();
        String confirmPass = etConfirmPassword.getText().toString();

        // Validation
        if (username.isEmpty()) {
            shakeView(etUsername);
            etUsername.setError("Username is required");
            return;
        }
        if (username.length() < 3) {
            shakeView(etUsername);
            etUsername.setError("Username must be at least 3 characters");
            return;
        }
        if (email.isEmpty()) {
            shakeView(etEmail);
            etEmail.setError("Email is required");
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            shakeView(etEmail);
            etEmail.setError("Enter a valid email address");
            return;
        }
        if (password.isEmpty()) {
            shakeView(etPassword);
            etPassword.setError("Password is required");
            return;
        }
        if (password.length() < 6) {
            shakeView(etPassword);
            etPassword.setError("Password must be at least 6 characters");
            return;
        }
        if (!password.equals(confirmPass)) {
            shakeView(etConfirmPassword);
            etConfirmPassword.setError("Passwords do not match");
            return;
        }

        btnRegister.setEnabled(false);
        btnRegister.setText("Checking availability…");

        dbHelper.checkAvailability(username, email, (usernameTaken, emailTaken) -> {
            if (usernameTaken) {
                shakeView(etUsername);
                etUsername.setError("Username already taken");
                btnRegister.setEnabled(true);
                btnRegister.setText("CREATE ACCOUNT");
                return;
            }
            if (emailTaken) {
                shakeView(etEmail);
                etEmail.setError("Email already registered");
                btnRegister.setEnabled(true);
                btnRegister.setText("CREATE ACCOUNT");
                return;
            }

            // All checks passed — send verification email
            btnRegister.setText("Sending verification…");

            EmailSender.sendVerificationCode(email, username, new EmailSender.EmailCallback() {
                @Override
                public void onSuccess(String code) {
                    Intent intent = new Intent(RegisterActivity.this, VerifyCodeActivity.class);
                    intent.putExtra("type", "register");
                    intent.putExtra("code", code);
                    intent.putExtra("email", email);
                    intent.putExtra("username", username);
                    intent.putExtra("password", password);
                    startActivity(intent);
                    overridePendingTransition(R.anim.slide_up, android.R.anim.fade_out);
                    
                    // Reset button state gracefully so it's ready if the user navigates back
                    runOnUiThread(() -> {
                        btnRegister.setEnabled(true);
                        btnRegister.setText("CREATE ACCOUNT");
                    });
                }

                @Override
                public void onFailure(String error) {
                    btnRegister.setEnabled(true);
                    btnRegister.setText("CREATE ACCOUNT");
                    Toast.makeText(RegisterActivity.this,
                            "Failed to send verification email. Please try again.",
                            Toast.LENGTH_LONG).show();
                }
            });
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
