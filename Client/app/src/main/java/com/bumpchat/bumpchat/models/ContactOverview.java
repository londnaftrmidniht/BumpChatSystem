package com.bumpchat.bumpchat.models;

import androidx.room.DatabaseView;

@DatabaseView("SELECT " +
              "  c.inbox_identifier, " +
              "  c.name, " +
              "  MAX(m.message_id) AS lastMessageId, " +
              "  (SELECT message FROM messages WHERE message_id = m.message_id) AS lastMessage, " +
              "  (SELECT date FROM messages WHERE message_id = m.message_id) AS lastMessageDate, " +
              "  (SELECT COUNT(*) FROM messages WHERE inbox_identifier = c.inbox_identifier AND read = 0) AS unreadMessages " +
              "FROM " +
              "  messages m " +
              "INNER JOIN " +
              "  contacts c ON m.inbox_identifier = c.inbox_identifier " +
              "GROUP BY " +
              "  m.inbox_identifier " +
              "ORDER BY " +
              "  m.message_id DESC")
public class ContactOverview {
    public String inbox_identifier;
    public String name;
    public long lastMessageId;
    public String lastMessage;
    public long lastMessageDate;
    public int unreadMessages;

    public String getInbox_identifier() {
        return this.inbox_identifier;
    }

    public String getName() {
        return this.name;
    }

    public long getLastMessageId() {
        return this.lastMessageId;
    }

    public String getLastMessage() {
        return this.lastMessage;
    }

    public long getLastMessageDate() {
        return this.lastMessageDate;
    }

    public int getUnreadMessages() {
        return this.unreadMessages;
    }
}