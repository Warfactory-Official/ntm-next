// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.inventory.fluid.NTMFluidProperty;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.main.ResourceManager;
import com.hbm.render.util.BeamPronter.EnumWaveType;
import com.hbm.tileentity.machine.storage.BlockEntityMachineOrbus;
import com.hbm.util.GameTime;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.material.CardinalLightingMode;
import dev.engine_room.flywheel.api.material.Material;
import dev.engine_room.flywheel.api.visual.ShaderLightVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.material.CutoutShaders;
import dev.engine_room.flywheel.lib.material.LightShaders;
import dev.engine_room.flywheel.lib.material.Materials;
import dev.engine_room.flywheel.lib.material.SimpleMaterial;
import java.util.function.Consumer;
import net.minecraft.core.Direction;
import net.minecraft.util.CommonColors;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public final class OrbusVisual extends HbmDynamicBlockEntityVisual<BlockEntityMachineOrbus>
        implements ShaderLightVisual {
    private static final Vec3 BEAM = new Vec3(0D, 3D, 0D);
    private static final Material SPHERE_MATERIAL =
            SimpleMaterial.builderOf(Materials.CUTOUT)
                    .texture(ResourceManager.white_tex)
                    .mipmap(false)
                    .cutout(CutoutShaders.ONE_TENTH)
                    .light(LightShaders.SMOOTH)
                    .useOverlay(false)
                    .ambientOcclusion(false)
                    .cardinalLightingMode(CardinalLightingMode.OFF)
                    .build();
    private static final MeshPart[] SPHERE_PARTS =
            MeshPart.objParts(ResourceManager.sphere_uv, SPHERE_MATERIAL);
    private final TransformedInstance[] spheres;
    private final BeamVisual[] beams = new BeamVisual[3];
    private final Matrix4f bodyPose = new Matrix4f();
    private final Matrix4f spherePose = new Matrix4f();
    private final Matrix4f instancePose = new Matrix4f();
    private final Matrix4f beamPose = new Matrix4f();
    private final Matrix4f lastSpherePose = new Matrix4f();
    private boolean sphereStateKnown, sphereShown;
    private int lastFluidColor;

    public OrbusVisual(
            VisualizationContext context, BlockEntityMachineOrbus blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);
        Direction facing = blockState.getValue(BlockMultiblockCore.FACING);
        float offX = facing == Direction.NORTH || facing == Direction.WEST ? 1F : 0F;
        float offZ = facing == Direction.NORTH || facing == Direction.EAST ? 1F : 0F;
        bodyPose.translation(offX, 0F, offZ);
        beamPose.translation(offX, 1F, offZ);
        spheres = new TransformedInstance[SPHERE_PARTS.length];
        for (int i = 0; i < SPHERE_PARTS.length; i++)
            spheres[i] =
                    instancerProvider()
                            .instancer(InstanceTypes.TRANSFORMED, SPHERE_PARTS[i].model())
                            .createInstance();
        for (int i = 0; i < beams.length; i++)
            beams[i] = new BeamVisual(context, level, pos, false, 2, 1F);
        writeFrame(partialTick);
    }

    public static void initModels() {}

    @Override
    protected void frame(Context context) {
        writeFrame(context.partialTick());
    }

    private void writeFrame(float partialTick) {
        FluidTankNTM tank = blockEntity.tank;
        float scale = tank.getMaxFill() > 0 ? (float) tank.getFill() / tank.getMaxFill() : 0F;
        boolean hasFluid = tank.getFill() > 0;
        NTMFluidProperty property = hasFluid ? NTMFluidProperties.get(tank.getFluid()) : null;
        int fluidColor = property == null ? CommonColors.WHITE : property.colorARGB();

        if (!hasFluid) {
            if (!sphereStateKnown || sphereShown) {
                for (var sphere : spheres) sphere.setVisible(false);
                sphereStateKnown = true;
                sphereShown = false;
            }
            for (var beam : beams) beam.hide();
            return;
        }

        double ticks = GameTime.millis(blockEntity.getLevel()) / 50D + partialTick;
        float bob = (float) Math.sin(ticks * .1D % (Math.PI * 2D));
        spherePose
                .set(bodyPose)
                .translate(0F, (float) (2.5D + bob * .125D * scale), 0F)
                .scale(scale);
        boolean sphereChanged =
                !sphereStateKnown
                        || !sphereShown
                        || lastFluidColor != fluidColor
                        || !lastSpherePose.equals(spherePose);
        if (sphereChanged) {
            instancePose
                    .translation(visualPos.getX(), visualPos.getY(), visualPos.getZ())
                    .mul(spherePose);
            for (var sphere : spheres) {
                if (!sphereShown) sphere.setVisible(true);
                sphere.setTransform(instancePose).colorArgb(fluidColor).light(0).setChanged();
            }
            lastSpherePose.set(spherePose);
            lastFluidColor = fluidColor;
            sphereShown = true;
            sphereStateKnown = true;
        }

        int tick = (int) ticks;
        beams[0].update(
                beamPose, BEAM, EnumWaveType.SPIRAL, 0, 1, 0F, scale * .5F, 0x101020, 0x101020);
        beams[1].update(
                beamPose,
                BEAM,
                EnumWaveType.RANDOM,
                (int) (tick / 2L % 1000L),
                6,
                scale,
                .0625F * scale,
                0x202060,
                0x202060);
        beams[2].update(
                beamPose,
                BEAM,
                EnumWaveType.RANDOM,
                (int) (tick / 4L % 1000L),
                6,
                scale,
                .0625F * scale,
                0x202060,
                0x202060);
    }

    @Override
    protected AABB getRenderBoundingBox() {
        return new AABB(
                        pos.getX() - 2,
                        pos.getY(),
                        pos.getZ() - 2,
                        pos.getX() + 3,
                        pos.getY() + 5,
                        pos.getZ() + 3)
                .inflate(1);
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        for (var sphere : spheres) consumer.accept(sphere);
    }

    @Override
    protected void _delete() {
        for (var sphere : spheres) sphere.delete();
        for (var beam : beams) beam.delete();
    }
}
