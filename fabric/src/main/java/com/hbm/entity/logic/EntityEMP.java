// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: HbmMods (The Bobcat)
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.entity.logic;

import com.hbm.api.energymk2.IEnergyHandlerMK2;
import com.hbm.explosion.ExplosionNukeGeneric;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.ChunkSource;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class EntityEMP extends Entity {

    List<int[]> machines;
    int life = 10 * 60 * 20;

    private final BlockPos.MutableBlockPos scratch = new BlockPos.MutableBlockPos();

    public EntityEMP(EntityType<? extends EntityEMP> type, Level level) {
        super(type, level);
    }

    @Override
    public void tick() {

        if (!level().isClientSide()) {
            if (machines == null) {
                allocate();
            } else {
                shock();
            }

            if (this.tickCount > life) this.discard();
        }
    }

    private void allocate() {

        machines = new ArrayList<>();
        int radius = 100;

        int originX = (int) getX(), originY = (int) getY(), originZ = (int) getZ();
        ChunkSource chunks = level().getChunkSource();

        boolean[] chunkLoadedAtZ = new boolean[radius * 2 + 1];

        for (int x = -radius; x <= radius; x++) {
            int x2 = (int) Math.pow(x, 2);

            int chunkX = SectionPos.blockToSectionCoord(originX + x);
            int lastChunkZ = Integer.MIN_VALUE;
            boolean loaded = false;
            for (int z = -radius; z <= radius; z++) {
                int chunkZ = SectionPos.blockToSectionCoord(originZ + z);
                if (chunkZ != lastChunkZ) {
                    lastChunkZ = chunkZ;
                    loaded = chunks.getChunkNow(chunkX, chunkZ) != null;
                }
                chunkLoadedAtZ[z + radius] = loaded;
            }

            for (int y = -radius; y <= radius; y++) {
                int y2 = (int) Math.pow(y, 2);

                for (int z = -radius; z <= radius; z++) {
                    if (!chunkLoadedAtZ[z + radius]) continue;
                    int z2 = (int) Math.pow(z, 2);

                    if (Math.sqrt(x2 + y2 + z2) <= radius) {
                        add(this.scratch.set(originX + x, originY + y, originZ + z));
                    }
                }
            }
        }
    }

    private void shock() {

        for (int i = 0; i < machines.size(); i++) {
            emp(machines.get(i)[0], machines.get(i)[1], machines.get(i)[2]);
        }
    }

    private void add(BlockPos pos) {

        if (ExplosionNukeGeneric.hasDrainableEnergy(level(), pos)) {
            machines.add(new int[] {pos.getX(), pos.getY(), pos.getZ()});
        }
    }

    private void emp(int x, int y, int z) {

        BlockPos pos = this.scratch.set(x, y, z);
        BlockEntity te = level().getBlockEntity(pos);

        boolean flag = false;

        if (te instanceof IEnergyHandlerMK2 handler) {

            handler.setPower(0);
            flag = true;
        }

        if (ExplosionNukeGeneric.drainForeignEnergy(level(), pos)) flag = true;

        if (flag && random.nextInt(20) == 0) {

            if (level() instanceof ServerLevel server) {
                server.sendParticles(
                        new BlockParticleOption(
                                ParticleTypes.BLOCK,
                                Blocks.STAINED_GLASS.lightBlue().defaultBlockState()),
                        x + 0.5D,
                        y + 0.5D,
                        z + 0.5D,
                        64,
                        0.25D,
                        0.25D,
                        0.25D,
                        0.0D);
            }
        }
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {

        return false;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {}

    @Override
    protected void readAdditionalSaveData(ValueInput input) {}

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {}
}
