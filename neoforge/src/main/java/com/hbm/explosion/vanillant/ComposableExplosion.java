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

package com.hbm.explosion.vanillant;

import com.hbm.explosion.vanillant.interfaces.ExplosionBlockProcessor;
import com.hbm.explosion.vanillant.standard.AttenuatedRayBlockSelector;
import com.hbm.explosion.vanillant.standard.CellDestruction;
import com.hbm.explosion.vanillant.standard.ExplosionEffectAmat;
import com.hbm.explosion.vanillant.standard.ExplosionEffectStandard;
import com.hbm.explosion.vanillant.standard.ExplosionEntityProcessor;
import com.hbm.explosion.vanillant.standard.RadiationExplosionHitListener;
import com.hbm.explosion.vanillant.standard.SinglePointExplosionEntityProcessor;
import com.hbm.packet.PacketWire;
import com.hbm.packet.layout.ExplosionKnockbackLayout;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerExplosion;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class ComposableExplosion extends ServerExplosion {

    private static final BlastEffect[] NO_EFFECTS = {};

    private final double x;
    private final double y;
    private final double z;
    private @Nullable ExplosionBlockSelector selector;
    private @Nullable ExplosionBlockProcessor handler;
    private @Nullable ExplosionEntityProcessor sweep;
    private BlastEffect[] effects = NO_EFFECTS;
    protected @Nullable Set<BlockPos> cells;
    protected List<BlockPos> snapshot = List.of();

    public ComposableExplosion(
            Level level, double x, double y, double z, float size, @Nullable Entity source) {
        super(
                (ServerLevel) level,
                source,
                null,
                null,
                new Vec3(x, y, z),
                size,
                false,
                BlockInteraction.DESTROY_WITH_DECAY);
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public ComposableExplosion(Level level, double x, double y, double z, float size) {
        this(level, x, y, z, size, null);
    }

    public ComposableExplosion(Level level, BlockPos pos, float size) {
        this(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, size, null);
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    public @Nullable Set<BlockPos> getAffectedBlocks() {
        return cells;
    }

    public List<BlockPos> getBlockSnapshot() {
        return snapshot;
    }

    public ComposableExplosion blockSelector(@Nullable ExplosionBlockSelector selector) {
        this.selector = selector;
        return this;
    }

    public ComposableExplosion blockProcessor(@Nullable ExplosionBlockProcessor handler) {
        this.handler = handler;
        return this;
    }

    public ComposableExplosion entityProcessor(@Nullable ExplosionEntityProcessor sweep) {
        this.sweep = sweep;
        return this;
    }

    public ComposableExplosion effects(BlastEffect @Nullable ... effects) {
        this.effects = effects == null ? NO_EFFECTS : effects;
        return this;
    }

    public ComposableExplosion standard() {
        blockSelector(new AttenuatedRayBlockSelector(16));
        blockProcessor(CellDestruction.decay());
        entityProcessor(new SinglePointExplosionEntityProcessor());
        return effects(new ExplosionEffectStandard());
    }

    public ComposableExplosion antimatter() {
        blockSelector(new AttenuatedRayBlockSelector(radius() < 15 ? 16 : 32));
        blockProcessor(CellDestruction.noDrops());
        entityProcessor(
                new SinglePointExplosionEntityProcessor()
                        .range(2F)
                        .hook(new RadiationExplosionHitListener(50F)));
        return effects(new ExplosionEffectAmat());
    }

    protected final void adopt(Set<BlockPos> selected) {
        cells = selected;
        snapshot = List.copyOf(selected);
    }

    public static boolean resists(BlockState state) {
        return !state.isAir() || !state.getFluidState().isEmpty();
    }

    public float resistance(BlockGetter level, BlockPos pos, BlockState state) {
        var fluid = state.getFluidState();
        float base =
                Math.max(
                        state.getExplosionResistance(level, pos, (Explosion) this),
                        fluid.getExplosionResistance(level, pos, (Explosion) this));
        var source = getDirectSourceEntity();
        return source == null
                ? base
                : source.getBlockExplosionResistance(this, level, pos, state, fluid, base);
    }

    public float blockResistance(BlockGetter level, BlockPos pos, BlockState state) {
        float base = state.getExplosionResistance(level, pos, (Explosion) this);
        var source = getDirectSourceEntity();
        return source == null
                ? base
                : source.getBlockExplosionResistance(
                        this, level, pos, state, state.getFluidState(), base);
    }

    public boolean shouldExplodeBlock(
            BlockGetter level, BlockPos pos, BlockState state, float power) {
        var source = getDirectSourceEntity();
        return source == null || source.shouldBlockExplode(this, level, pos, state, power);
    }

    @Override
    public BlockInteraction getBlockInteraction() {
        return handler == null ? BlockInteraction.DESTROY_WITH_DECAY : handler.interaction();
    }

    @Override
    public boolean shouldAffectBlocklikeEntities() {
        return true;
    }

    @Override
    public int explode() {
        var level = level();
        level.gameEvent(getDirectSourceEntity(), GameEvent.EXPLODE, center());
        float size = radius();
        boolean blocks = selector != null && handler != null;
        if (blocks) adopt(selector.select(this, level, x, y, z, size));
        if (sweep != null) sweep.sweep(this);
        if (blocks) handler.process(this, level, x, y, z, cells);
        if (sweep != null) {
            for (var hit : getHitPlayers().entrySet()) {
                if (!(hit.getKey() instanceof ServerPlayer player)) continue;
                var impulse = hit.getValue();
                PacketWire.sendTo(
                        ExplosionKnockbackLayout.encode(
                                (float) impulse.x, (float) impulse.y, (float) impulse.z),
                        player);
            }
        }
        for (var effect : effects) effect.emit(this, level, x, y, z, size);
        return snapshot.size();
    }
}
