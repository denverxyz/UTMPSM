package com.daniel.utmpsm.Adapters;


import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.daniel.utmpsm.Models.ChatMessage;
import com.daniel.utmpsm.databinding.ItemContainerReceivedMessageBinding;
import com.daniel.utmpsm.databinding.ItemContainterSentMessageBinding;

import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final List<ChatMessage> chatMessages;
    private final String senderId;

    public static final int VIEW_TYPE_SENT = 1;
    public static final int VIEW_TYPE_RECEIVED = 2;

    public ChatAdapter(List<ChatMessage> chatMessages, String senderId) {
        this.chatMessages = chatMessages;
        this.senderId = senderId;
    }



    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_SENT){
            return new SentMessageViewHolder(ItemContainterSentMessageBinding.inflate
                    (LayoutInflater.from(parent.getContext()),parent,false));
        }else {
            return new ReceivedMessageViewHolder(ItemContainerReceivedMessageBinding.inflate
                    (LayoutInflater.from(parent.getContext()),parent,false));

        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (getItemViewType(position) == VIEW_TYPE_SENT){
            ((SentMessageViewHolder)holder).setData(chatMessages.get(position));

        }
        else {
            ((ReceivedMessageViewHolder)holder).setData(chatMessages.get(position));
        }
    }

    @Override
    public int getItemCount() {
        return chatMessages.size();
    }

    @Override
    public int getItemViewType(int position) {
        if(chatMessages.get(position).senderId.equals(senderId)){return VIEW_TYPE_SENT;}
        else {return VIEW_TYPE_RECEIVED;}
    }

    static class SentMessageViewHolder extends RecyclerView.ViewHolder{

        private final ItemContainterSentMessageBinding binding;


        public SentMessageViewHolder(ItemContainterSentMessageBinding itemContainterSentMessageBinding) {
            super(itemContainterSentMessageBinding.getRoot());
            binding = itemContainterSentMessageBinding;
        }

        void setData(ChatMessage chatMessage){
            binding.textMessage.setText(chatMessage.message);
            binding.textDateTime.setText(chatMessage.dateTime);
        }
    }

    static class ReceivedMessageViewHolder extends RecyclerView.ViewHolder{

        private final ItemContainerReceivedMessageBinding binding;

        public ReceivedMessageViewHolder(ItemContainerReceivedMessageBinding itemContainerReceivedMessageBinding) {
            super(itemContainerReceivedMessageBinding.getRoot());
            binding = itemContainerReceivedMessageBinding;
        }

        void setData(ChatMessage chatMessage){
            binding.textMessage.setText(chatMessage.message);
            binding.textDateTime.setText(chatMessage.dateTime);
        }
    }

}
