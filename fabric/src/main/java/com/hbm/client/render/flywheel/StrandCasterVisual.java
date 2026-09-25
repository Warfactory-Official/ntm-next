// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.main.ResourceManager;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.tileentity.machine.BlockEntityMachineStrandCaster;
import com.hbm.util.Facing;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.material.CardinalLightingMode;
import dev.engine_room.flywheel.api.material.Material;
import dev.engine_room.flywheel.api.visual.ShaderLightVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.ClipTransformedInstance;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.material.CutoutShaders;
import dev.engine_room.flywheel.lib.material.LightShaders;
import dev.engine_room.flywheel.lib.material.Materials;
import dev.engine_room.flywheel.lib.material.SimpleMaterial;
import java.util.function.Consumer;
import net.minecraft.util.ARGB;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;

public final class StrandCasterVisual
        extends HbmDynamicBlockEntityVisual<BlockEntityMachineStrandCaster>
        implements ShaderLightVisual {
    private static final int PLATE = ResourceManager.strand_caster.partId("plate");
    private static final double STRAND_REST = 3.4D;
    private static final double STRAND_PER_CAST = .375D;
    private static final double MELT_DEPTH = .675D;
    private static final double MELT_FLOOR = 2.3D;
    private static final HFRWavefrontObject MODEL = ResourceManager.strand_caster;
    private static final Material PLATE_MATERIAL =
            SimpleMaterial.builderOf(Materials.CUTOUT_CLIP_HALFSPACE)
                    .texture(ResourceManager.strand_caster_tex)
                    .mipmap(false)
                    .light(LightShaders.SMOOTH)
                    .ambientOcclusion(false)
                    .cardinalLightingMode(CardinalLightingMode.OFF)
                    .backfaceCulling(false)
                    .build();
    private static final MeshPart PLATE_PART =
            MeshPart.obj(MODEL.groups[PLATE], MODEL.smoothing(), PLATE_MATERIAL);
    private static final MeshPart MELT_PART = buildMelt();
    private final ClipTransformedInstance plate;
    private final TransformedInstance melt;
    private final Matrix4f platePose = new Matrix4f();
    private final Matrix4f meltPose = new Matrix4f();
    private final Matrix4f plateBasePose = new Matrix4f();
    private final Matrix4f meltBasePose = new Matrix4f();
    private final Matrix4f instancePose = new Matrix4f();
    private final AABB rawBodyBounds;
    private double lastMeltHeight = Double.NaN;
    private double lastStrand = Double.NaN;
    private int lastColor;
    private boolean lastMolten;
    private boolean initialized;

    public StrandCasterVisual(
            VisualizationContext context,
            BlockEntityMachineStrandCaster blockEntity,
            float partialTick) {
        super(context, blockEntity, partialTick);
        float yaw = Facing.yaw(BlockMultiblockCore.coreFacing(blockEntity.getBlockState()), 0);
        var rawBodyLocal = new Matrix4f().translation(.5F, 0F, .5F).rotateY(yaw * Mth.DEG_TO_RAD);
        rawBodyBounds = LightBounds.of(MODEL, "caster", rawBodyLocal, pos);
        plate =
                instancerProvider()
                        .instancer(InstanceTypes.CLIP_TRANSFORMED, PLATE_PART.model())
                        .createInstance();
        melt =
                instancerProvider()
                        .instancer(InstanceTypes.TRANSFORMED, MELT_PART.model())
                        .createInstance();
        plateBasePose
                .identity()
                .translate(.5F, 0F, .5F)
                .rotateY((yaw) * Mth.DEG_TO_RAD)
                .translate(.5F, 0F, .5F)
                .rotateY((180F) * Mth.DEG_TO_RAD);
        meltBasePose
                .identity()
                .translate(.5F, 0F, .5F)
                .rotateY((yaw) * Mth.DEG_TO_RAD)
                .translate(.5F, (float) MELT_FLOOR, .5F)
                .rotateY((180F) * Mth.DEG_TO_RAD);
        plate.setVisible(false);
        melt.setVisible(false);
        updateMovingParts(partialTick);
    }

    public static void initModels() {}

    private static MeshPart buildMelt() {
        float[] vertices = {
            -.9F, 0F, -.999F, 0F, 0F, 0F, 1F, 0F,
            -.9F, 0F, .999F, 0F, 1F, 0F, 1F, 0F,
            .9F, 0F, .999F, 1F, 1F, 0F, 1F, 0F,
            .9F, 0F, -.999F, 1F, 0F, 0F, 1F, 0F
        };
        var meltMaterial =
                SimpleMaterial.builderOf(Materials.CUTOUT)
                        .texture(ResourceManager.foundry_stream_tex)
                        .mipmap(false)
                        .cutout(CutoutShaders.ONE_TENTH)
                        .light(LightShaders.NONE)
                        .ambientOcclusion(false)
                        .cardinalLightingMode(CardinalLightingMode.OFF)
                        .backfaceCulling(false)
                        .build();
        var meltMesh = PackedQuadMesh.of(vertices, new int[] {-1, -1, -1, -1}, new int[4]);
        return FoundryTankVisual.molten(MeshPart.create(meltMesh, meltMaterial));
    }

    @Override
    protected void frame(Context context) {
        updateMovingParts(context.partialTick());
    }

    public void updateMovingParts(float partialTick) {
        var mold = blockEntity.getInstalledMold();
        boolean molten = blockEntity.amount != 0 && blockEntity.type != null && mold != null;
        int color = molten ? ARGB.opaque(blockEntity.type.moltenColor) : 0;
        if (!molten) {
            if (initialized && !lastMolten) return;
            plate.setVisible(false);
            melt.setVisible(false);
            lastMolten = false;
            initialized = true;
            return;
        }
        double liquid = (double) blockEntity.amount / (double) blockEntity.getCapacity();
        double meltHeight = liquid * MELT_DEPTH;
        double strand =
                Math.max(
                        STRAND_REST
                                - (double) blockEntity.amount
                                        / (double) mold.getCost()
                                        * STRAND_PER_CAST,
                        0D);
        boolean visibilityChanged = !initialized || !lastMolten;
        boolean colorChanged = !initialized || color != lastColor;
        boolean plateChanged =
                visibilityChanged
                        || colorChanged
                        || Double.doubleToLongBits(strand) != Double.doubleToLongBits(lastStrand);
        boolean meltChanged =
                visibilityChanged
                        || colorChanged
                        || Double.doubleToLongBits(meltHeight)
                                != Double.doubleToLongBits(lastMeltHeight);
        if (visibilityChanged) {
            plate.setVisible(true);
            melt.setVisible(true);
        }
        if (plateChanged) {
            platePose.set(plateBasePose).translate(0F, 0F, (float) strand);
            instancePose
                    .translation(visualPos.getX(), visualPos.getY(), visualPos.getZ())
                    .mul(platePose);
            plate.setTransform(instancePose).colorArgb(color).light(0);
            float clip = (float) (.5D - strand);
            plate.setSlide(0F, 0F, 0F).setPlane(0F, 0F, 1F, clip).setChanged();
        }
        if (meltChanged) {
            meltPose.set(meltBasePose).translate(0F, (float) meltHeight, 0F);
            instancePose
                    .translation(visualPos.getX(), visualPos.getY(), visualPos.getZ())
                    .mul(meltPose);
            melt.setTransform(instancePose)
                    .colorArgb(color)
                    .light(LightCoordsUtil.FULL_BRIGHT)
                    .setChanged();
        }
        lastMeltHeight = meltHeight;
        lastStrand = strand;
        lastColor = color;
        lastMolten = true;
        initialized = true;
    }

    @Override
    protected AABB getRenderBoundingBox() {
        return rawBodyBounds.minmax(
                new AABB(
                        pos.getX() - 8,
                        pos.getY(),
                        pos.getZ() - 8,
                        pos.getX() + 9,
                        pos.getY() + 4,
                        pos.getZ() + 9));
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        consumer.accept(plate);
    }

    @Override
    protected void _delete() {
        plate.delete();
        melt.delete();
    }
}
