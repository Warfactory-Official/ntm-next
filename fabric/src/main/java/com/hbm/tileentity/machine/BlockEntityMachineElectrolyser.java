// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine;

import com.hbm.api.control.IControlReceiver;
import com.hbm.api.energymk2.IBatteryItem;
import com.hbm.api.energymk2.IEnergyHandlerMK2;
import com.hbm.api.energymk2.ItemEnergyTransfer;
import com.hbm.api.fluidmk2.FluidFlushOutputs;
import com.hbm.api.fluidmk2.FluidFlushSender;
import com.hbm.api.fluidmk2.FlushFaces;
import com.hbm.api.fluidmk2.FlushLanes;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.inventory.IGUIProvider;
import com.hbm.inventory.container.MenuMachineElectrolyserFluid;
import com.hbm.inventory.container.MenuMachineElectrolyserMetal;
import com.hbm.inventory.fluid.FluidStackNTM;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.material.MaterialShapes;
import com.hbm.inventory.material.Mats.MaterialStack;
import com.hbm.inventory.material.Mats;
import com.hbm.inventory.recipes.ElectrolyserFluidRecipe;
import com.hbm.inventory.recipes.ElectrolyserFluidRecipes;
import com.hbm.inventory.recipes.ElectrolyserMetalRecipe;
import com.hbm.inventory.recipes.ElectrolyserMetalRecipes;
import com.hbm.items.machine.FluidIdentifierItem;
import com.hbm.items.machine.upgrade.ItemMachineUpgrade;
import com.hbm.items.machine.upgrade.UpgradeManager;
import com.hbm.items.machine.upgrade.UpgradeType;
import com.hbm.packet.SyncBindings;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncSlots;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.tileentity.BlockEntityMachineBase;
import com.hbm.tileentity.IFluidCopiable;
import com.hbm.tileentity.IMetalCopiable;
import com.hbm.tileentity.IUpgradeInfoProvider;
import com.hbm.util.CrucibleUtil;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

@SyncSlots(
        value = {14},
        units = 1L << 2)
public class BlockEntityMachineElectrolyser extends BlockEntityMachineBase
        implements IEnergyHandlerMK2,
                FluidFlushSender,
                IGUIProvider,
                IControlReceiver,
                IUpgradeInfoProvider,
                IFluidCopiable,
                IMetalCopiable,
                SyncUnitSchema {

    public static final int SLOT_COUNT = 21;

    public static final long MAX_POWER = 20_000_000L;
    public static final int usageOreBase = 10_000;
    public static final int usageFluidBase = 10_000;
    public static final int MAX_MATERIAL = MaterialShapes.BLOCK.q(16);

    private static final int[] ACCESSIBLE_SLOTS = {11, 12, 13, 14, 15, 16, 17, 18, 19, 20};
    private static final int[] VALID_UPGRADES =
            IUpgradeInfoProvider.upgradeCaps(
                    UpgradeType.SPEED, 3, UpgradeType.POWER, 3, UpgradeType.OVERDRIVE, 3);

    @SyncField(units = 0x78L)
    public final FluidTankNTM[] tanks = new FluidTankNTM[4];

    public final List<PourStream> streams = new ArrayList<>();

    @SyncField(units = 0x6L)
    private final UpgradeManager upgradeManager = new UpgradeManager(this);

    @SyncField(units = 1L << 0)
    public long power;

    @SyncField(units = 1L << 2)
    public int usageOre;

    @SyncField(units = 1L << 1)
    public int usageFluid;

    @SyncField(units = 1L << 1)
    public int progressFluid;

    public int processFluidTime = 100;

    @SyncField(units = 1L << 2)
    public int progressOre;

    public int processOreTime = 600;

    @SyncField(units = 1L << 7)
    public @Nullable MaterialStack leftStack;

    @SyncField(units = 1L << 8)
    public @Nullable MaterialStack rightStack;

    @SyncField(units = 1L << 9)
    private int lastSelectedGUI = 0;

    @SyncField(units = 1L << 10)
    private int leftPourColor = -1;

    @SyncField(units = 1L << 10)
    private float leftPourLen;

    @SyncField(units = 1L << 11)
    private int rightPourColor = -1;

    @SyncField(units = 1L << 11)
    private float rightPourLen;

    private final FluidFlushOutputs flush = new FluidFlushOutputs();
    private final FluidTankNTM[] sending;

    public BlockEntityMachineElectrolyser(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ELECTROLYSER.get(), pos, state, SLOT_COUNT);
        tanks[0] = new FluidTankNTM(NTMFluids.WATER, 16_000);
        tanks[1] = new FluidTankNTM(NTMFluids.HYDROGEN, 16_000);
        tanks[2] = new FluidTankNTM(NTMFluids.OXYGEN, 16_000);
        tanks[3] = new FluidTankNTM(NTMFluids.NITRIC_ACID, 16_000);
        sending = new FluidTankNTM[] {tanks[1], tanks[2]};
    }

    private static @Nullable FluidStackNTM secondOutput(ElectrolyserFluidRecipe recipe) {
        return recipe.outputFluid.length > 1 ? recipe.outputFluid[1] : null;
    }

    private static ItemStack byproduct(ElectrolyserFluidRecipe recipe, int i) {
        return recipe.outputItems()[i].unwrap().get(0).value();
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.machineElectrolyser");
    }

    @Override
    public void tickServer() {
        this.leftPourColor = -1;
        this.rightPourColor = -1;

        power += ItemEnergyTransfer.extract(this, 0, MAX_POWER - power, false);
        tanks[0].setType(3, 4, inventory);
        tanks[0].loadTank(5, 6, inventory);
        tanks[1].unloadTank(7, 8, inventory);
        tanks[2].unloadTank(9, 10, inventory);

        upgradeManager.scan(this, 1, 2);
        int speedLevel = upgradeManager.getLevel(UpgradeType.SPEED);
        int powerLevel = upgradeManager.getLevel(UpgradeType.POWER);

        usageOre = usageOreBase - usageOreBase * powerLevel / 4 + usageOreBase * speedLevel;
        usageFluid = usageFluidBase - usageFluidBase * powerLevel / 4 + usageFluidBase * speedLevel;

        for (int i = 0; i < getCycleCount(); i++) {
            if (canProcessFluid()) {
                progressFluid++;
                power -= usageFluid;
                if (progressFluid >= getDurationFluid()) {
                    processFluids();
                    progressFluid = 0;
                    setChanged();
                }
            }

            if (canProcessMetal()) {
                progressOre++;
                power -= usageOre;
                if (progressOre >= getDurationMetal()) {
                    processMetal();
                    progressOre = 0;
                    setChanged();
                }
            }
        }

        Direction facing = getBlockState().getValue(BlockMultiblockCore.FACING);
        if (leftStack != null) pour(facing.getOpposite(), true, speedLevel);
        if (rightStack != null) pour(facing, false, speedLevel);

        flush.provide((ServerLevel) level, this);

        networkPackNT(50);
    }

    private void pour(Direction dir, boolean left, int speedLevel) {
        MaterialStack stack = left ? leftStack : rightStack;
        List<MaterialStack> toCast = new ArrayList<>();
        toCast.add(stack);

        int rate = MaterialShapes.NUGGET.q(3) * Math.max(getCycleCount() * speedLevel, 1);
        double px = worldPosition.getX() + 0.5D + dir.getStepX() * 5.875D;
        double py = worldPosition.getY() + 2D;
        double pz = worldPosition.getZ() + 0.5D + dir.getStepZ() * 5.875D;

        CrucibleUtil.ImpactPos impact = new CrucibleUtil.ImpactPos();
        MaterialStack didPour =
                CrucibleUtil.pourFullStack(level, px, py, pz, 6, true, toCast, rate, impact);

        if (didPour != null) {
            float len =
                    Math.max(1F, worldPosition.getY() - (float) (Math.ceil(impact.y) - 0.875) + 2);
            if (left) {
                leftPourColor = didPour.material.moltenColor;
                leftPourLen = len;
                markSyncEvent();
            } else {
                rightPourColor = didPour.material.moltenColor;
                rightPourLen = len;
                markSyncEvent();
            }
            if (stack.amount <= 0) {
                if (left) leftStack = null;
                else rightStack = null;
            }
        }
    }

    @Override
    public void tickClient() {
        long now = level.getGameTime();
        streams.removeIf(s -> now - s.birth() >= 20);
    }

    public boolean canProcessFluid() {
        if (power < usageFluid) return false;

        ElectrolyserFluidRecipe recipe =
                ElectrolyserFluidRecipes.INSTANCE.getRecipe(tanks[0].getTankType());
        if (recipe == null) return false;
        if (recipe.inputFluid[0].amount() > tanks[0].getFill()) return false;
        FluidStackNTM output1 = recipe.outputFluid[0];
        FluidStackNTM output2 = secondOutput(recipe);

        if (output1.type() == tanks[1].getTankType()
                && output1.amount() + tanks[1].getFill() > tanks[1].getMaxFill()) return false;
        if (output2 != null
                && output2.type() == tanks[2].getTankType()
                && output2.amount() + tanks[2].getFill() > tanks[2].getMaxFill()) return false;

        for (int i = 0; i < recipe.outputItems().length; i++) {
            ItemStack slot = inventory.get(11 + i);
            ItemStack byproduct = byproduct(recipe, i);
            if (slot.isEmpty()) continue;
            if (!ItemStack.isSameItemSameComponents(slot, byproduct)) return false;
            if (slot.getCount() + byproduct.getCount() > slot.getMaxStackSize()) return false;
        }

        return true;
    }

    private void processFluids() {
        ElectrolyserFluidRecipe recipe =
                ElectrolyserFluidRecipes.INSTANCE.getRecipe(tanks[0].getTankType());
        FluidStackNTM output1 = recipe.outputFluid[0];
        FluidStackNTM output2 = secondOutput(recipe);
        tanks[0].setFill(tanks[0].getFill() - (int) recipe.inputFluid[0].amount());

        tanks[1].setTankType(output1.type());
        tanks[1].setFill(tanks[1].getFill() + (int) output1.amount());
        tanks[2].setTankType(output2 != null ? output2.type() : null);
        if (output2 != null) tanks[2].setFill(tanks[2].getFill() + (int) output2.amount());

        for (int i = 0; i < recipe.outputItems().length; i++) {
            ItemStack slot = inventory.get(11 + i);
            ItemStack byproduct = byproduct(recipe, i);
            if (slot.isEmpty()) inventory.set(11 + i, byproduct.copy());
            else slot.grow(byproduct.getCount());
        }
    }

    public boolean canProcessMetal() {
        ItemStack input = inventory.get(14);
        if (input.isEmpty()) return false;
        if (power < usageOre) return false;
        if (tanks[3].getFill() < 100) return false;

        ElectrolyserMetalRecipe recipe = ElectrolyserMetalRecipes.INSTANCE.getRecipe(input);
        if (recipe == null) return false;

        if (leftStack != null) {
            if (recipe.output1.material != leftStack.material) return false;
            if (recipe.output1.amount + leftStack.amount > MAX_MATERIAL) return false;
        }
        if (rightStack != null && recipe.output2 != null) {
            if (recipe.output2.material != rightStack.material) return false;
            if (recipe.output2.amount + rightStack.amount > MAX_MATERIAL) return false;
        }

        for (int i = 0; i < recipe.byproductCount(); i++) {
            ItemStack slot = inventory.get(15 + i);
            ItemStack byproduct = recipe.byproduct(i);
            if (slot.isEmpty()) continue;
            if (!ItemStack.isSameItemSameComponents(slot, byproduct)) return false;
            if (slot.getCount() + byproduct.getCount() > slot.getMaxStackSize()) return false;
        }

        return true;
    }

    private void processMetal() {
        ElectrolyserMetalRecipe recipe =
                ElectrolyserMetalRecipes.INSTANCE.getRecipe(inventory.get(14));
        if (leftStack == null)
            leftStack = new MaterialStack(recipe.output1.material, recipe.output1.amount);
        else leftStack.amount += recipe.output1.amount;

        if (recipe.output2 != null) {
            if (rightStack == null)
                rightStack = new MaterialStack(recipe.output2.material, recipe.output2.amount);
            else rightStack.amount += recipe.output2.amount;
        }

        for (int i = 0; i < recipe.byproductCount(); i++) {
            ItemStack slot = inventory.get(15 + i);
            ItemStack byproduct = recipe.byproduct(i);
            if (slot.isEmpty()) inventory.set(15 + i, byproduct.copy());
            else slot.grow(byproduct.getCount());
        }

        tanks[3].setFill(tanks[3].getFill() - 100);
        removeItem(14, 1);
    }

    public int getDurationMetal() {
        ElectrolyserMetalRecipe result =
                ElectrolyserMetalRecipes.INSTANCE.getRecipe(inventory.get(14));
        int base = result != null ? result.duration : 600;
        int speed =
                upgradeManager.getLevel(UpgradeType.SPEED)
                        - Math.min(upgradeManager.getLevel(UpgradeType.POWER), 1);
        return (int) Math.ceil(base * Math.max(1F - 0.25F * speed, 0.2));
    }

    public int getDurationFluid() {
        ElectrolyserFluidRecipe result =
                ElectrolyserFluidRecipes.INSTANCE.getRecipe(tanks[0].getTankType());
        int base = result != null ? result.duration : 100;
        int speed =
                upgradeManager.getLevel(UpgradeType.SPEED)
                        - Math.min(upgradeManager.getLevel(UpgradeType.POWER), 1);
        return (int) Math.ceil(base * Math.max(1F - 0.25F * speed, 0.2));
    }

    public int getCycleCount() {
        int speed = upgradeManager.getLevel(UpgradeType.OVERDRIVE);
        return Math.min(1 + speed * 2, 7);
    }

    public int getSelectedGUI() {
        return lastSelectedGUI;
    }

    @Override
    public long getPower() {
        return power;
    }

    @Override
    public void setPower(long power) {
        this.power = power;
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
        if (tanks[0].accepts(type)) headroom += tanks[0].getMaxFill() - tanks[0].getFill();
        if (tanks[3].accepts(type)) headroom += tanks[3].getMaxFill() - tanks[3].getFill();
        return headroom;
    }

    @Override
    public long transferFluid(Fluid type, int pressure, long amount) {
        if (pressure != 0) return amount;
        long remaining = amount;
        if (remaining > 0 && tanks[0].accepts(type)) {
            remaining -= tanks[0].fill(type, (int) Math.min(remaining, Integer.MAX_VALUE), true);
        }
        if (remaining > 0 && tanks[3].accepts(type)) {
            remaining -= tanks[3].fill(type, (int) Math.min(remaining, Integer.MAX_VALUE), true);
        }
        return remaining;
    }

    @Override
    public FluidTankNTM[] getSendingTanks() {
        return sending;
    }

    @Override
    public void declareFlush(FlushLanes out) {
        out.add(tanks[1], FlushFaces.own(), 20);
        out.add(tanks[2], FlushFaces.own(), 20);
    }

    @Override
    public long getFluidAvailable(Fluid type, int pressure) {
        long available = 0;
        if (tanks[1].provides(type) && tanks[1].getPressure() == pressure)
            available += tanks[1].getFill();
        if (tanks[2].provides(type) && tanks[2].getPressure() == pressure)
            available += tanks[2].getFill();
        return available;
    }

    @Override
    public void useUpFluid(Fluid type, int pressure, long amount) {
        long remaining = amount;
        if (remaining > 0 && tanks[1].provides(type) && tanks[1].getPressure() == pressure) {
            remaining -= tanks[1].drain((int) Math.min(remaining, Integer.MAX_VALUE), true);
        }
        if (remaining > 0 && tanks[2].provides(type) && tanks[2].getPressure() == pressure) {
            tanks[2].drain((int) Math.min(remaining, Integer.MAX_VALUE), true);
        }
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return ACCESSIBLE_SLOTS;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return switch (slot) {
            case 0 -> IBatteryItem.isBattery(stack);
            case 1, 2 -> ItemMachineUpgrade.isUpgrade(stack);
            case 3 -> stack.getItem() instanceof FluidIdentifierItem;
            case 5, 7, 9 -> true;
            case 14 -> ElectrolyserMetalRecipes.INSTANCE.getRecipe(stack) != null;
            default -> false;
        };
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return slot == 14 && ElectrolyserMetalRecipes.INSTANCE.getRecipe(stack) != null;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot != 14;
    }

    @Override
    public int[] getValidUpgrades() {
        return VALID_UPGRADES;
    }

    @Override
    public boolean hasPermission(Player player) {
        return stillValid(player);
    }

    @Override
    public void receiveControl(CompoundTag data) {}

    @Override
    public void receiveControl(Player player, CompoundTag data) {
        if (data.contains("sgm")) lastSelectedGUI = 1;
        if (data.contains("sgf")) lastSelectedGUI = 0;
        setChanged();

        IGUIProvider.openBlockMenu(player, this, worldPosition, lastSelectedGUI);
    }

    @Override
    public Component getDisplayName() {
        return getName();
    }

    @Override
    public AbstractContainerMenu createMenu(
            int containerId, Inventory playerInventory, Player player) {
        return new MenuMachineElectrolyserFluid(containerId, playerInventory, this);
    }

    @Override
    public AbstractContainerMenu createMenu(
            int containerId, Inventory playerInventory, Player player, int dispatchId) {
        return dispatchId == 0
                ? new MenuMachineElectrolyserFluid(containerId, playerInventory, this)
                : new MenuMachineElectrolyserMetal(containerId, playerInventory, this);
    }

    @Override
    public FluidTankNTM getTankToPaste() {
        return tanks[0];
    }

    @Override
    public int[] getMatsToCopy() {
        int n = (leftStack != null ? 1 : 0) + (rightStack != null ? 1 : 0);
        int[] out = new int[n];
        int i = 0;
        if (leftStack != null) out[i++] = leftStack.material.id;
        if (rightStack != null) out[i] = rightStack.material.id;
        return out;
    }

    @Override
    public CompoundTag getSettings(Level level, BlockPos pos) {
        CompoundTag tag = new CompoundTag();
        String[] ids = getFluidIDToCopy();
        if (ids.length > 0) {
            tag.putInt("fluidCount", ids.length);
            for (int i = 0; i < ids.length; i++) tag.putString("fluidID" + i, ids[i]);
        }
        if (getMatsToCopy().length > 0) tag.putIntArray("matFilter", getMatsToCopy());
        return tag;
    }

    @Override
    public void pasteSettings(
            CompoundTag nbt, int index, Level level, Player player, BlockPos pos) {
        IFluidCopiable.super.pasteSettings(nbt, index, level, player, pos);
    }

    @Override
    public String[] infoForDisplay(Level level, BlockPos pos) {
        List<String> names = new ArrayList<>();
        for (String id : getFluidIDToCopy())
            names.add(NTMFluidProperties.nameKey(Identifier.parse(id)));
        for (int matId : getMatsToCopy()) names.add(Mats.matById.get(matId).getUnlocalizedName());
        return names.toArray(new String[0]);
    }

    private void writeFluidLoop(ByteBuf output) {
        output.writeInt(progressFluid);
        output.writeInt(usageFluid);
        output.writeInt(getDurationFluid());
    }

    private void readFluidLoop(ByteBuf input) {
        progressFluid = input.readInt();
        usageFluid = input.readInt();
        processFluidTime = input.readInt();
    }

    private void writeOreLoop(ByteBuf output) {
        output.writeInt(progressOre);
        output.writeInt(usageOre);
        output.writeInt(getDurationMetal());
    }

    private void readOreLoop(ByteBuf input) {
        progressOre = input.readInt();
        usageOre = input.readInt();
        processOreTime = input.readInt();
    }

    private void writeTank(int index, ByteBuf output) {
        tanks[index].packetSerialize(output);
    }

    private void readTank(int index, ByteBuf input) {
        tanks[index].packetDeserialize(input);
    }

    private void writeLeftStack(ByteBuf output) {
        writeMaterial(output, leftStack);
    }

    private void readLeftStack(ByteBuf input) {
        leftStack = readMaterial(input);
    }

    private void writeRightStack(ByteBuf output) {
        writeMaterial(output, rightStack);
    }

    private void readRightStack(ByteBuf input) {
        rightStack = readMaterial(input);
    }

    private static void writeMaterial(ByteBuf output, @Nullable MaterialStack material) {
        output.writeBoolean(material != null);
        if (material != null) {
            output.writeInt(material.material.id);
            output.writeInt(material.amount);
        }
    }

    private static @Nullable MaterialStack readMaterial(ByteBuf input) {
        return input.readBoolean()
                ? new MaterialStack(Mats.matById.get(input.readInt()), input.readInt())
                : null;
    }

    private void writeLeftPour(ByteBuf output) {
        output.writeInt(leftPourColor);
        output.writeFloat(leftPourLen);
    }

    private void readLeftPour(ByteBuf input) {
        leftPourColor = input.readInt();
        leftPourLen = input.readFloat();
    }

    private void writeRightPour(ByteBuf output) {
        output.writeInt(rightPourColor);
        output.writeFloat(rightPourLen);
    }

    private void readRightPour(ByteBuf input) {
        rightPourColor = input.readInt();
        rightPourLen = input.readFloat();
    }

    @Override
    public void afterSyncUnits(long units) {
        if (level == null || !level.isClientSide()) return;
        long now = level.getGameTime();
        if ((units & 1L << 10) != 0 && leftPourColor != -1) {
            streams.add(new PourStream(leftPourColor, true, leftPourLen, now));
        }
        if ((units & 1L << 11) != 0 && rightPourColor != -1) {
            streams.add(new PourStream(rightPourColor, false, rightPourLen, now));
        }
    }

    @Override
    public void afterInitialSyncUnits() {}

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);

        input.getLong("power").ifPresent(v -> power = v);
        input.getInt("progressFluid").ifPresent(v -> progressFluid = v);
        input.getInt("progressOre").ifPresent(v -> progressOre = v);
        input.getInt("processFluidTime").ifPresent(v -> processFluidTime = v);
        input.getInt("processOreTime").ifPresent(v -> processOreTime = v);
        input.getInt("lastSelectedGUI").ifPresent(v -> lastSelectedGUI = v);
        input.getInt("leftType")
                .ifPresent(
                        id ->
                                leftStack =
                                        new MaterialStack(
                                                Mats.matById.get(id),
                                                input.getIntOr("leftAmount", 0)));
        input.getInt("rightType")
                .ifPresent(
                        id ->
                                rightStack =
                                        new MaterialStack(
                                                Mats.matById.get(id),
                                                input.getIntOr("rightAmount", 0)));
        for (int i = 0; i < 4; i++) input.child("t" + i).ifPresent(tanks[i]::deserialize);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("power", power);
        output.putInt("progressFluid", progressFluid);
        output.putInt("progressOre", progressOre);
        output.putInt("processFluidTime", getDurationFluid());
        output.putInt("processOreTime", getDurationMetal());
        output.putInt("lastSelectedGUI", lastSelectedGUI);
        if (leftStack != null) {
            output.putInt("leftType", leftStack.material.id);
            output.putInt("leftAmount", leftStack.amount);
        }
        if (rightStack != null) {
            output.putInt("rightType", rightStack.material.id);
            output.putInt("rightAmount", rightStack.amount);
        }
        for (int i = 0; i < 4; i++) tanks[i].serialize(output.child("t" + i));
    }

    public record PourStream(int color, boolean left, float len, long birth) {}

    @Override
    public long syncUnitMask() {
        return 0xfffL;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeLong(this.power);
            case 1 -> writeFluidLoop(output);
            case 2 -> writeOreLoop(output);
            case 3 -> writeTank(0, output);
            case 4 -> writeTank(1, output);
            case 5 -> writeTank(2, output);
            case 6 -> writeTank(3, output);
            case 7 -> writeLeftStack(output);
            case 8 -> writeRightStack(output);
            case 9 -> output.writeInt(this.lastSelectedGUI);
            case 10 -> writeLeftPour(output);
            case 11 -> writeRightPour(output);
            default -> throw new IllegalArgumentException();
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> this.power = input.readLong();
            case 1 -> readFluidLoop(input);
            case 2 -> readOreLoop(input);
            case 3 -> readTank(0, input);
            case 4 -> readTank(1, input);
            case 5 -> readTank(2, input);
            case 6 -> readTank(3, input);
            case 7 -> readLeftStack(input);
            case 8 -> readRightStack(input);
            case 9 -> this.lastSelectedGUI = input.readInt();
            case 10 -> readLeftPour(input);
            case 11 -> readRightPour(input);
            default -> throw new IllegalArgumentException();
        }
    }

    @Override
    public long syncEventUnits() {
        return 0xc00L;
    }

    @Override
    public void bindSyncValue(Object value, int flags, long units) {
        if (value == this.tanks) {
            SyncBindings.bindIndexed(this, value, flags, 3, 4, 0L);
            return;
        }
        super.bindSyncValue(value, flags, units);
    }

    @Override
    public void syncArrayChanged(Object value, int index, int flags, long units) {
        if (value == this.tanks) {
            long selected =
                    index < 0
                            ? 0x78L
                            : 1L << (3 + index / SyncBindings.groupWidth(this.tanks.length, 4));
            if (index >= 0 && syncBound() && value instanceof Object[] entries) {
                SyncBindings.bindUnits(this, entries[index], flags, selected);
            }
            syncUnitsChanged(flags, selected);
            return;
        }
        super.syncArrayChanged(value, index, flags, units);
    }
}
