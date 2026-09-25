// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.tool;

import com.hbm.entity.projectile.EntityRubble;
import com.hbm.items.special.ItemCustomLore;
import com.hbm.sound.ModSounds;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public class ItemWeaponSpecial extends ItemCustomLore {

    private static final float CABER_BLAST = 7.5F;
    private static final int CABER_COST = 505;
    private static final float LOUD = 3.0F;

    private final Effect effect;

    public ItemWeaponSpecial(Properties properties, Effect effect) {
        super(properties);
        this.effect = effect;
    }

    @Override
    public void hurtEnemy(ItemStack stack, LivingEntity victim, LivingEntity attacker) {
        if (!(victim.level() instanceof ServerLevel level)) return;

        switch (effect) {
            case INSTANT_KILL -> victim.setHealth(0.0F);
            case HALVE -> victim.setHealth(victim.getHealth() / 2.0F);
            case LAUNCH ->
                    victim.setDeltaMovement(
                            victim.getDeltaMovement().add(attacker.getLookAngle().scale(5)));

            case CABER -> {
                level.explode(
                        null,
                        victim.getX(),
                        victim.getY(),
                        victim.getZ(),
                        CABER_BLAST,
                        Level.ExplosionInteraction.BLOCK);
                stack.hurtAndBreak(CABER_COST, attacker, EquipmentSlot.MAINHAND);
            }
            case SOUND_ONLY, NONE -> {}
        }
        SoundEvent sound = effect.sound();
        if (sound != null) {
            level.playSound(
                    null, victim.blockPosition(), sound, SoundSource.PLAYERS, effect.volume, 1.0F);
        }
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (effect != Effect.LAUNCH) return super.useOn(context);

        Player player = context.getPlayer();
        if (player != null && context.getLevel() instanceof ServerLevel level) {
            BlockPos pos = context.getClickedPos();
            BlockState state = level.getBlockState(pos);

            if (!state.isAir() && state.getBlock().getExplosionResistance() < 6000F) {
                EntityRubble rubble =
                        new EntityRubble(level, pos.getX() + 0.5F, pos.getY(), pos.getZ() + 0.5F);
                rubble.setBlockState(state);
                rubble.setDeltaMovement(player.getLookAngle().scale(5));
                level.playSound(
                        null,
                        rubble.getX(),
                        rubble.getY(),
                        rubble.getZ(),
                        ModSounds.BANG.get(),
                        SoundSource.PLAYERS,
                        3.0F,
                        1.0F);
                level.addFreshEntity(rubble);
                level.destroyBlock(pos, false);
            }
        }

        return InteractionResult.SUCCESS;
    }

    public enum Effect {
        INSTANT_KILL(() -> ModSounds.BONK.get(), LOUD),

        HALVE(() -> ModSounds.SLICE.get(), LOUD),

        LAUNCH(() -> ModSounds.BANG.get(), LOUD),

        CABER(null, 0F),

        SOUND_ONLY(() -> ModSounds.STOP.get(), 1.0F),

        NONE(null, 0F);

        private final @Nullable Supplier<SoundEvent> sound;
        private final float volume;

        Effect(@Nullable Supplier<SoundEvent> sound, float volume) {
            this.sound = sound;
            this.volume = volume;
        }

        @Nullable SoundEvent sound() {
            return sound == null ? null : sound.get();
        }
    }
}
