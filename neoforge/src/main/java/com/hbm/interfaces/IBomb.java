// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: HbmMods (The Bobcat)
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.interfaces;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

public interface IBomb {

    BombReturnCode explode(Level level, BlockPos pos, @Nullable Entity detonator);

    enum BombReturnCode {
        UNDEFINED(false, ""),
        DETONATED(true, "bomb.detonated"),
        TRIGGERED(true, "bomb.triggered"),
        LAUNCHED(true, "bomb.launched"),
        ERROR_MISSING_COMPONENT(false, "bomb.missingComponent"),
        ERROR_INCOMPATIBLE(false, "bomb.incompatible"),
        ERROR_NO_BOMB(false, "bomb.nobomb"),

        ERROR_TNT_DISABLED(false, "block.minecraft.tnt.disabled");

        private final String unloc;
        private final boolean success;

        BombReturnCode(boolean success, String unloc) {
            this.success = success;
            this.unloc = unloc;
        }

        public String getUnlocalizedMessage() {
            return this.unloc;
        }

        public boolean wasSuccessful() {
            return this.success;
        }
    }
}
