// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.tool;

import com.hbm.capability.NtmContracts;
import com.hbm.client.ClientPlayerAccess;
import com.hbm.interfaces.IToolable;
import com.hbm.inventory.material.MaterialShapes;
import com.hbm.inventory.material.Mats;
import com.hbm.items.IAnimatedItem;
import com.hbm.items.ModItems;
import com.hbm.items.special.Autogen;
import com.hbm.lib.ModDamageTypes;
import com.hbm.packet.toclient.HbmAnimationPayload;
import com.hbm.platform.Services;
import com.hbm.render.anim.AnimationEnums;
import com.hbm.render.anim.BusAnimation;
import com.hbm.render.anim.BusAnimationSequence;
import com.hbm.render.anim.HbmAnimations;
import com.hbm.sound.ModSounds;
import com.hbm.util.EntityDamageUtil;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class ItemBoltgun extends Item implements IToolable.Tool, IAnimatedItem {

    private static final float MELEE_DAMAGE = 10F;

    public ItemBoltgun(Properties props) {
        super(props);
    }

    @Override
    public IToolable.ToolType toolType() {
        return IToolable.ToolType.BOLT;
    }

    private static List<Item> boltTypes() {
        return List.of(
                ModItems.BOLT_SPIKE.get(),
                Autogen.require(MaterialShapes.BOLT, Mats.MAT_STEEL),
                Autogen.require(MaterialShapes.BOLT, Mats.MAT_TUNGSTEN),
                Autogen.require(MaterialShapes.BOLT, Mats.MAT_DURA));
    }

    public static boolean handleAttack(Player player, Entity entity) {
        if (!(player.getMainHandItem().getItem() instanceof ItemBoltgun)) return false;
        Level level = player.level();
        if (!entity.isAlive()) return false;

        for (Item bolt : boltTypes()) {
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                ItemStack slot = player.getInventory().getItem(i);
                if (slot.isEmpty() || slot.getItem() != bolt) continue;

                if (!level.isClientSide()) {
                    level.playSound(
                            null,
                            entity.getX(),
                            entity.getY(),
                            entity.getZ(),
                            ModSounds.BOLTGUN.get(),
                            SoundSource.PLAYERS,
                            1.0F,
                            1.0F);
                    player.getInventory().removeItem(i, 1);

                    EntityDamageUtil.attackEntityFromIgnoreIFrame(
                            entity, boltDamage(level, player), MELEE_DAMAGE);

                    burst(
                            level,
                            entity.getX(),
                            entity.getY() + entity.getBbHeight() / 2F,
                            entity.getZ());
                    playSwing(player);
                }
                return true;
            }
        }
        return false;
    }

    private static DamageSource boltDamage(Level level, Player player) {
        Holder<DamageType> type =
                level.registryAccess()
                        .lookupOrThrow(Registries.DAMAGE_TYPE)
                        .getOrThrow(ModDamageTypes.BOLT);
        return new DamageSource(type, player);
    }

    private static void burst(Level level, double x, double y, double z) {
        if (level instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.EXPLOSION, x, y, z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
    }

    private static void playSwing(@Nullable Player player) {
        if (player instanceof ServerPlayer sp) {
            Services.NETWORK.sendTo(
                    new HbmAnimationPayload(AnimationEnums.ToolAnimation.SWING.ordinal(), 0, 0),
                    sp);
        }
    }

    public static void clientHeldTick() {
        Player player = ClientPlayerAccess.player();
        if (player == null
                || !ClientPlayerAccess.firstPersonCamera()
                || !(player.getMainHandItem().getItem() instanceof ItemBoltgun)) return;
        if (HbmAnimations.getRelevantTransformation("RECOIL")[0] != 0) player.swinging = false;
    }

    public static BusAnimation animation(AnimationEnums.ToolAnimation type) {
        if (type != AnimationEnums.ToolAnimation.SWING) return null;
        return new BusAnimation()
                .addBus(
                        "RECOIL",
                        new BusAnimationSequence().addPos(1, 0, 1, 50).addPos(0, 0, 1, 100));
    }

    @Override
    public @Nullable BusAnimation getAnimation(AnimationEnums.ToolAnimation type, ItemStack stack) {
        return animation(type);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        BlockPos pos = ctx.getClickedPos();
        IToolable toolable = NtmContracts.TOOLABLE.at(level, pos);
        if (toolable == null) return InteractionResult.PASS;

        if (!toolable.onScrew(
                level,
                ctx.getPlayer(),
                pos,
                ctx.getClickedFace(),
                ctx.getClickLocation(),
                toolType())) {
            return InteractionResult.PASS;
        }

        if (!level.isClientSide()) {
            level.playSound(null, pos, ModSounds.BOLTGUN.get(), SoundSource.PLAYERS, 1.0F, 1.0F);

            Direction side = ctx.getClickedFace();
            double off = 0.25D;
            Vec3 hit = ctx.getClickLocation();
            burst(
                    level,
                    hit.x + side.getStepX() * off,
                    hit.y + side.getStepY() * off,
                    hit.z + side.getStepZ() * off);
            playSwing(ctx.getPlayer());
        }
        return InteractionResult.SUCCESS;
    }
}
