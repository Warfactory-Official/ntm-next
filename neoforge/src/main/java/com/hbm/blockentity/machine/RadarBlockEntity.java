/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-FileCopyrightText: HbmMods (The Bobcat)
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 * Copyright (C) HbmMods (The Bobcat). All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License version 3 only,
 * as published by the Free Software Foundation.
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this code. If not, see <https://www.gnu.org/licenses/>.
 *
 * Under GPLv3 §7(c) and §7(e), these additional terms apply to LGPL-covered
 * material authored by movblock:
 *
 * - the license grants no rights to the name "NTM NEXT";
 * - a modified version must not be named NTM NEXT or presented as the
 *   official release, and must be marked as different from it.
 *
 * Accurately identifying a work as derived from NTM NEXT does not violate
 * these additional terms, provided it is clearly distinguished from the
 * official release and does not imply endorsement. Ports are encouraged to
 * acknowledge NTM NEXT as their upstream project and provide a link to it.
 */

package com.hbm.blockentity.machine;

import com.hbm.api.block.RadarCommandHandler;
import com.hbm.api.energymk2.EnergyHandler;
import com.hbm.api.energymk2.ItemEnergyTransfer;
import com.hbm.api.entity.RadarEntry;
import com.hbm.api.entity.RadarProvider;
import com.hbm.api.entity.RadarScanParams;
import com.hbm.api.entity.RadarTargetType;
import com.hbm.blockentity.MachineBlockEntity;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.data.MachineData;
import com.hbm.extprop.LivingHazards;
import com.hbm.inventory.container.ModMenus;
import com.hbm.inventory.container.RadarMenu;
import com.hbm.inventory.container.RadarSlotsMenu;
import com.hbm.items.ModItems;
import com.hbm.items.SatelliteFrequencyProvider;
import com.hbm.items.tool.RadarLinkerItem;
import com.hbm.packet.BeSyncTable;
import com.hbm.packet.BlobSynced;
import com.hbm.packet.SyncAudience;
import com.hbm.packet.SyncBlob;
import com.hbm.packet.SyncList;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.saveddata.SatelliteSavedData;
import com.hbm.saveddata.satellites.RadiationBursts;
import com.hbm.saveddata.satellites.SatelliteRayEvents;
import com.hbm.sound.EntitySounds;
import com.hbm.sound.ModSounds;
import com.hbm.sound.WorldSounds;
import com.hbm.util.ChunkUtil;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Mth;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class RadarBlockEntity extends MachineBlockEntity
        implements EnergyHandler, MenuProvider, SyncUnitSchema, BlobSynced, SyncAudience {
    public static final int SLOT_LINK_COUNT = 8;
    public static final int SLOT_LINKER = 8;
    public static final int SLOT_BATTERY = 9;
    public static final int SLOT_COUNT = 10;
    public static final int MAP_SIDE = 200;
    public static final int MAP_SIZE = MAP_SIDE * MAP_SIDE;
    public static final int REPORT_PERIOD = 20;
    private static Class<?>[] trackedFamilies;

    public long power;
    public boolean scanMissiles;

    {
        hbm$syncSet$RadarBlockEntity$scanMissiles(true);
    }

    public boolean scanShells;

    {
        hbm$syncSet$RadarBlockEntity$scanShells(true);
    }

    public boolean scanPlayers;

    {
        hbm$syncSet$RadarBlockEntity$scanPlayers(true);
    }

    public boolean smartMode;

    {
        hbm$syncSet$RadarBlockEntity$smartMode(true);
    }

    public boolean redMode;

    {
        hbm$syncSet$RadarBlockEntity$redMode(true);
    }

    public boolean showMap;
    public boolean jammed;
    public final List<RadarEntry> entries = new SyncList<>();
    private boolean clearMap;
    public final byte[] map = new byte[MAP_SIZE];
    private final SyncBlob terrain = new SyncBlob(map, this);
    private final BlockPos.MutableBlockPos samplePosition = new BlockPos.MutableBlockPos();
    private RadarScanParams parameters = new RadarScanParams(true, true, true, true);
    private int parameterBits = 15;
    private int redPower;
    private int pingTicks;
    public float prevRotation;
    public float rotation;

    public RadarBlockEntity(BlockPos pos, BlockState state) {
        this(ModBlockEntities.RADAR.get(), pos, state);
    }

    protected RadarBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, SLOT_COUNT);
    }

    public static void registerScanSystem() {
        trackedFamilies = new Class<?>[] {RadarProvider.class, Player.class};
    }

    public static boolean tracks(Entity entity) {
        for (var family : trackedFamilies) if (family.isInstance(entity)) return true;
        return false;
    }

    @Override
    public void tickServer() {
        long before = power;
        hbm$syncSet$RadarBlockEntity$power(
                (long)
                        (power
                                + (ItemEnergyTransfer.extract(
                                        this, SLOT_BATTERY, getMaxPower() - power, false))));
        hbm$syncSet$RadarBlockEntity$power(
                (long)
                        (power
                                + (ItemEnergyTransfer.extract(
                                        this, 0, getMaxPower() - power, false))));
        hbm$syncSet$RadarBlockEntity$jammed(false);
        scan((ServerLevel) level);
        if (power != before) setChanged();
        updateRedstone();
        if (!isMuffled() && ++pingTicks >= 80 && power > 0) {
            WorldSounds.machine(
                    level,
                    null,
                    worldPosition,
                    ModSounds.SONAR_PING.get(),
                    SoundSource.BLOCKS,
                    5F,
                    1F);
            pingTicks = 0;
        }
        if (showMap) sampleTerrain((ServerLevel) level);
        pushDisplay();
        if (clearMap && terrain.clear()) setChanged();
        networkPackNT(50);
        hbm$syncSet$RadarBlockEntity$clearMap(false);
        flushSyncBlob();
    }

    private RadarScanParams scanParameters() {
        int bits =
                (scanMissiles ? 1 : 0)
                        | (scanShells ? 2 : 0)
                        | (scanPlayers ? 4 : 0)
                        | (smartMode ? 8 : 0);
        if (bits != parameterBits) {
            parameters = new RadarScanParams(scanMissiles, scanShells, scanPlayers, smartMode);
            parameterBits = bits;
        }
        return parameters;
    }

    private void scan(ServerLevel server) {
        if (worldPosition.getY() < MachineData.RADAR_ALTITUDE.get()) {
            entries.clear();
            return;
        }
        long cost = MachineData.RADAR_CONSUMPTION.get();
        if (power < cost) {
            hbm$syncSet$RadarBlockEntity$power(0);
            entries.clear();
            return;
        }
        hbm$syncSet$RadarBlockEntity$power((long) (power - (cost)));
        var settings = scanParameters();
        int range = getRange();
        int count = 0;
        for (var entity :
                ((com.hbm.interfaces.injected.ServerLevelExtension) (server)).hbm$radarTargets()) {
            if (entity.isSpectator()
                    || Math.abs(entity.getX() - worldPosition.getX() - 0.5D) > range
                    || Math.abs(entity.getZ() - worldPosition.getZ() - 0.5D) > range
                    || entity.getY() - worldPosition.getY() <= MachineData.RADAR_BUFFER.get())
                continue;
            if (entity instanceof LivingEntity living && LivingHazards.digamma(living) > 0.001D) {
                hbm$syncSet$RadarBlockEntity$jammed(true);
                entries.clear();
                return;
            }
            if (entity instanceof RadarProvider contact
                    && contact.canBeSeenBy(this)
                    && contact.matchesScan(settings)) {
                retainContact(
                        count++,
                        entity,
                        contact.getRadarName(),
                        contact.getTargetType().id(),
                        contact.suppliesRedstone(settings));
            } else if (scanPlayers && entity instanceof Player player) {
                retainContact(
                        count++,
                        entity,
                        player.getName().getString(),
                        RadarTargetType.PLAYER.id(),
                        true);
            }
        }
        while (entries.size() > count) entries.removeLast();
        if (server.getGameTime() % REPORT_PERIOD == 0) {
            RadiationBursts.report(
                    server,
                    RadiationBursts.Intensity.MEDIUM,
                    worldPosition.getX(),
                    worldPosition.getZ());
            SatelliteRayEvents.report(server, worldPosition, SatelliteRayEvents.RADAR_WAVES, 200);
        }
    }

    private void retainContact(
            int index, Entity entity, String name, int kind, boolean suppliesRedstone) {
        int x = Mth.floor(entity.getX()),
                y = Mth.floor(entity.getY()),
                z = Mth.floor(entity.getZ());
        if (index < entries.size()) {
            var previous = entries.get(index);
            if (previous.entityID == entity.getId()
                    && previous.blipLevel == kind
                    && previous.posX == x
                    && previous.posY == y
                    && previous.posZ == z
                    && previous.getRadarName.equals(name)) {
                previous.redstone = suppliesRedstone;
                return;
            }
            entries.set(
                    index, new RadarEntry(name, kind, x, y, z, entity.getId(), suppliesRedstone));
        } else {
            entries.add(new RadarEntry(name, kind, x, y, z, entity.getId(), suppliesRedstone));
        }
    }

    private void updateRedstone() {
        int next = 0;
        double diagonal = getRange() * Math.sqrt(2D);
        for (int i = 0; i < entries.size(); i++) {
            var contact = entries.get(i);
            if (!contact.redstone) continue;
            int value =
                    redMode
                            ? 15
                                    - (int)
                                            Math.floor(
                                                    15D
                                                            * Math.hypot(
                                                                    (double) contact.posX
                                                                            - worldPosition.getX(),
                                                                    (double) contact.posZ
                                                                            - worldPosition.getZ())
                                                            / diagonal)
                            : contact.blipLevel + 1;
            next = Math.max(next, value);
        }
        if (next != redPower) {
            redPower = next;
            notifyRedstone();
        }
    }

    protected void notifyRedstone() {
        level.updateNeighborsAt(worldPosition, getBlockState().getBlock());
    }

    public int getRedPower() {
        return redPower;
    }

    public int getRange() {
        return MachineData.RADAR_RANGE.get();
    }

    private void sampleTerrain(ServerLevel server) {
        int start = (int) (server.getGameTime() % 400) * 100;
        int range = getRange();
        int acquired = 0;
        boolean changed = false;
        for (int i = start; i < start + 100; i++) {
            int x = worldPosition.getX() - range + i % MAP_SIDE * range * 2 / MAP_SIDE;
            int z = worldPosition.getZ() - range + i / MAP_SIDE * range * 2 / MAP_SIDE;
            samplePosition.set(x, worldPosition.getY(), z);
            ChunkAccess chunk = ChunkUtil.chunkIfLoaded(server, samplePosition);
            if (chunk == null) {
                if (map[i] != 0 || acquired >= MachineData.RADAR_CHUNK_LOAD_CAP.get()) continue;
                chunk =
                        MachineData.RADAR_GENERATE_CHUNKS.get()
                                ? server.getChunk(x >> 4, z >> 4)
                                : server.getChunkSource()
                                        .getChunk(x >> 4, z >> 4, ChunkStatus.EMPTY, true);
                if (chunk == null) continue;
                acquired++;
            }
            byte height =
                    (byte)
                            Mth.clamp(
                                    chunk.getHeight(
                                            Heightmap.Types.MOTION_BLOCKING, x & 15, z & 15),
                                    50,
                                    128);
            if (map[i] != height) {
                hbm$syncArraySet$RadarBlockEntity$map(map, i, height);
                changed = true;
            }
        }
        if (changed) setChanged();
    }

    private void pushDisplay() {
        var linker = inventory.get(SLOT_LINKER);
        if (!(linker.getItem() instanceof RadarLinkerItem)) return;
        var target = RadarLinkerItem.getPosition(linker);
        if (target != null
                && ChunkUtil.blockEntityIfLoaded(level, target)
                        instanceof RadarScreenBlockEntity display) {
            display.acceptRadar(worldPosition, getRange(), entries);
        }
    }

    @Override
    public void tickClient() {
        prevRotation = rotation;
        if (power <= 0) return;
        rotation += 5F;
        if (rotation >= 360F) {
            rotation -= 360F;
            prevRotation -= 360F;
        }
    }

    public boolean isRadarMenuValid(Player player) {
        return ChunkUtil.blockEntityIfLoaded(level, worldPosition) == this
                && (player.containerMenu instanceof RadarMenu menu && menu.blockEntity() == this
                        || player.containerMenu instanceof RadarSlotsMenu slots
                                && slots.blockEntity() == this);
    }

    public boolean openMap(Player player) {
        if (!aboveMinimumAltitude(player)) return false;
        ModMenus.MACHINE_RADAR.open(player, this);
        return true;
    }

    public boolean openSlots(Player player) {
        if (!aboveMinimumAltitude(player)) return false;
        ModMenus.MACHINE_RADAR_SLOTS.open(player, this);
        return true;
    }

    private boolean aboveMinimumAltitude(Player player) {
        if (worldPosition.getY() >= MachineData.RADAR_ALTITUDE.get()) return true;
        player.sendSystemMessage(
                Component.translatable("desc.block.radar.errorRadarAltitudeNot")
                        .withStyle(ChatFormatting.RED));
        return false;
    }

    private void admit(Player player) {
        if (player instanceof ServerPlayer server) {
            networkPackNT(50);
            BeSyncTable.admit(this, server);
        }
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        admit(player);
        return new RadarMenu(id, inventory, this);
    }

    public RadarSlotsMenu slotsMenu(int id, Inventory inventory) {
        admit(inventory.player);
        return new RadarSlotsMenu(id, inventory, this);
    }

    @Override
    public boolean includes(ServerPlayer player) {
        return isRadarMenuValid(player);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.radar");
    }

    public void applyControlToggles(
            boolean missiles,
            boolean shells,
            boolean players,
            boolean smart,
            boolean red,
            boolean map,
            boolean clear) {
        if (missiles) hbm$syncSet$RadarBlockEntity$scanMissiles(!scanMissiles);
        if (shells) hbm$syncSet$RadarBlockEntity$scanShells(!scanShells);
        if (players) hbm$syncSet$RadarBlockEntity$scanPlayers(!scanPlayers);
        if (smart) hbm$syncSet$RadarBlockEntity$smartMode(!smartMode);
        if (red) hbm$syncSet$RadarBlockEntity$redMode(!redMode);
        if (map) hbm$syncSet$RadarBlockEntity$showMap(!showMap);
        if (clear) hbm$syncSet$RadarBlockEntity$clearMap(true);
        if (missiles || shells || players || smart || red || map) setChanged();
    }

    public void dispatchLinkEntity(Player player, int slot, int entityId) {
        if (player.isSpectator()) return;
        var link = inventory.get(slot);
        if (!(link.getItem() instanceof RadarLinkerItem)) return;
        var targetPos = RadarLinkerItem.getPosition(link);
        if (targetPos == null
                || !(ChunkUtil.blockEntityIfLoaded(level, targetPos)
                        instanceof RadarCommandHandler target)) return;
        var entity = level.getEntity(entityId);
        if (entity != null && !entity.isSpectator() && target.strike(entity)) bleep(player);
    }

    public void dispatchLinkPosition(Player player, int slot, int x, int z) {
        if (player.isSpectator()) return;
        var link = inventory.get(slot);
        if (link.is(ModItems.SAT_RELAY.get())) {
            var server = (ServerLevel) level;
            var satellite =
                    SatelliteSavedData.at(
                                    server,
                                    worldPosition.getX(),
                                    worldPosition.getZ(),
                                    SatelliteFrequencyProvider.frequency(link))
                            .sats
                            .get(SatelliteFrequencyProvider.frequency(link));
            if (satellite != null) {
                bleep(player);
                satellite.designate(server, player, x, 60, z);
            }
        } else if (link.getItem() instanceof RadarLinkerItem) {
            var targetPos = RadarLinkerItem.getPosition(link);
            if (targetPos != null
                    && ChunkUtil.blockEntityIfLoaded(level, targetPos)
                            instanceof RadarCommandHandler target
                    && target.strikeAt(x, worldPosition.getY(), z)) bleep(player);
        }
    }

    private static void bleep(Player player) {
        EntitySounds.play(
                player,
                player.getX(),
                player.getY(),
                player.getZ(),
                ModSounds.TECH_BLEEP.get(),
                SoundSource.PLAYERS,
                1F,
                1F);
    }

    @Override
    public long getPower() {
        return power;
    }

    @Override
    public long getMaxPower() {
        return MachineData.RADAR_MAX_POWER.get();
    }

    @Override
    public void setPower(long stored) {
        if (power != stored) {
            hbm$syncSet$RadarBlockEntity$power(stored);
            setChanged();
        }
    }

    private void terrainChanged(int index) {
        if (index >= 0) terrain.changed(index);
        else for (int i = 0; i < MAP_SIZE; i++) terrain.changed(i);
    }

    @Override
    public SyncBlob syncBlob() {
        return terrain;
    }

    @Override
    public void flushSyncBlob() {
        terrain.flush(this, 50, showMap);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        hbm$syncSet$RadarBlockEntity$power(input.getLongOr("power", 0));
        hbm$syncSet$RadarBlockEntity$scanMissiles(input.getBooleanOr("scanMissiles", false));
        hbm$syncSet$RadarBlockEntity$scanShells(input.getBooleanOr("scanShells", false));
        hbm$syncSet$RadarBlockEntity$scanPlayers(input.getBooleanOr("scanPlayers", false));
        hbm$syncSet$RadarBlockEntity$smartMode(input.getBooleanOr("smartMode", false));
        hbm$syncSet$RadarBlockEntity$redMode(input.getBooleanOr("redMode", false));
        hbm$syncSet$RadarBlockEntity$showMap(input.getBooleanOr("showMap", false));
        input.read("map", ExtraCodecs.BASE64_STRING)
                .ifPresent(
                        samples -> {
                            if (samples.length != MAP_SIZE)
                                throw new IllegalArgumentException("Invalid radar map size");
                            terrain.copy(samples);
                        });
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("power", power);
        output.putBoolean("scanMissiles", scanMissiles);
        output.putBoolean("scanShells", scanShells);
        output.putBoolean("scanPlayers", scanPlayers);
        output.putBoolean("smartMode", smartMode);
        output.putBoolean("redMode", redMode);
        output.putBoolean("showMap", showMap);
        output.store("map", ExtraCodecs.BASE64_STRING, map);
    }

    @Override
    public long syncUnitMask() {
        return 1023;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeLong(power);
            case 1 -> output.writeBoolean(scanMissiles);
            case 2 -> output.writeBoolean(scanShells);
            case 3 -> output.writeBoolean(scanPlayers);
            case 4 -> output.writeBoolean(smartMode);
            case 5 -> output.writeBoolean(redMode);
            case 6 -> output.writeBoolean(showMap);
            case 7 -> output.writeBoolean(jammed);
            case 8 -> {
                output.writeInt(entries.size());
                for (int i = 0; i < entries.size(); i++) entries.get(i).toBytes(output);
            }
            case 9 -> output.writeBoolean(clearMap);
            default -> throw new IllegalArgumentException();
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> hbm$syncSet$RadarBlockEntity$power(input.readLong());
            case 1 -> hbm$syncSet$RadarBlockEntity$scanMissiles(input.readBoolean());
            case 2 -> hbm$syncSet$RadarBlockEntity$scanShells(input.readBoolean());
            case 3 -> hbm$syncSet$RadarBlockEntity$scanPlayers(input.readBoolean());
            case 4 -> hbm$syncSet$RadarBlockEntity$smartMode(input.readBoolean());
            case 5 -> hbm$syncSet$RadarBlockEntity$redMode(input.readBoolean());
            case 6 -> hbm$syncSet$RadarBlockEntity$showMap(input.readBoolean());
            case 7 -> hbm$syncSet$RadarBlockEntity$jammed(input.readBoolean());
            case 8 -> {
                int count = input.readInt();
                if (count < 0) throw new DecoderException("Negative radar contact count");
                entries.clear();
                for (int i = 0; i < count; i++) entries.add(new RadarEntry(input));
            }
            case 9 -> {
                hbm$syncSet$RadarBlockEntity$clearMap(input.readBoolean());
                if (clearMap) terrain.clearReceived();
            }
            default -> throw new IllegalArgumentException();
        }
    }

    @Override
    public void writeInitialExtras(ByteBuf output) {
        output.writeBoolean(showMap);
        if (showMap) terrain.writeInitial(output);
    }

    @Override
    public void readInitialExtras(ByteBuf input) {
        if (input.readBoolean()) terrain.readInitial(input);
    }

    public final void hbm$syncSet$RadarBlockEntity$power(long next) {
        if (this.power != next) {
            this.power = next;
            if ((1) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1);
        }
    }

    public final void hbm$syncSet$RadarBlockEntity$scanMissiles(boolean next) {
        if (this.scanMissiles != next) {
            this.scanMissiles = next;
            if ((2) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 2);
        }
    }

    public final void hbm$syncSet$RadarBlockEntity$scanShells(boolean next) {
        if (this.scanShells != next) {
            this.scanShells = next;
            if ((4) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 4);
        }
    }

    public final void hbm$syncSet$RadarBlockEntity$scanPlayers(boolean next) {
        if (this.scanPlayers != next) {
            this.scanPlayers = next;
            if ((8) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 8);
        }
    }

    public final void hbm$syncSet$RadarBlockEntity$smartMode(boolean next) {
        if (this.smartMode != next) {
            this.smartMode = next;
            if ((16) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 16);
        }
    }

    public final void hbm$syncSet$RadarBlockEntity$redMode(boolean next) {
        if (this.redMode != next) {
            this.redMode = next;
            if ((32) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 32);
        }
    }

    public final void hbm$syncSet$RadarBlockEntity$showMap(boolean next) {
        if (this.showMap != next) {
            this.showMap = next;
            if ((64) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 64);
        }
    }

    public final void hbm$syncSet$RadarBlockEntity$jammed(boolean next) {
        if (this.jammed != next) {
            this.jammed = next;
            if ((128) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 128);
        }
    }

    public final void hbm$syncSet$RadarBlockEntity$clearMap(boolean next) {
        if (this.clearMap != next) {
            this.clearMap = next;
            if ((512) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 512);
        }
    }

    public final byte hbm$syncArraySet$RadarBlockEntity$map(byte[] array, int index, byte next) {
        if (array[index] != next) {
            array[index] = next;
            if (array == this.map) {
                syncArrayChanged(array, index, 2, 0L);
                terrainChanged(index);
            }
        }
        return next;
    }

    static {
        com.hbm.packet.SyncBindings.field(RadarBlockEntity.class, "entries", 3, 256);
        com.hbm.packet.SyncBindings.field(RadarBlockEntity.class, "map", 2, 0L);
    }
}
