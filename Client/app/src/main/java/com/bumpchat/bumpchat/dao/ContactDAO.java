package com.bumpchat.bumpchat.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.bumpchat.bumpchat.models.Contact;

import java.util.List;

@Dao
public interface ContactDAO {
    @Insert
    public void insert(Contact... items);

    @Update
    public void update(Contact... items);

    @Delete
    public void delete(Contact item);

    @Query("SELECT * FROM contacts WHERE inbox_identifier = :inboxIdentifier")
    public Contact getContactByIdentifier(String inboxIdentifier);

    @Query("SELECT * FROM contacts")
    public List<Contact> getContacts();
}