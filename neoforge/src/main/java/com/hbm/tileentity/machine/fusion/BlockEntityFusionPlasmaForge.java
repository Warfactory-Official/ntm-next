// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: HbmMods (The Bobcat)
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine.fusion;

import com.hbm.api.control.IControlReceiver;
import com.hbm.api.energymk2.IEnergyHandlerMK2;
import com.hbm.api.energymk2.ItemEnergyTransfer;
import com.hbm.api.fluidmk2.FluidTankEndpoint;
import com.hbm.api.redstoneoverradio.IRORValueProvider;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.capability.port.IPortHost;
import com.hbm.capability.port.ItemPort;
import com.hbm.inventory.OreDictManager;
import com.hbm.inventory.container.MenuMachinePlasmaForge;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.recipes.PlasmaForgeRecipe;
import com.hbm.inventory.recipes.PlasmaForgeRecipes;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.items.machine.ItemBlueprints;
import com.hbm.modules.machine.ModuleMachinePlasma;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.tileentity.Audible;
import com.hbm.tileentity.BlockEntityMachineBase;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
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

public class BlockEntityFusionPlasmaForge extends BlockEntityMachineBase
        implements Audible,
                IFusionPowerReceiver,
                IEnergyHandlerMK2,
                FluidTankEndpoint,
                IControlReceiver,
                MenuProvider,
                IPortHost,
                IRORValueProvider,
                SyncUnitSchema {

    public static final String[] ROR =
            new String[] {
                PREFIX_VALUE + "progress",
                PREFIX_VALUE + "recipe",
                PREFIX_VALUE + "active",
                PREFIX_VALUE + "booster",
                PREFIX_VALUE + "plasma",
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

    private static final ItemPort OUTPUT_PORT = ItemPort.extractOnly(SLOT_OUTPUT);
    private static final List<Booster> BOOSTERS = new ArrayList<>();

    public static Consumer<BlockEntityFusionPlasmaForge> PLAY_STRIKER = be -> {};

    @SyncField(units = 1L << 0)
    public final FluidTankNTM inputTank;

    private final FluidTankNTM[] receiving;
    public final ForgeArm armStriker = new ForgeArm(ForgeArmType.STRIKER);
    public final ForgeArm armJet = new ForgeArm(ForgeArmType.JET);
    private final FusionItemAccessCache itemPorts = new FusionItemAccessCache(OUTPUT_PORT);

    @SyncField(units = 1L << 9)
    public final ModuleMachinePlasma module;

    @SyncField(units = 1L << 3)
    public long power;

    @SyncField(units = 1L << 4)
    public long maxPower = 10_000_000;

    @SyncField(units = 1L << 5)
    public boolean didProcess;

    @SyncField(units = 1L << 1)
    public float plasmaRed;

    @SyncField(units = 1L << 1)
    public float plasmaGreen;

    @SyncField(units = 1L << 1)
    public float plasmaBlue;

    public long plasmaEnergy;

    @SyncField(units = 1L << 2)
    public long plasmaEnergySync;

    public double neutronEnergy;

    @SyncField(units = 1L << 6)
    public boolean connected;

    @SyncField(units = 1L << 7)
    public int booster;

    @SyncField(units = 1L << 8)
    public int maxBooster;

    public double prevRing;
    public double ring;
    public double ringSpeed;
    public double ringTarget;
    public int ringDelay;

    public BlockEntityFusionPlasmaForge(BlockPos pos, BlockState state) {
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

        booster(OreDictManager.CO60.nugget(), 20);
        booster(OreDictManager.CO60.billet(), 120);
        booster(OreDictManager.CO60.ingot(), 200);
        booster(OreDictManager.CO60.dust(), 200);
        booster(OreDictManager.SR90.nugget(), 40);
        booster(OreDictManager.SR90.dustTiny(), 40);
        booster(OreDictManager.SR90.billet(), 240);
        booster(OreDictManager.SR90.ingot(), 400);
        booster(OreDictManager.SR90.dust(), 400);
        booster(OreDictManager.AU198.nugget(), 60);
        booster(OreDictManager.AU198.billet(), 360);
        booster(OreDictManager.AU198.ingot(), 600);
        booster(OreDictManager.AU198.dust(), 600);
        booster(OreDictManager.I131.dustTiny(), 60);
        booster(OreDictManager.I131.dust(), 600);
        booster(OreDictManager.XE135.dustTiny(), 60);
        booster(OreDictManager.XE135.dust(), 600);
        booster(OreDictManager.CS137.dustTiny(), 50);
        booster(OreDictManager.CS137.dust(), 500);
        booster(OreDictManager.AT209.dust(), 1_200);
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
        Direction facing = BlockMultiblockCore.coreFacing(getBlockState());
        return links(worldPosition, facing);
    }

    @Override
    public boolean receivesFusionPower() {
        return true;
    }

    @Override
    public void receiveFusionPower(
            long fusionPower, double neutronPower, float r, float g, float b) {
        plasmaEnergy = fusionPower;
        neutronEnergy = neutronPower;
        plasmaRed = r;
        plasmaGreen = g;
        plasmaBlue = b;
    }

    @Override
    public void tickServer() {
        ServerLevel serverLevel = (ServerLevel) level;

        if (maxPower <= 0) maxPower = 1_000_000;

        plasmaEnergySync = plasmaEnergy;
        plasmaEnergy = 0;

        if (booster <= 0 && !inventory.get(SLOT_BOOSTER).isEmpty()) {
            for (Booster candidate : boosters()) {
                if (candidate.ingredient().test(inventory.get(SLOT_BOOSTER))) {
                    maxBooster = booster = candidate.duration();
                    removeItem(SLOT_BOOSTER, 1);
                    break;
                }
            }
        }

        List<FusionPorts.Port> links = links();

        GenericRecipe selected = module.getRecipe();
        if (selected != null) maxPower = selected.power * 100;
        maxPower = Math.max(power, Math.max(maxPower, 100_000L));
        power += ItemEnergyTransfer.extract(this, SLOT_BATTERY, maxPower - power, false);

        double speed = booster > 0 ? 4D : 1D;
        boolean ignition =
                !(selected instanceof PlasmaForgeRecipe forge)
                        || forge.ignitionTemp <= plasmaEnergySync;

        module.update(speed, 1D, ignition, inventory.get(SLOT_BLUEPRINT));
        didProcess = module.didProcess;
        if (module.markDirty) setChanged();

        long powerReceived = (long) Math.ceil(plasmaEnergySync * 0.75);

        FusionPorts.Port out = links.get(1);
        connected = FusionPorts.peer(serverLevel, out, IFusionPowerReceiver.class) != null;

        if (powerReceived > 0) {

            IFusionPowerReceiver receiver =
                    FusionPorts.peer(serverLevel, out, IFusionPowerReceiver.class);
            if (receiver != null) {
                receiver.receiveFusionPower(
                        powerReceived, neutronEnergy, plasmaRed, plasmaGreen, plasmaBlue);
            }
        }

        if (didProcess && booster > 0) booster--;
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
        if (slot == SLOT_BLUEPRINT) return stack.getItem() instanceof ItemBlueprints;
        if (module.isItemValid(slot, stack)) return true;
        if (slot == SLOT_BOOSTER) {
            for (Booster candidate : boosters())
                if (candidate.ingredient().test(stack)) return true;
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
        this.power = power;
    }

    @Override
    public long getMaxPower() {
        return maxPower;
    }

    @Override
    public FluidTankNTM[] getReceivingTanks() {
        return receiving;
    }

    @Override
    public boolean hasPermission(Player player) {
        return stillValid(player);
    }

    @Override
    public void receiveControl(CompoundTag data) {
        if (data.contains("index") && data.contains("selection")) {
            if (data.getIntOr("index", 0) != 0) return;
            String key = data.getStringOr("selection", "");

            module.setRecipe(key);
            setChanged();
        }
    }

    @Override
    public String[] getFunctionInfo() {
        return ROR;
    }

    @Override
    public @Nullable String provideRORValue(String name) {
        if ((PREFIX_VALUE + "progress").equals(name))
            return "" + (int) Math.round(module.progress * 100);
        if ((PREFIX_VALUE + "recipe").equals(name)) return module.legacyRecipeName();
        if ((PREFIX_VALUE + "active").equals(name)) return "" + (didProcess ? 1 : 0);
        if ((PREFIX_VALUE + "booster").equals(name)) return "" + booster;
        if ((PREFIX_VALUE + "plasma").equals(name)) return "" + plasmaEnergy;
        return null;
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.child("i").ifPresent(inputTank::deserialize);
        input.getLong("power").ifPresent(v -> power = v);
        input.getLong("maxPower").ifPresent(v -> maxPower = v);
        input.getInt("booster").ifPresent(v -> booster = v);
        input.getInt("maxBooster").ifPresent(v -> maxBooster = v);
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
        return new MenuMachinePlasmaForge(containerId, playerInventory, this);
    }

    @Override
    public @Nullable ItemPort itemAccess(BlockPos cell, Direction side) {
        return itemPorts.get(this, cell);
    }

    public enum ForgeArmType {
        STRIKER(BlockEntityFusionPlasmaForge::strikerStateMachine, 6),

        JET(BlockEntityFusionPlasmaForge::jetStateMachine, 4);

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
            this.angles = new double[type.angleCount];
            this.prevAngles = new double[type.angleCount];
            this.targetAngles = new double[type.angleCount];
            this.speed = new double[type.angleCount];

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
            PLAY_STRIKER.accept(BlockEntityFusionPlasmaForge.this);
        }

        public void getPositions(float interp, double[] out) {
            for (int i = 0; i < angles.length; i++) {
                out[i] = prevAngles[i] + (angles[i] - prevAngles[i]) * interp;
            }
        }
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
            case 0 -> this.inputTank.packetSerialize(output);
            case 1 -> {
                output.writeFloat(this.plasmaRed);
                output.writeFloat(this.plasmaGreen);
                output.writeFloat(this.plasmaBlue);
            }
            case 2 -> output.writeLong(this.plasmaEnergySync);
            case 3 -> output.writeLong(this.power);
            case 4 -> output.writeLong(this.maxPower);
            case 5 -> output.writeBoolean(this.didProcess);
            case 6 -> output.writeBoolean(this.connected);
            case 7 -> output.writeInt(this.booster);
            case 8 -> output.writeInt(this.maxBooster);
            case 9 -> this.module.serialize(output);
            default -> super.writeSyncUnit(unit, output);
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> this.inputTank.packetDeserialize(input);
            case 1 -> {
                this.plasmaRed = input.readFloat();
                this.plasmaGreen = input.readFloat();
                this.plasmaBlue = input.readFloat();
            }
            case 2 -> this.plasmaEnergySync = input.readLong();
            case 3 -> this.power = input.readLong();
            case 4 -> this.maxPower = input.readLong();
            case 5 -> this.didProcess = input.readBoolean();
            case 6 -> this.connected = input.readBoolean();
            case 7 -> this.booster = input.readInt();
            case 8 -> this.maxBooster = input.readInt();
            case 9 -> this.module.deserialize(input);
            default -> super.readSyncUnit(unit, input);
        }
    }
}
