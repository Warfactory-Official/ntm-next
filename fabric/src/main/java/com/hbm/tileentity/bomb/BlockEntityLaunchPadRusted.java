// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.bomb;

import com.hbm.api.control.IControlReceiver;
import com.hbm.api.item.IDesignatorItem;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.entity.missile.EntityMissileBaseNT;
import com.hbm.entity.missile.EntityMissileTier4.EntityMissileDoomsdayRusted;
import com.hbm.interfaces.IBomb;
import com.hbm.inventory.container.MenuLaunchPadRusted;
import com.hbm.items.ModItems;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.particle.HbmParticles;
import com.hbm.sound.ModSounds;
import com.hbm.tileentity.BlockEntityMachineBase;
import io.netty.buffer.ByteBuf;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;

public class BlockEntityLaunchPadRusted extends BlockEntityMachineBase
        implements MenuProvider, IControlReceiver, SyncUnitSchema {

    public static final int SLOT_RELEASE = 0;
    public static final int SLOT_CODE = 1;
    public static final int SLOT_KEY = 2;
    public static final int SLOT_DESIGNATOR = 3;
    public static final int SLOT_COUNT = 4;
    private final Set<BlockPos> activatedBlocks = new HashSet<>(4);
    public int redstonePower;

    @SyncField(units = 1L)
    public boolean missileLoaded;

    public BlockEntityLaunchPadRusted(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LAUNCHPAD_RUSTED.get(), pos, state, SLOT_COUNT);
    }

    @Override
    public void tickClient() {
        if (level.getEntitiesOfClass(
                        EntityMissileBaseNT.class,
                        new AABB(
                                worldPosition.getX() - .5D,
                                worldPosition.getY(),
                                worldPosition.getZ() - .5D,
                                worldPosition.getX() + 1.5D,
                                worldPosition.getY() + 10D,
                                worldPosition.getZ() + 1.5D))
                .isEmpty()) return;
        Direction facing = BlockMultiblockCore.coreFacing(getBlockState());
        for (int i = 0; i < 15; i++) {
            Direction direction = facing;
            if (level.getRandom().nextBoolean()) direction = direction.getOpposite();
            if (level.getRandom().nextBoolean()) direction = direction.getClockWise();
            double motionX = level.getRandom().nextGaussian() * .15D + .75D;
            double motionZ = level.getRandom().nextGaussian() * .15D + .75D;
            level.addParticle(
                    HbmParticles.LAUNCH_SMOKE.get(),
                    true,
                    false,
                    worldPosition.getX() + .5D,
                    worldPosition.getY() + .25D,
                    worldPosition.getZ() + .5D,
                    motionX * direction.getStepX(),
                    0D,
                    motionZ * direction.getStepZ());
        }
    }

    public IBomb.BombReturnCode launch() {
        ItemStack code = getItem(SLOT_CODE);
        ItemStack key = getItem(SLOT_KEY);
        ItemStack designatorStack = getItem(SLOT_DESIGNATOR);
        if (!missileLoaded
                || code.getItem() != ModItems.LAUNCH_CODE.get()
                || key.getItem() != ModItems.LAUNCH_KEY.get()
                || !(designatorStack.getItem() instanceof IDesignatorItem designator)
                || !designator.isReady(designatorStack))
            return IBomb.BombReturnCode.ERROR_MISSING_COMPONENT;

        EntityMissileDoomsdayRusted missile = new EntityMissileDoomsdayRusted(level);
        missile.launch(
                worldPosition.getX() + 0.5D,
                worldPosition.getY() + 1D,
                worldPosition.getZ() + 0.5D,
                designator.getTargetX(designatorStack),
                designator.getTargetZ(designatorStack));

        missile.setFacing(BlockMultiblockCore.coreFacing(getBlockState()).get3DDataValue());
        level.addFreshEntity(missile);
        level.playSound(
                null, worldPosition, ModSounds.MISSILE_TAKE_OFF.get(), SoundSource.BLOCKS, 2F, 1F);
        missileLoaded = false;
        removeItem(SLOT_CODE, 1);
        setChanged();
        networkPackNT(250);
        return IBomb.BombReturnCode.LAUNCHED;
    }

    public void updateRedstonePower(BlockPos pos) {
        int before = redstonePower;
        boolean powered = level.hasNeighborSignal(pos);
        boolean contained = activatedBlocks.contains(pos);
        if (!contained && powered) {
            activatedBlocks.add(pos);
            if (redstonePower == -1) redstonePower = 0;
            redstonePower++;
        } else if (contained && !powered) {
            activatedBlocks.remove(pos);
            redstonePower--;
            if (redstonePower == 0) redstonePower = -1;
        }
        if (redstonePower > 0 && before <= 0) launch();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return switch (slot) {
            case SLOT_CODE -> stack.is(ModItems.LAUNCH_CODE.get());
            case SLOT_KEY -> stack.is(ModItems.LAUNCH_KEY.get());
            case SLOT_DESIGNATOR -> stack.getItem() instanceof IDesignatorItem;
            default -> false;
        };
    }

    @Override
    public boolean hasPermission(Player player) {
        return stillValid(player);
    }

    @Override
    public void receiveControl(CompoundTag data) {
        if (data.getBooleanOr("release", false)
                && missileLoaded
                && getItem(SLOT_RELEASE).isEmpty()) {
            missileLoaded = false;
            setItem(SLOT_RELEASE, new ItemStack(ModItems.MISSILE_DOOMSDAY_RUSTED));
            setChanged();
            networkPackNT(250);
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        missileLoaded = input.getBooleanOr("missileLoaded", false);
        redstonePower = input.getIntOr("redstonePower", 0);
        activatedBlocks.clear();
        input.read("activatedBlocks", BlockPos.CODEC.listOf()).ifPresent(activatedBlocks::addAll);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putBoolean("missileLoaded", missileLoaded);
        output.putInt("redstonePower", redstonePower);
        output.store("activatedBlocks", BlockPos.CODEC.listOf(), List.copyOf(activatedBlocks));
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.launchPadRusted");
    }

    @Override
    public Component getDisplayName() {
        return getName();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new MenuLaunchPadRusted(id, inventory, this);
    }

    @Override
    public long syncUnitMask() {
        return 1L;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeBoolean(this.missileLoaded);
            default -> throw new IllegalArgumentException();
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> this.missileLoaded = input.readBoolean();
            default -> throw new IllegalArgumentException();
        }
    }
}
