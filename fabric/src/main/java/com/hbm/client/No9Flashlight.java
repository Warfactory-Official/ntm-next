// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client;

import com.hbm.items.ModDataComponents;
import com.hbm.items.ModItems;
import com.hbm.util.TickPhase;
import it.unimi.dsi.fastutil.longs.Long2LongMap;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.Iterator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class No9Flashlight {

    private static final double RANGE = 50D;

    private static final int LINGER = 5;

    private static final Direction[] SPREAD = {
        Direction.EAST,
        Direction.WEST,
        Direction.UP,
        Direction.DOWN,
        Direction.SOUTH,
        Direction.NORTH
    };

    private static final Long2LongOpenHashMap written = new Long2LongOpenHashMap();
    private static final LongOpenHashSet visited = new LongOpenHashSet();

    private static final Long2ObjectOpenHashMap<DataLayer> patched = new Long2ObjectOpenHashMap<>();

    private static ClientLevel owner;

    private No9Flashlight() {}

    public static void clientTick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        ClientLevel level = mc.level;
        if (level != owner) {
            written.clear();
            owner = level;
        }
        if (player == null || level == null) return;

        expire(level);

        if (!TickPhase.every(player, 2)) return;
        ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
        if (helmet.getItem() != ModItems.NO9.get()) return;
        if (!helmet.getOrDefault(ModDataComponents.NO9_LAMP.get(), false)) return;

        Vec3 eye = player.getEyePosition();
        BlockHitResult hit =
                level.clip(
                        new ClipContext(
                                eye,
                                eye.add(player.getLookAngle().scale(RANGE)),
                                ClipContext.Block.COLLIDER,
                                ClipContext.Fluid.NONE,
                                player));
        if (hit.getType() != HitResult.Type.BLOCK) return;

        int light = (int) (25D - eye.distanceTo(hit.getLocation()) * 25D / RANGE);
        spread(level, hit.getBlockPos().relative(hit.getDirection()), Math.min(15, light));
        visited.clear();
        flush(level);
    }

    private static void spread(ClientLevel level, BlockPos pos, int light) {
        if (light <= 0) return;
        if (!visited.add(pos.asLong())) return;

        if (level.getBlockState(pos).getLightDampening() >= 15) return;

        write(
                level,
                pos,
                Math.min(15, Math.max(level.getBrightness(LightLayer.BLOCK, pos), light)));

        for (Direction direction : SPREAD) spread(level, pos.relative(direction), light - 1);
    }

    private static void write(ClientLevel level, BlockPos pos, int value) {
        SectionPos section = SectionPos.of(pos);
        DataLayer layer = patched.get(section.asLong());
        if (layer == null) {
            DataLayer current =
                    level.getLightEngine()
                            .getLayerListener(LightLayer.BLOCK)
                            .getDataLayerData(section);
            layer = current == null ? new DataLayer() : current.copy();
            patched.put(section.asLong(), layer);
        }
        layer.set(
                SectionPos.sectionRelative(pos.getX()),
                SectionPos.sectionRelative(pos.getY()),
                SectionPos.sectionRelative(pos.getZ()),
                value);
        written.put(pos.asLong(), level.getGameTime() + LINGER);
    }

    private static void flush(ClientLevel level) {
        LevelLightEngine engine = level.getLightEngine();
        for (Long2ObjectMap.Entry<DataLayer> entry : patched.long2ObjectEntrySet()) {
            SectionPos section = SectionPos.of(entry.getLongKey());
            engine.queueSectionData(LightLayer.BLOCK, section, entry.getValue());
            level.setSectionDirtyWithNeighbors(section.x(), section.y(), section.z());
        }
        patched.clear();
    }

    private static void expire(ClientLevel level) {
        if (written.isEmpty()) return;
        long now = level.getGameTime();
        LevelLightEngine engine = level.getLightEngine();
        Iterator<Long2LongMap.Entry> it = written.long2LongEntrySet().iterator();
        while (it.hasNext()) {
            Long2LongMap.Entry entry = it.next();
            if (now <= entry.getLongValue()) continue;
            engine.checkBlock(BlockPos.of(entry.getLongKey()));
            it.remove();
        }
    }
}
