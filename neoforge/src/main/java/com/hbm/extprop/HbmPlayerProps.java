// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.extprop;

import com.hbm.handler.ArmorModHandler;
import com.hbm.handler.EnumCraneKey;
import com.hbm.handler.EnumToolKey;
import com.hbm.handler.HbmKeybinds.EnumKeybind;
import com.hbm.items.armor.ItemModShield;
import com.hbm.platform.Services;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;

public final class HbmPlayerProps {

    public static final int dashCooldownLength = 5;
    public static final int plinkCooldownLength = 10;
    public static final float shieldCap = 100F;
    public static final MapCodec<HbmPlayerProps> MAP_CODEC =
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
                                                    .optionalFieldOf("plinkCooldown", 0)
                                                    .forGetter(p -> p.plinkCooldown),
                                            Codec.INT
                                                    .optionalFieldOf("dashCount", 0)
                                                    .forGetter(p -> p.dashCount),
                                            Codec.INT
                                                    .optionalFieldOf("dashCooldown", 0)
                                                    .forGetter(p -> p.dashCooldown),
                                            Codec.INT
                                                    .optionalFieldOf("stamina", 0)
                                                    .forGetter(p -> p.stamina),
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
                                    .apply(i, HbmPlayerProps::create));

    public final boolean[] craneKeysPressed = new boolean[EnumCraneKey.VALUES.length];
    public final boolean[] toolKeysPressed = new boolean[EnumToolKey.VALUES.length];

    public final boolean[] keysPressed = new boolean[EnumKeybind.VALUES.length];
    public float shield;
    public float maxShield;

    public int lastDamage;
    public int plinkCooldown;
    public int dashCount;
    public int dashCooldown;
    public int stamina;
    public int reputation;

    public boolean enableHUD = true;

    public boolean enableBackpack = true;

    public boolean enableMagnet = true;

    public boolean crateOpenHeld = true;
    public int maskManTimer;
    public long fbiMark;
    public boolean radMark;
    public boolean dashKeyPressed;

    public int grenadeDeployment;

    public HbmPlayerProps() {}

    private static HbmPlayerProps create(
            float shield,
            float maxShield,
            int plinkCooldown,
            int dashCount,
            int dashCooldown,
            int stamina,
            int reputation,
            boolean enableHUD,
            boolean enableBackpack,
            boolean enableMagnet,
            boolean crateOpenHeld,
            int maskManTimer,
            long fbiMark,
            boolean radMark) {
        HbmPlayerProps p = new HbmPlayerProps();
        p.maskManTimer = maskManTimer;
        p.fbiMark = fbiMark;
        p.radMark = radMark;
        p.enableHUD = enableHUD;
        p.enableBackpack = enableBackpack;
        p.enableMagnet = enableMagnet;
        p.crateOpenHeld = crateOpenHeld;
        p.shield = shield;
        p.maxShield = maxShield;
        p.plinkCooldown = plinkCooldown;
        p.dashCount = dashCount;
        p.dashCooldown = dashCooldown;
        p.stamina = stamina;
        p.reputation = reputation;
        return p;
    }

    public static HbmPlayerProps getData(Player player) {
        return Services.ENTITY_DATA.get(player, ModEntityData.PLAYER_PROPS);
    }

    public boolean isCraneKeyPressed(EnumCraneKey key) {
        return craneKeysPressed[key.ordinal()];
    }

    public void setCraneKeyPressed(EnumCraneKey key, boolean pressed) {
        craneKeysPressed[key.ordinal()] = pressed;
    }

    public boolean isToolKeyPressed(EnumToolKey key) {
        return toolKeysPressed[key.ordinal()];
    }

    public void setToolKeyPressed(EnumToolKey key, boolean pressed) {
        toolKeysPressed[key.ordinal()] = pressed;
    }

    public boolean getKeyPressed(EnumKeybind key) {
        return keysPressed[key.ordinal()];
    }

    public void setKeyPressed(EnumKeybind key, boolean pressed) {
        keysPressed[key.ordinal()] = pressed;
    }

    public static void plink(Player player, SoundEvent sound, float volume, float pitch) {
        HbmPlayerProps props = getData(player);
        if (props.plinkCooldown > 0) return;
        player.level()
                .playSound(
                        null,
                        player.getX(),
                        player.getY(),
                        player.getZ(),
                        sound,
                        SoundSource.PLAYERS,
                        volume,
                        pitch);
        props.plinkCooldown = plinkCooldownLength;
    }

    public boolean isJetpackActive() {
        return enableBackpack && getKeyPressed(EnumKeybind.JETPACK);
    }

    public boolean isMagnetActive() {
        return enableMagnet;
    }

    public float getEffectiveMaxShield(Player player) {
        float maximum = maxShield;
        var chest = player.getItemBySlot(EquipmentSlot.CHEST);
        if (chest.isEmpty()) return maximum;
        var mod = ArmorModHandler.pryMod(chest, ArmorModHandler.KEVLAR);
        if (mod.getItem() instanceof ItemModShield shieldMod) maximum += shieldMod.shield;
        return maximum;
    }
}
