// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.tool;

import com.hbm.blocks.machine.rbmk.RBMKBase;
import com.hbm.blocks.machine.rbmk.RBMKConsole;
import com.hbm.blocks.machine.rbmk.RBMKCraneConsole;
import com.hbm.blocks.machine.rbmk.RBMKDisplay;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.items.ModDataComponents;
import com.hbm.tileentity.machine.rbmk.BlockEntityCraneConsole;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKConsole;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKDisplay;
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
import net.minecraft.world.level.block.entity.BlockEntity;

public class ItemRBMKTool extends Item {

    public ItemRBMKTool(Properties props) {
        super(props);
    }

    private static void message(Player player, String key) {
        if (player != null)
            player.sendSystemMessage(Component.translatable(key).withStyle(ChatFormatting.YELLOW));
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        BlockPos pos = ctx.getClickedPos();
        Block b = level.getBlockState(pos).getBlock();
        ItemStack stack = ctx.getItemInHand();
        Player player = ctx.getPlayer();

        if (b instanceof RBMKBase) {
            if (!level.isClientSide()) {

                BlockPos core = MultiblockSurface.coreOfAny(level, pos);
                if (core != null) {
                    stack.set(ModDataComponents.RBMK_TOOL_TARGET.get(), core.asLong());
                    message(player, "item.hbm.rbmk_tool.linked");
                }
            }
            return InteractionResult.SUCCESS;
        }

        if (b instanceof RBMKDisplay && stack.has(ModDataComponents.RBMK_TOOL_TARGET.get())) {
            if (!level.isClientSide()
                    && level.getBlockEntity(pos) instanceof BlockEntityRBMKDisplay display) {
                BlockPos t = BlockPos.of(stack.get(ModDataComponents.RBMK_TOOL_TARGET.get()));
                display.setTarget(t.getX(), t.getY(), t.getZ());
                message(player, "item.hbm.rbmk_tool.set");
            }
            return InteractionResult.SUCCESS;
        }

        BlockPos consoleCore = MultiblockSurface.coreOfAny(level, pos);
        if (consoleCore == null) return InteractionResult.PASS;
        Block coreBlock = level.getBlockState(consoleCore).getBlock();
        if (!(coreBlock instanceof RBMKConsole) && !(coreBlock instanceof RBMKCraneConsole)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide()) {
            Long target = stack.get(ModDataComponents.RBMK_TOOL_TARGET.get());
            BlockEntity be = level.getBlockEntity(consoleCore);
            if (target != null) {
                BlockPos t = BlockPos.of(target);
                if (be instanceof BlockEntityRBMKConsole console) {
                    console.setTarget(t.getX(), t.getY(), t.getZ());
                    message(player, "item.hbm.rbmk_tool.set");
                } else if (be instanceof BlockEntityCraneConsole crane) {
                    crane.setTarget(t.getX(), t.getY(), t.getZ());
                    message(player, "item.hbm.rbmk_tool.set");
                }
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
        for (String line : I18nUtil.loreLines(this.getDescriptionId() + ".desc")) {
            adder.accept(Component.literal(line).withStyle(ChatFormatting.YELLOW));
        }
    }
}
