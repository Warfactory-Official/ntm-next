// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.packet;

import com.hbm.packet.synthetic.PingPayload;
import com.hbm.packet.toclient.*;
import com.hbm.packet.toserver.*;
import com.hbm.platform.FabricNetwork;
import com.hbm.platform.FabricNetwork;
import com.hbm.platform.FabricServerEvents;

public final class PacketDispatcher {

    private PacketDispatcher() {}

    public static void register() {
        FabricNetwork.clientbound.add(
                new FabricNetwork.Registration<>(
                        BlobPayload.TYPE,
                        BlobPayload.STREAM_CODEC,
                        BlobPayload::handle,
                        PayloadHandlerThread.MAIN));
        FabricNetwork.serverbound.add(
                new FabricNetwork.Registration<>(
                        SyncRequestPayload.TYPE,
                        SyncRequestPayload.STREAM_CODEC,
                        SyncRequestPayload::handle,
                        PayloadHandlerThread.MAIN));
        FabricNetwork.clientbound.add(
                new FabricNetwork.Registration<>(
                        UnitPayload.TYPE,
                        UnitPayload.STREAM_CODEC,
                        UnitPayload::handle,
                        PayloadHandlerThread.MAIN));
        FabricNetwork.clientbound.add(
                new FabricNetwork.Registration<>(
                        PingPayload.S2C_TYPE,
                        PingPayload.S2C_STREAM_CODEC,
                        PingPayload::handleClient,
                        PayloadHandlerThread.NETWORK));
        FabricNetwork.clientbound.add(
                new FabricNetwork.Registration<>(
                        FluidPipeTintPayload.TYPE,
                        FluidPipeTintPayload.STREAM_CODEC,
                        FluidPipeTintPayload::handle,
                        PayloadHandlerThread.MAIN));
        FabricNetwork.clientbound.add(
                new FabricNetwork.Registration<>(
                        FluidPipeTintBulkPayload.TYPE,
                        FluidPipeTintBulkPayload.STREAM_CODEC,
                        FluidPipeTintBulkPayload::handle,
                        PayloadHandlerThread.MAIN));
        FabricNetwork.clientbound.add(
                new FabricNetwork.Registration<>(
                        RBMKDiagPayload.TYPE,
                        RBMKDiagPayload.STREAM_CODEC,
                        RBMKDiagPayload::handle,
                        PayloadHandlerThread.MAIN));
        FabricNetwork.clientbound.add(
                new FabricNetwork.Registration<>(
                        RadVisSnapshotPayload.TYPE,
                        RadVisSnapshotPayload.STREAM_CODEC,
                        RadVisSnapshotPayload::handle,
                        PayloadHandlerThread.MAIN));
        FabricNetwork.registerPreJoinSync(
                FluidPropertySyncPayload.TYPE,
                FluidPropertySyncPayload.STREAM_CODEC,
                FluidPropertySyncPayload::apply,
                FluidPropertySyncPayload::of);
        FabricNetwork.registerPreJoinSync(
                BlueprintPoolSyncPayload.TYPE,
                BlueprintPoolSyncPayload.STREAM_CODEC,
                BlueprintPoolSyncPayload::apply,
                BlueprintPoolSyncPayload::of);
        FabricNetwork.registerPreJoinSync(
                OreGenerationSyncPayload.TYPE,
                OreGenerationSyncPayload.STREAM_CODEC,
                OreGenerationSyncPayload::apply,
                OreGenerationSyncPayload::of);
        FabricServerEvents.onServerTickPre(FluidPropertySyncPayload::resendIfPending);
        FabricNetwork.datapackSync.add(
                new FabricNetwork.DatapackSync<>(
                        RecipeSyncPayload.TYPE,
                        RecipeSyncPayload.STREAM_CODEC,
                        RecipeSyncPayload::apply,
                        RecipeSyncPayload::of));
        FabricNetwork.datapackSync.add(
                new FabricNetwork.DatapackSync<>(
                        QMAWSyncPayload.TYPE,
                        QMAWSyncPayload.STREAM_CODEC,
                        QMAWSyncPayload::apply,
                        QMAWSyncPayload::of));
        FabricServerEvents.onServerStopping(server -> com.hbm.qmaw.QMAWCatalog.clearServer());
        FabricNetwork.clientbound.add(
                new FabricNetwork.Registration<>(
                        HbmPlayerSyncPayload.TYPE,
                        HbmPlayerSyncPayload.STREAM_CODEC,
                        HbmPlayerSyncPayload::handle,
                        PayloadHandlerThread.MAIN));
        FabricNetwork.clientbound.add(
                new FabricNetwork.Registration<>(
                        ImpactSyncPayload.TYPE,
                        ImpactSyncPayload.STREAM_CODEC,
                        ImpactSyncPayload::handle,
                        PayloadHandlerThread.MAIN));
        FabricNetwork.clientbound.add(
                new FabricNetwork.Registration<>(
                        PollutionSyncPayload.TYPE,
                        PollutionSyncPayload.STREAM_CODEC,
                        PollutionSyncPayload::handle,
                        PayloadHandlerThread.MAIN));
        FabricNetwork.clientbound.add(
                new FabricNetwork.Registration<>(
                        PlayerPropsPayload.TYPE,
                        PlayerPropsPayload.STREAM_CODEC,
                        PlayerPropsPayload::handle,
                        PayloadHandlerThread.MAIN));
        FabricNetwork.clientbound.add(
                new FabricNetwork.Registration<>(
                        PlayerAppearancePayload.TYPE,
                        PlayerAppearancePayload.STREAM_CODEC,
                        PlayerAppearancePayload::handle,
                        PayloadHandlerThread.MAIN));
        FabricNetwork.serverbound.add(
                new FabricNetwork.Registration<>(
                        ArmorFlightPayload.TYPE,
                        ArmorFlightPayload.STREAM_CODEC,
                        ArmorFlightPayload::handle,
                        PayloadHandlerThread.MAIN));
        FabricServerEvents.onStartTracking(PlayerAppearancePayload::sendInitial);
        FabricNetwork.clientbound.add(
                new FabricNetwork.Registration<>(
                        EffectNTPayload.TYPE,
                        EffectNTPayload.STREAM_CODEC,
                        EffectNTPayload::handle,
                        PayloadHandlerThread.MAIN));
        FabricNetwork.clientbound.add(
                new FabricNetwork.Registration<>(
                        GasFlamePayload.TYPE,
                        GasFlamePayload.STREAM_CODEC,
                        GasFlamePayload::handleClient,
                        PayloadHandlerThread.MAIN));
        FabricNetwork.clientbound.add(
                new FabricNetwork.Registration<>(
                        JetpackParticlePayload.TYPE,
                        JetpackParticlePayload.STREAM_CODEC,
                        JetpackParticlePayload::handle,
                        PayloadHandlerThread.MAIN));

        FabricNetwork.clientbound.add(
                new FabricNetwork.Registration<>(
                        OcclusionRefreshPayload.TYPE,
                        OcclusionRefreshPayload.STREAM_CODEC,
                        OcclusionRefreshPayload::handle,
                        PayloadHandlerThread.MAIN));

        FabricNetwork.clientbound.add(
                new FabricNetwork.Registration<>(
                        FallingMultiblockPayload.TYPE,
                        FallingMultiblockPayload.STREAM_CODEC,
                        FallingMultiblockPayload::handleClient,
                        PayloadHandlerThread.MAIN));
        FabricNetwork.clientbound.add(
                new FabricNetwork.Registration<>(
                        FallingBlockBatchPayload.TYPE,
                        FallingBlockBatchPayload.STREAM_CODEC,
                        FallingBlockBatchPayload::handleClient,
                        PayloadHandlerThread.MAIN));

        FabricNetwork.clientbound.add(
                new FabricNetwork.Registration<>(
                        HbmAnimationPayload.TYPE,
                        HbmAnimationPayload.STREAM_CODEC,
                        HbmAnimationPayload::handleClient,
                        PayloadHandlerThread.MAIN));
        FabricNetwork.clientbound.add(
                new FabricNetwork.Registration<>(
                        GunSoundPayload.TYPE,
                        GunSoundPayload.STREAM_CODEC,
                        GunSoundPayload::handleClient,
                        PayloadHandlerThread.MAIN));
        FabricNetwork.clientbound.add(
                new FabricNetwork.Registration<>(
                        GrenadeAnimationPayload.TYPE,
                        GrenadeAnimationPayload.STREAM_CODEC,
                        GrenadeAnimationPayload::handleClient,
                        PayloadHandlerThread.MAIN));
        FabricNetwork.clientbound.add(
                new FabricNetwork.Registration<>(
                        MuzzleFlashPayload.TYPE,
                        MuzzleFlashPayload.STREAM_CODEC,
                        MuzzleFlashPayload::handleClient,
                        PayloadHandlerThread.MAIN));
        FabricNetwork.clientbound.add(
                new FabricNetwork.Registration<>(
                        TeslaArcPayload.TYPE,
                        TeslaArcPayload.STREAM_CODEC,
                        TeslaArcPayload::handleClient,
                        PayloadHandlerThread.MAIN));
        FabricNetwork.clientbound.add(
                new FabricNetwork.Registration<>(
                        GlyphidPathPayload.TYPE,
                        GlyphidPathPayload.STREAM_CODEC,
                        GlyphidPathPayload::handleClient,
                        PayloadHandlerThread.MAIN));
        FabricNetwork.clientbound.add(
                new FabricNetwork.Registration<>(
                        CasingEjectPayload.TYPE,
                        CasingEjectPayload.STREAM_CODEC,
                        CasingEjectPayload::handleClient,
                        PayloadHandlerThread.MAIN));
        FabricNetwork.clientbound.add(
                new FabricNetwork.Registration<>(
                        GunCasingEjectPayload.TYPE,
                        GunCasingEjectPayload.STREAM_CODEC,
                        GunCasingEjectPayload::handleClient,
                        PayloadHandlerThread.MAIN));
        FabricNetwork.clientbound.add(
                new FabricNetwork.Registration<>(
                        PlasmaBlastPayload.TYPE,
                        PlasmaBlastPayload.STREAM_CODEC,
                        PlasmaBlastPayload::handleClient,
                        PayloadHandlerThread.MAIN));
        FabricNetwork.clientbound.add(
                new FabricNetwork.Registration<>(
                        ExplosionSmallPayload.TYPE,
                        ExplosionSmallPayload.STREAM_CODEC,
                        ExplosionSmallPayload::handleClient,
                        PayloadHandlerThread.MAIN));
        FabricNetwork.clientbound.add(
                new FabricNetwork.Registration<>(
                        HazePayload.TYPE,
                        HazePayload.STREAM_CODEC,
                        HazePayload::handleClient,
                        PayloadHandlerThread.MAIN));
        FabricNetwork.clientbound.add(
                new FabricNetwork.Registration<>(
                        ExplosionStandardPayload.TYPE,
                        ExplosionStandardPayload.STREAM_CODEC,
                        ExplosionStandardPayload::handleClient,
                        PayloadHandlerThread.MAIN));
        FabricNetwork.clientbound.add(
                new FabricNetwork.Registration<>(
                        SmokeShockPayload.TYPE,
                        SmokeShockPayload.STREAM_CODEC,
                        SmokeShockPayload::handleClient,
                        PayloadHandlerThread.MAIN));
        FabricNetwork.clientbound.add(
                new FabricNetwork.Registration<>(
                        MukePayload.TYPE,
                        MukePayload.STREAM_CODEC,
                        MukePayload::handleClient,
                        PayloadHandlerThread.MAIN));
        FabricNetwork.clientbound.add(
                new FabricNetwork.Registration<>(
                        AmatFlashPayload.TYPE,
                        AmatFlashPayload.STREAM_CODEC,
                        AmatFlashPayload::handleClient,
                        PayloadHandlerThread.MAIN));
        FabricNetwork.clientbound.add(
                new FabricNetwork.Registration<>(
                        RbmkJetPayload.TYPE,
                        RbmkJetPayload.STREAM_CODEC,
                        RbmkJetPayload::handleClient,
                        PayloadHandlerThread.MAIN));
        FabricNetwork.clientbound.add(
                new FabricNetwork.Registration<>(
                        BlackPowderPayload.TYPE,
                        BlackPowderPayload.STREAM_CODEC,
                        BlackPowderPayload::handleClient,
                        PayloadHandlerThread.MAIN));
        FabricNetwork.clientbound.add(
                new FabricNetwork.Registration<>(
                        SparkBurstPayload.TYPE,
                        SparkBurstPayload.STREAM_CODEC,
                        SparkBurstPayload::handleClient,
                        PayloadHandlerThread.MAIN));
        FabricNetwork.clientbound.add(
                new FabricNetwork.Registration<>(
                        DebugParticlePayload.TYPE,
                        DebugParticlePayload.STREAM_CODEC,
                        DebugParticlePayload::handleClient,
                        PayloadHandlerThread.MAIN));
        FabricNetwork.clientbound.add(
                new FabricNetwork.Registration<>(
                        AshesPayload.TYPE,
                        AshesPayload.STREAM_CODEC,
                        AshesPayload::handleClient,
                        PayloadHandlerThread.MAIN));
        FabricNetwork.clientbound.add(
                new FabricNetwork.Registration<>(
                        VomitPayload.TYPE,
                        VomitPayload.STREAM_CODEC,
                        VomitPayload::handleClient,
                        PayloadHandlerThread.MAIN));
        FabricNetwork.clientbound.add(
                new FabricNetwork.Registration<>(
                        SweatPayload.TYPE,
                        SweatPayload.STREAM_CODEC,
                        SweatPayload::handleClient,
                        PayloadHandlerThread.MAIN));
        FabricNetwork.clientbound.add(
                new FabricNetwork.Registration<>(
                        ProperJoltPayload.TYPE,
                        ProperJoltPayload.STREAM_CODEC,
                        ProperJoltPayload::handleClient,
                        PayloadHandlerThread.MAIN));
        FabricNetwork.clientbound.add(
                new FabricNetwork.Registration<>(
                        SkeletonPayload.TYPE,
                        SkeletonPayload.STREAM_CODEC,
                        SkeletonPayload::handleClient,
                        PayloadHandlerThread.MAIN));
        FabricNetwork.clientbound.add(
                new FabricNetwork.Registration<>(
                        GibletPayload.TYPE,
                        GibletPayload.STREAM_CODEC,
                        GibletPayload::handleClient,
                        PayloadHandlerThread.MAIN));
        FabricNetwork.clientbound.add(
                new FabricNetwork.Registration<>(
                        BlockDustBurstPayload.TYPE,
                        BlockDustBurstPayload.STREAM_CODEC,
                        BlockDustBurstPayload::handleClient,
                        PayloadHandlerThread.MAIN));
        FabricNetwork.clientbound.add(
                new FabricNetwork.Registration<>(
                        BlockDestroyPayload.TYPE,
                        BlockDestroyPayload.STREAM_CODEC,
                        BlockDestroyPayload::handleClient,
                        PayloadHandlerThread.MAIN));
        FabricNetwork.clientbound.add(
                new FabricNetwork.Registration<>(
                        PwrPrintPayload.TYPE,
                        PwrPrintPayload.STREAM_CODEC,
                        PwrPrintPayload::handle,
                        PayloadHandlerThread.MAIN));
        FabricNetwork.clientbound.add(
                new FabricNetwork.Registration<>(
                        MarkerPayload.TYPE,
                        MarkerPayload.STREAM_CODEC,
                        MarkerPayload::handle,
                        PayloadHandlerThread.MAIN));
        FabricNetwork.clientbound.add(
                new FabricNetwork.Registration<>(
                        PneumoAccessSyncPayload.TYPE,
                        PneumoAccessSyncPayload.STREAM_CODEC,
                        PneumoAccessSyncPayload::handleClient,
                        PayloadHandlerThread.MAIN));
        FabricNetwork.clientbound.add(
                new FabricNetwork.Registration<>(
                        PlayerInformPayload.TYPE,
                        PlayerInformPayload.STREAM_CODEC,
                        PlayerInformPayload::handle,
                        PayloadHandlerThread.MAIN));
        FabricNetwork.clientbound.add(
                new FabricNetwork.Registration<>(
                        OreDensityPayload.TYPE,
                        OreDensityPayload.STREAM_CODEC,
                        OreDensityPayload::handle,
                        PayloadHandlerThread.MAIN));

        FabricNetwork.serverbound.add(
                new FabricNetwork.Registration<>(
                        PingPayload.C2S_TYPE,
                        PingPayload.C2S_STREAM_CODEC,
                        PingPayload::handleServer,
                        PayloadHandlerThread.MAIN));

        FabricNetwork.serverbound.add(
                new FabricNetwork.Registration<>(
                        NbtControlPayload.TYPE,
                        NbtControlPayload.STREAM_CODEC,
                        NbtControlPayload::handleServer,
                        PayloadHandlerThread.MAIN));
        FabricNetwork.serverbound.add(
                new FabricNetwork.Registration<>(
                        FluidPipeTintRequestPayload.TYPE,
                        FluidPipeTintRequestPayload.STREAM_CODEC,
                        FluidPipeTintRequestPayload::handle,
                        PayloadHandlerThread.MAIN));
        FabricNetwork.serverbound.add(
                new FabricNetwork.Registration<>(
                        RBMKDiagRequestPayload.TYPE,
                        RBMKDiagRequestPayload.STREAM_CODEC,
                        RBMKDiagRequestPayload::handle,
                        PayloadHandlerThread.MAIN));
        FabricNetwork.serverbound.add(
                new FabricNetwork.Registration<>(
                        RadVisWatchPayload.TYPE,
                        RadVisWatchPayload.STREAM_CODEC,
                        RadVisWatchPayload::handle,
                        PayloadHandlerThread.MAIN));
        FabricNetwork.serverbound.add(
                new FabricNetwork.Registration<>(
                        FluidIdControlPayload.TYPE,
                        FluidIdControlPayload.STREAM_CODEC,
                        FluidIdControlPayload::handleServer,
                        PayloadHandlerThread.MAIN));
        FabricNetwork.serverbound.add(
                new FabricNetwork.Registration<>(
                        ToolPresetPayload.TYPE,
                        ToolPresetPayload.STREAM_CODEC,
                        ToolPresetPayload::handleServer,
                        PayloadHandlerThread.MAIN));
        FabricNetwork.serverbound.add(
                new FabricNetwork.Registration<>(
                        DesignatorCoordPayload.TYPE,
                        DesignatorCoordPayload.STREAM_CODEC,
                        DesignatorCoordPayload::handleServer,
                        PayloadHandlerThread.MAIN));
        FabricNetwork.serverbound.add(
                new FabricNetwork.Registration<>(
                        ItemControlPayload.TYPE,
                        ItemControlPayload.STREAM_CODEC,
                        ItemControlPayload::handleServer,
                        PayloadHandlerThread.MAIN));
        FabricNetwork.serverbound.add(
                new FabricNetwork.Registration<>(
                        RTTYPagerControlPayload.TYPE,
                        RTTYPagerControlPayload.STREAM_CODEC,
                        RTTYPagerControlPayload::handleServer,
                        PayloadHandlerThread.MAIN));
        FabricNetwork.serverbound.add(
                new FabricNetwork.Registration<>(
                        AnvilCraftPayload.TYPE,
                        AnvilCraftPayload.STREAM_CODEC,
                        AnvilCraftPayload::handleServer,
                        PayloadHandlerThread.MAIN));
        FabricNetwork.serverbound.add(
                new FabricNetwork.Registration<>(
                        BobmazonPurchasePayload.TYPE,
                        BobmazonPurchasePayload.STREAM_CODEC,
                        BobmazonPurchasePayload::handleServer,
                        PayloadHandlerThread.MAIN));
        FabricNetwork.serverbound.add(
                new FabricNetwork.Registration<>(
                        PneumoAccessInputPayload.TYPE,
                        PneumoAccessInputPayload.STREAM_CODEC,
                        PneumoAccessInputPayload::handleServer,
                        PayloadHandlerThread.MAIN));
        FabricNetwork.serverbound.add(
                new FabricNetwork.Registration<>(
                        CraneKeyPayload.TYPE,
                        CraneKeyPayload.STREAM_CODEC,
                        CraneKeyPayload::handleServer,
                        PayloadHandlerThread.MAIN));
        FabricNetwork.serverbound.add(
                new FabricNetwork.Registration<>(
                        ToolKeyPayload.TYPE,
                        ToolKeyPayload.STREAM_CODEC,
                        ToolKeyPayload::handleServer,
                        PayloadHandlerThread.MAIN));
        FabricNetwork.serverbound.add(
                new FabricNetwork.Registration<>(
                        KeybindPayload.TYPE,
                        KeybindPayload.STREAM_CODEC,
                        KeybindPayload::handleServer,
                        PayloadHandlerThread.MAIN));
        FabricNetwork.serverbound.add(
                new FabricNetwork.Registration<>(
                        CrateOpenHeldPayload.TYPE,
                        CrateOpenHeldPayload.STREAM_CODEC,
                        CrateOpenHeldPayload::handleServer,
                        PayloadHandlerThread.MAIN));
    }
}
