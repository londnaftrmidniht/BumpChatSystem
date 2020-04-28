package com.bumpchat.bumpchat.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.bumpchat.bumpchat.models.Attachment;
import java.util.List;

@Dao
public interface AttachmentDAO {
    @Insert
    public void insert(Attachment... items);
    @Update
    public void update(Attachment... items);
    @Delete
    public void delete(Attachment item);

    @Query("SELECT * FROM attachments WHERE attachment_id = :id")
    public Attachment getAttachmentById(Long id);

    @Query("SELECT * FROM attachments WHERE message_id = :id")
    public List<Attachment> getAttachmentsByMessage(Long id);

    @Query("SELECT * FROM attachments")
    public List<Attachment> getAttachments();
}