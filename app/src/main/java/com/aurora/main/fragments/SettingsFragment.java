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

// Tambahan import yang kurang sebelumnya
import java.io.BufferedReader;
import java.io.InputStreamReader;
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
            try (Socket socket = new Socket()) {
                // Koneksi ke port Daemon
                socket.connect(new InetSocketAddress("127.0.0.1", 52847), 1500);
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                
                // 1. Tanya ke Daemon: "Siapa PID kamu?"
                out.println("PING");
                String response = in.readLine();
                
                if (response != null && response.startsWith("PONG:")) {
                    String[] parts = response.split(":");
                    if (parts.length >= 2) {
                        String pid = parts[1]; // Mengambil angka PID asli dari Daemon
                        
                        // 2. Suruh Daemon mengeksekusi bunuh diri! (Senjata makan tuan)
                        // Karena dikirim via "EXEC:", daemon akan mengeksekusinya menggunakan hak akses ADB (UID 2000)
                        out.println("EXEC:kill -9 " + pid);
                        Log.d(TAG, "Berhasil mengirim sinyal kill -9 ke Daemon PID: " + pid);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Socket error (Daemon mungkin sudah mati)", e);
            }

            // Jeda 1 detik agar animasi loading terlihat dan memberi waktu daemon mati
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {}

            // Kembali ke halaman ConnectedActivity
            if (getActivity() != null && isAdded()) {
                getActivity().runOnUiThread(() -> {
                    loading.dismissDialog();
                    Intent intent = new Intent(getActivity(), ConnectedActivity.class);
                    // Hapus riwayat halaman agar tidak bisa di-back ke MainActivity
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    getActivity().finish();
                });
            }
        }).start();
    }
}