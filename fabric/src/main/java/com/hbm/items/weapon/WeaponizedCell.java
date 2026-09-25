// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.weapon;

import com.hbm.data.ItemData;
import com.hbm.entity.effect.EntityCloudFleijaRainbow;
import com.hbm.entity.logic.EntityNukeExplosionMK3;
import com.hbm.interfaces.IItemEntityUpdate;
import com.hbm.platform.Services;
import java.util.function.Consumer;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

public class WeaponizedCell extends Item implements IItemEntityUpdate {

    public static final int FUSE = 50 * 20;

    public WeaponizedCell(Properties properties) {
        super(properties);
    }

    @Override
    public boolean updateDroppedItem(ItemEntity entity) {
        ServerLevel level = (ServerLevel) entity.level();

        if (entity.tickCount > FUSE || entity.isOnFire()) {
            if (ItemData.DROP_STAR.get()) {
                EntityNukeExplosionMK3 blast =
                        EntityNukeExplosionMK3.statFacFleija(
                                level, entity.getX(), entity.getY(), entity.getZ(), 100);
                if (!blast.isRemoved()) {
                    level.playSound(
                            null,
                            entity.getX(),
                            entity.getY(),
                            entity.getZ(),
                            SoundEvents.GENERIC_EXPLODE.value(),
                            SoundSource.BLOCKS,
                            100.0F,
                            level.getRandom().nextFloat() * 0.1F + 0.9F);
                    level.addFreshEntity(blast);
                    level.addFreshEntity(
                            EntityCloudFleijaRainbow.statFac(
                                    level, 100, entity.getX(), entity.getY(), entity.getZ()));
                }
            }
            entity.discard();
            return true;
        }

        int remaining = Math.max(FUSE - entity.tickCount, 1);
        spark(
                level,
                entity,
                level.getRandom().nextInt(FUSE) >= remaining
                        ? DustParticleOptions.REDSTONE
                        : ParticleTypes.SMOKE);
        if (remaining < 100) spark(level, entity, ParticleTypes.LAVA);
        return false;
    }

    private static void spark(ServerLevel level, ItemEntity entity, ParticleOptions particle) {
        level.sendParticles(
                particle,
                entity.getX(),
                entity.getY(),
                entity.getZ(),
                1,
                entity.getBbWidth() / 2,
                entity.getBbHeight(),
                entity.getBbWidth() / 2,
                0.0D);
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> adder,
            TooltipFlag flag) {
        adder.accept(Component.translatable("desc.item.weaponizedCell.aChargedEnergy"));
        adder.accept(Component.translatable("desc.item.weaponizedCell.whenLeftOn"));
    }
}
