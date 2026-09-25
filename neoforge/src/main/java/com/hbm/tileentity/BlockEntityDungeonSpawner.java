// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity;

import com.hbm.blocks.ModBlockEntities;
import com.hbm.entity.ModEntities;
import com.hbm.entity.mob.EntityUndeadSoldier;
import com.hbm.items.EnumSecretType;
import com.hbm.items.ModItems;
import com.hbm.platform.Services;
import com.hbm.util.TickPhase;
import java.util.function.Consumer;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;

public final class BlockEntityDungeonSpawner extends BlockEntity {

    private int phase;
    private int timer;
    private SpawnerType type = SpawnerType.ABERRATOR;

    public BlockEntityDungeonSpawner(BlockPos pos, BlockState state) {
        super(ModBlockEntities.NTM_DUNGEON_SPAWNER.get(), pos, state);
    }

    public static void tick(
            Level level, BlockPos pos, BlockState state, BlockEntityDungeonSpawner be) {
        be.serverTick((ServerLevel) level);
    }

    private static boolean canAdvanceAberrator(BlockEntityDungeonSpawner spawner) {
        ServerLevel level = (ServerLevel) spawner.getLevel();
        BlockPos pos = spawner.getBlockPos();
        if (level.getDifficulty().getId() == 0) return false;
        if (spawner.phase == 0) {
            return TickPhase.every(spawner, 20)
                    && !level.getEntitiesOfClass(Player.class, legacyPlayerBox(pos, 20, 10))
                            .isEmpty();
        }
        if (spawner.phase < 3) {
            return TickPhase.every(spawner, 20)
                    && spawner.timer >= 60
                    && level.getEntitiesOfClass(EntityUndeadSoldier.class, legacySoldierBox(pos))
                            .isEmpty();
        }
        return false;
    }

    private static void runAberrator(BlockEntityDungeonSpawner spawner) {
        ServerLevel level = (ServerLevel) spawner.getLevel();
        BlockPos pos = spawner.getBlockPos();
        if ((spawner.phase == 1 || spawner.phase == 2) && spawner.timer == 0) {
            double x = 10D;
            double z = 0D;
            double cos = Math.cos(Math.PI / 5D);
            double sin = Math.sin(Math.PI / 5D);
            for (int i = 0; i < 10; i++) {
                EntityUndeadSoldier soldier =
                        ModEntities.UNDEAD_SOLDIER.get().create(level, EntitySpawnReason.EVENT);
                if (soldier == null)
                    throw new IllegalStateException("Could not construct ABERRATOR soldier");
                for (int retry = 0; retry < 7; retry++) {
                    soldier.snapTo(
                            pos.getX() + 0.5D + x,
                            pos.getY() - 5D,
                            pos.getZ() + 0.5D + z,
                            i * 36F,
                            0F);
                    if (soldier.canSpawnAtCurrentPosition()) {
                        Services.PLATFORM.finalizeSpawn(
                                soldier,
                                level,
                                level.getCurrentDifficultyAt(soldier.blockPosition()),
                                EntitySpawnReason.EVENT,
                                null);
                        level.addFreshEntity(soldier);
                        break;
                    }
                }
                double previousX = x;
                x = x * cos + z * sin;
                z = z * cos - previousX * sin;
            }
        }
        if (spawner.phase > 2) {
            BlockPos holderPos = pos.above(18);
            if (level.getBlockEntity(holderPos) instanceof BlockEntitySkeletonHolder holder) {
                ItemStack reward =
                        level.getRandom().nextInt(5) == 0
                                ? ModItems.ITEM_SECRET.stack(EnumSecretType.ABERRATOR)
                                : new ItemStack(ModItems.CLAY_TABLET_1);
                holder.item = reward;
                holder.setChanged();
                BlockState state = level.getBlockState(holderPos);
                level.sendBlockUpdated(holderPos, state, state, Block.UPDATE_CLIENTS);
            }
            level.setBlockAndUpdate(pos, Blocks.OBSIDIAN.defaultBlockState());
        }
    }

    private static AABB legacyPlayerBox(BlockPos pos, int radius, int vertical) {
        return new AABB(
                pos.getX() - radius,
                pos.getY() - vertical,
                pos.getZ() - radius,
                pos.getX() + 1 + radius,
                pos.getY() - 2 + vertical,
                pos.getZ() + 1 + radius);
    }

    private static AABB legacySoldierBox(BlockPos pos) {
        return new AABB(
                pos.getX() - 50,
                pos.getY() - 20,
                pos.getZ() - 50,
                pos.getX() + 48,
                pos.getY() + 21,
                pos.getZ() + 51);
    }

    private void serverTick(ServerLevel level) {
        type.phase.accept(this);
        if (type.phaseCondition.test(this)) {
            phase++;
            timer = 0;
        } else {
            timer++;
        }
        setChanged();
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("phase", phase);
        output.putInt("timer", timer);
        output.putByte("type", (byte) type.ordinal());
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        phase = input.getIntOr("phase", 0);
        timer = input.getIntOr("timer", 0);
        type = SpawnerType.byOrdinal(input.getByteOr("type", (byte) 0));
    }

    private enum SpawnerType {
        ABERRATOR(
                BlockEntityDungeonSpawner::canAdvanceAberrator,
                BlockEntityDungeonSpawner::runAberrator);

        private final Predicate<BlockEntityDungeonSpawner> phaseCondition;
        private final Consumer<BlockEntityDungeonSpawner> phase;

        SpawnerType(
                Predicate<BlockEntityDungeonSpawner> phaseCondition,
                Consumer<BlockEntityDungeonSpawner> phase) {
            this.phaseCondition = phaseCondition;
            this.phase = phase;
        }

        private static SpawnerType byOrdinal(int ordinal) {
            return ABERRATOR;
        }
    }
}
