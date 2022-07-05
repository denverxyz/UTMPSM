package com.daniel.utmpsm.Activities;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.daniel.utmpsm.EcdhFunction;
import com.daniel.utmpsm.R;
import com.daniel.utmpsm.Utilities.Constants;
import com.daniel.utmpsm.Utilities.PreferenceManager;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.SignInMethodQueryResult;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;


import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.ECPublicKey;

import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private static final String TAG = "test";
    //init variable
    private EditText guestEmail;
    private ProgressBar progressBar;
    private Button guestLoginButton;
    private String defaultPassword="guestpassword";
    private String userID;
    private FirebaseAuth mAuth;
    private FirebaseFirestore firebaseFirestore;
    private PreferenceManager preferenceManager;
    private KeyPair keyPair;
    PublicKey publicKey ;
    PrivateKey privateKey ;
    PublicKey pkReconstructed ;
    PrivateKey skReconstructed;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_register);

        //set var with view
        guestEmail = findViewById(R.id.guestEmail);
        progressBar = findViewById(R.id.progressBar);
        guestLoginButton =findViewById(R.id.guestLoginButton);

        //firebase and get instance
        mAuth = FirebaseAuth.getInstance();
        firebaseFirestore = FirebaseFirestore.getInstance();

        //hide view first
        progressBar.setVisibility(View.INVISIBLE);

        preferenceManager = new PreferenceManager(getApplicationContext());


        //run when click login button

        guestLoginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                hideButton();
                final String email = guestEmail.getText().toString();

                //if email empty
                if(email.isEmpty()){

                    //send showMessage function
                    showMessage("please fill up form");
                    showButton();
                }

                else{
                            handleUserAccount(email);
                }

            }
        });

    }


    private void handleUserAccount(String email) {
        mAuth.fetchSignInMethodsForEmail(email).addOnCompleteListener(new OnCompleteListener<SignInMethodQueryResult>() {
            @Override
            public void onComplete(@NonNull Task<SignInMethodQueryResult> task) {

                boolean check = !task.getResult().getSignInMethods().isEmpty();


                //not found any email on db
                if(!check){


                    mAuth.createUserWithEmailAndPassword(email,defaultPassword).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @RequiresApi(api = Build.VERSION_CODES.S)
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {

                            if (task.isSuccessful()) {
                                Log.i(TAG, "onComplete: test");

                                userID = mAuth.getCurrentUser().getUid();

                                keyPair = EcdhFunction.generateKeyPair();


                                publicKey = keyPair.getPublic();
                                privateKey = keyPair.getPrivate();

                                String publicKeyHex = EcdhFunction.bytesToHex(publicKey.getEncoded());

                                DocumentReference documentReference1 = firebaseFirestore.collection(Constants.KEY_COLLECTION_USERS).document(userID);
                                Map<String, Object> user = new HashMap<>();
                                user.put(Constants.KEY_NAME, "Anonymous");
                                user.put(Constants.KEY_EMAIL, email);
                                user.put(Constants.KEY_TYPE_OF_USER, "guest");
                                user.put(Constants.KEY_PUBLIC_KEY,publicKeyHex);
                                //user.put(Constants.KEY_PUBLIC_KEY,privateKey.getEncoded());
                                documentReference1.set(user).addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        if (task.isSuccessful()) {
                                            showMessage("Profile has been saved");
                                            Log.i(TAG, "onComplete: test2");
                                        } else {
                                            showMessage("Profile cannot be saved at the moment...");

                                        }
                                    }
                                });



                                preferenceManager.putBoolean(Constants.KEY_IS_SIGNED_IN,true);
                                preferenceManager.putString(Constants.KEY_USER_ID,userID);
                                preferenceManager.putString(Constants.KEY_EMAIL,email);
                                preferenceManager.putString(Constants.KEY_NAME,"Anonymous");
                                preferenceManager.putString(Constants.KEY_PUBLIC_KEY,publicKeyHex);

                            } else {
                                Log.e(TAG, "Firebase authentication failed", task.getException());
                            }
                        }
                    });

                    updateUI();

                    
                }


                //found username in db and do sign in method
                else{

                    mAuth.signInWithEmailAndPassword(email,defaultPassword).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {

                            //EThree.initialize(getApplicationContext(), tokenCallback).addCallback((initializeListener.onSuccess(eThree););


                            if (task.isSuccessful()) {
                                Log.i(TAG, "onComplete: test");

                                userID = mAuth.getCurrentUser().getUid();
                                Log.i(TAG, "Firebase authentication complete");
                                FirebaseUser firebaseUser = mAuth.getCurrentUser();
                                Log.i(TAG, "firebaseser "+firebaseUser.getUid());

                                FirebaseFirestore database = FirebaseFirestore.getInstance();
                                database.collection(Constants.KEY_COLLECTION_USERS).whereEqualTo(Constants.KEY_EMAIL,email).get().addOnCompleteListener(task1 -> {

                                    if (task1.isSuccessful() && task1.getResult() != null && task1.getResult().getDocuments().size() >0){
                                        DocumentSnapshot documentSnapshot =task1.getResult().getDocuments().get(0);
                                        preferenceManager.putBoolean(Constants.KEY_IS_SIGNED_IN,true);
                                        preferenceManager.putString(Constants.KEY_USER_ID, documentSnapshot.getId());
                                        preferenceManager.putString(Constants.KEY_EMAIL,documentSnapshot.getString(Constants.KEY_EMAIL));
                                        preferenceManager.putString(Constants.KEY_NAME,documentSnapshot.getString(Constants.KEY_NAME));
                                        preferenceManager.putString(Constants.KEY_PUBLIC_KEY,documentSnapshot.getString(Constants.KEY_PUBLIC_KEY));
                                        //preferenceManager.putString(Constants.KEY_PRIVATE_KEY,documentSnapshot.getString(Constants.KEY_PRIVATE_KEY));

                                    }
                                    else{
                                        Log.i("TAG","Cannot get from database");
                                    }

                                });

                            } else {
                                Log.e(TAG, "Firebase authentication failed", task.getException());

                            }
                        }
                    });

                    updateUI();

                }  //end else

            }
        });

    }

    private void updateUI() {

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {

                Intent homeActivity = new Intent(getApplicationContext(),Home.class);
                homeActivity.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP);

                startActivity(homeActivity);
                finishAffinity();
            }
        }, 5000);


    }

    private void hideButton(){
        guestLoginButton.setVisibility(View.INVISIBLE);
        progressBar.setVisibility(View.VISIBLE);
    }

    private void showButton(){
        guestLoginButton.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.INVISIBLE);
    }

    private void showMessage(String message) {

        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        
    }







}