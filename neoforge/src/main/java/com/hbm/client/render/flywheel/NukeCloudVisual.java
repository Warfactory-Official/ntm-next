// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.main.ResourceManager;
import dev.engine_room.flywheel.api.model.Mesh;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.model.SingleMeshModel;
import dev.engine_room.flywheel.lib.util.OverlayTexture;
import java.util.Arrays;
import java.util.function.ToIntFunction;
import net.minecraft.util.ARGB;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import org.joml.FrustumIntersection;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

public final class NukeCloudVisual<T extends Entity> extends HbmDynamicEntityVisual<T> {

    private static final Mesh SPHERE =
            PackedQuadMesh.of(
                    ResourceManager.black_hole, ResourceManager.black_hole.partId("Icosphere"));
    private static final Mesh SHELL =
            PackedQuadMesh.of(
                    ResourceManager.sphere_new, ResourceManager.sphere_new.partId("Icosphere"));
    private static final float SPHERE_REACH = reach(SPHERE);
    private static final float SHELL_REACH = reach(SHELL);
    static final Model SPHERE_OPAQUE =
            new SingleMeshModel(SPHERE, EffectVisuals.Shared.CLOUD_OPAQUE);
    static final Model SPHERE_ADDITIVE =
            new SingleMeshModel(SPHERE, EffectVisuals.Shared.CLOUD_ADDITIVE);

    static final Model SPHERE_TRANSLUCENT =
            new SingleMeshModel(SPHERE, EffectVisuals.Shared.CLOUD_TRANSLUCENT);
    private static final Model SHELL_OPAQUE =
            new SingleMeshModel(SHELL, EffectVisuals.Shared.CLOUD_OPAQUE);
    private static final Model SHELL_OPAQUE_UNCULLED =
            new SingleMeshModel(SHELL, EffectVisuals.Shared.CLOUD_OPAQUE_UNCULLED);
    private static final Model SHELL_ADDITIVE =
            new SingleMeshModel(SHELL, EffectVisuals.Shared.CLOUD_ADDITIVE);

    private static final int FLEIJA_CORE = ARGB.colorFromFloat(1F, 0F, 1F, 1F);
    private static final int FLEIJA_SHELL = ARGB.colorFromFloat(1F, 0F, 0.125F, 0.125F);
    private static final int SOLINIUM = 0x27FFDA;
    private static final int SOLINIUM_CORE =
            ARGB.colorFromFloat(
                    1F,
                    ARGB.redFloat(SOLINIUM),
                    ARGB.greenFloat(SOLINIUM),
                    ARGB.blueFloat(SOLINIUM));
    private static final int SOLINIUM_SHELL =
            ARGB.colorFromFloat(
                    0.125F,
                    ARGB.redFloat(SOLINIUM),
                    ARGB.greenFloat(SOLINIUM),
                    ARGB.blueFloat(SOLINIUM));

    private static final float[] RAINBOW_SHELLS = rainbowShells();
    private final Vector3f interpolatedPosition = new Vector3f();
    private final Kind kind;
    private final ToIntFunction<T> age;
    private final @Nullable ToIntFunction<T> maxAge;
    private final RandomSource random = RandomSource.create();
    private final TransformedInstance core;
    private final TransformedInstance[] shells;
    private final @Nullable TransformedInstance shockwave;
    private final Matrix4f pose = new Matrix4f();
    private final Matrix4f scratch = new Matrix4f();

    public NukeCloudVisual(
            VisualizationContext ctx,
            T entity,
            float partialTick,
            Kind kind,
            ToIntFunction<T> age,
            @Nullable ToIntFunction<T> maxAge) {
        super(ctx, entity, partialTick);
        this.kind = kind;
        this.age = age;
        this.maxAge = maxAge;

        this.core =
                instance(
                        switch (kind) {
                            case FLEIJA -> SHELL_OPAQUE;
                            case SOLINIUM -> SHELL_OPAQUE_UNCULLED;
                            case RAINBOW -> SPHERE_OPAQUE;
                        });
        Model shellModel = kind == Kind.RAINBOW ? SPHERE_ADDITIVE : SHELL_ADDITIVE;
        this.shells = new TransformedInstance[kind == Kind.RAINBOW ? RAINBOW_SHELLS.length : 3];
        for (int i = 0; i < shells.length; i++) shells[i] = instance(shellModel);
        this.shockwave = kind == Kind.FLEIJA ? instance(SHELL_ADDITIVE) : null;

        writeFrame(partialTick);
    }

    public static void initModels() {}

    private static float reach(Mesh mesh) {
        var sphere = mesh.boundingSphere();
        return (float)
                        Math.sqrt(
                                sphere.x() * sphere.x()
                                        + sphere.y() * sphere.y()
                                        + sphere.z() * sphere.z())
                + sphere.w();
    }

    private static float[] rainbowShells() {
        float[] scales = new float[8];
        int found = 0;
        for (float i = 0.6F; i <= 1F; i += 0.1F) scales[found++] = i;
        return Arrays.copyOf(scales, found);
    }

    private TransformedInstance instance(Model model) {
        TransformedInstance created =
                instancerProvider().instancer(InstanceTypes.TRANSFORMED, model).createInstance();
        created.overlay(OverlayTexture.NO_OVERLAY);
        created.light(LightCoordsUtil.FULL_BRIGHT);
        return created;
    }

    @Override
    public boolean isVisible(FrustumIntersection frustum) {
        float age = this.age.applyAsInt(entity) + 1F;
        return sphereVisible(
                frustum,
                0F,
                switch (kind) {
                    case FLEIJA -> 10F * age * SHELL_REACH;
                    case SOLINIUM -> 1.08F * age * SHELL_REACH;
                    case RAINBOW -> age * SPHERE_REACH;
                });
    }

    @Override
    protected void frame(Context context) {
        writeFrame(context.partialTick());
    }

    private void writeFrame(float partialTick) {
        Vector3f visualPos = getVisualPosition(partialTick, interpolatedPosition);
        pose.translation(visualPos.x, visualPos.y, visualPos.z);
        switch (kind) {
            case FLEIJA -> fleija(partialTick);
            case SOLINIUM -> solinium(partialTick);
            case RAINBOW -> rainbow();
        }
    }

    private void fleija(float partialTick) {
        double baseScale = (age.applyAsInt(entity) + partialTick) * 2D;
        double ageScale = baseScale / maxAge.applyAsInt(entity);

        double scale = ageScale * 1.2D;
        if (scale > 1D) scale = Math.max(1D - (scale - 1D) * 5D, 0D);
        scale *= 2D * baseScale;

        write(core, (float) scale, FLEIJA_CORE);
        for (TransformedInstance shell : shells) {
            scale *= 1.05D;
            write(shell, (float) scale, FLEIJA_SHELL);
        }

        float tint = Mth.clamp((1F - (float) ageScale) * 0.75F, 0F, 1F);
        write(shockwave, (float) (5D * baseScale), ARGB.colorFromFloat(1F, tint, tint, tint));
    }

    private void solinium(float partialTick) {
        double scale = age.applyAsInt(entity) + partialTick;
        write(core, (float) scale, SOLINIUM_CORE);
        for (TransformedInstance shell : shells) {
            scale *= 1.025D;
            write(shell, (float) scale, SOLINIUM_SHELL);
        }
    }

    private void rainbow() {
        float scale = age.applyAsInt(entity);
        write(core, scale * 0.5F, roll());
        for (int i = 0; i < shells.length; i++) {
            write(shells[i], scale * RAINBOW_SHELLS[i], roll());
        }
    }

    private int roll() {
        return ARGB.color(255, random.nextInt(0x100), random.nextInt(0x100), random.nextInt(0x100));
    }

    private void write(TransformedInstance instance, float scale, int color) {
        instance.setTransform(scratch.set(pose).scale(scale));
        instance.colorArgb(color);
        instance.setChanged();
    }

    @Override
    protected void _delete() {
        core.delete();
        for (TransformedInstance shell : shells) shell.delete();
        if (shockwave != null) shockwave.delete();
    }

    public enum Kind {
        FLEIJA,
        SOLINIUM,
        RAINBOW
    }
}
