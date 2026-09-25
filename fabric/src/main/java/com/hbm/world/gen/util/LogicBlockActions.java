// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.world.gen.util;

import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.bomb.BlockChargeBase;
import com.hbm.entity.ModEntities;
import com.hbm.entity.item.EntityFallingBlockNT;
import com.hbm.entity.mob.EntityUndeadSoldier;
import com.hbm.entity.mob.ai.EntityAIFireGun;
import com.hbm.items.EnumSecretType;
import com.hbm.items.ModItems;
import com.hbm.items.weapon.sedna.factory.GunFactory;
import com.hbm.lib.Library;
import com.hbm.platform.Services;
import com.hbm.tileentity.BlockEntityLogicBlock;
import com.hbm.tileentity.BlockEntitySkeletonHolder;
import com.hbm.tileentity.bomb.BlockEntityCharge;
import com.hbm.tileentity.machine.BlockEntityLockableBase;
import com.hbm.util.MobUtil;
import com.hbm.world.NtmWorldgenFields;
import com.hbm.world.WorldgenHash;
import com.hbm.world.gen.WorldgenHeight;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntitySpawnRequest;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.skeleton.Skeleton;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;

public final class LogicBlockActions {

    public static final Map<String, Consumer<BlockEntityLogicBlock>> ACTIONS =
            new LinkedHashMap<>();

    private static final long POWER_LOCK_PINS =
            WorldgenHash.identifier(Library.id("logic/power_lock_pins"));
    private static final long ABERRATOR_REWARD =
            WorldgenHash.identifier(Library.id("logic/aberrator_reward"));

    static {
        ACTIONS.put("FODDER_WAVE", LogicBlockActions::fodderWave);
        ACTIONS.put("COLLAPSE_ROOF_RAD_5", LogicBlockActions::collapseRoofRad5);
        ACTIONS.put("POWER_LOCK", LogicBlockActions::powerLock);
        ACTIONS.put("BOMB_CRANE", LogicBlockActions::bombCrane);
        ACTIONS.put("SKELETON_GUN_TIER_1", tile -> skeletonGunTier(tile, 1));
        ACTIONS.put("SKELETON_GUN_TIER_2", tile -> skeletonGunTier(tile, 2));
        ACTIONS.put("SKELETON_GUN_TIER_3", tile -> skeletonGunTier(tile, 3));
        ACTIONS.put("ZOMBIE_TIER_1", tile -> zombies(tile, MobUtil.SLOT_POOL_COMMON));
        ACTIONS.put("ZOMBIE_TIER_2", tile -> zombies(tile, MobUtil.SLOT_POOL_ADV));
        ACTIONS.put("ABERRATOR", LogicBlockActions::aberrator);
        ACTIONS.put("DEAD_GUY_CRANE", deadGuyCrane());
        ACTIONS.put("DEAD_GUY_BASE_TOWER", LogicBlockActions::deadGuyBaseTower);
    }

    private LogicBlockActions() {}

    private static Consumer<BlockEntityLogicBlock> deadGuyCrane() {
        return tile -> {
            if (tile.phase != 1 || !(tile.getLevel() instanceof ServerLevel level)) return;

            BlockPos pos = tile.getBlockPos();
            level.setBlockAndUpdate(pos, ModBlocks.SKELETON_HOLDER.get().defaultBlockState());
            Player player =
                    level.getEntitiesOfClass(
                                    Player.class,
                                    new AABB(
                                                    pos.getX(),
                                                    pos.getY(),
                                                    pos.getZ(),
                                                    pos.getX() + 1,
                                                    pos.getY() - 2,
                                                    pos.getZ() + 1)
                                            .inflate(25.0))
                            .getFirst();
            if (!(level.getBlockEntity(pos) instanceof BlockEntitySkeletonHolder skeleton)) return;

            skeleton.item =
                    hasHangman(player)
                            ? new ItemStack(ModItems.CLAY_TABLET)
                            : new ItemStack(ModItems.GUN_HANGMAN);
            skeleton.setChanged();
            BlockState state = level.getBlockState(pos);
            level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
        };
    }

    private static void deadGuyBaseTower(BlockEntityLogicBlock tile) {
        if (tile.phase != 1 || !(tile.getLevel() instanceof ServerLevel level)) return;

        BlockPos pos = tile.getBlockPos();
        level.setBlockAndUpdate(pos, ModBlocks.SKELETON_HOLDER.get().defaultBlockState());
        if (!(level.getBlockEntity(pos) instanceof BlockEntitySkeletonHolder skeleton)) return;

        int roll = level.getRandom().nextInt(100);
        skeleton.item =
                roll < 44
                        ? new ItemStack(
                                ModItems.AMMO_STANDARD.get(GunFactory.EnumAmmo.CT_MORTAR), 3)
                        : roll < 71
                                ? new ItemStack(
                                        ModItems.AMMO_STANDARD.get(
                                                GunFactory.EnumAmmo.CT_MORTAR_CHARGE))
                                : roll < 92
                                        ? new ItemStack(
                                                ModItems.AMMO_STANDARD.get(
                                                        GunFactory.EnumAmmo.NUKE_STANDARD))
                                        : roll < 96
                                                ? ModItems.ITEM_SECRET.stack(
                                                        EnumSecretType.ABERRATOR)
                                                : ModItems.ITEM_SECRET.stack(EnumSecretType.FOLLY);
        skeleton.setChanged();
        BlockState state = level.getBlockState(pos);
        level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
    }

    private static boolean hasHangman(Player player) {
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < Inventory.INVENTORY_SIZE; slot++) {
            if (inventory.getItem(slot).is(ModItems.GUN_HANGMAN.get())) return true;
        }
        return false;
    }

    private static void collapseRoofRad5(BlockEntityLogicBlock tile) {
        if (tile.phase == 0 || !(tile.getLevel() instanceof ServerLevel level)) return;

        BlockPos origin = tile.getBlockPos();
        int radius = 4;
        int radiusSquaredHalf = radius * radius / 2;
        for (int x = -radius; x < radius; x++) {
            int xSquared = x * x;
            for (int y = -radius; y < radius; y++) {
                int xySquared = xSquared + y * y;
                for (int z = -radius; z < radius; z++) {
                    if (xySquared + z * z >= radiusSquaredHalf) continue;

                    BlockPos pos = origin.offset(x, y, z);
                    BlockState state = level.getBlockState(pos);
                    if (state.getBlock().getExplosionResistance() <= 70F) {
                        EntityFallingBlockNT.fall(level, pos, state);
                    }
                }
            }
        }
        level.setBlockAndUpdate(origin, Blocks.AIR.defaultBlockState());
    }

    private static void powerLock(BlockEntityLogicBlock tile) {
        if (tile.phase != 0 || !(tile.getLevel() instanceof ServerLevel level)) return;

        BlockPos pos = tile.getBlockPos();
        if (level.getEntitiesOfClass(Player.class, legacyPlayerBox(pos, 3)).isEmpty()) return;

        Player player = level.getNearestPlayer(pos.getX(), pos.getY(), pos.getZ(), 300D, false);
        player.sendSystemMessage(
                Component.translatable("chat.logicBlock.powerLock")
                        .withStyle(ChatFormatting.LIGHT_PURPLE)
                        .append(
                                Component.translatable("chat.logicBlock.lowPowerWarningLocking")
                                        .withStyle(ChatFormatting.RESET)));
        tile.phase++;

        for (Direction direction : Direction.VALUES) {
            BlockEntity neighbor = level.getBlockEntity(pos.relative(direction));
            if (neighbor instanceof BlockEntityLockableBase safe) {
                safe.setPins(
                        NtmWorldgenFields.get(level).random(POWER_LOCK_PINS, pos).nextInt(999));
                break;
            }
        }
    }

    private static AABB legacyPlayerBox(BlockPos pos, int radius) {
        return new AABB(
                pos.getX() - radius,
                pos.getY() - radius,
                pos.getZ() - radius,
                pos.getX() + 1 + radius,
                pos.getY() - 2 + radius,
                pos.getZ() + 1 + radius);
    }

    private static void bombCrane(BlockEntityLogicBlock tile) {
        if (!(tile.getLevel() instanceof ServerLevel level)) return;

        BlockPos pos = tile.getBlockPos();
        BlockPos chargePos = pos.above();
        if (tile.phase == 0) {
            level.setBlockAndUpdate(
                    chargePos,
                    ModBlocks.CHARGE_C4
                            .get()
                            .defaultBlockState()
                            .setValue(BlockChargeBase.FACING, Direction.UP));
            if (level.getBlockEntity(chargePos) instanceof BlockEntityCharge charge) {
                charge.timer = 1200;
                charge.changed();
            }
        }

        if (tile.phase >= 1) {
            if (level.getBlockEntity(chargePos) instanceof BlockEntityCharge charge) {
                charge.arm(level);
            }
            level.setBlockAndUpdate(pos, hbmBlock("block_steel").defaultBlockState());
        }
    }

    private static void aberrator(BlockEntityLogicBlock tile) {
        if (!(tile.getLevel() instanceof ServerLevel level)) return;

        BlockPos pos = tile.getBlockPos();
        if (tile.phase == 1 || tile.phase == 2) {
            Player target = level.getNearestPlayer(pos.getX(), pos.getY(), pos.getZ(), 25D, false);
            if (tile.timer == 0) {
                double x = 20D;
                double z = 0D;
                double cos = Math.cos(Math.PI / 5D);
                double sin = Math.sin(Math.PI / 5D);
                for (int i = 0; i < 10; i++) {
                    if (x > 8D) x += level.getRandom().nextInt(10) - 5;
                    EntityUndeadSoldier soldier =
                            ModEntities.UNDEAD_SOLDIER.get().create(level, EntitySpawnReason.EVENT);
                    if (soldier == null)
                        throw new IllegalStateException("Could not construct ABERRATOR soldier");
                    for (int retry = 0; retry < 7; retry++) {
                        double sx = pos.getX() + 0.5D + x;
                        double sz = pos.getZ() + 0.5D + z;
                        LevelChunk chunk =
                                level.getChunkSource().getChunkNow((int) sx >> 4, (int) sz >> 4);

                        if (chunk == null) break;
                        int sy = WorldgenHeight.lightBlocking(chunk, (int) sx, (int) sz);
                        soldier.snapTo(sx, sy, sz, i * 36F, 0F);
                        if (soldier.canSpawnAtCurrentPosition()) {
                            Services.PLATFORM.finalizeSpawn(
                                    soldier,
                                    level,
                                    level.getCurrentDifficultyAt(soldier.blockPosition()),
                                    EntitySpawnReason.EVENT,
                                    null);
                            if (target != null) soldier.setTarget(target);
                            level.addFreshEntity(soldier);
                            break;
                        }
                    }
                    double previousX = x;
                    x = x * cos + z * sin;
                    z = z * cos - previousX * sin;
                }
            }
        }
        if (tile.phase > 2) {
            BlockPos holderPos = pos.above(18);
            if (level.getBlockEntity(holderPos) instanceof BlockEntitySkeletonHolder holder) {
                holder.item =
                        NtmWorldgenFields.get(level).random(ABERRATOR_REWARD, pos).nextInt(5) == 0
                                ? ModItems.ITEM_SECRET.stack(EnumSecretType.ABERRATOR)
                                : new ItemStack(ModItems.CLAY_TABLET_1);
                holder.setChanged();
                BlockState state = level.getBlockState(holderPos);
                level.sendBlockUpdated(holderPos, state, state, Block.UPDATE_CLIENTS);
            }
            level.setBlockAndUpdate(pos, Blocks.OBSIDIAN.defaultBlockState());
        }
    }

    private static Block hbmBlock(String path) {
        return BuiltInRegistries.BLOCK
                .getOptional(Library.id(path))
                .orElseThrow(
                        () ->
                                new IllegalStateException(
                                        "Logic action needs unregistered hbm:" + path));
    }

    private static void skeletonGunTier(BlockEntityLogicBlock tile, int tier) {
        if (tile.phase != 1 || !(tile.getLevel() instanceof ServerLevel level)) return;

        BlockPos pos = tile.getBlockPos();
        for (int i = 0; i < 3; i++) {

            Skeleton skeleton =
                    EntityTypes.SKELETON.create(
                            level, new EntitySpawnRequest(EntitySpawnReason.EVENT, true));
            if (skeleton == null)
                throw new IllegalStateException("Could not construct logic-block skeleton");
            skeleton.snapTo(pos, 0F, 0F);

            if (tier == 1) {

                MobUtil.addFireTask(skeleton);
                MobUtil.equipGunTier(skeleton, 1, new Random());
                MobUtil.equipArmorPool(skeleton, MobUtil.SLOT_POOL_MASKS, new Random());
                MobUtil.equipArmorPool(skeleton, MobUtil.SLOT_POOL_RANGED, new Random());
            } else {
                EntityAIFireGun gunTask = new EntityAIFireGun(skeleton);
                gunTask.minWait = 4;
                gunTask.maxWait = 5;
                gunTask.maxRange = tier == 2 ? 50D : 100D;
                gunTask.burstTime = 6;
                gunTask.inaccuracy = tier == 2 ? 5F : 1F;
                gunTask.randomBurst = false;
                MobUtil.addFireTask(skeleton, gunTask);
                MobUtil.equipGunTier(skeleton, tier, new Random());
                MobUtil.equipArmorPool(
                        skeleton,
                        tier == 2 ? MobUtil.SLOT_POOL_RANGED : MobUtil.SLOT_POOL_ADV_RANGED,
                        new Random());
            }

            level.addFreshEntity(skeleton);
            level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        }
    }

    private static void fodderWave(BlockEntityLogicBlock tile) {
        if (tile.phase != 1 || !(tile.getLevel() instanceof ServerLevel level)) return;

        BlockPos pos = tile.getBlockPos();

        int y = WorldgenHeight.lightBlocking(level, pos.getX(), pos.getZ());
        double x = 5D;
        double z = 0D;
        double cos = Math.cos(Math.PI / 5D);
        double sin = Math.sin(Math.PI / 5D);
        for (int i = 0; i < 10; i++) {
            Zombie zombie =
                    EntityTypes.ZOMBIE.create(
                            level, new EntitySpawnRequest(EntitySpawnReason.EVENT, true));
            if (zombie == null)
                throw new IllegalStateException("Could not construct logic-block zombie");
            zombie.snapTo(pos.getX() + 0.5D + x, y, pos.getZ() + 0.5D + z, i * 36F, 0F);
            MobUtil.equipArmorPool(zombie, MobUtil.SLOT_POOL_ADV, new Random());
            level.addFreshEntity(zombie);
            double previousX = x;
            x = x * cos + z * sin;
            z = z * cos - previousX * sin;
        }
        level.setBlockAndUpdate(pos, hbmBlock("block_steel").defaultBlockState());
    }

    private static void zombies(
            BlockEntityLogicBlock tile, Map<EquipmentSlot, List<MobUtil.WeightedItem>> pool) {
        if (tile.phase != 1 || !(tile.getLevel() instanceof ServerLevel level)) return;

        BlockPos pos = tile.getBlockPos();
        for (int i = 0; i < 3; i++) {

            Zombie zombie =
                    EntityTypes.ZOMBIE.create(
                            level, new EntitySpawnRequest(EntitySpawnReason.EVENT, true));
            if (zombie == null)
                throw new IllegalStateException("Could not construct logic-block zombie");
            zombie.snapTo(pos, 0F, 0F);
            MobUtil.equipArmorPool(zombie, pool, new Random());
            level.addFreshEntity(zombie);
            level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        }
    }
}
