// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.tool;

import com.hbm.api.item.IDesignatorItem;
import com.hbm.blocks.bomb.LaunchPad;
import com.hbm.items.ModDataComponents;
import com.hbm.sound.ModSounds;
import java.util.function.Consumer;
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

public class ItemDesignator extends Item implements IDesignatorItem {

    public ItemDesignator(Properties props) {
        super(props);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (level.getBlockState(pos).getBlock() instanceof LaunchPad) return InteractionResult.PASS;
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        context.getItemInHand()
                .set(
                        ModDataComponents.TARGET_DESIGNATOR.get(),
                        IDesignatorItem.pack(pos.getX(), pos.getZ()));
        Player player = context.getPlayer();
        if (player != null) {
            level.playSound(
                    null,
                    player.getX(),
                    player.getY(),
                    player.getZ(),
                    ModSounds.TECH_BLEEP.get(),
                    SoundSource.PLAYERS,
                    1.0F,
                    1.0F);
            player.sendSystemMessage(Component.translatable("desc.item.designator.positionSet"));
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide()) {
            player.getItemInHand(hand)
                    .set(
                            ModDataComponents.TARGET_DESIGNATOR.get(),
                            IDesignatorItem.pack(player.getBlockX(), player.getBlockZ()));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean isReady(ItemStack stack) {
        return stack.has(ModDataComponents.TARGET_DESIGNATOR.get());
    }

    @Override
    public int getTargetX(ItemStack stack) {
        return IDesignatorItem.unpackX(
                stack.getOrDefault(ModDataComponents.TARGET_DESIGNATOR.get(), 0L));
    }

    @Override
    public int getTargetZ(ItemStack stack) {
        return IDesignatorItem.unpackZ(
                stack.getOrDefault(ModDataComponents.TARGET_DESIGNATOR.get(), 0L));
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            Item.TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> adder,
            TooltipFlag flag) {
        if (isReady(stack)) {
            adder.accept(Component.translatable("item.hbm.designator.target_coord"));
            adder.accept(Component.literal("X: " + getTargetX(stack)));
            adder.accept(Component.literal("Z: " + getTargetZ(stack)));
        } else {
            adder.accept(Component.translatable("item.hbm.designator.choose_target"));
        }
    }
}
