package com.bumpchat.bumpchat;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

import com.bumpchat.bumpchat.dao.ContactOverviewDAO;
import com.bumpchat.bumpchat.helpers.AppExecutors;
import com.bumpchat.bumpchat.helpers.TemporaryKeyStorage;

import net.sqlcipher.database.SQLiteDatabase;

import java.util.Timer;
import java.util.TimerTask;

public class LoginActivity extends AppCompatActivity {
    final int MIN_PASS_LEN = 12;
    Context context;
    TextView resultText;
    ImageView lockImage;
    ProgressBar loadingProgressBar;
    Button loginButton;
    EditText passwordEditText;
    ColorStateList oldPasswordColor;
    ColorStateList oldPasswordTint;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        context = this;
        setContentView(R.layout.activity_login);

        passwordEditText = findViewById(R.id.password);
        loginButton = findViewById(R.id.login);
        loadingProgressBar = findViewById(R.id.loading);
        resultText = findViewById(R.id.result);
        lockImage = findViewById(R.id.image_lock);
        oldPasswordColor = passwordEditText.getTextColors();
        oldPasswordTint = passwordEditText.getBackgroundTintList();

        // Reset key transfer
        TemporaryKeyStorage.Clear();

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
                boolean enabled = passwordEditText.getText().length() >= MIN_PASS_LEN;

                if (enabled) {
                    passwordEditText.setTextColor(ContextCompat.getColor(context, R.color.colorPrimary));
                    passwordEditText.setBackgroundTintList(ContextCompat.getColorStateList(context, R.color.colorPrimary));
                } else {
                    passwordEditText.setTextColor(oldPasswordColor);
                    passwordEditText.setBackgroundTintList(oldPasswordTint);
                }

                loginButton.setEnabled(enabled);
            }
        };
        passwordEditText.addTextChangedListener(afterTextChangedListener);
        passwordEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {

            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    doLogin(passwordEditText.getText().toString());
                }
                return false;
            }
        });

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadingProgressBar.setVisibility(View.VISIBLE);
                doLogin(passwordEditText.getText().toString());
            }
        });
    }

    private void showLoginSuccess(String successMessage) {
        loadingProgressBar.setVisibility(View.VISIBLE);
        loginButton.setEnabled(false);
        lockImage.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_lock_closed_green, null));
        resultText.setText(successMessage);
        resultText.setTextColor(ContextCompat.getColor(context, R.color.colorPrimary));
    }

    private void showLoginFailed(String errorString) {
        lockImage.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_lock_closed_red, null));
        resultText.setText(errorString);
        resultText.setTextColor(ContextCompat.getColor(context, R.color.colorError));
    }

    private void doLogin(String password) {
        try {
            SQLiteDatabase.loadLibs(this);
            final AppDatabase appDatabase = AppDatabase.startInstance(this, password);

            loadingProgressBar.setVisibility(View.GONE);
            passwordEditText.setEnabled(false);

            AppExecutors.getInstance().diskIO().execute(new Runnable() {
                @Override
                public void run() {
                    // Hack to test if database was properly decrypted
                    boolean failed = false;

                    try {
                        ContactOverviewDAO contactOverviewDAO = appDatabase.getContactOveriewDAO();
                        contactOverviewDAO.getContactOverviews();
                    } catch (Exception ex) {
                        Log.d("BumpChatLogin", "Wrong password");
                        failed = true;
                    }
                    finally {
                        if (failed) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    passwordEditText.setEnabled(true);
                                    passwordEditText.setTextColor(ContextCompat.getColor(context, R.color.colorError));
                                    passwordEditText.setBackgroundTintList(ContextCompat.getColorStateList(context, R.color.colorError));
                                    showLoginFailed("Bad password. Please try again.");
                                }
                            });
                        } else {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    showLoginSuccess("Database decrypted successfully. Starting...");
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
                        }
                    }
                }
            });
        } catch (Exception ex) {
            showLoginFailed(ex.toString());
            Log.d("BumpChatLogin", ex.toString());
        }
    }

}
