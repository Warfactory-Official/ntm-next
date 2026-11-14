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

import com.hbm.NuclearTech;
import com.hbm.config.HbmConfig;
import com.hbm.config.RuntimeConfig;
import com.hbm.data.ItemData;
import com.hbm.interfaces.DroppedItemUpdateHandler;
import com.hbm.items.ModDataComponents;
import com.hbm.items.ModItems;
import com.hbm.sound.ModSounds;
import com.hbm.sound.WorldSounds;
import com.hbm.util.KeyHints;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

public class DeadManDetonatorItem extends Item implements DroppedItemUpdateHandler {
    private static final float SWITCH_BLAST = 0.0F;
    private static final float EXPLOSIVE_BLAST = 15.0F;

    private final boolean linked;

    public DeadManDetonatorItem(Properties properties, boolean linked) {
        super(properties);
        this.linked = linked;
    }

    @Override
    public boolean updateDroppedItem(ItemEntity entity) {
        if (!(entity.level() instanceof ServerLevel level)) return false;

        if (linked) {
            Long packed = entity.getItem().get(ModDataComponents.DETONATOR_POS.get());
            if (packed != null) {
                var target = Detonation.find(level, BlockPos.of(packed));
                if (target != null) Detonation.fire(level, target, entity, "dead man's switch");
            }
            level.explode(
                    entity,
                    entity.getX(),
                    entity.getY(),
                    entity.getZ(),
                    SWITCH_BLAST,
                    Level.ExplosionInteraction.BLOCK);
            entity.discard();
            return true;
        }

        if (ItemData.DROP_DEAD_MAN.get()) {
            level.explode(
                    entity,
                    entity.getX(),
                    entity.getY(),
                    entity.getZ(),
                    EXPLOSIVE_BLAST,
                    Level.ExplosionInteraction.BLOCK);
            if (((RuntimeConfig) HbmConfig.RUNTIME).extendedLogging()) {
                NuclearTech.LOGGER.info(
                        "[DET] Detonated dead man's explosive at {} / {} / {}!",
                        (int) entity.getX(),
                        (int) entity.getY(),
                        (int) entity.getZ());
            }
        }
        entity.discard();
        return true;
    }

    public static void onOwnerDeath(ServerPlayer player) {
        ServerLevel level = player.level();
        Inventory inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            Long packed =
                    stack.is(ModItems.DETONATOR_DEADMAN.get())
                            ? stack.get(ModDataComponents.DETONATOR_POS.get())
                            : null;
            if (packed == null) continue;
            var target = Detonation.find(level, BlockPos.of(packed));
            if (target != null) {
                Detonation.fire(
                        level,
                        target,
                        player,
                        "dead man's switch from " + player.getDisplayName().getString());
            }
            inventory.setItem(i, ItemStack.EMPTY);
        }
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (!linked || player == null || !player.isShiftKeyDown()) return InteractionResult.PASS;

        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        context.getItemInHand().set(ModDataComponents.DETONATOR_POS.get(), pos.asLong());
        if (!level.isClientSide()) {
            player.sendSystemMessage(Component.translatable("chat.deadman.positionSet"));
            WorldSounds.play(
                    level, null, pos, ModSounds.TECH_BOOP.get(), SoundSource.PLAYERS, 2.0F, 1.0F);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> adder,
            TooltipFlag flag) {
        if (linked) {
            adder.accept(
                    Component.translatable(
                            "tooltip.hbm.detonator.set_position",
                            KeyHints.key("key.sneak", "key.keyboard.left.shift"),
                            KeyHints.key("key.use", "key.mouse.right")));
            adder.accept(
                    Component.translatable(
                            "tooltip.hbm.deadman.drop",
                            KeyHints.key("key.drop", "key.keyboard.q")));
            Long packed = stack.get(ModDataComponents.DETONATOR_POS.get());
            if (packed == null) {
                adder.accept(Component.translatable("desc.item.deadman.noPositionSet"));
            } else {
                BlockPos pos = BlockPos.of(packed);
                adder.accept(
                        Component.translatable(
                                "desc.item.deadman.setPosTo", pos.getX(), pos.getY(), pos.getZ()));
            }
        } else {
            adder.accept(Component.translatable("desc.item.deadman.explodesWhenDropped"));
        }
        adder.accept(
                Component.literal("[")
                        .append(Component.translatable("trait.drop"))
                        .append("]")
                        .withStyle(ChatFormatting.RED));
    }
}
