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

package com.hbm.extprop;

import com.hbm.handler.ArmorModHandler;
import com.hbm.handler.CraneKey;
import com.hbm.handler.KeybindType;
import com.hbm.handler.ToolKey;
import com.hbm.items.armor.ModShieldItem;
import com.hbm.sound.EntitySounds;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;

public final class PlayerState {
    public static final float SHIELD_CAP = 100F;
    public static final int DASH_COOLDOWN = 5;
    public static final int PLINK_COOLDOWN = 10;

    public static final MapCodec<PlayerState> CODEC =
            RecordCodecBuilder.mapCodec(
                    i ->
                            i.group(
                                            Codec.FLOAT
                                                    .optionalFieldOf("shield", 0F)
                                                    .forGetter(p -> p.shield),
                                            Codec.FLOAT
                                                    .optionalFieldOf("maxShield", 0F)
                                                    .forGetter(p -> p.maxShield),
                                            Codec.INT
                                                    .optionalFieldOf("reputation", 0)
                                                    .forGetter(p -> p.reputation),
                                            Codec.BOOL
                                                    .optionalFieldOf("enableHUD", true)
                                                    .forGetter(p -> p.enableHUD),
                                            Codec.BOOL
                                                    .optionalFieldOf("enableBackpack", true)
                                                    .forGetter(p -> p.enableBackpack),
                                            Codec.BOOL
                                                    .optionalFieldOf("enableMagnet", true)
                                                    .forGetter(p -> p.enableMagnet),
                                            Codec.BOOL
                                                    .optionalFieldOf("crateOpenHeld", true)
                                                    .forGetter(p -> p.crateOpenHeld),
                                            Codec.INT
                                                    .optionalFieldOf("maskManTimer", 0)
                                                    .forGetter(p -> p.maskManTimer),
                                            Codec.LONG
                                                    .optionalFieldOf("fbiMark", 0L)
                                                    .forGetter(p -> p.fbiMark),
                                            Codec.BOOL
                                                    .optionalFieldOf("radMark", false)
                                                    .forGetter(p -> p.radMark))
                                    .apply(i, PlayerState::decoded));

    public float shield;
    public float maxShield;
    public int lastDamage;
    public int reputation;
    public boolean enableHUD = true;
    public boolean enableBackpack = true;
    public boolean enableMagnet = true;
    public boolean crateOpenHeld = true;
    public long fbiMark;
    public boolean radMark;
    public int maskManTimer;
    public int plinkCooldown;
    public int dashCount, dashCooldown, stamina;
    public int grenadeDraw;
    public int flightIntent;
    public boolean fallCapped;
    private int playerKeys;
    private int craneKeys;
    private int toolKeys;

    private static PlayerState decoded(
            float shield,
            float maxShield,
            int reputation,
            boolean enableHUD,
            boolean enableBackpack,
            boolean enableMagnet,
            boolean crateOpenHeld,
            int maskManTimer,
            long fbiMark,
            boolean radMark) {
        var state = new PlayerState();
        state.shield = shield;
        state.maxShield = maxShield;
        state.reputation = reputation;
        state.enableHUD = enableHUD;
        state.enableBackpack = enableBackpack;
        state.enableMagnet = enableMagnet;
        state.crateOpenHeld = crateOpenHeld;
        state.maskManTimer = maskManTimer;
        state.fbiMark = fbiMark;
        state.radMark = radMark;
        return state;
    }

    public static PlayerState getOrCreate(Player player) {
        return ((Entity) player).getAttachedOrCreate(ModEntityData.PLAYER_PROPS);
    }

    static void respawned(ServerPlayer player) {
        var old = getOrCreate(player);
        var fresh =
                decoded(
                        old.shield,
                        old.maxShield,
                        old.reputation,
                        old.enableHUD,
                        old.enableBackpack,
                        old.enableMagnet,
                        old.crateOpenHeld,
                        old.maskManTimer,
                        old.fbiMark,
                        old.radMark);
        ((Entity) player).setAttached(ModEntityData.PLAYER_PROPS, fresh);
    }

    void changedLevel() {
        grenadeDraw = 0;
        setHeld(KeybindType.JUMP, false);
    }

    public boolean held(KeybindType key) {
        return (playerKeys & 1 << key.ordinal()) != 0;
    }

    public void setHeld(KeybindType key, boolean held) {
        playerKeys = held ? playerKeys | 1 << key.ordinal() : playerKeys & ~(1 << key.ordinal());
    }

    public boolean held(CraneKey key) {
        return (craneKeys & 1 << key.ordinal()) != 0;
    }

    public void setHeld(CraneKey key, boolean held) {
        craneKeys = held ? craneKeys | 1 << key.ordinal() : craneKeys & ~(1 << key.ordinal());
    }

    public boolean held(ToolKey key) {
        return (toolKeys & 1 << key.ordinal()) != 0;
    }

    public void setHeld(ToolKey key, boolean held) {
        toolKeys = held ? toolKeys | 1 << key.ordinal() : toolKeys & ~(1 << key.ordinal());
    }

    public boolean jetpackActive() {
        return enableBackpack && held(KeybindType.JUMP);
    }

    public boolean magnetActive() {
        return enableMagnet;
    }

    public float shieldCapacity(Player player) {
        return ArmorModHandler.modItem(
                                player.getItemBySlot(EquipmentSlot.CHEST), ArmorModHandler.KEVLAR)
                        instanceof ModShieldItem mod
                ? maxShield + mod.shield
                : maxShield;
    }

    public static void plink(Player player, SoundEvent sound, float volume, float pitch) {
        var state = getOrCreate(player);
        if (state.plinkCooldown > 0) return;
        EntitySounds.play(
                player,
                player.getX(),
                player.getY(),
                player.getZ(),
                sound,
                SoundSource.PLAYERS,
                volume,
                pitch);
        state.plinkCooldown = PLINK_COOLDOWN;
    }

    public static void acceptMirror(
            Player player,
            float shield,
            float maxShield,
            int reputation,
            boolean enableBackpack,
            boolean enableHUD,
            boolean enableMagnet) {
        var state = getOrCreate(player);
        state.shield = shield;
        state.maxShield = maxShield;
        state.reputation = reputation;
        state.enableBackpack = enableBackpack;
        state.enableHUD = enableHUD;
        state.enableMagnet = enableMagnet;
    }
}
