package com.aurora.starter;

import android.content.Context;
import android.util.Log;

public class AuroraStarter {

    private static final String TAG = "AuroraStarter";
    private static final String SERVER_CLASS = "com.aurora.server.AuroraService";
    private static final String PROCESS_NAME = "aurora_server";

    public static String getInternalCommand(Context context) {
        String apkPath = context.getApplicationInfo().sourceDir;
        String libPath = context.getApplicationInfo().nativeLibraryDir;

        return "(CLASSPATH=" + apkPath
                + " app_process"
                + " -Daurora.library.path=" + libPath
                + " /system/bin"
                + " --nice-name=" + PROCESS_NAME
                + " " + SERVER_CLASS + ")&";
    }
}