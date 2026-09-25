// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.blocks.machine.rbmk.RBMKMiniPanelBase;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKDisplay;
import com.hbm.tileentity.machine.rbmk.RBMKColumn;
import com.hbm.tileentity.machine.rbmk.RBMKColumnType;
import com.hbm.util.Facing;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import java.util.function.Consumer;
import net.minecraft.core.Direction;
import net.minecraft.util.ARGB;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;

public final class RBMKDisplayVisual extends HbmDynamicBlockEntityVisual<BlockEntityRBMKDisplay> {
    private static final int STRIDE = BlockEntityRBMKDisplay.SIZE;
    private static final int COLUMNS = STRIDE * STRIDE;
    private final TransformedInstance[] columns = new TransformedInstance[COLUMNS];
    private final TransformedInstance[] dots = new TransformedInstance[COLUMNS];
    private final Matrix4f[] columnPoses = new Matrix4f[COLUMNS];
    private final Matrix4f[] dotPoses = new Matrix4f[COLUMNS];
    private final int[] lastColumnColors = new int[COLUMNS];
    private final int[] lastDotColors = new int[COLUMNS];
    private final boolean[] lastColumnVisible = new boolean[COLUMNS];
    private final boolean[] lastDotVisible = new boolean[COLUMNS];
    private final Matrix4f basePose = new Matrix4f();
    private final Matrix4f instancePose = new Matrix4f();
    private boolean initialized;

    public RBMKDisplayVisual(
            VisualizationContext context, BlockEntityRBMKDisplay blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);
        Direction facing = blockEntity.getBlockState().getValue(RBMKMiniPanelBase.FACING);
        float yaw = Facing.yaw(facing, 90);
        basePose.identity()
                .translate(.5F, 0F, .5F)
                .rotateY(yaw * Mth.DEG_TO_RAD)
                .translate(0F, .5F, 0F)
                .scale(1F, 8F / 7F, 8F / 7F)
                .translate(0F, -.5F, 0F);
        for (int i = 0; i < COLUMNS; i++) {
            columns[i] =
                    instancerProvider()
                            .instancer(InstanceTypes.TRANSFORMED, RBMKPanelModels.COLUMN)
                            .createInstance();
            dots[i] =
                    instancerProvider()
                            .instancer(InstanceTypes.TRANSFORMED, RBMKPanelModels.DOT)
                            .createInstance();
            columns[i].setVisible(false);
            dots[i].setVisible(false);
            float y = -(i / STRIDE) * .125F + .875F;
            float z = -(i % STRIDE) * .125F + .125F * 3F;
            columnPoses[i] = new Matrix4f(basePose).translate(.28125F, y, z);
            dotPoses[i] = new Matrix4f(basePose).translate(.29125F, y, z);
        }
        updateMovingParts(partialTick);
    }

    public static void initModels() {}

    private static boolean hasDot(RBMKColumn column) {
        return column.type == RBMKColumnType.FUEL
                || column.type == RBMKColumnType.FUEL_SIM
                || column.type == RBMKColumnType.CONTROL
                || column.type == RBMKColumnType.CONTROL_AUTO;
    }

    private static int columnColor(RBMKColumn column, int index) {
        float r = 1F, g = 1F, b = 1F;
        short dye = column instanceof RBMKColumn.ControlColumn control ? control.color : -1;
        if (dye >= 0) {
            switch (dye) {
                case 0 -> {
                    g = 0F;
                    b = 0F;
                }
                case 1 -> b = 0F;
                case 2 -> {
                    r = 0F;
                    g = .5F;
                    b = 0F;
                }
                case 3 -> {
                    r = 0F;
                    g = 0F;
                }
                case 4 -> {
                    r = .5F;
                    g = 0F;
                }
                default -> {}
            }
        } else {
            double heat = column.heat / column.maxHeat;
            double cv = .65D + (index % 2) * .05D;
            r = (float) (cv + (1D - cv) * heat);
            g = (float) cv;
            b = (float) cv;
        }
        if (column.indicator > 0) {
            r = 1F;
            g = 1F;
            b = 0F;
        }
        return ARGB.colorFromFloat(
                1F, Math.clamp(r, 0F, 1F), Math.clamp(g, 0F, 1F), Math.clamp(b, 0F, 1F));
    }

    private static int dotColor(RBMKColumn column) {
        float r, g, b;
        switch (column.type) {
            case FUEL, FUEL_SIM -> {
                r = 0F;
                g = .25F + (float) ((RBMKColumn.FuelColumn) column).enrichment * .75F;
                b = 0F;
            }
            case CONTROL -> {
                float level = (float) ((RBMKColumn.ControlColumn) column).level;
                r = level;
                g = level;
                b = 0F;
            }
            case CONTROL_AUTO -> {
                float level = (float) ((RBMKColumn.ControlColumn) column).level;
                r = level;
                g = 0F;
                b = level;
            }
            default -> {
                r = 0F;
                g = 0F;
                b = 0F;
            }
        }
        return ARGB.colorFromFloat(
                1F, Math.clamp(r, 0F, 1F), Math.clamp(g, 0F, 1F), Math.clamp(b, 0F, 1F));
    }

    @Override
    protected void frame(Context context) {
        updateMovingParts(context.partialTick());
    }

    public void updateMovingParts(float partialTick) {
        RBMKColumn[] values = blockEntity.columns;
        for (int i = 0; i < columns.length; i++) {
            RBMKColumn column = values[i];
            boolean columnVisible = column != null;
            int columnColor = columnVisible ? columnColor(column, i) : 0;
            if (columnVisible) {
                if (!initialized || !lastColumnVisible[i] || lastColumnColors[i] != columnColor)
                    write(columns[i], columnPoses[i], columnColor);
            } else if (!initialized || lastColumnVisible[i]) {
                columns[i].setVisible(false);
            }
            boolean dotVisible = columnVisible && hasDot(column);
            int dotColor = dotVisible ? dotColor(column) : 0;
            if (dotVisible) {
                if (!initialized || !lastDotVisible[i] || lastDotColors[i] != dotColor)
                    write(dots[i], dotPoses[i], dotColor);
            } else if (!initialized || lastDotVisible[i]) {
                dots[i].setVisible(false);
            }
            lastColumnVisible[i] = columnVisible;
            lastColumnColors[i] = columnColor;
            lastDotVisible[i] = dotVisible;
            lastDotColors[i] = dotColor;
        }
        initialized = true;
    }

    private void write(TransformedInstance instance, Matrix4f local, int color) {
        instance.setVisible(true);
        instancePose.translation(visualPos.getX(), visualPos.getY(), visualPos.getZ()).mul(local);
        instance.setTransform(instancePose)
                .colorArgb(color)
                .light(LightCoordsUtil.pack(15, 0))
                .setChanged();
    }

    @Override
    protected AABB getRenderBoundingBox() {
        return new AABB(pos).inflate(1);
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {}

    @Override
    protected void _delete() {
        for (TransformedInstance instance : columns) instance.delete();
        for (TransformedInstance instance : dots) instance.delete();
    }
}
