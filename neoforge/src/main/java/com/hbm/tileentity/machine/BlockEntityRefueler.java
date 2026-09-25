// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine;

import com.hbm.api.fluidmk2.FluidTankEndpoint;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.machine.BlockMachineHorizontal;
import com.hbm.handler.ArmorModHandler;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.items.machine.IFluidContainerItem;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.tileentity.BlockEntityMachineBase;
import com.hbm.util.BobMathUtil;
import io.netty.buffer.ByteBuf;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;

public class BlockEntityRefueler extends BlockEntityMachineBase
        implements FluidTankEndpoint, SyncUnitSchema {

    private static final EquipmentSlot[] EQUIPMENT = {
        EquipmentSlot.MAINHAND,
        EquipmentSlot.FEET,
        EquipmentSlot.LEGS,
        EquipmentSlot.CHEST,
        EquipmentSlot.HEAD
    };

    public static Consumer<BlockEntityRefueler> CLIENT_PARTICLE = refueler -> {};

    @SyncField(units = 1L << 0)
    private boolean operating;

    @SyncField(units = 1L << 1)
    public final FluidTankNTM tank = new FluidTankNTM(NTMFluids.KEROSENE, 100);

    private final FluidTankNTM[] receiving = {tank};
    private int operatingTime;

    public double fillLevel;
    public double prevFillLevel;

    public BlockEntityRefueler(BlockPos pos, BlockState state) {
        super(ModBlockEntities.REFUELER.get(), pos, state, 0);
    }

    @Override
    public void tickServer() {
        operating = false;
        AABB bounds =
                new AABB(
                        worldPosition.getX(),
                        worldPosition.getY(),
                        worldPosition.getZ(),
                        worldPosition.getX() + 1,
                        worldPosition.getY() + 0.5,
                        worldPosition.getZ() + 1);
        for (Player player : level.getEntitiesOfClass(Player.class, bounds)) {
            for (EquipmentSlot slot : EQUIPMENT) {
                ItemStack stack = player.getItemBySlot(slot);
                if (stack.isEmpty()) continue;

                if (fill(stack)) operating = true;
                if (!ArmorModHandler.isArmor(stack) || !ArmorModHandler.hasMods(stack)) continue;

                for (ItemStack mod : ArmorModHandler.pryMods(stack)) {
                    if (!fill(mod)) continue;
                    ArmorModHandler.applyMod(stack, mod);
                    operating = true;
                }
            }
        }

        if (operating) {
            if (operatingTime % 20 == 0) {
                level.playSound(
                        null,
                        worldPosition,
                        SoundEvents.FIRE_EXTINGUISH,
                        SoundSource.BLOCKS,
                        0.2F,
                        0.5F);
            }
            operatingTime++;
            setChanged();
        } else {
            operatingTime = 0;
        }

        networkPackNT(150);
    }

    @Override
    public void tickClient() {
        if (operating) CLIENT_PARTICLE.accept(this);

        prevFillLevel = fillLevel;
        double target = (double) tank.getFill() / tank.getMaxFill();
        fillLevel =
                BobMathUtil.interp(
                        fillLevel, target, target > fillLevel || !operating ? 0.1F : 0.01F);
    }

    private boolean fill(ItemStack stack) {
        if (!(stack.getItem() instanceof IFluidContainerItem container)
                || !container.machineFillable()) {
            return false;
        }
        Fluid fluid = tank.getFluid();
        if (fluid == null) return false;
        int accepted = container.fill(stack, fluid, tank.getFill(), tank.getPressure());
        if (accepted <= 0) return false;
        tank.setFill(tank.getFill() - accepted);
        return true;
    }

    public Direction receivingFace() {
        return getBlockState().getValue(BlockMachineHorizontal.FACING).getOpposite();
    }

    @Override
    public FluidTankNTM[] getReceivingTanks() {
        return receiving;
    }

    @Override
    public FluidTankNTM[] getAllTanks() {
        return receiving;
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.child("tank").ifPresent(tank::deserialize);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        tank.serialize(output.child("tank"));
    }

    @Override
    public long syncUnitMask() {
        return 0x3L;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeBoolean(operating);
            case 1 -> tank.packetSerialize(output);
            default -> throw new IllegalArgumentException();
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> operating = input.readBoolean();
            case 1 -> tank.packetDeserialize(input);
            default -> throw new IllegalArgumentException();
        }
    }
}
