// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.render.loader.GroupObject;
import com.hbm.render.loader.HFRWavefrontObject;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.api.visual.SectionTrackedVisual.SectionCollector;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4fc;
import org.jspecify.annotations.Nullable;

public final class LightBounds {
    private LightBounds() {}

    public static AABB lightBounds(Model model, Matrix4fc localPose, BlockPos anchor) {
        var sphere = model.boundingSphere();
        double x =
                anchor.getX()
                        + localPose.m00() * sphere.x()
                        + localPose.m10() * sphere.y()
                        + localPose.m20() * sphere.z()
                        + localPose.m30();
        double y =
                anchor.getY()
                        + localPose.m01() * sphere.x()
                        + localPose.m11() * sphere.y()
                        + localPose.m21() * sphere.z()
                        + localPose.m31();
        double z =
                anchor.getZ()
                        + localPose.m02() * sphere.x()
                        + localPose.m12() * sphere.y()
                        + localPose.m22() * sphere.z()
                        + localPose.m32();
        double dx =
                sphere.w()
                        * (Math.abs(localPose.m00())
                                + Math.abs(localPose.m10())
                                + Math.abs(localPose.m20()));
        double dy =
                sphere.w()
                        * (Math.abs(localPose.m01())
                                + Math.abs(localPose.m11())
                                + Math.abs(localPose.m21()));
        double dz =
                sphere.w()
                        * (Math.abs(localPose.m02())
                                + Math.abs(localPose.m12())
                                + Math.abs(localPose.m22()));
        return new AABB(x - dx, y - dy, z - dz, x + dx, y + dy, z + dz).inflate(1);
    }

    public static AABB of(GroupObject group, Matrix4fc localPose, BlockPos anchor) {
        float[] b = group.bounds();
        double[] out = {
            Double.POSITIVE_INFINITY,
            Double.POSITIVE_INFINITY,
            Double.POSITIVE_INFINITY,
            Double.NEGATIVE_INFINITY,
            Double.NEGATIVE_INFINITY,
            Double.NEGATIVE_INFINITY
        };
        for (int corner = 0; corner < 8; corner++) {
            float x = b[(corner & 1) == 0 ? 0 : 3],
                    y = b[(corner & 2) == 0 ? 1 : 4],
                    z = b[(corner & 4) == 0 ? 2 : 5];
            double px =
                    localPose.m00() * x
                            + localPose.m10() * y
                            + localPose.m20() * z
                            + localPose.m30();
            double py =
                    localPose.m01() * x
                            + localPose.m11() * y
                            + localPose.m21() * z
                            + localPose.m31();
            double pz =
                    localPose.m02() * x
                            + localPose.m12() * y
                            + localPose.m22() * z
                            + localPose.m32();
            out[0] = Math.min(out[0], px);
            out[1] = Math.min(out[1], py);
            out[2] = Math.min(out[2], pz);
            out[3] = Math.max(out[3], px);
            out[4] = Math.max(out[4], py);
            out[5] = Math.max(out[5], pz);
        }
        return new AABB(out[0], out[1], out[2], out[3], out[4], out[5]).move(anchor).inflate(1);
    }

    public static AABB of(
            HFRWavefrontObject mesh, String group, Matrix4fc localPose, BlockPos anchor) {
        return of(mesh.groups[mesh.partId(group)], localPose, anchor);
    }

    public static @Nullable AABB sections(
            @Nullable SectionCollector collector, AABB bounds, @Nullable AABB previous) {
        if (collector == null) return null;
        int x0 = section(bounds.minX), y0 = section(bounds.minY), z0 = section(bounds.minZ);
        int x1 = section(bounds.maxX), y1 = section(bounds.maxY), z1 = section(bounds.maxZ);
        if (previous != null
                && section(previous.minX) == x0
                && section(previous.minY) == y0
                && section(previous.minZ) == z0
                && section(previous.maxX) == x1
                && section(previous.maxY) == y1
                && section(previous.maxZ) == z1) return previous;
        var sections = new LongOpenHashSet();
        for (int x = x0; x <= x1; x++)
            for (int y = y0; y <= y1; y++)
                for (int z = z0; z <= z1; z++) sections.add(SectionPos.asLong(x, y, z));
        collector.sections(sections);
        return bounds;
    }

    public static void resetBounds(double[] bounds, AABB fixed) {
        bounds[0] = fixed.minX;
        bounds[1] = fixed.minY;
        bounds[2] = fixed.minZ;
        bounds[3] = fixed.maxX;
        bounds[4] = fixed.maxY;
        bounds[5] = fixed.maxZ;
    }

    public static void includeLightBounds(
            double[] bounds, Model model, Matrix4fc pose, BlockPos anchor) {
        var sphere = model.boundingSphere();
        double x =
                anchor.getX()
                        + pose.m00() * sphere.x()
                        + pose.m10() * sphere.y()
                        + pose.m20() * sphere.z()
                        + pose.m30();
        double y =
                anchor.getY()
                        + pose.m01() * sphere.x()
                        + pose.m11() * sphere.y()
                        + pose.m21() * sphere.z()
                        + pose.m31();
        double z =
                anchor.getZ()
                        + pose.m02() * sphere.x()
                        + pose.m12() * sphere.y()
                        + pose.m22() * sphere.z()
                        + pose.m32();
        double dx =
                sphere.w() * (Math.abs(pose.m00()) + Math.abs(pose.m10()) + Math.abs(pose.m20()))
                        + 1;
        double dy =
                sphere.w() * (Math.abs(pose.m01()) + Math.abs(pose.m11()) + Math.abs(pose.m21()))
                        + 1;
        double dz =
                sphere.w() * (Math.abs(pose.m02()) + Math.abs(pose.m12()) + Math.abs(pose.m22()))
                        + 1;
        bounds[0] = Math.min(bounds[0], x - dx);
        bounds[1] = Math.min(bounds[1], y - dy);
        bounds[2] = Math.min(bounds[2], z - dz);
        bounds[3] = Math.max(bounds[3], x + dx);
        bounds[4] = Math.max(bounds[4], y + dy);
        bounds[5] = Math.max(bounds[5], z + dz);
    }

    public static void includeLightBounds(double[] bounds, AABB box) {
        bounds[0] = Math.min(bounds[0], box.minX - 1);
        bounds[1] = Math.min(bounds[1], box.minY - 1);
        bounds[2] = Math.min(bounds[2], box.minZ - 1);
        bounds[3] = Math.max(bounds[3], box.maxX + 1);
        bounds[4] = Math.max(bounds[4], box.maxY + 1);
        bounds[5] = Math.max(bounds[5], box.maxZ + 1);
    }

    public static @Nullable AABB sections(
            @Nullable SectionCollector collector, double[] bounds, @Nullable AABB previous) {
        if (collector == null) return null;
        int x0 = section(bounds[0]), y0 = section(bounds[1]), z0 = section(bounds[2]);
        int x1 = section(bounds[3]), y1 = section(bounds[4]), z1 = section(bounds[5]);
        if (previous != null
                && section(previous.minX) == x0
                && section(previous.minY) == y0
                && section(previous.minZ) == z0
                && section(previous.maxX) == x1
                && section(previous.maxY) == y1
                && section(previous.maxZ) == z1) return previous;
        return sections(
                collector,
                new AABB(bounds[0], bounds[1], bounds[2], bounds[3], bounds[4], bounds[5]),
                null);
    }

    public static AABB sectionBounds(double[] bounds, @Nullable AABB previous) {
        double x0 = section(bounds[0]) * 16D,
                y0 = section(bounds[1]) * 16D,
                z0 = section(bounds[2]) * 16D;
        double x1 = (section(bounds[3]) + 1) * 16D,
                y1 = (section(bounds[4]) + 1) * 16D,
                z1 = (section(bounds[5]) + 1) * 16D;
        if (previous != null
                && previous.minX == x0
                && previous.minY == y0
                && previous.minZ == z0
                && previous.maxX == x1
                && previous.maxY == y1
                && previous.maxZ == z1) return previous;
        return new AABB(x0, y0, z0, x1, y1, z1);
    }

    private static int section(double coordinate) {
        return SectionPos.blockToSectionCoord(Mth.floor(coordinate));
    }
}
