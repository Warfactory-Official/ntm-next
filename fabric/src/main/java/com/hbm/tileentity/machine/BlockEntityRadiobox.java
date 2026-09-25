// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine;

import com.hbm.api.energymk2.IEnergyHandlerMK2;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.entity.mob.EntityFBI;
import com.hbm.entity.mob.EntityFBIDrone;
import com.hbm.items.ModItems;
import com.hbm.lib.ModDamageTypes;
import com.hbm.tileentity.BlockEntityMachineBase;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;

public class BlockEntityRadiobox extends BlockEntityMachineBase implements IEnergyHandlerMK2 {

    public static final long MAX_POWER = 500_000L;
    public static final long POWER_PER_TICK = 25_000L;
    public static final int RANGE = 15;
    public static final float DAMAGE = 20.0F;

    private long power;
    private boolean infinite;

    public BlockEntityRadiobox(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RADIOBOX.get(), pos, state, 0);
    }

    @Override
    public void tickServer() {
        if (power < POWER_PER_TICK && !infinite) return;

        if (!infinite) {
            power -= POWER_PER_TICK;
            setChanged();
        }

        ServerLevel server = (ServerLevel) level;
        DamageSource damage = server.damageSources().source(ModDamageTypes.ENERVATION);
        for (LivingEntity entity :
                server.getEntitiesOfClass(
                        LivingEntity.class,
                        effectBounds(worldPosition),
                        entity ->
                                entity instanceof Enemy
                                        && !(entity instanceof EntityFBI)
                                        && !(entity instanceof EntityFBIDrone))) {
            entity.hurtServer(server, damage, DAMAGE);
        }
    }

    public static AABB effectBounds(BlockPos pos) {
        return new AABB(
                pos.getX() - RANGE,
                pos.getY() - RANGE,
                pos.getZ() - RANGE,
                pos.getX() + RANGE,
                pos.getY() + RANGE,
                pos.getZ() + RANGE);
    }

    public boolean isInfinite() {
        return infinite;
    }

    public void installInfinite() {
        infinite = true;
        setChanged();
    }

    @Override
    public long getPower() {
        return power;
    }

    @Override
    public void setPower(long power) {
        this.power = power;
    }

    @Override
    public long getMaxPower() {
        return MAX_POWER;
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (infinite && level != null) {
            level.addFreshEntity(
                    new ItemEntity(
                            level,
                            pos.getX() + 0.5D,
                            pos.getY() + 0.5D,
                            pos.getZ() + 0.5D,
                            new ItemStack(ModItems.BATTERY_SPARK.get())));
        }
        super.preRemoveSideEffects(pos, state);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.getLong("power").ifPresent(value -> power = value);
        infinite = input.getBooleanOr("infinite", false);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("power", power);
        output.putBoolean("infinite", infinite);
    }
}
