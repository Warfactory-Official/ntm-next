/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-FileCopyrightText: HbmMods (The Bobcat)
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 * Copyright (C) HbmMods (The Bobcat). All rights reserved.
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

package com.hbm.items.tool;

import com.hbm.interfaces.TriggerResult;
import com.hbm.packet.ClientEventConstants;
import com.hbm.packet.PacketWire;
import com.hbm.packet.layout.PlayerInformLayout;
import com.hbm.sound.EntitySounds;
import com.hbm.sound.ModSounds;
import java.util.function.Consumer;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class LaserDetonatorItem extends Item {

    private static final double REACH = 500.0D;
    private static final double TRAIL = 15.0D;

    public LaserDetonatorItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        BlockHitResult hit = (BlockHitResult) player.pick(REACH, 1.0F, false);
        if (level.isClientSide())
            trail(level, player.getEyePosition(), Vec3.atCenterOf(hit.getBlockPos()));

        var target =
                hit.getType() == HitResult.Type.BLOCK
                        ? Detonation.find(level, hit.getBlockPos())
                        : null;
        if (player instanceof ServerPlayer server) {
            if (target == null) {
                inform(
                        server,
                        ModSounds.TECH_BOOP.get(),
                        Detonation.report(TriggerResult.NO_DEVICE));
            } else {
                TriggerResult code =
                        Detonation.fire(level, target, player, player.getDisplayName().getString());
                inform(server, ModSounds.TECH_BLEEP.get(), Detonation.report(code));
            }
        }
        return target == null ? InteractionResult.FAIL : InteractionResult.SUCCESS;
    }

    private static void trail(Level level, Vec3 eye, Vec3 aimed) {
        Vec3 unit = aimed.subtract(eye).normalize();
        double shown = Math.min(aimed.distanceTo(eye), TRAIL);
        for (int i = 0; i < shown; i++) {
            Vec3 dust = eye.add(unit.scale(level.getRandom().nextDouble() * shown + 3));
            level.addParticle(DustParticleOptions.REDSTONE, dust.x, dust.y, dust.z, 0, 0, 0);
        }
    }

    private static void inform(ServerPlayer player, SoundEvent sound, Component message) {
        EntitySounds.play(
                player,
                player.getX(),
                player.getY(),
                player.getZ(),
                sound,
                SoundSource.PLAYERS,
                1.0F,
                1.0F);
        PacketWire.sendTo(
                PlayerInformLayout.encode(
                        message,
                        ClientEventConstants.PLAYER_INFORM_ID_DETONATOR,
                        ClientEventConstants.PLAYER_INFORM_DEFAULT_MILLIS),
                player);
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            Item.TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> adder,
            TooltipFlag flag) {
        adder.accept(Component.translatable("item.hbm.detonator_laser.desc"));
    }
}
