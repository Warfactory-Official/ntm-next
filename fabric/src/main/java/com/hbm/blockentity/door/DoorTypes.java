/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-FileCopyrightText: Contributors to Hbm's Nuclear Tech Mod
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 * Copyright (C) Contributors to Hbm's Nuclear Tech Mod. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License version 3 only,
 * as published by the Free Software Foundation.
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this code. If not, see <https://www.gnu.org/licenses/>.
 *
 * Under GPLv3 §7(c) and §7(e), these additional terms apply to LGPL-covered
 * material authored by movblock:
 *
 * - the license grants no rights to the name "NTM NEXT";
 * - a modified version must not be named NTM NEXT or presented as the
 *   official release, and must be marked as different from it.
 *
 * Accurately identifying a work as derived from NTM NEXT does not violate
 * these additional terms, provided it is clearly distinguished from the
 * official release and does not imply endorsement. Ports are encouraged to
 * acknowledge NTM NEXT as their upstream project and provide a link to it.
 */

package com.hbm.blockentity.door;

import com.hbm.client.render.AirlockDoorRenderer;
import com.hbm.client.render.CargoDoorRenderer;
import com.hbm.client.render.ContainmentDoorRenderer;
import com.hbm.client.render.DoorAnimations;
import com.hbm.client.render.DoorRenderDefinition;
import com.hbm.client.render.DoorTextures;
import com.hbm.client.render.FireDoorRenderer;
import com.hbm.client.render.SealDoorRenderer;
import com.hbm.client.render.SecureDoorRenderer;
import com.hbm.client.render.SlidingBlastDoorRenderer;
import com.hbm.client.render.SlidingDoorRenderer;
import com.hbm.client.render.TransitionSealRenderer;
import com.hbm.client.render.VaultDoorRenderer;
import com.hbm.client.render.VehicleDoorRenderer;
import com.hbm.client.render.WaterDoorRenderer;
import com.hbm.main.ResourceManager;
import com.hbm.render.anim.AnimationSequence;
import com.hbm.render.anim.BusAnimation;
import com.hbm.render.anim.BusAnimationKeyframe.IType;
import com.hbm.sound.ModSounds;
import java.util.List;

import static com.hbm.blockentity.door.DoorCollisionShapes.Box.box;
import static com.hbm.blockentity.door.DoorCollisionShapes.Box.floor;
import static com.hbm.blockentity.door.DoorCollisionShapes.Box.split;
import static com.hbm.blockentity.door.DoorCollisionShapes.Cell.y;
import static com.hbm.blockentity.door.DoorCollisionShapes.Cell.yAbove;
import static com.hbm.blockentity.door.DoorCollisionShapes.Cell.z;

public final class DoorTypes {

    public static final DoorType SILO_HATCH =
            DoorType.builder(60)
                    .footprint(0, 0, 2, 2, 2, 2)
                    .offset(2)
                    .row(1, 0, 1, -3, 3, 0)
                    .row(0, 0, 1, -3, 3, 0)
                    .row(-1, 0, 1, -3, 3, 0)
                    .rowsOver(20, 20)
                    .sounds(
                            () ->
                                    DoorSounds.of(
                                            null,
                                            ModSounds.DOOR_WGH_BIG_START,
                                            ModSounds.DOOR_WGH_BIG_STOP))
                    .volume(2)
                    .remote()
                    .hinge(new DoorHinge("Hatch", 0, 0.875F, -1.875F, -240, 20, 100, 0.25F, 0, 10))
                    .look(
                            kind ->
                                    DoorRenderDefinition.builder()
                                            .model(() -> ResourceManager.silo_hatch)
                                            .staticParts("Frame")
                                            .movingParts("Hatch")
                                            .skins(
                                                    DoorTextures.fixed(
                                                            ResourceManager.silo_hatch_tex))
                                            .build(kind))
                    .build();

    public static final DoorType SILO_HATCH_LARGE =
            DoorType.builder(60)
                    .footprint(0, 0, 3, 3, 3, 3)
                    .offset(3)
                    .row(2, 0, 1, -3, 3, 0)
                    .row(1, 0, 2, -5, 3, 0)
                    .row(0, 0, 2, -5, 3, 0)
                    .row(-1, 0, 2, -5, 3, 0)
                    .row(-2, 0, 1, -3, 3, 0)
                    .rowsOver(20, 20)
                    .sounds(
                            () ->
                                    DoorSounds.of(
                                            null,
                                            ModSounds.DOOR_WGH_BIG_START,
                                            ModSounds.DOOR_WGH_BIG_STOP))
                    .volume(2)
                    .remote()
                    .hinge(new DoorHinge("Hatch", 0, 0.875F, -2.875F, -240, 20, 100, 0.25F, 0, 10))
                    .look(
                            kind ->
                                    DoorRenderDefinition.builder()
                                            .model(() -> ResourceManager.silo_hatch_large)
                                            .staticParts("Frame")
                                            .movingParts("Hatch")
                                            .skins(
                                                    DoorTextures.fixed(
                                                            ResourceManager.silo_hatch_large_tex))
                                            .build(kind))
                    .build();

    public static final DoorType FIRE_DOOR =
            DoorType.builder(160)
                    .footprint(2, 0, 0, 0, 2, 1)
                    .row(-1, 0, 0, 3, 4, 1)
                    .bounds(
                            DoorCollisionShapes.DEFAULT
                                    .open(z(1), box(0.5, 0, 0, 1, 1, 1))
                                    .open(z(-2), box(0, 0, 0, 0.5, 1, 1))
                                    .open(yAbove(1), box(0, 0.75, 0, 1, 1, 1))
                                    .open(y(0), floor(0, 0, 1, 1, 0.1)))
                    .sounds(
                            () ->
                                    DoorSounds.of(
                                                    null,
                                                    ModSounds.DOOR_WGH_START_STREAM,
                                                    ModSounds.DOOR_WGH_STOP_STREAM)
                                            .withSecondLoop(ModSounds.DOOR_ALARM6))
                    .volume(2)
                    .skins(5)
                    .look(
                            kind ->
                                    DoorRenderDefinition.builder()
                                            .motion(FireDoorRenderer.INSTANCE)
                                            .clips(DoorAnimations.linear(kind.getOpeningTicks()))
                                            .model(() -> ResourceManager.pheo_fire_door)
                                            .staticParts("Frame")
                                            .movingParts("Door")
                                            .skins(
                                                    DoorTextures.cycle(
                                                            ResourceManager.pheo_fire_door_tex,
                                                            ResourceManager
                                                                    .pheo_fire_door_black_tex,
                                                            ResourceManager
                                                                    .pheo_fire_door_orange_tex,
                                                            ResourceManager
                                                                    .pheo_fire_door_yellow_tex,
                                                            ResourceManager
                                                                    .pheo_fire_door_trefoil_tex))
                                            .yaw(90)
                                            .offset(-0.5F, 0, 0)
                                            .build(kind))
                    .build();

    public static final DoorType SECURE_ACCESS_DOOR =
            DoorType.builder(120)
                    .footprint(4, 0, 0, 0, 2, 2)
                    .row(-2, 1, 0, 4, 5, 1)
                    .bounds(
                            DoorCollisionShapes.DEFAULT
                                    .closed(yAbove(0), box(0, 0, 0.375, 1, 1, 0.625))
                                    .open(y(1), floor(0, 0, 1, 1, 0.0625))
                                    .open(y(4), box(0, 0.5, 0.15, 1, 1, 0.85))
                                    .open(y(0), DoorCollisionShapes.Box.FULL))
                    .sounds(
                            () ->
                                    DoorSounds.of(
                                            null,
                                            ModSounds.DOOR_GARAGE_MOVE,
                                            ModSounds.DOOR_GARAGE_STOP))
                    .volume(2)
                    .skins(4)
                    .look(
                            kind ->
                                    DoorRenderDefinition.builder()
                                            .motion(SecureDoorRenderer.INSTANCE)
                                            .clips(DoorAnimations.linear(kind.getOpeningTicks()))
                                            .model(() -> ResourceManager.pheo_secure_door)
                                            .staticParts("Frame")
                                            .movingParts("Door")
                                            .skins(
                                                    DoorTextures.cycle(
                                                            ResourceManager.pheo_secure_door_tex,
                                                            ResourceManager
                                                                    .pheo_secure_door_grey_tex,
                                                            ResourceManager
                                                                    .pheo_secure_door_black_tex,
                                                            ResourceManager
                                                                    .pheo_secure_door_yellow_tex))
                                            .offset(0, 1, 0)
                                            .build(kind))
                    .build();

    public static final DoorType QE_SLIDING_DOOR =
            DoorType.builder(10)
                    .footprint(1, 0, 0, 0, 1, 0)
                    .row(0, 0, 0, 2, 2, 2)
                    .bounds(
                            DoorCollisionShapes.DEFAULT
                                    .closed(box(0, 0, 0.8125, 1, 1, 1))
                                    .open(
                                            z(0),
                                            split(
                                                    box(0, 0, 0.8125, 1, 1, 1),
                                                    box(0.875, 0, 0.8125, 1, 1, 1)))
                                    .open(
                                            split(
                                                    box(0, 0, 0.8125, 1, 1, 1),
                                                    box(0, 0, 0.8125, 0.125, 1, 1))))
                    .sounds(
                            () ->
                                    DoorSounds.of(
                                                    null,
                                                    ModSounds.DOOR_QE_SLIDING_OPENING,
                                                    ModSounds.DOOR_QE_SLIDING_OPENED)
                                            .withCloseEnd(ModSounds.DOOR_QE_SLIDING_SHUT))
                    .volume(2)
                    .look(
                            kind ->
                                    DoorRenderDefinition.builder()
                                            .motion(SlidingDoorRenderer.INSTANCE)
                                            .clips(DoorAnimations.linear(kind.getOpeningTicks()))
                                            .model(() -> ResourceManager.pheo_sliding_door)
                                            .staticParts("Frame")
                                            .movingParts("Left", "Right")
                                            .skins(
                                                    DoorTextures.fixed(
                                                            ResourceManager.pheo_sliding_door_tex))
                                            .offset(0.53125F, 0.001F, 0.5F)
                                            .build(kind))
                    .build();

    public static final DoorType CARGO_DOOR =
            DoorType.builder(60)
                    .footprint(2, 0, 0, 0, 1, 1)
                    .row(-1, -1, 0, 3, 3, 1)
                    .bounds(
                            DoorCollisionShapes.DEFAULT
                                    .closed(box(0, 0, 0.375, 1, 1, 0.625))
                                    .open(yAbove(1), box(0, 0.25, 0.375, 1, 1, 0.625))
                                    .open(y(0), floor(0, 0.375, 1, 0.625, 0.125)))
                    .sounds(
                            () ->
                                    DoorSounds.of(
                                            null,
                                            ModSounds.DOOR_GARAGE_MOVE,
                                            ModSounds.DOOR_GARAGE_STOP))
                    .volume(2)
                    .skins(1)
                    .look(
                            kind -> {
                                int half = kind.getOpeningTicks() * 25;
                                return DoorRenderDefinition.builder()
                                        .motion(CargoDoorRenderer.INSTANCE)
                                        .clips(
                                                DoorAnimations.of(
                                                        () ->
                                                                new BusAnimation()
                                                                        .addBus(
                                                                                "BOT",
                                                                                new AnimationSequence()
                                                                                        .jump(
                                                                                                0,
                                                                                                0,
                                                                                                0)
                                                                                        .to(
                                                                                                0,
                                                                                                1,
                                                                                                0,
                                                                                                2
                                                                                                        * half))
                                                                        .addBus(
                                                                                "TOP",
                                                                                new AnimationSequence()
                                                                                        .jump(
                                                                                                0,
                                                                                                0,
                                                                                                0)
                                                                                        .to(
                                                                                                0,
                                                                                                0,
                                                                                                0,
                                                                                                half)
                                                                                        .to(
                                                                                                0,
                                                                                                1,
                                                                                                0,
                                                                                                half)),
                                                        () ->
                                                                new BusAnimation()
                                                                        .addBus(
                                                                                "BOT",
                                                                                new AnimationSequence()
                                                                                        .jump(
                                                                                                0,
                                                                                                1,
                                                                                                0)
                                                                                        .to(
                                                                                                0,
                                                                                                0,
                                                                                                0,
                                                                                                2
                                                                                                        * half))
                                                                        .addBus(
                                                                                "TOP",
                                                                                new AnimationSequence()
                                                                                        .jump(
                                                                                                0,
                                                                                                1,
                                                                                                0)
                                                                                        .to(
                                                                                                0,
                                                                                                1,
                                                                                                0,
                                                                                                half)
                                                                                        .to(
                                                                                                0,
                                                                                                0,
                                                                                                0,
                                                                                                half))))
                                        .model(() -> ResourceManager.pheo_cargo_door)
                                        .staticParts("Frame")
                                        .movingParts("DoorTop", "DoorBot")
                                        .skins(
                                                DoorTextures.cycle(
                                                        ResourceManager.pheo_cargo_door_tex))
                                        .build(kind);
                            })
                    .build();

    public static final DoorType WATER_DOOR =
            DoorType.builder(60)
                    .footprint(2, 0, 0, 0, 1, 1)
                    .row(1, 0, 0, -3, 3, 2)
                    .rowsOver(35, 40)
                    .bounds(
                            DoorCollisionShapes.DEFAULT
                                    .closed(box(0, 0, 0.75, 1, 1, 1))
                                    .open(yAbove(1), box(0, 0.85, 0.75, 1, 1, 1))
                                    .open(y(0), floor(0, 0.75, 1, 1, 0.15)))
                    .sounds(
                            () ->
                                    DoorSounds.of(
                                                    ModSounds.DOOR_LEVER,
                                                    ModSounds.DOOR_WGH_BIG_START,
                                                    ModSounds.DOOR_WGH_BIG_STOP)
                                            .withCloseStart(null)
                                            .withCloseEnd(ModSounds.DOOR_LEVER))
                    .volume(2)
                    .skins(2)
                    .larpExempt()
                    .look(
                            kind ->
                                    DoorRenderDefinition.builder()
                                            .motion(WaterDoorRenderer.INSTANCE)
                                            .clips(
                                                    DoorAnimations.of(
                                                            () ->
                                                                    new BusAnimation()
                                                                            .addBus(
                                                                                    "DOOR",
                                                                                    new AnimationSequence()
                                                                                            .jump(
                                                                                                    0,
                                                                                                    0,
                                                                                                    0)
                                                                                            .to(
                                                                                                    0,
                                                                                                    0,
                                                                                                    0,
                                                                                                    1500)
                                                                                            .to(
                                                                                                    0,
                                                                                                    1,
                                                                                                    0,
                                                                                                    1500,
                                                                                                    IType
                                                                                                            .SIN_FULL))
                                                                            .addBus(
                                                                                    "BOLT",
                                                                                    new AnimationSequence()
                                                                                            .jump(
                                                                                                    0,
                                                                                                    0,
                                                                                                    0)
                                                                                            .to(
                                                                                                    0,
                                                                                                    0,
                                                                                                    1,
                                                                                                    1500,
                                                                                                    IType
                                                                                                            .SIN_FULL)),
                                                            () ->
                                                                    new BusAnimation()
                                                                            .addBus(
                                                                                    "DOOR",
                                                                                    new AnimationSequence()
                                                                                            .jump(
                                                                                                    0,
                                                                                                    1,
                                                                                                    0)
                                                                                            .to(
                                                                                                    0,
                                                                                                    0,
                                                                                                    0,
                                                                                                    1500,
                                                                                                    IType
                                                                                                            .SIN_FULL))
                                                                            .addBus(
                                                                                    "BOLT",
                                                                                    new AnimationSequence()
                                                                                            .jump(
                                                                                                    0,
                                                                                                    0,
                                                                                                    1)
                                                                                            .to(
                                                                                                    0,
                                                                                                    0,
                                                                                                    1,
                                                                                                    1200)
                                                                                            .to(
                                                                                                    0,
                                                                                                    0,
                                                                                                    0,
                                                                                                    1500,
                                                                                                    IType
                                                                                                            .SIN_FULL))))
                                            .model(() -> ResourceManager.pheo_water_door)
                                            .staticParts("Frame")
                                            .movingParts("Door_Cube.003", "Bolts", "Top", "Bottom")
                                            .skins(
                                                    DoorTextures.cycle(
                                                            ResourceManager.pheo_water_door_tex,
                                                            ResourceManager
                                                                    .pheo_water_door_clean_tex))
                                            .yaw(90)
                                            .offset(0, 0, 0.375F)
                                            .build(kind))
                    .build();

    public static final DoorType VAULT_DOOR =
            DoorType.builder(120)
                    .footprint(4, 0, 0, 0, 2, 2)
                    .extra(0, 0, 1, -1, 2, 2)
                    .row(-1, 1, 0, 3, 3, 2)
                    .bounds(DoorCollisionShapes.DEFAULT.open(y(0), DoorCollisionShapes.Box.FULL))
                    .skins(7)
                    .cues(
                            new DoorSoundTimeline(
                                    () -> ModSounds.BLOCK_VAULT_SCRAPE_NEW,
                                    0,
                                    30,
                                    () -> ModSounds.BLOCK_VAULT_THUD_NEW,
                                    45,
                                    115,
                                    10))
                    .look(
                            kind ->
                                    DoorRenderDefinition.builder()
                                            .motion(VaultDoorRenderer.INSTANCE)
                                            .clips(
                                                    DoorAnimations.of(
                                                            () ->
                                                                    new BusAnimation()
                                                                            .addBus(
                                                                                    "PULL",
                                                                                    new AnimationSequence()
                                                                                            .jump(
                                                                                                    0,
                                                                                                    0,
                                                                                                    0)
                                                                                            .to(
                                                                                                    0,
                                                                                                    0,
                                                                                                    1,
                                                                                                    2000,
                                                                                                    IType
                                                                                                            .SIN_FULL))
                                                                            .addBus(
                                                                                    "SLIDE",
                                                                                    new AnimationSequence()
                                                                                            .to(
                                                                                                    0,
                                                                                                    0,
                                                                                                    0,
                                                                                                    2000)
                                                                                            .to(
                                                                                                    1,
                                                                                                    0,
                                                                                                    0,
                                                                                                    4000)),
                                                            () ->
                                                                    new BusAnimation()
                                                                            .addBus(
                                                                                    "PULL",
                                                                                    new AnimationSequence()
                                                                                            .jump(
                                                                                                    0,
                                                                                                    0,
                                                                                                    1)
                                                                                            .to(
                                                                                                    0,
                                                                                                    0,
                                                                                                    1,
                                                                                                    4000)
                                                                                            .to(
                                                                                                    0,
                                                                                                    0,
                                                                                                    0,
                                                                                                    2000,
                                                                                                    IType
                                                                                                            .SIN_FULL))
                                                                            .addBus(
                                                                                    "SLIDE",
                                                                                    new AnimationSequence()
                                                                                            .jump(
                                                                                                    1,
                                                                                                    0,
                                                                                                    0)
                                                                                            .to(
                                                                                                    0,
                                                                                                    0,
                                                                                                    0,
                                                                                                    4000))))
                                            .model(() -> ResourceManager.pheo_vault_door)
                                            .staticParts("Frame")
                                            .movingParts("Door", "Label")
                                            .skins(
                                                    DoorTextures.perPart(
                                                            List.of(
                                                                    ResourceManager
                                                                            .pheo_vault_door_3,
                                                                    ResourceManager
                                                                            .pheo_vault_door_3,
                                                                    ResourceManager
                                                                            .pheo_vault_door_3,
                                                                    ResourceManager
                                                                            .pheo_vault_door_4,
                                                                    ResourceManager
                                                                            .pheo_vault_door_4,
                                                                    ResourceManager
                                                                            .pheo_vault_door_s,
                                                                    ResourceManager
                                                                            .pheo_vault_door_s),
                                                            "Label",
                                                            List.of(
                                                                    ResourceManager.pheo_label_101,
                                                                    ResourceManager.pheo_label_87,
                                                                    ResourceManager.pheo_label_106,
                                                                    ResourceManager.pheo_label_81,
                                                                    ResourceManager.pheo_label_111,
                                                                    ResourceManager.pheo_label_2,
                                                                    ResourceManager.pheo_label_99)))
                                            .build(kind))
                    .build();

    public static final DoorType SLIDING_SEAL_DOOR =
            DoorType.builder(20)
                    .footprint(1, 0, 0, 0, 0, 0)
                    .row(0, 0, 0, 1, 2, 2)
                    .bounds(
                            DoorCollisionShapes.DEFAULT
                                    .closed(box(0, 0, 0.75, 1, 1, 1))
                                    .open(
                                            split(
                                                    box(0, 0, 0.75, 1, 1, 1),
                                                    DoorCollisionShapes.Box.ZERO)))
                    .sounds(
                            () ->
                                    DoorSounds.of(
                                            ModSounds.DOOR_SLIDING_SEAL_OPEN,
                                            null,
                                            ModSounds.DOOR_SLIDING_SEAL_STOP))
                    .volume(2)
                    .look(
                            kind ->
                                    DoorRenderDefinition.builder()
                                            .motion(SealDoorRenderer.INSTANCE)
                                            .clips(DoorAnimations.linear(kind.getOpeningTicks()))
                                            .model(() -> ResourceManager.pheo_seal_door)
                                            .staticParts("Frame")
                                            .movingParts("Door")
                                            .skins(
                                                    DoorTextures.fixed(
                                                            ResourceManager.pheo_seal_door_tex))
                                            .offset(0.5F, 0, 0)
                                            .build(kind))
                    .build();

    public static final DoorType QE_CONTAINMENT =
            DoorType.builder(160)
                    .footprint(2, 0, 0, 0, 1, 1)
                    .row(-1, 0, 0, 3, 3, 1)
                    .bounds(
                            DoorCollisionShapes.DEFAULT
                                    .closed(box(0, 0, 0.5, 1, 1, 1))
                                    .open(yAbove(1), box(0, 0.25, 0.5, 1, 1, 1))
                                    .open(y(0), floor(0, 0.5, 1, 1, 0.125)))
                    .sounds(
                            () ->
                                    DoorSounds.of(
                                            null,
                                            ModSounds.DOOR_WGH_START,
                                            ModSounds.DOOR_WGH_STOP))
                    .volume(2)
                    .skins(3)
                    .look(
                            kind ->
                                    DoorRenderDefinition.builder()
                                            .motion(ContainmentDoorRenderer.INSTANCE)
                                            .clips(DoorAnimations.linear(kind.getOpeningTicks()))
                                            .model(() -> ResourceManager.pheo_containment_door)
                                            .staticParts("Frame")
                                            .movingParts("Door")
                                            .skins(
                                                    DoorTextures.cycle(
                                                            ResourceManager
                                                                    .pheo_containment_door_tex,
                                                            ResourceManager
                                                                    .pheo_containment_door_trefoil_tex,
                                                            ResourceManager
                                                                    .pheo_containment_door_trefoil_yellow_tex))
                                            .offset(0.25F, 0, 0)
                                            .build(kind))
                    .build();

    public static final DoorType ROUND_AIRLOCK_DOOR =
            DoorType.builder(60)
                    .footprint(3, 0, 0, 0, 2, 1)
                    .row(0, 0, 0, -2, 4, 2)
                    .row(0, 0, 0, 3, 4, 2)
                    .bounds(
                            DoorCollisionShapes.DEFAULT
                                    .open(z(1), box(0.4, 0, 0, 1, 1, 1))
                                    .open(z(-2), box(0, 0, 0, 0.6, 1, 1))
                                    .open(y(3), box(0, 0.5, 0, 1, 1, 1))
                                    .open(y(0), floor(0, 0, 1, 1, 0.0625)))
                    .sounds(
                            () ->
                                    DoorSounds.of(
                                            null,
                                            ModSounds.DOOR_GARAGE_MOVE,
                                            ModSounds.DOOR_GARAGE_STOP))
                    .volume(2)
                    .skins(3)
                    .look(
                            kind ->
                                    DoorRenderDefinition.builder()
                                            .motion(AirlockDoorRenderer.INSTANCE)
                                            .clips(DoorAnimations.linear(kind.getOpeningTicks()))
                                            .model(() -> ResourceManager.pheo_airlock_door)
                                            .staticParts("Frame")
                                            .movingParts("Left", "Right")
                                            .skins(
                                                    DoorTextures.cycle(
                                                            ResourceManager.pheo_airlock_door_tex,
                                                            ResourceManager
                                                                    .pheo_airlock_door_clean_tex,
                                                            ResourceManager
                                                                    .pheo_airlock_door_green_tex))
                                            .offset(0, 0, 0.5F)
                                            .build(kind))
                    .build();

    public static final DoorType SLIDING_BLAST_DOOR =
            DoorType.builder(24)
                    .footprint(3, 0, 0, 0, 3, 3)
                    .row(-2, 0, 0, 4, 5, 1)
                    .bounds(
                            DoorCollisionShapes.DEFAULT
                                    .open(y(3), box(0, 0.5, 0, 1, 1, 1))
                                    .open(y(0), floor(0, 0, 1, 1, 0.08)))
                    .sounds(
                            () ->
                                    DoorSounds.of(
                                                    null,
                                                    ModSounds.DOOR_SLIDING_DOOR_OPENING,
                                                    ModSounds.DOOR_SLIDING_DOOR_OPENED)
                                            .withCloseEnd(ModSounds.DOOR_SLIDING_DOOR_SHUT)
                                            .withSecondLoop(ModSounds.DOOR_SLIDING_DOOR_OPENING))
                    .volume(2)
                    .skins(3)
                    .look(
                            kind ->
                                    DoorRenderDefinition.builder()
                                            .motion(SlidingBlastDoorRenderer.INSTANCE)
                                            .clips(
                                                    DoorAnimations.of(
                                                            () ->
                                                                    new BusAnimation()
                                                                            .addBus(
                                                                                    "LOCK",
                                                                                    new AnimationSequence()
                                                                                            .jump(
                                                                                                    0,
                                                                                                    0,
                                                                                                    0)
                                                                                            .to(
                                                                                                    1,
                                                                                                    0,
                                                                                                    0,
                                                                                                    200))
                                                                            .addBus(
                                                                                    "DOOR",
                                                                                    new AnimationSequence()
                                                                                            .jump(
                                                                                                    0,
                                                                                                    0,
                                                                                                    0)
                                                                                            .to(
                                                                                                    0,
                                                                                                    0,
                                                                                                    0,
                                                                                                    350)
                                                                                            .to(
                                                                                                    0,
                                                                                                    0.05,
                                                                                                    0,
                                                                                                    200)
                                                                                            .to(
                                                                                                    0,
                                                                                                    1,
                                                                                                    0,
                                                                                                    650,
                                                                                                    IType
                                                                                                            .SIN_UP)),
                                                            () ->
                                                                    new BusAnimation()
                                                                            .addBus(
                                                                                    "LOCK",
                                                                                    new AnimationSequence()
                                                                                            .jump(
                                                                                                    1,
                                                                                                    0,
                                                                                                    0)
                                                                                            .to(
                                                                                                    1,
                                                                                                    0,
                                                                                                    0,
                                                                                                    1000)
                                                                                            .to(
                                                                                                    0,
                                                                                                    0,
                                                                                                    0,
                                                                                                    200))
                                                                            .addBus(
                                                                                    "DOOR",
                                                                                    new AnimationSequence()
                                                                                            .jump(
                                                                                                    0,
                                                                                                    1,
                                                                                                    0)
                                                                                            .to(
                                                                                                    0,
                                                                                                    0.05,
                                                                                                    0,
                                                                                                    650,
                                                                                                    IType
                                                                                                            .SIN_UP)
                                                                                            .to(
                                                                                                    0,
                                                                                                    0,
                                                                                                    0,
                                                                                                    200))))
                                            .model(() -> ResourceManager.pheo_blast_door)
                                            .staticParts("Frame")
                                            .movingParts(
                                                    "LeftDoor",
                                                    "RightLock",
                                                    "RightDoor",
                                                    "LeftLock")
                                            .skins(
                                                    DoorTextures.fixed(
                                                            ResourceManager.pheo_blast_door_tex))
                                            .build(kind))
                    .build();

    public static final DoorType LARGE_VEHICLE_DOOR =
            DoorType.builder(60)
                    .footprint(5, 0, 0, 0, 3, 3)
                    .row(0, 0, 0, -4, 6, 2)
                    .row(0, 0, 0, 4, 6, 2)
                    .bounds(
                            DoorCollisionShapes.DEFAULT
                                    .open(z(3), box(0.4, 0, 0, 1, 1, 1))
                                    .open(z(-3), box(0, 0, 0, 0.6, 1, 1))
                                    .open(y(0), floor(0, 0, 1, 1, 0.0625)))
                    .sounds(
                            () ->
                                    DoorSounds.of(
                                            null,
                                            ModSounds.DOOR_GARAGE_MOVE,
                                            ModSounds.DOOR_GARAGE_STOP))
                    .volume(2)
                    .look(
                            kind ->
                                    DoorRenderDefinition.builder()
                                            .motion(VehicleDoorRenderer.INSTANCE)
                                            .clips(DoorAnimations.linear(kind.getOpeningTicks()))
                                            .model(() -> ResourceManager.pheo_vehicle_door)
                                            .staticParts("Frame")
                                            .movingParts("Left", "Right")
                                            .skins(
                                                    DoorTextures.fixed(
                                                            ResourceManager.pheo_vehicle_door_tex))
                                            .yaw(90)
                                            .build(kind))
                    .build();

    public static final DoorType TRANSITION_SEAL =
            DoorType.builder(480)
                    .footprint(23, 0, 0, 0, 13, 12)
                    .row(-9, 2, 0, 20, 20, 1)
                    .sounds(() -> DoorSounds.of(ModSounds.DOOR_TRANSITION_SEAL_OPEN, null, null))
                    .volume(6)
                    .look(
                            kind ->
                                    DoorRenderDefinition.builder()
                                            .motion(TransitionSealRenderer.INSTANCE)
                                            .skins(
                                                    DoorTextures.fixed(
                                                            ResourceManager.transition_seal_tex))
                                            .build(kind))
                    .build();

    private DoorTypes() {}
}
