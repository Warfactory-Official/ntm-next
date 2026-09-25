// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.machine;

import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.items.ModDataComponents;
import com.hbm.sound.ModSounds;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

public class ItemReactorSensor extends Item {

    public ItemReactorSensor(Properties props) {
        super(props);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();

        BlockPos core = MultiblockSurface.coreOfAny(level, pos, level.getBlockState(pos));
        if (core == null || !level.getBlockState(core).is(ModBlocks.REACTOR_RESEARCH.get())) {
            return InteractionResult.PASS;
        }

        context.getItemInHand().set(ModDataComponents.REACTOR_POS.get(), pos.asLong());
        Player player = context.getPlayer();
        if (!level.isClientSide() && player != null) {
            player.sendSystemMessage(
                    Component.literal("[")
                            .append(Component.translatable("item.hbm.reactor_sensor"))
                            .append("] ")
                            .withStyle(ChatFormatting.DARK_AQUA)
                            .append(
                                    Component.translatable("chat.reactorSensor.positionSet")
                                            .withStyle(ChatFormatting.GREEN)));
        }
        level.playSound(null, pos, ModSounds.TECH_BOOP.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        if (player != null) player.swing(InteractionHand.MAIN_HAND);
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> adder,
            TooltipFlag flag) {
        Long packed = stack.get(ModDataComponents.REACTOR_POS.get());
        if (packed == null) {
            adder.accept(Component.translatable("desc.item.reactorSensor.noReactor"));
            return;
        }
        BlockPos pos = BlockPos.of(packed);
        adder.accept(Component.literal("x: " + pos.getX()));
        adder.accept(Component.literal("y: " + pos.getY()));
        adder.accept(Component.literal("z: " + pos.getZ()));
    }
}
