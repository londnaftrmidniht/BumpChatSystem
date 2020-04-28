package com.bumpchat.bumpchat;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

import com.bumpchat.bumpchat.dao.ContactDAO;
import com.bumpchat.bumpchat.helpers.AppExecutors;
import com.bumpchat.bumpchat.models.Contact;

import net.sqlcipher.database.SQLiteDatabase;

public class ContactEdit extends AppCompatActivity {
    Context context;
    Button saveButton;
    String inboxIdentifier;
    EditText contactName;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        context = this;
        setContentView(R.layout.activity_contact_edit);
        saveButton = findViewById(R.id.save_edit_contact);
        contactName = findViewById(R.id.contact_edit_name);
        inboxIdentifier = getIntent().getStringExtra("inboxIdentifier");

        // Set page title
        setTitle("Edit Contact");

        // Load contact from db
        SQLiteDatabase.loadLibs(this);
        final AppDatabase appDatabase = AppDatabase.getInstance();

        AppExecutors.getInstance().diskIO().execute(new Runnable() {
            @Override
            public void run() {
                ContactDAO contactDAO = appDatabase.getContactDAO();
                final Contact contact = contactDAO.getContactByIdentifier(inboxIdentifier);

                runOnUiThread(new Runnable() {
                    public void run() {
                        contactName.setText(contact.getName());
                    }
                });
            }
        });

        // Text change listener
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
                boolean saveEnabled = contactName.getText().length() > 0;
                saveButton.setEnabled(saveEnabled);
            }
        };
        contactName.addTextChangedListener(afterTextChangedListener);

        // Save button listener
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    SQLiteDatabase.loadLibs(context);
                    final AppDatabase appDatabase = AppDatabase.getInstance();

                    AppExecutors.getInstance().diskIO().execute(new Runnable() {
                        @Override
                        public void run() {
                            // Create new contact
                            ContactDAO contactDAO = appDatabase.getContactDAO();

                            Contact contact = contactDAO.getContactByIdentifier(inboxIdentifier);
                            contact.setName(contactName.getText().toString());
                            contactDAO.update(contact);

                            // Complete activity so user can't go back
                            finish();

                            // Launch to chat list
                            Intent intent = new Intent(context, ChatSingle.class);
                            intent.putExtra("inboxIdentifier", inboxIdentifier);
                            startActivity(intent);
                        }
                    });
                } catch (Exception ex) {
                    Log.d("ContactEdit", ex.toString());
                }
            }
        });
    }
}
