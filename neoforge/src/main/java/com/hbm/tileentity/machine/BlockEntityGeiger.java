// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: HbmMods (The Bobcat)
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine;

import com.hbm.api.redstoneoverradio.IRORValueProvider;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.handler.radiation.RadiationSystemNT;
import com.hbm.sound.ModSounds;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public class BlockEntityGeiger extends BlockEntity implements IRORValueProvider {

    public static final String[] ROR =
            new String[] {
                PREFIX_VALUE + "rad",
            };

    private int timer;
    public double ticker;

    public BlockEntityGeiger(BlockPos pos, BlockState state) {
        super(ModBlockEntities.GEIGER.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, BlockEntityGeiger be) {
        be.serverTick((ServerLevel) level);
    }

    private static int pick(double x, ServerLevel level) {
        List<Integer> list = new ArrayList<>();
        if (x < 1) list.add(0);
        if (x < 5) list.add(0);
        if (x < 10) list.add(1);
        if (x > 5 && x < 15) list.add(2);
        if (x > 10 && x < 20) list.add(3);
        if (x > 15 && x < 25) list.add(4);
        if (x > 20 && x < 30) list.add(5);
        if (x > 25) list.add(6);
        return list.get(level.getRandom().nextInt(list.size()));
    }

    private void serverTick(ServerLevel level) {
        timer++;
        if (timer >= 10) {
            timer = 0;
            ticker = check();
            level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
        }
        if (timer % 5 == 0) {
            if (ticker > 0D) {
                int r = pick(ticker, level);
                if (r > 0) play(level, r);
            } else if (level.getRandom().nextInt(50) == 0) {
                play(level, 1);
            }
        }
    }

    private void play(ServerLevel level, int r) {
        level.playSound(
                null,
                worldPosition.getX(),
                worldPosition.getY(),
                worldPosition.getZ(),
                ModSounds.GEIGER[r - 1].get(),
                SoundSource.BLOCKS,
                1.0F,
                1.0F);
    }

    public float check() {
        return level instanceof ServerLevel sl
                ? (float) RadiationSystemNT.getRadForCoord(sl, worldPosition)
                : 0F;
    }

    public int comparatorPower() {
        return Math.clamp((int) Math.ceil(check() / 5F), 0, 15);
    }

    @Override
    public String[] getFunctionInfo() {
        return ROR;
    }

    @Override
    public @Nullable String provideRORValue(String name) {
        if ((PREFIX_VALUE + "rad").equals(name)) return "" + (int) Math.ceil(ticker);
        return null;
    }
}
