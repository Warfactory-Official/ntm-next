// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.entity.mob.glyphid;

import com.hbm.entity.effect.EntityMist;
import com.hbm.entity.mob.glyphid.GlyphidStats.StatBundle;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.items.ModItems;
import com.hbm.items.weapon.ItemDisperser;
import com.hbm.main.ResourceManager;
import com.hbm.registration.Reg;
import com.hbm.util.MobUtil;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class EntityGlyphidBrenda extends EntityGlyphid {

    public static Reg.@Nullable EntityHandle<EntityGlyphid> GRUNT_TYPE;

    public EntityGlyphidBrenda(EntityType<? extends EntityGlyphidBrenda> type, Level level) {
        super(type, level);
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return EntityGlyphid.createAttributes()
                .add(Attributes.MAX_HEALTH, GlyphidStats.getStats().getBrenda().health)
                .add(
                        Attributes.MOVEMENT_SPEED,
                        MobUtil.oldAiWalkSpeed(GlyphidStats.getStats().getBrenda().speed))
                .add(Attributes.ATTACK_DAMAGE, GlyphidStats.getStats().getBrenda().damage);
    }

    @Override
    protected EntityDimensions getDefaultDimensions(Pose pose) {
        return EntityDimensions.fixed(2.5F, 1.75F);
    }

    @Override
    public Identifier getSkin() {
        return ResourceManager.glyphid_brenda_tex;
    }

    @Override
    public double getGlyphidScale() {
        return 2D;
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        getAttribute(Attributes.MAX_HEALTH)
                .setBaseValue(GlyphidStats.getStats().getBrenda().health);
        getAttribute(Attributes.MOVEMENT_SPEED)
                .setBaseValue(MobUtil.oldAiWalkSpeed(GlyphidStats.getStats().getBrenda().speed));
        getAttribute(Attributes.ATTACK_DAMAGE)
                .setBaseValue(GlyphidStats.getStats().getBrenda().damage);
    }

    @Override
    public StatBundle getStats() {
        return GlyphidStats.getStats().statsBrenda;
    }

    @Override
    public boolean isArmorBroken(float amount) {
        return random.nextInt(100) <= Math.min(Math.pow(amount * 0.12, 2), 100);
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);

        if (!level().isClientSide() && getHealth() <= 0.0F) {
            EntityMist mist = new EntityMist(level());
            mist.setType(NTMFluids.PHEROMONE);
            mist.setPos(getX(), getY(), getZ());
            mist.setArea(14, 6);
            mist.setDuration(80);
            level().addFreshEntity(mist);

            if (GRUNT_TYPE != null && level() instanceof ServerLevel serverLevel) {
                for (int i = 0; i < 12; i++) {
                    EntityGlyphid glyphid = new EntityGlyphid(GRUNT_TYPE.get(), serverLevel);
                    glyphid.snapTo(
                            getX(), getY() + 0.5D, getZ(), random.nextFloat() * 360.0F, 0.0F);
                    serverLevel.addFreshEntity(glyphid);
                    glyphid.move(
                            MoverType.SELF,
                            new Vec3(random.nextGaussian(), 0, random.nextGaussian()));
                }
            }
        }
    }

    @Override
    protected void dropCustomDeathLoot(
            ServerLevel level, DamageSource source, boolean killedByPlayer) {
        super.dropCustomDeathLoot(level, source, killedByPlayer);
        if (random.nextInt(3) == 0) spawnAtLocation(level, glandStack(NTMFluids.PHEROMONE));
    }

    private static ItemStack glandStack(Fluid fluid) {
        ItemDisperser gland = ModItems.GLYPHID_GLAND.get();
        return gland.make(fluid, gland.capacity);
    }
}
