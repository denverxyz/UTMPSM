package com.daniel.utmpsm.List;

public class ChatList {

    private String email, name, message, date, time;

    public ChatList(String email, String name, String message, String date, String time) {
        this.email = email;
        this.name = name;
        this.message = message;
        this.date = date;
        this.time = time;
    }

    public String getEmail() {
        return email;
    }

    public String getName() {
        return name;
    }

    public String getMessage() {
        return message;
    }

    public String getDate() {
        return date;
    }

    public String getTime() {
        return time;
    }
}
