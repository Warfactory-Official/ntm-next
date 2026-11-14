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

package com.hbm.items.weapon.grenade;

import com.hbm.client.ClientPlayerAccess;
import com.hbm.entity.grenade.GrenadeUniversalEntity;
import com.hbm.extprop.PlayerState;
import com.hbm.itempool.ItemPool;
import com.hbm.items.ModDataComponents;
import com.hbm.items.ModItems;
import com.hbm.items.weapon.grenade.GrenadeShellItem;
import com.hbm.items.weapon.sedna.GunBaseNTItem;
import com.hbm.packet.PacketWire;
import com.hbm.packet.layout.GrenadeAnimationLayout;
import com.hbm.sound.EntitySounds;
import com.hbm.sound.ModSounds;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

public class GrenadeUniversalItem extends Item {
    private static int clientSlot = -1;
    private static @Nullable GrenadeData clientData;

    public GrenadeUniversalItem(Properties properties) {
        super(properties);
    }

    private static int deployment(Player player) {
        return PlayerState.getOrCreate(player).grenadeDraw;
    }

    private static void onEquip(ServerPlayer player, ItemStack stack) {
        PlayerState.getOrCreate(player).grenadeDraw = 0;
        PacketWire.sendTo(GrenadeAnimationLayout.encode(getData(stack).shell()), player);
    }

    public static void clientHeldTick() {
        Player player = ClientPlayerAccess.player();
        if (player == null) return;
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof GrenadeUniversalItem)) {
            clientSlot = -1;
            return;
        }
        int slot = player.getInventory().getSelectedSlot();
        GrenadeData data = getData(stack);
        PlayerState props = PlayerState.getOrCreate(player);
        if (slot != clientSlot || !data.equals(clientData)) {
            clientSlot = slot;
            clientData = data;
            props.grenadeDraw = 0;
        } else {
            props.grenadeDraw++;
        }
    }

    public static GrenadeData getData(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.GRENADE.get(), GrenadeData.DEFAULT);
    }

    public static ItemStack make(
            GrenadeShellItem.EnumGrenadeShell shell,
            GrenadeFillingDefinition filling,
            GrenadeFuzeItem.EnumGrenadeFuze fuze) {
        return make(shell, filling, fuze, null, 1);
    }

    public static ItemStack make(
            GrenadeShellItem.EnumGrenadeShell shell,
            GrenadeFillingDefinition filling,
            GrenadeFuzeItem.EnumGrenadeFuze fuze,
            GrenadeExtraItem.@Nullable EnumGrenadeExtra extra) {
        return make(shell, filling, fuze, extra, 1);
    }

    public static ItemStack make(
            GrenadeShellItem.EnumGrenadeShell shell,
            GrenadeFillingDefinition filling,
            GrenadeFuzeItem.EnumGrenadeFuze fuze,
            GrenadeExtraItem.@Nullable EnumGrenadeExtra extra,
            int amount) {
        ItemStack stack = new ItemStack(ModItems.GRENADE_UNIVERSAL.get(), amount);
        stack.set(ModDataComponents.GRENADE.get(), new GrenadeData(shell, filling, fuze, extra));
        stack.set(DataComponents.MAX_STACK_SIZE, shell.getStackLimit());
        return stack;
    }

    public static ItemPool.Entry poolEntry(
            GrenadeShellItem.EnumGrenadeShell shell,
            GrenadeFillingDefinition filling,
            GrenadeFuzeItem.EnumGrenadeFuze fuze,
            GrenadeExtraItem.@Nullable EnumGrenadeExtra extra,
            int min,
            int max,
            int weight) {
        return new ItemPool.Entry(
                ModItems.GRENADE_UNIVERSAL.get().builtInRegistryHolder(),
                DataComponentPatch.builder()
                        .set(
                                ModDataComponents.GRENADE.get(),
                                new GrenadeData(shell, filling, fuze, extra))
                        .set(DataComponents.MAX_STACK_SIZE, shell.getStackLimit())
                        .build(),
                Math.min(min, max),
                Math.max(min, max),
                weight);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (deployment(player) < getData(stack).shell().getDrawDuration())
            return InteractionResult.PASS;
        if (!level.isClientSide())
            level.addFreshEntity(new GrenadeUniversalEntity(level, player, stack));
        stack.consume(1, player);
        if (!stack.isEmpty()) {
            if (player instanceof ServerPlayer server) onEquip(server, stack);
            else PlayerState.getOrCreate(player).grenadeDraw = 0;
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public void inventoryTick(
            ItemStack stack, ServerLevel level, Entity owner, @Nullable EquipmentSlot slot) {
        boolean isHeld = slot == EquipmentSlot.MAINHAND;
        boolean wasHeld = GunBaseNTItem.getIsEquipped(stack);
        if (wasHeld != isHeld) GunBaseNTItem.setIsEquipped(stack, isHeld);
        if (!(owner instanceof ServerPlayer player) || !isHeld) return;
        if (!wasHeld) {
            onEquip(player, stack);
            return;
        }
        int deployed = PlayerState.getOrCreate(player).grenadeDraw + 1;
        PlayerState.getOrCreate(player).grenadeDraw = deployed;
        switch (getData(stack).shell()) {
            case FRAG -> {
                if (deployed == 18)
                    EntitySounds.play(
                            player,
                            player.getX(),
                            player.getY(),
                            player.getZ(),
                            ModSounds.GUN_REVOLVER_COCK.get(),
                            SoundSource.PLAYERS,
                            1F,
                            1F);
            }
            case STICK -> {
                if (deployed == 16 || deployed == 25)
                    EntitySounds.play(
                            player,
                            player.getX(),
                            player.getY(),
                            player.getZ(),
                            ModSounds.GUN_BOLT_OPEN.get(),
                            SoundSource.PLAYERS,
                            1F,
                            1.25F);
            }
            case TECH -> {
                if (deployed == 18)
                    EntitySounds.play(
                            player,
                            player.getX(),
                            player.getY(),
                            player.getZ(),
                            ModSounds.GRENADE_TECH.get(),
                            SoundSource.PLAYERS,
                            1F,
                            1F);
            }
            case NUKE -> {
                if (deployed == 26)
                    EntitySounds.play(
                            player,
                            player.getX(),
                            player.getY(),
                            player.getZ(),
                            ModSounds.GRENADE_NUKA.get(),
                            SoundSource.PLAYERS,
                            1F,
                            1F);
            }
        }
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> adder,
            TooltipFlag flag) {
        GrenadeData data = getData(stack);
        adder.accept(
                Component.translatable(ModItems.GRENADE_SHELL.get(data.shell()).getDescriptionId())
                        .withStyle(ChatFormatting.YELLOW));
        adder.accept(
                Component.translatable(data.filling().item().getDescriptionId())
                        .withStyle(ChatFormatting.YELLOW));
        adder.accept(
                Component.translatable(ModItems.GRENADE_FUZE.get(data.fuze()).getDescriptionId())
                        .withStyle(ChatFormatting.YELLOW));
        if (data.extra() != null) {
            adder.accept(
                    Component.translatable(
                                    ModItems.GRENADE_EXTRA.get(data.extra()).getDescriptionId())
                            .withStyle(ChatFormatting.RED));
        }
    }
}
