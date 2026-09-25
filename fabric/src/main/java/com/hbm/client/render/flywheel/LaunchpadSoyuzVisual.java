// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.main.ResourceManager;
import com.hbm.tileentity.machine.BlockEntityLaunchpadSoyuz;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visual.ShaderLightVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import java.util.function.Consumer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

public final class LaunchpadSoyuzVisual
        extends HbmDynamicBlockEntityVisual<BlockEntityLaunchpadSoyuz>
        implements ShaderLightVisual {

    private static final double[] WHEEL_FORWARD = {17D, 19D, 29D, 31D};
    private static final double[] WHEEL_SIDE = {6.75D, 5.25D, -5.25D, -6.75D};
    private static final MeshPart[] PARTS = parts();

    public static void initModels() {}

    private final Piece[] moving = new Piece[PARTS.length];
    private final Matrix4f base = new Matrix4f();
    private final Matrix4f local = new Matrix4f();
    private final Matrix4f carriagePose = new Matrix4f();
    private final Matrix4f rotorPose = new Matrix4f();
    private final Matrix4f mountPose = new Matrix4f();
    private final Matrix4f instancePose = new Matrix4f();
    private final double[] bounds = new double[6];
    private final float[] lastPositions = new float[8];
    private @Nullable AABB lastLightBounds;
    private Piece @Nullable [] rocket;
    private int lastSkin = Integer.MIN_VALUE;
    private BlockEntityLaunchpadSoyuz.SoyuzStatus lastStatus;
    private boolean initialized;

    public LaunchpadSoyuzVisual(
            VisualizationContext context,
            BlockEntityLaunchpadSoyuz blockEntity,
            float partialTick) {
        super(context, blockEntity, partialTick);
        for (int i = 0; i < moving.length; i++) moving[i] = piece(PARTS[i]);
        updateMovingParts(partialTick);
    }

    private static MeshPart[] parts() {
        var model = ResourceManager.launchpad_soyuz;
        var material = MeshPart.litCutout(ResourceManager.launchpad_soyuz_tex);
        MeshPart[] parts = new MeshPart[24];
        for (int i = 0; i < 5; i++)
            parts[i] =
                    MeshPart.obj(
                            model.groups[model.partId("Strut" + (i + 1))],
                            model.smoothing(),
                            material);
        parts[5] =
                MeshPart.obj(model.groups[model.partId("Carriage")], model.smoothing(), material);
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                String name = "Wheel_" + (i + 1) + "_" + (j + 1);
                parts[6 + i * 4 + j] =
                        MeshPart.obj(model.groups[model.partId(name)], model.smoothing(), material);
            }
        }
        parts[22] = MeshPart.obj(model.groups[model.partId("Rotor")], model.smoothing(), material);
        parts[23] = MeshPart.obj(model.groups[model.partId("Mount")], model.smoothing(), material);
        return parts;
    }

    private Piece piece(MeshPart part) {
        return new Piece(
                part,
                instancerProvider()
                        .instancer(InstanceTypes.TRANSFORMED, part.model())
                        .createInstance());
    }

    private void syncRocket(int skin, boolean visible) {
        if (!visible || skin < 0 || skin >= 3) {
            if (rocket != null) delete(rocket);
            rocket = null;
            return;
        }
        if (rocket != null && lastSkin == skin) return;
        if (rocket != null) delete(rocket);
        MeshPart[] parts = SoyuzLauncherVisual.rocketParts(skin);
        rocket = new Piece[parts.length];
        for (int i = 0; i < parts.length; i++) rocket[i] = piece(parts[i]);
    }

    @Override
    protected void frame(Context context) {
        updateMovingParts(context.partialTick());
    }

    private void updateMovingParts(float partialTick) {
        int skin = blockEntity.loadedType;
        var status = blockEntity.soyuzStatus;
        boolean changed = !initialized || skin != lastSkin || status != lastStatus;
        for (int i = 0; i < lastPositions.length; i++) {
            float position = blockEntity.getInterpPos(i, partialTick);
            if (Float.compare(position, lastPositions[i]) != 0) changed = true;
            lastPositions[i] = position;
        }
        if (!changed) return;
        initialized = true;
        syncRocket(skin, status != BlockEntityLaunchpadSoyuz.SoyuzStatus.ABSENT);
        lastSkin = skin;
        lastStatus = status;
        float rotation =
                switch (blockEntity.getBlockState().getValue(BlockMultiblockCore.FACING)) {
                    case NORTH -> 90F;
                    case WEST -> 180F;
                    case SOUTH -> 270F;
                    default -> 0F;
                };
        base.translation(.5F, 0F, .5F).rotateY(rotation * Mth.DEG_TO_RAD).translate(-4F, 0F, -4F);
        for (int i = 0; i < bounds.length / 2; i++) bounds[i] = Double.POSITIVE_INFINITY;
        for (int i = 3; i < bounds.length; i++) bounds[i] = Double.NEGATIVE_INFINITY;

        if (rocket != null && status == BlockEntityLaunchpadSoyuz.SoyuzStatus.LAUNCHING) {
            local.set(base).translate(0F, 4F, 0F).rotateY(-rotation * Mth.DEG_TO_RAD);
            writeRocket(local);
        }

        for (int i = 0; i < 5; i++) {
            float extension = i == 4 ? 3F : 4.5F;
            float shift = Mth.clamp((1F - lastPositions[i]) * extension, 0F, extension);
            local.set(base).translate(0F, 0F, shift);
            write(moving[i], local);
        }

        float carriage =
                Mth.clamp(
                        (1F - lastPositions[BlockEntityLaunchpadSoyuz.INDEX_CARRIAGE]) * 19.5F,
                        0F,
                        19.5F);
        float wheels = (float) (carriage * 360D / Math.PI);
        carriagePose
                .set(base)
                .translate(0F, 0F, -carriage)
                .translate(0F, 1.5F, -32F)
                .rotateX(-lastPositions[BlockEntityLaunchpadSoyuz.INDEX_TILT] * Mth.DEG_TO_RAD)
                .translate(0F, -1.5F, 32F);
        write(moving[5], carriagePose);
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                float side = (float) WHEEL_SIDE[j], forward = (float) WHEEL_FORWARD[i];
                local.set(carriagePose)
                        .translate(side, 0F, -forward)
                        .rotateY(wheels * (j % 2 == 1 ? -1F : 1F) * Mth.DEG_TO_RAD)
                        .translate(-side, 0F, forward);
                write(moving[6 + i * 4 + j], local);
            }
        }
        float rotor =
                Mth.clamp(
                        (1F - lastPositions[BlockEntityLaunchpadSoyuz.INDEX_ROTOR]) * 180F,
                        0F,
                        180F);
        rotorPose
                .set(carriagePose)
                .translate(0F, 24.5F, -18F)
                .rotateX(-rotor * Mth.DEG_TO_RAD)
                .translate(0F, -24.5F, 18F);
        write(moving[22], rotorPose);
        mountPose
                .set(rotorPose)
                .translate(0F, 24.5F, -6F)
                .rotateX(rotor * Mth.DEG_TO_RAD)
                .translate(0F, -24.5F, 6F);
        write(moving[23], mountPose);

        if (rocket != null && status != BlockEntityLaunchpadSoyuz.SoyuzStatus.LAUNCHING) {
            local.set(mountPose).translate(0F, 4F, 0F).rotateY(-rotation * Mth.DEG_TO_RAD);
            writeRocket(local);
        }
        lastLightBounds = LightBounds.sections(lightSections, bounds, lastLightBounds);
    }

    private void writeRocket(Matrix4f pose) {
        for (Piece piece : rocket) write(piece, pose);
    }

    private void write(Piece piece, Matrix4f pose) {
        instancePose.translation(visualPos.getX(), visualPos.getY(), visualPos.getZ()).mul(pose);
        piece.instance.setTransform(instancePose).light(0).setChanged();
        LightBounds.includeLightBounds(bounds, piece.part.model(), pose, pos);
    }

    @Override
    protected AABB getRenderBoundingBox() {
        return new AABB(
                pos.getX() - 60,
                pos.getY() - 2,
                pos.getZ() - 60,
                pos.getX() + 61,
                pos.getY() + 57,
                pos.getZ() + 61);
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        for (Piece piece : moving) consumer.accept(piece.instance);
        if (rocket != null) for (Piece piece : rocket) consumer.accept(piece.instance);
    }

    @Override
    protected void _delete() {
        delete(moving);
        if (rocket != null) delete(rocket);
    }

    private static void delete(Piece[] pieces) {
        for (Piece piece : pieces) piece.instance.delete();
    }

    private record Piece(MeshPart part, TransformedInstance instance) {}
}
