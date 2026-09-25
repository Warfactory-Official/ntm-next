// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.entity.mob;

import com.hbm.advancement.DetonationTrigger;
import com.hbm.advancement.HbmCriteria;
import com.hbm.entity.logic.EntityNukeExplosionMK5;
import com.hbm.explosion.ExplosionNukeGeneric;
import com.hbm.explosion.ExplosionNukeSmall;
import com.hbm.handler.threading.TargetPoint;
import com.hbm.items.ModItems;
import com.hbm.items.weapon.sedna.factory.GunFactory;
import com.hbm.lib.ModDamageTypes;
import com.hbm.packet.toclient.MukePayload;
import com.hbm.platform.Services;
import com.hbm.sound.ModSounds;
import com.hbm.util.ContaminationUtil.ContaminationType;
import com.hbm.util.ContaminationUtil.HazardType;
import com.hbm.util.ContaminationUtil;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.skeleton.AbstractSkeleton;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;

public class EntityCreeperNuclear extends EntityCreeperBase {

    public EntityCreeperNuclear(EntityType<? extends EntityCreeperNuclear> type, Level level) {
        super(type, level);
        maxSwell = 75;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Creeper.createAttributes()
                .add(Attributes.MAX_HEALTH, 50.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {

        if (isRemoved()) return false;

        if (source.is(ModDamageTypes.RADIATION) || source.is(ModDamageTypes.MUD_POISONING)) {
            if (isAlive()) heal(amount);
            return false;
        }

        return super.hurtServer(level, source, amount);
    }

    @Override
    protected void dropCustomDeathLoot(
            ServerLevel level, DamageSource source, boolean killedByPlayer) {
        super.dropCustomDeathLoot(level, source, killedByPlayer);

        int looting = 0;
        if (source.getEntity() instanceof LivingEntity attacker) {
            Holder<Enchantment> lootingEnchant =
                    level.registryAccess()
                            .lookupOrThrow(Registries.ENCHANTMENT)
                            .getOrThrow(Enchantments.LOOTING);
            looting = EnchantmentHelper.getEnchantmentLevel(lootingEnchant, attacker);
        }

        int tnt = random.nextInt(3) + (looting > 0 ? random.nextInt(looting + 1) : 0);
        for (int i = 0; i < tnt; i++) spawnAtLocation(level, new ItemStack(Items.TNT));

        if (random.nextInt(3) == 0)
            spawnAtLocation(level, new ItemStack(ModItems.COIN_CREEPER.get()));
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);

        if (level() instanceof ServerLevel server) {
            for (Player player :
                    server.getEntitiesOfClass(Player.class, getBoundingBox().inflate(50D))) {
                if (player instanceof ServerPlayer serverPlayer) {
                    HbmCriteria.detonation(serverPlayer, DetonationTrigger.Kind.NUCLEAR_CREEPER);
                }
            }
        }

        boolean shotBySkeleton = source.getEntity() instanceof AbstractSkeleton;
        boolean strayArrow =
                source.getDirectEntity() instanceof Arrow arrow && arrow.getOwner() == null;

        if ((shotBySkeleton || strayArrow) && level() instanceof ServerLevel server) {
            spawnAtLocation(
                    server, ModItems.AMMO_STANDARD.stack(GunFactory.EnumAmmo.NUKE_STANDARD));
        }
    }

    @Override
    public void tick() {

        if (level() instanceof ServerLevel server) {
            for (Entity e : server.getEntities(this, getBoundingBox().inflate(5, 5, 5))) {
                if (e instanceof LivingEntity living) {
                    ContaminationUtil.contaminate(
                            living, HazardType.RADIATION, ContaminationType.CREATIVE, 0.25F);
                }
            }
        }

        super.tick();

        if (isAlive() && getHealth() < getMaxHealth() && tickCount % 10 == 0) heal(1.0F);
    }

    @Override
    protected void detonate(ServerLevel level) {

        boolean griefing = Services.PLATFORM.canEntityGrief(level, this);

        if (isPowered()) {

            Services.NETWORK.sendToAllAround(
                    new MukePayload(getX(), getY() + 0.5D, getZ(), false, false),
                    new TargetPoint(level, getX(), getY(), getZ(), 250));
            level.playSound(
                    null,
                    getX(),
                    getY() + 0.5D,
                    getZ(),
                    ModSounds.GUN_MINI_NUKE_EXPLOSION.get(),
                    SoundSource.BLOCKS,
                    15.0F,
                    1.0F);

            if (griefing) {
                level.addFreshEntity(
                        EntityNukeExplosionMK5.statFac(level, 50, getX(), getY(), getZ()));
            } else {
                ExplosionNukeGeneric.dealDamage(level, getX(), getY() + 0.5D, getZ(), 100);
            }
        } else {
            ExplosionNukeSmall.explode(
                    level,
                    getX(),
                    getY() + 0.5D,
                    getZ(),
                    griefing ? ExplosionNukeSmall.PARAMS_MEDIUM : ExplosionNukeSmall.PARAMS_SAFE);
        }
    }
}
