// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.bomb;

import com.hbm.blocks.ITickingBlock;
import com.hbm.config.BombConfig;
import com.hbm.data.ExplosionData;
import com.hbm.entity.ModEntities;
import com.hbm.entity.logic.EntityBalefire;
import com.hbm.entity.logic.EntityNukeExplosionMK5;
import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.explosion.vanillant.standard.BlockAllocatorStandard;
import com.hbm.explosion.vanillant.standard.BlockProcessorStandard;
import com.hbm.explosion.vanillant.standard.EntityProcessorCross;
import com.hbm.explosion.vanillant.standard.PlayerProcessorStandard;
import com.hbm.handler.threading.TargetPoint;
import com.hbm.interfaces.IBomb;
import com.hbm.interfaces.IToolable;
import com.hbm.inventory.material.Mats;
import com.hbm.items.ModItems;
import com.hbm.main.Polaroid;
import com.hbm.packet.toclient.MukePayload;
import com.hbm.particle.helper.ExplosionCreator;
import com.hbm.platform.Services;
import com.hbm.sound.ModSounds;
import com.hbm.tileentity.bomb.TileEntityCrashedBomb;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class BlockCrashedBomb extends Block implements ITickingBlock, IToolable, IBomb {
    public final EnumDudType type;
    private final MapCodec<BlockCrashedBomb> codec;

    public BlockCrashedBomb(Properties props, EnumDudType type) {
        super(props);
        this.type = type;
        this.codec = simpleCodec(p -> new BlockCrashedBomb(p, type));
    }

    private static void dropItems(Level level, BlockPos pos, ItemStack... drops) {
        for (ItemStack drop : drops) {
            level.addFreshEntity(
                    new ItemEntity(
                            level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, drop));
        }
    }

    private static void spawnMush(
            ServerLevel server, double x, double y, double z, boolean balefire) {
        server.playSound(
                null,
                x,
                y,
                z,
                ModSounds.GUN_MINI_NUKE_EXPLOSION.get(),
                SoundSource.BLOCKS,
                15.0F,
                1.0F);
        Services.NETWORK.sendToAllAround(
                new MukePayload(x, y, z, false, balefire), new TargetPoint(server, x, y, z, 250));
    }

    @Override
    protected MapCodec<BlockCrashedBomb> codec() {
        return codec;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityCrashedBomb(pos, state);
    }

    @Override
    public boolean onScrew(
            Level level,
            Player player,
            BlockPos pos,
            @Nullable Direction side,
            Vec3 hit,
            ToolType tool) {
        if (tool != ToolType.DEFUSER) return false;
        if (level.isClientSide()) return true;

        switch (type) {
            case BALEFIRE -> dropItems(level, pos, new ItemStack(ModItems.EGG_BALEFIRE_SHARD));
            case CONVENTIONAL -> dropItems(level, pos, new ItemStack(ModItems.BALL_TNT.get(), 16));
            case NUKE ->
                    dropItems(
                            level,
                            pos,
                            new ItemStack(ModItems.BALL_TNT.get(), 8),
                            new ItemStack(ModItems.billet(Mats.MAT_PLUTONIUM), 4));
            case SALTED ->
                    dropItems(
                            level,
                            pos,
                            new ItemStack(ModItems.BALL_TNT.get(), 8),
                            new ItemStack(ModItems.billet(Mats.MAT_PLUTONIUM), 2),
                            new ItemStack(ModItems.ingot(Mats.MAT_COBALT), 12));
        }

        level.destroyBlock(pos, false);
        return true;
    }

    @Override
    public BombReturnCode explode(Level level, BlockPos pos, @Nullable Entity detonator) {
        if (level.isClientSide()) return BombReturnCode.DETONATED;
        ServerLevel server = (ServerLevel) level;
        server.removeBlock(pos, false);

        switch (type) {
            case BALEFIRE -> {
                EntityBalefire bf = new EntityBalefire(ModEntities.BALEFIRE.get(), server);

                bf.setPos(pos.getX(), pos.getY(), pos.getZ());
                bf.destructionRange = (int) (ExplosionData.FATMAN_RADIUS.get() * 1.25);
                server.addFreshEntity(bf);
                spawnMush(server, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, true);
            }
            case CONVENTIONAL -> {
                double x = pos.getX() + 0.5, y = pos.getY() + 0.5, z = pos.getZ() + 0.5;
                ExplosionVNT xnt = new ExplosionVNT(server, x, y, z, 35F);
                xnt.setBlockAllocator(new BlockAllocatorStandard(24));
                xnt.setBlockProcessor(new BlockProcessorStandard().setNoDrop());
                xnt.setEntityProcessor(new EntityProcessorCross(5D).withRangeMod(1.5F));
                xnt.setPlayerProcessor(new PlayerProcessorStandard());
                xnt.explode();
                ExplosionCreator.composeEffectLarge(server, x, y, z);
            }
            case NUKE -> {
                double x = pos.getX() + 0.5, y = pos.getY() + 0.5, z = pos.getZ() + 0.5;
                server.addFreshEntity(EntityNukeExplosionMK5.statFac(server, 35, x, y, z));
                spawnMush(
                        server,
                        x,
                        y,
                        z,
                        Polaroid.isBalefireDay() || server.getRandom().nextInt(100) == 0);
            }
            case SALTED -> {
                double x = pos.getX() + 0.5, y = pos.getY() + 0.5, z = pos.getZ() + 0.5;
                server.addFreshEntity(
                        EntityNukeExplosionMK5.statFac(server, 25, x, y, z).moreFallout(25));
                spawnMush(
                        server,
                        x,
                        y,
                        z,
                        Polaroid.isBalefireDay() || server.getRandom().nextInt(100) == 0);
            }
        }

        return BombReturnCode.DETONATED;
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return InteractionResult.PASS;
    }

    public enum EnumDudType {
        BALEFIRE,
        CONVENTIONAL,
        NUKE,
        SALTED
    }
}
