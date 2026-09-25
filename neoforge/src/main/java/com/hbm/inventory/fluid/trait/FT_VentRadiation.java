// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.fluid.trait;

import com.hbm.handler.radiation.RadiationSystemNT;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.util.I18nUtil;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

public class FT_VentRadiation extends FluidTrait {

    private float radPerMB;

    public FT_VentRadiation() {}

    public FT_VentRadiation(float radPerMB) {
        this.radPerMB = radPerMB;
    }

    public float getRadPerMB() {
        return radPerMB;
    }

    @Override
    public void onFluidRelease(
            Level level,
            BlockPos pos,
            FluidTankNTM tank,
            int overflowAmount,
            FluidReleaseType type) {
        RadiationSystemNT.incrementRad((ServerLevel) level, pos, overflowAmount * radPerMB);
    }

    @Override
    public void addInfo(List<String> info) {
        info.add(
                ChatFormatting.YELLOW
                        + "["
                        + I18nUtil.resolveKey("hbmfluid.trait.radioactive")
                        + "]");
    }
}
