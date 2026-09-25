// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine;

import com.hbm.api.control.IControlReceiver;
import com.hbm.api.energymk2.IBatteryItem;
import com.hbm.api.energymk2.IEnergyHandlerMK2;
import com.hbm.api.energymk2.ItemEnergyTransfer;
import com.hbm.api.fluidmk2.FluidFlushOutputs;
import com.hbm.api.fluidmk2.FluidTankEndpoint;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.gas.BlockGasBase;
import com.hbm.inventory.container.ContainerSync;
import com.hbm.inventory.container.MenuMachineMiningLaser;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.recipes.CentrifugeRecipes;
import com.hbm.inventory.recipes.CrystallizerRecipes;
import com.hbm.inventory.recipes.ShredderRecipes;
import com.hbm.items.ModItems;
import com.hbm.items.machine.upgrade.ItemMachineUpgrade;
import com.hbm.items.machine.upgrade.UpgradeManager;
import com.hbm.items.machine.upgrade.UpgradeType;
import com.hbm.lib.Library;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.sound.ModSounds;
import com.hbm.tileentity.BlockEntityMachineBase;
import com.hbm.tileentity.IUpgradeInfoProvider;
import com.hbm.util.ForeignItems;
import com.hbm.util.InventoryUtil;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

public class BlockEntityMachineMiningLaser extends BlockEntityMachineBase
        implements IEnergyHandlerMK2,
                FluidTankEndpoint,
                MenuProvider,
                IUpgradeInfoProvider,
                IControlReceiver,
                SyncUnitSchema {

    public static final int SLOT_BATTERY = 0;
    public static final int SLOT_UPGRADE_START = 1;
    public static final int SLOT_UPGRADE_END = 8;
    public static final int SLOT_OUTPUT_START = 9;
    public static final int SLOT_OUTPUT_END = 29;
    public static final int SLOT_COUNT = 30;

    public static final long MAX_POWER = 100_000_000L;
    public static final int CONSUMPTION = 10_000;
    public static final int TANK_CAPACITY = 64_000;
    public static final int OIL_PER_ORE = 500;

    private static final int[] ACCESSIBLE_SLOTS = {
        9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29
    };
    private static final int[] VALID_UPGRADES =
            IUpgradeInfoProvider.upgradeCaps(
                    UpgradeType.SPEED,
                    12,
                    UpgradeType.POWER,
                    12,
                    UpgradeType.EFFECT,
                    12,
                    UpgradeType.FORTUNE,
                    3,
                    UpgradeType.OVERDRIVE,
                    9);

    private static final TagKey<Item> WORTHLESS =
            TagKey.create(Registries.ITEM, Library.id("mining_laser_worthless"));

    @SyncField(units = 1L << 10)
    public final FluidTankNTM tank = new FluidTankNTM(NTMFluids.OIL, TANK_CAPACITY);

    private final FluidTankNTM[] sending;
    private final UpgradeManager upgradeManager = new UpgradeManager(this);
    private final RecipeManager.CachedCheck<SingleRecipeInput, SmeltingRecipe> smeltCheck =
            RecipeManager.createCheck(RecipeType.SMELTING);

    @SyncField(units = 1L << 0)
    @ContainerSync
    public long power;

    @SyncField(units = 1L << 8)
    public boolean isOn;

    @SyncField(units = 1L << 7)
    public boolean beam;

    @SyncField(units = 1L << 4)
    public int targetX;

    @SyncField(units = 1L << 5)
    public int targetY;

    @SyncField(units = 1L << 6)
    public int targetZ;

    @SyncField(units = 1L << 1)
    public int lastTargetX;

    @SyncField(units = 1L << 2)
    public int lastTargetY;

    @SyncField(units = 1L << 3)
    public int lastTargetZ;

    @SyncField(units = 1L << 9)
    public double clientBreakProgress;

    private int unloadFaces;
    private boolean unloadKnown;

    @SyncField(units = 1L << 11)
    private boolean redstonePowered;

    private boolean targetArmed;
    private double breakProgress;

    private final FluidFlushOutputs flush = new FluidFlushOutputs();

    public BlockEntityMachineMiningLaser(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MINING_LASER.get(), pos, state, SLOT_COUNT);
        sending = new FluidTankNTM[] {tank};
    }

    public void refreshUnloadTargets() {
        int faces = 0;
        boolean complete = true;
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            BlockPos pos = worldPosition.relative(dir, 2);

            if (!level.isLoaded(pos)) {
                complete = false;
                continue;
            }
            if (InventoryUtil.inventoryAt(level, pos, dir.getOpposite())) {
                faces |= 1 << dir.get3DDataValue();
            }
        }
        unloadFaces = faces;
        unloadKnown = complete;
    }

    public void updateRedstonePower() {
        boolean powered = false;
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            if (level.hasNeighborSignal(worldPosition.relative(dir))) {
                powered = true;
                break;
            }
        }
        if (powered != redstonePowered) {
            redstonePowered = powered;
            setChanged();
        }
    }

    @Override
    public void tickServer() {

        flush.provide((ServerLevel) level, this);

        power += ItemEnergyTransfer.extract(this, SLOT_BATTERY, MAX_POWER - power, false);

        if (lastTargetX != targetX || lastTargetY != targetY || lastTargetZ != targetZ)
            breakProgress = 0;
        lastTargetX = targetX;
        lastTargetY = targetY;
        lastTargetZ = targetZ;

        if (this.isOn && !this.redstonePowered) {
            upgradeManager.scan(this, SLOT_UPGRADE_START, SLOT_UPGRADE_END);
            int cycles = 1 + upgradeManager.getLevel(UpgradeType.OVERDRIVE);
            int speed = 1 + upgradeManager.getLevel(UpgradeType.SPEED);
            int range = 1 + upgradeManager.getLevel(UpgradeType.EFFECT) * 2;
            int fortune = upgradeManager.getLevel(UpgradeType.FORTUNE);
            int draw =
                    CONSUMPTION
                            - (CONSUMPTION * upgradeManager.getLevel(UpgradeType.POWER) / 16)
                            + (CONSUMPTION * upgradeManager.getLevel(UpgradeType.SPEED) / 16);

            for (int i = 0; i < cycles; i++) {
                if (power < draw) {
                    beam = false;
                    break;
                }
                power -= draw;

                if (!targetArmed || targetY <= level.getMinY()) {
                    targetY = worldPosition.getY() - 2;
                    targetArmed = true;
                }

                scan(range);

                BlockPos target = new BlockPos(targetX, targetY, targetZ);
                BlockState state = level.getBlockState(target);

                if (!state.getFluidState().isEmpty()) {
                    level.removeBlock(target, false);
                    buildDam(target);
                    continue;
                }

                if (beam && canBreak(state, target)) {
                    breakProgress += getBreakSpeed(state, target, speed);
                    clientBreakProgress = Math.min(breakProgress, 1);

                    if (breakProgress < 1) {
                        level.destroyBlockProgress(
                                -1, target, (int) Math.floor(breakProgress * 10));
                    } else {
                        breakBlock(target, fortune);
                        buildDam(target);
                    }
                }
            }
        } else {
            targetY = worldPosition.getY() - 2;
            beam = false;
        }

        if (!unloadKnown) refreshUnloadTargets();
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            if ((unloadFaces & (1 << dir.get3DDataValue())) == 0) continue;
            BlockPos pos = worldPosition.relative(dir, 2);
            if (!level.isLoaded(pos)) continue;
            tryFillNeighbour(pos, dir.getOpposite());
        }

        networkPackNT(250);
    }

    private void buildDam(BlockPos target) {
        for (Direction dir : Direction.VALUES) {
            BlockPos side = target.relative(dir);
            if (!level.getBlockState(side).getFluidState().isEmpty()) {
                level.setBlockAndUpdate(side, ModBlocks.BARRICADE.get().defaultBlockState());
            }
        }
    }

    private void tryFillNeighbour(BlockPos pos, Direction face) {
        for (int i = SLOT_OUTPUT_START; i <= SLOT_OUTPUT_END; i++) {
            ItemStack stack = inventory.get(i);
            if (stack.isEmpty()) continue;
            int prev = stack.getCount();
            ItemStack left = ForeignItems.insert(level, pos, face, stack);
            inventory.set(i, left);
            if (left.isEmpty() || left.getCount() < prev) return;
        }
    }

    private void breakBlock(BlockPos target, int fortune) {
        BlockState state = level.getBlockState(target);
        ItemStack stack = new ItemStack(state.getBlock().asItem());
        boolean normal = true;

        if (!stack.isEmpty()) {
            if (hasUpgrade(ModItems.UPGRADE_CRYSTALLIZER.get())) {
                var recipe = CrystallizerRecipes.INSTANCE.getOutput(stack, NTMFluids.PEROXIDE);
                if (recipe == null)
                    recipe = CrystallizerRecipes.INSTANCE.getOutput(stack, NTMFluids.SULFURIC_ACID);
                ItemStack result = recipe == null ? null : recipe.getIcon();
                if (result != null && !result.isEmpty()) {
                    drop(target, result.copy());
                    normal = false;
                }
            } else if (hasUpgrade(ModItems.UPGRADE_CENTRIFUGE.get())) {
                ItemStack[] result = CentrifugeRecipes.INSTANCE.getOutputs(stack, level);
                if (result != null) {
                    for (ItemStack out : result) {
                        if (out != null && !out.isEmpty()) {
                            drop(target, out.copy());
                            normal = false;
                        }
                    }
                }
            } else if (hasUpgrade(ModItems.UPGRADE_SHREDDER.get())) {
                ItemStack result = ShredderRecipes.getShredderResult(stack);
                if (result != null
                        && !result.isEmpty()
                        && result.getItem() != ModItems.SCRAP.get()) {
                    drop(target, result.copy());
                    normal = false;
                }
            } else if (hasUpgrade(ModItems.UPGRADE_SMELTER.get())) {
                ItemStack result = smeltingResult(stack);
                if (result != null && !result.isEmpty()) {
                    drop(target, result.copy());
                    normal = false;
                }
            }
        }

        if (normal) {
            for (ItemStack dropped :
                    Block.getDrops(
                            state,
                            (ServerLevel) level,
                            target,
                            level.getBlockEntity(target),
                            null,
                            fortuneTool(fortune))) {
                drop(target, dropped);
            }
        }
        level.removeBlock(target, false);

        suckDrops(target);
        if (doesScream()) {
            level.playSound(
                    null,
                    target.getX() + 0.5,
                    target.getY() + 0.5,
                    target.getZ() + 0.5,
                    ModSounds.BLOCK_SCREM.get(),
                    SoundSource.BLOCKS,
                    2000F,
                    1F);
        }
        breakProgress = 0;
    }

    private ItemStack fortuneTool(int fortune) {
        if (fortune <= 0) return ItemStack.EMPTY;
        ItemStack tool = new ItemStack(Items.NETHERITE_PICKAXE);
        var lookup = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        ItemEnchantments.Mutable enchantments =
                new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        enchantments.set(lookup.getOrThrow(Enchantments.FORTUNE), fortune);
        tool.set(DataComponents.ENCHANTMENTS, enchantments.toImmutable());
        return tool;
    }

    private @Nullable ItemStack smeltingResult(ItemStack input) {
        if (input.isEmpty() || !(level instanceof ServerLevel server)) return null;
        SingleRecipeInput in = new SingleRecipeInput(input);
        RecipeHolder<SmeltingRecipe> recipe = smeltCheck.getRecipeFor(in, server).orElse(null);
        return recipe == null ? null : recipe.value().assemble(in);
    }

    private void drop(BlockPos target, ItemStack stack) {
        level.addFreshEntity(
                new ItemEntity(
                        level,
                        target.getX() + 0.5,
                        target.getY() + 0.5,
                        target.getZ() + 0.5,
                        stack));
    }

    private void suckDrops(BlockPos target) {
        boolean nullifier = hasUpgrade(ModItems.UPGRADE_NULLIFIER.get());
        AABB box =
                new AABB(
                        target.getX() + 0.5 - 3,
                        target.getY() + 0.5 - 1,
                        target.getZ() + 0.5 - 3,
                        target.getX() + 0.5 + 3,
                        target.getY() + 0.5 + 1,
                        target.getZ() + 0.5 + 3);

        for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, box)) {
            if (item.isRemoved()) continue;
            ItemStack stack = item.getItem();

            if (nullifier && stack.is(WORTHLESS)) {
                item.discard();
                continue;
            }

            if (stack.getItem() == ModBlocks.ORE_OIL.get().asItem()) {
                tank.setTankType(NTMFluids.OIL);
                tank.setFill(Math.min(tank.getFill() + OIL_PER_ORE, tank.getMaxFill()));
                item.discard();
                continue;
            }

            ItemStack left =
                    InventoryUtil.tryAddItemToInventory(
                            inventory, SLOT_OUTPUT_START, SLOT_OUTPUT_END, stack.copy());
            if (left.isEmpty()) item.discard();
            else item.setItem(left.copy());
        }

        AABB burn =
                new AABB(
                        target.getX() - 0.5,
                        target.getY() - 0.5,
                        target.getZ() - 0.5,
                        target.getX() + 1.5,
                        target.getY() + 1.5,
                        target.getZ() + 1.5);
        for (LivingEntity mob : level.getEntitiesOfClass(LivingEntity.class, burn))
            mob.igniteForSeconds(5);
    }

    public double getBreakSpeed(BlockState state, BlockPos pos, int speed) {
        float hardness = state.getDestroySpeed(level, pos) * 15 / speed;
        if (hardness == 0) return 1;
        return 1 / hardness;
    }

    public void scan(int range) {
        for (int x = -range; x <= range; x++) {
            for (int z = -range; z <= range; z++) {
                BlockPos pos =
                        new BlockPos(worldPosition.getX() + x, targetY, worldPosition.getZ() + z);
                BlockState state = level.getBlockState(pos);
                if (!state.getFluidState().isEmpty()) continue;
                if (canBreak(state, pos)) {
                    targetX = pos.getX();
                    targetZ = pos.getZ();
                    beam = true;
                    return;
                }
            }
        }
        beam = false;
        targetY--;
    }

    private boolean canBreak(BlockState state, BlockPos pos) {
        return !state.isAir()
                && !(state.getBlock() instanceof BlockGasBase)
                && state.getDestroySpeed(level, pos) >= 0
                && state.getFluidState().isEmpty()
                && !state.is(Blocks.BEDROCK);
    }

    private boolean hasUpgrade(Item upgrade) {
        for (int i = SLOT_UPGRADE_START; i <= SLOT_UPGRADE_END; i++) {
            if (inventory.get(i).getItem() == upgrade) return true;
        }
        return false;
    }

    public int getRange() {
        int range = 1;
        for (int i = SLOT_UPGRADE_START; i <= SLOT_UPGRADE_END; i++) {
            Item item = inventory.get(i).getItem();
            if (item == ModItems.UPGRADE_EFFECT_1.get()) range += 2;
            else if (item == ModItems.UPGRADE_EFFECT_2.get()) range += 4;
            else if (item == ModItems.UPGRADE_EFFECT_3.get()) range += 6;
        }
        return Math.min(range, 25);
    }

    public int getWidth() {
        return 1 + getRange() * 2;
    }

    public boolean doesScream() {
        return hasUpgrade(ModItems.UPGRADE_SCREM.get());
    }

    @Override
    public long getPower() {
        return power;
    }

    @Override
    public void setPower(long p) {
        power = Math.max(0L, Math.min(p, MAX_POWER));
    }

    @Override
    public long getMaxPower() {
        return MAX_POWER;
    }

    @Override
    public FluidTankNTM[] getSendingTanks() {
        return sending;
    }

    @Override
    public int[] getValidUpgrades() {
        return VALID_UPGRADES;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot == SLOT_BATTERY) return IBatteryItem.isBattery(stack);
        if (slot >= SLOT_UPGRADE_START && slot <= SLOT_UPGRADE_END)
            return ItemMachineUpgrade.isUpgrade(stack);
        return false;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return ACCESSIBLE_SLOTS;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot >= SLOT_OUTPUT_START && slot <= SLOT_OUTPUT_END;
    }

    public void setOn(boolean on) {
        this.isOn = on;
        setChanged();
    }

    @Override
    public boolean hasPermission(Player player) {
        return true;
    }

    @Override
    public void receiveControl(CompoundTag data) {
        if (data.contains("toggle")) setOn(!isOn);
    }

    @Override
    public Component getDisplayName() {
        return getName();
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.miningLaser");
    }

    @Override
    public AbstractContainerMenu createMenu(
            int containerId, Inventory playerInventory, Player player) {
        return new MenuMachineMiningLaser(containerId, playerInventory, this);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.getLong("power").ifPresent(v -> power = v);
        isOn = input.getBooleanOr("isOn", false);

        redstonePowered = false;
        input.child("oil").ifPresent(tank::deserialize);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("power", power);
        output.putBoolean("isOn", isOn);
        tank.serialize(output.child("oil"));
    }

    @Override
    public long syncUnitMask() {
        return 0xfffL;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeLong(this.power);
            case 1 -> output.writeInt(this.lastTargetX);
            case 2 -> output.writeInt(this.lastTargetY);
            case 3 -> output.writeInt(this.lastTargetZ);
            case 4 -> output.writeInt(this.targetX);
            case 5 -> output.writeInt(this.targetY);
            case 6 -> output.writeInt(this.targetZ);
            case 7 -> output.writeBoolean(this.beam);
            case 8 -> output.writeBoolean(this.isOn);
            case 9 -> output.writeDouble(this.clientBreakProgress);
            case 10 -> this.tank.packetSerialize(output);
            case 11 -> output.writeBoolean(this.redstonePowered);
            default -> throw new IllegalArgumentException();
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> this.power = input.readLong();
            case 1 -> this.lastTargetX = input.readInt();
            case 2 -> this.lastTargetY = input.readInt();
            case 3 -> this.lastTargetZ = input.readInt();
            case 4 -> this.targetX = input.readInt();
            case 5 -> this.targetY = input.readInt();
            case 6 -> this.targetZ = input.readInt();
            case 7 -> this.beam = input.readBoolean();
            case 8 -> this.isOn = input.readBoolean();
            case 9 -> this.clientBreakProgress = input.readDouble();
            case 10 -> this.tank.packetDeserialize(input);
            case 11 -> this.redstonePowered = input.readBoolean();
            default -> throw new IllegalArgumentException();
        }
    }
}
