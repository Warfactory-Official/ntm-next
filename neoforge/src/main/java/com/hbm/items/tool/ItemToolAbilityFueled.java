// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.tool;

import com.hbm.handler.ability.AvailableAbilities;
import com.hbm.inventory.fluid.FluidStackNTM;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.items.ModDataComponents;
import com.hbm.items.machine.IFluidContainerItem;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

public class ItemToolAbilityFueled extends ItemToolAbility implements IFluidContainerItem {

    private final Supplier<Fluid[]> acceptedFuels;
    private final int maxFuel;
    private final int consumption;
    private final int fillRate;

    public ItemToolAbilityFueled(
            Properties properties,
            AvailableAbilities abilities,
            boolean shears,
            Supplier<Fluid[]> acceptedFuels,
            int maxFuel,
            int consumption,
            int fillRate) {
        super(properties, abilities, false, shears);
        this.acceptedFuels = acceptedFuels;
        this.maxFuel = maxFuel;
        this.consumption = consumption;
        this.fillRate = fillRate;
    }

    public Fluid[] acceptedFuels() {
        return acceptedFuels.get();
    }

    @Override
    public int getFill(ItemStack stack) {
        FluidStackNTM content = stack.get(ModDataComponents.FLUID_CONTENT.get());
        return content == null ? maxFuel : (int) Math.clamp(content.amount(), 0L, maxFuel);
    }

    public void setFill(ItemStack stack, int fill) {
        FluidStackNTM content = stack.get(ModDataComponents.FLUID_CONTENT.get());
        setFill(stack, content == null ? Fluids.EMPTY : content.type(), fill);
    }

    private void setFill(ItemStack stack, Fluid type, int fill) {
        stack.set(
                ModDataComponents.FLUID_CONTENT.get(),
                new FluidStackNTM(type, Math.clamp(fill, 0, maxFuel), 0));
    }

    public ItemStack emptyTool() {
        ItemStack stack = new ItemStack(this);
        setFill(stack, Fluids.EMPTY, 0);
        return stack;
    }

    @Override
    public boolean canOperate(ItemStack stack) {
        return getFill(stack) >= consumption;
    }

    @Override
    protected void spendPerBlock(ItemStack stack, LivingEntity user) {
        setFill(stack, getFill(stack) - consumption);
    }

    @Override
    public boolean mineBlock(
            ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity owner) {
        if (!level.isClientSide() && state.getDestroySpeed(level, pos) != 0.0F)
            spendPerBlock(stack, owner);
        return true;
    }

    @Override
    public void hurtEnemy(ItemStack stack, LivingEntity victim, LivingEntity attacker) {
        super.hurtEnemy(stack, victim, attacker);
        if (!victim.level().isClientSide()) spendPerBlock(stack, attacker);
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return getFill(stack) < maxFuel;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(getFill(stack) * 13F / maxFuel);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return Mth.hsvToRgb((float) getFill(stack) / maxFuel / 3.0F, 1.0F, 1.0F);
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> adder,
            TooltipFlag flag) {
        adder.accept(
                Component.translatable("desc.item.toolAbilityFueled.fuel", getFill(stack), maxFuel)
                        .withStyle(ChatFormatting.GOLD));
        for (Fluid fuel : acceptedFuels()) {
            adder.accept(
                    Component.translatable(
                                    "desc.item.toolAbilityFueled.fuelType",
                                    NTMFluidProperties.getDisplayName(fuel))
                            .withStyle(ChatFormatting.YELLOW));
        }
        super.appendHoverText(stack, context, display, adder, flag);
    }

    @Override
    public boolean machineFillable() {
        return true;
    }

    @Override
    public int capacity(ItemStack stack) {
        return maxFuel;
    }

    @Override
    public FluidStackNTM getContent(ItemStack stack) {
        int fill = getFill(stack);
        if (fill <= 0) return EMPTY_CONTENT;
        FluidStackNTM content = stack.get(ModDataComponents.FLUID_CONTENT.get());
        Fluid type = content == null ? Fluids.EMPTY : content.type();
        return new FluidStackNTM(type == Fluids.EMPTY ? acceptedFuels()[0] : type, fill, 0);
    }

    @Override
    public void setContent(ItemStack stack, FluidStackNTM content) {
        if (content == null) setFill(stack, Fluids.EMPTY, 0);
        else setFill(stack, content.type(), (int) content.amount());
    }

    @Override
    public int fill(ItemStack stack, Fluid type, int amount, int pressure) {
        if (pressure != 0 || !acceptsFluid(type)) return 0;
        int fill = getFill(stack);
        int accepted = Math.min(Math.min(amount, fillRate), maxFuel - fill);
        if (accepted <= 0) return 0;
        setFill(stack, type, fill + accepted);
        return accepted;
    }

    @Override
    public int drain(ItemStack stack, Fluid type, int amount, int pressure) {
        return 0;
    }

    private boolean acceptsFluid(Fluid type) {
        for (Fluid fuel : acceptedFuels()) {
            if (fuel == type) return true;
        }
        return false;
    }
}
