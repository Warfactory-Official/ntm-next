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

package com.hbm.compat.computercraft.machine;

import com.hbm.api.entity.RadarEntry;
import com.hbm.api.entity.RadarTargetType;
import com.hbm.blockentity.machine.RadarBlockEntity;
import com.hbm.compat.computercraft.SnapshotPeripheral;
import dan200.computercraft.api.lua.LuaFunction;
import java.util.List;

public final class RadarPeripheral extends SnapshotPeripheral<RadarBlockEntity> {

    private static final int MISSILES = 0;
    private static final int SHELLS = 1;
    private static final int PLAYERS = 2;
    private static final int SMART = 3;
    private static final int RANGE = 4;
    private static final int POWER = 5;
    private static final int MAX_POWER = 6;
    private static final int JAMMED = 7;
    private static final int AMOUNT = 8;
    private static final int X = 9;
    private static final int Y = 10;
    private static final int Z = 11;
    private static final int CONTACTS = 0;
    private static final int TYPE = 0;
    private static final int CONTACT_X = 1;
    private static final int CONTACT_Y = 2;
    private static final int CONTACT_Z = 3;
    private static final int STRIDE = 4;
    private static final Contacts NO_CONTACTS = new Contacts(new int[0], new String[0]);

    private record Contacts(int[] data, String[] names) {}

    public RadarPeripheral(RadarBlockEntity machine) {
        super(machine, "ntm_radar", 12, 1);
        putRef(CONTACTS, NO_CONTACTS);
    }

    @Override
    protected void capture(RadarBlockEntity machine) {
        put(MISSILES, machine.scanMissiles);
        put(SHELLS, machine.scanShells);
        put(PLAYERS, machine.scanPlayers);
        put(SMART, machine.smartMode);
        put(RANGE, machine.getRange());
        put(POWER, machine.getPower());
        put(MAX_POWER, machine.getMaxPower());
        put(JAMMED, machine.jammed);
        put(AMOUNT, machine.entries.size());
        put(X, machine.getBlockPos().getX());
        put(Y, machine.getBlockPos().getY());
        put(Z, machine.getBlockPos().getZ());
        captureContacts(machine.entries);
    }

    private void captureContacts(List<RadarEntry> entries) {
        Contacts previous = (Contacts) refAt(CONTACTS);
        if (sameContacts(entries, previous)) return;

        int size = entries.size();
        if (size == 0) {
            putRef(CONTACTS, NO_CONTACTS);
            return;
        }
        int[] contacts = new int[size * STRIDE];
        String[] names = new String[size];
        for (int i = 0; i < size; i++) {
            RadarEntry entry = entries.get(i);
            int b = i * STRIDE;
            contacts[b + TYPE] = entry.blipLevel;
            contacts[b + CONTACT_X] = entry.posX;
            contacts[b + CONTACT_Y] = entry.posY;
            contacts[b + CONTACT_Z] = entry.posZ;
            names[i] = entry.getRadarName;
        }
        putRef(CONTACTS, new Contacts(contacts, names));
    }

    private static boolean sameContacts(List<RadarEntry> entries, Contacts previous) {
        int[] contacts = previous.data;
        String[] names = previous.names;
        if (entries.size() != names.length) return false;
        for (int i = 0; i < names.length; i++) {
            RadarEntry entry = entries.get(i);
            int b = i * STRIDE;
            if (contacts[b + TYPE] != entry.blipLevel
                    || contacts[b + CONTACT_X] != entry.posX
                    || contacts[b + CONTACT_Y] != entry.posY
                    || contacts[b + CONTACT_Z] != entry.posZ
                    || !names[i].equals(entry.getRadarName)) return false;
        }
        return true;
    }

    @LuaFunction
    public Object[] getSettings() {
        return read(
                s ->
                        new Object[] {
                            s.booleanAt(MISSILES),
                            s.booleanAt(SHELLS),
                            s.booleanAt(PLAYERS),
                            s.booleanAt(SMART)
                        });
    }

    @LuaFunction
    public Object[] getRange() {
        return new Object[] {intAt(RANGE)};
    }

    @LuaFunction(mainThread = true)
    public Object[] setSettings(boolean missiles, boolean shells, boolean players, boolean smart) {
        RadarBlockEntity machine = machine();
        machine.hbm$syncSet$RadarBlockEntity$scanMissiles(missiles);
        machine.hbm$syncSet$RadarBlockEntity$scanShells(shells);
        machine.hbm$syncSet$RadarBlockEntity$scanPlayers(players);
        machine.hbm$syncSet$RadarBlockEntity$smartMode(smart);
        machine.setChanged();
        return new Object[] {};
    }

    @LuaFunction
    public Object[] getEnergyInfo() {
        return read(s -> new Object[] {s.longAt(POWER), s.longAt(MAX_POWER)});
    }

    @LuaFunction
    public Object[] isJammed() {
        return new Object[] {booleanAt(JAMMED)};
    }

    @LuaFunction
    public Object[] getAmount() {
        return new Object[] {intAt(AMOUNT)};
    }

    private static Object[] noEntity() {
        return new Object[] {null, "No entity exists at that index."};
    }

    private static int offset(Contacts contacts, int index) {
        return index <= 0 || index > contacts.names.length ? -1 : (index - 1) * STRIDE;
    }

    @LuaFunction
    public Object[] isIndexPlayer(int index) {
        return read(
                s -> {
                    Contacts contacts = (Contacts) s.refAt(CONTACTS);
                    int b = offset(contacts, index);
                    return b < 0
                            ? noEntity()
                            : new Object[] {contacts.data[b + TYPE] == RadarTargetType.PLAYER.id()};
                });
    }

    @LuaFunction
    public Object[] getIndexType(int index) {
        return read(
                s -> {
                    Contacts contacts = (Contacts) s.refAt(CONTACTS);
                    int b = offset(contacts, index);
                    return b < 0 ? noEntity() : new Object[] {contacts.data[b + TYPE]};
                });
    }

    @LuaFunction
    public Object[] getEntityAtIndex(int index) {
        return read(
                s -> {
                    Contacts view = (Contacts) s.refAt(CONTACTS);
                    int b = offset(view, index);
                    if (b < 0) return noEntity();
                    int[] contacts = view.data;
                    int type = contacts[b + TYPE];
                    if (type == RadarTargetType.PLAYER.id()) {
                        return new Object[] {
                            true,
                            contacts[b + CONTACT_X],
                            contacts[b + CONTACT_Y],
                            contacts[b + CONTACT_Z],
                            type,
                            view.names[index - 1]
                        };
                    }
                    return new Object[] {
                        false,
                        contacts[b + CONTACT_X],
                        contacts[b + CONTACT_Y],
                        contacts[b + CONTACT_Z],
                        type
                    };
                });
    }

    @LuaFunction
    public Object[] getPos() {
        return read(s -> new Object[] {s.intAt(X), s.intAt(Y), s.intAt(Z)});
    }
}
