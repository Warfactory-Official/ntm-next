// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: HbmMods (The Bobcat)
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.weapon.sedna.impl;

import com.hbm.config.GunVisualConfig;
import com.hbm.entity.ModEntities;
import com.hbm.entity.projectile.EntityChemical;
import com.hbm.inventory.fluid.FluidStackNTM;
import com.hbm.items.machine.IFluidContainerItem;
import com.hbm.items.weapon.sedna.GunConfig;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.items.weapon.sedna.Receiver;
import com.hbm.items.weapon.sedna.mags.IMagazine;
import com.hbm.items.weapon.sedna.mags.MagazineFluid;
import com.hbm.render.anim.AnimationEnums.GunAnimation;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.Vec3;

public class ItemGunChemthrower extends ItemGunBaseNT implements IFluidContainerItem {

    public static final int CONSUMPTION = 3;
    public static final int transferSpeed = 50;
    public static BiFunction<ItemStack, LambdaContext, Boolean> LAMBDA_CAN_FIRE =
            (stack, ctx) -> {
                return ctx.config()
                                .getReceivers(stack)[0]
                                .getMagazine(stack)
                                .getAmount(stack, ctx.inventory())
                        >= CONSUMPTION;
            };
    public static BiConsumer<ItemStack, LambdaContext> LAMBDA_FIRE =
            (stack, ctx) -> {
                LivingEntity entity = ctx.entity();
                Player player = ctx.getPlayer();
                int index = ctx.configIndex();
                ItemGunBaseNT.playAnimation(player, stack, GunAnimation.CYCLE, ctx.configIndex());

                Receiver primary = ctx.config().getReceivers(stack)[0];
                IMagazine mag = primary.getMagazine(stack);

                Vec3 offset = primary.getProjectileOffset(stack);
                double forwardOffset = offset.x;
                double heightOffset = offset.y;
                double sideOffset = offset.z;

                EntityChemical chem =
                        new EntityChemical(
                                ModEntities.CHEMICAL.get(),
                                entity.level(),
                                entity,
                                sideOffset,
                                heightOffset,
                                forwardOffset);
                chem.setFluid((Fluid) mag.getType(stack, ctx.inventory()));
                entity.level().addFreshEntity(chem);

                mag.useUpAmmo(stack, ctx.inventory(), CONSUMPTION);
                ItemGunBaseNT.setWear(
                        stack,
                        index,
                        Math.min(
                                ItemGunBaseNT.getWear(stack, index) + 1F,
                                ctx.config().getDurability(stack)));
            };

    public ItemGunChemthrower(WeaponQuality quality, Properties properties, GunConfig... cfg) {
        super(quality, properties, cfg);
    }

    public static int getMagType(ItemStack stack) {
        return ItemGunBaseNT.getValueInt(stack, MagazineFluid.KEY_MAG_TYPE + 0);
    }

    public static void setMagType(ItemStack stack, int value) {
        ItemGunBaseNT.setValueInt(stack, MagazineFluid.KEY_MAG_TYPE + 0, value);
    }

    public static int getMagCount(ItemStack stack) {
        return ItemGunBaseNT.getValueInt(stack, MagazineFluid.KEY_MAG_COUNT + 0);
    }

    public static void setMagCount(ItemStack stack, int value) {
        ItemGunBaseNT.setValueInt(stack, MagazineFluid.KEY_MAG_COUNT + 0, value);
    }

    public Fluid getFluidType(ItemStack stack) {
        return BuiltInRegistries.FLUID.byId(getMagType(stack));
    }

    @Override
    public boolean machineFillable() {
        return true;
    }

    @Override
    public Fluid firstFluidType(ItemStack stack) {
        return getFluidType(stack);
    }

    @Override
    public int capacity(ItemStack stack) {
        return this.getConfig(stack, 0)
                .getReceivers(stack)[0]
                .getMagazine(stack)
                .getCapacity(stack);
    }

    @Override
    public FluidStackNTM getContent(ItemStack stack) {
        int fill = getMagCount(stack);
        if (fill <= 0) return EMPTY_CONTENT;
        return new FluidStackNTM(getFluidType(stack), fill, 0);
    }

    @Override
    public void setContent(ItemStack stack, FluidStackNTM content) {
        setMagType(stack, BuiltInRegistries.FLUID.getId(content.type()));
        setMagCount(stack, (int) Math.min(content.amount(), capacity(stack)));
    }

    @Override
    public int fill(ItemStack stack, Fluid type, int amount, int pressure) {
        if (getFluidType(stack) != type && getMagCount(stack) != 0) return 0;
        if (getMagCount(stack) == 0) setMagType(stack, BuiltInRegistries.FLUID.getId(type));

        int fill = getMagCount(stack);
        int req = capacity(stack) - fill;
        int toFill = Math.min(amount, req);
        toFill = Math.min(toFill, transferSpeed);
        setMagCount(stack, fill + toFill);
        return toFill;
    }

    @Override
    public int drain(ItemStack stack, Fluid type, int amount, int pressure) {
        if (getFluidType(stack) != type) return 0;
        int fill = getMagCount(stack);
        int toUnload = Math.min(fill, amount);
        toUnload = Math.min(toUnload, transferSpeed);
        setMagCount(stack, fill - toUnload);
        return toUnload;
    }
}
