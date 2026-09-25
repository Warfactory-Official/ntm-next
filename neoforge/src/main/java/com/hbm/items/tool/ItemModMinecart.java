// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: HbmMods (The Bobcat)
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.tool;

import com.hbm.entity.cart.EntityMinecartCrate;
import com.hbm.entity.cart.EntityMinecartDestroyer;
import com.hbm.entity.cart.EntityMinecartNTM;
import com.hbm.entity.cart.EntityMinecartOre;
import com.hbm.entity.cart.EntityMinecartPowder;
import com.hbm.entity.cart.EntityMinecartSemtex;
import com.hbm.items.ModDataComponents;
import com.hbm.items.ModItems;
import com.mojang.serialization.Codec;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

public class ItemModMinecart extends Item {

    public final EnumMinecart type;

    public ItemModMinecart(Properties props, EnumMinecart type) {
        super(props);
        this.type = type;
    }

    public static ItemStack createCartItem(EnumCartBase base, EnumMinecart cart) {
        ItemStack stack = ModItems.CART.stack(cart);
        stack.set(ModDataComponents.CART_BASE.get(), base);
        return stack;
    }

    public static EnumMinecart getCartType(ItemStack stack) {
        return ((ItemModMinecart) stack.getItem()).type;
    }

    public static EnumCartBase getBaseType(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.CART_BASE.get(), EnumCartBase.VANILLA);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();

        if (!level.getBlockState(pos).is(BlockTags.RAILS)) return InteractionResult.PASS;

        if (!level.isClientSide()) {
            ItemStack stack = context.getItemInHand();
            Entity cart =
                    createMinecart(
                            level, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, stack);

            if (stack.has(DataComponents.CUSTOM_NAME)) {
                cart.setCustomName(stack.get(DataComponents.CUSTOM_NAME));
            }

            level.addFreshEntity(cart);
            stack.shrink(1);
        }

        return InteractionResult.SUCCESS;
    }

    public static Entity createMinecart(
            Level level, double x, double y, double z, ItemStack stack) {
        EnumCartBase base = getBaseType(stack);
        return switch (getCartType(stack)) {
            case CRATE -> new EntityMinecartCrate(level, x, y, z, base, stack);
            case DESTROYER -> new EntityMinecartDestroyer(level, x, y, z, base);
            case POWDER -> new EntityMinecartPowder(level, x, y, z, base);
            case SEMTEX -> new EntityMinecartSemtex(level, x, y, z, base);
            case EMPTY -> new EntityMinecartOre(level, x, y, z, base);
        };
    }

    public enum EnumCartBase implements StringRepresentable {
        VANILLA,
        WOOD,
        STEEL,
        PAINTED;

        public static final EnumCartBase[] VALUES = values();
        public static final Codec<EnumCartBase> CODEC =
                StringRepresentable.fromEnum(EnumCartBase::values);
        public final String id = name().toLowerCase(Locale.ROOT);

        @Override
        public String getSerializedName() {
            return id;
        }
    }

    public enum EnumMinecart {
        EMPTY(EnumCartBase.WOOD, EnumCartBase.STEEL, EnumCartBase.PAINTED),
        CRATE(EnumCartBase.VANILLA),
        DESTROYER(EnumCartBase.STEEL, EnumCartBase.PAINTED),
        POWDER(EnumCartBase.WOOD, EnumCartBase.STEEL, EnumCartBase.PAINTED),
        SEMTEX(EnumCartBase.WOOD, EnumCartBase.STEEL, EnumCartBase.PAINTED);

        public static final EnumMinecart[] VALUES = values();

        public final int types;
        public final String id = name().toLowerCase(Locale.ROOT);

        EnumMinecart(EnumCartBase... accepted) {
            int mask = 0;
            for (EnumCartBase type : accepted) mask |= 1 << type.ordinal();
            this.types = mask;
        }

        public boolean supportsBase(EnumCartBase type) {
            return (this.types & (1 << type.ordinal())) > 0;
        }
    }

    @Override
    public boolean canFitInsideContainerItems() {
        return type != EnumMinecart.CRATE;
    }
}
