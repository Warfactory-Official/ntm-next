// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.animloader.AnimatedModel;
import com.hbm.animloader.Animation;
import com.hbm.animloader.AnimationWrapper;
import com.hbm.render.anim.HbmAnimations;
import com.hbm.render.loader.GroupObject;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.tileentity.BlockEntityDoorGeneric;
import com.hbm.tileentity.DoorDecl;
import com.hbm.util.Facing;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

public final class DoorState implements AnimatedModel.NodeVisitor {
    private static final int EXTENT_SAMPLES = 64;
    private static final byte[] MOVING = {
        BlockEntityDoorGeneric.STATE_OPENING, BlockEntityDoorGeneric.STATE_CLOSING
    };
    private static final byte[] RESTING = {
        BlockEntityDoorGeneric.STATE_CLOSED, BlockEntityDoorGeneric.STATE_OPEN
    };
    public final DoorFrame frame = new DoorFrame();
    private final AnimatedModel.Walk walk = new AnimatedModel.Walk();
    private final float[] origin = new float[3];
    private final float[] rotation = new float[3];
    private final float[] translation = new float[3];
    public DoorDecl decl;
    public Direction facing;
    public int skinIndex;
    public float openTicks;
    public byte doorState;
    public long animStartTime;
    public HbmAnimations.@Nullable Animation animation;

    public long animMillis;

    private @Nullable AnimationWrapper wrapper;
    private @Nullable Identifier nodeTexture;

    public static float[] extent(DoorDecl decl) {
        DoorState probe = new DoorState();
        probe.decl = decl;
        float[] box = {Float.MAX_VALUE, -Float.MAX_VALUE, 0F};
        long span = decl.timeToOpen() * 50L;

        for (byte moving : MOVING) {
            HbmAnimations.Animation anim = decl.getSEDNAAnim(moving, (byte) 0, 0L);
            long window = Math.max(span, anim == null ? 0L : anim.animation.getDuration());
            probe.doorState = moving;
            probe.animation = anim;
            probe.animStartTime = 0L;
            for (int i = 0; i <= EXTENT_SAMPLES; i++) {
                int step = moving == BlockEntityDoorGeneric.STATE_OPENING ? i : EXTENT_SAMPLES - i;
                probe.animMillis = window * i / EXTENT_SAMPLES;
                probe.openTicks = decl.timeToOpen() * step / (float) EXTENT_SAMPLES;
                accumulate(probe.emit(), box);
            }
        }

        for (byte resting : RESTING) {
            probe.doorState = resting;
            probe.animation = null;
            probe.animMillis = 0L;
            probe.openTicks = resting == BlockEntityDoorGeneric.STATE_OPEN ? decl.timeToOpen() : 0F;
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
        AnimatedModel.Mesh mesh = part.mesh;
        if (mesh == null) return null;

        float[] box = {
            Float.MAX_VALUE,
            Float.MAX_VALUE,
            Float.MAX_VALUE,
            -Float.MAX_VALUE,
            -Float.MAX_VALUE,
            -Float.MAX_VALUE
        };
        for (int v = 0; v < mesh.vertexData.length; v += GroupObject.STRIDE) {
            for (int a = 0; a < 3; a++) {
                box[a] = Math.min(box[a], mesh.vertexData[v + a]);
                box[a + 3] = Math.max(box[a + 3], mesh.vertexData[v + a]);
            }
        }
        return box[0] > box[3] ? null : box;
    }

    public void sample(BlockEntityDoorGeneric door, float partialTicks) {
        decl = door.getDoorType();
        skinIndex = door.getSkinIndex();
        doorState = door.state;
        animation = door.currentAnimation;
        animStartTime = door.animStartTime;
        facing = door.facing();

        animMillis = door.getLevel().getGameTime() * 50L + (long) (partialTicks * 50F);

        openTicks =
                switch (door.state) {
                    case BlockEntityDoorGeneric.STATE_OPENING ->
                            Math.min(decl.timeToOpen(), door.openTicks + partialTicks);
                    case BlockEntityDoorGeneric.STATE_CLOSING ->
                            Math.max(0F, door.openTicks - partialTicks);
                    default -> door.openTicks;
                };
    }

    public DoorFrame emit() {
        frame.rewind();
        DoorRenderer sedna = decl.getSEDNARenderer();
        if (sedna != null) {
            frame.cull = sedna.culls();
            frame.blended = sedna.blends();
            sedna.emit(this, frame);
            return frame;
        }
        frame.blended = true;

        String[] dynamic = decl.getDynamicParts();
        int[] dynamicIds = decl.getDynamicPartIds();
        for (int i = 0; i < dynamic.length; i++) {
            String part = dynamic[i];
            decl.getOrigin(part, origin);
            decl.getRotation(part, openTicks, rotation);
            decl.getTranslation(part, openTicks, false, translation);

            DoorFrame.Part p =
                    frame.push()
                            .obj(decl.getModel(), dynamicIds[i])
                            .texture(decl.getTextureForPart(skinIndex, part));
            p.pose.translation(origin[0], origin[1], origin[2]);
            if (rotation[0] != 0F) p.pose.rotateX((float) Math.toRadians(rotation[0]));
            if (rotation[1] != 0F) p.pose.rotateY((float) Math.toRadians(rotation[1]));
            if (rotation[2] != 0F) p.pose.rotateZ((float) Math.toRadians(rotation[2]));
            p.pose.translate(
                    -origin[0] + translation[0],
                    -origin[1] + translation[1],
                    -origin[2] + translation[2]);
        }
        return frame;
    }

    public double[] bus(String name) {
        return DoorRenderer.getRelevantTransformation(name, animation, animMillis);
    }

    public float facingYaw() {
        return Facing.yaw(facing, 90);
    }

    public AnimationWrapper wrapper(Animation anim) {
        if (wrapper == null)
            wrapper =
                    new AnimationWrapper(animStartTime, anim)
                            .onEnd(AnimationWrapper.EndResult.STAY);
        return wrapper;
    }

    public void walkArmature(
            AnimatedModel model,
            long sysTime,
            AnimationWrapper anim,
            Identifier texture,
            float rootX,
            float rootY,
            float rootZ) {
        nodeTexture = texture;
        walk.root().translation(rootX, rootY, rootZ);
        model.walk(sysTime, anim, walk, this);
    }

    @Override
    public void node(AnimatedModel.Mesh mesh, Matrix4fc pose, boolean hidden) {
        DoorFrame.Part part = frame.push().node(mesh).texture(nodeTexture);
        part.pose.set(pose);
        part.hidden = hidden;
    }
}
