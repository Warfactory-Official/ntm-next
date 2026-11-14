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

package com.hbm.client;

import com.hbm.client.InfoSystem;
import com.hbm.client.SootFog;
import com.hbm.client.render.AssemblyMarkers.Marker;
import com.hbm.client.render.AssemblyMarkers;
import com.hbm.client.render.GlyphidPathRenderer;
import com.hbm.client.weapon.GunClientState;
import com.hbm.config.GunVisualConfig;
import com.hbm.entity.item.FallingBlockBatchEntity;
import com.hbm.entity.item.FallingMultiblockEntity;
import com.hbm.entity.mob.TaintCrabEntity;
import com.hbm.entity.mob.TeslaCrabEntity;
import com.hbm.extprop.LivingHazards;
import com.hbm.handler.ImpactWorldHandler;
import com.hbm.items.AnimatedItem;
import com.hbm.items.armor.ArmorSuitEffects;
import com.hbm.items.armor.ModArmorItem;
import com.hbm.items.weapon.grenade.GrenadeShellItem.EnumGrenadeShell;
import com.hbm.items.weapon.grenade.GrenadeUniversalItem;
import com.hbm.items.weapon.sedna.GunBaseNTItem.LambdaContext;
import com.hbm.items.weapon.sedna.GunBaseNTItem;
import com.hbm.items.weapon.sedna.GunConfig;
import com.hbm.packet.ClientEventConstants;
import com.hbm.particle.HbmEffectNT;
import com.hbm.render.anim.AnimationEnums.GunAnimation;
import com.hbm.render.anim.AnimationEnums;
import com.hbm.render.anim.AnimationPlayback;
import com.hbm.render.anim.AnimationSequence;
import com.hbm.render.anim.BusAnimation;
import com.hbm.render.anim.BusAnimationKeyframe.IType;
import com.hbm.render.item.weapon.sedna.WeaponItemRenderer;
import com.hbm.util.GameTime;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.ViewArea;
import net.minecraft.client.renderer.chunk.CompiledSectionMesh;
import net.minecraft.client.renderer.chunk.SectionMesh;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public final class ClientPacketEvents {

    private ClientPacketEvents() {}

    public static void gunAnimation(
            int type, int receiverIndex, int itemIndex, int sentSlot, int sentItem) {
        Player player = ClientPlayerAccess.player();
        ItemStack stack = player.getMainHandItem();
        int slot = player.getInventory().getSelectedSlot();
        if (stack.isEmpty()) return;
        if (sentSlot != slot || sentItem != BuiltInRegistries.ITEM.getId(stack.getItem())) return;
        if (stack.getItem() instanceof GunBaseNTItem) {
            sednaAnimation(
                    player, stack, slot, GunAnimation.VALUES[type], receiverIndex, itemIndex);
        } else if (stack.getItem() instanceof AnimatedItem item) {
            animatedItem(
                    slot, stack, item.animation(AnimationEnums.ToolAnimation.VALUES[type], stack));
        }
    }

    private static void animatedItem(int slot, ItemStack stack, @Nullable BusAnimation animation) {
        if (animation == null) return;
        HeldItemAnimations.put(
                slot, 0, new AnimationPlayback(stack.getItem(), GameTime.millis(), animation));
    }

    public static void sednaAnimation(
            Player player,
            ItemStack stack,
            int slot,
            GunAnimation type,
            int receiverIndex,
            int gunIndex) {
        GunBaseNTItem gun = (GunBaseNTItem) stack.getItem();
        GunConfig config = gun.getConfig(stack, gunIndex);
        GunClientState.recordAnimation(player, type, gunIndex);
        if (type == GunAnimation.CYCLE) {
            GunClientState.onFired(
                    gun,
                    gunIndex,
                    GameTime.millis(player.level()),
                    player.level().getRandom().nextDouble());
            var receivers = config.getReceivers(stack);
            if (receiverIndex >= 0 && receiverIndex < receivers.size()) {
                var receiver = receivers.get(receiverIndex);
                var recoil = receiver.getRecoil(stack);
                if (recoil != null)
                    recoil.accept(
                            stack,
                            new LambdaContext(
                                    config, player, player.getInventory(), receiverIndex));
            }
        }
        var animations = config.getAnims(stack);
        BusAnimation animation = animations.apply(stack, type);
        if (animation == null
                && (type == GunAnimation.ALT_CYCLE || type == GunAnimation.CYCLE_EMPTY)) {
            animation = animations.apply(stack, GunAnimation.CYCLE);
        }
        if (animation != null) {
            boolean reload = type == GunAnimation.RELOAD || type == GunAnimation.RELOAD_CYCLE;
            double timeScale =
                    reload && ArmorSuitEffects.hasFullSet(player, ModArmorItem.Suit.TRENCHMASTER)
                            ? 0.5
                            : 1;
            HeldItemAnimations.put(
                    slot,
                    gunIndex,
                    WeaponItemRenderer.playback(
                            stack,
                            GameTime.millis(),
                            animation,
                            reload && config.getReloadAnimSequential(stack),
                            timeScale));
        }
    }

    public static void grenadeAnimation(EnumGrenadeShell shell) {
        try {
            Player player = ClientPlayerAccess.player();
            ItemStack stack = player.getMainHandItem();
            if (!(stack.getItem() instanceof GrenadeUniversalItem)) return;
            HeldItemAnimations.put(
                    player.getInventory().getSelectedSlot(),
                    0,
                    new AnimationPlayback(
                            stack.getItem(),
                            GameTime.millis(player.level()),
                            grenadeShellAnimation(shell)));
        } catch (Exception ignored) {
        }
    }

    private static BusAnimation grenadeShellAnimation(EnumGrenadeShell shell) {
        return switch (shell) {
            case FRAG, TECH ->
                    new BusAnimation()
                            .addBus(
                                    "BODYMOVE",
                                    new AnimationSequence()
                                            .jump(0, -5, 0)
                                            .to(0, -3, 0, 350)
                                            .to(0, 0, 0, 350, IType.SIN_DOWN))
                            .addBus(
                                    "BODYTURN",
                                    new AnimationSequence()
                                            .to(0, 0, 45, 350)
                                            .to(0, 0, -15, 350, IType.SIN_DOWN)
                                            .stay(200)
                                            .to(0, 0, -20, 100, IType.SIN_DOWN)
                                            .to(0, 0, 0, 500, IType.SIN_FULL))
                            .addBus(
                                    "RINGMOVE",
                                    new AnimationSequence()
                                            .stay(900)
                                            .to(0, 0, 1, 150)
                                            .to(0, -3, 3, 300))
                            .addBus("RINGTURN", new AnimationSequence().stay(900).to(0, 0, 45, 300))
                            .addBus(
                                    "RENDERRING",
                                    new AnimationSequence().jump(1, 1, 1).stay(1350).jump(0, 0, 0));
            case STICK ->
                    new BusAnimation()
                            .addBus(
                                    "BODYMOVE",
                                    new AnimationSequence()
                                            .jump(0, -7, 0)
                                            .to(0, 3, 0, 750, IType.SIN_DOWN)
                                            .stayUntil(1900)
                                            .to(0, 0, 0, 250, IType.SIN_FULL))
                            .addBus(
                                    "BODYTURN",
                                    new AnimationSequence()
                                            .jump(0, 0, 90)
                                            .to(0, 0, -45, 750, IType.SIN_DOWN)
                                            .stayUntil(1900)
                                            .to(0, 0, 0, 250, IType.SIN_FULL))
                            .addBus(
                                    "RINGMOVE",
                                    new AnimationSequence()
                                            .stay(800)
                                            .to(0, -0.25, 0, 200, IType.SIN_FULL)
                                            .stay(250)
                                            .to(0, -0.5, 0, 200, IType.SIN_FULL)
                                            .to(2, -5, 0, 350, IType.SIN_UP))
                            .addBus(
                                    "RINGTURN",
                                    new AnimationSequence()
                                            .stay(800)
                                            .to(0, 360, 0, 200, IType.SIN_FULL)
                                            .stay(250)
                                            .to(0, 720, 0, 200, IType.SIN_FULL))
                            .addBus(
                                    "RENDERRING",
                                    new AnimationSequence().jump(1, 1, 1).stay(2100).jump(0, 0, 0));
            case NUKE ->
                    new BusAnimation()
                            .addBus(
                                    "BODYMOVE",
                                    new AnimationSequence()
                                            .jump(0, -5, 0)
                                            .stay(250)
                                            .to(0, 0, 0, 850, IType.SIN_DOWN))
                            .addBus(
                                    "BODYTURN",
                                    new AnimationSequence()
                                            .jump(0, 0, 90)
                                            .stay(250)
                                            .to(0, 0, -25, 850, IType.SIN_DOWN)
                                            .stay(200)
                                            .to(0, 0, -30, 100, IType.SIN_DOWN)
                                            .to(0, 0, 0, 750, IType.SIN_FULL))
                            .addBus(
                                    "RINGMOVE",
                                    new AnimationSequence()
                                            .stay(1300)
                                            .to(0, 0, 1, 150)
                                            .to(0, -3, 3, 300))
                            .addBus(
                                    "RINGTURN",
                                    new AnimationSequence().stay(1300).to(0, 0, 720, 500))
                            .addBus(
                                    "RENDERRING",
                                    new AnimationSequence().jump(1, 1, 1).stay(1750).jump(0, 0, 0));
        };
    }

    public static void muzzleFlash(int entityId) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return;
        Entity entity = minecraft.level.getEntity(entityId);
        if (!(entity instanceof LivingEntity living)
                || living == minecraft.player
                || !(living.getMainHandItem().getItem() instanceof GunBaseNTItem)) return;
        WeaponItemRenderer.flashMap.put(living, GameTime.millis(living.level()));
    }

    public static void teslaArc(int entityId, List<Vec3> targets) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return;
        Entity entity = minecraft.level.getEntity(entityId);
        if (entity instanceof TeslaCrabEntity tesla) tesla.targets = targets;
        else if (entity instanceof TaintCrabEntity taint) taint.targets = targets;
    }

    public static void glyphidPath(
            int entityId,
            int nextIndex,
            int targetX,
            int targetY,
            int targetZ,
            int[] nodes,
            float[] costs) {
        if (Minecraft.getInstance().level == null) return;
        if (costs.length == 0) {
            if (entityId < 0) GlyphidPathRenderer.clearAll();
            else GlyphidPathRenderer.clear(entityId);
            return;
        }
        GlyphidPathRenderer.accept(entityId, nodes, costs, nextIndex, targetX, targetY, targetZ);
    }

    public static void gasFlame(
            Player viewer,
            double x,
            double y,
            double z,
            double motionX,
            double motionY,
            double motionZ,
            float scale) {
        ClientEffects.spawnGasFlame(viewer.level(), x, y, z, motionX, motionY, motionZ, scale);
    }

    public static void jetpackDns(Player viewer, int playerId) {
        ClientEffects.spawnJetpackPlume(viewer.level(), playerId, ClientEventConstants.JETPACK_DNS);
    }

    public static void jetpackBj(Player viewer, int playerId) {
        ClientEffects.spawnJetpackPlume(viewer.level(), playerId, ClientEventConstants.JETPACK_BJ);
    }

    public static void jetpackRegular(Player viewer, int playerId) {
        ClientEffects.spawnJetpackPlume(
                viewer.level(), playerId, ClientEventConstants.JETPACK_REGULAR);
    }

    public static void jetpackVector(Player viewer, int playerId) {
        ClientEffects.spawnJetpackPlume(
                viewer.level(), playerId, ClientEventConstants.JETPACK_VECTOR);
    }

    public static void effectRadFog(Player viewer, double x, double y, double z) {
        effectNT(viewer, HbmEffectNT.RadFog, x, y, z, 0F);
    }

    public static void effectExplosionSmall(Player viewer, double x, double y, double z) {
        effectNT(viewer, HbmEffectNT.ExplosionSmall, x, y, z, 0F);
    }

    public static void effectExplosionStandard(Player viewer, double x, double y, double z) {
        effectNT(viewer, HbmEffectNT.ExplosionStandard, x, y, z, 0F);
    }

    public static void effectExplosionLarge(Player viewer, double x, double y, double z) {
        effectNT(viewer, HbmEffectNT.ExplosionLarge, x, y, z, 0F);
    }

    public static void effectRBMKMush(Player viewer, double x, double y, double z, float scale) {
        effectNT(viewer, HbmEffectNT.RBMKMush, x, y, z, scale);
    }

    public static void effectGasFlame(Player viewer, double x, double y, double z) {
        effectNT(viewer, HbmEffectNT.GasFlame, x, y, z, 0F);
    }

    public static void effectGasFlameFlare(Player viewer, double x, double y, double z) {
        effectNT(viewer, HbmEffectNT.GasFlameFlare, x, y, z, 0F);
    }

    public static void effectHadron(Player viewer, double x, double y, double z) {
        effectNT(viewer, HbmEffectNT.Hadron, x, y, z, 0F);
    }

    public static void effectAnnihilatorFlame(Player viewer, double x, double y, double z) {
        effectNT(viewer, HbmEffectNT.AnnihilatorFlame, x, y, z, 0F);
    }

    public static void effectGasFlameVent(Player viewer, double x, double y, double z) {
        effectNT(viewer, HbmEffectNT.GasFlameVent, x, y, z, 0F);
    }

    private static void effectNT(
            Player viewer, HbmEffectNT effect, double x, double y, double z, float scale) {
        ClientEffects.spawn(effect, viewer.level(), x, y, z, scale);
    }

    public static void gunSound(
            Player viewer,
            boolean legacy,
            int emitterId,
            Holder<SoundEvent> sound,
            SoundSource source,
            int x,
            int y,
            int z,
            float volume,
            float pitch,
            long seed) {
        if (GunVisualConfig.legacyAnimations != legacy) return;
        if (emitterId == viewer.getId()) {
            viewer.level().playSeededSound(viewer, viewer, sound, source, volume, pitch, seed);
            return;
        }
        viewer.level()
                .playSeededSound(
                        viewer, x / 8.0F, y / 8.0F, z / 8.0F, sound, source, volume, pitch, seed);
    }

    public static void hbmPlayerSync(
            Player player,
            double radiation,
            double digamma,
            double radBuf,
            int asbestos,
            int bombTimer,
            int contagion,
            int blacklung,
            int oil) {
        LivingHazards props = LivingHazards.getOrCreate(player);
        props.radiation = radiation;
        props.digamma = digamma;
        props.fieldLastSecond = radBuf;
        props.asbestos = asbestos;
        props.bombTimer = bombTimer;
        props.setContagion(contagion);
        props.blackLung = blacklung;
        props.oil = oil;
    }

    public static void impactSync(Player player, float fire, float dust) {
        ImpactWorldHandler.receive(player, fire, dust);
    }

    public static void explosionKnockback(Player player, float x, float y, float z) {
        player.addDeltaMovement(new Vec3(x, y, z));
    }

    public static void pollutionSync(float soot) {
        SootFog.receive(soot);
    }

    public static void playerAppearance(Player viewer, int entityId, int flags) {
        Entity entity = viewer.level().getEntity(entityId);
        if (entity instanceof Player player)
            ((com.hbm.interfaces.injected.PlayerAppearance) (player))
                    .hbm$setAppearance((byte) flags);
    }

    public static void occlusionRefresh(long packedChunkPos) {
        Minecraft minecraft = Minecraft.getInstance();
        LevelRenderer levelRenderer = minecraft.levelRenderer;
        ClientLevel level = minecraft.level;
        if (level == null) return;
        ViewArea viewArea = levelRenderer.viewArea;
        if (viewArea == null) return;
        int chunkX = ChunkPos.getX(packedChunkPos);
        int chunkZ = ChunkPos.getZ(packedChunkPos);
        LevelChunk chunk = level.getChunkSource().getChunk(chunkX, chunkZ, ChunkStatus.FULL, false);
        if (chunk == null) return;
        LevelChunkSection[] sections = chunk.getSections();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int i = 0; i < sections.length; i++) {
            pos.set(chunkX << 4, chunk.getSectionYFromSectionIndex(i) << 4, chunkZ << 4);
            SectionRenderDispatcher.RenderSection renderSection = viewArea.getRenderSectionAt(pos);
            if (renderSection == null) continue;
            SectionMesh mesh = renderSection.getSectionMesh();
            if (sections[i].hasOnlyAir()
                    && mesh != CompiledSectionMesh.UNCOMPILED
                    && mesh != CompiledSectionMesh.EMPTY) renderSection.reset();
            levelRenderer.sectionOcclusionGraph.schedulePropagationFrom(renderSection);
        }
    }

    public static long[] fallingMultiblockMembers(int entityId, int count) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level != null && level.getEntity(entityId) instanceof FallingMultiblockEntity falling) {
            long[] members = falling.membersPacked();
            if (members.length == count) return members;
        }
        return new long[count];
    }

    public static void fallingMultiblock(
            int entityId,
            long corePos,
            long[] members,
            boolean carriedPresent,
            @Nullable CompoundTag carried) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return;
        Entity entity = level.getEntity(entityId);
        if (entity instanceof FallingMultiblockEntity falling) {
            falling.setMembersFromNetwork(members, corePos, carriedPresent ? carried : null);
        }
    }

    public static @Nullable FallingBlockBatchEntity fallingBlockBatch(int entityId, int revision) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || !(level.getEntity(entityId) instanceof FallingBlockBatchEntity batch))
            return null;
        return batch.beginNetworkSnapshot(revision) ? batch : null;
    }

    public static void fallingBlockBatchApplied(
            int entityId,
            int revision,
            boolean running,
            @Nullable FallingBlockBatchEntity children) {
        if (children != null) children.endNetworkSnapshot(revision, running);
    }

    public static long markerExpiry(int ttlMillis) {
        return ttlMillis > 0 ? GameTime.millis() + ttlMillis : 0;
    }

    public static void marker(
            BlockPos position, Component label, long expireAt, double maxDistance, int color) {
        AssemblyMarkers.queue(new Marker(position, label, expireAt, maxDistance, color));
    }

    public static void playerInform(Component message, int id, int millis) {
        InfoSystem.push(new InfoSystem.InfoEntry(message, millis), id);
    }

    public static void oreDensityRow(int id, int millis, Component row) {
        InfoSystem.push(new InfoSystem.InfoEntry(row, millis), id);
    }

    public static void oreDensity(int firstId, int millis, int rows, Component summary) {
        InfoSystem.push(new InfoSystem.InfoEntry(summary, millis), firstId + rows);
    }
}
