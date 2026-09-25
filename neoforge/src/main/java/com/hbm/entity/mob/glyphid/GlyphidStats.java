// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: HbmMods (The Bobcat)
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.entity.mob.glyphid;

import com.hbm.extprop.HbmLivingProps;
import com.hbm.lib.ModDamageTypes;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;

public abstract class GlyphidStats {

    public static GlyphidStats GLYPHID_STATS_70K = new GlyphidStats70K();
    public static GlyphidStats GLYPHID_STATS_NT = new GlyphidStatsNT();

    public static GlyphidStats getStats() {
        return GLYPHID_STATS_NT;
    }

    protected StatBundle statsGrunt;
    protected StatBundle statsBombardier;
    protected StatBundle statsBrawler;
    protected StatBundle statsDigger;
    protected StatBundle statsBlaster;
    protected StatBundle statsBehemoth;
    protected StatBundle statsBrenda;
    protected StatBundle statsNuclear;
    protected StatBundle statsScout;

    public static class StatBundle {
        public final double health;
        public final double speed;
        public final double damage;
        @Deprecated public final float divisor;
        @Deprecated public final float damageThreshold;
        public final float thresholdMultForArmor;
        public final float resistanceMult;

        public StatBundle(
                double health, double speed, double damage, float divisor, float damageThreshold) {
            this(health, speed, damage, divisor, damageThreshold, 0F, 0F);
        }

        public StatBundle(
                double health,
                double speed,
                double damage,
                float divisor,
                float damageThreshold,
                float thresholdMultPerArmor,
                float resistanceMult) {
            this.health = health;
            this.speed = speed;
            this.damage = damage;
            this.divisor = divisor;
            this.damageThreshold = damageThreshold;
            this.thresholdMultForArmor = thresholdMultPerArmor;
            this.resistanceMult = resistanceMult;
        }
    }

    public abstract boolean handleAttack(EntityGlyphid glyphid, DamageSource source, float amount);

    public StatBundle getGrunt() {
        return statsGrunt;
    }

    public StatBundle getBombardier() {
        return statsBombardier;
    }

    public StatBundle getBrawler() {
        return statsBrawler;
    }

    public StatBundle getDigger() {
        return statsDigger;
    }

    public StatBundle getBlaster() {
        return statsBlaster;
    }

    public StatBundle getBehemoth() {
        return statsBehemoth;
    }

    public StatBundle getBrenda() {
        return statsBrenda;
    }

    public StatBundle getNuclear() {
        return statsNuclear;
    }

    public StatBundle getScout() {
        return statsScout;
    }

    public static class GlyphidStats70K extends GlyphidStats {

        public GlyphidStats70K() {
            this.statsGrunt = new StatBundle(30D, 1D, 5D, 1F, 0.5F);
            this.statsBombardier = new StatBundle(20D, 1D, 5D, 1F, 0.5F);
            this.statsBrawler = new StatBundle(50D, 1D, 10D, 3F, 1F);
            this.statsDigger = new StatBundle(50D, 1D, 5D, 1F, 0.5F);
            this.statsBlaster = new StatBundle(50D, 1D, 10D, 2F, 1F);
            this.statsBehemoth = new StatBundle(130D, 0.8D, 25D, 4F, 2.5F);
            this.statsBrenda = new StatBundle(250D, 1.2D, 50D, 5F, 10F);
            this.statsNuclear = new StatBundle(100D, 0.8D, 50D, 5F, 10F);
            this.statsScout = new StatBundle(20D, 1.5D, 2D, 1F, 0.5F);
        }

        @Override
        public boolean handleAttack(EntityGlyphid glyphid, DamageSource source, float amount) {

            if (!source.is(DamageTypeTags.BYPASSES_ARMOR)
                    && !glyphid.level().isClientSide()
                    && !EntityGlyphid.isFireDamage(source)
                    && !source.is(ModDamageTypes.ICE)) {
                byte armor = glyphid.armor();

                if (armor != 0) {

                    if (amount < glyphid.getStats().damageThreshold) return false;

                    if (amount > 1 && glyphid.isArmorBroken(amount)) {
                        glyphid.breakOffArmor();
                        amount *= 0.25F;
                    }

                    amount -= glyphid.getStats().damageThreshold;
                    if (amount < 0) return true;
                }
            }

            if (EntityGlyphid.isFireDamage(source)) {
                amount *= 0.7F;
            } else if (source.is(DamageTypes.PLAYER_ATTACK)) {
                amount *=
                        glyphid.getGlyphidScale() < 1.25
                                ? 1.5
                                : glyphid.getGlyphidScale() < 1.3 ? 0.8 : 0.5;
            } else if (source.is(ModDamageTypes.ACID)) {
                amount = 0;
            } else if (source.is(DamageTypes.IN_WALL)) {
                amount *= 15F;
            }

            HbmLivingProps props = HbmLivingProps.peek(glyphid);
            if (props != null && props.phosphorus > 0) {
                amount *= 1.5F;
            }

            return glyphid.attackSuperclass(source, amount);
        }
    }

    public static class GlyphidStatsNT extends GlyphidStats {

        public GlyphidStatsNT() {
            this.statsGrunt = new StatBundle(20D, 1D, 2D, 0.25F, 0F, 1F, 0.1F);
            this.statsBombardier = new StatBundle(15D, 1D, 2D, 0.25F, 0F, 1F, 0.1F);
            this.statsBrawler = new StatBundle(35D, 1D, 10D, 0.5F, 0.5F, 2F, 0.15F);
            this.statsDigger = new StatBundle(50D, 1D, 10D, 0.5F, 0.5F, 3F, 0.20F);
            this.statsBlaster = new StatBundle(35D, 1D, 10D, 0.5F, 0.5F, 2F, 0.15F);
            this.statsBehemoth = new StatBundle(125D, 0.8D, 25D, 1.5F, 2F, 5F, 0.35F);
            this.statsBrenda = new StatBundle(250D, 1.2D, 50D, 2.5F, 5F, 10F, 0.5F);
            this.statsNuclear = new StatBundle(100D, 0.8D, 50D, 2.5F, 5F, 10F, 0.5F);
            this.statsScout = new StatBundle(20D, 1.5D, 5D, 0.5F, 0F, 0.5F, 0.5F);
        }

        @Override
        public boolean handleAttack(EntityGlyphid glyphid, DamageSource source, float amount) {
            if (source.is(ModDamageTypes.ACID_PLAYER)
                    && source.getEntity() instanceof EntityGlyphid) return false;
            return glyphid.attackSuperclass(source, amount);
        }
    }
}
