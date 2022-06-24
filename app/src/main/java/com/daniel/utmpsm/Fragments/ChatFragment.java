package com.daniel.utmpsm.Fragments;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.daniel.utmpsm.Activities.Chat;
import com.daniel.utmpsm.Activities.UsersActivity;
import com.daniel.utmpsm.Adapters.RecentConversionsAdapter;
import com.daniel.utmpsm.Listeners.ConversionListener;
import com.daniel.utmpsm.Models.ChatMessage;
import com.daniel.utmpsm.Models.User;
import com.daniel.utmpsm.Utilities.Constants;
import com.daniel.utmpsm.Utilities.PreferenceManager;
import com.daniel.utmpsm.databinding.FragmentChatBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ChatFragment extends Fragment implements ConversionListener {


    private FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
    private FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
    private FragmentChatBinding binding;
    PreferenceManager preferenceManager;
    private List<ChatMessage> conversations;
    private RecentConversionsAdapter conversionsAdapter;
    private FirebaseFirestore database;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment

        binding = FragmentChatBinding.inflate(inflater,container,false);
        View view = binding.getRoot();
        preferenceManager = new PreferenceManager(view.getContext());
        init();
        getToken();
        setListeners();
        listenConversation();

        return view;

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void setListeners() {
        binding.fabNewChat.setOnClickListener(view -> startActivity(new Intent(getActivity().getApplicationContext(), UsersActivity.class)));
    }

    private void listenConversation(){
        database.collection(Constants.KEY_CONVERSATIONS)
                .whereEqualTo(Constants.KEY_SENDER_ID,preferenceManager.getString(Constants.KEY_USER_ID))
                .addSnapshotListener(eventListener);

        database.collection(Constants.KEY_CONVERSATIONS)
                .whereEqualTo(Constants.KEY_RECEIVER_ID,preferenceManager.getString(Constants.KEY_USER_ID))
                .addSnapshotListener(eventListener);
    }

    private void init(){
        conversations = new ArrayList<>();
        conversionsAdapter = new RecentConversionsAdapter(conversations, this);
        binding.conversationsRecyclerView.setAdapter(conversionsAdapter);
        database = FirebaseFirestore.getInstance();
    }

    private void updateToken(String token){
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        DocumentReference documentReference = database.collection(Constants.KEY_COLLECTION_USERS).document(preferenceManager.getString(Constants.KEY_USER_ID));

        documentReference.update(Constants.KEY_FCM_TOKEN,token).addOnSuccessListener(unused -> showMessage("Token Updated Successfully")).addOnFailureListener(e -> showMessage("Unable to update token"));
    }

    private final EventListener<QuerySnapshot> eventListener =  (value, error) -> {
        if(error != null){
            showMessage(error.getMessage());
        }
        if (value != null){
            for (DocumentChange documentChange : value.getDocumentChanges()){
                if (documentChange.getType() == DocumentChange.Type.ADDED){
                    String senderID = documentChange.getDocument().getString(Constants.KEY_SENDER_ID);
                    String receiverID = documentChange.getDocument().getString(Constants.KEY_RECEIVER_ID);

                    ChatMessage chatMessage = new ChatMessage();
                    chatMessage.senderId = senderID;
                    chatMessage.receiverId = receiverID;

                    if (preferenceManager.getString(Constants.KEY_USER_ID).equals(senderID)){
                        chatMessage.conversionID = documentChange.getDocument().getString(Constants.KEY_RECEIVER_ID);
                        chatMessage.conversionName = documentChange.getDocument().getString(Constants.KEY_RECEIVER_NAME);

                    } else {
                        chatMessage.conversionID = documentChange.getDocument().getString(Constants.KEY_SENDER_ID);
                        chatMessage.conversionName = documentChange.getDocument().getString(Constants.KEY_SENDER_NAME);

                    }

                    chatMessage.message = documentChange.getDocument().getString(Constants.KEY_LAST_MESSAGE);
                    chatMessage.dateObject = documentChange.getDocument().getDate(Constants.KEY_TIMESTAMP);
                    conversations.add(chatMessage);

                }else if (documentChange.getType() == DocumentChange.Type.MODIFIED) {
                    for (int i=0;i<conversations.size();i++){
                        String senderID = documentChange.getDocument().getString(Constants.KEY_SENDER_ID);
                        String receiverID = documentChange.getDocument().getString(Constants.KEY_RECEIVER_ID);
                        if (conversations.get(i).senderId.equals(senderID) && conversations.get(i).receiverId.equals(receiverID)){
                            conversations.get(i).message = documentChange.getDocument().getString(Constants.KEY_LAST_MESSAGE);
                            conversations.get(i).dateObject = documentChange.getDocument().getDate(Constants.KEY_TIMESTAMP);
                            break;

                        }

                    }

                }
            }
            binding.conversationsRecyclerView.setAdapter(conversionsAdapter);
            Collections.sort(conversations,(obj1, obj2) -> obj2.dateObject.compareTo(obj1.dateObject));
            conversionsAdapter.notifyDataSetChanged();
            binding.conversationsRecyclerView.smoothScrollToPosition(0);



        }

    };

    private void getToken(){
        FirebaseMessaging.getInstance().getToken().addOnSuccessListener(this::updateToken);
    }

    private void showMessage(String message) {

        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();

    }
    public void onConversionClicked(User user){
        binding.progressBar.setVisibility(View.VISIBLE);
        new Handler().postDelayed(new Runnable() {
            @Override

            public void run() {

                Intent intent = new Intent(getActivity(), Chat.class);
                intent.putExtra(Constants.KEY_USER,user);
                startActivity(intent);

            }
        }, 5000);

        binding.progressBar.setVisibility(View.GONE);


    }
}
