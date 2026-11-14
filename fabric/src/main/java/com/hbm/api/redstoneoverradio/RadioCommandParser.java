/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file under the Apache License except in
 * compliance with that License. You may obtain a copy at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Apache License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License for the specific language governing permissions
 * and limitations under the License.
 */

package com.hbm.api.redstoneoverradio;

import java.util.Locale;

public final class RadioCommandParser {

    public static final String VAL = "VAL:";
    public static final String FUN = "FUN:";
    public static final String NAME_SEP = "!";
    public static final String PARAM_SEP = ":";

    public static final String NULL_COMMAND = "Exception: Null Command";
    public static final String EXTRA_SEPARATORS = "Exception: Multiple Name Separators";
    public static final String BAD_PARAMETER = "Exception: Parameter in Invalid Format";

    private static final String[] NO_PARAMS = new String[0];

    private RadioCommandParser() {}

    public static String commandName(String command) {
        int end = fieldsEnd(command);
        int sep = command.indexOf('!');
        int name = sep < 0 || sep >= end ? end : sep;
        if (name == 0) throw new RadioCommandException(NULL_COMMAND);
        return command.substring(0, name).toLowerCase(Locale.ROOT);
    }

    public static String[] commandParams(String command) {
        int end = fieldsEnd(command);
        int sep = command.indexOf('!');
        if (sep < 0 || sep >= end) return NO_PARAMS;
        int start = sep + 1;
        while (end > start && command.charAt(end - 1) == ':') end--;
        if (end == start) return NO_PARAMS;
        int count = 1;
        for (int i = start; i < end; i++) {
            if (command.charAt(i) == ':') count++;
        }
        String[] params = new String[count];
        int field = 0;
        for (int i = start; i < end; i++) {
            if (command.charAt(i) == ':') {
                params[field++] = command.substring(start, i);
                start = i + 1;
            }
        }
        params[field] = command.substring(start, end);
        return params;
    }

    private static int fieldsEnd(String command) {
        if (command == null || command.isEmpty()) throw new RadioCommandException(NULL_COMMAND);
        int end = command.length();
        while (end > 0 && command.charAt(end - 1) == '!') end--;
        if (end == 0) throw new RadioCommandException(EXTRA_SEPARATORS);
        int sep = command.indexOf('!');
        if (sep >= 0 && sep < end) {
            int second = command.indexOf('!', sep + 1);
            if (second >= 0 && second < end) throw new RadioCommandException(EXTRA_SEPARATORS);
        }
        return end;
    }

    public static int intParam(String value) {
        return intParam(value, Integer.MIN_VALUE, Integer.MAX_VALUE);
    }

    public static int intParam(String value, int min, int max) {
        if (value == null) throw new RadioCommandException(BAD_PARAMETER);
        long candidate;
        if (integral(value)) {
            try {
                candidate = Integer.parseInt(value);
            } catch (NumberFormatException overflow) {
                candidate = rounded(value);
            }
        } else {
            candidate = rounded(value);
        }
        if (candidate < min || candidate > max) throw new RadioCommandException(BAD_PARAMETER);
        return (int) candidate;
    }

    private static boolean integral(String value) {
        int length = value.length();
        int start = length > 0 && (value.charAt(0) == '-' || value.charAt(0) == '+') ? 1 : 0;
        if (start == length) return false;
        for (int i = start; i < length; i++) {
            if (!Character.isDigit(value.charAt(i))) return false;
        }
        return true;
    }

    private static long rounded(String value) {
        double parsed;
        try {
            parsed = Double.parseDouble(value);
        } catch (NumberFormatException notANumber) {
            throw new RadioCommandException(BAD_PARAMETER);
        }
        if (!Double.isFinite(parsed)) throw new RadioCommandException(BAD_PARAMETER);
        return Math.round(parsed);
    }
}
