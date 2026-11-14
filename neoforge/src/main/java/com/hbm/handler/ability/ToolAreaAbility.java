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

package com.hbm.handler.ability;

import com.hbm.data.DataGroups;
import com.hbm.data.ItemData;
import com.hbm.explosion.vanillant.ComposableExplosion;
import com.hbm.explosion.vanillant.standard.AttenuatedRayBlockSelector;
import com.hbm.explosion.vanillant.standard.CellDestruction;
import com.hbm.items.tool.ToolAbilityItem;
import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.jspecify.annotations.Nullable;

public final class ToolAreaAbility extends BaseAbility {

    private static final int[] VEIN_RADII = {3, 4, 5, 6, 7, 9, 10};
    private static final int[] AOE_RANGES = {1, 2, 3, 4};
    private static final float[] BLAST_STRENGTHS = {2.5F, 5.0F, 10.0F, 15.0F};
    private static final float PUFF_POWER = 0.1F;

    public static final ToolAreaAbility NONE =
            new ToolAreaAbility(
                    0,
                    "none",
                    "",
                    oneLevel(),
                    null,
                    true,
                    (_, _) -> false);
    public static final ToolAreaAbility VEIN_MINER =
            new ToolAreaAbility(
                    1,
                    "recursion",
                    "tool.ability.recursion",
                    levelText(VEIN_RADII),
                    ItemData.TOOL_ABILITY_VEIN,
                    true,
                    (dig, level) -> VeinMining.run(dig, VEIN_RADII[level]));
    public static final ToolAreaAbility AOE =
            new ToolAreaAbility(
                    2,
                    "hammer",
                    "tool.ability.hammer",
                    levelText(AOE_RANGES),
                    ItemData.TOOL_ABILITY_HAMMER,
                    true,
                    (dig, level) ->
                            box(dig, AOE_RANGES[level], AOE_RANGES[level], AOE_RANGES[level]));
    public static final ToolAreaAbility FLAT_AOE =
            new ToolAreaAbility(
                    3,
                    "hammer_flat",
                    "tool.ability.hammer_flat",
                    levelText(AOE_RANGES),
                    ItemData.TOOL_ABILITY_HAMMER,
                    true,
                    (dig, level) -> facingSquare(dig, AOE_RANGES[level]));
    public static final ToolAreaAbility EXPLOSION =
            new ToolAreaAbility(
                    4,
                    "explosion",
                    "tool.ability.explosion",
                    levelText(BLAST_STRENGTHS),
                    ItemData.TOOL_ABILITY_EXPLOSION,
                    false,
                    (dig, level) -> blast(dig, BLAST_STRENGTHS[level]));

    private static final ToolAreaAbility[] MEMBERS = {NONE, VEIN_MINER, AOE, FLAT_AOE, EXPLOSION};

    public static final Codec<ToolAreaAbility> CODEC =
            AbilityCodecs.byId(MEMBERS, ToolAreaAbility::id);
    public static final StreamCodec<ByteBuf, ToolAreaAbility> STREAM_CODEC =
            AbilityCodecs.byIndex(MEMBERS);

    private final String id;
    private final boolean permitsHarvest;
    private final Reach reach;

    private ToolAreaAbility(
            int sortOrder,
            String id,
            String translationKey,
            String[] levelText,
            DataGroups.@Nullable Param<Boolean> gate,
            boolean permitsHarvest,
            Reach reach) {
        super(translationKey, sortOrder, levelText, gate);
        this.id = id;
        this.permitsHarvest = permitsHarvest;
        this.reach = reach;
    }

    public String id() {
        return id;
    }

    @Override
    int index() {
        return sortOrder();
    }

    public boolean permitsHarvest() {
        return permitsHarvest;
    }

    public boolean dig(ToolDig dig, int level) {
        return reach.take(dig, level);
    }

    private static boolean box(ToolDig dig, int rx, int ry, int rz) {
        BlockPos reference = dig.reference();
        var pos = new BlockPos.MutableBlockPos();
        for (int dx = -rx; dx <= rx; dx++) {
            for (int dy = -ry; dy <= ry; dy++) {
                for (int dz = -rz; dz <= rz; dz++) {
                    if ((dx | dy | dz) == 0) continue;
                    ToolAbilityItem.breakExtraBlock(dig, pos.setWithOffset(reference, dx, dy, dz));
                }
            }
        }
        return false;
    }

    private static boolean facingSquare(ToolDig dig, int r) {
        var player = dig.player();
        HitResult hit = player.pick(player.blockInteractionRange(), 1.0F, false);
        if (hit.getType() != HitResult.Type.BLOCK) return true;

        Direction.Axis axis = ((BlockHitResult) hit).getDirection().getAxis();
        return box(
                dig,
                axis == Direction.Axis.X ? 0 : r,
                axis == Direction.Axis.Y ? 0 : r,
                axis == Direction.Axis.Z ? 0 : r);
    }

    private static boolean blast(ToolDig dig, float strength) {
        var level = dig.level();
        var player = dig.player();
        BlockPos reference = dig.reference();
        double x = reference.getX() + 0.5D;
        double y = reference.getY() + 0.5D;
        double z = reference.getZ() + 0.5D;
        new ComposableExplosion(level, x, y, z, strength, player)
                .blockSelector(new AttenuatedRayBlockSelector(16))
                .blockProcessor(CellDestruction.allDrops())
                .effects(
                        (_,
                                server,
                                bx,
                                by,
                                bz,
                                _) -> {
                            var random = server.getRandom();
                            server.playSound(
                                    null,
                                    bx,
                                    by,
                                    bz,
                                    SoundEvents.GENERIC_EXPLODE,
                                    SoundSource.BLOCKS,
                                    4.0F,
                                    (1.0F + (random.nextFloat() - random.nextFloat()) * 0.2F)
                                            * 0.7F);
                        })
                .explode();
        level.explode(player, x, y, z, PUFF_POWER, Level.ExplosionInteraction.NONE);
        return true;
    }

    @FunctionalInterface
    private interface Reach {

        boolean take(ToolDig dig, int level);
    }
}
