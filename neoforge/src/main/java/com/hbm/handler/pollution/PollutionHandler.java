// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: Contributors to Hbm's Nuclear Tech Mod
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.handler.pollution;

import com.hbm.config.RadiationConfig;
import com.hbm.data.MobData;
import com.hbm.data.RadiationData;
import com.hbm.entity.ModEntities;
import com.hbm.entity.mob.glyphid.EntityGlyphid;
import com.hbm.entity.mob.glyphid.EntityGlyphidDigger;
import com.hbm.entity.mob.glyphid.EntityGlyphidScout;
import com.hbm.handler.pollution.PollutionSavedData.PollutionData;
import com.hbm.hazard.HazardClass;
import com.hbm.lib.Library;
import com.hbm.packet.toclient.PollutionSyncPayload;
import com.hbm.platform.Services;
import com.hbm.potion.HbmPotion;
import com.hbm.util.ArmorRegistry;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class PollutionHandler {

    public static final float SOOT_PER_SECOND = 1F / 25F;

    public static final float HEAVY_METAL_PER_SECOND = 1F / 50F;

    public static final float POISON_PER_SECOND = 1F / 50F;
    protected static final float DESTRUCTION_THRESHOLD = 15F;
    protected static final int DESTRUCTION_COUNT = 5;
    static int eggTimer = 0;

    private static final Map<UUID, Float> SOOT_SENT = new HashMap<>();

    public static void init() {
        Services.SERVER.onServerTickPost(PollutionHandler::updateSystem);
        Services.SERVER.onServerTickPost(PollutionHandler::syncSoot);
        Services.SERVER.onPlayerJoin(player -> SOOT_SENT.remove(player.getUUID()));
        Services.SERVER.onPlayerRespawn(player -> SOOT_SENT.remove(player.getUUID()));
        Services.SERVER.onPlayerChangeLevel(
                (player, origin, destination) -> SOOT_SENT.remove(player.getUUID()));
        Services.SERVER.onPlayerDisconnect(player -> SOOT_SENT.remove(player.getUUID()));
        Services.SERVER.onServerStopping(server -> SOOT_SENT.clear());
    }

    private static void syncSoot(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            float soot = getPollution(player.level(), player.blockPosition(), PollutionType.SOOT);
            Float sent = SOOT_SENT.put(player.getUUID(), soot);
            if (sent == null || sent != soot)
                Services.NETWORK.sendTo(new PollutionSyncPayload(soot), player);
        }
    }

    public static double sootFogThreshold() {
        double threshold = RadiationData.SOOT_FOG_THRESHOLD.get();
        return MobData.RAMPANT_MODE.get() ? threshold * pollutionMult() : threshold;
    }

    public static void onBlockBroken(Player player, BlockPos pos) {
        if (!(player instanceof ServerPlayer) || !RadiationData.ENABLE_LEAD_FROM_BLOCKS.get())
            return;
        if (ArmorRegistry.hasProtection(player, EquipmentSlot.HEAD, HazardClass.PARTICLE_FINE))
            return;
        float metal = getPollution(player.level(), pos, PollutionType.HEAVYMETAL);
        if (metal < 5) return;
        player.addEffect(
                new MobEffectInstance(HbmPotion.lead(), 100, metal < 10 ? 0 : metal < 25 ? 1 : 2));
    }

    public static void incrementPollution(
            Level world, BlockPos pos, PollutionType type, float amount) {
        if (!RadiationData.ENABLE_POLLUTION.get() || pos == null) return;
        if (!(world instanceof ServerLevel server)) return;

        PollutionSavedData ppw = PollutionSavedData.get(server);
        ChunkPos chPos = new ChunkPos(pos.getX() >> 6, pos.getZ() >> 6);
        PollutionData data = ppw.pollution.computeIfAbsent(chPos, k -> new PollutionData());
        data.pollution[type.ordinal()] =
                Mth.clamp(
                        (float) (data.pollution[type.ordinal()] + amount * pollutionMult()),
                        0F,
                        10_000F);
        ppw.setDirty();
    }

    public static void decrementPollution(
            Level world, BlockPos pos, PollutionType type, float amount) {
        incrementPollution(world, pos, type, -amount);
    }

    public static void setPollution(Level world, BlockPos pos, PollutionType type, float amount) {
        if (!RadiationData.ENABLE_POLLUTION.get()) return;
        if (!(world instanceof ServerLevel server)) return;

        PollutionSavedData ppw = PollutionSavedData.get(server);
        ChunkPos chPos = new ChunkPos(pos.getX() >> 6, pos.getZ() >> 6);
        PollutionData data = ppw.pollution.computeIfAbsent(chPos, k -> new PollutionData());
        data.pollution[type.ordinal()] = amount;
        ppw.setDirty();
    }

    public static float getPollution(Level world, BlockPos pos, PollutionType type) {
        if (!RadiationData.ENABLE_POLLUTION.get()) return 0;
        if (!(world instanceof ServerLevel server)) return 0F;

        PollutionSavedData ppw = PollutionSavedData.getExisting(server);
        if (ppw == null) return 0F;
        ChunkPos chPos = new ChunkPos(pos.getX() >> 6, pos.getZ() >> 6);
        PollutionData data = ppw.pollution.get(chPos);
        if (data == null) return 0F;
        return data.pollution[type.ordinal()];
    }

    private static final Identifier SOOT_HEALTH = Library.id("soot_anger_health");
    private static final Identifier SOOT_DAMAGE = Library.id("soot_anger_damage");

    public static void decorateMob(LivingEntity living) {
        PollutionData data = getPollutionData(living.level(), living.blockPosition());
        if (data == null) return;
        if (!(living instanceof Enemy) || living instanceof EntityGlyphid) return;
        if (data.pollution[PollutionType.SOOT.ordinal()] <= RadiationData.BUFF_MOB_THRESHOLD.get())
            return;
        buff(living.getAttribute(Attributes.MAX_HEALTH), SOOT_HEALTH, 1D);
        buff(living.getAttribute(Attributes.ATTACK_DAMAGE), SOOT_DAMAGE, 1.5D);
        living.heal(living.getMaxHealth());
    }

    private static void buff(@Nullable AttributeInstance attribute, Identifier id, double amount) {
        if (attribute != null && !attribute.hasModifier(id))
            attribute.addPermanentModifier(
                    new AttributeModifier(
                            id, amount, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
    }

    private static double pollutionMult() {
        double mult = RadiationData.POLLUTION_MULT.get();
        return MobData.RAMPANT_MODE.get() && mult == 1D ? 3D : mult;
    }

    public static PollutionData getPollutionData(Level world, BlockPos pos) {
        if (!RadiationData.ENABLE_POLLUTION.get()) return null;
        if (!(world instanceof ServerLevel server)) return null;

        PollutionSavedData ppw = PollutionSavedData.getExisting(server);
        if (ppw == null) return null;
        ChunkPos chPos = new ChunkPos(pos.getX() >> 6, pos.getZ() >> 6);
        return ppw.pollution.get(chPos);
    }

    public static void updateSystem(MinecraftServer server) {
        if (!RadiationData.ENABLE_POLLUTION.get()) return;

        handleWorldDestruction(server);

        eggTimer++;
        if (eggTimer < 60) return;
        eggTimer = 0;

        for (ServerLevel world : server.getAllLevels()) {
            PollutionSavedData ppw = PollutionSavedData.getExisting(world);
            if (ppw == null || ppw.pollution.isEmpty()) continue;

            HashMap<ChunkPos, PollutionData> newPollution = new HashMap<>();

            for (Map.Entry<ChunkPos, PollutionData> chunk : ppw.pollution.entrySet()) {
                int x = chunk.getKey().x();
                int z = chunk.getKey().z();
                PollutionData data = chunk.getValue();

                float[] pollutionForNeightbors = new float[PollutionType.VALUES.length];
                int S = PollutionType.SOOT.ordinal();
                int H = PollutionType.HEAVYMETAL.ordinal();
                int P = PollutionType.POISON.ordinal();

                if (data.pollution[S] > 10) {
                    pollutionForNeightbors[S] = data.pollution[S] * 0.05F;
                    data.pollution[S] *= 0.8F;
                }

                data.pollution[S] *= 0.99F;
                data.pollution[H] *= 0.9995F;

                if (data.pollution[P] > 10) {
                    pollutionForNeightbors[P] = data.pollution[P] * 0.025F;
                    data.pollution[P] *= 0.9F;
                } else {
                    data.pollution[P] *= 0.995F;
                }

                PollutionData newData = newPollution.get(chunk.getKey());
                if (newData == null) newData = new PollutionData();

                boolean shouldPut = false;
                for (int i = 0; i < newData.pollution.length; i++) {
                    newData.pollution[i] += data.pollution[i];
                    if (newData.pollution[i] > 0) shouldPut = true;
                }
                if (shouldPut) newPollution.put(chunk.getKey(), newData);

                int[][] offsets = new int[][] {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
                for (int[] offset : offsets) {
                    ChunkPos offPos = new ChunkPos(x + offset[0], z + offset[1]);
                    PollutionData offsetData = newPollution.get(offPos);
                    if (offsetData == null) offsetData = new PollutionData();

                    shouldPut = false;
                    for (int i = 0; i < offsetData.pollution.length; i++) {
                        offsetData.pollution[i] += pollutionForNeightbors[i];
                        if (offsetData.pollution[i] > 0) shouldPut = true;
                    }
                    if (shouldPut) newPollution.put(offPos, offsetData);
                }
            }

            ppw.pollution.clear();
            ppw.pollution.putAll(newPollution);
            ppw.setDirty();
        }
    }

    protected static void handleWorldDestruction(MinecraftServer server) {
        for (ServerLevel world : server.getAllLevels()) {
            PollutionSavedData ppw = PollutionSavedData.getExisting(world);
            if (ppw == null || ppw.pollution.isEmpty()) continue;

            RandomSource rand = world.getRandom();

            for (Map.Entry<ChunkPos, PollutionData> pollution : ppw.pollution.entrySet()) {

                float poison = pollution.getValue().pollution[PollutionType.POISON.ordinal()];
                if (poison < DESTRUCTION_THRESHOLD) continue;

                ChunkPos entryPos = pollution.getKey();

                for (int i = 0; i < DESTRUCTION_COUNT; i++) {
                    int x = (entryPos.x() << 6) + rand.nextInt(64);
                    int z = (entryPos.z() << 6) + rand.nextInt(64);

                    if (world.hasChunk(x >> 4, z >> 4)) {
                        int y =
                                world.getHeight(Heightmap.Types.WORLD_SURFACE, x, z)
                                        - rand.nextInt(3)
                                        + 1;
                        PollutionWorldHandler.destroyBlock(world, new BlockPos(x, y, z));
                    }
                }
            }
        }
    }

    public static Vec3 targetCoords;

    public static void rampantTargetSetter(BlockPos bedPos) {
        if (MobData.rampantGlyphidGuidance()) {
            targetCoords = new Vec3(bedPos.getX(), bedPos.getY(), bedPos.getZ());
        }
    }

    public static void rampantScoutPopulator(ServerLevel level, BlockPos pos) {
        if (!MobData.rampantNaturalScoutSpawn()) return;
        if (level.dimension() != Level.OVERWORLD) return;
        if (!level.canSeeSky(pos)) return;

        if (level.getRandom().nextInt(MobData.RAMPANT_SCOUT_SPAWN_CHANCE.get()) == 0) {

            float soot = getPollution(level, pos, PollutionType.SOOT);

            if (soot >= MobData.RAMPANT_SCOUT_SPAWN_THRESH.get()) {
                EntityGlyphidScout scout =
                        new EntityGlyphidScout(ModEntities.GLYPHID_SCOUT.get(), level);
                scout.snapTo(
                        pos.getX(),
                        pos.getY(),
                        pos.getZ(),
                        level.getRandom().nextFloat() * 360.0F,
                        0.0F);

                if (scout.isValidLightLevel()) {
                    EntityGlyphidDigger digger =
                            new EntityGlyphidDigger(ModEntities.GLYPHID_DIGGER.get(), level);
                    scout.snapTo(
                            pos.getX(),
                            pos.getY(),
                            pos.getZ(),
                            level.getRandom().nextFloat() * 360.0F,
                            0.0F);
                    digger.snapTo(
                            pos.getX(),
                            pos.getY(),
                            pos.getZ(),
                            level.getRandom().nextFloat() * 360.0F,
                            0.0F);

                    if (canSpawnHere(level, scout)) level.addFreshEntity(scout);
                    if (canSpawnHere(level, digger)) level.addFreshEntity(digger);
                }
            }
        }
    }

    private static boolean canSpawnHere(ServerLevel level, EntityGlyphid glyphid) {
        return level.getDifficulty() != Difficulty.PEACEFUL
                && level.noCollision(glyphid, glyphid.getBoundingBox())
                && !level.containsAnyLiquid(glyphid.getBoundingBox());
    }
}
