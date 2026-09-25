// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.fluid;

import com.hbm.inventory.fluid.trait.FT_Flammable;
import com.hbm.inventory.fluid.trait.FluidTrait;
import java.util.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.material.Fluid;

public class FluidBuilder {

    public static final int ROOM_TEMPERATURE = 20;

    private static final List<FluidBuilder> PENDING = new ArrayList<>();
    private static final Map<String, FluidBuilder> BY_NAME = new HashMap<>();

    private final String name;
    private final int color;
    private final int poison;
    private final int flammability;
    private final int reactivity;
    private final EnumSymbol symbol;
    private final Map<Class<? extends FluidTrait>, FluidTrait> traits = new LinkedHashMap<>();
    private int temperature = ROOM_TEMPERATURE;
    private final List<String> shares = new ArrayList<>();

    public FluidBuilder(
            String name,
            int color,
            int poison,
            int flammability,
            int reactivity,
            EnumSymbol symbol) {
        this.name = name;
        this.color = color;
        this.poison = poison;
        this.flammability = flammability;
        this.reactivity = reactivity;
        this.symbol = symbol;
        BY_NAME.put(name, this);
    }

    public static FluidBuilder get(String name) {
        return BY_NAME.get(name);
    }

    public static void declareAll() {
        for (FluidBuilder b : PENDING) {
            Fluid fluid = NTMFluids.byName(b.name);
            NTMFluidProperty property = b.bundleProperty();
            NTMFluidProperties.register(fluid, property);
        }
    }

    public FluidBuilder setTemp(int temperature) {
        this.temperature = temperature;
        return this;
    }

    public FluidBuilder addTraits(FluidTrait... entries) {
        for (FluidTrait t : entries) traits.put(t.getClass(), t);
        return this;
    }

    public FluidBuilder shares(String... conventionalTags) {
        shares.addAll(Arrays.asList(conventionalTags));
        return this;
    }

    public static void visitShares(SharesVisitor visitor) {
        for (FluidBuilder b : PENDING) {
            Fluid fluid = NTMFluids.byName(b.name);
            b.shares.forEach(tag -> visitor.visit(fluid, tag));
        }
    }

    @FunctionalInterface
    public interface SharesVisitor {
        void visit(Fluid fluid, String conventionalTag);
    }

    public void declare() {
        PENDING.add(this);
    }

    public long flammableEnergy() {
        FT_Flammable f = (FT_Flammable) traits.get(FT_Flammable.class);
        return f == null ? 0L : f.getHeatEnergy();
    }

    private NTMFluidProperty bundleProperty() {
        return new NTMFluidProperty(
                color,
                symbol,
                temperature,
                poison,
                flammability,
                reactivity,
                traits.values().toArray(new FluidTrait[0]),
                shares.stream()
                        .map(tag -> TagKey.create(Registries.FLUID, Identifier.parse(tag)))
                        .toList());
    }
}
