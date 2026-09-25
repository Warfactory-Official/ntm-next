// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: HbmMods (The Bobcat)
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.entity.mob.glyphid;

import com.hbm.entity.mob.glyphid.GlyphidStats.StatBundle;
import com.hbm.lib.Library;
import com.hbm.main.ResourceManager;
import com.hbm.registration.IRegistrar;
import com.hbm.registration.Reg;
import com.hbm.util.MobUtil;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

public class EntityGlyphidBlaster extends EntityGlyphidBombardier {

    public static Reg.@Nullable EntityHandle<EntityGlyphidBlaster> TYPE;

    public static void register(IRegistrar r) {
        TYPE =
                Reg.entity(
                        "entity_glyphid_blaster",
                        () ->
                                EntityType.Builder.<EntityGlyphidBlaster>of(
                                                EntityGlyphidBlaster::new, MobCategory.MONSTER)
                                        .noLootTable()
                                        .sized(2F, 1.125F)
                                        .clientTrackingRange(5)
                                        .updateInterval(3)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        Library.id("entity_glyphid_blaster"))));
        r.registerLivingAttributes(TYPE, EntityGlyphidBlaster::createAttributes);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return EntityGlyphidBombardier.createAttributes()
                .add(Attributes.MAX_HEALTH, GlyphidStats.getStats().getBlaster().health)
                .add(
                        Attributes.MOVEMENT_SPEED,
                        MobUtil.oldAiWalkSpeed(GlyphidStats.getStats().getBlaster().speed))
                .add(Attributes.ATTACK_DAMAGE, GlyphidStats.getStats().getBlaster().damage);
    }

    public EntityGlyphidBlaster(EntityType<? extends EntityGlyphidBlaster> type, Level level) {
        super(type, level);
    }

    @Override
    public Identifier getSkin() {
        return ResourceManager.glyphid_blaster_tex;
    }

    @Override
    public double getGlyphidScale() {
        return 1.25D;
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        getAttribute(Attributes.MAX_HEALTH)
                .setBaseValue(GlyphidStats.getStats().getBlaster().health);
        getAttribute(Attributes.MOVEMENT_SPEED)
                .setBaseValue(MobUtil.oldAiWalkSpeed(GlyphidStats.getStats().getBlaster().speed));
        getAttribute(Attributes.ATTACK_DAMAGE)
                .setBaseValue(GlyphidStats.getStats().getBlaster().damage);
    }

    @Override
    public StatBundle getStats() {
        return GlyphidStats.getStats().statsBlaster;
    }

    @Override
    public boolean isArmorBroken(float amount) {
        return random.nextInt(100) <= Math.min(Math.pow(amount * 0.25, 2), 100);
    }

    @Override
    public float getBombDamage() {
        return 15F;
    }

    @Override
    public int getBombCount() {
        return 10;
    }

    @Override
    public float getSpreadMult() {
        return 0.5F;
    }

    @Override
    public double getV0() {
        return 1.25D;
    }
}
