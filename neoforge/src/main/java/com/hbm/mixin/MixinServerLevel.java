// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin;

import com.hbm.api.fluidmk2.FlushIndex;
import com.hbm.blockentity.GraphResident;
import com.hbm.capability.CapabilityInvalidation;
import com.hbm.capability.ResolvedCapCache;
import com.hbm.handler.UnstableFuses;
import com.hbm.interfaces.injected.IServerLevelExtension;
import com.hbm.uninos.graph.EndpointRegistry;
import com.hbm.uninos.graph.LevelNodeGraph;
import com.hbm.world.NtmWorldgenFields;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerLevel.class)
public abstract class MixinServerLevel implements IServerLevelExtension {

    @Unique private @Nullable EndpointRegistry hbm$endpoints;
    @Unique private @Nullable List<LevelNodeGraph<?>> hbm$graphs;
    @Unique private NtmWorldgenFields hbm$worldgenFields;
    @Unique private @Nullable ResolvedCapCache hbm$resolvedCaps;

    @Unique private final Set<Entity> hbm$radarTargets = new ObjectLinkedOpenHashSet<>();
    @Unique private final Set<LivingEntity> hbm$livingTargets = new ObjectLinkedOpenHashSet<>();
    @Unique private final Set<Entity> hbm$storageEntities = new ObjectOpenHashSet<>();

    @Inject(method = "<init>", at = @At("TAIL"))
    private void hbm$initializeWorldgenFields(CallbackInfo ci) {
        ServerLevel level = (ServerLevel) (Object) this;
        hbm$worldgenFields = new NtmWorldgenFields(level.getSeed(), level.dimension().identifier());
    }

    @Override
    public EndpointRegistry hbm$endpoints() {
        EndpointRegistry registry = hbm$endpoints;
        if (registry == null) hbm$endpoints = registry = new EndpointRegistry();
        return registry;
    }

    @Override
    public ResolvedCapCache hbm$resolvedCaps() {
        ResolvedCapCache cache = hbm$resolvedCaps;
        if (cache == null) hbm$resolvedCaps = cache = new ResolvedCapCache();
        return cache;
    }

    @Override
    public List<LevelNodeGraph<?>> hbm$graphs() {
        List<LevelNodeGraph<?>> graphs = hbm$graphs;
        if (graphs == null) hbm$graphs = graphs = new ArrayList<>();
        return graphs;
    }

    @Override
    public NtmWorldgenFields hbm$worldgenFields() {
        return hbm$worldgenFields;
    }

    @Unique private @Nullable Long2IntOpenHashMap hbm$capGraphChunks;

    @Override
    public Long2IntOpenHashMap hbm$capGraphChunks() {
        Long2IntOpenHashMap chunks = hbm$capGraphChunks;
        if (chunks == null) hbm$capGraphChunks = chunks = new Long2IntOpenHashMap();
        return chunks;
    }

    @Unique private @Nullable Long2IntOpenHashMap hbm$capShapePending;
    @Unique private @Nullable Long2IntOpenHashMap hbm$capShapeDraining;

    @Inject(method = "invalidateCapabilities(Lnet/minecraft/core/BlockPos;)V", at = @At("TAIL"))
    private void hbm$capabilityPositionInvalidated(BlockPos pos, CallbackInfo ci) {
        if (hbm$graphs == null && hbm$endpoints == null) return;
        long posKey = pos.asLong();
        int neighbours =
                CapabilityInvalidation.invalidatePosition(
                        (ServerLevel) (Object) this,
                        hbm$graphs,
                        hbm$capGraphChunks,
                        hbm$endpoints,
                        posKey);
        if (neighbours != 0) CapabilityInvalidation.queue(hbm$capShapes(), posKey, neighbours);
    }

    @Inject(
            method = "invalidateCapabilities(Lnet/minecraft/world/level/ChunkPos;)V",
            at = @At("TAIL"))
    private void hbm$capabilityChunkInvalidated(ChunkPos pos, CallbackInfo ci) {
        if (hbm$graphs == null && hbm$endpoints == null) return;
        FlushIndex.invalidateChunk((ServerLevel) (Object) this, pos.pack());
        if (!CapabilityInvalidation.hasConsumers(hbm$capGraphChunks, hbm$endpoints)) return;
        CapabilityInvalidation.invalidateChunk(
                hbm$graphs, hbm$capGraphChunks, hbm$endpoints, pos.pack(), hbm$capShapes());
    }

    @Unique
    private Long2IntOpenHashMap hbm$capShapes() {
        Long2IntOpenHashMap pending = hbm$capShapePending;
        if (pending == null) hbm$capShapePending = pending = new Long2IntOpenHashMap();
        return pending;
    }

    @Override
    public void hbm$redrawCapabilityShapes() {
        Long2IntOpenHashMap pending = hbm$capShapePending;
        if (pending == null || pending.isEmpty()) return;
        hbm$capShapePending = hbm$capShapeDraining;
        hbm$capShapeDraining = pending;
        CapabilityInvalidation.redrawShapes((ServerLevel) (Object) this, pending);
    }

    @Override
    public Set<Entity> hbm$radarTargets() {
        return hbm$radarTargets;
    }

    @Override
    public Set<LivingEntity> hbm$livingTargets() {
        return hbm$livingTargets;
    }

    @Override
    public Set<Entity> hbm$storageEntities() {
        return hbm$storageEntities;
    }

    @Inject(method = "onBlockEntityAdded", at = @At("TAIL"))
    private void hbm$graphResidentLoad(BlockEntity blockEntity, CallbackInfo ci) {
        UnstableFuses.onBlockLoad((ServerLevel) (Object) this, blockEntity);
        if (blockEntity instanceof GraphResident)
            GraphResident.onLoad((ServerLevel) (Object) this, blockEntity);
    }
}
