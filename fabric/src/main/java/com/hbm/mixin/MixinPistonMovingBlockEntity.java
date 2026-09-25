// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin;

import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.interfaces.RigidPistonStructure;
import com.hbm.interfaces.StoredItems;
import com.hbm.interfaces.injected.MovedBlockEntityData;
import com.hbm.items.special.CarriedBlockItems;
import com.hbm.tileentity.BlockEntityMachineBase;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.piston.PistonMovingBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PistonMovingBlockEntity.class)
public abstract class MixinPistonMovingBlockEntity implements MovedBlockEntityData, StoredItems {

    @Unique private static final Logger HBM$LOGGER = LoggerFactory.getLogger("NTM");

    @Unique private static final String HBM$CARRIED = "hbmMovedData";

    @Unique private @Nullable CompoundTag hbm$carried;

    @Unique private @Nullable BlockEntity hbm$carriedInventory;

    @Override
    public void visitStoredItems(Visitor visitor) {
        if (hbm$carried == null || !(hbm$self().getLevel() instanceof ServerLevel server)) return;
        if (hbm$carriedInventory == null) {
            hbm$carriedInventory =
                    CarriedBlockItems.load(
                            getMovedState(),
                            hbm$self().getBlockPos(),
                            hbm$carried,
                            server.registryAccess());
        }
        if (hbm$carriedInventory != null && visitor.visit(hbm$carriedInventory)) {
            hbm$carried = hbm$carriedInventory.saveCustomOnly(server.registryAccess());
        }
    }

    @WrapMethod(method = "tick")
    private static void hbm$tickRigidly(
            Level level,
            BlockPos pos,
            BlockState state,
            PistonMovingBlockEntity entity,
            Operation<Void> original) {
        boolean rigid = entity.getMovedState().getBlock() instanceof RigidPistonStructure;
        boolean previous = BlockMultiblockCore.isBusy();
        if (rigid) BlockMultiblockCore.setBusy(true);
        try {
            original.call(level, pos, state, entity);
        } finally {
            if (rigid) BlockMultiblockCore.setBusy(previous);
        }
        if (rigid) entity.hbm$restoreCarried();
    }

    @Shadow
    public abstract BlockState getMovedState();

    @Unique
    private PistonMovingBlockEntity hbm$self() {
        return (PistonMovingBlockEntity) (Object) this;
    }

    @Override
    public void hbm$captureFrom(BlockEntity source) {
        Level level = source.getLevel();
        if (level == null) return;
        this.hbm$carried = source.saveCustomOnly(level.registryAccess());
        this.hbm$carriedInventory = null;
    }

    @Override
    public void hbm$restoreCarried() {
        if (this.hbm$carried == null) return;
        PistonMovingBlockEntity self = hbm$self();
        Level level = self.getLevel();
        if (level == null || level.isClientSide()) return;
        BlockPos pos = self.getBlockPos();

        if (!level.getBlockState(pos).is(getMovedState().getBlock())) return;
        BlockEntity landed = level.getBlockEntity(pos);
        if (landed == null) return;

        CompoundTag data = this.hbm$carried;
        this.hbm$carried = null;
        this.hbm$carriedInventory = null;
        try (ProblemReporter.ScopedCollector reporter =
                new ProblemReporter.ScopedCollector(landed.problemPath(), HBM$LOGGER)) {
            landed.loadCustomOnly(TagValueInput.create(reporter, level.registryAccess(), data));
        }
        landed.setChanged();
    }

    @WrapMethod(method = "finalTick")
    private void hbm$landRigidly(Operation<Void> original) {
        boolean rigid = getMovedState().getBlock() instanceof RigidPistonStructure;
        boolean previous = BlockMultiblockCore.isBusy();
        if (rigid) BlockMultiblockCore.setBusy(true);
        try {
            original.call();
        } finally {
            if (rigid) BlockMultiblockCore.setBusy(previous);
        }
        if (rigid) hbm$restoreCarried();
    }

    @WrapMethod(method = "preRemoveSideEffects")
    private void hbm$spillIfLost(BlockPos pos, BlockState state, Operation<Void> original) {
        original.call(pos, state);
        if (this.hbm$carried == null) return;

        CompoundTag data = this.hbm$carried;
        this.hbm$carried = null;
        this.hbm$carriedInventory = null;
        BlockState moved = getMovedState();
        if (!(hbm$self().getLevel() instanceof ServerLevel server)) return;
        if (!(moved.getBlock() instanceof EntityBlock entityBlock)) return;
        BlockEntity ghost = entityBlock.newBlockEntity(pos, moved);
        if (ghost == null) return;

        ghost.setLevel(server);
        try (ProblemReporter.ScopedCollector reporter =
                new ProblemReporter.ScopedCollector(ghost.problemPath(), HBM$LOGGER)) {
            ghost.loadCustomOnly(TagValueInput.create(reporter, server.registryAccess(), data));
        }
        if (ghost instanceof BlockEntityMachineBase machine) machine.spillInventory(pos);
        else if (ghost instanceof Container container)
            Containers.dropContents(server, pos, container);
    }

    @Inject(method = "saveAdditional", at = @At("RETURN"))
    private void hbm$saveCarried(ValueOutput output, CallbackInfo ci) {
        if (this.hbm$carried != null)
            output.store(HBM$CARRIED, CompoundTag.CODEC, this.hbm$carried);
    }

    @Inject(method = "loadAdditional", at = @At("RETURN"))
    private void hbm$loadCarried(ValueInput input, CallbackInfo ci) {
        this.hbm$carried = input.read(HBM$CARRIED, CompoundTag.CODEC).orElse(null);
        this.hbm$carriedInventory = null;
    }
}
