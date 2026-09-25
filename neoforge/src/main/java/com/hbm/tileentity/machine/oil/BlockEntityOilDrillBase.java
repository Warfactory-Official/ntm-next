// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine.oil;

import com.hbm.api.energymk2.IEnergyHandlerMK2;
import com.hbm.api.energymk2.ItemEnergyTransfer;
import com.hbm.api.fluidmk2.FluidFlushOutputs;
import com.hbm.api.fluidmk2.FluidFlushSender;
import com.hbm.blocks.ModBlocks;
import com.hbm.inventory.container.MenuMachineOilWell;
import com.hbm.inventory.fluid.FluidStackNTM;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.items.ModDataComponents;
import com.hbm.items.machine.upgrade.ItemMachineUpgrade;
import com.hbm.items.machine.upgrade.UpgradeManager;
import com.hbm.items.machine.upgrade.UpgradeType;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.sound.ModSounds;
import com.hbm.tileentity.BlockEntityMachineBase;
import com.hbm.tileentity.IFluidCopiable;
import com.hbm.tileentity.IUpgradeInfoProvider;
import com.hbm.tileentity.PersistentDrop;
import com.hbm.util.BobMathUtil;
import com.hbm.util.TickPhase;
import io.netty.buffer.ByteBuf;
import java.util.HashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public abstract class BlockEntityOilDrillBase extends BlockEntityMachineBase
        implements IEnergyHandlerMK2,
                FluidFlushSender,
                IUpgradeInfoProvider,
                MenuProvider,
                PersistentDrop,
                IFluidCopiable,
                SyncUnitSchema {

    public static final int SLOT_BATTERY = 0;
    public static final int SLOT_OIL_IN = 1;
    public static final int SLOT_OIL_OUT = 2;
    public static final int SLOT_GAS_IN = 3;
    public static final int SLOT_GAS_OUT = 4;
    public static final int SLOT_UPGRADE_START = 5;
    public static final int SLOT_UPGRADE_END = 7;
    public static final int SLOT_COUNT = 8;

    private static final int[] VALID_UPGRADES =
            IUpgradeInfoProvider.upgradeCaps(
                    UpgradeType.SPEED,
                    3,
                    UpgradeType.POWER,
                    3,
                    UpgradeType.AFTERBURN,
                    3,
                    UpgradeType.OVERDRIVE,
                    3);

    public static final int TANK_CAPACITY = 64_000;

    @SyncField(units = 1L << 1)
    public final FluidTankNTM[] tanks = new FluidTankNTM[2];

    protected final UpgradeManager upgradeManager = new UpgradeManager(this);
    private final HashSet<Long> trace = new HashSet<>();

    @SyncField(units = 1L << 5)
    public int indicator = 0;

    private static final String[] PERSISTENT_KEYS = {"power", "oil", "gas"};

    @SyncField(units = 1L << 0)
    public long power;

    @SyncField(units = 1L << 5)
    protected int speedLevel;

    protected int energyLevel;

    @SyncField(units = 1L << 5)
    protected int overLevel;

    private final FluidFlushOutputs flush = new FluidFlushOutputs();

    protected BlockEntityOilDrillBase(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, SLOT_COUNT);
        tanks[0] = new FluidTankNTM(NTMFluids.OIL, TANK_CAPACITY);
        tanks[1] = new FluidTankNTM(NTMFluids.GAS, TANK_CAPACITY);
    }

    @Override
    public FluidTankNTM[] getSendingTanks() {
        return tanks;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        super.setItem(slot, stack);
        if (level == null || level.isClientSide()) return;
        if (slot >= SLOT_UPGRADE_START
                && slot <= SLOT_UPGRADE_END
                && ItemMachineUpgrade.isUpgrade(stack)) {
            level.playSound(
                    null,
                    worldPosition.getX() + 0.5D,
                    worldPosition.getY() + 1.5D,
                    worldPosition.getZ() + 0.5D,
                    ModSounds.UPGRADE_PLUG.get(),
                    SoundSource.BLOCKS,
                    1.0F,
                    1.0F);
        }
    }

    public abstract long getMaxPower();

    public abstract long getPowerReq();

    public abstract int getDelay();

    protected void onDrill(BlockPos pos) {}

    protected abstract void onSuck(BlockPos pos);

    protected int getDrillDepth() {
        return level.getMinY() + 5;
    }

    @Override
    public Component getDisplayName() {
        return getName();
    }

    @Override
    public AbstractContainerMenu createMenu(
            int containerId, Inventory playerInventory, Player player) {
        return new MenuMachineOilWell(containerId, playerInventory, this);
    }

    @Override
    public void tickServer() {
        power += ItemEnergyTransfer.extract(this, SLOT_BATTERY, getMaxPower() - power, false);

        upgradeManager.scan(this, SLOT_UPGRADE_START, SLOT_UPGRADE_END);
        this.speedLevel = upgradeManager.getLevel(UpgradeType.SPEED);
        this.energyLevel = upgradeManager.getLevel(UpgradeType.POWER);
        this.overLevel = upgradeManager.getLevel(UpgradeType.OVERDRIVE) + 1;
        int abLevel = upgradeManager.getLevel(UpgradeType.AFTERBURN);

        int toBurn = Math.min(tanks[1].getFill(), abLevel * 10);
        if (toBurn > 0) {
            tanks[1].setFill(tanks[1].getFill() - toBurn);
            power = Math.min(power + toBurn * 5L, getMaxPower());
            setChanged();
        }

        boolean unloaded = tanks[0].unloadTank(SLOT_OIL_IN, SLOT_OIL_OUT, inventory);
        unloaded |= tanks[1].unloadTank(SLOT_GAS_IN, SLOT_GAS_OUT, inventory);
        if (unloaded) setChanged();

        flush.provide((ServerLevel) level, this);

        if (power >= getPowerReqEff()
                && tanks[0].getFill() < tanks[0].getMaxFill()
                && tanks[1].getFill() < tanks[1].getMaxFill()) {
            power -= getPowerReqEff();

            if (TickPhase.every(this, getDelayEff())) {
                this.indicator = 0;
                boolean foundFloor = false;

                for (int y = worldPosition.getY() - 1; y >= getDrillDepth(); y--) {
                    BlockPos pos = new BlockPos(worldPosition.getX(), y, worldPosition.getZ());

                    if (!level.getBlockState(pos).is(ModBlocks.OIL_PIPE.get())) {
                        if (trySuck(pos)) {
                            break;
                        } else {
                            tryDrill(pos);
                            break;
                        }
                    }
                    if (y == getDrillDepth()) foundFloor = true;
                }
                if (foundFloor) this.indicator = 1;
            }
        } else {
            this.indicator = 2;
        }

        setChanged();
        networkPackNT(25);
    }

    private long getPowerReqEff() {
        long req = getPowerReq();
        return (req + (req / 4 * speedLevel) - (req / 4 * energyLevel)) * overLevel;
    }

    private long getDelayEff() {
        long delay = getDelay();
        return Math.max(
                (delay - (delay / 4 * speedLevel) + (delay / 10 * energyLevel)) / overLevel, 1);
    }

    private void tryDrill(BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock().getExplosionResistance() < 1000F) {
            onDrill(pos);
            level.setBlock(pos, ModBlocks.OIL_PIPE.get().defaultBlockState(), 3);
        } else {
            this.indicator = 2;
        }
    }

    private boolean trySuck(BlockPos pos) {
        Block b = level.getBlockState(pos).getBlock();
        if (!canSuckBlock(b)) return false;
        if (!canPump()) return true;

        trace.clear();
        return suckRec(pos, 0);
    }

    protected boolean canSuckBlock(Block b) {
        return b == ModBlocks.ORE_OIL.get() || b == ModBlocks.ORE_OIL_EMPTY.get();
    }

    protected boolean canPump() {
        return true;
    }

    public @Nullable FluidTankNTM extraTank() {
        return null;
    }

    private boolean suckRec(BlockPos pos, int layer) {
        long key = pos.asLong();
        if (!trace.add(key)) return false;
        if (layer > 64) return false;

        Block b = level.getBlockState(pos).getBlock();
        if (b == ModBlocks.ORE_OIL.get() || b == ModBlocks.ORE_BEDROCK_OIL.get()) {
            doSuck(pos);
            return true;
        }

        if (b == ModBlocks.ORE_OIL_EMPTY.get()) {
            for (Direction dir : BobMathUtil.getShuffledDirs()) {
                if (suckRec(pos.relative(dir), layer + 1)) return true;
            }
        }
        return false;
    }

    protected void doSuck(BlockPos pos) {
        if (level.getBlockState(pos).is(ModBlocks.ORE_OIL.get())) {
            onSuck(pos);
        }
    }

    @Override
    public long getPower() {
        return power;
    }

    @Override
    public void setPower(long p) {
        this.power = Math.max(0L, Math.min(p, getMaxPower()));
    }

    @Override
    public long getFluidAvailable(Fluid type, int pressure) {
        for (FluidTankNTM tank : tanks) {
            if (tank.provides(type) && pressure == tank.getPressure()) return tank.getFill();
        }
        return 0L;
    }

    @Override
    public void useUpFluid(Fluid type, int pressure, long amount) {
        for (FluidTankNTM tank : tanks) {
            if (tank.provides(type) && pressure == tank.getPressure()) {
                int drained = tank.drain((int) Math.min(amount, Integer.MAX_VALUE), true);
                if (drained > 0) setChanged();
                return;
            }
        }
    }

    @Override
    public int[] getValidUpgrades() {
        return VALID_UPGRADES;
    }

    @Override
    public FluidTankNTM[] getAllTanks() {
        return drillTanks();
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        power = input.getLongOr("power", 0L);
        input.child("oil").ifPresent(tanks[0]::deserialize);
        input.child("gas").ifPresent(tanks[1]::deserialize);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("power", power);
        tanks[0].serialize(output.child("oil"));
        tanks[1].serialize(output.child("gas"));
    }

    protected FluidTankNTM[] drillTanks() {
        return tanks;
    }

    @Override
    public void writePersistent(DataComponentMap.Builder components) {
        FluidTankNTM[] persistent = drillTanks();
        boolean empty = power == 0;
        for (FluidTankNTM tank : persistent) if (tank.getFill() > 0) empty = false;
        if (empty) return;
        components.set(
                ModDataComponents.DRILL_CONTENTS.get(),
                new DrillContents(power, FluidStackNTM.snapshot(persistent)));
    }

    @Override
    public void readPersistent(DataComponentGetter components) {
        DrillContents contents = components.get(ModDataComponents.DRILL_CONTENTS.get());
        if (contents == null) return;
        power = contents.power();
        FluidStackNTM.restore(contents.tanks(), drillTanks());
    }

    @Override
    public String[] persistentKeys() {
        return PERSISTENT_KEYS;
    }

    private void writeTanks(ByteBuf output) {
        for (int i = 0; i < 2; i++) tanks[i].packetSerialize(output);
    }

    private void readTanks(ByteBuf input) {
        for (int i = 0; i < 2; i++) tanks[i].packetDeserialize(input);
    }

    private void writeDrillState(ByteBuf output) {
        output.writeInt(indicator);
    }

    private void readDrillState(ByteBuf input) {
        indicator = input.readInt();
    }

    @Override
    public long syncUnitMask() {
        return 0x23L;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeLong(this.power);
            case 1 -> writeTanks(output);
            case 5 -> writeDrillState(output);
            default -> throw new IllegalArgumentException();
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> this.power = input.readLong();
            case 1 -> readTanks(input);
            case 5 -> readDrillState(input);
            default -> throw new IllegalArgumentException();
        }
    }
}
