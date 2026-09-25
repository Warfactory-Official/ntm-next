// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.client.render.RenderRBMKConsole;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKConsole;
import com.hbm.tileentity.machine.rbmk.RBMKColumn;
import com.hbm.tileentity.machine.rbmk.RBMKColumnType;
import com.hbm.util.Facing;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visual.ShaderLightVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import java.util.Objects;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ARGB;
import net.minecraft.util.CommonColors;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

public final class RBMKConsoleVisual extends HbmDynamicBlockEntityVisual<BlockEntityRBMKConsole>
        implements ShaderLightVisual {
    private static final int STRIDE = 15;
    private static final int COLUMNS = STRIDE * STRIDE;
    private static final int SCREENS = 6;
    private final TransformedInstance[] columns = new TransformedInstance[COLUMNS];
    private final TransformedInstance[] dots = new TransformedInstance[COLUMNS];

    private final @Nullable RBMKColumn[] shownColumns = new RBMKColumn[COLUMNS];
    private final Matrix4f basePose = new Matrix4f();
    private final Matrix4f instancePose = new Matrix4f();
    private final boolean[] columnVisible = new boolean[COLUMNS], dotVisible = new boolean[COLUMNS];
    private final int[] columnColors = new int[COLUMNS], dotColors = new int[COLUMNS];
    private final WorldText[] screens = new WorldText[SCREENS];
    private final WorldText.Posing[] screenPosings = new WorldText.Posing[SCREENS];
    private final @Nullable String[] shownDisplays = new String[SCREENS];

    public RBMKConsoleVisual(
            VisualizationContext context, BlockEntityRBMKConsole blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);
        basePose.translate(.5F, 0F, .5F)
                .rotateY(
                        Facing.yaw(BlockMultiblockCore.coreFacing(blockState), 90) * Mth.DEG_TO_RAD)
                .translate(.5F, 0F, 0F);
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
        }
        for (int i = 0; i < SCREENS; i++) {
            screens[i] = new WorldText(instancerProvider(), WorldText.Style.POLYGON_OFFSET, true);
            int screen = i;
            screenPosings[i] =
                    (width, out) -> {
                        float scale = Math.min(.03F, .8F / Math.max(width, 1));
                        out.translation(visualPos.getX(), visualPos.getY(), visualPos.getZ())
                                .mul(basePose)
                                .translate(-.42F, 3.5F, 1.75F);
                        if (screen % 2 == 1) out.translate(0F, 0F, 1.75F * -2F);
                        out.translate(0F, -.75F * (screen >> 1), 0F)
                                .scale(scale, -scale, scale)
                                .rotateY(90F * Mth.DEG_TO_RAD)
                                .translate(-width / 2, 0F, 0F);
                    };
        }
        updateMovingParts(partialTick);
        updateScreens();
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

    private static void hide(TransformedInstance instance, boolean[] visibility, int index) {
        if (!visibility[index]) return;
        instance.setVisible(false);
        visibility[index] = false;
    }

    @Override
    protected void frame(Context context) {
        updateMovingParts(context.partialTick());
        updateScreens();
    }

    private void updateScreens() {
        float y = -Minecraft.getInstance().font.lineHeight / 2;
        for (int i = 0; i < SCREENS; i++) {
            String display = blockEntity.screens[i].display;
            if (!Objects.equals(display, shownDisplays[i])) {
                shownDisplays[i] = display;
                screens[i].set(RenderRBMKConsole.screenText(display), 0F, y, CommonColors.GREEN);
            }
            screens[i].write(screenPosings[i]);
        }
    }

    public void updateMovingParts(float partialTick) {
        RBMKColumn[] values = blockEntity.columns;
        for (int i = 0; i < columns.length; i++) {
            RBMKColumn column = values[i];
            if (column == shownColumns[i]) continue;
            shownColumns[i] = column;
            if (column == null) {
                hide(columns[i], columnVisible, i);
                hide(dots[i], dotVisible, i);
                continue;
            }
            float y = -(i / STRIDE) * .125F + 3.625F;
            float z = -(i % STRIDE) * .125F + .125F * 7F;
            write(
                    columns[i],
                    columnVisible,
                    columnColors,
                    i,
                    y,
                    z,
                    -.3725F,
                    columnColor(column, i));
            if (hasDot(column)) {
                write(dots[i], dotVisible, dotColors, i, y, z, -.3725F + .01F, dotColor(column));
            } else hide(dots[i], dotVisible, i);
        }
    }

    private void write(
            TransformedInstance instance,
            boolean[] visibility,
            int[] colors,
            int index,
            float y,
            float z,
            float x,
            int color) {
        if (visibility[index] && colors[index] == color) return;
        if (!visibility[index]) {
            instance.setVisible(true);
            instancePose
                    .translation(visualPos.getX(), visualPos.getY(), visualPos.getZ())
                    .mul(basePose)
                    .translate(x, y, z);
            instance.setTransform(instancePose).light(LightCoordsUtil.pack(15, 0));
        }
        instance.colorArgb(color).setChanged();
        visibility[index] = true;
        colors[index] = color;
    }

    @Override
    protected AABB getRenderBoundingBox() {
        return new AABB(
                        pos.getX() - 2,
                        pos.getY(),
                        pos.getZ() - 2,
                        pos.getX() + 3,
                        pos.getY() + 4,
                        pos.getZ() + 3)
                .inflate(1);
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {}

    @Override
    protected void _delete() {
        for (TransformedInstance instance : columns) instance.delete();
        for (TransformedInstance instance : dots) instance.delete();
        for (WorldText screen : screens) screen.delete();
    }
}
