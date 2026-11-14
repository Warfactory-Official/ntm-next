/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file under the Apache License except in
 * compliance with that License. You may obtain a copy at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Apache License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License for the specific language governing permissions
 * and limitations under the License.
 */

package com.hbm.registration;

import com.hbm.handler.MissileStruct;
import com.hbm.inventory.fluid.FluidStackNTM;
import com.hbm.items.ModDataComponents;
import com.hbm.items.machine.BatteryItem;
import com.hbm.items.machine.FluidIdentifierData;
import com.hbm.items.machine.ICFPelletItem;
import com.hbm.items.machine.ScrapsItem;
import com.hbm.items.special.BedrockOreBaseItem;
import com.hbm.items.weapon.CustomMissileItem;
import com.hbm.items.weapon.grenade.GrenadeUniversalItem;
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
            return ((BatteryItem) stack.getItem()).getCharge(stack) > 0L;
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
            ScrapsItem.ScrapData data = stack.get(ModDataComponents.SCRAP.get());
            return data != null ? data.material() : null;
        }
    },
    GRENADE {
        @Override
        public Object getSubtypeData(ItemStack stack) {
            return GrenadeUniversalItem.getData(stack);
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
                    ICFPelletItem.getType(stack, true),
                    ICFPelletItem.getType(stack, false),
                    ICFPelletItem.isMuonCatalyzed(stack));
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
            MissileStruct parts = CustomMissileItem.getStruct(stack);
            return parts == null
                    ? null
                    : new MissileSubtype(parts, stack.get(ModDataComponents.MISSILE_CHIP.get()));
        }
    },
    BEDROCK_ORE_SET {
        @Override
        public Object getSubtypeData(ItemStack stack) {
            return BedrockOreBaseItem.sample(stack).set().id();
        }
    };

    public abstract Object getSubtypeData(ItemStack stack);

    private record ICFPelletSubtype(
            ICFPelletItem.EnumICFFuel first, ICFPelletItem.EnumICFFuel second, boolean muon) {}

    private record FluidSubtype(Identifier fluid, int pressure) {}

    private record FluidIdentifierSubtype(Identifier primary, Identifier secondary) {}

    private record MissileSubtype(MissileStruct parts, @Nullable Item chip) {}
}
