// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.api.fluidmk2.FluidPipeTintData;
import com.hbm.blocks.network.FluidPipeAnchorBlock;
import com.hbm.main.ResourceManager;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.tileentity.network.BlockEntityPipeAnchor;
import com.hbm.util.ChunkUtil;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visual.ShaderLightVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.visual.SimpleTickableVisual;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

public final class PipeAnchorVisual extends HbmBlockEntityVisual<BlockEntityPipeAnchor>
        implements SimpleTickableVisual, ShaderLightVisual {
    private static final int PIPE = ResourceManager.pipe_anchor.partId("Pipe");
    private static final int RING = ResourceManager.pipe_anchor.partId("Ring");
    private static final HFRWavefrontObject MODEL = ResourceManager.pipe_anchor;
    private static final MeshPart PIPE_PART =
            MeshPart.obj(
                    MODEL.groups[PIPE],
                    MODEL.smoothing(),
                    MeshPart.litCutout(ResourceManager.pipe_anchor_tex));
    private static final MeshPart RING_PART =
            MeshPart.obj(
                    MODEL.groups[RING],
                    MODEL.smoothing(),
                    MeshPart.litCutout(ResourceManager.pipe_anchor_tex));
    private final AABB bodyBounds;
    private TransformedInstance[] pipes = new TransformedInstance[0];
    private TransformedInstance[] rings = new TransformedInstance[0];
    private final Matrix4f pipePose = new Matrix4f();
    private final Matrix4f ringPose = new Matrix4f();
    private final Matrix4f rootPose = new Matrix4f();
    private final Matrix4f world = new Matrix4f();
    private final ArrayList<Run> runs = new ArrayList<>();
    private int runCount;
    private boolean[] runVisible = new boolean[0];
    private final double[] bounds = new double[6];
    private AABB renderBounds;
    private @Nullable AABB lastLightBounds;
    private long[] lastConnections = new long[0];
    private Fluid lastFluid = Fluids.EMPTY;
    private @Nullable BlockEntityPipeAnchor[] lastPeers = new BlockEntityPipeAnchor[0];
    private @Nullable Fluid[] lastPeerFluids = new Fluid[0];
    private boolean built;

    public PipeAnchorVisual(
            VisualizationContext context, BlockEntityPipeAnchor blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);
        Direction facing = blockState.getValue(FluidPipeAnchorBlock.FACING);
        Matrix4f bodyLocal =
                new Matrix4f()
                        .translation(.5F, .5F, .5F)
                        .rotate(facing.getRotation())
                        .translate(0F, -.5F, 0F);
        bodyBounds = LightBounds.of(MODEL, "Anchor", bodyLocal, pos);
        renderBounds = new AABB(pos).inflate(1);
        updateMovingParts(partialTick);
    }

    public static void initModels() {}

    private static boolean dominant(BlockPos first, BlockPos second) {
        if (first.getX() != second.getX()) return first.getX() < second.getX();
        if (first.getY() != second.getY()) return first.getY() < second.getY();
        if (first.getZ() != second.getZ()) return first.getZ() < second.getZ();
        return false;
    }

    private static int lighten(int rgb, double factor) {
        int r = (rgb >> 16) & 0xFF, g = (rgb >> 8) & 0xFF, b = rgb & 0xFF;
        r = (int) (r + (255 - r) * factor);
        g = (int) (g + (255 - g) * factor);
        b = (int) (b + (255 - b) * factor);
        return (r << 16) | (g << 8) | b;
    }

    @Override
    public void tick(Context context) {
        updateMovingParts(0F);
    }

    public void updateMovingParts(float partialTick) {
        if (!inputsChanged() && built) return;
        built = true;
        collectRuns();
        ensureCapacity(runCount);
        LightBounds.resetBounds(bounds, bodyBounds);
        for (int i = 0; i < runCount; i++) {
            Run run = runs.get(i);
            rootPose.identity()
                    .translate(.5F, .5F, .5F)
                    .rotateY(run.yaw * Mth.DEG_TO_RAD)
                    .rotateX((90F - run.pitch) * Mth.DEG_TO_RAD);
            pipePose.set(rootPose).scale(1F, run.length, 1F).translate(0F, -.5F, 0F);
            ringPose.set(rootPose).translate(0F, run.length / 2F - 1.5F, 0F);
            writeRun(i, run.color);
            bounds[0] = Math.min(bounds[0], run.minX);
            bounds[1] = Math.min(bounds[1], run.minY);
            bounds[2] = Math.min(bounds[2], run.minZ);
            bounds[3] = Math.max(bounds[3], run.maxX);
            bounds[4] = Math.max(bounds[4], run.maxY);
            bounds[5] = Math.max(bounds[5], run.maxZ);
        }
        for (int i = runCount; i < pipes.length; i++) hide(i);
        renderBounds = LightBounds.sectionBounds(bounds, renderBounds);
        lastLightBounds = LightBounds.sections(lightSections, bounds, lastLightBounds);
        refreshVisibleBounds();
    }

    private boolean inputsChanged() {
        long[] connections = blockEntity.connections;
        boolean changed =
                blockEntity.fluid != lastFluid || !Arrays.equals(connections, lastConnections);
        if (changed) {
            lastConnections = connections.clone();
            lastFluid = blockEntity.fluid;
            lastPeers = new BlockEntityPipeAnchor[connections.length];
            lastPeerFluids = new Fluid[connections.length];
        }
        var level = blockEntity.getLevel();
        for (int i = 0; i < connections.length; i++) {
            BlockEntityPipeAnchor peer =
                    level != null
                                    && ChunkUtil.blockEntityIfLoaded(
                                                    level, BlockPos.of(connections[i]))
                                            instanceof BlockEntityPipeAnchor other
                            ? other
                            : null;
            @Nullable Fluid fluid = peer == null ? null : peer.fluid;
            if (peer != lastPeers[i] || fluid != lastPeerFluids[i]) {
                lastPeers[i] = peer;
                lastPeerFluids[i] = fluid;
                changed = true;
            }
        }
        return changed;
    }

    private void collectRuns() {
        runCount = 0;
        if (blockEntity.fluid == Fluids.EMPTY && blockEntity.connections.length == 0) return;
        int color = lighten(ARGB.transparent(FluidPipeTintData.colorFor(blockEntity.fluid)), .25D);
        for (int i = 0; i < lastConnections.length; i++) {
            BlockEntityPipeAnchor other = lastPeers[i];
            if (other == null || lastPeerFluids[i] != lastFluid) continue;
            BlockPos otherPos = BlockPos.of(lastConnections[i]);
            if (!dominant(pos, otherPos)) continue;
            double dx = otherPos.getX() - pos.getX(), dy = otherPos.getY() - pos.getY();
            double dz = otherPos.getZ() - pos.getZ();
            double horizontal = Mth.length(dx, dz);
            if (runCount == runs.size()) runs.add(new Run());
            runs.get(runCount++)
                    .set(
                            (float) Math.toDegrees(Math.atan2(dx, dz)),
                            (float) Math.toDegrees(Math.atan2(dy, horizontal)),
                            (float) Mth.length(dx, dy, dz),
                            color,
                            pos,
                            otherPos);
        }
    }

    private void ensureCapacity(int count) {
        if (count <= pipes.length) return;
        int old = pipes.length;
        pipes = Arrays.copyOf(pipes, count);
        rings = Arrays.copyOf(rings, count);
        runVisible = Arrays.copyOf(runVisible, count);
        for (int i = old; i < count; i++) {
            pipes[i] =
                    instancerProvider()
                            .instancer(InstanceTypes.TRANSFORMED, PIPE_PART.model())
                            .createInstance();
            rings[i] =
                    instancerProvider()
                            .instancer(InstanceTypes.TRANSFORMED, RING_PART.model())
                            .createInstance();
            pipes[i].setVisible(false);
            rings[i].setVisible(false);
        }
    }

    private void writeRun(int index, int color) {
        if (!runVisible[index]) {
            pipes[index].setVisible(true);
            rings[index].setVisible(true);
            runVisible[index] = true;
        }
        world.translation(visualPos.getX(), visualPos.getY(), visualPos.getZ()).mul(pipePose);
        pipes[index].setTransform(world).light(0).colorArgb(ARGB.opaque(color)).setChanged();
        world.translation(visualPos.getX(), visualPos.getY(), visualPos.getZ()).mul(ringPose);
        rings[index].setTransform(world).light(0).colorArgb(-1).setChanged();
    }

    private void hide(int index) {
        if (!runVisible[index]) return;
        pipes[index].setVisible(false);
        rings[index].setVisible(false);
        runVisible[index] = false;
    }

    @Override
    protected AABB getRenderBoundingBox() {
        return renderBounds;
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        for (TransformedInstance pipe : pipes) consumer.accept(pipe);
        for (TransformedInstance ring : rings) consumer.accept(ring);
    }

    @Override
    protected void _delete() {
        for (TransformedInstance pipe : pipes) pipe.delete();
        for (TransformedInstance ring : rings) ring.delete();
    }

    private static final class Run {
        float yaw, pitch, length;
        int color;
        double minX, minY, minZ, maxX, maxY, maxZ;

        void set(
                float yaw, float pitch, float length, int color, BlockPos origin, BlockPos target) {
            this.yaw = yaw;
            this.pitch = pitch;
            this.length = length;
            this.color = color;
            minX = Math.min(origin.getX(), target.getX()) + .5D;
            minY = Math.min(origin.getY(), target.getY()) + .5D;
            minZ = Math.min(origin.getZ(), target.getZ()) + .5D;
            maxX = Math.max(origin.getX(), target.getX()) + .5D;
            maxY = Math.max(origin.getY(), target.getY()) + .5D;
            maxZ = Math.max(origin.getZ(), target.getZ()) + .5D;
        }
    }
}
