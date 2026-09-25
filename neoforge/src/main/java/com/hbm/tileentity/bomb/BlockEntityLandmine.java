// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.bomb;

import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.bomb.BlockLandmine;
import com.hbm.blocks.bomb.BlockLandmineAP;
import com.hbm.sound.ModSounds;
import com.hbm.stats.ModStats;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;

public class BlockEntityLandmine extends BlockEntity {

    private boolean isPrimed = false;

    private boolean waitingForPlayer = false;

    public BlockEntityLandmine(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LANDMINE.get(), pos, state);
    }

    public void deferUntilPlayerNear() {
        waitingForPlayer = true;
        setChanged();
    }

    public static void tick(Level level, BlockPos pos, BlockState state, BlockEntityLandmine be) {
        be.serverTick((ServerLevel) level, pos, state);
    }

    private void serverTick(ServerLevel level, BlockPos pos, BlockState state) {
        if (!(state.getBlock() instanceof BlockLandmine landmine)) return;

        if (landmine instanceof BlockLandmineAP) BlockLandmineAP.refreshGround(level, pos, state);

        double range = landmine.range;
        double height = landmine.height;

        if (waitingForPlayer) {
            range = 25;
            height = 25;
        } else if (!isPrimed) {
            range *= 2;
            height *= 2;
        }

        if (!level.getBlockState(pos.above()).isAir()) return;

        AABB box =
                new AABB(
                        pos.getX() - range,
                        pos.getY() - height,
                        pos.getZ() - range,
                        pos.getX() + range + 1,
                        pos.getY() + height,
                        pos.getZ() + range + 1);
        List<Entity> list = level.getEntities((Entity) null, box, e -> true);

        for (Entity entity : list) {
            EntityType<?> type = entity.getType();
            if (type.getCategory() == MobCategory.WATER_CREATURE) continue;
            if (type.getCategory() == MobCategory.AMBIENT) continue;

            if (waitingForPlayer) {

                if (entity instanceof Player) {
                    waitingForPlayer = false;
                    setChanged();
                    return;
                }
            } else if (entity instanceof LivingEntity) {
                if (isPrimed) {
                    landmine.explode(level, pos, entity);

                    if (entity instanceof Player player) player.awardStat(ModStats.MINES.get());
                }
                return;
            }
        }

        if (!isPrimed && !waitingForPlayer) {
            level.playSound(
                    null, pos, ModSounds.FSTBMB_START.get(), SoundSource.BLOCKS, 3.0F, 1.0F);
            isPrimed = true;
            setChanged();
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        isPrimed = input.getBooleanOr("primed", false);
        waitingForPlayer = input.getBooleanOr("waiting", false);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putBoolean("primed", isPrimed);
        output.putBoolean("waiting", waitingForPlayer);
    }
}
