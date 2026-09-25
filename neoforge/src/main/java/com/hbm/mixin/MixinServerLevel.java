// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin;

import com.hbm.capability.ResolvedCapCache;
import com.hbm.interfaces.injected.IServerLevelExtension;
import com.hbm.tileentity.GraphResident;
import com.hbm.uninos.graph.EndpointRegistry;
import com.hbm.uninos.graph.LevelNodeGraph;
import com.hbm.world.NtmWorldgenFields;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
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

    @Inject(method = "<init>", at = @At("TAIL"))
    private void hbm$initializeWorldgenFields(CallbackInfo ci) {
        ServerLevel level = (ServerLevel) (Object) this;
        this.hbm$worldgenFields =
                new NtmWorldgenFields(level.getSeed(), level.dimension().identifier());
    }

    @Override
    public EndpointRegistry hbm$endpoints() {
        EndpointRegistry registry = this.hbm$endpoints;
        if (registry == null) this.hbm$endpoints = registry = new EndpointRegistry();
        return registry;
    }

    @Unique private @Nullable ResolvedCapCache hbm$resolvedCaps;

    @Override
    public ResolvedCapCache hbm$resolvedCaps() {
        ResolvedCapCache cache = this.hbm$resolvedCaps;
        if (cache == null) this.hbm$resolvedCaps = cache = new ResolvedCapCache();
        return cache;
    }

    @Override
    public List<LevelNodeGraph<?>> hbm$graphs() {
        List<LevelNodeGraph<?>> graphs = this.hbm$graphs;
        if (graphs == null) this.hbm$graphs = graphs = new ArrayList<>();
        return graphs;
    }

    @Override
    public NtmWorldgenFields hbm$worldgenFields() {
        return this.hbm$worldgenFields;
    }

    @Inject(method = "onBlockEntityAdded", at = @At("TAIL"))
    private void hbm$graphResidentLoad(BlockEntity blockEntity, CallbackInfo ci) {
        if (blockEntity instanceof GraphResident)
            GraphResident.onLoad((ServerLevel) (Object) this, blockEntity);
    }
}
