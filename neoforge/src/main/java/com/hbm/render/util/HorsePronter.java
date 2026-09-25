// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: HbmMods (The Bobcat)
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.render.util;

import com.hbm.main.ResourceManager;
import com.hbm.render.loader.HFRWavefrontObject;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;

public class HorsePronter {

    public static final int id_head = 0;
    public static final int id_lfl = 1;
    public static final int id_rfl = 2;
    public static final int id_lbl = 3;
    public static final int id_rbl = 4;
    public static final int id_tail = 5;
    public static final int id_body = 6;
    public static final int id_position = 7;

    private static final HFRWavefrontObject horse = ResourceManager.horse;

    private static final int part_body = horse.partId("Body");
    private static final int part_head = horse.partId("Head");
    private static final int part_mane = horse.partId("Mane");
    private static final int part_noseMale = horse.partId("NoseMale");
    private static final int part_noseFemale = horse.partId("NoseFemale");
    private static final int part_hornPointy = horse.partId("HornPointy");
    private static final int part_lfl = horse.partId("LeftFrontLeg");
    private static final int part_rfl = horse.partId("RightFrontLeg");
    private static final int part_lbl = horse.partId("LeftBackLeg");
    private static final int part_rbl = horse.partId("RightBackLeg");
    private static final int part_tail = horse.partId("Tail");
    private static final int part_leftWing = horse.partId("LeftWing");
    private static final int part_rightWing = horse.partId("RightWing");

    private static final double[][] pose = new double[8][3];

    private static final double[][] offsets = {
        {0, 1.125, 0.375},
        {0.125, 0.75, 0.3125},
        {-0.125, 0.75, 0.3125},
        {0.125, 0.75, -0.25},
        {-0.125, 0.75, -0.25},
        {0, 1.125, -0.4375},
        {0, 0, 0},
        {0, 0, 0}
    };

    private static boolean wings = false;
    private static boolean horn = false;
    private static boolean maleSnoot = false;

    public static void reset() {
        wings = false;
        horn = false;
        for (double[] angles : pose) {
            angles[0] = 0;
            angles[1] = 0;
            angles[2] = 0;
        }
    }

    public static void enableHorn() {
        horn = true;
    }

    public static void enableWings() {
        wings = true;
    }

    public static void setMaleSnoot() {
        maleSnoot = true;
    }

    public static void setAlicorn() {
        enableHorn();
        enableWings();
    }

    public static void poseStandardSit() {
        double r = 60;
        pose(id_body, 0, -r, 0);
        pose(id_tail, 0, 45, 90);
        pose(id_lbl, 0, -90 + r, 35);
        pose(id_rbl, 0, -90 + r, -35);
        pose(id_lfl, 0, r - 10, 5);
        pose(id_rfl, 0, r - 10, -5);
        pose(id_head, 0, r, 0);
    }

    public static void pose(int id, double yaw, double pitch, double roll) {
        pose[id][0] = yaw;
        pose[id][1] = pitch;
        pose[id][2] = roll;
    }

    public static void pront(
            PoseStack ps, SubmitNodeCollector collector, RenderType type, int light) {
        visit(
                ps,
                pose,
                horn,
                wings,
                maleSnoot,
                (poses, part) ->
                        collector.submitCustomGeometry(
                                poses,
                                type,
                                (matrices, buffer) ->
                                        horse.renderPart(matrices, buffer, light, -1, part)));
    }

    public static void visit(
            PoseStack ps,
            double[][] angles,
            boolean horn,
            boolean wings,
            boolean maleSnoot,
            PartVisitor visitor) {
        ps.pushPose();
        doTransforms(ps, angles, id_body);

        visitor.accept(ps, part_body);

        int snoot = maleSnoot ? part_noseMale : part_noseFemale;
        if (horn) {
            visitWithTransform(
                    ps, angles, visitor, id_head, part_head, part_mane, snoot, part_hornPointy);
        } else {
            visitWithTransform(ps, angles, visitor, id_head, part_head, part_mane, snoot);
        }

        visitWithTransform(ps, angles, visitor, id_lfl, part_lfl);
        visitWithTransform(ps, angles, visitor, id_rfl, part_rfl);
        visitWithTransform(ps, angles, visitor, id_lbl, part_lbl);
        visitWithTransform(ps, angles, visitor, id_rbl, part_rbl);
        visitWithTransform(ps, angles, visitor, id_tail, part_tail);

        if (wings) {
            visitor.accept(ps, part_leftWing);
            visitor.accept(ps, part_rightWing);
        }

        ps.popPose();
    }

    private static void doTransforms(PoseStack ps, double[][] angles, int id) {
        double[] rotation = angles[id];
        double[] offset = offsets[id];
        ps.translate(offset[0], offset[1], offset[2]);
        ps.mulPose(Axis.YP.rotationDegrees((float) rotation[0]));
        ps.mulPose(Axis.XP.rotationDegrees((float) rotation[1]));
        ps.mulPose(Axis.ZP.rotationDegrees((float) rotation[2]));
        ps.translate(-offset[0], -offset[1], -offset[2]);
    }

    private static void visitWithTransform(
            PoseStack ps, double[][] angles, PartVisitor visitor, int id, int... parts) {
        ps.pushPose();
        doTransforms(ps, angles, id);
        for (int part : parts) visitor.accept(ps, part);
        ps.popPose();
    }

    @FunctionalInterface
    public interface PartVisitor {
        void accept(PoseStack ps, int part);
    }
}
