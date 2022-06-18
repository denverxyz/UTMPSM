package com.daniel.utmpsm.List;

public class MessagesList {

    private String name, lastMessage, chatKey;
    private int unseenMessages;

    public MessagesList(String name, String lastMessage, int unseenMessages, String chatKey) {
        this.name = name;
        this.lastMessage = lastMessage;
        this.unseenMessages = unseenMessages;
        this.chatKey = chatKey;
    }

    public String getName() {
        return name;
    }


    public String getLastMessage() {
        return lastMessage;
    }


    public int getUnseenMessages() {
        return unseenMessages;
    }

    public String getChatKey() {
        return chatKey;
    }
}
