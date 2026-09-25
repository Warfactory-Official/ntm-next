// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: Contributors to Hbm's Nuclear Tech Mod
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity;

import com.hbm.client.render.*;
import com.hbm.main.ResourceManager;
import com.hbm.registration.RegistryHandle;
import com.hbm.render.anim.BusAnimation;
import com.hbm.render.anim.BusAnimationKeyframe.IType;
import com.hbm.render.anim.BusAnimationSequence;
import com.hbm.render.anim.HbmAnimations;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.sound.ModSounds;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

public abstract class DoorDecl {

    public static final DoorDecl SILO_HATCH =
            new DoorDecl() {

                @Override
                public RegistryHandle<SoundEvent> getOpenSoundEnd() {
                    return ModSounds.DOOR_WGH_BIG_STOP;
                }

                @Override
                public RegistryHandle<SoundEvent> getOpenSoundLoop() {
                    return ModSounds.DOOR_WGH_BIG_START;
                }

                @Override
                public RegistryHandle<SoundEvent> getOpenSoundStart() {
                    return null;
                }

                @Override
                public RegistryHandle<SoundEvent> getCloseSoundStart() {
                    return null;
                }

                @Override
                public RegistryHandle<SoundEvent> getCloseSoundEnd() {
                    return ModSounds.DOOR_WGH_BIG_STOP;
                }

                @Override
                public float getSoundVolume() {
                    return 2;
                }

                @Override
                public boolean remoteControllable() {
                    return true;
                }

                @Override
                public void getTranslation(
                        String partName, float openTicks, boolean child, float[] trans) {
                    if ("Hatch".equals(partName)) {
                        set(trans, 0, 0.25F * smoothstep(getNormTime(openTicks, 0, 10)), 0);
                    } else {
                        set(trans, 0, 0, 0);
                    }
                }

                @Override
                public void getOrigin(String partName, float[] orig) {
                    if ("Hatch".equals(partName)) {
                        set(orig, 0F, 0.875F, -1.875F);
                        return;
                    }
                    set(orig, 0, 0, 0);
                }

                @Override
                public void getRotation(String partName, float openTicks, float[] rot) {
                    if ("Hatch".equals(partName)) {
                        set(rot, smoothstep(getNormTime(openTicks, 20, 100)) * -240, 0, 0);
                        return;
                    }
                    super.getRotation(partName, openTicks, rot);
                }

                @Override
                public int timeToOpen() {
                    return 60;
                }

                @Override
                public int[][] getDoorOpenRanges() {
                    return new int[][] {
                        {1, 0, 1, -3, 3, 0}, {0, 0, 1, -3, 3, 0}, {-1, 0, 1, -3, 3, 0}
                    };
                }

                @Override
                public float getDoorRangeOpenTime(int ticks, int idx) {
                    return getNormTime(ticks, 20, 20);
                }

                @Override
                public int getBlockOffset() {
                    return 2;
                }

                @Override
                public int[] getDimensions() {
                    return new int[] {0, 0, 2, 2, 2, 2};
                }

                @Override
                public Identifier getTextureForPart(int skinIndex, String partName) {
                    return ResourceManager.silo_hatch_tex;
                }

                @Override
                public HFRWavefrontObject getModel() {
                    return ResourceManager.silo_hatch;
                }

                @Override
                public String[] getStaticParts() {
                    return FRAME;
                }

                @Override
                public String[] getDynamicParts() {
                    return HATCH;
                }
            };

    public static final DoorDecl SILO_HATCH_LARGE =
            new DoorDecl() {

                @Override
                public RegistryHandle<SoundEvent> getOpenSoundEnd() {
                    return ModSounds.DOOR_WGH_BIG_STOP;
                }

                @Override
                public RegistryHandle<SoundEvent> getOpenSoundLoop() {
                    return ModSounds.DOOR_WGH_BIG_START;
                }

                @Override
                public RegistryHandle<SoundEvent> getOpenSoundStart() {
                    return null;
                }

                @Override
                public RegistryHandle<SoundEvent> getCloseSoundStart() {
                    return null;
                }

                @Override
                public RegistryHandle<SoundEvent> getCloseSoundEnd() {
                    return ModSounds.DOOR_WGH_BIG_STOP;
                }

                @Override
                public float getSoundVolume() {
                    return 2;
                }

                @Override
                public boolean remoteControllable() {
                    return true;
                }

                @Override
                public void getTranslation(
                        String partName, float openTicks, boolean child, float[] trans) {
                    if ("Hatch".equals(partName)) {
                        set(trans, 0, 0.25F * smoothstep(getNormTime(openTicks, 0, 10)), 0);
                    } else {
                        set(trans, 0, 0, 0);
                    }
                }

                @Override
                public void getOrigin(String partName, float[] orig) {
                    if ("Hatch".equals(partName)) {
                        set(orig, 0F, 0.875F, -2.875F);
                        return;
                    }
                    set(orig, 0, 0, 0);
                }

                @Override
                public void getRotation(String partName, float openTicks, float[] rot) {
                    if ("Hatch".equals(partName)) {
                        set(rot, smoothstep(getNormTime(openTicks, 20, 100)) * -240, 0, 0);
                        return;
                    }
                    super.getRotation(partName, openTicks, rot);
                }

                @Override
                public int timeToOpen() {
                    return 60;
                }

                @Override
                public int[][] getDoorOpenRanges() {
                    return new int[][] {
                        {2, 0, 1, -3, 3, 0},
                        {1, 0, 2, -5, 3, 0},
                        {0, 0, 2, -5, 3, 0},
                        {-1, 0, 2, -5, 3, 0},
                        {-2, 0, 1, -3, 3, 0}
                    };
                }

                @Override
                public float getDoorRangeOpenTime(int ticks, int idx) {
                    return getNormTime(ticks, 20, 20);
                }

                @Override
                public int getBlockOffset() {
                    return 3;
                }

                @Override
                public int[] getDimensions() {
                    return new int[] {0, 0, 3, 3, 3, 3};
                }

                @Override
                public Identifier getTextureForPart(int skinIndex, String partName) {
                    return ResourceManager.silo_hatch_large_tex;
                }

                @Override
                public HFRWavefrontObject getModel() {
                    return ResourceManager.silo_hatch_large;
                }

                @Override
                public String[] getStaticParts() {
                    return FRAME;
                }

                @Override
                public String[] getDynamicParts() {
                    return HATCH;
                }
            };

    public static final DoorDecl FIRE_DOOR =
            new DoorDecl() {

                private Identifier[] skins;

                @Override
                public RegistryHandle<SoundEvent> getOpenSoundEnd() {
                    return ModSounds.DOOR_WGH_STOP_STREAM;
                }

                @Override
                public RegistryHandle<SoundEvent> getOpenSoundLoop() {
                    return ModSounds.DOOR_WGH_START_STREAM;
                }

                @Override
                public RegistryHandle<SoundEvent> getSoundLoop2() {
                    return ModSounds.DOOR_ALARM6;
                }

                @Override
                public float getSoundVolume() {
                    return 2;
                }

                @Override
                public DoorRenderer getSEDNARenderer() {
                    return RenderFireDoor.INSTANCE;
                }

                @Override
                public BusAnimation getBusAnimation(byte state, byte skinIndex) {
                    if (state == BlockEntityDoorGeneric.STATE_OPENING)
                        return new BusAnimation()
                                .addBus(
                                        "DOOR",
                                        new BusAnimationSequence()
                                                .setPos(0, 0, 0)
                                                .addPos(0, 1, 0, timeToOpen() * 50));
                    if (state == BlockEntityDoorGeneric.STATE_CLOSING)
                        return new BusAnimation()
                                .addBus(
                                        "DOOR",
                                        new BusAnimationSequence()
                                                .setPos(0, 1, 0)
                                                .addPos(0, 0, 0, timeToOpen() * 50));
                    return null;
                }

                @Override
                public Identifier[] getSEDNASkins() {
                    if (skins == null)
                        skins =
                                new Identifier[] {
                                    ResourceManager.pheo_fire_door_tex,
                                    ResourceManager.pheo_fire_door_black_tex,
                                    ResourceManager.pheo_fire_door_orange_tex,
                                    ResourceManager.pheo_fire_door_yellow_tex,
                                    ResourceManager.pheo_fire_door_trefoil_tex
                                };
                    return skins;
                }

                @Override
                public int getSkinCount() {
                    return 5;
                }

                @Override
                public int timeToOpen() {
                    return 160;
                }

                @Override
                public int[][] getDoorOpenRanges() {
                    return new int[][] {{-1, 0, 0, 3, 4, 1}};
                }

                @Override
                public int[] getDimensions() {
                    return new int[] {2, 0, 0, 0, 2, 1};
                }

                @Override
                public AABB getBlockBound(int x, int y, int z, boolean open, boolean forCollision) {
                    if (!open) return new AABB(0, 0, 0, 1, 1, 1);
                    if (z == 1) return new AABB(0.5, 0, 0, 1, 1, 1);
                    else if (z == -2) return new AABB(0, 0, 0, 0.5, 1, 1);
                    else if (y > 1) return new AABB(0, 0.75, 0, 1, 1, 1);
                    else if (y == 0) return new AABB(0, 0, 0, 1, forCollision ? 0 : 0.1, 1);
                    else return super.getBlockBound(x, y, z, open, forCollision);
                }

                @Override
                public HFRWavefrontObject getModel() {
                    return ResourceManager.pheo_fire_door;
                }

                @Override
                public Identifier getTextureForPart(int skinIndex, String partName) {
                    return getSkinFromIndex(skinIndex);
                }

                @Override
                public String[] getStaticParts() {
                    return FRAME;
                }

                @Override
                public String[] getDynamicParts() {
                    return DOOR;
                }

                @Override
                public int getStaticYaw() {
                    return 90;
                }

                @Override
                public float[] getStaticOffset() {
                    return new float[] {-0.5F, 0F, 0F};
                }
            };

    public static final DoorDecl SECURE_ACCESS_DOOR =
            new DoorDecl() {

                private Identifier[] skins;

                @Override
                public RegistryHandle<SoundEvent> getCloseSoundLoop() {
                    return ModSounds.DOOR_GARAGE_MOVE;
                }

                @Override
                public RegistryHandle<SoundEvent> getCloseSoundEnd() {
                    return ModSounds.DOOR_GARAGE_STOP;
                }

                @Override
                public RegistryHandle<SoundEvent> getOpenSoundEnd() {
                    return ModSounds.DOOR_GARAGE_STOP;
                }

                @Override
                public RegistryHandle<SoundEvent> getOpenSoundLoop() {
                    return ModSounds.DOOR_GARAGE_MOVE;
                }

                @Override
                public float getSoundVolume() {
                    return 2;
                }

                @Override
                public DoorRenderer getSEDNARenderer() {
                    return RenderSecureDoor.INSTANCE;
                }

                @Override
                public BusAnimation getBusAnimation(byte state, byte skinIndex) {
                    if (state == BlockEntityDoorGeneric.STATE_OPENING)
                        return new BusAnimation()
                                .addBus(
                                        "DOOR",
                                        new BusAnimationSequence()
                                                .setPos(0, 0, 0)
                                                .addPos(0, 1, 0, timeToOpen() * 50));
                    if (state == BlockEntityDoorGeneric.STATE_CLOSING)
                        return new BusAnimation()
                                .addBus(
                                        "DOOR",
                                        new BusAnimationSequence()
                                                .setPos(0, 1, 0)
                                                .addPos(0, 0, 0, timeToOpen() * 50));
                    return null;
                }

                @Override
                public int timeToOpen() {
                    return 120;
                }

                @Override
                public int[][] getDoorOpenRanges() {
                    return new int[][] {{-2, 1, 0, 4, 5, 1}};
                }

                @Override
                public int[] getDimensions() {
                    return new int[] {4, 0, 0, 0, 2, 2};
                }

                @Override
                public AABB getBlockBound(int x, int y, int z, boolean open, boolean forCollision) {
                    if (!open) {
                        if (y > 0) return new AABB(0, 0, 0.375, 1, 1, 0.625);
                        return new AABB(0, 0, 0, 1, 1, 1);
                    }
                    if (y == 1) return new AABB(0, 0, 0, 1, forCollision ? 0 : 0.0625, 1);
                    else if (y == 4) return new AABB(0, 0.5, 0.15, 1, 1, 0.85);
                    else if (y == 0) return new AABB(0, 0, 0, 1, 1, 1);
                    else return super.getBlockBound(x, y, z, open, forCollision);
                }

                @Override
                public Identifier[] getSEDNASkins() {
                    if (skins == null)
                        skins =
                                new Identifier[] {
                                    ResourceManager.pheo_secure_door_tex,
                                    ResourceManager.pheo_secure_door_grey_tex,
                                    ResourceManager.pheo_secure_door_black_tex,
                                    ResourceManager.pheo_secure_door_yellow_tex
                                };
                    return skins;
                }

                @Override
                public int getSkinCount() {
                    return 4;
                }

                @Override
                public HFRWavefrontObject getModel() {
                    return ResourceManager.pheo_secure_door;
                }

                @Override
                public Identifier getTextureForPart(int skinIndex, String partName) {
                    return getSkinFromIndex(skinIndex);
                }

                @Override
                public String[] getStaticParts() {
                    return FRAME;
                }

                @Override
                public String[] getDynamicParts() {
                    return DOOR;
                }

                @Override
                public float[] getStaticOffset() {
                    return new float[] {0F, 1F, 0F};
                }
            };

    public static final DoorDecl QE_SLIDING =
            new DoorDecl() {

                @Override
                public RegistryHandle<SoundEvent> getOpenSoundEnd() {
                    return ModSounds.DOOR_QE_SLIDING_OPENED;
                }

                @Override
                public RegistryHandle<SoundEvent> getCloseSoundEnd() {
                    return ModSounds.DOOR_QE_SLIDING_SHUT;
                }

                @Override
                public RegistryHandle<SoundEvent> getOpenSoundLoop() {
                    return ModSounds.DOOR_QE_SLIDING_OPENING;
                }

                @Override
                public float getSoundVolume() {
                    return 2;
                }

                @Override
                public int timeToOpen() {
                    return 10;
                }

                @Override
                public DoorRenderer getSEDNARenderer() {
                    return RenderSlidingDoor.INSTANCE;
                }

                @Override
                public BusAnimation getBusAnimation(byte state, byte skinIndex) {
                    if (state == BlockEntityDoorGeneric.STATE_OPENING)
                        return new BusAnimation()
                                .addBus(
                                        "DOOR",
                                        new BusAnimationSequence()
                                                .setPos(0, 0, 0)
                                                .addPos(0, 1, 0, timeToOpen() * 50));
                    if (state == BlockEntityDoorGeneric.STATE_CLOSING)
                        return new BusAnimation()
                                .addBus(
                                        "DOOR",
                                        new BusAnimationSequence()
                                                .setPos(0, 1, 0)
                                                .addPos(0, 0, 0, timeToOpen() * 50));
                    return null;
                }

                @Override
                public AABB getBlockBound(int x, int y, int z, boolean open, boolean forCollision) {
                    if (forCollision && open) {
                        if (z == 0) return new AABB(1 - 0.125, 0, 1 - 0.1875, 1, 1, 1);
                        else return new AABB(0, 0, 1 - 0.1875, 0.125, 1, 1);
                    } else {
                        return new AABB(0, 0, 1 - 0.1875, 1, 1, 1);
                    }
                }

                @Override
                public int[][] getDoorOpenRanges() {
                    return new int[][] {{0, 0, 0, 2, 2, 2}};
                }

                @Override
                public int[] getDimensions() {
                    return new int[] {1, 0, 0, 0, 1, 0};
                }

                @Override
                public HFRWavefrontObject getModel() {
                    return ResourceManager.pheo_sliding_door;
                }

                @Override
                public Identifier getTextureForPart(int skinIndex, String partName) {
                    return ResourceManager.pheo_sliding_door_tex;
                }

                @Override
                public String[] getStaticParts() {
                    return FRAME;
                }

                @Override
                public String[] getDynamicParts() {
                    return LEFT_RIGHT;
                }

                @Override
                public float[] getStaticOffset() {
                    return new float[] {0.53125F, 0.001F, 0.5F};
                }
            };

    public static final DoorDecl CARGO_DOOR =
            new DoorDecl() {

                private Identifier[] skins;

                @Override
                public RegistryHandle<SoundEvent> getCloseSoundLoop() {
                    return ModSounds.DOOR_GARAGE_MOVE;
                }

                @Override
                public RegistryHandle<SoundEvent> getCloseSoundEnd() {
                    return ModSounds.DOOR_GARAGE_STOP;
                }

                @Override
                public RegistryHandle<SoundEvent> getOpenSoundEnd() {
                    return ModSounds.DOOR_GARAGE_STOP;
                }

                @Override
                public RegistryHandle<SoundEvent> getOpenSoundLoop() {
                    return ModSounds.DOOR_GARAGE_MOVE;
                }

                @Override
                public float getSoundVolume() {
                    return 2;
                }

                @Override
                public DoorRenderer getSEDNARenderer() {
                    return RenderCargoDoor.INSTANCE;
                }

                @Override
                public BusAnimation getBusAnimation(byte state, byte skinIndex) {
                    int half = timeToOpen() * 25;
                    if (state == BlockEntityDoorGeneric.STATE_OPENING)
                        return new BusAnimation()
                                .addBus(
                                        "BOT",
                                        new BusAnimationSequence()
                                                .setPos(0, 0, 0)
                                                .addPos(0, 1, 0, half * 2))
                                .addBus(
                                        "TOP",
                                        new BusAnimationSequence()
                                                .setPos(0, 0, 0)
                                                .addPos(0, 0, 0, half)
                                                .addPos(0, 1, 0, half));
                    if (state == BlockEntityDoorGeneric.STATE_CLOSING)
                        return new BusAnimation()
                                .addBus(
                                        "BOT",
                                        new BusAnimationSequence()
                                                .setPos(0, 1, 0)
                                                .addPos(0, 0, 0, half * 2))
                                .addBus(
                                        "TOP",
                                        new BusAnimationSequence()
                                                .setPos(0, 1, 0)
                                                .addPos(0, 1, 0, half)
                                                .addPos(0, 0, 0, half));
                    return null;
                }

                @Override
                public int timeToOpen() {
                    return 60;
                }

                @Override
                public int[][] getDoorOpenRanges() {
                    return new int[][] {{-1, -1, 0, 3, 3, 1}};
                }

                @Override
                public int[] getDimensions() {
                    return new int[] {2, 0, 0, 0, 1, 1};
                }

                @Override
                public AABB getBlockBound(int x, int y, int z, boolean open, boolean forCollision) {
                    if (!open) return new AABB(0, 0, 0.375, 1, 1, 0.625);
                    if (y > 1) return new AABB(0, 0.25, 0.375, 1, 1, 0.625);
                    else if (y == 0)
                        return new AABB(0, 0, 0.375, 1, forCollision ? 0 : 0.125, 0.625);
                    return super.getBlockBound(x, y, z, open, forCollision);
                }

                @Override
                public Identifier[] getSEDNASkins() {
                    if (skins == null)
                        skins = new Identifier[] {ResourceManager.pheo_cargo_door_tex};
                    return skins;
                }

                @Override
                public int getSkinCount() {
                    return 1;
                }

                @Override
                public HFRWavefrontObject getModel() {
                    return ResourceManager.pheo_cargo_door;
                }

                @Override
                public Identifier getTextureForPart(int skinIndex, String partName) {
                    return getSkinFromIndex(skinIndex);
                }

                @Override
                public String[] getStaticParts() {
                    return FRAME;
                }

                @Override
                public String[] getDynamicParts() {
                    return CARGO_PANELS;
                }
            };

    public static final DoorDecl WATER_DOOR =
            new DoorDecl() {

                private Identifier[] skins;

                @Override
                public RegistryHandle<SoundEvent> getOpenSoundEnd() {
                    return ModSounds.DOOR_WGH_BIG_STOP;
                }

                @Override
                public RegistryHandle<SoundEvent> getOpenSoundLoop() {
                    return ModSounds.DOOR_WGH_BIG_START;
                }

                @Override
                public RegistryHandle<SoundEvent> getOpenSoundStart() {
                    return ModSounds.DOOR_LEVER;
                }

                @Override
                public RegistryHandle<SoundEvent> getCloseSoundStart() {
                    return null;
                }

                @Override
                public RegistryHandle<SoundEvent> getCloseSoundEnd() {
                    return ModSounds.DOOR_LEVER;
                }

                @Override
                public float getSoundVolume() {
                    return 2;
                }

                @Override
                public DoorRenderer getSEDNARenderer() {
                    return RenderWaterDoor.INSTANCE;
                }

                @Override
                public BusAnimation getBusAnimation(byte state, byte skinIndex) {
                    if (state == BlockEntityDoorGeneric.STATE_OPENING)
                        return new BusAnimation()
                                .addBus(
                                        "DOOR",
                                        new BusAnimationSequence()
                                                .setPos(0, 0, 0)
                                                .addPos(0, 0, 0, 1500)
                                                .addPos(0, 1, 0, 1500, IType.SIN_FULL))
                                .addBus(
                                        "BOLT",
                                        new BusAnimationSequence()
                                                .setPos(0, 0, 0)
                                                .addPos(0, 0, 1, 1500, IType.SIN_FULL));
                    if (state == BlockEntityDoorGeneric.STATE_CLOSING)
                        return new BusAnimation()
                                .addBus(
                                        "DOOR",
                                        new BusAnimationSequence()
                                                .setPos(0, 1, 0)
                                                .addPos(0, 0, 0, 1500, IType.SIN_FULL))
                                .addBus(
                                        "BOLT",
                                        new BusAnimationSequence()
                                                .setPos(0, 0, 1)
                                                .addPos(0, 0, 1, 1200)
                                                .addPos(0, 0, 0, 1500, IType.SIN_FULL));
                    return null;
                }

                @Override
                public Identifier[] getSEDNASkins() {
                    if (skins == null)
                        skins =
                                new Identifier[] {
                                    ResourceManager.pheo_water_door_tex,
                                    ResourceManager.pheo_water_door_clean_tex
                                };
                    return skins;
                }

                @Override
                public int getSkinCount() {
                    return 2;
                }

                @Override
                public AABB getBlockBound(int x, int y, int z, boolean open, boolean forCollision) {
                    if (!open) return new AABB(0, 0, 0.75, 1, 1, 1);
                    else if (y > 1) return new AABB(0, 0.85, 0.75, 1, 1, 1);
                    else if (y == 0) return new AABB(0, 0, 0.75, 1, forCollision ? 0 : 0.15, 1);
                    return super.getBlockBound(x, y, z, open, forCollision);
                }

                @Override
                public int timeToOpen() {
                    return 60;
                }

                @Override
                public int[][] getDoorOpenRanges() {
                    return new int[][] {{1, 0, 0, -3, 3, 2}};
                }

                @Override
                public float getDoorRangeOpenTime(int ticks, int idx) {
                    return getNormTime(ticks, 35, 40);
                }

                @Override
                public int[] getDimensions() {
                    return new int[] {2, 0, 0, 0, 1, 1};
                }

                @Override
                public HFRWavefrontObject getModel() {
                    return ResourceManager.pheo_water_door;
                }

                @Override
                public Identifier getTextureForPart(int skinIndex, String partName) {
                    return getSkinFromIndex(skinIndex);
                }

                @Override
                public String[] getStaticParts() {
                    return FRAME;
                }

                @Override
                public String[] getDynamicParts() {
                    return WATER_PARTS;
                }

                @Override
                public int getStaticYaw() {
                    return 90;
                }

                @Override
                public float[] getStaticOffset() {
                    return new float[] {0F, 0F, 0.375F};
                }
            };

    public static final DoorDecl VAULT_DOOR =
            new DoorDecl() {

                private final Consumer<BlockEntityDoorGeneric> onUpdate =
                        door -> {
                            if (door.getLevel().isClientSide()) return;
                            if (door.state == BlockEntityDoorGeneric.STATE_OPENING) {
                                if (door.openTicks == 0)
                                    door.playCue(ModSounds.BLOCK_VAULT_SCRAPE_NEW);
                                for (int i = 45; i <= 115; i += 10)
                                    if (door.openTicks == i)
                                        door.playCue(ModSounds.BLOCK_VAULT_THUD_NEW);
                            } else if (door.state == BlockEntityDoorGeneric.STATE_CLOSING) {
                                if (door.openTicks == 30)
                                    door.playCue(ModSounds.BLOCK_VAULT_SCRAPE_NEW);
                                for (int i = 45; i <= 115; i += 10)
                                    if (door.openTicks == i)
                                        door.playCue(ModSounds.BLOCK_VAULT_THUD_NEW);
                            }
                        };

                @Override
                public DoorRenderer getSEDNARenderer() {
                    return RenderVaultDoor.INSTANCE;
                }

                @Override
                public BusAnimation getBusAnimation(byte state, byte skinIndex) {
                    if (state == BlockEntityDoorGeneric.STATE_OPENING)
                        return new BusAnimation()
                                .addBus(
                                        "PULL",
                                        new BusAnimationSequence()
                                                .setPos(0, 0, 0)
                                                .addPos(0, 0, 1, 2_000, IType.SIN_FULL))
                                .addBus(
                                        "SLIDE",
                                        new BusAnimationSequence()
                                                .addPos(0, 0, 0, 2000)
                                                .addPos(1, 0, 0, 4_000));
                    if (state == BlockEntityDoorGeneric.STATE_CLOSING)
                        return new BusAnimation()
                                .addBus(
                                        "PULL",
                                        new BusAnimationSequence()
                                                .setPos(0, 0, 1)
                                                .addPos(0, 0, 1, 4_000)
                                                .addPos(0, 0, 0, 2_000, IType.SIN_FULL))
                                .addBus(
                                        "SLIDE",
                                        new BusAnimationSequence()
                                                .setPos(1, 0, 0)
                                                .addPos(0, 0, 0, 4_000));
                    return null;
                }

                @Override
                public int getSkinCount() {
                    return 7;
                }

                @Override
                public int timeToOpen() {
                    return 120;
                }

                @Override
                public int[][] getDoorOpenRanges() {
                    return new int[][] {{-1, 1, 0, 3, 3, 2}};
                }

                @Override
                public int[] getDimensions() {
                    return new int[] {4, 0, 0, 0, 2, 2};
                }

                @Override
                public int[][] getExtraDimensions() {
                    return new int[][] {{0, 0, 1, -1, 2, 2}};
                }

                @Override
                public AABB getBlockBound(int x, int y, int z, boolean open, boolean forCollision) {
                    if (!open || y == 0) return new AABB(0, 0, 0, 1, 1, 1);
                    else return super.getBlockBound(x, y, z, open, forCollision);
                }

                @Override
                public Consumer<BlockEntityDoorGeneric> onDoorUpdate() {
                    return onUpdate;
                }

                @Override
                public HFRWavefrontObject getModel() {
                    return ResourceManager.pheo_vault_door;
                }

                @Override
                public Identifier getTextureForPart(int skinIndex, String partName) {
                    if ("Label".equals(partName)) {
                        return switch (Math.abs(skinIndex) % 7) {
                            case 1 -> ResourceManager.pheo_label_87;
                            case 2 -> ResourceManager.pheo_label_106;
                            case 3 -> ResourceManager.pheo_label_81;
                            case 4 -> ResourceManager.pheo_label_111;
                            case 5 -> ResourceManager.pheo_label_2;
                            case 6 -> ResourceManager.pheo_label_99;
                            default -> ResourceManager.pheo_label_101;
                        };
                    }
                    return switch (Math.abs(skinIndex) % 7) {
                        case 3, 4 -> ResourceManager.pheo_vault_door_4;
                        case 5, 6 -> ResourceManager.pheo_vault_door_s;
                        default -> ResourceManager.pheo_vault_door_3;
                    };
                }

                @Override
                public Identifier getSkinFromIndex(int index) {
                    return getTextureForPart(index, "Door");
                }

                @Override
                public String[] getStaticParts() {
                    return FRAME;
                }

                @Override
                public String[] getDynamicParts() {
                    return VAULT_PARTS;
                }
            };

    public static final DoorDecl SLIDING_SEAL_DOOR =
            new DoorDecl() {

                @Override
                public RegistryHandle<SoundEvent> getOpenSoundEnd() {
                    return ModSounds.DOOR_SLIDING_SEAL_STOP;
                }

                @Override
                public RegistryHandle<SoundEvent> getOpenSoundStart() {
                    return ModSounds.DOOR_SLIDING_SEAL_OPEN;
                }

                @Override
                public float getSoundVolume() {
                    return 2;
                }

                @Override
                public DoorRenderer getSEDNARenderer() {
                    return RenderSealDoor.INSTANCE;
                }

                @Override
                public BusAnimation getBusAnimation(byte state, byte skinIndex) {
                    if (state == BlockEntityDoorGeneric.STATE_OPENING)
                        return new BusAnimation()
                                .addBus(
                                        "DOOR",
                                        new BusAnimationSequence()
                                                .setPos(0, 0, 0)
                                                .addPos(0, 1, 0, timeToOpen() * 50));
                    if (state == BlockEntityDoorGeneric.STATE_CLOSING)
                        return new BusAnimation()
                                .addBus(
                                        "DOOR",
                                        new BusAnimationSequence()
                                                .setPos(0, 1, 0)
                                                .addPos(0, 0, 0, timeToOpen() * 50));
                    return null;
                }

                @Override
                public int timeToOpen() {
                    return 20;
                }

                @Override
                public AABB getBlockBound(int x, int y, int z, boolean open, boolean forCollision) {
                    if (forCollision && open) return new AABB(0, 0, 0, 0, 0, 0);
                    else return new AABB(0, 0, 1 - 0.25, 1, 1, 1);
                }

                @Override
                public int[][] getDoorOpenRanges() {
                    return new int[][] {{0, 0, 0, 1, 2, 2}};
                }

                @Override
                public int[] getDimensions() {
                    return new int[] {1, 0, 0, 0, 0, 0};
                }

                @Override
                public HFRWavefrontObject getModel() {
                    return ResourceManager.pheo_seal_door;
                }

                @Override
                public Identifier getTextureForPart(int skinIndex, String partName) {
                    return ResourceManager.pheo_seal_door_tex;
                }

                @Override
                public String[] getStaticParts() {
                    return FRAME;
                }

                @Override
                public String[] getDynamicParts() {
                    return DOOR;
                }

                @Override
                public float[] getStaticOffset() {
                    return new float[] {0.5F, 0F, 0F};
                }
            };

    public static final DoorDecl QE_CONTAINMENT =
            new DoorDecl() {

                private Identifier[] skins;

                @Override
                public RegistryHandle<SoundEvent> getOpenSoundEnd() {
                    return ModSounds.DOOR_WGH_STOP;
                }

                @Override
                public RegistryHandle<SoundEvent> getOpenSoundLoop() {
                    return ModSounds.DOOR_WGH_START;
                }

                @Override
                public float getSoundVolume() {
                    return 2;
                }

                @Override
                public DoorRenderer getSEDNARenderer() {
                    return RenderContainmentDoor.INSTANCE;
                }

                @Override
                public BusAnimation getBusAnimation(byte state, byte skinIndex) {
                    if (state == BlockEntityDoorGeneric.STATE_OPENING)
                        return new BusAnimation()
                                .addBus(
                                        "DOOR",
                                        new BusAnimationSequence()
                                                .setPos(0, 0, 0)
                                                .addPos(0, 1, 0, timeToOpen() * 50));
                    if (state == BlockEntityDoorGeneric.STATE_CLOSING)
                        return new BusAnimation()
                                .addBus(
                                        "DOOR",
                                        new BusAnimationSequence()
                                                .setPos(0, 1, 0)
                                                .addPos(0, 0, 0, timeToOpen() * 50));
                    return null;
                }

                @Override
                public Identifier[] getSEDNASkins() {
                    if (skins == null)
                        skins =
                                new Identifier[] {
                                    ResourceManager.pheo_containment_door_tex,
                                    ResourceManager.pheo_containment_door_trefoil_tex,
                                    ResourceManager.pheo_containment_door_trefoil_yellow_tex
                                };
                    return skins;
                }

                @Override
                public int getSkinCount() {
                    return 3;
                }

                @Override
                public int timeToOpen() {
                    return 160;
                }

                @Override
                public int[][] getDoorOpenRanges() {
                    return new int[][] {{-1, 0, 0, 3, 3, 1}};
                }

                @Override
                public int[] getDimensions() {
                    return new int[] {2, 0, 0, 0, 1, 1};
                }

                @Override
                public AABB getBlockBound(int x, int y, int z, boolean open, boolean forCollision) {
                    if (!open) return new AABB(0, 0, 0.5, 1, 1, 1);
                    if (y > 1) return new AABB(0, 0.25, 0.5, 1, 1, 1);
                    else if (y == 0) return new AABB(0, 0, 0.5, 1, forCollision ? 0 : 0.125, 1);
                    return super.getBlockBound(x, y, z, open, forCollision);
                }

                @Override
                public HFRWavefrontObject getModel() {
                    return ResourceManager.pheo_containment_door;
                }

                @Override
                public Identifier getTextureForPart(int skinIndex, String partName) {
                    return getSkinFromIndex(skinIndex);
                }

                @Override
                public String[] getStaticParts() {
                    return FRAME;
                }

                @Override
                public String[] getDynamicParts() {
                    return DOOR;
                }

                @Override
                public float[] getStaticOffset() {
                    return new float[] {0.25F, 0F, 0F};
                }
            };

    public static final DoorDecl ROUND_AIRLOCK_DOOR =
            new DoorDecl() {

                private Identifier[] skins;

                @Override
                public RegistryHandle<SoundEvent> getOpenSoundEnd() {
                    return ModSounds.DOOR_GARAGE_STOP;
                }

                @Override
                public RegistryHandle<SoundEvent> getOpenSoundLoop() {
                    return ModSounds.DOOR_GARAGE_MOVE;
                }

                @Override
                public float getSoundVolume() {
                    return 2;
                }

                @Override
                public DoorRenderer getSEDNARenderer() {
                    return RenderAirlockDoor.INSTANCE;
                }

                @Override
                public BusAnimation getBusAnimation(byte state, byte skinIndex) {
                    if (state == BlockEntityDoorGeneric.STATE_OPENING)
                        return new BusAnimation()
                                .addBus(
                                        "DOOR",
                                        new BusAnimationSequence()
                                                .setPos(0, 0, 0)
                                                .addPos(0, 1, 0, timeToOpen() * 50));
                    if (state == BlockEntityDoorGeneric.STATE_CLOSING)
                        return new BusAnimation()
                                .addBus(
                                        "DOOR",
                                        new BusAnimationSequence()
                                                .setPos(0, 1, 0)
                                                .addPos(0, 0, 0, timeToOpen() * 50));
                    return null;
                }

                @Override
                public Identifier[] getSEDNASkins() {
                    if (skins == null)
                        skins =
                                new Identifier[] {
                                    ResourceManager.pheo_airlock_door_tex,
                                    ResourceManager.pheo_airlock_door_clean_tex,
                                    ResourceManager.pheo_airlock_door_green_tex
                                };
                    return skins;
                }

                @Override
                public int getSkinCount() {
                    return 3;
                }

                @Override
                public AABB getBlockBound(int x, int y, int z, boolean open, boolean forCollision) {
                    if (!open) return super.getBlockBound(x, y, z, open, forCollision);
                    if (z == 1) return new AABB(0.4, 0, 0, 1, 1, 1);
                    else if (z == -2) return new AABB(0, 0, 0, 0.6, 1, 1);
                    else if (y == 3) return new AABB(0, 0.5, 0, 1, 1, 1);
                    else if (y == 0) return new AABB(0, 0, 0, 1, forCollision ? 0 : 0.0625, 1);
                    return super.getBlockBound(x, y, z, open, forCollision);
                }

                @Override
                public int timeToOpen() {
                    return 60;
                }

                @Override
                public int[][] getDoorOpenRanges() {
                    return new int[][] {{0, 0, 0, -2, 4, 2}, {0, 0, 0, 3, 4, 2}};
                }

                @Override
                public int[] getDimensions() {
                    return new int[] {3, 0, 0, 0, 2, 1};
                }

                @Override
                public HFRWavefrontObject getModel() {
                    return ResourceManager.pheo_airlock_door;
                }

                @Override
                public Identifier getTextureForPart(int skinIndex, String partName) {
                    return getSkinFromIndex(skinIndex);
                }

                @Override
                public String[] getStaticParts() {
                    return FRAME;
                }

                @Override
                public String[] getDynamicParts() {
                    return LEFT_RIGHT;
                }

                @Override
                public float[] getStaticOffset() {
                    return new float[] {0F, 0F, 0.5F};
                }
            };

    public static final DoorDecl SLIDE_DOOR =
            new DoorDecl() {

                @Override
                public RegistryHandle<SoundEvent> getOpenSoundEnd() {
                    return ModSounds.DOOR_SLIDING_DOOR_OPENED;
                }

                @Override
                public RegistryHandle<SoundEvent> getCloseSoundEnd() {
                    return ModSounds.DOOR_SLIDING_DOOR_SHUT;
                }

                @Override
                public RegistryHandle<SoundEvent> getOpenSoundLoop() {
                    return ModSounds.DOOR_SLIDING_DOOR_OPENING;
                }

                @Override
                public RegistryHandle<SoundEvent> getSoundLoop2() {
                    return ModSounds.DOOR_SLIDING_DOOR_OPENING;
                }

                @Override
                public float getSoundVolume() {
                    return 2;
                }

                @Override
                public DoorRenderer getSEDNARenderer() {
                    return RenderSlidingBlastDoor.INSTANCE;
                }

                @Override
                public BusAnimation getBusAnimation(byte state, byte skinIndex) {
                    if (state == BlockEntityDoorGeneric.STATE_OPENING)
                        return new BusAnimation()
                                .addBus(
                                        "LOCK",
                                        new BusAnimationSequence()
                                                .setPos(0, 0, 0)
                                                .addPos(1, 0, 0, 200))
                                .addBus(
                                        "DOOR",
                                        new BusAnimationSequence()
                                                .setPos(0, 0, 0)
                                                .addPos(0, 0, 0, 350)
                                                .addPos(0, 0.05, 0, 200)
                                                .addPos(0, 1, 0, 650, IType.SIN_UP));
                    if (state == BlockEntityDoorGeneric.STATE_CLOSING)
                        return new BusAnimation()
                                .addBus(
                                        "LOCK",
                                        new BusAnimationSequence()
                                                .setPos(1, 0, 0)
                                                .addPos(1, 0, 0, 1000)
                                                .addPos(0, 0, 0, 200))
                                .addBus(
                                        "DOOR",
                                        new BusAnimationSequence()
                                                .setPos(0, 1, 0)
                                                .addPos(0, 0.05, 0, 650, IType.SIN_UP)
                                                .addPos(0, 0, 0, 200));
                    return null;
                }

                @Override
                public int timeToOpen() {
                    return 24;
                }

                @Override
                public int[][] getDoorOpenRanges() {
                    return new int[][] {{-2, 0, 0, 4, 5, 1}};
                }

                @Override
                public int[] getDimensions() {
                    return new int[] {3, 0, 0, 0, 3, 3};
                }

                @Override
                public AABB getBlockBound(int x, int y, int z, boolean open, boolean forCollision) {
                    if (open) {
                        if (y == 3) return new AABB(0, 0.5, 0, 1, 1, 1);
                        else if (y == 0) return new AABB(0, 0, 0, 1, forCollision ? 0 : 0.08, 1);
                    }
                    return super.getBlockBound(x, y, z, open, forCollision);
                }

                @Override
                public int getSkinCount() {
                    return 3;
                }

                @Override
                public HFRWavefrontObject getModel() {
                    return ResourceManager.pheo_blast_door;
                }

                @Override
                public Identifier getTextureForPart(int skinIndex, String partName) {
                    return ResourceManager.pheo_blast_door_tex;
                }

                @Override
                public String[] getStaticParts() {
                    return FRAME;
                }

                @Override
                public String[] getDynamicParts() {
                    return BLAST_PARTS;
                }
            };

    public static final DoorDecl LARGE_VEHICLE_DOOR =
            new DoorDecl() {

                @Override
                public RegistryHandle<SoundEvent> getOpenSoundEnd() {
                    return ModSounds.DOOR_GARAGE_STOP;
                }

                @Override
                public RegistryHandle<SoundEvent> getOpenSoundLoop() {
                    return ModSounds.DOOR_GARAGE_MOVE;
                }

                @Override
                public float getSoundVolume() {
                    return 2;
                }

                @Override
                public DoorRenderer getSEDNARenderer() {
                    return RenderVehicleDoor.INSTANCE;
                }

                @Override
                public BusAnimation getBusAnimation(byte state, byte skinIndex) {
                    if (state == BlockEntityDoorGeneric.STATE_OPENING)
                        return new BusAnimation()
                                .addBus(
                                        "DOOR",
                                        new BusAnimationSequence()
                                                .setPos(0, 0, 0)
                                                .addPos(0, 1, 0, timeToOpen() * 50));
                    if (state == BlockEntityDoorGeneric.STATE_CLOSING)
                        return new BusAnimation()
                                .addBus(
                                        "DOOR",
                                        new BusAnimationSequence()
                                                .setPos(0, 1, 0)
                                                .addPos(0, 0, 0, timeToOpen() * 50));
                    return null;
                }

                @Override
                public AABB getBlockBound(int x, int y, int z, boolean open, boolean forCollision) {
                    if (!open) return super.getBlockBound(x, y, z, open, forCollision);
                    if (z == 3) return new AABB(0.4, 0, 0, 1, 1, 1);
                    else if (z == -3) return new AABB(0, 0, 0, 0.6, 1, 1);
                    else if (y == 0) return new AABB(0, 0, 0, 1, forCollision ? 0 : 0.0625, 1);
                    return super.getBlockBound(x, y, z, open, forCollision);
                }

                @Override
                public int timeToOpen() {
                    return 60;
                }

                @Override
                public int[][] getDoorOpenRanges() {
                    return new int[][] {{0, 0, 0, -4, 6, 2}, {0, 0, 0, 4, 6, 2}};
                }

                @Override
                public int[] getDimensions() {
                    return new int[] {5, 0, 0, 0, 3, 3};
                }

                @Override
                public HFRWavefrontObject getModel() {
                    return ResourceManager.pheo_vehicle_door;
                }

                @Override
                public Identifier getTextureForPart(int skinIndex, String partName) {
                    return ResourceManager.pheo_vehicle_door_tex;
                }

                @Override
                public String[] getStaticParts() {
                    return FRAME;
                }

                @Override
                public String[] getDynamicParts() {
                    return LEFT_RIGHT;
                }

                @Override
                public int getStaticYaw() {
                    return 90;
                }
            };

    public static final DoorDecl TRANSITION_SEAL =
            new DoorDecl() {

                @Override
                public RegistryHandle<SoundEvent> getOpenSoundStart() {
                    return ModSounds.DOOR_TRANSITION_SEAL_OPEN;
                }

                @Override
                public float getSoundVolume() {
                    return 6;
                }

                @Override
                public DoorRenderer getSEDNARenderer() {
                    return RenderTransitionSeal.INSTANCE;
                }

                @Override
                public int timeToOpen() {
                    return 480;
                }

                @Override
                public int[][] getDoorOpenRanges() {

                    return new int[][] {{-9, 2, 0, 20, 20, 1}};
                }

                @Override
                public int[] getDimensions() {
                    return new int[] {23, 0, 0, 0, 13, 12};
                }

                @Override
                public String[] getStaticParts() {
                    return NOTHING;
                }

                @Override
                public String[] getDynamicParts() {
                    return NOTHING;
                }

                @Override
                public Identifier getTextureForPart(int skinIndex, String partName) {
                    return ResourceManager.transition_seal_tex;
                }
            };

    private static final String[] NOTHING = {};
    private static final float[] NO_OFFSET = {0F, 0F, 0F};
    private static final String[] FRAME = {"Frame"};
    private static final String[] HATCH = {"Hatch"};
    private static final String[] DOOR = {"Door"};
    private static final String[] LEFT_RIGHT = {"Left", "Right"};
    private static final String[] CARGO_PANELS = {"DoorTop", "DoorBot"};
    private static final String[] WATER_PARTS = {"Door_Cube.003", "Bolts", "Top", "Bottom"};
    private static final String[] VAULT_PARTS = {"Door", "Label"};
    private static final String[] BLAST_PARTS = {"LeftDoor", "RightLock", "RightDoor", "LeftLock"};
    private int[] dynamicPartIds;

    public static float smoothstep(float t) {
        t = Mth.clamp(t, 0F, 1F);
        return t * t * (3F - 2F * t);
    }

    public abstract int[][] getDoorOpenRanges();

    public abstract int[] getDimensions();

    public int[][] getExtraDimensions() {
        return null;
    }

    public int getBlockOffset() {
        return 0;
    }

    public boolean remoteControllable() {
        return false;
    }

    public float getDoorRangeOpenTime(int ticks, int idx) {
        return getNormTime(ticks);
    }

    public int timeToOpen() {
        return 20;
    }

    public float getNormTime(float time) {
        return getNormTime(time, 0, timeToOpen());
    }

    public float getNormTime(float time, float min, float max) {
        return Mth.clamp((time - min) / (max - min), 0F, 1F);
    }

    public Identifier getTextureForPart(int skinIndex, String partName) {
        return null;
    }

    public HFRWavefrontObject getModel() {
        return null;
    }

    public String[] getStaticParts() {
        return NOTHING;
    }

    public String[] getDynamicParts() {
        return NOTHING;
    }

    public int[] getDynamicPartIds() {
        int[] ids = dynamicPartIds;
        if (ids == null) ids = dynamicPartIds = getModel().partIds(getDynamicParts());
        return ids;
    }

    public void getTranslation(String partName, float openTicks, boolean child, float[] trans) {
        set(trans, 0, 0, 0);
    }

    public void getRotation(String partName, float openTicks, float[] rot) {
        set(rot, 0, 0, 0);
    }

    public void getOrigin(String partName, float[] orig) {
        set(orig, 0, 0, 0);
    }

    public AABB getBlockBound(int x, int y, int z, boolean open, boolean forCollision) {
        return open ? new AABB(0, 0, 0, 0, 0, 0) : new AABB(0, 0, 0, 1, 1, 1);
    }

    public boolean isLadder(boolean open) {
        return false;
    }

    public @Nullable RegistryHandle<SoundEvent> getOpenSoundLoop() {
        return null;
    }

    public @Nullable RegistryHandle<SoundEvent> getSoundLoop2() {
        return null;
    }

    public @Nullable RegistryHandle<SoundEvent> getCloseSoundLoop() {
        return getOpenSoundLoop();
    }

    public @Nullable RegistryHandle<SoundEvent> getOpenSoundStart() {
        return null;
    }

    public @Nullable RegistryHandle<SoundEvent> getCloseSoundStart() {
        return getOpenSoundStart();
    }

    public @Nullable RegistryHandle<SoundEvent> getOpenSoundEnd() {
        return null;
    }

    public @Nullable RegistryHandle<SoundEvent> getCloseSoundEnd() {
        return getOpenSoundEnd();
    }

    public float getSoundVolume() {
        return 1;
    }

    public float[] set(float[] f, float x, float y, float z) {
        f[0] = x;
        f[1] = y;
        f[2] = z;
        return f;
    }

    public boolean hasSkins() {
        return getSkinCount() > 0;
    }

    public int getSkinCount() {
        return 0;
    }

    public List<Identifier> getDistinctTextures(String partName) {
        List<Identifier> distinct = new ArrayList<>();
        for (int i = 0; i < Math.max(1, getSkinCount()); i++) {
            Identifier texture = getTextureForPart(i, partName);
            if (!distinct.contains(texture)) distinct.add(texture);
        }
        return distinct;
    }

    public @Nullable Consumer<BlockEntityDoorGeneric> onDoorUpdate() {
        return null;
    }

    public @Nullable DoorRenderer getSEDNARenderer() {
        return null;
    }

    public @Nullable BusAnimation getBusAnimation(byte state, byte skinIndex) {
        return null;
    }

    public HbmAnimations.@Nullable Animation getSEDNAAnim(
            byte state, byte skinIndex, long startMillis) {
        BusAnimation anim = getBusAnimation(state, skinIndex);
        return anim == null ? null : new HbmAnimations.Animation("DOOR_ANIM", startMillis, anim);
    }

    public float[] getStaticOffset() {
        return NO_OFFSET;
    }

    public int getStaticYaw() {
        return 0;
    }

    public Identifier @Nullable [] getSEDNASkins() {
        return null;
    }

    public Identifier getSkinFromIndex(int index) {
        Identifier[] skins = getSEDNASkins();
        return skins[Math.abs(index) % skins.length];
    }
}
