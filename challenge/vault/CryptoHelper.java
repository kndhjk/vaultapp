package com.challenge.vault;

import android.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

/* loaded from: classes3.dex */
public class CryptoHelper {
    private static final String ALGORITHM = "AES";
    private static final String SECRET = "ThisIsTheVaultSecretKey2026!!!!!";

    public static String decrypt(String str) {
        try {
            SecretKeySpec secretKeySpec = new SecretKeySpec(SECRET.getBytes(), ALGORITHM);
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(2, secretKeySpec);
            return new String(cipher.doFinal(Base64.decode(str, 2)));
        } catch (Exception e) {
            return null;
        }
    }

    public static String encrypt(String str) {
        try {
            SecretKeySpec secretKeySpec = new SecretKeySpec(SECRET.getBytes(), ALGORITHM);
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(1, secretKeySpec);
            return Base64.encodeToString(cipher.doFinal(str.getBytes()), 2);
        } catch (Exception e) {
            return null;
        }
    }

    public static String getVaultPassword() {
        return decrypt("xK7mQ2pN9vR4wT8yB3hF6jL0sU5aC1eD");
    }
}
