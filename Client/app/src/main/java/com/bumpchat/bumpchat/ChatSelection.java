package com.bumpchat.bumpchat;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.bumpchat.bumpchat.adapters.ContactMessageAdapter;
import com.bumpchat.bumpchat.dao.ContactDAO;
import com.bumpchat.bumpchat.dao.ContactOverviewDAO;
import com.bumpchat.bumpchat.dao.MessageDAO;
import com.bumpchat.bumpchat.helpers.AppExecutors;
import com.bumpchat.bumpchat.models.Contact;
import com.bumpchat.bumpchat.models.ContactOverview;
import com.bumpchat.bumpchat.models.Message;

import net.sqlcipher.database.SQLiteDatabase;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class ChatSelection extends AppCompatActivity implements AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener {
    Context context;
    HashSet<Integer> selectedHashSet;
    boolean deleteMode;
    MenuItem deleteMenuItem;
    ListView contactListView;
    ContactMessageAdapter contactMessageAdapter;
    AppDatabase appDatabase;
    ContactOverviewDAO contactOverviewDAO;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        context = this;
        setContentView(R.layout.activity_chat_selection);

        setTitle("Inbox");

        selectedHashSet = new HashSet<>();

        appDatabase = AppDatabase.getInstance();
        contactOverviewDAO = appDatabase.getContactOveriewDAO();

        contactListView = findViewById(R.id.chat_selection_listview);
        contactListView.setOnItemClickListener(this);
        contactListView.setOnItemLongClickListener(this);

        loadSavedContacts(contactListView);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_chat_selection, menu);
        // Delete button is the only menu item
        deleteMenuItem = menu.getItem(0);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.delete_menu_button) {
            for (int selectedId: selectedHashSet) {
                ContactOverview contactOverview = (ContactOverview) contactListView.getItemAtPosition(selectedId);

                // Delete messages then inbox
                AppExecutors.getInstance().diskIO().execute(() -> {
                    ContactDAO contactDAO = appDatabase.getContactDAO();
                    MessageDAO messageDAO = appDatabase.getMessageDAO();
                    List<Message> messages = messageDAO.getMessagesByInbox(contactOverview.inbox_identifier);

                    // Delete all messages
                    for (Message message : messages) {
                        messageDAO.delete(message);
                    }

                    // Delete inbox
                    Contact contact = contactDAO.getContactByIdentifier(contactOverview.inbox_identifier);
                    contactDAO.delete(contact);

                    runOnUiThread(() -> {
                        contactMessageAdapter.remove(contactOverview);
                        contactMessageAdapter.notifyDataSetChanged();
                    });
                });
            }

            selectedHashSet.clear();

            deleteMode = false;
            updateDeleteMenuActive();

            return true;
        }

        return false;
    }

    @Override
    public void onBackPressed() {
        // Reset all selections
        if (deleteMode) {
            deleteMode = false;
            updateDeleteMenuActive();

            contactMessageAdapter.notifyDataSetChanged();
            selectedHashSet.clear();
        }
    }

    public void updateDeleteMenuActive() {
        deleteMenuItem.setVisible(deleteMode);
    }

    public void onNewContactClick(View view) {
        // Launch to new contact activity
        Intent intent = new Intent(this, ContactNew.class);
        startActivity(intent);
    }

    public void onItemClick(AdapterView<?> l, View view, int position, long id) {
        if (deleteMode) {
            boolean alreadyChecked = selectedHashSet.contains(position);

            if (alreadyChecked) {
                view.setBackgroundColor(ContextCompat.getColor(context, android.R.color.white));
                selectedHashSet.remove(position);
            } else {
                view.setBackgroundColor(ContextCompat.getColor(context, R.color.colorPrimaryLight));
                selectedHashSet.add(position);
            }
        } else {
            ContactOverview contactOverview = (ContactOverview) l.getItemAtPosition(position)
                    ;
            // Launch to chat list
            Intent intent = new Intent(this, ChatSingle.class);
            intent.putExtra("inboxIdentifier", contactOverview.getInbox_identifier());
            startActivity(intent);
        }
    }

    private void loadSavedContacts(ListView contactListView) {
        SQLiteDatabase.loadLibs(this);
        final ArrayList<ContactOverview> contactOverviews = new ArrayList<>();

        // Create an ArrayAdapter from List
        contactMessageAdapter = new ContactMessageAdapter(contactOverviews, this);
        contactListView.setAdapter(contactMessageAdapter);

        AppExecutors.getInstance().diskIO().execute(new Runnable() {
            @Override
            public void run() {
                contactOverviews.addAll(contactOverviewDAO.getContactOverviews());

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        contactMessageAdapter.notifyDataSetChanged();
                    }
                });
            }
        });
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> adapterView, View view, int position, long l) {
        if (!deleteMode) {
            deleteMode = true;
            updateDeleteMenuActive();

            view.setBackgroundColor(ContextCompat.getColor(context, R.color.colorPrimaryLight));
            selectedHashSet.add(position);
        }

        // Return true to stop item click from firing
        return true;
    }
}
