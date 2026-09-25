// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: Contributors to Hbm's Nuclear Tech Mod
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.weapon.sedna.factory;

import com.hbm.blocks.ModBlocks;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.items.ModItems;
import com.hbm.items.weapon.sedna.Crosshair;
import com.hbm.items.weapon.sedna.GunConfig;
import com.hbm.items.weapon.sedna.ItemGunBaseNT.LambdaContext;
import com.hbm.items.weapon.sedna.ItemGunBaseNT.WeaponQuality;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.items.weapon.sedna.Receiver;
import com.hbm.items.weapon.sedna.impl.ItemGunDrill;
import com.hbm.items.weapon.sedna.mags.IMagazine;
import com.hbm.items.weapon.sedna.mags.MagazineLiquidEngine;
import com.hbm.items.weapon.sedna.mods.XWeaponModManager;
import com.hbm.packet.toclient.BlockDestroyPayload;
import com.hbm.platform.Services;
import com.hbm.registration.IRegistrar;
import com.hbm.render.anim.AnimationEnums.GunAnimation;
import com.hbm.render.anim.BusAnimation;
import com.hbm.render.anim.BusAnimationKeyframe.IType;
import com.hbm.render.anim.BusAnimationSequence;
import com.hbm.render.anim.HbmAnimations;
import com.hbm.util.EntityDamageUtil;
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

    public static final String D_REACH = "D_REACH";
    public static final String F_DTNEG = "F_DTNEG";
    public static final String F_PIERCE = "F_PIERCE";
    public static final String I_AOE = "I_AOE";
    public static final String I_HARVEST = "I_HARVEST";
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
                                        new BusAnimationSequence()
                                                .setPos(-1, 0, 0)
                                                .addPos(0, 0, 0, 750, IType.SIN_DOWN));
                    case CYCLE:
                        double deploy = HbmAnimations.getRelevantTransformation("DEPLOY")[0];
                        double speed = HbmAnimations.getRelevantTransformation("SPEED")[0];
                        double spin = HbmAnimations.getRelevantTransformation("SPIN")[0] % 360;
                        return new BusAnimation()
                                .addBus(
                                        "DEPLOY",
                                        new BusAnimationSequence()
                                                .setPos(deploy, 0, 0)
                                                .addPos(
                                                        1,
                                                        0,
                                                        0,
                                                        (int) (500 * (1 - deploy)),
                                                        IType.SIN_FULL)
                                                .hold(1000)
                                                .addPos(0, 0, 0, 500, IType.SIN_FULL))
                                .addBus(
                                        "SPIN",
                                        new BusAnimationSequence()
                                                .setPos(spin, 0, 0)
                                                .addPos(spin + 360 * 1.5, 0, 0, 1500)
                                                .addPos(
                                                        360 * 3,
                                                        0,
                                                        0,
                                                        750 + (int) (1000 * (1D - spin / 360D)),
                                                        IType.SIN_DOWN))
                                .addBus(
                                        "SPEED",
                                        new BusAnimationSequence()
                                                .setPos(speed, 0, 0)
                                                .addPos(1, 0, 0, 500)
                                                .hold(1000)
                                                .addPos(
                                                        0,
                                                        0,
                                                        0,
                                                        750 + (int) (1000 * (1D - spin / 360D)),
                                                        IType.SIN_DOWN));
                    case CYCLE_DRY:
                        return new BusAnimation()
                                .addBus(
                                        "DEPLOY",
                                        new BusAnimationSequence()
                                                .addPos(0.25, 0, 0, 250, IType.SIN_FULL)
                                                .addPos(0, 0, 0, 250, IType.SIN_FULL))
                                .addBus(
                                        "SPIN",
                                        new BusAnimationSequence()
                                                .addPos(360, 0, 0, 1500, IType.SIN_DOWN))
                                .addBus(
                                        "SPEED",
                                        new BusAnimationSequence()
                                                .addPos(0.75, 0, 0, 250)
                                                .addPos(0, 0, 0, 1000, IType.SIN_DOWN));
                    case INSPECT:
                        return new BusAnimation()
                                .addBus(
                                        "LIFT",
                                        new BusAnimationSequence()
                                                .addPos(-45, 0, 0, 500, IType.SIN_FULL)
                                                .hold(1000)
                                                .addPos(0, 0, 0, 500, IType.SIN_DOWN));
                    default:
                        return null;
                }
            };

    public static void init(IRegistrar r) {

        ModItems.GUN_DRILL =
                r.registerItem(
                        "gun_drill",
                        props ->
                                new ItemGunDrill(
                                        WeaponQuality.UTILITY,
                                        props,
                                        new GunConfig()
                                                .dura(3_000)
                                                .draw(10)
                                                .inspect(55)
                                                .hideCrosshair(false)
                                                .crosshair(Crosshair.L_CIRCUMFLEX)
                                                .rec(
                                                        new Receiver(0)
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
                                                .orchestra(Orchestras.ORCHESTRA_DRILL)),
                        Item.Properties::new);
    }

    public static void doStandardFire(
            ItemStack stack, LambdaContext ctx, GunAnimation anim, boolean calcWear) {
        Player player = ctx.getPlayer();
        int index = ctx.configIndex();
        if (anim != null) ItemGunBaseNT.playAnimation(player, stack, anim, ctx.configIndex());

        Receiver primary = ctx.config().getReceivers(stack)[0];
        IMagazine mag = primary.getMagazine(stack);

        HitResult mop =
                EntityDamageUtil.getMouseOver(ctx.getPlayer(), getModdableReach(stack, 5.0D));
        if (mop != null) {
            if (mop instanceof EntityHitResult entityHit) {
                float damage = primary.getBaseDamage(stack);
                if (entityHit.getEntity() instanceof LivingEntity living) {
                    EntityDamageUtil.attackEntityFromNT(
                            living,
                            ctx.getPlayer().damageSources().playerAttack(ctx.getPlayer()),
                            damage,
                            true,
                            true,
                            0.1F,
                            getModdableDTNegation(stack, 2F),
                            getModdablePiercing(stack, 0.15F));
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
        mag.useUpAmmo(stack, ctx.inventory(), ammoToUse);

        if (calcWear)
            ItemGunBaseNT.setWear(
                    stack,
                    index,
                    Math.min(
                            ItemGunBaseNT.getWear(stack, index),
                            ctx.config().getDurability(stack)));
    }

    public static void breakExtraBlock(
            Level world, int x, int y, int z, Player playerEntity, int refX, int refY, int refZ) {
        BlockPos pos = new BlockPos(x, y, z);
        if (world.isEmptyBlock(pos)) return;
        if (!(playerEntity instanceof ServerPlayer player)) return;

        BlockState state = world.getBlockState(pos);

        if (!Services.PLATFORM.canHarvestBlock(world, pos, state, player)
                || (state.getDestroySpeed(world, pos) == -1.0F
                        && state.getDestroyProgress(player, world, pos) == 0.0F)
                || state.is(ModBlocks.STONE_KEYHOLE.get())) {
            if (!didPlink) {
                world.playSound(
                        null,
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
                Services.NETWORK.sendTo(new BlockDestroyPayload(state, pos), player);
            }
        }
    }

    public static double getModdableReach(ItemStack stack, double base) {
        return XWeaponModManager.eval(base, stack, D_REACH, ModItems.GUN_DRILL.get(), 0);
    }

    public static float getModdableDTNegation(ItemStack stack, float base) {
        return XWeaponModManager.eval(base, stack, F_DTNEG, ModItems.GUN_DRILL.get(), 0);
    }

    public static float getModdablePiercing(ItemStack stack, float base) {
        return XWeaponModManager.eval(base, stack, F_PIERCE, ModItems.GUN_DRILL.get(), 0);
    }

    public static int getModdableAoE(ItemStack stack, int base) {
        return XWeaponModManager.eval(base, stack, I_AOE, ModItems.GUN_DRILL.get(), 0);
    }

    public static int getModdableHarvestLevel(ItemStack stack, int base) {
        return XWeaponModManager.eval(base, stack, I_HARVEST, ModItems.GUN_DRILL.get(), 0);
    }

    public static void submitBlockHighlight(
            PoseStack poseStack,
            SubmitNodeCollector collector,
            Vec3 cameraPos,
            Player player,
            ItemStack drill) {
        HitResult mop = EntityDamageUtil.getMouseOver(player, getModdableReach(drill, 5.0D));

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
