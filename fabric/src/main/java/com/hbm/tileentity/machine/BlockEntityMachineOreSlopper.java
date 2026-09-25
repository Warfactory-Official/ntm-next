// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: HbmMods (The Bobcat)
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine;

import com.hbm.api.energymk2.IBatteryItem;
import com.hbm.api.energymk2.IEnergyHandlerMK2;
import com.hbm.api.energymk2.ItemEnergyTransfer;
import com.hbm.api.fluidmk2.FluidFlushOutputs;
import com.hbm.api.fluidmk2.FluidTankEndpoint;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.client.ClientEffects;
import com.hbm.client.ClientPlayerAccess;
import com.hbm.inventory.container.MenuMachineOreSlopper;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.items.ModItems;
import com.hbm.items.machine.upgrade.ItemMachineUpgrade;
import com.hbm.items.machine.upgrade.UpgradeManager;
import com.hbm.items.machine.upgrade.UpgradeType;
import com.hbm.items.special.BedrockOreSample;
import com.hbm.items.special.ItemBedrockOreBase;
import com.hbm.items.special.ItemBedrockOreNew.BedrockOreGrade;
import com.hbm.items.special.ItemBedrockOreNew.BedrockOreType;
import com.hbm.items.special.ItemBedrockOreNew;
import com.hbm.lib.ModDamageTypes;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.particle.ParticleGiblet;
import com.hbm.particle.helper.ParticleCreators;
import com.hbm.tileentity.BlockEntityMachineBase;
import com.hbm.tileentity.IFluidCopiable;
import com.hbm.tileentity.IUpgradeInfoProvider;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

public class BlockEntityMachineOreSlopper extends BlockEntityMachineBase
        implements IEnergyHandlerMK2,
                FluidTankEndpoint,
                MenuProvider,
                IUpgradeInfoProvider,
                IFluidCopiable,
                SyncUnitSchema {

    public static final int SLOT_BATTERY = 0;
    public static final int SLOT_FLUID_ID = 1;
    public static final int SLOT_INPUT = 2;
    public static final int SLOT_OUTPUT_START = 3;
    public static final int SLOT_OUTPUT_END = 8;
    public static final int SLOT_UPGRADE_START = 9;
    public static final int SLOT_UPGRADE_END = 10;
    public static final int SLOT_COUNT = 11;

    public static final long maxPower = 100_000L;
    public static final int waterUsed = 1_000;
    public static final long consumptionBase = 200L;
    public static final int TANK_CAPACITY = 16_000;

    private static final int[] ACCESSIBLE_SLOTS = {2, 3, 4, 5, 6, 7, 8};
    private static final int[] VALID_UPGRADES =
            IUpgradeInfoProvider.upgradeCaps(UpgradeType.SPEED, 3, UpgradeType.EFFECT, 3);

    @SyncField(units = 1L << 4)
    public final FluidTankNTM[] tanks = new FluidTankNTM[2];

    private final FluidTankNTM[] receiving;
    private final FluidTankNTM[] sending;

    public final double[] ores = new double[BedrockOreType.VALUES.length];
    private final UpgradeManager upgradeManager = new UpgradeManager(this);

    @SyncField(units = 1L << 1)
    public long power;

    @SyncField(units = 1L << 0)
    public long consumption = consumptionBase;

    @SyncField(units = 1L << 3)
    public float progress;

    @SyncField(units = 1L << 2)
    public boolean processing;

    public SlopperAnimation animation = SlopperAnimation.LOWERING;
    public float slider, prevSlider;
    public float bucket, prevBucket;
    public float blades, prevBlades;
    public float fan, prevFan;
    public int delay;

    private final FluidFlushOutputs flush = new FluidFlushOutputs();

    public BlockEntityMachineOreSlopper(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ORE_SLOPPER.get(), pos, state, SLOT_COUNT);
        tanks[0] = new FluidTankNTM(NTMFluids.WATER, TANK_CAPACITY);
        tanks[1] = new FluidTankNTM(NTMFluids.SLOP, TANK_CAPACITY);
        receiving = new FluidTankNTM[] {tanks[0]};
        sending = new FluidTankNTM[] {tanks[1]};
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.machineOreSlopper");
    }

    @Override
    public void tickServer() {
        power += ItemEnergyTransfer.extract(this, SLOT_BATTERY, maxPower - power, false);

        tanks[0].setType(SLOT_FLUID_ID, SLOT_FLUID_ID, inventory);
        Fluid conversion = getFluidOutput(tanks[0].getTankType());
        if (conversion != null) tanks[1].setTankType(conversion);

        this.processing = false;

        upgradeManager.scan(this, SLOT_UPGRADE_START, SLOT_UPGRADE_END);
        int speed = Math.min(upgradeManager.getLevel(UpgradeType.SPEED), 3);
        int efficiency = Math.min(upgradeManager.getLevel(UpgradeType.EFFECT), 3);

        this.consumption =
                consumptionBase + (consumptionBase * speed) / 2 + (consumptionBase * efficiency);

        if (canSlop()) {
            this.power -= this.consumption;
            this.progress += 1F / (600 - speed * 150);
            this.processing = true;

            while (progress >= 1F && canSlop()) {
                progress -= 1F;

                BedrockOreSample sample = ItemBedrockOreBase.sample(inventory.get(SLOT_INPUT));
                double multiplier = 1D + efficiency * 0.1D;
                ores[BedrockOreType.LIGHT_METAL.ordinal()] += sample.lightMetal() * multiplier;
                ores[BedrockOreType.HEAVY_METAL.ordinal()] += sample.heavyMetal() * multiplier;
                ores[BedrockOreType.RARE_EARTH.ordinal()] += sample.rareEarth() * multiplier;
                ores[BedrockOreType.ACTINIDE.ordinal()] += sample.actinide() * multiplier;
                ores[BedrockOreType.NON_METAL.ordinal()] += sample.nonMetal() * multiplier;
                ores[BedrockOreType.CRYSTALLINE.ordinal()] += sample.crystalline() * multiplier;

                inventory.get(SLOT_INPUT).shrink(1);
                tanks[0].setFill(tanks[0].getFill() - waterUsed);
                tanks[1].setFill(tanks[1].getFill() + waterUsed);
                setChanged();
            }

            shred();
        } else {
            this.progress = 0;
        }

        for (BedrockOreType type : BedrockOreType.VALUES) {
            ItemStack output = ItemBedrockOreNew.make(BedrockOreGrade.BASE, type);
            outer:
            while (ores[type.ordinal()] >= 1) {
                for (int i = SLOT_OUTPUT_START; i <= SLOT_OUTPUT_END; i++) {
                    ItemStack slot = inventory.get(i);
                    if (!slot.isEmpty()
                            && ItemStack.isSameItemSameComponents(slot, output)
                            && slot.getCount() < slot.getMaxStackSize()) {
                        slot.grow(1);
                        ores[type.ordinal()] -= 1F;
                        continue outer;
                    }
                }
                for (int i = SLOT_OUTPUT_START; i <= SLOT_OUTPUT_END; i++) {
                    if (inventory.get(i).isEmpty()) {
                        inventory.set(i, output.copy());
                        ores[type.ordinal()] -= 1F;
                        continue outer;
                    }
                }
                break;
            }
        }

        flush.provide((ServerLevel) level, this);

        networkPackNT(150);
    }

    private void shred() {
        ServerLevel server = (ServerLevel) level;
        Direction dir = BlockMultiblockCore.coreFacing(getBlockState());
        AABB box =
                new AABB(
                                worldPosition.getX() - 0.5D,
                                worldPosition.getY() + 1,
                                worldPosition.getZ() - 0.5D,
                                worldPosition.getX() + 1.5D,
                                worldPosition.getY() + 3,
                                worldPosition.getZ() + 1.5D)
                        .move(dir.getStepX(), 0, dir.getStepZ());

        for (Entity e : server.getEntities((Entity) null, box, entity -> true)) {
            e.hurtServer(server, server.damageSources().source(ModDamageTypes.BLENDER), 1000F);

            if (!e.isAlive() && e instanceof LivingEntity) {
                ParticleCreators.giblets(server, e, ParticleGiblet.TYPE_MEAT, 5);
                server.playSound(
                        null,
                        e.getX(),
                        e.getY(),
                        e.getZ(),
                        SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR,
                        SoundSource.BLOCKS,
                        2.0F,
                        0.95F + server.getRandom().nextFloat() * 0.2F);
            }
        }
    }

    @Override
    public void tickClient() {
        this.prevSlider = this.slider;
        this.prevBucket = this.bucket;
        this.prevBlades = this.blades;
        this.prevFan = this.fan;

        if (!this.processing) return;

        this.blades += 15F;
        this.fan += 35F;

        if (blades >= 360) {
            blades -= 360;
            prevBlades -= 360;
        }
        if (fan >= 360) {
            fan -= 360;
            prevFan -= 360;
        }

        Player viewer = ClientPlayerAccess.player();
        if (animation == SlopperAnimation.DUMPING
                && viewer != null
                && viewer.getEyePosition()
                                .distanceToSqr(
                                        worldPosition.getX() + 0.5,
                                        worldPosition.getY() + 4,
                                        worldPosition.getZ() + 0.5)
                        <= 2500) {
            Direction dir = BlockMultiblockCore.coreFacing(getBlockState());
            RandomSource rand = level.getRandom();
            ClientEffects.spawnExtBlockDust(
                    worldPosition.getX() + 0.5 + dir.getStepX() + rand.nextGaussian() * 0.25,
                    worldPosition.getY() + 4.25,
                    worldPosition.getZ() + 0.5 + dir.getStepZ() + rand.nextGaussian() * 0.25,
                    Blocks.IRON_BLOCK.defaultBlockState());
        }

        if (delay > 0) {
            delay--;
            return;
        }

        switch (animation) {
            case LOWERING -> {
                this.bucket += 1F / 40F;
                if (bucket >= 1F) {
                    bucket = 1F;
                    animation = SlopperAnimation.LIFTING;
                    delay = 20;
                }
            }
            case LIFTING -> {
                this.bucket -= 1F / 40F;
                if (bucket <= 0) {
                    bucket = 0F;
                    animation = SlopperAnimation.MOVE_SHREDDER;
                    delay = 10;
                }
            }
            case MOVE_SHREDDER -> {
                this.slider += 1F / 50F;
                if (slider >= 1F) {
                    slider = 1F;
                    animation = SlopperAnimation.DUMPING;
                    delay = 60;
                }
            }
            case DUMPING -> animation = SlopperAnimation.MOVE_BUCKET;
            case MOVE_BUCKET -> {
                this.slider -= 1F / 50F;
                if (slider <= 0F) {
                    animation = SlopperAnimation.LOWERING;
                    delay = 10;
                }
            }
        }
    }

    private static @Nullable Fluid getFluidOutput(@Nullable Fluid input) {
        return input == NTMFluids.WATER ? NTMFluids.SLOP : null;
    }

    public boolean canSlop() {
        if (getFluidOutput(tanks[0].getTankType()) == null) return false;
        if (tanks[0].getFill() < waterUsed) return false;
        if (tanks[1].getFill() + waterUsed > tanks[1].getMaxFill()) return false;
        if (power < consumption) return false;
        ItemStack in = inventory.get(SLOT_INPUT);
        return !in.isEmpty() && in.getItem() == ModItems.BEDROCK_ORE_BASE.get();
    }

    @Override
    public long getPower() {
        return power;
    }

    @Override
    public void setPower(long p) {
        this.power = Math.max(0L, Math.min(p, maxPower));
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
    public FluidTankNTM[] getSendingTanks() {
        return sending;
    }

    @Override
    public FluidTankNTM[] getAllTanks() {
        return tanks;
    }

    @Override
    public FluidTankNTM getTankToPaste() {
        return tanks[0];
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return switch (slot) {
            case SLOT_INPUT -> stack.getItem() == ModItems.BEDROCK_ORE_BASE.get();
            case SLOT_BATTERY -> IBatteryItem.isBattery(stack);
            case SLOT_UPGRADE_START, SLOT_UPGRADE_END -> ItemMachineUpgrade.isUpgrade(stack);
            default -> slot < SLOT_OUTPUT_START || slot > SLOT_OUTPUT_END;
        };
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return ACCESSIBLE_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return slot == SLOT_INPUT;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot >= SLOT_OUTPUT_START && slot <= SLOT_OUTPUT_END;
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
        return new MenuMachineOreSlopper(containerId, playerInventory, this);
    }

    private void writeTanks(ByteBuf output) {
        tanks[0].packetSerialize(output);
        tanks[1].packetSerialize(output);
    }

    private void readTanks(ByteBuf input) {
        tanks[0].packetDeserialize(input);
        tanks[1].packetDeserialize(input);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        power = input.getLongOr("power", 0L);
        progress = input.getFloatOr("progress", 0F);
        input.child("water").ifPresent(tanks[0]::deserialize);
        input.child("slop").ifPresent(tanks[1]::deserialize);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("power", power);
        output.putFloat("progress", progress);
        tanks[0].serialize(output.child("water"));
        tanks[1].serialize(output.child("slop"));
    }

    public enum SlopperAnimation {
        LOWERING,
        LIFTING,
        MOVE_SHREDDER,
        DUMPING,
        MOVE_BUCKET
    }

    @Override
    public long syncUnitMask() {
        return 0x1fL;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeLong(this.consumption);
            case 1 -> output.writeLong(this.power);
            case 2 -> output.writeBoolean(this.processing);
            case 3 -> output.writeFloat(this.progress);
            case 4 -> writeTanks(output);
            default -> throw new IllegalArgumentException();
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> this.consumption = input.readLong();
            case 1 -> this.power = input.readLong();
            case 2 -> this.processing = input.readBoolean();
            case 3 -> this.progress = input.readFloat();
            case 4 -> readTanks(input);
            default -> throw new IllegalArgumentException();
        }
    }
}
