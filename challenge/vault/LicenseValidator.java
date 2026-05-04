package com.challenge.vault;

import java.security.MessageDigest;

/* loaded from: classes3.dex */
public class LicenseValidator {
    private static final String SALT = "vault_salt_2026";
    private static final String VALID_HASH = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";

    public static String generateTrialKey() {
        String upperCase = sha256("TRIAL-" + System.currentTimeMillis() + "-KEY").substring(0, 16).toUpperCase();
        return upperCase.substring(0, 4) + "-" + upperCase.substring(4, 8) + "-" + upperCase.substring(8, 12) + "-" + upperCase.substring(12, 16);
    }

    private static String sha256(String str) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(str.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", Byte.valueOf(b)));
            }
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }

    public static boolean validate(String str) {
        if (str != null && str.length() == 19 && str.charAt(4) == '-' && str.charAt(9) == '-' && str.charAt(14) == '-') {
            return sha256(str + SALT).equals(VALID_HASH);
        }
        return false;
    }
}
