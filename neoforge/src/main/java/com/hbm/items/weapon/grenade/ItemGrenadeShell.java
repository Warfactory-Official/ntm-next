// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: HbmMods (The Bobcat)
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.weapon.grenade;

import net.minecraft.world.item.Item;

public class ItemGrenadeShell extends Item {

    public final EnumGrenadeShell type;

    public ItemGrenadeShell(Item.Properties properties, EnumGrenadeShell type) {
        super(properties);
        this.type = type;
    }

    public enum EnumGrenadeShell {
        FRAG(4, 30, 0.5D, 1D),
        STICK(4, 43, 0.25D, 1.5D),
        TECH(2, 30, 0.5D, 1D),
        NUKE(1, 43, 0.25D, 1.5D);

        private final int stackLimit;
        private final int drawDuration;
        private final double bounceModifier;
        private final double yeetForce;

        EnumGrenadeShell(
                int stackLimit, int drawDuration, double bounceModifier, double yeetForce) {
            this.stackLimit = stackLimit;
            this.drawDuration = drawDuration;
            this.bounceModifier = bounceModifier;
            this.yeetForce = yeetForce;
        }

        public int getStackLimit() {
            return stackLimit;
        }

        public int getDrawDuration() {
            return drawDuration;
        }

        public double getBounce() {
            return bounceModifier;
        }

        public double getYeetForce() {
            return yeetForce;
        }
    }
}
