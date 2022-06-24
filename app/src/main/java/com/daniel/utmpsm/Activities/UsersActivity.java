package com.daniel.utmpsm.Activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.daniel.utmpsm.Adapters.UsersAdapter;
import com.daniel.utmpsm.Listeners.UserListener;
import com.daniel.utmpsm.Models.User;
import com.daniel.utmpsm.Utilities.Constants;
import com.daniel.utmpsm.Utilities.PreferenceManager;
import com.daniel.utmpsm.databinding.ActivityUsersBinding;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class UsersActivity extends AppCompatActivity implements UserListener {

    private ActivityUsersBinding binding;
    private PreferenceManager preferenceManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityUsersBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        preferenceManager = new PreferenceManager(getApplicationContext());
        setListener();
        getUsers();
    }

    private void setListener() {
        binding.imageBack.setOnClickListener(view -> onBackPressed());
    }

    private void getUsers(){
        showProgressBar();
        FirebaseFirestore firebaseFirestore = FirebaseFirestore.getInstance();
        firebaseFirestore.collection(Constants.KEY_COLLECTION_USERS).get().addOnCompleteListener(task -> {
            String currentUserId = preferenceManager.getString(Constants.KEY_USER_ID);
            if(task.isSuccessful() && task.getResult()!=null){
                hideProgressBar();

                List<User> users = new ArrayList<>();
                for (QueryDocumentSnapshot queryDocumentSnapshot :task.getResult()){
                    if (currentUserId.equals(queryDocumentSnapshot.getId())) {continue;}

                    User user = new User();
                    user.name= queryDocumentSnapshot.getString(Constants.KEY_NAME);
                    user.email = queryDocumentSnapshot.getString(Constants.KEY_EMAIL);
                    user.typeOfUser = queryDocumentSnapshot.getString(Constants.KEY_TYPE_OF_USER);
                    user.token = queryDocumentSnapshot.getString(Constants.KEY_FCM_TOKEN);
                    user.publicKey = queryDocumentSnapshot.getString(Constants.KEY_PUBLIC_KEY);
                    user.id = queryDocumentSnapshot.getId();

                    users.add(user);

                }
                if (users.size() > 0){
                    UsersAdapter usersAdapter = new UsersAdapter(users, this);
                    binding.usersRecyclerView.setAdapter(usersAdapter);
                    binding.usersRecyclerView.setVisibility(View.VISIBLE);
                }
                else{showMessage("no users to show");}
            }
            else{
                showMessage("Error on database");
            }



        });
    }

    private void hideProgressBar(){
        binding.progressBar.setVisibility(View.INVISIBLE);
    }

    private void showProgressBar(){
        binding.progressBar.setVisibility(View.VISIBLE);

    }
    private void showMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    public void onUserClicked(User user){
        Intent intent = new Intent(getApplicationContext(),Chat.class);
        intent.putExtra(Constants.KEY_USER,user);
        startActivity(intent);
        finish();
    }



}