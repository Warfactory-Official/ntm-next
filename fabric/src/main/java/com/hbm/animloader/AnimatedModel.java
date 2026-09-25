// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.animloader;

import com.hbm.util.BobMathUtil;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

public class AnimatedModel {
    private static final AnimatedModel[] NO_CHILDREN = new AnimatedModel[0];
    public final Matrix4f transform = new Matrix4f();
    public String name = "";
    public boolean hasTransform;

    public String geoName = "";
    public AnimatedModel parent;
    public AnimatedModel[] children = NO_CHILDREN;
    private @Nullable Mesh mesh;

    private static float fract(float number) {
        return (float) (number - Math.floor(number));
    }

    public void walk(long sysTime, AnimationWrapper anim, Walk scratch, NodeVisitor out) {
        if (anim == AnimationWrapper.EMPTY) {
            walkStatic(scratch, 0, scratch.root, out);
            return;
        }

        int numKeyFrames = anim.anim.numKeyFrames;
        int diff = (int) ((sysTime - anim.startTime) * anim.speedScale);
        if (diff > anim.anim.length) {
            int diff2 = diff % anim.anim.length;
            switch (anim.endResult.type()) {
                case END -> {
                    walkStatic(scratch, 0, scratch.root, out);
                    return;
                }
                case REPEAT -> anim.startTime = sysTime - diff2;
                case REPEAT_REVERSE -> {
                    anim.startTime = sysTime - diff2;
                    anim.reverse = !anim.reverse;
                }
                case START_NEW -> {
                    anim.cloneStats(anim.endResult.next());
                    anim.startTime = sysTime - diff2;
                }
                case STAY -> anim.startTime = sysTime - anim.anim.length;
            }
        }

        diff = (int) (sysTime - anim.startTime);
        if (anim.reverse) diff = anim.anim.length - diff;
        diff = (int) (diff * anim.speedScale);

        float remappedTime =
                Math.clamp(
                        (float)
                                BobMathUtil.interp(
                                        0F, numKeyFrames - 1F, diff / (float) anim.anim.length),
                        0F,
                        numKeyFrames - 1F);
        int first = (int) remappedTime;
        int next = first < numKeyFrames - 1 ? first + 1 : first;

        walkAnimated(scratch, 0, scratch.root, anim, fract(remappedTime), first, next, out);
        anim.prevFrame = first;
    }

    private void walkAnimated(
            Walk scratch,
            int depth,
            Matrix4fc parentPose,
            AnimationWrapper anim,
            float inter,
            int firstIndex,
            int nextIndex,
            NodeVisitor out) {
        Matrix4f pose = scratch.at(depth).set(parentPose);
        boolean hidden = false;
        if (hasTransform) {
            Transform[] transforms = anim.anim.objectTransforms.get(name);
            if (transforms != null) {
                hidden = transforms[firstIndex].hidden;
                pose.mul(
                        transforms[firstIndex].interpolate(
                                scratch.local, transforms[nextIndex], inter, scratch.rotation));
            } else {
                pose.mul(transform);
            }
        }
        if (mesh != null) out.node(mesh, pose, hidden);
        for (AnimatedModel child : children) {
            child.walkAnimated(scratch, depth + 1, pose, anim, inter, firstIndex, nextIndex, out);
        }
    }

    private void walkStatic(Walk scratch, int depth, Matrix4fc parentPose, NodeVisitor out) {
        Matrix4f pose = scratch.at(depth).set(parentPose);
        if (hasTransform) pose.mul(transform);
        if (mesh != null) out.node(mesh, pose, false);
        for (AnimatedModel child : children) {
            child.walkStatic(scratch, depth + 1, pose, out);
        }
    }

    void setChildren(AnimatedModel[] children) {
        this.children = children.length == 0 ? NO_CHILDREN : children;
    }

    void setMesh(@Nullable Mesh mesh) {
        this.mesh = mesh;
    }

    public @Nullable Mesh mesh() {
        return mesh;
    }

    public interface NodeVisitor {
        void node(Mesh mesh, Matrix4fc pose, boolean hidden);
    }

    public static final class Walk {
        final Matrix4f local = new Matrix4f();
        final Quaternionf rotation = new Quaternionf();
        final Matrix4f root = new Matrix4f();
        private Matrix4f[] stack = {new Matrix4f()};

        public Matrix4f root() {
            return root;
        }

        Matrix4f at(int depth) {
            if (depth >= stack.length) {
                Matrix4f[] grown = new Matrix4f[Math.max(depth + 1, stack.length * 2)];
                System.arraycopy(stack, 0, grown, 0, stack.length);
                for (int i = stack.length; i < grown.length; i++) grown[i] = new Matrix4f();
                stack = grown;
            }
            return stack[depth];
        }
    }

    public static final class Mesh {
        public final String name;
        public final float[] vertexData;

        private final Matrix3f normalMatrix = new Matrix3f();
        private final Matrix4f normalScratch = new Matrix4f();
        private final Vector3f normal = new Vector3f();
        private final Vector3f position = new Vector3f();

        public Mesh(String name, float[] vertexData) {
            this.name = name;
            this.vertexData = vertexData;
        }

        public void render(
                Matrix4fc pose, VertexConsumer consumer, int packedLight, int packedOverlay) {
            normalScratch.set(pose).normal(normalMatrix);
            for (int tri = 0; tri < vertexData.length; tri += 24) {
                emitVertex(pose, normalMatrix, normal, consumer, packedLight, packedOverlay, tri);
                emitVertex(
                        pose, normalMatrix, normal, consumer, packedLight, packedOverlay, tri + 8);
                emitVertex(
                        pose, normalMatrix, normal, consumer, packedLight, packedOverlay, tri + 16);
                emitVertex(
                        pose, normalMatrix, normal, consumer, packedLight, packedOverlay, tri + 16);
            }
        }

        private void emitVertex(
                Matrix4fc pose,
                Matrix3f normalMatrix,
                Vector3f normal,
                VertexConsumer consumer,
                int packedLight,
                int packedOverlay,
                int i) {
            normalMatrix
                    .transform(vertexData[i + 5], vertexData[i + 6], vertexData[i + 7], normal)
                    .normalize();
            Vector3f p =
                    pose.transformPosition(
                            vertexData[i], vertexData[i + 1], vertexData[i + 2], position);
            consumer.addVertex(
                    p.x,
                    p.y,
                    p.z,
                    0xFFFFFFFF,
                    vertexData[i + 3],
                    vertexData[i + 4],
                    packedOverlay,
                    packedLight,
                    normal.x,
                    normal.y,
                    normal.z);
        }
    }
}
