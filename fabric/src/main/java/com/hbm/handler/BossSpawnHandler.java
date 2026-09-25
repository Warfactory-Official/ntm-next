// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.handler;

import com.hbm.advancement.AwardRegions;
import com.hbm.blocks.ModBlocks;
import com.hbm.data.MobData;
import com.hbm.data.WorldData;
import com.hbm.entity.ModEntities;
import com.hbm.entity.mob.EntityRADBeast;
import com.hbm.entity.projectile.EntityMeteor;
import com.hbm.extprop.HbmLivingProps;
import com.hbm.extprop.HbmPlayerProps;
import com.hbm.items.ModItems;
import com.hbm.platform.Services;
import com.hbm.tileentity.BlockEntityPedestal.PedestalEntry;
import com.hbm.tileentity.BlockEntityPedestal.PedestalEntryType;
import com.hbm.tileentity.BlockEntityPedestal;
import com.hbm.util.ContaminationUtil;
import com.hbm.util.DropHeight;
import com.hbm.world.gen.WorldgenHeight;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.ServerStatsCounter;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;

public final class BossSpawnHandler {

    private static final RandomSource meteorRand = RandomSource.create();

    public static int meteorShower = 0;

    private BossSpawnHandler() {}

    public static void init() {
        Services.SERVER.onServerTickPre(BossSpawnHandler::onServerTick);
        Services.SERVER.onPlayerRespawn(player -> HbmPlayerProps.getData(player).maskManTimer = 0);
    }

    private static void onServerTick(MinecraftServer server) {
        for (ServerLevel level : server.getAllLevels()) rollTheDice(level);
    }

    public static void rollTheDice(ServerLevel level) {
        RandomSource random = level.getRandom();
        long time = level.getGameTime();
        boolean surface = level.dimension() != Level.NETHER && level.dimension() != Level.END;
        List<ServerPlayer> players = level.players();

        if (MobData.ENABLE_MASKMAN.get()
                && time % 20 == 0
                && surface
                && level.getDifficulty() != Difficulty.PEACEFUL) {
            for (ServerPlayer player : players) maskManUpdate(level, player);
        }

        if (MobData.ENABLE_RAIDS.get()
                && time % MobData.RAID_DELAY.get() == 0
                && random.nextInt(MobData.RAID_CHANCE.get()) == 0
                && !players.isEmpty()
                && surface) {
            raid(level, players.get(random.nextInt(players.size())));
        }

        if (MobData.ENABLE_ELEMENTALS.get()
                && time % MobData.ELEMENTAL_DELAY.get() == 0
                && random.nextInt(MobData.ELEMENTAL_CHANCE.get()) == 0
                && !players.isEmpty()
                && surface) {
            elementals(level, players.get(random.nextInt(players.size())));
        }

        if (WorldData.ENABLE_METEOR_STRIKES.get()) meteorUpdate(level);

        if (time % 20 == 0 && random.nextInt(5) == 0 && !players.isEmpty() && surface) {
            ghost(level, players.get(random.nextInt(players.size())));
        }
    }

    public static void maskManUpdate(ServerLevel level, ServerPlayer player) {
        Item acidizer = ModBlocks.MACHINE_CRYSTALLIZER.get().asItem();
        ServerStatsCounter stats = player.getStats();

        boolean acidizerStat =
                stats.getValue(Stats.ITEM_CRAFTED.get(acidizer)) > 0
                        || stats.getValue(Stats.ITEM_USED.get(acidizer)) > 0;
        boolean hasRads = ContaminationUtil.getRads(player) >= MobData.MASKMAN_MIN_RAD.get();
        boolean underground = underground(level, player) || !MobData.MASKMAN_UNDERGROUND.get();
        HbmPlayerProps data = HbmPlayerProps.getData(player);

        if (!(acidizerStat && hasRads && underground)) {
            data.maskManTimer = 0;
            return;
        }

        data.maskManTimer++;

        if (data.maskManTimer == MobData.MASKMAN_DELAY.get() - 60) {
            player.sendSystemMessage(
                    Component.translatable("chat.bossSpawnHandler.theMaskManDraws")
                            .withStyle(ChatFormatting.RED));
        }

        if (data.maskManTimer >= MobData.MASKMAN_DELAY.get()) {
            data.maskManTimer = 0;

            RandomSource random = level.getRandom();
            double spawnX = player.getX() + random.nextGaussian() * 20;
            double spawnZ = player.getZ() + random.nextGaussian() * 20;
            if (trySpawn(
                    level,
                    spawnX,
                    0D,
                    spawnZ,
                    Mth.floor(spawnX),
                    Mth.floor(spawnZ),
                    create(level, ModEntities.MASK_MAN.get()))) {
                player.sendSystemMessage(
                        Component.translatable("chat.bossSpawnHandler.theMaskManIs")
                                .withStyle(ChatFormatting.RED));
            } else {
                player.sendSystemMessage(
                        Component.translatable("chat.bossSpawnHandler.seemsLikeMaskMan")
                                .withStyle(ChatFormatting.BLUE));
            }
        }
    }

    public static void raid(ServerLevel level, ServerPlayer player) {
        if (HbmPlayerProps.getData(player).fbiMark >= level.getGameTime()) return;

        RandomSource random = level.getRandom();
        player.sendSystemMessage(
                Component.translatable("chat.bossSpawnHandler.fbiOpenUp")
                        .withStyle(ChatFormatting.RED));

        Vec3 vec =
                new Vec3(MobData.RAID_ATTACK_DISTANCE.get(), 0, 0)
                        .yRot((float) (Math.PI * 2) * random.nextFloat());

        for (int i = 0; i < MobData.RAID_AMOUNT.get(); i++) {
            double spawnX = player.getX() + vec.x + random.nextGaussian() * 5;
            double spawnZ = player.getZ() + vec.z + random.nextGaussian() * 5;
            trySpawn(
                    level,
                    spawnX,
                    0D,
                    spawnZ,
                    (int) spawnX,
                    (int) spawnZ,
                    create(level, ModEntities.FBI.get()));
        }

        for (int i = 0; i < MobData.RAID_DRONES.get(); i++) {
            double spawnX = player.getX() + vec.x + random.nextGaussian() * 5;
            double spawnZ = player.getZ() + vec.z + random.nextGaussian() * 5;
            trySpawn(
                    level,
                    spawnX,
                    10D,
                    spawnZ,
                    (int) spawnX,
                    (int) spawnZ,
                    create(level, ModEntities.FBI_DRONE.get()));
        }
    }

    public static void elementals(ServerLevel level, ServerPlayer player) {
        HbmPlayerProps data = HbmPlayerProps.getData(player);
        if (!data.radMark) return;

        RandomSource random = level.getRandom();
        player.sendSystemMessage(
                Component.translatable("chat.bossSpawnHandler.youHearAFaint")
                        .withStyle(ChatFormatting.YELLOW));
        data.radMark = false;

        Vec3 vec = new Vec3(MobData.ELEMENTAL_DISTANCE.get(), 0, 0);

        for (int i = 0; i < MobData.ELEMENTAL_AMOUNT.get(); i++) {
            vec = vec.yRot((float) (Math.PI * 2) * random.nextFloat());

            double spawnX = player.getX() + vec.x + random.nextGaussian();
            double spawnZ = player.getZ() + vec.z + random.nextGaussian();

            EntityRADBeast rad = create(level, ModEntities.RAD_BEAST.get());
            if (i == 0) rad.makeLeader();

            trySpawn(level, spawnX, 0D, spawnZ, (int) spawnX, (int) spawnZ, rad);
        }
    }

    public static void ghost(ServerLevel level, ServerPlayer player) {
        if (HbmLivingProps.getDigamma(player) <= 0) return;

        RandomSource random = level.getRandom();
        Vec3 vec = new Vec3(75, 0, 0).yRot((float) (Math.PI * 2) * random.nextFloat());
        double spawnX = player.getX() + vec.x + random.nextGaussian();
        double spawnZ = player.getZ() + vec.z + random.nextGaussian();
        trySpawn(
                level,
                spawnX,
                0D,
                spawnZ,
                (int) spawnX,
                (int) spawnZ,
                create(level, ModEntities.GHOST.get()));
    }

    public static void markFBI(Player player) {

        if (!player.level().isClientSide()) {
            HbmPlayerProps.getData(player).fbiMark = player.level().getGameTime() + 20 * 60 * 20;
        }
    }

    public static void markRad(ServerLevel level, double x, double y, double z) {
        if (!MobData.ENABLE_ELEMENTALS.get()) return;

        AwardRegions.within(
                level, x, y, z, 100, player -> HbmPlayerProps.getData(player).radMark = true);
    }

    private static boolean underground(ServerLevel level, Player player) {
        int x = Mth.floor(player.getX());
        int z = Mth.floor(player.getZ());
        LevelChunk chunk = level.getChunkSource().getChunkNow(x >> 4, z >> 4);
        return chunk != null && WorldgenHeight.lightBlocking(chunk, x, z) > player.getY() + 3;
    }

    private static <T extends Mob> T create(ServerLevel level, EntityType<T> type) {
        T mob = type.create(level, EntitySpawnReason.EVENT);
        if (mob == null) throw new IllegalStateException("Could not construct " + type);
        return mob;
    }

    private static boolean trySpawn(
            ServerLevel level,
            double x,
            double yAbove,
            double z,
            int columnX,
            int columnZ,
            Mob mob) {
        LevelChunk chunk = level.getChunkSource().getChunkNow(columnX >> 4, columnZ >> 4);

        if (chunk == null) return false;
        mob.snapTo(
                x,
                WorldgenHeight.lightBlocking(chunk, columnX, columnZ) + yAbove,
                z,
                level.getRandom().nextFloat() * 360.0F,
                0.0F);
        Services.PLATFORM.finalizeSpawn(
                mob,
                level,
                level.getCurrentDifficultyAt(mob.blockPosition()),
                EntitySpawnReason.EVENT,
                null);
        return level.addFreshEntity(mob);
    }

    private static void meteorUpdate(ServerLevel level) {

        if (meteorRand.nextInt(
                        meteorShower > 0
                                ? WorldData.METEOR_SHOWER_CHANCE.get()
                                : WorldData.METEOR_STRIKE_CHANCE.get())
                == 0) {

            List<ServerPlayer> players = level.players();

            if (!players.isEmpty()) {

                Player p = players.get(meteorRand.nextInt(players.size()));

                if (p != null && level.dimension() == Level.OVERWORLD) {

                    boolean repell = false;
                    boolean strike = true;

                    for (EquipmentSlot slot : ArmorUtil.ARMOR_SLOTS) {
                        ItemStack armor = p.getItemBySlot(slot);
                        if (!armor.isEmpty() && ArmorModHandler.hasMods(armor)) {

                            for (ItemStack mod : ArmorModHandler.pryMods(armor)) {
                                if (mod.isEmpty()) continue;
                                if (mod.is(ModItems.PROTECTION_CHARM.get())) repell = true;
                                if (mod.is(ModItems.METEOR_CHARM.get())) strike = false;
                            }
                        }
                    }

                    if (!repell || strike) {
                        int x = Mth.floor(p.getX());
                        int z = Mth.floor(p.getZ());

                        List<PedestalEntry> entries =
                                BlockEntityPedestal.getEntriesForDimension(level.dimension());
                        if (entries != null)
                            for (PedestalEntry entry : entries) {
                                if (Math.abs(entry.pos().getX() - x) <= 100
                                        && Math.abs(entry.pos().getZ() - z) <= 100) {
                                    if (entry.type() == PedestalEntryType.CHARM_OF_PROTECTION)
                                        repell = true;
                                    if (entry.type() == PedestalEntryType.METEORITE_CHARM)
                                        strike = false;
                                }
                            }
                    }

                    if (strike) spawnMeteorAtPlayer(p, repell);
                }
            }
        }

        if (meteorShower > 0) {
            meteorShower--;
        }

        if (meteorRand.nextInt(WorldData.METEOR_STRIKE_CHANCE.get() * 100) == 0
                && WorldData.ENABLE_METEOR_SHOWERS.get()) {
            meteorShower =
                    (int)
                            (WorldData.METEOR_SHOWER_DURATION.get() * 0.75
                                    + WorldData.METEOR_SHOWER_DURATION.get()
                                            * 0.25
                                            * meteorRand.nextFloat());
        }
    }

    public static void spawnMeteorAtPlayer(Player player, boolean repell) {

        EntityMeteor meteor = new EntityMeteor(player.level());
        double meteorX = player.getX() + meteorRand.nextInt(201) - 100;
        double meteorZ = player.getZ() + meteorRand.nextInt(201) - 100;
        meteor.snapTo(
                meteorX,
                DropHeight.clearOfTerrain(player.level(), 384, meteorX, meteorZ),
                meteorZ,
                0F,
                0F);

        Vec3 vec;
        if (repell) {
            vec =
                    new Vec3(meteor.getX() - player.getX(), 0, meteor.getZ() - player.getZ())
                            .normalize();
            double vel = meteorRand.nextDouble();
            vec = new Vec3(vec.x * vel, vec.y, vec.z * vel);
            meteor.safe = true;
        } else {
            vec =
                    new Vec3(meteorRand.nextDouble() - 0.5D, 0, 0)
                            .yRot((float) (Math.PI * meteorRand.nextDouble()));
        }

        meteor.setDeltaMovement(vec.x, -2.5D, vec.z);
        player.level().addFreshEntity(meteor);
    }
}
