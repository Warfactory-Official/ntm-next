// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: Contributors to Hbm's Nuclear Tech Mod
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine;

import com.hbm.advancement.AwardRegions;
import com.hbm.advancement.DetonationTrigger;
import com.hbm.advancement.HbmCriteria;
import com.hbm.api.control.IControlReceiver;
import com.hbm.api.fluidmk2.FluidFlushOutputs;
import com.hbm.api.fluidmk2.FluidFlushSender;
import com.hbm.api.fluidmk2.FlushFaces;
import com.hbm.api.fluidmk2.FlushLanes;
import com.hbm.api.redstoneoverradio.IRORValueProvider;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.capability.port.FluidPort;
import com.hbm.capability.port.IPortHost;
import com.hbm.entity.projectile.EntityShrapnel;
import com.hbm.handler.radiation.RadiationSystemNT;
import com.hbm.inventory.container.MenuWatz;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.fluid.trait.FT_Heatable.HeatingStep;
import com.hbm.inventory.fluid.trait.FT_Heatable;
import com.hbm.inventory.material.MaterialShapes;
import com.hbm.inventory.material.Mats;
import com.hbm.items.ModItems;
import com.hbm.items.machine.ItemWatzPellet.EnumWatzType;
import com.hbm.items.machine.ItemWatzPellet;
import com.hbm.items.special.Autogen;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.particle.helper.ExplosionCreator;
import com.hbm.sound.ModSounds;
import com.hbm.tileentity.BlockEntityMachineBase;
import com.hbm.tileentity.IFluidCopiable;
import com.hbm.util.function.Function;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public class BlockEntityWatz extends BlockEntityMachineBase
        implements IPortHost,
                MenuProvider,
                IControlReceiver,
                IRORValueProvider,
                IFluidCopiable,
                FluidFlushSender,
                SyncUnitSchema {

    public static final int SLOT_COUNT = 24;
    public static final int TANK_CAPACITY = 64_000;

    public static final double PASSIVE_COOLING = 0.99D;

    public static final double COOLING_FACTOR = 0.2D;

    public static final int SEGMENT_PITCH = 3;

    public static final List<StructureDrop> STRUCTURE_DROPS =
            List.of(
                    new StructureDrop(() -> ModBlocks.WATZ_END.get().asItem(), 48),
                    new StructureDrop(BlockEntityWatz::duraBolt, 64),
                    new StructureDrop(BlockEntityWatz::duraBolt, 64),
                    new StructureDrop(BlockEntityWatz::duraBolt, 64),
                    new StructureDrop(() -> ModBlocks.WATZ_ELEMENT.get().asItem(), 36),
                    new StructureDrop(() -> ModBlocks.WATZ_COOLER.get().asItem(), 26),
                    new StructureDrop(() -> ModBlocks.STRUCT_WATZ_CORE.get().asItem(), 1));
    public static final String[] ROR =
            new String[] {
                PREFIX_VALUE + "heat",
                PREFIX_VALUE + "flux",
                PREFIX_VALUE + "mud",
                PREFIX_VALUE + "coolant_hot",
                PREFIX_VALUE + "coolant_cold",
            };
    private static final int[] ACCESSIBLE_SLOTS = {
        0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23
    };
    public final FluidTankNTM[] tanks = new FluidTankNTM[3];

    @SyncField(units = 1L << 4)
    public final FluidTankNTM[] sharedTanks = new FluidTankNTM[3];

    public final NonNullList<ItemStack> locks = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private final FluidPort intake;
    private final FluidPort outlet;
    private final FluidFlushOutputs flush = new FluidFlushOutputs();
    private final FluidTankNTM[] sendingBottom;

    @SyncField(units = 1L << 0)
    public int heat;

    @SyncField(units = 1L << 3)
    public double fluxLastBase;

    @SyncField(units = 1L << 3)
    public double fluxLastReaction;

    public double fluxDisplay;

    @SyncField(units = 1L << 1)
    public boolean isOn;

    @SyncField(units = 1L << 2)
    public boolean isLocked = false;

    public BlockEntityWatz(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WATZ.get(), pos, state, SLOT_COUNT);
        this.tanks[0] = new FluidTankNTM(NTMFluids.COOLANT, TANK_CAPACITY);
        this.tanks[1] = new FluidTankNTM(NTMFluids.COOLANT_HOT, TANK_CAPACITY);
        this.tanks[2] = new FluidTankNTM(NTMFluids.WATZ_MUD, TANK_CAPACITY);
        for (int i = 0; i < 3; i++) {
            this.sharedTanks[i] = new FluidTankNTM(this.tanks[i].getTankType(), TANK_CAPACITY);
        }
        this.intake = FluidPort.of(tanks, new int[] {0}, new int[0], this::markChanged);
        this.outlet = FluidPort.of(tanks, new int[] {0}, new int[] {1, 2}, this::markChanged);
        this.sendingBottom = new FluidTankNTM[] {tanks[1], tanks[2]};
    }

    private static void addMud(FluidTankNTM pooled, int amount, int[] mudOverflow) {
        int before = pooled.getFill();
        pooled.setFill(before + amount);
        mudOverflow[0] += amount - (pooled.getFill() - before);
    }

    private static Item duraBolt() {
        return Autogen.require(MaterialShapes.BOLT, Mats.MAT_DURA);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.watzPowerplant");
    }

    protected void resetSharedTanks() {
        for (int i = 0; i < 3; i++) {
            sharedTanks[i].setTankType(tanks[i].getTankType());
            sharedTanks[i].changeTankSize(TANK_CAPACITY);
            sharedTanks[i].setFill(tanks[i].getFill());
        }
    }

    @Override
    public void tickServer() {
        resetSharedTanks();

        if (updateLock()) return;

        boolean turnedOn = pumpAbove && level.getSignal(worldPosition.above(5), Direction.DOWN) > 0;

        List<BlockEntityWatz> segments = new ArrayList<>();
        segments.add(this);

        for (int y = worldPosition.getY() - SEGMENT_PITCH;
                y >= level.getMinY();
                y -= SEGMENT_PITCH) {
            BlockPos below = new BlockPos(worldPosition.getX(), y, worldPosition.getZ());
            if (level.getBlockEntity(below) instanceof BlockEntityWatz segment) {
                segments.add(segment);
            } else {
                break;
            }
        }

        FluidTankNTM[] pooled = new FluidTankNTM[3];
        for (int i = 0; i < 3; i++) pooled[i] = new FluidTankNTM(tanks[i].getTankType(), 0);

        for (BlockEntityWatz segment : segments) {
            segment.setupCoolant();
            for (int i = 0; i < 3; i++) {
                pooled[i].changeTankSize(pooled[i].getMaxFill() + segment.tanks[i].getMaxFill());
                pooled[i].setFill(pooled[i].getFill() + segment.tanks[i].getFill());
            }
        }

        for (int i = segments.size() - 1; i >= 0; i--) {
            segments.get(i).updateCoolant(pooled);
        }

        int[] mudOverflow = new int[1];
        this.updateReaction(null, pooled, turnedOn, mudOverflow);
        for (int i = 1; i < segments.size(); i++) {
            segments.get(i).updateReaction(segments.get(i - 1), pooled, turnedOn, mudOverflow);
        }

        for (BlockEntityWatz segment : segments) {
            for (int i = 0; i < 3; i++) {
                segment.sharedTanks[i].setTankType(pooled[i].getTankType());
                segment.sharedTanks[i].changeTankSize(pooled[i].getMaxFill());
                segment.sharedTanks[i].setFill(pooled[i].getFill());
            }
            segment.isOn = turnedOn;
            segment.networkPackNT(25);
            segment.heat *= PASSIVE_COOLING;
            segment.markChanged();
        }

        int[] remaining = {pooled[0].getFill(), pooled[1].getFill(), pooled[2].getFill()};
        for (int i = segments.size() - 1; i >= 0; i--) {
            BlockEntityWatz segment = segments.get(i);
            for (int j = 0; j < 3; j++) {
                int min = Math.min(segment.tanks[j].getMaxFill(), remaining[j]);
                remaining[j] -= min;
                segment.tanks[j].setFill(min);
            }
        }

        segments.get(segments.size() - 1).sendOutBottom((ServerLevel) level);

        if (remaining[2] > 0 || mudOverflow[0] > 0) meltdown();
    }

    private void sendOutBottom(ServerLevel level) {
        flush.provide(level, this);
    }

    @Override
    public FluidTankNTM[] getSendingTanks() {
        return sendingBottom;
    }

    @Override
    public void declareFlush(FlushLanes out) {
        for (FluidTankNTM tank : sendingBottom) {
            out.add(outlet, tank, FlushFaces.activePlane((cell, side) -> side == Direction.DOWN));
        }
    }

    public void setupCoolant() {
        tanks[0].setTankType(NTMFluids.COOLANT);
        tanks[1].setTankType(
                NTMFluidProperties.getTrait(NTMFluids.COOLANT, FT_Heatable.class)
                        .getFirstStep()
                        .typeProduced());
    }

    public void updateCoolant(FluidTankNTM[] tanks) {

        double heatToUse = this.heat * COOLING_FACTOR;

        FT_Heatable trait = NTMFluidProperties.getTrait(tanks[0].getTankType(), FT_Heatable.class);
        HeatingStep step = trait.getFirstStep();

        int heatCycles = (int) (heatToUse / step.heatReq);
        int coolCycles = tanks[0].getFill() / step.amountReq;
        int hotCycles = (tanks[1].getMaxFill() - tanks[1].getFill()) / step.amountProduced;

        int cycles = Math.min(heatCycles, Math.min(hotCycles, coolCycles));
        this.heat -= cycles * step.heatReq;
        tanks[0].setFill(tanks[0].getFill() - cycles * step.amountReq);
        tanks[1].setFill(tanks[1].getFill() + cycles * step.amountProduced);
    }

    public void updateReaction(
            @Nullable BlockEntityWatz above,
            FluidTankNTM[] tanks,
            boolean turnedOn,
            int[] mudOverflow) {

        if (turnedOn) {
            List<ItemStack> pellets = new ArrayList<>();

            for (int i = 0; i < SLOT_COUNT; i++) {
                ItemStack stack = inventory.get(i);
                if (ModItems.WATZ_PELLET.typeOf(stack) != null) pellets.add(stack);
            }

            double baseFlux = 0D;

            for (ItemStack stack : pellets) {
                baseFlux += ItemWatzPellet.typeOf(stack).passive;
            }

            double inputFlux = baseFlux + fluxLastReaction;
            double addedFlux = 0D;
            double addedHeat = 0D;

            for (ItemStack stack : pellets) {
                EnumWatzType type = ItemWatzPellet.typeOf(stack);
                Function burnFunc = type.burnFunc;
                Function heatDiv = type.heatDiv;

                if (burnFunc != null) {
                    double div = heatDiv != null ? heatDiv.effonix(heat) : 1D;
                    double burn = burnFunc.effonix(inputFlux) / div;
                    ItemWatzPellet.setYield(stack, ItemWatzPellet.getYield(stack) - burn);
                    addedFlux += burn;
                    addedHeat += type.heatEmission * burn;
                    addMud(tanks[2], (int) Math.round(type.mudContent * burn), mudOverflow);
                }
            }

            for (ItemStack stack : pellets) {
                EnumWatzType type = ItemWatzPellet.typeOf(stack);
                Function absorbFunc = type.absorbFunc;

                if (absorbFunc != null) {
                    double absorb = absorbFunc.effonix(baseFlux + fluxLastReaction);
                    addedHeat += absorb;
                    ItemWatzPellet.setYield(stack, ItemWatzPellet.getYield(stack) - absorb);
                    addMud(tanks[2], (int) Math.round(type.mudContent * absorb), mudOverflow);
                }
            }

            this.heat += addedHeat;
            this.fluxLastBase = baseFlux;
            this.fluxLastReaction = addedFlux;

        } else {
            this.fluxLastBase = 0;
            this.fluxLastReaction = 0;
        }

        for (int i = 0; i < SLOT_COUNT; i++) {
            ItemStack stack = inventory.get(i);

            if (ModItems.WATZ_PELLET.typeOf(stack) != null
                    && ItemWatzPellet.getEnrichment(stack) <= 0) {
                inventory.set(i, ModItems.WATZ_PELLET_DEPLETED.stack(ItemWatzPellet.typeOf(stack)));
            }
        }

        if (above != null) {
            for (int i = 0; i < SLOT_COUNT; i++) {
                ItemStack stackBottom = inventory.get(i);
                ItemStack stackTop = above.inventory.get(i);

                if (stackBottom.isEmpty() && !stackTop.isEmpty()) {
                    inventory.set(i, stackTop.copy());
                    above.inventory.set(i, ItemStack.EMPTY);
                }

                if (ModItems.WATZ_PELLET.typeOf(stackBottom) != null
                        && ModItems.WATZ_PELLET_DEPLETED.typeOf(stackTop) != null) {
                    ItemStack buf = stackTop.copy();
                    above.inventory.set(i, stackBottom.copy());
                    inventory.set(i, buf);
                }
            }
        }
    }

    public boolean updateLock() {
        return level.getBlockEntity(worldPosition.above(SEGMENT_PITCH)) instanceof BlockEntityWatz;
    }

    private void meltdown() {
        int x = worldPosition.getX(), y = worldPosition.getY(), z = worldPosition.getZ();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        BlockMultiblockCore.withoutTeardown(
                () -> {
                    for (int ox = -3; ox <= 3; ox++) {
                        for (int oy = 3; oy < 6; oy++) {
                            for (int oz = -3; oz <= 3; oz++) {
                                level.setBlock(
                                        cursor.set(x + ox, y + oy, z + oz),
                                        Blocks.AIR.defaultBlockState(),
                                        3);
                            }
                        }
                    }
                    disassemble();
                });

        if (level instanceof ServerLevel server) {
            RadiationSystemNT.incrementRad(server, worldPosition.above(), 1_000D, 1_000D * 1024D);
        }

        level.playSound(
                null,
                x + 0.5,
                y + 2,
                z + 0.5,
                ModSounds.RBMK_EXPLOSION.get(),
                SoundSource.BLOCKS,
                50.0F,
                1.0F);
        ExplosionCreator.composeEffectRBMKMush(level, x + 0.5, y + 2, z + 0.5, 5F);
    }

    private void disassemble() {
        int count = 20;
        RandomSource rand = level.getRandom();
        for (int i = 0; i < count * 5; i++) {
            EntityShrapnel shrapnel =
                    new EntityShrapnel(
                            level,
                            worldPosition.getX() + 0.5,
                            worldPosition.getY() + 3,
                            worldPosition.getZ() + 0.5);
            shrapnel.setDeltaMovement(
                    rand.nextGaussian() * 1 * (1 + (count / 100)),
                    ((rand.nextFloat() * 0.5) + 0.5) * (1 + (count / (15 + rand.nextInt(21))))
                            + (rand.nextFloat() / 50 * count),
                    rand.nextGaussian() * 1 * (1 + (count / 100)));
            shrapnel.setTrail(EntityShrapnel.TRAIL_WATZ);
            level.addFreshEntity(shrapnel);
        }

        dropStructure();

        Block mud = ModBlocks.MUD_BLOCK.get();
        for (int i = 0; i < 3; i++) {
            level.setBlock(worldPosition.above(i), mud.defaultBlockState(), 3);
        }

        Block element = ModBlocks.WATZ_ELEMENT.get();
        Block cooler = ModBlocks.WATZ_COOLER.get();
        Block end = ModBlocks.WATZ_END_BOLTED.get();

        setBrokenColumn(0, element, 1, 0);
        setBrokenColumn(0, element, 2, 0);
        setBrokenColumn(0, element, 0, 1);
        setBrokenColumn(0, element, 0, 2);
        setBrokenColumn(0, element, -1, 0);
        setBrokenColumn(0, element, -2, 0);
        setBrokenColumn(0, element, 0, -1);
        setBrokenColumn(0, element, 0, -2);
        setBrokenColumn(0, element, 1, 1);
        setBrokenColumn(0, element, 1, -1);
        setBrokenColumn(0, element, -1, 1);
        setBrokenColumn(0, element, -1, -1);
        setBrokenColumn(0, cooler, 2, 1);
        setBrokenColumn(0, cooler, 2, -1);
        setBrokenColumn(0, cooler, 1, 2);
        setBrokenColumn(0, cooler, -1, 2);
        setBrokenColumn(0, cooler, -2, 1);
        setBrokenColumn(0, cooler, -2, -1);
        setBrokenColumn(0, cooler, 1, -2);
        setBrokenColumn(0, cooler, -1, -2);

        for (int j = -1; j < 2; j++) {
            setBrokenColumn(1, end, 3, j);
            setBrokenColumn(1, end, j, 3);
            setBrokenColumn(1, end, -3, j);
            setBrokenColumn(1, end, j, -3);
        }
        setBrokenColumn(1, end, 2, 2);
        setBrokenColumn(1, end, 2, -2);
        setBrokenColumn(1, end, -2, 2);
        setBrokenColumn(1, end, -2, -2);

        if (level instanceof ServerLevel server) {
            AwardRegions.within(
                    server,
                    worldPosition.getX() + 0.5,
                    worldPosition.getY() + 0.5,
                    worldPosition.getZ() + 0.5,
                    50,
                    p -> HbmCriteria.detonation(p, DetonationTrigger.Kind.WATZ));
        }
    }

    private void dropStructure() {
        for (StructureDrop drop : STRUCTURE_DROPS) {
            Block.popResource(level, worldPosition, new ItemStack(drop.item().get(), drop.count()));
        }
    }

    private void setBrokenColumn(int minHeight, Block block, int x, int z) {
        int height = minHeight + level.getRandom().nextInt(3 - minHeight);

        for (int i = 0; i < 3; i++) {
            BlockPos pos = worldPosition.offset(x, i, z);
            level.setBlock(
                    pos,
                    i <= height
                            ? block.defaultBlockState()
                            : ModBlocks.MUD_BLOCK.get().defaultBlockState(),
                    3);
        }
    }

    @Override
    public boolean hasPermission(Player player) {
        return stillValid(player);
    }

    @Override
    public void receiveControl(CompoundTag data) {
        if (data.contains("lock")) {
            if (this.isLocked) {
                locks.clear();
            } else {
                for (int i = 0; i < inventory.size(); i++) locks.set(i, inventory.get(i).copy());
            }

            this.isLocked = !this.isLocked;
            this.markChanged();
        }
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return ACCESSIBLE_SLOTS;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (ModItems.WATZ_PELLET.typeOf(stack) == null) return false;
        if (!this.isLocked) return true;
        ItemStack lock = locks.get(slot);
        return !lock.isEmpty() && lock.getItem() == stack.getItem();
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return ModItems.WATZ_PELLET.typeOf(stack) == null;
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public FluidPort fluidAccess(BlockPos cell, Direction side) {
        return side == Direction.DOWN && cell.getY() == worldPosition.getY() ? outlet : intake;
    }

    @Override
    public Component getDisplayName() {
        return getName();
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(
            int containerId, Inventory playerInventory, Player player) {
        return new MenuWatz(containerId, playerInventory, this);
    }

    @Override
    public String[] getFunctionInfo() {
        return ROR;
    }

    @Override
    public @Nullable String provideRORValue(String name) {
        if ((PREFIX_VALUE + "heat").equals(name)) return "" + this.heat;
        if ((PREFIX_VALUE + "flux").equals(name))
            return "" + (int) (this.fluxLastBase + this.fluxLastReaction);
        if ((PREFIX_VALUE + "mud").equals(name)) return "" + this.tanks[2].getFill();
        if ((PREFIX_VALUE + "coolant_hot").equals(name)) return "" + this.tanks[1].getFill();
        if ((PREFIX_VALUE + "coolant_cold").equals(name)) return "" + this.tanks[0].getFill();
        return null;
    }

    @Override
    public FluidTankNTM[] copiableTanks() {
        return tanks;
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        pumpAbove = input.getBooleanOr("pumpAbove", false);

        for (int i = 0; i < tanks.length; i++) {
            int slot = i;
            input.child("t" + i).ifPresent(tanks[slot]::deserialize);
        }
        input.child("locks").ifPresent(in -> ContainerHelper.loadAllItems(in, locks));
        this.heat = input.getIntOr("heat", heat);
        this.fluxLastBase = input.getDoubleOr("lastFluxB", fluxLastBase);
        this.fluxLastReaction = input.getDoubleOr("lastFluxR", fluxLastReaction);
        this.isLocked = input.getBooleanOr("isLocked", isLocked);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putBoolean("pumpAbove", pumpAbove);
        for (int i = 0; i < tanks.length; i++) tanks[i].serialize(output.child("t" + i));
        ContainerHelper.saveAllItems(output.child("locks"), locks);
        output.putInt("heat", this.heat);
        output.putDouble("lastFluxB", fluxLastBase);
        output.putDouble("lastFluxR", fluxLastReaction);
        output.putBoolean("isLocked", isLocked);
    }

    public record StructureDrop(Supplier<Item> item, int count) {}

    private boolean pumpAbove;

    public void refreshPumpAbove() {
        pumpAbove =
                level.getBlockState(worldPosition.above(SEGMENT_PITCH))
                        .is(ModBlocks.WATZ_PUMP.get());
    }

    private void writeFlux(ByteBuf output) {
        output.writeDouble(fluxLastReaction + fluxLastBase);
    }

    private void readFlux(ByteBuf input) {
        fluxDisplay = input.readDouble();
    }

    private void writeSharedTanks(ByteBuf output) {
        for (FluidTankNTM tank : sharedTanks) tank.packetSerialize(output);
    }

    private void readSharedTanks(ByteBuf input) {
        for (FluidTankNTM tank : tanks) tank.packetDeserialize(input);
    }

    @Override
    public long syncUnitMask() {
        return 0x1fL;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeInt(this.heat);
            case 1 -> output.writeBoolean(this.isOn);
            case 2 -> output.writeBoolean(this.isLocked);
            case 3 -> writeFlux(output);
            case 4 -> writeSharedTanks(output);
            default -> throw new IllegalArgumentException();
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> this.heat = input.readInt();
            case 1 -> this.isOn = input.readBoolean();
            case 2 -> this.isLocked = input.readBoolean();
            case 3 -> readFlux(input);
            case 4 -> readSharedTanks(input);
            default -> throw new IllegalArgumentException();
        }
    }
}
