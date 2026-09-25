// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.fluid.trait;

import com.hbm.handler.ArmorUtil;
import com.hbm.hazard.HazardClass;
import com.hbm.util.ArmorRegistry;
import com.hbm.util.I18nUtil;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;

public class FT_Toxin extends FluidTrait {

    public List<ToxinEntry> entries = new ArrayList<>();

    public FT_Toxin addEntry(ToxinEntry entry) {
        entries.add(entry);
        return this;
    }

    @Override
    public void addInfoHidden(List<String> info) {
        info.add(
                ChatFormatting.LIGHT_PURPLE
                        + "["
                        + I18nUtil.resolveKey("hbmfluid.trait.toxin")
                        + "]");
        for (ToxinEntry entry : entries) entry.addInfo(info);
    }

    public void affect(LivingEntity entity, double intensity) {
        for (ToxinEntry entry : entries) entry.poison(entity, intensity);
    }

    public abstract static class ToxinEntry {

        public HazardClass clazz;
        public boolean fullBody;

        public ToxinEntry(HazardClass clazz, boolean fullBody) {
            this.clazz = clazz;
            this.fullBody = fullBody;
        }

        public boolean isProtected(LivingEntity entity) {

            boolean hasMask = clazz == null;
            boolean hasSuit = !fullBody;

            if (clazz != null && ArmorRegistry.hasProtection(entity, EquipmentSlot.HEAD, clazz)) {
                ArmorUtil.damageGasMaskFilter(entity, 1);
                hasMask = true;
            }

            if (fullBody && ArmorUtil.checkForHazmat(entity)) {
                hasSuit = true;
            }

            return hasMask && hasSuit;
        }

        public abstract void poison(LivingEntity entity, double intensity);

        public abstract void addInfo(List<String> info);
    }

    public static class ToxinDirectDamage extends ToxinEntry {

        public ResourceKey<DamageType> damage;
        public float amount;
        public int delay;

        public ToxinDirectDamage(
                ResourceKey<DamageType> damage,
                float amount,
                int delay,
                HazardClass clazz,
                boolean fullBody) {
            super(clazz, fullBody);
            this.damage = damage;
            this.amount = amount;
            this.delay = delay;
        }

        @Override
        public void poison(LivingEntity entity, double intensity) {

            if (isProtected(entity)) return;
            if (!(entity.level() instanceof ServerLevel server)) return;

            if (delay == 0 || server.getGameTime() % delay == 0) {
                Holder<DamageType> type =
                        server.registryAccess()
                                .lookupOrThrow(Registries.DAMAGE_TYPE)
                                .getOrThrow(damage);
                entity.hurtServer(server, new DamageSource(type), (float) (amount * intensity));
            }
        }

        @Override
        public void addInfo(List<String> info) {
            info.add(
                    ChatFormatting.YELLOW
                            + "- "
                            + I18nUtil.resolveKey(clazz.lang)
                            + (fullBody
                                    ? ChatFormatting.RED
                                            + " ("
                                            + I18nUtil.resolveKey("hbmfluid.trait.hazmat")
                                            + ")"
                                    : "")
                            + ": "
                            + ChatFormatting.YELLOW
                            + String.format(
                                    Locale.US,
                                    "%,.1f",
                                    amount * SharedConstants.TICKS_PER_SECOND / delay)
                            + " "
                            + I18nUtil.resolveKey("hbmfluid.trait.perDamage"));
        }
    }

    public static class ToxinEffects extends ToxinEntry {

        public List<MobEffectInstance> effects = new ArrayList<>();

        public ToxinEffects(HazardClass clazz, boolean fullBody) {
            super(clazz, fullBody);
        }

        public ToxinEffects add(MobEffectInstance... effs) {
            Collections.addAll(this.effects, effs);
            return this;
        }

        @Override
        public void poison(LivingEntity entity, double intensity) {

            if (isProtected(entity)) return;

            for (MobEffectInstance eff : effects) {
                entity.addEffect(
                        new MobEffectInstance(
                                eff.getEffect(),
                                (int) (eff.getDuration() * intensity),
                                eff.getAmplifier()));
            }
        }

        @Override
        public void addInfo(List<String> info) {
            info.add(
                    ChatFormatting.YELLOW
                            + "- "
                            + I18nUtil.resolveKey(clazz.lang)
                            + (fullBody
                                    ? ChatFormatting.RED
                                            + " ("
                                            + I18nUtil.resolveKey("hbmfluid.trait.hazmat")
                                            + ")"
                                            + ChatFormatting.YELLOW
                                    : "")
                            + ":");
            for (MobEffectInstance eff : effects) {
                info.add(
                        ChatFormatting.YELLOW
                                + "   - "
                                + eff.getEffect().value().getDisplayName().getString()
                                + (eff.getAmplifier() > 0
                                        ? " "
                                                + I18nUtil.resolveKey(
                                                        "potion.potency." + eff.getAmplifier())
                                        : "")
                                + " "
                                + MobEffectUtil.formatDuration(
                                                eff, 1.0F, SharedConstants.TICKS_PER_SECOND)
                                        .getString());
            }
        }
    }
}
