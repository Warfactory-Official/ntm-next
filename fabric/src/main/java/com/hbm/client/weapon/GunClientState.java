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

package com.hbm.client.weapon;

import com.hbm.items.weapon.sedna.GunBaseNTItem;
import com.hbm.render.anim.AnimationEnums.GunAnimation;
import com.hbm.util.GameTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import org.jspecify.annotations.Nullable;

public final class GunClientState {

    private static final float AIM_STEP = 0.25F;
    private static final ConcurrentHashMap<Item, Shots> SHOTS = new ConcurrentHashMap<>();
    private static final GunAnimation[] CUES = new GunAnimation[8];
    private static final long[] CUE_TICKS = new long[8];
    private static @Nullable Player heldPlayer;
    private static @Nullable Item heldItem;
    private static int heldSlot;
    private static float previousAim;
    private static float aim;
    private static float handPartialTick;

    private GunClientState() {}

    public static float getAim() {
        return aim;
    }

    public static float getPreviousAim() {
        return previousAim;
    }

    public static float getAim(float partialTick) {
        return previousAim + (aim - previousAim) * partialTick;
    }

    public static float getHandAim() {
        return getAim(handPartialTick);
    }

    public static void onHandRendered(float partialTick) {
        handPartialTick = partialTick;
    }

    public static boolean isSteadyAim() {
        return previousAim >= 1F && aim >= 1F;
    }

    public static void setAim(float previous, float current) {
        previousAim = previous;
        aim = current;
    }

    public static void advanceAim(boolean aiming) {
        previousAim = aim;
        if (aiming && aim < 1F) aim += AIM_STEP;
        if (!aiming && aim > 0F) aim -= AIM_STEP;
        aim = Mth.clamp(aim, 0F, 1F);
    }

    public static void updateHeldItem(@Nullable Player player) {
        if (player == null) {
            heldPlayer = null;
            heldItem = null;
            return;
        }
        var item = player.getMainHandItem().getItem();
        int slot = player.getInventory().getSelectedSlot();
        if (player == heldPlayer && item == heldItem && slot == heldSlot) return;
        heldPlayer = player;
        heldItem = item;
        heldSlot = slot;
        Arrays.fill(CUES, GunAnimation.EQUIP);
        Arrays.fill(CUE_TICKS, GameTime.ticks());
    }

    public static void recordAnimation(Player player, GunAnimation type, int config) {
        updateHeldItem(player);
        CUES[config] = type;
        CUE_TICKS[config] = GameTime.ticks();
    }

    public static @Nullable GunAnimation getLastAnimation(int config) {
        return CUES[config];
    }

    public static int getAnimationAge(int config) {
        return (int) (GameTime.ticks() - CUE_TICKS[config]);
    }

    public static long getLastShotTime(GunBaseNTItem gun, int config) {
        return shots(gun).lastShot[config];
    }

    public static double getShotRandom(GunBaseNTItem gun) {
        return shots(gun).random;
    }

    public static List<SmokeNode> getSmokeParticles(GunBaseNTItem gun, int config) {
        return shots(gun).smoke.get(config);
    }

    public static void onFired(GunBaseNTItem gun, int config, long millis, double random) {
        var shots = shots(gun);
        shots.lastShot[config] = millis;
        shots.random = random;
    }

    private static Shots shots(GunBaseNTItem gun) {
        var shots = SHOTS.get(gun);
        return shots != null
                ? shots
                : SHOTS.computeIfAbsent(
                        gun, _ -> new Shots(gun.getConfigCount()));
    }

    private static final class Shots {
        final long[] lastShot;
        final List<List<SmokeNode>> smoke;
        double random;

        Shots(int configs) {
            lastShot = new long[configs];
            var smoke = new ArrayList<List<SmokeNode>>(configs);
            for (int i = 0; i < configs; i++) smoke.add(new ArrayList<>());
            this.smoke = List.copyOf(smoke);
        }
    }

    public static final class SmokeNode {

        public double forward;
        public double side;
        public double lift;
        public double alpha;
        public double width = 1D;

        public SmokeNode(double alpha) {
            this.alpha = alpha;
        }
    }
}
