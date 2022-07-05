package com.daniel.utmpsm.Activities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.daniel.utmpsm.EcdhFunction;
import com.daniel.utmpsm.R;
import com.daniel.utmpsm.Utilities.Constants;
import com.daniel.utmpsm.Utilities.PreferenceManager;
import com.daniel.utmpsm.databinding.ActivityLoginBinding;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInOptionsExtension;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {

    private static final int RC_SIGN_IN = 123;
    GoogleSignInClient mGoogleSignInClient;
    private FirebaseAuth mAuth;
    private PreferenceManager preferenceManager;
    private FirebaseFirestore firebaseFirestore;
    private KeyPair keyPair;
    GoogleSignInOptions gso;
    PublicKey publicKey ;
    PrivateKey privateKey;
    String publicKeyHex ;
    private ActivityLoginBinding binding;

    @Override
    protected void onStart() {
        super.onStart();

        FirebaseUser user = mAuth.getCurrentUser();
        if(user!=null){
            Intent intent = new Intent(LoginActivity.this,Home.class);
            startActivity(intent);

        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        preferenceManager = new PreferenceManager(getApplicationContext());
        //getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(binding.getRoot());
        init();
        setListener();
    }


    public void init(){
        // Configure sign-in to request the user's ID, email address, and basic
        // profile. ID and basic profile are included in DEFAULT_SIGN_IN.
         gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                 .requestEmail()
                 .requestIdToken(getString(R.string.default_web_client_id))
                 .build();

        // Build a GoogleSignInClient with the options specified by gso.
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
        mAuth = FirebaseAuth.getInstance();
        firebaseFirestore = FirebaseFirestore.getInstance();
    }

    public void setListener(){
        binding.gsignin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                signIn();
            }
        });
    }

    private void signIn() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == RC_SIGN_IN) {

                Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
                try {
                    GoogleSignInAccount account  = task.getResult(ApiException.class);
                    firebaseAuthWithGoogle(account);

                } catch (ApiException e) {
                    Log.e("Test",e.getMessage());
                }
            
        }
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {

        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);


        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        binding.loginProgressBar.setVisibility(View.VISIBLE);

                        // If sign in fails, display a message to the user. If sign in succeeds
                        // the auth state listener will be notified and logic to handle the
                        // signed in user can be handled in the listener.
                        if (!task.isSuccessful()) {

                            Toast.makeText(LoginActivity.this, "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();

                        }else {

                            boolean newuser = task.getResult().getAdditionalUserInfo().isNewUser();

                            if(newuser){


                                    keyPair = EcdhFunction.generateKeyPair();


                                publicKey = keyPair.getPublic();
                                privateKey = keyPair.getPrivate();
                                publicKeyHex  = EcdhFunction.bytesToHex(publicKey.getEncoded());


                                DocumentReference documentReference1 = firebaseFirestore.collection(Constants.KEY_COLLECTION_USERS).document(mAuth.getCurrentUser().getUid());
                                Map<String, Object> user = new HashMap<>();
                                user.put(Constants.KEY_NAME, acct.getDisplayName());
                                user.put(Constants.KEY_EMAIL, acct.getEmail());
                                user.put(Constants.KEY_TYPE_OF_USER, "guest");
                                user.put(Constants.KEY_PUBLIC_KEY,publicKeyHex);
                                preferenceManager.putBoolean(Constants.KEY_IS_SIGNED_IN,true);
                                preferenceManager.putString(Constants.KEY_USER_ID,mAuth.getCurrentUser().getUid());
                                preferenceManager.putString(Constants.KEY_EMAIL,acct.getEmail());
                                preferenceManager.putString(Constants.KEY_NAME,acct.getDisplayName());
                                preferenceManager.putString(Constants.KEY_PUBLIC_KEY,publicKeyHex);

                                documentReference1.set(user).addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        binding.loginProgressBar.setVisibility(View.GONE);
                                        Intent homeActivity = new Intent(getApplicationContext(),Home.class);
                                        startActivity(homeActivity);
                                    }
                                });

                            }else{
                                //Continue with Sign in
                                FirebaseFirestore database = FirebaseFirestore.getInstance();
                                database.collection(Constants.KEY_COLLECTION_USERS).whereEqualTo(Constants.KEY_EMAIL,acct.getEmail()).get().addOnCompleteListener(task1 -> {

                                    if (task1.isSuccessful() && task1.getResult() != null && task1.getResult().getDocuments().size() >0){
                                        DocumentSnapshot documentSnapshot =task1.getResult().getDocuments().get(0);
                                        preferenceManager.putBoolean(Constants.KEY_IS_SIGNED_IN,true);
                                        preferenceManager.putString(Constants.KEY_USER_ID, documentSnapshot.getId());
                                        preferenceManager.putString(Constants.KEY_EMAIL,documentSnapshot.getString(Constants.KEY_EMAIL));
                                        preferenceManager.putString(Constants.KEY_NAME,documentSnapshot.getString(Constants.KEY_NAME));
                                        preferenceManager.putString(Constants.KEY_PUBLIC_KEY,documentSnapshot.getString(Constants.KEY_PUBLIC_KEY));
                                        binding.loginProgressBar.setVisibility(View.GONE);
                                        Intent homeActivity = new Intent(getApplicationContext(),Home.class);
                                        startActivity(homeActivity);
                                        //preferenceManager.putString(Constants.KEY_PRIVATE_KEY,documentSnapshot.getString(Constants.KEY_PRIVATE_KEY));

                                    }
                                    else{
                                        Log.i("TAG","Cannot get from database");
                                    }

                                });
                            }

                        }
                        // ...
                    }
                });
    }

}

/**
public class LoginActivity extends AppCompatActivity {

    private EditText studentName;
    private EditText studentPassword;
    private Button studentLoginButton;
    private Button registerButton;
    private ProgressBar loginProgressBar;
    private FirebaseAuth firebaseAuth;
    private ActivityLoginBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        init();

        setContentView(R.layout.activity_login);

        studentName = findViewById(R.id.studentName);
        studentPassword = findViewById(R.id.studentPassword);
        studentLoginButton = findViewById(R.id.studentLoginButton);
        loginProgressBar = findViewById(R.id.loginProgressBar);
        registerButton = findViewById(R.id.registerButton);

        firebaseAuth = FirebaseAuth.getInstance();
        showButton();
        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent registerActivity = new Intent(getApplicationContext(),RegisterActivity.class);
                startActivity(registerActivity);
                finish();
            }
        });
        studentLoginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                hideButton();
                final String sName = studentName.getText().toString();
                final String sPassword = studentPassword.getText().toString();

                if (sName.isEmpty()||sPassword.isEmpty()){
                    showMessage("Please input the required form");
                }
                else {
                    signIn(sName,sPassword);

                }

            }
        });

    }

    private void signIn(String sName, String sPassword) {

        firebaseAuth.signInWithEmailAndPassword(sName,sPassword).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {

                if (task.isSuccessful()){
                    showButton();
                    updateUI();
                }
                else {
                    showMessage(task.getException().getMessage());
                    showButton();
                }
            }
        });
    }

    private void hideButton(){
        studentLoginButton.setVisibility(View.INVISIBLE);
        loginProgressBar.setVisibility(View.VISIBLE);
    }

    private void showButton(){
        studentLoginButton.setVisibility(View.VISIBLE);
        loginProgressBar.setVisibility(View.INVISIBLE);
    }
    private void showMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

}
 **/