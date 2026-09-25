// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.bomb;

import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.explosion.vanillant.standard.BlockAllocatorStandard;
import com.hbm.explosion.vanillant.standard.BlockProcessorStandard;
import com.hbm.explosion.vanillant.standard.EntityProcessorStandard;
import com.hbm.explosion.vanillant.standard.PlayerProcessorStandard;
import com.hbm.particle.helper.ExplosionCreator;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.jspecify.annotations.Nullable;

public class BlockChargeC4 extends BlockChargeBase {

    public BlockChargeC4(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public BombReturnCode explode(Level level, BlockPos pos, @Nullable Entity detonator) {
        if (level.isClientSide()) return BombReturnCode.UNDEFINED;
        if (!TntRule.explodes(level)) return BombReturnCode.ERROR_TNT_DISABLED;

        removeSafely(level, pos);
        double x = pos.getX() + 0.5D;
        double y = pos.getY() + 0.5D;
        double z = pos.getZ() + 0.5D;
        ExplosionVNT explosion = new ExplosionVNT(level, x, y, z, 15F);
        explosion.setBlockAllocator(new BlockAllocatorStandard(32));
        explosion.setBlockProcessor(new BlockProcessorStandard().setNoDrop());
        explosion.setEntityProcessor(new EntityProcessorStandard());
        explosion.setPlayerProcessor(new PlayerProcessorStandard());
        explosion.explode();
        ExplosionCreator.composeEffectSmall(level, x, pos.getY() + 1.0D, z);
        return BombReturnCode.DETONATED;
    }

    @Override
    public void addChargeTooltip(Consumer<Component> tooltip) {
        super.addChargeTooltip(tooltip);
        tooltip.accept(
                Component.translatable("desc.block.chargeC4.doesNotDropBlocks")
                        .withStyle(ChatFormatting.BLUE));
    }
}
