// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine;

import com.hbm.api.control.IControlReceiver;
import com.hbm.api.energymk2.IEnergyHandlerMK2.ConnectionPriority;
import com.hbm.api.fluidmk2.FluidTankEndpoint;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.handler.radiation.RadiationSystemNT;
import com.hbm.handler.threading.TargetPoint;
import com.hbm.hazard.HazardRegistry;
import com.hbm.hazard.HazardSystem;
import com.hbm.inventory.container.MenuMachineAnnihilator;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.fluid.trait.FluidTrait.FluidReleaseType;
import com.hbm.inventory.fluid.trait.FluidTrait;
import com.hbm.inventory.recipes.AnnihilatorRecipes;
import com.hbm.items.ModDataComponents;
import com.hbm.items.machine.FluidIdentifierData;
import com.hbm.items.machine.FluidIdentifierItem;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.packet.toclient.EffectNTPayload;
import com.hbm.particle.HbmEffectNT;
import com.hbm.platform.Services;
import com.hbm.saveddata.AnnihilatorSavedData;
import com.hbm.sound.ModSounds;
import com.hbm.tileentity.BlockEntityMachineBase;
import com.hbm.util.TickPhase;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import java.math.BigInteger;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public class BlockEntityMachineAnnihilator extends BlockEntityMachineBase
        implements FluidTankEndpoint, IControlReceiver, MenuProvider, SyncUnitSchema {

    public static final int SLOT_TRASH = 0;
    public static final int SLOT_FLUID_ID = 1;
    public static final int SLOT_PAYOUT_START = 2;
    public static final int SLOT_PAYOUT_END = 7;
    public static final int SLOT_MONITOR = 8;
    public static final int SLOT_PAYOUT_REQUEST = 9;
    public static final int SLOT_PAYOUT_RESULT = 10;
    public static final int SLOT_COUNT = 11;

    public static final int TANK_CAPACITY = 2_500_000;

    private static final int[] ACCESSIBLE_SLOTS = {SLOT_TRASH, 2, 3, 4, 5, 6, 7};
    public final FluidTankNTM tank = new FluidTankNTM(TANK_CAPACITY);
    private final FluidTankNTM[] receiving;

    @SyncField(units = 1L << 0)
    public String pool = "Recycling";

    @SyncField(units = 1L << 1)
    public BigInteger monitorAmount = BigInteger.ZERO;

    public BlockEntityMachineAnnihilator(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ANNIHILATOR.get(), pos, state, SLOT_COUNT);
        receiving = new FluidTankNTM[] {tank};
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.annihilator");
    }

    @Override
    public void tickServer() {
        tank.setType(SLOT_FLUID_ID, SLOT_FLUID_ID, inventory);

        if (pool != null && !pool.isEmpty()) {
            AnnihilatorSavedData data = AnnihilatorSavedData.get((ServerLevel) level);
            boolean didSomething = false;

            ItemStack trash = inventory.get(SLOT_TRASH);
            if (!trash.isEmpty()) {
                onDestroy(trash);
                tryAddPayout(data.pushToPool(pool, trash, false));
                inventory.set(SLOT_TRASH, ItemStack.EMPTY);
                setChanged();
                didSomething = true;
            }

            if (tank.getFill() > 0) {
                Fluid type = tank.getTankType();
                FluidTrait.onRelease(
                        level,
                        worldPosition,
                        type,
                        tank,
                        FluidReleaseType.BURN,
                        tank.getFill() * 2);
                tryAddPayout(data.pushToPool(pool, type, tank.getFill(), false));
                tank.setFill(0);
                setChanged();
                didSomething = true;
            }

            if (didSomething) {
                Direction dir = facing();
                double px = worldPosition.getX() + 0.5 - dir.getStepX() * 3;
                double py = worldPosition.getY() + 8.75;
                double pz = worldPosition.getZ() + 0.5 - dir.getStepZ() * 3;
                ServerLevel server = (ServerLevel) level;
                Services.NETWORK.sendToAllAround(
                        new EffectNTPayload(HbmEffectNT.AnnihilatorFlame, px, py, pz),
                        new TargetPoint(server, px, py, pz, 150));
                if (TickPhase.every(this, 3)) {
                    level.playSound(
                            null,
                            px,
                            py,
                            pz,
                            ModSounds.FLAMETHROWER_SHOOT.get(),
                            SoundSource.BLOCKS,
                            1.0F,
                            0.5F + level.getRandom().nextFloat() * 0.25F);
                }
            }

            ItemStack monitorItem = inventory.get(SLOT_MONITOR);
            if (!monitorItem.isEmpty()) {
                if (monitorItem.getItem() instanceof FluidIdentifierItem) {
                    FluidIdentifierData fid =
                            monitorItem.getOrDefault(
                                    ModDataComponents.FLUID_IDENTIFIER.get(),
                                    FluidIdentifierData.EMPTY);
                    monitor(data, fid.primary());
                } else {
                    monitor(data, AnnihilatorRecipes.StackKey.of(monitorItem));
                }
            }

            ItemStack request = inventory.get(SLOT_PAYOUT_REQUEST);
            if (!request.isEmpty()) {
                ItemStack single = request.copy();
                single.setCount(1);
                onDestroy(single);
                ItemStack payout = data.pushToPool(pool, single, true);
                request.shrink(1);
                if (payout != null) {
                    ItemStack result = inventory.get(SLOT_PAYOUT_RESULT);
                    if (result.isEmpty()) {
                        inventory.set(SLOT_PAYOUT_RESULT, payout);
                    } else if (ItemStack.isSameItemSameComponents(result, payout)
                            && result.getMaxStackSize() >= result.getCount() + payout.getCount()) {
                        result.grow(payout.getCount());
                    }
                }
                setChanged();
            }
        }

        networkPackNT(25);
    }

    public void onDestroy(ItemStack stack) {
        double radiation = HazardSystem.getHazardLevelFromStack(stack, HazardRegistry.RADIATION);
        if (radiation > 0 && level instanceof ServerLevel server) {
            Direction dir = facing();
            BlockPos at = worldPosition.offset(-dir.getStepX() * 3, 9, -dir.getStepZ() * 3);
            RadiationSystemNT.incrementRad(server, at, Math.min(radiation * 5D, 1_000D));
        }
    }

    private Direction facing() {
        return BlockMultiblockCore.coreFacing(getBlockState());
    }

    public void monitor(AnnihilatorSavedData data, Object key) {
        AnnihilatorSavedData.AnnihilatorPool poolData = data.pools.get(this.pool);
        BigInteger amount = poolData == null ? null : poolData.items.get(key);
        this.monitorAmount = amount == null ? BigInteger.ZERO : amount;
    }

    public void tryAddPayout(ItemStack payout) {
        if (payout == null) return;
        for (int i = SLOT_PAYOUT_START; i <= SLOT_PAYOUT_END; i++) {
            ItemStack slot = inventory.get(i);
            if (!slot.isEmpty()
                    && ItemStack.isSameItemSameComponents(slot, payout)
                    && slot.getMaxStackSize() >= slot.getCount() + payout.getCount()) {
                slot.grow(payout.getCount());
                setChanged();
                return;
            }
        }
        for (int i = SLOT_PAYOUT_START; i <= SLOT_PAYOUT_END; i++) {
            if (inventory.get(i).isEmpty()) {
                inventory.set(i, payout);
                setChanged();
                return;
            }
        }
    }

    @Override
    public FluidTankNTM[] getReceivingTanks() {
        return receiving;
    }

    @Override
    public ConnectionPriority getFluidPriority() {
        return ConnectionPriority.LOW;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return switch (slot) {
            case SLOT_TRASH, SLOT_MONITOR, SLOT_PAYOUT_REQUEST -> true;
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
        return slot == SLOT_TRASH;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot >= SLOT_PAYOUT_START && slot <= SLOT_PAYOUT_END;
    }

    @Override
    public boolean hasPermission(Player player) {
        return stillValid(player);
    }

    @Override
    public void receiveControl(CompoundTag data) {
        if (data.contains("pool")) {
            String newPool = data.getStringOr("pool", pool);
            if (newPool != null && !newPool.isEmpty()) {
                this.pool = newPool;
                setChanged();
            }
        }
    }

    @Override
    public Component getDisplayName() {
        return getName();
    }

    @Override
    public AbstractContainerMenu createMenu(
            int containerId, Inventory playerInventory, Player player) {
        return new MenuMachineAnnihilator(containerId, playerInventory, this);
    }

    private void writePool(ByteBuf output) {
        new FriendlyByteBuf(output).writeUtf(pool == null ? "" : pool);
    }

    private void readPool(ByteBuf input) {
        pool = new FriendlyByteBuf(input).readUtf();
    }

    private void writeMonitorAmount(ByteBuf output) {
        byte[] amount = monitorAmount.toByteArray();
        output.writeInt(amount.length);
        output.writeBytes(amount);
    }

    private void readMonitorAmount(ByteBuf input) {
        int length = input.readInt();
        if (length <= 0 || length > input.readableBytes())
            throw new DecoderException("Invalid annihilator amount");
        byte[] amount = new byte[length];
        input.readBytes(amount);
        monitorAmount = new BigInteger(amount);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.child("tank").ifPresent(tank::deserialize);
        pool = input.getStringOr("pool", pool);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        tank.serialize(output.child("tank"));
        output.putString("pool", pool == null ? "" : pool);
    }

    @Override
    public long syncUnitMask() {
        return 0x3L;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> writePool(output);
            case 1 -> writeMonitorAmount(output);
            default -> throw new IllegalArgumentException();
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> readPool(input);
            case 1 -> readMonitorAmount(input);
            default -> throw new IllegalArgumentException();
        }
    }
}
