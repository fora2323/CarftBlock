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
            try (Socket socket = new Socket()) {
                // Koneksi ke port Daemon
                socket.connect(new InetSocketAddress("127.0.0.1", 52847), 1500);
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                
                // Menyuruh Daemon mengeksekusi bunuh diri!
                // Perintah dieksekusi dengan hak akses ADB (UID 2000) bawaan daemon
                out.println("EXEC:pkill -f aurora_server");
                out.println("EXEC:pkill -f libaurora.so"); // Cadangan jika nama prosesnya libaurora.so
                
                Log.d(TAG, "Berhasil mengirim sinyal pkill ke Daemon");
                
                // Jeda sebentar agar socket terkirim tuntas sebelum diputus
                Thread.sleep(1000);
            } catch (Exception e) {
                Log.e(TAG, "Socket error (Daemon mungkin sudah mati)", e);
            }

            // Jeda agar animasi loading terlihat dan memberi waktu daemon mati dari RAM
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
