// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.entity.cart;

import com.hbm.blocks.ModBlocks;
import com.hbm.entity.ModEntities;
import com.hbm.inventory.container.MenuCrate;
import com.hbm.items.tool.ItemModMinecart.EnumCartBase;
import com.hbm.items.tool.ItemModMinecart.EnumMinecart;
import com.hbm.items.tool.ItemModMinecart;
import com.hbm.platform.Services;
import com.hbm.tileentity.machine.storage.CrateType;
import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gamerules.GameRules;

public class EntityMinecartCrate extends EntityMinecartContainerBase {

    private static final CrateType CRATE = CrateType.STEEL;

    public EntityMinecartCrate(EntityType<? extends EntityMinecartCrate> type, Level level) {
        super(type, level);
    }

    public EntityMinecartCrate(
            Level level, double x, double y, double z, EnumCartBase base, ItemStack stack) {
        super(ModEntities.CART_CRATE.get(), level, x, y, z, base);

        if (stack != null && !stack.isEmpty()) {
            stack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY)
                    .copyInto(getItemStacks());
        }
    }

    @Override
    public void destroy(ServerLevel level, DamageSource source) {
        List<ItemStack> held = List.copyOf(getItemStacks());
        ItemContainerContents contents = ItemContainerContents.fromItems(held);
        clearContent();
        kill(level);

        if (!level.getGameRules().get(GameRules.ENTITY_DROPS)) return;

        ItemStack stack = getCartItem();
        if (hasCustomName()) stack.set(DataComponents.CUSTOM_NAME, getCustomName());

        if (contents != ItemContainerContents.EMPTY) stack.set(DataComponents.CONTAINER, contents);

        spawnAtLocation(level, stack);
    }

    @Override
    public int getContainerSize() {
        return CRATE.slots;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return Services.PLATFORM.canFitInsideContainerItems(stack);
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return new MenuCrate(containerId, inventory, CRATE, this);
    }

    @Override
    public BlockState getDefaultDisplayBlockState() {
        return ModBlocks.CRATE_STEEL.get().defaultBlockState();
    }

    @Override
    public int getDefaultDisplayOffset() {
        return 6;
    }

    @Override
    public ItemStack getCartItem() {
        return ItemModMinecart.createCartItem(getBase(), EnumMinecart.CRATE);
    }
}
