// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.weapon.sedna;

import com.hbm.interfaces.injected.GunTickState;
import com.hbm.items.ModDataComponents;
import com.hbm.items.weapon.sedna.impl.ItemGunNI4NI;
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
        var tag = ItemGunBaseNT.getData(stack).tag().copy();
        for (int i = 0; i < timers.configurations(); i++) {
            tag.putInt(ItemGunBaseNT.KEY_TIMER + i, ItemGunBaseNT.getTimer(stack, i));
            tag.putInt(ItemGunBaseNT.KEY_ANIMTIMER + i, timers.animTimer(i));
        }
        if (stack.getItem() instanceof ItemGunNI4NI)
            tag.putInt(ItemGunNI4NI.KEY_COIN_CHARGE, timers.coinCharge);
        copy.set(ModDataComponents.GUN_STATE.get(), new GunStateData(tag));
        return copy;
    }

    private int configurations() {
        return extra == null ? 1 : 1 + extra.length / 2;
    }

    public static @Nullable GunTimers of(ItemStack stack) {
        return ((GunTickState) (Object) stack).hbm$gunTimers();
    }

    public static GunTimers entry(ItemStack stack) {
        GunTimers open = of(stack);
        if (open != null) return open;

        ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
        open = new GunTimers(gun.getConfigCount());
        for (int i = 0; i < open.configurations(); i++) {
            open.setAnimTimer(i, ItemGunBaseNT.getValueInt(stack, ItemGunBaseNT.KEY_ANIMTIMER + i));
        }
        if (gun instanceof ItemGunNI4NI)
            open.coinCharge = ItemGunBaseNT.getValueInt(stack, ItemGunNI4NI.KEY_COIN_CHARGE);
        ((GunTickState) (Object) stack).hbm$setGunTimers(open);
        return open;
    }

    public static GunTimers open(ItemStack stack) {
        GunTimers open = entry(stack);
        if (open.held) return open;

        for (int i = 0; i < open.configurations(); i++) {
            open.setTimer(i, ItemGunBaseNT.getValueInt(stack, ItemGunBaseNT.KEY_TIMER + i));
        }
        open.held = true;
        return open;
    }

    public static void close(ItemStack stack) {
        GunTimers open = of(stack);
        if (open == null || !open.held) return;

        open.held = false;
        for (int i = 0; i < open.configurations(); i++) {
            String key = ItemGunBaseNT.KEY_TIMER + i;
            if (ItemGunBaseNT.getValueInt(stack, key) != open.timer(i)) {
                ItemGunBaseNT.setValueInt(stack, key, open.timer(i));
            }
        }
    }

    public boolean owns() {
        return held;
    }

    int timer(int index) {
        return index == 0 ? timer : extra[(index - 1) * 2];
    }

    void setTimer(int index, int value) {
        if (index == 0) timer = value;
        else extra[(index - 1) * 2] = value;
    }

    int animTimer(int index) {
        return index == 0 ? animTimer : extra[(index - 1) * 2 + 1];
    }

    void setAnimTimer(int index, int value) {
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
