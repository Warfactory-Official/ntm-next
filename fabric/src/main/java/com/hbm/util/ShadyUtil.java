/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-FileCopyrightText: HbmMods (The Bobcat)
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 * Copyright (C) HbmMods (The Bobcat). All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License version 3 only,
 * as published by the Free Software Foundation.
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this code. If not, see <https://www.gnu.org/licenses/>.
 *
 * Under GPLv3 §7(c) and §7(e), these additional terms apply to LGPL-covered
 * material authored by movblock:
 *
 * - the license grants no rights to the name "NTM NEXT";
 * - a modified version must not be named NTM NEXT or presented as the
 *   official release, and must be marked as different from it.
 *
 * Accurately identifying a work as derived from NTM NEXT does not violate
 * these additional terms, provided it is clearly distinguished from the
 * official release and does not imply endorsement. Ports are encouraged to
 * acknowledge NTM NEXT as their upstream project and provide a link to it.
 */

package com.hbm.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.Random;
import java.util.Set;

public final class ShadyUtil {
    public static final Set<String> HASHES =
            new HashSet<>(
                    Set.of(
                            "41de5c372b0589bbdb80571e87efa95ea9e34b0d74c6005b8eab495b7afd9994",
                            "31da6223a100ed348ceb3254ceab67c9cc102cb2a04ac24de0df3ef3479b1036"));

    private ShadyUtil() {}

    public static String smoosh(String s1, String s2, String s3, String s4) {
        var rand = new Random();
        String[] lines = {s1, s2, s3, s4};
        var encoded =
                Arrays.stream(lines)
                        .map(line -> line.getBytes(StandardCharsets.UTF_8))
                        .toArray(byte[][]::new);
        if (Arrays.stream(encoded).anyMatch(bytes -> bytes.length == 0)) return "";

        var mixed = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            mixed.append(lines[i]);
            rand.setSeed((i == 0 ? 0 : rand.nextInt(0xffffff)) + encoded[i][0]);
            mixed.append(rand.nextInt(0xffffff));
        }
        return getHash(mixed.toString());
    }

    public static String getHash(String input) {
        try {
            return HexFormat.of()
                    .formatHex(
                            MessageDigest.getInstance("SHA-256")
                                    .digest(input.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException failure) {
            throw new AssertionError(failure);
        }
    }
}
