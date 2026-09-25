// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.tool;

import com.hbm.items.IItemControlReceiver;
import com.hbm.items.ModDataComponents;
import com.hbm.items.machine.ItemSatelliteChip;
import com.hbm.saveddata.SatelliteSavedData;
import com.hbm.saveddata.satellites.Satellite;
import java.util.function.Consumer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

public class ItemSatelliteInterface extends ItemSatelliteChip implements IItemControlReceiver {

    public static final String KEY_CONNECTED = "connected";

    public static Consumer<Player> OPEN_SCREEN = player -> {};

    public ItemSatelliteInterface(Properties properties) {
        super(properties);
    }

    @Override
    public void receiveControl(ServerPlayer player, ItemStack stack, CompoundTag data) {
        Satellite satellite = SatelliteSavedData.get(player.level()).getSatFromFreq(getFreq(stack));
        if (satellite != null) {
            satellite.onCoordAction(
                    player.level(),
                    player,
                    data.getIntOr("x", 0),
                    data.getIntOr("y", -1),
                    data.getIntOr("z", 0));
        }
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) OPEN_SCREEN.accept(player);
        return InteractionResult.CONSUME;
    }

    @Override
    public void inventoryTick(
            ItemStack stack, ServerLevel level, Entity entity, @Nullable EquipmentSlot slot) {
        if (!(entity instanceof ServerPlayer player)) return;
        if (player.getMainHandItem() != stack) return;
        boolean connected = SatelliteSavedData.get(level).getSatFromFreq(getFreq(stack)) != null;
        CustomData data = stack.get(ModDataComponents.PERSISTENT_DATA.get());
        if (data != null && data.copyTag().getBooleanOr(KEY_CONNECTED, false) == connected) return;
        if (data == null && !connected) return;
        CustomData.update(
                ModDataComponents.PERSISTENT_DATA.get(),
                stack,
                tag -> tag.putBoolean(KEY_CONNECTED, connected));
    }
}
