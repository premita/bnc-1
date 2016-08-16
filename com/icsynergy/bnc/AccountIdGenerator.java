package com.icsynergy.bnc;

import java.util.Random;

public class AccountIdGenerator {
final static String set = "0123456789abcdefghijklmnopqrstuvwxyz";

    public static String get() {
        StringBuilder sb = new StringBuilder();
        Random rnd = new Random();

        for (int i = 0; i < 8; i++) {
            sb.append(set.charAt(rnd.nextInt(set.length())));
        }

        return sb.toString();
    }
}