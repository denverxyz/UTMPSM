package com.daniel.utmpsm.Fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.daniel.utmpsm.R;
import com.daniel.utmpsm.Utilities.Constants;
import com.daniel.utmpsm.Utilities.PreferenceManager;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;

/**

 */
public class ProfileFragment extends Fragment  {

     EditText profileFullName;
     Button profileUpdateButton;
     TextView profileEmail;
     FirebaseAuth firebaseAuth;
     FirebaseFirestore firebaseFirestore;
     String userID,updatedFullName;

    private PreferenceManager preferenceManager;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

       View view = inflater.inflate(R.layout.fragment_profile, container, false);

        firebaseAuth = FirebaseAuth.getInstance();
        firebaseFirestore = FirebaseFirestore.getInstance();
        userID = firebaseAuth.getCurrentUser().getUid();
        profileFullName = view.findViewById(R.id.profileFullName);
        profileEmail = view.findViewById(R.id.profileEmail);
        profileUpdateButton = view.findViewById(R.id.profileUpdateButton);
        preferenceManager = new PreferenceManager(getActivity().getApplicationContext());
        SharedPreferences preferences = this.getActivity().getSharedPreferences(Constants.KEY_PREFERENCE_NAME, Context.MODE_PRIVATE);


        DocumentReference documentReference = firebaseFirestore.collection("Users").document(userID);
        documentReference.addSnapshotListener( new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot value, @Nullable FirebaseFirestoreException error) {
                profileEmail.setText(value.getString("Email"));
                profileFullName.setText(value.getString("FullName"));
            }
        });

        profileUpdateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                updatedFullName = profileFullName.getText().toString();
                updateData(profileEmail.getText().toString(),updatedFullName);

            }
        });


        // Inflate the layout for this fragment
        return view;

    }

    public void updateData(String email,String fullName){
        Map<String,Object> userDetail = new HashMap<>();
        userDetail.put("FullName",fullName);

        firebaseFirestore.collection("Users").whereEqualTo("Email",email).get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()){
                    DocumentSnapshot documentSnapshot =task.getResult().getDocuments().get(0);
                    String documentID = documentSnapshot.getId();
                    firebaseFirestore.collection("Users").document(documentID).update(userDetail).addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void unused) {
                            preferenceManager.putString(Constants.KEY_NAME,fullName);
                            showMessage("Successfully updated");
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            showMessage("Failed :"+e.getMessage());
                        }
                    });

                }else{

                    showMessage("Failed :"+ task.getException().toString());
                }
            }
        });

    }

    private void showMessage(String message) {

        Toast.makeText(getContext(),message, Toast.LENGTH_SHORT).show();

    }


}