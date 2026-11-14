/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file under the Apache License except in
 * compliance with that License. You may obtain a copy at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Apache License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License for the specific language governing permissions
 * and limitations under the License.
 */

package com.hbm.mixin;

import com.hbm.api.environment.LevelWeather;
import com.hbm.api.fluidmk2.FlushIndex;
import com.hbm.blockentity.ChunkPublication;
import com.hbm.capability.ResolvedCapCache;
import com.hbm.explosion.FalloutJournal;
import com.hbm.handler.neutron.NeutronRay;
import com.hbm.handler.pollution.PollutionSavedData;
import com.hbm.handler.radio.RadioSystem;
import com.hbm.interfaces.injected.ServerLevelExtension;
import com.hbm.uninos.graph.EndpointRegistry;
import com.hbm.uninos.graph.LevelNodeGraph;
import com.hbm.uninos.graph.PhysicalGraph;
import com.hbm.world.NtmWorldgenFields;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import java.util.ArrayList;
import java.util.List;
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
public abstract class MixinServerLevel implements ServerLevelExtension {

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
    @Unique private @Nullable PollutionSavedData hbm$pollution;
    @Unique private final List<NeutronRay> hbm$neutronRays = new ArrayList<>();
    @Unique private final RadioSystem.Table hbm$radio = new RadioSystem.Table();

    @Override
    public @Nullable PollutionSavedData hbm$pollution() {
        return hbm$pollution;
    }

    @Override
    public void hbm$pollution(PollutionSavedData field) {
        hbm$pollution = field;
    }

    @Override
    public List<NeutronRay> hbm$neutronRays() {
        return hbm$neutronRays;
    }

    @Override
    public RadioSystem.Table hbm$radio() {
        return hbm$radio;
    }

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

    @Inject(method = "tickChunk", at = @At("HEAD"))
    private void hbm$falloutCores(LevelChunk chunk, int tickSpeed, CallbackInfo ci) {
        FalloutJournal.tickCores((ServerLevel) (Object) this, chunk);
    }

    @Inject(method = "startTickingChunk", at = @At("HEAD"))
    private void hbm$publishBeforeTicking(LevelChunk chunk, CallbackInfo ci) {
        ChunkPublication.promote((ServerLevel) (Object) this, chunk);
    }
}
