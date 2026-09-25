// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin;

import com.hbm.NuclearTech;
import com.hbm.blocks.IBlockHighlight;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.client.render.CullableRenderer;
import com.hbm.client.render.MultiblockCrumbling;
import com.hbm.client.render.MultiblockOutline;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import java.util.List;
import java.util.SortedSet;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.extract.LevelExtractor;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.BlockBreakingRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.BlockDestructionProgress;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelExtractor.class)
public class MixinLevelExtractor {
    @Shadow private ClientLevel level;
    @Shadow @Final private LevelRenderState levelRenderState;

    private static final String BLOCK_ENTITIES =
            "extractVisibleBlockEntities(Lnet/minecraft/client/Camera;FLnet/minecraft/client/renderer/state/level/LevelRenderState;Lnet/minecraft/client/renderer/culling/Frustum;)V";
    private static final String EXTRACT_ENTITY =
            "Lnet/minecraft/client/renderer/blockentity/BlockEntityRenderDispatcher;tryExtractRenderState(Lnet/minecraft/world/level/block/entity/BlockEntity;FLnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;ZLnet/minecraft/client/renderer/culling/Frustum;)Lnet/minecraft/client/renderer/blockentity/state/BlockEntityRenderState;";

    @ModifyExpressionValue(
            method = "extractBlockOutline",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/level/block/state/BlockState;getShape(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/phys/shapes/CollisionContext;)Lnet/minecraft/world/phys/shapes/VoxelShape;"))
    private VoxelShape hbm$machineOutline(
            VoxelShape original,
            @Local BlockPos pos,
            @Local BlockState state,
            @Local CollisionContext context) {
        if (!MultiblockSurface.isSurface(state) && !(state.getBlock() instanceof IBlockHighlight))
            return original;
        return MultiblockOutline.shape(level, pos, state, context, original);
    }

    @Inject(method = BLOCK_ENTITIES, at = @At("HEAD"))
    private void hbm$collectCrumbling(CallbackInfo ci) {
        MultiblockCrumbling.collect(level);
    }

    @SuppressWarnings("unchecked")
    @ModifyExpressionValue(
            method = BLOCK_ENTITIES,
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lit/unimi/dsi/fastutil/longs/Long2ObjectMap;get(J)Ljava/lang/Object;"))
    private Object hbm$coreProgress(Object original, @Local BlockPos blockPos) {
        return MultiblockCrumbling.progressAt(
                blockPos.asLong(), (SortedSet<BlockDestructionProgress>) original);
    }

    @ModifyArg(
            method = BLOCK_ENTITIES,
            at = @At(value = "INVOKE", target = EXTRACT_ENTITY, ordinal = 1),
            index = 2)
    private ModelFeatureRenderer.@Nullable CrumblingOverlay hbm$globalProgress(
            ModelFeatureRenderer.@Nullable CrumblingOverlay original,
            @Local BlockEntity blockEntity,
            @Local(argsOnly = true) Camera camera) {
        return MultiblockCrumbling.globalOverlay(blockEntity, camera, level);
    }

    @WrapOperation(
            method = "extractBlockDestroyAnimation",
            at = @At(value = "INVOKE", target = "Ljava/util/List;add(Ljava/lang/Object;)Z"))
    private boolean hbm$coreBreaking(
            List<BlockBreakingRenderState> states, Object state, Operation<Boolean> original) {
        BlockBreakingRenderState mined = (BlockBreakingRenderState) state;
        if (!MultiblockCrumbling.remaps(mined)) return original.call(states, mined);
        MultiblockCrumbling.extractOwner(mined, level, owner -> original.call(states, owner));
        return true;
    }

    @WrapOperation(
            method = "isEntityVisible",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/client/renderer/LevelRenderer;isSectionCompiledAndVisible(Lnet/minecraft/core/BlockPos;)Z"))
    private boolean hbm$unculledEntity(
            LevelRenderer renderer,
            BlockPos pos,
            Operation<Boolean> original,
            @Local(argsOnly = true) Entity entity) {
        if (original.call(renderer, pos)) return true;
        return NuclearTech.MOD_ID.equals(
                        BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).getNamespace())
                && !((CullableRenderer) renderer.entityRenderDispatcher().getRenderer(entity))
                        .hbm$affectedByCulling(entity);
    }

    @Inject(method = "setLevel", at = @At("HEAD"))
    private void hbm$releaseGeometry(ClientLevel next, CallbackInfo ci) {
        if (level != null && level != next) level.hbm$sectionGeometryIndex().clear();
    }

    @Inject(
            method = "extract",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/client/multiplayer/ClientChunkCache;flipUpdateTrackingSets()V",
                            shift = At.Shift.AFTER))
    private void hbm$includeGeometry(CallbackInfo ci) {
        level.hbm$sectionGeometryIndex()
                .emptySections(level, levelRenderState.chunkLoadingRenderState);
    }
}
