// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.entity.cart;

import com.hbm.blocks.ModBlocks;
import com.hbm.entity.ModEntities;
import com.hbm.inventory.container.MenuCartDestroyer;
import com.hbm.items.tool.ItemModMinecart.EnumCartBase;
import com.hbm.items.tool.ItemModMinecart.EnumMinecart;
import com.hbm.items.tool.ItemModMinecart;
import java.util.List;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public class EntityMinecartDestroyer extends EntityMinecartContainerBase {

    public static final int FILTER_SLOTS = 18;
    private static final int EXACT_SLOTS = 9;

    public EntityMinecartDestroyer(
            EntityType<? extends EntityMinecartDestroyer> type, Level level) {
        super(type, level);
    }

    public EntityMinecartDestroyer(Level level, double x, double y, double z, EnumCartBase base) {
        super(ModEntities.CART_DESTROYER.get(), level, x, y, z, base);
    }

    @Override
    public int getContainerSize() {
        return FILTER_SLOTS;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return false;
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return new MenuCartDestroyer(containerId, inventory, this);
    }

    @Override
    public BlockState getDefaultDisplayBlockState() {
        return ModBlocks.MACHINE_SHREDDER.get().defaultBlockState();
    }

    @Override
    public int getDefaultDisplayOffset() {
        return 6;
    }

    @Override
    public void tick() {
        super.tick();

        if (level().isClientSide()) {
            if (tickCount % 5 == 0) {
                level().addParticle(
                                ParticleTypes.SMOKE,
                                getX(),
                                getY() + 0.75D,
                                getZ(),
                                0.0D,
                                0.01D,
                                0.0D);
            }
            return;
        }

        if (tickCount % 5 != 0) return;

        List<ItemEntity> items =
                level().getEntitiesOfClass(
                                ItemEntity.class,
                                new AABB(
                                        getX() - 2.5D,
                                        getY() - 1.5D,
                                        getZ() - 2.5D,
                                        getX() + 2.5D,
                                        getY() + 2D,
                                        getZ() + 2.5D));

        boolean shredded = false;

        for (ItemEntity item : items) {
            if (matchesFilter(item.getItem())) {
                item.discard();
                shredded = true;
            }
        }

        if (shredded) {
            level().playSound(
                            null,
                            getX(),
                            getY(),
                            getZ(),
                            SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR,
                            SoundSource.NEUTRAL,
                            0.5F,
                            0.5F + random.nextFloat() * 0.2F);
        }
    }

    private boolean matchesFilter(ItemStack stack) {
        for (int i = 0; i < EXACT_SLOTS; i++) {
            ItemStack filter = getItem(i);

            if (!filter.isEmpty() && ItemStack.isSameItemSameComponents(filter, stack)) return true;
        }
        for (int i = EXACT_SLOTS; i < FILTER_SLOTS; i++) {
            ItemStack filter = getItem(i);
            if (!filter.isEmpty() && filter.is(stack.getItem())) return true;
        }
        return false;
    }

    @Override
    public ItemStack getCartItem() {
        return ItemModMinecart.createCartItem(getBase(), EnumMinecart.DESTROYER);
    }
}
