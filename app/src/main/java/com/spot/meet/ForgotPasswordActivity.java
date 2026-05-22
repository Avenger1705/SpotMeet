package com.spot.meet;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class ForgotPasswordActivity extends AppCompatActivity {

    private EditText etSearchInput, etConfirmEmail;
    private TextView tvMaskedEmail, tvFullEmailReveal, btnBack, tvConfirmStepLabel;
    private RelativeLayout layoutConfirmEmailInput;
    private Button btnSearchAccount, btnSendCode;
    private LinearLayout cardSearchAccount, cardConfirmAccount;

    private DatabaseHelper dbHelper;
    private String foundEmail = null;   // The real email associated with the account
    private boolean searchedByEmail;    // true = user typed email, false = user typed username
    private boolean codeSent = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        // Apply Window Insets (Keyboard/IME aware)
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.activity_forgot_password_root), (v, insets) -> {
            androidx.core.graphics.Insets systemBars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars());
            androidx.core.graphics.Insets imeInsets = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.ime());
            View content = findViewById(R.id.forgot_scroll_content);
            if (content != null) {
                content.setPadding(content.getPaddingLeft(), content.getPaddingTop(), 
                                 content.getPaddingRight(), Math.max(40, imeInsets.bottom));
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
                    ScrollView scrollView = findViewById(R.id.activity_forgot_password_root);
                    // Calculate the real Y position of the view relative to the ScrollView content
                    int[] location = new int[2];
                    v.getLocationOnScreen(location);
                    int[] scrollLocation = new int[2];
                    scrollView.getLocationOnScreen(scrollLocation);
                    
                    int y = location[1] - scrollLocation[1] + scrollView.getScrollY();
                    // Scroll a bit further to ensure the field and its label are visible
                    scrollView.smoothScrollTo(0, y - 100); 
                }, 200);
            }
        };
        etSearchInput.setOnFocusChangeListener(scrollFocusListener);
        etConfirmEmail.setOnFocusChangeListener(scrollFocusListener);
    }

    private void initViews() {
        etSearchInput       = findViewById(R.id.et_forgot_search_email);   // now accepts username OR email
        etConfirmEmail      = findViewById(R.id.et_forgot_confirm_email);
        tvMaskedEmail       = findViewById(R.id.tv_masked_email);
        tvFullEmailReveal   = findViewById(R.id.tv_full_email_reveal);
        tvConfirmStepLabel  = findViewById(R.id.tv_confirm_step_label);
        layoutConfirmEmailInput = findViewById(R.id.layout_confirm_email_input);
        btnBack             = findViewById(R.id.btn_back_forgot);
        btnSearchAccount    = findViewById(R.id.btn_search_account);
        btnSendCode         = findViewById(R.id.btn_send_code);
        cardSearchAccount   = findViewById(R.id.card_search_account);
        cardConfirmAccount  = findViewById(R.id.card_confirm_account);
    }

    private void startEntranceAnimations() {
        LinearLayout header = findViewById(R.id.forgot_header);
        header.setAlpha(0f);
        cardSearchAccount.setAlpha(0f);
        cardSearchAccount.setTranslationY(30f);

        header.animate().alpha(1f).setDuration(500).setStartDelay(100).start();
        cardSearchAccount.animate().alpha(1f).translationY(0f).setDuration(550).setStartDelay(220).start();
    }

    private void setListeners() {
        btnBack.setOnClickListener(v -> {
            handleBackToStep1OrExit();
        });

        // Step 1: search by username OR email
        btnSearchAccount.setOnClickListener(v -> searchAccount());

        // Watch confirm email field:
        // - If searched by EMAIL → reveal full email when typed correctly
        // - If searched by USERNAME → never reveal full email, keep showing masked only
        etConfirmEmail.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (!searchedByEmail) return; // username search → never reveal full email

                String typed = s.toString().trim();
                if (foundEmail != null && typed.equalsIgnoreCase(foundEmail)) {
                    // Reveal full email with green check
                    tvFullEmailReveal.setVisibility(View.VISIBLE);
                    tvFullEmailReveal.setText("✓  " + foundEmail);
                    tvFullEmailReveal.setAlpha(0f);
                    tvFullEmailReveal.animate().alpha(1f).setDuration(400).start();
                } else {
                    tvFullEmailReveal.setVisibility(View.GONE);
                }
            }
        });

        // Step 2: Send code
        btnSendCode.setOnClickListener(v -> sendCode());
    }

    // ─── Step 1: Find account ────────────────────────────────────────────────

    private void searchAccount() {
        String input = etSearchInput.getText().toString().trim();

        if (input.isEmpty()) {
            shakeView(etSearchInput);
            etSearchInput.setError("Please enter your username or email");
            return;
        }

        // Detect whether it's an email or a username
        boolean looksLikeEmail = Patterns.EMAIL_ADDRESS.matcher(input).matches();
        
        btnSearchAccount.setEnabled(false);
        btnSearchAccount.setText("Searching…");

        DatabaseHelper.EmailLookupCallback callback = new DatabaseHelper.EmailLookupCallback() {
            @Override
            public void onResult(String email) {
                if (email == null) {
                    shakeView(btnSearchAccount);
                    String notFoundMsg = looksLikeEmail
                            ? "No account found with that email."
                            : "No account found with that username.";
                    Toast.makeText(ForgotPasswordActivity.this, notFoundMsg, Toast.LENGTH_SHORT).show();
                    btnSearchAccount.setText("FIND ACCOUNT");
                    btnSearchAccount.setEnabled(true);
                    return;
                }

                btnSearchAccount.setText("FIND ACCOUNT");
                btnSearchAccount.setEnabled(true);

                // Account found
                foundEmail = email;

                // Configure Step 2 based on HOW they searched
                if (searchedByEmail) {
                    tvMaskedEmail.setText(email); // Show FULL email
                    tvConfirmStepLabel.setText("We will send the reset code to this address.");
                } else {
                    tvMaskedEmail.setText(maskEmail(email)); // Show MASKED email
                    tvConfirmStepLabel.setText("This is the masked email associated with your account.\nWe will send the reset code to this address.");
                }

                // Hide the confirm input block entirely for both flows
                tvConfirmStepLabel.setVisibility(View.VISIBLE);
                layoutConfirmEmailInput.setVisibility(View.GONE);
                tvFullEmailReveal.setVisibility(View.GONE);

                // Animate out Step 1 to make it look like a "next page"
                cardSearchAccount.animate()
                        .alpha(0f)
                        .translationX(-50f)
                        .setDuration(300)
                        .withEndAction(() -> {
                            cardSearchAccount.setVisibility(View.GONE);

                            // Animate in Step 2
                            cardConfirmAccount.setAlpha(0f);
                            cardConfirmAccount.setTranslationX(50f);
                            cardConfirmAccount.setVisibility(View.VISIBLE);
                            cardConfirmAccount.animate()
                                    .alpha(1f)
                                    .translationX(0f)
                                    .setDuration(400)
                                    .start();
                        }).start();

                Toast.makeText(ForgotPasswordActivity.this, "Account found!", Toast.LENGTH_SHORT).show();
            }
        };

        if (looksLikeEmail) {
            searchedByEmail = true;
            dbHelper.getUserEmailByEmail(input, callback);
        } else {
            searchedByEmail = false;
            dbHelper.getUserEmailByUsername(input, callback);
        }
    }

    private void sendCode() {

        if (codeSent) {
            Toast.makeText(this, "Code already sent. Check your email.", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSendCode.setEnabled(false);
        btnSendCode.setText("Sending…");

        EmailSender.sendResetCode(foundEmail, new EmailSender.EmailCallback() {
            @Override
            public void onSuccess(String code) {
                codeSent = true;
                btnSendCode.setText("CODE SENT ✓");

                Intent intent = new Intent(ForgotPasswordActivity.this, VerifyCodeActivity.class);
                intent.putExtra("type", "reset");
                intent.putExtra("email", foundEmail);
                intent.putExtra("code", code);
                startActivity(intent);
                overridePendingTransition(R.anim.slide_up, android.R.anim.fade_out);
            }

            @Override
            public void onFailure(String error) {
                btnSendCode.setEnabled(true);
                btnSendCode.setText("SEND RESET CODE");
                Toast.makeText(ForgotPasswordActivity.this,
                        "Failed to send email. Please try again.", Toast.LENGTH_LONG).show();
            }
        });
    }

    private String maskEmail(String email) {
        String[] parts = email.split("@");
        if (parts.length != 2) return email;

        String name   = parts[0];
        String domain = parts[1];

        if (name.length() <= 3) {
            return name.charAt(0) + "*".repeat(Math.max(1, name.length() - 1)) + "@" + domain;
        }

        char   first       = name.charAt(0);
        String last2       = name.substring(name.length() - 2);
        int    maskedCount = name.length() - 3;
        String stars       = "*".repeat(Math.max(2, maskedCount));

        return first + stars + last2 + "@" + domain;
    }

    @Override
    public void onBackPressed() {
        handleBackToStep1OrExit();
    }

    private void handleBackToStep1OrExit() {
        if (cardConfirmAccount.getVisibility() == View.VISIBLE) {
            cardConfirmAccount.animate()
                    .alpha(0f)
                    .translationX(50f)
                    .setDuration(300)
                    .withEndAction(() -> {
                        cardConfirmAccount.setVisibility(View.GONE);

                        // Animate in Step 1
                        cardSearchAccount.setAlpha(0f);
                        cardSearchAccount.setTranslationX(-50f);
                        cardSearchAccount.setVisibility(View.VISIBLE);
                        cardSearchAccount.animate()
                                .alpha(1f)
                                .translationX(0f)
                                .setDuration(400)
                                .start();
                        
                        // Reset search box stuff securely
                        foundEmail = null;
                        codeSent = false;
                        btnSendCode.setText("SEND RESET CODE");
                        btnSendCode.setEnabled(true);
                    }).start();
        } else {
            // Exit the activity completely
            super.onBackPressed();
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        }
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
