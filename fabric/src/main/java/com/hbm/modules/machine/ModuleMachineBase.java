// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.modules.machine;

import com.hbm.api.energymk2.IEnergyHandlerMK2;
import com.hbm.inventory.fluid.FluidStackNTM;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.recipes.ingredient.CountIngredient;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.inventory.recipes.loader.GenericRecipes;
import com.hbm.items.machine.ItemBlueprints;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncSource;
import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.random.Weighted;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public class ModuleMachineBase implements SyncSource {

    private static final int[] EMPTY_INT = new int[0];

    public final int index;
    protected final GenericRecipes<?, ?> recipeSet;
    protected final Container inventory;
    protected final int[] inputSlots;
    protected final int[] outputSlots;
    protected final FluidTankNTM[] inputTanks;
    protected final FluidTankNTM[] outputTanks;

    private final @Nullable IEnergyHandlerMK2 battery;

    @SyncField protected String recipe = "";

    @SyncField public double progress;
    @SyncField public boolean restrictedMode;
    public boolean didProcess;
    public boolean markDirty;

    public ModuleMachineBase(
            int index,
            @Nullable IEnergyHandlerMK2 battery,
            GenericRecipes<?, ?> recipeSet,
            Container inventory,
            int[] inputSlots,
            int[] outputSlots,
            FluidTankNTM[] inputTanks,
            FluidTankNTM[] outputTanks) {
        this.index = index;
        this.battery = battery;
        this.recipeSet = recipeSet;
        this.inventory = inventory;
        this.inputSlots = inputSlots;
        this.outputSlots = outputSlots;
        this.inputTanks = inputTanks;
        this.outputTanks = outputTanks;
    }

    public ModuleMachineBase(
            GenericRecipes<?, ?> recipeSet,
            Container inventory,
            int[] inputSlots,
            int[] outputSlots,
            FluidTankNTM[] inputTanks,
            FluidTankNTM[] outputTanks) {
        this(0, null, recipeSet, inventory, inputSlots, outputSlots, inputTanks, outputTanks);
    }

    private static int[] trim(int[] array, int length) {
        if (length == array.length) return array;
        int[] out = new int[length];
        System.arraycopy(array, 0, out, 0, length);
        return out;
    }

    protected static long consumption(GenericRecipe recipe, double power) {
        return power == 1D ? recipe.power : (long) (recipe.power * power);
    }

    protected static void sizeTanks(FluidTankNTM[] tanks, FluidStackNTM[] fluids, int floor) {
        for (int i = 0; i < tanks.length && i < fluids.length; i++) {
            tanks[i].changeTankSize(
                    (int) Math.max(Math.max(tanks[i].getFill(), fluids[i].amount() * 2L), floor));
        }
    }

    private static void conform(FluidTankNTM[] tanks, FluidStackNTM @Nullable [] fluids) {
        for (int i = 0; i < tanks.length; i++) {
            if (fluids != null && fluids.length > i) tanks[i].conform(fluids[i]);
            else tanks[i].resetTank();
        }
    }

    protected IEnergyHandlerMK2 battery() {
        return battery;
    }

    public String getRecipeName() {
        return recipe;
    }

    public String legacyRecipeName() {
        return recipe.isEmpty() ? "null" : recipe;
    }

    public void setRecipe(String name) {
        setRecipe(name, false);
    }

    public void setRecipe(String name, boolean ror) {
        this.recipe = "null".equals(name) ? "" : name;
        this.restrictedMode = ror;
    }

    public @Nullable GenericRecipe getRecipe() {
        return recipeSet.getRecipe(recipe);
    }

    public void update(double speed, double power, boolean extraCondition, ItemStack blueprint) {
        GenericRecipe selected = getRecipe();

        if (selected != null
                && selected.isPooled()
                && !selected.isPartOfPool(ItemBlueprints.grabPool(blueprint))) {
            didProcess = false;
            progress = 0D;
            recipe = "";
            return;
        }

        setupTanks(selected);

        didProcess = false;
        markDirty = false;

        Match match = extraCondition ? canProcess(selected, power) : null;
        if (match != null) {
            process(match, speed, power);
            didProcess = true;
        } else {
            progress = 0D;
        }
    }

    public void setupTanks(@Nullable GenericRecipe recipe) {
        if (recipe == null) return;
        conform(inputTanks, recipe.inputFluid);
        conform(outputTanks, recipe.outputFluid);
    }

    public @Nullable Match canProcess(@Nullable GenericRecipe recipe, double power) {
        if (recipe == null) return null;

        String switched = autoSwitchTarget(recipe, this.recipe);
        if (switched != null) {
            this.recipe = switched;
            return null;
        }

        if (battery.getPower() < consumption(recipe, power)) return null;

        Match match = match(recipe);
        if (match == null) return null;
        if (!hasInputFluids(recipe)) return null;
        return canFitOutput(recipe) ? match : null;
    }

    protected boolean hasInputFluids(GenericRecipe recipe) {
        return recipe.matchesInputFluids(inputTanks);
    }

    public void process(Match match, double speed, double power) {
        if (restrictedMode) speed *= 0.25D;
        GenericRecipe recipe = match.recipe;
        battery.setPower(battery.getPower() - consumption(recipe, power));
        progress += Math.min(speed / recipe.duration, 1D);

        if (progress >= 1D) {
            craft(match);
            progress = canProcess(recipe, power) != null ? progress - 1D : 0D;
        }
    }

    public void craft(Match match) {
        consumeInput(match);
        produceItem(match.recipe);
    }

    public Match findMatch() {
        for (GenericRecipe recipe : recipeSet.recipes()) {
            Match match = match(recipe);
            if (match == null) continue;
            if (!hasInputFluids(recipe)) continue;
            if (!canFitOutput(recipe)) continue;
            return match;
        }
        return null;
    }

    public Match match(GenericRecipe recipe) {
        CountIngredient[] ingredients = recipe.inputItem;
        if (ingredients == null || ingredients.length == 0) {
            return new Match(recipe, EMPTY_INT, EMPTY_INT);
        }

        boolean[] used = new boolean[inputSlots.length];

        int[] debitSlot = new int[ingredients.length * inputSlots.length];
        int[] debitCount = new int[debitSlot.length];
        int debits = 0;

        for (CountIngredient ingredient : ingredients) {
            int needed = ingredient.count();
            for (int s = 0; s < inputSlots.length && needed > 0; s++) {
                if (used[s]) continue;
                ItemStack stack = inventory.getItem(inputSlots[s]);
                if (stack.isEmpty() || !ingredient.matchesItem(stack)) continue;
                int take = Math.min(needed, stack.getCount());
                used[s] = true;
                debitSlot[debits] = inputSlots[s];
                debitCount[debits] = take;
                debits++;
                needed -= take;
            }
            if (needed > 0) return null;
        }

        return new Match(recipe, trim(debitSlot, debits), trim(debitCount, debits));
    }

    public boolean canFitOutput(GenericRecipe recipe) {
        return canFitOutputItems(recipe) && recipe.outputFluidsFit(outputTanks);
    }

    private boolean canFitOutputItems(GenericRecipe recipe) {
        WeightedList<ItemStack>[] outputs = recipe.outputItems();
        if (outputs == null) return true;
        int n = Math.min(outputs.length, outputSlots.length);
        for (int i = 0; i < n; i++) {
            ItemStack slot = inventory.getItem(outputSlots[i]);
            if (slot.isEmpty()) continue;
            ItemStack single = single(outputs[i]);
            if (single == null) return false;
            if (single.isEmpty()) continue;
            if (!ItemStack.isSameItemSameComponents(slot, single)) return false;
            if (slot.getCount() + single.getCount() > slot.getMaxStackSize()) return false;
        }
        return true;
    }

    private static @Nullable ItemStack single(WeightedList<ItemStack> out) {
        List<Weighted<ItemStack>> entries = out.unwrap();
        if (entries.size() == 1) return entries.getFirst().value();
        if (entries.size() != 2) return null;
        ItemStack first = entries.get(0).value();
        ItemStack second = entries.get(1).value();
        if (first.isEmpty() == second.isEmpty()) return null;
        return first.isEmpty() ? second : first;
    }

    private static ItemStack roll(WeightedList<ItemStack> out) {
        List<Weighted<ItemStack>> entries = out.unwrap();
        ItemStack single = single(out);
        if (entries.size() != 2 || single == null)
            return out.getRandom(GenericRecipes.RNG).orElse(ItemStack.EMPTY);
        int total = entries.get(0).weight() + entries.get(1).weight();
        int weight =
                entries.get(0).value().isEmpty()
                        ? entries.get(1).weight()
                        : entries.get(0).weight();
        float chance = (float) weight / (float) total;
        int count = 0;
        for (int k = 0; k < single.getCount(); k++) {
            if (GenericRecipes.RNG.nextFloat() <= chance) count++;
        }
        return count == 0 ? ItemStack.EMPTY : single.copyWithCount(count);
    }

    public boolean isItemValid(int slot, ItemStack stack) {
        GenericRecipe recipe = getRecipe();
        if (recipe == null || recipe.inputItem == null) return false;

        for (int i = 0; i < Math.min(inputSlots.length, recipe.inputItem.length); i++) {
            if (inputSlots[i] == slot && recipe.inputItem[i].matchesItem(stack)) return true;
        }

        if (recipe.autoSwitchGroup != null && isFirstInputSlot(slot)) {
            for (GenericRecipe sibling : recipeSet.autoSwitchGroup(recipe.autoSwitchGroup)) {
                if (sibling.inputItem == null || sibling.inputItem.length == 0) continue;
                if (sibling.inputItem[0].matchesItem(stack)) return true;
            }
        }

        return false;
    }

    public boolean isSlotClogged(int slot) {
        if (!isInputSlot(slot)) return false;
        ItemStack stack = inventory.getItem(slot);
        if (stack.isEmpty()) return false;
        return !isItemValid(slot, stack);
    }

    private @Nullable String autoSwitchTarget(GenericRecipe recipe, String currentName) {
        if (recipe.autoSwitchGroup == null || inputSlots.length == 0) return null;

        ItemStack switchBy = inventory.getItem(inputSlots[0]);
        if (switchBy.isEmpty()) return null;

        for (GenericRecipe sibling : recipeSet.autoSwitchGroup(recipe.autoSwitchGroup)) {
            if (sibling.getInternalName().equals(currentName)) continue;
            if (sibling.inputItem == null || sibling.inputItem.length == 0) continue;
            if (sibling.inputItem[0].matchesItem(switchBy)) return sibling.getInternalName();
        }
        return null;
    }

    private boolean isInputSlot(int slot) {
        for (int i : inputSlots) if (i == slot) return true;
        return false;
    }

    private boolean isFirstInputSlot(int slot) {
        return inputSlots.length > 0 && inputSlots[0] == slot;
    }

    protected void consumeInput(Match match) {
        for (int k = 0; k < match.debitSlot.length; k++) {
            int slot = match.debitSlot[k];
            ItemStack stack = inventory.getItem(slot).copy();
            stack.shrink(match.debitCount[k]);
            inventory.setItem(slot, stack.isEmpty() ? ItemStack.EMPTY : stack);
        }
        match.recipe.drainInputFluids(inputTanks);
    }

    protected void produceItem(GenericRecipe recipe) {
        WeightedList<ItemStack>[] outputs = recipe.outputItems();
        if (outputs != null) {
            int n = Math.min(outputs.length, outputSlots.length);
            for (int i = 0; i < n; i++) {
                ItemStack rolled = roll(outputs[i]);
                if (rolled.isEmpty()) continue;
                int idx = outputSlots[i];
                ItemStack slot = inventory.getItem(idx);
                if (slot.isEmpty()) {
                    inventory.setItem(idx, rolled.copy());
                } else {
                    ItemStack grown = slot.copy();
                    grown.grow(rolled.getCount());
                    inventory.setItem(idx, grown);
                }
            }
        }
        recipe.fillOutputFluids(outputTanks);
        markDirty = true;
    }

    public void serialize(ByteBuf buf) {
        buf.writeDouble(progress);
        buf.writeBoolean(restrictedMode);
        new FriendlyByteBuf(buf).writeUtf(recipe);
    }

    public void deserialize(ByteBuf buf) {
        progress = buf.readDouble();
        restrictedMode = buf.readBoolean();
        recipe = new FriendlyByteBuf(buf).readUtf();
    }

    public void save(ValueOutput out) {
        out.putDouble("progress" + index, progress);
        if (!recipe.isEmpty()) out.putString("recipe" + index, recipe);
        if (restrictedMode) out.putBoolean("restrictedMode" + index, true);
    }

    public void load(ValueInput in) {
        progress = in.getDoubleOr("progress" + index, progress);
        setRecipe(
                in.getStringOr("recipe" + index, recipe),
                in.getBooleanOr("restrictedMode" + index, false));
    }

    public static final class Match {

        public final GenericRecipe recipe;

        private final int[] debitSlot;
        private final int[] debitCount;

        private Match(GenericRecipe recipe, int[] debitSlot, int[] debitCount) {
            this.recipe = recipe;
            this.debitSlot = debitSlot;
            this.debitCount = debitCount;
        }
    }
}
