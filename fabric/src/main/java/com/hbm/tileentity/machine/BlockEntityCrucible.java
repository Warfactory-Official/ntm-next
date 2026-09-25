// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: Contributors to Hbm's Nuclear Tech Mod
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine;

import com.hbm.api.block.ICrucibleAcceptor;
import com.hbm.api.control.IControlReceiver;
import com.hbm.api.tile.IHeatSource;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.capability.NtmContracts;
import com.hbm.data.MachineData;
import com.hbm.handler.pollution.PollutionHandler;
import com.hbm.handler.pollution.PollutionType;
import com.hbm.inventory.container.MenuCrucible;
import com.hbm.inventory.material.MaterialShapes;
import com.hbm.inventory.material.Mats.MaterialStack;
import com.hbm.inventory.material.Mats;
import com.hbm.inventory.material.NTMMaterial;
import com.hbm.inventory.recipes.CrucibleRecipe;
import com.hbm.inventory.recipes.CrucibleRecipes;
import com.hbm.items.machine.ItemScraps;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncList;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.particle.CoolingTowerParticleOptions;
import com.hbm.tileentity.BlockEntityMachineBase;
import com.hbm.tileentity.IMetalCopiable;
import com.hbm.util.CrucibleUtil;
import com.hbm.util.TickPhase;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class BlockEntityCrucible extends BlockEntityMachineBase
        implements MenuProvider,
                ICrucibleAcceptor,
                IControlReceiver,
                IMetalCopiable,
                SyncUnitSchema {

    public static final int SLOT_COUNT = 10;
    private static final int[] ACCESSIBLE_SLOTS = {1, 2, 3, 4, 5, 6, 7, 8, 9};

    @SyncField(units = 1L << 3)
    public final List<MaterialStack> recipeStack = new SyncList<>();

    @SyncField(units = 1L << 4)
    public final List<MaterialStack> wasteStack = new SyncList<>();

    public final List<PourStream> streams = new ArrayList<>();

    @SyncField(units = 1L << 1)
    public int heat;

    @SyncField(units = 1L << 0)
    public int progress;

    @SyncField(units = 1L << 2)
    public String recipe = "null";

    @SyncField(units = 1L << 5)
    private int recipePourColor = -1;

    @SyncField(units = 1L << 5)
    private float recipePourLen;

    @SyncField(units = 1L << 6)
    private int wastePourColor = -1;

    @SyncField(units = 1L << 6)
    private float wastePourLen;

    public BlockEntityCrucible(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CRUCIBLE.get(), pos, state, SLOT_COUNT);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.machineCrucible");
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public void tickServer() {
        this.recipePourColor = -1;
        this.wastePourColor = -1;

        tryPullHeat();

        int x = worldPosition.getX(), y = worldPosition.getY(), z = worldPosition.getZ();

        if (TickPhase.every(this, 5)) {
            List<ItemEntity> list =
                    level.getEntitiesOfClass(
                            ItemEntity.class,
                            new AABB(x - 0.5, y + 0.5, z - 0.5, x + 1.5, y + 1, z + 1.5));

            for (ItemEntity item : list) {
                if (item.isRemoved()) continue;
                ItemStack stack = item.getItem();
                if (!isItemSmeltable(stack)) continue;

                for (int i = 1; i < SLOT_COUNT; i++) {
                    if (!inventory.get(i).isEmpty()) continue;

                    if (stack.getCount() == 1) {
                        inventory.set(i, stack.copy());
                        item.discard();
                        setChanged();
                        break;
                    } else {
                        inventory.set(i, stack.copyWithCount(1));
                        stack.shrink(1);
                        item.setItem(stack);
                        setChanged();
                    }
                }
            }
        }

        int totalCap =
                MachineData.CRUCIBLE_RECIPE_CAPACITY.get()
                        + MachineData.CRUCIBLE_WASTE_CAPACITY.get();
        int totalMass = 0;
        for (MaterialStack stack : recipeStack) totalMass += stack.amount;
        for (MaterialStack stack : wasteStack) totalMass += stack.amount;

        double fill = ((double) totalMass / (double) totalCap) * 0.875D;

        List<LivingEntity> living =
                level.getEntitiesOfClass(
                        LivingEntity.class,
                        new AABB(x + 0.5, y + 0.5, z + 0.5, x + 0.5, y + 0.5 + fill, z + 0.5)
                                .inflate(1, 0, 1));
        for (LivingEntity entity : living) {
            entity.hurtServer((ServerLevel) level, level.damageSources().lava(), 5F);
            entity.igniteForSeconds(5);
        }

        if (!trySmelt()) {
            this.progress = 0;
        }

        tryRecipe();

        Direction facing = BlockMultiblockCore.coreFacing(getBlockState());

        if (!this.wasteStack.isEmpty()) {
            pourOut(facing.getOpposite(), this.wasteStack, true);
        }

        if (!this.recipeStack.isEmpty()) {
            CrucibleRecipe loaded = getLoadedRecipe();
            List<MaterialStack> toCast;

            if (loaded == null) {
                toCast = this.recipeStack;
            } else {
                toCast = new ArrayList<>();
                for (MaterialStack stack : this.recipeStack) {
                    for (MaterialStack output : loaded.output) {
                        if (stack.material == output.material) {
                            toCast.add(stack);
                            break;
                        }
                    }
                }
            }

            pourOut(facing, toCast, false);
        }

        this.recipeStack.removeIf(o -> o.amount <= 0);
        this.wasteStack.removeIf(o -> o.amount <= 0);

        networkPackNT(25);
    }

    private void pourOut(Direction dir, List<MaterialStack> stacks, boolean waste) {
        int x = worldPosition.getX(), y = worldPosition.getY(), z = worldPosition.getZ();
        CrucibleUtil.ImpactPos impact = new CrucibleUtil.ImpactPos();
        MaterialStack didPour =
                CrucibleUtil.pourFullStack(
                        level,
                        x + 0.5D + dir.getStepX() * 1.875D,
                        y + 0.25D,
                        z + 0.5D + dir.getStepZ() * 1.875D,
                        6,
                        true,
                        stacks,
                        MaterialShapes.NUGGET.q(3),
                        impact);

        if (didPour != null) {
            float len = Math.max(1F, y - (float) (Math.ceil(impact.y) - 0.875));
            if (waste) {
                this.wastePourColor = didPour.material.moltenColor;
                this.wastePourLen = len;
                markSyncEvent();
            } else {
                this.recipePourColor = didPour.material.moltenColor;
                this.recipePourLen = len;
                markSyncEvent();
            }
        }

        PollutionHandler.incrementPollution(
                level, worldPosition, PollutionType.SOOT, PollutionHandler.SOOT_PER_SECOND / 20F);
    }

    @Override
    public void tickClient() {
        long now = level.getGameTime();
        for (Iterator<PourStream> it = streams.iterator(); it.hasNext(); ) {
            if (now - it.next().birth() >= 20) it.remove();
        }

        if ((!this.recipeStack.isEmpty() || !this.wasteStack.isEmpty()) && now % 10 == 0) {

            CoolingTowerParticleOptions opts =
                    new CoolingTowerParticleOptions.Builder()
                            .setLift(10F)
                            .setBaseScale(0.75F)
                            .setMaxScale(3.5F)
                            .setLife(100 + level.getRandom().nextInt(20))
                            .setColor(0x202020)
                            .build();
            level.addParticle(
                    opts,
                    worldPosition.getX() + 0.5,
                    worldPosition.getY() + 1,
                    worldPosition.getZ() + 0.5,
                    0,
                    0,
                    0);
        }
    }

    private void writeRecipe(ByteBuf output) {
        ByteBufCodecs.STRING_UTF8.encode(output, recipe);
    }

    private void readRecipe(ByteBuf input) {
        recipe = ByteBufCodecs.STRING_UTF8.decode(input);
    }

    private void writeRecipeStack(ByteBuf output) {
        writeMaterials(output, recipeStack);
    }

    private void readRecipeStack(ByteBuf input) {
        readMaterials(input, recipeStack);
    }

    private void writeWasteStack(ByteBuf output) {
        writeMaterials(output, wasteStack);
    }

    private void readWasteStack(ByteBuf input) {
        readMaterials(input, wasteStack);
    }

    private static void writeMaterials(ByteBuf output, List<MaterialStack> materials) {
        output.writeShort(materials.size());
        for (MaterialStack material : materials) {
            output.writeInt(material.material.id);
            output.writeInt(material.amount);
        }
    }

    private static void readMaterials(ByteBuf input, List<MaterialStack> materials) {
        int count = input.readShort();
        if (count < 0 || count > input.readableBytes() / 8)
            throw new DecoderException("Invalid crucible material count");
        materials.clear();
        for (int i = 0; i < count; i++) {
            materials.add(new MaterialStack(Mats.matById.get(input.readInt()), input.readInt()));
        }
    }

    private void writeRecipePour(ByteBuf output) {
        output.writeInt(recipePourColor);
        output.writeFloat(recipePourLen);
    }

    private void readRecipePour(ByteBuf input) {
        recipePourColor = input.readInt();
        recipePourLen = input.readFloat();
    }

    private void writeWastePour(ByteBuf output) {
        output.writeInt(wastePourColor);
        output.writeFloat(wastePourLen);
    }

    private void readWastePour(ByteBuf input) {
        wastePourColor = input.readInt();
        wastePourLen = input.readFloat();
    }

    @Override
    public void afterSyncUnits(long units) {
        if (level == null || !level.isClientSide()) return;
        long now = level.getGameTime();
        if ((units & 1L << 5) != 0 && recipePourColor != -1) {
            streams.add(new PourStream(recipePourColor, false, recipePourLen, now));
        }
        if ((units & 1L << 6) != 0 && wastePourColor != -1) {
            streams.add(new PourStream(wastePourColor, true, wastePourLen, now));
        }
    }

    @Override
    public void afterInitialSyncUnits() {}

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);

        this.recipe = input.getStringOr("recipe", "null");

        int[] rec = input.getIntArray("rec").orElse(new int[0]);
        for (int i = 0; i < rec.length / 2; i++) {
            recipeStack.add(new MaterialStack(Mats.matById.get(rec[i * 2]), rec[i * 2 + 1]));
        }

        int[] was = input.getIntArray("was").orElse(new int[0]);
        for (int i = 0; i < was.length / 2; i++) {
            wasteStack.add(new MaterialStack(Mats.matById.get(was[i * 2]), was[i * 2 + 1]));
        }

        this.progress = input.getIntOr("progress", 0);
        this.heat = input.getIntOr("heat", 0);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);

        output.putString("recipe", this.recipe);

        int[] rec = new int[recipeStack.size() * 2];
        int[] was = new int[wasteStack.size() * 2];
        for (int i = 0; i < recipeStack.size(); i++) {
            MaterialStack sta = recipeStack.get(i);
            rec[i * 2] = sta.material.id;
            rec[i * 2 + 1] = sta.amount;
        }
        for (int i = 0; i < wasteStack.size(); i++) {
            MaterialStack sta = wasteStack.get(i);
            was[i * 2] = sta.material.id;
            was[i * 2 + 1] = sta.amount;
        }
        output.putIntArray("rec", rec);
        output.putIntArray("was", was);
        output.putInt("progress", progress);
        output.putInt("heat", heat);
    }

    protected void tryPullHeat() {
        int maxHeat = MachineData.CRUCIBLE_MAX_HEAT.get();
        if (this.heat >= maxHeat) return;

        BlockPos heatPos = worldPosition.below();
        IHeatSource source = NtmContracts.HEAT_SOURCE.at(level, heatPos);
        if (source != null) {
            int diff = source.getHeatStored(level, heatPos) - this.heat;
            if (diff == 0) return;

            diff = Math.min(diff, maxHeat - this.heat);

            if (diff > 0) {
                diff = (int) Math.ceil(diff * MachineData.CRUCIBLE_DIFFUSION.get());
                source.useUpHeat(level, heatPos, diff);
                this.heat += diff;
                if (this.heat > maxHeat) this.heat = maxHeat;
                return;
            }
        }

        this.heat = Math.max(this.heat - Math.max(this.heat / 1000, 1), 0);
    }

    protected boolean trySmelt() {
        if (this.heat < MachineData.CRUCIBLE_MAX_HEAT.get() / 2) return false;

        int slot = getFirstSmeltableSlot();
        if (slot == -1) return false;

        int delta = this.heat - (MachineData.CRUCIBLE_MAX_HEAT.get() / 2);
        delta *= 0.05;

        this.progress += delta;
        this.heat -= delta;

        if (this.progress >= MachineData.CRUCIBLE_PROCESS_TIME.get()) {
            this.progress = 0;

            List<MaterialStack> materials = Mats.getSmeltingMaterialsFromItem(inventory.get(slot));
            CrucibleRecipe loaded = getLoadedRecipe();

            for (MaterialStack material : materials) {
                boolean recipeMaterial =
                        loaded != null
                                && (getQuantaFromType(loaded.input, material.material) > 0
                                        || getQuantaFromType(loaded.output, material.material) > 0);

                if (recipeMaterial) {
                    addToStack(this.recipeStack, material);
                } else {
                    addToStack(this.wasteStack, material);
                }
            }

            removeItem(slot, 1);
        }

        return true;
    }

    protected void tryRecipe() {
        CrucibleRecipe loaded = getLoadedRecipe();

        if (loaded == null) return;
        if (!TickPhase.every(this, loaded.frequency)) return;

        for (MaterialStack stack : loaded.input) {
            if (getQuantaFromType(this.recipeStack, stack.material) < stack.amount) return;
        }

        for (MaterialStack stack : this.recipeStack) {
            stack.amount -= getQuantaFromType(loaded.input, stack.material);
        }

        outer:
        for (MaterialStack out : loaded.output) {
            for (MaterialStack stack : this.recipeStack) {
                if (stack.material == out.material) {
                    stack.amount += out.amount;
                    continue outer;
                }
            }
            this.recipeStack.add(out.copy());
        }
    }

    protected int getFirstSmeltableSlot() {
        for (int i = 1; i < SLOT_COUNT; i++) {
            ItemStack stack = inventory.get(i);
            if (!stack.isEmpty() && isItemSmeltable(stack)) return i;
        }
        return -1;
    }

    public boolean isItemSmeltable(ItemStack stack) {
        List<MaterialStack> materials = Mats.getSmeltingMaterialsFromItem(stack);

        if (materials.isEmpty()) return false;
        CrucibleRecipe loaded = getLoadedRecipe();

        boolean matchesRecipe = loaded == null;

        int recipeContent = loaded != null ? loaded.getInputAmount() : 0;

        int recipeAmount = getQuantaFromType(this.recipeStack, null);
        int wasteAmount = getQuantaFromType(this.wasteStack, null);

        for (MaterialStack mat : materials) {

            int recipeInputRequired =
                    loaded != null ? getQuantaFromType(loaded.input, mat.material) : 0;

            if (loaded != null && getQuantaFromType(loaded.output, mat.material) > 0) {
                recipeAmount += mat.amount;
                matchesRecipe = true;
                continue;
            }

            if (recipeInputRequired == 0) {
                wasteAmount += mat.amount;
            } else {

                int matMaximum =
                        recipeInputRequired
                                * MachineData.CRUCIBLE_RECIPE_CAPACITY.get()
                                / recipeContent;
                int amountStored = getQuantaFromType(recipeStack, mat.material);

                matchesRecipe = true;
                recipeAmount += mat.amount;

                if (amountStored + mat.amount > matMaximum) return false;
            }
        }

        return recipeAmount <= MachineData.CRUCIBLE_RECIPE_CAPACITY.get()
                && wasteAmount <= MachineData.CRUCIBLE_WASTE_CAPACITY.get()
                && matchesRecipe;
    }

    public void addToStack(List<MaterialStack> stack, MaterialStack matStack) {
        for (MaterialStack mat : stack) {
            if (mat.material == matStack.material) {
                mat.amount += matStack.amount;
                return;
            }
        }
        stack.add(matStack.copy());
    }

    public @Nullable CrucibleRecipe getLoadedRecipe() {
        return CrucibleRecipes.INSTANCE.getRecipe(recipe);
    }

    public int getQuantaFromType(MaterialStack[] stacks, @Nullable NTMMaterial mat) {
        for (MaterialStack stack : stacks) {
            if (mat == null || stack.material == mat) return stack.amount;
        }
        return 0;
    }

    public int getQuantaFromType(List<MaterialStack> stacks, @Nullable NTMMaterial mat) {
        int sum = 0;
        for (MaterialStack stack : stacks) {
            if (stack.material == mat) return stack.amount;
            if (mat == null) sum += stack.amount;
        }
        return sum;
    }

    private List<ItemStack> takeMelt() {
        List<ItemStack> scraps = new ArrayList<>();
        for (MaterialStack stack : recipeStack) scraps.add(ItemScraps.create(stack));
        for (MaterialStack stack : wasteStack) scraps.add(ItemScraps.create(stack));
        recipeStack.clear();
        wasteStack.clear();
        setChanged();
        return scraps;
    }

    public void scoopMelt(Player player) {
        for (ItemStack scrap : takeMelt()) player.getInventory().placeItemBackInInventory(scrap);
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {

        if (level != null && !level.isClientSide()) {
            for (ItemStack scrap : takeMelt()) {
                level.addFreshEntity(
                        new ItemEntity(
                                level,
                                pos.getX() + 0.5,
                                pos.getY() + 0.5,
                                pos.getZ() + 0.5,
                                scrap));
            }
        }
        super.preRemoveSideEffects(pos, state);
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return ACCESSIBLE_SLOTS;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return isItemSmeltable(stack);
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot >= 1 && canPlaceItem(slot, stack);
    }

    @Override
    public boolean canAcceptPartialPour(
            Level level, BlockPos pos, Vec3 hit, Direction side, MaterialStack stack) {
        CrucibleRecipe loaded = getLoadedRecipe();

        if (loaded == null) {
            return getQuantaFromType(this.wasteStack, null)
                    < MachineData.CRUCIBLE_WASTE_CAPACITY.get();
        }

        int recipeContent = loaded.getInputAmount();
        int recipeInputRequired = getQuantaFromType(loaded.input, stack.material);
        int matMaximum =
                recipeInputRequired * MachineData.CRUCIBLE_RECIPE_CAPACITY.get() / recipeContent;
        int amountStored = getQuantaFromType(recipeStack, stack.material);

        return amountStored < matMaximum
                && getQuantaFromType(this.recipeStack, null)
                        < MachineData.CRUCIBLE_RECIPE_CAPACITY.get();
    }

    @Override
    public MaterialStack pour(
            Level level, BlockPos pos, Vec3 hit, Direction side, MaterialStack stack) {
        CrucibleRecipe loaded = getLoadedRecipe();

        if (loaded == null) {
            int amount = getQuantaFromType(this.wasteStack, null);

            if (amount + stack.amount <= MachineData.CRUCIBLE_WASTE_CAPACITY.get()) {
                addToStack(this.wasteStack, stack.copy());
                return null;
            } else {
                int toAdd = MachineData.CRUCIBLE_WASTE_CAPACITY.get() - amount;
                addToStack(this.wasteStack, new MaterialStack(stack.material, toAdd));
                return new MaterialStack(stack.material, stack.amount - toAdd);
            }
        }

        int recipeContent = loaded.getInputAmount();
        int recipeInputRequired = getQuantaFromType(loaded.input, stack.material);
        int matMaximum =
                recipeInputRequired * MachineData.CRUCIBLE_RECIPE_CAPACITY.get() / recipeContent;

        if (recipeInputRequired + stack.amount <= matMaximum) {
            addToStack(this.recipeStack, stack.copy());
            return null;
        }

        int toAdd = matMaximum - stack.amount;
        toAdd =
                Math.min(
                        toAdd,
                        MachineData.CRUCIBLE_RECIPE_CAPACITY.get()
                                - getQuantaFromType(this.recipeStack, null));
        addToStack(this.recipeStack, new MaterialStack(stack.material, toAdd));
        return new MaterialStack(stack.material, stack.amount - toAdd);
    }

    @Override
    public boolean canAcceptPartialFlow(
            Level level, BlockPos pos, Direction side, MaterialStack stack) {
        return false;
    }

    @Override
    public MaterialStack flow(Level level, BlockPos pos, Direction side, MaterialStack stack) {
        return null;
    }

    @Override
    public int[] getMatsToCopy() {
        int[] ids = new int[recipeStack.size() + wasteStack.size()];
        int i = 0;
        for (MaterialStack stack : recipeStack) ids[i++] = stack.material.id;
        for (MaterialStack stack : wasteStack) ids[i++] = stack.material.id;
        return ids;
    }

    @Override
    public boolean hasPermission(Player player) {
        return stillValid(player);
    }

    @Override
    public void receiveControl(CompoundTag data) {
        if (data.contains("index") && data.contains("selection")) {
            if (data.getIntOr("index", 0) != 0) return;
            this.recipe = data.getStringOr("selection", "null");
            setChanged();
        }
    }

    @Override
    public Component getDisplayName() {
        return getName();
    }

    @Override
    public AbstractContainerMenu createMenu(
            int containerId, Inventory playerInventory, Player player) {
        return new MenuCrucible(containerId, playerInventory, this);
    }

    public record PourStream(int color, boolean waste, float len, long birth) {}

    @Override
    public long syncUnitMask() {
        return 0x7fL;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeInt(this.progress);
            case 1 -> output.writeInt(this.heat);
            case 2 -> writeRecipe(output);
            case 3 -> writeRecipeStack(output);
            case 4 -> writeWasteStack(output);
            case 5 -> writeRecipePour(output);
            case 6 -> writeWastePour(output);
            default -> throw new IllegalArgumentException();
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> this.progress = input.readInt();
            case 1 -> this.heat = input.readInt();
            case 2 -> readRecipe(input);
            case 3 -> readRecipeStack(input);
            case 4 -> readWasteStack(input);
            case 5 -> readRecipePour(input);
            case 6 -> readWastePour(input);
            default -> throw new IllegalArgumentException();
        }
    }

    @Override
    public long syncEventUnits() {
        return 0x60L;
    }
}
