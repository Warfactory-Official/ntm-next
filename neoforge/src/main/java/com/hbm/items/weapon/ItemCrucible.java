// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: Contributors to Hbm's Nuclear Tech Mod
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.weapon;

import com.hbm.handler.ability.AvailableAbilities;
import com.hbm.handler.threading.TargetPoint;
import com.hbm.items.IAnimatedItem;
import com.hbm.items.ISwingReceiver;
import com.hbm.items.tool.ItemSwordAbility;
import com.hbm.packet.toclient.BlockDustBurstPayload;
import com.hbm.platform.Services;
import com.hbm.render.anim.AnimationEnums.ToolAnimation;
import com.hbm.render.anim.BusAnimation;
import com.hbm.render.anim.BusAnimationSequence;
import com.hbm.render.anim.HbmAnimations;
import com.hbm.sound.ModSounds;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Blocks;
import org.jspecify.annotations.Nullable;

public final class ItemCrucible extends ItemSwordAbility implements IAnimatedItem, ISwingReceiver {

    public static final int CHARGES = 3;
    public static Consumer<Float> PLAY_SWING;

    public ItemCrucible(Properties properties) {
        super(properties, AvailableAbilities.EMPTY);
    }

    @Override
    public boolean canOperate(ItemStack stack) {
        return stack.getDamageValue() < CHARGES;
    }

    public ItemStack depleted() {
        ItemStack stack = new ItemStack(this);
        stack.setDamageValue(CHARGES);
        return stack;
    }

    @Override
    public void onEquip(Player player, ItemStack stack) {
        if (!(player instanceof ServerPlayer server) || !canOperate(stack)) return;
        player.level()
                .playSound(
                        null,
                        player.getX(),
                        player.getY(),
                        player.getZ(),
                        ModSounds.CRUCIBLE_DEPLOY.get(),
                        SoundSource.PLAYERS,
                        1.0F,
                        1.0F);
        playAnimation(server, ToolAnimation.EQUIP);
    }

    @Override
    public void onEntitySwing(ServerPlayer player, ItemStack stack) {
        if (canOperate(stack)) playAnimation(player, ToolAnimation.SWING);
    }

    @Override
    public void hurtEnemy(ItemStack stack, LivingEntity victim, LivingEntity attacker) {
        if (!canOperate(stack)) {
            if (!attacker.level().isClientSide() && attacker instanceof Player player) {
                player.sendSystemMessage(
                        Component.translatable("desc.item.crucible.noEnergy")
                                .withStyle(ChatFormatting.RED));
            }
            return;
        }
        attacker.level()
                .playSound(
                        null,
                        victim.getX(),
                        victim.getY(),
                        victim.getZ(),
                        SoundEvents.ZOMBIE_ATTACK_WOODEN_DOOR,
                        SoundSource.PLAYERS,
                        1.0F,
                        0.75F + victim.getRandom().nextFloat() * 0.2F);
        if (victim.level() instanceof ServerLevel level && !victim.isAlive()) {
            int count = Math.min((int) Math.ceil(victim.getMaxHealth() / 3.0D), 250) * 4;
            double y = victim.getY() + victim.getBbHeight() * 0.5D;
            Services.NETWORK.sendToAllAround(
                    new BlockDustBurstPayload(
                            Blocks.REDSTONE_BLOCK.defaultBlockState(),
                            victim.getX(),
                            y,
                            victim.getZ(),
                            count,
                            0.1D),
                    new TargetPoint(level, victim.getX(), y, victim.getZ(), 50));
        }
    }

    @Override
    public void postHurtEnemy(ItemStack stack, LivingEntity victim, LivingEntity attacker) {
        if (!canOperate(stack)) return;
        if (!(attacker.level() instanceof ServerLevel level)) return;
        ServerPlayer player = attacker instanceof ServerPlayer server ? server : null;
        int wear = stack.processDurabilityChange(1, level, player);
        if (wear == 0) return;
        int damage = Math.min(stack.getDamageValue() + wear, CHARGES);
        if (player != null) CriteriaTriggers.ITEM_DURABILITY_CHANGED.trigger(player, stack, damage);

        stack.setDamageValue(damage);
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13.0F - stack.getDamageValue() * 13.0F / CHARGES);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return Mth.hsvToRgb(
                Math.max(0.0F, 1.0F - stack.getDamageValue() / (float) CHARGES) / 3.0F, 1.0F, 1.0F);
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> adder,
            TooltipFlag flag) {
        StringBuilder charge = new StringBuilder();
        for (int i = 2; i >= 0; i--) charge.append(stack.getDamageValue() <= i ? "||||||" : "   ");
        adder.accept(
                Component.translatable("desc.item.crucible.charge", charge.toString())
                        .withStyle(ChatFormatting.RED));
    }

    @Override
    public @Nullable BusAnimation getAnimation(ToolAnimation type, ItemStack stack) {
        if (type == ToolAnimation.EQUIP) {
            return new BusAnimation()
                    .addBus(
                            "GUARD_ROT",
                            new BusAnimationSequence()
                                    .addPos(90, 0, 1, 0)
                                    .addPos(90, 0, 1, 800)
                                    .addPos(0, 0, 1, 50));
        }
        if (type != ToolAnimation.SWING
                || HbmAnimations.getRelevantTransformation("SWING_ROT")[0] != 0) {
            return null;
        }
        var random = com.hbm.client.ClientPlayerAccess.player().getRandom();
        int offset = random.nextInt(80) - 20;
        PLAY_SWING.accept(0.8F + random.nextFloat() * 0.2F);
        return new BusAnimation()
                .addBus(
                        "SWING_ROT",
                        new BusAnimationSequence()
                                .addPos(90 - offset, 90 - offset, 35, 75)
                                .addPos(90 + offset, 90 - offset, -45, 150)
                                .addPos(0, 0, 0, 500))
                .addBus(
                        "SWING_TRANS",
                        new BusAnimationSequence()
                                .addPos(-3, 0, 0, 75)
                                .addPos(8, 0, 0, 150)
                                .addPos(0, 0, 0, 500));
    }
}
