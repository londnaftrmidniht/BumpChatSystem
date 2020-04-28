package com.bumpchat.bumpchat.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.bumpchat.bumpchat.models.Contact;
import com.bumpchat.bumpchat.models.ContactOverview;

import java.util.List;

@Dao
public interface ContactOverviewDAO {
    @Query("SELECT * FROM contactoverview")
    public List<ContactOverview> getContactOverviews();
}