// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.entity.logic.EntityDeathBlast;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import net.minecraft.util.ARGB;
import org.joml.FrustumIntersection;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class DeathBlastVisual extends LaserColumnVisual<EntityDeathBlast> {
    private static final int SHELLS = 8;
    private static final float REACH = reach();

    private final TransformedInstance core;
    private final TransformedInstance[] shells = new TransformedInstance[SHELLS];
    private final Matrix4f pose = new Matrix4f();
    private final Vector3f position = new Vector3f();

    public DeathBlastVisual(VisualizationContext ctx, EntityDeathBlast entity, float partialTick) {
        super(ctx, entity, partialTick, 0x00FF00, 0xFF00FF);
        core = instance(NukeCloudVisual.SPHERE_TRANSLUCENT, 0xFFFFFFFF);
        for (int i = 0; i < SHELLS; i++)
            shells[i] = instance(NukeCloudVisual.SPHERE_ADDITIVE, 0xFFFFFFFF);
        writeSpheres(partialTick);
    }

    private static float reach() {
        var sphere = NukeCloudVisual.SPHERE_ADDITIVE.boundingSphere();
        float extent =
                (float)
                                Math.sqrt(
                                        sphere.x() * sphere.x()
                                                + sphere.y() * sphere.y()
                                                + sphere.z() * sphere.z())
                        + sphere.w();
        return 10F * 1.25F * (float) Math.pow(1.05D, SHELLS - 1) * extent;
    }

    @Override
    public boolean isVisible(FrustumIntersection frustum) {
        var origin = renderOrigin();
        float x = (float) (entity.getX() - origin.getX());
        float y = (float) (entity.getY() - origin.getY());
        float z = (float) (entity.getZ() - origin.getZ());
        return frustum.testAab(
                x - REACH, y - REACH, z - REACH, x + REACH, y + Math.max(HEIGHT, REACH), z + REACH);
    }

    @Override
    protected void frame(Context ctx) {
        super.frame(ctx);
        writeSpheres(ctx.partialTick());
    }

    private void writeSpheres(float partialTick) {
        float age = (entity.tickCount + partialTick) / EntityDeathBlast.maxAge;
        float scale = Math.max(10F - 10F * age, 0F);

        Vector3f visualPos = getVisualPosition(partialTick, position);
        pose.translation(visualPos.x, visualPos.y, visualPos.z).scale(scale);
        core.setTransform(pose);
        core.colorArgb(ARGB.colorFromFloat(age, 0.05F, 1F, 0.05F));
        core.setChanged();

        int shellColor = ARGB.colorFromFloat(age * 0.125F, 0F, 1F, 0F);
        pose.scale(1.25F);
        for (TransformedInstance shell : shells) {
            shell.setTransform(pose);
            shell.colorArgb(shellColor);
            shell.setChanged();
            pose.scale(1.05F);
        }
    }

    @Override
    protected void _delete() {
        super._delete();
        core.delete();
        for (TransformedInstance shell : shells) shell.delete();
    }
}
