package com.daniel.utmpsm.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.daniel.utmpsm.R;
import com.daniel.utmpsm.Utilities.Constants;
import com.daniel.utmpsm.Utilities.PreferenceManager;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GetTokenResult;
import com.google.firebase.auth.SignInMethodQueryResult;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.annotations.NotNull;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.functions.FirebaseFunctions;
import com.virgilsecurity.android.common.callback.OnGetTokenCallback;
import com.virgilsecurity.android.common.model.EThreeParams;
import com.virgilsecurity.android.ethree.interaction.EThree;
import com.virgilsecurity.common.callback.OnResultListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import kotlin.jvm.functions.Function0;

public class RegisterActivity extends AppCompatActivity {

    private static final String TAG = "test";
    //init variable
    private EditText guestEmail;
    private ProgressBar progressBar;
    private Button guestLoginButton;
    private String defaultPassword="guestpassword";
    private String userID;
    private String fullName;
    private String authTokenUser;
    private FirebaseAuth mAuth;
    private FirebaseFirestore firebaseFirestore;

    private OnGetTokenCallback tokenCallback;
    private PreferenceManager preferenceManager;
    DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReferenceFromUrl("https://utm-psm-default-rtdb.firebaseio.com/");



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

                //hide button and progress
                hideButton();

                //email string get from Editext
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


    private EThree eThreeUser;

    private final Function0<String> getAuthTokenUserOne = new Function0<String>() {

        @Override
        public String invoke() { return getVirgilJwt(authTokenUser); }
    };



    private final com.virgilsecurity.common.callback.OnCompleteListener registerListener = new com.virgilsecurity.common.callback.OnCompleteListener() {

        @Override
        public void onSuccess() {
            Log.i(TAG, "User registered");
        }

        @Override
        public void onError(@NonNull Throwable throwable) {
            Log.e(TAG, "Can't get Virgil token", throwable);

            showMessage(throwable.getMessage());
        }
    };

    OnResultListener<EThree> initializeListener = new OnResultListener<EThree>() {

        @Override public void onSuccess(EThree result) {
            // Init done!
            // Save the eThree instance
            eThreeUser =result;
        }

        @Override public void onError(@NotNull Throwable throwable) {
            // Error handling
        }
    };


    private void handleUserAccount(String email) {
        mAuth.fetchSignInMethodsForEmail(email).addOnCompleteListener(new OnCompleteListener<SignInMethodQueryResult>() {
            @Override
            public void onComplete(@NonNull Task<SignInMethodQueryResult> task) {

                boolean check = !task.getResult().getSignInMethods().isEmpty();


                //not found any email on db
                if(!check){

                    OnResultListener<String> onResultListener = new OnResultListener<String>() {
                        @Override
                        public void onSuccess(String s) {

                            authTokenUser = s;
                            EThreeParams params = new EThreeParams(email, getAuthTokenUserOne, getApplicationContext());
                            eThreeUser = new EThree(params);
                            eThreeUser.register().addCallback(registerListener);

                        }

                        @Override
                        public void onError(@NonNull Throwable throwable) {}
                    };


                    authenticateCreate(email, defaultPassword,onResultListener);





                }


                //found username in db and do sign in method
                else{

                    OnResultListener<String> onResultListener = new OnResultListener<String>() {
                        @Override
                        public void onSuccess(String s) {
                            Log.v(TAG, "onsuccess s is:"+s);
                            authTokenUser = s;
                            EThreeParams params = new EThreeParams(email, getAuthTokenUserOne, getApplicationContext());
                            eThreeUser = new EThree(params);
                            eThreeUser.register().addCallback(registerListener);
                        }

                        @Override
                        public void onError(@NonNull Throwable throwable) {}
                    };


                    authenticateLogIn(email, defaultPassword,onResultListener);

                }  //end else


            }
        });

    }

    private void updateUI() {

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent homeActivity = new Intent(getApplicationContext(),Home.class);
                homeActivity.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);

                startActivity(homeActivity);
            }
        }, 7000);


        finish();
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

    private String getVirgilJwt(String authToken) {
        try {
            String url = "https://us-central1-utm-psm.cloudfunctions.net/getVirgilJwt";
            URL object = new URL(url);

            HttpURLConnection con = (HttpURLConnection) object.openConnection();
            con.setRequestProperty("Authorization", "Bearer " + authToken);
            con.setRequestProperty("Content-Type", "application/json");
            con.setRequestMethod("POST");
            try (OutputStream os = con.getOutputStream()) {
                os.write("{\"data\":{}}".getBytes(StandardCharsets.UTF_8));
            }

            StringBuilder sb = new StringBuilder();
            int HttpResult = con.getResponseCode();
            if (HttpResult == HttpURLConnection.HTTP_OK) {
                BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8));
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line).append("\n");
                }
                br.close();
                JSONObject jsonObject = new JSONObject(sb.toString());

                return jsonObject.getJSONObject("result").getString("token");
            } else {
                Log.e(TAG, "Can't get Virgil token: " + con.getResponseMessage());
                throw new RuntimeException("Some connection error1");
            }
        } catch (IOException exception) {
            Log.e(TAG, "Can't get Virgil token", exception);
            throw new RuntimeException("Some connection error");
        } catch (JSONException e) {
            throw new RuntimeException("Parsing virgil jwt json error");
        }
    }

    private void authenticateCreate(final String identity,
                              String password,
                              final OnResultListener<String> onResultListener) {

        mAuth.createUserWithEmailAndPassword(identity,password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {

                        if (task.isSuccessful()) {
                            Log.i(TAG, "onComplete: test");

                            userID = mAuth.getCurrentUser().getUid();

                            preferenceManager.putBoolean(Constants.KEY_IS_SIGNED_IN,true);
                            preferenceManager.putString(Constants.KEY_USER_ID,userID );
                            preferenceManager.putString(Constants.KEY_NAME,"Anonymous");


                            DocumentReference documentReference1 = firebaseFirestore.collection(Constants.KEY_COLLECTION_USERS).document(userID);
                            Map<String, Object> user = new HashMap<>();
                            user.put(Constants.KEY_NAME, "Anonymous");
                            user.put(Constants.KEY_EMAIL, identity);
                            user.put(Constants.KEY_TYPE_OF_USER, "guest");
                            documentReference1.set(user).addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    if (task.isSuccessful()) {
                                        showMessage("Profile has been saved");
                                        Log.i(TAG, "onComplete: test2");
                                    } else {
                                        showMessage("Profile cannot be saved at the moment...");
                                        Log.i(TAG, "onComplete: test3");
                                    }
                                }
                            });

                            /**for message
                            databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                    databaseReference.child("Users").child(userID).child("Email").setValue(identity);
                                    databaseReference.child("Users").child(userID).child("FullName").setValue("Anonymous");

                                    MemoryData.saveData(identity,RegisterActivity.this);
                                    MemoryData.saveName("Anonymous",RegisterActivity.this);
                                    showMessage(snapshot.toString());

                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {
                                    showMessage(error.getMessage());
                                }
                            });**/


                            Log.i(TAG, "Firebase authentication complete");
                            FirebaseUser firebaseUser = mAuth.getCurrentUser();
                            Log.i(TAG, "firebaseser "+firebaseUser.getUid());

                            firebaseUser.getIdToken(true).addOnCompleteListener(new OnCompleteListener<GetTokenResult>() {
                                @Override
                                public void onComplete(@NonNull Task<GetTokenResult> task) {
                                    if (task.isSuccessful()) {
                                        Log.i(TAG, "ID Token received", task.getException());
                                        onResultListener.onSuccess(task.getResult().getToken());
                                    } else {
                                        Log.e(TAG, "ID Token not received", task.getException());
                                        onResultListener.onError(task.getException());
                                    }
                                }
                            });


                        } else {
                            Log.e(TAG, "Firebase authentication failed", task.getException());
                            onResultListener.onError(task.getException());
                        }
                    }
                });
        updateUI();
    }

    private void authenticateLogIn(final String identity, String password,final OnResultListener<String> onResultListener) {

        mAuth.signInWithEmailAndPassword(identity,password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {

                if (task.isSuccessful()) {
                    Log.i(TAG, "onComplete: test");

                    userID = mAuth.getCurrentUser().getUid();
                    Log.i(TAG, "Firebase authentication complete");
                    FirebaseUser firebaseUser = mAuth.getCurrentUser();
                    Log.i(TAG, "firebaseser "+firebaseUser.getUid());

                    FirebaseFirestore database = FirebaseFirestore.getInstance();
                    database.collection(Constants.KEY_COLLECTION_USERS).whereEqualTo(Constants.KEY_EMAIL,identity).get().addOnCompleteListener(task1 -> {

                        if (task1.isSuccessful() && task1.getResult() != null && task1.getResult().getDocuments().size() >0){
                                DocumentSnapshot documentSnapshot =task1.getResult().getDocuments().get(0);
                                preferenceManager.putBoolean(Constants.KEY_IS_SIGNED_IN,true);
                                preferenceManager.putString(Constants.KEY_USER_ID, documentSnapshot.getId());
                                preferenceManager.putString(Constants.KEY_EMAIL,documentSnapshot.getString(Constants.KEY_EMAIL));
                                preferenceManager.putString(Constants.KEY_NAME,documentSnapshot.getString(Constants.KEY_NAME));


                        }
                        else{
                            Log.i("TAG","Cannot get from database");
                        }

                    });



                    firebaseUser.getIdToken(true).addOnCompleteListener(new OnCompleteListener<GetTokenResult>() {
                        @Override
                        public void onComplete(@NonNull Task<GetTokenResult> task) {
                            if (task.isSuccessful()) {
                                Log.i(TAG, "ID Token received", task.getException());
                                onResultListener.onSuccess(task.getResult().getToken());
                            } else {
                                Log.e(TAG, "ID Token not received", task.getException());
                                onResultListener.onError(task.getException());
                            }
                        }
                    });


                } else {
                    Log.e(TAG, "Firebase authentication failed", task.getException());
                    onResultListener.onError(task.getException());
                }
            }
        });

        updateUI();

    }





}