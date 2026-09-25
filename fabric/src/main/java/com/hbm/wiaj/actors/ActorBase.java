// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.wiaj.actors;

import net.minecraft.nbt.CompoundTag;

public abstract class ActorBase implements ISpecialActor {
    protected CompoundTag data = new CompoundTag();

    @Override
    public void setActorData(CompoundTag data) {
        this.data = data;
    }

    @Override
    public void setDataPoint(String tag, Object value) {
        if (value instanceof String v) data.putString(tag, v);
        if (value instanceof Integer v) data.putInt(tag, v);
        if (value instanceof Float v) data.putFloat(tag, v);
        if (value instanceof Double v) data.putDouble(tag, v);
        if (value instanceof Boolean v) data.putBoolean(tag, v);
        if (value instanceof Byte v) data.putByte(tag, v);
        if (value instanceof Short v) data.putShort(tag, v);
    }
}
