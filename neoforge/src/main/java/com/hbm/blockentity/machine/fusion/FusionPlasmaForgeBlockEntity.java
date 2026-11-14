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

package com.hbm.blockentity.machine.fusion;

import com.hbm.api.energymk2.EnergyHandler;
import com.hbm.api.energymk2.ItemEnergyTransfer;
import com.hbm.api.fluidmk2.FluidTankHandler;
import com.hbm.api.redstoneoverradio.RadioValueProvider;
import com.hbm.blockentity.MachineBlockEntity;
import com.hbm.blockentity.MachineSoundProvider;
import com.hbm.blockentity.machine.fusion.FusionItemAccessCache;
import com.hbm.blockentity.machine.fusion.FusionPorts;
import com.hbm.blockentity.machine.fusion.PlasmaReceiver;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.multiblock.MultiblockCoreBlock;
import com.hbm.capability.port.CapabilityPortProvider;
import com.hbm.capability.port.ItemPort;
import com.hbm.inventory.container.PlasmaForgeMenu;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.material.MaterialTagRegistry;
import com.hbm.inventory.recipes.PlasmaForgeRecipe;
import com.hbm.inventory.recipes.PlasmaForgeRecipes;
import com.hbm.inventory.recipes.ingredient.RecipeInputState;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.items.machine.BlueprintsItem;
import com.hbm.modules.machine.ModuleMachinePlasma;
import com.hbm.packet.SyncUnitSchema;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

import static com.hbm.api.redstoneoverradio.RadioCommandParser.VAL;

public class FusionPlasmaForgeBlockEntity extends MachineBlockEntity
        implements MachineSoundProvider,
                PlasmaReceiver,
                EnergyHandler,
                FluidTankHandler,
                MenuProvider,
                CapabilityPortProvider,
                RadioValueProvider,
                SyncUnitSchema {

    public static final String[] ROR =
            new String[] {
                VAL + "progress", VAL + "recipe", VAL + "active", VAL + "booster", VAL + "plasma",
            };
    public static final int SLOT_BATTERY = 0;
    public static final int SLOT_BLUEPRINT = 1;
    public static final int SLOT_BOOSTER = 2;
    public static final int SLOT_INPUT_START = 3;
    public static final int SLOT_OUTPUT = 15;
    public static final int SLOT_COUNT = 16;
    public static final Random RAND = new Random();
    public static final double[][] STRIKER_POSITIONS = {
        {20, -30, -20, 30},
        {45, -80, 15, 30},
        {30, -45, -10, 30},
        {15, -20, -30, 30},
        {0, 10, -55, 30}
    };
    public static final double[][] JET_POSITIONS = {
        {10, 45, -120},
        {20, 45, -140},
        {0, 30, -80},
        {0, 40, -100},
        {30, 50, -160}
    };
    private static final int[] INPUT_SLOTS = {3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14};
    private static final int[] OUTPUT_SLOTS = {SLOT_OUTPUT};
    private static final int[] ACCESSIBLE_SLOTS = {2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15};
    private static final List<Booster> BOOSTERS = new ArrayList<>();
    public static Consumer<FusionPlasmaForgeBlockEntity> PLAY_STRIKER = be -> {};
    public final FluidTankNTM inputTank;
    public final ForgeArm armStriker = new ForgeArm(ForgeArmType.STRIKER);
    public final ForgeArm armJet = new ForgeArm(ForgeArmType.JET);
    public final ModuleMachinePlasma module;
    private final FluidTankNTM[] receiving;
    private final FusionItemAccessCache itemPorts =
            new FusionItemAccessCache(
                    new ItemPort(
                            ACCESSIBLE_SLOTS,
                            this::canPlaceItem,
                            (slot, stack) -> canTakeItemThroughFace(slot, stack, null)));
    public long power;
    public long maxPower;

    {
        hbm$syncSet$FusionPlasmaForgeBlockEntity$maxPower(10_000_000);
    }

    public boolean didProcess;
    public float plasmaRed;
    public float plasmaGreen;
    public float plasmaBlue;
    public long plasmaEnergy;
    public long plasmaEnergySync;
    public double neutronEnergy;
    public boolean connected;
    public int booster;
    public int maxBooster;
    public double prevRing;
    public double ring;
    public double ringSpeed;
    public double ringTarget;
    public int ringDelay;

    public FusionPlasmaForgeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FUSION_PLASMA_FORGE.get(), pos, state, SLOT_COUNT);
        inputTank = new FluidTankNTM(16_000);
        module =
                new ModuleMachinePlasma(
                        0,
                        this,
                        PlasmaForgeRecipes.INSTANCE,
                        this,
                        INPUT_SLOTS,
                        OUTPUT_SLOTS,
                        new FluidTankNTM[] {inputTank},
                        new FluidTankNTM[0]);
        receiving = new FluidTankNTM[] {inputTank};
    }

    public static List<FusionPorts.Port> links(BlockPos core, Direction facing) {
        Direction rot = facing.getClockWise();
        return List.of(
                new FusionPorts.Port(
                        FusionPorts.Kind.PLASMA,
                        core.offset(rot.getStepX() * 5, 2, rot.getStepZ() * 5),
                        rot),
                new FusionPorts.Port(
                        FusionPorts.Kind.PLASMA,
                        core.offset(-rot.getStepX() * 5, 2, -rot.getStepZ() * 5),
                        rot.getOpposite()));
    }

    public static List<Booster> boosters() {
        return BOOSTERS;
    }

    public static void registerBoosters() {
        BOOSTERS.clear();

        booster(MaterialTagRegistry.CO60.nugget(), 20);
        booster(MaterialTagRegistry.CO60.billet(), 120);
        booster(MaterialTagRegistry.CO60.ingot(), 200);
        booster(MaterialTagRegistry.CO60.dust(), 200);
        booster(MaterialTagRegistry.SR90.nugget(), 40);
        booster(MaterialTagRegistry.SR90.dustTiny(), 40);
        booster(MaterialTagRegistry.SR90.billet(), 240);
        booster(MaterialTagRegistry.SR90.ingot(), 400);
        booster(MaterialTagRegistry.SR90.dust(), 400);
        booster(MaterialTagRegistry.AU198.nugget(), 60);
        booster(MaterialTagRegistry.AU198.billet(), 360);
        booster(MaterialTagRegistry.AU198.ingot(), 600);
        booster(MaterialTagRegistry.AU198.dust(), 600);
        booster(MaterialTagRegistry.I131.dustTiny(), 60);
        booster(MaterialTagRegistry.I131.dust(), 600);
        booster(MaterialTagRegistry.XE135.dustTiny(), 60);
        booster(MaterialTagRegistry.XE135.dust(), 600);
        booster(MaterialTagRegistry.CS137.dustTiny(), 50);
        booster(MaterialTagRegistry.CS137.dust(), 500);
        booster(MaterialTagRegistry.AT209.dust(), 1_200);
    }

    private static void booster(TagKey<Item> tag, int duration) {
        BuiltInRegistries.ITEM
                .get(tag)
                .ifPresent(holders -> BOOSTERS.add(new Booster(Ingredient.of(holders), duration)));
    }

    private static void strikerStateMachine(ForgeArm arm) {
        switch (arm.state) {
            case REPOSITION -> {
                if (arm.move()) {
                    arm.actionDelay = 5;
                    arm.state = ForgeArmState.EXTEND1;
                    arm.targetAngles[4] = 0.5D;
                }
            }
            case EXTEND1 -> {
                if (arm.move()) {
                    arm.actionDelay = 0;
                    arm.state = ForgeArmState.RETRACT1;
                    arm.targetAngles[4] = 0D;
                    arm.playStrikerSound();
                }
            }
            case RETRACT1 -> {
                if (arm.move()) {
                    arm.actionDelay = 0;
                    arm.state = ForgeArmState.EXTEND2;
                    arm.targetAngles[5] = 0.5D;
                }
            }
            case EXTEND2 -> {
                if (arm.move()) {
                    arm.actionDelay = 0;
                    arm.state = ForgeArmState.RETRACT2;
                    arm.targetAngles[5] = 0D;
                    arm.playStrikerSound();
                }
            }
            case RETRACT2 -> {
                if (arm.move()) {
                    if (RAND.nextInt(3) == 0) {
                        arm.actionDelay = 10;
                        arm.state = ForgeArmState.REPOSITION;
                        choosePosition(arm, STRIKER_POSITIONS);
                    } else {
                        arm.actionDelay = 5;
                        arm.state = ForgeArmState.EXTEND1;
                        arm.targetAngles[4] = 0.5D;
                    }
                }
            }
            case RETIRE -> {
                Arrays.fill(arm.targetAngles, 0);
                if (arm.move()) {
                    arm.actionDelay = 10;
                    arm.state = ForgeArmState.REPOSITION;
                    choosePosition(arm, STRIKER_POSITIONS);
                }
            }
        }
    }

    private static void jetStateMachine(ForgeArm arm) {
        switch (arm.state) {
            case REPOSITION -> {
                if (arm.move()) {
                    arm.actionDelay = 20 + RAND.nextInt(3) * 10;
                    arm.state = ForgeArmState.REPOSITION;
                    choosePosition(arm, JET_POSITIONS);
                }
            }
            case RETIRE -> {
                Arrays.fill(arm.targetAngles, 0);
                if (arm.move()) {
                    arm.actionDelay = 10;
                    arm.state = ForgeArmState.REPOSITION;
                    choosePosition(arm, JET_POSITIONS);
                }
            }
            default -> {}
        }
    }

    private static void choosePosition(ForgeArm arm, double[][] positions) {
        double[] newPos = positions[RAND.nextInt(positions.length)];
        System.arraycopy(newPos, 0, arm.targetAngles, 0, newPos.length);
    }

    private List<FusionPorts.Port> links() {
        Direction facing = MultiblockCoreBlock.coreFacing(getBlockState());
        return links(worldPosition, facing);
    }

    @Override
    public boolean isPlasmaConsumer() {
        return true;
    }

    @Override
    public void receivePlasma(long fusionPower, double neutronPower, float r, float g, float b) {
        plasmaEnergy = fusionPower;
        neutronEnergy = neutronPower;
        hbm$syncSet$FusionPlasmaForgeBlockEntity$plasmaRed(r);
        hbm$syncSet$FusionPlasmaForgeBlockEntity$plasmaGreen(g);
        hbm$syncSet$FusionPlasmaForgeBlockEntity$plasmaBlue(b);
    }

    @Override
    public void tickServer() {
        ServerLevel serverLevel = (ServerLevel) level;

        if (maxPower <= 0) hbm$syncSet$FusionPlasmaForgeBlockEntity$maxPower(1_000_000);

        hbm$syncSet$FusionPlasmaForgeBlockEntity$plasmaEnergySync(plasmaEnergy);
        plasmaEnergy = 0;

        if (booster <= 0 && !inventory.get(SLOT_BOOSTER).isEmpty()) {
            for (Booster candidate : boosters()) {
                if (candidate.ingredient().test(inventory.get(SLOT_BOOSTER))
                        && RecipeInputState.accepts(
                                candidate.ingredient(), inventory.get(SLOT_BOOSTER))) {
                    hbm$syncSet$FusionPlasmaForgeBlockEntity$booster(candidate.duration());
                    hbm$syncSet$FusionPlasmaForgeBlockEntity$maxBooster(booster);
                    removeItem(SLOT_BOOSTER, 1);
                    break;
                }
            }
        }

        List<FusionPorts.Port> links = links();

        GenericRecipe selected = module.getRecipe();
        if (selected != null)
            hbm$syncSet$FusionPlasmaForgeBlockEntity$maxPower(selected.power * 100);
        hbm$syncSet$FusionPlasmaForgeBlockEntity$maxPower(
                Math.max(power, Math.max(maxPower, 100_000L)));
        hbm$syncSet$FusionPlasmaForgeBlockEntity$power(
                (long)
                        (power
                                + (ItemEnergyTransfer.extract(
                                        this, SLOT_BATTERY, maxPower - power, false))));

        double speed = booster > 0 ? 4D : 1D;
        boolean ignition =
                !(selected instanceof PlasmaForgeRecipe forge)
                        || forge.ignitionTemp <= plasmaEnergySync;

        double prevProgress = module.progress;
        module.update(speed, 1D, ignition, inventory.get(SLOT_BLUEPRINT));
        hbm$syncSet$FusionPlasmaForgeBlockEntity$didProcess(module.didProcess);
        if (module.markDirty) setChanged();
        if (didProcess || module.progress != prevProgress) markChanged();

        long powerReceived = (long) Math.ceil(plasmaEnergySync * 0.75);

        FusionPorts.Port out = links.get(1);
        hbm$syncSet$FusionPlasmaForgeBlockEntity$connected(
                FusionPorts.peer(serverLevel, out, PlasmaReceiver.class) != null);

        if (powerReceived > 0) {
            PlasmaReceiver receiver = FusionPorts.peer(serverLevel, out, PlasmaReceiver.class);
            if (receiver != null) {
                receiver.receivePlasma(
                        powerReceived, neutronEnergy, plasmaRed, plasmaGreen, plasmaBlue);
            }
        }

        if (didProcess && booster > 0)
            hbm$syncSet$FusionPlasmaForgeBlockEntity$booster((int) (booster - 1));
        neutronEnergy = 0D;

        networkPackNT(100);
    }

    @Override
    public void tickClient() {
        armStriker.updateArm();
        armJet.updateArm();

        prevRing = ring;

        if (!didProcess) return;

        if (ring != ringTarget) {
            double ringDelta = Math.abs(ringTarget - ring);
            if (ringDelta <= ringSpeed) ring = ringTarget;
            if (ringTarget > ring) ring += ringSpeed;
            if (ringTarget < ring) ring -= ringSpeed;
            if (ringTarget == ring) {
                double sub = ringTarget >= 360 ? -360D : 360D;
                ringTarget += sub;
                ring += sub;
                prevRing += sub;
                ringDelay = 100 + level.getRandom().nextInt(41);
            }
        } else {
            if (ringDelay > 0) ringDelay--;
            if (ringDelay <= 0) {
                ringTarget +=
                        (level.getRandom().nextDouble() + 1)
                                * 60
                                * (level.getRandom().nextBoolean() ? -1 : 1);
                ringSpeed = 2.5D;
            }
        }
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot == SLOT_BATTERY) return true;
        if (slot == SLOT_BLUEPRINT) return stack.getItem() instanceof BlueprintsItem;
        if (module.isItemValid(slot, stack)) return true;
        if (slot == SLOT_BOOSTER) {
            for (Booster candidate : boosters()) {
                if (candidate.ingredient().test(stack)
                        && RecipeInputState.accepts(candidate.ingredient(), stack)) return true;
            }
        }
        return false;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot == SLOT_OUTPUT || module.isSlotClogged(slot);
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
    public void setPower(long power) {
        this.hbm$syncSet$FusionPlasmaForgeBlockEntity$power(power);
    }

    @Override
    public long getMaxPower() {
        return maxPower;
    }

    @Override
    public int tankCount() {
        return receiving.length;
    }

    @Override
    public FluidTankNTM tank(int index) {
        return receiving[index];
    }

    @Override
    public FluidTankNTM[] getReceivingTanks() {
        return receiving;
    }

    public void setSelectedRecipe(String key) {
        module.setRecipe(key);
        setChanged();
    }

    @Override
    public String[] getFunctionInfo() {
        return ROR;
    }

    @Override
    public @Nullable String getRadioValue(String name) {
        if ((VAL + "progress").equals(name)) return "" + (int) Math.round(module.progress * 100);
        if ((VAL + "recipe").equals(name)) return module.legacyRecipeName();
        if ((VAL + "active").equals(name)) return "" + (didProcess ? 1 : 0);
        if ((VAL + "booster").equals(name)) return "" + booster;
        if ((VAL + "plasma").equals(name)) return "" + plasmaEnergy;
        return null;
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.child("i").ifPresent(inputTank::deserialize);
        input.getLong("power").ifPresent(v -> hbm$syncSet$FusionPlasmaForgeBlockEntity$power(v));
        input.getLong("maxPower")
                .ifPresent(v -> hbm$syncSet$FusionPlasmaForgeBlockEntity$maxPower(v));
        input.getInt("booster").ifPresent(v -> hbm$syncSet$FusionPlasmaForgeBlockEntity$booster(v));
        input.getInt("maxBooster")
                .ifPresent(v -> hbm$syncSet$FusionPlasmaForgeBlockEntity$maxBooster(v));
        module.load(input);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        inputTank.serialize(output.child("i"));
        output.putLong("power", power);
        output.putLong("maxPower", maxPower);
        output.putInt("booster", booster);
        output.putInt("maxBooster", maxBooster);
        module.save(output);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.machinePlasmaForge");
    }

    @Override
    public Component getDisplayName() {
        return getName();
    }

    @Override
    public AbstractContainerMenu createMenu(
            int containerId, Inventory playerInventory, Player player) {
        return new PlasmaForgeMenu(containerId, playerInventory, this);
    }

    @Override
    public @Nullable ItemPort itemAccess(BlockPos cell, Direction side) {
        return itemPorts.get(this, cell);
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
            case 0 -> inputTank.packetSerialize(output);
            case 1 -> {
                output.writeFloat(plasmaRed);
                output.writeFloat(plasmaGreen);
                output.writeFloat(plasmaBlue);
            }
            case 2 -> output.writeLong(plasmaEnergySync);
            case 3 -> output.writeLong(power);
            case 4 -> output.writeLong(maxPower);
            case 5 -> output.writeBoolean(didProcess);
            case 6 -> output.writeBoolean(connected);
            case 7 -> output.writeInt(booster);
            case 8 -> output.writeInt(maxBooster);
            case 9 -> module.serialize(output);
            default -> super.writeSyncUnit(unit, output);
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> inputTank.packetDeserialize(input);
            case 1 -> {
                hbm$syncSet$FusionPlasmaForgeBlockEntity$plasmaRed(input.readFloat());
                hbm$syncSet$FusionPlasmaForgeBlockEntity$plasmaGreen(input.readFloat());
                hbm$syncSet$FusionPlasmaForgeBlockEntity$plasmaBlue(input.readFloat());
            }
            case 2 -> hbm$syncSet$FusionPlasmaForgeBlockEntity$plasmaEnergySync(input.readLong());
            case 3 -> hbm$syncSet$FusionPlasmaForgeBlockEntity$power(input.readLong());
            case 4 -> hbm$syncSet$FusionPlasmaForgeBlockEntity$maxPower(input.readLong());
            case 5 -> hbm$syncSet$FusionPlasmaForgeBlockEntity$didProcess(input.readBoolean());
            case 6 -> hbm$syncSet$FusionPlasmaForgeBlockEntity$connected(input.readBoolean());
            case 7 -> hbm$syncSet$FusionPlasmaForgeBlockEntity$booster(input.readInt());
            case 8 -> hbm$syncSet$FusionPlasmaForgeBlockEntity$maxBooster(input.readInt());
            case 9 -> module.deserialize(input);
            default -> super.readSyncUnit(unit, input);
        }
    }

    public enum ForgeArmType {
        STRIKER(FusionPlasmaForgeBlockEntity::strikerStateMachine, 6),
        JET(FusionPlasmaForgeBlockEntity::jetStateMachine, 4);

        final int angleCount;
        final Consumer<ForgeArm> stateMachine;

        ForgeArmType(Consumer<ForgeArm> stateMachine, int angleCount) {
            this.stateMachine = stateMachine;
            this.angleCount = angleCount;
        }
    }

    public enum ForgeArmState {
        REPOSITION,
        EXTEND1,
        EXTEND2,
        RETRACT1,
        RETRACT2,
        RETIRE
    }

    public record Booster(Ingredient ingredient, int duration) {}

    public class ForgeArm {

        public final ForgeArmType type;
        public final double[] angles;
        public final double[] prevAngles;
        public final double[] targetAngles;
        public final double[] speed;
        public ForgeArmState state = ForgeArmState.RETIRE;
        public int actionDelay = 0;

        public ForgeArm(ForgeArmType type) {
            this.type = type;
            angles = new double[type.angleCount];
            prevAngles = new double[type.angleCount];
            targetAngles = new double[type.angleCount];
            speed = new double[type.angleCount];

            for (int i = 0; i < speed.length; i++) {
                if (i < 3 || i == 4) speed[i] = 15;
                if (i == 3) speed[i] = 15;
                if (i > 4) speed[i] = 0.5;
            }
        }

        public void updateArm() {
            System.arraycopy(angles, 0, prevAngles, 0, angles.length);

            if (!didProcess) state = ForgeArmState.RETIRE;
            if (state == ForgeArmState.RETIRE) actionDelay = 0;

            if (actionDelay > 0) {
                actionDelay--;
                return;
            }

            type.stateMachine.accept(this);
        }

        public boolean move() {
            boolean didMove = false;

            for (int i = 0; i < angles.length; i++) {
                if (angles[i] == targetAngles[i]) continue;
                didMove = true;

                double angle = angles[i];
                double target = targetAngles[i];
                double turn = speed[i];

                if (Math.abs(angle - target) <= turn) {
                    angles[i] = targetAngles[i];
                    continue;
                }
                if (angle < target) angles[i] += turn;
                else angles[i] -= turn;
            }

            return !didMove;
        }

        public void playStrikerSound() {
            PLAY_STRIKER.accept(FusionPlasmaForgeBlockEntity.this);
        }

        public void getPositions(float interp, double[] out) {
            for (int i = 0; i < angles.length; i++) {
                out[i] = prevAngles[i] + (angles[i] - prevAngles[i]) * interp;
            }
        }
    }

    public final void hbm$syncSet$FusionPlasmaForgeBlockEntity$power(long next) {
        if (this.power != next) {
            this.power = next;
            if ((1L << 3) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 3);
        }
    }

    public final void hbm$syncSet$FusionPlasmaForgeBlockEntity$maxPower(long next) {
        if (this.maxPower != next) {
            this.maxPower = next;
            if ((1L << 4) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 4);
        }
    }

    public final void hbm$syncSet$FusionPlasmaForgeBlockEntity$didProcess(boolean next) {
        if (this.didProcess != next) {
            this.didProcess = next;
            if ((1L << 5) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 5);
        }
    }

    public final void hbm$syncSet$FusionPlasmaForgeBlockEntity$plasmaRed(float next) {
        if (Float.floatToRawIntBits(this.plasmaRed) != Float.floatToRawIntBits(next)) {
            this.plasmaRed = next;
            if ((1L << 1) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 1);
        }
    }

    public final void hbm$syncSet$FusionPlasmaForgeBlockEntity$plasmaGreen(float next) {
        if (Float.floatToRawIntBits(this.plasmaGreen) != Float.floatToRawIntBits(next)) {
            this.plasmaGreen = next;
            if ((1L << 1) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 1);
        }
    }

    public final void hbm$syncSet$FusionPlasmaForgeBlockEntity$plasmaBlue(float next) {
        if (Float.floatToRawIntBits(this.plasmaBlue) != Float.floatToRawIntBits(next)) {
            this.plasmaBlue = next;
            if ((1L << 1) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 1);
        }
    }

    public final void hbm$syncSet$FusionPlasmaForgeBlockEntity$plasmaEnergySync(long next) {
        if (this.plasmaEnergySync != next) {
            this.plasmaEnergySync = next;
            if ((1L << 2) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 2);
        }
    }

    public final void hbm$syncSet$FusionPlasmaForgeBlockEntity$connected(boolean next) {
        if (this.connected != next) {
            this.connected = next;
            if ((1L << 6) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 6);
        }
    }

    public final void hbm$syncSet$FusionPlasmaForgeBlockEntity$booster(int next) {
        if (this.booster != next) {
            this.booster = next;
            if ((1L << 7) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 7);
        }
    }

    public final void hbm$syncSet$FusionPlasmaForgeBlockEntity$maxBooster(int next) {
        if (this.maxBooster != next) {
            this.maxBooster = next;
            if ((1L << 8) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 8);
        }
    }

    static {
        com.hbm.packet.SyncBindings.field(
                FusionPlasmaForgeBlockEntity.class, "inputTank", 3, 1L << 0);
        com.hbm.packet.SyncBindings.field(FusionPlasmaForgeBlockEntity.class, "module", 3, 1L << 9);
    }
}
