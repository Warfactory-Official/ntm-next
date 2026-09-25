// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.registration;

import com.hbm.handler.MissileStruct;
import com.hbm.inventory.fluid.FluidStackNTM;
import com.hbm.items.ModDataComponents;
import com.hbm.items.machine.FluidIdentifierData;
import com.hbm.items.machine.ItemBattery;
import com.hbm.items.machine.ItemICFPellet;
import com.hbm.items.machine.ItemScraps;
import com.hbm.items.weapon.ItemCustomMissile;
import com.hbm.items.weapon.grenade.ItemGrenadeUniversal;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluids;
import org.jspecify.annotations.Nullable;

public enum ItemSubtype {
    STATE {
        @Override
        public Object getSubtypeData(ItemStack stack) {
            return ItemStates.isPrototype(stack) ? null : ItemStates.key(stack);
        }
    },
    BATTERY_CHARGE {
        @Override
        public Object getSubtypeData(ItemStack stack) {
            return stack.getOrDefault(
                            ModDataComponents.BATTERY_CHARGE.get(),
                            ((ItemBattery) stack.getItem()).maxCharge)
                    > 0L;
        }
    },
    FLUID_CONTENT {
        @Override
        public Object getSubtypeData(ItemStack stack) {
            FluidStackNTM content = stack.get(ModDataComponents.FLUID_CONTENT.get());
            if (content == null || content.type() == Fluids.EMPTY) return null;
            return new FluidSubtype(
                    BuiltInRegistries.FLUID.getKey(content.type()), content.pressure());
        }
    },
    SCRAP_MATERIAL {
        @Override
        public Object getSubtypeData(ItemStack stack) {
            ItemScraps.ScrapData data = stack.get(ModDataComponents.SCRAP.get());
            return data != null ? data.material() : null;
        }
    },
    GRENADE {
        @Override
        public Object getSubtypeData(ItemStack stack) {
            return ItemGrenadeUniversal.getData(stack);
        }
    },
    FLUID_IDENTIFIER {
        @Override
        public Object getSubtypeData(ItemStack stack) {
            FluidIdentifierData data =
                    stack.getOrDefault(
                            ModDataComponents.FLUID_IDENTIFIER.get(), FluidIdentifierData.EMPTY);
            if (data.equals(FluidIdentifierData.EMPTY)) return null;
            return new FluidIdentifierSubtype(
                    BuiltInRegistries.FLUID.getKey(data.primary()),
                    BuiltInRegistries.FLUID.getKey(data.secondary()));
        }
    },
    BLUEPRINT_POOL {
        @Override
        public Object getSubtypeData(ItemStack stack) {
            return stack.get(ModDataComponents.BLUEPRINT_POOL.get());
        }
    },
    ICF_FUEL {
        @Override
        public Object getSubtypeData(ItemStack stack) {
            return new ICFPelletSubtype(
                    ItemICFPellet.getType(stack, true),
                    ItemICFPellet.getType(stack, false),
                    ItemICFPellet.isMuonCatalyzed(stack));
        }
    },
    CUSTOM_MACHINE {
        @Override
        public Object getSubtypeData(ItemStack stack) {
            return stack.get(ModDataComponents.CUSTOM_MACHINE_TYPE.get());
        }
    },
    MISSILE {
        @Override
        public Object getSubtypeData(ItemStack stack) {
            MissileStruct parts = ItemCustomMissile.getStruct(stack);
            return parts == null
                    ? null
                    : new MissileSubtype(parts, stack.get(ModDataComponents.MISSILE_CHIP.get()));
        }
    };

    public abstract Object getSubtypeData(ItemStack stack);

    private record ICFPelletSubtype(
            ItemICFPellet.EnumICFFuel first, ItemICFPellet.EnumICFFuel second, boolean muon) {}

    private record FluidSubtype(Identifier fluid, int pressure) {}

    private record FluidIdentifierSubtype(Identifier primary, Identifier secondary) {}

    private record MissileSubtype(MissileStruct parts, @Nullable Item chip) {}
}
