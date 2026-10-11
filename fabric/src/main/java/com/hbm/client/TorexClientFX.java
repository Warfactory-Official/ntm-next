// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client;

import com.hbm.entity.effect.EntityNukeTorex;
import com.hbm.sound.ModSounds;
import dev.engine_room.flywheel.api.lod.LodSurface;
import dev.engine_room.flywheel.api.lod.LodTerrain;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;

public final class TorexClientFX {
    private static final int LOD_RADIUS = 228;

    private TorexClientFX() {}

    public static int lodSurface(EntityNukeTorex torex, int x, int z) {
        if (torex.lodSurface == null) {
            torex.lodSurface =
                    LodTerrain.surface(
                            Heightmap.Types.MOTION_BLOCKING,
                            Mth.floor(torex.getX()) - LOD_RADIUS,
                            Mth.floor(torex.getZ()) - LOD_RADIUS,
                            2 * LOD_RADIUS + 1);
        }
        LodSurface surface = (LodSurface) torex.lodSurface.getNow(null);
        int top = surface == null ? LodSurface.UNKNOWN : surface.top(x, z);
        return top != LodSurface.UNKNOWN ? top : (int) torex.getY();
    }

    public static void skyFlash(Level level) {
        if (level instanceof ClientLevel client) {
            client.setSkyFlashTime(2);
        }
    }

    public static void hudFlash(EntityNukeTorex torex) {
        if (torex.age() < 10) NukeHud.armFlash();
    }

    public static void shockwaveArrival(EntityNukeTorex torex) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;
        if (player.getEyePosition().distanceTo(torex.position()) >= (torex.age() * 1.5 + 1) * 1.5)
            return;

        if (!torex.didPlaySound) {
            torex.level()
                    .playLocalSound(
                            torex.getX(),
                            torex.getY(),
                            torex.getZ(),
                            ModSounds.NUCLEAR_EXPLOSION.get(),
                            SoundSource.HOSTILE,
                            10_000F,
                            1F,
                            false);
            torex.didPlaySound = true;
        }
        if (!torex.didShake && NukeHud.armShake()) {
            torex.didShake = true;
            player.animateHurt(0F);
            player.hurtTime = 15;
            player.hurtDuration = 15;
        }
    }
}
