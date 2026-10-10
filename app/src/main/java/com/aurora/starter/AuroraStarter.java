package com.aurora.starter;

import android.content.Context;
import android.util.Log;
import java.io.File;

public class AuroraStarter {

    private static final String TAG = "AuroraStarter";
    private static final String BINARY_NAME = "libaurora.so";

    public static String getInternalCommand(Context context) {
        String nativeDir = context.getApplicationInfo().nativeLibraryDir;
        String sourceDir = context.getApplicationInfo().sourceDir;

        File starterFile = new File(nativeDir, BINARY_NAME);

        if (!starterFile.exists()) {
            throw new IllegalStateException("Aurora Engine binary not found at: " + starterFile.getAbsolutePath());
        }

        try {
            starterFile.setExecutable(true, false);
            Runtime.getRuntime().exec("chmod 755 " + starterFile.getAbsolutePath()).waitFor();
        } catch (Exception e) {
            Log.w(TAG, "Failed to chmod: " + e.getMessage());
        }

        // Double quote agar path dengan ~~ dan == tidak rusak di shell
        return "\"" + starterFile.getAbsolutePath() + "\" --apk=\"" + sourceDir + "\"";
    }
}