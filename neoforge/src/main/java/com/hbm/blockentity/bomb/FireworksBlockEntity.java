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

package com.hbm.blockentity.bomb;

import com.hbm.blockentity.BlockEntityTicks;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.entity.item.FireworksEntity;
import com.hbm.sound.ModSounds;
import com.hbm.sound.WorldSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public final class FireworksBlockEntity extends BlockEntity implements BlockEntityTicks {

    private static final int CHARACTER_INTERVAL = 30;
    private static final int WORD_INTERVAL = 100;
    private static final double FLAME_RANGE_SQUARED = 100D * 100D;

    public int color = 0xff0000;
    public String message = "NUCLEAR TECH";
    public int charges;

    private int nextCharacter;
    private int ticksUntilLaunch;

    public FireworksBlockEntity(BlockPos position, BlockState state) {
        super(ModBlockEntities.FIREWORK_BOX.get(), position, state);
    }

    @Override
    public void tickServer() {
        if (level instanceof ServerLevel serverLevel) {
            this.advanceSequence(serverLevel, worldPosition);
        }
    }

    public void resetSequence() {
        nextCharacter = 0;
        ticksUntilLaunch = 0;
    }

    public void replaceMessage(String replacement) {
        if (replacement.equals(message)) return;
        message = replacement;
        resetSequence();
        setChanged();
    }

    private void advanceSequence(ServerLevel level, BlockPos position) {
        if (charges <= 0 || message.isEmpty()) {
            resetSequence();
            return;
        }
        if (ticksUntilLaunch > 0) {
            ticksUntilLaunch--;
            if (ticksUntilLaunch > 0) return;
        }

        int muzzle = nextCharacter % 9;
        double x = position.getX() + 0.1875D + muzzle / 3 * 0.3125D;
        double z = position.getZ() + 0.1875D + muzzle % 3 * 0.3125D;
        var shot =
                new FireworksEntity(
                        level, x, position.getY() + 1.5D, z, color, message.charAt(nextCharacter));
        level.addFreshEntity(shot);
        WorldSounds.machine(
                level,
                null,
                shot.getX(),
                shot.getY(),
                shot.getZ(),
                ModSounds.WEAPON_ROCKET_FLAME.get(),
                SoundSource.BLOCKS,
                3F,
                1F);
        charges--;
        setChanged();
        emitMuzzleFlame(level, x, position.getY() + 1.125D, z);

        nextCharacter++;
        if (nextCharacter == message.length()) {
            nextCharacter = 0;
            ticksUntilLaunch = WORD_INTERVAL;
        } else {
            ticksUntilLaunch = CHARACTER_INTERVAL;
        }
    }

    private static void emitMuzzleFlame(ServerLevel level, double x, double y, double z) {
        var packet =
                new ClientboundLevelParticlesPacket(
                        ParticleTypes.FLAME, true, true, x, y, z, 0F, 0F, 0F, 0F, 1);
        var players = level.players();
        for (int index = 0; index < players.size(); index++) {
            var player = players.get(index);
            if (player.distanceToSqr(x, y, z) <= FLAME_RANGE_SQUARED) {
                player.connection.send(packet);
            }
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        charges = input.getIntOr("charges", 0);
        color = input.getIntOr("color", color);
        message = input.getStringOr("message", message);
        resetSequence();
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("charges", charges);
        output.putInt("color", color);
        output.putString("message", message);
    }
}
