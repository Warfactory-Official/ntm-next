// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.special;

import com.hbm.blocks.rail.BlockRailNTM;
import com.hbm.blocks.rail.IRailNTM.RailCheckType;
import com.hbm.entity.ModEntities;
import com.hbm.entity.train.EntityRailCarBase;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

public class ItemTrain extends Item {

    private static final Component STANDARD_GAUGE =
            Component.translatable("desc.item.train.standardGauge");
    private static final Component YES = Component.translatable("desc.item.train.yes");

    public final EnumTrainType type;

    public ItemTrain(Properties props, EnumTrainType type) {
        super(props);
        this.type = type;
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> tooltip,
            TooltipFlag flag) {
        EnumTrainType train = type;

        if (train.engine != null)
            tooltip.accept(Component.translatable("desc.item.train.engine", train.engine));
        tooltip.accept(Component.translatable("desc.item.train.gauge", train.gauge));
        if (train.maxSpeed != null)
            tooltip.accept(Component.translatable("desc.item.train.maxSpeed", train.maxSpeed));
        if (train.acceleration != null) {
            tooltip.accept(
                    Component.translatable("desc.item.train.acceleration", train.acceleration));
        }
        if (train.brakeThreshold != null) {
            tooltip.accept(
                    Component.translatable(
                            "desc.item.train.engineBrakeThreshold", train.brakeThreshold));
        }
        if (train.parkingBrake != null) {
            tooltip.accept(
                    Component.translatable("desc.item.train.parkingBrake", train.parkingBrake));
        }
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {

        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockRailNTM.Owner owner = new BlockRailNTM.Owner();

        if (!BlockRailNTM.railAt(level, pos, owner)) return InteractionResult.PASS;

        ItemStack stack = context.getItemInHand();
        EntityRailCarBase train = type.train.get().create(level, EntitySpawnReason.SPAWN_ITEM_USE);
        if (train == null) return InteractionResult.PASS;

        if (train.getGauge() != owner.rail.getGauge(level, pos.getX(), pos.getY(), pos.getZ())) {
            return InteractionResult.PASS;
        }

        train.setPos(context.getClickLocation());
        train.setYRot(context.getPlayer() != null ? context.getPlayer().getYRot() : 0F);

        BlockPos anchor = train.getCurrentAnchorPos();
        if (!train.walkRail(
                anchor.getX(), anchor.getY(), anchor.getZ(), 0, RailCheckType.CORE, 0)) {
            return InteractionResult.PASS;
        }
        train.setPos(train.railResult().x, train.railResult().y, train.railResult().z);

        if (!train.walkRail(
                anchor.getX(),
                anchor.getY(),
                anchor.getZ(),
                train.getLengthSpan(),
                RailCheckType.FRONT,
                train.getCollisionSpan() - train.getLengthSpan())) {
            return InteractionResult.PASS;
        }
        double frontX = train.railResult().x, frontZ = train.railResult().z;

        if (!train.walkRail(
                anchor.getX(),
                anchor.getY(),
                anchor.getZ(),
                -train.getLengthSpan(),
                RailCheckType.BACK,
                train.getCollisionSpan() - train.getLengthSpan())) {
            return InteractionResult.PASS;
        }

        if (!level.isClientSide()) {
            train.setYRot(
                    EntityRailCarBase.generateYaw(
                            frontX, frontZ, train.railResult().x, train.railResult().z));
            level.addFreshEntity(train);
        }
        stack.shrink(1);

        return InteractionResult.SUCCESS;
    }

    public enum EnumTrainType {
        CARGO_TRAM(
                () -> ModEntities.CARGO_TRAM.get(),
                Component.translatable("desc.item.train.electric"),
                STANDARD_GAUGE,
                Component.translatable("desc.item.train.metresPerSecond", 10),
                Component.translatable("desc.item.train.metresPerSecondSquared", 0.2D),
                Component.translatable("desc.item.train.belowMetresPerSecond", 1),
                YES),
        CARGO_TRAM_TRAILER(
                () -> ModEntities.CARGO_TRAM_TRAILER.get(),
                null,
                STANDARD_GAUGE,
                YES,
                null,
                null,
                Component.translatable("desc.item.train.no"));

        public static final EnumTrainType[] VALUES = values();

        public final Supplier<EntityType<? extends EntityRailCarBase>> train;
        public final @Nullable Component engine;
        public final @Nullable Component maxSpeed;
        public final @Nullable Component acceleration;
        public final @Nullable Component brakeThreshold;
        public final @Nullable Component parkingBrake;
        public final Component gauge;
        public final String id = name().toLowerCase(Locale.ROOT);

        EnumTrainType(
                Supplier<EntityType<? extends EntityRailCarBase>> train,
                @Nullable Component engine,
                Component gauge,
                @Nullable Component maxSpeed,
                @Nullable Component acceleration,
                @Nullable Component brakeThreshold,
                @Nullable Component parkingBrake) {
            this.train = train;
            this.engine = engine;
            this.maxSpeed = maxSpeed;
            this.acceleration = acceleration;
            this.brakeThreshold = brakeThreshold;
            this.parkingBrake = parkingBrake;
            this.gauge = gauge;
        }
    }
}
