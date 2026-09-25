// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: HbmMods (The Bobcat)
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.explosion.vanillant;

import com.hbm.explosion.vanillant.interfaces.*;
import com.hbm.explosion.vanillant.standard.*;
import com.hbm.platform.Services;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class ExplosionVNT implements Explosion {

    public Level world;
    public double posX;
    public double posY;
    public double posZ;
    public float size;
    public Entity exploder;

    public HashSet<BlockPos> affectedBlocks;

    public List<BlockPos> allocatedBlocks = List.of();
    private IBlockAllocator blockAllocator;
    private IEntityProcessor entityProcessor;
    private IBlockProcessor blockProcessor;
    private IPlayerProcessor playerProcessor;
    private IExplosionSFX[] sfx;

    public ExplosionVNT(Level world, double x, double y, double z, float size) {
        this(world, x, y, z, size, null);
    }

    public ExplosionVNT(Level world, BlockPos pos, float size) {
        this(world, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, size, null);
    }

    public ExplosionVNT(Level world, double x, double y, double z, float size, Entity exploder) {
        this.world = world;
        this.posX = x;
        this.posY = y;
        this.posZ = z;
        this.size = size;
        this.exploder = exploder;
    }

    public void explode() {

        boolean processBlocks = blockAllocator != null && blockProcessor != null;
        boolean processEntities = entityProcessor != null && playerProcessor != null;

        HashMap<Player, Vec3> affectedPlayers = null;

        if (processBlocks) {
            this.affectedBlocks = blockAllocator.allocate(this, world, posX, posY, posZ, size);
            this.allocatedBlocks = List.copyOf(affectedBlocks);
        }
        if (processEntities)
            affectedPlayers = entityProcessor.process(this, world, posX, posY, posZ, size);

        if (processBlocks) blockProcessor.process(this, world, posX, posY, posZ, affectedBlocks);
        if (processEntities)
            playerProcessor.process(this, world, posX, posY, posZ, affectedPlayers);

        if (sfx != null) {
            for (IExplosionSFX fx : sfx) {
                fx.doEffect(this, world, posX, posY, posZ, size);
            }
        }
    }

    @Override
    public ServerLevel level() {
        return (ServerLevel) world;
    }

    @Override
    public BlockInteraction getBlockInteraction() {
        return blockProcessor == null
                ? BlockInteraction.DESTROY_WITH_DECAY
                : blockProcessor.interaction();
    }

    @Override
    public @Nullable LivingEntity getIndirectSourceEntity() {
        return Explosion.getIndirectSourceEntity(exploder);
    }

    @Override
    public @Nullable Entity getDirectSourceEntity() {
        return exploder;
    }

    @Override
    public float radius() {
        return size;
    }

    @Override
    public Vec3 center() {
        return new Vec3(posX, posY, posZ);
    }

    @Override
    public boolean canTriggerBlocks() {
        return false;
    }

    @Override
    public boolean shouldAffectBlocklikeEntities() {
        return true;
    }

    public float blockResistance(Level world, BlockPos pos, BlockState state) {
        float resistance = Services.PLATFORM.explosionResistance(world, pos, state, this);
        return exploder == null
                ? resistance
                : exploder.getBlockExplosionResistance(
                        this, world, pos, state, state.getFluidState(), resistance);
    }

    public boolean blockExplodes(Level world, BlockPos pos, BlockState state, float power) {
        return exploder == null || exploder.shouldBlockExplode(this, world, pos, state, power);
    }

    public ExplosionVNT setBlockAllocator(IBlockAllocator blockAllocator) {
        this.blockAllocator = blockAllocator;
        return this;
    }

    public ExplosionVNT setEntityProcessor(IEntityProcessor entityProcessor) {
        this.entityProcessor = entityProcessor;
        return this;
    }

    public ExplosionVNT setBlockProcessor(IBlockProcessor blockProcessor) {
        this.blockProcessor = blockProcessor;
        return this;
    }

    public ExplosionVNT setPlayerProcessor(IPlayerProcessor playerProcessor) {
        this.playerProcessor = playerProcessor;
        return this;
    }

    public ExplosionVNT setSFX(IExplosionSFX... sfx) {
        this.sfx = sfx;
        return this;
    }

    @SuppressWarnings("deprecation")
    public ExplosionVNT makeStandard() {
        this.setBlockAllocator(new BlockAllocatorStandard());
        this.setBlockProcessor(new BlockProcessorStandard());
        this.setEntityProcessor(new EntityProcessorStandard());
        this.setPlayerProcessor(new PlayerProcessorStandard());
        this.setSFX(new ExplosionEffectStandard());
        return this;
    }

    @SuppressWarnings("deprecation")
    public ExplosionVNT makeAmat() {
        this.setBlockAllocator(new BlockAllocatorStandard(this.size < 15 ? 16 : 32));
        this.setBlockProcessor(new BlockProcessorStandard().setNoDrop());
        this.setEntityProcessor(
                new EntityProcessorStandard()
                        .withRangeMod(2F)
                        .withDamageMod(new CustomDamageHandlerAmat(50F)));
        this.setPlayerProcessor(new PlayerProcessorStandard());
        this.setSFX(new ExplosionEffectAmat());
        return this;
    }
}
