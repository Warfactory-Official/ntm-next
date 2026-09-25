// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine;

import com.hbm.api.control.IControlReceiver;
import com.hbm.api.conveyor.IConveyorBelt;
import com.hbm.api.energymk2.IEnergyHandlerMK2;
import com.hbm.api.energymk2.ItemEnergyTransfer;
import com.hbm.api.fluidmk2.FluidTankEndpoint;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.gas.BlockGasBase;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.blocks.network.CraneInserter;
import com.hbm.capability.NtmContracts;
import com.hbm.entity.item.EntityMovingItem;
import com.hbm.inventory.container.ContainerSync;
import com.hbm.inventory.container.MenuMachineExcavator;
import com.hbm.inventory.fluid.FluidStackNTM;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.recipes.ShredderRecipes;
import com.hbm.items.ModItems;
import com.hbm.items.machine.ItemDrillbit.EnumDrillType;
import com.hbm.items.machine.ItemDrillbit;
import com.hbm.items.machine.upgrade.UpgradeManager;
import com.hbm.items.machine.upgrade.UpgradeType;
import com.hbm.items.special.ItemBedrockOreBase;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.tags.HbmBlockTags;
import com.hbm.tileentity.BlockEntityBedrockOre;
import com.hbm.tileentity.BlockEntityMachineBase;
import com.hbm.tileentity.IFluidCopiable;
import com.hbm.tileentity.IUpgradeInfoProvider;
import com.hbm.util.InventoryUtil;
import com.hbm.util.TickPhase;
import com.hbm.world.NtmWorldgenFields;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class BlockEntityMachineExcavator extends BlockEntityMachineBase
        implements IEnergyHandlerMK2,
                FluidTankEndpoint,
                MenuProvider,
                IUpgradeInfoProvider,
                IControlReceiver,
                IFluidCopiable,
                SyncUnitSchema {

    public static final int SLOT_BATTERY = 0;
    public static final int SLOT_FLUID_ID = 1;
    public static final int SLOT_UPGRADE_START = 2;
    public static final int SLOT_UPGRADE_END = 3;
    public static final int SLOT_DRILL = 4;
    public static final int SLOT_BUFFER_START = 5;
    public static final int SLOT_BUFFER_END = 13;
    public static final int SLOT_COUNT = 14;

    public static final long MAX_POWER = 1_000_000L;
    public static final long BASE_CONSUMPTION = 10_000L;
    public static final int TANK_CAPACITY = 16_000;
    public static final int CHUTE_TIME = 40;

    public static final int BEDROCK_HARDNESS = 5 * 60 * 20;
    public static final int VEIN_DEPTH = 10;

    public static final int PICKUP_DELAY = 60;

    private static final TagKey<Block> ORES =
            TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath("c", "ores"));

    private static final int[] VALID_UPGRADES =
            IUpgradeInfoProvider.upgradeCaps(
                    UpgradeType.SPEED, 3, UpgradeType.POWER, 3, UpgradeType.EFFECT, 3);

    @SyncField(units = 1L << 9)
    public final FluidTankNTM tank = new FluidTankNTM(null, TANK_CAPACITY);

    private final FluidTankNTM[] receiving;
    private final UpgradeManager upgradeManager = new UpgradeManager(this);

    @SyncField(units = 1L << 8)
    @ContainerSync
    public long power;

    @SyncField(units = 1L << 5)
    public boolean operational;

    @SyncField(units = 1L << 0)
    public boolean enableDrill;

    @SyncField(units = 1L << 1)
    public boolean enableCrusher;

    @SyncField(units = 1L << 2)
    public boolean enableWalling;

    @SyncField(units = 1L << 3)
    public boolean enableVeinMiner;

    @SyncField(units = 1L << 4)
    public boolean enableSilkTouch;

    @SyncField(units = 1L << 7)
    public int chuteTimer;

    @SyncField(units = 1L << 6)
    public int targetDepth;

    public float drillRotation;
    public float prevDrillRotation;
    public float drillExtension;
    public float prevDrillExtension;
    public float crusherRotation;
    public float prevCrusherRotation;

    private int ticksWorked;
    private boolean bedrockDrilling;
    private double speed = 1.0D;
    private long consumption = BASE_CONSUMPTION;

    private final Set<BlockPos> recursionBrake = new HashSet<>();
    private int minX, minY, minZ, maxX, maxY, maxZ;

    public BlockEntityMachineExcavator(BlockPos pos, BlockState state) {
        super(ModBlockEntities.EXCAVATOR.get(), pos, state, SLOT_COUNT);
        receiving = new FluidTankNTM[] {tank};
    }

    private int scanUpgrades() {
        upgradeManager.scan(this, SLOT_UPGRADE_START, SLOT_UPGRADE_END);
        int speedLevel = upgradeManager.getLevel(UpgradeType.SPEED);
        consumption =
                BASE_CONSUMPTION
                        * (1 + speedLevel)
                        / (1 + upgradeManager.getLevel(UpgradeType.POWER));
        return speedLevel;
    }

    @Override
    public void tickServer() {
        int speedLevel = scanUpgrades();

        tank.setType(SLOT_FLUID_ID, SLOT_FLUID_ID, inventory);

        if (TickPhase.every(this, 20)) tryEjectBuffer();
        if (chuteTimer > 0) chuteTimer--;

        power += ItemEnergyTransfer.extract(this, SLOT_BATTERY, MAX_POWER - power, false);
        operational = false;
        int radiusLevel = upgradeManager.getLevel(UpgradeType.EFFECT);

        EnumDrillType type = getInstalledDrill();
        if (enableDrill && type != null && power >= consumption) {
            operational = true;
            power -= consumption;

            speed = type.speed * (1 + speedLevel / 2D);

            int maxDepth = maxDepth();
            if ((bedrockDrilling || targetDepth <= maxDepth) && tryDrill(1 + radiusLevel * 2)) {
                targetDepth++;
                if (targetDepth > maxDepth) enableDrill = false;
            }
        } else {
            targetDepth = 0;
        }

        networkPackNT(150);
    }

    @Override
    public void tickClient() {
        scanUpgrades();
        prevDrillExtension = drillExtension;

        if (drillExtension != targetDepth) {
            float diff = Math.abs(drillExtension - targetDepth);
            float step = Math.max(0.15F, diff / 10F);

            if (diff <= step) {
                drillExtension = targetDepth;
            } else {
                drillExtension -= Math.signum(drillExtension - targetDepth) * step;
            }
        }

        prevDrillRotation = drillRotation;
        prevCrusherRotation = crusherRotation;

        if (operational) {
            drillRotation += 15F;
            if (enableCrusher) crusherRotation += 15F;
        }

        if (drillRotation >= 360F) {
            drillRotation -= 360F;
            prevDrillRotation -= 360F;
        }

        if (crusherRotation >= 360F) {
            crusherRotation -= 360F;
            prevCrusherRotation -= 360F;
        }
    }

    private int maxDepth() {
        return worldPosition.getY() - 4 - level.getMinY();
    }

    public int drillY() {
        return worldPosition.getY() - targetDepth - 4;
    }

    private Direction facing() {
        return BlockMultiblockCore.coreFacing(getBlockState());
    }

    private boolean tryDrill(int radius) {
        int y = drillY();

        if (targetDepth == 0 || y == level.getMinY()) radius = 1;

        for (int ring = 1; ring <= radius; ring++) {

            boolean ignoreAll = true;
            float combinedHardness = 0F;
            BlockPos bedrockOre = null;
            bedrockDrilling = false;

            for (int x = worldPosition.getX() - ring; x <= worldPosition.getX() + ring; x++) {
                for (int z = worldPosition.getZ() - ring; z <= worldPosition.getZ() + ring; z++) {

                    if (ring != 1
                            && x != worldPosition.getX() - ring
                            && x != worldPosition.getX() + ring
                            && z != worldPosition.getZ() - ring
                            && z != worldPosition.getZ() + ring) {
                        continue;
                    }

                    BlockPos pos = new BlockPos(x, y, z);
                    BlockState state = level.getBlockState(pos);

                    if (state.is(ModBlocks.ORE_BEDROCK.get())) {
                        combinedHardness = BEDROCK_HARDNESS;
                        bedrockOre = pos;
                        bedrockDrilling = true;
                        enableCrusher = false;
                        ignoreAll = false;
                        break;
                    }

                    if (state.is(HbmBlockTags.DEPTH_ROCK)) enableDrill = false;

                    if (shouldIgnoreBlock(state, pos)) continue;

                    ignoreAll = false;
                    combinedHardness += state.getDestroySpeed(level, pos);
                }
            }

            if (!ignoreAll) {
                ticksWorked++;

                int ticksToWork = (int) Math.ceil(combinedHardness / speed);

                if (ticksWorked >= ticksToWork) {

                    if (bedrockOre == null) {
                        breakBlocks(ring);
                        buildWall(ring + 1, ring == radius && enableWalling);
                        if (ring == radius) mineOuterOres(ring + 1);
                        tryCollect(radius + 1);
                    } else {
                        collectBedrock(bedrockOre);
                    }
                    ticksWorked = 0;
                }

                return false;
            } else {
                tryCollect(radius + 1);
            }
        }

        buildWall(radius + 1, enableWalling);
        ticksWorked = 0;
        return true;
    }

    private void collectBedrock(BlockPos pos) {
        if (!(level.getBlockEntity(pos) instanceof BlockEntityBedrockOre ore)) return;
        if (ore.resource.isEmpty()) return;

        EnumDrillType drill = getInstalledDrill();
        if (drill == null || ore.tier > drill.tier) return;

        FluidStackNTM acid = ore.acid;
        if (acid != null) {
            if (acid.type() != tank.getTankType() || acid.amount() > tank.getFill()) return;
            tank.setFill(tank.getFill() - acid.amount());
        }

        ItemStack stack = ore.resource.copy();
        if (stack.getItem() == ModItems.BEDROCK_ORE_BASE.get()) {
            ItemBedrockOreBase.setOreAmount(
                    stack,
                    NtmWorldgenFields.get((ServerLevel) level).bedrock(),
                    pos.getX(),
                    pos.getZ(),
                    1D + drill.fortune * 0.1D);
        }

        List<ItemStack> stacks = new ArrayList<>();
        stacks.add(stack);
        supplyOutput(stacks);

        if (stack.isEmpty()) return;
        bankStack(stack);
    }

    private void breakBlocks(int ring) {
        int y = drillY();

        for (int x = worldPosition.getX() - ring; x <= worldPosition.getX() + ring; x++) {
            for (int z = worldPosition.getZ() - ring; z <= worldPosition.getZ() + ring; z++) {

                if (ring != 1
                        && x != worldPosition.getX() - ring
                        && x != worldPosition.getX() + ring
                        && z != worldPosition.getZ() - ring
                        && z != worldPosition.getZ() + ring) {
                    continue;
                }

                BlockPos pos = new BlockPos(x, y, z);
                if (!shouldIgnoreBlock(level.getBlockState(pos), pos)) tryMineAtLocation(pos);
            }
        }
    }

    public void tryMineAtLocation(BlockPos pos) {
        BlockState state = level.getBlockState(pos);

        if (canVeinMine() && isOre(state)) {
            minX = maxX = pos.getX();
            minY = maxY = pos.getY();
            minZ = maxZ = pos.getZ();
            breakRecursively(pos, VEIN_DEPTH);
            recursionBrake.clear();

            AABB box = new AABB(minX, minY, minZ, maxX + 1, maxY + 1, maxZ + 1);
            for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, box)) {
                item.setPos(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
            }
            return;
        }

        breakSingleBlock(state, pos);
    }

    private boolean isOre(BlockState state) {
        return state.is(ORES);
    }

    private void breakRecursively(BlockPos pos, int depth) {
        if (depth < 0) return;
        if (!recursionBrake.add(pos)) return;

        BlockState state = level.getBlockState(pos);

        for (Direction dir : Direction.VALUES) {
            BlockPos side = pos.relative(dir);
            if (level.getBlockState(side).is(state.getBlock())) breakRecursively(side, depth - 1);
        }

        breakSingleBlock(state, pos);

        minX = Math.min(minX, pos.getX());
        maxX = Math.max(maxX, pos.getX());
        minY = Math.min(minY, pos.getY());
        maxY = Math.max(maxY, pos.getY());
        minZ = Math.min(minZ, pos.getZ());
        maxZ = Math.max(maxZ, pos.getZ());

        if (enableWalling)
            level.setBlockAndUpdate(pos, ModBlocks.BARRICADE.get().defaultBlockState());
    }

    private void breakSingleBlock(BlockState state, BlockPos pos) {
        List<ItemStack> items =
                new ArrayList<>(
                        Block.getDrops(
                                state,
                                (ServerLevel) level,
                                pos,
                                level.getBlockEntity(pos),
                                null,
                                drillTool()));

        if (enableCrusher) {
            List<ItemStack> crushedList = new ArrayList<>();

            for (ItemStack stack : items) {
                ItemStack crushed = ShredderRecipes.getShredderResult(stack).copy();

                if (crushed.getItem() == ModItems.SCRAP.get()
                        || crushed.getItem() == ModItems.DUST.get()) {
                    crushedList.add(stack);
                } else {
                    crushed.setCount(crushed.getCount() * stack.getCount());
                    crushedList.add(crushed);
                }
            }

            items = crushedList;
        }

        if (state.is(ModBlocks.BARRICADE.get())) items.clear();

        for (ItemStack item : items) {
            level.addFreshEntity(
                    new ItemEntity(
                            level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, item));
        }

        level.destroyBlock(pos, false);
    }

    private ItemStack drillTool() {
        int fortune = getFortuneLevel();
        boolean silk = canSilkTouch();
        if (fortune <= 0 && !silk) return ItemStack.EMPTY;

        ItemStack tool = new ItemStack(Items.NETHERITE_PICKAXE);
        var lookup = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        ItemEnchantments.Mutable enchantments =
                new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        if (silk) enchantments.set(lookup.getOrThrow(Enchantments.SILK_TOUCH), 1);
        else if (fortune > 0) enchantments.set(lookup.getOrThrow(Enchantments.FORTUNE), fortune);
        tool.set(DataComponents.ENCHANTMENTS, enchantments.toImmutable());
        return tool;
    }

    private void buildWall(int ring, boolean wallEverything) {
        int y = drillY();

        for (int x = worldPosition.getX() - ring; x <= worldPosition.getX() + ring; x++) {
            for (int z = worldPosition.getZ() - ring; z <= worldPosition.getZ() + ring; z++) {

                BlockPos pos = new BlockPos(x, y, z);
                BlockState state = level.getBlockState(pos);
                boolean liquid = !state.getFluidState().isEmpty();

                if (x == worldPosition.getX() - ring
                        || x == worldPosition.getX() + ring
                        || z == worldPosition.getZ() - ring
                        || z == worldPosition.getZ() + ring) {

                    if (state.canBeReplaced() && (wallEverything || liquid)) {
                        level.setBlockAndUpdate(pos, ModBlocks.BARRICADE.get().defaultBlockState());
                    }
                } else if (liquid) {
                    level.removeBlock(pos, false);
                }
            }
        }
    }

    private void mineOuterOres(int ring) {
        int y = drillY();

        for (int x = worldPosition.getX() - ring; x <= worldPosition.getX() + ring; x++) {
            for (int z = worldPosition.getZ() - ring; z <= worldPosition.getZ() + ring; z++) {

                if (ring != 1
                        && x != worldPosition.getX() - ring
                        && x != worldPosition.getX() + ring
                        && z != worldPosition.getZ() - ring
                        && z != worldPosition.getZ() + ring) {
                    continue;
                }

                BlockPos pos = new BlockPos(x, y, z);
                BlockState state = level.getBlockState(pos);
                if (!shouldIgnoreBlock(state, pos) && isOre(state)) tryMineAtLocation(pos);
            }
        }
    }

    private void tryEjectBuffer() {
        List<ItemStack> items = new ArrayList<>();

        for (int i = SLOT_BUFFER_START; i <= SLOT_BUFFER_END; i++) {
            ItemStack stack = inventory.get(i);
            if (!stack.isEmpty()) items.add(stack.copy());
        }

        supplyOutput(items);
        items.removeIf(ItemStack::isEmpty);

        for (int i = SLOT_BUFFER_START; i <= SLOT_BUFFER_END; i++) {
            int index = i - SLOT_BUFFER_START;
            inventory.set(i, items.size() > index ? items.get(index).copy() : ItemStack.EMPTY);
        }
        setChanged();
    }

    private void tryCollect(int radius) {
        int y = drillY();

        AABB box =
                new AABB(
                        worldPosition.getX() - radius,
                        y - 1,
                        worldPosition.getZ() - radius,
                        worldPosition.getX() + radius + 1,
                        y + 2,
                        worldPosition.getZ() + radius + 1);
        List<ItemEntity> items = level.getEntitiesOfClass(ItemEntity.class, box);

        List<ItemStack> stacks = new ArrayList<>();
        for (ItemEntity item : items) if (!item.isRemoved()) stacks.add(item.getItem());

        supplyOutput(stacks);

        for (ItemEntity item : items) {
            if (item.isRemoved()) continue;
            ItemStack stack = item.getItem();
            if (stack.isEmpty()) {
                item.discard();
                continue;
            }

            if (bankStack(stack)) {
                item.discard();
                item.setPickUpDelay(PICKUP_DELAY);
            }
        }
    }

    private boolean bankStack(ItemStack stack) {
        int before = stack.getCount();
        ItemStack left =
                InventoryUtil.tryAddItemToInventory(
                        inventory, SLOT_BUFFER_START, SLOT_BUFFER_END, stack);

        if (left.getCount() < before) {
            chuteTimer = CHUTE_TIME;
            setChanged();
        }

        return left.isEmpty();
    }

    public BlockPos chutePos() {
        Direction dir = facing();
        return worldPosition.offset(dir.getStepX() * 4, -3, dir.getStepZ() * 4);
    }

    private void supplyOutput(List<ItemStack> items) {
        BlockPos pos = chutePos();
        if (!level.isLoaded(pos)) return;
        Direction side = facing().getOpposite();

        if (InventoryUtil.inventoryAt(level, pos, side)) {
            for (ItemStack item : items) {
                if (item.isEmpty()) continue;
                InventoryUtil.insertAt(level, pos, side, item);
                chuteTimer = CHUTE_TIME;
            }
        }

        IConveyorBelt belt = NtmContracts.CONVEYOR_BELT.at(level, pos);
        if (belt != null) supplyConveyor(belt, items, pos);
    }

    private void supplyConveyor(IConveyorBelt belt, List<ItemStack> items, BlockPos pos) {
        RandomSource random = level.getRandom();

        for (ItemStack item : items) {
            if (item.isEmpty()) continue;

            Vec3 base =
                    new Vec3(
                            pos.getX() + random.nextDouble(),
                            pos.getY() + 0.5,
                            pos.getZ() + random.nextDouble());
            Vec3 snap = belt.getClosestSnappingPosition(level, pos, base);

            EntityMovingItem moving = new EntityMovingItem(level);
            moving.setItemStack(item.copy());
            moving.snapTo(base.x, snap.y, base.z, 0F, 0F);
            level.addFreshEntity(moving);

            item.setCount(0);
            chuteTimer = CHUTE_TIME;
        }
    }

    public boolean shouldIgnoreBlock(BlockState state, BlockPos pos) {
        return state.isAir()
                || state.getBlock() instanceof BlockGasBase
                || state.getDestroySpeed(level, pos) < 0
                || !state.getFluidState().isEmpty()
                || state.is(Blocks.BEDROCK);
    }

    public @Nullable EnumDrillType getInstalledDrill() {
        return ItemDrillbit.typeOf(inventory.get(SLOT_DRILL));
    }

    public int getFortuneLevel() {
        EnumDrillType type = getInstalledDrill();
        return type != null ? type.fortune : 0;
    }

    public boolean canVeinMine() {
        EnumDrillType type = getInstalledDrill();
        return enableVeinMiner && type != null && type.vein;
    }

    public boolean canSilkTouch() {
        EnumDrillType type = getInstalledDrill();
        return enableSilkTouch && type != null && type.silk;
    }

    public long getPowerConsumption() {
        return consumption;
    }

    @Override
    public boolean hasPermission(Player player) {
        return stillValid(player);
    }

    @Override
    public void receiveControl(CompoundTag data) {
        if (data.contains("drill")) enableDrill = !enableDrill;
        if (data.contains("crusher")) enableCrusher = !enableCrusher;
        if (data.contains("walling")) enableWalling = !enableWalling;
        if (data.contains("veinminer")) enableVeinMiner = !enableVeinMiner;
        if (data.contains("silktouch")) enableSilkTouch = !enableSilkTouch;
        setChanged();
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
    public FluidTankNTM[] getReceivingTanks() {
        return receiving;
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
        return new MenuMachineExcavator(containerId, playerInventory, this);
    }

    @Override
    public FluidTankNTM[] getAllTanks() {
        return new FluidTankNTM[] {tank};
    }

    @Override
    public FluidTankNTM getTankToPaste() {
        return tank;
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        enableDrill = input.getBooleanOr("d", false);
        enableCrusher = input.getBooleanOr("c", false);
        enableWalling = input.getBooleanOr("w", false);
        enableVeinMiner = input.getBooleanOr("v", false);
        enableSilkTouch = input.getBooleanOr("s", false);
        targetDepth = input.getIntOr("t", 0);
        input.getLong("power").ifPresent(v -> power = v);
        input.child("tank").ifPresent(tank::deserialize);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putBoolean("d", enableDrill);
        output.putBoolean("c", enableCrusher);
        output.putBoolean("w", enableWalling);
        output.putBoolean("v", enableVeinMiner);
        output.putBoolean("s", enableSilkTouch);
        output.putInt("t", targetDepth);
        output.putLong("power", power);
        tank.serialize(output.child("tank"));
    }

    @Override
    public long syncUnitMask() {
        return 0x3ffL;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeBoolean(this.enableDrill);
            case 1 -> output.writeBoolean(this.enableCrusher);
            case 2 -> output.writeBoolean(this.enableWalling);
            case 3 -> output.writeBoolean(this.enableVeinMiner);
            case 4 -> output.writeBoolean(this.enableSilkTouch);
            case 5 -> output.writeBoolean(this.operational);
            case 6 -> output.writeInt(this.targetDepth);
            case 7 -> output.writeInt(this.chuteTimer);
            case 8 -> output.writeLong(this.power);
            case 9 -> this.tank.packetSerialize(output);
            default -> throw new IllegalArgumentException();
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> this.enableDrill = input.readBoolean();
            case 1 -> this.enableCrusher = input.readBoolean();
            case 2 -> this.enableWalling = input.readBoolean();
            case 3 -> this.enableVeinMiner = input.readBoolean();
            case 4 -> this.enableSilkTouch = input.readBoolean();
            case 5 -> this.operational = input.readBoolean();
            case 6 -> this.targetDepth = input.readInt();
            case 7 -> this.chuteTimer = input.readInt();
            case 8 -> this.power = input.readLong();
            case 9 -> this.tank.packetDeserialize(input);
            default -> throw new IllegalArgumentException();
        }
    }
}
