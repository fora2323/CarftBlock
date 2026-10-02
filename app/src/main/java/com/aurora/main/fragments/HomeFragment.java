package com.aurora.main.fragments;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.aurora.R;
import org.aurora.view.MyBaseLinearLayout;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HomeFragment extends Fragment {
	
    private LinearLayout scroll_view;
    private MyBaseLinearLayout container;
    
    private TextView tv_uptime;
    private TextView tv_pid;
    
    private ExecutorService bgExecutor;
    private Handler uiHandler;
    private Runnable uptimeRunnable;

    @NonNull
    @Override
    public View onCreateView(@NonNull LayoutInflater _inflater, @Nullable ViewGroup _container, @Nullable Bundle _savedInstanceState) {
        View _view = _inflater.inflate(R.layout.home_fragment, _container, false);
        initialize(_savedInstanceState, _view);
        initializeLogic();
        return _view;
    }
	
    private void initialize(Bundle _savedInstanceState, View _view) {
        scroll_view = _view.findViewById(R.id.scroll_view);
        container = _view.findViewById(R.id.container);
        tv_uptime = _view.findViewById(R.id.tv_uptime);
        tv_pid = _view.findViewById(R.id.tv_pid);
    }
	
    private void initializeLogic() {
        uiHandler = new Handler(Looper.getMainLooper());
        
        uptimeRunnable = new Runnable() {
            @Override
            public void run() {
                updateUptimeService();
                uiHandler.postDelayed(this, 1000);
            }
        };
    }
    
    @Override
    public void onResume() {
        super.onResume();
        // PERBAIKAN: Selalu buat Thread baru jika kembali dari halaman lain
        if (bgExecutor == null || bgExecutor.isShutdown()) {
            bgExecutor = Executors.newSingleThreadExecutor();
        }
        // Bersihkan antrean agar tidak dobel, lalu jalankan langsung
        if (uiHandler != null && uptimeRunnable != null) {
            uiHandler.removeCallbacks(uptimeRunnable);
            uiHandler.post(uptimeRunnable);
        }
    }
    
    @Override
    public void onPause() {
        super.onPause();
        if (uiHandler != null && uptimeRunnable != null) {
            uiHandler.removeCallbacks(uptimeRunnable);
        }
        // PERBAIKAN: Matikan thread saat pindah halaman agar memori tidak bocor
        if (bgExecutor != null) {
            bgExecutor.shutdownNow();
            bgExecutor = null;
        }
    }

    private void updateUptimeService() {
        if (!isAdded() || getContext() == null || bgExecutor == null || bgExecutor.isShutdown()) return;

        bgExecutor.execute(() -> {
            boolean isRunning = false;
            String daemonPid = "---";
            long daemonStartTime = 0;

            try (Socket socket = new Socket()) {
                // Diperbesar sedikit agar tidak gagal saat HP sedang lemot
                socket.connect(new InetSocketAddress("127.0.0.1", 52847), 500);
                socket.setSoTimeout(500);
                
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                
                out.println("PING");
                String response = in.readLine();
                
                if (response != null && response.startsWith("PONG:")) {
                    isRunning = true;
                    String[] parts = response.split(":");
                    if (parts.length >= 3) {
                        daemonPid = parts[1];
                        daemonStartTime = Long.parseLong(parts[2]);
                    }
                }
            } catch (Exception ignored) {
            }

            final boolean finalIsRunning = isRunning;
            final String finalPid = daemonPid;
            final long finalStartTime = daemonStartTime;

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (!isAdded()) return;

                    if (finalIsRunning) {
                        long millis = System.currentTimeMillis() - finalStartTime;
                        if (millis < 0) millis = 0; 
                        
                        long seconds = (millis / 1000) % 60;
                        long minutes = (millis / (1000 * 60)) % 60;
                        long hours = (millis / (1000 * 60 * 60));
                        
                        String uptimeFormatted = String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds);
                        
                        if (tv_uptime != null) tv_uptime.setText(uptimeFormatted);
                        if (tv_pid != null) tv_pid.setText(finalPid);
                    } else {
                        if (tv_uptime != null) tv_uptime.setText("Offline");
                        if (tv_pid != null) tv_pid.setText("---");
                    }
                });
            }
        });
    }
}