// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks;

public interface ISectionGeometry {

    default boolean contextualGeometry() {
        return false;
    }
}
