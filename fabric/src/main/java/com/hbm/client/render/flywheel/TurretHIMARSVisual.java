// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.items.weapon.ItemAmmoHIMARS.HIMARSRocketType;
import com.hbm.items.weapon.ItemAmmoHIMARS;
import com.hbm.main.ResourceManager;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.tileentity.turret.BlockEntityTurretHIMARS;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.material.Material;
import dev.engine_room.flywheel.api.visual.ShaderLightVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import java.util.function.Consumer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

public final class TurretHIMARSVisual extends HbmDynamicBlockEntityVisual<BlockEntityTurretHIMARS>
        implements ShaderLightVisual {
    private static final HFRWavefrontObject HIMARS = ResourceManager.turret_himars;
    private static final Material HIMARS_MATERIAL =
            MeshPart.litCutout(ResourceManager.turret_himars_tex);
    private static final MeshPart CARRIAGE_PART =
            MeshPart.obj(
                    HIMARS.groups[HIMARS.partId("Carriage")], HIMARS.smoothing(), HIMARS_MATERIAL);
    private static final MeshPart LAUNCHER_PART =
            MeshPart.obj(
                    HIMARS.groups[HIMARS.partId("Launcher")], HIMARS.smoothing(), HIMARS_MATERIAL);
    private static final MeshPart CRANE_PART =
            MeshPart.obj(
                    HIMARS.groups[HIMARS.partId("Crane")], HIMARS.smoothing(), HIMARS_MATERIAL);
    private static final RocketAssets[] ROCKETS = buildRockets();
    private final AABB bodyBounds;
    private final TransformedInstance carriage;
    private final TransformedInstance launcher;
    private final TransformedInstance crane;
    private @Nullable RocketVariant rocketVariant;
    private int rocketType = -1;
    private final Matrix4f carriagePose = new Matrix4f();
    private final Matrix4f rootPose = new Matrix4f();
    private final Matrix4f launcherPose = new Matrix4f();
    private final Matrix4f cranePose = new Matrix4f();
    private final Matrix4f instancePose = new Matrix4f();
    private final double[] lightBounds = new double[6];
    private @Nullable AABB lastLightBounds;
    private float lastYaw = Float.NaN;
    private float lastPitch = Float.NaN;
    private float lastTravel = Float.NaN;
    private int lastAmmo;
    private boolean lastHasAmmo;
    private boolean initialized;

    public TurretHIMARSVisual(
            VisualizationContext context, BlockEntityTurretHIMARS blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);
        Vec3 offset = blockEntity.getHorizontalOffset();
        Matrix4f bodyLocal = new Matrix4f().translation((float) offset.x, 0F, (float) offset.z);
        bodyBounds = LightBounds.of(ResourceManager.turret_arty, "Base", bodyLocal, pos);
        rootPose.set(bodyLocal);
        carriage =
                instancerProvider()
                        .instancer(InstanceTypes.TRANSFORMED, CARRIAGE_PART.model())
                        .createInstance();
        launcher =
                instancerProvider()
                        .instancer(InstanceTypes.TRANSFORMED, LAUNCHER_PART.model())
                        .createInstance();
        crane =
                instancerProvider()
                        .instancer(InstanceTypes.TRANSFORMED, CRANE_PART.model())
                        .createInstance();
        updateMovingParts(partialTick);
    }

    public static void initModels() {}

    private static RocketAssets[] buildRockets() {
        var types = HIMARSRocketType.values();
        var result = new RocketAssets[types.length];
        for (var type : types) result[type.ordinal()] = buildRocket(type);
        return result;
    }

    private static RocketAssets buildRocket(HIMARSRocketType type) {
        var material = MeshPart.litCutout(type.texture);
        if (type.modelType != 0) {
            return new RocketAssets(
                    MeshPart.obj(
                            HIMARS.groups[HIMARS.partId("TubeSingle")],
                            HIMARS.smoothing(),
                            material),
                    new MeshPart[] {
                        MeshPart.obj(
                                HIMARS.groups[HIMARS.partId("CapSingle")],
                                HIMARS.smoothing(),
                                material)
                    });
        }
        var caps = new MeshPart[6];
        for (int i = 0; i < caps.length; i++)
            caps[i] =
                    MeshPart.obj(
                            HIMARS.groups[HIMARS.partId("CapStandard" + (6 - i))],
                            HIMARS.smoothing(),
                            material);
        return new RocketAssets(
                MeshPart.obj(
                        HIMARS.groups[HIMARS.partId("TubeStandard")], HIMARS.smoothing(), material),
                caps);
    }

    @Override
    protected void frame(Context context) {
        updateMovingParts(context.partialTick());
    }

    public void updateMovingParts(float partialTick) {
        float yawRadians =
                (float)
                        -Mth.lerp(
                                partialTick, blockEntity.lastRotationYaw, blockEntity.rotationYaw);
        float pitchRadians =
                (float)
                        Mth.lerp(
                                partialTick,
                                blockEntity.lastRotationPitch,
                                blockEntity.rotationPitch);
        float travel = Mth.lerp(partialTick, blockEntity.lastCrane, blockEntity.crane) * -5F;
        int typeIndex =
                blockEntity.typeLoaded < 0
                        ? -1
                        : ItemAmmoHIMARS.byIndex(blockEntity.typeLoaded).ordinal();
        boolean yawChanged =
                !initialized || Float.floatToIntBits(yawRadians) != Float.floatToIntBits(lastYaw);
        boolean pitchChanged =
                !initialized
                        || Float.floatToIntBits(pitchRadians) != Float.floatToIntBits(lastPitch);
        boolean travelChanged =
                !initialized || Float.floatToIntBits(travel) != Float.floatToIntBits(lastTravel);
        boolean coreChanged = yawChanged || pitchChanged || travelChanged;
        boolean typeChanged = typeIndex != rocketType;
        boolean hasAmmo = blockEntity.hasAmmo();
        boolean ammoChanged =
                !initialized || blockEntity.ammo != lastAmmo || hasAmmo != lastHasAmmo;
        if (!coreChanged && !typeChanged && !ammoChanged) return;
        if (yawChanged) {
            carriagePose.set(rootPose).rotateY(yawRadians - Mth.PI);
            write(carriage, carriagePose);
        }
        if (yawChanged || pitchChanged) {
            launcherPose
                    .set(carriagePose)
                    .translate(0F, 2.25F, 2F)
                    .rotateX(pitchRadians)
                    .translate(0F, -2.25F, -2F);
            write(launcher, launcherPose);
        }
        if (coreChanged) {
            cranePose.set(launcherPose).translate(0F, 0F, travel);
            write(crane, cranePose);
        }

        if (typeChanged) {
            if (rocketVariant != null) rocketVariant.delete();
            rocketVariant =
                    typeIndex < 0
                            ? null
                            : new RocketVariant(ItemAmmoHIMARS.byIndex(blockEntity.typeLoaded));
            rocketType = typeIndex;
        }
        if (rocketVariant != null) rocketVariant.write(blockEntity.ammo, hasAmmo, cranePose);

        LightBounds.resetBounds(lightBounds, bodyBounds);
        LightBounds.includeLightBounds(lightBounds, CARRIAGE_PART.model(), carriagePose, pos);
        LightBounds.includeLightBounds(lightBounds, LAUNCHER_PART.model(), launcherPose, pos);
        LightBounds.includeLightBounds(lightBounds, CRANE_PART.model(), cranePose, pos);
        if (rocketVariant != null)
            rocketVariant.includeBounds(lightBounds, blockEntity.ammo, hasAmmo, cranePose);
        lastLightBounds = LightBounds.sections(lightSections, lightBounds, lastLightBounds);
        lastYaw = yawRadians;
        lastPitch = pitchRadians;
        lastTravel = travel;
        lastAmmo = blockEntity.ammo;
        lastHasAmmo = hasAmmo;
        initialized = true;
    }

    private void write(TransformedInstance instance, Matrix4f pose) {
        instancePose.translation(visualPos.getX(), visualPos.getY(), visualPos.getZ()).mul(pose);
        instance.setTransform(instancePose).light(0).setChanged();
    }

    @Override
    protected AABB getRenderBoundingBox() {
        return bodyBounds.minmax(new AABB(pos).inflate(16));
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        consumer.accept(carriage);
        consumer.accept(launcher);
        consumer.accept(crane);
        if (rocketVariant != null) rocketVariant.collect(consumer);
    }

    @Override
    protected void _delete() {
        carriage.delete();
        launcher.delete();
        crane.delete();
        if (rocketVariant != null) rocketVariant.delete();
    }

    private record RocketAssets(MeshPart tube, MeshPart[] caps) {}

    private final class RocketVariant {
        private final boolean standard;
        private final RocketAssets assets;
        private final TransformedInstance tube;
        private final TransformedInstance[] caps;

        private RocketVariant(HIMARSRocketType type) {
            standard = type.modelType == 0;
            assets = ROCKETS[type.ordinal()];
            tube =
                    instancerProvider()
                            .instancer(InstanceTypes.TRANSFORMED, assets.tube().model())
                            .createInstance();
            caps = new TransformedInstance[assets.caps().length];
            for (int i = 0; i < caps.length; i++)
                caps[i] =
                        instancerProvider()
                                .instancer(InstanceTypes.TRANSFORMED, assets.caps()[i].model())
                                .createInstance();
        }

        private boolean capVisible(int index, int ammo, boolean hasAmmo) {
            return standard ? index < ammo : hasAmmo;
        }

        private void write(int ammo, boolean hasAmmo, Matrix4f pose) {
            TurretHIMARSVisual.this.write(tube, pose);
            for (int i = 0; i < caps.length; i++) {
                boolean visible = capVisible(i, ammo, hasAmmo);
                caps[i].setVisible(visible);
                if (visible) TurretHIMARSVisual.this.write(caps[i], pose);
            }
        }

        private void includeBounds(double[] bounds, int ammo, boolean hasAmmo, Matrix4f pose) {
            LightBounds.includeLightBounds(bounds, assets.tube().model(), pose, pos);
            for (int i = 0; i < caps.length; i++)
                if (capVisible(i, ammo, hasAmmo))
                    LightBounds.includeLightBounds(bounds, assets.caps()[i].model(), pose, pos);
        }

        private void collect(Consumer<Instance> consumer) {
            consumer.accept(tube);
            for (TransformedInstance cap : caps) consumer.accept(cap);
        }

        private void delete() {
            tube.delete();
            for (TransformedInstance cap : caps) cap.delete();
        }
    }
}
