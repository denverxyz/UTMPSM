package com.daniel.utmpsm.Activities;


import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.daniel.utmpsm.Adapters.ChatAdapter;
import com.daniel.utmpsm.EcdhFunction;
import com.daniel.utmpsm.Models.ChatMessage;
import com.daniel.utmpsm.Models.User;
import com.daniel.utmpsm.Utilities.Constants;
import com.daniel.utmpsm.Utilities.PreferenceManager;
import com.daniel.utmpsm.databinding.ActivityChatBinding;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;

import java.io.IOException;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import javax.crypto.SecretKey;

public class Chat extends AppCompatActivity {

    private ActivityChatBinding binding;
    private User receiverUser;
    private List<ChatMessage> chatMessages;
    private ChatAdapter chatAdapter;
    private PreferenceManager preferenceManager;
    private FirebaseFirestore firebaseFirestore;
    private String conversionID = null;
    private String plainText,cipherText;
    private KeyPair keyPair = null;

    private KeyStore keyStore = null;

    private PublicKey publicKeyReceiver;
    private PrivateKey privateKeySender =null;
    private SecretKey secretKey;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        init();
        loadReceiverDetails();

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                setEncryptionMethod();
            }
        }, 5000);


        setListeners();

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                listenMessage();
            }
        }, 5000);



    }



    private void init(){
        preferenceManager = new PreferenceManager(getApplicationContext());
        chatMessages = new ArrayList<>();
        chatAdapter = new ChatAdapter(chatMessages,preferenceManager.getString(Constants.KEY_USER_ID));
        binding.chatRecyclerView.setAdapter(chatAdapter);
        firebaseFirestore = FirebaseFirestore.getInstance();
        receiverUser =(User) getIntent().getSerializableExtra(Constants.KEY_USER);


    }  //end of init method

     private final EventListener<QuerySnapshot> eventListener = (value, error) ->{
        if(error != null){
            return ;
        }
        if (value != null){
            int count = chatMessages.size();
            for (DocumentChange documentChange: value.getDocumentChanges()){
                if (documentChange.getType()== DocumentChange.Type.ADDED){
                    ChatMessage chatMessage = new ChatMessage();
                    chatMessage.senderId = documentChange.getDocument().getString(Constants.KEY_SENDER_ID);
                    chatMessage.receiverId = documentChange.getDocument().getString(Constants.KEY_RECEIVER_ID);
                    //chatMessage.message = documentChange.getDocument().getString(Constants.KEY_MESSAGE);
                    //documentChange.getDocument().getString(Constants.KEY_MESSAGE) is encrypred, decrypt using the sharedkey
                    cipherText = documentChange.getDocument().getString(Constants.KEY_MESSAGE);
                    plainText = EcdhFunction.decryptString(secretKey,cipherText);
                    chatMessage.message = plainText;
                    chatMessage.dateTime = getDate(documentChange.getDocument().getDate(Constants.KEY_TIMESTAMP));
                    chatMessage.dateObject = documentChange.getDocument().getDate(Constants.KEY_TIMESTAMP);
                    chatMessages.add(chatMessage);


                }
            }
            Collections.sort(chatMessages,(obj1,obj2) -> obj1.dateObject.compareTo(obj2.dateObject));
            if (count == 0){
                chatAdapter.notifyDataSetChanged();
            }else {

                chatAdapter.notifyItemRangeChanged(chatMessages.size(),chatMessages.size());
                binding.chatRecyclerView.smoothScrollToPosition(chatMessages.size()-1);
            }
            binding.chatRecyclerView.setVisibility(View.VISIBLE);

        }
        binding.progressBar.setVisibility(View.GONE);
        if (conversionID ==null){
            checkForConversion();
        }
     };

    private void sendMessage(){
        HashMap<String,Object> message = new HashMap<>();
        message.put(Constants.KEY_SENDER_ID,preferenceManager.getString(Constants.KEY_USER_ID));
        message.put(Constants.KEY_RECEIVER_ID,receiverUser.id);

        //
        plainText = binding.inputMessage.getText().toString();

        cipherText = EcdhFunction.encryptString(secretKey,plainText);

        message.put(Constants.KEY_MESSAGE,cipherText);
        message.put(Constants.KEY_TIMESTAMP,new Date());
        firebaseFirestore.collection(Constants.KEY_COLLECTION_CHAT).add(message);

        if (conversionID != null){
            updateConversion(binding.inputMessage.getText().toString());
        } else {
            HashMap<String,Object> conversion = new HashMap<>();
            conversion.put(Constants.KEY_SENDER_ID,preferenceManager.getString(Constants.KEY_USER_ID));
            conversion.put(Constants.KEY_SENDER_NAME,preferenceManager.getString(Constants.KEY_NAME));
            conversion.put(Constants.KEY_RECEIVER_ID,receiverUser.id);
            conversion.put(Constants.KEY_RECEIVER_NAME,receiverUser.name);
            conversion.put(Constants.KEY_LAST_MESSAGE,binding.inputMessage.getText().toString());
            conversion.put(Constants.KEY_TIMESTAMP,new Date());
            addConversion(conversion);

        }
        binding.inputMessage.setText(null);
    }

    private void listenMessage(){

        firebaseFirestore.collection(Constants.KEY_COLLECTION_CHAT)
                .whereEqualTo(Constants.KEY_SENDER_ID,preferenceManager.getString(Constants.KEY_USER_ID))
                .whereEqualTo(Constants.KEY_RECEIVER_ID,receiverUser.id)
                .addSnapshotListener(eventListener);

        firebaseFirestore.collection(Constants.KEY_COLLECTION_CHAT)
                .whereEqualTo(Constants.KEY_SENDER_ID,receiverUser.id)
                .whereEqualTo(Constants.KEY_RECEIVER_ID,preferenceManager.getString(Constants.KEY_USER_ID))
                .addSnapshotListener(eventListener);
    }

    private void setListeners() {
        binding.imageBack.setOnClickListener(view -> onBackPressed());
        binding.layoutSend.setOnClickListener(view -> sendMessage());

    }

    private void loadReceiverDetails() {

        binding.textName.setText(receiverUser.name);

    }


    private String getDate(Date date){
        return new SimpleDateFormat("dd MMMM, yyyy - hh:mm a",Locale.getDefault()).format(date);
    }

    private final OnCompleteListener<QuerySnapshot> conversionOnCompleteListener = task -> {
      if (task.isSuccessful() && task.getResult() != null && task.getResult().getDocuments().size() > 0){
          DocumentSnapshot documentSnapshot = task.getResult().getDocuments().get(0);
          conversionID = documentSnapshot.getId();
      }
    };
    private void setEncryptionMethod() {
        try {
            keyStore = KeyStore.getInstance("AndroidKeyStore");
            keyStore.load(null);
        } catch (CertificateException | NoSuchAlgorithmException | KeyStoreException | IOException e) {
            Log.e("TEST",e.getMessage());
        }


        KeyStore.PrivateKeyEntry privateKeyEntry = null;
        try {
            privateKeyEntry  = (KeyStore.PrivateKeyEntry)keyStore.getEntry("AndroidKeyStore",null);
        } catch (KeyStoreException | NoSuchAlgorithmException | UnrecoverableEntryException e) {
            e.printStackTrace();
        }
        assert privateKeyEntry != null;
        privateKeySender = privateKeyEntry.getPrivateKey();
        Log.e("test",receiverUser.id);
         firebaseFirestore.collection(Constants.KEY_COLLECTION_USERS).document(receiverUser.id);
        publicKeyReceiver = EcdhFunction.getPublicKey(EcdhFunction.hexToBytes(receiverUser.publicKey));
        secretKey = EcdhFunction.generateSharedSecret(privateKeySender,publicKeyReceiver);



    }



    private void checkForConversionRemotely(String senderID,String receiverID){
        firebaseFirestore.collection(Constants.KEY_CONVERSATIONS)
                .whereEqualTo(Constants.KEY_SENDER_ID,senderID)
                .whereEqualTo(Constants.KEY_RECEIVER_ID,receiverID)
                .get()
                .addOnCompleteListener(conversionOnCompleteListener);

    }

    private void addConversion(HashMap<String,Object> conversion){
        firebaseFirestore.collection(Constants.KEY_CONVERSATIONS).add(conversion).addOnSuccessListener(documentReference ->
                conversionID = documentReference.getId());
    }

    private void updateConversion(String message){

        DocumentReference documentReference = firebaseFirestore.collection(Constants.KEY_CONVERSATIONS).document(conversionID);
        documentReference.update(Constants.KEY_LAST_MESSAGE,message,Constants.KEY_TIMESTAMP,new Date());

    }

    private void checkForConversion(){
        if(chatMessages.size() != 0){
            checkForConversionRemotely(preferenceManager.getString(Constants.KEY_USER_ID), receiverUser.id);

        }
        checkForConversionRemotely(receiverUser.id, preferenceManager.getString(Constants.KEY_USER_ID));
    }

    private void showMessage(String message) {

        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();

    }

}