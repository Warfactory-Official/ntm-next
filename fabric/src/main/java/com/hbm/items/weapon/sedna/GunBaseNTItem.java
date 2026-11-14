/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-FileCopyrightText: Contributors to Hbm's Nuclear Tech Mod
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 * Copyright (C) Contributors to Hbm's Nuclear Tech Mod. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License version 3 only,
 * as published by the Free Software Foundation.
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this code. If not, see <https://www.gnu.org/licenses/>.
 *
 * Under GPLv3 §7(c) and §7(e), these additional terms apply to LGPL-covered
 * material authored by movblock:
 *
 * - the license grants no rights to the name "NTM NEXT";
 * - a modified version must not be named NTM NEXT or presented as the
 *   official release, and must be marked as different from it.
 *
 * Accurately identifying a work as derived from NTM NEXT does not violate
 * these additional terms, provided it is clearly distinguished from the
 * official release and does not imply endorsement. Ports are encouraged to
 * acknowledge NTM NEXT as their upstream project and provide a link to it.
 */

package com.hbm.items.weapon.sedna;

import com.hbm.client.ClientPlayerAccess;
import com.hbm.client.weapon.GunClientState;
import com.hbm.config.GunVisualConfig;
import com.hbm.data.ItemData;
import com.hbm.handler.KeybindType;
import com.hbm.items.EquipmentChangeListener;
import com.hbm.items.KeybindHandler;
import com.hbm.items.ModDataComponents;
import com.hbm.items.armor.ArmorSuitEffects;
import com.hbm.items.armor.ModArmorItem;
import com.hbm.items.weapon.sedna.AkimboGhost;
import com.hbm.items.weapon.sedna.GunConfig;
import com.hbm.items.weapon.sedna.GunStateData;
import com.hbm.items.weapon.sedna.GunTimers;
import com.hbm.items.weapon.sedna.impl.GunChargeThrowerItem;
import com.hbm.items.weapon.sedna.impl.GunStingerItem;
import com.hbm.items.weapon.sedna.mags.Magazine;
import com.hbm.items.weapon.sedna.mags.MagazineInfinite;
import com.hbm.items.weapon.sedna.mods.GunAttribute;
import com.hbm.items.weapon.sedna.mods.InstalledWeaponMods;
import com.hbm.items.weapon.sedna.mods.XWeaponModManager;
import com.hbm.packet.PacketWire;
import com.hbm.packet.layout.GunAnimationLayout;
import com.hbm.render.anim.AnimationEnums.GunAnimation;
import com.hbm.sound.SoundHandle;
import com.hbm.util.Blink;
import com.mojang.serialization.Codec;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import org.jetbrains.annotations.Nullable;

public class GunBaseNTItem extends Item implements KeybindHandler, EquipmentChangeListener {

    private static final int[] EMPTY_INTS = new int[0];

    public static final DecimalFormatSymbols SYMBOLS_ROOT = new DecimalFormatSymbols(Locale.ROOT);
    public static final DecimalFormat FORMAT_DMG = new DecimalFormat("#.##", SYMBOLS_ROOT);
    public static final GunAttribute.Ref<GunConfig> GUN_SPEC = new GunAttribute.Ref<>();
    public static final String KEY_DRAWN = "drawn";
    public static final String KEY_AIMING = "aiming";
    public static final String KEY_MODE = "mode_";
    public static final String KEY_WEAR = "wear_";
    public static final String KEY_TIMER = "timer_";
    public static final String KEY_STATE = "state_";
    public static final String KEY_PRIMARY = "mouse1_";
    public static final String KEY_SECONDARY = "mouse2_";
    public static final String KEY_TERTIARY = "mouse3_";
    public static final String KEY_RELOAD = "reload_";
    public static final String KEY_LASTANIM = "lastanim_";
    public static final String KEY_ANIMTIMER = "animtimer_";
    public static final String KEY_LOCKONTARGET = "lockontarget";
    public static final String KEY_LOCKEDON = "lockedon";
    public static final String KEY_CANCELRELOAD = "cancel";
    public static final String KEY_EQUIPPED = "eqipped";
    public static List<Item> secrets = new ArrayList<>();
    private static float recoilVertical;
    private static float recoilHorizontal;
    private static float recoilDecay = 0.75F;
    private static float recoilRebound = 0.25F;
    private static float offsetVertical;
    private static float offsetHorizontal;
    private static Player recoilPlayer;
    public static ConcurrentHashMap<LivingEntity, SoundHandle> loopedSounds =
            new ConcurrentHashMap<>();
    public List<Supplier<ItemStack>> recognizedMods = new ArrayList<>();
    public Enum<?> defaultAmmoType;
    public int defaultAmmoAmount;
    public boolean isDefaultExpensive = false;
    public Function<ItemStack, String> LAMBDA_NAME_MUTATOR;
    public WeaponQuality quality;
    private final GunConfig[] specs;

    public GunBaseNTItem(WeaponQuality quality, Properties properties, GunConfig.Builder... specs) {
        super(properties.stacksTo(1));
        this.specs = new GunConfig[specs.length];
        for (int i = 0; i < specs.length; i++) this.specs[i] = specs[i].build(i);
        this.quality = quality;
        if (quality == WeaponQuality.LEGENDARY || quality == WeaponQuality.SECRET)
            secrets.add(this);
    }

    public static void setupRecoil(float vertical, float horizontal, float decay, float rebound) {
        bindRecoilPlayer(ClientPlayerAccess.player());
        recoilVertical += vertical;
        recoilHorizontal += horizontal;
        recoilDecay = decay;
        recoilRebound = rebound;
    }

    public static void setupRecoil(float vertical, float horizontal) {
        setupRecoil(vertical, horizontal, 0.75F, 0.25F);
    }

    private static Player clientPlayer() {
        return ClientPlayerAccess.player();
    }

    public static void playAnimation(Player player, ItemStack stack, GunAnimation type, int index) {
        if (player instanceof ServerPlayer serverPlayer) {
            PacketWire.sendTo(
                    GunAnimationLayout.encode(
                            type.ordinal(),
                            0,
                            index,
                            serverPlayer.getInventory().getSelectedSlot(),
                            BuiltInRegistries.ITEM.getId(stack.getItem())),
                    serverPlayer);
        }

        setLastAnim(stack, index, type);
        setAnimTimer(stack, index, 0);
    }

    public static void clientHeldTick() {
        Player player = ClientPlayerAccess.player();
        GunClientState.updateHeldItem(player);
        if (player == null) return;
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof GunBaseNTItem gun)) return;

        int confNo = gun.specs.length;

        GunClientState.advanceAim(getIsAiming(stack));

        if (gun instanceof GunStingerItem) {
            GunStingerItem.clientLockonTick(stack);
        }
        if (gun instanceof GunChargeThrowerItem) {
            GunChargeThrowerItem.clientGrappleTick(stack);
        }

        for (int i = 0; i < confNo; i++) {
            GunConfig config = gun.getConfig(stack, i);
            LambdaContext ctx = new LambdaContext(config, player, player.getInventory(), i);

            if (config.getSmokeHandler(stack) != null)
                config.getSmokeHandler(stack).accept(stack, ctx);

            BiConsumer<ItemStack, LambdaContext> orchestra = config.getOrchestra(stack);
            if (orchestra != null) orchestra.accept(stack, ctx);
        }
    }

    public static void clientRecoilTick() {
        var player = ClientPlayerAccess.player();
        bindRecoilPlayer(player);
        if (player == null) return;

        if (GunVisualConfig.visualRecoil) {
            var pitch = player.getXRot();
            var yaw = player.getYRot();
            var vertical = -recoilVertical + (offsetVertical + recoilVertical) * recoilRebound;
            var horizontal =
                    -recoilHorizontal + (offsetHorizontal + recoilHorizontal) * recoilRebound;
            player.setXRot(Mth.clamp(pitch + vertical, -90F, 90F));
            player.setYRot(yaw + horizontal);

            offsetVertical += pitch - player.getXRot();
            offsetHorizontal += yaw - player.getYRot();
            recoilVertical *= recoilDecay;
            recoilHorizontal *= recoilDecay;
        } else {
            resetRecoil();
        }
    }

    private static void bindRecoilPlayer(Player player) {
        if (recoilPlayer == player) return;
        recoilPlayer = player;
        resetRecoil();
    }

    private static void resetRecoil() {
        offsetVertical = 0;
        offsetHorizontal = 0;
        recoilVertical = 0;
        recoilHorizontal = 0;
    }

    public static boolean getIsDrawn(ItemStack stack) {
        return getValueBool(stack, KEY_DRAWN);
    }

    public static void setIsDrawn(ItemStack stack, boolean value) {
        setValueBool(stack, KEY_DRAWN, value);
    }

    public static int getTimer(ItemStack stack, int index) {
        GunTimers held = GunTimers.of(stack);
        return held != null && held.owns()
                ? held.timer(index)
                : getValueInt(stack, KEY_TIMER + index);
    }

    public static void setTimer(ItemStack stack, int index, int value) {
        GunTimers held = GunTimers.of(stack);
        if (held != null && held.owns()) held.setTimer(index, value);
        else setValueInt(stack, KEY_TIMER + index, value);
    }

    public static GunState getState(ItemStack stack, int index) {
        return GunState.VALUES[getValueByte(stack, KEY_STATE + index)];
    }

    public static void setState(ItemStack stack, int index, GunState value) {
        setValueByte(stack, KEY_STATE + index, (byte) value.ordinal());
    }

    public static int getMode(ItemStack stack, int index) {
        return getValueInt(stack, KEY_MODE + index);
    }

    public static void setMode(ItemStack stack, int index, int value) {
        setValueInt(stack, KEY_MODE + index, value);
    }

    public static boolean getIsAiming(ItemStack stack) {
        return getValueBool(stack, KEY_AIMING);
    }

    public static void setIsAiming(ItemStack stack, boolean value) {
        setValueBool(stack, KEY_AIMING, value);
    }

    public static float getWear(ItemStack stack, int index) {
        return getValueFloat(stack, KEY_WEAR + index);
    }

    public static void setWear(ItemStack stack, int index, float value) {
        setValueFloat(stack, KEY_WEAR + index, value);
    }

    public static int getLockonTarget(ItemStack stack) {
        return getValueInt(stack, KEY_LOCKONTARGET);
    }

    public static void setLockonTarget(ItemStack stack, int value) {
        setValueInt(stack, KEY_LOCKONTARGET, value);
    }

    public static boolean getIsLockedOn(ItemStack stack) {
        return getValueBool(stack, KEY_LOCKEDON);
    }

    public static void setIsLockedOn(ItemStack stack, boolean value) {
        setValueBool(stack, KEY_LOCKEDON, value);
    }

    public static GunAnimation getLastAnim(ItemStack stack, int index) {
        return GunAnimation.VALUES[getValueInt(stack, KEY_LASTANIM + index)];
    }

    public static void setLastAnim(ItemStack stack, int index, GunAnimation value) {
        setValueInt(stack, KEY_LASTANIM + index, value.ordinal());
    }

    public static int getAnimTimer(ItemStack stack, int index) {
        GunTimers held = GunTimers.of(stack);
        return held != null ? held.animTimer(index) : getValueInt(stack, KEY_ANIMTIMER + index);
    }

    public static void setAnimTimer(ItemStack stack, int index, int value) {
        GunTimers.entry(stack).setAnimTimer(index, value);
    }

    public static boolean getPrimary(ItemStack stack, int index) {
        return getValueBool(stack, KEY_PRIMARY + index);
    }

    public static void setPrimary(ItemStack stack, int index, boolean value) {
        setValueBool(stack, KEY_PRIMARY + index, value);
    }

    public static boolean getSecondary(ItemStack stack, int index) {
        return getValueBool(stack, KEY_SECONDARY + index);
    }

    public static void setSecondary(ItemStack stack, int index, boolean value) {
        setValueBool(stack, KEY_SECONDARY + index, value);
    }

    public static boolean getTertiary(ItemStack stack, int index) {
        return getValueBool(stack, KEY_TERTIARY + index);
    }

    public static void setTertiary(ItemStack stack, int index, boolean value) {
        setValueBool(stack, KEY_TERTIARY + index, value);
    }

    public static boolean getReloadKey(ItemStack stack, int index) {
        return getValueBool(stack, KEY_RELOAD + index);
    }

    public static void setReloadKey(ItemStack stack, int index, boolean value) {
        setValueBool(stack, KEY_RELOAD + index, value);
    }

    public static boolean getReloadCancel(ItemStack stack) {
        return getValueBool(stack, KEY_CANCELRELOAD);
    }

    public static void setReloadCancel(ItemStack stack, boolean value) {
        setValueBool(stack, KEY_CANCELRELOAD, value);
    }

    public static boolean getIsEquipped(ItemStack stack) {
        return getValueBool(stack, KEY_EQUIPPED);
    }

    public static void setIsEquipped(ItemStack stack, boolean value) {
        setValueBool(stack, KEY_EQUIPPED, value);
    }

    public static GunStateData getData(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.GUN_STATE.get(), GunStateData.EMPTY);
    }

    private static void updateData(ItemStack stack, Consumer<CompoundTag> mutator) {
        CompoundTag tag = getData(stack).tag().copy();
        mutator.accept(tag);
        stack.set(ModDataComponents.GUN_STATE.get(), new GunStateData(tag));
    }

    public static int getValueInt(ItemStack stack, String name) {
        return getData(stack).tag().getIntOr(name, 0);
    }

    public static void setValueInt(ItemStack stack, String name, int value) {
        updateData(stack, tag -> tag.putInt(name, value));
    }

    public static float getValueFloat(ItemStack stack, String name) {
        return getData(stack).tag().getFloatOr(name, 0F);
    }

    public static void setValueFloat(ItemStack stack, String name, float value) {
        updateData(stack, tag -> tag.putFloat(name, value));
    }

    public static byte getValueByte(ItemStack stack, String name) {
        return getData(stack).tag().getByteOr(name, (byte) 0);
    }

    public static void setValueByte(ItemStack stack, String name, byte value) {
        updateData(stack, tag -> tag.putByte(name, value));
    }

    public static boolean getValueBool(ItemStack stack, String name) {
        return getData(stack).tag().getBooleanOr(name, false);
    }

    public static void setValueBool(ItemStack stack, String name, boolean value) {
        updateData(stack, tag -> tag.putBoolean(name, value));
    }

    public static Optional<String> getValueString(ItemStack stack, String name) {
        return getData(stack).tag().getString(name);
    }

    public static void setValueString(ItemStack stack, String name, String value) {
        updateData(stack, tag -> tag.putString(name, value));
    }

    public static int[] getValueIntArray(ItemStack stack, String name) {
        return getData(stack).tag().getIntArray(name).orElse(EMPTY_INTS);
    }

    public static void setValueIntArray(ItemStack stack, String name, int[] value) {
        updateData(stack, tag -> tag.putIntArray(name, value));
    }

    public static <T> void setValue(ItemStack stack, String name, Codec<T> codec, T value) {
        updateData(stack, tag -> tag.store(name, codec, value));
    }

    public static void removeValue(ItemStack stack, String name) {
        updateData(stack, tag -> tag.remove(name));
    }

    public GunConfig getConfig(ItemStack stack, int index) {
        return GUN_SPEC.resolve(specs[index], InstalledWeaponMods.of(stack, index));
    }

    public int getConfigCount() {
        return specs.length;
    }

    public GunBaseNTItem setDefaultAmmo(Enum<?> ammo, int amount) {
        this.defaultAmmoType = ammo;
        this.defaultAmmoAmount = amount;
        return this;
    }

    public GunBaseNTItem setDefaultAmmoExpensive(Enum<?> ammo, int amount) {
        this.isDefaultExpensive = true;
        return setDefaultAmmo(ammo, amount);
    }

    public GunBaseNTItem setNameMutator(Function<ItemStack, String> lambda) {
        this.LAMBDA_NAME_MUTATOR = lambda;
        return this;
    }

    @Override
    public Component getName(ItemStack stack) {

        if (this.LAMBDA_NAME_MUTATOR != null) {
            String unloc = this.LAMBDA_NAME_MUTATOR.apply(stack);
            if (unloc != null) return Component.translatable(unloc);
        }

        return super.getName(stack);
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> list,
            TooltipFlag ext) {

        Player player = clientPlayer();
        if (player == null) return;

        int configs = this.specs.length;
        for (int i = 0; i < configs; i++) {
            GunConfig config = getConfig(stack, i);
            for (var rec : config.getReceivers(stack)) {
                Magazine mag = rec.getMagazine(stack);
                if (!(mag instanceof MagazineInfinite)) {
                    ItemStack icon = mag.getIconForHud(stack, player);
                    list.accept(
                            Component.translatable("gui.weapon.ammo")
                                    .append(": ")
                                    .append(icon != null ? icon.getHoverName() : Component.empty())
                                    .append(" " + mag.getAmmoStateForHud(stack, player)));
                }
                float dmg = rec.getBaseDamage(stack);
                list.accept(
                        Component.translatable("gui.weapon.baseDamage")
                                .append(": " + FORMAT_DMG.format(dmg)));
                if (mag.getType(stack, player.getInventory()) instanceof BulletConfig bullet) {
                    int min = (int) (bullet.projectilesMin * rec.getSplitProjectiles(stack));
                    int max = (int) (bullet.projectilesMax * rec.getSplitProjectiles(stack));
                    list.accept(
                            Component.translatable("gui.weapon.damageWithAmmo")
                                    .append(
                                            ": "
                                                    + FORMAT_DMG.format(dmg * bullet.damageMult)
                                                    + (min > 1
                                                            ? (" x"
                                                                    + (min != max
                                                                            ? (min + "-" + max)
                                                                            : min))
                                                            : "")));
                }
            }

            float maxDura = config.getDurability(stack);
            if (maxDura > 0) {
                int dura = Mth.clamp((int) ((maxDura - getWear(stack, i)) * 100 / maxDura), 0, 100);
                list.accept(
                        Component.translatable("gui.weapon.condition").append(": " + dura + "%"));
            }

            for (ItemStack upgrade : XWeaponModManager.getUpgradeItems(stack, i)) {
                list.accept(upgrade.getHoverName().copy().withStyle(ChatFormatting.YELLOW));
            }
        }

        switch (this.quality) {
            case A_SIDE:
                list.accept(
                        Component.translatable("gui.weapon.quality.aside")
                                .withStyle(ChatFormatting.YELLOW));
                break;
            case B_SIDE:
                list.accept(
                        Component.translatable("gui.weapon.quality.bside")
                                .withStyle(ChatFormatting.GOLD));
                break;
            case LEGENDARY:
                list.accept(
                        Component.translatable("gui.weapon.quality.legendary")
                                .withStyle(ChatFormatting.RED));
                break;
            case SPECIAL:
                list.accept(
                        Component.translatable("gui.weapon.quality.special")
                                .withStyle(ChatFormatting.AQUA));
                break;
            case UTILITY:
                list.accept(
                        Component.translatable("gui.weapon.quality.utility")
                                .withStyle(ChatFormatting.GREEN));
                break;
            case SECRET:
                list.accept(
                        Component.translatable("gui.weapon.quality.secret")
                                .withStyle(
                                        Blink.lit()
                                                ? ChatFormatting.DARK_RED
                                                : ChatFormatting.RED));
                break;
            case DEBUG:
                list.accept(
                        Component.translatable("gui.weapon.quality.debug")
                                .withStyle(
                                        Blink.lit() ? ChatFormatting.YELLOW : ChatFormatting.GOLD));
                break;
        }

        if (ClientPlayerAccess.weaponTableOpen()) {
            list.accept(Component.translatable("gui.weapon.accepts"));
            for (Supplier<ItemStack> mod : recognizedMods) {
                list.accept(mod.get().getHoverName().copy().withStyle(ChatFormatting.YELLOW));
            }
        }
    }

    @Override
    public boolean canHandleKeybind(ServerPlayer player, ItemStack stack, KeybindType keybind) {
        if (AkimboGhost.isGhost(stack)) return false;
        return keybind == KeybindType.GUN_PRIMARY
                || keybind == KeybindType.GUN_SECONDARY
                || keybind == KeybindType.GUN_TERTIARY
                || keybind == KeybindType.RELOAD;
    }

    @Override
    public void onKeyChanged(
            ServerPlayer player, ItemStack stack, KeybindType keybind, boolean newState) {
        handleKeybind(player, player.getInventory(), stack, keybind, newState);
    }

    public void handleKeybind(
            LivingEntity entity,
            Container inventory,
            ItemStack stack,
            KeybindType keybind,
            boolean newState) {
        if (!ItemData.ENABLE_GUNS.get()) return;

        int configs = this.specs.length;

        for (int i = 0; i < configs; i++) {
            GunConfig config = getConfig(stack, i);
            LambdaContext ctx = new LambdaContext(config, entity, inventory, i);

            if (keybind == KeybindType.GUN_PRIMARY && newState && !getPrimary(stack, i)) {
                if (config.getPressPrimary(stack) != null)
                    config.getPressPrimary(stack).accept(stack, ctx);
                setPrimary(stack, i, newState);
                continue;
            }
            if (keybind == KeybindType.GUN_PRIMARY && !newState && getPrimary(stack, i)) {
                if (config.getReleasePrimary(stack) != null)
                    config.getReleasePrimary(stack).accept(stack, ctx);
                setPrimary(stack, i, newState);
                continue;
            }
            if (keybind == KeybindType.GUN_SECONDARY && newState && !getSecondary(stack, i)) {
                if (config.getPressSecondary(stack) != null)
                    config.getPressSecondary(stack).accept(stack, ctx);
                setSecondary(stack, i, newState);
                continue;
            }
            if (keybind == KeybindType.GUN_SECONDARY && !newState && getSecondary(stack, i)) {
                if (config.getReleaseSecondary(stack) != null)
                    config.getReleaseSecondary(stack).accept(stack, ctx);
                setSecondary(stack, i, newState);
                continue;
            }
            if (keybind == KeybindType.GUN_TERTIARY && newState && !getTertiary(stack, i)) {
                if (config.getPressTertiary(stack) != null)
                    config.getPressTertiary(stack).accept(stack, ctx);
                setTertiary(stack, i, newState);
                continue;
            }
            if (keybind == KeybindType.GUN_TERTIARY && !newState && getTertiary(stack, i)) {
                if (config.getReleaseTertiary(stack) != null)
                    config.getReleaseTertiary(stack).accept(stack, ctx);
                setTertiary(stack, i, newState);
                continue;
            }
            if (keybind == KeybindType.RELOAD && newState && !getReloadKey(stack, i)) {
                if (config.getPressReload(stack) != null)
                    config.getPressReload(stack).accept(stack, ctx);
                setReloadKey(stack, i, newState);
                continue;
            }
            if (keybind == KeybindType.RELOAD && !newState && getReloadKey(stack, i)) {
                if (config.getReleaseReload(stack) != null)
                    config.getReleaseReload(stack).accept(stack, ctx);
                setReloadKey(stack, i, newState);
                continue;
            }
        }
    }

    @Override
    public void onEquip(Player player, ItemStack stack) {
        if (AkimboGhost.isGhost(stack)) return;
        for (int i = 0; i < this.specs.length; i++) {
            if (getLastAnim(stack, i) == GunAnimation.EQUIP && getAnimTimer(stack, i) < 5) continue;
            playAnimation(player, stack, GunAnimation.EQUIP, i);
            setPrimary(stack, i, false);
            setSecondary(stack, i, false);
            setTertiary(stack, i, false);
            setReloadKey(stack, i, false);
        }
    }

    @Override
    public void inventoryTick(
            ItemStack stack, ServerLevel level, Entity entity, @Nullable EquipmentSlot slot) {
        if (AkimboGhost.isGhost(stack)) return;
        if (!(entity instanceof LivingEntity)) return;
        Player player = entity instanceof Player ? (Player) entity : null;
        boolean isHeld = slot == EquipmentSlot.MAINHAND && !entity.isSpectator();
        int confNo = this.specs.length;
        GunConfig single = confNo == 1 ? getConfig(stack, 0) : null;
        GunConfig[] configs = confNo == 1 ? null : new GunConfig[confNo];
        if (configs != null) {
            for (int i = 0; i < confNo; i++) configs[i] = getConfig(stack, i);
        }

        boolean wasHeld = getIsEquipped(stack);

        if (isHeld) {
            GunTimers.open(stack);
            if (player != null && !wasHeld) this.onEquip(player, stack);
        } else {
            GunTimers.close(stack);
        }

        if (wasHeld != isHeld) setIsEquipped(stack, isHeld);

        if (!isHeld) {
            for (int i = 0; i < confNo; i++) {
                GunState current = getState(stack, i);
                if (current != GunState.JAMMED) {
                    if (current != GunState.DRAWING) setState(stack, i, GunState.DRAWING);
                    int draw = (single != null ? single : configs[i]).getDrawDuration(stack);
                    if (getTimer(stack, i) != draw) setTimer(stack, i, draw);
                }
                if (getLastAnim(stack, i) != GunAnimation.CYCLE)
                    setLastAnim(stack, i, GunAnimation.CYCLE);
            }
            if (getIsAiming(stack)) setIsAiming(stack, false);
            if (getReloadCancel(stack)) setReloadCancel(stack, false);
            return;
        }

        for (int i = 0; i < confNo; i++) {
            GunConfig config = single != null ? single : configs[i];
            LambdaContext ctx =
                    new LambdaContext(
                            config,
                            (LivingEntity) entity,
                            player != null ? player.getInventory() : null,
                            i);
            for (int k = 0;
                    k == 0
                            || (k < 2
                                    && player != null
                                    && ArmorSuitEffects.hasFullSet(
                                            player, ModArmorItem.Suit.TRENCHMASTER)
                                    && getState(stack, i) == GunState.RELOADING);
                    k++) {
                BiConsumer<ItemStack, LambdaContext> orchestra = config.getOrchestra(stack);
                if (orchestra != null) orchestra.accept(stack, ctx);

                setAnimTimer(stack, i, getAnimTimer(stack, i) + 1);

                int timer = getTimer(stack, i);
                if (timer > 0) setTimer(stack, i, timer - 1);
                if (timer <= 1) config.getDecider(stack).accept(stack, ctx);
            }
        }
    }

    public enum WeaponQuality {
        A_SIDE,
        B_SIDE,
        LEGENDARY,
        SPECIAL,
        UTILITY,
        SECRET,
        DEBUG
    }

    public enum GunState {
        DRAWING,
        IDLE,
        COOLDOWN,
        RELOADING,
        JAMMED;

        public static final GunState[] VALUES = values();
    }

    public record LambdaContext(
            GunConfig config, LivingEntity entity, Container inventory, int configIndex) {

        public Player getPlayer() {
            if (!(entity instanceof Player)) return null;
            return (Player) entity;
        }
    }
}
