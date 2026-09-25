// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.armor;

import com.hbm.registration.RegistryHandle;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.effect.MobEffect;
import org.jspecify.annotations.Nullable;

public final class ArmorFullSetBonus {

    private static final Map<ModArmorItem.Suit, ArmorFullSetBonus> BY_SUIT =
            new EnumMap<>(ModArmorItem.Suit.class);

    private final List<EffectBonus> effects;
    private final boolean geigerSound;
    private final boolean hardLanding;

    private final boolean vats;
    private final boolean thermal;
    private final @Nullable RegistryHandle<SoundEvent> step;
    private final @Nullable RegistryHandle<SoundEvent> jump;
    private final @Nullable RegistryHandle<SoundEvent> fall;

    private final long drain;

    private final boolean noHelmet;

    private final boolean rocketBoots;
    private final boolean fastFall;
    private final boolean sprintBoost;

    private final boolean moreAmmo;

    private final int dashCount;

    private final boolean geigerHUD;

    private final int stepSize;

    private ArmorFullSetBonus(Builder b) {
        this.effects = List.copyOf(b.effects);
        this.geigerSound = b.geigerSound;
        this.hardLanding = b.hardLanding;
        this.vats = b.vats;
        this.thermal = b.thermal;
        this.step = b.step;
        this.jump = b.jump;
        this.fall = b.fall;
        this.drain = b.drain;
        this.noHelmet = b.noHelmet;
        this.rocketBoots = b.rocketBoots;
        this.fastFall = b.fastFall;
        this.sprintBoost = b.sprintBoost;
        this.moreAmmo = b.moreAmmo;
        this.dashCount = b.dashCount;
        this.geigerHUD = b.geigerHUD;
        this.stepSize = b.stepSize;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static void register(ModArmorItem.Suit suit, ArmorFullSetBonus bonus) {
        BY_SUIT.put(suit, bonus);
    }

    public static @Nullable ArmorFullSetBonus get(ModArmorItem.Suit suit) {
        return BY_SUIT.get(suit);
    }

    public List<EffectBonus> effects() {
        return effects;
    }

    public boolean geigerSound() {
        return geigerSound;
    }

    public boolean hardLanding() {
        return hardLanding;
    }

    public boolean vats() {
        return vats;
    }

    public boolean thermal() {
        return thermal;
    }

    public @Nullable SoundEvent step() {
        return step == null ? null : step.get();
    }

    public @Nullable SoundEvent jump() {
        return jump == null ? null : jump.get();
    }

    public @Nullable SoundEvent fall() {
        return fall == null ? null : fall.get();
    }

    public long drain() {
        return drain;
    }

    public boolean noHelmet() {
        return noHelmet;
    }

    public boolean rocketBoots() {
        return rocketBoots;
    }

    public boolean fastFall() {
        return fastFall;
    }

    public boolean sprintBoost() {
        return sprintBoost;
    }

    public boolean moreAmmo() {
        return moreAmmo;
    }

    public int dashCount() {
        return dashCount;
    }

    public boolean geigerHUD() {
        return geigerHUD;
    }

    public int stepSize() {
        return stepSize;
    }

    public record EffectBonus(Holder<MobEffect> effect, int amplifier) {}

    public static final class Builder {
        private final List<EffectBonus> effects = new ArrayList<>();
        private boolean geigerSound;
        private boolean hardLanding;
        private boolean vats;
        private boolean thermal;
        private @Nullable RegistryHandle<SoundEvent> step;
        private @Nullable RegistryHandle<SoundEvent> jump;
        private @Nullable RegistryHandle<SoundEvent> fall;
        private long drain;
        private boolean noHelmet;
        private boolean rocketBoots;
        private boolean fastFall;
        private boolean sprintBoost;
        private boolean moreAmmo;
        private int dashCount;
        private boolean geigerHUD;
        private int stepSize;

        private Builder() {}

        public Builder noHelmet() {
            this.noHelmet = true;
            return this;
        }

        public Builder rocketBoots() {
            this.rocketBoots = true;
            return this;
        }

        public Builder fastFall() {
            this.fastFall = true;
            return this;
        }

        public Builder sprintBoost() {
            this.sprintBoost = true;
            return this;
        }

        public Builder moreAmmo() {
            this.moreAmmo = true;
            return this;
        }

        public Builder dashCount(int dashCount) {
            this.dashCount = dashCount;
            return this;
        }

        public Builder geigerHUD() {
            this.geigerHUD = true;
            return this;
        }

        public Builder stepSize(int stepSize) {
            this.stepSize = stepSize;
            return this;
        }

        public Builder effect(Holder<MobEffect> effect, int amplifier) {
            effects.add(new EffectBonus(effect, amplifier));
            return this;
        }

        public Builder geigerSound() {
            this.geigerSound = true;
            return this;
        }

        public Builder hardLanding() {
            this.hardLanding = true;
            return this;
        }

        public Builder vats() {
            this.vats = true;
            return this;
        }

        public Builder thermal() {
            this.thermal = true;
            return this;
        }

        public Builder steps(
                RegistryHandle<SoundEvent> step,
                RegistryHandle<SoundEvent> jump,
                RegistryHandle<SoundEvent> fall) {
            this.step = step;
            this.jump = jump;
            this.fall = fall;
            return this;
        }

        public Builder drain(long drain) {
            this.drain = drain;
            return this;
        }

        public ArmorFullSetBonus build() {
            return new ArmorFullSetBonus(this);
        }
    }
}
