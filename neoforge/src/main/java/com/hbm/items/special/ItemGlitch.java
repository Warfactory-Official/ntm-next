// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.special;

import com.hbm.api.energymk2.IBatteryItem;
import com.hbm.blocks.ModBlocks;
import com.hbm.entity.ModEntities;
import com.hbm.entity.effect.EntityVortex;
import com.hbm.entity.projectile.EntityBoxcar;
import com.hbm.entity.projectile.EntityMeteor;
import com.hbm.explosion.ExplosionChaos;
import com.hbm.explosion.ExplosionLarge;
import com.hbm.inventory.material.Mats;
import com.hbm.items.ModItems;
import com.hbm.items.weapon.sedna.factory.GunFactory.EnumAmmo;
import com.hbm.lib.ModDamageTypes;
import com.hbm.main.Polaroid;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

public class ItemGlitch extends Item implements IBatteryItem {

    private static final int USE_COST = 5;
    private static final int OUTCOMES = 31;
    private static final int BUFF_TICKS = 60 * 20;
    private static final int BUFF_AMPLIFIER = 9;
    private static final long CHARGE = 200;

    public ItemGlitch(Properties props) {
        super(props);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!(level instanceof ServerLevel server)) return InteractionResult.SUCCESS;
        int outcome = server.getRandom().nextInt(OUTCOMES);
        for (ItemStack gift : gifts(outcome)) player.getInventory().placeItemBackInInventory(gift);
        player.getItemInHand(hand).hurtAndBreak(USE_COST, player, hand);
        roll(server, player, outcome);
        return InteractionResult.SUCCESS;
    }

    private static List<ItemStack> gifts(int outcome) {
        return switch (outcome) {
            case 5 -> List.of(treasure(1));
            case 6 -> List.of(treasure(3));
            case 7 -> List.of(treasure(10));
            case 8 -> List.of(new ItemStack(ModItems.AMMO_CONTAINER.get(), 10));
            case 9 -> List.of(new ItemStack(ModItems.NUKE_ADVANCED_KIT.get()));
            case 10 -> List.of(new ItemStack(ModItems.NUKE_STARTER_KIT.get()));
            case 13 ->
                    List.of(
                            new ItemStack(ModItems.GUN_HEAVY_REVOLVER_LILMAC.get()),
                            new ItemStack(ModItems.BOTTLE_SPARKLE.get()),
                            new ItemStack(ModItems.GEIGER_COUNTER.get()));
            case 15 -> {
                List<ItemStack> dirt = new ArrayList<>();
                for (int i = 0; i < 36; i++) dirt.add(new ItemStack(Items.DIRT, 64));
                yield dirt;
            }
            case 18 ->
                    List.of(
                            new ItemStack(ModItems.GUN_MARESLEG.get()),
                            ModItems.AMMO_STANDARD.stack(EnumAmmo.G12, 12));
            case 21 -> List.of(new ItemStack(ModItems.MISSILE_NUCLEAR.get()));
            case 30 -> List.of(new ItemStack(ModItems.plate(Mats.MAT_SATURN)));
            default -> List.of();
        };
    }

    private void roll(ServerLevel level, Player player, int outcome) {
        switch (outcome) {
            case 2 ->
                    player.hurtServer(
                            level, level.damageSources().source(ModDamageTypes.RADIATION), 1000);
            case 3 ->
                    player.hurtServer(
                            level, level.damageSources().source(ModDamageTypes.BOXCAR), 1000);
            case 4 ->
                    player.hurtServer(
                            level, level.damageSources().source(ModDamageTypes.BLACKHOLE), 1000);
            case 11 -> boxcar(level, player, 0);
            case 12 -> {
                for (int i = 0; i < 10; i++) boxcar(level, player, 25);
            }
            case 14 -> {
                player.getInventory().dropAll();
                ExplosionChaos.igniteAllBlocks(
                        level, player.getBlockX(), player.getBlockY(), player.getBlockZ(), 5);
            }
            case 24 ->
                    player.addEffect(
                            new MobEffectInstance(
                                    MobEffects.RESISTANCE, BUFF_TICKS, BUFF_AMPLIFIER));
            case 25 ->
                    player.addEffect(
                            new MobEffectInstance(MobEffects.STRENGTH, BUFF_TICKS, BUFF_AMPLIFIER));
            case 26 ->
                    player.addEffect(
                            new MobEffectInstance(MobEffects.SLOWNESS, BUFF_TICKS, BUFF_AMPLIFIER));
            case 27 -> {
                EntityVortex vortex = new EntityVortex(level, 2.5F);
                vortex.setPos(player.getX(), player.getY() - 15, player.getZ());
                level.addFreshEntity(vortex);
            }
            case 28 -> {
                EntityMeteor meteor = new EntityMeteor(level);
                meteor.setPos(player.getX(), player.getY() + 100, player.getZ());
                level.addFreshEntity(meteor);
            }
            case 29 ->
                    ExplosionLarge.spawnBurst(
                            level, player.getX(), player.getY(), player.getZ(), 27, 3);
            default -> {}
        }
        say(player, outcome);
    }

    private static void say(Player player, int outcome) {
        String key = "chat.glitch." + outcome;
        Component line = Component.translatable(key);
        if (line.getString().equals(key)) return;
        player.sendSystemMessage(line);
    }

    private static ItemStack treasure(int count) {
        return new ItemStack(ModBlocks.BLOCK_METEOR_TREASURE.get(), count);
    }

    private static void boxcar(ServerLevel level, Player player, double spread) {
        EntityBoxcar boxcar = new EntityBoxcar(ModEntities.BOXCAR.get(), level);
        RandomSource rand = level.getRandom();
        boxcar.setPos(
                player.getX() + (spread == 0 ? 0 : rand.nextGaussian() * spread),
                player.getY() + 50,
                player.getZ() + (spread == 0 ? 0 : rand.nextGaussian() * spread));
        level.addFreshEntity(boxcar);
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> adder,
            TooltipFlag flag) {
        adder.accept(Component.translatable("desc.item.glitch"));
        adder.accept(Component.empty());
        for (String line : Polaroid.loreLines("desc.item.glitch."))
            adder.accept(Component.literal(line));
    }

    @Override
    public void chargeBattery(ItemStack stack, long amount) {}

    @Override
    public void setCharge(ItemStack stack, long charge) {}

    @Override
    public void dischargeBattery(ItemStack stack, long amount) {}

    @Override
    public long getCharge(ItemStack stack) {
        return CHARGE;
    }

    @Override
    public long getMaxCharge(ItemStack stack) {
        return CHARGE;
    }

    @Override
    public long getChargeRate(ItemStack stack) {
        return 0;
    }

    @Override
    public long getDischargeRate(ItemStack stack) {
        return CHARGE;
    }
}
