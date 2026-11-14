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

package com.hbm.items.weapon.sedna.factory;

import com.hbm.blocks.ModBlocks;
import com.hbm.client.HeldItemAnimations;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.items.ModItems;
import com.hbm.items.weapon.sedna.Crosshair;
import com.hbm.items.weapon.sedna.GunBaseNTItem.LambdaContext;
import com.hbm.items.weapon.sedna.GunBaseNTItem.WeaponQuality;
import com.hbm.items.weapon.sedna.GunBaseNTItem;
import com.hbm.items.weapon.sedna.GunConfig;
import com.hbm.items.weapon.sedna.Receiver;
import com.hbm.items.weapon.sedna.factory.GunAnimationActions;
import com.hbm.items.weapon.sedna.impl.GunDrillItem;
import com.hbm.items.weapon.sedna.mags.Magazine;
import com.hbm.items.weapon.sedna.mags.MagazineLiquidEngine;
import com.hbm.items.weapon.sedna.mods.GunAttribute;
import com.hbm.items.weapon.sedna.mods.InstalledWeaponMods;
import com.hbm.items.weapon.sedna.mods.XWeaponModManager;
import com.hbm.lib.ModifiedDamageSource;
import com.hbm.packet.PacketWire;
import com.hbm.packet.layout.BlockDestroyLayout;
import com.hbm.platform.Platform;
import com.hbm.registration.Registrar;
import com.hbm.render.anim.AnimationEnums.GunAnimation;
import com.hbm.render.anim.AnimationSequence;
import com.hbm.render.anim.BusAnimation;
import com.hbm.render.anim.BusAnimationKeyframe.IType;
import com.hbm.sound.EntitySounds;
import com.hbm.util.EntityTargeting;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundLevelEventPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.Shapes;

public class XFactoryDrill {

    public static final GunAttribute.OfDouble REACH = new GunAttribute.OfDouble();
    public static final GunAttribute.OfFloat DT_NEGATION = new GunAttribute.OfFloat();
    public static final GunAttribute.OfFloat PIERCE = new GunAttribute.OfFloat();
    public static final GunAttribute.OfInt AOE = new GunAttribute.OfInt();
    public static final GunAttribute.OfInt HARVEST = new GunAttribute.OfInt();
    public static BiConsumer<ItemStack, LambdaContext> LAMBDA_DRILL_FIRE =
            (stack, ctx) -> {
                doStandardFire(stack, ctx, GunAnimation.CYCLE, true);
            };
    public static boolean didPlink = false;
    public static BiFunction<ItemStack, GunAnimation, BusAnimation> LAMBDA_DRILL_ANIMS =
            (stack, type) -> {
                switch (type) {
                    case EQUIP:
                        return new BusAnimation()
                                .addBus(
                                        "EQUIP",
                                        new AnimationSequence()
                                                .jump(-1, 0, 0)
                                                .to(0, 0, 0, 750, IType.SIN_DOWN));
                    case CYCLE:
                        double deploy = HeldItemAnimations.getChannelValue("DEPLOY", 0);
                        double speed = HeldItemAnimations.getChannelValue("SPEED", 0);
                        double spin = HeldItemAnimations.getChannelValue("SPIN", 0) % 360;
                        return new BusAnimation()
                                .addBus(
                                        "DEPLOY",
                                        new AnimationSequence()
                                                .jump(deploy, 0, 0)
                                                .to(
                                                        1,
                                                        0,
                                                        0,
                                                        (int) (500 * (1 - deploy)),
                                                        IType.SIN_FULL)
                                                .stay(1000)
                                                .to(0, 0, 0, 500, IType.SIN_FULL))
                                .addBus(
                                        "SPIN",
                                        new AnimationSequence()
                                                .jump(spin, 0, 0)
                                                .to(spin + 360 * 1.5, 0, 0, 1500)
                                                .to(
                                                        360 * 3,
                                                        0,
                                                        0,
                                                        750 + (int) (1000 * (1D - spin / 360D)),
                                                        IType.SIN_DOWN))
                                .addBus(
                                        "SPEED",
                                        new AnimationSequence()
                                                .jump(speed, 0, 0)
                                                .to(1, 0, 0, 500)
                                                .stay(1000)
                                                .to(
                                                        0,
                                                        0,
                                                        0,
                                                        750 + (int) (1000 * (1D - spin / 360D)),
                                                        IType.SIN_DOWN));
                    case CYCLE_DRY:
                        return new BusAnimation()
                                .addBus(
                                        "DEPLOY",
                                        new AnimationSequence()
                                                .to(0.25, 0, 0, 250, IType.SIN_FULL)
                                                .to(0, 0, 0, 250, IType.SIN_FULL))
                                .addBus(
                                        "SPIN",
                                        new AnimationSequence().to(360, 0, 0, 1500, IType.SIN_DOWN))
                                .addBus(
                                        "SPEED",
                                        new AnimationSequence()
                                                .to(0.75, 0, 0, 250)
                                                .to(0, 0, 0, 1000, IType.SIN_DOWN));
                    case INSPECT:
                        return new BusAnimation()
                                .addBus(
                                        "LIFT",
                                        new AnimationSequence()
                                                .to(-45, 0, 0, 500, IType.SIN_FULL)
                                                .stay(1000)
                                                .to(0, 0, 0, 500, IType.SIN_DOWN));
                    default:
                        return null;
                }
            };

    public static void init() {

        ModItems.GUN_DRILL =
                Registrar.registerItem(
                        "gun_drill",
                        props ->
                                new GunDrillItem(
                                        WeaponQuality.UTILITY,
                                        props,
                                        GunConfig.builder()
                                                .dura(3_000)
                                                .draw(10)
                                                .inspect(55)
                                                .hideCrosshair(false)
                                                .crosshair(Crosshair.L_CIRCUMFLEX)
                                                .rec(
                                                        Receiver.builder()
                                                                .dmg(10F)
                                                                .delay(20)
                                                                .dry(30)
                                                                .auto(true)
                                                                .jam(0)
                                                                .mag(
                                                                        new MagazineLiquidEngine(
                                                                                0,
                                                                                4_000,
                                                                                () ->
                                                                                        new Fluid
                                                                                                [] {
                                                                                            NTMFluids
                                                                                                    .GASOLINE,
                                                                                            NTMFluids
                                                                                                    .GASOLINE_LEADED,
                                                                                            NTMFluids
                                                                                                    .COALGAS,
                                                                                            NTMFluids
                                                                                                    .COALGAS_LEADED
                                                                                        }))
                                                                .offset(1, -0.0625 * 2.5, -0.25D)
                                                                .canFire(
                                                                        Lego
                                                                                .LAMBDA_STANDARD_CAN_FIRE)
                                                                .fire(LAMBDA_DRILL_FIRE))
                                                .pp(Lego.LAMBDA_STANDARD_CLICK_PRIMARY)
                                                .pr(Lego.LAMBDA_STANDARD_RELOAD)
                                                .decider(GunStateDecider.LAMBDA_STANDARD_DECIDER)
                                                .anim(LAMBDA_DRILL_ANIMS)
                                                .orchestra(GunAnimationActions.DRILL)),
                        Item.Properties::new);
    }

    public static void doStandardFire(
            ItemStack stack, LambdaContext ctx, GunAnimation anim, boolean calcWear) {
        Player player = ctx.getPlayer();
        int index = ctx.configIndex();
        if (anim != null) GunBaseNTItem.playAnimation(player, stack, anim, ctx.configIndex());

        Receiver primary = ctx.config().getReceivers(stack).get(0);
        Magazine mag = primary.getMagazine(stack);

        HitResult mop =
                EntityTargeting.getMouseOver(ctx.getPlayer(), getModdableReach(stack, 5.0D));
        if (mop != null) {
            if (mop instanceof EntityHitResult entityHit) {
                float damage = primary.getBaseDamage(stack);
                if (entityHit.getEntity() instanceof LivingEntity living) {
                    if (living.level() instanceof ServerLevel server) {
                        living.hurtServer(
                                server,
                                ModifiedDamageSource.of(
                                        ctx.getPlayer()
                                                .damageSources()
                                                .playerAttack(ctx.getPlayer()),
                                        true,
                                        0.1F,
                                        getModdableDTNegation(stack, 2F),
                                        getModdablePiercing(stack, 0.15F)),
                                damage);
                    }
                } else if (entityHit.getEntity().level() instanceof ServerLevel server) {
                    entityHit
                            .getEntity()
                            .hurtServer(
                                    server,
                                    ctx.getPlayer().damageSources().playerAttack(ctx.getPlayer()),
                                    damage);
                }
            }
            if (player != null && mop instanceof BlockHitResult blockHit) {

                BlockPos hitPos = blockHit.getBlockPos();
                int aoe = player.isShiftKeyDown() ? 0 : getModdableAoE(stack, 1);
                breakExtraBlock(
                        player.level(),
                        hitPos.getX(),
                        hitPos.getY(),
                        hitPos.getZ(),
                        player,
                        hitPos.getX(),
                        hitPos.getY(),
                        hitPos.getZ());
                for (int i = -aoe; i <= aoe; i++)
                    for (int j = -aoe; j <= aoe; j++)
                        for (int k = -aoe; k <= aoe; k++) {
                            if (i == 0 && j == 0 && k == 0) continue;
                            breakExtraBlock(
                                    player.level(),
                                    hitPos.getX() + i,
                                    hitPos.getY() + j,
                                    hitPos.getZ() + k,
                                    player,
                                    hitPos.getX(),
                                    hitPos.getY(),
                                    hitPos.getZ());
                        }

                didPlink = false;
            }
        }

        int ammoToUse = 10;
        if (XWeaponModManager.hasUpgrade(stack, 0, XWeaponModManager.ID_ENGINE_ELECTRIC))
            ammoToUse = 1_000;
        mag.consumeAmmo(stack, ctx.inventory(), ammoToUse);
        if (calcWear)
            GunBaseNTItem.setWear(
                    stack,
                    index,
                    Math.min(
                            GunBaseNTItem.getWear(stack, index),
                            ctx.config().getDurability(stack)));
    }

    public static void breakExtraBlock(
            Level world, int x, int y, int z, Player playerEntity, int refX, int refY, int refZ) {
        BlockPos pos = new BlockPos(x, y, z);
        if (world.isEmptyBlock(pos)) return;
        if (!(playerEntity instanceof ServerPlayer player)) return;

        BlockState state = world.getBlockState(pos);

        if (!Platform.canHarvestBlock(world, pos, state, player)
                || (state.getDestroySpeed(world, pos) == -1.0F
                        && state.getDestroyProgress(player, world, pos) == 0.0F)
                || state.is(ModBlocks.STONE_KEYHOLE.get())) {
            if (!didPlink) {
                EntitySounds.play(
                        player,
                        player.getX(),
                        player.getY(),
                        player.getZ(),
                        SoundEvents.ITEM_BREAK.value(),
                        SoundSource.PLAYERS,
                        0.5F,
                        0.8F + world.getRandom().nextFloat() * 0.6F);
                didPlink = true;
            }
            return;
        }

        if (player.gameMode.destroyBlock(pos)) {
            ServerLevel server = (ServerLevel) world;
            server.levelEvent(player, 2001, pos, Block.getId(state));
            if (x == refX && y == refY && z == refZ) {
                player.connection.send(
                        new ClientboundLevelEventPacket(2001, pos, Block.getId(state), false));
            } else {
                PacketWire.sendTo(
                        BlockDestroyLayout.encode(
                                pos.getX(),
                                pos.getY(),
                                pos.getZ(),
                                Block.BLOCK_STATE_REGISTRY.getId(state)),
                        player);
            }
        }
    }

    public static double getModdableReach(ItemStack stack, double base) {
        return REACH.resolve(base, InstalledWeaponMods.of(stack, 0));
    }

    public static float getModdableDTNegation(ItemStack stack, float base) {
        return DT_NEGATION.resolve(base, InstalledWeaponMods.of(stack, 0));
    }

    public static float getModdablePiercing(ItemStack stack, float base) {
        return PIERCE.resolve(base, InstalledWeaponMods.of(stack, 0));
    }

    public static int getModdableAoE(ItemStack stack, int base) {
        return AOE.resolve(base, InstalledWeaponMods.of(stack, 0));
    }

    public static int getModdableHarvestLevel(ItemStack stack, int base) {
        return HARVEST.resolve(base, InstalledWeaponMods.of(stack, 0));
    }

    public static void submitBlockHighlight(
            PoseStack poseStack,
            SubmitNodeCollector collector,
            Vec3 cameraPos,
            Player player,
            ItemStack drill) {
        HitResult mop = EntityTargeting.getMouseOver(player, getModdableReach(drill, 5.0D));

        if (mop instanceof BlockHitResult blockHit && mop.getType() == HitResult.Type.BLOCK) {
            BlockPos pos = blockHit.getBlockPos();
            int aoe = player.isShiftKeyDown() ? 0 : getModdableAoE(drill, 1);

            float exp = 0.002F;
            poseStack.pushPose();
            poseStack.translate(
                    pos.getX() - cameraPos.x, pos.getY() - cameraPos.y, pos.getZ() - cameraPos.z);

            collector.submitShapeOutline(
                    poseStack,
                    Shapes.create(new AABB(0, 0, 0, 1, 1, 1).inflate(exp)),
                    RenderTypes.lines(),
                    aoe > 0 ? ARGB.black(102) : 0xFF800000,
                    2.0F,
                    false);

            if (aoe > 0) {
                collector.submitShapeOutline(
                        poseStack,
                        Shapes.create(
                                new AABB(-aoe, -aoe, -aoe, 1 + aoe, 1 + aoe, 1 + aoe).inflate(exp)),
                        RenderTypes.lines(),
                        0xFF800000,
                        2.0F,
                        false);
            }

            poseStack.popPose();
        }
    }
}
