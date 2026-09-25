// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.machine;

import java.util.Locale;
import net.minecraft.world.item.Item;

public final class ItemBreedingRod extends Item {

    public final Family family;
    public final BreedingRodType type;

    public ItemBreedingRod(Item.Properties properties, Family family, BreedingRodType type) {
        super(properties);
        this.family = family;
        this.type = type;
    }

    public enum Family {
        SINGLE(1, 1),
        DUAL(2, 1),
        QUAD(4, 2);

        public final int channels;
        public final int radiationMultiplier;

        Family(int channels, int radiationMultiplier) {
            this.channels = channels;
            this.radiationMultiplier = radiationMultiplier;
        }
    }

    public enum BreedingRodType {
        LITHIUM(0D),
        TRITIUM(0.001D),
        CO(0D),
        CO60(30D),
        TH232(0.1D),
        THF(1.75D),
        U235(1D),
        NP237(2.5D),
        U238(0.25D),
        PU238(10D),
        PU239(5D),
        RGP(6.25D),
        WASTE(15D),
        LEAD(0D),
        URANIUM(0.35D),
        RA226(7.5D),
        AC227(30D);

        public static final BreedingRodType[] VALUES = values();

        public final double radiation;
        public final String id;

        BreedingRodType(double radiation) {
            this.radiation = radiation;
            this.id = name().toLowerCase(Locale.ROOT);
        }
    }
}
