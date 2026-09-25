// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine;

import com.hbm.api.control.IControlReceiver;
import com.hbm.api.energymk2.IEnergyHandlerMK2;
import com.hbm.api.energymk2.ItemEnergyTransfer;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.handler.pollution.PollutionHandler;
import com.hbm.handler.pollution.PollutionType;
import com.hbm.inventory.container.MenuMachineArcFurnace;
import com.hbm.inventory.material.MaterialShapes;
import com.hbm.inventory.material.Mats.MaterialStack;
import com.hbm.inventory.material.Mats;
import com.hbm.inventory.recipes.ArcFurnaceRecipe;
import com.hbm.inventory.recipes.ArcFurnaceRecipes;
import com.hbm.items.ModItems;
import com.hbm.items.machine.ItemArcElectrode;
import com.hbm.items.machine.ItemArcElectrodeBurnt;
import com.hbm.items.machine.ItemScraps;
import com.hbm.items.machine.upgrade.UpgradeManager;
import com.hbm.items.machine.upgrade.UpgradeType;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncList;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.sound.AudioWrapper;
import com.hbm.tileentity.Audible;
import com.hbm.tileentity.BlockEntityMachineBase;
import com.hbm.tileentity.IUpgradeInfoProvider;
import com.hbm.util.CrucibleUtil;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public class BlockEntityMachineArcFurnace extends BlockEntityMachineBase
        implements Audible,
                IEnergyHandlerMK2,
                IControlReceiver,
                MenuProvider,
                IUpgradeInfoProvider,
                SyncUnitSchema {

    public static final int BASE_DURATION = 400;

    public static final int SLOT_COUNT = 30;
    public static final int SLOT_GRID_START = 5;
    public static final int SLOT_GRID_END = 24;
    public static final long MAX_POWER = 2_500_000L;
    public static final int MAX_LIQUID = MaterialShapes.BLOCK.q(128);
    public static final byte ELECTRODE_NONE = 0;
    public static final byte ELECTRODE_FRESH = 1;
    public static final byte ELECTRODE_USED = 2;
    public static final byte ELECTRODE_DEPLETED = 3;
    private static final int SLOT_ELECTRODE_2 = 2;
    private static final int SLOT_BATTERY = 3;
    private static final int SLOT_UPGRADE = 4;
    private static final int SLOT_QUEUE_START = 25;
    private static final int SLOT_QUEUE_END = 29;

    private static final int[] OUTPUT_SLOTS = gridSlots();
    private static final int[] VALID_UPGRADES =
            IUpgradeInfoProvider.upgradeCaps(UpgradeType.SPEED, 3);
    private static final int[] ACCESSIBLE_SLOTS = buildAccessibleSlots();

    public static Consumer<BlockEntityMachineArcFurnace> CLIENT_SOUND = be -> {};

    @SyncField(units = 1L << 7)
    public final byte[] electrodes = new byte[3];

    @SyncField(units = 1L << 8)
    public final List<MaterialStack> liquids = new SyncList<>();

    public final List<BlockEntityCrucible.PourStream> streams = new ArrayList<>();
    private final UpgradeManager upgradeManager = new UpgradeManager(this);

    @SyncField(units = 1L << 0)
    public long power;

    @SyncField(units = 1L << 4)
    public boolean liquidMode;

    @SyncField(units = 1L << 1)
    public float progress;

    @SyncField(units = 1L << 3)
    public boolean isProgressing;

    @SyncField(units = 1L << 5)
    public boolean hasMaterial;

    public int delay;

    @SyncField(units = 1L << 2)
    public float lid;

    @SyncField(units = 1L << 2)
    public float prevLid;

    @SyncField(units = 1L << 6)
    public int upgrade;

    public AudioWrapper audioLid;
    public AudioWrapper audioProgress;

    @SyncField(units = 1L << 9)
    private int pourColor = -1;

    @SyncField(units = 1L << 9)
    private float pourLen;

    public BlockEntityMachineArcFurnace(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ARC_FURNACE_LARGE.get(), pos, state, SLOT_COUNT);
    }

    private static int[] gridSlots() {
        int[] slots = new int[SLOT_GRID_END - SLOT_GRID_START + 1];
        for (int i = 0; i < slots.length; i++) slots[i] = SLOT_GRID_START + i;
        return slots;
    }

    public static int getStackAmount(List<MaterialStack> stacks) {
        int amount = 0;
        for (MaterialStack mat : stacks) amount += mat.amount;
        return amount;
    }

    public static int getStackAmount(MaterialStack[] stacks) {
        int amount = 0;
        for (MaterialStack mat : stacks) amount += mat.amount;
        return amount;
    }

    private static int[] buildAccessibleSlots() {
        int[] slots =
                new int
                        [3
                                + (SLOT_GRID_END - SLOT_GRID_START + 1)
                                + (SLOT_QUEUE_END - SLOT_QUEUE_START + 1)];
        int idx = 0;
        for (int i = 0; i <= SLOT_ELECTRODE_2; i++) slots[idx++] = i;
        for (int i = SLOT_GRID_START; i <= SLOT_QUEUE_END; i++) slots[idx++] = i;
        return slots;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.machineArcFurnaceLarge");
    }

    public Direction coreFacing() {
        return BlockMultiblockCore.coreFacing(getBlockState());
    }

    public int getMaxInputSize() {
        return upgrade == 0 ? 1 : upgrade == 1 ? 4 : upgrade == 2 ? 8 : 16;
    }

    @Override
    public void tickServer() {
        this.prevLid = this.lid;
        this.pourColor = -1;

        upgradeManager.scan(this, SLOT_UPGRADE, SLOT_UPGRADE);
        this.upgrade = upgradeManager.getLevel(UpgradeType.SPEED);

        this.power += ItemEnergyTransfer.extract(this, SLOT_BATTERY, MAX_POWER - power, false);
        this.isProgressing = false;

        if (lid > 0) loadIngredients();

        if (power > 0) {
            boolean ingredients = hasIngredients();
            boolean electrodesReady = hasElectrodes();
            long consumption = (long) (1_000 * Math.pow(5, upgrade));

            if (ingredients && electrodesReady && delay <= 0 && liquids.isEmpty()) {
                if (lid > 0) {
                    lid -= 1F / (60F / (upgrade * 0.5F + 1));
                    if (lid < 0) lid = 0;
                    this.progress = 0;
                } else if (power >= consumption) {
                    int duration = BASE_DURATION / (upgrade * 2 + 1);
                    this.progress += 1F / duration;
                    this.isProgressing = true;
                    this.power -= consumption;
                    if (this.progress >= 1F) {
                        process();
                        this.progress = 0;
                        setChanged();
                        this.delay = (int) (120 / (upgrade * 0.5F + 1));
                        PollutionHandler.incrementPollution(
                                level, worldPosition, PollutionType.SOOT, 10F);
                    }
                }
            } else {
                if (this.delay > 0) delay--;
                this.progress = 0;
                if (lid < 1) {
                    lid += 1F / (60F / (upgrade * 0.5F + 1));
                    if (lid > 1) lid = 1;
                }
            }

            hasMaterial = ingredients;
        }

        decideElectrodeState();
        if (!hasMaterial) hasMaterial = hasIngredients();

        if (!liquids.isEmpty() && lid > 0F) {
            Direction dir = coreFacing();
            int x = worldPosition.getX(), y = worldPosition.getY(), z = worldPosition.getZ();
            CrucibleUtil.ImpactPos impact = new CrucibleUtil.ImpactPos();
            MaterialStack didPour =
                    CrucibleUtil.pourFullStack(
                            level,
                            x + 0.5D + dir.getStepX() * 2.875D,
                            y + 1.25D,
                            z + 0.5D + dir.getStepZ() * 2.875D,
                            6,
                            true,
                            liquids,
                            MaterialShapes.INGOT.q(1),
                            impact);

            if (didPour != null) {
                this.pourColor = didPour.material.moltenColor;
                this.pourLen = Math.max(1F, y + 1 - (float) (Math.ceil(impact.y) - 0.875));
                markSyncEvent();
            }
        }

        liquids.removeIf(o -> o.amount <= 0);

        networkPackNT(150);
    }

    @Override
    public void tickClient() {
        long now = level.getGameTime();
        streams.removeIf(s -> now - s.birth() >= 20);
        CLIENT_SOUND.accept(this);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (audioLid != null) {
            audioLid.stopSound();
            audioLid = null;
        }
        if (audioProgress != null) {
            audioProgress.stopSound();
            audioProgress = null;
        }
    }

    public void loadIngredients() {
        boolean changed = false;

        for (int q = SLOT_QUEUE_START; q <= SLOT_QUEUE_END; q++) {
            ItemStack queued = getItem(q);
            if (queued.isEmpty()) continue;
            ArcFurnaceRecipe recipe = ArcFurnaceRecipes.INSTANCE.getOutput(queued, liquidMode);
            if (recipe == null) continue;

            ItemStack solid = recipe.solidOutput();
            int max = getMaxInputSize();
            int recipeMax =
                    liquidMode || solid == null ? max : queued.getMaxStackSize() / solid.getCount();
            max = Math.min(max, recipeMax);

            for (int i = SLOT_GRID_START; i <= SLOT_GRID_END; i++) {
                queued = getItem(q);
                if (queued.isEmpty()) break;
                ItemStack grid = getItem(i);
                if (grid.isEmpty() || !ItemStack.isSameItemSameComponents(queued, grid)) continue;
                int toMove =
                        Math.min(
                                Math.min(
                                        grid.getMaxStackSize() - grid.getCount(),
                                        queued.getCount()),
                                max - grid.getCount());
                if (toMove <= 0) continue;
                grid.grow(toMove);
                queued.shrink(toMove);
                changed = true;
            }

            queued = getItem(q);
            if (!queued.isEmpty())
                for (int i = SLOT_GRID_START; i <= SLOT_GRID_END; i++) {
                    queued = getItem(q);
                    if (queued.isEmpty()) break;
                    if (!getItem(i).isEmpty()) continue;
                    int toMove = Math.min(max, queued.getCount());
                    setItem(i, queued.copyWithCount(toMove));
                    queued.shrink(toMove);
                    changed = true;
                }
        }

        if (changed) setChanged();
    }

    public void decideElectrodeState() {
        for (int i = 0; i <= SLOT_ELECTRODE_2; i++) {
            ItemStack stack = getItem(i);
            if (!stack.isEmpty()) {
                if (stack.getItem() instanceof ItemArcElectrodeBurnt) {
                    electrodes[i] = ELECTRODE_DEPLETED;
                    continue;
                }
                if (stack.getItem() instanceof ItemArcElectrode) {
                    electrodes[i] =
                            (isProgressing || ItemArcElectrode.getDurability(stack) > 0)
                                    ? ELECTRODE_USED
                                    : ELECTRODE_FRESH;
                    continue;
                }
            }
            electrodes[i] = ELECTRODE_NONE;
        }
    }

    public void process() {
        for (int i = SLOT_GRID_START; i <= SLOT_GRID_END; i++) {
            ItemStack stack = getItem(i);
            if (stack.isEmpty()) continue;
            ArcFurnaceRecipe recipe = ArcFurnaceRecipes.INSTANCE.getOutput(stack, liquidMode);
            if (recipe == null) continue;

            ItemStack solid = recipe.solidOutput();
            if (!liquidMode && solid != null) {
                int amount = stack.getCount();
                ItemStack out = solid.copy();
                out.setCount(out.getCount() * amount);
                setItem(i, out);
            }

            MaterialStack[] fluid = recipe.fluidOutput();
            if (liquidMode && fluid != null) {
                while (!getItem(i).isEmpty()) {
                    int liquid = getStackAmount(liquids);
                    int toAdd = getStackAmount(fluid);
                    if (liquid + toAdd <= MAX_LIQUID) {
                        removeItem(i, 1);
                        for (MaterialStack stackOut : fluid) addToStack(stackOut);
                    } else {
                        break;
                    }
                }
            }
        }

        for (int i = 0; i <= SLOT_ELECTRODE_2; i++) {
            ItemStack electrode = getItem(i);
            if (electrode.isEmpty() || !(electrode.getItem() instanceof ItemArcElectrode arc))
                continue;
            if (ItemArcElectrode.damage(electrode)) {
                setItem(i, ModItems.ARC_ELECTRODE_BURNT.stack(arc.type));
            }
        }
    }

    public boolean hasIngredients() {
        for (int i = SLOT_GRID_START; i <= SLOT_GRID_END; i++) {
            ItemStack stack = getItem(i);
            if (stack.isEmpty()) continue;
            ArcFurnaceRecipe recipe = ArcFurnaceRecipes.INSTANCE.getOutput(stack, liquidMode);
            if (recipe == null) continue;
            if (liquidMode && recipe.fluidOutput() != null) return true;
            if (!liquidMode && recipe.solidOutput() != null) return true;
        }
        return false;
    }

    public boolean hasElectrodes() {
        for (int i = 0; i <= SLOT_ELECTRODE_2; i++) {
            if (!(getItem(i).getItem() instanceof ItemArcElectrode)) return false;
        }
        return true;
    }

    public void addToStack(MaterialStack matStack) {
        for (MaterialStack mat : liquids) {
            if (mat.material == matStack.material) {
                mat.amount += matStack.amount;
                return;
            }
        }
        liquids.add(matStack.copy());
    }

    public void spillLiquids(@Nullable Player intoInventory) {
        for (MaterialStack stack : liquids) {
            ItemStack scrap = ItemScraps.create(stack);
            if (intoInventory != null) {
                intoInventory.getInventory().placeItemBackInInventory(scrap);
            } else {
                level.addFreshEntity(
                        new ItemEntity(
                                level,
                                worldPosition.getX() + 0.5,
                                worldPosition.getY() + 0.5,
                                worldPosition.getZ() + 0.5,
                                scrap));
            }
        }
        liquids.clear();
        setChanged();
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return ACCESSIBLE_SLOTS;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot <= SLOT_ELECTRODE_2) return stack.getItem() instanceof ItemArcElectrode;
        if (slot > SLOT_UPGRADE) {
            ArcFurnaceRecipe recipe = ArcFurnaceRecipes.INSTANCE.getOutput(stack, liquidMode);
            if (recipe == null) return false;
            return liquidMode ? recipe.fluidOutput() != null : recipe.solidOutput() != null;
        }
        return false;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        if (slot <= SLOT_ELECTRODE_2) return stack.getItem() instanceof ItemArcElectrode;
        if (slot >= SLOT_QUEUE_START)
            return ArcFurnaceRecipes.INSTANCE.getOutput(stack, liquidMode) != null;
        return false;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        if (slot <= SLOT_ELECTRODE_2)
            return lid >= 1 && !(stack.getItem() instanceof ItemArcElectrode);
        if (slot >= SLOT_GRID_START && slot <= SLOT_GRID_END) {
            return lid > 0 && ArcFurnaceRecipes.INSTANCE.getOutput(stack, liquidMode) == null;
        }
        if (slot >= SLOT_QUEUE_START)
            return ArcFurnaceRecipes.INSTANCE.getOutput(stack, liquidMode) == null;
        return false;
    }

    private void writeUpgrade(ByteBuf output) {
        output.writeByte(upgrade);
    }

    private void readUpgrade(ByteBuf input) {
        upgrade = input.readByte();
    }

    private void writeElectrodes(ByteBuf output) {
        for (byte electrode : electrodes) output.writeByte(electrode);
    }

    private void readElectrodes(ByteBuf input) {
        for (int i = 0; i < electrodes.length; i++) electrodes[i] = input.readByte();
    }

    private void writeLiquids(ByteBuf output) {
        output.writeShort(liquids.size());
        for (MaterialStack mat : liquids) {
            output.writeInt(mat.material.id);
            output.writeInt(mat.amount);
        }
    }

    private void readLiquids(ByteBuf input) {
        int count = input.readShort();
        if (count < 0) throw new DecoderException("Invalid arc furnace liquid count");
        liquids.clear();
        for (int i = 0; i < count; i++) {
            liquids.add(new MaterialStack(Mats.matById.get(input.readInt()), input.readInt()));
        }
    }

    private void writePour(ByteBuf output) {
        output.writeInt(pourColor);
        output.writeFloat(pourLen);
    }

    private void readPour(ByteBuf input) {
        pourColor = input.readInt();
        pourLen = input.readFloat();
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.power = input.getLongOr("power", 0L);
        this.liquidMode = input.getBooleanOr("liquidMode", false);
        this.progress = input.getFloatOr("progress", 0F);
        this.lid = input.getFloatOr("lid", 0F);
        this.prevLid = this.lid;
        this.delay = input.getIntOr("delay", 0);

        int[] mats = input.getIntArray("liquidMats").orElse(new int[0]);
        int[] amounts = input.getIntArray("liquidAmounts").orElse(new int[0]);
        liquids.clear();
        for (int i = 0; i < mats.length && i < amounts.length; i++) {
            liquids.add(new MaterialStack(Mats.matById.get(mats[i]), amounts[i]));
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("power", power);
        output.putBoolean("liquidMode", liquidMode);
        output.putFloat("progress", progress);
        output.putFloat("lid", lid);
        output.putInt("delay", delay);

        int[] mats = new int[liquids.size()];
        int[] amounts = new int[liquids.size()];
        for (int i = 0; i < liquids.size(); i++) {
            MaterialStack stack = liquids.get(i);
            mats[i] = stack.material.id;
            amounts[i] = stack.amount;
        }
        output.putIntArray("liquidMats", mats);
        output.putIntArray("liquidAmounts", amounts);
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
    public boolean hasPermission(Player player) {
        return stillValid(player);
    }

    @Override
    public void receiveControl(CompoundTag data) {
        if (data.contains("liquid")) {
            this.liquidMode = !this.liquidMode;
            setChanged();
        }
    }

    @Override
    public int[] getValidUpgrades() {
        return VALID_UPGRADES;
    }

    @Override
    public Component getDisplayName() {
        return getName();
    }

    @Override
    public AbstractContainerMenu createMenu(
            int containerId, Inventory playerInventory, Player player) {
        return new MenuMachineArcFurnace(containerId, playerInventory, this);
    }

    @Override
    protected boolean syncMuffled() {
        return true;
    }

    @Override
    public long syncUnitMask() {
        return super.syncUnitMask() | 0x3ffL;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeLong(this.power);
            case 1 -> output.writeFloat(this.progress);
            case 2 -> {
                output.writeFloat(this.lid);
                output.writeFloat(this.prevLid);
            }
            case 3 -> output.writeBoolean(this.isProgressing);
            case 4 -> output.writeBoolean(this.liquidMode);
            case 5 -> output.writeBoolean(this.hasMaterial);
            case 6 -> writeUpgrade(output);
            case 7 -> writeElectrodes(output);
            case 8 -> writeLiquids(output);
            case 9 -> writePour(output);
            default -> super.writeSyncUnit(unit, output);
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> this.power = input.readLong();
            case 1 -> this.progress = input.readFloat();
            case 2 -> {
                this.lid = input.readFloat();
                this.prevLid = input.readFloat();
            }
            case 3 -> this.isProgressing = input.readBoolean();
            case 4 -> this.liquidMode = input.readBoolean();
            case 5 -> this.hasMaterial = input.readBoolean();
            case 6 -> readUpgrade(input);
            case 7 -> readElectrodes(input);
            case 8 -> readLiquids(input);
            case 9 -> readPour(input);
            default -> super.readSyncUnit(unit, input);
        }
    }

    @Override
    public long syncEventUnits() {
        return 1L << 9;
    }

    @Override
    public void afterSyncUnits(long units) {
        if ((units & 1L << 9) != 0 && level != null && level.isClientSide() && pourColor != -1) {
            streams.add(
                    new BlockEntityCrucible.PourStream(
                            pourColor, false, pourLen, level.getGameTime()));
        }
    }

    @Override
    public void afterInitialSyncUnits() {}
}
