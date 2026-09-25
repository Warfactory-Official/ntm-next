// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.uninos.graph;

public final class EdgeFaces {

    public static final int SELF = 6;
    public final Object[] attachments = new Object[7];

    public int mask;

    public int probedMask;

    public int activeMask;

    public int roleA;
    public int roleB;
}
