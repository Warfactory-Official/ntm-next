// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.armor;

import com.hbm.handler.ArmorModHandler;
import com.hbm.handler.ArmorUtil;
import com.hbm.hazard.HazardClass;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class ItemModGasmask extends ItemArmorMod implements IGasMask {

    private final Variant variant;

    public ItemModGasmask(Properties properties, Variant variant) {
        super(properties, ArmorModHandler.HELMET_ONLY, true, false, false, false);
        this.variant = variant;
    }

    @Override
    public HazardClass[] filterBlacklist() {
        return variant == Variant.MONO
                ? new HazardClass[] {
                    HazardClass.GAS_LUNG, HazardClass.GAS_BLISTERING, HazardClass.BACTERIA
                }
                : new HazardClass[] {HazardClass.GAS_BLISTERING};
    }

    @Override
    protected void addSpecificTooltip(ItemStack stack, Consumer<Component> adder) {
        adder.accept(
                Component.translatable("desc.item.armorMod.gasMask")
                        .withStyle(ChatFormatting.GREEN));
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            net.minecraft.world.item.component.TooltipDisplay display,
            Consumer<Component> adder,
            net.minecraft.world.item.TooltipFlag flag) {
        super.appendHoverText(stack, context, display, adder, flag);
        ArmorUtil.addGasMaskTooltip(stack, context, flag, adder);
        ArmorUtil.addGasMaskBlacklist(this, adder);
    }

    @Override
    public void addDescription(List<Component> tooltip, ItemStack stack, ItemStack armor) {
        tooltip.add(
                Component.translatable("desc.item.armorMod.gasMask.installed", stack.getHoverName())
                        .withStyle(ChatFormatting.GREEN));
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (ArmorUtil.ejectGasMaskFilter(player, player.getItemInHand(hand)))
            return InteractionResult.SUCCESS;
        return super.use(level, player, hand);
    }

    public Variant variant() {
        return variant;
    }

    public enum Variant {
        STANDARD,
        MONO
    }
}
