// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.config.RenderConfig;
import com.hbm.main.ResourceManager;
import com.hbm.tileentity.network.BlockEntityPylonBase;
import com.hbm.util.ChunkUtil;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.material.CardinalLightingMode;
import dev.engine_room.flywheel.api.material.Material;
import dev.engine_room.flywheel.api.visual.SectionTrackedVisual.SectionCollector;
import dev.engine_room.flywheel.api.visual.ShaderLightVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.AffineUvTransformedInstance;
import dev.engine_room.flywheel.lib.material.SimpleMaterial;
import dev.engine_room.flywheel.lib.visual.SimpleTickableVisual;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

public final class PylonWiresVisual extends HbmBlockEntityVisual<BlockEntityPylonBase>
        implements SimpleTickableVisual, ShaderLightVisual {
    private static final double GIRTH = .03125D, MAX_SAG = 2.5D;
    private static final int SEGMENTS = 10;
    private static final PackedQuadMesh WIRE_MESH =
            PackedQuadMesh.of(
                    new float[] {
                        0, 0, 0, 0, 0, 0, 0, 1, 0, 1, 0, 0, 1, 0, 0, 1, 1, 1, 0, 1, 1, 0, 0, 1, 1,
                        0, 0, 1, 0, 0, 0, 1
                    },
                    new int[] {-1, -1, -1, -1},
                    new int[4]);
    private static final MeshPart[] WIRE_MODELS = {
        model(ResourceManager.wire_tex), model(ResourceManager.wire_greyscale_tex)
    };
    private final ArrayList<Segment> path = new ArrayList<>();
    private final ArrayList<AffineUvTransformedInstance> strips = new ArrayList<>();
    private final Matrix4f local = new Matrix4f(), pose = new Matrix4f();
    private @Nullable MeshPart model;
    private int used;
    private long[] lastConnections = new long[0];
    private @Nullable BlockEntityPylonBase[] lastPeers = new BlockEntityPylonBase[0];
    private @Nullable BlockState[] lastPeerStates = new BlockState[0];
    private int lastColor;
    private boolean lastHang;
    private boolean built;

    public PylonWiresVisual(
            VisualizationContext context, BlockEntityPylonBase blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);
        updateMovingParts(partialTick);
    }

    public static void initModels() {}

    private static MeshPart model(Identifier texture) {
        Material material =
                SimpleMaterial.builderOf(MeshPart.litCutout(texture))
                        .ambientOcclusion(false)
                        .cardinalLightingMode(CardinalLightingMode.OFF)
                        .backfaceCulling(false)
                        .build();
        return MeshPart.create(WIRE_MESH, material);
    }

    private static boolean crossed(BlockEntityPylonBase first, BlockEntityPylonBase second) {
        Direction a = facingOf(first);
        Direction b = facingOf(second);
        return (a == Direction.EAST && b == Direction.NORTH)
                || (a == Direction.NORTH && b == Direction.EAST);
    }

    private static @Nullable Direction facingOf(BlockEntityPylonBase pylon) {
        BlockState state = pylon.getBlockState();
        return state.hasProperty(BlockMultiblockCore.FACING)
                ? state.getValue(BlockMultiblockCore.FACING)
                : null;
    }

    @Override
    public void tick(Context context) {
        updateMovingParts(0);
    }

    public void updateMovingParts(float partialTick) {
        if (!inputsChanged() && built) return;
        built = true;
        path.clear();
        Vec3[] mine = blockEntity.mounts();
        for (int c = 0; c < lastConnections.length; c++) {
            BlockEntityPylonBase other = lastPeers[c];
            if (other == null) continue;
            BlockPos peer = BlockPos.of(lastConnections[c]);
            Vec3[] theirs = other.mounts();
            int count = Math.min(mine.length, theirs.length);
            int shift = count == 4 && crossed(blockEntity, other) ? 2 : 0;
            for (int i = 0; i < count; i++) {
                Vec3 from = mine[i % mine.length];
                Vec3 to =
                        theirs[(i + shift) % theirs.length].add(
                                peer.getX() - pos.getX(),
                                peer.getY() - pos.getY(),
                                peer.getZ() - pos.getZ());
                half(from, from.add(to.subtract(from).scale(.5D)), lastHang);
            }
        }
        var wanted = WIRE_MODELS[lastColor == 0 ? 0 : 1];
        if (wanted != model) {
            clear();
            model = wanted;
        }
        int color = lastColor == 0 ? -1 : 0xFF000000 | lastColor;
        used = 0;
        for (var segment : path) {
            Vec3 delta = segment.to.subtract(segment.from);
            int wrap = (int) Math.ceil(delta.length() * 8D);
            double jx = segment.jX, jz = segment.jZ;
            if (delta.x + delta.z < 0) {
                wrap = -wrap;
                jx = -jx;
                jz = -jz;
            }
            strip(segment.from, segment.to, segment.iX, segment.iY, segment.iZ, wrap, color);
            strip(segment.from, segment.to, jx, 0, jz, wrap, color);
        }
        while (strips.size() > used) {
            strips.removeLast().delete();
        }
        publishSections();
    }

    private boolean inputsChanged() {
        long[] connections = blockEntity.connections;
        boolean changed =
                blockEntity.color != lastColor
                        || RenderConfig.cableHang != lastHang
                        || !Arrays.equals(connections, lastConnections);
        if (changed) {
            lastConnections = connections.clone();
            lastColor = blockEntity.color;
            lastHang = RenderConfig.cableHang;
            lastPeers = new BlockEntityPylonBase[connections.length];
            lastPeerStates = new BlockState[connections.length];
        }
        for (int i = 0; i < connections.length; i++) {
            BlockEntityPylonBase peer =
                    ChunkUtil.blockEntityIfLoaded(level, BlockPos.of(connections[i]))
                                    instanceof BlockEntityPylonBase other
                            ? other
                            : null;
            @Nullable BlockState state = peer == null ? null : peer.getBlockState();
            if (peer != lastPeers[i] || state != lastPeerStates[i]) {
                lastPeers[i] = peer;
                lastPeerStates[i] = state;
                changed = true;
            }
        }
        return changed;
    }

    private void strip(Vec3 from, Vec3 to, double ox, double oy, double oz, int wrap, int color) {
        double ax = to.x - from.x, ay = to.y - from.y, az = to.z - from.z;
        double bx = -2 * ox, by = -2 * oy, bz = -2 * oz;
        double nx = ay * bz - az * by, ny = az * bx - ax * bz, nz = ax * by - ay * bx;
        double length = Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (length == 0) return;
        local.identity()
                .m00((float) ax)
                .m01((float) ay)
                .m02((float) az)
                .m10((float) bx)
                .m11((float) by)
                .m12((float) bz)
                .m20((float) (nx / length))
                .m21((float) (ny / length))
                .m22((float) (nz / length))
                .m30((float) (from.x + ox))
                .m31((float) (from.y + oy))
                .m32((float) (from.z + oz));
        pose.translation(visualPos.getX(), visualPos.getY(), visualPos.getZ()).mul(local);
        if (used == strips.size()) {
            strips.add(
                    instancerProvider()
                            .instancer(AffineUvTransformedInstance.TYPE, model.model())
                            .createInstance());
        }
        var strip = strips.get(used);
        strip.setTransform(pose).colorArgb(color).light(0);
        strip.uv(wrap, 0, 0, 1, 0, 0).setChanged();
        used++;
    }

    @Override
    public void setSectionCollector(SectionCollector collector) {
        lightSections = collector;
        publishSections();
    }

    private void publishSections() {
        if (lightSections == null) return;
        var sections = new LongOpenHashSet();
        for (var segment : path) {
            Vec3 delta = segment.to.subtract(segment.from);
            int steps = Math.max(1, (int) Math.ceil(delta.length() / 8));
            for (int i = 0; i <= steps; i++) {
                Vec3 point = segment.from.add(delta.scale((double) i / steps));
                int x = SectionPos.blockToSectionCoord(Mth.floor(pos.getX() + point.x));
                int y = SectionPos.blockToSectionCoord(Mth.floor(pos.getY() + point.y));
                int z = SectionPos.blockToSectionCoord(Mth.floor(pos.getZ() + point.z));
                for (int dx = -1; dx <= 1; dx++)
                    for (int dy = -1; dy <= 1; dy++)
                        for (int dz = -1; dz <= 1; dz++)
                            sections.add(SectionPos.asLong(x + dx, y + dy, z + dz));
            }
        }
        lightSections.sections(sections);
    }

    @Override
    protected AABB getRenderBoundingBox() {
        double reach = blockEntity.maxWireLength() + 1D;
        return new AABB(
                pos.getX() - reach,
                pos.getY() - reach,
                pos.getZ() - reach,
                pos.getX() + reach + 1D,
                pos.getY() + blockEntity.familyLift() + 1D,
                pos.getZ() + reach + 1D);
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {}

    private void clear() {
        for (var strip : strips) strip.delete();
        strips.clear();
    }

    @Override
    protected void _delete() {
        clear();
    }

    private void half(Vec3 from, Vec3 mid, boolean hang) {
        double dX = from.x - mid.x, dY = from.y - mid.y, dZ = from.z - mid.z;
        double yaw = Math.atan2(dX, dZ);
        double pitch = Math.atan2(dY, Mth.length(dX, dZ));
        double newPitch = pitch + Math.PI * 0.5D;
        double newYaw = yaw + Math.PI * 0.5D;
        double iZ = Math.cos(yaw) * Math.cos(newPitch) * GIRTH;
        double iX = Math.sin(yaw) * Math.cos(newPitch) * GIRTH;
        double iY = Math.sin(newPitch) * GIRTH;
        double jZ = Math.cos(newYaw) * GIRTH;
        double jX = Math.sin(newYaw) * GIRTH;

        if (!hang) {
            path.add(new Segment(from, mid, iX, iY, iZ, jX, jZ));
            return;
        }

        double sag = Math.min(Math.sqrt(dX * dX + dY * dY + dZ * dZ) / 15D, MAX_SAG);
        Vec3 delta = mid.subtract(from);
        for (int j = 0; j < SEGMENTS; j++) {
            double sagJ = Math.sin((double) j / SEGMENTS * Math.PI * 0.5) * sag;
            double sagK = Math.sin((j + 1D) / SEGMENTS * Math.PI * 0.5) * sag;
            Vec3 start = from.add(delta.scale((double) j / SEGMENTS)).subtract(0, sagJ, 0);
            Vec3 end = from.add(delta.scale((j + 1D) / SEGMENTS)).subtract(0, sagK, 0);
            path.add(new Segment(start, end, iX, iY, iZ, jX, jZ));
        }
    }

    private record Segment(
            Vec3 from, Vec3 to, double iX, double iY, double iZ, double jX, double jZ) {}
}
