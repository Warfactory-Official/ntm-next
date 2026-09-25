// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.recipes;

import com.hbm.NuclearTech;
import com.hbm.inventory.material.MaterialDistributionRecipe;
import com.hbm.inventory.material.MaterialShapes;
import com.hbm.inventory.material.Mats.MaterialStack;
import com.hbm.inventory.material.Mats;
import com.hbm.inventory.material.NTMMaterial.SmeltingBehavior;
import com.hbm.inventory.material.NTMMaterial;
import com.hbm.inventory.recipes.ingredient.CountIngredient;
import com.hbm.inventory.recipes.ingredient.HbmNarrowedIngredient;
import com.hbm.inventory.recipes.loader.GenericRecipes;
import com.hbm.inventory.recipes.loader.RecipeSource;
import com.hbm.items.machine.ItemScraps;
import com.hbm.lib.Library;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import org.jspecify.annotations.Nullable;

public class ArcFurnaceRecipes extends GenericRecipes<ArcFurnaceRecipe, ArcFurnaceRecipes.Derived> {

    public static final ArcFurnaceRecipes INSTANCE = new ArcFurnaceRecipes();

    private static final Set<String> ARC_SMELTABLE_SHAPES =
            Set.of(
                    "ingots",
                    "ores",
                    "plates",
                    "plates_cast",
                    "plates_welded",
                    "storage_blocks",
                    "glass_blocks");

    private static final Set<TagKey<Item>> VANILLA_NAMED_BACK = vanillaNamedBack();

    private static Set<TagKey<Item>> vanillaNamedBack() {
        Set<TagKey<Item>> tags = new HashSet<>();
        for (String material :
                List.of(
                        "gold",
                        "iron",
                        "lapis",
                        "diamond",
                        "redstone",
                        "emerald",
                        "quartz",
                        "copper")) {
            tags.add(MaterialShapes.ORE.tagFor(material));
            tags.add(MaterialShapes.BLOCK.tagFor(material));
        }
        for (String material : List.of("iron", "gold", "copper"))
            tags.add(MaterialShapes.INGOT.tagFor(material));
        return Set.copyOf(tags);
    }

    private static boolean hasOutputFor(ArcFurnaceRecipe recipe, boolean liquid) {
        return liquid ? recipe.fluidOutput() != null : recipe.solidOutput() != null;
    }

    private static boolean arcSmeltable(Item item) {
        if (item == Items.BRICK || item == Items.NETHER_BRICK) return true;
        Holder.Reference<Item> holder = item.builtInRegistryHolder();
        boolean vanilla =
                holder.key().identifier().getNamespace().equals(Identifier.DEFAULT_NAMESPACE);
        for (TagKey<Item> tag : (Iterable<TagKey<Item>>) holder.tags()::iterator) {
            if (vanilla) {
                if (VANILLA_NAMED_BACK.contains(tag)) return true;
                continue;
            }
            String namespace = tag.location().getNamespace();
            String path = tag.location().getPath();
            int slash = path.indexOf('/');
            if ((namespace.equals("c") || namespace.equals(NuclearTech.MOD_ID))
                    && ARC_SMELTABLE_SHAPES.contains(slash < 0 ? path : path.substring(0, slash))) {
                return true;
            }
        }
        return false;
    }

    private static @Nullable SmeltingRecipe smeltingRecipe(RecipeHolder<?> holder) {
        return holder.value().getType() == RecipeType.SMELTING
                        && holder.value() instanceof SmeltingRecipe recipe
                ? recipe
                : null;
    }

    private static ItemStack furnaceOutput(SmeltingRecipe recipe, Holder<Item> input) {
        ItemStack output = recipe.assemble(new SingleRecipeInput(new ItemStack(input)));
        if (output.isEmpty() || !arcSmeltable(input.value()) && !arcSmeltable(output.getItem()))
            return ItemStack.EMPTY;
        return output;
    }

    public static boolean yieldsFurnaceRows(RecipeHolder<?> holder) {
        SmeltingRecipe smelting = smeltingRecipe(holder);
        if (smelting == null) return false;
        for (Holder<Item> input : smelting.input().items().toList()) {
            if (!furnaceOutput(smelting, input).isEmpty()) return true;
        }
        return false;
    }

    @Override
    protected String registryName() {
        return "arc_furnace";
    }

    @Override
    protected RecipeSerializer<ArcFurnaceRecipe> serializer() {
        return ArcFurnaceRecipe.SERIALIZER;
    }

    @Override
    protected Derived indexRows(List<ArcFurnaceRecipe> rows) {
        if (RecipeSource.token() == null) return new Derived(List.of(), Map.of());
        return derive(rows, RecipeSource.recipes());
    }

    public static Derived derive(
            List<ArcFurnaceRecipe> authored, Collection<RecipeHolder<?>> loaded) {
        return new Builder(authored).build(loaded);
    }

    public @Nullable ArcFurnaceRecipe getOutput(ItemStack stack, boolean liquid) {
        if (stack.isEmpty()) return null;

        MaterialStack scrap = ItemScraps.getMats(stack);
        if (scrap != null && liquid) {
            if (scrap.material.smeltable != SmeltingBehavior.SMELTABLE) return null;
            return new ArcFurnaceRecipe("arc.scraps").outputMaterials(scrap);
        }

        ArcFurnaceRecipe row =
                findByItem(
                        stack, r -> r.inputItem[0].matchesItem(stack) && hasOutputFor(r, liquid));
        return row != null ? row : index().find(stack, liquid);
    }

    public List<ArcFurnaceRecipe> pageRows() {
        List<ArcFurnaceRecipe> out = new ArrayList<>(recipes());
        out.addAll(index().rows());
        return out;
    }

    private static final class Occupied {

        private final Set<Item> anyComponents = new HashSet<>();
        private final Map<Item, Set<DataComponentPatch>> patches = new HashMap<>();

        boolean overlaps(ItemStack stack, boolean any) {
            if (anyComponents.contains(stack.getItem())) return true;
            Set<DataComponentPatch> held = patches.get(stack.getItem());
            return held != null && (any || held.contains(stack.getComponentsPatch()));
        }

        void add(ItemStack stack, boolean any) {
            if (any) anyComponents.add(stack.getItem());
            else
                patches.computeIfAbsent(stack.getItem(), k -> new HashSet<>())
                        .add(stack.getComponentsPatch());
        }
    }

    public record Derived(List<ArcFurnaceRecipe> rows, Map<Item, List<ArcFurnaceRecipe>> byItem) {

        public @Nullable ArcFurnaceRecipe find(ItemStack stack, boolean liquid) {
            for (ArcFurnaceRecipe derived : byItem.getOrDefault(stack.getItem(), List.of())) {
                if (derived.inputItem[0].matchesItem(stack) && hasOutputFor(derived, liquid))
                    return derived;
            }
            return null;
        }
    }

    private static final class Builder {

        private final Occupied occupiedSolid = new Occupied();
        private final Occupied occupiedLiquid = new Occupied();
        private final List<ArcFurnaceRecipe> rows = new ArrayList<>();
        private final Map<Item, List<ArcFurnaceRecipe>> byItem = new HashMap<>();

        Builder(List<ArcFurnaceRecipe> authored) {
            for (ArcFurnaceRecipe recipe : authored)
                occupy(recipe, recipe.inputItem[0].displayStacks());
        }

        private static ResourceKey<Recipe<?>> key(String path) {
            return ResourceKey.create(Registries.RECIPE, Library.id("arc_furnace/" + path));
        }

        private static String segment(Identifier id) {
            return id.getNamespace() + "/" + id.getPath();
        }

        private static boolean anyComponents(ArcFurnaceRecipe recipe) {
            return recipe.inputItem[0].ingredient().getCustomIngredient() == null;
        }

        private void occupy(ArcFurnaceRecipe recipe, List<ItemStack> stacks) {
            boolean any = anyComponents(recipe);
            for (ItemStack stack : stacks) {
                if (recipe.solidOutput() != null) occupiedSolid.add(stack, any);
                if (recipe.fluidOutput() != null) occupiedLiquid.add(stack, any);
            }
        }

        private void register(ArcFurnaceRecipe recipe) {
            List<Holder<Item>> items = recipe.inputItem[0].ingredient().items().toList();
            if (items.isEmpty()) return;
            List<ItemStack> stacks = recipe.inputItem[0].displayStacks();
            boolean any = anyComponents(recipe);
            for (ItemStack stack : stacks) {
                if (recipe.solidOutput() != null && occupiedSolid.overlaps(stack, any)) return;
                if (recipe.fluidOutput() != null && occupiedLiquid.overlaps(stack, any)) return;
            }
            occupy(recipe, stacks);
            rows.add(recipe);
            for (Holder<Item> item : items)
                byItem.computeIfAbsent(item.value(), k -> new ArrayList<>(1)).add(recipe);
        }

        private void fluidRow(String path, CountIngredient input, List<MaterialStack> materials) {
            ArcFurnaceRecipe recipe = new ArcFurnaceRecipe("arc." + path.replace('/', '.'));
            recipe.inputItems(input);
            recipe.outputMaterials(materials);
            register(recipe.withId(key(path)));
        }

        private void customSmeltable(
                String path, CountIngredient input, List<MaterialStack> materials) {
            List<MaterialStack> smeltables = new ArrayList<>();
            for (MaterialStack material : materials) {
                if (material.material.smeltable == SmeltingBehavior.SMELTABLE)
                    smeltables.add(material.copy());
            }
            if (!smeltables.isEmpty()) fluidRow(path, input, smeltables);
        }

        Derived build(Collection<RecipeHolder<?>> loaded) {
            for (NTMMaterial material : Mats.orderedList) {
                NTMMaterial convert = material.smeltsInto;
                if (convert.smeltable != SmeltingBehavior.SMELTABLE) continue;
                for (MaterialShapes shape : MaterialShapes.allShapes) {
                    if (shape.tagPathPlural == null || shape.q(1) == 0) continue;
                    TagKey<Item> tag = shape.tagFor(material.tagPath);
                    Optional<HolderSet.Named<Item>> members = BuiltInRegistries.ITEM.get(tag);
                    if (members.isEmpty() || members.get().size() == 0) continue;
                    fluidRow(
                            "shape/" + segment(tag.location()),
                            CountIngredient.of(members.get(), 1),
                            List.of(
                                    new MaterialStack(
                                            convert,
                                            shape.q(1) * material.convOut / material.convIn)));
                }
            }

            Map<TagKey<Item>, MaterialDistributionRecipe> oreEntries = new LinkedHashMap<>();
            Map<Item, MaterialDistributionRecipe> itemEntries = new LinkedHashMap<>();
            List<RecipeHolder<?>> smelting = new ArrayList<>();
            for (RecipeHolder<?> holder : loaded) {
                if (holder.value() instanceof MaterialDistributionRecipe entry) {
                    entry.targets()
                            .unwrap()
                            .ifLeft(tag -> oreEntries.put(tag, entry))
                            .ifRight(
                                    items ->
                                            items.forEach(
                                                    item -> itemEntries.put(item.value(), entry)));
                } else if (smeltingRecipe(holder) != null) {
                    smelting.add(holder);
                }
            }

            for (var row : oreEntries.entrySet()) {
                Optional<HolderSet.Named<Item>> members = BuiltInRegistries.ITEM.get(row.getKey());
                if (members.isEmpty() || members.get().size() == 0) continue;
                customSmeltable(
                        "ore/" + segment(row.getKey().location()),
                        CountIngredient.of(members.get(), 1),
                        row.getValue().materials());
            }
            for (var row : itemEntries.entrySet()) {
                String path = "item/" + segment(BuiltInRegistries.ITEM.getKey(row.getKey()));
                customSmeltable(
                        path, CountIngredient.of(row.getKey(), 1), row.getValue().materials());
            }

            for (RecipeHolder<?> holder : smelting) {
                SmeltingRecipe cooking = smeltingRecipe(holder);
                for (Holder<Item> input : cooking.input().items().toList()) {
                    ItemStack output = furnaceOutput(cooking, input);
                    if (output.isEmpty()) continue;
                    String path =
                            "furnace/"
                                    + segment(holder.id().identifier())
                                    + "/"
                                    + segment(BuiltInRegistries.ITEM.getKey(input.value()));
                    ArcFurnaceRecipe recipe = new ArcFurnaceRecipe("arc." + path.replace('/', '.'));

                    Ingredient narrowed =
                            cooking.input().getCustomIngredient() == null
                                    ? Ingredient.of(input.value())
                                    : HbmNarrowedIngredient.of(input, cooking.input());
                    recipe.inputItems(new CountIngredient(narrowed, 1));
                    recipe.outputTemplates(ItemStackTemplate.fromStack(output));
                    register(recipe.withId(key(path)));
                }
            }

            Map<Item, List<ArcFurnaceRecipe>> frozen = new HashMap<>(byItem.size());
            byItem.forEach((item, found) -> frozen.put(item, List.copyOf(found)));
            return new Derived(List.copyOf(rows), Map.copyOf(frozen));
        }
    }
}
