// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.handler.neutron;

import com.hbm.blocks.machine.pile.BlockGraphiteDrilledBase;
import com.hbm.blocks.machine.pile.BlockGraphiteNeutronDetector;
import com.hbm.blocks.machine.pile.BlockGraphiteRod;
import com.hbm.handler.neutron.NeutronNodeWorld.StreamWorld;
import com.hbm.interfaces.IPileNeutronReceiver;
import com.hbm.lib.Library;
import com.hbm.tileentity.machine.pile.BlockEntityPileBase;
import com.hbm.util.ContaminationUtil;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class PileNeutronHandler {

    private static final TagKey<Block> FLUX_ABSORBER =
            TagKey.create(Registries.BLOCK, Library.id("pile/flux_absorber"));
    private static final TagKey<Block> FLUX_ATTENUATOR =
            TagKey.create(Registries.BLOCK, Library.id("pile/flux_attenuator"));
    public static int range = 5;

    public static PileNeutronNode makeNode(StreamWorld streamWorld, BlockEntityPileBase tile) {
        NeutronNode node = streamWorld.getNode(tile.getBlockPos());
        return node instanceof PileNeutronNode pile ? pile : new PileNeutronNode(tile);
    }

    private static @Nullable BlockEntity blockPosToTE(Level level, BlockPos pos) {
        return level.getBlockEntity(pos);
    }

    public static class PileNeutronNode extends NeutronNode {

        public PileNeutronNode(BlockEntityPileBase tile) {
            super(tile, NeutronStream.NeutronType.PILE);
        }
    }

    public static class PileNeutronStream extends NeutronStream {

        public PileNeutronStream(NeutronNode origin, Vec3 vector, double flux) {
            super(origin, vector, flux, 0D, NeutronType.PILE);
        }

        @Override
        public void runStreamInteraction(Level level, StreamWorld streamWorld) {

            BlockEntity originTE = origin.tile;
            BlockPos.MutableBlockPos pos = originTE.getBlockPos().mutable();

            for (float i = 1; i <= range; i += 0.5F) {

                BlockPos nodePos =
                        new BlockPos(
                                (int) Math.floor(pos.getX() + 0.5 + vector.x * i),
                                (int) Math.floor(pos.getY() + 0.5 + vector.y * i),
                                (int) Math.floor(pos.getZ() + 0.5 + vector.z * i));

                if (nodePos.equals(pos)) continue;

                pos.set(nodePos);

                BlockEntity tile;

                NeutronNode node = streamWorld.getNode(nodePos);
                if (node instanceof PileNeutronNode) {
                    tile = node.tile;
                } else {
                    tile = blockPosToTE(level, nodePos);
                    if (tile == null) return;

                    if (tile instanceof BlockEntityPileBase pileTile) {
                        streamWorld.addNode(new PileNeutronNode(pileTile));
                    }
                }

                BlockState state = tile.getBlockState();
                if (!(tile instanceof BlockEntityPileBase)) {

                    if (state.is(FLUX_ABSORBER)) return;
                    else if (state.is(FLUX_ATTENUATOR)) fluxQuantity *= 0.25;

                    if (state.getBlock() instanceof BlockGraphiteRod
                            && !state.getValue(BlockGraphiteDrilledBase.WITHDRAWN)) return;
                }

                if (tile instanceof IPileNeutronReceiver rec) {

                    rec.receiveNeutrons((int) Math.floor(fluxQuantity));

                    if (!(state.getBlock() instanceof BlockGraphiteNeutronDetector)
                            || !state.getValue(BlockGraphiteDrilledBase.WITHDRAWN)) return;
                }

                int x = (int) (nodePos.getX() + 0.5);
                int y = (int) (nodePos.getY() + 0.5);
                int z = (int) (nodePos.getZ() + 0.5);
                List<LivingEntity> entities =
                        level.getEntitiesOfClass(LivingEntity.class, new AABB(x, y, z, x, y, z));

                for (LivingEntity e : entities) {
                    ContaminationUtil.contaminate(
                            e,
                            ContaminationUtil.HazardType.RADIATION,
                            ContaminationUtil.ContaminationType.CREATIVE,
                            fluxQuantity / 4D);
                }
            }
        }
    }
}
