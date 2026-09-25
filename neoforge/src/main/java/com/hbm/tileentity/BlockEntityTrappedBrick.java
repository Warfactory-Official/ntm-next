// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity;

import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.generic.TrappedBrick.Trap;
import com.hbm.blocks.generic.TrappedBrick;
import com.hbm.entity.projectile.EntityRubble;
import com.hbm.items.ModItems;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.spider.CaveSpider;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public class BlockEntityTrappedBrick extends BlockEntity {

    private AABB detector;
    private Direction dir = null;

    public BlockEntityTrappedBrick(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TRAPPED_BRICK.get(), pos, state);
    }

    public static void tick(
            Level level, BlockPos pos, BlockState state, BlockEntityTrappedBrick be) {
        be.serverTick((ServerLevel) level, pos, state);
    }

    private void serverTick(ServerLevel level, BlockPos pos, BlockState state) {
        if (!(state.getBlock() instanceof TrappedBrick trapBlock)) return;

        if (detector == null) setDetector(level, pos, trapBlock.trap);
        if (!level.getEntitiesOfClass(Player.class, detector).isEmpty())
            trigger(level, pos, trapBlock.trap);
    }

    private void trigger(ServerLevel level, BlockPos pos, Trap trap) {
        double cx = pos.getX() + 0.5D, cz = pos.getZ() + 0.5D;

        switch (trap) {
            case FALLING_ROCKS -> {
                for (int x = 0; x < 3; x++) {
                    for (int z = 0; z < 3; z++) {
                        EntityRubble rubble =
                                new EntityRubble(
                                        level,
                                        pos.getX() - 0.5 + x,
                                        pos.getY() - 0.5,
                                        pos.getZ() - 0.5 + z);
                        rubble.setBlockState(ModBlocks.REINFORCED_STONE.get().defaultBlockState());
                        level.addFreshEntity(rubble);
                    }
                }
            }
            case ARROW -> spawnArrow(level, pos, false);
            case FLAMING_ARROW -> spawnArrow(level, pos, true);
            case PILLAR -> {
                for (int i = 0; i < 3; i++) {
                    level.setBlockAndUpdate(
                            pos.below(1 + i), ModBlocks.CONCRETE_PILLAR.get().defaultBlockState());
                }
            }
            case POISON_DART -> {}
            case ZOMBIE -> {
                Zombie zombie = EntityTypes.ZOMBIE.create(level, EntitySpawnReason.TRIGGERED);
                if (zombie != null) {
                    zombie.setPos(cx, pos.getY() + 1, cz);
                    zombie.setItemSlot(
                            EquipmentSlot.MAINHAND,
                            new ItemStack(
                                    switch (level.getRandom().nextInt(3)) {
                                        case 0 -> ModItems.CHERNOBYLSIGN;
                                        case 1 -> ModItems.COBALT_SWORD;
                                        default -> ModItems.CMB_HOE;
                                    }));
                    zombie.setDropChance(EquipmentSlot.MAINHAND, 1.0F);
                    level.addFreshEntity(zombie);
                }
            }
            case SPIDERS -> {
                for (int i = 0; i < 3; i++) {
                    CaveSpider spider =
                            EntityTypes.CAVE_SPIDER.create(level, EntitySpawnReason.TRIGGERED);
                    if (spider != null) {
                        spider.setPos(cx, pos.getY() - 1, cz);
                        level.addFreshEntity(spider);
                    }
                }
            }
            default -> {}
        }

        level.playSound(null, pos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.3F, 0.6F);
        level.setBlockAndUpdate(pos, ModBlocks.BRICK_JUNGLE.get().defaultBlockState());
    }

    private void spawnArrow(ServerLevel level, BlockPos pos, boolean flaming) {

        int dx = dir == null ? 0 : dir.getStepX();
        int dz = dir == null ? 0 : dir.getStepZ();
        Arrow arrow =
                new Arrow(
                        level,
                        pos.getX() + 0.5 + dx,
                        pos.getY() + 0.5,
                        pos.getZ() + 0.5 + dz,
                        ItemStack.EMPTY,
                        ItemStack.EMPTY);
        arrow.setDeltaMovement(dx, 0, dz);
        if (flaming) arrow.igniteForSeconds(60);
        level.addFreshEntity(arrow);
    }

    private void setDetector(ServerLevel level, BlockPos pos, Trap trap) {
        switch (trap) {
            case FALLING_ROCKS, SPIDERS ->
                    detector =
                            new AABB(
                                    pos.getX() - 1,
                                    pos.getY() - 3,
                                    pos.getZ() - 1,
                                    pos.getX() + 2,
                                    pos.getY(),
                                    pos.getZ() + 2);
            case PILLAR ->
                    detector =
                            new AABB(
                                    pos.getX() + 0.2,
                                    pos.getY() - 3,
                                    pos.getZ() + 0.2,
                                    pos.getX() + 0.8,
                                    pos.getY(),
                                    pos.getZ() + 0.8);
            case ARROW, FLAMING_ARROW, POISON_DART -> setDetectorDirectional(level, pos);
            case ZOMBIE ->
                    detector =
                            new AABB(
                                    pos.getX() - 1,
                                    pos.getY() + 1,
                                    pos.getZ() - 1,
                                    pos.getX() + 2,
                                    pos.getY() + 2,
                                    pos.getZ() + 2);
            default -> detector = new AABB(pos);
        }
    }

    private void setDetectorDirectional(ServerLevel level, BlockPos pos) {
        List<Direction> dirs = new ArrayList<>(Direction.Plane.HORIZONTAL.stream().toList());
        RandomSource rand = level.getRandom();
        for (int i = dirs.size() - 1; i > 0; i--) {
            int j = rand.nextInt(i + 1);
            Direction tmp = dirs.get(i);
            dirs.set(i, dirs.get(j));
            dirs.set(j, tmp);
        }

        for (Direction d : dirs) {
            if (level.getBlockState(pos.relative(d)).isAir()) {
                double minX = pos.getX() + 0.4, minY = pos.getY() + 0.4, minZ = pos.getZ() + 0.4;
                double maxX = pos.getX() + 0.6, maxY = pos.getY() + 0.6, maxZ = pos.getZ() + 0.6;
                if (d.getStepX() > 0) maxX += 3;
                else if (d.getStepX() < 0) minX -= 3;
                if (d.getStepZ() > 0) maxZ += 3;
                else if (d.getStepZ() < 0) minZ -= 3;
                detector = new AABB(minX, minY, minZ, maxX, maxY, maxZ);
                this.dir = d;
                return;
            }
        }
        detector = new AABB(pos);
    }
}
