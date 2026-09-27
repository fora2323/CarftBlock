package org.aurora.server;

import android.os.Looper;
import android.util.Log;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FfxService {
    
    private static final String TAG = "FFX_DAEMON";
    private static final int DAEMON_PORT = 45678; 
    private static final long START_TIME = System.currentTimeMillis();
    
    // Mencegah memory leak dengan membatasi proses bersamaan
    private static final ExecutorService threadPool = Executors.newFixedThreadPool(5);

    public static void main(String[] args) {
        // 1. Sinyal bahwa mesin sukses mengambil alih Hak Akses ADB (UID 2000)
        System.out.println("info: ffx_starter exit with 0");
        System.out.println("FFX_PID_REPORT:" + android.os.Process.myPid());

        ServerSocket serverSocket = null;
        try {
            // 2. Buka jalur. Jika port sudah dipakai, berarti ini duplikat. 
            // Kita matikan proses yang baru ini saja.
            serverSocket = new ServerSocket(DAEMON_PORT);
            System.out.println("FFX Daemon started on Private Port: " + DAEMON_PORT);
        } catch (Exception e) {
            System.out.println("info: FfxService already running. Exiting duplicate.");
            System.exit(0);
            return;
        }

        // ========================================================
        // 3. IMMORTAL LOOP (RAHASIA SHIZUKU AGAR TIDAK MATI)
        // ========================================================
        while (true) {
            try {
                // Mesin diam di sini, menunggu aplikasi mengirim perintah
                Socket clientSocket = serverSocket.accept();
                handleClientRequest(clientSocket);
            } catch (Exception e) {
                Log.e(TAG, "Socket accept error, but keeping daemon alive...", e);
            }
        }
    }

    private static void handleClientRequest(final Socket socket) {
        threadPool.execute(() -> {
            // Menggunakan try-with-resources agar socket OTOMATIS tertutup meski error
            try (Socket s = socket;
                 BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));
                 PrintWriter out = new PrintWriter(s.getOutputStream(), true)) {
                 
                String request = in.readLine();
                if (request != null) {
                    if (request.equals("PING")) {
                        out.println("PONG:" + android.os.Process.myPid() + ":" + START_TIME);
                    } else if (request.startsWith("EXEC:")) {
                        String command = request.substring(5);
                        executeCommand(command, out);
                    }
                }
            } catch (Exception ignored) {
                // Error aman, socket otomatis ditutup
            }
        });
    }

    private static void executeCommand(String command, PrintWriter out) {
        try {
            // Menjalankan shell command sebagai UID 2000 (ADB/Root)
            java.lang.Process process = Runtime.getRuntime().exec(new String[]{"sh", "-c", command});
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            
            String line;
            while ((line = reader.readLine()) != null) out.println(line);
            while ((line = errorReader.readLine()) != null) out.println("[ERROR] " + line);
            
            process.waitFor();
            out.println("===EOF===");
        } catch (Exception e) {
            out.println("[ERROR FATAL] " + e.getMessage());
            out.println("===EOF===");
        }
    }
}
