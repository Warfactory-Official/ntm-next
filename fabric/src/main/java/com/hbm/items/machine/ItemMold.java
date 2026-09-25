// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.machine;

import com.hbm.inventory.material.MaterialShapes;
import com.hbm.inventory.material.NTMMaterial;
import com.hbm.lib.Library;
import com.hbm.util.I18nUtil;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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

public class ItemMold extends Item {

    public static final int SIZE_SMALL = 0;
    public static final int SIZE_LARGE = 1;

    public static final List<Mold> molds = new ArrayList<>();

    public final Mold mold;

    public ItemMold(Properties properties, Mold mold) {
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
        adder.accept(Component.literal(mold.getTitle()).withStyle(ChatFormatting.YELLOW));
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

        public abstract @Nullable ItemStack getOutput(NTMMaterial mat);

        public abstract int getCost();

        public abstract String getTitle();
    }

    public static class MoldShape extends Mold {

        public final MaterialShapes shape;
        public final int amount;

        public MoldShape(int size, String name, MaterialShapes shape) {
            this(size, name, shape, 1);
        }

        public MoldShape(int size, String name, MaterialShapes shape, int amount) {
            super(size, name);
            this.shape = shape;
            this.amount = amount;
        }

        @Override
        public @Nullable ItemStack getOutput(NTMMaterial mat) {
            var holders = BuiltInRegistries.ITEM.get(mat.tag(shape));
            if (holders.isEmpty() || holders.get().size() == 0) return null;

            Item fallback = null;
            for (var holder : holders.get()) {
                Item item = holder.value();
                Identifier id = BuiltInRegistries.ITEM.getKey(item);
                if (fallback == null) fallback = item;
                if (!"hbm".equals(id.getNamespace())) continue;
                if (id.getPath().contains("fragment")) continue;
                return new ItemStack(item, this.amount);
            }
            return new ItemStack(fallback, this.amount);
        }

        @Override
        public int getCost() {
            return shape.q(amount);
        }

        @Override
        public String getTitle() {
            return I18nUtil.resolveKey("shape." + shape.name()) + " x" + amount;
        }
    }

    public static class MoldBlock extends MoldShape {

        private final HashMap<NTMMaterial, Identifier> blockOverrides = new HashMap<>();

        public MoldBlock(int size, String name, MaterialShapes shape) {
            super(size, name, shape);
        }

        public MoldBlock override(NTMMaterial mat, String vanillaId) {
            blockOverrides.put(mat, Identifier.parse(vanillaId));
            return this;
        }

        @Override
        public @Nullable ItemStack getOutput(NTMMaterial mat) {
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

        private final HashMap<NTMMaterial, Identifier> map = new HashMap<>();
        private final int amount;

        public MoldMulti(int size, String name, int amount, Object... inputs) {
            super(size, name);
            this.amount = amount;
            for (int i = 0; i < inputs.length; i += 2) {
                map.put((NTMMaterial) inputs[i], Library.id((String) inputs[i + 1]));
            }
        }

        @Override
        public @Nullable ItemStack getOutput(NTMMaterial mat) {
            Identifier id = map.get(mat);
            if (id == null) return null;
            return BuiltInRegistries.ITEM.getOptional(id).map(ItemStack::new).orElse(null);
        }

        @Override
        public int getCost() {
            return amount;
        }

        @Override
        public String getTitle() {
            return I18nUtil.resolveKey("shape." + name) + " x1";
        }
    }
}
