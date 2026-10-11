// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.machine;

import com.hbm.blockentity.machine.BlockEntityMachineSatLink;
import com.hbm.blocks.ITickingBlock;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.interfaces.ILookOverlay;
import com.hbm.items.ISatChip;
import com.hbm.sound.EntitySounds;
import com.hbm.sound.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

public final class MachineSatLink extends BlockMultiblockCore
        implements ITickingBlock, ILookOverlay {
    public MachineSatLink(Properties properties) {
        super(properties);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected InteractionResult useItemOnAtCore(
            ItemStack held,
            BlockState coreState,
            Level level,
            BlockPos core,
            Player player,
            InteractionHand hand,
            BlockHitResult hit) {
        if (player.isSecondaryUseActive() || !(held.getItem() instanceof ISatChip))
            return InteractionResult.PASS;
        if (!level.isClientSide()) {
            BlockEntityMachineSatLink link = (BlockEntityMachineSatLink) level.getBlockEntity(core);
            link.setFrequency(ISatChip.getFreqS(held));
            player.sendSystemMessage(
                    Component.translatable("chat.machineSatLink.frequencySet", link.freq)
                            .withStyle(ChatFormatting.YELLOW));
            EntitySounds.play(
                    player,
                    player.blockPosition(),
                    ModSounds.TECH_BLEEP.get(),
                    SoundSource.PLAYERS,
                    1.0F,
                    1.0F);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useAtCore(
            BlockState coreState, Level level, BlockPos core, Player player, BlockHitResult hit) {
        return InteractionResult.PASS;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityMachineSatLink(pos, state);
    }

    @Override
    public void buildLookOverlay(Level level, BlockPos pos, LookInfo info) {
        BlockEntityMachineSatLink link = (BlockEntityMachineSatLink) level.getBlockEntity(pos);
        info.title(getName().getString(), 0xFFFF00, 0x404000);
        info.line(
                Component.empty()
                        .append(
                                Component.translatable(
                                        "desc.block.machineSatLink.frequency", link.freq)));
        String answer =
                (link.connected ? ChatFormatting.GREEN : ChatFormatting.RED)
                        + Component.translatable(
                                        link.connected
                                                ? "tile.machine_satlink.yes"
                                                : "tile.machine_satlink.no")
                                .getString();
        info.line(
                Component.empty()
                        .append(
                                Component.translatable(
                                        "desc.block.machineSatLink.connected", answer)));
        for (Component line : link.info) info.line(line.getString());
    }
}
