// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.armor;

import com.hbm.extprop.HbmPlayerProps;
import com.hbm.interfaces.injected.PlayerAppearance;
import com.hbm.items.armor.ArmorSuitEffects.Jetpack;
import net.minecraft.world.entity.player.Player;
import org.jspecify.annotations.Nullable;

public final class ArmorFlight {

    public enum Mode {
        NONE(false),
        LIFT(false),
        HOVER(false),
        DIRECTIONAL(true),
        GLIDE(true),
        DESCEND(false),
        PARACHUTE(false);

        public static final Mode[] VALUES = values();
        public final boolean prone;

        Mode(boolean prone) {
            this.prone = prone;
        }
    }

    private ArmorFlight() {}

    public static Mode mode(Player player, @Nullable Jetpack pack, ItemWings.@Nullable Kind wings) {
        return mode(
                player,
                pack,
                wings,
                player.getDeltaMovement().y,
                player.getDeltaMovement().horizontalDistanceSqr() > 0D
                        || player.zza != 0F
                        || player.xxa != 0F);
    }

    private static Mode mode(
            Player player,
            @Nullable Jetpack pack,
            ItemWings.@Nullable Kind wings,
            double dy,
            boolean moving) {
        HbmPlayerProps props = HbmPlayerProps.getData(player);
        boolean active = props.isJetpackActive();
        boolean sneak = player.isShiftKeyDown();
        Mode result = Mode.NONE;
        if (pack != null && active && !(pack == Jetpack.BRAKE && sneak)) {
            result = pack.lookThrust > 0D ? Mode.DIRECTIONAL : Mode.LIFT;
        } else if (!player.isFallFlying() || active) {
            if (!player.onGround()) {
                if (pack == Jetpack.BRAKE && props.enableBackpack && (!sneak || active))
                    result = Mode.HOVER;
                if (pack == Jetpack.DNS) {
                    if (!sneak && props.enableBackpack) result = moving ? Mode.GLIDE : Mode.HOVER;
                    else if (sneak) result = Mode.DESCEND;
                }
                if (pack == Jetpack.BJ && sneak && dy < -0.08D) result = Mode.GLIDE;
            }
        }
        if (wings == null
                || player.onGround()
                || player.isFallFlying() && (wings == ItemWings.Kind.LIMP || !active))
            return result;
        Mode wingMode;
        if (wings == ItemWings.Kind.LIMP)
            wingMode = sneak && dy < -0.08D ? Mode.GLIDE : Mode.PARACHUTE;
        else if (!props.enableBackpack) wingMode = Mode.NONE;
        else if (active && sneak) wingMode = Mode.HOVER;
        else if (moving) wingMode = Mode.GLIDE;
        else wingMode = active ? Mode.LIFT : Mode.HOVER;
        return wingMode.prone || result == Mode.NONE ? wingMode : result;
    }

    public static boolean prone(Player player) {
        if (!player.isAlive()
                || player.onGround()
                || player.isPassenger()
                || player.isSleeping()
                || player.isInWater()
                || player.isInLava()
                || player.isSwimming()
                || player.isAutoSpinAttack()
                || player.getAbilities().flying
                || player.isSpectator()) return false;
        int intent = HbmPlayerProps.getData(player).armorFlightIntent;
        if (intent < 0 || intent >= Mode.VALUES.length || !Mode.VALUES[intent].prone) return false;
        return Mode.VALUES[intent]
                == mode(
                        player,
                        ArmorSuitEffects.jetpackKind(player),
                        ArmorSuitEffects.wingsKind(player),
                        Double.NEGATIVE_INFINITY,
                        true);
    }

    public static boolean renderProne(Player player) {
        return player.isLocalPlayer()
                ? prone(player)
                : (player.hbm$appearance() & PlayerAppearance.ARMOR_FLIGHT) != 0;
    }
}
