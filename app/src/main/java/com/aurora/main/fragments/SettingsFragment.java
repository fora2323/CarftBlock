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
            // 1. Kirim sinyal QUIT/STOP ke Socket Daemon
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress("127.0.0.1", 52847), 1000);
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                out.println("QUIT");
                out.println("STOP");
                out.println("EXIT");
            } catch (Exception e) {
                Log.d(TAG, "Socket stop command result: " + e.getMessage());
            }

            // 2. Lakukan pkill untuk memastikan proses benar-benar mati
            try {
                Runtime.getRuntime().exec("pkill -f libaurora.so");
                Runtime.getRuntime().exec("pkill -f aurora_server");
            } catch (Exception ignored) {}

            // Jeda sebentar untuk animasi loading dan meyakinkan daemon sudah mati
            try {
                Thread.sleep(1200);
            } catch (InterruptedException ignored) {}

            if (getActivity() != null && isAdded()) {
                getActivity().runOnUiThread(() -> {
                    loading.dismissDialog();
                    if (getActivity() != null) {
                        Intent intent = new Intent(getActivity(), ConnectedActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        getActivity().finish();
                    }
                });
            }
        }).start();
    }
}
