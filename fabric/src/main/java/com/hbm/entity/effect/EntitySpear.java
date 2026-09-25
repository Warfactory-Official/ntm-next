// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.entity.effect;

import com.hbm.advancement.DetonationTrigger;
import com.hbm.advancement.HbmCriteria;
import com.hbm.client.ClientEffects;
import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.explosion.vanillant.standard.BlockAllocatorStandard;
import com.hbm.explosion.vanillant.standard.BlockMutatorDigamma;
import com.hbm.explosion.vanillant.standard.BlockProcessorStandard;
import com.hbm.lib.ModDamageTypes;
import com.hbm.sound.ModSounds;
import com.hbm.util.ContaminationUtil.ContaminationType;
import com.hbm.util.ContaminationUtil.HazardType;
import com.hbm.util.ContaminationUtil;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

public class EntitySpear extends Entity {

    public int ticksInGround;
    private boolean landedFlash;

    public EntitySpear(EntityType<? extends EntitySpear> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {}

    @Override
    public boolean shouldRenderAtSqrDistance(double distSq) {
        return distSq < 25000;
    }

    @Override
    public void tick() {
        this.xOld = getX();
        this.yOld = getY();
        this.zOld = getZ();

        int x = Mth.floor(getX());
        int y = Mth.floor(getY());
        int z = Mth.floor(getZ());

        if (level().getBlockState(new BlockPos(x, y - 1, z)).isAir()) {
            setPos(getX(), getY() - 0.2, getZ());

            if (level() instanceof ServerLevel server) {
                double ix = getX() + random.nextGaussian() * 25;
                double iz = getZ() + random.nextGaussian() * 25;
                double iy =
                        server.getHeight(
                                        Heightmap.Types.MOTION_BLOCKING,
                                        Mth.floor(ix),
                                        Mth.floor(iz))
                                + 2;

                boolean circuit = new Vec3(ix - getX(), 0, iz - getZ()).length() < 20;
                new ExplosionVNT(server, ix, iy, iz, 7.5F, this)
                        .setBlockAllocator(new BlockAllocatorStandard())
                        .setBlockProcessor(
                                new BlockProcessorStandard()
                                        .setNoDrop()
                                        .withBlockEffect(new BlockMutatorDigamma(circuit)))
                        .explode();
                for (ServerPlayer player : server.players()) {
                    ContaminationUtil.contaminate(
                            player, HazardType.DIGAMMA, ContaminationType.DIGAMMA, 0.05D);
                    HbmCriteria.detonation(player, DetonationTrigger.Kind.DIGAMMA_SPEAR);
                }
            } else {
                double dy = level().getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) + 2;
                ClientEffects.radialDigamma(level(), getX(), dy, getZ(), 5);
            }

            if (level().getBlockState(new BlockPos(x, y - 3, z)).isAir()) ticksInGround = 0;
        } else {
            ticksInGround++;

            if (level() instanceof ServerLevel server && ticksInGround > 100) {

                List<Entity> entities = new ArrayList<>();
                server.getAllEntities().forEach(entities::add);
                for (Entity e : entities) {
                    if (e instanceof LivingEntity living) {
                        ContaminationUtil.contaminate(
                                living, HazardType.DIGAMMA, ContaminationType.DIGAMMA2, 10D);

                        living.hurtServer(
                                server,
                                server.damageSources().source(ModDamageTypes.DIGAMMA),
                                living.getMaxHealth());
                    }
                }
                server.playSound(
                        null,
                        getX(),
                        getY(),
                        getZ(),
                        ModSounds.DFLASH.get(),
                        SoundSource.HOSTILE,
                        25000.0F,
                        1.0F);
                discard();
            } else if (level().isClientSide() && ticksInGround > 100 && !landedFlash) {

                landedFlash = true;
                ClientEffects.radialDigamma(level(), getX(), getY() + 7, getZ(), 100);
            }
        }
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {}

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {}
}
