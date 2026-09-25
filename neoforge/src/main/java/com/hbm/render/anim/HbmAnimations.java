// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: Contributors to Hbm's Nuclear Tech Mod
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.render.anim;

import com.hbm.util.GameTime;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class HbmAnimations {

    public static final Animation[][] hotbar = new Animation[9][8];

    public static void expireFinished() {
        long now = GameTime.millis();
        for (int slot = 0; slot < hotbar.length; slot++) {
            for (int rail = 0; rail < hotbar[slot].length; rail++) {
                Animation animation = hotbar[slot][rail];
                if (animation == null || animation.holdLastFrame) continue;
                if (now - animation.startMillis > animation.animation.getDuration())
                    hotbar[slot][rail] = null;
            }
        }
    }

    public static Animation getRelevantAnim() {
        return getRelevantAnim(0);
    }

    public static Animation getRelevantAnim(int index) {

        Player player = Minecraft.getInstance().player;
        int slot = player.getInventory().getSelectedSlot();
        ItemStack stack = player.getMainHandItem();

        if (stack.isEmpty()) return null;

        if (slot < 0 || slot > 8) {
            slot = Math.abs(slot) % 9;
        }

        if (hotbar[slot][index] == null) return null;

        if (hotbar[slot][index].key.equals(stack.getItem().getDescriptionId())) {
            return hotbar[slot][index];
        }

        return null;
    }

    public static double[] getRelevantTransformation(String bus) {
        return getRelevantTransformation(bus, 0);
    }

    public static double[] getRelevantTransformation(String bus, int index) {

        Animation anim = HbmAnimations.getRelevantAnim(index);

        if (anim != null) {

            BusAnimation buses = anim.animation;
            int millis = (int) (GameTime.now() - anim.startMillis);

            BusAnimationSequence seq = buses.getBus(bus);

            if (seq != null) {
                double[] trans = seq.getTransformation(millis);

                if (trans != null) return trans;
            }
        }

        return identity();
    }

    public static double[] identity() {
        return new double[] {
            0, 0, 0,
            0, 0, 0,
            1, 1, 1,
            0, 0, 0,
            0, 1, 2,
        };
    }

    public static void applyRelevantTransformation(PoseStack pose, String bus) {
        applyRelevantTransformation(pose, bus, 0);
    }

    public static void applyRelevantTransformation(PoseStack pose, String bus, int index) {
        double[] transform = getRelevantTransformation(bus, index);
        int[] rot = new int[] {(int) transform[12], (int) transform[13], (int) transform[14]};

        pose.translate(transform[0], transform[1], transform[2]);
        pose.mulPose(axis(rot[0]).rotationDegrees((float) transform[3 + rot[0]]));
        pose.mulPose(axis(rot[1]).rotationDegrees((float) transform[3 + rot[1]]));
        pose.mulPose(axis(rot[2]).rotationDegrees((float) transform[3 + rot[2]]));
        pose.translate(-transform[9], -transform[10], -transform[11]);
        pose.scale((float) transform[6], (float) transform[7], (float) transform[8]);
    }

    private static Axis axis(int index) {
        return index == 0 ? Axis.XP : index == 1 ? Axis.YP : Axis.ZP;
    }

    public static class Animation {

        public String key;

        public long startMillis;

        public BusAnimation animation;

        public boolean holdLastFrame = false;

        public Animation(String key, long startMillis, BusAnimation animation) {
            this.key = key;
            this.startMillis = startMillis;
            this.animation = animation;
        }

        public Animation(
                String key, long startMillis, BusAnimation animation, boolean holdLastFrame) {
            this(key, startMillis, animation);
            this.holdLastFrame = holdLastFrame;
        }
    }
}
