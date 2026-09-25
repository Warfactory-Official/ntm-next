// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.hazard.type;

import com.hbm.config.BalanceConfig;
import com.hbm.config.RadiationConfig;
import com.hbm.hazard.helper.HazardHelper;
import com.hbm.hazard.modifier.IHazardModifier;
import com.hbm.lib.Library;
import com.hbm.util.BobMathUtil;
import com.hbm.util.ContaminationUtil.ContaminationType;
import com.hbm.util.ContaminationUtil.HazardType;
import com.hbm.util.ContaminationUtil;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public class HazardTypeRadiation implements IHazardType {

    public static double getNewValue(double radiation) {
        if (radiation < 1_000_000D) return radiation;
        if (radiation < 1_000_000_000D) return radiation * 0.000001D;
        return radiation * 0.000000001D;
    }

    private static String getSuffixKey(double radiation) {
        if (radiation < 1_000_000D) return null;
        if (radiation < 1_000_000_000D) return "desc.mil";
        return "desc.bil";
    }

    public static void appendRadiationLines(List<Component> list, double perItem, int count) {
        list.add(
                Component.literal("[")
                        .append(Component.translatable("trait.radioactive"))
                        .append("]")
                        .withStyle(ChatFormatting.GREEN));
        list.add(radLine(perItem, null));
        if (count > 1) list.add(radLine(perItem * count, "desc.stack"));
    }

    private static Component radLine(double rads, @Nullable String prefixKey) {
        String suffixKey = getSuffixKey(rads);
        String value = " " + Library.roundFloat(getNewValue(rads), 3);
        MutableComponent line =
                prefixKey == null
                        ? Component.literal(value)
                        : Component.empty()
                                .append(" ")
                                .append(Component.translatable(prefixKey))
                                .append(value);
        if (suffixKey != null) line.append(Component.translatable(suffixKey));
        return line.append(" ")
                .append(Component.translatable("desc.rads"))
                .withStyle(ChatFormatting.YELLOW);
    }

    @Override
    public void onUpdate(LivingEntity target, double level, ItemStack stack) {
        boolean reacher = HazardHelper.isHoldingReacher(target);
        level *= stack.getCount();
        if (level <= 0) return;

        double rad = level / 20D;
        if (BalanceConfig.enable528 && reacher) {
            rad = rad / 49D;
        } else if (reacher) {
            rad = BobMathUtil.sqrt(rad);
        }

        ContaminationUtil.contaminate(
                target,
                HazardType.RADIATION,
                ContaminationType.CREATIVE,
                rad * RadiationConfig.hazardRate);
    }

    @Override
    public void updateEntity(ItemEntity item, double level) {}

    @Override
    public void addHazardInformation(
            Player player,
            List<Component> list,
            double level,
            ItemStack stack,
            List<IHazardModifier> modifiers) {
        level = IHazardModifier.evalAllModifiers(stack, player, level, modifiers);
        if (level < 1e-5D) return;
        appendRadiationLines(list, level, stack.getCount());
    }
}
