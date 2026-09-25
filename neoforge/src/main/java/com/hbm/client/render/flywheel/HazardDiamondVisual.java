// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.inventory.fluid.EnumSymbol;
import com.hbm.lib.Library;
import dev.engine_room.flywheel.api.material.CardinalLightingMode;
import dev.engine_room.flywheel.api.material.Material;
import dev.engine_room.flywheel.api.material.Transparency;
import dev.engine_room.flywheel.api.material.WriteMask;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.UvTransformedInstance;
import dev.engine_room.flywheel.lib.material.CutoutShaders;
import dev.engine_room.flywheel.lib.material.LightShaders;
import dev.engine_room.flywheel.lib.material.Materials;
import dev.engine_room.flywheel.lib.material.SimpleMaterial;
import dev.engine_room.flywheel.lib.visual.AbstractVisual;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;

public final class HazardDiamondVisual extends AbstractVisual {
    private static final float PIXEL = 1F / 256F;
    private static final float SCALE = 1F / 139F;
    private static final float[] QUAD = {
        0, 1, -1, 1, 0, 1, 0, 0,
        0, 1, 1, 0, 0, 1, 0, 0,
        0, -1, 1, 0, 1, 1, 0, 0,
        0, -1, -1, 1, 1, 1, 0, 0
    };
    private static final int[] WHITE = {-1, -1, -1, -1};
    private static final int[] LIGHT = {0, 0, 0, 0};
    private static final Material MATERIAL =
            SimpleMaterial.builderOf(Materials.CUTOUT)
                    .texture(Library.id("textures/models/misc/danger_diamond.png"))
                    .mipmap(false)
                    .cutout(CutoutShaders.ONE_TENTH)
                    .light(LightShaders.SMOOTH)
                    .ambientOcclusion(false)
                    .cardinalLightingMode(CardinalLightingMode.OFF)
                    .transparency(Transparency.ORDER_INDEPENDENT)
                    .writeMask(WriteMask.COLOR)
                    .backfaceCulling(false)
                    .build();
    private static final MeshPart PART;

    static {
        var mesh = PackedQuadMesh.of(QUAD, WHITE, LIGHT);
        PART = MeshPart.create(mesh, MATERIAL);
    }

    private final BlockPos anchor;
    private final UvTransformedInstance[][] labels;
    private final Matrix4f[] lastPoses;
    private final int[] lastHealth, lastFlame, lastReact;
    private final EnumSymbol[] lastSymbols;
    private final boolean[] visible;
    private final Matrix4f local = new Matrix4f();
    private final Matrix4f world = new Matrix4f();

    public HazardDiamondVisual(
            VisualizationContext context, Level level, BlockPos anchor, int count) {
        super(context, level, 0);
        this.anchor = anchor.immutable();
        labels = new UvTransformedInstance[count][5];
        lastPoses = new Matrix4f[count];
        lastHealth = new int[count];
        lastFlame = new int[count];
        lastReact = new int[count];
        lastSymbols = new EnumSymbol[count];
        visible = new boolean[count];
        for (int i = 0; i < count; i++) lastPoses[i] = new Matrix4f();
        for (int i = 0; i < count; i++)
            for (int j = 0; j < 5; j++) {
                labels[i][j] =
                        instancerProvider()
                                .instancer(InstanceTypes.UV_TRANSFORMED, PART.model())
                                .createInstance();
                labels[i][j].setVisible(false);
            }
    }

    public static void initModels() {}

    private static float digitLeft(int value) {
        if (value < 0 || value >= 6) return -1;
        return (value == 0 ? 125 : 5 + (value - 1) * 24) * PIXEL;
    }

    private static float digitRight(int value) {
        float left = digitLeft(value);
        return left < 0 ? -1F : left + 20 * PIXEL;
    }

    public void update(
            int index, Matrix4fc base, int health, int flame, int react, EnumSymbol symbol) {
        if (visible[index]
                && lastHealth[index] == health
                && lastFlame[index] == flame
                && lastReact[index] == react
                && lastSymbols[index] == symbol
                && lastPoses[index].equals(base)) return;
        visible[index] = true;
        lastHealth[index] = health;
        lastFlame[index] = flame;
        lastReact[index] = react;
        lastSymbols[index] = symbol;
        lastPoses[index].set(base);
        UvTransformedInstance[] row = labels[index];
        write(
                0,
                row[0],
                base,
                0F,
                0F,
                0F,
                .5F,
                .5F,
                5 * PIXEL,
                144 * PIXEL,
                45 * PIXEL,
                184 * PIXEL);
        write(
                1,
                row[1],
                base,
                .01F,
                0F,
                33 * SCALE,
                14 * SCALE,
                10 * SCALE,
                digitLeft(health),
                digitRight(health),
                5 * PIXEL,
                33 * PIXEL);
        write(
                2,
                row[2],
                base,
                .01F,
                33 * SCALE,
                0F,
                14 * SCALE,
                10 * SCALE,
                digitLeft(flame),
                digitRight(flame),
                5 * PIXEL,
                33 * PIXEL);
        write(
                3,
                row[3],
                base,
                .01F,
                0F,
                -33 * SCALE,
                14 * SCALE,
                10 * SCALE,
                digitLeft(react),
                digitRight(react),
                5 * PIXEL,
                33 * PIXEL);
        if (symbol == EnumSymbol.NONE) {
            hide(row[4]);
        } else {
            float size = 59F / 2F * SCALE;
            write(
                    4,
                    row[4],
                    base,
                    .01F,
                    -33 * SCALE,
                    0F,
                    size,
                    size,
                    symbol.x * PIXEL,
                    (symbol.x + 59) * PIXEL,
                    symbol.y * PIXEL,
                    (symbol.y + 59) * PIXEL);
        }
    }

    public void hide(int index) {
        if (!visible[index]) return;
        visible[index] = false;
        for (int label = 0; label < labels[index].length; label++) hide(labels[index][label]);
    }

    private void write(
            int label,
            UvTransformedInstance instance,
            Matrix4fc base,
            float x,
            float y,
            float z,
            float halfY,
            float halfZ,
            float left,
            float right,
            float top,
            float bottom) {
        if (left < 0F || right < 0F) {
            hide(instance);
            return;
        }
        local.set(base).translate(x, y, z).scale(1F, halfY, halfZ);
        world.translation(
                        anchor.getX() - renderOrigin().getX(),
                        anchor.getY() - renderOrigin().getY(),
                        anchor.getZ() - renderOrigin().getZ())
                .mul(local);
        instance.setVisible(true);
        instance.setTransform(world).light(0);
        instance.uvRegion(left, top, right - left, bottom - top).setChanged();
    }

    private void hide(UvTransformedInstance instance) {
        instance.setVisible(false);
    }

    @Override
    protected void _delete() {
        for (int i = 0; i < labels.length; i++)
            for (int j = 0; j < labels[i].length; j++) {
                labels[i][j].delete();
            }
    }
}
