// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.tool;

import com.hbm.extprop.HbmPlayerProps;
import com.hbm.handler.EnumToolKey;
import com.hbm.interfaces.ICopiable;
import com.hbm.items.ModDataComponents;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

public class ItemSettingsTool extends Item {

    public ItemSettingsTool(Properties properties) {
        super(properties);
    }

    public static InteractionResult useFirst(ItemStack stack, UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null || player.isShiftKeyDown()) return InteractionResult.PASS;
        BlockPos pos = context.getClickedPos();
        ICopiable copiable = findSource(context.getLevel(), pos);
        return copiable == null
                ? InteractionResult.PASS
                : pasteSettings(copiable, context.getLevel(), pos, player, stack);
    }

    public static @Nullable CompoundTag getData(ItemStack stack) {
        CustomData data = stack.get(ModDataComponents.SETTINGS_TOOL_DATA.get());
        return data == null ? null : data.copyTag();
    }

    public static InteractionResult pasteSettings(
            ICopiable copiable, Level level, BlockPos pos, Player player, ItemStack stack) {
        if (level.isClientSide()) return InteractionResult.PASS;
        CompoundTag settings = getData(stack);
        if (settings != null) {
            copiable.pasteSettings(settings, settings.getIntOr("copyIndex", 0), level, player, pos);
        }
        return InteractionResult.SUCCESS;
    }

    public static @Nullable ICopiable findSource(Level level, BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof ICopiable copiable) return copiable;
        return level.getBlockState(pos).getBlock() instanceof ICopiable copiable
                        && copiable.copiable(level, pos)
                ? copiable
                : null;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        ICopiable copiable = findSource(level, pos);
        if (copiable == null) return InteractionResult.PASS;

        Player player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;
        ItemStack stack = context.getItemInHand();

        if (player.isShiftKeyDown()) {
            CompoundTag settings = copiable.getSettings(level, pos);
            if (level.isClientSide()) {
                if (settings != null) {
                    player.sendSystemMessage(
                            prefix().append(
                                            Component.translatable(
                                                            "desc.item.settingsTool.copiedSettingsOf")
                                                    .withStyle(ChatFormatting.AQUA))
                                    .append(
                                            copiable.getSettingsSourceDisplay(level, pos)
                                                    .copy()
                                                    .withStyle(ChatFormatting.AQUA)));
                }
                return InteractionResult.PASS;
            }

            if (settings == null) {
                stack.remove(ModDataComponents.SETTINGS_TOOL_DATA.get());
                player.sendSystemMessage(
                        prefix().append(
                                        Component.translatable(
                                                        "desc.item.settingsTool.copyFailedMachineHas")
                                                .withStyle(ChatFormatting.RED))
                                .append(
                                        copiable.getSettingsSourceDisplay(level, pos)
                                                .copy()
                                                .withStyle(ChatFormatting.RED)));
                return InteractionResult.SUCCESS;
            }

            settings = settings.copy();
            settings.putString("tileName", copiable.getSettingsSourceID(level, pos));
            settings.putInt("copyIndex", 0);
            settings.putInt("inputDelay", 0);

            String[] info = copiable.infoForDisplay(level, pos);
            if (info != null) {
                ListTag displayInfo = new ListTag();
                for (String key : info) {
                    CompoundTag line = new CompoundTag();
                    line.putString("info", key);
                    displayInfo.add(line);
                }
                settings.put("displayInfo", displayInfo);
            }

            CustomData.set(ModDataComponents.SETTINGS_TOOL_DATA.get(), stack, settings);
            return InteractionResult.SUCCESS;
        }

        if (level.isClientSide()) return InteractionResult.PASS;
        return pasteSettings(copiable, level, pos, player, stack);
    }

    @Override
    public void inventoryTick(
            ItemStack stack, ServerLevel level, Entity owner, @Nullable EquipmentSlot slot) {
        if (!(owner instanceof Player player) || player.getMainHandItem() != stack) return;
        CompoundTag data = getData(stack);
        if (data == null) return;

        int delay = data.getIntOr("inputDelay", 0) + 1;
        if (HbmPlayerProps.getData(player).isToolKeyPressed(EnumToolKey.ALT) && delay > 4) {
            int count = data.getListOrEmpty("displayInfo").size();
            int index = data.getIntOr("copyIndex", 0) + 1;
            if (index > count - 1) index = 0;
            data.putInt("copyIndex", index);
            delay = 0;
        }
        data.putInt("inputDelay", delay);
        CustomData.set(ModDataComponents.SETTINGS_TOOL_DATA.get(), stack, data);
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> adder,
            TooltipFlag flag) {
        adder.accept(Component.translatable("desc.item.settingsTool.canCopyTheSettings"));
        adder.accept(Component.translatable("desc.item.settingsTool.shiftRightClickTo"));
        adder.accept(Component.translatable("desc.item.settingsTool.ctrlClickOnPipes"));

        CompoundTag data = getData(stack);
        if (data == null) return;
        String source = data.getStringOr("tileName", "");
        if (source.isEmpty())
            adder.accept(
                    Component.translatable("desc.item.settingsTool.none")
                            .withStyle(ChatFormatting.RED));
        else adder.accept(Component.translatable(source).withStyle(ChatFormatting.BLUE));
    }

    private MutableComponent prefix() {
        return Component.literal("[")
                .withStyle(ChatFormatting.DARK_AQUA)
                .append(getName(new ItemStack(this)).copy().withStyle(ChatFormatting.DARK_AQUA))
                .append(Component.literal("] ").withStyle(ChatFormatting.DARK_AQUA));
    }
}
