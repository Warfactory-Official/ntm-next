// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.tool;

import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.explosion.vanillant.standard.EntityProcessorCrossSmooth;
import com.hbm.explosion.vanillant.standard.PlayerProcessorStandard;
import com.hbm.items.special.ItemCustomLore;
import com.hbm.particle.helper.ExplosionCreator;
import com.hbm.sound.ModSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class ItemMemespoon extends ItemCustomLore {

    private static final float CRIT_FALL = 2F, BLAST_FALL = 20F;
    private static final float CRIT_DAMAGE = 50F;

    private static final float BLAST = 15F;
    private static final int PIERCE = 25;

    public ItemMemespoon(Properties properties) {
        super(properties);
    }

    @Override
    public void hurtEnemy(ItemStack stack, LivingEntity victim, LivingEntity attacker) {
        if (!(victim.level() instanceof ServerLevel level) || !(attacker instanceof Player player))
            return;
        if (attacker.fallDistance < CRIT_FALL) return;

        level.playSound(
                null,
                victim.blockPosition(),
                ModSounds.BANG.get(),
                SoundSource.PLAYERS,
                3.0F,
                0.75F);
        victim.hurtServer(level, level.damageSources().playerAttack(player), CRIT_DAMAGE);

        if (attacker.fallDistance < BLAST_FALL || player.hasInfiniteMaterials()) return;
        ExplosionVNT blast =
                new ExplosionVNT(
                        level,
                        victim.getX(),
                        victim.getY() + victim.getBbHeight() / 2D,
                        victim.getZ(),
                        BLAST,
                        attacker);
        blast.setEntityProcessor(
                new EntityProcessorCrossSmooth(1, 150).setupPiercing(PIERCE, 0.5F));
        blast.setPlayerProcessor(new PlayerProcessorStandard());
        ExplosionCreator.composeEffectSmall(
                level, victim.getX(), victim.getY() + victim.getBbHeight() / 2D, victim.getZ());
        blast.explode();
    }
}
