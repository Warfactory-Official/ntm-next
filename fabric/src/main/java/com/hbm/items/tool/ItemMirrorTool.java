// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.tool;

import com.hbm.blocks.machine.MachineSolarBoiler;
import com.hbm.blocks.machine.SolarMirror;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.items.ModDataComponents;
import com.hbm.tileentity.machine.BlockEntitySolarMirror;
import com.hbm.util.I18nUtil;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

public class ItemMirrorTool extends Item {

    public ItemMirrorTool(Properties props) {
        super(props);
    }

    private static void message(Player player, String key, ChatFormatting color) {
        if (player != null) player.sendSystemMessage(Component.translatable(key).withStyle(color));
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        BlockPos pos = ctx.getClickedPos();
        Block b = level.getBlockState(pos).getBlock();
        ItemStack stack = ctx.getItemInHand();
        Player player = ctx.getPlayer();

        BlockPos boilerCore = MultiblockSurface.coreOfAny(level, pos);
        if (boilerCore != null
                && level.getBlockState(boilerCore).getBlock() instanceof MachineSolarBoiler) {
            if (!level.isClientSide()) {

                stack.set(ModDataComponents.MIRROR_TOOL_TARGET.get(), boilerCore.above().asLong());
                message(player, "item.hbm.mirror_tool.linked", ChatFormatting.YELLOW);
            }
            return InteractionResult.SUCCESS;
        }

        if (b instanceof SolarMirror) {
            Long target = stack.get(ModDataComponents.MIRROR_TOOL_TARGET.get());
            if (target == null) return InteractionResult.PASS;
            if (!level.isClientSide()
                    && level.getBlockEntity(pos) instanceof BlockEntitySolarMirror mirror) {
                BlockPos t = BlockPos.of(target);
                int dx = pos.getX() - t.getX();
                int dy = pos.getY() - t.getY();
                int dz = pos.getZ() - t.getZ();

                boolean withinReach = Math.sqrt(dx * dx + dy * dy + dz * dz) <= 100;

                boolean withinAngle = dx * dx + dz * dz <= dy * dy;

                if (!withinReach) message(player, "item.hbm.mirror_tool.reach", ChatFormatting.RED);
                else if (!withinAngle)
                    message(player, "item.hbm.mirror_tool.angle", ChatFormatting.RED);
                else mirror.setTarget(t.getX(), t.getY(), t.getZ());
            }
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            Item.TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> adder,
            TooltipFlag flag) {
        for (String line : I18nUtil.loreLines(this.getDescriptionId() + ".desc")) {
            adder.accept(Component.literal(line).withStyle(ChatFormatting.YELLOW));
        }
    }
}
