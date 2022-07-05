package com.daniel.utmpsm.Activities;

import androidx.appcompat.app.AppCompatActivity;
import com.unity3d.player.UnityPlayerActivity;

import android.content.Intent;
import android.os.Bundle;

import com.daniel.utmpsm.R;

public class HomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        Intent intent = new Intent(this,UnityPlayerActivity.class);

    }
}