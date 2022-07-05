package com.daniel.utmpsm.Adapters;

import android.os.Handler;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.daniel.utmpsm.Listeners.ConversionListener;
import com.daniel.utmpsm.Models.ChatMessage;
import com.daniel.utmpsm.Models.User;
import com.daniel.utmpsm.Utilities.Constants;
import com.daniel.utmpsm.databinding.ItemContainerRecentConversionBinding;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;

import java.util.List;

public class RecentConversionsAdapter extends  RecyclerView.Adapter<RecentConversionsAdapter.ConversionViewHolder>{

    private final List<ChatMessage> chatMessages;
    private final ConversionListener conversionListener;

    public RecentConversionsAdapter(List<ChatMessage> chatMessages, ConversionListener conversionListener) {
        this.chatMessages = chatMessages;
        this.conversionListener = conversionListener;
    }

    @NonNull
    @Override
    public ConversionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        ItemContainerRecentConversionBinding itemContainerRecentConversionBinding =  ItemContainerRecentConversionBinding.inflate(LayoutInflater.from(parent.getContext()),parent,false);
        return new ConversionViewHolder(itemContainerRecentConversionBinding);
    }

    @Override
    public void onBindViewHolder(@NonNull ConversionViewHolder holder, int position) {
        holder.setData(chatMessages.get(position));
    }

    @Override
    public int getItemCount() {
        return chatMessages.size();
    }

    class ConversionViewHolder extends RecyclerView.ViewHolder{

        ItemContainerRecentConversionBinding binding;



        ConversionViewHolder(ItemContainerRecentConversionBinding itemContainerRecentConversionBinding){
            super(itemContainerRecentConversionBinding.getRoot());
            binding = itemContainerRecentConversionBinding;
        }

        void setData(ChatMessage chatMessage){
            binding.textName.setText(chatMessage.conversionName);
            binding.textRecentMessage.setText(chatMessage.message);

            binding.getRoot().setOnClickListener(view -> {
                User user = new User();
                user.id= chatMessage.conversionID;
                user.name = chatMessage.conversionName;
                conversionListener.onConversionClicked(user);


            });
        }
    }
}
