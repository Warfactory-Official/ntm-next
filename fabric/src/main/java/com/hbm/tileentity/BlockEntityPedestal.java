// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity;

import com.hbm.blocks.ModBlockEntities;
import com.hbm.items.ModItems;
import com.hbm.items.armor.ItemModDefuser;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.platform.Services;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

public class BlockEntityPedestal extends BlockEntity implements Synced, SyncUnitSchema {

    public static final int TIMEOUT = 60;
    private static final Map<ResourceKey<Level>, List<PedestalEntry>> PEDESTAL_ENTRIES =
            new HashMap<>();

    @SyncField(units = 1L)
    public ItemStack item = ItemStack.EMPTY;

    public BlockEntityPedestal(BlockPos pos, BlockState state) {
        super(ModBlockEntities.NTM_PEDESTAL.get(), pos, state);
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (item.isEmpty() || level == null) return;
        level.addFreshEntity(
                new ItemEntity(level, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, item.copy()));
    }

    public static void init() {
        Services.SERVER.onServerTickPost(
                server -> {
                    for (ServerLevel level : server.getAllLevels()) {
                        long time = level.getGameTime();
                        if (time % 20 == 0) checkPedestalEntries(level.dimension(), time);
                    }
                });
    }

    public static void tick(Level level, BlockPos pos, BlockState state, BlockEntityPedestal be) {
        be.serverTick(level, pos, state);
    }

    public static void pushPedestalEntry(Level level, PedestalEntryType type, BlockPos pos) {
        PEDESTAL_ENTRIES
                .computeIfAbsent(level.dimension(), ignored -> new ArrayList<>())
                .add(new PedestalEntry(type, pos.immutable(), level.getGameTime()));
    }

    public static void checkPedestalEntries(ResourceKey<Level> dimension, long currentTime) {
        List<PedestalEntry> entries = PEDESTAL_ENTRIES.get(dimension);
        if (entries != null) entries.removeIf(entry -> entry.timestamp < currentTime - TIMEOUT);
    }

    public static @Nullable List<PedestalEntry> getEntriesForDimension(
            ResourceKey<Level> dimension) {
        return PEDESTAL_ENTRIES.get(dimension);
    }

    private void serverTick(Level level, BlockPos pos, BlockState state) {
        long now = level.getGameTime();
        if (now % 20 != 0 || item.isEmpty()) return;

        if (item.is(ModItems.PROTECTION_CHARM.get())) {
            pushPedestalEntry(level, PedestalEntryType.CHARM_OF_PROTECTION, pos);
        }
        if (item.is(ModItems.METEOR_CHARM.get())) {
            pushPedestalEntry(level, PedestalEntryType.METEORITE_CHARM, pos);
        }
        if (now % 60 == 0 && item.is(ModItems.DEFUSER_GOLD.get())) castrateCreepers(level, pos);
    }

    private void castrateCreepers(Level level, BlockPos pos) {
        for (Creeper creeper : level.getEntitiesOfClass(Creeper.class, new AABB(pos).inflate(25))) {
            ItemModDefuser.castrateCreeper(creeper, null, false);
        }
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        if (!item.isEmpty()) out.store("item", ItemStack.CODEC, item);
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        item = in.read("item", ItemStack.CODEC).orElse(ItemStack.EMPTY);
    }

    @Override
    public long syncUnitMask() {
        return 1L;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        if (unit != 0) throw new IllegalArgumentException();
        ItemStack.OPTIONAL_STREAM_CODEC.encode(
                new RegistryFriendlyByteBuf(output, level.registryAccess()), item);
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        if (unit != 0) throw new IllegalArgumentException();
        item =
                ItemStack.OPTIONAL_STREAM_CODEC.decode(
                        new RegistryFriendlyByteBuf(input, level.registryAccess()));
    }

    public enum PedestalEntryType {
        CHARM_OF_PROTECTION,
        METEORITE_CHARM
    }

    public record PedestalEntry(PedestalEntryType type, BlockPos pos, long timestamp) {}
}
