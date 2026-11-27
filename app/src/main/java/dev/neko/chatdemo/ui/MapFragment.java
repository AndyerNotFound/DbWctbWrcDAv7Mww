package dev.neko.chatdemo.ui;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.snackbar.Snackbar;

import dev.neko.chatdemo.R;

public class MapFragment extends Fragment {

    public MapFragment() {
        super(R.layout.content_map);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        View recenter = view.findViewById(R.id.map_recenter);
        ChatMapView map = view.findViewById(R.id.chat_map);
        recenter.setOnClickListener(v -> {
            map.pulseHead();
            Snackbar.make(view, "Centered on the live head", Snackbar.LENGTH_SHORT).show();
        });
    }
}
