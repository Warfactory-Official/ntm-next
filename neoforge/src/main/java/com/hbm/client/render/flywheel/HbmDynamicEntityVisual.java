// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.visual.AbstractEntityVisual;
import dev.engine_room.flywheel.lib.visual.SimpleDynamicVisual;
import net.minecraft.world.entity.Entity;
import org.joml.FrustumIntersection;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector4fc;

public abstract class HbmDynamicEntityVisual<T extends Entity> extends AbstractEntityVisual<T>
        implements SimpleDynamicVisual {
    private final Vector3f scale = new Vector3f();
    private final float[] drawn = new float[6];
    private float lastX, lastY, lastZ, lastRadius;

    protected HbmDynamicEntityVisual(VisualizationContext context, T entity, float partialTick) {
        super(context, entity, partialTick);
    }

    @Override
    public final void beginFrame(Context context) {
        if (!isVisible(context.frustum())) return;
        frame(context);
    }

    protected abstract void frame(Context context);

    protected final boolean sphereVisible(FrustumIntersection frustum, float lift, float radius) {
        if (isFirstPersonCameraEntity()) return false;
        var origin = renderOrigin();
        float x = (float) (entity.getX() - origin.getX());
        float y = (float) (entity.getY() + lift - origin.getY());
        float z = (float) (entity.getZ() - origin.getZ());
        boolean visible =
                frustum.testSphere(x, y, z, radius)
                        || lastRadius > 0F && frustum.testSphere(lastX, lastY, lastZ, lastRadius);
        if (visible) {
            lastX = x;
            lastY = y;
            lastZ = z;
            lastRadius = radius;
        }
        return visible;
    }

    protected final void resetDrawn() {
        drawn[0] = drawn[1] = drawn[2] = Float.POSITIVE_INFINITY;
        drawn[3] = drawn[4] = drawn[5] = Float.NEGATIVE_INFINITY;
    }

    protected final void includeDrawn(Matrix4fc pose, Model model) {
        Vector4fc sphere = model.boundingSphere();
        float radius = sphere.w() * pose.getScale(scale).get(scale.maxComponent());
        float x =
                pose.m00() * sphere.x()
                        + pose.m10() * sphere.y()
                        + pose.m20() * sphere.z()
                        + pose.m30();
        float y =
                pose.m01() * sphere.x()
                        + pose.m11() * sphere.y()
                        + pose.m21() * sphere.z()
                        + pose.m31();
        float z =
                pose.m02() * sphere.x()
                        + pose.m12() * sphere.y()
                        + pose.m22() * sphere.z()
                        + pose.m32();
        drawn[0] = Math.min(drawn[0], x - radius);
        drawn[1] = Math.min(drawn[1], y - radius);
        drawn[2] = Math.min(drawn[2], z - radius);
        drawn[3] = Math.max(drawn[3], x + radius);
        drawn[4] = Math.max(drawn[4], y + radius);
        drawn[5] = Math.max(drawn[5], z + radius);
    }

    protected final boolean drawnVisible(FrustumIntersection frustum) {
        return drawn[0] <= drawn[3]
                && !isFirstPersonCameraEntity()
                && frustum.testAab(drawn[0], drawn[1], drawn[2], drawn[3], drawn[4], drawn[5]);
    }
}
