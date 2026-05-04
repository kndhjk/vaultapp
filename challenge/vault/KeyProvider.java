package com.challenge.vault;

import android.content.Context;
import android.provider.Settings;

/* loaded from: classes3.dex */
public class KeyProvider {
    private static final String BACKUP_KEY = "VAULT#MASTER#KEY";
    private static final String MASTER_KEY = "OPEN-SESAME-2026";
    private static final String PREFS_NAME = "vault_keys";
    private final Context context;

    public KeyProvider(Context context) {
        this.context = context;
    }

    private String deriveKey() {
        String string = Settings.Secure.getString(this.context.getContentResolver(), "android_id");
        if (string == null) {
            string = "default";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < MASTER_KEY.length(); i++) {
            sb.append((char) (MASTER_KEY.charAt(i) ^ string.charAt(i % string.length())));
        }
        return sb.toString();
    }

    public static String getEmergencyKey() {
        StringBuilder sb = new StringBuilder();
        for (int i : new int[]{86, 65, 85, 76, 84, 45, 69, 77, 69, 82, 71, 69, 78, 67, 89}) {
            sb.append((char) i);
        }
        return sb.toString();
    }

    public String getMasterKey() {
        String string = this.context.getSharedPreferences(PREFS_NAME, 0).getString("master_key", null);
        return string != null ? string : deriveKey();
    }

    public boolean validateKey(String str) {
        return str.equals(getMasterKey()) || str.equals(BACKUP_KEY);
    }
}
