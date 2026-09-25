// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.handler.radiation;

import com.hbm.handler.radiation.RadVisGeometry.ErrorRecord;
import com.hbm.handler.radiation.RadVisGeometry.FocusFilter;
import com.hbm.handler.radiation.RadVisGeometry.Mode;
import com.hbm.handler.radiation.RadVisGeometry.Piece;
import com.hbm.handler.radiation.RadVisGeometry.Probe;
import com.hbm.packet.toserver.RadVisWatchPayload;
import com.hbm.platform.Services;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public final class RadVisOverlay {
    public static final Config CONFIG = new Config();

    private static final double PROBE_RANGE = 64.0;

    public static final int STALE_TICKS = 100;
    private static final int RAMP_WIDTH = 100;

    private static final Long2ObjectOpenHashMap<RadVisSnapshot.Layout> LAYOUTS =
            new Long2ObjectOpenHashMap<>();
    private static @Nullable ResourceKey<Level> layoutsDimension;
    private static int watchedRadius = -1;
    private static @Nullable RadVisSnapshot snapshot;
    private static int silentTicks;
    private static List<ErrorRecord> errors = List.of();
    private static @Nullable Probe probe;

    private static long nextVerifyTick;
    private static volatile @Nullable Frame frame;

    record Frame(List<Piece> pieces, boolean xray) {}

    private RadVisOverlay() {}

    public static void clear() {
        watchedRadius = -1;
        snapshot = null;
        errors = List.of();
        probe = null;
        frame = null;
        nextVerifyTick = 0L;
        LAYOUTS.clear();
        layoutsDimension = null;
        RadVisMeshes.clear();
    }

    static @Nullable Frame frame() {
        return frame;
    }

    static @Nullable RadVisSnapshot snapshot() {
        return snapshot;
    }

    static List<ErrorRecord> errors() {
        return errors;
    }

    public static void accept(RadVisSnapshot incoming) {
        if (watchedRadius < 0) return;
        if (incoming.dimension != layoutsDimension) {
            LAYOUTS.clear();
            layoutsDimension = incoming.dimension;
        }
        snapshot = incoming.resolve(LAYOUTS);
        silentTicks = 0;
    }

    public static void clientTick() {
        Minecraft mc = Minecraft.getInstance();
        int want =
                CONFIG.enabled
                                && mc.level != null
                                && mc.player != null
                                && mc.getConnection() != null
                        ? Mth.clamp(CONFIG.radiusChunks, 0, RadVisServer.MAX_RADIUS)
                        : -1;
        if (want != watchedRadius) {
            if (mc.getConnection() != null)
                Services.NETWORK.sendToServer(new RadVisWatchPayload(want));
            watchedRadius = want;
            snapshot = null;
            errors = List.of();
            probe = null;
            LAYOUTS.clear();
            layoutsDimension = null;
        }
        RadVisVisual.sync(want < 0 ? null : mc.level);
        if (++silentTicks > STALE_TICKS) snapshot = null;
        RadVisSnapshot snap = snapshot;
        if (want < 0 || snap == null || snap.dimension != mc.level.dimension()) {
            frame = null;
            probe = null;
            return;
        }
        FocusFilter filter = focus(mc.player.blockPosition());
        if (!CONFIG.verify) {
            errors = List.of();
        } else if (mc.level.getGameTime() >= nextVerifyTick) {
            nextVerifyTick = mc.level.getGameTime() + Math.max(1, CONFIG.verifyInterval);
            errors = RadVisGeometry.verify(snap, filter);
        }
        probe = CONFIG.mode == Mode.PROBE ? RadVisGeometry.probeAt(snap, probeCell(mc)) : null;
        float alpha = Mth.clamp(CONFIG.alpha, 0.0f, 1.0f);
        List<Piece> pieces = new ArrayList<>();
        switch (CONFIG.mode) {
            case HEAT -> RadVisGeometry.heat(snap, filter, sliceY(mc), alpha, pieces);
            case POCKETS -> RadVisGeometry.pockets(snap, filter, alpha, CONFIG.colorByRad, pieces);
            case SECTIONS -> RadVisGeometry.sections(snap, filter, alpha, pieces);
            case PROBE -> RadVisGeometry.probe(probe, alpha, pieces);
            case ERRORS -> RadVisGeometry.errors(snap, filter, errors, alpha, pieces);
        }
        frame = new Frame(List.copyOf(pieces), CONFIG.xray);
    }

    private static BlockPos probeCell(Minecraft mc) {
        Vec3 eye = mc.player.getEyePosition();
        BlockHitResult hit =
                mc.level.clip(
                        new ClipContext(
                                eye,
                                eye.add(mc.player.getViewVector(1.0F).scale(PROBE_RANGE)),
                                ClipContext.Block.COLLIDER,
                                ClipContext.Fluid.NONE,
                                mc.player));
        return hit.getType() == HitResult.Type.BLOCK
                ? hit.getBlockPos().relative(hit.getDirection())
                : BlockPos.containing(eye);
    }

    private static int sliceY(Minecraft mc) {
        int y = CONFIG.sliceAutoY ? mc.player.getBlockY() : CONFIG.sliceY;
        return Mth.clamp(y, mc.level.getMinY(), mc.level.getMaxY());
    }

    private static @Nullable FocusFilter focus(BlockPos player) {
        Config cfg = CONFIG;
        if (!cfg.focusEnabled) return null;
        BlockPos anchor = cfg.focusAnchor != null ? cfg.focusAnchor : player;
        int dx = Math.max(0, cfg.focusDx),
                dy = Math.max(0, cfg.focusDy),
                dz = Math.max(0, cfg.focusDz);
        if (dx == 0 && dy == 0 && dz == 0) return null;
        return new FocusFilter(
                anchor.getX() - dx,
                anchor.getX() + dx,
                anchor.getY() - dy,
                anchor.getY() + dy,
                anchor.getZ() - dz,
                anchor.getZ() + dz);
    }

    public static void renderHud(GuiGraphicsExtractor g) {
        Minecraft mc = Minecraft.getInstance();
        if (!CONFIG.enabled || mc.level == null) return;
        Font font = mc.font;
        List<Component> head = new ArrayList<>(3);
        head.add(
                Component.translatable(
                        "radvis.hbm.hud.title",
                        Component.translatable(
                                "radvis.hbm.mode." + CONFIG.mode.name().toLowerCase(Locale.ROOT))));
        if (!VisualizationManager.supportsVisualization(mc.level)) {
            head.add(Component.translatable("radvis.hbm.hud.no_backend"));
        } else if (snapshot == null) {
            head.add(Component.translatable("radvis.hbm.hud.waiting"));
        }
        boolean legend =
                CONFIG.mode == Mode.HEAT
                        || CONFIG.mode == Mode.PROBE
                        || CONFIG.mode == Mode.POCKETS && CONFIG.colorByRad;
        Component scale =
                Component.translatable(
                        "radvis.hbm.hud.scale",
                        formatDecade(RadVisGeometry.HEAT_LOW),
                        formatDecade(RadVisGeometry.HEAT_HIGH));
        Probe p = probe;
        List<Component> tail = p == null ? List.of() : p.lines();

        int lineHeight = font.lineHeight + 1;
        int width = legend ? Math.max(RAMP_WIDTH, font.width(scale)) : 0;
        for (Component line : head) width = Math.max(width, font.width(line));
        for (Component line : tail) width = Math.max(width, font.width(line));
        int height = (head.size() + tail.size() + (legend ? 1 : 0)) * lineHeight + (legend ? 8 : 0);
        int x = 4, y = 4;
        g.fill(x - 2, y - 2, x + width + 2, y + height + 1, 0x90000000);
        for (Component line : head) {
            g.text(font, line, x, y, 0xFFFFFFFF, false);
            y += lineHeight;
        }
        if (legend) {
            for (int i = 0; i < RAMP_WIDTH; i++) {
                g.fill(
                        x + i,
                        y,
                        x + i + 1,
                        y + 6,
                        RadVisGeometry.rampColor(i / (RAMP_WIDTH - 1.0F), 1.0F));
            }
            y += 8;
            g.text(font, scale, x, y, 0xFFC0C0C0, false);
            y += lineHeight;
        }
        for (Component line : tail) {
            g.text(font, line, x, y, 0xFFFFFFFF, false);
            y += lineHeight;
        }
    }

    private static String formatDecade(double exponent) {
        return "1e" + (int) exponent;
    }

    public static final class Config {
        public boolean enabled;
        public int radiusChunks = 2;
        public Mode mode = Mode.HEAT;
        public boolean sliceAutoY = true;
        public int sliceY;
        public boolean xray;
        public float alpha = 0.4f;
        public boolean colorByRad;
        public boolean verify;
        public int verifyInterval = 10;
        public boolean focusEnabled;
        public @Nullable BlockPos focusAnchor;
        public int focusDx, focusDy, focusDz;

        public void reset() {
            enabled = false;
            radiusChunks = 2;
            mode = Mode.HEAT;
            sliceAutoY = true;
            sliceY = 0;
            xray = false;
            alpha = 0.4f;
            colorByRad = false;
            verify = false;
            verifyInterval = 10;
            focusEnabled = false;
            focusAnchor = null;
            focusDx = focusDy = focusDz = 0;
        }
    }
}
