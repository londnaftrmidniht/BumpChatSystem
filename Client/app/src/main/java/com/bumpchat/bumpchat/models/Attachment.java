package com.bumpchat.bumpchat.models;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "attachments")
public class Attachment {
    @PrimaryKey
    @NonNull
    private Long attachment_id;
    private Long message_id;
    private String name;
    private String public_key;

    public Attachment() {
    }

    @NonNull
    public Long getAttachment_id() {
        return attachment_id;
    }

    public void setAttachment_id(@NonNull Long attachment_id) {
        this.attachment_id = attachment_id;
    }

    public Long getMessage_id() {
        return message_id;
    }

    public void setMessage_id(Long message_id) {
        this.message_id = message_id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPublic_key() {
        return public_key;
    }

    public void setPublic_key(String public_key) {
        this.public_key = public_key;
    }
}