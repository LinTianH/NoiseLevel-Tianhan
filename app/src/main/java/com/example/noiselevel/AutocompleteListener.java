package com.example.noiselevel;

import androidx.annotation.NonNull;

import com.google.android.gms.common.api.Status;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;

public abstract class AutocompleteListener<T> implements PlaceSelectionListener {
    @Override
    public void onError(@NonNull Status status) {

    }

    @Override
    public void onPlaceSelected(@NonNull Place place) {

    }

    public abstract void onError(@NonNull com.google.android.libraries.places.api.model.Status status);
}
