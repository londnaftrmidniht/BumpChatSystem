package com.bumpchat.bumpchat;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.bumpchat.bumpchat.adapters.MessageAdapter;
import com.bumpchat.bumpchat.connector.Connector;
import com.bumpchat.bumpchat.dao.ContactDAO;
import com.bumpchat.bumpchat.dao.MessageDAO;
import com.bumpchat.bumpchat.encryption.Rsa;
import com.bumpchat.bumpchat.helpers.AppExecutors;
import com.bumpchat.bumpchat.models.Contact;
import com.bumpchat.bumpchat.models.Message;

import net.sqlcipher.database.SQLiteDatabase;

import java.io.File;
import java.io.IOException;
import java.security.KeyPair;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import static android.content.ContentValues.TAG;

public class ChatSingle extends AppCompatActivity implements AdapterView.OnItemClickListener {
    private EditText editText;
    private Connector connector;
    private Contact contact;
    private Context context;
    private ListView messagesListView;
    private MessageAdapter messageAdapter;
    private AppDatabase appDatabase;
    private KeyPair keyPair;
    private String inboxIdentifier;

    static final int REQUEST_IMAGE_CAPTURE = 1;
    static final int REQUEST_TAKE_PHOTO = 1;
    private ImageView imageView;
    String currentPhotoPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = this;
        setContentView(R.layout.activity_chat_single);
        editText = findViewById(R.id.chat_message_edit_text);

        // Initiate external API connector
        connector = new Connector();

        // Attach listview
        messagesListView = findViewById(R.id.chat_message_listview);
        messagesListView.setOnItemClickListener(this);

        // Get passed identifier
        inboxIdentifier = getIntent().getStringExtra("inboxIdentifier");

        // Get database connector
        SQLiteDatabase.loadLibs(this);

        // Attach message adapter
        ArrayList<Message> messages = new ArrayList<>();
        messageAdapter = new MessageAdapter(messages, this);
        messagesListView.setAdapter(messageAdapter);

        appDatabase = AppDatabase.getInstance();

        loadChatHistory(inboxIdentifier);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_chat_single, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.menu_chat_single_edit){
            // liew
            Log.w(TAG, "Edit button clicked");
            // Launch to chat list
            Intent intent = new Intent(this, ContactEdit.class);
            intent.putExtra("inboxIdentifier", getIntent().getStringExtra("inboxIdentifier"));
            startActivity(intent);
        } else if (id == R.id.menu_chat_single_clear){
            AppExecutors.getInstance().diskIO().execute(() -> {
                MessageDAO messageDAO = appDatabase.getMessageDAO();
                List<Message> messages = messageDAO.getMessagesByInbox(inboxIdentifier);

                // Delete all messages
                for (Message message : messages) {
                    messageDAO.delete(message);
                }

                // Add placeholder message to keep inbox from being empty
                final Message placeholderMessage = new Message(
                    contact.getInbox_identifier(),
                    "Messages cleared",
                    Instant.now().toEpochMilli(),
                    true);

                messageDAO.insert(placeholderMessage);

                runOnUiThread(() -> {
                    messageAdapter.clear();
                    messageAdapter.add(placeholderMessage);
                    messageAdapter.notifyDataSetChanged();
                });
            });
        }
        return super.onOptionsItemSelected(item);
    }

    public void onItemClick(AdapterView<?> l, View v, int position, long id) {
        Message message = (Message) l.getItemAtPosition(position);

        System.out.println("Item clicked with id/position: " + id + "/" + position);
        System.out.println("Item clicked: " + message.getInbox_identifier());
    }

    private void loadChatHistory(final String inboxIdentifier) {
        AppExecutors.getInstance().diskIO().execute(() -> {
        // Register new inbox
        ContactDAO contactDAO = appDatabase.getContactDAO();
        contact = contactDAO.getContactByIdentifier(inboxIdentifier);
        keyPair = Rsa.convertStringToKeyPair(contact.getInbox_public_key(), contact.getInbox_private_key());

        // Claim inbox on first run
        if (!contact.getClaimed()) {
            Connector connector = new Connector();
            if (connector.registerInbox(keyPair)) {
                // Inbox is now claimed
                contact.setClaimed(true);
                contactDAO.update(contact);
            }

            final String message = connector.getResponseMessage();

            runOnUiThread(() -> {
                final Toast toast = Toast.makeText(context, message, Toast.LENGTH_LONG);
                toast.show();
            });
        }

        // Get all current messages
        MessageDAO messageDAO = appDatabase.getMessageDAO();
        final List<Message> currentMessages = messageDAO.getMessagesByInbox(inboxIdentifier);

        runOnUiThread(() -> messageAdapter.addAll(currentMessages));

        // Get the messages from server
        // Fixed delay execution checking for new messages
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                getMessages();
            }
        }, 0, 5000);

        // Update title and list
        runOnUiThread(() -> {
            String contactName = contact.getName();
            if (contactName.length() > 15) {
                contactName = contactName.substring(0, 15) + "...";
            }

            setTitle(contactName);
            //        getActionBar().setIcon(R.drawable.my_icon);

            messageAdapter.notifyDataSetChanged();
            messagesListView.setSelection(messageAdapter.getCount() - 1);
        });
        });
    }

    public void getMessages() {
        AppExecutors.getInstance().networkIO().execute(() -> {
            MessageDAO messageDAO = appDatabase.getMessageDAO();

            // Get messages
            final List<Message> messages = connector.getMessages(keyPair, contact.getShared_aes_key());

            // Update database and view if new messages retrieved
            if (messages.size() > 0)
            {
                for (Message message : messages) {
                    messageDAO.insert(message);
                }

                runOnUiThread(() -> {
                    messageAdapter.addAll(messages);
                    messageAdapter.notifyDataSetChanged();
                    messagesListView.setSelection(messageAdapter.getCount() - 1);
                });
            }
        });
    }

    public void sendMessage(View view) {
        final String messageText = editText.getText().toString();

        if (messageText.length() > 0) {
            editText.getText().clear();

            final Message message = new Message(contact.getInbox_identifier(), messageText, Instant.now().toEpochMilli(), false);

            AppExecutors.getInstance().networkIO().execute(() -> {
                MessageDAO messageDAO = appDatabase.getMessageDAO();

                // Send message
                KeyPair keyPair = Rsa.convertStringToKeyPair(contact.getInbox_public_key(), contact.getInbox_private_key());

                if (connector.sendMessage(keyPair, contact.getRecipient_identifier(), contact.getShared_aes_key(), messageText)) {
                    // Add message to current chat list
                    messageDAO.insert(message);
                    runOnUiThread(() -> {
                        messageAdapter.add(message);
                        messageAdapter.notifyDataSetChanged();
                        messagesListView.setSelection(messageAdapter.getCount() - 1);
                    });
                }

                final String responseMessage = connector.getResponseMessage();

                runOnUiThread(() -> {
                    // This needs to change to a icon change below the message
                    final Toast toast = Toast.makeText(context, responseMessage, Toast.LENGTH_LONG);
                    toast.show();
                });
            });
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            Bitmap imageBitmap = (Bitmap) extras.get("data");
            // Get the thumbnail back
            //imageView.setImageBitmap(imageBitmap);
        }
    }


    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    // Call the device to open the camera app
    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.bumpchat.bumpchat.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
            }
        }
    }

    private void galleryAddPic() {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File f = new File(currentPhotoPath);
        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        this.sendBroadcast(mediaScanIntent);
    }

    private void setPic() {
        // Get the dimensions of the View
        int targetW = imageView.getWidth();
        int targetH = imageView.getHeight();

        // Get the dimensions of the bitmap
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;

        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;

        // Determine how much to scale down the image
        int scaleFactor = Math.min(photoW/targetW, photoH/targetH);

        // Decode the image file into a Bitmap sized to fill the View
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;
        bmOptions.inPurgeable = true;

        Bitmap bitmap = BitmapFactory.decodeFile(currentPhotoPath, bmOptions);
        imageView.setImageBitmap(bitmap);
    }
}
