// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.blocks.ModBlocks;
import com.hbm.tileentity.machine.BlockEntityMultiblock;
import com.hbm.util.GameTime;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visual.ShaderLightVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.UvTransformedInstance;
import java.util.ArrayList;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

public final class MultiblockStructPreviewVisual
        extends HbmDynamicBlockEntityVisual<BlockEntityMultiblock> implements ShaderLightVisual {
    private static final Direction[] COLUMN_SIDES = {
        Direction.EAST, Direction.SOUTH, Direction.WEST, Direction.NORTH
    };
    private final ArrayList<UvTransformedInstance> cubes = new ArrayList<>();
    private final UvTransformedInstance[][] columns;
    private final @Nullable TextureAtlasSprite scaffold;
    private final AABB bounds;
    private final Matrix4f pose = new Matrix4f();
    private int phase = -1;

    public MultiblockStructPreviewVisual(
            VisualizationContext context, BlockEntityMultiblock blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);
        boolean large = BlockEntityMultiblock.isLarge(blockState);
        int radius =
                large ? BlockEntityMultiblock.TABLE_RADIUS : BlockEntityMultiblock.COMPACT_RADIUS;
        int height = large ? BlockEntityMultiblock.SCAFFOLD_HEIGHT : 1;
        bounds =
                new AABB(
                                pos.getX() - radius,
                                pos.getY(),
                                pos.getZ() - radius,
                                pos.getX() + radius + 1,
                                pos.getY() + height,
                                pos.getZ() + radius + 1)
                        .inflate(1);
        var pad = sprite(ModBlocks.STRUCT_LAUNCHER.get());
        BlockEntityMultiblock.forEachPadCell(radius, (x, z) -> cube(pad, x, 0, z));
        columns = new UvTransformedInstance[large ? 4 : 0][];
        scaffold = large ? sprite(ModBlocks.STRUCT_SCAFFOLD.get()) : null;
        if (large) {
            for (int side = 0; side < 4; side++) {
                columns[side] = new UvTransformedInstance[height - 1];
                int x = COLUMN_SIDES[side].getStepX() * BlockEntityMultiblock.SCAFFOLD_OFFSET;
                int z = COLUMN_SIDES[side].getStepZ() * BlockEntityMultiblock.SCAFFOLD_OFFSET;
                for (int y = 1; y < height; y++) {
                    var instance = cube(scaffold, x, y, z);
                    instance.setVisible(false);
                    columns[side][y - 1] = instance;
                }
            }
            updatePhase();
        }
    }

    private static TextureAtlasSprite sprite(Block block) {
        return Minecraft.getInstance()
                .getModelManager()
                .getBlockStateModelSet()
                .getParticleMaterial(block.defaultBlockState())
                .sprite();
    }

    private UvTransformedInstance cube(TextureAtlasSprite sprite, int x, int y, int z) {
        var instance =
                instancerProvider()
                        .instancer(InstanceTypes.UV_TRANSFORMED, PreviewCube.SOUTH_MODEL)
                        .createInstance();
        instance.setTransform(
                        pose.translation(
                                visualPos.getX() + x, visualPos.getY() + y, visualPos.getZ() + z))
                .setChanged();
        region(instance, sprite);
        VisualTextures.animated(sprite);
        cubes.add(instance);
        return instance;
    }

    private static void region(UvTransformedInstance instance, TextureAtlasSprite sprite) {
        instance.uvRegion(
                sprite.getU0(),
                sprite.getV0(),
                sprite.getU1() - sprite.getU0(),
                sprite.getV1() - sprite.getV0());
    }

    private void updatePhase() {
        int wanted = (int) (GameTime.millis() % 4000 / 1000);
        if (wanted == phase) return;
        if (phase >= 0) for (var instance : columns[phase]) instance.setVisible(false);
        int x = COLUMN_SIDES[wanted].getStepX() * BlockEntityMultiblock.SCAFFOLD_OFFSET;
        int z = COLUMN_SIDES[wanted].getStepZ() * BlockEntityMultiblock.SCAFFOLD_OFFSET;
        for (int i = 0; i < columns[wanted].length; i++) {
            var instance = columns[wanted][i];
            instance.setVisible(true);
            region(instance, scaffold);
            instance.setTransform(
                            pose.translation(
                                    visualPos.getX() + x,
                                    visualPos.getY() + i + 1,
                                    visualPos.getZ() + z))
                    .setChanged();
        }
        phase = wanted;
    }

    @Override
    protected void frame(Context context) {
        if (columns.length != 0) updatePhase();
    }

    @Override
    protected AABB getRenderBoundingBox() {
        return bounds;
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {}

    @Override
    protected void _delete() {
        cubes.forEach(Instance::delete);
    }
}
