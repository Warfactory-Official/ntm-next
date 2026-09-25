// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.main.ResourceManager;
import com.hbm.tileentity.machine.BlockEntityCrucible.PourStream;
import com.hbm.tileentity.machine.BlockEntityMachineArcFurnace;
import com.hbm.util.Facing;
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
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.Direction;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;

public final class ArcFurnaceVisual
        extends HbmDynamicBlockEntityVisual<BlockEntityMachineArcFurnace>
        implements ShaderLightVisual {
    private static final int HOT = 0, COLD = 1, LID = 2;
    private static final int RING = 3, FRESH = 6, USED = 9, SHORT = 12, CABLE = 15;
    private static final int[] RING_GROUP = electrodeParts("Ring", "");
    private static final int[] FRESH_GROUP = electrodeParts("Electrode", "");
    private static final int[] USED_GROUP = electrodeParts("Electrode", "Hot");
    private static final int[] SHORT_GROUP = electrodeParts("Electrode", "Short");
    private static final int[] CABLE_GROUP = electrodeParts("Cable", "");
    private static final float[] CABLE_OFFSET = {.5F, 0F, -.5F};
    private static final float MAX_AGE = 20F;
    private static final float LID_TRAVEL = 2F;
    private static final Material BODY_MATERIAL =
            MeshPart.litCutout(ResourceManager.arc_furnace_tex);
    private static final Material HOT_MATERIAL =
            SimpleMaterial.builderOf(Materials.CUTOUT)
                    .texture(ResourceManager.arc_furnace_tex)
                    .mipmap(false)
                    .cutout(CutoutShaders.ONE_TENTH)
                    .light(LightShaders.NONE)
                    .ambientOcclusion(false)
                    .cardinalLightingMode(CardinalLightingMode.OFF)
                    .backfaceCulling(false)
                    .build();
    private static final MeshPart[] PARTS = buildParts();
    private final TransformedInstance[] instances = new TransformedInstance[18];
    private final AABB bodyBounds;
    private final List<PourVisual> pours = new ArrayList<>();
    private final Direction facing;
    private final Matrix4f basePose = new Matrix4f();
    private final Matrix4f hotPose = new Matrix4f();
    private final Matrix4f lidPose = new Matrix4f();
    private final Matrix4f[] cablePoses = {new Matrix4f(), new Matrix4f(), new Matrix4f()};
    private final Matrix4f instancePose = new Matrix4f();
    private final byte[] lastElectrodes = {Byte.MIN_VALUE, Byte.MIN_VALUE, Byte.MIN_VALUE};
    private double lastLidLift = Double.NaN;
    private int lastLiquidAmount = Integer.MIN_VALUE;
    private double lastWobble = Double.NaN;
    private boolean lastProgressing;
    private boolean lastHasMaterial;
    private boolean lastLiquidsEmpty;
    private boolean initialized;

    public ArcFurnaceVisual(
            VisualizationContext context,
            BlockEntityMachineArcFurnace blockEntity,
            float partialTick) {
        super(context, blockEntity, partialTick);
        facing = BlockMultiblockCore.coreFacing(blockEntity.getBlockState());
        basePose.translation(.5F, 0F, .5F).rotateY(Facing.yaw(facing, 90) * Mth.DEG_TO_RAD);
        bodyBounds =
                LightBounds.of(ResourceManager.arc_furnace, "Furnace", basePose, pos)
                        .minmax(
                                LightBounds.of(
                                        ResourceManager.arc_furnace,
                                        "Lid",
                                        new Matrix4f(basePose).translate(0F, LID_TRAVEL, 0F),
                                        pos));
        for (int i = 0; i < instances.length; i++) {
            instances[i] =
                    instancerProvider()
                            .instancer(InstanceTypes.TRANSFORMED, PARTS[i].model())
                            .createInstance();
            instances[i].setVisible(false);
        }
        updateMovingParts(partialTick);
    }

    public static void initModels() {}

    private static MeshPart[] buildParts() {
        var model = ResourceManager.arc_furnace;
        var parts = new MeshPart[18];
        parts[HOT] =
                FoundryTankVisual.molten(
                        MeshPart.obj(
                                model.groups[model.partId("ContentsHot")],
                                model.smoothing(),
                                HOT_MATERIAL));
        parts[COLD] =
                MeshPart.obj(
                        model.groups[model.partId("ContentsCold")],
                        model.smoothing(),
                        BODY_MATERIAL);
        parts[LID] =
                MeshPart.obj(model.groups[model.partId("Lid")], model.smoothing(), BODY_MATERIAL);
        for (int i = 0; i < 3; i++) {
            parts[RING + i] =
                    MeshPart.obj(model.groups[RING_GROUP[i]], model.smoothing(), BODY_MATERIAL);
            parts[FRESH + i] =
                    MeshPart.obj(model.groups[FRESH_GROUP[i]], model.smoothing(), BODY_MATERIAL);
            parts[USED + i] =
                    MeshPart.obj(model.groups[USED_GROUP[i]], model.smoothing(), HOT_MATERIAL);
            parts[SHORT + i] =
                    MeshPart.obj(model.groups[SHORT_GROUP[i]], model.smoothing(), HOT_MATERIAL);
            parts[CABLE + i] =
                    MeshPart.obj(model.groups[CABLE_GROUP[i]], model.smoothing(), BODY_MATERIAL);
        }
        return parts;
    }

    private static int[] electrodeParts(String stem, String suffix) {
        int[] ids = new int[3];
        for (int i = 0; i < 3; i++)
            ids[i] = ResourceManager.arc_furnace.partId(stem + (i + 1) + suffix);
        return ids;
    }

    @Override
    protected void frame(Context context) {
        updateMovingParts(context.partialTick());
    }

    public void updateMovingParts(float partialTick) {
        float lidLift = Mth.lerp(partialTick, blockEntity.prevLid, blockEntity.lid);
        boolean progressing = blockEntity.isProgressing;
        boolean hasMaterial = blockEntity.hasMaterial;
        boolean liquidsEmpty = blockEntity.liquids.isEmpty();
        int liquidAmount = BlockEntityMachineArcFurnace.getStackAmount(blockEntity.liquids);
        double wobble = level.getGameTime() + partialTick;
        boolean electrodeChanged = !initialized;
        for (int i = 0; i < 3; i++)
            electrodeChanged |= blockEntity.electrodes[i] != lastElectrodes[i];
        boolean contentsChanged =
                !initialized
                        || liquidAmount != lastLiquidAmount
                        || hasMaterial != lastHasMaterial
                        || liquidsEmpty != lastLiquidsEmpty;
        boolean lidChanged =
                !initialized
                        || lidLift != lastLidLift
                        || electrodeChanged
                        || progressing != lastProgressing
                        || progressing && wobble != lastWobble;
        if (contentsChanged) {
            lastLiquidAmount = liquidAmount;
            lastHasMaterial = hasMaterial;
            lastLiquidsEmpty = liquidsEmpty;
            hotPose.set(basePose)
                    .translate(
                            0F,
                            (float)
                                    (-1.75D
                                            + liquidAmount
                                                    * 1.75D
                                                    / (double)
                                                            BlockEntityMachineArcFurnace
                                                                    .MAX_LIQUID),
                            0F);
            write(HOT, !liquidsEmpty, hotPose, LightCoordsUtil.FULL_BRIGHT);
            write(COLD, liquidsEmpty && hasMaterial, basePose, 0);
        }
        if (lidChanged) {
            lastLidLift = lidLift;
            lastProgressing = progressing;
            lastWobble = wobble;
            for (int i = 0; i < 3; i++) lastElectrodes[i] = blockEntity.electrodes[i];
            lidPose.set(basePose).translate(0F, LID_TRAVEL * lidLift, 0F);
            if (progressing) lidPose.translate(0F, 0F, (float) (Math.sin(wobble) * .005D));
            for (int i = 0; i < 3; i++) {
                float offset = CABLE_OFFSET[i];
                cablePoses[i].set(lidPose).translate(0F, 5.5F, offset);
                if (progressing)
                    cablePoses[i].rotateX((float) (Math.sin(wobble / 2D) * 30D * Mth.DEG_TO_RAD));
                cablePoses[i].translate(0F, -5.5F, -offset);
            }
            write(LID, true, lidPose, 0);
            for (int i = 0; i < 3; i++) {
                byte electrode = blockEntity.electrodes[i];
                boolean present = electrode != BlockEntityMachineArcFurnace.ELECTRODE_NONE;
                write(RING + i, present, lidPose, 0);
                write(
                        FRESH + i,
                        electrode == BlockEntityMachineArcFurnace.ELECTRODE_FRESH,
                        lidPose,
                        0);
                write(
                        USED + i,
                        electrode == BlockEntityMachineArcFurnace.ELECTRODE_USED,
                        lidPose,
                        LightCoordsUtil.FULL_BRIGHT);
                write(
                        SHORT + i,
                        electrode == BlockEntityMachineArcFurnace.ELECTRODE_DEPLETED,
                        lidPose,
                        LightCoordsUtil.FULL_BRIGHT);
                write(CABLE + i, present, cablePoses[i], 0);
            }
        }
        initialized = true;

        long now = level.getGameTime();
        int slot = 0;
        for (PourStream stream : blockEntity.streams) {
            while (pours.size() <= slot)
                pours.add(new PourVisual(visualizationContext, level, pos));
            float age = (float) (now - stream.birth()) + partialTick;
            pours.get(slot++)
                    .update(
                            stream.color(),
                            facing,
                            stream.len(),
                            age,
                            .625F,
                            .625F,
                            (float) (.5D + facing.getStepX() * 2.875D),
                            1F,
                            (float) (.5D + facing.getStepZ() * 2.875D));
        }
        while (slot < pours.size()) {
            pours.get(slot++)
                    .update(
                            0,
                            facing,
                            0F,
                            MAX_AGE,
                            .625F,
                            .625F,
                            (float) (.5D + facing.getStepX() * 2.875D),
                            1F,
                            (float) (.5D + facing.getStepZ() * 2.875D));
        }
    }

    private void write(int index, boolean visible, Matrix4f pose, int light) {
        TransformedInstance instance = instances[index];
        instance.setVisible(visible);
        if (!visible) return;
        instancePose.translation(visualPos.getX(), visualPos.getY(), visualPos.getZ()).mul(pose);
        instance.setTransform(instancePose).light(light).setChanged();
    }

    @Override
    protected AABB getRenderBoundingBox() {
        return bodyBounds.minmax(
                new AABB(
                        pos.getX() - 4,
                        pos.getY() - 6,
                        pos.getZ() - 4,
                        pos.getX() + 5,
                        pos.getY() + 7,
                        pos.getZ() + 5));
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        consumer.accept(instances[COLD]);
        consumer.accept(instances[LID]);
        for (int i = 0; i < 3; i++) {
            consumer.accept(instances[RING + i]);
            consumer.accept(instances[FRESH + i]);
            consumer.accept(instances[CABLE + i]);
        }
    }

    @Override
    protected void _delete() {
        for (TransformedInstance instance : instances) instance.delete();
        for (PourVisual pour : pours) pour.delete();
    }
}
