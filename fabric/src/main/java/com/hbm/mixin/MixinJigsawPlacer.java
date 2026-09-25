// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin;

import com.hbm.world.gen.nbt.JigsawFreeSpace;
import com.hbm.world.gen.nbt.ParentOverlapPoolElement;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import java.util.IdentityHashMap;
import java.util.List;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.pools.alias.PoolAliasLookup;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.world.level.levelgen.structure.pools.JigsawPlacement$Placer")
public abstract class MixinJigsawPlacer {
    @Shadow @Final private List<? super PoolElementStructurePiece> pieces;
    @Unique private MutableObject<VoxelShape> hbm$outerFree;
    @Unique private @Nullable AABB hbm$outerBounds;

    @Unique
    private final IdentityHashMap<MutableObject<VoxelShape>, JigsawFreeSpace> hbm$freeSpace =
            new IdentityHashMap<>();

    @Inject(method = "tryPlacingChildren", at = @At("HEAD"))
    private void hbm$retainOuterSpace(
            PoolElementStructurePiece source,
            MutableObject<VoxelShape> free,
            int depth,
            boolean expansion,
            LevelHeightAccessor height,
            RandomState random,
            PoolAliasLookup aliases,
            LiquidSettings liquid,
            CallbackInfo ci) {
        if (depth == 0) {
            hbm$outerFree = free;
            hbm$outerBounds = free.get().isEmpty() ? null : free.get().bounds();
        }
    }

    @Unique
    private JigsawFreeSpace hbm$space(MutableObject<VoxelShape> holder) {
        return hbm$freeSpace.computeIfAbsent(holder, h -> JigsawFreeSpace.of(h.get()));
    }

    @WrapOperation(
            method = "tryPlacingChildren",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/phys/shapes/Shapes;joinIsNotEmpty(Lnet/minecraft/world/phys/shapes/VoxelShape;Lnet/minecraft/world/phys/shapes/VoxelShape;Lnet/minecraft/world/phys/shapes/BooleanOp;)Z"))
    private boolean hbm$allowDeclaredParentOverlap(
            VoxelShape free,
            VoxelShape proposed,
            BooleanOp operation,
            Operation<Boolean> original,
            @Local(argsOnly = true) PoolElementStructurePiece source,
            @Local(name = "sourceJigsaw") StructureTemplate.JigsawBlockInfo connector,
            @Local(name = "targetBB") BoundingBox candidate,
            @Local(name = "childrenFree") LocalRef<MutableObject<VoxelShape>> childSpace) {
        if (!(source.getElement() instanceof ParentOverlapPoolElement element)
                || !element.allowsParentOverlap(connector.name()))
            return !hbm$space(childSpace.get()).contains(candidate);
        if (hbm$outerBounds == null) return true;
        AABB box = proposed.bounds();
        if (box.minX < hbm$outerBounds.minX
                || box.minY < hbm$outerBounds.minY
                || box.minZ < hbm$outerBounds.minZ
                || box.maxX > hbm$outerBounds.maxX
                || box.maxY > hbm$outerBounds.maxY
                || box.maxZ > hbm$outerBounds.maxZ) return true;
        for (Object value : pieces) {
            PoolElementStructurePiece placed = (PoolElementStructurePiece) value;
            if (placed != source && placed.getBoundingBox().intersects(candidate)) return true;
        }

        childSpace.set(hbm$outerFree);
        return false;
    }

    @WrapOperation(
            method = "tryPlacingChildren",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/phys/shapes/Shapes;joinUnoptimized(Lnet/minecraft/world/phys/shapes/VoxelShape;Lnet/minecraft/world/phys/shapes/VoxelShape;Lnet/minecraft/world/phys/shapes/BooleanOp;)Lnet/minecraft/world/phys/shapes/VoxelShape;"))
    private VoxelShape hbm$subtractPlaced(
            VoxelShape free,
            VoxelShape placed,
            BooleanOp operation,
            Operation<VoxelShape> original,
            @Local(name = "targetBB") BoundingBox candidate,
            @Local(name = "childrenFree") LocalRef<MutableObject<VoxelShape>> childSpace) {
        hbm$space(childSpace.get()).subtract(candidate);
        return free;
    }
}
