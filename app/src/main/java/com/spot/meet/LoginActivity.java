package com.spot.meet;

import android.content.Intent;
import android.os.Bundle;
import android.text.method.PasswordTransformationMethod;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ScrollView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class LoginActivity extends AppCompatActivity {

    private EditText etIdentifier, etPassword;
    private TextView btnTogglePassword, btnForgot, btnGoRegister, btnLogin;
    private boolean passwordVisible = false;
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Apply Window Insets (Keyboard/IME aware)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.activity_login), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime());
            View content = findViewById(R.id.login_scroll_content);
            if (content != null) {
                content.setPadding(content.getPaddingLeft(), content.getPaddingTop(), 
                                 content.getPaddingRight(), Math.max(32, imeInsets.bottom));
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
                    ScrollView scrollView = findViewById(R.id.activity_login);
                    int[] location = new int[2];
                    v.getLocationOnScreen(location);
                    int[] scrollLocation = new int[2];
                    scrollView.getLocationOnScreen(scrollLocation);

                    int y = location[1] - scrollLocation[1] + scrollView.getScrollY();
                    scrollView.smoothScrollTo(0, y - 100);
                }, 200);
            }
        };
        etIdentifier.setOnFocusChangeListener(scrollFocusListener);
        etPassword.setOnFocusChangeListener(scrollFocusListener);

        // Handle keyboard "Next" button flow to ensure password field is lifted
        etIdentifier.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_NEXT) {
                etPassword.requestFocus();
                return true;
            }
            return false;
        });
    }

    private void initViews() {
        etIdentifier = findViewById(R.id.et_login_identifier);
        etPassword = findViewById(R.id.et_login_password);
        btnTogglePassword = findViewById(R.id.btn_toggle_password);
        btnForgot = findViewById(R.id.btn_forgot_password);
        btnGoRegister = findViewById(R.id.btn_go_register);
        btnLogin = findViewById(R.id.btn_login);
    }

    private void startEntranceAnimations() {
        Animation slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up);

        LinearLayout header = findViewById(R.id.login_header);
        LinearLayout card = findViewById(R.id.login_card);

        header.setAlpha(0f);
        card.setAlpha(0f);

        header.animate().alpha(1f).setDuration(600).setStartDelay(100).start();
        card.animate().alpha(1f).translationY(0f).setDuration(600).setStartDelay(250).start();
        card.setTranslationY(40f);
    }

    private void setListeners() {

        // Toggle password visibility
        btnTogglePassword.setOnClickListener(v -> {
            passwordVisible = !passwordVisible;
            updatePasswordVisibility();
        });

        btnForgot.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, ForgotPasswordActivity.class));
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });

        btnGoRegister.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        });

        btnLogin.setOnClickListener(v -> attemptLogin());
    }

    private void updatePasswordVisibility() {
        if (passwordVisible) {
            etPassword.setTransformationMethod(null);
            btnTogglePassword.setText("🙈");
        } else {
            etPassword.setTransformationMethod(new PasswordTransformationMethod());
            btnTogglePassword.setText("👁️");
        }
        etPassword.setSelection(etPassword.getText().length());
    }

    private void attemptLogin() {
        String identifier = etIdentifier.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (identifier.isEmpty()) {
            shakeView(etIdentifier);
            etIdentifier.setError("Username or Email is required");
            return;
        }
        if (password.isEmpty()) {
            shakeView(etPassword);
            etPassword.setError("Password is required");
            return;
        }

        btnLogin.setEnabled(false);
        btnLogin.setText("Checking credentials…");

        dbHelper.loginUser(identifier, password, new DatabaseHelper.LoginCallback() {
            @Override
            public void onSuccess(String email, String username, boolean isAdmin) {
                // Success animation
                findViewById(R.id.login_card).animate().alpha(0f).scaleX(0.95f).scaleY(0.95f).setDuration(300)
                        .withEndAction(() -> {
                            // Session management
                            android.content.SharedPreferences prefs = getSharedPreferences("spotmeet_prefs", MODE_PRIVATE);
                            prefs.edit()
                                    .putBoolean("is_logged_in", true)
                                    .putString("user_email", email)
                                    .putString("user_username", username)
                                    .putBoolean("is_admin", isAdmin)
                                    .apply();

                            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            overridePendingTransition(R.anim.fade_in, android.R.anim.fade_out);
                            finish();
                        }).start();
            }

            @Override
            public void onAccountNotFound() {
                resetLoginButton();
                shakeView(findViewById(R.id.login_card));
                Toast.makeText(LoginActivity.this, "No account found with these details.", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onWrongPassword() {
                resetLoginButton();
                shakeView(findViewById(R.id.login_card));
                Toast.makeText(LoginActivity.this, "Incorrect password. Please try again.", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure() {
                resetLoginButton();
                Toast.makeText(LoginActivity.this, "Connection error. Please try again later.", Toast.LENGTH_SHORT).show();
            }

            private void resetLoginButton() {
                btnLogin.setEnabled(true);
                btnLogin.setText("SIGN IN");
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
