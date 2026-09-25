// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.blocks.machine.BlockMachineHorizontal;
import com.hbm.main.ResourceManager;
import com.hbm.tileentity.machine.BlockEntityMachineTapeDrive;
import com.hbm.util.Facing;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.material.CardinalLightingMode;
import dev.engine_room.flywheel.api.visual.ShaderLightVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.material.CutoutShaders;
import dev.engine_room.flywheel.lib.material.LightShaders;
import dev.engine_room.flywheel.lib.material.Materials;
import dev.engine_room.flywheel.lib.material.SimpleMaterial;
import java.util.Arrays;
import java.util.function.Consumer;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;

public final class TapeDriveVisual extends HbmDynamicBlockEntityVisual<BlockEntityMachineTapeDrive>
        implements ShaderLightVisual {
    private static final MeshPart DRIVE =
            MeshPart.obj(
                    ResourceManager.tape_drive.groups[ResourceManager.tape_drive.partId("Drive")],
                    ResourceManager.tape_drive.smoothing(),
                    MeshPart.litCutout(ResourceManager.tape_drive_tex));
    private static final MeshPart LIGHT =
            MeshPart.obj(
                    ResourceManager.tape_drive.groups[ResourceManager.tape_drive.partId("Light")],
                    ResourceManager.tape_drive.smoothing(),
                    SimpleMaterial.builderOf(Materials.CUTOUT)
                            .texture(ResourceManager.white_tex)
                            .mipmap(false)
                            .cutout(CutoutShaders.ONE_TENTH)
                            .light(LightShaders.NONE)
                            .useLight(false)
                            .useOverlay(false)
                            .ambientOcclusion(false)
                            .cardinalLightingMode(CardinalLightingMode.OFF)
                            .build());
    private static final int RED = 0xFFFF0000;
    private static final int ORANGE = 0xFFFFBF00;
    private static final int GREEN = 0xFF00FF00;

    private final TransformedInstance[] drives =
            new TransformedInstance[BlockEntityMachineTapeDrive.SLOT_COUNT];
    private final TransformedInstance[] lights =
            new TransformedInstance[BlockEntityMachineTapeDrive.SLOT_COUNT];
    private final int[] categories = new int[BlockEntityMachineTapeDrive.SLOT_COUNT];

    public TapeDriveVisual(
            VisualizationContext context,
            BlockEntityMachineTapeDrive blockEntity,
            float partialTick) {
        super(context, blockEntity, partialTick);
        Arrays.fill(categories, -1);
        Matrix4f base =
                new Matrix4f()
                        .translation(.5F, 0F, .5F)
                        .rotateY(
                                Facing.yaw(blockState.getValue(BlockMachineHorizontal.FACING), 90)
                                        * Mth.DEG_TO_RAD);
        for (int i = 0; i < drives.length; i++) {
            Matrix4f pose =
                    new Matrix4f()
                            .translation(visualPos.getX(), visualPos.getY(), visualPos.getZ())
                            .mul(base)
                            .translate(0F, .25F - .5F * (i / 6), .3125F - (i % 6) * .125F);
            drives[i] =
                    instancerProvider()
                            .instancer(InstanceTypes.TRANSFORMED, DRIVE.model())
                            .createInstance();
            lights[i] =
                    instancerProvider()
                            .instancer(InstanceTypes.TRANSFORMED, LIGHT.model())
                            .createInstance();
            drives[i].setTransform(pose).light(0);
            lights[i].setTransform(pose).light(LightCoordsUtil.FULL_BRIGHT);
        }
        writeFrame();
    }

    public static void initModels() {}

    @Override
    protected void frame(Context context) {
        writeFrame();
    }

    private void writeFrame() {
        for (int i = 0; i < drives.length; i++) {
            int category = blockEntity.category(i);
            if (category == categories[i]) continue;
            boolean visible = category != BlockEntityMachineTapeDrive.VISUAL_EMPTY;
            drives[i].setVisible(visible);
            drives[i].setChanged();
            lights[i].setVisible(visible);
            lights[i]
                    .colorArgb(
                            switch (category) {
                                case BlockEntityMachineTapeDrive.VISUAL_BLANK -> ORANGE;
                                case BlockEntityMachineTapeDrive.VISUAL_FILLED -> GREEN;
                                default -> RED;
                            })
                    .setChanged();
            categories[i] = category;
        }
    }

    @Override
    protected AABB getRenderBoundingBox() {
        return new AABB(pos);
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        for (TransformedInstance drive : drives) consumer.accept(drive);
    }

    @Override
    protected void _delete() {
        for (int i = 0; i < drives.length; i++) {
            drives[i].delete();
            lights[i].delete();
        }
    }
}
