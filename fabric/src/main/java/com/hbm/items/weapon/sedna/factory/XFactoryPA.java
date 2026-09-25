// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: HbmMods (The Bobcat)
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.weapon.sedna.factory;

import com.hbm.config.GunVisualConfig;
import com.hbm.items.ModItems;
import com.hbm.items.armor.IPAMelee;
import com.hbm.items.armor.IPARanged;
import com.hbm.items.armor.IPAWeaponsProvider;
import com.hbm.items.weapon.sedna.Crosshair;
import com.hbm.items.weapon.sedna.GunConfig;
import com.hbm.items.weapon.sedna.ItemGunBaseNT.GunState;
import com.hbm.items.weapon.sedna.ItemGunBaseNT.LambdaContext;
import com.hbm.items.weapon.sedna.ItemGunBaseNT.WeaponQuality;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.items.weapon.sedna.Receiver;
import com.hbm.registration.IRegistrar;
import com.hbm.registration.Reg;
import com.hbm.render.anim.AnimationEnums.GunAnimation;
import com.hbm.render.anim.BusAnimation;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

public class XFactoryPA {

    public static BiConsumer<ItemStack, LambdaContext> ORCHESTRA =
            (stack, ctx) -> {
                IPAMelee component = IPAWeaponsProvider.getMeleeComponentCommon(ctx.getPlayer());
                if (component != null) component.orchestra(stack, ctx);
            };
    public static BiFunction<ItemStack, GunAnimation, BusAnimation> LAMBDA_MELEE_ANIMS =
            (stack, type) -> {
                IPAMelee component = IPAWeaponsProvider.getMeleeComponentClient();
                if (component != null) return component.playAnim(stack, type);
                return null;
            };
    public static BiConsumer<ItemStack, LambdaContext> LAMBDA_CLICK_MELEE_PRIMARY =
            (stack, ctx) -> {
                IPAMelee component = IPAWeaponsProvider.getMeleeComponentCommon(ctx.getPlayer());
                if (component != null) component.clickPrimary(stack, ctx);
            };
    public static BiConsumer<ItemStack, LambdaContext> LAMBDA_CLICK_MELEE_SENONDARY =
            (stack, ctx) -> {
                IPAMelee component = IPAWeaponsProvider.getMeleeComponentCommon(ctx.getPlayer());
                if (component != null) component.clickSecondary(stack, ctx);
            };
    public static BiConsumer<ItemStack, LambdaContext> LAMBDA_CLICK_RANGED_PRIMARY =
            (stack, ctx) -> {
                IPARanged component = IPAWeaponsProvider.getRangedComponentCommon(ctx.getPlayer());
                if (component != null) component.clickPrimary(stack, ctx);
            };
    public static BiConsumer<ItemStack, LambdaContext> LAMBDA_CLICK_RANGED_SENONDARY =
            (stack, ctx) -> {
                IPARanged component = IPAWeaponsProvider.getRangedComponentCommon(ctx.getPlayer());
                if (component != null) component.clickSecondary(stack, ctx);
            };

    public static void init(IRegistrar r) {

        ModItems.GUN_PA_MELEE =
                r.registerItem(
                        "gun_pa_melee",
                        props ->
                                new ItemGunPA(
                                        WeaponQuality.UTILITY,
                                        props,
                                        new GunConfig()
                                                .draw(10)
                                                .crosshair(Crosshair.NONE)
                                                .rec(new Receiver(0))
                                                .pp(LAMBDA_CLICK_MELEE_PRIMARY)
                                                .ps(LAMBDA_CLICK_MELEE_SENONDARY)
                                                .decider(GunStateDecider.LAMBDA_STANDARD_DECIDER)
                                                .anim(LAMBDA_MELEE_ANIMS)
                                                .orchestra(ORCHESTRA)),
                        Item.Properties::new);

        ModItems.GUN_PA_RANGED =
                Reg.item(
                        "gun_pa_ranged",
                        props ->
                                new ItemGunPA(
                                        WeaponQuality.UTILITY,
                                        props,
                                        new GunConfig()
                                                .draw(0)
                                                .crosshair(Crosshair.CROSS)
                                                .rec(new Receiver(0))
                                                .pp(LAMBDA_CLICK_RANGED_PRIMARY)
                                                .ps(LAMBDA_CLICK_RANGED_SENONDARY)
                                                .decider(GunStateDecider.LAMBDA_STANDARD_DECIDER)),
                        Item.Properties::new);
    }

    public static void doSwing(
            ItemStack stack, LambdaContext ctx, GunAnimation anim, int cooldown) {

        Player player = ctx.getPlayer();
        int index = ctx.configIndex();
        GunState state = ItemGunBaseNT.getState(stack, index);

        if (state == GunState.IDLE) {
            ItemGunBaseNT.playAnimation(player, stack, anim, ctx.configIndex());
            ItemGunBaseNT.setState(stack, index, GunState.COOLDOWN);
            ItemGunBaseNT.setTimer(stack, index, cooldown);
        }
    }

    public static class ItemGunPA extends ItemGunBaseNT {

        public ItemGunPA(WeaponQuality quality, Properties properties, GunConfig... cfg) {
            super(quality, properties, cfg);
        }

        @Override
        public void appendHoverText(
                ItemStack stack,
                TooltipContext context,
                TooltipDisplay display,
                Consumer<Component> list,
                TooltipFlag ext) {}
    }
}
