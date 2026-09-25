// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.blocks.machine.FoundryChannel;
import com.hbm.client.render.FoundryFaces;
import com.hbm.tileentity.machine.BlockEntityFoundryChannel;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.AffineUvTransformedInstance;
import dev.engine_room.flywheel.lib.visual.SimpleTickableVisual;
import java.util.function.Consumer;
import net.minecraft.core.Direction;
import net.minecraft.util.LightCoordsUtil;
import org.joml.Matrix4f;

public final class FoundryChannelVisual extends HbmBlockEntityVisual<BlockEntityFoundryChannel>
        implements SimpleTickableVisual {
    private static final MeshPart[] MOLTEN_FACES = buildMoltenFaces();
    private final AffineUvTransformedInstance[] faces = new AffineUvTransformedInstance[9];
    private final Matrix4f local = new Matrix4f(), pose = new Matrix4f();
    private int used;
    private float lastTop = Float.NaN;
    private int lastColor;
    private boolean lastPresent;
    private boolean initialized;

    public FoundryChannelVisual(
            VisualizationContext context,
            BlockEntityFoundryChannel blockEntity,
            float partialTick) {
        super(context, blockEntity, partialTick);
        updateMovingParts();
    }

    public static void initModels() {}

    private static MeshPart[] buildMoltenFaces() {
        var faces = new MeshPart[Direction.values().length];
        for (Direction direction : Direction.values())
            faces[direction.ordinal()] =
                    FoundryTankVisual.molten(
                            MeshPart.face(direction, FoundryTankVisual.TERRAIN_MOLTEN));
        return faces;
    }

    @Override
    public void tick(Context context) {
        updateMovingParts();
    }

    public void updateMovingParts() {
        boolean present = blockEntity.amount > 0 && blockEntity.type != null;
        float top =
                present
                        ? .125F + (float) (blockEntity.amount * .25D / blockEntity.getCapacity())
                        : 0F;
        int color = present ? FoundryFaces.brightenMolten(blockEntity.type.moltenColor) : 0;
        if (initialized && present == lastPresent && top == lastTop && color == lastColor) return;
        lastPresent = present;
        lastTop = top;
        lastColor = color;
        initialized = true;
        used = 0;
        if (present) {
            var block = blockState;
            face(Direction.UP, .375F, .125F, .375F, .625F, top, .625F, color);
            if (block.getValue(FoundryChannel.EAST)) {
                face(Direction.UP, .625F, .125F, .3125F, 1F, top, .6875F, color);
                face(Direction.EAST, .625F, .125F, .3125F, 1F, top, .6875F, color);
            }
            if (block.getValue(FoundryChannel.WEST)) {
                face(Direction.UP, 0F, .125F, .3125F, .375F, top, .6875F, color);
                face(Direction.WEST, 0F, .125F, .3125F, .375F, top, .6875F, color);
            }
            if (block.getValue(FoundryChannel.SOUTH)) {
                face(Direction.UP, .3125F, .125F, .625F, .6875F, top, 1F, color);
                face(Direction.SOUTH, .3125F, .125F, .625F, .6875F, top, 1F, color);
            }
            if (block.getValue(FoundryChannel.NORTH)) {
                face(Direction.UP, .3125F, .125F, 0F, .6875F, top, .375F, color);
                face(Direction.NORTH, .3125F, .125F, 0F, .6875F, top, .375F, color);
            }
        }
        for (int i = used; i < faces.length; i++) if (faces[i] != null) faces[i].setVisible(false);
    }

    private void face(
            Direction direction,
            float x0,
            float y0,
            float z0,
            float x1,
            float y1,
            float z1,
            int color) {
        int i = used++;
        float dx = x1 - x0, dy = y1 - y0, dz = z1 - z0;
        local.translation(x0, y0, z0).scale(dx, dy, dz);
        pose.translation(visualPos.getX(), visualPos.getY(), visualPos.getZ()).mul(local);
        if (faces[i] == null) {
            var model = MOLTEN_FACES[direction.ordinal()];
            faces[i] =
                    instancerProvider()
                            .instancer(AffineUvTransformedInstance.TYPE, model.model())
                            .createInstance();
        }
        float su = direction.getAxis() == Direction.Axis.X ? dz : dx;
        float sv = direction.getAxis() == Direction.Axis.Y ? dz : dy;
        float ou = direction.getAxis() == Direction.Axis.X ? z0 : x0;
        float ov = direction.getAxis() == Direction.Axis.Y ? z0 : 1F - y1;
        faces[i].setVisible(true);
        faces[i].setTransform(pose).colorArgb(color).light(LightCoordsUtil.pack(15, 0));
        faces[i].uv(su, 0, 0, sv, ou, ov).setChanged();
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {}

    @Override
    protected void _delete() {
        for (var face : faces) if (face != null) face.delete();
    }
}
