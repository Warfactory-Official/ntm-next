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

package com.hbm.render.item.weapon.sedna;

import com.hbm.client.HeldItemAnimations;
import com.hbm.client.weapon.GunClientState.SmokeNode;
import com.hbm.client.weapon.GunClientState;
import com.hbm.items.weapon.sedna.GunBaseNTItem;
import com.hbm.render.anim.AnimationPlayback;
import com.hbm.render.anim.AnimationSequence;
import com.hbm.util.GameTime;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.jspecify.annotations.Nullable;

public final class GunRenderState {

    private final ItemStack stack;
    private final GunBaseNTItem gun;
    private final float partialTick;
    private final int slot;
    private final @Nullable Item animated;
    private final long now;
    private final double[] transform = new double[AnimationSequence.SIZE];
    private final Quaternionf rotation = new Quaternionf();

    private GunRenderState(ItemStack stack, float partialTick, int slot, @Nullable Item animated) {
        this.stack = stack;
        this.gun = (GunBaseNTItem) stack.getItem();
        this.partialTick = partialTick;
        this.slot = slot;
        this.animated = animated;
        this.now = GameTime.now();
    }

    public static GunRenderState extractLocalState(
            ItemStack stack, Player player, float partialTick) {
        var item = stack.getItem();
        return new GunRenderState(
                stack,
                partialTick,
                player.getInventory().getSelectedSlot(),
                player.getMainHandItem().is(item) ? item : null);
    }

    public ItemStack getItemStack() {
        return stack;
    }

    public GunBaseNTItem getGun() {
        return gun;
    }

    public float getPartialTick() {
        return partialTick;
    }

    public double get(int config, GunAnimationChannels.Scalar channel) {
        return value(config, channel, channel.axis);
    }

    public double x(int config, GunAnimationChannels.Vec3 channel) {
        return value(config, channel, 0);
    }

    public double y(int config, GunAnimationChannels.Vec3 channel) {
        return value(config, channel, 1);
    }

    public double z(int config, GunAnimationChannels.Vec3 channel) {
        return value(config, channel, 2);
    }

    public void translate(PoseStack pose, int config, GunAnimationChannels.Vec3 channel) {
        pose.translate(
                value(config, channel, 0), value(config, channel, 1), value(config, channel, 2));
    }

    public double rotation(int config, GunAnimationChannels.Part channel, int axis) {
        return value(config, channel, 3 + axis);
    }

    public void apply(PoseStack pose, int config, GunAnimationChannels.Part channel) {
        var playback = playback(config);
        var sequence = sequence(playback, channel);
        double[] v =
                sequence == null
                        ? AnimationSequence.identity(transform)
                        : sequence.sample(playback.millis(now), playback.timeScale(), transform);
        pose.translate(v[0], v[1], v[2]);
        for (int i = 12; i < 15; i++) {
            int axis = (int) v[i];
            float angle = (float) v[3 + axis] * (float) (Math.PI / 180.0);
            final Quaternionfc rotation2 =
                    axis == 0
                            ? rotation.rotationX(angle)
                            : axis == 1 ? rotation.rotationY(angle) : rotation.rotationZ(angle);
            pose.rotate(rotation2);
        }
        pose.translate(-v[9], -v[10], -v[11]);
        pose.scale((float) v[6], (float) v[7], (float) v[8]);
    }

    public boolean carries(int config, GunAnimationChannels.Channel channel) {
        return sequence(playback(config), channel) != null;
    }

    public float aim() {
        return GunClientState.getAim(partialTick);
    }

    public float tickAim() {
        return GunClientState.getAim();
    }

    public boolean steadyAim() {
        return GunClientState.isSteadyAim();
    }

    public long lastShot(int config) {
        return GunClientState.getLastShotTime(gun, config);
    }

    public double shotRandom() {
        return GunClientState.getShotRandom(gun);
    }

    public List<SmokeNode> smoke(int config) {
        return GunClientState.getSmokeParticles(gun, config);
    }

    private double value(int config, GunAnimationChannels.Channel channel, int dimension) {
        var playback = playback(config);
        var sequence = sequence(playback, channel);
        return sequence == null
                ? 0
                : sequence.getValue(dimension, playback.millis(now), playback.timeScale());
    }

    private @Nullable AnimationPlayback playback(int config) {
        if (animated == null) return null;
        var playback = HeldItemAnimations.get(slot, config);
        return playback != null && playback.item() == animated && playback.live(now)
                ? playback
                : null;
    }

    private static @Nullable AnimationSequence sequence(
            @Nullable AnimationPlayback playback, GunAnimationChannels.Channel channel) {
        return playback == null || playback.channels() == null
                ? null
                : playback.channels()[channel.slot];
    }
}
