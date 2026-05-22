package com.spot.meet;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class VerifyCodeActivity extends AppCompatActivity {

    private EditText[] otpBoxes = new EditText[6];
    private Button btnVerify;
    private TextView btnResend, tvTimer, btnBack;
    private LinearLayout verifyCard;

    private String expectedCode;
    private String email;
    private String type;       // "register" or "reset"
    private String username;   // only for register
    private String password;   // only for register
    private CountDownTimer countDownTimer;
    private boolean canResend = false;
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verify_code);

        // Apply Window Insets (Keyboard/IME aware)
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.activity_verify_code_root), (v, insets) -> {
            androidx.core.graphics.Insets systemBars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars());
            androidx.core.graphics.Insets imeInsets = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.ime());
            View content = findViewById(R.id.verify_scroll_content);
            if (content != null) {
                content.setPadding(content.getPaddingLeft(), content.getPaddingTop(), 
                                 content.getPaddingRight(), Math.max(40, imeInsets.bottom));
            }
            return insets;
        });

        // Get passed data
        email        = getIntent().getStringExtra("email");
        expectedCode = getIntent().getStringExtra("code");
        type         = getIntent().getStringExtra("type");   // "register" or "reset"
        username     = getIntent().getStringExtra("username");
        password     = getIntent().getStringExtra("password");
        dbHelper     = new DatabaseHelper(this);

        initViews();
        startEntranceAnimations();
        setupOtpBoxes();
        startResendTimer(120); // 2 minutes
        setListeners();
        setupFocusEnhancements();
    }

    private void setupFocusEnhancements() {
        View.OnFocusChangeListener scrollFocusListener = (v, hasFocus) -> {
            if (hasFocus) {
                v.postDelayed(() -> {
                    ScrollView scrollView = findViewById(R.id.activity_verify_code_root);
                    int[] location = new int[2];
                    v.getLocationOnScreen(location);
                    int[] scrollLocation = new int[2];
                    scrollView.getLocationOnScreen(scrollLocation);

                    int y = location[1] - scrollLocation[1] + scrollView.getScrollY();
                    scrollView.smoothScrollTo(0, y - 100);
                }, 200);
            }
        };
        for (EditText box : otpBoxes) {
            box.setOnFocusChangeListener(scrollFocusListener);
        }
    }

    private void initViews() {
        otpBoxes[0] = findViewById(R.id.otp1);
        otpBoxes[1] = findViewById(R.id.otp2);
        otpBoxes[2] = findViewById(R.id.otp3);
        otpBoxes[3] = findViewById(R.id.otp4);
        otpBoxes[4] = findViewById(R.id.otp5);
        otpBoxes[5] = findViewById(R.id.otp6);
        btnVerify = findViewById(R.id.btn_verify_code);
        btnResend = findViewById(R.id.btn_resend_code);
        tvTimer = findViewById(R.id.tv_resend_timer);
        btnBack = findViewById(R.id.btn_back_verify);
        verifyCard = findViewById(R.id.verify_card);

        // Update title/subtitle based on flow type
        TextView subtitle = findViewById(R.id.tv_verify_subtitle);
        TextView header   = findViewById(R.id.tv_verify_header_title);
        TextView btnChangeEmail = findViewById(R.id.btn_change_email);

        if ("register".equals(type)) {
            if (header != null) header.setText("Verify Your Email");
            subtitle.setText("Enter the 6-digit activation code\nsent to " + email);
            if (btnChangeEmail != null) btnChangeEmail.setVisibility(View.VISIBLE);
        } else {
            if (header != null) header.setText("Check Your Email");
            if (email != null) subtitle.setText("Enter the 6-digit reset code\nsent to " + email);
            if (btnChangeEmail != null) btnChangeEmail.setVisibility(View.GONE);
        }

        if (btnChangeEmail != null) {
            btnChangeEmail.setOnClickListener(v -> {
                onBackPressed();
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            });
        }
    }

    private void startEntranceAnimations() {
        LinearLayout header = findViewById(R.id.verify_header);
        header.setAlpha(0f);
        verifyCard.setAlpha(0f);
        verifyCard.setTranslationY(40f);

        header.animate().alpha(1f).setDuration(500).setStartDelay(100).start();
        verifyCard.animate().alpha(1f).translationY(0f).setDuration(550).setStartDelay(220).start();
    }

    /**
     * Set up auto-focus jump between OTP boxes.
     */
    private void setupOtpBoxes() {
        for (int i = 0; i < otpBoxes.length; i++) {
            final int index = i;
            otpBoxes[i].addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
                @Override public void onTextChanged(CharSequence s, int st, int b, int c) {}

                @Override
                public void afterTextChanged(Editable s) {
                    if (s.length() == 1) {
                        // Jump to next box
                        if (index < otpBoxes.length - 1) {
                            otpBoxes[index + 1].requestFocus();
                        } else {
                            // Last box — optionally auto-verify
                            otpBoxes[index].clearFocus();
                        }
                    } else if (s.length() == 0) {
                        // Jump back to previous box
                        if (index > 0) {
                            otpBoxes[index - 1].requestFocus();
                        }
                    }
                }
            });
        }
    }

    private void setListeners() {
        btnBack.setOnClickListener(v -> {
            onBackPressed();
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });

        btnVerify.setOnClickListener(v -> verifyCode());

        btnResend.setOnClickListener(v -> {
            if (!canResend) {
                Toast.makeText(this, "Please wait for the timer.", Toast.LENGTH_SHORT).show();
                return;
            }
            resendCode();
        });
    }

    private void verifyCode() {
        StringBuilder entered = new StringBuilder();
        for (EditText box : otpBoxes) {
            String digit = box.getText().toString().trim();
            if (digit.isEmpty()) {
                shakeAllBoxes();
                Toast.makeText(this, "Please enter all 6 digits.", Toast.LENGTH_SHORT).show();
                return;
            }
            entered.append(digit);
        }

        String enteredCode = entered.toString();

        if (enteredCode.equals(expectedCode)) {
            // Success animation — then route based on type
            verifyCard.animate()
                    .scaleX(1.03f).scaleY(1.03f).setDuration(150)
                    .withEndAction(() -> verifyCard.animate()
                            .scaleX(1f).scaleY(1f).setDuration(150)
                            .withEndAction(() -> {
                                if (countDownTimer != null) countDownTimer.cancel();
                                if ("register".equals(type)) {
                                    saveUserAndGoMain();
                                } else {
                                    Intent intent = new Intent(VerifyCodeActivity.this, ResetPasswordActivity.class);
                                    intent.putExtra("email", email);
                                    startActivity(intent);
                                    overridePendingTransition(R.anim.slide_up, android.R.anim.fade_out);
                                    finish();
                                }
                            }).start())
                    .start();
        } else {
            shakeAllBoxes();
            Toast.makeText(this, "Incorrect code. Please try again.", Toast.LENGTH_SHORT).show();
            // Clear all boxes
            for (EditText box : otpBoxes) box.setText("");
            otpBoxes[0].requestFocus();
        }
    }

    private void saveUserAndGoMain() {
        dbHelper.registerUser(username, email, password, success -> {
            if (success) {
                Toast.makeText(this, "🎉 Account created and verified!", Toast.LENGTH_SHORT).show();
                
                // Auto login user
                android.content.SharedPreferences prefs = getSharedPreferences("spotmeet_prefs", MODE_PRIVATE);
                prefs.edit()
                        .putBoolean("is_logged_in", true)
                        .putString("user_email", email)
                        .putString("user_username", username)
                        .apply();

                Intent intent = new Intent(VerifyCodeActivity.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                overridePendingTransition(R.anim.fade_in, android.R.anim.fade_out);
                finish();
            } else {
                // Edge case: account already exists or network error
                Toast.makeText(this, "Failed to create account. Please try again.", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(VerifyCodeActivity.this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                overridePendingTransition(R.anim.fade_in, android.R.anim.fade_out);
                finish();
            }
        });
    }

    private void resendCode() {
        canResend = false;
        btnResend.setTextColor(0xFF666699);

        EmailSender.EmailCallback resendCallback = new EmailSender.EmailCallback() {
            @Override
            public void onSuccess(String code) {
                expectedCode = code;
                Toast.makeText(VerifyCodeActivity.this,
                        "New code sent to " + email, Toast.LENGTH_SHORT).show();
                for (EditText box : otpBoxes) box.setText("");
                otpBoxes[0].requestFocus();
                startResendTimer(120);
            }

            @Override
            public void onFailure(String error) {
                canResend = true;
                btnResend.setTextColor(0xFFB388FF);
                Toast.makeText(VerifyCodeActivity.this,
                        "Failed to resend. Try again.", Toast.LENGTH_SHORT).show();
            }
        };

        if ("register".equals(type)) {
            EmailSender.sendVerificationCode(email, username, resendCallback);
        } else {
            EmailSender.sendResetCode(email, resendCallback);
        }
    }

    private void startResendTimer(int seconds) {
        if (countDownTimer != null) countDownTimer.cancel();
        canResend = false;
        btnResend.setTextColor(0xFF666699);

        countDownTimer = new CountDownTimer(seconds * 1000L, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                long s = (millisUntilFinished / 1000) % 60;
                long m = (millisUntilFinished / 1000) / 60;
                tvTimer.setText(String.format("Resend available in %d:%02d", m, s));
            }

            @Override
            public void onFinish() {
                tvTimer.setText("You can now resend the code.");
                canResend = true;
                btnResend.setTextColor(0xFFB388FF);
            }
        }.start();
    }

    private void shakeAllBoxes() {
        for (EditText box : otpBoxes) {
            box.animate()
                    .translationX(8f).setDuration(50)
                    .withEndAction(() -> box.animate()
                            .translationX(-8f).setDuration(50)
                            .withEndAction(() -> box.animate()
                                    .translationX(0f).setDuration(50).start())
                            .start())
                    .start();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) countDownTimer.cancel();
    }
}
