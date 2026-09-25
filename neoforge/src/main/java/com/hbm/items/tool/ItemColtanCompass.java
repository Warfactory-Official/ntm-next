// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.tool;

import com.hbm.packet.toclient.PlayerInformPayload;
import com.hbm.platform.Services;
import com.hbm.util.I18nUtil;
import com.hbm.world.NtmWorldgenFields;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.LodestoneTracker;
import net.minecraft.world.item.component.TooltipDisplay;
import org.jspecify.annotations.Nullable;

public class ItemColtanCompass extends Item {

    private static final int ID_COMPASS = 2;
    private static final double SPREAD = 1500;

    public ItemColtanCompass(Properties props) {
        super(props);
    }

    public static BlockPos deposit(ServerLevel level) {
        NtmWorldgenFields fields = NtmWorldgenFields.get(level);
        return new BlockPos(fields.coltanX(SPREAD), level.getSeaLevel(), fields.coltanZ(SPREAD));
    }

    @Override
    public void inventoryTick(
            ItemStack stack, ServerLevel level, Entity owner, @Nullable EquipmentSlot slot) {
        LodestoneTracker tracker = stack.get(DataComponents.LODESTONE_TRACKER);
        if (tracker == null || tracker.target().isEmpty()) {
            tracker =
                    new LodestoneTracker(
                            Optional.of(new GlobalPos(level.dimension(), deposit(level))), false);
            stack.set(DataComponents.LODESTONE_TRACKER, tracker);
        }
        if (!(owner instanceof ServerPlayer player)) return;

        BlockPos target = tracker.target().orElseThrow().pos();
        double dx = owner.getX() - target.getX();
        double dz = owner.getZ() - target.getZ();
        int distance = (int) Math.sqrt(dx * dx + dz * dz);
        Services.NETWORK.sendTo(
                new PlayerInformPayload(
                        Component.translatable("desc.item.coltanTool.distance", distance),
                        ID_COMPASS,
                        PlayerInformPayload.DEFAULT_MILLIS),
                player);
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> adder,
            TooltipFlag flag) {
        for (String line : I18nUtil.loreLines("desc.item.coltanTool"))
            adder.accept(Component.literal(line));
    }
}
