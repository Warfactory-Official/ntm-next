// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.machine;

import com.hbm.inventory.fluid.FluidStackNTM;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.inventory.fluid.NTMFluidProperty;
import com.hbm.inventory.fluid.trait.CD_Canister;
import com.hbm.inventory.fluid.trait.CD_Gastank;
import com.hbm.inventory.fluid.trait.FluidTraitSimple.FT_LeadContainer;
import com.hbm.inventory.fluid.trait.FluidTraitSimple.FT_NoContainer;
import com.hbm.items.ModDataComponents;
import com.hbm.items.StackRemainderItem;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.jspecify.annotations.Nullable;

public class ItemFluidTank extends StackRemainderItem implements IFluidContainerItem {

    public final int capacity;
    private final String base;
    private final Family family;

    public ItemFluidTank(Item.Properties properties, int capacity, String base) {
        this(properties, capacity, base, Family.TANK);
    }

    public ItemFluidTank(Item.Properties properties, int capacity, String base, Family family) {
        super(properties);
        this.capacity = capacity;
        this.base = base;
        this.family = family;
    }

    @Override
    protected @Nullable ItemStackTemplate remainder(ItemStack consumed) {
        return getContent(consumed).type() == Fluids.EMPTY ? null : new ItemStackTemplate(this);
    }

    @Override
    public boolean canStore(Fluid type) {
        if (type == null || type == Fluids.EMPTY || NTMFluidProperties.get(type) == null)
            return false;
        return switch (family) {
            case CANISTER -> NTMFluidProperties.getTrait(type, CD_Canister.class) != null;
            case GAS -> NTMFluidProperties.getTrait(type, CD_Gastank.class) != null;
            case LEAD, PACK -> !NTMFluidProperties.hasTrait(type, FT_NoContainer.class);
            case DISPERSER -> NTMFluidProperties.isDispersable(type);
            case TANK, BARREL ->
                    !NTMFluidProperties.hasTrait(type, FT_NoContainer.class)
                            && !NTMFluidProperties.hasTrait(type, FT_LeadContainer.class);
        };
    }

    @Override
    public boolean machineConvertible() {
        return family != Family.PACK;
    }

    @Override
    public int capacity(ItemStack stack) {
        return capacity;
    }

    @Override
    public FluidStackNTM getContent(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.FLUID_CONTENT.get(), EMPTY_CONTENT);
    }

    @Override
    public void setContent(ItemStack stack, FluidStackNTM content) {
        if (content == null || content.type() == Fluids.EMPTY || content.amount() <= 0) {
            stack.remove(ModDataComponents.FLUID_CONTENT.get());
            return;
        }
        long clamped = Math.min(content.amount(), capacity);
        stack.set(
                ModDataComponents.FLUID_CONTENT.get(),
                new FluidStackNTM(content.type(), clamped, content.pressure()));
    }

    @Override
    public int fill(ItemStack stack, Fluid type, int amount, int pressure) {
        if (type == null || type == Fluids.EMPTY || amount <= 0) return 0;
        if (!canStore(type)) return 0;
        FluidStackNTM held = getContent(stack);
        boolean empty = held.type() == Fluids.EMPTY;
        if (!empty && (held.type() != type || held.pressure() != pressure)) return 0;
        int fill = (int) held.amount();
        int accepted = Math.min(amount, capacity - fill);
        if (accepted <= 0) return 0;
        setContent(stack, new FluidStackNTM(type, fill + accepted, pressure));
        return accepted;
    }

    @Override
    public int drain(ItemStack stack, Fluid type, int amount, int pressure) {
        if (type == null || amount <= 0) return 0;
        FluidStackNTM held = getContent(stack);
        if (held.type() != type || held.pressure() != pressure || held.amount() <= 0) return 0;
        int taken = (int) Math.min(amount, held.amount());
        setContent(stack, new FluidStackNTM(type, held.amount() - taken, pressure));
        return taken;
    }

    public ItemStack make(Fluid type, int amount) {
        ItemStack stack = new ItemStack(this);
        setContent(stack, new FluidStackNTM(type, amount));
        return stack;
    }

    @Override
    public Component getName(ItemStack stack) {
        FluidStackNTM held = getContent(stack);
        if (held.type() == Fluids.EMPTY || held.amount() <= 0) {
            return Component.translatable("item.hbm." + base + ".empty");
        }
        return Component.translatable(
                "item.hbm." + base + ".not_empty", NTMFluidProperties.getDisplayName(held.type()));
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        int fill = getFill(stack);
        return fill > 0 && fill < capacity;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(getFill(stack) * 13.0F / capacity);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        NTMFluidProperty prop = NTMFluidProperties.get(getContent(stack).type());
        return prop != null ? ARGB.transparent(prop.color()) : 0x3399FF;
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            Item.TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> adder,
            TooltipFlag flag) {

        String s = getFill(stack) + "/" + capacity + " mB";
        if (stack.getCount() > 1) s = stack.getCount() + "x " + s;
        adder.accept(Component.literal(s).withStyle(ChatFormatting.GRAY));
    }

    public enum Family {
        TANK,
        BARREL,
        LEAD,
        CANISTER,
        GAS,
        PACK,
        DISPERSER
    }
}
