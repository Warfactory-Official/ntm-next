// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.main.ResourceManager;
import com.hbm.tileentity.machine.BlockEntityMachineSuperComputer;
import com.hbm.util.Facing;
import com.hbm.util.GameTime;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.material.CardinalLightingMode;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.UvTransformedInstance;
import dev.engine_room.flywheel.lib.material.CutoutShaders;
import dev.engine_room.flywheel.lib.material.LightShaders;
import dev.engine_room.flywheel.lib.material.Materials;
import dev.engine_room.flywheel.lib.material.SimpleMaterial;
import java.util.function.Consumer;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;

public final class SuperComputerVisual
        extends HbmDynamicBlockEntityVisual<BlockEntityMachineSuperComputer> {
    private static final MeshPart LIGHTS =
            MeshPart.obj(
                    ResourceManager.supercomputer
                            .groups[ResourceManager.supercomputer.partId("Lights")],
                    ResourceManager.supercomputer.smoothing(),
                    SimpleMaterial.builderOf(Materials.CUTOUT)
                            .texture(ResourceManager.supercomputer_scan_tex)
                            .mipmap(false)
                            .cutout(CutoutShaders.ONE_TENTH)
                            .light(LightShaders.NONE)
                            .useLight(false)
                            .useOverlay(false)
                            .ambientOcclusion(false)
                            .cardinalLightingMode(CardinalLightingMode.OFF)
                            .build());

    private final UvTransformedInstance lights;
    private final AABB bounds;
    private float lastScroll = Float.NaN;
    private boolean lastActive;

    public SuperComputerVisual(
            VisualizationContext context,
            BlockEntityMachineSuperComputer blockEntity,
            float partialTick) {
        super(context, blockEntity, partialTick);
        Matrix4f base =
                new Matrix4f()
                        .translation(.5F, 0F, .5F)
                        .rotateY(
                                Facing.yaw(BlockMultiblockCore.coreFacing(blockState), 180)
                                        * Mth.DEG_TO_RAD);
        Matrix4f world =
                new Matrix4f()
                        .translation(visualPos.getX(), visualPos.getY(), visualPos.getZ())
                        .mul(base);
        lights =
                instancerProvider()
                        .instancer(InstanceTypes.UV_TRANSFORMED, LIGHTS.model())
                        .createInstance();
        lights.setTransform(world).light(LightCoordsUtil.FULL_BRIGHT);
        bounds =
                new AABB(
                        pos.getX() - 8,
                        pos.getY(),
                        pos.getZ() - 8,
                        pos.getX() + 9,
                        pos.getY() + 9,
                        pos.getZ() + 9);
        writeFrame(partialTick);
    }

    public static void initModels() {}

    @Override
    protected void frame(Context context) {
        writeFrame(context.partialTick());
    }

    private void writeFrame(float partialTick) {
        float scroll =
                (float) -((GameTime.millis(blockEntity.getLevel()) + partialTick * 50D) % 1_000D)
                        / 1_000F;
        boolean active = blockEntity.didProcess;
        if (scroll == lastScroll && active == lastActive) return;
        lights.uvRegion(scroll, 0F, 1F, 1F)
                .colorArgb(active ? 0xFFFFFFFF : 0xFF000000)
                .setChanged();
        lastScroll = scroll;
        lastActive = active;
    }

    @Override
    protected AABB getRenderBoundingBox() {
        return bounds;
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {}

    @Override
    protected void _delete() {
        lights.delete();
    }
}
