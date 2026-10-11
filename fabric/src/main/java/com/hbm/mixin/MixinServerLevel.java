// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin;

import com.hbm.api.environment.LevelWeather;
import com.hbm.api.fluidmk2.FlushIndex;
import com.hbm.blockentity.ChunkPublication;
import com.hbm.capability.ResolvedCapCache;
import com.hbm.interfaces.injected.IServerLevelExtension;
import com.hbm.uninos.graph.EndpointRegistry;
import com.hbm.uninos.graph.LevelNodeGraph;
import com.hbm.uninos.graph.PhysicalGraph;
import com.hbm.world.NtmWorldgenFields;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import java.util.Set;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerLevel.class)
public abstract class MixinServerLevel implements IServerLevelExtension {

    @Unique private @Nullable EndpointRegistry hbm$endpoints;
    @Unique private LevelNodeGraph<?>[] hbm$graphs = LevelNodeGraph.emptySlots();
    @Unique private NtmWorldgenFields hbm$worldgenFields;
    @Unique private @Nullable ResolvedCapCache hbm$resolvedCaps;

    @Unique private final Set<Entity> hbm$radarTargets = new ObjectLinkedOpenHashSet<>();
    @Unique private final Set<LivingEntity> hbm$livingTargets = new ObjectLinkedOpenHashSet<>();
    @Unique private final Set<Entity> hbm$storageEntities = new ObjectOpenHashSet<>();
    @Unique private @Nullable LongOpenHashSet hbm$publishedChunks;
    @Unique private PhysicalGraph.@Nullable Batch hbm$physicalBatch;
    @Unique private @Nullable FlushIndex hbm$flushIndex;

    @Override
    public LongOpenHashSet hbm$publishedChunks() {
        LongOpenHashSet published = hbm$publishedChunks;
        if (published == null) hbm$publishedChunks = published = new LongOpenHashSet();
        return published;
    }

    @Override
    public PhysicalGraph.@Nullable Batch hbm$physicalBatch() {
        return hbm$physicalBatch;
    }

    @Override
    public void hbm$physicalBatch(PhysicalGraph.@Nullable Batch batch) {
        hbm$physicalBatch = batch;
    }

    @Override
    public @Nullable FlushIndex hbm$flushIndex() {
        return hbm$flushIndex;
    }

    @Override
    public void hbm$flushIndex(FlushIndex index) {
        hbm$flushIndex = index;
    }

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
    public LevelNodeGraph<?>[] hbm$graphs() {
        return hbm$graphs;
    }

    @Override
    public void hbm$graphs(LevelNodeGraph<?>[] graphs) {
        hbm$graphs = graphs;
    }

    @Override
    public NtmWorldgenFields hbm$worldgenFields() {
        return hbm$worldgenFields;
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

    @Inject(method = "advanceWeatherCycle", at = @At("HEAD"), cancellable = true)
    private void hbm$addonWeather(CallbackInfo ci) {
        if (LevelWeather.advance((ServerLevel) (Object) this)) ci.cancel();
    }

    @Inject(method = "wakeUpAllPlayers", at = @At("HEAD"))
    private void hbm$sleptThrough(CallbackInfo ci) {
        LevelWeather.sleptThrough((ServerLevel) (Object) this);
    }

    @Inject(method = "onBlockEntityAdded", at = @At("TAIL"))
    private void hbm$graphResidentLoad(BlockEntity blockEntity, CallbackInfo ci) {
        ChunkPublication.added((ServerLevel) (Object) this, blockEntity);
    }

    @Inject(method = "startTickingChunk", at = @At("HEAD"))
    private void hbm$publishBeforeTicking(LevelChunk chunk, CallbackInfo ci) {
        ChunkPublication.promote((ServerLevel) (Object) this, chunk);
    }
}
