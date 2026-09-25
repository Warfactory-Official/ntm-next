// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.entity.item;

import com.hbm.api.block.IDroneContainer;
import com.hbm.entity.ModEntities;
import com.hbm.interfaces.StoredItems;
import com.hbm.items.ModItems;
import com.hbm.items.tool.ItemDrone.EnumDroneType;
import com.hbm.items.tool.ItemDrone;
import com.hbm.sound.ModSounds;
import com.hbm.tileentity.network.RequestNetwork.Demand;
import com.hbm.util.InventoryUtil;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class EntityRequestDrone extends EntityDroneBase implements StoredItems {

    private static final double NODE_REACH = 4;
    private static final int ACTION_DELAY = 5;

    public ItemStack heldItem = ItemStack.EMPTY;

    @Override
    public void visitStoredItems(Visitor visitor) {
        if (visitor.visit(heldItem) && heldItem.isEmpty()) heldItem = ItemStack.EMPTY;
    }

    public final List<Step> program = new ArrayList<>();
    private int nextActionTimer;

    public EntityRequestDrone(EntityType<? extends EntityRequestDrone> type, Level level) {
        super(type, level);
    }

    public EntityRequestDrone(Level level) {
        super(ModEntities.REQUEST_DRONE.get(), level);
    }

    @Override
    public void setTarget(double x, double y, double z) {
        super.setTarget(x, y + 1, z);
    }

    @Override
    public double getSpeed() {
        return 0.625D;
    }

    @Override
    public boolean skipAttackInteraction(Entity attacker) {
        if (isRemoved()
                || !(attacker instanceof Player)
                || !(level() instanceof ServerLevel server)) {
            return false;
        }

        discard();
        if (!this.heldItem.isEmpty()) spawnAtLocation(server, this.heldItem);
        spawnAtLocation(server, ModItems.DRONE.stack(EnumDroneType.REQUEST));
        return false;
    }

    @Override
    public void tick() {
        super.tick();

        if (!(level() instanceof ServerLevel server)) return;
        if (getDeltaMovement().length() >= 0.01) return;

        if (this.nextActionTimer > 0) {
            this.nextActionTimer--;
            return;
        }

        if (this.program.isEmpty()) {
            selfDestruct(server);
            return;
        }

        switch (this.program.removeFirst()) {
            case Step.Goto(BlockPos pos) ->
                    setTarget(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
            case Step.Load(Demand want) -> {
                if (this.heldItem.isEmpty()) load(server, want);
                this.nextActionTimer = ACTION_DELAY;
            }
            case Step.Unload ignored -> {
                if (!this.heldItem.isEmpty()) unload(server);
                this.nextActionTimer = ACTION_DELAY;
            }
            case Step.Dock ignored -> dock(server);
        }
    }

    private void load(ServerLevel server, Demand want) {
        if (!(nodeBelow(server) instanceof IDroneContainer.Provider provider)) return;

        Container inventory = provider.droneOfferInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.isEmpty() || !want.test(stack)) continue;

            this.heldItem = stack.copy();
            setAppearance(1);
            playUnpack(server);
            inventory.setItem(i, ItemStack.EMPTY);
            inventory.setChanged();
            return;
        }
    }

    private void unload(ServerLevel server) {
        if (!(nodeBelow(server) instanceof IDroneContainer.Requester requester)) return;

        Container inventory = requester.droneDeliveryInventory();

        this.heldItem =
                InventoryUtil.tryAddItemToInventory(
                        inventory, 0, inventory.getContainerSize() - 1, this.heldItem);

        if (this.heldItem.isEmpty()) {
            setAppearance(0);
            playUnpack(server);
        }

        inventory.setChanged();
    }

    private void dock(ServerLevel server) {
        BlockEntity node = nodeBelow(server);

        if (node instanceof IDroneContainer.Dock dock) {
            Container inventory = dock.droneDockInventory();
            ItemStack drone = ModItems.DRONE.stack(EnumDroneType.REQUEST);

            for (int i = 0; i < inventory.getContainerSize(); i++) {
                ItemStack slot = inventory.getItem(i);
                boolean free = slot.isEmpty();
                boolean stackable =
                        ItemStack.isSameItemSameComponents(slot, drone)
                                && slot.getCount() < slot.getMaxStackSize();
                if (!free && !stackable) continue;

                discard();

                if (!this.heldItem.isEmpty()
                        && i + 1 < inventory.getContainerSize()
                        && inventory.getItem(i + 1).isEmpty()) {
                    inventory.setItem(i + 1, this.heldItem.copy());
                    this.heldItem = ItemStack.EMPTY;
                }

                if (!this.heldItem.isEmpty()) {
                    spawnAtLocation(server, this.heldItem);
                    this.heldItem = ItemStack.EMPTY;
                }

                if (free) inventory.setItem(i, drone);
                else slot.grow(1);

                inventory.setChanged();

                server.playSound(
                        null,
                        node.getBlockPos(),
                        ModSounds.CRATE_CLOSE.get(),
                        SoundSource.BLOCKS,
                        2.0F,
                        1.0F);
                break;
            }
        }

        if (!isRemoved()) selfDestruct(server);
    }

    private void selfDestruct(ServerLevel server) {
        discard();
        if (!this.heldItem.isEmpty()) spawnAtLocation(server, this.heldItem);
        spawnAtLocation(server, ModItems.DRONE.stack(EnumDroneType.REQUEST));
    }

    private @Nullable BlockEntity nodeBelow(ServerLevel server) {
        Vec3 from = position();
        Vec3 to = from.subtract(0, NODE_REACH, 0);
        BlockHitResult hit =
                server.clip(
                        new ClipContext(
                                from,
                                to,
                                ClipContext.Block.COLLIDER,
                                ClipContext.Fluid.NONE,
                                this));

        if (hit.getType() != HitResult.Type.BLOCK) return null;
        return server.getBlockEntity(hit.getBlockPos());
    }

    private void playUnpack(ServerLevel server) {
        server.playSound(
                null,
                getX(),
                getY(),
                getZ(),
                ModSounds.ITEM_UNPACK.get(),
                SoundSource.NEUTRAL,
                0.5F,
                0.75F);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.heldItem = input.read("held", ItemStack.CODEC).orElse(ItemStack.EMPTY);
        this.program.clear();
        this.program.addAll(input.read("program", Step.CODEC.listOf()).orElse(List.of()));
        this.nextActionTimer = ACTION_DELAY;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        if (!this.heldItem.isEmpty()) output.store("held", ItemStack.CODEC, this.heldItem);
        output.store("program", Step.CODEC.listOf(), List.copyOf(this.program));
    }

    public sealed interface Step {

        Codec<Step> CODEC = Codec.STRING.dispatch("type", Step::type, Step::codecFor);

        static MapCodec<? extends Step> codecFor(String type) {
            return switch (type) {
                case "pos" -> Goto.CODEC;
                case "want" -> Load.CODEC;
                case "dock" -> MapCodec.unit(new Dock());
                default -> MapCodec.unit(new Unload());
            };
        }

        String type();

        record Goto(BlockPos pos) implements Step {
            static final MapCodec<Goto> CODEC =
                    RecordCodecBuilder.mapCodec(
                            i ->
                                    i.group(BlockPos.CODEC.fieldOf("pos").forGetter(Goto::pos))
                                            .apply(i, Goto::new));

            @Override
            public String type() {
                return "pos";
            }
        }

        record Load(Demand want) implements Step {
            static final MapCodec<Load> CODEC =
                    RecordCodecBuilder.mapCodec(
                            i ->
                                    i.group(Demand.CODEC.fieldOf("want").forGetter(Load::want))
                                            .apply(i, Load::new));

            @Override
            public String type() {
                return "want";
            }
        }

        record Unload() implements Step {
            @Override
            public String type() {
                return "unload";
            }
        }

        record Dock() implements Step {
            @Override
            public String type() {
                return "dock";
            }
        }
    }
}
