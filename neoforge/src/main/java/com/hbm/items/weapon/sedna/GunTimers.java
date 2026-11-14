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

package com.hbm.items.weapon.sedna;

import com.hbm.items.ModDataComponents;
import com.hbm.items.weapon.sedna.GunBaseNTItem;
import com.hbm.items.weapon.sedna.impl.GunNI4NIItem;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public final class GunTimers {

    private int timer;
    private int animTimer;
    private final @Nullable int[] extra;
    private int coinCharge;
    private boolean held;

    private GunTimers(int configs) {
        extra = configs > 1 ? new int[(configs - 1) * 2] : null;
    }

    private GunTimers(GunTimers source) {
        timer = source.timer;
        animTimer = source.animTimer;
        extra = source.extra == null ? null : source.extra.clone();
        coinCharge = source.coinCharge;
        held = source.held;
    }

    public GunTimers copy() {
        return new GunTimers(this);
    }

    public static ItemStack forSave(ItemStack stack) {
        GunTimers timers = of(stack);
        if (timers == null) return stack;
        ItemStack copy = stack.copy();
        var tag = GunBaseNTItem.getData(stack).tag().copy();
        for (int i = 0; i < timers.configurations(); i++) {
            tag.putInt(GunBaseNTItem.KEY_TIMER + i, GunBaseNTItem.getTimer(stack, i));
            tag.putInt(GunBaseNTItem.KEY_ANIMTIMER + i, timers.animTimer(i));
        }
        if (stack.getItem() instanceof GunNI4NIItem)
            tag.putInt(GunNI4NIItem.KEY_COIN_CHARGE, timers.coinCharge);
        copy.set(ModDataComponents.GUN_STATE.get(), new GunStateData(tag));
        return copy;
    }

    private int configurations() {
        return extra == null ? 1 : 1 + extra.length / 2;
    }

    public static @Nullable GunTimers of(ItemStack stack) {
        return ((com.hbm.interfaces.injected.GunTickState) (java.lang.Object) (stack))
                .hbm$gunTimers();
    }

    public static GunTimers entry(ItemStack stack) {
        GunTimers open = of(stack);
        if (open != null) return open;

        GunBaseNTItem gun = (GunBaseNTItem) stack.getItem();
        open = new GunTimers(gun.getConfigCount());
        for (int i = 0; i < open.configurations(); i++) {
            open.setAnimTimer(i, GunBaseNTItem.getValueInt(stack, GunBaseNTItem.KEY_ANIMTIMER + i));
        }
        if (gun instanceof GunNI4NIItem)
            open.coinCharge = GunBaseNTItem.getValueInt(stack, GunNI4NIItem.KEY_COIN_CHARGE);
        ((com.hbm.interfaces.injected.GunTickState) (java.lang.Object) (stack))
                .hbm$setGunTimers(open);
        return open;
    }

    public static GunTimers open(ItemStack stack) {
        GunTimers open = entry(stack);
        if (open.held) return open;

        for (int i = 0; i < open.configurations(); i++) {
            open.setTimer(i, GunBaseNTItem.getValueInt(stack, GunBaseNTItem.KEY_TIMER + i));
        }
        open.held = true;
        return open;
    }

    public static void close(ItemStack stack) {
        GunTimers open = of(stack);
        if (open == null || !open.held) return;

        open.held = false;
        for (int i = 0; i < open.configurations(); i++) {
            String key = GunBaseNTItem.KEY_TIMER + i;
            if (GunBaseNTItem.getValueInt(stack, key) != open.timer(i)) {
                GunBaseNTItem.setValueInt(stack, key, open.timer(i));
            }
        }
    }

    public boolean owns() {
        return held;
    }

    public int timer(int index) {
        return index == 0 ? timer : extra[(index - 1) * 2];
    }

    public void setTimer(int index, int value) {
        if (index == 0) timer = value;
        else extra[(index - 1) * 2] = value;
    }

    public int animTimer(int index) {
        return index == 0 ? animTimer : extra[(index - 1) * 2 + 1];
    }

    public void setAnimTimer(int index, int value) {
        if (index == 0) animTimer = value;
        else extra[(index - 1) * 2 + 1] = value;
    }

    public int coinCharge() {
        return coinCharge;
    }

    public void setCoinCharge(int value) {
        coinCharge = value;
    }
}
