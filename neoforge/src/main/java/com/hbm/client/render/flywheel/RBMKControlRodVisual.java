// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.main.ResourceManager;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKControl;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKControlAuto;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKControlManual;
import com.hbm.tileentity.machine.rbmk.RBMKConfig;
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

public final class RBMKControlRodVisual extends HbmDynamicBlockEntityVisual<BlockEntityRBMKControl>
        implements ShaderLightVisual {
    private static final int LID = ResourceManager.rbmk_rods.partId("Lid");
    private static final int SKINS = 2 + 5;
    private static final MeshPart[] LIDS = buildLids();
    private final AABB bodyBounds;
    private final Matrix4f pose = new Matrix4f();
    private final Matrix4f world = new Matrix4f();
    private final double[] lightBounds = new double[6];
    private final float columnHeight;
    private @Nullable TransformedInstance lid;
    private @Nullable AABB lastLightBounds;
    private double lastLevel = Double.NaN;
    private int lastSelected = -1;

    public RBMKControlRodVisual(
            VisualizationContext context, BlockEntityRBMKControl blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);
        int segments = RBMKConfig.getColumnHeightRuleValue(blockEntity.getLevel());
        columnHeight = RBMKConfig.getColumnHeight(blockEntity.getLevel());

        bodyBounds = new AABB(pos).expandTowards(0, segments + .125D, 0).inflate(1);
        updateMovingParts(partialTick);
    }

    public static void initModels() {}

    private static MeshPart[] buildLids() {
        var model = ResourceManager.rbmk_rods;
        var mesh = PackedQuadMesh.of(model.groups[LID], model.smoothing());
        var skins = new MeshPart[SKINS];
        skins[0] = MeshPart.create(mesh, MeshPart.litCutout(ResourceManager.rbmk_control_tex));
        skins[1] = MeshPart.create(mesh, MeshPart.litCutout(ResourceManager.rbmk_control_auto_tex));
        for (int i = 0; i < 5; i++)
            skins[2 + i] =
                    MeshPart.create(
                            mesh, MeshPart.litCutout(ResourceManager.rbmk_control_color_tex[i]));
        return skins;
    }

    @Override
    protected void frame(Context context) {
        updateMovingParts(context.partialTick());
    }

    public void updateMovingParts(float partialTick) {
        double level = Mth.lerp(partialTick, blockEntity.lastLevel, blockEntity.level);
        int selected =
                blockEntity instanceof BlockEntityRBMKControlAuto
                        ? 1
                        : blockEntity instanceof BlockEntityRBMKControlManual manual
                                        && manual.color != null
                                ? 2 + manual.color.ordinal()
                                : 0;
        boolean levelChanged = Double.doubleToLongBits(level) != Double.doubleToLongBits(lastLevel);
        boolean selectedChanged = selected != lastSelected;
        if (!levelChanged && !selectedChanged) return;
        if (selectedChanged) {
            if (lid != null) lid.delete();
            lid =
                    instancerProvider()
                            .instancer(InstanceTypes.TRANSFORMED, LIDS[selected].model())
                            .createInstance();
        }
        pose.identity().translate(.5F, columnHeight + (float) level, .5F);
        world.translation(visualPos.getX(), visualPos.getY(), visualPos.getZ()).mul(pose);
        lid.setTransform(world).light(0).setChanged();
        LightBounds.resetBounds(lightBounds, bodyBounds);
        LightBounds.includeLightBounds(lightBounds, LIDS[selected].model(), pose, pos);
        lastLightBounds = LightBounds.sections(lightSections, lightBounds, lastLightBounds);
        lastLevel = level;
        lastSelected = selected;
    }

    @Override
    protected AABB getRenderBoundingBox() {
        return bodyBounds.minmax(
                new AABB(
                                pos.getX(),
                                pos.getY(),
                                pos.getZ(),
                                pos.getX() + 1D,
                                pos.getY() + 17D,
                                pos.getZ() + 1D)
                        .inflate(1));
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        if (lid != null) consumer.accept(lid);
    }

    @Override
    protected void _delete() {
        if (lid != null) lid.delete();
    }
}
