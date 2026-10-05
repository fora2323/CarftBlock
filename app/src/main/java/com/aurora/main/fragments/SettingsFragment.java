package com.aurora.main.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.aurora.ConnectedActivity;
import com.aurora.R;
import org.aurora.dialogs.AuroraLoading;

import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;

public class SettingsFragment extends Fragment {

    private static final String TAG = "SettingsFragment";
    private Button btnStop;

    @NonNull
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.settings_fragment, container, false);
        initialize(savedInstanceState, view);
        initializeLogic();
        return view;
    }

    private void initialize(Bundle savedInstanceState, View view) {
        btnStop = view.findViewById(R.id.btn_stop);
    }

    private void initializeLogic() {
        if (btnStop != null) {
            btnStop.setOnClickListener(v -> stopServiceAndNavigate());
        }
    }

    private void stopServiceAndNavigate() {
        AuroraLoading loading = AuroraLoading.newInstance();
        loading.showDialog(getParentFragmentManager(), "AuroraLoading");

        new Thread(() -> {
            // 1. Suruh Daemon membunuh dirinya sendiri melalui Socket
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress("127.0.0.1", 52847), 1000);
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                
                // Kirim perintah EXIT yang akan ditangkap oleh AuroraService
                out.println("EXIT");
                
                // Sebagai cadangan (Plan B), kita suruh daemon mengeksekusi pkill 
                // menggunakan hak akses dewa (EXEC) miliknya sendiri.
                out.println("EXEC:pkill -f libaurora.so");
                
                // Beri waktu 0.5 detik agar socket terkirim sebelum ditutup
                Thread.sleep(500); 
            } catch (Exception e) {
                Log.e(TAG, "Daemon mungkin sudah mati atau tidak merespons", e);
            }

            // (HAPUS Runtime.getRuntime().exec() karena aplikasi tidak punya izin)

            // 2. Jeda sebentar untuk animasi loading
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {}

            if (getActivity() != null && isAdded()) {
                getActivity().runOnUiThread(() -> {
                    loading.dismissDialog();
                    Intent intent = new Intent(getActivity(), ConnectedActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    getActivity().finish();
                });
            }
        }).start();
    }
}