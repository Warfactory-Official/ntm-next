// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.client.render.StructGhost;
import com.hbm.tileentity.machine.BlockEntitySoyuzStruct;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visual.ShaderLightVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.UvTransformedInstance;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.function.Consumer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;

public final class SoyuzStructPreviewVisual extends HbmBlockEntityVisual<BlockEntitySoyuzStruct>
        implements ShaderLightVisual {
    private final ArrayList<UvTransformedInstance> cubes = new ArrayList<>();
    private final AABB bounds;

    public SoyuzStructPreviewVisual(
            VisualizationContext context, BlockEntitySoyuzStruct blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);
        bounds =
                new AABB(
                                pos.getX() - 6,
                                pos.getY(),
                                pos.getZ() - 8,
                                pos.getX() + 7,
                                pos.getY() + 52,
                                pos.getZ() + 10)
                        .inflate(1);
        HashMap<Block, TextureAtlasSprite> sprites = new HashMap<>();
        Matrix4f pose = new Matrix4f();
        BlockEntitySoyuzStruct.forEachRequirement(
                (block, dx, dy, dz) -> {
                    var ghost =
                            new StructGhost(
                                    dx,
                                    dy,
                                    dz,
                                    sprites.computeIfAbsent(block, StructGhost::sprite));
                    var cube =
                            instancerProvider()
                                    .instancer(
                                            InstanceTypes.UV_TRANSFORMED, PreviewCube.SOUTH_MODEL)
                                    .createInstance();
                    var sprite = ghost.sprite();
                    cube.uvRegion(
                            sprite.getU0(),
                            sprite.getV0(),
                            sprite.getU1() - sprite.getU0(),
                            sprite.getV1() - sprite.getV0());
                    VisualTextures.animated(sprite);
                    pose.translation(
                            visualPos.getX() + ghost.x(),
                            visualPos.getY() + ghost.y(),
                            visualPos.getZ() + ghost.z());
                    cube.setTransform(pose).setChanged();
                    cubes.add(cube);
                });
    }

    @Override
    protected AABB getRenderBoundingBox() {
        return bounds;
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {}

    @Override
    protected void _delete() {
        for (UvTransformedInstance cube : cubes) cube.delete();
        cubes.clear();
    }
}
