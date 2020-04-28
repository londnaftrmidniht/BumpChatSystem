package com.bumpchat.bumpchat;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.NfcEvent;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.util.Log;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.bumpchat.bumpchat.connector.Connector;
import com.bumpchat.bumpchat.dao.ContactDAO;
import com.bumpchat.bumpchat.dao.MessageDAO;
import com.bumpchat.bumpchat.encryption.AesGcm;
import com.bumpchat.bumpchat.encryption.Dh;
import com.bumpchat.bumpchat.encryption.Rsa;
import com.bumpchat.bumpchat.helpers.AppExecutors;
import com.bumpchat.bumpchat.helpers.TemporaryKeyStorage;
import com.bumpchat.bumpchat.models.Contact;
import com.bumpchat.bumpchat.models.Message;

import net.sqlcipher.database.SQLiteDatabase;

import java.time.Instant;

import static android.nfc.NdefRecord.createMime;

public class ContactNew extends AppCompatActivity implements NfcAdapter.CreateNdefMessageCallback {
    Context context;
    NfcAdapter nfcAdapter;
    String nfcMimeType = "application/vnd.com.bumpchat.bumpchat.new";
    EditText contactName;
    Button saveButton;
    CheckBox keysGeneratedCheckbox;
    CheckBox keysReceivedCheckbox;
    CheckBox keysVerifiedCheckbox;
    CheckBox inboxCreatedCheckbox;
    TextView keysGeneratedLabel;
    TextView keysReceivedLabel;
    TextView keysVerifiedLabel;
    TextView inboxCreatedLabel;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contact_new);

        context = this;

        // Bind UI elements
        saveButton = findViewById(R.id.save_contact);
        contactName = findViewById(R.id.contact_name);
        keysGeneratedCheckbox = findViewById(R.id.pairing_keys_generated_checkbox);
        keysReceivedCheckbox = findViewById(R.id.pairing_keys_received_checkbox);
        keysVerifiedCheckbox = findViewById(R.id.pairing_keys_verified_checkbox);
        inboxCreatedCheckbox = findViewById(R.id.inbox_created_checkbox);
        keysGeneratedLabel = findViewById(R.id.pairing_keys_generated_label);
        keysReceivedLabel = findViewById(R.id.pairing_keys_received_label);
        keysVerifiedLabel = findViewById(R.id.pairing_keys_verified_label);
        inboxCreatedLabel = findViewById(R.id.inbox_created_label);

        // Clear state and regenerate keys
        if(!TemporaryKeyStorage.InUse)
        {
            TemporaryKeyStorage.Clear();
            TemporaryKeyStorage.InUse = true;
        }

        if (!TemporaryKeyStorage.InboxClaimed)
        {
            AppExecutors.getInstance().networkIO().execute(() -> {
                Connector connector = new Connector();
                if (connector.registerInbox(TemporaryKeyStorage.UserRsaKey)) {
                    // Inbox is now claimed
                    TemporaryKeyStorage.InboxClaimed = true;
                } else {
                    final String message = connector.getResponseMessage();

                    runOnUiThread(() -> {
                        final Toast toast = Toast.makeText(context, message, Toast.LENGTH_LONG);
                        toast.show();
                    });
                }

                runOnUiThread(this::updateUiFromState);
            });
        }

        // Set page title
        setTitle("Create Contact");

        // Mark state sent
        TemporaryKeyStorage.TransferState |= TemporaryKeyStorage.TransferStateSentMask;
        updateUiFromState();

        // Check for available NFC Adapter
        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (nfcAdapter == null) {
            Toast.makeText(this, "NFC is not available", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // Register callback
        nfcAdapter.setNdefPushMessageCallback(this, this);

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
                TemporaryKeyStorage.ContactName = s.toString();
                updateUiFromState();
            }
        };
        contactName.addTextChangedListener(afterTextChangedListener);

        saveButton.setOnClickListener(v -> {
            try {
                SQLiteDatabase.loadLibs(context);
                final AppDatabase appDatabase = AppDatabase.getInstance();

                AppExecutors.getInstance().diskIO().execute(() -> {
                    // Create new contact
                    ContactDAO contactDAO = appDatabase.getContactDAO();

                    // Create new inbox locally
                    Contact contact = new Contact(
                            Rsa.getHashedPublicKeyPem(TemporaryKeyStorage.UserRsaKey.getPublic()),
                            contactName.getText().toString(),
                            Base64.encodeToString(TemporaryKeyStorage.Aes256EncryptionKey, 16),
                            Rsa.getHashedPublicKeyPem(TemporaryKeyStorage.PartnerPublicRsaKey),
                            Rsa.encodeKeyToBase64(TemporaryKeyStorage.PartnerPublicRsaKey),
                            Rsa.encodeKeyToBase64(TemporaryKeyStorage.UserRsaKey.getPublic()),
                            Rsa.encodeKeyToBase64(TemporaryKeyStorage.UserRsaKey.getPrivate())
                    );

                    // Save to local db
                    contactDAO.insert(contact);

                    // Mark inbox claimed
                    if (TemporaryKeyStorage.InboxClaimed) {
                        contact.setClaimed(true);
                        contactDAO.update(contact);
                    }

                    // Clear temporary storage
                    TemporaryKeyStorage.Clear();

                    MessageDAO messageDAO = appDatabase.getMessageDAO();
                    Message message = new Message(
                            contact.getInbox_identifier(),
                            "Welcome to your new inbox!",
                            Instant.now().toEpochMilli(),
                            true
                    );
                    messageDAO.insert(message);

                    // Complete activity so user can't go back
                    finish();

                    // Launch to chat list
                    Intent intent = new Intent(context, ChatSelection.class);
                    startActivity(intent);
                });
            } catch (Exception ex) {
                Log.d("ContactNew", ex.toString());
            }
        });
    }

    @Override
    public NdefMessage createNdefMessage(NfcEvent event) {
        return new NdefMessage(
                new NdefRecord[] {
                        createMime(
                                nfcMimeType, new byte[]{TemporaryKeyStorage.TransferState}
                        ),
                        createMime(
                                nfcMimeType, TemporaryKeyStorage.UserRsaKey.getPublic().getEncoded()
                        ),
                        createMime(
                                nfcMimeType, TemporaryKeyStorage.UserDhKey.getPublic().getEncoded()
                        ),
                        createMime(
                                nfcMimeType, TemporaryKeyStorage.Hmac256Salt
                        )
                }
        );
    }

    @Override
    public void onResume() {
        super.onResume();
        // Check to see that the Activity started due to an Android Beam
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(getIntent().getAction())) {
            processIntent(getIntent());
        }
    }

    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        // onResume gets called after this to handle the intent
        setIntent(intent);
    }

    /**
     * Parses the NDEF Message from the intent and prints to the TextView
     */
    void processIntent(Intent intent) {
        Parcelable[] rawMsgs = intent.getParcelableArrayExtra(
                NfcAdapter.EXTRA_NDEF_MESSAGES
        );

        NdefMessage msg = (NdefMessage) rawMsgs[0];

        try {
            TemporaryKeyStorage.SaltShared = true;
            // Only set the salt if the partner has not already received
            // First to receive uses the others salt
            byte partnerState = msg.getRecords()[0].getPayload()[0];

            if ((partnerState & TemporaryKeyStorage.TransferStateUserMask) != TemporaryKeyStorage.TransferStateUserMask)
            {
                TemporaryKeyStorage.Hmac256Salt = msg.getRecords()[3].getPayload();
            }
            // Mark partner as received
            else
            {
                TemporaryKeyStorage.TransferState |= TemporaryKeyStorage.TransferStatePartnerMask;
                updateUiFromState();
            }

            TemporaryKeyStorage.PartnerPublicRsaKey = Rsa.decodePublicKey(msg.getRecords()[1].getPayload());
            TemporaryKeyStorage.PartnerPublicDhKey = Dh.decodePublicKey(msg.getRecords()[2].getPayload());
            TemporaryKeyStorage.Aes256EncryptionKey = AesGcm.deriveKeyFromDh(
                    Dh.generateDHSecret(TemporaryKeyStorage.UserDhKey.getPrivate(), TemporaryKeyStorage.PartnerPublicDhKey),
                    TemporaryKeyStorage.Hmac256Salt
            );

            // Mark state as keys received
            TemporaryKeyStorage.TransferState |= TemporaryKeyStorage.TransferStateUserMask;
            updateUiFromState();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateUiFromState()
    {
        if (!contactName.getText().toString().equals(TemporaryKeyStorage.ContactName))
        {
            contactName.setText(TemporaryKeyStorage.ContactName);
        }

        if ((TemporaryKeyStorage.TransferState & TemporaryKeyStorage.TransferStateSentMask) == TemporaryKeyStorage.TransferStateSentMask) {
            keysGeneratedCheckbox.setChecked(true);
            keysGeneratedLabel.setTextColor(ContextCompat.getColor(context, R.color.colorPrimary));
        }

        if ((TemporaryKeyStorage.TransferState & TemporaryKeyStorage.TransferStateUserMask) == TemporaryKeyStorage.TransferStateUserMask) {
            keysReceivedCheckbox.setChecked(true);
            keysReceivedLabel.setTextColor(ContextCompat.getColor(context, R.color.colorPrimary));
        }

        if ((TemporaryKeyStorage.TransferState & TemporaryKeyStorage.TransferStatePartnerMask) == TemporaryKeyStorage.TransferStatePartnerMask) {
            keysVerifiedCheckbox.setChecked(true);
            keysVerifiedLabel.setTextColor(ContextCompat.getColor(context, R.color.colorPrimary));
        }

        if (TemporaryKeyStorage.InboxClaimed) {
            inboxCreatedCheckbox.setChecked(true);
            inboxCreatedLabel.setTextColor(ContextCompat.getColor(context, R.color.colorPrimary));
        }

        boolean saveEnabled =
                (TemporaryKeyStorage.TransferState & TemporaryKeyStorage.TransferStateSyncedMask) == TemporaryKeyStorage.TransferStateSyncedMask
                && contactName.getText().length() > 0;

        saveButton.setEnabled(saveEnabled);
    }
}