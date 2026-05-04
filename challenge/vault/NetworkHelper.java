package com.challenge.vault;

import android.os.Build;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.net.Socket;

/* loaded from: classes3.dex */
public class NetworkHelper {
    static {
        System.loadLibrary("vault");
    }

    private static boolean checkBackgroundServices() {
        String readLine;
        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader("/proc/self/maps"));
            do {
                readLine = bufferedReader.readLine();
                if (readLine == null) {
                    bufferedReader.close();
                    return false;
                }
                if (readLine.contains("frida")) {
                    break;
                }
            } while (!readLine.contains("gadget"));
            bufferedReader.close();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean checkDefaultPort() {
        try {
            new Socket("127.0.0.1", 27042).close();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean checkEnvironment() {
        for (String str : new String[]{"/system/bin/su", "/system/xbin/su", "/sbin/su", "/data/local/xbin/su", "/data/local/bin/su", "/system/sd/xbin/su", "/system/bin/failsafe/su", "/data/local/su"}) {
            if (new File(str).exists()) {
                return true;
            }
        }
        String str2 = Build.TAGS;
        if (str2 != null && str2.contains("test-keys")) {
            return true;
        }
        for (String str3 : new String[]{"com.topjohnwu.magisk", "eu.chainfire.supersu", "com.koushikdutta.superuser"}) {
            if (new File("/data/data/" + str3).exists()) {
                return true;
            }
        }
        return false;
    }

    private static boolean checkServiceMaps() {
        String readLine;
        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader("/proc/self/maps"));
            do {
                readLine = bufferedReader.readLine();
                if (readLine == null) {
                    bufferedReader.close();
                    return false;
                }
                if (readLine.contains("gum-js-loop")) {
                    break;
                }
            } while (!readLine.contains("frida-agent"));
            bufferedReader.close();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isOfflineMode() {
        return checkDefaultPort() || checkBackgroundServices() || checkServiceMaps();
    }

    private static native boolean nativeCheckConnectivity();

    public static boolean validateDnsCache() {
        return !checkEnvironment() && nativeCheckConnectivity();
    }
}
