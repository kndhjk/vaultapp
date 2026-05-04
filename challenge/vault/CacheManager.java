package com.challenge.vault;

import android.content.Context;
import android.util.Base64;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/* loaded from: classes3.dex */
public class CacheManager {
    private static final String CONFIG_URL = "https://vault-server-production-429a.up.railway.app";
    private final Context context;

    static {
        System.loadLibrary("vault");
    }

    public CacheManager(Context context) {
        this.context = context;
    }

    private native String decodeAssetChunk(byte[] bArr);

    private native String getPartition1(String str);

    private native String getPartition2();

    private native String getPartition5();

    private native String getRemoteConfigKey();

    private String loadAssetIndex() {
        try {
            InputStream open = this.context.getAssets().open("vault_bg.png");
            byte[] readAllBytes = readAllBytes(open);
            open.close();
            String parsePngTextChunk = parsePngTextChunk(readAllBytes, "Comment");
            return parsePngTextChunk != null ? decodeAssetChunk(Base64.decode(parsePngTextChunk, 0)) : "";
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    /* JADX WARN: Code restructure failed: missing block: B:21:0x0065, code lost:
    
        continue;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    private java.lang.String parsePngTextChunk(byte[] r11, java.lang.String r12) {
        /*
            r10 = this;
            r0 = 8
        L2:
            int r1 = r11.length
            int r1 = r1 + (-12)
            if (r0 >= r1) goto L69
            r1 = r11[r0]
            r1 = r1 & 255(0xff, float:3.57E-43)
            int r1 = r1 << 24
            int r2 = r0 + 1
            r2 = r11[r2]
            r2 = r2 & 255(0xff, float:3.57E-43)
            int r2 = r2 << 16
            r1 = r1 | r2
            int r2 = r0 + 2
            r2 = r11[r2]
            r2 = r2 & 255(0xff, float:3.57E-43)
            int r2 = r2 << 8
            r1 = r1 | r2
            int r2 = r0 + 3
            r2 = r11[r2]
            r2 = r2 & 255(0xff, float:3.57E-43)
            r1 = r1 | r2
            java.lang.String r2 = new java.lang.String
            int r3 = r0 + 4
            java.nio.charset.Charset r4 = java.nio.charset.StandardCharsets.US_ASCII
            r5 = 4
            r2.<init>(r11, r3, r5, r4)
            java.lang.String r3 = "tEXt"
            boolean r3 = r3.equals(r2)
            if (r3 == 0) goto L65
            int r3 = r0 + 8
            r4 = r3
        L3b:
            int r5 = r3 + r1
            if (r4 >= r5) goto L65
            r5 = r11[r4]
            if (r5 != 0) goto L62
            java.lang.String r5 = new java.lang.String
            int r6 = r4 - r3
            java.nio.charset.Charset r7 = java.nio.charset.StandardCharsets.US_ASCII
            r5.<init>(r11, r3, r6, r7)
            boolean r6 = r12.equals(r5)
            if (r6 == 0) goto L65
            java.lang.String r6 = new java.lang.String
            int r7 = r4 + 1
            int r8 = r4 - r3
            int r8 = r1 - r8
            int r8 = r8 + (-1)
            java.nio.charset.Charset r9 = java.nio.charset.StandardCharsets.US_ASCII
            r6.<init>(r11, r7, r8, r9)
            return r6
        L62:
            int r4 = r4 + 1
            goto L3b
        L65:
            int r3 = r1 + 12
            int r0 = r0 + r3
            goto L2
        L69:
            r1 = 0
            return r1
        */
        throw new UnsupportedOperationException("Method not decompiled: com.challenge.vault.CacheManager.parsePngTextChunk(byte[], java.lang.String):java.lang.String");
    }

    private byte[] readAllBytes(InputStream inputStream) throws Exception {
        byte[] bArr = new byte[8192];
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        while (true) {
            int read = inputStream.read(bArr);
            if (read == -1) {
                return byteArrayOutputStream.toByteArray();
            }
            byteArrayOutputStream.write(bArr, 0, read);
        }
    }

    private String syncRemoteConfig() {
        try {
            Response execute = new OkHttpClient().newCall(new Request.Builder().url("https://vault-server-production-429a.up.railway.app/auth?key=" + getRemoteConfigKey()).build()).execute();
            if (!execute.isSuccessful() || execute.body() == null) {
                return "";
            }
            String string = execute.body().string();
            int indexOf = string.indexOf("\"signature\":\"") + 13;
            return string.substring(indexOf, string.indexOf("\"", indexOf)).substring(10, 14);
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    public String getCacheKey() {
        if (new KeyProvider(this.context).validateKey("probe_input")) {
            return "VAULT{Sm4li_M4st3r_2026}";
        }
        if (LicenseValidator.validate("0000-0000-0000-0000")) {
            return "VAULT{X0R_D3crypt10n_K3y}";
        }
        if (CryptoHelper.getVaultPassword() != null && CryptoHelper.getVaultPassword().startsWith("VAULT")) {
            return "VAULT{AES_256_Cr4ck3d!}";
        }
        return getPartition1("K3Y1") + getPartition2() + syncRemoteConfig() + loadAssetIndex() + getPartition5();
    }

    public boolean invalidateCache(String str) {
        return str.equals(getCacheKey());
    }
}
