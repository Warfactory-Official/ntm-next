// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine;

import com.hbm.api.block.IRadarCommandReceiver;
import com.hbm.api.control.IControlReceiver;
import com.hbm.api.energymk2.IEnergyHandlerMK2;
import com.hbm.api.energymk2.ItemEnergyTransfer;
import com.hbm.api.entity.IRadarDetectable;
import com.hbm.api.entity.IRadarDetectableNT.RadarScanParams;
import com.hbm.api.entity.IRadarDetectableNT;
import com.hbm.api.entity.RadarEntry;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.data.MachineData;
import com.hbm.extprop.HbmLivingProps;
import com.hbm.inventory.IGUIProvider;
import com.hbm.inventory.container.MenuMachineRadar;
import com.hbm.inventory.container.MenuMachineRadarSlots;
import com.hbm.items.ISatChip;
import com.hbm.items.ModItems;
import com.hbm.items.tool.ItemRadarLinker;
import com.hbm.packet.BlobSynced;
import com.hbm.packet.ChunkTrackerIndex;
import com.hbm.packet.SyncBlob;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncList;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.saveddata.SatelliteSavedData;
import com.hbm.saveddata.satellites.Satellite;
import com.hbm.saveddata.satellites.SatelliteDetector;
import com.hbm.saveddata.satellites.SatelliteRayEvents;
import com.hbm.sound.ModSounds;
import com.hbm.tileentity.BlockEntityMachineBase;
import com.hbm.util.ChunkUtil;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public class BlockEntityMachineRadar extends BlockEntityMachineBase
        implements IEnergyHandlerMK2, IGUIProvider, IControlReceiver, BlobSynced, SyncUnitSchema {

    public static final int SLOT_LINK_COUNT = 8;
    public static final int SLOT_LINKER = 8;
    public static final int SLOT_BATTERY = 9;
    public static final int SLOT_COUNT = 10;

    public static final int DISPATCH_MAP = 0;
    public static final int DISPATCH_SLOTS = 1;

    public static final int MAP_SIDE = 200;
    public static final int MAP_SIZE = MAP_SIDE * MAP_SIDE;

    private static final int MAP_SAMPLES_PER_TICK = 100;
    private static final int MAP_CYCLE = MAP_SIZE / MAP_SAMPLES_PER_TICK;
    private static final int MAP_HEIGHT_MIN = 50;
    private static final int MAP_HEIGHT_MAX = 128;

    private static final int SATELLITE_ALTITUDE = 60;
    private static final int PING_INTERVAL = 80;

    private static final float SPIN_PER_TICK = 5F;

    public static final List<Class<?>> DETECTABLE = new ArrayList<>();

    public static final List<RadarConverter> CONVERTERS = new ArrayList<>();

    public static final List<Entity> MATCHING = new ArrayList<>();

    @SyncField(units = 1L << 1)
    public boolean scanMissiles = true;

    @SyncField(units = 1L << 2)
    public boolean scanShells = true;

    @SyncField(units = 1L << 3)
    public boolean scanPlayers = true;

    @SyncField(units = 1L << 4)
    public boolean smartMode = true;

    @SyncField(units = 1L << 5)
    public boolean redMode = true;

    @SyncField(units = 1L << 6)
    public boolean showMap = false;

    @SyncField(units = 1L << 7)
    public boolean jammed = false;

    public float prevRotation;
    public float rotation;

    @SyncField(units = 1L)
    public long power = 0;

    @SyncField(value = 2, units = 0, changed = "mapChanged")
    public final byte[] map = new byte[MAP_SIZE];

    @SyncField(value = 2, units = 0)
    private final SyncBlob mapState = new SyncBlob(map, this);

    @SyncField(units = 1L << 9)
    public boolean clearFlag = false;

    @SyncField(units = 1L << 8)
    public final List<RadarEntry> entries = new SyncList<>();

    private int pingTimer = 0;
    private int lastPower;

    public BlockEntityMachineRadar(BlockPos pos, BlockState state) {
        this(ModBlockEntities.RADAR.get(), pos, state);
    }

    protected BlockEntityMachineRadar(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, SLOT_COUNT);
    }

    public static void registerScanSystem() {
        DETECTABLE.add(IRadarDetectableNT.class);
        DETECTABLE.add(IRadarDetectable.class);
        DETECTABLE.add(Player.class);

        CONVERTERS.add(
                (entity, radar, params) -> {
                    if (entity instanceof IRadarDetectableNT detectable
                            && detectable.canBeSeenBy(radar)
                            && detectable.paramsApplicable(params)) {
                        return new RadarEntry(
                                detectable, entity, detectable.suppliesRedstone(params));
                    }
                    return null;
                });

        CONVERTERS.add(
                (entity, radar, params) ->
                        entity instanceof IRadarDetectable detectable && params.scanMissiles
                                ? new RadarEntry(detectable, entity)
                                : null);
        CONVERTERS.add(
                (entity, radar, params) ->
                        entity instanceof Player player && params.scanPlayers
                                ? new RadarEntry(player)
                                : null);
    }

    public static void updateSystem(MinecraftServer server) {
        MATCHING.clear();
        for (ServerLevel level : server.getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                for (Class<?> type : DETECTABLE) {
                    if (type.isInstance(entity)) {
                        MATCHING.add(entity);
                        break;
                    }
                }
            }
        }
    }

    public int getRange() {
        return MachineData.RADAR_RANGE.get();
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.radar");
    }

    @Override
    public void tickServer() {

        this.power += ItemEnergyTransfer.extract(this, SLOT_BATTERY, getMaxPower() - power, false);
        this.power += ItemEnergyTransfer.extract(this, 0, getMaxPower() - power, false);

        this.jammed = false;
        allocateTargets();

        int redPower = getRedPower();
        if (this.lastPower != redPower) {
            markChanged();
            notifyRedstone();
        }
        this.lastPower = redPower;

        if (!isMuffled()) {
            pingTimer++;
            if (power > 0 && pingTimer >= PING_INTERVAL) {
                level.playSound(
                        null,
                        worldPosition,
                        ModSounds.SONAR_PING.get(),
                        SoundSource.BLOCKS,
                        5.0F,
                        1.0F);
                pingTimer = 0;
            }
        }

        if (this.showMap) sampleMap();

        pushToLinkedScreen();

        if (this.clearFlag) mapState.clear();
        networkPackNT(50);
        if (this.clearFlag) this.clearFlag = false;
        flushSyncBlob();
    }

    @Override
    public void tickClient() {
        prevRotation = rotation;
        if (power > 0) rotation += SPIN_PER_TICK;

        if (rotation >= 360) {
            rotation -= 360F;
            prevRotation -= 360F;
        }
    }

    private void sampleMap() {
        int chunkLoads = 0;
        int chunkLoadCap = MachineData.RADAR_CHUNK_LOAD_CAP.get();
        int range = getRange();

        for (int i = 0; i < MAP_SAMPLES_PER_TICK; i++) {
            int index = (int) (level.getGameTime() % MAP_CYCLE) * MAP_SAMPLES_PER_TICK + i;
            int iX = (index % MAP_SIDE) * range * 2 / MAP_SIDE;
            int iZ = index / MAP_SIDE * range * 2 / MAP_SIDE;

            int x = worldPosition.getX() - range + iX;
            int z = worldPosition.getZ() - range + iZ;

            ChunkAccess resident = ChunkUtil.chunkIfLoaded(level, new BlockPos(x, 0, z));
            if (resident != null) {
                this.map[index] = sample(resident, x, z);
                continue;
            }
            if (this.map[index] != 0 || chunkLoads >= chunkLoadCap) continue;

            ChunkAccess loaded = provideChunk(x, z);
            if (loaded == null) continue;
            this.map[index] = sample(loaded, x, z);
            chunkLoads++;
        }
    }

    private byte sample(ChunkAccess chunk, int x, int z) {
        int height = chunk.getHeight(Heightmap.Types.MOTION_BLOCKING, x & 15, z & 15);
        return (byte) Mth.clamp(height, MAP_HEIGHT_MIN, MAP_HEIGHT_MAX);
    }

    private @Nullable ChunkAccess provideChunk(int x, int z) {
        ServerLevel server = (ServerLevel) level;
        return MachineData.RADAR_GENERATE_CHUNKS.get()
                ? server.getChunk(x >> 4, z >> 4)
                : server.getChunkSource().getChunk(x >> 4, z >> 4, ChunkStatus.EMPTY, true);
    }

    protected void notifyRedstone() {
        level.updateNeighborsAt(worldPosition, getBlockState().getBlock());
    }

    private void pushToLinkedScreen() {
        ItemStack link = getItem(SLOT_LINKER);
        if (!(link.getItem() instanceof ItemRadarLinker)) return;

        BlockPos target = ItemRadarLinker.getPosition(link);
        if (target == null) return;
        if (!(ChunkUtil.blockEntityIfLoaded(level, target)
                instanceof BlockEntityMachineRadarScreen screen)) {
            return;
        }

        screen.acceptRadar(worldPosition, getRange(), entries);
    }

    protected void allocateTargets() {
        this.entries.clear();

        if (worldPosition.getY() < MachineData.RADAR_ALTITUDE.get()) return;
        long consumption = MachineData.RADAR_CONSUMPTION.get();
        if (this.power < consumption) {
            this.power = 0;
            return;
        }
        this.power -= consumption;
        int buffer = MachineData.RADAR_BUFFER.get();

        int scan = this.getRange();
        RadarScanParams params =
                new RadarScanParams(
                        this.scanMissiles, this.scanShells, this.scanPlayers, this.smartMode);

        for (Entity e : MATCHING) {
            if (e.level() != level) continue;
            if (Math.abs(e.getX() - (worldPosition.getX() + 0.5)) > scan) continue;
            if (Math.abs(e.getZ() - (worldPosition.getZ() + 0.5)) > scan) continue;
            if (e.getY() - worldPosition.getY() <= buffer) continue;

            if (e instanceof LivingEntity living && HbmLivingProps.getDigamma(living) > 0.001) {
                this.jammed = true;
                entries.clear();
                return;
            }

            for (RadarConverter converter : CONVERTERS) {
                RadarEntry entry = converter.convert(e, this, params);
                if (entry != null) {
                    this.entries.add(entry);
                    break;
                }
            }
        }
        if (level.getGameTime() % 20 == 0) {
            ServerLevel server = (ServerLevel) level;
            SatelliteDetector.reportEvent(
                    server,
                    SatelliteDetector.DURATION_MEDIUM,
                    SatelliteDetector.BurstIntensity.MEDIUM,
                    worldPosition.getX(),
                    worldPosition.getZ());
            SatelliteRayEvents.report(server, worldPosition, SatelliteRayEvents.RADAR_WAVES, 200);
        }
    }

    public int getRedPower() {
        if (entries.isEmpty()) return 0;

        if (redMode) {
            double maxRange = this.getRange() * Math.sqrt(2D);
            int best = 0;

            for (RadarEntry e : entries) {
                if (!e.redstone) continue;
                double dist =
                        Math.sqrt(
                                Math.pow(e.posX - worldPosition.getX(), 2)
                                        + Math.pow(e.posZ - worldPosition.getZ(), 2));
                int p = 15 - (int) Math.floor(dist / maxRange * 15);
                if (p > best) best = p;
            }
            return best;
        }

        int best = 0;
        for (RadarEntry e : entries) {
            if (!e.redstone) continue;
            if (e.blipLevel + 1 > best) best = e.blipLevel + 1;
        }
        return best;
    }

    @Override
    public long getPower() {
        return power;
    }

    @Override
    public void setPower(long i) {
        power = i;
    }

    @Override
    public long getMaxPower() {
        return MachineData.RADAR_MAX_POWER.get();
    }

    @Override
    public boolean hasPermission(Player player) {
        return stillValid(player) || isRadarMenuValid(player);
    }

    public boolean isRadarMenuValid(Player player) {
        AbstractContainerMenu menu = player.containerMenu;
        boolean ownsMenu =
                menu instanceof MenuMachineRadar radar && radar.blockEntity() == this
                        || menu instanceof MenuMachineRadarSlots slots
                                && slots.blockEntity() == this;
        return ownsMenu
                && level != null
                && ChunkUtil.blockEntityIfLoaded(level, worldPosition) == this;
    }

    @Override
    public void receiveControl(CompoundTag data) {
        if (data.contains("missiles")) this.scanMissiles = !this.scanMissiles;
        if (data.contains("shells")) this.scanShells = !this.scanShells;
        if (data.contains("players")) this.scanPlayers = !this.scanPlayers;
        if (data.contains("smart")) this.smartMode = !this.smartMode;
        if (data.contains("red")) this.redMode = !this.redMode;
        if (data.contains("map")) this.showMap = !this.showMap;
        if (data.contains("clear")) this.clearFlag = true;
    }

    @Override
    public void receiveControl(Player player, CompoundTag data) {
        receiveControl(data);

        if (data.contains("gui1"))
            IGUIProvider.openBlockMenu(player, this, worldPosition, DISPATCH_SLOTS);

        if (data.contains("gui0"))
            IGUIProvider.openBlockMenu(player, this, worldPosition, DISPATCH_MAP);
        if (data.contains("link")) dispatchLink(player, data);
    }

    private void dispatchLink(Player player, CompoundTag data) {
        int id = data.getIntOr("link", 0);
        if (id < 0 || id >= SLOT_LINK_COUNT) return;
        ItemStack link = getItem(id);
        if (link.isEmpty()) return;

        if (link.getItem() == ModItems.SAT_RELAY.get()) {
            dispatchSatellite(player, data, ISatChip.getFreqS(link));
            return;
        }

        if (!(link.getItem() instanceof ItemRadarLinker)) return;
        BlockPos target = ItemRadarLinker.getPosition(link);
        if (target == null) return;
        if (!(ChunkUtil.blockEntityIfLoaded(level, target)
                instanceof IRadarCommandReceiver receiver)) return;

        if (data.contains("launchEntity")) {
            Entity entity = level.getEntity(data.getIntOr("launchEntity", -1));
            if (entity != null && receiver.sendCommandEntity(entity)) bleep(player);
            return;
        }
        if (data.contains("launchPosX")) {
            int x = data.getIntOr("launchPosX", 0);
            int z = data.getIntOr("launchPosZ", 0);

            if (receiver.sendCommandPosition(x, worldPosition.getY(), z)) bleep(player);
        }
    }

    private void dispatchSatellite(Player player, CompoundTag data, int freq) {
        if (!data.contains("launchPosX")) return;

        if (!(player.level() instanceof ServerLevel orbit)) return;

        Satellite sat = SatelliteSavedData.get(orbit).getSatFromFreq(freq);
        if (sat == null) return;

        int x = data.getIntOr("launchPosX", 0);
        int z = data.getIntOr("launchPosZ", 0);

        bleep(player);
        sat.onCoordAction(orbit, player, x, SATELLITE_ALTITUDE, z);
    }

    protected void bleep(Player player) {
        level.playSound(
                null,
                player.blockPosition(),
                ModSounds.TECH_BLEEP.get(),
                SoundSource.PLAYERS,
                1.0F,
                1.0F);
    }

    @Override
    public Component getDisplayName() {
        return getName();
    }

    @Override
    public AbstractContainerMenu createMenu(
            int containerId, Inventory playerInventory, Player player) {
        return new MenuMachineRadar(containerId, playerInventory, this);
    }

    @Override
    public AbstractContainerMenu createMenu(
            int containerId, Inventory playerInventory, Player player, int dispatchId) {
        return dispatchId == DISPATCH_SLOTS
                ? new MenuMachineRadarSlots(containerId, playerInventory, this)
                : new MenuMachineRadar(containerId, playerInventory, this);
    }

    @Override
    public long syncUnitMask() {
        return 0b11_1111_1111;
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
                for (RadarEntry entry : entries) entry.toBytes(output);
            }
            case 9 -> output.writeBoolean(clearFlag);
            default -> throw new IllegalArgumentException("Invalid machine sync unit");
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> power = input.readLong();
            case 1 -> scanMissiles = input.readBoolean();
            case 2 -> scanShells = input.readBoolean();
            case 3 -> scanPlayers = input.readBoolean();
            case 4 -> smartMode = input.readBoolean();
            case 5 -> redMode = input.readBoolean();
            case 6 -> showMap = input.readBoolean();
            case 7 -> jammed = input.readBoolean();
            case 8 -> {
                int count = input.readInt();
                if (count < 0 || count > input.readableBytes() / 19) {
                    throw new DecoderException("Invalid radar entry count");
                }
                entries.clear();
                for (int i = 0; i < count; i++) entries.add(new RadarEntry(input));
            }
            case 9 -> {
                clearFlag = input.readBoolean();
                if (clearFlag) mapState.clearReceived();
            }
            default -> throw new IllegalArgumentException("Invalid machine sync unit");
        }
    }

    private void mapChanged(int index) {
        mapState.changed(index);
    }

    @Override
    public SyncBlob syncBlob() {
        return mapState;
    }

    @Override
    public void flushSyncBlob() {
        mapState.flush(this, 50, showMap);
    }

    @Override
    public void writeInitialExtras(ByteBuf output) {
        output.writeBoolean(showMap);
        if (showMap) mapState.writeInitial(output);
    }

    @Override
    public void readInitialExtras(ByteBuf input) {
        if (input.readBoolean()) mapState.readInitial(input);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        if (showMap) ChunkTrackerIndex.initialBlob(this, mapState.revision());
        return tag;
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.power = input.getLongOr("power", 0L);
        this.scanMissiles = input.getBooleanOr("scanMissiles", false);
        this.scanShells = input.getBooleanOr("scanShells", false);
        this.scanPlayers = input.getBooleanOr("scanPlayers", false);
        this.smartMode = input.getBooleanOr("smartMode", false);
        this.redMode = input.getBooleanOr("redMode", false);
        this.showMap = input.getBooleanOr("showMap", false);
        byte[] saved = input.read("map", ExtraCodecs.BASE64_STRING).orElse(null);
        if (saved != null && saved.length == MAP_SIZE) {
            mapState.copy(saved);
            syncChanged(2);
        }
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

    @FunctionalInterface
    public interface RadarConverter {

        @Nullable RadarEntry convert(Entity entity, Object radar, RadarScanParams params);
    }
}
