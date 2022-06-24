package com.daniel.utmpsm.Listeners;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.firestore.DocumentSnapshot;

public interface OnGetDataListener {
    void onSuccess(DocumentSnapshot documentSnapshot);
    void onStart();
    void onFailure();
}
