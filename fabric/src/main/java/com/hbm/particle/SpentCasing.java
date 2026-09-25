// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: HbmMods (The Bobcat)
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.particle;

import com.hbm.main.ResourceManager;
import java.util.HashMap;

public class SpentCasing implements Cloneable {

    public static final int COLOR_CASE_BRASS = 0xEBC35E;
    public static final int COLOR_CASE_EQUESTRIAN = 0x957BA0;
    public static final int COLOR_CASE_12GA = 0x757575;
    public static final int COLOR_CASE_4GA = 0xD8D8D8;
    public static final int COLOR_CASE_44 = 0x3E3E3E;
    public static final int COLOR_CASE_16INCH = 0xD89128;
    public static final int COLOR_CASE_16INCH_PHOS = 0xC8C8C8;
    public static final int COLOR_CASE_16INCH_NUKE = 0x495443;
    public static final int COLOR_CASE_40MM = 0x515151;

    public static final HashMap<String, SpentCasing> casingMap = new HashMap<String, SpentCasing>();

    public static final String PLINK_SHELL = "weapon.casing.shell";
    public static final String PLINK_SMALL = "weapon.casing.small";
    public static final String PLINK_MEDIUM = "weapon.casing.medium";
    public static final String PLINK_LARGE = "weapon.casing.large";
    private String registryName = "CHANGEME";
    private float scaleX = 1F;
    private float scaleY = 1F;
    private float scaleZ = 1F;
    private int[] colors;
    private final CasingType type;
    private String bounceSound;
    private float bounceYaw = 1F;
    private float bouncePitch = 1F;
    private int maxAge = 240;

    public SpentCasing(CasingType type) {
        this.type = type;

        if (type == CasingType.SHOTGUN) {
            this.setSound(PLINK_SHELL);
        } else {
            this.setSound(PLINK_SMALL);
        }
    }

    public static SpentCasing fromName(String name) {
        return casingMap.get(name);
    }

    public SpentCasing register(String name) {
        this.registryName = name;
        casingMap.put(name, this);
        return this;
    }

    public SpentCasing setScale(float scale) {
        return setScale(scale, scale, scale);
    }

    public SpentCasing setScale(float x, float y, float z) {
        this.scaleX = x;
        this.scaleY = y;
        this.scaleZ = z;
        if (x * y * z >= 3 && this.type != CasingType.SHOTGUN) this.setSound(PLINK_MEDIUM);
        if (x * y * z >= 100 && this.type != CasingType.SHOTGUN) this.setSound(PLINK_LARGE);
        return this;
    }

    public SpentCasing setColor(int... color) {
        this.colors = color;
        return this;
    }

    @Deprecated
    public SpentCasing setupSmoke(float chance, double lift, int duration, int nodeLife) {
        return this;
    }

    public SpentCasing setBounceMotion(float yaw, float pitch) {
        this.bounceYaw = yaw;
        this.bouncePitch = pitch;
        return this;
    }

    public String getName() {
        return this.registryName;
    }

    public float getScaleX() {
        return this.scaleX;
    }

    public float getScaleY() {
        return this.scaleY;
    }

    public float getScaleZ() {
        return this.scaleZ;
    }

    public int[] getColors() {
        return this.colors;
    }

    public CasingType getType() {
        return this.type;
    }

    public String getSound() {
        return this.bounceSound;
    }

    public SpentCasing setSound(String bounce) {
        this.bounceSound = bounce;
        return this;
    }

    public float getBounceYaw() {
        return this.bounceYaw;
    }

    public float getBouncePitch() {
        return this.bouncePitch;
    }

    public int getMaxAge() {
        return this.maxAge;
    }

    public SpentCasing setMaxAge(int age) {
        this.maxAge = age;
        return this;
    }

    @Override
    public SpentCasing clone() {
        try {
            return (SpentCasing) super.clone();
        } catch (CloneNotSupportedException e) {
            return new SpentCasing(this.type);
        }
    }

    public enum CasingType {
        STRAIGHT("Straight"),
        BOTTLENECK("Bottleneck"),
        SHOTGUN("Shotgun", "ShotgunCase");

        public final String[] partNames;
        private int[] partIds;

        CasingType(String... names) {
            this.partNames = names;
        }

        public int[] partIds() {
            int[] ids = partIds;
            if (ids == null) ids = partIds = ResourceManager.casings.partIds(partNames);
            return ids;
        }
    }
}
