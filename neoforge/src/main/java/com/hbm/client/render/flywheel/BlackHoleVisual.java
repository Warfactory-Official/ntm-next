// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.entity.effect.EntityBlackHole;
import com.hbm.main.ResourceManager;
import dev.engine_room.flywheel.api.material.CardinalLightingMode;
import dev.engine_room.flywheel.api.material.Material;
import dev.engine_room.flywheel.api.material.Transparency;
import dev.engine_room.flywheel.api.material.WriteMask;
import dev.engine_room.flywheel.api.model.Mesh;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.material.CutoutShaders;
import dev.engine_room.flywheel.lib.material.Materials;
import dev.engine_room.flywheel.lib.material.SimpleMaterial;
import dev.engine_room.flywheel.lib.model.SingleMeshModel;
import dev.engine_room.flywheel.lib.util.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.FrustumIntersection;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class BlackHoleVisual extends HbmDynamicEntityVisual<EntityBlackHole> {
    private static final int RING_COUNT = 16;
    private static final int DISC_STEPS = 15;
    private static final int BLACK = ARGB.color(255, 0, 0, 0);
    private static final int GLOW = ARGB.color(191, 255, 255, 255);
    private static final int JET_CORE = ARGB.color(89, 255, 255, 255);
    private static final int JET_TIP = ARGB.color(0, 255, 255, 255);
    private static final int WHITE = ARGB.color(255, 255, 255, 255);
    private static final int SPHERE_PART = ResourceManager.black_hole.partId("Icosphere");
    private static final Model SPHERE_MODEL =
            new SingleMeshModel(
                    PackedQuadMesh.of(ResourceManager.black_hole, SPHERE_PART),
                    SimpleMaterial.builder()
                            .cutout(CutoutShaders.ONE_TENTH)
                            .texture(ResourceManager.black_hole_tex)
                            .mipmap(false)
                            .useOverlay(false)
                            .backfaceCulling(false)
                            .cardinalLightingMode(CardinalLightingMode.OFF)
                            .build());
    private static final Model JET_MODEL =
            new SingleMeshModel(jets(), EffectVisuals.Shared.BEAM_ADDITIVE);
    private static final Kit[] KITS = buildKits();
    private final Vector3f interpolatedPosition = new Vector3f();
    private final Kind kind;
    private final TransformedInstance sphere;
    private final TransformedInstance jets;
    private final TransformedInstance[] translucent;
    private final TransformedInstance[] additive;
    private final Matrix4f pose = new Matrix4f();
    private final Matrix4f ring = new Matrix4f();
    private float lastX = Float.NaN, lastY, lastZ, lastSize;

    public BlackHoleVisual(
            VisualizationContext ctx, EntityBlackHole entity, float partialTick, Kind kind) {
        super(ctx, entity, partialTick);
        this.kind = kind;
        Kit kit = kit(kind);

        this.sphere = instance(SPHERE_MODEL);
        this.jets = kind.jets ? instance(JET_MODEL) : null;

        int rings = kit.translucent.length;
        this.translucent = new TransformedInstance[rings];
        this.additive = new TransformedInstance[rings];
        for (int i = 0; i < rings; i++) {
            translucent[i] = instance(kit.translucent[i]);
            additive[i] = instance(kit.additive[i]);
        }

        writeFrame(partialTick);
    }

    public static void initModels() {}

    private static Kit[] buildKits() {
        Kit[] kits = new Kit[Kind.values().length];
        for (Kind kind : Kind.values()) kits[kind.ordinal()] = Kit.of(kind);
        return kits;
    }

    private static Kit kit(Kind kind) {
        return KITS[kind.ordinal()];
    }

    private static Mesh outerRing(int near, int far) {
        PackedQuadMesh.Builder mesh = PackedQuadMesh.builder(RING_COUNT);
        Vec3 vector = new Vec3(1D, 0D, 0D);
        for (int i = 0; i < RING_COUNT; i++) {
            mesh.vertex(
                    vector.x, 0D, vector.z, 0.5D + vector.x * 0.25D, 0.5D + vector.z * 0.25D, near);
            mesh.vertex(
                    vector.x * 2D,
                    0D,
                    vector.z * 2D,
                    0.5D + vector.x * 0.5D,
                    0.5D + vector.z * 0.5D,
                    far);
            vector = vector.yRot((float) (Math.PI * 2D / RING_COUNT));
            mesh.vertex(
                    vector.x * 2D,
                    0D,
                    vector.z * 2D,
                    0.5D + vector.x * 0.5D,
                    0.5D + vector.z * 0.5D,
                    far);
            mesh.vertex(
                    vector.x, 0D, vector.z, 0.5D + vector.x * 0.25D, 0.5D + vector.z * 0.25D, near);
        }
        return mesh.build();
    }

    private static Mesh innerRing(double scale, int near) {
        PackedQuadMesh.Builder mesh = PackedQuadMesh.builder(RING_COUNT);
        Vec3 vector = new Vec3(1D, 0D, 0D);
        for (int i = 0; i < RING_COUNT; i++) {
            mesh.vertex(
                    vector.x * 0.9D,
                    0D,
                    vector.z * 0.9D,
                    0.5D + vector.x * 0.25D / scale * 0.9D,
                    0.5D + vector.z * 0.25D / scale * 0.9D,
                    BLACK);
            mesh.vertex(
                    vector.x * scale,
                    0D,
                    vector.z * scale,
                    0.5D + vector.x * 0.25D,
                    0.5D + vector.z * 0.25D,
                    near);
            vector = vector.yRot((float) (Math.PI * 2D / RING_COUNT));
            mesh.vertex(
                    vector.x * scale,
                    0D,
                    vector.z * scale,
                    0.5D + vector.x * 0.25D,
                    0.5D + vector.z * 0.25D,
                    near);
            mesh.vertex(
                    vector.x * 0.9D,
                    0D,
                    vector.z * 0.9D,
                    0.5D + vector.x * 0.25D / scale * 0.9D,
                    0.5D + vector.z * 0.25D / scale * 0.9D,
                    BLACK);
        }
        return mesh.build();
    }

    private static Mesh jets() {
        PackedQuadMesh.Builder mesh = PackedQuadMesh.builder(24);
        for (int j = -1; j <= 1; j += 2) {
            Vec3 rim = new Vec3(0.5D, 0D, 0D);
            for (int i = 0; i < 12; i++) {
                Vec3 next = rim.yRot((float) (Math.PI / 6D * -j));
                mesh.vertex(0D, 0D, 0D, JET_CORE);
                mesh.vertex(rim.x, 10D * j, rim.z, JET_TIP);
                mesh.vertex(next.x, 10D * j, next.z, JET_TIP);
                mesh.vertex(0D, 0D, 0D, JET_CORE);
                rim = next;
            }
        }
        return mesh.build();
    }

    private TransformedInstance instance(Model model) {
        TransformedInstance created =
                instancerProvider().instancer(InstanceTypes.TRANSFORMED, model).createInstance();
        created.overlay(OverlayTexture.NO_OVERLAY);
        created.colorArgb(WHITE);
        created.light(LightCoordsUtil.FULL_BRIGHT);
        return created;
    }

    @Override
    public boolean isVisible(FrustumIntersection frustum) {
        return sphereVisible(frustum, 0F, (kind.jets ? 10F : 6F) * entity.getSize());
    }

    @Override
    protected void frame(Context ctx) {
        writeFrame(ctx.partialTick());
    }

    private void writeFrame(float partialTick) {
        Vector3f visualPos = getVisualPosition(partialTick, interpolatedPosition);
        float size = entity.getSize();
        float age = entity.tickCount + partialTick;
        int id = entity.getId();

        boolean moved =
                visualPos.x != lastX
                        || visualPos.y != lastY
                        || visualPos.z != lastZ
                        || size != lastSize;
        lastX = visualPos.x;
        lastY = visualPos.y;
        lastZ = visualPos.z;
        lastSize = size;

        pose.translation(visualPos.x, visualPos.y, visualPos.z).scale(size);
        if (moved) sphere.setTransform(pose).setChanged();

        pose.rotateX((id % 90 - 45F) * Mth.DEG_TO_RAD).rotateY((id % 360) * Mth.DEG_TO_RAD);
        if (moved && jets != null) jets.setTransform(pose).setChanged();

        if (kind.swirl) {
            ring.set(pose).rotateY(-5F * age * Mth.DEG_TO_RAD);

            write(0, ring);
            write(1, ring.scale(3F));
        } else {
            for (int k = 0; k < DISC_STEPS; k++) {

                ring.set(pose)
                        .rotateY(-age * (float) Math.pow(k + 1, 1.25D) * Mth.DEG_TO_RAD)
                        .scale((float) (3D - k * 0.175D));
                write(k, ring);
            }
        }
    }

    private void write(int index, Matrix4f matrix) {
        translucent[index].setTransform(matrix);
        translucent[index].setChanged();
        additive[index].setTransform(matrix);
        additive[index].setChanged();
    }

    @Override
    protected void _delete() {
        sphere.delete();
        if (jets != null) jets.delete();
        for (TransformedInstance instance : translucent) instance.delete();
        for (TransformedInstance instance : additive) instance.delete();
    }

    public enum Kind {
        HOLE(ResourceManager.black_hole_disc_tex, 0xFFB900, false, true, 0.75F),
        VORTEX(ResourceManager.vortex_tex, 0x3898B3, true, false, 0.75F),
        RAGING(ResourceManager.vortex_tex, 0xE8390D, true, true, 0.25F),
        QUASAR(ResourceManager.quasar_disc_tex, 0xFFB900, false, true, 0.75F);

        final Identifier texture;
        final boolean swirl;
        final boolean jets;
        final float glow;
        final int full;
        final int empty;

        Kind(Identifier texture, int tint, boolean swirl, boolean jets, float glow) {
            this.texture = texture;
            this.swirl = swirl;
            this.jets = jets;
            this.glow = glow;
            this.full = tint | 0xFF000000;
            this.empty = tint & 0x00FFFFFF;
        }

        int discColor(int iteration, int alpha) {
            if (this == QUASAR) {
                int gb = (int) (Math.pow(iteration / 15D, 2D) * 255D);
                return ARGB.color(alpha, 255, gb, gb);
            }
            if (iteration < 5)
                return ARGB.color(alpha, 255, (int) ((0.125F + iteration / 10F) * 255F), 0);
            if (iteration == 5) return ARGB.color(alpha, 255, 255, 0);
            int i = iteration - 6;
            return ARGB.color(
                    alpha,
                    (int) ((1F - i / 9F) * 255F),
                    (int) ((1F - i / 9F) * 255F),
                    (int) (i / 5F * 255F));
        }
    }

    private record Kit(Model[] translucent, Model[] additive) {
        static Kit of(Kind kind) {

            Material sorted =
                    SimpleMaterial.builderOf(Materials.TRANSLUCENT_NO_DEPTH_WRITE_NO_CULL)
                            .texture(kind.texture)
                            .mipmap(false)
                            .useOverlay(false)
                            .build();
            Material glow =
                    EffectVisuals.Shared.unlit()
                            .texture(kind.texture)
                            .transparency(Transparency.ORDER_INDEPENDENT_ADDITIVE)
                            .fog(EffectVisuals.FADE)
                            .writeMask(WriteMask.COLOR)
                            .backfaceCulling(false)
                            .build();

            if (kind.swirl) {
                int swirlGlow = ARGB.color((int) (kind.glow * 255F), 255, 255, 255);
                return new Kit(
                        new Model[] {
                            new SingleMeshModel(innerRing(3D, kind.full), sorted),
                            new SingleMeshModel(outerRing(kind.full, kind.empty), sorted)
                        },
                        new Model[] {
                            new SingleMeshModel(innerRing(3D, swirlGlow), glow),
                            new SingleMeshModel(outerRing(swirlGlow, kind.empty), glow)
                        });
            }

            Model[] translucent = new Model[DISC_STEPS];
            Model[] additive = new Model[DISC_STEPS];
            for (int k = 0; k < DISC_STEPS; k++) {
                int far = kind.discColor(k, 0);
                translucent[k] =
                        new SingleMeshModel(outerRing(kind.discColor(k, 255), far), sorted);
                additive[k] = new SingleMeshModel(outerRing(GLOW, far), glow);
            }
            return new Kit(translucent, additive);
        }
    }
}
