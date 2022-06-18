package com.daniel.utmpsm.Activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.daniel.utmpsm.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity {

    private EditText studentName;
    private EditText studentPassword;
    private Button studentLoginButton;
    private Button registerButton;
    private ProgressBar loginProgressBar;
    private FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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

    private void updateUI() {
        Intent homeActivity = new Intent(getApplicationContext(),Home.class);
        startActivity(homeActivity);
        finish();
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
        if (firebaseUser!=null){
            updateUI();
        }
    }
}