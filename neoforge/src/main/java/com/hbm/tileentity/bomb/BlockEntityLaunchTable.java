// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.bomb;

import com.hbm.api.block.IRadarCommandReceiver;
import com.hbm.api.control.IControlReceiver;
import com.hbm.api.energymk2.IEnergyHandlerMK2;
import com.hbm.api.energymk2.ItemEnergyTransfer;
import com.hbm.api.fluidmk2.IFluidHandlerMK2;
import com.hbm.api.item.IDesignatorItem;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.entity.ModEntities;
import com.hbm.entity.missile.EntityMissileCustom;
import com.hbm.handler.MissileStruct;
import com.hbm.interfaces.IBomb;
import com.hbm.inventory.container.MenuLaunchTable;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.items.ModItems;
import com.hbm.items.weapon.ItemCustomMissile;
import com.hbm.items.weapon.ItemCustomMissilePart.FuelType;
import com.hbm.items.weapon.ItemCustomMissilePart.PartSize;
import com.hbm.items.weapon.ItemCustomMissilePart;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncSlots;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.particle.HbmParticles;
import com.hbm.sound.ModSounds;
import com.hbm.tileentity.BlockEntityMachineBase;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

@SyncSlots(
        value = {BlockEntityLaunchTable.SLOT_MISSILE},
        units = 1L << 6)
public class BlockEntityLaunchTable extends BlockEntityMachineBase
        implements IEnergyHandlerMK2,
                IFluidHandlerMK2,
                IControlReceiver,
                IRadarCommandReceiver,
                MenuProvider,
                SyncUnitSchema {

    public static final int SLOT_MISSILE = 0;
    public static final int SLOT_DESIGNATOR = 1;
    public static final int SLOT_FUEL_IN = 2;
    public static final int SLOT_OXIDIZER_IN = 3;
    public static final int SLOT_SOLID = 4;
    public static final int SLOT_BATTERY = 5;
    public static final int SLOT_FUEL_OUT = 6;
    public static final int SLOT_OXIDIZER_OUT = 7;
    public static final int SLOT_COUNT = 8;

    public static final long MAX_POWER = 100_000L;
    public static final int MAX_SOLID = 100_000;
    public static final int TANK_CAPACITY = 100_000;

    public static final int SOLID_PER_ITEM = 250;

    private static final double LAUNCH_OFFSET = 2.5D;
    private static final int FOOTPRINT_RADIUS = 4;

    private static final int[] ACCESSIBLE_SLOTS = {SLOT_MISSILE};

    @SyncField(units = 1L << 4)
    public final FluidTankNTM fuelTank = new FluidTankNTM(TANK_CAPACITY);

    @SyncField(units = 1L << 5)
    public final FluidTankNTM oxidizerTank = new FluidTankNTM(TANK_CAPACITY);

    private final FluidTankNTM[] tanks = {fuelTank, oxidizerTank};

    @SyncField(units = 1L)
    public long power;

    @SyncField(units = 1L << 1)
    public int solid;

    @SyncField(units = 1L << 2)
    public PartSize padSize = PartSize.SIZE_10;

    @SyncField(units = 1L << 3)
    public boolean missileValid;

    public MissileStruct loadedMissile = MissileStruct.EMPTY;

    public int height = 10;
    private boolean redstone;

    public BlockEntityLaunchTable(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LARGE_LAUNCH_TABLE.get(), pos, state, SLOT_COUNT);
    }

    @Override
    public void tickServer() {
        updateTypes();

        boolean loaded = fuelTank.loadTank(SLOT_FUEL_IN, SLOT_FUEL_OUT, inventory);
        loaded |= oxidizerTank.loadTank(SLOT_OXIDIZER_IN, SLOT_OXIDIZER_OUT, inventory);
        if (loaded) setChanged();

        power += ItemEnergyTransfer.extract(this, SLOT_BATTERY, MAX_POWER - power, false);

        ItemStack fuel = getItem(SLOT_SOLID);
        if (fuel.is(ModItems.ROCKET_FUEL.get()) && solid + SOLID_PER_ITEM <= MAX_SOLID) {
            removeItem(SLOT_SOLID, 1);
            solid += SOLID_PER_ITEM;
        }

        missileValid = isMissileValid();
        networkPackNT(50);

        if (redstone && canLaunch()) launchFromDesignator();
    }

    @Override
    public void tickClient() {
        AABB plume =
                new AABB(
                        worldPosition.getX() - .5D,
                        worldPosition.getY(),
                        worldPosition.getZ() - .5D,
                        worldPosition.getX() + 1.5D,
                        worldPosition.getY() + 10D,
                        worldPosition.getZ() + 1.5D);
        if (level.getEntitiesOfClass(EntityMissileCustom.class, plume).isEmpty()) return;

        for (int i = 0; i < 15; i++) {

            boolean alongZ = level.getRandom().nextBoolean();
            double motionX = alongZ ? 0D : level.getRandom().nextGaussian() * 0.65D;
            double motionZ = alongZ ? level.getRandom().nextGaussian() * 0.65D : 0D;
            level.addParticle(
                    HbmParticles.LAUNCH_SMOKE.get(),
                    true,
                    false,
                    worldPosition.getX() + .5D,
                    worldPosition.getY() + .25D,
                    worldPosition.getZ() + .5D,
                    motionX,
                    0D,
                    motionZ);
        }
    }

    public void refreshRedstone() {
        BlockPos core = getBlockPos();
        boolean powered = false;
        for (int dx = -FOOTPRINT_RADIUS; dx <= FOOTPRINT_RADIUS && !powered; dx++) {
            for (int dz = -FOOTPRINT_RADIUS; dz <= FOOTPRINT_RADIUS && !powered; dz++) {
                if (getLevel().hasNeighborSignal(core.offset(dx, 0, dz))) powered = true;
            }
        }
        redstone = powered;
    }

    private MissileStruct struct() {
        return ItemCustomMissile.getStruct(getItem(SLOT_MISSILE));
    }

    private static @Nullable ItemCustomMissilePart fuselageOf(ItemStack missile) {
        MissileStruct multipart = ItemCustomMissile.getStruct(missile);
        return multipart == null ? null : multipart.fuselage();
    }

    private void updateTypes() {
        MissileStruct multipart = struct();
        if (multipart == null || multipart.fuselage() == null) return;

        switch (multipart.fuselage().fuelType()) {
            case KEROSENE -> {
                fuelTank.setTankType(NTMFluids.KEROSENE);
                oxidizerTank.setTankType(NTMFluids.PEROXIDE);
            }
            case HYDROGEN -> {
                fuelTank.setTankType(NTMFluids.HYDROGEN);
                oxidizerTank.setTankType(NTMFluids.OXYGEN);
            }
            case XENON -> fuelTank.setTankType(NTMFluids.XENON);
            case BALEFIRE -> {
                fuelTank.setTankType(NTMFluids.BALEFIRE_FUEL);
                oxidizerTank.setTankType(NTMFluids.PEROXIDE);
            }
            case SOLID -> {}
        }
    }

    public boolean isMissileValid() {
        return isMissileValid(getItem(SLOT_MISSILE));
    }

    public boolean isMissileValid(ItemStack missile) {
        ItemCustomMissilePart fuselage = fuselageOf(missile);
        return fuselage != null && fuselage.top == padSize;
    }

    public boolean hasDesignator() {
        return hasDesignator(getItem(SLOT_DESIGNATOR));
    }

    public static boolean hasDesignator(ItemStack stack) {
        return stack.getItem() instanceof IDesignatorItem designator && designator.isReady(stack);
    }

    public int solidState(ItemStack missile) {
        ItemCustomMissilePart fuselage = fuselageOf(missile);
        if (fuselage == null || fuselage.fuelType() != FuelType.SOLID) return -1;
        return solid >= fuselage.fuelAmount() ? 1 : 0;
    }

    public int liquidState(ItemStack missile) {
        ItemCustomMissilePart fuselage = fuselageOf(missile);
        if (fuselage == null) return -1;
        return switch (fuselage.fuelType()) {
            case KEROSENE, HYDROGEN, XENON, BALEFIRE ->
                    fuelTank.getFill() >= fuselage.fuelAmount() ? 1 : 0;
            default -> -1;
        };
    }

    public int oxidizerState(ItemStack missile) {
        ItemCustomMissilePart fuselage = fuselageOf(missile);
        if (fuselage == null) return -1;
        return switch (fuselage.fuelType()) {
            case KEROSENE, HYDROGEN, BALEFIRE ->
                    oxidizerTank.getFill() >= fuselage.fuelAmount() ? 1 : 0;
            default -> -1;
        };
    }

    public boolean hasFuel() {
        ItemStack missile = getItem(SLOT_MISSILE);
        return solidState(missile) != 0 && liquidState(missile) != 0 && oxidizerState(missile) != 0;
    }

    public boolean canLaunch() {
        return power >= MAX_POWER * 0.75 && isMissileValid() && hasFuel();
    }

    @Override
    public boolean sendCommandEntity(Entity target) {
        return sendCommandPosition(
                (int) Math.floor(target.getX()),
                getBlockPos().getY(),
                (int) Math.floor(target.getZ()));
    }

    @Override
    public boolean sendCommandPosition(int x, int y, int z) {
        if (!canLaunch()) return false;
        launchTo(x, z);
        return true;
    }

    public IBomb.BombReturnCode launchFromDesignator() {
        ItemStack stack = getItem(SLOT_DESIGNATOR);
        if (!(stack.getItem() instanceof IDesignatorItem designator)
                || !designator.isReady(stack)) {
            return IBomb.BombReturnCode.ERROR_MISSING_COMPONENT;
        }
        launchTo(designator.getTargetX(stack), designator.getTargetZ(stack));
        return IBomb.BombReturnCode.LAUNCHED;
    }

    public void launchTo(int targetX, int targetZ) {
        BlockPos core = getBlockPos();
        getLevel()
                .playSound(
                        null,
                        core,
                        ModSounds.MISSILE_TAKE_OFF.get(),
                        SoundSource.BLOCKS,
                        10.0F,
                        1.0F);

        MissileStruct multipart = struct();
        ItemStack missileStack = getItem(SLOT_MISSILE);

        float chip = ItemCustomMissile.getChip(missileStack).inaccuracy();
        float fins = multipart.fins() != null ? multipart.fins().inaccuracy() : 1.0F;

        double scatterX = (core.getX() - targetX) * chip * fins;
        double scatterZ = (core.getZ() - targetZ) * chip * fins;
        float angle = getLevel().getRandom().nextFloat() * 360F;
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        double rotatedX = scatterX * cos + scatterZ * sin;
        double rotatedZ = scatterZ * cos - scatterX * sin;

        EntityMissileCustom missile =
                new EntityMissileCustom(ModEntities.MISSILE_CUSTOM.get(), getLevel());
        missile.launch(
                core.getX() + 0.5D,
                core.getY() + LAUNCH_OFFSET,
                core.getZ() + 0.5D,
                targetX + (int) rotatedX,
                targetZ + (int) rotatedZ,
                stackOf(multipart.warhead()),
                stackOf(multipart.fuselage()),
                stackOf(multipart.fins()),
                stackOf(multipart.thruster()));
        getLevel().addFreshEntity(missile);

        subtractFuel(multipart);
        setItem(SLOT_MISSILE, ItemStack.EMPTY);
    }

    private static ItemStack stackOf(ItemCustomMissilePart part) {
        return part == null ? ItemStack.EMPTY : new ItemStack(part);
    }

    private void subtractFuel(MissileStruct multipart) {
        if (multipart == null || multipart.fuselage() == null) return;
        ItemCustomMissilePart fuselage = multipart.fuselage();
        int fuel = (int) fuselage.fuelAmount();

        switch (fuselage.fuelType()) {
            case KEROSENE, HYDROGEN, BALEFIRE -> {
                fuelTank.setFill(fuelTank.getFill() - fuel);
                oxidizerTank.setFill(oxidizerTank.getFill() - fuel);
            }
            case XENON -> fuelTank.setFill(fuelTank.getFill() - fuel);
            case SOLID -> solid -= fuel;
        }

        power -= (long) (MAX_POWER * 0.75);
    }

    @Override
    public boolean hasPermission(Player player) {
        return true;
    }

    @Override
    public void receiveControl(CompoundTag data) {
        if (data.contains("padSize")) {
            int ordinal = data.getIntOr("padSize", padSize.ordinal());
            PartSize[] sizes = PartSize.values();
            if (ordinal >= 0 && ordinal < sizes.length) padSize = sizes[ordinal];
        }
        setChanged();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {

        return false;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return ACCESSIBLE_SLOTS;
    }

    @Override
    public long getPower() {
        return power;
    }

    @Override
    public void setPower(long p) {
        power = p;
    }

    @Override
    public long getMaxPower() {
        return MAX_POWER;
    }

    @Override
    public FluidTankNTM[] getAllTanks() {
        return tanks;
    }

    @Override
    public long getDemand(Fluid type, int pressure) {
        if (pressure != 0) return 0;
        long headroom = 0;
        for (FluidTankNTM tank : tanks)
            if (tank.accepts(type)) headroom += tank.getMaxFill() - tank.getFill();
        return headroom;
    }

    @Override
    public long transferFluid(Fluid type, int pressure, long amount) {
        if (pressure != 0) return amount;
        long remaining = amount;
        for (FluidTankNTM tank : tanks) {
            if (remaining <= 0) break;
            if (tank.accepts(type)) {
                remaining -= tank.fill(type, (int) Math.min(remaining, Integer.MAX_VALUE), true);
            }
        }
        return remaining;
    }

    private RegistryFriendlyByteBuf registryBuf(ByteBuf buf) {
        return new RegistryFriendlyByteBuf(buf, level.registryAccess());
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.getLong("power").ifPresent(v -> power = v);
        solid = input.getIntOr("solidfuel", 0);
        padSize =
                PartSize.values()[
                        Math.clamp(
                                input.getIntOr("padSize", PartSize.SIZE_10.ordinal()),
                                0,
                                PartSize.values().length - 1)];
        redstone = input.getBooleanOr("redstone", false);
        input.child("fuel").ifPresent(fuelTank::deserialize);
        input.child("oxidizer").ifPresent(oxidizerTank::deserialize);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("power", power);
        output.putInt("solidfuel", solid);
        output.putInt("padSize", padSize.ordinal());
        output.putBoolean("redstone", redstone);
        fuelTank.serialize(output.child("fuel"));
        oxidizerTank.serialize(output.child("oxidizer"));
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.launchTable");
    }

    @Override
    public Component getDisplayName() {
        return getName();
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new MenuLaunchTable(containerId, inventory, this);
    }

    private void writePadSize(ByteBuf output) {
        output.writeByte(padSize.ordinal());
    }

    private void readPadSize(ByteBuf input) {
        padSize = PartSize.values()[input.readByte()];
    }

    private void writeLoadedMissile(ByteBuf output) {
        MissileStruct multipart = struct();
        MissileStruct.STREAM_CODEC.encode(
                registryBuf(output), multipart == null ? MissileStruct.EMPTY : multipart);
    }

    private void readLoadedMissile(ByteBuf input) {
        loadedMissile = MissileStruct.STREAM_CODEC.decode(registryBuf(input)).sanitised();
    }

    @Override
    public long syncUnitMask() {
        return 0x7fL;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeLong(this.power);
            case 1 -> output.writeInt(this.solid);
            case 2 -> writePadSize(output);
            case 3 -> output.writeBoolean(this.missileValid);
            case 4 -> this.fuelTank.packetSerialize(output);
            case 5 -> this.oxidizerTank.packetSerialize(output);
            case 6 -> writeLoadedMissile(output);
            default -> throw new IllegalArgumentException();
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> this.power = input.readLong();
            case 1 -> this.solid = input.readInt();
            case 2 -> readPadSize(input);
            case 3 -> this.missileValid = input.readBoolean();
            case 4 -> this.fuelTank.packetDeserialize(input);
            case 5 -> this.oxidizerTank.packetDeserialize(input);
            case 6 -> readLoadedMissile(input);
            default -> throw new IllegalArgumentException();
        }
    }
}
