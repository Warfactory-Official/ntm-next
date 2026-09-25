// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.machine;

import com.hbm.blocks.machine.rbmk.RBMKBase;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.handler.neutron.NeutronNodeWorld;
import com.hbm.handler.neutron.RBMKNeutronHandler.RBMKNeutronNode;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;

public class ItemRBMKLid extends Item {

    public final RBMKBase.Lid lid;

    public ItemRBMKLid(Properties properties, RBMKBase.Lid lid) {
        super(properties);
        this.lid = lid;
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        BlockPos pos = ctx.getClickedPos();
        if (!(level.getBlockState(pos).getBlock() instanceof RBMKBase rbmk) || rbmk.hasOwnLid()) {
            return InteractionResult.PASS;
        }
        BlockPos core = MultiblockSurface.coreOfAny(level, pos);
        if (core == null) return InteractionResult.PASS;
        BlockState coreState = level.getBlockState(core);
        if (RBMKBase.lidOf(coreState).present()) return InteractionResult.PASS;

        if (level instanceof ServerLevel server) {

            SoundType sound = lid == RBMKBase.Lid.GLASS ? SoundType.GLASS : SoundType.STONE;
            level.playSound(
                    null,
                    core,
                    sound.getPlaceSound(),
                    SoundSource.BLOCKS,
                    (sound.getVolume() + 1.0F) / 2.0F,
                    sound.getPitch() * 0.8F);
            rbmk.writeLid(server, core, coreState, lid);
            if (NeutronNodeWorld.getNode(level, core) instanceof RBMKNeutronNode node)
                node.addLid();
            ctx.getItemInHand().consume(1, ctx.getPlayer());
        }
        return InteractionResult.SUCCESS;
    }
}
