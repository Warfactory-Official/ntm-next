// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine;

import com.hbm.api.tile.IHeatSource;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.capability.ContractLink;
import com.hbm.capability.NtmContracts;
import com.hbm.entity.projectile.EntitySawblade;
import com.hbm.handler.threading.TargetPoint;
import com.hbm.inventory.OreDictManager;
import com.hbm.items.ModDataComponents;
import com.hbm.items.ModItems;
import com.hbm.lib.ModDamageTypes;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncSlots;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.packet.toclient.BlockDustBurstPayload;
import com.hbm.platform.Services;
import com.hbm.sound.ModSounds;
import com.hbm.tileentity.BlockEntityMachineBase;
import com.hbm.tileentity.NeighborDerived;
import com.hbm.tileentity.PersistentDrop;
import com.hbm.util.Facing;
import io.netty.buffer.ByteBuf;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

@SyncSlots(all = true, components = false, units = 1L << 3)
public class BlockEntitySawmill extends BlockEntityMachineBase
        implements PersistentDrop, SyncUnitSchema {

    public static final double DIFFUSION = 0.1D;
    public static final int PROCESSING_TIME = 600;
    private static final int[] SLOTS = {0, 1, 2};
    private static final String[] PERSISTENT_KEYS = {"hasBlade"};

    @NeighborDerived(at = "below")
    private final ContractLink<IHeatSource> heatBelow =
            new ContractLink<>(NtmContracts.HEAT_SOURCE);

    private final RecipeManager.CachedCheck<CraftingInput, CraftingRecipe> craftingCheck =
            RecipeManager.createCheck(RecipeType.CRAFTING);

    @SyncField(units = 1L << 0)
    public int heat;

    @SyncField(units = 1L << 2)
    public boolean hasBlade = true;

    @SyncField(units = 1L << 1)
    public int progress;

    public float spin, lastSpin;
    private int warnCooldown;
    private int overspeed;

    public BlockEntitySawmill(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SAWMILL.get(), pos, state, 3);
    }

    public static float sawdustChance(ItemStack result) {
        if (result.is(ModItems.POWDER_SAWDUST.get())) return 0F;
        return result.is(Items.STICK) ? 0.1F : 0.5F;
    }

    @Override
    public void tickServer() {
        ServerLevel level = (ServerLevel) this.level;

        if (hasBlade) {
            tryPullHeat();
            if (warnCooldown > 0) warnCooldown--;

            if (heat >= 100) {
                ItemStack result = getOutput(inventory.get(0));
                if (result != null) {
                    progress += heat / 10;
                    if (progress >= PROCESSING_TIME) {
                        progress = 0;
                        inventory.set(0, ItemStack.EMPTY);
                        inventory.set(1, result);
                        float chance = sawdustChance(result);
                        if (chance > 0F && level.getRandom().nextFloat() < chance)
                            inventory.set(2, new ItemStack(ModItems.POWDER_SAWDUST));
                        setChanged();
                    }
                } else {
                    progress = 0;
                }

                for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, dangerBox())) {
                    if (e.isAlive()
                            && e.hurtServer(
                                    level,
                                    level.damageSources().source(ModDamageTypes.BLENDER),
                                    100)) {
                        level.playSound(
                                null,
                                e.getX(),
                                e.getY(),
                                e.getZ(),
                                SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR,
                                SoundSource.BLOCKS,
                                2.0F,
                                0.95F + level.getRandom().nextFloat() * 0.2F);
                        int count = Math.min((int) Math.ceil(e.getMaxHealth() / 4), 250) * 4;
                        Services.NETWORK.sendToAllAround(
                                new BlockDustBurstPayload(
                                        Blocks.REDSTONE_BLOCK.defaultBlockState(),
                                        e.getX(),
                                        e.getY() + e.getBbHeight() * 0.5,
                                        e.getZ(),
                                        count,
                                        0.1D),
                                new TargetPoint(level, e.getX(), e.getY(), e.getZ(), 50));
                    }
                }
            } else {
                progress = 0;
            }

            if (heat > 300) {
                overspeed++;
                if (overspeed > 60 && warnCooldown == 0) {
                    warnCooldown = 100;
                    level.playSound(
                            null,
                            worldPosition.getX() + 0.5,
                            worldPosition.getY() + 1,
                            worldPosition.getZ() + 0.5,
                            ModSounds.WARN_OVERSPEED.get(),
                            SoundSource.BLOCKS,
                            2.0F,
                            1.0F);
                }
                if (overspeed > 300) {
                    hasBlade = false;

                    level.explode(
                            null,
                            worldPosition.getX() + 0.5,
                            worldPosition.getY() + 1,
                            worldPosition.getZ() + 0.5,
                            5F,
                            false,
                            Level.ExplosionInteraction.NONE);
                    throwBlade(level);
                    setChanged();
                }
            } else {
                overspeed = 0;
            }
        } else {
            overspeed = 0;
            warnCooldown = 0;
        }

        networkPackNT(150);
        heat = 0;
    }

    @Override
    public void tickClient() {
        float momentum = heat * 25F / 300F;
        lastSpin = spin;
        spin += momentum;
        if (spin >= 360F) {
            spin -= 360F;
            lastSpin -= 360F;
        }
    }

    private void throwBlade(ServerLevel level) {
        Direction dir = BlockMultiblockCore.coreFacing(getBlockState());
        EntitySawblade cog =
                new EntitySawblade(
                                level,
                                worldPosition.getX() + 0.5 + dir.getStepX(),
                                worldPosition.getY() + 1,
                                worldPosition.getZ() + 0.5 + dir.getStepZ())
                        .setOrientation(dir.get3DDataValue());
        Direction rot = Facing.rotate(dir, Direction.DOWN);
        cog.setDeltaMovement(rot.getStepX(), 1 + (heat - 100) * 0.0001D, rot.getStepZ());
        level.addFreshEntity(cog);
    }

    private void tryPullHeat() {
        BlockPos heatPos = worldPosition.below();
        IHeatSource source = heatBelow.get(level, heatPos);
        if (source != null) {
            int heatSrc = (int) (source.getHeatStored(level, heatPos) * DIFFUSION);
            if (heatSrc > 0) {
                source.useUpHeat(level, heatPos, heatSrc);
                this.heat += heatSrc;
                return;
            }
        }
        this.heat = Math.max(this.heat - Math.max(this.heat / 1000, 1), 0);
    }

    public ItemStack getOutput(ItemStack input) {
        if (input.isEmpty()) return null;
        if (input.is(OreDictManager.KEY_STICK)) return new ItemStack(ModItems.POWDER_SAWDUST);
        if (input.is(ItemTags.LOGS)) {
            if (!(level instanceof ServerLevel server)) return null;
            CraftingInput ci = CraftingInput.of(1, 1, List.of(input.copyWithCount(1)));
            Optional<RecipeHolder<CraftingRecipe>> r = craftingCheck.getRecipeFor(ci, server);
            if (r.isPresent()) {
                ItemStack out = r.get().value().assemble(ci).copy();
                if (!out.isEmpty()) {
                    out.setCount(out.getCount() * 6 / 4);
                    return out;
                }
            }
            return null;
        }
        if (input.is(ItemTags.PLANKS)) return new ItemStack(Items.STICK, 6);
        if (input.is(ItemTags.SAPLINGS)) return new ItemStack(Items.STICK, 1);
        return null;
    }

    private AABB dangerBox() {
        Direction dir = BlockMultiblockCore.coreFacing(getBlockState());
        Direction rot = dir.getClockWise();
        AABB local = new AABB(-1D, 0.375D, -1D, -0.875D, 2.375D, 1D);
        return Facing.rotateAabb(local, rot)
                .move(worldPosition.getX() + 0.5, worldPosition.getY(), worldPosition.getZ() + 0.5);
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot == 0
                && inventory.get(0).isEmpty()
                && inventory.get(1).isEmpty()
                && inventory.get(2).isEmpty()
                && stack.getCount() == 1
                && getOutput(stack) != null;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return SLOTS;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot > 0;
    }

    @Override
    public void writePersistent(DataComponentMap.Builder components) {
        if (!hasBlade) components.set(ModDataComponents.HAS_BLADE.get(), false);
    }

    @Override
    public void readPersistent(DataComponentGetter components) {
        hasBlade = components.getOrDefault(ModDataComponents.HAS_BLADE.get(), true);
    }

    @Override
    public String[] persistentKeys() {
        return PERSISTENT_KEYS;
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        hasBlade = input.getBooleanOr("hasBlade", hasBlade);
        progress = input.getIntOr("progress", progress);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putBoolean("hasBlade", hasBlade);
        output.putInt("progress", progress);
    }

    private void writeStacks(ByteBuf output) {
        for (ItemStack slot : inventory) {
            output.writeInt(BuiltInRegistries.ITEM.getId(slot.getItem()));
            output.writeInt(slot.getCount());
        }
    }

    private void readStacks(ByteBuf input) {
        for (int i = 0; i < inventory.size(); i++) {
            int id = input.readInt();
            int count = input.readInt();
            inventory.set(
                    i,
                    count <= 0
                            ? ItemStack.EMPTY
                            : new ItemStack(BuiltInRegistries.ITEM.byId(id), count));
        }
    }

    @Override
    public long syncUnitMask() {
        return 0xfL;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeInt(this.heat);
            case 1 -> output.writeInt(this.progress);
            case 2 -> output.writeBoolean(this.hasBlade);
            case 3 -> writeStacks(output);
            default -> throw new IllegalArgumentException();
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> this.heat = input.readInt();
            case 1 -> this.progress = input.readInt();
            case 2 -> this.hasBlade = input.readBoolean();
            case 3 -> readStacks(input);
            default -> throw new IllegalArgumentException();
        }
    }
}
