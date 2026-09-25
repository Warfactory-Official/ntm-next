// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.client.render.StructGhost;
import com.hbm.tileentity.machine.BlockEntityWatzStruct;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visual.ShaderLightVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.UvTransformedInstance;
import java.util.ArrayList;
import java.util.function.Consumer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;

public final class WatzStructPreviewVisual extends HbmBlockEntityVisual<BlockEntityWatzStruct>
        implements ShaderLightVisual {
    private final ArrayList<UvTransformedInstance> ghosts = new ArrayList<>();

    public WatzStructPreviewVisual(
            VisualizationContext context, BlockEntityWatzStruct blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);
        Matrix4f pose = new Matrix4f();
        BlockEntityWatzStruct.forEachRequirement(
                (block, dx, dy, dz) -> {
                    TextureAtlasSprite sprite = StructGhost.sprite(block);
                    UvTransformedInstance instance =
                            instancerProvider()
                                    .instancer(InstanceTypes.UV_TRANSFORMED, PreviewCube.MODEL)
                                    .createInstance();
                    instance.uvRegion(
                            sprite.getU0(),
                            sprite.getV0(),
                            sprite.getU1() - sprite.getU0(),
                            sprite.getV1() - sprite.getV0());
                    VisualTextures.animated(sprite);
                    pose.translation(visualPos.getX(), visualPos.getY(), visualPos.getZ())
                            .translate(dx, dy, dz);
                    instance.setTransform(pose).setChanged();
                    ghosts.add(instance);
                });
    }

    @Override
    protected AABB getRenderBoundingBox() {
        return new AABB(
                pos.getX() - 3,
                pos.getY() - 1,
                pos.getZ() - 3,
                pos.getX() + 4,
                pos.getY() + 4,
                pos.getZ() + 4);
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {}

    @Override
    protected void _delete() {
        for (UvTransformedInstance ghost : ghosts) ghost.delete();
    }
}
