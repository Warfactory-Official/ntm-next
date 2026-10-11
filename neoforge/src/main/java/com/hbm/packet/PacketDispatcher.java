// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.packet;

import com.hbm.packet.synthetic.PingPayload;
import com.hbm.packet.toclient.*;
import com.hbm.packet.toserver.*;
import com.hbm.platform.NeoForgeNetwork;
import com.hbm.platform.NeoForgeNetwork;
import com.hbm.platform.NeoForgeServerEvents;

public final class PacketDispatcher {

    private PacketDispatcher() {}

    public static void register() {
        NeoForgeNetwork.clientbound.add(
                new NeoForgeNetwork.Registration<>(
                        BlobPayload.TYPE,
                        BlobPayload.STREAM_CODEC,
                        BlobPayload::handle,
                        PayloadHandlerThread.MAIN));
        NeoForgeNetwork.serverbound.add(
                new NeoForgeNetwork.Registration<>(
                        SyncRequestPayload.TYPE,
                        SyncRequestPayload.STREAM_CODEC,
                        SyncRequestPayload::handle,
                        PayloadHandlerThread.MAIN));
        NeoForgeNetwork.clientbound.add(
                new NeoForgeNetwork.Registration<>(
                        UnitPayload.TYPE,
                        UnitPayload.STREAM_CODEC,
                        UnitPayload::handle,
                        PayloadHandlerThread.MAIN));
        NeoForgeNetwork.clientbound.add(
                new NeoForgeNetwork.Registration<>(
                        PingPayload.S2C_TYPE,
                        PingPayload.S2C_STREAM_CODEC,
                        PingPayload::handleClient,
                        PayloadHandlerThread.NETWORK));
        NeoForgeNetwork.clientbound.add(
                new NeoForgeNetwork.Registration<>(
                        FluidPipeTintPayload.TYPE,
                        FluidPipeTintPayload.STREAM_CODEC,
                        FluidPipeTintPayload::handle,
                        PayloadHandlerThread.MAIN));
        NeoForgeNetwork.clientbound.add(
                new NeoForgeNetwork.Registration<>(
                        FluidPipeTintBulkPayload.TYPE,
                        FluidPipeTintBulkPayload.STREAM_CODEC,
                        FluidPipeTintBulkPayload::handle,
                        PayloadHandlerThread.MAIN));
        NeoForgeNetwork.clientbound.add(
                new NeoForgeNetwork.Registration<>(
                        RBMKDiagPayload.TYPE,
                        RBMKDiagPayload.STREAM_CODEC,
                        RBMKDiagPayload::handle,
                        PayloadHandlerThread.MAIN));
        NeoForgeNetwork.clientbound.add(
                new NeoForgeNetwork.Registration<>(
                        RadVisSnapshotPayload.TYPE,
                        RadVisSnapshotPayload.STREAM_CODEC,
                        RadVisSnapshotPayload::handle,
                        PayloadHandlerThread.MAIN));
        NeoForgeNetwork.registerPreJoinSync(
                FluidPropertySyncPayload.TYPE,
                FluidPropertySyncPayload.STREAM_CODEC,
                FluidPropertySyncPayload::apply,
                FluidPropertySyncPayload::of);
        NeoForgeNetwork.registerPreJoinSync(
                BlueprintPoolSyncPayload.TYPE,
                BlueprintPoolSyncPayload.STREAM_CODEC,
                BlueprintPoolSyncPayload::apply,
                BlueprintPoolSyncPayload::of);
        NeoForgeNetwork.registerPreJoinSync(
                OreGenerationSyncPayload.TYPE,
                OreGenerationSyncPayload.STREAM_CODEC,
                OreGenerationSyncPayload::apply,
                OreGenerationSyncPayload::of);
        NeoForgeServerEvents.onServerTickPre(FluidPropertySyncPayload::resendIfPending);
        NeoForgeNetwork.datapackSync.add(
                new NeoForgeNetwork.DatapackSync<>(
                        RecipeSyncPayload.TYPE,
                        RecipeSyncPayload.STREAM_CODEC,
                        RecipeSyncPayload::apply,
                        RecipeSyncPayload::of));
        NeoForgeNetwork.datapackSync.add(
                new NeoForgeNetwork.DatapackSync<>(
                        QMAWSyncPayload.TYPE,
                        QMAWSyncPayload.STREAM_CODEC,
                        QMAWSyncPayload::apply,
                        QMAWSyncPayload::of));
        NeoForgeServerEvents.onServerStopping(server -> com.hbm.qmaw.QMAWCatalog.clearServer());
        NeoForgeNetwork.clientbound.add(
                new NeoForgeNetwork.Registration<>(
                        HbmPlayerSyncPayload.TYPE,
                        HbmPlayerSyncPayload.STREAM_CODEC,
                        HbmPlayerSyncPayload::handle,
                        PayloadHandlerThread.MAIN));
        NeoForgeNetwork.clientbound.add(
                new NeoForgeNetwork.Registration<>(
                        ImpactSyncPayload.TYPE,
                        ImpactSyncPayload.STREAM_CODEC,
                        ImpactSyncPayload::handle,
                        PayloadHandlerThread.MAIN));
        NeoForgeNetwork.clientbound.add(
                new NeoForgeNetwork.Registration<>(
                        PollutionSyncPayload.TYPE,
                        PollutionSyncPayload.STREAM_CODEC,
                        PollutionSyncPayload::handle,
                        PayloadHandlerThread.MAIN));
        NeoForgeNetwork.clientbound.add(
                new NeoForgeNetwork.Registration<>(
                        PlayerPropsPayload.TYPE,
                        PlayerPropsPayload.STREAM_CODEC,
                        PlayerPropsPayload::handle,
                        PayloadHandlerThread.MAIN));
        NeoForgeNetwork.clientbound.add(
                new NeoForgeNetwork.Registration<>(
                        PlayerAppearancePayload.TYPE,
                        PlayerAppearancePayload.STREAM_CODEC,
                        PlayerAppearancePayload::handle,
                        PayloadHandlerThread.MAIN));
        NeoForgeNetwork.serverbound.add(
                new NeoForgeNetwork.Registration<>(
                        ArmorFlightPayload.TYPE,
                        ArmorFlightPayload.STREAM_CODEC,
                        ArmorFlightPayload::handle,
                        PayloadHandlerThread.MAIN));
        NeoForgeServerEvents.onStartTracking(PlayerAppearancePayload::sendInitial);
        NeoForgeNetwork.clientbound.add(
                new NeoForgeNetwork.Registration<>(
                        EffectNTPayload.TYPE,
                        EffectNTPayload.STREAM_CODEC,
                        EffectNTPayload::handle,
                        PayloadHandlerThread.MAIN));
        NeoForgeNetwork.clientbound.add(
                new NeoForgeNetwork.Registration<>(
                        GasFlamePayload.TYPE,
                        GasFlamePayload.STREAM_CODEC,
                        GasFlamePayload::handleClient,
                        PayloadHandlerThread.MAIN));
        NeoForgeNetwork.clientbound.add(
                new NeoForgeNetwork.Registration<>(
                        JetpackParticlePayload.TYPE,
                        JetpackParticlePayload.STREAM_CODEC,
                        JetpackParticlePayload::handle,
                        PayloadHandlerThread.MAIN));

        NeoForgeNetwork.clientbound.add(
                new NeoForgeNetwork.Registration<>(
                        OcclusionRefreshPayload.TYPE,
                        OcclusionRefreshPayload.STREAM_CODEC,
                        OcclusionRefreshPayload::handle,
                        PayloadHandlerThread.MAIN));

        NeoForgeNetwork.clientbound.add(
                new NeoForgeNetwork.Registration<>(
                        FallingMultiblockPayload.TYPE,
                        FallingMultiblockPayload.STREAM_CODEC,
                        FallingMultiblockPayload::handleClient,
                        PayloadHandlerThread.MAIN));
        NeoForgeNetwork.clientbound.add(
                new NeoForgeNetwork.Registration<>(
                        FallingBlockBatchPayload.TYPE,
                        FallingBlockBatchPayload.STREAM_CODEC,
                        FallingBlockBatchPayload::handleClient,
                        PayloadHandlerThread.MAIN));

        NeoForgeNetwork.clientbound.add(
                new NeoForgeNetwork.Registration<>(
                        HbmAnimationPayload.TYPE,
                        HbmAnimationPayload.STREAM_CODEC,
                        HbmAnimationPayload::handleClient,
                        PayloadHandlerThread.MAIN));
        NeoForgeNetwork.clientbound.add(
                new NeoForgeNetwork.Registration<>(
                        GunSoundPayload.TYPE,
                        GunSoundPayload.STREAM_CODEC,
                        GunSoundPayload::handleClient,
                        PayloadHandlerThread.MAIN));
        NeoForgeNetwork.clientbound.add(
                new NeoForgeNetwork.Registration<>(
                        GrenadeAnimationPayload.TYPE,
                        GrenadeAnimationPayload.STREAM_CODEC,
                        GrenadeAnimationPayload::handleClient,
                        PayloadHandlerThread.MAIN));
        NeoForgeNetwork.clientbound.add(
                new NeoForgeNetwork.Registration<>(
                        MuzzleFlashPayload.TYPE,
                        MuzzleFlashPayload.STREAM_CODEC,
                        MuzzleFlashPayload::handleClient,
                        PayloadHandlerThread.MAIN));
        NeoForgeNetwork.clientbound.add(
                new NeoForgeNetwork.Registration<>(
                        TeslaArcPayload.TYPE,
                        TeslaArcPayload.STREAM_CODEC,
                        TeslaArcPayload::handleClient,
                        PayloadHandlerThread.MAIN));
        NeoForgeNetwork.clientbound.add(
                new NeoForgeNetwork.Registration<>(
                        GlyphidPathPayload.TYPE,
                        GlyphidPathPayload.STREAM_CODEC,
                        GlyphidPathPayload::handleClient,
                        PayloadHandlerThread.MAIN));
        NeoForgeNetwork.clientbound.add(
                new NeoForgeNetwork.Registration<>(
                        CasingEjectPayload.TYPE,
                        CasingEjectPayload.STREAM_CODEC,
                        CasingEjectPayload::handleClient,
                        PayloadHandlerThread.MAIN));
        NeoForgeNetwork.clientbound.add(
                new NeoForgeNetwork.Registration<>(
                        GunCasingEjectPayload.TYPE,
                        GunCasingEjectPayload.STREAM_CODEC,
                        GunCasingEjectPayload::handleClient,
                        PayloadHandlerThread.MAIN));
        NeoForgeNetwork.clientbound.add(
                new NeoForgeNetwork.Registration<>(
                        PlasmaBlastPayload.TYPE,
                        PlasmaBlastPayload.STREAM_CODEC,
                        PlasmaBlastPayload::handleClient,
                        PayloadHandlerThread.MAIN));
        NeoForgeNetwork.clientbound.add(
                new NeoForgeNetwork.Registration<>(
                        ExplosionSmallPayload.TYPE,
                        ExplosionSmallPayload.STREAM_CODEC,
                        ExplosionSmallPayload::handleClient,
                        PayloadHandlerThread.MAIN));
        NeoForgeNetwork.clientbound.add(
                new NeoForgeNetwork.Registration<>(
                        HazePayload.TYPE,
                        HazePayload.STREAM_CODEC,
                        HazePayload::handleClient,
                        PayloadHandlerThread.MAIN));
        NeoForgeNetwork.clientbound.add(
                new NeoForgeNetwork.Registration<>(
                        ExplosionStandardPayload.TYPE,
                        ExplosionStandardPayload.STREAM_CODEC,
                        ExplosionStandardPayload::handleClient,
                        PayloadHandlerThread.MAIN));
        NeoForgeNetwork.clientbound.add(
                new NeoForgeNetwork.Registration<>(
                        SmokeShockPayload.TYPE,
                        SmokeShockPayload.STREAM_CODEC,
                        SmokeShockPayload::handleClient,
                        PayloadHandlerThread.MAIN));
        NeoForgeNetwork.clientbound.add(
                new NeoForgeNetwork.Registration<>(
                        MukePayload.TYPE,
                        MukePayload.STREAM_CODEC,
                        MukePayload::handleClient,
                        PayloadHandlerThread.MAIN));
        NeoForgeNetwork.clientbound.add(
                new NeoForgeNetwork.Registration<>(
                        AmatFlashPayload.TYPE,
                        AmatFlashPayload.STREAM_CODEC,
                        AmatFlashPayload::handleClient,
                        PayloadHandlerThread.MAIN));
        NeoForgeNetwork.clientbound.add(
                new NeoForgeNetwork.Registration<>(
                        RbmkJetPayload.TYPE,
                        RbmkJetPayload.STREAM_CODEC,
                        RbmkJetPayload::handleClient,
                        PayloadHandlerThread.MAIN));
        NeoForgeNetwork.clientbound.add(
                new NeoForgeNetwork.Registration<>(
                        BlackPowderPayload.TYPE,
                        BlackPowderPayload.STREAM_CODEC,
                        BlackPowderPayload::handleClient,
                        PayloadHandlerThread.MAIN));
        NeoForgeNetwork.clientbound.add(
                new NeoForgeNetwork.Registration<>(
                        SparkBurstPayload.TYPE,
                        SparkBurstPayload.STREAM_CODEC,
                        SparkBurstPayload::handleClient,
                        PayloadHandlerThread.MAIN));
        NeoForgeNetwork.clientbound.add(
                new NeoForgeNetwork.Registration<>(
                        DebugParticlePayload.TYPE,
                        DebugParticlePayload.STREAM_CODEC,
                        DebugParticlePayload::handleClient,
                        PayloadHandlerThread.MAIN));
        NeoForgeNetwork.clientbound.add(
                new NeoForgeNetwork.Registration<>(
                        AshesPayload.TYPE,
                        AshesPayload.STREAM_CODEC,
                        AshesPayload::handleClient,
                        PayloadHandlerThread.MAIN));
        NeoForgeNetwork.clientbound.add(
                new NeoForgeNetwork.Registration<>(
                        VomitPayload.TYPE,
                        VomitPayload.STREAM_CODEC,
                        VomitPayload::handleClient,
                        PayloadHandlerThread.MAIN));
        NeoForgeNetwork.clientbound.add(
                new NeoForgeNetwork.Registration<>(
                        SweatPayload.TYPE,
                        SweatPayload.STREAM_CODEC,
                        SweatPayload::handleClient,
                        PayloadHandlerThread.MAIN));
        NeoForgeNetwork.clientbound.add(
                new NeoForgeNetwork.Registration<>(
                        ProperJoltPayload.TYPE,
                        ProperJoltPayload.STREAM_CODEC,
                        ProperJoltPayload::handleClient,
                        PayloadHandlerThread.MAIN));
        NeoForgeNetwork.clientbound.add(
                new NeoForgeNetwork.Registration<>(
                        SkeletonPayload.TYPE,
                        SkeletonPayload.STREAM_CODEC,
                        SkeletonPayload::handleClient,
                        PayloadHandlerThread.MAIN));
        NeoForgeNetwork.clientbound.add(
                new NeoForgeNetwork.Registration<>(
                        GibletPayload.TYPE,
                        GibletPayload.STREAM_CODEC,
                        GibletPayload::handleClient,
                        PayloadHandlerThread.MAIN));
        NeoForgeNetwork.clientbound.add(
                new NeoForgeNetwork.Registration<>(
                        BlockDustBurstPayload.TYPE,
                        BlockDustBurstPayload.STREAM_CODEC,
                        BlockDustBurstPayload::handleClient,
                        PayloadHandlerThread.MAIN));
        NeoForgeNetwork.clientbound.add(
                new NeoForgeNetwork.Registration<>(
                        BlockDestroyPayload.TYPE,
                        BlockDestroyPayload.STREAM_CODEC,
                        BlockDestroyPayload::handleClient,
                        PayloadHandlerThread.MAIN));
        NeoForgeNetwork.clientbound.add(
                new NeoForgeNetwork.Registration<>(
                        PwrPrintPayload.TYPE,
                        PwrPrintPayload.STREAM_CODEC,
                        PwrPrintPayload::handle,
                        PayloadHandlerThread.MAIN));
        NeoForgeNetwork.clientbound.add(
                new NeoForgeNetwork.Registration<>(
                        MarkerPayload.TYPE,
                        MarkerPayload.STREAM_CODEC,
                        MarkerPayload::handle,
                        PayloadHandlerThread.MAIN));
        NeoForgeNetwork.clientbound.add(
                new NeoForgeNetwork.Registration<>(
                        PneumoAccessSyncPayload.TYPE,
                        PneumoAccessSyncPayload.STREAM_CODEC,
                        PneumoAccessSyncPayload::handleClient,
                        PayloadHandlerThread.MAIN));
        NeoForgeNetwork.clientbound.add(
                new NeoForgeNetwork.Registration<>(
                        PlayerInformPayload.TYPE,
                        PlayerInformPayload.STREAM_CODEC,
                        PlayerInformPayload::handle,
                        PayloadHandlerThread.MAIN));
        NeoForgeNetwork.clientbound.add(
                new NeoForgeNetwork.Registration<>(
                        OreDensityPayload.TYPE,
                        OreDensityPayload.STREAM_CODEC,
                        OreDensityPayload::handle,
                        PayloadHandlerThread.MAIN));

        NeoForgeNetwork.serverbound.add(
                new NeoForgeNetwork.Registration<>(
                        PingPayload.C2S_TYPE,
                        PingPayload.C2S_STREAM_CODEC,
                        PingPayload::handleServer,
                        PayloadHandlerThread.MAIN));

        NeoForgeNetwork.serverbound.add(
                new NeoForgeNetwork.Registration<>(
                        NbtControlPayload.TYPE,
                        NbtControlPayload.STREAM_CODEC,
                        NbtControlPayload::handleServer,
                        PayloadHandlerThread.MAIN));
        NeoForgeNetwork.serverbound.add(
                new NeoForgeNetwork.Registration<>(
                        FluidPipeTintRequestPayload.TYPE,
                        FluidPipeTintRequestPayload.STREAM_CODEC,
                        FluidPipeTintRequestPayload::handle,
                        PayloadHandlerThread.MAIN));
        NeoForgeNetwork.serverbound.add(
                new NeoForgeNetwork.Registration<>(
                        RBMKDiagRequestPayload.TYPE,
                        RBMKDiagRequestPayload.STREAM_CODEC,
                        RBMKDiagRequestPayload::handle,
                        PayloadHandlerThread.MAIN));
        NeoForgeNetwork.serverbound.add(
                new NeoForgeNetwork.Registration<>(
                        RadVisWatchPayload.TYPE,
                        RadVisWatchPayload.STREAM_CODEC,
                        RadVisWatchPayload::handle,
                        PayloadHandlerThread.MAIN));
        NeoForgeNetwork.serverbound.add(
                new NeoForgeNetwork.Registration<>(
                        FluidIdControlPayload.TYPE,
                        FluidIdControlPayload.STREAM_CODEC,
                        FluidIdControlPayload::handleServer,
                        PayloadHandlerThread.MAIN));
        NeoForgeNetwork.serverbound.add(
                new NeoForgeNetwork.Registration<>(
                        ToolPresetPayload.TYPE,
                        ToolPresetPayload.STREAM_CODEC,
                        ToolPresetPayload::handleServer,
                        PayloadHandlerThread.MAIN));
        NeoForgeNetwork.serverbound.add(
                new NeoForgeNetwork.Registration<>(
                        DesignatorCoordPayload.TYPE,
                        DesignatorCoordPayload.STREAM_CODEC,
                        DesignatorCoordPayload::handleServer,
                        PayloadHandlerThread.MAIN));
        NeoForgeNetwork.serverbound.add(
                new NeoForgeNetwork.Registration<>(
                        ItemControlPayload.TYPE,
                        ItemControlPayload.STREAM_CODEC,
                        ItemControlPayload::handleServer,
                        PayloadHandlerThread.MAIN));
        NeoForgeNetwork.serverbound.add(
                new NeoForgeNetwork.Registration<>(
                        RTTYPagerControlPayload.TYPE,
                        RTTYPagerControlPayload.STREAM_CODEC,
                        RTTYPagerControlPayload::handleServer,
                        PayloadHandlerThread.MAIN));
        NeoForgeNetwork.serverbound.add(
                new NeoForgeNetwork.Registration<>(
                        AnvilCraftPayload.TYPE,
                        AnvilCraftPayload.STREAM_CODEC,
                        AnvilCraftPayload::handleServer,
                        PayloadHandlerThread.MAIN));
        NeoForgeNetwork.serverbound.add(
                new NeoForgeNetwork.Registration<>(
                        BobmazonPurchasePayload.TYPE,
                        BobmazonPurchasePayload.STREAM_CODEC,
                        BobmazonPurchasePayload::handleServer,
                        PayloadHandlerThread.MAIN));
        NeoForgeNetwork.serverbound.add(
                new NeoForgeNetwork.Registration<>(
                        PneumoAccessInputPayload.TYPE,
                        PneumoAccessInputPayload.STREAM_CODEC,
                        PneumoAccessInputPayload::handleServer,
                        PayloadHandlerThread.MAIN));
        NeoForgeNetwork.serverbound.add(
                new NeoForgeNetwork.Registration<>(
                        CraneKeyPayload.TYPE,
                        CraneKeyPayload.STREAM_CODEC,
                        CraneKeyPayload::handleServer,
                        PayloadHandlerThread.MAIN));
        NeoForgeNetwork.serverbound.add(
                new NeoForgeNetwork.Registration<>(
                        ToolKeyPayload.TYPE,
                        ToolKeyPayload.STREAM_CODEC,
                        ToolKeyPayload::handleServer,
                        PayloadHandlerThread.MAIN));
        NeoForgeNetwork.serverbound.add(
                new NeoForgeNetwork.Registration<>(
                        KeybindPayload.TYPE,
                        KeybindPayload.STREAM_CODEC,
                        KeybindPayload::handleServer,
                        PayloadHandlerThread.MAIN));
        NeoForgeNetwork.serverbound.add(
                new NeoForgeNetwork.Registration<>(
                        CrateOpenHeldPayload.TYPE,
                        CrateOpenHeldPayload.STREAM_CODEC,
                        CrateOpenHeldPayload::handleServer,
                        PayloadHandlerThread.MAIN));
        NeoForgeNetwork.serverbound.add(
                new NeoForgeNetwork.Registration<>(
                        FarViewPayload.TYPE,
                        FarViewPayload.STREAM_CODEC,
                        FarViewPayload::handleServer,
                        PayloadHandlerThread.MAIN));
    }
}
