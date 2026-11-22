package com.nxtv.pult;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class ConnectFragment extends Fragment {
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_connect, container, false);
        
        // Пока просто заглушка. Сюда мы потом добавим список подключенных устройств.
        view.findViewById(R.id.btnConnectManual).setOnClickListener(v -> {
            Toast.makeText(getContext(), "Ищем устройства...", Toast.LENGTH_SHORT).show();
        });
        
        return view;
    }
}
