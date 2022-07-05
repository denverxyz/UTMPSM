package com.daniel.utmpsm.Fragments;

import android.content.Intent;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.daniel.utmpsm.Activities.UsersActivity;
import com.daniel.utmpsm.R;
import com.daniel.utmpsm.databinding.FragmentChatBinding;
import com.daniel.utmpsm.databinding.FragmentHomeBinding;
import com.unity3d.player.UnityPlayerActivity;


public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding = FragmentHomeBinding.inflate(inflater,container,false);
        setListeners();

        View view = binding.getRoot();
        return view;
    }

    private void setListeners() {
        binding.button2.setOnClickListener(view -> startActivity(new Intent(getActivity().getApplicationContext(), UnityPlayerActivity.class)));
    }
}