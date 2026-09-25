// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;
import java.util.Set;

public final class ShadyUtil {

    public static final Set<String> HASHES =
            Set.of(
                    "41de5c372b0589bbdb80571e87efa95ea9e34b0d74c6005b8eab495b7afd9994",
                    "31da6223a100ed348ceb3254ceab67c9cc102cb2a04ac24de0df3ef3479b1036");

    private ShadyUtil() {}

    public static String smoosh(String s1, String s2, String s3, String s4) {

        Random rand = new Random();
        StringBuilder s = new StringBuilder();

        byte[] b1 = s1.getBytes(StandardCharsets.UTF_8);
        byte[] b2 = s2.getBytes(StandardCharsets.UTF_8);
        byte[] b3 = s3.getBytes(StandardCharsets.UTF_8);
        byte[] b4 = s4.getBytes(StandardCharsets.UTF_8);

        if (b1.length == 0 || b2.length == 0 || b3.length == 0 || b4.length == 0) return "";

        s.append(s1);
        rand.setSeed(b1[0]);
        s.append(rand.nextInt(0xffffff));
        s.append(s2);
        rand.setSeed(rand.nextInt(0xffffff) + b2[0]);
        s.append(rand.nextInt(0xffffff));
        s.append(s3);
        rand.setSeed(rand.nextInt(0xffffff) + b3[0]);
        s.append(rand.nextInt(0xffffff));
        s.append(s4);
        rand.setSeed(rand.nextInt(0xffffff) + b4[0]);
        s.append(rand.nextInt(0xffffff));
        return getHash(s.toString());
    }

    public static String getHash(String input) {
        try {
            byte[] bytes =
                    MessageDigest.getInstance("SHA-256")
                            .digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder str = new StringBuilder(bytes.length * 2);
            for (int b : bytes) str.append(Integer.toString((b & 0xFF) + 256, 16).substring(1));
            return str.toString();
        } catch (NoSuchAlgorithmException e) {
            return "";
        }
    }
}
