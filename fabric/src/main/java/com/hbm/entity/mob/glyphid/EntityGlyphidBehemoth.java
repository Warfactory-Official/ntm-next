// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.entity.mob.glyphid;

import com.hbm.entity.ModEntities;
import com.hbm.entity.effect.EntityMist;
import com.hbm.entity.mob.glyphid.GlyphidStats.StatBundle;
import com.hbm.entity.projectile.EntityChemical;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.items.ModItems;
import com.hbm.items.weapon.ItemDisperser;
import com.hbm.main.ResourceManager;
import com.hbm.util.MobUtil;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;

public class EntityGlyphidBehemoth extends EntityGlyphid {

    public int timer = 120;
    public int breathTime = 0;

    public EntityGlyphidBehemoth(EntityType<? extends EntityGlyphidBehemoth> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return EntityGlyphid.createAttributes()
                .add(Attributes.MAX_HEALTH, GlyphidStats.getStats().getBehemoth().health)
                .add(
                        Attributes.MOVEMENT_SPEED,
                        MobUtil.oldAiWalkSpeed(GlyphidStats.getStats().getBehemoth().speed))
                .add(Attributes.ATTACK_DAMAGE, GlyphidStats.getStats().getBehemoth().damage);
    }

    @Override
    protected EntityDimensions getDefaultDimensions(Pose pose) {
        return EntityDimensions.fixed(2.5F, 1.5F);
    }

    @Override
    public Identifier getSkin() {
        return ResourceManager.glyphid_behemoth_tex;
    }

    @Override
    public double getGlyphidScale() {
        return 1.5D;
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        getAttribute(Attributes.MAX_HEALTH)
                .setBaseValue(GlyphidStats.getStats().getBehemoth().health);
        getAttribute(Attributes.MOVEMENT_SPEED)
                .setBaseValue(MobUtil.oldAiWalkSpeed(GlyphidStats.getStats().getBehemoth().speed));
        getAttribute(Attributes.ATTACK_DAMAGE)
                .setBaseValue(GlyphidStats.getStats().getBehemoth().damage);
    }

    @Override
    public StatBundle getStats() {
        return GlyphidStats.getStats().statsBehemoth;
    }

    @Override
    public void tick() {
        boolean lockYaw = !level().isClientSide() && breathTime > 0;
        float lockedYaw = getYRot();
        float lockedBodyYaw = yBodyRot;
        super.tick();

        if (lockYaw) {
            setYRot(lockedYaw);
            yBodyRot = lockedBodyYaw;
        }
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);

        if (getTarget() == null) {
            timer = 120;
            breathTime = 0;
        } else if (breathTime > 0) {
            if (!swinging) swing(InteractionHand.MAIN_HAND);
            acidAttack();
            breathTime--;
        } else if (--timer <= 0) {
            breathTime = 120;
            timer = 120;
        }
    }

    public void acidAttack() {
        LivingEntity target = getTarget();
        if (!level().isClientSide() && target != null && distanceTo(target) < 20) {
            addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 2 * 20, 6));
            EntityChemical chem =
                    new EntityChemical(ModEntities.CHEMICAL.get(), level(), this, 0, 0, 0);
            chem.setFluid(NTMFluids.SULFURIC_ACID);
            level().addFreshEntity(chem);
        }
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);

        if (!level().isClientSide()) {
            EntityMist mist = new EntityMist(level());
            mist.setType(NTMFluids.SULFURIC_ACID);
            mist.setPos(getX(), getY(), getZ());
            mist.setArea(10, 4);
            mist.setDuration(120);
            level().addFreshEntity(mist);
        }
    }

    @Override
    protected void dropCustomDeathLoot(
            ServerLevel level, DamageSource source, boolean killedByPlayer) {
        spawnAtLocation(level, glandStack(NTMFluids.SULFURIC_ACID));
        super.dropCustomDeathLoot(level, source, killedByPlayer);
    }

    private static ItemStack glandStack(Fluid fluid) {
        ItemDisperser gland = ModItems.GLYPHID_GLAND.get();
        return gland.make(fluid, gland.capacity);
    }

    @Override
    public boolean isArmorBroken(float amount) {
        return random.nextInt(100) <= Math.min(Math.pow(amount * 0.15, 2), 100);
    }

    @Override
    public int swingDuration() {
        return 100;
    }
}
