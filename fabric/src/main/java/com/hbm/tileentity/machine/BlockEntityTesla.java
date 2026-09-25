// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine;

import com.hbm.api.energymk2.IEnergyHandlerMK2;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.ModBlocks;
import com.hbm.entity.mob.EntityCyberCrab;
import com.hbm.entity.mob.EntityTaintCrab;
import com.hbm.entity.mob.EntityTeslaCrab;
import com.hbm.handler.ArmorUtil;
import com.hbm.lib.ModDamageTypes;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncList;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.sound.ModSounds;
import com.hbm.tileentity.BlockEntityMachineBase;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.feline.Ocelot;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.jspecify.annotations.Nullable;

public class BlockEntityTesla extends BlockEntityMachineBase
        implements IEnergyHandlerMK2, SyncUnitSchema {

    public static final long MAX_POWER = 100_000L;
    public static final long POWER_PER_ZAP = 5_000L;

    public static final double OFFSET = 1.75D;
    private static final double RANGE = 10D;

    @SyncField(units = 1L << 0)
    public final List<Vec3> targets = new SyncList<>();

    public long power;

    public BlockEntityTesla(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TESLA_COIL.get(), pos, state, 0);
    }

    public static boolean isObstructedBetween(
            ServerLevel level, double x, double y, double z, double ex, double ey, double ez) {
        return isObstructed(level, x, y, z, ex, ey, ez);
    }

    private static boolean isObstructed(
            ServerLevel level, double x, double y, double z, double ex, double ey, double ez) {

        ClipContext ctx =
                new ClipContext(
                        new Vec3(x, y, z),
                        new Vec3(ex, ey, ez),
                        ClipContext.Block.OUTLINE,
                        ClipContext.Fluid.NONE,
                        CollisionContext.empty());
        return level.clip(ctx).getType() != HitResult.Type.MISS;
    }

    @Override
    public void tickServer() {
        targets.clear();

        if (onMeteorBattery) power = MAX_POWER;

        if (power >= POWER_PER_ZAP) {
            ServerLevel serverLevel = (ServerLevel) level;
            power -= POWER_PER_ZAP;
            double x = worldPosition.getX() + 0.5D;
            double y = worldPosition.getY() + OFFSET;
            double z = worldPosition.getZ() + 0.5D;
            targets.addAll(zap(serverLevel, x, y, z, RANGE, null));
        }

        networkPackNT(100);
    }

    public static List<Vec3> zap(
            ServerLevel level,
            double x,
            double y,
            double z,
            double range,
            @Nullable Entity source) {
        List<Vec3> beams = new ArrayList<>();
        AABB box = new AABB(x - range, y - range, z - range, x + range, y + range, z + range);
        List<LivingEntity> found = level.getEntitiesOfClass(LivingEntity.class, box);
        DamageSource damage = level.damageSources().source(ModDamageTypes.ELECTRICITY);

        for (LivingEntity e : found) {
            if (e instanceof Ocelot || e == source) continue;

            double eyeY = e.getY() + e.getBbHeight() / 2D;
            double len =
                    Math.sqrt(
                            Math.pow(e.getX() - x, 2)
                                    + Math.pow(eyeY - y, 2)
                                    + Math.pow(e.getZ() - z, 2));
            if (len > range) continue;
            if (isObstructed(level, x, y, z, e.getX(), eyeY, e.getZ())) continue;

            if (e instanceof EntityTaintCrab) {
                beams.add(new Vec3(e.getX(), e.getY() + 1.25D, e.getZ()));
                e.heal(15F);
                continue;
            }

            if (e instanceof EntityTeslaCrab) {
                beams.add(new Vec3(e.getX(), e.getY() + 1D, e.getZ()));
                e.heal(10F);
                continue;
            }

            if (e instanceof EntityCyberCrab) {
                beams.add(new Vec3(e.getX(), eyeY, e.getZ()));
                continue;
            }

            if (e instanceof Creeper creeper) {

                creeper.getEntityData().set(Creeper.DATA_IS_POWERED, true);
                beams.add(new Vec3(e.getX(), eyeY, e.getZ()));
                continue;
            }

            float amount = Mth.clamp(e.getMaxHealth() * 0.5F, 3F, 20F) / found.size();

            if (!(e instanceof Player player && ArmorUtil.checkForFaraday(player))
                    && e.hurtServer(level, damage, amount))
                level.playSound(
                        null,
                        e.getX(),
                        e.getY(),
                        e.getZ(),
                        ModSounds.TESLA_ZAP.get(),
                        SoundSource.BLOCKS,
                        1.0F,
                        1.0F);

            beams.add(new Vec3(e.getX(), eyeY, e.getZ()));
        }

        return beams;
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

    private boolean onMeteorBattery;

    public void refreshMeteorBattery() {
        onMeteorBattery =
                level.getBlockState(worldPosition.below()).is(ModBlocks.METEOR_BATTERY.get());
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        onMeteorBattery = input.getBooleanOr("meteorBattery", false);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putBoolean("meteorBattery", onMeteorBattery);
    }

    private void writeTargets(ByteBuf output) {
        output.writeShort((short) targets.size());
        for (Vec3 target : targets) {
            output.writeDouble(target.x);
            output.writeDouble(target.y);
            output.writeDouble(target.z);
        }
    }

    private void readTargets(ByteBuf input) {
        int count = input.readShort();
        targets.clear();
        for (int i = 0; i < count; i++)
            targets.add(new Vec3(input.readDouble(), input.readDouble(), input.readDouble()));
    }

    @Override
    public long syncUnitMask() {
        return 1L << 0;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> writeTargets(output);
            default -> throw new IllegalArgumentException();
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> readTargets(input);
            default -> throw new IllegalArgumentException();
        }
    }
}
