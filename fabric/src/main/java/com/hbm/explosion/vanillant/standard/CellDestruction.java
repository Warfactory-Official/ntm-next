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

package com.hbm.explosion.vanillant.standard;

import com.hbm.explosion.vanillant.ComposableExplosion;
import com.hbm.explosion.vanillant.ExplosionBlockTransform;
import com.hbm.explosion.vanillant.interfaces.ExplosionBlockProcessor;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.ArrayList;
import java.util.Set;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Util;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public final class CellDestruction implements ExplosionBlockProcessor {

    static final BiConsumer<ItemStack, BlockPos> DISCARD =
            (_, _) -> {};

    private final Explosion.BlockInteraction interaction;
    private final boolean drops;
    private final int fortune;
    private @Nullable ExplosionBlockTransform transform;

    private CellDestruction(Explosion.BlockInteraction interaction, boolean drops, int fortune) {
        this.interaction = interaction;
        this.drops = drops;
        this.fortune = fortune;
    }

    public static CellDestruction decay() {
        return new CellDestruction(Explosion.BlockInteraction.DESTROY_WITH_DECAY, true, 0);
    }

    public static CellDestruction allDrops() {
        return new CellDestruction(Explosion.BlockInteraction.DESTROY, true, 0);
    }

    public static CellDestruction noDrops() {
        return new CellDestruction(Explosion.BlockInteraction.DESTROY_WITH_DECAY, false, 0);
    }

    public static CellDestruction fortune(int level) {
        if (level <= 0) throw new IllegalArgumentException("Fortune level " + level + " <= 0");
        return new CellDestruction(Explosion.BlockInteraction.DESTROY, true, level);
    }

    public CellDestruction transform(@Nullable ExplosionBlockTransform transform) {
        this.transform = transform;
        return this;
    }

    @Override
    public Explosion.BlockInteraction interaction() {
        return interaction;
    }

    @Override
    public void process(
            ComposableExplosion blast,
            ServerLevel level,
            double x,
            double y,
            double z,
            Set<BlockPos> cells) {
        var order = new ObjectArrayList<BlockPos>(cells);
        Util.shuffle(order, level.getRandom());
        var collector = drops ? new Drops() : null;
        BiConsumer<ItemStack, BlockPos> sink =
                collector == null || fortune > 0 ? DISCARD : collector;
        var tool = fortune > 0 ? fortuneTool(level) : ItemStack.EMPTY;
        int kept = 0;
        for (int i = 0, n = order.size(); i < n; i++) {
            var pos = order.get(i);
            var state = level.getBlockState(pos);
            if (state.isAir()) {
                cells.remove(pos);
                continue;
            }
            if (fortune > 0 && state.getBlock().dropFromExplosion((Explosion) blast)) {
                var blockEntity = state.hasBlockEntity() ? level.getBlockEntity(pos) : null;
                var loot =
                        new LootParams.Builder(level)
                                .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos))
                                .withParameter(LootContextParams.TOOL, tool)
                                .withOptionalParameter(LootContextParams.BLOCK_ENTITY, blockEntity)
                                .withOptionalParameter(
                                        LootContextParams.THIS_ENTITY,
                                        blast.getDirectSourceEntity());
                for (var stack : state.getDrops(loot)) collector.accept(stack, pos);
            }
            state.onExplosionHit(level, pos, blast, sink);
            if (transform != null) transform.afterDestroy(blast, state, pos);
            order.set(kept++, pos);
        }
        if (collector != null) collector.pop(level);
        if (transform == null) return;
        for (int i = 0; i < kept; i++) {
            var pos = order.get(i);
            if (level.getBlockState(pos).isAir()) transform.afterPass(blast, pos);
        }
    }

    private ItemStack fortuneTool(ServerLevel level) {
        var tool = new ItemStack(Items.STICK);
        tool.enchant(
                level.registryAccess()
                        .lookupOrThrow(Registries.ENCHANTMENT)
                        .getOrThrow(Enchantments.FORTUNE),
                fortune);
        return tool;
    }

    static final class Drops implements BiConsumer<ItemStack, BlockPos> {

        private final ArrayList<ItemStack> stacks = new ArrayList<>();
        private final ArrayList<BlockPos> origins = new ArrayList<>();

        @Override
        public void accept(ItemStack stack, BlockPos pos) {
            for (int i = 0, n = stacks.size(); i < n; i++) {
                var held = stacks.get(i);
                if (ItemEntity.areMergable(held, stack))
                    stacks.set(i, ItemEntity.merge(held, stack, 16));
                if (stack.isEmpty()) return;
            }
            stacks.add(stack);
            origins.add(pos);
        }

        void pop(ServerLevel level) {
            for (int i = 0, n = stacks.size(); i < n; i++)
                Block.popResource(level, origins.get(i), stacks.get(i));
        }
    }
}
