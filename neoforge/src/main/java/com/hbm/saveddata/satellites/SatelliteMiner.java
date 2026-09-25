// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.saveddata.satellites;

import com.hbm.itempool.ItemPool;
import com.hbm.saveddata.SatelliteSavedData;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public class SatelliteMiner extends Satellite {

    public static final MapCodec<SatelliteMiner> CODEC =
            RecordCodecBuilder.mapCodec(
                    i ->
                            i.group(
                                            Codec.DOUBLE
                                                    .optionalFieldOf("progress", 0D)
                                                    .forGetter(sat -> sat.progress))
                                    .apply(i, SatelliteMiner::new));

    public static final double SPEED = 1D / (15 * 60 * 20);
    public double progress;

    public SatelliteMiner() {
        this(0D);
    }

    protected SatelliteMiner(double progress) {
        this.progress = progress;
    }

    public static @Nullable String getCargoForItem(Item item) {
        SatelliteType type = SatelliteType.fromItem(item);
        return type == null ? null : type.cargo();
    }

    public @Nullable String getCargo() {
        return type().cargo();
    }

    @Override
    public void onUpdateTick(ServerLevel level) {
        if (!requestableSlots.isEmpty()) return;
        progress += SPEED;
        if (progress >= 1D) {
            progress = 0D;
            int count = 10 + level.getRandom().nextInt(6);
            List<ItemStack> cargo = new ArrayList<>(count);
            for (int i = 0; i < count; i++)
                cargo.add(ItemPool.getStack(getCargo(), level.getRandom()));
            requestableSlots = List.copyOf(cargo);
            SatelliteSavedData.get(level).setDirty();
        }
        if (level.getGameTime() % 1200 == 0) SatelliteSavedData.get(level).setDirty();
    }

    @Override
    public SatelliteType type() {
        return SatelliteType.MINER;
    }
}
