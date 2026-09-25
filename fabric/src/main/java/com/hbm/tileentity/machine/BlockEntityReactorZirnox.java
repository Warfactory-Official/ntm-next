// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: Contributors to Hbm's Nuclear Tech Mod
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine;

import com.hbm.advancement.AwardRegions;
import com.hbm.advancement.DetonationTrigger;
import com.hbm.advancement.HbmCriteria;
import com.hbm.api.control.IControlReceiver;
import com.hbm.api.fluidmk2.FluidFlushOutputs;
import com.hbm.api.fluidmk2.FluidTankEndpoint;
import com.hbm.api.redstoneoverradio.IRORInteractive;
import com.hbm.api.redstoneoverradio.IRORValueProvider;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.machine.ZirnoxDestroyed;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.entity.projectile.EntityZirnoxDebris.DebrisType;
import com.hbm.entity.projectile.EntityZirnoxDebris;
import com.hbm.explosion.ExplosionNukeGeneric;
import com.hbm.handler.BossSpawnHandler;
import com.hbm.inventory.container.MenuReactorZirnox;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.items.ModItems;
import com.hbm.items.machine.ItemZirnoxRod.EnumZirnoxType;
import com.hbm.items.machine.ItemZirnoxRod;
import com.hbm.items.tool.ItemSwordMeteorite;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.particle.helper.ExplosionCreator;
import com.hbm.saveddata.satellites.SatelliteRayEvents;
import com.hbm.sound.ModSounds;
import com.hbm.tileentity.BlockEntityMachineBase;
import com.hbm.tileentity.Tiltable;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public class BlockEntityReactorZirnox extends BlockEntityMachineBase
        implements FluidTankEndpoint,
                MenuProvider,
                IControlReceiver,
                IRORValueProvider,
                IRORInteractive,
                SyncUnitSchema,
                Tiltable {

    public static final int maxHeat = 100000;
    public static final int maxPressure = 100000;
    public static final String[] ROR =
            new String[] {
                PREFIX_VALUE + "heat",
                PREFIX_VALUE + "pressure",
                PREFIX_VALUE + "water",
                PREFIX_VALUE + "steam",
                PREFIX_VALUE + "co2",
                PREFIX_VALUE + "state",
                PREFIX_VALUE + "slottype",
                PREFIX_VALUE + "slotdep",
                PREFIX_VALUE + "slotmaxdep",
                PREFIX_FUNCTION + "setslot" + NAME_SEPARATOR + "id (0 to 23)",
                PREFIX_FUNCTION + "setstate" + NAME_SEPARATOR + "active (0 or 1)",
                PREFIX_FUNCTION + "ventco2"
            };
    private static final int[][] NEIGHBOURS = {
        {1, 7},
        {0, 2, 8},
        {1, 9},
        {4, 10},
        {3, 5, 11},
        {4, 6, 12},
        {5, 13},
        {0, 8, 14},
        {1, 7, 9, 15},
        {2, 8, 16},
        {3, 11, 17},
        {4, 10, 12, 18},
        {5, 11, 13, 19},
        {6, 12, 20},
        {7, 15, 21},
        {8, 14, 16, 22},
        {9, 15, 23},
        {10, 18},
        {11, 17, 19},
        {12, 18, 20},
        {13, 19},
        {14, 22},
        {15, 21, 23},
        {16, 22}
    };
    private static final int[] ACCESSIBLE_SLOTS = {
        0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23
    };

    @SyncField(units = 1L << 4)
    public final FluidTankNTM steam = new FluidTankNTM(NTMFluids.SUPERHOTSTEAM, 8000);

    private final FluidTankNTM[] receiving;
    private final FluidTankNTM[] sending;

    @SyncField(units = 1L << 5)
    public final FluidTankNTM carbonDioxide = new FluidTankNTM(NTMFluids.CARBONDIOXIDE, 16000);

    @SyncField(units = 1L << 6)
    public final FluidTankNTM water = new FluidTankNTM(NTMFluids.WATER, 32000);

    @SyncField(units = 1L << 0)
    public int heat;

    @SyncField(units = 1L << 1)
    public int pressure;

    @SyncField(units = 1L << 2)
    public boolean isOn = false;

    protected int output;

    @SyncField(units = 1L << 3)
    private boolean redstonePowered = false;

    private int rorSlot;

    private final FluidFlushOutputs flush = new FluidFlushOutputs();

    public BlockEntityReactorZirnox(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ZIRNOX.get(), pos, state, 28);
        receiving = new FluidTankNTM[] {water, carbonDioxide};
        sending = new FluidTankNTM[] {steam};
    }

    public void setRedstonePowered(boolean powered) {
        if (!powered && this.redstonePowered) isOn = false;
        this.redstonePowered = powered;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.zirnox");
    }

    @Override
    public void tickServer() {
        checkTilt(TiltType.CONFIG, true);

        if (redstonePowered) isOn = true;
        this.output = 0;

        boolean loaded = carbonDioxide.loadTank(24, 26, inventory);
        loaded |= water.loadTank(25, 27, inventory);
        if (loaded) setChanged();

        if (isOn) {
            for (int i = 0; i < 24; i++) {
                if (inventory.get(i).getItem() instanceof ItemZirnoxRod) decay(i);
                else
                    ItemSwordMeteorite.upgrade(
                            inventory,
                            i,
                            ModItems.METEORITE_SWORD_BRED,
                            ModItems.METEORITE_SWORD_IRRADIATED);
            }
        }

        this.pressure =
                (carbonDioxide.getFill() * 2)
                        + (int)
                                ((float) heat
                                        * ((float) carbonDioxide.getFill()
                                                / (float) carbonDioxide.getMaxFill()));

        if (heat > 0 && heat < maxHeat) {
            if (water.getFill() > 0
                    && carbonDioxide.getFill() > 0
                    && steam.getFill() < steam.getMaxFill()) {
                generateSteam();
                heat -= (int) ((float) heat * (float) pressure / 1000000F);
            } else {
                heat -= 10;
            }
            if (level.getGameTime() % 100 == 0) {
                SatelliteRayEvents.report(
                        (ServerLevel) level,
                        worldPosition,
                        SatelliteRayEvents.NEUTRON_EMISSION,
                        200);
            }
        }

        if (pressure > maxPressure || heat > maxHeat) meltdown();

        if (!isTilted()) flush.provide((ServerLevel) level, this);

        networkPackNT(150);
    }

    @Override
    public int getFloorCount() {
        return 3 * 3;
    }

    @Override
    public BlockPos getFloorPosFromIndex(int index) {
        return standardFloor5x5(index);
    }

    private void generateSteam() {
        if (heat > 10256) {
            int cycle =
                    (int)
                            ((((float) heat - 10256F) / (float) maxHeat)
                                    * Math.min((float) carbonDioxide.getFill() / 14000F, 1F)
                                    * 25F
                                    * 5F);
            this.output = cycle;
            water.setFill(water.getFill() - cycle);
            steam.setFill(steam.getFill() + cycle);
            if (water.getFill() < 0) water.setFill(0);
            if (steam.getFill() > steam.getMaxFill()) steam.setFill(steam.getMaxFill());
        }
    }

    private boolean hasFuelRod(int id) {
        if (inventory.get(id).getItem() instanceof ItemZirnoxRod rod) return !rod.type.breeding;
        return false;
    }

    private int getNeighbourCount(int id) {
        int count = 0;
        for (int neighbour : NEIGHBOURS[id]) if (hasFuelRod(neighbour)) count++;
        return count;
    }

    private void decay(int id) {
        int decay = getNeighbourCount(id);
        ItemStack stack = inventory.get(id);
        EnumZirnoxType num = ((ItemZirnoxRod) stack.getItem()).type;

        if (!num.breeding) decay++;

        for (int i = 0; i < decay; i++) {
            this.heat += num.heat;
            ItemZirnoxRod.incrementLifeTime(stack);

            if (ItemZirnoxRod.getLifeTime(stack) > num.maxLife) {
                ItemLike out = ModItems.spentZirnoxFuel(stack.getItem());
                inventory.set(id, out != null ? new ItemStack(out) : ItemStack.EMPTY);
                break;
            }
        }
        setChanged();
    }

    private void meltdown() {
        inventory.clear();

        Direction dir = getBlockState().getValue(BlockMultiblockCore.FACING);

        BlockMultiblockCore.withoutTeardown(
                () -> {
                    for (int ox = -2; ox <= 2; ox++)
                        for (int oz = -2; oz <= 2; oz++)
                            for (int oy = 2; oy <= 5; oy++)
                                level.setBlock(
                                        worldPosition.offset(ox, oy, oz),
                                        Blocks.AIR.defaultBlockState(),
                                        3);

                    ZirnoxDestroyed shell = ModBlocks.ZIRNOX_DESTROYED.get();
                    level.setBlock(
                            worldPosition,
                            shell.defaultBlockState().setValue(BlockMultiblockCore.FACING, dir),
                            3);
                    shell.fillSpace(level, worldPosition, dir);
                });

        double cx = worldPosition.getX() + 0.5,
                cy = worldPosition.getY() + 0.5,
                cz = worldPosition.getZ() + 0.5;
        ExplosionCreator.composeEffectRBMKMush(level, cx, cy, cz, 9F);
        level.playSound(
                null,
                worldPosition.getX(),
                worldPosition.getY() + 2,
                worldPosition.getZ(),
                ModSounds.RBMK_EXPLOSION.get(),
                SoundSource.BLOCKS,
                10.0F,
                1.0F);

        if (level instanceof ServerLevel serverLevel) {
            level.explode(
                    null,
                    worldPosition.getX(),
                    worldPosition.getY() + 3,
                    worldPosition.getZ(),
                    12.0F,
                    false,
                    Level.ExplosionInteraction.BLOCK);
            zirnoxDebris();
            ExplosionNukeGeneric.waste(level, worldPosition, 35, serverLevel.getRandom());

            AwardRegions.within(
                    serverLevel,
                    cx,
                    cy,
                    cz,
                    100,
                    p -> HbmCriteria.detonation(p, DetonationTrigger.Kind.ZIRNOX));
            BossSpawnHandler.markRad(serverLevel, cx, cy, cz);
        }
    }

    private void zirnoxDebris() {
        for (int i = 0; i < 2; i++) spawnDebris(DebrisType.EXCHANGER);
        for (int i = 0; i < 20; i++) {
            spawnDebris(DebrisType.CONCRETE);
            spawnDebris(DebrisType.BLANK);
        }
        for (int i = 0; i < 10; i++) {
            spawnDebris(DebrisType.ELEMENT);
            spawnDebris(DebrisType.GRAPHITE);
            spawnDebris(DebrisType.SHRAPNEL);
        }
    }

    private void spawnDebris(DebrisType type) {
        EntityZirnoxDebris debris =
                new EntityZirnoxDebris(
                        level,
                        worldPosition.getX() + 0.5D,
                        worldPosition.getY() + 4D,
                        worldPosition.getZ() + 0.5D,
                        type);
        RandomSource rand = level.getRandom();
        double mx = rand.nextGaussian() * 0.75D;
        double mz = rand.nextGaussian() * 0.75D;
        double my = 0.01D + rand.nextDouble() * 1.25D;

        if (type == DebrisType.CONCRETE) {
            mx *= 0.25D;
            my += rand.nextDouble();
            mz *= 0.25D;
        }
        if (type == DebrisType.EXCHANGER) {
            mx += 0.5D;
            my *= 0.1D;
            mz += 0.5D;
        }

        debris.setDeltaMovement(mx, my, mz);
        level.addFreshEntity(debris);
    }

    public int getGaugeScaled(int i, int type) {
        return switch (type) {
            case 0 -> (steam.getFill() * i) / steam.getMaxFill();
            case 1 -> (carbonDioxide.getFill() * i) / carbonDioxide.getMaxFill();
            case 2 -> (water.getFill() * i) / water.getMaxFill();
            case 3 -> (heat * i) / maxHeat;
            case 4 -> (pressure * i) / maxPressure;
            default -> 1;
        };
    }

    @Override
    public void receiveControl(CompoundTag data) {
        if (data.contains("control") && !redstonePowered) this.isOn = !this.isOn;
        if (data.contains("vent"))
            carbonDioxide.setFill(Math.max(carbonDioxide.getFill() - 1000, 0));
        setChanged();
    }

    @Override
    public boolean hasPermission(Player player) {
        return player.getEyePosition()
                        .distanceToSqr(
                                worldPosition.getX(), worldPosition.getY(), worldPosition.getZ())
                < 20 * 20;
    }

    @Override
    public FluidTankNTM[] getReceivingTanks() {
        return receiving;
    }

    @Override
    public FluidTankNTM[] getSendingTanks() {
        return sending;
    }

    @Override
    public long getProviderSpeed(Fluid type, int pressure) {
        return isTilted() ? 0L : FluidTankEndpoint.super.getProviderSpeed(type, pressure);
    }

    @Override
    public boolean netSubscribed() {
        return !isTilted();
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return ACCESSIBLE_SLOTS;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot < 24 && stack.getItem() instanceof ItemZirnoxRod;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot < 24 && !(stack.getItem() instanceof ItemZirnoxRod);
    }

    @Override
    public Component getDisplayName() {
        return getName();
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(
            int containerId, Inventory playerInventory, Player player) {
        return new MenuReactorZirnox(containerId, playerInventory, this);
    }

    @Override
    public String[] getFunctionInfo() {
        return ROR;
    }

    @Override
    public @Nullable String provideRORValue(String name) {
        if ((PREFIX_VALUE + "heat").equals(name))
            return "" + (int) Math.round(heat * 1.0E-5D * 780.0D + 20.0D);
        if ((PREFIX_VALUE + "pressure").equals(name))
            return "" + (int) Math.round(pressure * 1.0E-5D * 30.0D);
        if ((PREFIX_VALUE + "water").equals(name)) return "" + water.getFill();
        if ((PREFIX_VALUE + "steam").equals(name)) return "" + steam.getFill();
        if ((PREFIX_VALUE + "co2").equals(name)) return "" + carbonDioxide.getFill();
        if ((PREFIX_VALUE + "state").equals(name)) return "" + (isOn ? 1 : 0);
        if ((PREFIX_VALUE + "slottype").equals(name)) {
            return hasFuelRod(rorSlot)
                    ? ((ItemZirnoxRod) inventory.get(rorSlot).getItem()).type.legacyName()
                    : "";
        }
        if ((PREFIX_VALUE + "slotdep").equals(name)) {
            return hasFuelRod(rorSlot)
                    ? "" + ItemZirnoxRod.getLifeTime(inventory.get(rorSlot))
                    : null;
        }
        if ((PREFIX_VALUE + "slotmaxdep").equals(name)) {
            return hasFuelRod(rorSlot)
                    ? "" + ((ItemZirnoxRod) inventory.get(rorSlot).getItem()).type.maxLife
                    : null;
        }
        return null;
    }

    @Override
    public @Nullable String runRORFunction(String name, String[] params) {
        if ((PREFIX_FUNCTION + "setslot").equals(name) && params.length > 0) {
            if (redstonePowered) return null;
            try {
                int slot = Integer.parseInt(params[0]);
                if (slot >= 0 && slot < 24) rorSlot = slot;
            } catch (NumberFormatException ignored) {
            }
            return null;
        }
        if ((PREFIX_FUNCTION + "setstate").equals(name) && params.length > 0) {
            if (redstonePowered) return null;

            try {
                isOn = Integer.parseInt(params[0]) == 1;
                setChanged();
            } catch (NumberFormatException ignored) {
            }
            return null;
        }
        if ((PREFIX_FUNCTION + "ventco2").equals(name)) {
            carbonDioxide.setFill(Math.max(carbonDioxide.getFill() - 1000, 0));
            setChanged();
            return null;
        }
        return null;
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        heat = input.getIntOr("heat", 0);
        pressure = input.getIntOr("pressure", 0);
        isOn = input.getBooleanOr("isOn", false);
        redstonePowered = input.getBooleanOr("redstonePowered", false);
        input.child("steam").ifPresent(steam::deserialize);
        input.child("carbondioxide").ifPresent(carbonDioxide::deserialize);
        input.child("water").ifPresent(water::deserialize);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("heat", heat);
        output.putInt("pressure", pressure);
        output.putBoolean("isOn", isOn);
        output.putBoolean("redstonePowered", redstonePowered);
        steam.serialize(output.child("steam"));
        carbonDioxide.serialize(output.child("carbondioxide"));
        water.serialize(output.child("water"));
    }

    @Override
    public long syncUnitMask() {
        return 0x7fL;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeInt(this.heat);
            case 1 -> output.writeInt(this.pressure);
            case 2 -> output.writeBoolean(this.isOn);
            case 3 -> output.writeBoolean(this.redstonePowered);
            case 4 -> this.steam.packetSerialize(output);
            case 5 -> this.carbonDioxide.packetSerialize(output);
            case 6 -> this.water.packetSerialize(output);
            default -> throw new IllegalArgumentException();
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> this.heat = input.readInt();
            case 1 -> this.pressure = input.readInt();
            case 2 -> this.isOn = input.readBoolean();
            case 3 -> this.redstonePowered = input.readBoolean();
            case 4 -> this.steam.packetDeserialize(input);
            case 5 -> this.carbonDioxide.packetDeserialize(input);
            case 6 -> this.water.packetDeserialize(input);
            default -> throw new IllegalArgumentException();
        }
    }
}
