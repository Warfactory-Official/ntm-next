// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.blocks.machine.BlockICFStruct;
import com.hbm.tileentity.machine.BlockEntityICFStruct;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visual.ShaderLightVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.UvTransformedInstance;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;

public final class ICFStructPreviewVisual extends HbmBlockEntityVisual<BlockEntityICFStruct>
        implements ShaderLightVisual {
    private final List<UvTransformedInstance> ghosts = new ArrayList<>();

    public ICFStructPreviewVisual(
            VisualizationContext context, BlockEntityICFStruct blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);
        Direction facing = blockState.getValue(BlockICFStruct.FACING);
        Matrix4f pose = new Matrix4f();
        BlockEntityICFStruct.forEachRequirement(
                (block, widthwise, y, lengthwise) -> {
                    BlockPos target = blockEntity.requirementPos(facing, widthwise, y, lengthwise);
                    TextureAtlasSprite sprite = sprite(block);
                    var instance =
                            instancerProvider()
                                    .instancer(InstanceTypes.UV_TRANSFORMED, PreviewCube.MODEL)
                                    .createInstance();
                    instance.uvRegion(
                            sprite.getU0(),
                            sprite.getV0(),
                            sprite.getU1() - sprite.getU0(),
                            sprite.getV1() - sprite.getV0());
                    VisualTextures.animated(sprite);
                    pose.translation(
                            visualPos.getX() + target.getX() - pos.getX(),
                            visualPos.getY() + target.getY() - pos.getY(),
                            visualPos.getZ() + target.getZ() - pos.getZ());
                    instance.setTransform(pose).setChanged();
                    ghosts.add(instance);
                });
    }

    private static TextureAtlasSprite sprite(Block block) {
        return Minecraft.getInstance()
                .getModelManager()
                .getBlockStateModelSet()
                .getParticleMaterial(block.defaultBlockState())
                .sprite();
    }

    @Override
    protected AABB getRenderBoundingBox() {
        return new AABB(
                pos.getX() - 9,
                pos.getY() - 1,
                pos.getZ() - 9,
                pos.getX() + 10,
                pos.getY() + 7,
                pos.getZ() + 10);
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {}

    @Override
    protected void _delete() {
        for (var ghost : ghosts) ghost.delete();
    }
}
