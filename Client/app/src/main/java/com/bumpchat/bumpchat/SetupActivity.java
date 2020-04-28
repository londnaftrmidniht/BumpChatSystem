package com.bumpchat.bumpchat;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

import net.sqlcipher.database.SQLiteDatabase;

import java.util.Timer;
import java.util.TimerTask;

public class SetupActivity extends AppCompatActivity {
    final int MIN_PASS_LEN = 12;

    Context context;
    ImageView lockImage;
    ProgressBar loadingProgressBar;
    Button saveButton;
    EditText passwordEditText;
    EditText passwordConfirmEditText;
    ColorStateList oldPasswordColor;
    ColorStateList oldPasswordTint;
    TextView introduction;
    TextView hint;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        context = this;
        setContentView(R.layout.activity_setup);

        passwordEditText = findViewById(R.id.password);
        passwordConfirmEditText = findViewById(R.id.password_confirm);
        saveButton = findViewById(R.id.set_password);
        loadingProgressBar = findViewById(R.id.loading);
        lockImage = findViewById(R.id.image_lock);
        introduction = findViewById(R.id.introduction);
        hint = findViewById(R.id.password_rules);
        oldPasswordColor = passwordEditText.getTextColors();
        oldPasswordTint = passwordEditText.getBackgroundTintList();

        TextWatcher afterTextChangedListener = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // ignore
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // ignore
            }

            @Override
            public void afterTextChanged(Editable s) {
                updateUi();
            }
        };

        passwordEditText.addTextChangedListener(afterTextChangedListener);
        passwordConfirmEditText.addTextChangedListener(afterTextChangedListener);

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadingProgressBar.setVisibility(View.VISIBLE);
                doLogin(passwordEditText.getText().toString());
            }
        });
    }

    private void updateUi() {
        String password = passwordEditText.getText().toString();
        String passwordConfirm = passwordConfirmEditText.getText().toString();
        boolean passwordValid = true;

        if (password.length() >= MIN_PASS_LEN) {
            passwordEditText.setTextColor(ContextCompat.getColor(context, R.color.colorPrimary));
            passwordEditText.setBackgroundTintList(ContextCompat.getColorStateList(context, R.color.colorPrimary));
        } else {
            passwordEditText.setTextColor(oldPasswordColor);
            passwordEditText.setBackgroundTintList(oldPasswordTint);
            passwordValid = false;
        }

        if (password.equals(passwordConfirm)) {
            passwordConfirmEditText.setTextColor(ContextCompat.getColor(context, R.color.colorPrimary));
            passwordConfirmEditText.setBackgroundTintList(ContextCompat.getColorStateList(context, R.color.colorPrimary));
        } else {
            passwordConfirmEditText.setTextColor(oldPasswordColor);
            passwordConfirmEditText.setBackgroundTintList(oldPasswordTint);
            passwordValid = false;
        }

        saveButton.setEnabled(passwordValid);
    }

    private void showLoginSuccess(String successMessage) {
        passwordEditText.setVisibility(View.INVISIBLE);
        passwordConfirmEditText.setVisibility(View.INVISIBLE);
        loadingProgressBar.setVisibility(View.VISIBLE);
        hint.setVisibility(View.INVISIBLE);
        introduction.setText(successMessage);

        saveButton.setEnabled(false);
        lockImage.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_lock_closed_green, null));
    }

    private void doLogin(String password) {
        try {
            // First time password will set it
            SQLiteDatabase.loadLibs(this);
            AppDatabase.startInstance(this, password);

            loadingProgressBar.setVisibility(View.GONE);

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    showLoginSuccess("Database encrypted successfully. Starting...");
                }
            });

            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    // Complete activity so user can't go back
                    finish();

                    // Launch to chat list
                    Intent intent = new Intent(context, ChatSelection.class);
                    startActivity(intent);
                    Log.d("BumpChatLogin", "Login success!");
                }
            }, 3000);
        } catch (Exception ex) {
            Log.d("BumpChatLogin", ex.toString());
        }
    }

}
