// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.packet.toclient;

import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.lib.Library;
import com.hbm.packet.IPayloadHandlerContext;
import com.hbm.packet.threading.ThreadedPayload;
import com.hbm.render.item.weapon.sedna.ItemRenderWeaponBase;
import com.hbm.util.GameTime;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

public final class MuzzleFlashPayload extends ThreadedPayload {
    public static final Type<MuzzleFlashPayload> TYPE = new Type<>(Library.id("muzzle_flash"));
    public static final StreamCodec<ByteBuf, MuzzleFlashPayload> STREAM_CODEC =
            streamCodec(MuzzleFlashPayload::decode);

    private final int entityId;

    public MuzzleFlashPayload(LivingEntity entity) {
        this(entity.getId());
    }

    private MuzzleFlashPayload(int entityId) {
        this.entityId = entityId;
    }

    private static MuzzleFlashPayload decode(ByteBuf buf) {
        return new MuzzleFlashPayload(ByteBufCodecs.VAR_INT.decode(buf));
    }

    public static void handleClient(MuzzleFlashPayload payload, IPayloadHandlerContext context) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return;
        Entity entity = minecraft.level.getEntity(payload.entityId);
        if (!(entity instanceof LivingEntity living)
                || living == minecraft.player
                || !(living.getMainHandItem().getItem() instanceof ItemGunBaseNT)) return;
        ItemRenderWeaponBase.flashMap.put(living, GameTime.millis(living.level()));
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufCodecs.VAR_INT.encode(buf, entityId);
    }

    @Override
    public @NotNull Type<MuzzleFlashPayload> type() {
        return TYPE;
    }
}
