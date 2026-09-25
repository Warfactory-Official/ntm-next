// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.weapon;

import com.hbm.entity.grenade.EntityGrenadeBouncyGeneric;
import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileItem;
import net.minecraft.world.level.Level;

public class ItemGenericGrenade extends ItemGrenade implements ProjectileItem {

    public ItemGenericGrenade(Properties properties, int fuse) {
        super(properties, fuse);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide()) {

            level.playSound(
                    null,
                    player.getX(),
                    player.getY(),
                    player.getZ(),
                    SoundEvents.ARROW_SHOOT,
                    SoundSource.PLAYERS,
                    0.5F,
                    0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F));
            level.addFreshEntity(new EntityGrenadeBouncyGeneric(level, player).setType(this));
            stack.consume(1, player);
        }

        return InteractionResult.SUCCESS;
    }

    public void explode(
            Entity grenade, LivingEntity thrower, Level level, double x, double y, double z) {}

    public int getMaxTimer() {
        return this.fuse * 20;
    }

    public double getBounceMod() {
        return 0.5D;
    }

    @Override
    public Projectile asProjectile(
            Level level, Position position, ItemStack stack, Direction direction) {
        return new EntityGrenadeBouncyGeneric(level, position.x(), position.y(), position.z())
                .setType(this);
    }

    public Identifier iconTexture() {
        return BuiltInRegistries.ITEM.getKey(this);
    }
}
