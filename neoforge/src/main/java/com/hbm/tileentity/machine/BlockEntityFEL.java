// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine;

import com.hbm.api.control.IControlReceiver;
import com.hbm.api.energymk2.IEnergyHandlerMK2;
import com.hbm.api.energymk2.ItemEnergyTransfer;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.multiblock.BlockMultiblockCell;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.inventory.container.MenuFEL;
import com.hbm.items.machine.EnumWavelengths;
import com.hbm.items.machine.ItemFELCrystal;
import com.hbm.main.Polaroid;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.sound.AudioSystem;
import com.hbm.sound.AudioWrapper;
import com.hbm.sound.ModSounds;
import com.hbm.tileentity.AudioLoop;
import com.hbm.tileentity.BlockEntityMachineBase;
import com.hbm.util.ContaminationUtil.ContaminationType;
import com.hbm.util.ContaminationUtil.HazardType;
import com.hbm.util.ContaminationUtil;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;

public class BlockEntityFEL extends BlockEntityMachineBase
        implements AudioLoop, IEnergyHandlerMK2, MenuProvider, IControlReceiver, SyncUnitSchema {

    public static final int SLOT_BATTERY = 0;
    public static final int SLOT_CRYSTAL = 1;
    public static final int SLOT_COUNT = 2;

    public static final long maxPower = 20_000_000L;
    public static final int powerReq = 1250;
    public static final int RANGE = 24;

    @SyncField(units = 1L << 0)
    public long power;

    @SyncField(units = 1L << 1)
    public EnumWavelengths mode = EnumWavelengths.NULL;

    @SyncField(units = 1L << 2)
    public boolean isOn;

    @SyncField(units = 1L << 3)
    public boolean missingValidSilex = true;

    @SyncField(units = 1L << 4)
    public int distance;

    private int audioDuration;

    public BlockEntityFEL(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FEL.get(), pos, state, SLOT_COUNT);
    }

    private static boolean beamOpaque(BlockState state) {
        if (state.getBlock() instanceof BlockMultiblockCell
                || state.getBlock() instanceof BlockMultiblockCore) {
            return true;
        }
        return state.blocksMotion()
                && state.canOcclude()
                && state.instrument() != NoteBlockInstrument.HAT
                && !state.is(Blocks.GLOWSTONE);
    }

    private static boolean isSilexSurface(Level level, BlockPos pos, BlockState state) {
        if (state.getBlock() == ModBlocks.MACHINE_SILEX.get()) return true;
        if (!MultiblockSurface.isFoldedCell(state)) return false;
        BlockPos core = MultiblockSurface.coreOfAny(level, pos, state);
        return core != null
                && level.getBlockState(core).getBlock() == ModBlocks.MACHINE_SILEX.get();
    }

    private static boolean rotationIsValid(Direction silexDir, Direction felDir) {
        return silexDir == felDir || silexDir == felDir.getOpposite();
    }

    @Override
    public void tickClient() {

        boolean lasing =
                power > powerReq * Math.pow(2, mode.ordinal())
                        && isOn
                        && mode != EnumWavelengths.NULL
                        && distance - 3 > 0;
        audioDuration = Mth.clamp(audioDuration + (lasing ? 2 : -3), 0, 60);

        audioLoop(audioDuration > 10, 2F, (audioDuration - 10) / 100F + 0.5F);
    }

    @Override
    public AudioWrapper createAudioLoop() {
        return AudioSystem.getLoopedSound(
                ModSounds.FEL_LOOP.get(),
                SoundSource.BLOCKS,
                worldPosition.getX(),
                worldPosition.getY(),
                worldPosition.getZ(),
                2.0F,
                10F,
                2.0F);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.machineFEL");
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot == SLOT_CRYSTAL
                ? stack.getItem() instanceof ItemFELCrystal
                : slot == SLOT_BATTERY;
    }

    @Override
    public void tickServer() {
        Direction dir = getBlockState().getValue(BlockMultiblockCore.FACING);
        power += ItemEnergyTransfer.extract(this, SLOT_BATTERY, maxPower - power, false);

        ItemStack crystal = inventory.get(SLOT_CRYSTAL);
        if (isOn && !crystal.isEmpty() && crystal.getItem() instanceof ItemFELCrystal fel) {
            mode = fel.wavelength;
        } else {
            mode = EnumWavelengths.NULL;
        }

        boolean silexSpacing = false;
        boolean armedSilex = false;
        int req = (int) (powerReq * (mode.ordinal() == 0 ? 0 : Math.pow(3, mode.ordinal())));

        if (isOn && mode != EnumWavelengths.NULL && power < req) {
            power = 0;
        }

        if (isOn && power >= req && mode != EnumWavelengths.NULL) {
            int dist = distance - 1;
            double x0 = worldPosition.getX();
            double y0 = worldPosition.getY();
            double z0 = worldPosition.getZ();
            double blx = Math.min(x0, x0 + (double) dir.getStepX() * dist) + 0.2;
            double bux = Math.max(x0, x0 + (double) dir.getStepX() * dist) + 0.8;
            double bly = Math.min(y0, 1 + y0) + 0.2;
            double buy = Math.max(y0, 1 + y0) + 0.8;
            double blz = Math.min(z0, z0 + (double) dir.getStepZ() * dist) + 0.2;
            double buz = Math.max(z0, z0 + (double) dir.getStepZ() * dist) + 0.8;

            for (LivingEntity entity :
                    level.getEntitiesOfClass(
                            LivingEntity.class, new AABB(blx, bly, blz, bux, buy, buz))) {
                switch (mode) {
                    case VISIBLE -> {
                        entity.addEffect(
                                new MobEffectInstance(MobEffects.BLINDNESS, 60 * 60 * 65536, 0));
                        entity.igniteForSeconds(10);
                    }
                    case IR, UV -> entity.igniteForSeconds(10);
                    case GAMMA ->
                            ContaminationUtil.contaminate(
                                    entity, HazardType.RADIATION, ContaminationType.CREATIVE, 25);
                    case DRX -> ContaminationUtil.applyDigammaData(entity, 0.1F);
                    default -> {}
                }
            }

            power -= req;

            for (int i = 3; i < RANGE; i++) {
                BlockPos beam = worldPosition.offset(dir.getStepX() * i, 1, dir.getStepZ() * i);
                BlockState b = level.getBlockState(beam);

                if (isSilexSurface(level, beam, b)) {
                    BlockPos silexCore = beam.offset(dir.getStepX(), -1, dir.getStepZ());
                    if (level.getBlockEntity(silexCore) instanceof BlockEntitySILEX silex) {
                        Direction silexDir =
                                level.getBlockState(silexCore).getValue(BlockMultiblockCore.FACING);

                        if (rotationIsValid(silexDir, dir) && i >= 5 && !silexSpacing) {
                            armedSilex = true;
                            if (silex.mode != this.mode) {
                                silex.setMode(this.mode);
                                silex.setChanged();
                                silexSpacing = true;
                            }
                        } else {
                            level.destroyBlock(silexCore, true);
                        }
                    }
                    continue;
                }

                if (!beamOpaque(b) && b.getBlock() != Blocks.TNT) {
                    this.distance = RANGE;
                    silexSpacing = false;
                    continue;
                }

                this.distance = i;

                if (b.getBlock() instanceof LiquidBlock) {
                    level.playSound(
                            null,
                            beam,
                            SoundEvents.FIRE_EXTINGUISH,
                            SoundSource.BLOCKS,
                            1.0F,
                            1.0F);
                    level.removeBlock(beam, false);
                    break;
                }

                float hardness = b.getBlock().getExplosionResistance();
                if (hardness < 75 && level.getRandom().nextInt(5) == 0) {
                    level.playSound(
                            null,
                            beam,
                            SoundEvents.FIRE_EXTINGUISH,
                            SoundSource.BLOCKS,
                            1.0F,
                            1.0F);

                    BlockState fire =
                            mode != EnumWavelengths.DRX
                                    ? Blocks.FIRE.defaultBlockState()
                                    : Polaroid.isBalefireDay()
                                            ? ModBlocks.DIGAMMA_MATTER.get().defaultBlockState()
                                            : ModBlocks.FIRE_DIGAMMA.get().defaultBlockState();
                    level.setBlockAndUpdate(beam, fire);
                    if (mode == EnumWavelengths.DRX) {
                        level.setBlockAndUpdate(
                                beam.below(), ModBlocks.ASH_DIGAMMA.get().defaultBlockState());
                    }
                }
                break;
            }
        }

        this.missingValidSilex = !armedSilex;

        networkPackNT(250);
        setChanged();
    }

    public long getPowerScaled(long i) {
        return (power * i) / maxPower;
    }

    @Override
    public boolean hasPermission(Player player) {
        return true;
    }

    @Override
    public void receiveControl(CompoundTag data) {
        if (data.contains("toggle")) {
            isOn = !isOn;
            setChanged();
        }
    }

    @Override
    public long getPower() {
        return power;
    }

    @Override
    public void setPower(long p) {
        power = Math.max(0L, Math.min(p, maxPower));
    }

    @Override
    public long getMaxPower() {
        return maxPower;
    }

    @Override
    public Component getDisplayName() {
        return getName();
    }

    @Override
    public AbstractContainerMenu createMenu(
            int containerId, Inventory playerInventory, Player player) {
        return new MenuFEL(containerId, playerInventory, this);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        power = input.getLongOr("power", 0L);
        mode = EnumWavelengths.valueOf(input.getStringOr("mode", "NULL"));
        isOn = input.getBooleanOr("isOn", false);
        distance = input.getIntOr("distance", 0);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("power", power);
        output.putString("mode", mode.name());
        output.putBoolean("isOn", isOn);
        output.putInt("distance", distance);
    }

    private void writeMode(ByteBuf output) {
        output.writeInt(mode.ordinal());
    }

    private void readMode(ByteBuf input) {
        mode = EnumWavelengths.values()[input.readInt()];
    }

    @Override
    public long syncUnitMask() {
        return super.syncUnitMask() | 0x1fL;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeLong(this.power);
            case 1 -> writeMode(output);
            case 2 -> output.writeBoolean(this.isOn);
            case 3 -> output.writeBoolean(this.missingValidSilex);
            case 4 -> output.writeInt(this.distance);
            default -> super.writeSyncUnit(unit, output);
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> this.power = input.readLong();
            case 1 -> readMode(input);
            case 2 -> this.isOn = input.readBoolean();
            case 3 -> this.missingValidSilex = input.readBoolean();
            case 4 -> this.distance = input.readInt();
            default -> super.readSyncUnit(unit, input);
        }
    }
}
