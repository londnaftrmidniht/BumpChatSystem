package com.bumpchat.bumpchat.models;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "contacts")
public class Contact {
    @PrimaryKey
    @NonNull
    private String inbox_identifier; // Identifier used to retrieve inbox
    private String inbox_public_key; // Public key used to retrieve inbox
    private String inbox_private_key; // Private key used to authenticate with inbox
    private String name;
    private String recipient_identifier; // Identifier of contact receiving messages
    private String recipient_public_key; // Public key of contact receiving messages
    private String shared_aes_key; // AES key used to actually encrypt/decrypt messages
    private Boolean claimed; // Maker to show inbox exists on server

    public Contact(
            String inbox_identifier,
            String name,
            String shared_aes_key,
            String recipient_identifier,
            String recipient_public_key,
            String inbox_public_key,
            String inbox_private_key
    ) {
        this.inbox_identifier = inbox_identifier;
        this.name = name;
        this.shared_aes_key = shared_aes_key;
        this.recipient_identifier = recipient_identifier;
        this.recipient_public_key = recipient_public_key;
        this.inbox_public_key = inbox_public_key;
        this.inbox_private_key = inbox_private_key;
        this.claimed = false;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRecipient_public_key() {
        return recipient_public_key;
    }

    public String getInbox_public_key() {
        return inbox_public_key;
    }

    public String getInbox_private_key() {
        return inbox_private_key;
    }

    public void setClaimed(Boolean claimed) {
        this.claimed = claimed;
    }

    @NonNull
    public String getInbox_identifier() {
        return inbox_identifier;
    }

    public Boolean getClaimed() {
        return claimed;
    }

    public String getShared_aes_key() {
        return shared_aes_key;
    }

    public String getRecipient_identifier() {
        return recipient_identifier;
    }
}