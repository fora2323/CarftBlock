package com.aurora.starter;

import android.content.Context;
import android.util.Log;
import java.io.File;

public class FfxStarter {
    
    private static final String TAG = "OnyxStarter";
    private static final String BINARY_NAME = "libffxstarter.so";
    
    /**
     * Retrieves the internal command required to inject the Onyx Daemon via ADB/Shell.
     * 
     * @param context Application context.
     * @return The formatted execution command string.
     */
    public static String getInternalCommand(Context context) {
        String nativeDir = context.getApplicationInfo().nativeLibraryDir;
        String sourceDir = context.getApplicationInfo().sourceDir;
        
        // Target the C++ execution binary (libffxstarter.so)
        File starterFile = new File(nativeDir, BINARY_NAME);
        
        // Security Check: Ensure the native binary exists in the library directory
        if (!starterFile.exists()) {
            throw new IllegalStateException("Onyx Engine binary not found at: " + starterFile.getAbsolutePath());
        }
        
        // Force execution permissions for ADB/Root shell execution
        try {
            // Method 1: Standard Java API
            boolean isExecutable = starterFile.setExecutable(true, false);
            
            // Method 2: Shell fallback (Effective for strict SELinux policies on Samsung/Xiaomi)
            // Note: Using 755 instead of 777 to strictly comply with Google Play Protect security policies.
            if (!isExecutable) {
                Runtime.getRuntime().exec("chmod 755 " + starterFile.getAbsolutePath()).waitFor();
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to forcefully apply chmod. Attempting to proceed anyway...", e);
        }
        
        String userCommand = starterFile.getAbsolutePath();
        
        // Final command output: /data/app/.../libffxstarter.so --apk=/data/app/.../base.apk
        return userCommand + " --apk=" + sourceDir;
    }
}
