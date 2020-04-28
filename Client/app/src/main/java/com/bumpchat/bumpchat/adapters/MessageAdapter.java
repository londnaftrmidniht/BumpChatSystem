package com.bumpchat.bumpchat.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumpchat.bumpchat.R;
import com.bumpchat.bumpchat.models.ContactOverview;
import com.bumpchat.bumpchat.models.Message;

import java.util.ArrayList;

public class MessageAdapter extends ArrayAdapter<Message> implements View.OnClickListener {
    // View lookup cache
    private static class ViewHolder {
        TextView txtMessage;
        TextView txtMessageTime;
    }

    public MessageAdapter(ArrayList<Message> data, Context context) {
        super(context, R.layout.chat_message_item, data);
    }

    @Override
    public void onClick(View v) {

        int position=(Integer) v.getTag();
        Message message = getItem(position);

        // Call activity to load single chat
    }


    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Get the data item for this position
        Message message = getItem(position);
        // Check if an existing view is being reused, otherwise inflate the view
        ViewHolder viewHolder; // view lookup cache stored in tag

        viewHolder = new ViewHolder();
        LayoutInflater inflater = LayoutInflater.from(getContext());

        if (message.getIncoming()) {
            convertView = inflater.inflate(R.layout.message_incoming, parent, false);
        } else {
            convertView = inflater.inflate(R.layout.message_outgoing, parent, false);
        }

        viewHolder.txtMessage = convertView.findViewById(R.id.message_body);
        viewHolder.txtMessageTime = convertView.findViewById(R.id.message_time);

        convertView.setTag(viewHolder);

        CharSequence messageDate = android.text.format.DateUtils.getRelativeTimeSpanString(message.getDate());
        viewHolder.txtMessage.setText(message.getMessage());
        viewHolder.txtMessageTime.setText(messageDate);

        // Return the completed view to render on screen
        return convertView;
    }
}