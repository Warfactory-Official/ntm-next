// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: HbmMods (The Bobcat)
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.machine;

import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.material.Mats;
import com.hbm.inventory.material.NTMMaterial;
import com.hbm.items.ModDataComponents;
import com.hbm.util.BobMathUtil;
import com.hbm.util.EnumUtil;
import com.hbm.util.I18nUtil;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.material.Fluid;

public class ItemICFPellet extends Item {

    public static final Map<Fluid, EnumICFFuel> fluidMap = new HashMap<>();
    public static final Map<NTMMaterial, EnumICFFuel> materialMap = new HashMap<>();

    public ItemICFPellet(Properties properties) {
        super(properties);
    }

    public static void init() {
        if (!fluidMap.isEmpty() && !materialMap.isEmpty()) return;
        fluidMap.put(NTMFluids.HYDROGEN, EnumICFFuel.HYDROGEN);
        fluidMap.put(NTMFluids.DEUTERIUM, EnumICFFuel.DEUTERIUM);
        fluidMap.put(NTMFluids.TRITIUM, EnumICFFuel.TRITIUM);
        fluidMap.put(NTMFluids.HELIUM3, EnumICFFuel.HELIUM3);
        fluidMap.put(NTMFluids.HELIUM4, EnumICFFuel.HELIUM4);
        materialMap.put(Mats.MAT_LITHIUM, EnumICFFuel.LITHIUM);
        materialMap.put(Mats.MAT_BERYLLIUM, EnumICFFuel.BERYLLIUM);
        materialMap.put(Mats.MAT_BORON, EnumICFFuel.BORON);
        materialMap.put(Mats.MAT_GRAPHITE, EnumICFFuel.CARBON);
        fluidMap.put(NTMFluids.OXYGEN, EnumICFFuel.OXYGEN);
        materialMap.put(Mats.MAT_SODIUM, EnumICFFuel.SODIUM);
        fluidMap.put(NTMFluids.CHLORINE, EnumICFFuel.CHLORINE);
        materialMap.put(Mats.MAT_CALCIUM, EnumICFFuel.CALCIUM);
    }

    public static long getMaxDepletion(ItemStack stack) {
        long base = 50_000_000_000L;
        base /= getType(stack, true).depletionSpeed;
        base /= getType(stack, false).depletionSpeed;
        return base;
    }

    public static long getFusingDifficulty(ItemStack stack) {
        long base = 10_000_000L;
        base *= getType(stack, true).fusingDifficulty * getType(stack, false).fusingDifficulty;
        if (isMuonCatalyzed(stack)) base /= 4;
        return base;
    }

    public static long getDepletion(ItemStack stack) {
        ICFPelletData data = stack.get(ModDataComponents.ICF_PELLET.get());
        return data == null ? 0L : data.depletion();
    }

    public static boolean isMuonCatalyzed(ItemStack stack) {
        ICFPelletData data = stack.get(ModDataComponents.ICF_PELLET.get());
        return data != null && data.muon();
    }

    public static long react(ItemStack stack, long heat) {
        ICFPelletData data = stack.getOrDefault(ModDataComponents.ICF_PELLET.get(), defaultData());
        stack.set(ModDataComponents.ICF_PELLET.get(), data.withDepletion(data.depletion() + heat));
        return (long)
                (heat * getType(stack, true).reactionMult * getType(stack, false).reactionMult);
    }

    public static ItemStack setup(
            ItemStack stack, EnumICFFuel type1, EnumICFFuel type2, boolean muon) {
        ICFPelletData data = stack.getOrDefault(ModDataComponents.ICF_PELLET.get(), defaultData());
        stack.set(
                ModDataComponents.ICF_PELLET.get(),
                new ICFPelletData(type1.ordinal(), type2.ordinal(), muon, data.depletion()));
        return stack;
    }

    private static ICFPelletData defaultData() {
        return new ICFPelletData(
                EnumICFFuel.DEUTERIUM.ordinal(), EnumICFFuel.TRITIUM.ordinal(), false, 0L);
    }

    public static EnumICFFuel getType(ItemStack stack, boolean first) {
        ICFPelletData data = stack.get(ModDataComponents.ICF_PELLET.get());
        if (data == null) return first ? EnumICFFuel.DEUTERIUM : EnumICFFuel.TRITIUM;
        return EnumUtil.grabEnumSafely(EnumICFFuel.class, first ? data.type1() : data.type2());
    }

    public static double getDepletionFraction(ItemStack stack) {
        return (double) getDepletion(stack) / (double) getMaxDepletion(stack);
    }

    public static int getBlendedColor(ItemStack stack) {
        EnumICFFuel type1 = getType(stack, true);
        EnumICFFuel type2 = getType(stack, false);
        int r = (((type1.color & 0xff0000) >> 16) + ((type2.color & 0xff0000) >> 16)) / 2;
        int g = (((type1.color & 0x00ff00) >> 8) + ((type2.color & 0x00ff00) >> 8)) / 2;
        int b = ((type1.color & 0x0000ff) + (type2.color & 0x0000ff)) / 2;
        return r << 16 | g << 8 | b;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return getDepletionFraction(stack) > 0D;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Mth.clamp((int) Math.round(13.0D - getDepletionFraction(stack) * 13.0D), 0, 13);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return Mth.hsvToRgb(
                (float) Math.max(0D, 1D - getDepletionFraction(stack)) / 3.0F, 1.0F, 1.0F);
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            Item.TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> adder,
            TooltipFlag flag) {
        adder.accept(
                Component.translatable(
                                "desc.shared.depletion",
                                String.format(
                                        Locale.US, "%.1f", getDepletionFraction(stack) * 100D))
                        .withStyle(ChatFormatting.GREEN));
        adder.accept(
                Component.translatable(
                                "desc.item.icfPellet.fuel",
                                I18nUtil.resolveKey(getType(stack, true).translationKey()),
                                I18nUtil.resolveKey(getType(stack, false).translationKey()))
                        .withStyle(ChatFormatting.YELLOW));
        adder.accept(
                Component.translatable(
                                "desc.item.icfPellet.heatRequired",
                                BobMathUtil.getShortNumber(getFusingDifficulty(stack)))
                        .withStyle(ChatFormatting.YELLOW));
        adder.accept(
                Component.translatable(
                                "desc.item.icfPellet.reactivityMultiplier",
                                (int)
                                                (getType(stack, true).reactionMult
                                                        * getType(stack, false).reactionMult
                                                        * 100)
                                        / 100D)
                        .withStyle(ChatFormatting.YELLOW));
        if (isMuonCatalyzed(stack)) {
            adder.accept(
                    Component.translatable("desc.item.icfPellet.muonCatalyzed")
                            .withStyle(ChatFormatting.DARK_AQUA));
        }
    }

    public enum EnumICFFuel {
        HYDROGEN(0x4040FF, 1.00D, 0.85D, 1.00D),
        DEUTERIUM(0x2828CB, 1.25D, 1.00D, 1.00D),
        TRITIUM(0x000092, 1.50D, 1.00D, 1.05D),
        HELIUM3(0xFFF09F, 1.75D, 1.00D, 1.25D),
        HELIUM4(0xFF9B60, 2.00D, 1.00D, 1.50D),
        LITHIUM(0xE9E9E9, 1.25D, 0.85D, 2.00D),
        BERYLLIUM(0xA79D80, 2.00D, 1.00D, 2.50D),
        BORON(0x697F89, 3.00D, 0.50D, 3.50D),
        CARBON(0x454545, 2.00D, 1.00D, 5.00D),
        OXYGEN(0xB4E2FF, 1.25D, 1.50D, 7.50D),
        SODIUM(0xDFE4E7, 3.00D, 0.75D, 8.75D),

        CHLORINE(0xDAE598, 2.50D, 1.00D, 9.25D),
        CALCIUM(0xD2C7A9, 3.00D, 1.00D, 9.75D),
        ;

        public static final EnumICFFuel[] VALUES = values();

        public final int color;
        public final double reactionMult;
        public final double depletionSpeed;
        public final double fusingDifficulty;

        EnumICFFuel(int color, double react, double depl, double laser) {
            this.color = color;
            this.reactionMult = react;
            this.depletionSpeed = depl;
            this.fusingDifficulty = laser;
        }

        public String translationKey() {
            return "icffuel." + name().toLowerCase(Locale.US);
        }
    }
}
