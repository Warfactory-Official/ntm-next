// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.packet.toserver;

import com.hbm.entity.missile.EntityBobmazon;
import com.hbm.handler.BobmazonOffer;
import com.hbm.handler.BobmazonOffers;
import com.hbm.items.ModItems;
import com.hbm.lib.Library;
import com.hbm.lib.ModDamageTypes;
import com.hbm.packet.IPayloadHandlerContext;
import com.hbm.packet.threading.ThreadedPayload;
import com.hbm.util.DropHeight;
import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public final class BobmazonPurchasePayload extends ThreadedPayload {

    public static final Type<BobmazonPurchasePayload> TYPE =
            new Type<>(Library.id("bobmazon_purchase"));
    public static final StreamCodec<ByteBuf, BobmazonPurchasePayload> STREAM_CODEC =
            streamCodec(BobmazonPurchasePayload::decode);

    private final int offer;

    public BobmazonPurchasePayload(int offer) {
        this.offer = offer;
    }

    private static BobmazonPurchasePayload decode(ByteBuf buf) {
        return new BobmazonPurchasePayload(ByteBufCodecs.INT.decode(buf));
    }

    public static void handleServer(BobmazonPurchasePayload payload, IPayloadHandlerContext ctx) {
        if (!(ctx.player() instanceof ServerPlayer player)) return;
        if (!(player.level() instanceof ServerLevel level)) return;

        List<BobmazonOffer> catalog = BobmazonOffers.forStack(player.getMainHandItem());

        if (catalog == null) {

            player.sendSystemMessage(
                    Component.translatable("chat.bobmazonPurchase.thereAppearsToBe"));
            player.sendSystemMessage(
                    Component.translatable("chat.bobmazonPurchase.engagingFailSafe"));
            player.hurtServer(
                    level, level.damageSources().source(ModDamageTypes.NUCLEAR_BLAST), 1000);
            player.setDeltaMovement(player.getDeltaMovement().x, 2.0D, player.getDeltaMovement().z);
            player.hurtMarked = true;
            return;
        }

        if (payload.offer < 0 || payload.offer >= catalog.size()) return;

        BobmazonOffer offer = catalog.get(payload.offer);
        boolean creative = player.getAbilities().instabuild;

        if (!offer.requirement().fulfills(player) && !creative) {
            player.sendSystemMessage(
                    Component.translatable("chat.bobmazonPurchase.achievementRequirementNot"));
            return;
        }

        if (countCaps(player) < offer.cost() && !creative) {
            player.sendSystemMessage(Component.translatable("chat.bobmazonPurchase.notEnoughCaps"));
            return;
        }

        payCaps(player, offer.cost());
        player.containerMenu.broadcastChanges();

        RandomSource random = level.getRandom();
        EntityBobmazon bob = new EntityBobmazon(level);
        double dropX = player.getX() + random.nextGaussian() * 10;
        double dropZ = player.getZ() + random.nextGaussian() * 10;
        bob.setPos(dropX, DropHeight.clearOfTerrain(level, 300, dropX, dropZ), dropZ);
        bob.payload = offer.offer().copy();

        level.addFreshEntity(bob);
    }

    private static boolean isCap(Item item) {
        return item == ModItems.CAP_FRITZ.get()
                || item == ModItems.CAP_KORL.get()
                || item == ModItems.CAP_NUKA.get()
                || item == ModItems.CAP_QUANTUM.get()
                || item == ModItems.CAP_RAD.get()
                || item == ModItems.CAP_SPARKLE.get();
    }

    private static int countCaps(ServerPlayer player) {
        Inventory inventory = player.getInventory();
        int count = 0;
        for (int i = 0; i < Inventory.INVENTORY_SIZE; i++) {
            ItemStack stack = inventory.getItem(i);
            if (isCap(stack.getItem())) count += stack.getCount();
        }
        return count;
    }

    private static void payCaps(ServerPlayer player, int price) {
        if (price == 0) return;

        Inventory inventory = player.getInventory();
        for (int i = 0; i < Inventory.INVENTORY_SIZE && price > 0; i++) {
            ItemStack stack = inventory.getItem(i);
            if (!isCap(stack.getItem())) continue;

            int taken = Math.min(price, stack.getCount());
            inventory.removeItem(i, taken);
            price -= taken;
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufCodecs.INT.encode(buf, offer);
    }

    @Override
    public @NotNull Type<BobmazonPurchasePayload> type() {
        return TYPE;
    }
}
