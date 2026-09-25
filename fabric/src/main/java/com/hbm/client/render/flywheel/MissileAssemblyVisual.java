// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.blocks.machine.BlockMachineHorizontal;
import com.hbm.client.model.Meshes;
import com.hbm.handler.MissileStruct;
import com.hbm.items.weapon.ItemCustomMissilePart.PartType;
import com.hbm.items.weapon.ItemCustomMissilePart;
import com.hbm.main.ResourceManager;
import com.hbm.tileentity.machine.BlockEntityMachineMissileAssembly;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

public final class MissileAssemblyVisual
        extends HbmDynamicBlockEntityVisual<BlockEntityMachineMissileAssembly>
        implements ShaderLightVisual {
    private static final Map<PartKey, MeshPart[]> PART_MODELS = new ConcurrentHashMap<>();
    private static final MeshPart[] STRUT_PARTS =
            MeshPart.objParts(
                    ResourceManager.strut,
                    SimpleMaterial.builderOf(Materials.CUTOUT_NO_CULL)
                            .texture(ResourceManager.strut_tex)
                            .mipmap(false)
                            .cutout(CutoutShaders.ONE_TENTH)
                            .light(LightShaders.SMOOTH)
                            .ambientOcclusion(false)
                            .cardinalLightingMode(CardinalLightingMode.CHUNK)
                            .build());

    private final AABB bodyBounds;
    private final Matrix4f rootPose = new Matrix4f();
    private final Matrix4f strutPose = new Matrix4f();
    private final Matrix4f missilePose = new Matrix4f();
    private final Matrix4f stackPose = new Matrix4f();
    private final Matrix4f instancePose = new Matrix4f();
    private @Nullable StrutSet strutSet;
    private int strutCount = -1;
    private @Nullable MissileSet missileSet;
    private @Nullable MissileStruct currentMissile;
    private MissileStruct trackedMissile;
    private float lastHeight;
    private boolean written;
    private @Nullable AABB lastLightBounds;

    public MissileAssemblyVisual(
            VisualizationContext context,
            BlockEntityMachineMissileAssembly blockEntity,
            float partialTick) {
        super(context, blockEntity, partialTick);
        bodyBounds = new AABB(pos).inflate(1);
        trackedMissile = blockEntity.loadedMissile;
        rootPose.translate(.5F, 0F, .5F)
                .rotateY(
                        Facing.yaw(blockState.getValue(BlockMachineHorizontal.FACING), 180)
                                * Mth.DEG_TO_RAD);
        writeFrame();
    }

    public static void initModels() {}

    private static boolean valid(@Nullable ItemCustomMissilePart part, PartType type) {
        return part != null && part.type == type && part.mesh != null && part.skin != null;
    }

    @Override
    protected void trackExtent() {
        if (blockEntity.loadedMissile == trackedMissile) return;
        trackedMissile = blockEntity.loadedMissile;
        lastLightBounds =
                LightBounds.sections(lightSections, getRenderBoundingBox(), lastLightBounds);
        refreshVisibleBounds();
    }

    @Override
    protected void frame(Context context) {
        writeFrame();
    }

    private void writeFrame() {
        MissileStruct source = trackedMissile;
        float height = source.height();
        if (written && source == currentMissile && lastHeight == height) return;
        int reach = (int) (height / 2F - 1F);
        int step = reach >= 2 ? 2 : 1;
        int slots = 0;
        for (int i = -reach; i <= reach; i += step) if (i != 0) slots++;
        if (slots != strutCount) {
            if (strutSet != null) strutSet.delete();
            strutSet = new StrutSet(slots);
            strutCount = slots;
        }
        if (source != currentMissile) {
            if (missileSet != null) missileSet.delete();
            currentMissile = source;
            missileSet = new MissileSet(source);
        }

        int slot = 0;
        for (int i = -reach; i <= reach; i += step) {
            if (i == 0) continue;
            strutSet.write(slot++, strutPose.set(rootPose).translate(i, 0F, 0F));
        }

        missilePose
                .set(rootPose)
                .translate(0F, 1.5F, 0F)
                .rotateZ((float) Math.PI)
                .translate(-height / 2F, 0F, 0F)
                .rotateX(-90F * Mth.DEG_TO_RAD)
                .rotateZ(-90F * Mth.DEG_TO_RAD);
        missileSet.write(missilePose);
        lastHeight = height;
        written = true;
    }

    @Override
    protected AABB getRenderBoundingBox() {
        int reach = (int) (trackedMissile.height() / 2F + 2F);
        return bodyBounds.minmax(
                new AABB(
                                pos.getX() - reach,
                                pos.getY(),
                                pos.getZ() - reach,
                                pos.getX() + reach + 1,
                                pos.getY() + 4,
                                pos.getZ() + reach + 1)
                        .inflate(1));
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        if (strutSet != null) strutSet.collect(consumer);
        if (missileSet != null) missileSet.collect(consumer);
    }

    @Override
    protected void _delete() {
        if (strutSet != null) strutSet.delete();
        if (missileSet != null) missileSet.delete();
    }

    private record PartKey(Identifier mesh, Identifier skin) {}

    private final class StrutSet {
        private final PartVisual[] parts;

        private StrutSet(int slots) {
            parts = new PartVisual[slots * STRUT_PARTS.length];
            for (int slot = 0; slot < slots; slot++) {
                for (int group = 0; group < STRUT_PARTS.length; group++) {
                    parts[slot * STRUT_PARTS.length + group] = new PartVisual(STRUT_PARTS[group]);
                }
            }
        }

        private void write(int slot, Matrix4f local) {
            int offset = slot * STRUT_PARTS.length;
            for (int group = 0; group < STRUT_PARTS.length; group++)
                parts[offset + group].write(local);
        }

        private void collect(Consumer<Instance> consumer) {
            for (var part : parts) part.collect(consumer);
        }

        private void delete() {
            for (var part : parts) part.delete();
        }
    }

    private final class MissileSet {
        private final List<MissilePartVisual> parts = new ArrayList<>();

        private MissileSet(MissileStruct source) {
            if (valid(source.thruster(), PartType.THRUSTER))
                parts.add(new MissilePartVisual(source.thruster()));
            if (valid(source.fuselage(), PartType.FUSELAGE)) {
                if (valid(source.fins(), PartType.FINS))
                    parts.add(new MissilePartVisual(source.fins()));
                parts.add(new MissilePartVisual(source.fuselage()));
            }
            if (valid(source.warhead(), PartType.WARHEAD))
                parts.add(new MissilePartVisual(source.warhead()));
        }

        private void write(Matrix4f base) {
            Matrix4f pose = stackPose.set(base);
            int index = 0;
            ItemCustomMissilePart thruster = currentMissile.thruster();
            if (valid(thruster, PartType.THRUSTER)) {
                parts.get(index++).write(pose);
                pose.translate(0F, thruster.height, 0F);
            }
            ItemCustomMissilePart fuselage = currentMissile.fuselage();
            if (valid(fuselage, PartType.FUSELAGE)) {
                ItemCustomMissilePart fins = currentMissile.fins();
                if (valid(fins, PartType.FINS)) parts.get(index++).write(pose);
                parts.get(index++).write(pose);
                pose.translate(0F, fuselage.height, 0F);
            }
            ItemCustomMissilePart warhead = currentMissile.warhead();
            if (valid(warhead, PartType.WARHEAD)) parts.get(index).write(pose);
        }

        private void collect(Consumer<Instance> consumer) {
            for (var part : parts) part.collect(consumer);
        }

        private void delete() {
            for (var part : parts) part.delete();
        }
    }

    private final class PartVisual {
        private final TransformedInstance instance;

        private PartVisual(MeshPart part) {
            instance =
                    instancerProvider()
                            .instancer(InstanceTypes.TRANSFORMED, part.model())
                            .createInstance();
        }

        private void write(Matrix4f local) {
            instancePose
                    .translation(visualPos.getX(), visualPos.getY(), visualPos.getZ())
                    .mul(local);
            instance.setTransform(instancePose).light(0).setChanged();
        }

        private void collect(Consumer<Instance> consumer) {
            consumer.accept(instance);
        }

        private void delete() {
            instance.delete();
        }
    }

    private final class MissilePartVisual {
        private final List<PartVisual> groups;

        private MissilePartVisual(ItemCustomMissilePart source) {
            MeshPart[] models =
                    PART_MODELS.computeIfAbsent(
                            new PartKey(source.mesh, source.skin),
                            key ->
                                    MeshPart.objParts(
                                            Meshes.faceNormals(key.mesh),
                                            MeshPart.litCutout(key.skin)));
            groups = new ArrayList<>(models.length);
            for (var model : models) groups.add(new PartVisual(model));
        }

        private void write(Matrix4f pose) {
            for (var group : groups) group.write(pose);
        }

        private void collect(Consumer<Instance> consumer) {
            for (var group : groups) group.collect(consumer);
        }

        private void delete() {
            for (var group : groups) group.delete();
        }
    }
}
