// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: HbmMods (The Bobcat)
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine;

import com.hbm.api.fluidmk2.FluidFlushOutputs;
import com.hbm.api.fluidmk2.FluidTankEndpoint;
import com.hbm.api.tile.IHeatSource;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.handler.pollution.PollutionHandler;
import com.hbm.handler.pollution.PollutionType;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.items.ModItems;
import com.hbm.items.machine.EnumAshType;
import com.hbm.items.machine.EnumBriquetteType;
import com.hbm.modules.ModuleBurnTime;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.tileentity.Audible;
import com.hbm.tileentity.BlockEntityMachinePolluting;
import com.hbm.util.TickPhase;
import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ContainerUser;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import static com.hbm.inventory.OreDictManager.ANY_COKE;
import static com.hbm.inventory.OreDictManager.COAL;
import static com.hbm.inventory.OreDictManager.COALCOKE;
import static com.hbm.inventory.OreDictManager.KEY_STICK;
import static com.hbm.inventory.OreDictManager.LIGCOKE;
import static com.hbm.inventory.OreDictManager.LIGNITE;
import static com.hbm.inventory.OreDictManager.PETCOKE;
import static com.hbm.inventory.OreDictManager.WOOD;

public abstract class BlockEntityFireboxBase extends BlockEntityMachinePolluting
        implements Audible, IHeatSource, FluidTankEndpoint, SyncUnitSchema {

    private static final int[] ACCESSIBLE_SLOTS = {0, 1};

    private static final List<TagKey<Item>> COAL_ASH =
            List.of(
                    COAL.gem(),
                    COAL.dustTiny(),
                    COAL.dust(),
                    COAL.ore(),
                    COAL.block(),
                    COAL.fragment(),
                    LIGNITE.gem(),
                    LIGNITE.dust(),
                    LIGNITE.ore(),
                    LIGNITE.fragment(),
                    COALCOKE.gem(),
                    COALCOKE.block(),
                    PETCOKE.gem(),
                    PETCOKE.block(),
                    LIGCOKE.gem(),
                    LIGCOKE.block(),
                    ANY_COKE.gem(),
                    ANY_COKE.block());

    private static final List<TagKey<Item>> WOOD_ASH =
            List.of(
                    ItemTags.LOGS,
                    ItemTags.PLANKS,
                    ItemTags.WOODEN_SLABS,
                    ItemTags.WOODEN_STAIRS,
                    KEY_STICK,
                    ItemTags.SAPLINGS,
                    WOOD.stock(),
                    WOOD.grip());

    @SyncField(units = 1L << 0)
    public int maxBurnTime;

    @SyncField(units = 1L << 1)
    public int burnTime;

    @SyncField(units = 1L << 2)
    public int burnHeat;

    @SyncField(units = 1L << 5)
    public boolean wasOn = false;

    public float doorAngle = 0;
    public float prevDoorAngle = 0;

    @SyncField(units = 1L << 3)
    public int heatEnergy;

    @SyncField(units = 1L << 4)
    private int playersUsing = 0;

    private final FluidFlushOutputs flush = new FluidFlushOutputs();

    protected BlockEntityFireboxBase(BlockEntityType<?> type, BlockPos pos, BlockState state) {

        super(type, pos, state, 2, 50);
    }

    @Override
    public void startOpen(ContainerUser user) {
        if (!level.isClientSide()) this.playersUsing++;
    }

    @Override
    public void stopOpen(ContainerUser user) {
        if (!level.isClientSide()) this.playersUsing--;
    }

    @Override
    public void tickServer() {
        wasOn = false;

        if (burnTime <= 0) {

            for (int i = 0; i < 2; i++) {
                ItemStack stack = inventory.get(i);
                if (stack.isEmpty()) continue;

                int baseTime = getModule().getBurnTime(level, stack);
                if (baseTime <= 0) continue;

                this.maxBurnTime = this.burnTime = (int) (baseTime * getTimeMult());
                this.burnHeat = getModule().getBurnHeat(getBaseHeat(), stack);

                if (level.getBlockEntity(worldPosition.below())
                        instanceof BlockEntityAshpit ashpit) {
                    EnumAshType ashType = getAshFromFuel(stack);
                    if (ashType == EnumAshType.COAL) ashpit.ashLevelCoal += baseTime;
                    else if (ashType == EnumAshType.WOOD) ashpit.ashLevelWood += baseTime;
                    else if (ashType == EnumAshType.MISC) ashpit.ashLevelMisc += baseTime;
                }

                ItemStackTemplate remainder = stack.getCraftingRemainder();
                stack.shrink(1);
                if (stack.isEmpty() && remainder != null) inventory.set(i, remainder.create());

                this.wasOn = true;
                break;
            }
        } else {

            if (this.heatEnergy < getMaxHeat()) {
                burnTime--;
                if (TickPhase.every(this, SharedConstants.TICKS_PER_SECOND)) {

                    this.pollute(PollutionType.SOOT, PollutionHandler.SOOT_PER_SECOND * 3);
                }
            }
            this.wasOn = true;

            if (level.getRandom().nextInt(15) == 0 && !isMuffled()) {
                level.playSound(
                        null,
                        worldPosition,
                        SoundEvents.FIRE_AMBIENT,
                        SoundSource.BLOCKS,
                        1.0F,
                        0.5F + level.getRandom().nextFloat() * 0.5F);
            }
        }

        if (wasOn) {
            this.heatEnergy = Math.min(this.heatEnergy + this.burnHeat, getMaxHeat());
        } else {
            this.heatEnergy = Math.max(this.heatEnergy - Math.max(this.heatEnergy / 1000, 1), 0);
            this.burnHeat = 0;
        }

        flush.provide((ServerLevel) level, this);

        networkPackNT(50);
    }

    @Override
    public void tickClient() {
        this.prevDoorAngle = this.doorAngle;
        float swingSpeed = (doorAngle / 10F) + 3;

        if (this.playersUsing > 0) {
            this.doorAngle += swingSpeed;
        } else {
            this.doorAngle -= swingSpeed;
        }

        this.doorAngle = Mth.clamp(this.doorAngle, 0F, 135F);

        if (wasOn && TickPhase.every(this, 5)) {
            Direction dir = BlockMultiblockCore.coreFacing(getBlockState());
            double x = worldPosition.getX() + 0.5 + dir.getStepX();
            double y = worldPosition.getY() + 0.25;
            double z = worldPosition.getZ() + 0.5 + dir.getStepZ();
            level.addParticle(
                    ParticleTypes.FLAME,
                    x + level.getRandom().nextDouble() * 0.5 - 0.25,
                    y + level.getRandom().nextDouble() * 0.25,
                    z + level.getRandom().nextDouble() * 0.5 - 0.25,
                    0,
                    0,
                    0);
        }
    }

    public static EnumAshType getAshFromFuel(ItemStack stack) {
        EnumBriquetteType briquette = ModItems.BRIQUETTE.typeOf(stack);
        if (briquette != null)
            return briquette == EnumBriquetteType.WOOD ? EnumAshType.WOOD : EnumAshType.COAL;
        for (TagKey<Item> tag : COAL_ASH) if (stack.is(tag)) return EnumAshType.COAL;
        for (TagKey<Item> tag : WOOD_ASH) if (stack.is(tag)) return EnumAshType.WOOD;
        return EnumAshType.MISC;
    }

    public abstract ModuleBurnTime getModule();

    public abstract int getBaseHeat();

    public abstract double getTimeMult();

    public abstract int getMaxHeat();

    @Override
    public int[] getSlotsForFace(Direction side) {
        return ACCESSIBLE_SLOTS;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return getModule().getBurnTime(level, stack) > 0;
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.maxBurnTime = input.getIntOr("maxBurnTime", 0);
        this.burnTime = input.getIntOr("burnTime", 0);
        this.burnHeat = input.getIntOr("burnHeat", 0);
        this.heatEnergy = input.getIntOr("heatEnergy", 0);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("maxBurnTime", maxBurnTime);
        output.putInt("burnTime", burnTime);
        output.putInt("burnHeat", burnHeat);
        output.putInt("heatEnergy", heatEnergy);
    }

    @Override
    public int getHeatStored(Level level, BlockPos pos) {
        return BlockMultiblockCore.vendsHeatAt(this, pos) ? heatEnergy : 0;
    }

    @Override
    public void useUpHeat(Level level, BlockPos pos, int heat) {
        if (!BlockMultiblockCore.vendsHeatAt(this, pos)) return;
        this.heatEnergy = Math.max(0, this.heatEnergy - heat);
    }

    @Override
    public FluidTankNTM[] getSendingTanks() {
        return getSmokeTanks();
    }

    @Override
    public long syncUnitMask() {
        return 0x3fL;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeInt(this.maxBurnTime);
            case 1 -> output.writeInt(this.burnTime);
            case 2 -> output.writeInt(this.burnHeat);
            case 3 -> output.writeInt(this.heatEnergy);
            case 4 -> output.writeInt(this.playersUsing);
            case 5 -> output.writeBoolean(this.wasOn);
            default -> throw new IllegalArgumentException();
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> this.maxBurnTime = input.readInt();
            case 1 -> this.burnTime = input.readInt();
            case 2 -> this.burnHeat = input.readInt();
            case 3 -> this.heatEnergy = input.readInt();
            case 4 -> this.playersUsing = input.readInt();
            case 5 -> this.wasOn = input.readBoolean();
            default -> throw new IllegalArgumentException();
        }
    }
}
