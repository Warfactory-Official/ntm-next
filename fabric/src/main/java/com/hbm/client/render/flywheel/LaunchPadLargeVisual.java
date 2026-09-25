// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.client.render.PadMissiles;
import com.hbm.items.weapon.ItemMissile.MissileFormFactor;
import com.hbm.main.ResourceManager;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.tileentity.bomb.BlockEntityLaunchPadLarge;
import com.hbm.util.Facing;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visual.ShaderLightVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import java.util.function.Consumer;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

public final class LaunchPadLargeVisual
        extends HbmDynamicBlockEntityVisual<BlockEntityLaunchPadLarge>
        implements ShaderLightVisual {
    private static final int PARTS_PER_FORM = 4;
    private static final double MISSILE_Y = 2D;
    private static final HFRWavefrontObject MODEL = ResourceManager.missile_erector;
    private static final Row[] STATIC_ROWS = rows(MODEL);
    private static final MeshPart[][] ERECTOR_PARTS = buildErectorParts();
    private final PartVisual[] erectorParts;
    private final Matrix4f rootPose = new Matrix4f();
    private final Matrix4f pivotPose = new Matrix4f();
    private final Matrix4f armPose = new Matrix4f();
    private final Matrix4f missilePose = new Matrix4f();
    private int lastSelected = Integer.MIN_VALUE;
    private float lastErectorAngle = Float.NaN, lastLift = Float.NaN;
    private boolean lastErected, lastHasMissile, lastReadyToLoad, initialized;
    private @Nullable Item currentItem;
    private @Nullable MissileDraw missile;
    private final Matrix4f instancePose = new Matrix4f();

    public LaunchPadLargeVisual(
            VisualizationContext context,
            BlockEntityLaunchPadLarge blockEntity,
            float partialTick) {
        super(context, blockEntity, partialTick);
        erectorParts = new PartVisual[STATIC_ROWS.length * PARTS_PER_FORM];
        for (int i = 0; i < STATIC_ROWS.length; i++) {
            int base = i * PARTS_PER_FORM;
            erectorParts[base] = new PartVisual(ERECTOR_PARTS[i][0]);
            erectorParts[base + 1] = new PartVisual(ERECTOR_PARTS[i][1]);
            erectorParts[base + 2] = new PartVisual(ERECTOR_PARTS[i][2]);
            erectorParts[base + 3] = new PartVisual(ERECTOR_PARTS[i][3]);
        }
        rootPose.identity()
                .translate(.5F, 0F, .5F)
                .rotateY(
                        Facing.yaw(BlockMultiblockCore.coreFacing(blockState), 90)
                                * Mth.DEG_TO_RAD);
        writeFrame(partialTick);
    }

    private static MeshPart[][] buildErectorParts() {
        var parts = new MeshPart[STATIC_ROWS.length][PARTS_PER_FORM];
        for (int i = 0; i < parts.length; i++) {
            Row row = STATIC_ROWS[i];
            var material = MeshPart.litCutout(row.texture);
            parts[i][0] = MeshPart.obj(MODEL.groups[row.pad], MODEL.smoothing(), material);
            parts[i][1] = MeshPart.obj(MODEL.groups[row.arm], MODEL.smoothing(), material);
            parts[i][2] = MeshPart.obj(MODEL.groups[row.pivot], MODEL.smoothing(), material);
            parts[i][3] = MeshPart.obj(MODEL.groups[row.rope], MODEL.smoothing(), material);
        }
        return parts;
    }

    public static void initModels() {}

    private static Row[] rows(HFRWavefrontObject model) {
        Row[] rows = new Row[MissileFormFactor.values().length];
        rows[MissileFormFactor.ABM.ordinal()] =
                row(model, "ABM", 1.5D, 1.25D, ResourceManager.missile_erector_abm_tex);
        rows[MissileFormFactor.MICRO.ordinal()] =
                row(model, "Micro", 1.5D, 1.25D, ResourceManager.missile_erector_micro_tex);
        rows[MissileFormFactor.V2.ordinal()] =
                row(model, "V2", 1.75D, 1.25D, ResourceManager.missile_erector_v2_tex);
        rows[MissileFormFactor.STRONG.ordinal()] =
                row(model, "Strong", 3D, 1.5D, ResourceManager.missile_erector_strong_tex);
        rows[MissileFormFactor.HUGE.ordinal()] =
                row(model, "Huge", 3D, 1.5D, ResourceManager.missile_erector_huge_tex);
        rows[MissileFormFactor.ATLAS.ordinal()] =
                row(model, "Atlas", 4D, 1.5D, ResourceManager.missile_erector_atlas_tex);
        rows[MissileFormFactor.OTHER.ordinal()] = rows[MissileFormFactor.ABM.ordinal()];
        return rows;
    }

    private static Row row(
            HFRWavefrontObject model,
            String prefix,
            double pivotZ,
            double pivotY,
            Identifier texture) {
        return new Row(
                model.partId(prefix + "_Pad"),
                model.partId(prefix + "_Erector"),
                model.partId(prefix + "_Pivot"),
                model.partId(prefix + "_Rope"),
                pivotZ,
                pivotY,
                texture);
    }

    @Override
    protected void frame(Context context) {
        writeFrame(context.partialTick());
    }

    private void writeFrame(float partialTick) {
        Item item = blockEntity.loadedMissile;
        boolean itemChanged = item != currentItem;
        if (itemChanged) {
            if (missile != null) missile.delete();
            currentItem = item;
            PadMissiles.Missile source = item == null ? null : PadMissiles.table().get(item);
            MeshPart[] parts = item == null ? null : LaunchPadVisual.missileParts(item);
            missile = source == null || parts == null ? null : new MissileDraw(source, parts);
        }

        int selected =
                blockEntity.formFactor >= 0 && blockEntity.formFactor < STATIC_ROWS.length
                        ? blockEntity.formFactor
                        : -1;
        boolean erected = blockEntity.erected;
        boolean hasMissile = blockEntity.loadedMissile != null;
        float erectorAngle = Mth.lerp(partialTick, blockEntity.prevErector, blockEntity.erector);
        float lift = Mth.lerp(partialTick, blockEntity.prevLift, blockEntity.lift);
        boolean selectedChanged = !initialized || selected != lastSelected;
        boolean angleChanged = !initialized || erectorAngle != lastErectorAngle;
        boolean liftChanged = !initialized || lift != lastLift;
        boolean erectedChanged = !initialized || erected != lastErected;
        boolean missilePresenceChanged = !initialized || hasMissile != lastHasMissile;
        boolean readyChanged = !initialized || blockEntity.readyToLoad != lastReadyToLoad;
        if (selected >= 0) {
            Row row = STATIC_ROWS[selected];
            pivotPose
                    .set(rootPose)
                    .translate(0F, (float) row.pivotY, (float) -row.pivotZ)
                    .rotateX(-Mth.DEG_TO_RAD * erectorAngle)
                    .translate(0F, (float) -row.pivotY, (float) row.pivotZ);
            armPose.set(pivotPose).translate(0F, lift, 0F);
        } else {
            pivotPose.set(rootPose);
            armPose.set(rootPose);
        }
        if (selectedChanged) {
            for (int i = 0; i < STATIC_ROWS.length; i++) {
                int base = i * PARTS_PER_FORM;
                boolean visible = i == selected;
                erectorParts[base].write(visible, rootPose);
                erectorParts[base + 1].write(visible, armPose);
                erectorParts[base + 2].write(visible, pivotPose);
                erectorParts[base + 3].write(visible && erected && hasMissile, rootPose);
            }
        } else if (selected >= 0) {
            int base = selected * PARTS_PER_FORM;
            if (angleChanged || liftChanged) erectorParts[base + 1].write(true, armPose);
            if (angleChanged) erectorParts[base + 2].write(true, pivotPose);
            if (erectedChanged || missilePresenceChanged)
                erectorParts[base + 3].write(erected && hasMissile, rootPose);
        }

        boolean missilePoseChanged =
                itemChanged
                        || selectedChanged
                        || missilePresenceChanged
                        || readyChanged
                        || erectedChanged
                        || (!erected && (angleChanged || liftChanged));
        if (missile != null && missilePoseChanged) {
            if (selected >= 0) {
                missilePose.set(erected ? rootPose : armPose).translate(0F, (float) MISSILE_Y, 0F);
                if (missile.scale != 1F) missilePose.scale(missile.scale);
                missile.write(missilePose, erected || blockEntity.readyToLoad);
            } else {
                missile.write(rootPose, false);
            }
        }
        lastSelected = selected;
        lastErectorAngle = erectorAngle;
        lastLift = lift;
        lastErected = erected;
        lastHasMissile = hasMissile;
        lastReadyToLoad = blockEntity.readyToLoad;
        initialized = true;
    }

    @Override
    protected AABB getRenderBoundingBox() {
        return new AABB(
                        pos.getX() - 10,
                        pos.getY(),
                        pos.getZ() - 10,
                        pos.getX() + 11,
                        pos.getY() + 20,
                        pos.getZ() + 11)
                .inflate(1);
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        for (var part : erectorParts) part.collect(consumer);
        if (missile != null) missile.collect(consumer);
    }

    @Override
    protected void _delete() {
        for (var part : erectorParts) part.delete();
        if (missile != null) missile.delete();
    }

    private record Row(
            int pad,
            int arm,
            int pivot,
            int rope,
            double pivotZ,
            double pivotY,
            Identifier texture) {}

    private final class PartVisual {
        private final TransformedInstance instance;

        private PartVisual(MeshPart part) {
            instance =
                    instancerProvider()
                            .instancer(InstanceTypes.TRANSFORMED, part.model())
                            .createInstance();
        }

        private void write(boolean shown, Matrix4f local) {
            instance.setVisible(shown);
            if (!shown) return;
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

    private final class MissileDraw {
        private final float scale;
        private final TransformedInstance[] instances;

        private MissileDraw(PadMissiles.Missile source, MeshPart[] parts) {
            scale = source.scale();
            instances = new TransformedInstance[parts.length];
            for (int i = 0; i < parts.length; i++) {
                instances[i] =
                        instancerProvider()
                                .instancer(InstanceTypes.TRANSFORMED, parts[i].model())
                                .createInstance();
            }
        }

        private void write(Matrix4f local, boolean shown) {
            for (int i = 0; i < instances.length; i++) {
                instances[i].setVisible(shown);
                if (!shown) continue;
                instancePose
                        .translation(visualPos.getX(), visualPos.getY(), visualPos.getZ())
                        .mul(local);
                instances[i].setTransform(instancePose).light(0).setChanged();
            }
        }

        private void collect(Consumer<Instance> consumer) {
            for (var instance : instances) consumer.accept(instance);
        }

        private void delete() {
            for (var instance : instances) instance.delete();
        }
    }
}
