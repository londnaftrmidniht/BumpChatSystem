package com.bumpchat.bumpchat.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.bumpchat.bumpchat.models.Message;
import java.util.List;

@Dao
public interface MessageDAO {
    @Insert
    public void insert(Message... item);

    @Update
    public void update(Message... items);

    @Delete
    public void delete(Message item);

    @Query("SELECT * FROM messages WHERE inbox_identifier = :inbox_identifier ORDER BY date ASC")
    public List<Message> getMessagesByInbox(String inbox_identifier);

    @Query("SELECT * FROM messages")
    public List<Message> getMessages();
}