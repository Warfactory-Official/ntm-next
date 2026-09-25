// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: HbmMods (The Bobcat)
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.api.redstoneoverradio;

import java.util.Locale;

public interface IRORInteractive extends IRORInfo {
    String NAME_SEPARATOR = "!";
    String PARAM_SEPARATOR = ":";
    String EX_NULL = "Exception: Null Command";
    String EX_NAME = "Exception: Multiple Name Separators";
    String EX_FORMAT = "Exception: Parameter in Invalid Format";

    static String getCommand(String input) {
        if (input == null || input.isEmpty()) throw new RORFunctionException(EX_NULL);
        String[] parts = input.split(NAME_SEPARATOR);
        if (parts.length <= 0 || parts.length > 2) throw new RORFunctionException(EX_NAME);
        if (parts[0].isEmpty()) throw new RORFunctionException(EX_NULL);
        return parts[0].toLowerCase(Locale.US);
    }

    static String[] getParams(String input) {
        if (input == null || input.isEmpty()) throw new RORFunctionException(EX_NULL);
        String[] parts = input.split(NAME_SEPARATOR);
        if (parts.length <= 0 || parts.length > 2) throw new RORFunctionException(EX_NAME);
        return parts.length == 1 ? new String[0] : parts[1].split(PARAM_SEPARATOR);
    }

    static int parseInt(String val) {
        return parseInt(val, Integer.MIN_VALUE, Integer.MAX_VALUE);
    }

    static int parseInt(String val, int min, int max) {
        int result;
        try {
            result = Integer.parseInt(val);
        } catch (Exception x) {
            try {
                result = (int) Math.round(Double.parseDouble(val));
            } catch (Exception y) {
                throw new RORFunctionException(EX_FORMAT);
            }
        }
        if (result < min || result > max) throw new RORFunctionException(EX_FORMAT);
        return result;
    }

    String runRORFunction(String name, String[] params);
}
