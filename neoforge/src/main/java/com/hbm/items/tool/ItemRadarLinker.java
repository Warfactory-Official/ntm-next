// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.tool;

import com.hbm.api.block.IRadarCommandReceiver;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.items.ModDataComponents;
import com.hbm.sound.ModSounds;
import com.hbm.tileentity.machine.BlockEntityMachineRadarScreen;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jspecify.annotations.Nullable;

public class ItemRadarLinker extends Item {

    public ItemRadarLinker(Properties props) {
        super(props);
    }

    public static @Nullable BlockPos getPosition(ItemStack stack) {
        Long packed = stack.get(ModDataComponents.RADAR_LINKER_TARGET.get());
        return packed == null ? null : BlockPos.of(packed);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();

        BlockPos target = MultiblockSurface.coreOfAny(level, ctx.getClickedPos());
        if (target == null) target = ctx.getClickedPos();

        BlockEntity be = level.getBlockEntity(target);
        if (!(be instanceof IRadarCommandReceiver)
                && !(be instanceof BlockEntityMachineRadarScreen)) {
            return InteractionResult.PASS;
        }

        if (!level.isClientSide()) {
            ctx.getItemInHand().set(ModDataComponents.RADAR_LINKER_TARGET.get(), target.asLong());
            Player player = ctx.getPlayer();
            if (player != null) {
                level.playSound(
                        null,
                        player.blockPosition(),
                        ModSounds.TECH_BLEEP.get(),
                        SoundSource.PLAYERS,
                        1.0F,
                        1.0F);
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            Item.TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> adder,
            TooltipFlag flag) {
        BlockPos target = getPosition(stack);
        if (target == null) {
            adder.accept(
                    Component.translatable("desc.item.radarLinker.noPositionSet")
                            .withStyle(ChatFormatting.RED));
            return;
        }
        adder.accept(Component.literal("X: " + target.getX()));
        adder.accept(Component.literal("Y: " + target.getY()));
        adder.accept(Component.literal("Z: " + target.getZ()));
    }
}
