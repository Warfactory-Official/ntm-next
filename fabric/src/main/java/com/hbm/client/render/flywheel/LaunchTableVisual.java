// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.handler.MissileStruct;
import com.hbm.items.weapon.ItemCustomMissilePart.PartSize;
import com.hbm.items.weapon.ItemCustomMissilePart.PartType;
import com.hbm.items.weapon.ItemCustomMissilePart;
import com.hbm.main.ResourceManager;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.tileentity.bomb.BlockEntityLaunchTable;
import com.hbm.util.Facing;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visual.SectionTrackedVisual;
import dev.engine_room.flywheel.api.visual.ShaderLightVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

public final class LaunchTableVisual extends HbmDynamicBlockEntityVisual<BlockEntityLaunchTable>
        implements ShaderLightVisual {
    private static final double MISSILE_Y = 2.0625D;
    private static final Map<PartKey, MeshPart[]> PART_MODELS = new ConcurrentHashMap<>();
    private static final MeshPart LARGE_PAD =
            part(
                    ResourceManager.launch_table_large_pad,
                    ResourceManager.launch_table_large_pad_tex);
    private static final MeshPart SMALL_PAD =
            part(
                    ResourceManager.launch_table_small_pad,
                    ResourceManager.launch_table_small_pad_tex);
    private static final MeshPart SMALL_BASE =
            part(
                    ResourceManager.launch_table_small_scaffold_base,
                    ResourceManager.launch_table_small_scaffold_base_tex);
    private static final MeshPart LARGE_BASE =
            part(
                    ResourceManager.launch_table_large_scaffold_base,
                    ResourceManager.launch_table_large_scaffold_base_tex);
    private static final MeshPart SMALL_CONNECTOR =
            part(
                    ResourceManager.launch_table_small_scaffold_connector,
                    ResourceManager.launch_table_small_scaffold_connector_tex);
    private static final MeshPart LARGE_CONNECTOR =
            part(
                    ResourceManager.launch_table_large_scaffold_connector,
                    ResourceManager.launch_table_large_scaffold_connector_tex);
    private static final MeshPart SMALL_EMPTY =
            part(
                    ResourceManager.launch_table_small_scaffold_empty,
                    ResourceManager.launch_table_small_scaffold_base_tex);
    private static final MeshPart LARGE_EMPTY =
            part(
                    ResourceManager.launch_table_large_scaffold_empty,
                    ResourceManager.launch_table_large_scaffold_base_tex);
    private final Matrix4f rootPose = new Matrix4f();
    private final Matrix4f missilePose = new Matrix4f();
    private final Matrix4f scaffoldPose = new Matrix4f();
    private final Matrix4f stackPose = new Matrix4f();
    private final Matrix4f instancePose = new Matrix4f();
    private final double[] lightBoundsAccumulator = new double[6];
    private final AABB anchorBounds;

    private int height;
    private MissileStruct trackedMissile;
    private @Nullable Key currentKey;
    private @Nullable Definition definition;
    private @Nullable AABB lastLightBounds;

    public LaunchTableVisual(
            VisualizationContext context, BlockEntityLaunchTable blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);
        anchorBounds = new AABB(pos);
        trackedMissile = blockEntity.loadedMissile;
        height =
                trackedMissile.fuselage() != null
                        ? (int) trackedMissile.height()
                        : blockEntity.height;
        rootPose.translate(.5F, 0F, .5F)
                .rotateY(
                        Facing.yaw(BlockMultiblockCore.coreFacing(blockState), 90)
                                * Mth.DEG_TO_RAD);
        writeFrame();
    }

    public static void initModels() {}

    private static MeshPart part(HFRWavefrontObject mesh, Identifier texture) {
        return MeshPart.obj(mesh.groups[0], mesh.smoothing(), MeshPart.litCutout(texture));
    }

    private static boolean valid(@Nullable ItemCustomMissilePart part, PartType type) {
        return part != null && part.type == type && part.mesh != null && part.skin != null;
    }

    @Override
    protected void trackExtent() {
        MissileStruct missile = blockEntity.loadedMissile;
        if (missile == trackedMissile) return;
        trackedMissile = missile;
        if (missile.fuselage() == null || (int) missile.height() == height) return;
        height = (int) missile.height();
        refreshVisibleBounds();
    }

    @Override
    protected void frame(Context context) {
        writeFrame();
    }

    @Override
    public void setSectionCollector(SectionTrackedVisual.SectionCollector collector) {
        super.setSectionCollector(collector);
        lastLightBounds =
                LightBounds.sections(lightSections, lightBoundsAccumulator, lastLightBounds);
    }

    private void writeFrame() {
        MissileStruct missile = trackedMissile;
        if (currentKey != null
                && currentKey.size == blockEntity.padSize
                && currentKey.height == height
                && currentKey.valid == blockEntity.missileValid
                && currentKey.parts == missile) return;
        if (definition != null) definition.delete();
        Key wanted = new Key(blockEntity.padSize, height, blockEntity.missileValid, missile);
        currentKey = wanted;
        definition = build(wanted);
        LightBounds.resetBounds(lightBoundsAccumulator, anchorBounds);
        definition.write(rootPose, missile);
        lastLightBounds =
                LightBounds.sections(lightSections, lightBoundsAccumulator, lastLightBounds);
    }

    private Definition build(Key key) {
        boolean largePad = key.size == PartSize.SIZE_20;
        boolean smallScaffold = key.size == PartSize.SIZE_10;
        ArrayList<PartVisual> parts = new ArrayList<>();
        parts.add(new PartVisual(largePad ? LARGE_PAD : SMALL_PAD));
        MeshPart base = smallScaffold ? SMALL_BASE : LARGE_BASE;
        MeshPart connector = smallScaffold ? SMALL_CONNECTOR : LARGE_CONNECTOR;
        MeshPart empty = smallScaffold ? SMALL_EMPTY : LARGE_EMPTY;
        int split = (int) (key.height * .75D);
        for (int i = 0; i <= key.height; i++) {
            if (i < split) parts.add(new PartVisual(base));
            else if (i > split) parts.add(new PartVisual(empty));
            else parts.add(new PartVisual(key.valid ? connector : base));
        }

        ItemCustomMissilePart fuselage = key.parts.fuselage();
        List<MissilePartVisual> missile =
                fuselage != null && fuselage.top == key.size ? missileParts(key.parts) : List.of();
        return new Definition(parts, missile, key.size, key.height);
    }

    private List<MissilePartVisual> missileParts(MissileStruct missile) {
        ArrayList<MissilePartVisual> parts = new ArrayList<>();
        if (valid(missile.thruster(), PartType.THRUSTER))
            parts.add(new MissilePartVisual(missile.thruster()));
        if (valid(missile.fuselage(), PartType.FUSELAGE)) {
            if (valid(missile.fins(), PartType.FINS))
                parts.add(new MissilePartVisual(missile.fins()));
            parts.add(new MissilePartVisual(missile.fuselage()));
        }
        if (valid(missile.warhead(), PartType.WARHEAD))
            parts.add(new MissilePartVisual(missile.warhead()));
        return parts;
    }

    @Override
    protected AABB getRenderBoundingBox() {
        return new AABB(
                        pos.getX() - 5,
                        pos.getY(),
                        pos.getZ() - 5,
                        pos.getX() + 6,
                        pos.getY() + height + 4,
                        pos.getZ() + 6)
                .inflate(1);
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        if (definition != null) definition.collect(consumer);
    }

    @Override
    protected void _delete() {
        if (definition != null) definition.delete();
    }

    private record Key(PartSize size, int height, boolean valid, MissileStruct parts) {}

    private record PartKey(Identifier mesh, Identifier skin) {}

    private final class Definition {
        private final List<PartVisual> frame;
        private final List<MissilePartVisual> missile;
        private final PartSize size;
        private final int height;

        private Definition(
                List<PartVisual> frame,
                List<MissilePartVisual> missile,
                PartSize size,
                int height) {
            this.frame = frame;
            this.missile = missile;
            this.size = size;
            this.height = height;
        }

        private void write(Matrix4f root, MissileStruct source) {
            frame.get(0).write(root);
            frame.get(0).include(lightBoundsAccumulator, root);
            Matrix4f scaffold = scaffoldPose.set(root);
            if (size == PartSize.SIZE_10) scaffold.translate(0F, 0F, -1F);
            scaffold.translate(0F, 1F, 3.5F);
            for (int i = 0; i <= height; i++) {
                frame.get(i + 1).write(scaffold);
                frame.get(i + 1).include(lightBoundsAccumulator, scaffold);
                scaffold.translate(0F, 1F, 0F);
            }

            ItemCustomMissilePart fuselage = source.fuselage();
            if (fuselage == null || fuselage.top != size) return;

            missilePose.set(root).translate(0F, (float) MISSILE_Y, 0F);
            Matrix4f stack = stackPose.set(missilePose);
            int index = 0;
            ItemCustomMissilePart thruster = source.thruster();
            if (valid(thruster, PartType.THRUSTER)) {
                missile.get(index++).write(stack);
                missile.get(index - 1).include(lightBoundsAccumulator, stack);
                stack.translate(0F, thruster.height, 0F);
            }
            if (valid(fuselage, PartType.FUSELAGE)) {
                ItemCustomMissilePart fins = source.fins();
                if (valid(fins, PartType.FINS)) {
                    missile.get(index).write(stack);
                    missile.get(index++).include(lightBoundsAccumulator, stack);
                }
                missile.get(index).write(stack);
                missile.get(index++).include(lightBoundsAccumulator, stack);
                stack.translate(0F, fuselage.height, 0F);
            }
            ItemCustomMissilePart warhead = source.warhead();
            if (valid(warhead, PartType.WARHEAD)) {
                missile.get(index).write(stack);
                missile.get(index).include(lightBoundsAccumulator, stack);
            }
        }

        private void collect(Consumer<Instance> consumer) {
            for (var part : frame) part.collect(consumer);
            for (var part : missile) part.collect(consumer);
        }

        private void delete() {
            for (var part : frame) part.delete();
            for (var part : missile) part.delete();
        }
    }

    private final class PartVisual {
        private final MeshPart part;
        private final TransformedInstance instance;

        private PartVisual(MeshPart part) {
            this.part = part;
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

        private void include(double[] bounds, Matrix4f local) {
            LightBounds.includeLightBounds(bounds, part.model(), local, pos);
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
            MeshPart[] parts =
                    PART_MODELS.computeIfAbsent(
                            new PartKey(source.mesh, source.skin),
                            key ->
                                    MeshPart.objParts(
                                            new HFRWavefrontObject(
                                                    Minecraft.getInstance().getResourceManager(),
                                                    key.mesh),
                                            MeshPart.litCutout(key.skin)));
            groups = new ArrayList<>(parts.length);
            for (var part : parts) groups.add(new PartVisual(part));
        }

        private void write(Matrix4f local) {
            for (var group : groups) group.write(local);
        }

        private void include(double[] bounds, Matrix4f local) {
            for (var group : groups) group.include(bounds, local);
        }

        private void collect(Consumer<Instance> consumer) {
            for (var group : groups) group.collect(consumer);
        }

        private void delete() {
            for (var group : groups) group.delete();
        }
    }
}
