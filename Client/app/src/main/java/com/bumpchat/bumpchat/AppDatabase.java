package com.bumpchat.bumpchat;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import com.bumpchat.bumpchat.dao.AttachmentDAO;
import com.bumpchat.bumpchat.dao.ContactDAO;
import com.bumpchat.bumpchat.dao.ContactOverviewDAO;
import com.bumpchat.bumpchat.dao.MessageDAO;
import com.bumpchat.bumpchat.helpers.Converters;
import com.bumpchat.bumpchat.models.Attachment;
import com.bumpchat.bumpchat.models.Contact;
import com.bumpchat.bumpchat.models.ContactOverview;
import com.bumpchat.bumpchat.models.Message;

import net.sqlcipher.database.SQLiteDatabase;
import net.sqlcipher.database.SupportFactory;

@Database(
        entities = {Attachment.class, Contact.class, Message.class},
        views = {ContactOverview.class},
        version = 1
)
@TypeConverters({Converters.class})
public abstract class AppDatabase extends RoomDatabase {
    private static AppDatabase instance;

    static synchronized AppDatabase startInstance(Context context, String password) {
        final byte[] passphrase = SQLiteDatabase.getBytes(password.toCharArray());
        final SupportFactory factory = new SupportFactory(passphrase);

        String DB_NAME = "bumpChat";
        instance = Room.databaseBuilder(context.getApplicationContext(), AppDatabase.class, DB_NAME)
                .openHelperFactory(factory)
                .fallbackToDestructiveMigration()
                .build();

        return instance;
    }

    // Only return instance after database has been opened
    public static synchronized AppDatabase getInstance() {
        return instance;
    }

    public abstract AttachmentDAO getAttachmentDAO();
    public abstract ContactDAO getContactDAO();
    public abstract MessageDAO getMessageDAO();
    public abstract ContactOverviewDAO getContactOveriewDAO();
}
