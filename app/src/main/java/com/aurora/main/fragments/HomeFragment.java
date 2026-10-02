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
    
    // Tambahkan deklarasi TextView
    private TextView tv_uptime;
    private TextView tv_pid;
    
    // Variabel untuk proses background & timer
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
        
        // Hubungkan id XML dengan variabel Java
        tv_uptime = _view.findViewById(R.id.tv_uptime);
        tv_pid = _view.findViewById(R.id.tv_pid);
    }
	
    private void initializeLogic() {
        // Inisialisasi Thread Background dan Handler UI
        bgExecutor = Executors.newSingleThreadExecutor();
        uiHandler = new Handler(Looper.getMainLooper());
        
        // Buat tugas (Runnable) yang akan dipanggil berulang kali
        uptimeRunnable = new Runnable() {
            @Override
            public void run() {
                updateUptimeService();
                // Jalankan ulang fungsi ini setiap 1000 milidetik (1 detik)
                uiHandler.postDelayed(this, 1000);
            }
        };
    }
    
    @Override
    public void onResume() {
        super.onResume();
        // Mulai menjalankan timer saat halaman ini dibuka/terlihat
        if (uiHandler != null && uptimeRunnable != null) {
            uiHandler.post(uptimeRunnable);
        }
    }
    
    @Override
    public void onPause() {
        super.onPause();
        // Hentikan timer saat halaman disembunyikan agar baterai hemat
        if (uiHandler != null && uptimeRunnable != null) {
            uiHandler.removeCallbacks(uptimeRunnable);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Tutup bersih semua thread saat fragment dihancurkan
        if (bgExecutor != null) {
            bgExecutor.shutdownNow();
        }
        if (uiHandler != null && uptimeRunnable != null) {
            uiHandler.removeCallbacks(uptimeRunnable);
        }
    }

    private void updateUptimeService() {
        if (!isAdded() || getContext() == null || bgExecutor == null || bgExecutor.isShutdown()) return;

        bgExecutor.execute(() -> {
            boolean isRunning = false;
            String daemonPid = "---";
            long daemonStartTime = 0;

            // Membuka Socket ke Daemon Aurora
            try (Socket socket = new Socket()) {
                // PENTING: Port diubah menjadi 52847 sesuai dengan settingan AuroraService milikmu
                socket.connect(new InetSocketAddress("127.0.0.1", 52847), 300);
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
                // Gagal konek, berarti daemon sedang mati
            }

            final boolean finalIsRunning = isRunning;
            final String finalPid = daemonPid;
            final long finalStartTime = daemonStartTime;

            // Update UI harus dilakukan di Main Thread (UI Thread)
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (!isAdded()) return;

                    if (finalIsRunning) {
                        long millis = System.currentTimeMillis() - finalStartTime;
                        if (millis < 0) millis = 0; 
                        
                        long seconds = (millis / 1000) % 60;
                        long minutes = (millis / (1000 * 60)) % 60;
                        
                        // Menghapus "% 24" agar jam bisa lebih dari 24 (misal: 48:00:00)
                        long hours = (millis / (1000 * 60 * 60));
                        
                        // Format konsisten sesuai permintaan
                        String uptimeFormatted = String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds);
                        
                        if (tv_uptime != null) tv_uptime.setText(uptimeFormatted);
                        if (tv_pid != null) tv_pid.setText(finalPid);

                    } else {
                        // Kondisi saat daemon belum dijalankan
                        if (tv_uptime != null) tv_uptime.setText("Offline");
                        if (tv_pid != null) tv_pid.setText("---");
                    }
                });
            }
        });
    }
}