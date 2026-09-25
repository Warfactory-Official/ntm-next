// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine.oil;

import com.google.common.base.Suppliers;
import com.hbm.api.energymk2.IBatteryItem;
import com.hbm.api.energymk2.IEnergyHandlerMK2;
import com.hbm.api.energymk2.ItemEnergyTransfer;
import com.hbm.api.fluidmk2.FluidFlushOutputs;
import com.hbm.api.fluidmk2.FluidTankEndpoint;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.machine.MachineRefinery;
import com.hbm.handler.pollution.PollutionHandler;
import com.hbm.handler.pollution.PollutionType;
import com.hbm.handler.threading.TargetPoint;
import com.hbm.interfaces.IOverpressurable;
import com.hbm.interfaces.IRepairable;
import com.hbm.inventory.OreDictManager;
import com.hbm.inventory.container.ContainerSync;
import com.hbm.inventory.container.MenuMachineRefinery;
import com.hbm.inventory.fluid.FluidStackNTM;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.recipes.RefineryRecipe;
import com.hbm.inventory.recipes.RefineryRecipes;
import com.hbm.inventory.recipes.ingredient.CountIngredient;
import com.hbm.items.ModDataComponents;
import com.hbm.items.ModItems;
import com.hbm.items.machine.FluidIdentifierItem;
import com.hbm.items.machine.IFluidContainerItem;
import com.hbm.items.machine.ItemFluidContainerInfinite;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.packet.toclient.EffectNTPayload;
import com.hbm.particle.HbmEffectNT;
import com.hbm.platform.Services;
import com.hbm.sound.AudioSystem;
import com.hbm.sound.AudioWrapper;
import com.hbm.sound.ModSounds;
import com.hbm.tileentity.AudioLoop;
import com.hbm.tileentity.BlockEntityMachineBase;
import com.hbm.tileentity.IFluidCopiable;
import com.hbm.tileentity.PersistentDrop;
import com.hbm.tileentity.Tiltable;
import com.hbm.util.TickPhase;
import io.netty.buffer.ByteBuf;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

public class BlockEntityMachineRefinery extends BlockEntityMachineBase
        implements AudioLoop,
                Tiltable,
                IEnergyHandlerMK2,
                FluidTankEndpoint,
                IOverpressurable,
                IRepairable,
                MenuProvider,
                PersistentDrop,
                IFluidCopiable,
                SyncUnitSchema {

    public static final int SLOT_BATTERY = 0;

    public static final int SLOT_SOLID_OUT = 11;
    public static final int SLOT_FLUID_ID = 12;
    public static final int SLOT_COUNT = 13;

    public static final long MAX_POWER = 1_000L;
    public static final int TANK_CAPACITY_INPUT = 64_000;
    public static final int TANK_CAPACITY_OUTPUT = 24_000;

    public static final int MAX_SULFUR = 10;

    private static final int[] ACCESSIBLE_SLOTS = {SLOT_SOLID_OUT};

    private static final Supplier<List<CountIngredient>> REPAIR_MATERIALS =
            Suppliers.memoize(
                    () ->
                            List.of(
                                    CountIngredient.of(OreDictManager.STEEL.plate(), 8),
                                    CountIngredient.of(ModItems.DUCTTAPE.get(), 4)));

    public static Consumer<BlockEntityMachineRefinery> CLIENT_SOUND = be -> {};

    @SyncField(units = 1L << 1)
    public final FluidTankNTM[] tanks = new FluidTankNTM[5];

    private final FluidTankNTM[] receiving;
    private final FluidTankNTM[] sending;

    @SyncField(units = 1L << 0)
    @ContainerSync
    public long power;

    public int sulfur;

    @SyncField(units = 1L << 4)
    public boolean isOn;

    private static final String[] PERSISTENT_KEYS = {
        "onFire", "tank0", "tank1", "tank2", "tank3", "tank4"
    };

    @SyncField(units = 1L << 3)
    public boolean onFire;

    public @Nullable Explosion lastExplosion;

    public int audioTime;

    private final FluidFlushOutputs flush = new FluidFlushOutputs();

    public BlockEntityMachineRefinery(BlockPos pos, BlockState state) {
        super(ModBlockEntities.REFINERY.get(), pos, state, SLOT_COUNT);
        tanks[0] = new FluidTankNTM(NTMFluids.HOTOIL, TANK_CAPACITY_INPUT);
        tanks[1] = new FluidTankNTM(NTMFluids.HEAVYOIL, TANK_CAPACITY_OUTPUT);
        tanks[2] = new FluidTankNTM(NTMFluids.NAPHTHA, TANK_CAPACITY_OUTPUT);
        tanks[3] = new FluidTankNTM(NTMFluids.LIGHTOIL, TANK_CAPACITY_OUTPUT);
        tanks[4] = new FluidTankNTM(NTMFluids.PETROLEUM, TANK_CAPACITY_OUTPUT);
        receiving = new FluidTankNTM[] {tanks[0]};
        sending = new FluidTankNTM[] {tanks[1], tanks[2], tanks[3], tanks[4]};
    }

    private static boolean isFluidContainer(ItemStack stack) {
        return FluidTankNTM.isFluidContainer(stack);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.machineRefinery");
    }

    @Override
    public void tickClient() {
        CLIENT_SOUND.accept(this);
    }

    @Override
    public int getFloorCount() {
        return 2 * 2;
    }

    @Override
    public BlockPos getFloorPosFromIndex(int index) {
        return standardFloor3x3(index);
    }

    @Override
    public AudioWrapper createAudioLoop() {

        return AudioSystem.getLoopedSound(
                ModSounds.REFINERY_LOOP.get(),
                SoundSource.BLOCKS,
                worldPosition.getX(),
                worldPosition.getY(),
                worldPosition.getZ(),
                0.25F,
                15F,
                1.0F,
                20);
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
    public FluidTankNTM[] getSendingTanks() {
        return sending;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return switch (slot) {
            case SLOT_BATTERY -> IBatteryItem.isBattery(stack);
            case 1, 3, 5, 7, 9 -> isFluidContainer(stack);
            case SLOT_FLUID_ID -> stack.getItem() instanceof FluidIdentifierItem;
            default -> false;
        };
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return ACCESSIBLE_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return false;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot == SLOT_SOLID_OUT;
    }

    @Override
    public void tickServer() {
        checkTilt(Tiltable.TiltType.CONFIG, false);

        isOn = false;

        if (!hasExploded()) {
            power += ItemEnergyTransfer.extract(this, SLOT_BATTERY, MAX_POWER - power, false);
            boolean changed = tanks[0].setType(SLOT_FLUID_ID, SLOT_FLUID_ID, inventory);
            changed |= tanks[0].loadTank(1, 2, inventory);
            refine();
            for (int i = 1; i < 5; i++)
                changed |= tanks[i].unloadTank(1 + i * 2, 2 + i * 2, inventory);
            if (changed) setChanged();

            flush.provide((ServerLevel) level, this);
        } else if (onFire) {
            burn();
        }

        networkPackNT(150);
    }

    private void burn() {
        boolean hasFuel = false;
        for (FluidTankNTM tank : tanks) {
            if (tank.getFill() > 0) {
                tank.setFill(Math.max(tank.getFill() - 10, 0));
                hasFuel = true;
            }
        }
        if (!hasFuel || !(level instanceof ServerLevel server)) return;

        AABB box =
                new AABB(
                        worldPosition.getX() - 1.5,
                        worldPosition.getY(),
                        worldPosition.getZ() - 1.5,
                        worldPosition.getX() + 2.5,
                        worldPosition.getY() + 8,
                        worldPosition.getZ() + 2.5);
        for (Entity e : level.getEntitiesOfClass(Entity.class, box)) e.igniteForSeconds(5F);

        RandomSource rand = level.getRandom();
        double px = worldPosition.getX() + rand.nextDouble();
        double py = worldPosition.getY() + 1.5D + rand.nextDouble() * 3D;
        double pz = worldPosition.getZ() + rand.nextDouble();

        Services.NETWORK.sendToAllAround(
                new EffectNTPayload(HbmEffectNT.AnnihilatorFlame, px, py, pz),
                new TargetPoint(server, px, py, pz, 75));

        if (TickPhase.every(this, SharedConstants.TICKS_PER_SECOND)) {
            PollutionHandler.incrementPollution(
                    level,
                    worldPosition,
                    PollutionType.SOOT,
                    PollutionHandler.SOOT_PER_SECOND * 70);
        }
    }

    @Override
    public void explode(Level lvl, BlockPos pos) {
        if (hasExploded()) return;
        setExploded(true);
        onFire = true;
        setChanged();
    }

    @Override
    public boolean isDamaged() {
        return hasExploded();
    }

    public boolean hasExploded() {
        return getBlockState().getValue(MachineRefinery.EXPLODED);
    }

    private void setExploded(boolean exploded) {
        if (hasExploded() == exploded) return;
        level.setBlock(
                worldPosition,
                getBlockState().setValue(MachineRefinery.EXPLODED, exploded),
                Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SKIP_ON_PLACE);
    }

    @Override
    public List<CountIngredient> getRepairMaterials() {
        return REPAIR_MATERIALS.get();
    }

    @Override
    public void repair(Player player) {
        setExploded(false);
        setChanged();
    }

    @Override
    public void tryExtinguish(Level lvl, BlockPos pos, EnumExtinguishType type) {
        if (!hasExploded() || !onFire) return;

        if (type == EnumExtinguishType.FOAM || type == EnumExtinguishType.CO2) {
            onFire = false;
            setChanged();
            return;
        }

        if (type == EnumExtinguishType.WATER) {
            for (FluidTankNTM tank : tanks) {
                if (tank.getFill() > 0) {
                    lvl.explode(
                            null,
                            worldPosition.getX() + 0.5,
                            worldPosition.getY() + 1.5,
                            worldPosition.getZ() + 0.5,
                            5F,
                            true,
                            Level.ExplosionInteraction.BLOCK);
                    return;
                }
            }
        }
    }

    private void refine() {
        RefineryRecipe recipe = RefineryRecipes.INSTANCE.getRefinery(tanks[0].getTankType());
        if (recipe == null) {
            for (int i = 1; i < 5; i++) tanks[i].setTankType(null);
            return;
        }

        FluidStackNTM[] outputs = recipe.outputFluid;
        for (int i = 0; i < outputs.length; i++) tanks[i + 1].setTankType(outputs[i].type());

        if (power < 5 || tanks[0].getFill() < 100) return;

        for (int i = 0; i < outputs.length; i++) {
            if (tanks[i + 1].getFill() + outputs[i].amount() > tanks[i + 1].getMaxFill()) return;
        }

        isOn = true;
        tanks[0].setFill(tanks[0].getFill() - 100);
        for (int i = 0; i < outputs.length; i++) {
            tanks[i + 1].setFill((int) (tanks[i + 1].getFill() + outputs[i].amount()));
        }

        sulfur++;
        if (sulfur >= MAX_SULFUR) {
            sulfur -= MAX_SULFUR;

            ItemStack out = recipe.solidOutput();
            if (!out.isEmpty()) {
                ItemStack slot = inventory.get(SLOT_SOLID_OUT);
                if (slot.isEmpty()) {
                    inventory.set(SLOT_SOLID_OUT, out.copy());
                } else if (ItemStack.isSameItemSameComponents(slot, out)
                        && slot.getCount() + out.getCount() <= slot.getMaxStackSize()) {
                    slot.grow(out.getCount());
                }
            }
            setChanged();
        }

        if (TickPhase.every(this, SharedConstants.TICKS_PER_SECOND)) {
            PollutionHandler.incrementPollution(
                    level, worldPosition, PollutionType.SOOT, PollutionHandler.SOOT_PER_SECOND * 5);
        }
        power -= 5;
    }

    @Override
    public FluidTankNTM[] getAllTanks() {
        return tanks;
    }

    @Override
    public Component getDisplayName() {
        return getName();
    }

    @Override
    public AbstractContainerMenu createMenu(
            int containerId, Inventory playerInventory, Player player) {
        return new MenuMachineRefinery(containerId, playerInventory, this);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.getLong("power").ifPresent(v -> power = v);
        input.getInt("sulfur").ifPresent(v -> sulfur = v);
        onFire = input.getBooleanOr("onFire", false);
        for (int i = 0; i < 5; i++) {
            int idx = i;
            input.child("tank" + i).ifPresent(t -> tanks[idx].deserialize(t));
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("power", power);
        output.putInt("sulfur", sulfur);
        output.putBoolean("onFire", onFire);
        for (int i = 0; i < 5; i++) tanks[i].serialize(output.child("tank" + i));
    }

    @Override
    public void writePersistent(DataComponentMap.Builder components) {
        boolean empty = !hasExploded();
        for (FluidTankNTM tank : tanks) if (tank.getFill() > 0) empty = false;
        if (empty) return;
        components.set(
                ModDataComponents.REFINERY_CONTENTS.get(),
                new RefineryContents(FluidStackNTM.snapshot(tanks), hasExploded(), onFire));
    }

    @Override
    public void readPersistent(DataComponentGetter components) {
        RefineryContents contents = components.get(ModDataComponents.REFINERY_CONTENTS.get());
        if (contents == null) return;
        FluidStackNTM.restore(contents.tanks(), tanks);
        setExploded(contents.hasExploded());
        onFire = contents.onFire();
    }

    @Override
    public String[] persistentKeys() {
        return PERSISTENT_KEYS;
    }

    private void writeTanks(ByteBuf output) {
        for (int i = 0; i < 5; i++) tanks[i].packetSerialize(output);
    }

    private void readTanks(ByteBuf input) {
        for (int i = 0; i < 5; i++) tanks[i].packetDeserialize(input);
    }

    @Override
    public long syncUnitMask() {
        return super.syncUnitMask() | 0x1bL;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeLong(this.power);
            case 1 -> writeTanks(output);
            case 3 -> output.writeBoolean(this.onFire);
            case 4 -> output.writeBoolean(this.isOn);
            default -> super.writeSyncUnit(unit, output);
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> this.power = input.readLong();
            case 1 -> readTanks(input);
            case 3 -> this.onFire = input.readBoolean();
            case 4 -> this.isOn = input.readBoolean();
            default -> super.readSyncUnit(unit, input);
        }
    }
}
