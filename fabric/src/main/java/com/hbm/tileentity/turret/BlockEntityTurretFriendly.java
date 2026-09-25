// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.turret;

import com.hbm.blocks.ModBlockEntities;
import com.hbm.handler.CasingEjector;
import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.items.weapon.sedna.factory.XFactory556mm;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public class BlockEntityTurretFriendly extends BlockEntityTurretChekhov {

    private static final CasingEjector FRIENDLY_EJECTOR =
            new CasingEjector().setMotion(-0.3, 0.6, 0).setAngleRange(0.02F, 0.05F);

    private static @Nullable List<BulletConfig> configs;

    public BlockEntityTurretFriendly(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TURRET_FRIENDLY.get(), pos, state);
    }

    @Override
    protected @Nullable List<BulletConfig> getAmmoList() {
        if (configs == null) {
            configs =
                    List.of(
                            XFactory556mm.r556_sp,
                            XFactory556mm.r556_fmj,
                            XFactory556mm.r556_jhp,
                            XFactory556mm.r556_ap);
        }
        return configs;
    }

    @Override
    public Component getName() {
        return Component.translatable("container.turretFriendly");
    }

    @Override
    public int getDelay() {
        return 5;
    }

    @Override
    protected @Nullable CasingEjector getEjector() {
        return FRIENDLY_EJECTOR;
    }
}
