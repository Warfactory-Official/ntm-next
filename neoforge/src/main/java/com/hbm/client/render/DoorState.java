/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file under the Apache License except in
 * compliance with that License. You may obtain a copy at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Apache License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License for the specific language governing permissions
 * and limitations under the License.
 */

package com.hbm.client.render;

import com.hbm.animloader.ArmaturePoseEvaluator;
import com.hbm.animloader.ArmatureScene;
import com.hbm.animloader.SampledClip;
import com.hbm.animloader.TriangleSource;
import com.hbm.blockentity.door.DoorBlockEntity;
import com.hbm.blockentity.door.DoorType;
import com.hbm.blockentity.door.DoorTypes;
import com.hbm.main.ResourceManager;
import com.hbm.render.anim.AnimationPlayback;
import com.hbm.render.anim.AnimationSequence;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.util.Facing;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

public final class DoorState implements ArmaturePoseEvaluator.GeometryVisitor {
    private static final int EXTENT_SAMPLES = 64;
    private static final byte[] MOVING = {DoorBlockEntity.OPENING, DoorBlockEntity.SHUTTING};
    private static final byte[] RESTING = {DoorBlockEntity.CLOSED, DoorBlockEntity.OPEN};
    public final DoorFrame frame = new DoorFrame();
    private final ArmaturePoseEvaluator walk = new ArmaturePoseEvaluator();
    private final Matrix4f armatureRoot = new Matrix4f();
    public DoorType kind;
    public DoorRenderDefinition look;
    public Direction facing;
    public int skinIndex;
    public float openTicks;
    public byte doorState;
    public long animStartTime;
    public @Nullable AnimationPlayback animation;
    public long animMillis;
    private @Nullable Identifier nodeTexture;

    static float[] extent(DoorType kind) {
        if (kind == DoorTypes.TRANSITION_SEAL) {
            float radius = ResourceManager.transition_seal().getRadius();
            return new float[] {-radius, radius, radius + .5F};
        }
        DoorState probe = new DoorState();
        probe.kind = kind;
        probe.look = kind.getRenderDefinition();
        float[] box = {Float.MAX_VALUE, -Float.MAX_VALUE, 0F};
        long span = kind.getOpeningTicks() * 50L;

        for (byte moving : MOVING) {
            AnimationPlayback anim = probe.look.createPlayback(moving, 0L);
            long window = Math.max(span, anim == null ? 0L : anim.clip().getDuration());
            probe.doorState = moving;
            probe.animation = anim;
            probe.animStartTime = 0L;
            for (int i = 0; i <= EXTENT_SAMPLES; i++) {
                int step = moving == DoorBlockEntity.OPENING ? i : EXTENT_SAMPLES - i;
                probe.animMillis = window * i / EXTENT_SAMPLES;
                probe.openTicks = kind.getOpeningTicks() * step / (float) EXTENT_SAMPLES;
                accumulate(probe.emit(), box);
            }
        }
        for (byte resting : RESTING) {
            probe.doorState = resting;
            probe.animation = null;
            probe.animMillis = 0L;
            probe.openTicks = resting == DoorBlockEntity.OPEN ? kind.getOpeningTicks() : 0F;
            accumulate(probe.emit(), box);
        }
        return box[0] > box[1] ? new float[] {0F, 1F, 0.5F} : box;
    }

    private static void accumulate(DoorFrame frame, float[] box) {
        Vector3f corner = new Vector3f();
        for (int i = 0; i < frame.size(); i++) {
            DoorFrame.Part part = frame.get(i);
            if (part.hidden) continue;
            float[] local = localBounds(part);
            if (local == null) continue;

            Matrix4f pose = part.drawPose();
            for (int c = 0; c < 8; c++) {
                corner.set(
                        local[(c & 1) == 0 ? 0 : 3],
                        local[(c & 2) == 0 ? 1 : 4],
                        local[(c & 4) == 0 ? 2 : 5]);
                pose.transformPosition(corner);
                box[0] = Math.min(box[0], corner.y);
                box[1] = Math.max(box[1], corner.y);
                box[2] = Math.max(box[2], Math.max(Math.abs(corner.x), Math.abs(corner.z)));
            }
        }
    }

    private static float @Nullable [] localBounds(DoorFrame.Part part) {
        HFRWavefrontObject model = part.model;
        if (model != null) return model.groups[part.group].bounds();
        TriangleSource mesh = part.mesh;
        if (mesh == null) return null;

        float[] box = {
            Float.MAX_VALUE,
            Float.MAX_VALUE,
            Float.MAX_VALUE,
            -Float.MAX_VALUE,
            -Float.MAX_VALUE,
            -Float.MAX_VALUE
        };
        for (int v = 0; v < mesh.getCornerCount(); v++) {
            for (int a = 0; a < 3; a++) {
                box[a] = Math.min(box[a], mesh.getComponent(v, a));
                box[a + 3] = Math.max(box[a + 3], mesh.getComponent(v, a));
            }
        }
        return box[0] > box[3] ? null : box;
    }

    public void sample(DoorBlockEntity door, float partialTicks) {
        kind = door.getDoorType();
        look = kind.getRenderDefinition();
        skinIndex = door.getSkinIndex();
        doorState = door.phase;
        animation = door.playback;
        animStartTime = door.transitionStart;
        facing = door.getFacing();
        animMillis = door.getAnimationTimeMillis();
        openTicks = door.getOpenTicks(partialTicks);
    }

    public DoorFrame emit() {
        frame.rewind();
        DoorMotion motion = look.getMotion();
        if (motion != null) {
            frame.cull = motion.backfaceCulled();
            frame.blended = motion.blended();
            motion.placeParts(this, frame);
            return frame;
        }
        frame.blended = true;

        var hinge = kind.getHinge();
        int hinged = look.getHingeIndex();
        int[] ids = look.getMovingPartIds();
        for (int i = 0; i < ids.length; i++) {
            DoorFrame.Part p =
                    frame.push()
                            .obj(look.getModel(), ids[i])
                            .texture(look.getMovingTexture(skinIndex, i));
            if (i != hinged) continue;
            float swing = hinge.swingAt(openTicks);
            p.pose.translation(hinge.originX(), hinge.originY(), hinge.originZ());
            if (swing != 0F) p.pose.rotateX((float) Math.toRadians(swing));
            p.pose.translate(
                    0F - hinge.originX(),
                    hinge.liftAt(openTicks) - hinge.originY(),
                    0F - hinge.originZ());
        }
        return frame;
    }

    public double bus(String name, int channel) {
        AnimationPlayback playing = animation;
        AnimationSequence bus = playing == null ? null : playing.clip().getBus(name);
        return bus == null
                ? AnimationSequence.rest(channel)
                : bus.getValue(channel, (int) (animMillis - playing.start()));
    }

    public float facingYaw() {
        return Facing.yaw(facing, 90);
    }

    public void walkArmature(
            ArmatureScene model,
            SampledClip clip,
            long time,
            long anchor,
            boolean reverse,
            Identifier texture,
            float rootX,
            float rootY,
            float rootZ) {
        nodeTexture = texture;
        armatureRoot.translation(rootX, rootY, rootZ);
        walk.visit(model, clip, time, anchor, reverse, armatureRoot, this);
    }

    @Override
    public void geometry(TriangleSource mesh, Matrix4f pose, boolean hidden) {
        DoorFrame.Part part = frame.push().node(mesh).texture(nodeTexture);
        part.pose.set(pose);
        part.hidden = hidden;
    }
}
