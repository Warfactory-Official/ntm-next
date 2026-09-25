// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.machine;

import com.hbm.blocks.ITickingBlock;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.capability.ICapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.interfaces.ILookOverlay;
import com.hbm.tileentity.machine.BlockEntityMachineTeleporter;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public class MachineTeleporter extends Block
        implements ITickingBlock, ILookOverlay, ICapabilityBlock {

    public MachineTeleporter(Properties props) {
        super(props);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityMachineTeleporter(pos, state);
    }

    @Override
    public void buildLookOverlay(Level level, BlockPos pos, LookInfo info) {
        if (!(level.getBlockEntity(pos) instanceof BlockEntityMachineTeleporter tele)) return;

        info.title(getName().getString(), 0xFFFF00, 0x404000);
        GlobalPos target = tele.target;
        if (target == null) {
            info.line(ChatFormatting.RED + "No destination set!");
            return;
        }
        info.line(
                (tele.power >= BlockEntityMachineTeleporter.CONSUMPTION
                                ? ChatFormatting.GREEN
                                : ChatFormatting.RED)
                        + String.format(Locale.US, "%,d", tele.power)
                        + " / "
                        + String.format(Locale.US, "%,d", BlockEntityMachineTeleporter.MAX_POWER));
        info.line(
                "Destination: "
                        + target.pos().getX()
                        + " / "
                        + target.pos().getY()
                        + " / "
                        + target.pos().getZ()
                        + " (D: "
                        + target.dimension().identifier()
                        + ")");
    }

    @Override
    public MachineCaps caps() {
        return MachineCaps.of(ModBlockEntities.TELEBLOCK).powerIn().fe();
    }
}
