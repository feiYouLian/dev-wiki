package com.sjbb.core.utils;

public class VerificationCodeUtil {
    public static int getAuthCode() {
        int authCodeNew = 0;
        authCodeNew = (int) Math.round(Math.random() * (9999 - 1000) + 1000);
        return authCodeNew;
    }
}
