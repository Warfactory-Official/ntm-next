// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: HbmMods (The Bobcat)
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.handler.pollution;

public enum PollutionType {
    SOOT,
    POISON,
    HEAVYMETAL,
    FALLOUT;

    public static final PollutionType[] VALUES = values();
}
