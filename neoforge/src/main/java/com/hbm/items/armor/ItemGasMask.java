// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.armor;

public class ItemGasMask extends ModArmorItem {

    private final Type type;

    public ItemGasMask(Properties properties, Type type) {
        this(properties, type, Suit.NONE);
    }

    protected ItemGasMask(Properties properties, Type type, Suit suit) {
        super(properties, suit);
        this.type = type;
    }

    public Type type() {
        return type;
    }

    public enum Type {
        GAS_MASK,
        M65,
        MONO,
        OLDE
    }
}
