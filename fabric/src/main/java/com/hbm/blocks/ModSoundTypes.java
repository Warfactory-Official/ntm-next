// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks;

import com.hbm.sound.ModSounds;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.level.block.SoundType;

public final class ModSoundTypes {

    public static final SoundType PIPE = new PipeSoundType();
    public static final SoundType PLATEMETAL = new PlatemetalSoundType();
    public static final SoundType FLESH = new FleshSoundType();

    private ModSoundTypes() {}

    private static final class PlatemetalSoundType extends SoundType {
        private PlatemetalSoundType() {
            super(
                    1.0F,
                    1.0F,
                    SoundType.METAL.getBreakSound(),
                    SoundType.METAL.getStepSound(),
                    SoundType.METAL.getPlaceSound(),
                    SoundType.METAL.getHitSound(),
                    SoundType.METAL.getFallSound());
        }

        @Override
        public SoundEvent getBreakSound() {
            return ModSounds.PLATEMETAL_PLACE.get();
        }

        @Override
        public SoundEvent getPlaceSound() {
            return ModSounds.PLATEMETAL_PLACE.get();
        }

        @Override
        public SoundEvent getStepSound() {
            return ModSounds.PLATEMETAL_STEP.get();
        }

        @Override
        public SoundEvent getHitSound() {
            return ModSounds.PLATEMETAL_STEP.get();
        }

        @Override
        public SoundEvent getFallSound() {
            return ModSounds.PLATEMETAL_STEP.get();
        }
    }

    private static final class FleshSoundType extends SoundType {
        private FleshSoundType() {
            super(
                    0.5F,
                    1.0F,
                    SoundType.WOOL.getBreakSound(),
                    SoundType.WOOL.getStepSound(),
                    SoundType.WOOL.getPlaceSound(),
                    SoundType.WOOL.getHitSound(),
                    SoundType.WOOL.getFallSound());
        }

        @Override
        public SoundEvent getBreakSound() {
            return ModSounds.FLESH.get();
        }

        @Override
        public SoundEvent getPlaceSound() {
            return ModSounds.FLESH.get();
        }

        @Override
        public SoundEvent getStepSound() {
            return ModSounds.FLESH.get();
        }

        @Override
        public SoundEvent getHitSound() {
            return ModSounds.FLESH.get();
        }

        @Override
        public SoundEvent getFallSound() {
            return ModSounds.FLESH.get();
        }
    }

    private static final class PipeSoundType extends SoundType {

        private static final ThreadLocal<Boolean> BREAKING =
                ThreadLocal.withInitial(() -> Boolean.FALSE);

        private PipeSoundType() {
            super(
                    0.85F,
                    0.85F,
                    SoundType.METAL.getBreakSound(),
                    SoundType.METAL.getStepSound(),
                    SoundType.METAL.getPlaceSound(),
                    SoundType.METAL.getHitSound(),
                    SoundType.METAL.getFallSound());
        }

        @Override
        public SoundEvent getBreakSound() {
            BREAKING.set(Boolean.TRUE);
            return ModSounds.PIPE_PLACED.get();
        }

        @Override
        public SoundEvent getPlaceSound() {
            BREAKING.set(Boolean.FALSE);
            return ModSounds.PIPE_PLACED.get();
        }

        @Override
        public SoundEvent getStepSound() {
            BREAKING.set(Boolean.FALSE);
            return super.getStepSound();
        }

        @Override
        public float getPitch() {
            float base = BREAKING.get() ? super.getPitch() - 0.15F : super.getPitch();
            return base + ThreadLocalRandom.current().nextFloat() * 0.2F;
        }
    }
}
