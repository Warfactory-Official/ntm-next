// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.tool;

import com.hbm.items.ModDataComponents;
import com.hbm.sound.ModSounds;
import com.hbm.tileentity.machine.BlockEntityMachineTeleporter;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
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

public class ItemTeleLink extends Item {

    public ItemTeleLink(Properties props) {
        super(props);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Player player = ctx.getPlayer();
        if (player == null || player.isShiftKeyDown()) return InteractionResult.PASS;

        Level level = ctx.getLevel();
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        BlockPos pos = ctx.getClickedPos();
        ItemStack stack = ctx.getItemInHand();

        if (!(level.getBlockEntity(pos) instanceof BlockEntityMachineTeleporter tele)) {
            stack.set(ModDataComponents.LINKER_TARGET.get(), GlobalPos.of(level.dimension(), pos));
            level.playSound(
                    null,
                    player.blockPosition(),
                    ModSounds.TECH_BLEEP.get(),
                    SoundSource.PLAYERS,
                    1.0F,
                    1.0F);
            player.sendSystemMessage(
                    Component.translatable(
                                    "desc.item.teleLink.setTeleporterExitTo",
                                    pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + ".")
                            .withStyle(ChatFormatting.AQUA));
            return InteractionResult.SUCCESS;
        }

        GlobalPos target = stack.get(ModDataComponents.LINKER_TARGET.get());
        if (target == null) {
            level.playSound(
                    null,
                    player.blockPosition(),
                    ModSounds.TECH_BOOP.get(),
                    SoundSource.PLAYERS,
                    1.0F,
                    1.0F);
            player.sendSystemMessage(
                    Component.translatable("desc.item.teleLink.noDestinationSet")
                            .withStyle(ChatFormatting.RED));
            return InteractionResult.FAIL;
        }

        tele.setTarget(target);
        level.playSound(
                null,
                player.blockPosition(),
                ModSounds.TECH_BLEEP.get(),
                SoundSource.PLAYERS,
                1.0F,
                1.0F);
        player.sendSystemMessage(
                Component.translatable("desc.item.teleLink.teleportersDestinationHas")
                        .withStyle(ChatFormatting.AQUA));
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            Item.TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> adder,
            TooltipFlag flag) {
        GlobalPos target = stack.get(ModDataComponents.LINKER_TARGET.get());
        if (target == null) {
            adder.accept(
                    Component.translatable("desc.item.teleLink.selectExitLocationFirst")
                            .withStyle(ChatFormatting.RED));
            return;
        }
        adder.accept(Component.literal("X: " + target.pos().getX()));
        adder.accept(Component.literal("Y: " + target.pos().getY()));
        adder.accept(Component.literal("Z: " + target.pos().getZ()));
        adder.accept(Component.literal("D: " + target.dimension().identifier()));
    }
}
