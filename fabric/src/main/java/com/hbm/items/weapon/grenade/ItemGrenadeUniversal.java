// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.weapon.grenade;

import com.hbm.client.ClientPlayerAccess;
import com.hbm.entity.grenade.EntityGrenadeUniversal;
import com.hbm.extprop.HbmPlayerProps;
import com.hbm.itempool.ItemPool;
import com.hbm.items.ModDataComponents;
import com.hbm.items.ModItems;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.packet.toclient.GrenadeAnimationPayload;
import com.hbm.platform.Services;
import com.hbm.sound.ModSounds;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

public class ItemGrenadeUniversal extends Item {

    private static int clientSlot = -1;
    private static @Nullable GrenadeData clientData;

    public ItemGrenadeUniversal(Properties properties) {
        super(properties);
    }

    private static int deployment(Player player) {
        return HbmPlayerProps.getData(player).grenadeDeployment;
    }

    private static void onEquip(ServerPlayer player, ItemStack stack) {
        HbmPlayerProps.getData(player).grenadeDeployment = 0;
        Services.NETWORK.sendTo(
                new GrenadeAnimationPayload(getData(stack).shell().ordinal()), player);
    }

    public static void clientHeldTick() {
        Player player = ClientPlayerAccess.player();
        if (player == null) return;
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof ItemGrenadeUniversal)) {
            clientSlot = -1;
            return;
        }
        int slot = player.getInventory().getSelectedSlot();
        GrenadeData data = getData(stack);
        HbmPlayerProps props = HbmPlayerProps.getData(player);
        if (slot != clientSlot || !data.equals(clientData)) {
            clientSlot = slot;
            clientData = data;
            props.grenadeDeployment = 0;
        } else {
            props.grenadeDeployment++;
        }
    }

    public static GrenadeData getData(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.GRENADE.get(), GrenadeData.DEFAULT);
    }

    public static ItemStack make(
            ItemGrenadeShell.EnumGrenadeShell shell,
            ItemGrenadeFilling.EnumGrenadeFilling filling,
            ItemGrenadeFuze.EnumGrenadeFuze fuze) {
        return make(shell, filling, fuze, null, 1);
    }

    public static ItemStack make(
            ItemGrenadeShell.EnumGrenadeShell shell,
            ItemGrenadeFilling.EnumGrenadeFilling filling,
            ItemGrenadeFuze.EnumGrenadeFuze fuze,
            ItemGrenadeExtra.@Nullable EnumGrenadeExtra extra) {
        return make(shell, filling, fuze, extra, 1);
    }

    public static ItemStack make(
            ItemGrenadeShell.EnumGrenadeShell shell,
            ItemGrenadeFilling.EnumGrenadeFilling filling,
            ItemGrenadeFuze.EnumGrenadeFuze fuze,
            ItemGrenadeExtra.@Nullable EnumGrenadeExtra extra,
            int amount) {
        ItemStack stack = new ItemStack(ModItems.GRENADE_UNIVERSAL.get(), amount);
        stack.set(ModDataComponents.GRENADE.get(), new GrenadeData(shell, filling, fuze, extra));
        stack.set(DataComponents.MAX_STACK_SIZE, shell.getStackLimit());
        return stack;
    }

    public static ItemPool.Entry poolEntry(
            ItemGrenadeShell.EnumGrenadeShell shell,
            ItemGrenadeFilling.EnumGrenadeFilling filling,
            ItemGrenadeFuze.EnumGrenadeFuze fuze,
            ItemGrenadeExtra.@Nullable EnumGrenadeExtra extra,
            int min,
            int max,
            int weight) {
        return new ItemPool.Entry(
                ModItems.GRENADE_UNIVERSAL.get().builtInRegistryHolder(),
                DataComponentPatch.builder()
                        .set(
                                ModDataComponents.GRENADE.get(),
                                new GrenadeData(shell, filling, fuze, extra))
                        .set(DataComponents.MAX_STACK_SIZE, shell.getStackLimit())
                        .build(),
                Math.min(min, max),
                Math.max(min, max),
                weight);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (deployment(player) < getData(stack).shell().getDrawDuration())
            return InteractionResult.PASS;
        if (!level.isClientSide())
            level.addFreshEntity(new EntityGrenadeUniversal(level, player, stack));
        stack.consume(1, player);
        if (!stack.isEmpty()) {
            if (player instanceof ServerPlayer server) onEquip(server, stack);
            else HbmPlayerProps.getData(player).grenadeDeployment = 0;
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public void inventoryTick(
            ItemStack stack, ServerLevel level, Entity owner, @Nullable EquipmentSlot slot) {
        boolean isHeld = slot == EquipmentSlot.MAINHAND;
        boolean wasHeld = ItemGunBaseNT.getIsEquipped(stack);
        if (wasHeld != isHeld) ItemGunBaseNT.setIsEquipped(stack, isHeld);
        if (!(owner instanceof ServerPlayer player) || !isHeld) return;
        if (!wasHeld) {
            onEquip(player, stack);
            return;
        }
        int deployed = HbmPlayerProps.getData(player).grenadeDeployment + 1;
        HbmPlayerProps.getData(player).grenadeDeployment = deployed;
        switch (getData(stack).shell()) {
            case FRAG -> {
                if (deployed == 18)
                    level.playSound(
                            null,
                            player.getX(),
                            player.getY(),
                            player.getZ(),
                            ModSounds.GUN_REVOLVER_COCK.get(),
                            SoundSource.PLAYERS,
                            1F,
                            1F);
            }
            case STICK -> {
                if (deployed == 16 || deployed == 25)
                    level.playSound(
                            null,
                            player.getX(),
                            player.getY(),
                            player.getZ(),
                            ModSounds.GUN_BOLT_OPEN.get(),
                            SoundSource.PLAYERS,
                            1F,
                            1.25F);
            }
            case TECH -> {
                if (deployed == 18)
                    level.playSound(
                            null,
                            player.getX(),
                            player.getY(),
                            player.getZ(),
                            ModSounds.GRENADE_TECH.get(),
                            SoundSource.PLAYERS,
                            1F,
                            1F);
            }
            case NUKE -> {
                if (deployed == 26)
                    level.playSound(
                            null,
                            player.getX(),
                            player.getY(),
                            player.getZ(),
                            ModSounds.GRENADE_NUKA.get(),
                            SoundSource.PLAYERS,
                            1F,
                            1F);
            }
        }
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> adder,
            TooltipFlag flag) {
        GrenadeData data = getData(stack);
        adder.accept(
                Component.translatable(ModItems.GRENADE_SHELL.get(data.shell()).getDescriptionId())
                        .withStyle(ChatFormatting.YELLOW));
        adder.accept(
                Component.translatable(
                                ModItems.GRENADE_FILLING.get(data.filling()).getDescriptionId())
                        .withStyle(ChatFormatting.YELLOW));
        adder.accept(
                Component.translatable(ModItems.GRENADE_FUZE.get(data.fuze()).getDescriptionId())
                        .withStyle(ChatFormatting.YELLOW));
        if (data.extra() != null) {
            adder.accept(
                    Component.translatable(
                                    ModItems.GRENADE_EXTRA.get(data.extra()).getDescriptionId())
                            .withStyle(ChatFormatting.RED));
        }
    }
}
