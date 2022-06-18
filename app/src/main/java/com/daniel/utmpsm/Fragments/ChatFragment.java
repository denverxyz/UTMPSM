package com.daniel.utmpsm.Fragments;

import android.app.ProgressDialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.daniel.utmpsm.Activities.MemoryData;
import com.daniel.utmpsm.List.MessagesList;
import com.daniel.utmpsm.Adapters.MessageAdapter;
import com.daniel.utmpsm.R;
import com.daniel.utmpsm.Utilities.Constants;
import com.daniel.utmpsm.Utilities.PreferenceManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.ArrayList;
import java.util.List;

public class ChatFragment extends Fragment {

    private String email;
    private String name;
    private RecyclerView messageRecyclerView;
    private MessageAdapter messagesAdapter;
    private final List<MessagesList> messagesLists = new ArrayList<>();
    private int unseenMessages = 0;
    private String lastMessage = "";
    private String chatKey = "";
    private FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
    private FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
    PreferenceManager preferenceManager;


    private boolean dataSet = false;
    DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReferenceFromUrl("https://utm-psm-default-rtdb.firebaseio.com/");

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View privateChatView = inflater.inflate(R.layout.fragment_chat, container, false);
        messageRecyclerView = privateChatView.findViewById(R.id.messageRecyclerView);
        messageRecyclerView.setLayoutManager(new LinearLayoutManager(privateChatView.getContext()));

        messageRecyclerView.setHasFixedSize(true);


        messagesAdapter = new MessageAdapter(messagesLists,privateChatView.getContext());
        messageRecyclerView.setAdapter(messagesAdapter);
        preferenceManager = new PreferenceManager(privateChatView.getContext());

        getToken();

        databaseReference.addValueEventListener(new ValueEventListener() {
            private static final String TAG = "TAG" ;

            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                messagesLists.clear();
                unseenMessages = 0;
                lastMessage = "";
                chatKey = "";

                for(DataSnapshot dataSnapshot : snapshot.child("Users").getChildren()){

                    final String getUid = dataSnapshot.getKey();

                    dataSet = false;



                    if(!getUid.equals(firebaseUser.getUid())){

                        final String getName = dataSnapshot.child("FullName").getValue(String.class);
                        //final String getProfilePic = dataSnapshot.child("profile_pic").getValue(String.class);

                        databaseReference.child("chat").addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {

                                int getChatCounts = (int)snapshot.getChildrenCount();

                                if(getChatCounts > 0){

                                    for (DataSnapshot dataSnapshot1 : snapshot.getChildren()){

                                        final String getKey = dataSnapshot1.getKey();
                                        chatKey = getKey;

                                        if(dataSnapshot1.hasChild("user_1") && dataSnapshot1.hasChild("user_2") && dataSnapshot1.hasChild("messages")){
                                            final String getUserOne = dataSnapshot1.child("user_1").getValue(String.class);
                                            final String getUserTwo = dataSnapshot1.child("user_2").getValue(String.class);

                                            if((getUserOne.equals(getUid) && getUserTwo.equals(firebaseUser.getUid())) || (getUserOne.equals(firebaseUser.getUid()) && getUserTwo.equals(getUid))){

                                                for(DataSnapshot chatDataSnapshot : dataSnapshot1.child("messages").getChildren()){

                                                    final long getMessageKey = Long.parseLong(chatDataSnapshot.getKey());
                                                    final long getLastSeenMessage = Long.parseLong(MemoryData.getLastMsgTS(getActivity(), getKey));

                                                    lastMessage = chatDataSnapshot.child("msg").getValue(String.class);
                                                    if(getMessageKey > getLastSeenMessage){
                                                        unseenMessages++;
                                                    }

                                                }
                                            }
                                        }
                                    }
                                }

                                MessagesList messagesList = new MessagesList(getName,  lastMessage,unseenMessages, chatKey);
                                messagesLists.add(messagesList);
                                messagesAdapter.updateData(messagesLists);

                                if(!dataSet){
                                    Log.v(TAG,"snapshot count = "+dataSet);
                                    dataSet = true;
                                    //MessagesList messagesList = new MessagesList(getName,  lastMessage,unseenMessages, chatKey);
                                   // messagesLists.add(messagesList);
                                   // messagesAdapter.updateData(messagesLists);
                                }

                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {

                            }
                        });

                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });





        return privateChatView;

    }

    private void updateToken(String token){
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        DocumentReference documentReference = database.collection(Constants.KEY_COLLECTION_USERS).document(preferenceManager.getString(Constants.KEY_USER_ID));

        documentReference.update(Constants.KEY_FCM_TOKEN,token).addOnSuccessListener(unused -> showMessage("Token Updated Successfully")).addOnFailureListener(e -> showMessage("Unable to update token"));
    }

    private void getToken(){
        FirebaseMessaging.getInstance().getToken().addOnSuccessListener(this::updateToken);
    }

    private void showMessage(String message) {

        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();

    }
}
