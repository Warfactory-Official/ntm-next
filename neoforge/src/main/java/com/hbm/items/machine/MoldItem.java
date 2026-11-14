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

package com.hbm.items.machine;

import com.hbm.inventory.material.Material;
import com.hbm.inventory.material.MaterialShape;
import com.hbm.registration.Registrar;
import com.hbm.util.RegistryUtil;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import org.jspecify.annotations.Nullable;

public class MoldItem extends Item {

    public static final int SIZE_SMALL = 0;
    public static final int SIZE_LARGE = 1;

    public static final List<Mold> molds = new ArrayList<>();

    public final Mold mold;

    public MoldItem(Properties properties, Mold mold) {
        super(properties);
        this.mold = mold;
        molds.add(mold);
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            Item.TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> adder,
            TooltipFlag flag) {
        adder.accept(mold.getTitle().copy().withStyle(ChatFormatting.YELLOW));
        if (mold.size == SIZE_SMALL) {
            adder.accept(
                    Component.translatable("block.hbm.foundry_mold")
                            .withStyle(ChatFormatting.GOLD));
        }
        if (mold.size == SIZE_LARGE) {
            adder.accept(
                    Component.translatable("block.hbm.foundry_basin")
                            .withStyle(ChatFormatting.RED));
        }
    }

    public abstract static class Mold {

        public final int size;
        public final String name;

        protected Mold(int size, String name) {
            this.size = size;
            this.name = name;
        }

        public abstract @Nullable ItemStack getOutput(Material mat);

        public abstract int getCost();

        public abstract Component getTitle();
    }

    public static class MoldShape extends Mold {

        public final MaterialShape shape;
        public final int amount;

        public MoldShape(int size, String name, MaterialShape shape) {
            this(size, name, shape, 1);
        }

        public MoldShape(int size, String name, MaterialShape shape, int amount) {
            super(size, name);
            this.shape = shape;
            this.amount = amount;
        }

        @Override
        public @Nullable ItemStack getOutput(Material mat) {
            var holders = BuiltInRegistries.ITEM.get(mat.tag(shape));
            if (holders.isEmpty() || holders.get().size() == 0) return null;

            Item fallback = null;
            for (var holder : holders.get()) {
                Item item = holder.value();
                Identifier id = BuiltInRegistries.ITEM.getKey(item);
                if (fallback == null) fallback = item;
                if (!"hbm".equals(id.getNamespace())) continue;
                if (id.getPath().contains("fragment")) continue;
                return new ItemStack(item, amount);
            }
            return new ItemStack(fallback, amount);
        }

        @Override
        public int getCost() {
            return shape.amount(amount);
        }

        @Override
        public Component getTitle() {
            return Component.translatable("shape." + shape.plural()).append(" x" + amount);
        }
    }

    public static class MoldBlock extends MoldShape {

        private final HashMap<Material, Identifier> blockOverrides = new HashMap<>();

        public MoldBlock(int size, String name, MaterialShape shape) {
            super(size, name, shape);
        }

        public MoldBlock override(Material mat, String vanillaId) {
            blockOverrides.put(mat, Identifier.parse(vanillaId));
            return this;
        }

        @Override
        public @Nullable ItemStack getOutput(Material mat) {
            Identifier override = blockOverrides.get(mat);
            if (override != null) {
                return BuiltInRegistries.ITEM
                        .getOptional(override)
                        .map(ItemStack::new)
                        .orElse(null);
            }
            return super.getOutput(mat);
        }
    }

    public static class MoldMulti extends Mold {

        private static final Map<String, MoldMulti> NAMED = new HashMap<>();

        private final HashMap<Material, Identifier> map = new HashMap<>();
        private final int amount;

        public MoldMulti(int size, String name, int amount, Object... inputs) {
            super(size, name);
            this.amount = amount;
            for (int i = 0; i < inputs.length; i += 2) {
                map.put((Material) inputs[i], RegistryUtil.id((String) inputs[i + 1]));
            }
            NAMED.put(name, this);
        }

        public static MoldMulti named(String name) {
            MoldMulti mold = NAMED.get(name);
            if (mold == null) throw new IllegalArgumentException("no fixed-output mold " + name);
            return mold;
        }

        public void addon(Material mat, Identifier item) {
            Registrar.assertOpen("MoldMulti.addon(" + name + ")");
            if (map.putIfAbsent(mat, item) != null) {
                throw new IllegalStateException(name + " already casts " + mat.name());
            }
        }

        @Override
        public @Nullable ItemStack getOutput(Material mat) {
            Identifier id = map.get(mat);
            if (id == null) return null;
            return BuiltInRegistries.ITEM.getOptional(id).map(ItemStack::new).orElse(null);
        }

        @Override
        public int getCost() {
            return amount;
        }

        @Override
        public Component getTitle() {
            return Component.translatable("shape." + name).append(" x1");
        }
    }
}
