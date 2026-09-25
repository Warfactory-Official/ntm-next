// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.entity.missile;

import com.hbm.entity.ModEntities;
import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.explosion.vanillant.standard.BlockAllocatorStandard;
import com.hbm.explosion.vanillant.standard.BlockProcessorStandard;
import com.hbm.explosion.vanillant.standard.EntityProcessorCross;
import com.hbm.explosion.vanillant.standard.PlayerProcessorStandard;
import com.hbm.inventory.material.Mats;
import com.hbm.items.ModItems;
import com.hbm.particle.helper.ExplosionCreator;
import com.hbm.sound.ModSounds;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;

public class EntityMissileShuttle extends EntityMissileBaseNT {

    public EntityMissileShuttle(EntityType<? extends EntityMissileShuttle> type, Level level) {
        super(type, level);
    }

    public EntityMissileShuttle(Level level) {
        super(ModEntities.MISSILE_SHUTTLE.get(), level);
    }

    @Override
    public void onMissileImpact(BlockHitResult mop) {

        new ExplosionVNT(level(), getX() + 0.5F, getY() + 0.5F, getZ() + 0.5F, 20.0F)
                .setBlockAllocator(new BlockAllocatorStandard(64))
                .setBlockProcessor(new BlockProcessorStandard())
                .setEntityProcessor(new EntityProcessorCross(7.5D).withRangeMod(2F))
                .setPlayerProcessor(new PlayerProcessorStandard())
                .explode();

        ExplosionCreator.composeEffectRBMKMush(
                level(), getX() + 0.5D, getY() + 1D, getZ() + 0.5D, 10F);

        level().playSound(
                        null,
                        getX(),
                        getY(),
                        getZ(),
                        ModSounds.ROBIN_EXPLOSION.get(),
                        SoundSource.BLOCKS,
                        4.0F,
                        (1.0F + (random.nextFloat() - random.nextFloat()) * 0.2F) * 0.7F);
    }

    @Override
    public List<ItemStack> getDebris() {
        List<ItemStack> list = new ArrayList<>();

        list.add(new ItemStack(ModItems.plate(Mats.MAT_STEEL), 8));
        list.add(new ItemStack(ModItems.THRUSTER_MEDIUM.get(), 2));
        list.add(new ItemStack(ModItems.CANISTER.get()));
        list.add(new ItemStack(Blocks.GLASS_PANE, 2));

        return list;
    }

    @Override
    public ItemStack getDebrisRareDrop() {
        return new ItemStack(ModItems.MISSILE_GENERIC.get());
    }

    @Override
    public String getRadarName() {
        return "radar.target.shuttle";
    }

    @Override
    public ItemStack getMissileItemForInfo() {
        return new ItemStack(ModItems.MISSILE_SHUTTLE.get());
    }
}
