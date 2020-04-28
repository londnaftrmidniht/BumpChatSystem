package com.bumpchat.bumpchat.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.bumpchat.bumpchat.R;
import com.bumpchat.bumpchat.models.ContactOverview;

import java.util.ArrayList;

public class ContactMessageAdapter extends ArrayAdapter<ContactOverview> implements View.OnClickListener {

    private ArrayList<ContactOverview> dataSet;
    private int lastPosition = -1;
    private Context mContext;

    // View lookup cache
    private static class ViewHolder {
        TextView txtName;
//        TextView txtUnreadCount;
        TextView txtLastMessage;
        TextView txtLastMessageTime;
//        ImageView info;
    }

    public ContactMessageAdapter(ArrayList<ContactOverview> data, Context context) {
        super(context, R.layout.chat_selection_item, data);
        this.dataSet = data;
        this.mContext=context;
    }

    @Override
    public void onClick(View v) {

        int position=(Integer) v.getTag();
        ContactOverview contactMessage = getItem(position);

        // Call activity to load single chat
    }


    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ContactOverview contactMessage = getItem(position);
        ViewHolder viewHolder;

        if (convertView == null) {

            viewHolder = new ViewHolder();
            LayoutInflater inflater = LayoutInflater.from(getContext());
            convertView = inflater.inflate(R.layout.chat_selection_item, parent, false);
            viewHolder.txtName = convertView.findViewById(R.id.selection_contact_name);
//            viewHolder.txtUnreadCount = convertView.findViewById(R.id.selection_contact_unread_count);
            viewHolder.txtLastMessage = convertView.findViewById(R.id.selection_contact_last_message);
            viewHolder.txtLastMessageTime = convertView.findViewById(R.id.selection_contact_last_message_time);
            //viewHolder.info = convertView.findViewById(R.id.item_info);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
            // Reset color to white on redraw
            convertView.setBackgroundColor(ContextCompat.getColor(mContext, android.R.color.white));
        }

        viewHolder.txtName.setText(contactMessage.getName());
//        viewHolder.txtUnreadCount.setText(Integer.toString(contactMessage.getUnreadMessages()));

        CharSequence lastMessageDate = android.text.format.DateUtils.getRelativeTimeSpanString(contactMessage.getLastMessageDate());
        String lastMessage = contactMessage.getLastMessage();
        if (lastMessage.length() > 250) {
            lastMessage = lastMessage.substring(0, 250);
        }

        viewHolder.txtLastMessage.setText(lastMessage);
        viewHolder.txtLastMessageTime.setText(lastMessageDate);
        //viewHolder.info.setOnClickListener(this);
        //viewHolder.info.setTag(position);
        // Return the completed view to render on screen
        return convertView;
    }
}