// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.armor;

import com.hbm.entity.projectile.EntityBulletBaseMK4;
import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.items.weapon.sedna.ItemGunBaseNT.GunState;
import com.hbm.items.weapon.sedna.ItemGunBaseNT.LambdaContext;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.items.weapon.sedna.factory.XFactoryRocket;
import com.hbm.items.weapon.sedna.mags.MagazineBelt;
import com.hbm.sound.ModSounds;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class ArmorNCRPARanged implements IPARanged {

    public static final MagazineBelt rocketSteerMag = new MagazineBelt();
    public static final MagazineBelt rocketMag = new MagazineBelt();

    private static final float DAMAGE = 25F;
    private static final float SIDE_OFFSET = 0.25F;
    private static final int COOLDOWN = 10;

    @Override
    public void clickPrimary(ItemStack stack, LambdaContext ctx) {
        fireRocket(stack, ctx, true);
    }

    @Override
    public void clickSecondary(ItemStack stack, LambdaContext ctx) {
        fireRocket(stack, ctx, false);
    }

    public static void fireRocket(ItemStack stack, LambdaContext ctx, boolean steer) {
        Player player = ctx.getPlayer();
        if (player == null) return;
        if (ItemGunBaseNT.getState(stack, 0) != GunState.IDLE) return;

        MagazineBelt mag = steer ? rocketSteerMag : rocketMag;
        if (mag.acceptedBullets.isEmpty()) {
            mag.addConfigs(steer ? XFactoryRocket.rocket_ncrpa_steer : XFactoryRocket.rocket_ncrpa);
        }

        BulletConfig cfg = mag.getType(stack, player.getInventory());
        int amount = mag.getAmount(stack, player.getInventory());

        ItemGunBaseNT.setState(stack, 0, GunState.COOLDOWN);
        ItemGunBaseNT.setTimer(stack, 0, COOLDOWN);

        if (amount <= 0) {
            player.level()
                    .playSound(
                            null,
                            player.getX(),
                            player.getY(),
                            player.getZ(),
                            ModSounds.GUN_DRY_FIRE.get(),
                            SoundSource.PLAYERS,
                            1F,
                            1F);
            return;
        }

        mag.useUpAmmo(stack, player.getInventory(), 1);
        float side = SIDE_OFFSET * (player.getRandom().nextBoolean() ? -1F : 1F);
        EntityBulletBaseMK4 rocket = new EntityBulletBaseMK4(player, cfg, DAMAGE, 0F, side, 0D, 0D);
        player.level().addFreshEntity(rocket);
        player.level()
                .playSound(
                        null,
                        player.getX(),
                        player.getY(),
                        player.getZ(),
                        ModSounds.GUN_ROCKET_FIRE.get(),
                        SoundSource.PLAYERS,
                        0.5F,
                        0.9F + player.getRandom().nextFloat() * 0.2F);
        player.inventoryMenu.broadcastChanges();
    }
}
