// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client;

import com.hbm.entity.effect.EntityNukeTorex;
import com.hbm.sound.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public final class TorexClientFX {

    private TorexClientFX() {}

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
