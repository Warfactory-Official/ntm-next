// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: HbmMods (The Bobcat)
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.tool;

import com.hbm.entity.mob.glyphid.EntityGlyphidNuclear;
import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.explosion.vanillant.standard.EntityProcessorCrossSmooth;
import com.hbm.explosion.vanillant.standard.ExplosionEffectWeapon;
import com.hbm.explosion.vanillant.standard.PlayerProcessorStandard;
import com.hbm.interfaces.IToolable;
import com.hbm.items.ModItems;
import com.hbm.items.armor.ItemModDefuser;
import com.hbm.items.weapon.sedna.factory.ConfettiUtil;
import com.hbm.items.weapon.sedna.factory.GunFactory.EnumAmmo;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class ItemDefuser extends ItemTooling {

    public ItemDefuser(Item.Properties properties, IToolable.ToolType type) {
        super(properties, type);
    }

    @Override
    public InteractionResult interactLivingEntity(
            ItemStack stack, Player player, LivingEntity entity, InteractionHand hand) {

        if (entity instanceof Creeper creeper) {
            return ItemModDefuser.castrateCreeper(creeper, player, true)
                    ? InteractionResult.SUCCESS
                    : InteractionResult.PASS;
        }

        if (entity instanceof EntityGlyphidNuclear john) {

            if (!player.level().isClientSide()
                    && john.deathTicks > 0
                    && john.level() instanceof ServerLevel server) {
                john.discard();

                ExplosionVNT vnt =
                        new ExplosionVNT(server, john.getX(), john.getY(), john.getZ(), 5F, john);
                vnt.setEntityProcessor(
                        new EntityProcessorCrossSmooth(1, 20).setupPiercing(10F, 0.2F));
                vnt.setPlayerProcessor(new PlayerProcessorStandard());
                vnt.setSFX(new ExplosionEffectWeapon(10, 2.5F, 1F));
                vnt.explode();

                ConfettiUtil.gib(john);

                if (ModItems.AMMO_STANDARD != null) {
                    john.spawnAtLocation(
                            server, ModItems.AMMO_STANDARD.stack(EnumAmmo.NUKE_DEMO), 1.5F);
                }
            }

            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }
}
