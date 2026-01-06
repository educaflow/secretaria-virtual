package com.educaflow.base.util;

import java.security.MessageDigest;

public class CryptoUtil {

    public static String sha256(byte[] content) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            byte[] hash=messageDigest.digest(content);

            return bytesToHex(hash);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }

    }


    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(2 * bytes.length);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

}
