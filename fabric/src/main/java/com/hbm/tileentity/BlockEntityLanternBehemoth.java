// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity;

import com.google.common.base.Suppliers;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.generic.BlockLanternBehemoth;
import com.hbm.entity.missile.EntityBobmazon;
import com.hbm.extprop.HbmPlayerProps;
import com.hbm.interfaces.IRepairable;
import com.hbm.inventory.OreDictManager;
import com.hbm.inventory.recipes.ingredient.CountIngredient;
import com.hbm.items.ModItems;
import com.hbm.items.special.ItemKitCustom;
import com.hbm.sound.ModSounds;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;

public final class BlockEntityLanternBehemoth extends BlockEntity implements IRepairable {

    private static final Supplier<List<CountIngredient>> REPAIR_MATERIALS =
            Suppliers.memoize(
                    () ->
                            List.of(
                                    CountIngredient.of(OreDictManager.STEEL.plate(), 2),
                                    CountIngredient.of(ModItems.CIRCUIT_BASIC.get(), 1)));

    private int communicationTimer = -1;
    private int repairReputation;

    public BlockEntityLanternBehemoth(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LANTERN_BEHEMOTH.get(), pos, state);
    }

    public void tickServer() {
        if (!(level instanceof ServerLevel server)) return;
        if (communicationTimer == 360) play(server, ModSounds.HORN_NEAR_SINGLE.get(), 10.0F);
        if (communicationTimer == 280) play(server, ModSounds.HORN_FAR_SINGLE.get(), 10_000.0F);
        if (communicationTimer == 220) play(server, ModSounds.HORN_NEAR_DUAL.get(), 10.0F);
        if (communicationTimer == 100) play(server, ModSounds.HORN_FAR_DUAL.get(), 10_000.0F);
        if (communicationTimer == 0) dispatchSupplies(server);
        if (communicationTimer >= 0) {
            communicationTimer--;
            setChanged();
        }
    }

    private void play(ServerLevel level, SoundEvent sound, float volume) {
        level.playSound(null, worldPosition, sound, SoundSource.BLOCKS, volume, 1.0F);
    }

    private void dispatchSupplies(ServerLevel level) {
        int x = worldPosition.getX();
        int y = worldPosition.getY();
        int z = worldPosition.getZ();
        boolean bonus = repairReputation >= 10;

        EntityBobmazon shuttle = new EntityBobmazon(level);
        shuttle.setPos(
                x + 0.5D + level.getRandom().nextGaussian() * 10.0D,
                300.0D,
                z + 0.5D + level.getRandom().nextGaussian() * 10.0D);
        shuttle.payload =
                ItemKitCustom.create(
                        "Supplies",
                        null,
                        0xFFFFFF,
                        0x008000,
                        new ItemStack(
                                ModItems.CIRCUIT_BASIC.get(), 4 + level.getRandom().nextInt(4)),
                        new ItemStack(
                                ModItems.CIRCUIT_ADVANCED.get(), 4 + level.getRandom().nextInt(2)),
                        new ItemStack(ModItems.COIN_TOKEN.get(), Math.max(1, repairReputation)),
                        bonus
                                ? new ItemStack(ModItems.GEM_ALEXANDRITE.get())
                                : new ItemStack(Items.DIAMOND, 6 + level.getRandom().nextInt(6)),
                        new ItemStack(Items.POPPY));
        level.addFreshEntity(shuttle);
    }

    @Override
    public boolean isDamaged() {
        return getBlockState().getValue(BlockLanternBehemoth.BROKEN);
    }

    @Override
    public List<CountIngredient> getRepairMaterials() {
        return REPAIR_MATERIALS.get();
    }

    @Override
    public void repair(Player player) {
        BlockState state = getBlockState();
        if (state.getValue(BlockLanternBehemoth.BROKEN)) {
            level.setBlock(
                    worldPosition,
                    state.setValue(BlockLanternBehemoth.BROKEN, false),
                    Block.UPDATE_CLIENTS);
        }
        communicationTimer = 400;
        repairReputation = HbmPlayerProps.getData(player).reputation;
        setChanged();
    }

    @Override
    public void tryExtinguish(
            net.minecraft.world.level.Level level, BlockPos pos, EnumExtinguishType type) {}

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (!(level instanceof ServerLevel server)) return;
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();
        for (Player player :
                server.getEntitiesOfClass(
                        Player.class, new AABB(x - 50, y - 50, z - 50, x + 51, y + 51, z + 51))) {
            HbmPlayerProps props = HbmPlayerProps.getData(player);
            if (props.reputation > -25) props.reputation--;
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        communicationTimer = input.getIntOr("comTimer", -1);
        repairReputation = input.getIntOr("reputation", 0);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("comTimer", communicationTimer);
        output.putInt("reputation", repairReputation);
    }
}
