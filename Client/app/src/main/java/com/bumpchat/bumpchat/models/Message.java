package com.bumpchat.bumpchat.models;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import java.sql.Date;


@Entity(tableName = "messages",
        indices = {@Index(value = {"inbox_identifier"}), @Index(value = {"date"})},
        foreignKeys = @ForeignKey(entity = Contact.class,
                parentColumns = "inbox_identifier",
                childColumns = "inbox_identifier",
                onDelete = 5) // CASCADE https://developer.android.com/reference/androidx/room/ForeignKey.html#CASCADE
)
public class Message {
    @PrimaryKey(autoGenerate = true)
    @NonNull
    private Long message_id;
    private String inbox_identifier;
    private String message;
    private Long date;
    private Boolean incoming;
    private Boolean read;

    public Message(String inbox_identifier, String message, Long date, Boolean incoming) {
        this.inbox_identifier = inbox_identifier;
        this.message = message;
        this.date = date;
        this.incoming = incoming;
        this.read = false;
    }

    @NonNull
    public Long getMessage_id() {
        return message_id;
    }

    public void setMessage_id(@NonNull Long message_id) {
        this.message_id = message_id;
    }

    public Long getDate() {
        return date;
    }

    public void setDate(Long date) {
        this.date = date;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getInbox_identifier() {
        return inbox_identifier;
    }

    public void setInbox_identifier(String inbox_identifier) {
        this.inbox_identifier = inbox_identifier;
    }

    public Boolean getRead() {
        return read;
    }

    public void setRead(Boolean read) {
        this.read = read;
    }

    public Boolean getIncoming() {
        return incoming;
    }

    public void setIncoming(Boolean incoming) {
        this.incoming = incoming;
    }
}