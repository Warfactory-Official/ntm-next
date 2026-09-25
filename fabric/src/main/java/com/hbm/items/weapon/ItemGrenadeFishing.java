// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.weapon;

import com.hbm.entity.item.EntityItemBuoyant;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;

public class ItemGrenadeFishing extends ItemGenericGrenade {

    public ItemGrenadeFishing(Properties properties, int fuse) {
        super(properties, fuse);
    }

    public static ItemStack getRandomLoot(ServerLevel level) {
        LootTable table =
                level.getServer()
                        .reloadableRegistries()
                        .getLootTable(BuiltInLootTables.FISHING_FISH);

        List<ItemStack> loot =
                table.getRandomItems(
                        new LootParams.Builder(level).create(LootContextParamSets.EMPTY));
        return loot.isEmpty() ? ItemStack.EMPTY : loot.getFirst();
    }

    @Override
    public void explode(
            Entity grenade, LivingEntity thrower, Level level, double x, double y, double z) {

        level.explode(null, x, y + 0.25D, z, 3F, Level.ExplosionInteraction.NONE);

        if (!(level instanceof ServerLevel server)) return;

        int iX = Mth.floor(x);
        int iY = Mth.floor(y);
        int iZ = Mth.floor(z);

        for (int i = 0; i < 15; i++) {

            int rX = iX + level.getRandom().nextInt(15) - 7;
            int rY = iY + level.getRandom().nextInt(15) - 7;
            int rZ = iZ + level.getRandom().nextInt(15) - 7;
            BlockPos pos = new BlockPos(rX, rY, rZ);
            BlockState state = level.getBlockState(pos);

            if (state.getBlock() instanceof LiquidBlock
                    && state.getFluidState().is(FluidTags.WATER)) {
                ItemStack loot = getRandomLoot(server);
                if (!loot.isEmpty()) {
                    EntityItemBuoyant item =
                            new EntityItemBuoyant(level, rX + 0.5, rY + 0.5, rZ + 0.5, loot.copy());
                    item.setDeltaMovement(item.getDeltaMovement().x, 1, item.getDeltaMovement().z);
                    level.addFreshEntity(item);
                }
            }
        }
    }

    @Override
    public int getMaxTimer() {
        return 60;
    }

    @Override
    public double getBounceMod() {
        return 0.5D;
    }
}
