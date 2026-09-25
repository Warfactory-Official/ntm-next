// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.entity.mob;

import com.hbm.entity.projectile.EntityBulletBaseMK4;
import com.hbm.handler.threading.TargetPoint;
import com.hbm.items.ModItems;
import com.hbm.items.weapon.sedna.factory.XFactory762mm;
import com.hbm.packet.toclient.TeslaArcPayload;
import com.hbm.platform.Services;
import com.hbm.potion.HbmPotion;
import com.hbm.sound.ModSounds;
import com.hbm.tileentity.machine.BlockEntityTesla;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class EntityTaintCrab extends EntityCyberCrab {

    public List<Vec3> targets = new ArrayList<>();

    public EntityTaintCrab(EntityType<? extends EntityTaintCrab> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return EntityCyberCrab.createAttributes()
                .add(Attributes.MAX_HEALTH, 25.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.5D);
    }

    @Override
    protected EntityDimensions getDefaultDimensions(Pose pose) {
        return EntityDimensions.fixed(1.25F, 1.25F);
    }

    @Override
    protected Goal rangedGoal() {
        return new RangedAttackGoal(this, 0.5D, 5, 5, 50.0F);
    }

    @Override
    public void tick() {
        if (level() instanceof ServerLevel server) {
            List<Vec3> zapped =
                    BlockEntityTesla.zap(server, getX(), getY() + 1.25D, getZ(), 10D, this);

            if (!zapped.equals(targets)) {
                targets = zapped;
                Services.NETWORK.sendToAllAround(
                        new TeslaArcPayload(this, targets),
                        new TargetPoint(server, getX(), getY(), getZ(), 64));
            }

            AABB box =
                    new AABB(
                            getX() - 5, getY() - 5, getZ() - 5, getX() + 5, getY() + 5, getZ() + 5);
            for (LivingEntity e : server.getEntitiesOfClass(LivingEntity.class, box)) {
                if (!(e instanceof EntityCyberCrab))
                    e.addEffect(new MobEffectInstance(HbmPotion.radiation(), 10, 15));
            }
        }

        super.tick();
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

        int amount = random.nextInt(3) + (looting > 0 ? random.nextInt(looting + 1) : 0);
        for (int i = 0; i < amount; i++)
            spawnAtLocation(level, new ItemStack(ModItems.COIL_COPPER.get()));

        if (!killedByPlayer) return;

        if (random.nextInt(200) - looting < 5) {
            spawnAtLocation(level, new ItemStack(ModItems.COIL_MAGNETIZED_TUNGSTEN.get()));
        }
    }

    @Override
    public void performRangedAttack(LivingEntity target, float power) {
        EntityBulletBaseMK4 bullet =
                new EntityBulletBaseMK4(this, XFactory762mm.r762_fmj, 10F, 0F, 0F, 0F, 0F);
        Vec3 motion = bullet.getDeltaMovement().scale(0.3D);

        if (level() instanceof ServerLevel server) {
            server.sendParticles(
                    ParticleTypes.FLAME,
                    bullet.getX(),
                    bullet.getY(),
                    bullet.getZ(),
                    0,
                    motion.x,
                    motion.y,
                    motion.z,
                    1.0D);
        }

        level().addFreshEntity(bullet);
        playSound(ModSounds.SAW_SHOOT.get(), 1.0F, 0.5F);
    }
}
