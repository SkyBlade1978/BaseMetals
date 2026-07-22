package com.mcmoddev.basemetals.data;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mcmoddev.basemetals.BaseMetals;
import com.mcmoddev.basemetals.content.ModContent;
import com.mcmoddev.basemetals.material.MaterialCatalogue;
import com.mcmoddev.basemetals.material.MaterialDefinition;

import net.minecraft.data.DataGenerator;
import net.minecraft.data.DataProvider;
import net.minecraft.data.HashCache;

/** Small catalogue-driven generator for server data and compatibility manifests. */
final class GeneratedData implements DataProvider {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final List<String> LOCALES = List.of("de_de", "en_au", "en_ca", "en_en", "en_gb",
            "en_pt", "en_us", "es_es", "es_mx", "fr_ca", "fr_fr", "it_it", "ja_jp", "nl_nl",
            "pt_br", "ru_ru", "zh_cn", "zh_tw");
    private static final List<String> ADVANCEMENTS = List.of("this_is_new", "blocktastic", "geologist",
            "metallurgy", "brass_maker", "bronze_maker", "cupronickel_maker", "electrum_maker",
            "steel_maker", "invar_maker", "mithril_maker", "aquarium_maker", "pewter_maker",
            "angel_of_death", "scuba_diver", "demon_slayer", "juggernaut", "moon_boots");
    private static final List<String> PROCESSING_MATERIALS = List.of(
            "adamantine", "antimony", "bismuth", "coldiron",
            "nickel", "platinum", "starsteel", "zinc");
    private static final Map<String, List<AlloyComponent>> ALLOYS = Map.ofEntries(
            Map.entry("aquarium", List.of(component("copper", 2), component("zinc", 1),
                    new AlloyComponent("#forge:dusts/prismarine", 1))),
            Map.entry("brass", List.of(component("copper", 3), component("zinc", 1))),
            Map.entry("bronze", List.of(component("copper", 3), component("tin", 1))),
            Map.entry("cupronickel", List.of(component("copper", 3), component("nickel", 1))),
            Map.entry("electrum", List.of(component("gold", 1), component("silver", 1))),
            Map.entry("invar", List.of(component("iron", 2), component("nickel", 1))),
            Map.entry("mithril", List.of(component("silver", 2), component("coldiron", 1),
                    new AlloyComponent("#forge:ingots/mercury", 1))),
            Map.entry("pewter", List.of(component("tin", 1), component("copper", 1), component("lead", 1))),
            Map.entry("steel", List.of(component("iron", 8), component("coal", 1))));
    private static final Map<String, Integer> ALLOY_OUTPUTS = Map.of(
            "aquarium", 3, "brass", 4, "bronze", 4, "cupronickel", 4,
            "electrum", 2, "invar", 3, "mithril", 3, "pewter", 3, "steel", 8);

    private final Path output;
    private final Path legacyReference;

    GeneratedData(DataGenerator generator) {
        this.output = generator.getOutputFolder();
        this.legacyReference = output.toAbsolutePath().getParent().getParent().getParent()
                .resolve("reference/1.12");
    }

    @Override
    public void run(HashCache cache) throws IOException {
        writeFluidBlockstates(cache);
        writeLoot(cache);
        writeTags(cache);
        writeRecipes(cache);
        writeTinkersMaterials(cache);
        writeAdvancements(cache);
        writeChestLoot(cache);
        writeLanguages(cache);
        writeManifest(cache);
    }

    private void writeFluidBlockstates(HashCache cache) throws IOException {
        for (String name : ModContent.fluids().keySet()) {
            JsonObject custom = new JsonObject();
            custom.addProperty("fluid", BaseMetals.MOD_ID + ":" + name);
            JsonObject model = new JsonObject();
            // LiquidBlockRenderer supplies the fluid surface; the vanilla water
            // model is the canonical particle-only fallback for a liquid block.
            model.addProperty("model", "minecraft:block/water");
            model.add("custom", custom);
            JsonObject variants = new JsonObject();
            variants.add("", model);
            JsonObject root = new JsonObject();
            root.add("variants", variants);
            save(cache, root, "assets/basemetals/blockstates/" + name + ".json");
        }
    }

    private void writeLoot(HashCache cache) throws IOException {
        for (String id : ModContent.blocksById().keySet()) {
            if (ModContent.fluids().containsKey(id)) continue;
            String drop = id;
            boolean compatibilitySlab = id.startsWith("double_") && id.endsWith("_slab");
            if (compatibilitySlab) drop = id.substring("double_".length());
            JsonObject entry = object("type", "minecraft:item", "name", "basemetals:" + drop);
            JsonArray functions = new JsonArray();
            if (compatibilitySlab) {
                functions.add(object("function", "minecraft:set_count", "count", 2));
            } else if (ModContent.blocksById().get(id).get() instanceof net.minecraft.world.level.block.SlabBlock) {
                JsonObject doubleCondition = object("condition", "minecraft:block_state_property",
                        "block", "basemetals:" + id);
                doubleCondition.add("properties", object("type", "double"));
                JsonArray conditions = new JsonArray(); conditions.add(doubleCondition);
                JsonObject count = object("function", "minecraft:set_count", "count", 2);
                count.add("conditions", conditions);
                functions.add(count);
            }
            entry.add("functions", functions);
            JsonObject pool = new JsonObject();
            pool.addProperty("rolls", 1);
            JsonArray entries = new JsonArray(); entries.add(entry); pool.add("entries", entries);
            JsonArray conditions = new JsonArray(); conditions.add(object("condition", "minecraft:survives_explosion"));
            if (ModContent.blocksById().get(id).get() instanceof net.minecraft.world.level.block.DoorBlock) {
                JsonObject lowerHalf = object("condition", "minecraft:block_state_property",
                        "block", "basemetals:" + id);
                lowerHalf.add("properties", object("half", "lower"));
                conditions.add(lowerHalf);
            }
            pool.add("conditions", conditions);
            JsonObject root = object("type", "minecraft:block");
            JsonArray pools = new JsonArray(); pools.add(pool); root.add("pools", pools);
            save(cache, root, "data/basemetals/loot_tables/blocks/" + id + ".json");
        }
    }

    private void writeTags(HashCache cache) throws IOException {
        List<String> allBlockDrops = new ArrayList<>();
        for (String id : ModContent.blocksById().keySet()) {
            if (!ModContent.fluids().containsKey(id)) allBlockDrops.add("basemetals:" + id);
        }
        tag(cache, "minecraft", "blocks/mineable/pickaxe", allBlockDrops);
        tag(cache, "minecraft", "blocks/anvil", List.of(
                "basemetals:stone_anvil", "basemetals:steel_anvil", "basemetals:adamantine_anvil"));
        tag(cache, "basemetals", "blocks/scythe_harvestable", List.of(
                "#minecraft:leaves", "#minecraft:saplings", "#minecraft:flowers",
                "minecraft:grass", "minecraft:tall_grass", "minecraft:fern", "minecraft:large_fern",
                "minecraft:vine", "minecraft:cobweb", "minecraft:cactus",
                "minecraft:pumpkin", "minecraft:melon"));
        tag(cache, "forge", "blocks/netherrack", List.of("minecraft:netherrack"));
        tag(cache, "forge", "blocks/end_stones", List.of("minecraft:end_stone"));

        Map<String, List<String>> aggregateItems = new LinkedHashMap<>();
        Map<String, List<String>> aggregateBlocks = new LinkedHashMap<>();
        for (MaterialDefinition material : MaterialCatalogue.ALL) {
            String name = material.name();
            itemTag(cache, "ingots", name, entries(name, "ingot", name.equals("copper") ? "minecraft:copper_ingot" : null));
            itemTag(cache, "nuggets", name, entries(name, "nugget"));
            itemTag(cache, "dusts", name, entries(name, "powder"));
            itemTag(cache, "tiny_dusts", name, entries(name, "smallpowder"));
            if (material.hasEquipment()) {
                itemTag(cache, "plates", name, entries(name, "plate"));
                itemTag(cache, "rods", name, entries(name, "rod"));
                itemTag(cache, "gears", name, entries(name, "gear"));
                blockTag(cache, "storage_blocks", name, entries(name, "block",
                        name.equals("copper") ? "minecraft:copper_block" : null));
                itemTag(cache, "storage_blocks", name, entries(name, "block",
                        name.equals("copper") ? "minecraft:copper_block" : null));
            }
            if (material.hasOre()) {
                List<String> extra = name.equals("copper")
                        ? List.of("minecraft:copper_ore", "minecraft:deepslate_copper_ore") : List.of();
                blockTag(cache, "ores", name, entries(name, "ore", extra));
                itemTag(cache, "ores", name, entries(name, "ore", extra));
            }
            if (material.isAlloy()) {
                itemTag(cache, "blends", name, entries(name, "blend"));
                itemTag(cache, "small_blends", name, entries(name, "smallblend"));
            }
            if (material.processingForms()) {
                for (String form : List.of("casings", "dense_plates", "crushed_ores", "purified_ores",
                        "crystals", "shards", "clumps", "dirty_dusts")) {
                    String itemSuffix = switch (form) {
                        case "casings" -> "casing";
                        case "dense_plates" -> "dense_plate";
                        case "crushed_ores" -> "crushed";
                        case "purified_ores" -> "crushed_purified";
                        case "dirty_dusts" -> "powder_dirty";
                        default -> form.substring(0, form.length() - 1);
                    };
                    itemTag(cache, form, name, entries(name, itemSuffix));
                }
            }
            addAggregate(aggregateItems, "ingots", "#forge:ingots/" + name);
            addAggregate(aggregateItems, "nuggets", "#forge:nuggets/" + name);
            addAggregate(aggregateItems, "dusts", "#forge:dusts/" + name);
            if (material.hasOre()) addAggregate(aggregateBlocks, "ores", "#forge:ores/" + name);
        }
        aggregateItems.forEach((path, values) -> uncheckedTag(cache, "forge", "items/" + path, values));
        aggregateBlocks.forEach((path, values) -> uncheckedTag(cache, "forge", "blocks/" + path, values));

        for (String alias : List.of("adamant", "adamantite", "adamantium")) {
            tag(cache, "forge", "items/ingots/" + alias, List.of("#forge:ingots/adamantine"));
            tag(cache, "forge", "items/nuggets/" + alias, List.of("#forge:nuggets/adamantine"));
            tag(cache, "forge", "items/dusts/" + alias, List.of("#forge:dusts/adamantine"));
        }
        tag(cache, "forge", "items/ingots/quicksilver", List.of("#forge:ingots/mercury"));
        tag(cache, "forge", "items/gears/steel", List.of("basemetals:steel_gear"));
        tag(cache, "forge", "items/sprockets/steel", List.of("#forge:gears/steel"));
        writeVanillaBitsTags(cache);
        writeCompatibilityTags(cache);
    }

    private void writeVanillaBitsTags(HashCache cache) throws IOException {
        Map<String, String> vanillaIngot = Map.of(
                "diamond", "minecraft:diamond", "emerald", "minecraft:emerald",
                "gold", "minecraft:gold_ingot", "iron", "minecraft:iron_ingot",
                "quartz", "minecraft:quartz", "obsidian", "basemetals:obsidian_ingot");
        Map<String, String> vanillaBlock = Map.of(
                "diamond", "minecraft:diamond_block", "emerald", "minecraft:emerald_block",
                "gold", "minecraft:gold_block", "iron", "minecraft:iron_block",
                "quartz", "minecraft:quartz_block", "obsidian", "minecraft:obsidian");
        for (String material : List.of("diamond", "emerald", "gold", "iron", "obsidian", "quartz")) {
            if (ModContent.itemsById().containsKey(material + "_rod")) {
                tag(cache, "forge", "items/rods/" + material,
                        List.of("basemetals:" + material + "_rod"));
            }
            if (ModContent.itemsById().containsKey(material + "_gear")) {
                tag(cache, "forge", "items/gears/" + material,
                        List.of("basemetals:" + material + "_gear"));
            }
            tag(cache, "forge", "items/materials/" + material,
                    List.of(vanillaIngot.get(material)));
            tag(cache, "forge", "items/storage_blocks/" + material,
                    List.of(vanillaBlock.get(material)));
            tag(cache, "forge", "blocks/storage_blocks/" + material,
                    List.of(vanillaBlock.get(material)));
        }
        tag(cache, "forge", "items/rods/stone", List.of("basemetals:stone_rod"));
        for (String material : List.of("wood", "stone")) {
            tag(cache, "forge", "items/gears/" + material,
                    List.of("basemetals:" + material + "_gear"));
        }
        for (String material : List.of("coal", "charcoal", "diamond", "emerald", "gold", "iron", "obsidian", "quartz")) {
            tag(cache, "forge", "items/dusts/" + material,
                    List.of("basemetals:" + material + "_powder"));
            tag(cache, "forge", "items/tiny_dusts/" + material,
                    List.of("basemetals:" + material + "_smallpowder"));
        }
        tag(cache, "forge", "items/tiny_dusts/redstone", List.of("basemetals:redstone_smallpowder"));
        tag(cache, "forge", "items/tiny_dusts/lapis", List.of("basemetals:lapis_smallpowder"));
        tag(cache, "forge", "items/dusts/redstone", List.of("minecraft:redstone"));
        tag(cache, "forge", "items/dusts/lapis", List.of("minecraft:lapis_lazuli"));
        tag(cache, "forge", "items/dusts/prismarine", List.of("minecraft:prismarine_shard"));
        for (String material : List.of("coal", "charcoal", "diamond", "emerald", "obsidian", "quartz")) {
            tag(cache, "forge", "items/nuggets/" + material,
                    List.of("basemetals:" + material + "_nugget"));
        }
        tag(cache, "forge", "items/ingots/obsidian", List.of("basemetals:obsidian_ingot"));
        tag(cache, "forge", "items/ingots/redstone", List.of("basemetals:redstone_ingot"));
        tag(cache, "forge", "items/plates/gold", List.of("basemetals:gold_plate"));
        tag(cache, "forge", "items/plates/iron", List.of("basemetals:iron_plate"));
        for (String material : List.of("diamond", "gold", "iron")) {
            tag(cache, "forge", "items/blends/" + material,
                    List.of("basemetals:" + material + "_blend"));
            tag(cache, "forge", "items/small_blends/" + material,
                    List.of("basemetals:" + material + "_smallblend"));
        }
        tag(cache, "forge", "items/storage_blocks/charcoal", List.of("basemetals:charcoal_block"));
        tag(cache, "forge", "blocks/storage_blocks/charcoal", List.of("basemetals:charcoal_block"));
    }

    private void writeCompatibilityTags(HashCache cache) throws IOException {
        for (MaterialDefinition material : MaterialCatalogue.ALL) {
            tag(cache, "forge", "fluids/molten_" + material.name(), List.of(
                    "basemetals:" + material.name(),
                    "basemetals:flowing_" + material.name()));
        }
        for (String material : PROCESSING_MATERIALS) {
            for (String family : List.of("dirty_dusts", "clumps", "crystals", "shards")) {
                tag(cache, "mekanism", "items/" + family + "/" + material,
                        List.of("#forge:" + family + "/" + material));
            }
        }
    }

    private void writeRecipes(HashCache cache) throws IOException {
        for (MaterialDefinition material : MaterialCatalogue.ALL) {
            String name = material.name();
            String ingot = "basemetals:" + name + "_ingot";
            String ingotIngredient = name.equals("copper") ? ingot : "#forge:ingots/" + name;
            if (material.hasEquipment()) {
                shaped(cache, name + "_block", List.of("XXX", "XXX", "XXX"), Map.of('X', ingotIngredient),
                        "basemetals:" + name + "_block", 1);
                shapeless(cache, name + "_block_ingot", List.of("basemetals:" + name + "_block"), ingot, 9);
                shaped(cache, name + "_plate", List.of("XXX"), Map.of('X', ingotIngredient),
                        "basemetals:" + name + "_plate", 1);
                shaped(cache, name + "_rod", List.of("X", "X"), Map.of('X', ingotIngredient),
                        "basemetals:" + name + "_rod", 1);
                shaped(cache, name + "_gear", List.of(" X ", "XRX", " X "),
                        Map.of('X', ingotIngredient, 'R', "#forge:rods/" + name),
                        "basemetals:" + name + "_gear", 1);
                equipmentRecipes(cache, name, ingotIngredient);
                decorativeRecipes(cache, name, ingotIngredient);
            }
            String nuggetIngredient = name.equals("copper")
                    ? "basemetals:copper_nugget"
                    : "#forge:nuggets/" + name;
            shaped(cache, name + "_ingot_from_nuggets", List.of("XXX", "XXX", "XXX"),
                    Map.of('X', nuggetIngredient), ingot, 1);
            shapeless(cache, name + "_nuggets", List.of(ingotIngredient), "basemetals:" + name + "_nugget", 9);
            shaped(cache, name + "_powder_from_small", List.of("XXX", "XXX", "XXX"),
                    Map.of('X', "#forge:tiny_dusts/" + name), "basemetals:" + name + "_powder", 1);
            shapeless(cache, name + "_smallpowder", List.of("#forge:dusts/" + name),
                    "basemetals:" + name + "_smallpowder", 9);
            smelting(cache, name + "_powder_smelting", "#forge:dusts/" + name, ingot,
                    material.oreSmeltingExperience(), 200, "minecraft:smelting");
            if (material.hasOre()) {
                smelting(cache, name + "_ore_smelting", "#forge:ores/" + name, ingot,
                        material.oreSmeltingExperience(), 200, "minecraft:smelting");
                smelting(cache, name + "_ore_blasting", "#forge:ores/" + name, ingot,
                        material.oreSmeltingExperience(), 100, "minecraft:blasting");
                crushing(cache, name + "_ore_crushing", "#forge:ores/" + name,
                        "basemetals:" + name + "_powder", 2);
            }
            if (material.isAlloy()) {
                shaped(cache, name + "_blend_from_small", List.of("XXX", "XXX", "XXX"),
                        Map.of('X', "#forge:small_blends/" + name), "basemetals:" + name + "_blend", 1);
                shapeless(cache, name + "_smallblend", List.of("#forge:blends/" + name),
                        "basemetals:" + name + "_smallblend", 9);
                smelting(cache, name + "_blend_smelting", "#forge:blends/" + name, ingot,
                        material.oreSmeltingExperience(), 200, "minecraft:smelting");
            }
        }
        alloyRecipes(cache);
        vanillaBitsRecipes(cache);
        anvilRecipes(cache);
        writeCompatibilityRecipes(cache);
    }

    private void equipmentRecipes(HashCache cache, String name, String metal) throws IOException {
        Map<Character, String> tool = Map.of('X', metal, 'S', "minecraft:stick");
        shaped(cache, name + "_pickaxe", List.of("XXX", " S ", " S "), tool, "basemetals:" + name + "_pickaxe", 1);
        shaped(cache, name + "_axe", List.of("XX", "XS", " S"), tool, "basemetals:" + name + "_axe", 1);
        shaped(cache, name + "_shovel", List.of("X", "S", "S"), tool, "basemetals:" + name + "_shovel", 1);
        shaped(cache, name + "_hoe", List.of("XX", " S", " S"), tool, "basemetals:" + name + "_hoe", 1);
        shaped(cache, name + "_sword", List.of("X", "X", "S"), tool, "basemetals:" + name + "_sword", 1);
        shaped(cache, name + "_helmet", List.of("XXX", "X X"), Map.of('X', metal), "basemetals:" + name + "_helmet", 1);
        shaped(cache, name + "_chestplate", List.of("X X", "XXX", "XXX"), Map.of('X', metal), "basemetals:" + name + "_chestplate", 1);
        shaped(cache, name + "_leggings", List.of("XXX", "X X", "X X"), Map.of('X', metal), "basemetals:" + name + "_leggings", 1);
        shaped(cache, name + "_boots", List.of("X X", "X X"), Map.of('X', metal), "basemetals:" + name + "_boots", 1);
        shaped(cache, name + "_crackhammer", List.of("X", "S", "S"),
                Map.of('X', "#forge:storage_blocks/" + name, 'S', "minecraft:stick"),
                "basemetals:" + name + "_crackhammer", 1);
        shaped(cache, name + "_scythe", List.of("XX ", "  X", " S "), tool, "basemetals:" + name + "_scythe", 1);
        shaped(cache, name + "_shears", List.of(" X", "X "), Map.of('X', metal),
                "basemetals:" + name + "_shears", 1);
        shaped(cache, name + "_fishingrod", List.of("  R", " RS", "R S"),
                Map.of('R', "#forge:rods/" + name, 'S', "minecraft:string"),
                "basemetals:" + name + "_fishing_rod", 1);
        shaped(cache, name + "_horsearmor", List.of("  X", "XWX", "XXX"),
                Map.of('X', metal, 'W', "#minecraft:wool"),
                "basemetals:" + name + "_horse_armor", 1);
        shaped(cache, name + "_arrow", List.of("N", "R", "F"),
                Map.of('N', "#forge:nuggets/" + name, 'R', "#forge:rods/" + name, 'F', "minecraft:feather"),
                "basemetals:" + name + "_arrow", 4);
        shaped(cache, name + "_bolt", List.of("R", "R", "F"),
                Map.of('R', "#forge:rods/" + name, 'F', "minecraft:feather"),
                "basemetals:" + name + "_bolt", 4);
        shaped(cache, name + "_bow", List.of(" RS", "R S", " RS"),
                Map.of('R', "#forge:rods/" + name, 'S', "minecraft:string"), "basemetals:" + name + "_bow", 1);
        shaped(cache, name + "_crossbow", List.of("GRR", " SR", "R G"),
                Map.of('R', "#forge:rods/" + name, 'S', "minecraft:string", 'G', "#forge:gears/" + name),
                "basemetals:" + name + "_crossbow", 1);
        shaped(cache, name + "_shield", List.of("XWX", "XXX", " X "),
                Map.of('X', metal, 'W', "#minecraft:planks"), "basemetals:" + name + "_shield", 1);
    }

    private void decorativeRecipes(HashCache cache, String name, String metal) throws IOException {
        shaped(cache, name + "_bars", List.of("XXX", "XXX"), Map.of('X', metal), "basemetals:" + name + "_bars", 16);
        shaped(cache, name + "_door", List.of("XX", "XX", "XX"), Map.of('X', metal), "basemetals:" + name + "_door", 3);
        shaped(cache, name + "_trapdoor", List.of("XXX", "XXX"), Map.of('X', metal), "basemetals:" + name + "_trapdoor", 2);
        shapeless(cache, name + "_button", List.of(metal), "basemetals:" + name + "_button", 1);
        shaped(cache, name + "_slab", List.of("XXX"), Map.of('X', metal), "basemetals:" + name + "_slab", 6);
        shaped(cache, name + "_pressure_plate", List.of("XX"), Map.of('X', metal), "basemetals:" + name + "_pressure_plate", 1);
        shaped(cache, name + "_stairs", List.of("X  ", "XX ", "XXX"), Map.of('X', metal), "basemetals:" + name + "_stairs", 4);
        shaped(cache, name + "_wall", List.of("XXX", "XXX"), Map.of('X', metal), "basemetals:" + name + "_wall", 6);
        shaped(cache, name + "_lever", List.of("R", "X"),
                Map.of('R', "#forge:rods/" + name, 'X', metal), "basemetals:" + name + "_lever", 1);
    }

    private void vanillaBitsRecipes(HashCache cache) throws IOException {
        Map<String, String> materials = Map.of(
                "diamond", "minecraft:diamond", "emerald", "minecraft:emerald",
                "gold", "#forge:ingots/gold", "iron", "#forge:ingots/iron",
                "obsidian", "#forge:ingots/obsidian", "quartz", "minecraft:quartz");
        for (Map.Entry<String, String> entry : materials.entrySet()) {
            String name = entry.getKey();
            String material = entry.getValue();
            if (hasItem(name + "_rod")) {
                shaped(cache, name + "_rod", List.of("X", "X"), Map.of('X', material),
                        "basemetals:" + name + "_rod", 1);
            }
            if (hasItem(name + "_gear")) {
                shaped(cache, name + "_gear", List.of(" X ", "XRX", " X "),
                        Map.of('X', material, 'R', "#forge:rods/" + name),
                        "basemetals:" + name + "_gear", 1);
            }
            vanillaEquipmentRecipes(cache, name, material);
            vanillaDecorativeRecipes(cache, name, material);
        }
        for (String name : List.of("stone", "wood")) {
            String material = name.equals("stone") ? "minecraft:cobblestone" : "#minecraft:planks";
            if (hasItem(name + "_gear")) {
                shaped(cache, name + "_gear", List.of(" X ", "XXX", " X "), Map.of('X', material),
                        "basemetals:" + name + "_gear", 1);
            }
            if (hasItem(name + "_crackhammer")) {
                shaped(cache, name + "_crackhammer", List.of("X", "S", "S"),
                        Map.of('X', material, 'S', "minecraft:stick"),
                        "basemetals:" + name + "_crackhammer", 1);
            }
            if (hasItem(name + "_scythe")) {
                shaped(cache, name + "_scythe", List.of("XX ", "  X", " S "),
                        Map.of('X', material, 'S', "minecraft:stick"),
                        "basemetals:" + name + "_scythe", 1);
            }
        }
        vanillaPowderRecipes(cache);
        crushing(cache, "stone_crushing", "#forge:stone", "minecraft:cobblestone", 1);
        crushing(cache, "cobblestone_crushing", "#forge:cobblestone", "minecraft:gravel", 1);
        crushing(cache, "gravel_crushing", "#forge:gravel", "minecraft:sand", 1);
        crushing(cache, "obsidian_crushing", "minecraft:obsidian", "basemetals:obsidian_powder", 4);
        crushing(cache, "sugar_cane_crushing", "minecraft:sugar_cane", "minecraft:sugar", 2);
        crushing(cache, "bone_crushing", "minecraft:bone", "minecraft:bone_meal", 3);
        crushing(cache, "blaze_rod_crushing", "minecraft:blaze_rod", "minecraft:blaze_powder", 2);
    }

    private void vanillaEquipmentRecipes(HashCache cache, String name, String material) throws IOException {
        if (hasItem(name + "_crackhammer")) shaped(cache, name + "_crackhammer", List.of("X", "S", "S"),
                Map.of('X', "#forge:storage_blocks/" + name, 'S', "minecraft:stick"),
                "basemetals:" + name + "_crackhammer", 1);
        if (hasItem(name + "_scythe")) shaped(cache, name + "_scythe", List.of("XX ", "  X", " S "),
                Map.of('X', material, 'S', "minecraft:stick"), "basemetals:" + name + "_scythe", 1);
        if (hasItem(name + "_shears")) shaped(cache, name + "_shears", List.of(" X", "X "),
                Map.of('X', material), "basemetals:" + name + "_shears", 1);
        if (hasItem(name + "_fishing_rod")) shaped(cache, name + "_fishingrod", List.of("  R", " RS", "R S"),
                Map.of('R', "#forge:rods/" + name, 'S', "minecraft:string"),
                "basemetals:" + name + "_fishing_rod", 1);
        if (hasItem(name + "_arrow")) shaped(cache, name + "_arrow", List.of("N", "R", "F"),
                Map.of('N', "#forge:nuggets/" + name, 'R', "#forge:rods/" + name, 'F', "minecraft:feather"),
                "basemetals:" + name + "_arrow", 4);
        if (hasItem(name + "_bolt")) shaped(cache, name + "_bolt", List.of("R", "R", "F"),
                Map.of('R', "#forge:rods/" + name, 'F', "minecraft:feather"),
                "basemetals:" + name + "_bolt", 4);
        if (hasItem(name + "_bow")) shaped(cache, name + "_bow", List.of(" RS", "R S", " RS"),
                Map.of('R', "#forge:rods/" + name, 'S', "minecraft:string"),
                "basemetals:" + name + "_bow", 1);
        if (hasItem(name + "_crossbow")) shaped(cache, name + "_crossbow", List.of("GRR", " SR", "R G"),
                Map.of('R', "#forge:rods/" + name, 'S', "minecraft:string", 'G', "#forge:gears/" + name),
                "basemetals:" + name + "_crossbow", 1);
        if (hasItem(name + "_shield")) shaped(cache, name + "_shield", List.of("XWX", "XXX", " X "),
                Map.of('X', material, 'W', "#minecraft:planks"), "basemetals:" + name + "_shield", 1);
        if (hasItem(name + "_horse_armor")) shaped(cache, name + "_horsearmor", List.of("  X", "XWX", "XXX"),
                Map.of('X', material, 'W', "#minecraft:wool"), "basemetals:" + name + "_horse_armor", 1);
        if (hasItem(name + "_sword")) {
            equipmentRecipesForVanillaTools(cache, name, material);
        }
    }

    private void equipmentRecipesForVanillaTools(HashCache cache, String name, String material) throws IOException {
        Map<Character, String> tool = Map.of('X', material, 'S', "minecraft:stick");
        shaped(cache, name + "_pickaxe", List.of("XXX", " S ", " S "), tool, "basemetals:" + name + "_pickaxe", 1);
        shaped(cache, name + "_axe", List.of("XX", "XS", " S"), tool, "basemetals:" + name + "_axe", 1);
        shaped(cache, name + "_shovel", List.of("X", "S", "S"), tool, "basemetals:" + name + "_shovel", 1);
        shaped(cache, name + "_hoe", List.of("XX", " S", " S"), tool, "basemetals:" + name + "_hoe", 1);
        shaped(cache, name + "_sword", List.of("X", "X", "S"), tool, "basemetals:" + name + "_sword", 1);
        shaped(cache, name + "_helmet", List.of("XXX", "X X"), Map.of('X', material), "basemetals:" + name + "_helmet", 1);
        shaped(cache, name + "_chestplate", List.of("X X", "XXX", "XXX"), Map.of('X', material), "basemetals:" + name + "_chestplate", 1);
        shaped(cache, name + "_leggings", List.of("XXX", "X X", "X X"), Map.of('X', material), "basemetals:" + name + "_leggings", 1);
        shaped(cache, name + "_boots", List.of("X X", "X X"), Map.of('X', material), "basemetals:" + name + "_boots", 1);
    }

    private void vanillaDecorativeRecipes(HashCache cache, String name, String material) throws IOException {
        if (hasItem(name + "_bars")) shaped(cache, name + "_bars", List.of("XXX", "XXX"), Map.of('X', material), "basemetals:" + name + "_bars", 16);
        if (hasItem(name + "_door")) shaped(cache, name + "_door", List.of("XX", "XX", "XX"), Map.of('X', material), "basemetals:" + name + "_door", 3);
        if (hasItem(name + "_trapdoor")) shaped(cache, name + "_trapdoor", List.of("XXX", "XXX"), Map.of('X', material), "basemetals:" + name + "_trapdoor", 2);
        if (hasItem(name + "_button")) shapeless(cache, name + "_button", List.of(material), "basemetals:" + name + "_button", 1);
        if (hasItem(name + "_slab")) shaped(cache, name + "_slab", List.of("XXX"), Map.of('X', material), "basemetals:" + name + "_slab", 6);
        if (hasItem(name + "_pressure_plate")) shaped(cache, name + "_pressure_plate", List.of("XX"), Map.of('X', material), "basemetals:" + name + "_pressure_plate", 1);
        if (hasItem(name + "_stairs")) shaped(cache, name + "_stairs", List.of("X  ", "XX ", "XXX"), Map.of('X', material), "basemetals:" + name + "_stairs", 4);
        if (hasItem(name + "_wall")) shaped(cache, name + "_wall", List.of("XXX", "XXX"), Map.of('X', material), "basemetals:" + name + "_wall", 6);
        if (hasItem(name + "_lever")) shaped(cache, name + "_lever", List.of("R", "X"),
                Map.of('R', "#forge:rods/" + name, 'X', material), "basemetals:" + name + "_lever", 1);
    }

    private void vanillaPowderRecipes(HashCache cache) throws IOException {
        for (String name : List.of("coal", "charcoal", "diamond", "emerald", "gold", "iron", "obsidian", "quartz")) {
            shaped(cache, name + "_powder_from_small", List.of("XXX", "XXX", "XXX"),
                    Map.of('X', "#forge:tiny_dusts/" + name), "basemetals:" + name + "_powder", 1);
            shapeless(cache, name + "_smallpowder", List.of("#forge:dusts/" + name),
                    "basemetals:" + name + "_smallpowder", 9);
        }
        shaped(cache, "redstone_from_smallpowder", List.of("XXX", "XXX", "XXX"),
                Map.of('X', "#forge:tiny_dusts/redstone"), "minecraft:redstone", 1);
        shaped(cache, "lapis_from_smallpowder", List.of("XXX", "XXX", "XXX"),
                Map.of('X', "#forge:tiny_dusts/lapis"), "minecraft:lapis_lazuli", 1);
    }

    private void anvilRecipes(HashCache cache) throws IOException {
        shaped(cache, "stone_anvil", List.of("BBB", " C ", "SSS"),
                Map.of('B', "minecraft:stone_bricks", 'C', "#forge:stone", 'S', "minecraft:stone_slab"),
                "basemetals:stone_anvil", 1);
        for (String material : List.of("steel", "adamantine")) {
            shaped(cache, material + "_anvil", List.of("BBB", " B ", "III"),
                    Map.of('B', "#forge:storage_blocks/" + material, 'I', "#forge:ingots/" + material),
                    "basemetals:" + material + "_anvil", 1);
        }
    }

    private static boolean hasItem(String id) {
        return ModContent.itemsById().containsKey(id);
    }

    private void alloyRecipes(HashCache cache) throws IOException {
        alloy(cache, "aquarium", 3, "#forge:dusts/copper", "#forge:dusts/copper", "#forge:dusts/zinc", "#forge:dusts/prismarine");
        alloy(cache, "brass", 4, "#forge:dusts/copper", "#forge:dusts/copper", "#forge:dusts/copper", "#forge:dusts/zinc");
        alloy(cache, "bronze", 4, "#forge:dusts/copper", "#forge:dusts/copper", "#forge:dusts/copper", "#forge:dusts/tin");
        alloy(cache, "cupronickel", 4, "#forge:dusts/copper", "#forge:dusts/copper", "#forge:dusts/copper", "#forge:dusts/nickel");
        alloy(cache, "electrum", 2, "#forge:dusts/gold", "#forge:dusts/silver");
        alloy(cache, "invar", 3, "#forge:dusts/iron", "#forge:dusts/iron", "#forge:dusts/nickel");
        alloy(cache, "mithril", 3, "#forge:dusts/silver", "#forge:dusts/silver", "#forge:dusts/coldiron", "#forge:ingots/mercury");
        alloy(cache, "pewter", 3, "#forge:dusts/tin", "#forge:dusts/copper", "#forge:dusts/lead");
        List<String> steel = new ArrayList<>();
        for (int i = 0; i < 8; i++) steel.add("#forge:dusts/iron");
        steel.add("#forge:dusts/coal");
        alloy(cache, "steel", 8, steel.toArray(String[]::new));
    }

    private void alloy(HashCache cache, String name, int count, String... ingredients) throws IOException {
        shapeless(cache, name + "_blend", List.of(ingredients), "basemetals:" + name + "_blend", count);
    }

    private void writeCompatibilityRecipes(HashCache cache) throws IOException {
        writeMekanismRecipes(cache);
        writeThermalRecipes(cache);
        writeTinkersRecipes(cache);
        writeEnderIoRecipes(cache);
    }

    private void writeMekanismRecipes(HashCache cache) throws IOException {
        for (String name : PROCESSING_MATERIALS) {
            String root = "compat/mekanism/" + name + "/";
            mekanismItemToItem(cache, root + "dirty_dust_to_dust", "mekanism:enriching",
                    "#forge:dirty_dusts/" + name, "basemetals:" + name + "_powder", 1);
            mekanismItemToItem(cache, root + "clump_to_dirty_dust", "mekanism:crushing",
                    "#forge:clumps/" + name, "basemetals:" + name + "_powder_dirty", 1);
            mekanismItemChemicalToItem(cache, root + "shard_to_clump", "mekanism:purifying",
                    "#forge:shards/" + name, "mekanism:oxygen", 1,
                    "basemetals:" + name + "_clump", 1);
            mekanismItemChemicalToItem(cache, root + "crystal_to_shard", "mekanism:injecting",
                    "#forge:crystals/" + name, "mekanism:hydrogen_chloride", 1,
                    "basemetals:" + name + "_shard", 1);
            mekanismCrystallizing(cache, root + "clean_slurry_to_crystal", name);
            mekanismWashing(cache, root + "dirty_slurry_to_clean_slurry", name);

            mekanismItemToItem(cache, root + "ore_to_dust", "mekanism:enriching",
                    "#forge:ores/" + name, "basemetals:" + name + "_powder", 2);
            mekanismItemChemicalToItem(cache, root + "ore_to_clump", "mekanism:purifying",
                    "#forge:ores/" + name, "mekanism:oxygen", 1,
                    "basemetals:" + name + "_clump", 3);
            mekanismItemChemicalToItem(cache, root + "ore_to_shard", "mekanism:injecting",
                    "#forge:ores/" + name, "mekanism:hydrogen_chloride", 1,
                    "basemetals:" + name + "_shard", 4);
            mekanismDissolution(cache, root + "ore_to_dirty_slurry", name);
            mekanismItemToItem(cache, root + "ingot_to_dust", "mekanism:crushing",
                    "#forge:ingots/" + name, "basemetals:" + name + "_powder", 1);
        }
    }

    private void mekanismItemToItem(HashCache cache, String id, String type, String input,
            String outputItem, int count) throws IOException {
        JsonObject root = object("type", type);
        root.add("input", object("ingredient", ingredient(input)));
        root.add("output", result(outputItem, count));
        saveConditionalRecipe(cache, root, id, "mekanism");
    }

    private void mekanismItemChemicalToItem(HashCache cache, String id, String type, String input,
            String gas, int gasAmount, String outputItem, int count) throws IOException {
        JsonObject root = object("type", type);
        root.add("itemInput", object("ingredient", ingredient(input)));
        root.add("chemicalInput", object("amount", gasAmount, "gas", gas));
        root.add("output", result(outputItem, count));
        saveConditionalRecipe(cache, root, id, "mekanism");
    }

    private void mekanismCrystallizing(HashCache cache, String id, String material) throws IOException {
        JsonObject root = object("type", "mekanism:crystallizing", "chemicalType", "slurry");
        root.add("input", object("amount", 200, "slurry", "basemetals:clean_" + material));
        root.add("output", result("basemetals:" + material + "_crystal", 1));
        saveConditionalRecipe(cache, root, id, "mekanism");
    }

    private void mekanismWashing(HashCache cache, String id, String material) throws IOException {
        JsonObject root = object("type", "mekanism:washing");
        root.add("fluidInput", object("amount", 5, "tag", "minecraft:water"));
        root.add("slurryInput", object("amount", 1, "slurry", "basemetals:dirty_" + material));
        root.add("output", object("amount", 1, "slurry", "basemetals:clean_" + material));
        saveConditionalRecipe(cache, root, id, "mekanism");
    }

    private void mekanismDissolution(HashCache cache, String id, String material) throws IOException {
        JsonObject root = object("type", "mekanism:dissolution");
        root.add("itemInput", object("ingredient", ingredient("#forge:ores/" + material)));
        root.add("gasInput", object("amount", 1, "gas", "mekanism:sulfuric_acid"));
        root.add("output", object("amount", 1000, "slurry", "basemetals:dirty_" + material,
                "chemicalType", "slurry"));
        saveConditionalRecipe(cache, root, id, "mekanism");
    }

    private void writeThermalRecipes(HashCache cache) throws IOException {
        for (MaterialDefinition material : MaterialCatalogue.ALL) {
            String name = material.name();
            String root = "compat/thermal/" + name + "/";
            thermalItemMachine(cache, root + "ingot_pulverizing", "thermal:pulverizer",
                    "#forge:ingots/" + name, "basemetals:" + name + "_powder", 1, 4000);
            thermalItemMachine(cache, root + "dust_furnace", "thermal:furnace",
                    "#forge:dusts/" + name, "basemetals:" + name + "_ingot", 1, 2000);
            thermalCrucible(cache, root + "ingot_crucible", name);
            if (material.hasOre()) {
                thermalItemMachine(cache, root + "ore_pulverizing", "thermal:pulverizer",
                        "#forge:ores/" + name, "basemetals:" + name + "_powder", 2, 4000);
            }
            if (material.hasEquipment()) {
                thermalPress(cache, root + "plate_press", "#forge:ingots/" + name,
                        null, 1, "basemetals:" + name + "_plate");
                thermalPress(cache, root + "gear_press", "#forge:ingots/" + name,
                        "thermal:press_gear_die", 4, "basemetals:" + name + "_gear");
            }
        }
        for (Map.Entry<String, List<AlloyComponent>> alloy : ALLOYS.entrySet()) {
            JsonObject recipe = object("type", "thermal:smelter", "energy", 6400);
            JsonArray inputs = new JsonArray();
            alloy.getValue().forEach(component -> inputs.add(thermalAlloyIngredient(component)));
            recipe.add("ingredients", inputs);
            JsonArray outputs = new JsonArray();
            outputs.add(result("basemetals:" + alloy.getKey() + "_ingot", ALLOY_OUTPUTS.get(alloy.getKey())));
            recipe.add("result", outputs);
            saveConditionalRecipe(cache, recipe, "compat/thermal/alloys/" + alloy.getKey(), "thermal_expansion");
        }
    }

    private void thermalItemMachine(HashCache cache, String id, String type, String input,
            String output, int count, int energy) throws IOException {
        JsonObject root = object("type", type, "energy", energy);
        root.add("ingredient", ingredient(input));
        JsonArray results = new JsonArray(); results.add(result(output, count)); root.add("result", results);
        saveConditionalRecipe(cache, root, id, "thermal_expansion");
    }

    private void thermalCrucible(HashCache cache, String id, String material) throws IOException {
        JsonObject root = object("type", "thermal:crucible", "energy", 2000);
        root.add("ingredient", ingredient("#forge:ingots/" + material));
        JsonArray results = new JsonArray();
        results.add(object("fluid", "basemetals:" + material, "amount", 90));
        root.add("result", results);
        saveConditionalRecipe(cache, root, id, "thermal_expansion");
    }

    private void thermalPress(HashCache cache, String id, String input, String die,
            int count, String output) throws IOException {
        JsonObject root = object("type", "thermal:press");
        if (die == null) {
            root.add("ingredient", ingredient(input));
        } else {
            JsonArray inputs = new JsonArray();
            JsonObject metal = ingredient(input); metal.addProperty("count", count); inputs.add(metal);
            inputs.add(ingredient(die)); root.add("ingredients", inputs);
        }
        JsonArray results = new JsonArray(); results.add(result(output, 1)); root.add("result", results);
        saveConditionalRecipe(cache, root, id, "thermal_expansion");
    }

    private static JsonObject thermalAlloyIngredient(AlloyComponent component) {
        JsonArray accepted = new JsonArray();
        String tag = component.ingredient().substring(1);
        accepted.add(object("tag", tag));
        if (tag.startsWith("forge:dusts/")) {
            String material = tag.substring("forge:dusts/".length());
            if (MaterialCatalogue.BY_NAME.containsKey(material)
                    || material.equals("iron") || material.equals("gold")) {
                accepted.add(object("tag", "forge:ingots/" + material));
            }
        }
        JsonObject result = object("count", component.count()); result.add("value", accepted); return result;
    }

    private void writeEnderIoRecipes(HashCache cache) throws IOException {
        for (MaterialDefinition material : MaterialCatalogue.ALL) {
            String name = material.name();
            enderSag(cache, "compat/enderio/" + name + "/ingot_sag_milling",
                    "#forge:ingots/" + name, "basemetals:" + name + "_powder", 1);
            if (material.hasOre()) {
                enderSag(cache, "compat/enderio/" + name + "/ore_sag_milling",
                        "#forge:ores/" + name, "basemetals:" + name + "_powder", 2);
            }
        }
        for (Map.Entry<String, List<AlloyComponent>> alloy : ALLOYS.entrySet()) {
            JsonObject root = object("type", "enderio_machines:alloy_smelting", "energy", 6400,
                    "experience", 0.3F);
            JsonArray inputs = new JsonArray();
            for (AlloyComponent component : alloy.getValue()) {
                JsonObject counted = object("count", component.count());
                counted.add("ingredient", ingredient(component.ingredient())); inputs.add(counted);
            }
            root.add("inputs", inputs);
            root.add("result", result("basemetals:" + alloy.getKey() + "_ingot",
                    ALLOY_OUTPUTS.get(alloy.getKey())));
            saveConditionalRecipe(cache, root, "compat/enderio/alloys/" + alloy.getKey(), "enderio_machines");
        }
    }

    private void enderSag(HashCache cache, String id, String input, String output, int count) throws IOException {
        JsonObject root = object("type", "enderio_machines:sagmilling", "energy", 2400);
        root.add("input", ingredient(input));
        JsonArray outputs = new JsonArray(); outputs.add(result(output, count)); root.add("outputs", outputs);
        saveConditionalRecipe(cache, root, id, "enderio_machines");
    }

    private void writeTinkersRecipes(HashCache cache) throws IOException {
        for (MaterialDefinition material : MaterialCatalogue.ALL) {
            String name = material.name();
            int temperature = 769;
            tinkerMelting(cache, name, "ingot", 90, temperature);
            tinkerMelting(cache, name, "nugget", 10, temperature);
            if (material.hasEquipment()) {
                tinkerMelting(cache, name, "block", 810, temperature);
                tinkerMelting(cache, name, "plate", 90, temperature);
                tinkerMelting(cache, name, "rod", 45, temperature);
                tinkerMelting(cache, name, "gear", 360, temperature);
                tinkerCasting(cache, name, "ingot", 90, temperature, "ingot");
                tinkerCasting(cache, name, "nugget", 10, temperature, "nugget");
                tinkerCasting(cache, name, "plate", 90, temperature, "plate");
                tinkerCasting(cache, name, "rod", 45, temperature, "rod");
                tinkerCasting(cache, name, "gear", 360, temperature, "gear");
                tinkerBlockCasting(cache, name, temperature);
            }
        }
        for (String name : List.of("aquarium", "brass", "bronze", "cupronickel",
                "electrum", "invar", "mithril", "pewter")) {
            JsonObject root = object("type", "tconstruct:alloy", "temperature", 769);
            JsonArray inputs = new JsonArray();
            for (AlloyComponent component : ALLOYS.get(name)) {
                String fluidTag;
                if (component.ingredient().equals("#forge:dusts/prismarine")) {
                    fluidTag = "tconstruct:molten_glass";
                } else {
                    String material = component.ingredient().substring(component.ingredient().lastIndexOf('/') + 1);
                    fluidTag = "forge:molten_" + material;
                }
                inputs.add(object("tag", fluidTag, "amount", component.count() * 90));
            }
            root.add("inputs", inputs);
            root.add("result", object("fluid", "basemetals:" + name,
                    "amount", ALLOY_OUTPUTS.get(name) * 90));
            saveConditionalRecipe(cache, root, "compat/tconstruct/alloys/" + name, "tconstruct");
        }
    }

    private void tinkerMelting(HashCache cache, String material, String form, int amount,
            int temperature) throws IOException {
        String tagFamily = switch (form) {
            case "block" -> "storage_blocks";
            case "plate" -> "plates";
            case "rod" -> "rods";
            case "gear" -> "gears";
            default -> form + "s";
        };
        JsonObject root = object("type", "tconstruct:melting", "temperature", temperature,
                "time", Math.max(16, (int) Math.sqrt(amount) * 5));
        root.add("ingredient", ingredient("#forge:" + tagFamily + "/" + material));
        root.add("result", object("fluid", "basemetals:" + material, "amount", amount));
        saveConditionalRecipe(cache, root, "compat/tconstruct/melting/" + material + "/" + form, "tconstruct");
    }

    private void tinkerCasting(HashCache cache, String material, String form, int amount,
            int temperature, String cast) throws IOException {
        for (boolean reusable : List.of(true, false)) {
            JsonObject root = object("type", "tconstruct:casting_table",
                    "cooling_time", Math.max(16, (int) Math.sqrt(amount) * 5));
            root.add("cast", ingredient("#tconstruct:casts/" + (reusable ? "multi_use/" : "single_use/") + cast));
            if (!reusable) root.addProperty("cast_consumed", true);
            root.add("fluid", object("tag", "forge:molten_" + material, "amount", amount));
            root.add("result", result("basemetals:" + material + "_" + form, 1));
            saveConditionalRecipe(cache, root, "compat/tconstruct/casting/" + material + "/" + form
                    + (reusable ? "_gold_cast" : "_sand_cast"), "tconstruct");
        }
    }

    private void tinkerBlockCasting(HashCache cache, String material, int temperature) throws IOException {
        JsonObject root = object("type", "tconstruct:casting_basin", "cooling_time", 141);
        root.add("fluid", object("tag", "forge:molten_" + material, "amount", 810));
        root.add("result", result("basemetals:" + material + "_block", 1));
        saveConditionalRecipe(cache, root, "compat/tconstruct/casting/" + material + "/block", "tconstruct");
    }

    private void writeTinkersMaterials(HashCache cache) throws IOException {
        int order = 0;
        for (MaterialDefinition material : MaterialCatalogue.ALL) {
            if (!material.hasEquipment()) continue;
            String name = material.name();
            JsonObject definition = object("craftable", false,
                    "tier", Math.max(1, Math.min(4, material.toolLevel() + 1)),
                    "sortOrder", order++, "hidden", false);
            save(cache, definition, "data/basemetals/tinkering/materials/definition/" + name + ".json");

            JsonObject stats = new JsonObject();
            stats.add("tconstruct:extra", new JsonObject());
            stats.add("tconstruct:handle", object("durability", 1.0F, "miningSpeed", 1.0F,
                    "attackSpeed", 1.0F, "attackDamage", 1.0F));
            stats.add("tconstruct:head", object("durability", material.toolDurability(),
                    "miningSpeed", material.toolEfficiency(), "harvestTier", harvestTier(material.toolLevel()),
                    "attack", material.baseAttackDamage()));
            stats.add("tconstruct:limb", object("durability", material.toolDurability(),
                    "drawSpeed", 0.0F, "velocity", Math.min(0.3F, (float) material.hardness() / 50.0F),
                    "accuracy", 0.0F));
            stats.add("tconstruct:grip", object("durability", 1.0F, "accuracy", 0.0F,
                    "meleeAttack", material.baseAttackDamage()));
            JsonObject statsRoot = new JsonObject(); statsRoot.add("stats", stats);
            save(cache, statsRoot, "data/basemetals/tinkering/materials/stats/" + name + ".json");
            JsonObject traits = new JsonObject(); traits.add("default", new JsonArray());
            save(cache, traits, "data/basemetals/tinkering/materials/traits/" + name + ".json");

            JsonObject render = object("color", String.format(Locale.ROOT, "%06X", material.colour()),
                    "skipUniqueTexture", true, "luminosity", 0);
            render.add("fallbacks", array("metal"));
            save(cache, render, "assets/basemetals/tinkering/materials/" + name + ".json");

            tinkerMaterialIngredient(cache, name, "ingot", 1, 1);
            tinkerMaterialIngredient(cache, name, "nugget", 1, 9);
            tinkerMaterialIngredient(cache, name, "storage_blocks", 9, 1);

            JsonObject melting = object("type", "tconstruct:material_melting", "input", "basemetals:" + name,
                    "temperature", 769);
            melting.add("result", object("fluid", "basemetals:" + name, "amount", 90));
            saveConditionalRecipe(cache, melting, "compat/tconstruct/materials/melting/" + name, "tconstruct");
            JsonObject casting = object("type", "tconstruct:material_fluid", "temperature", 769,
                    "output", "basemetals:" + name);
            casting.add("fluid", object("tag", "forge:molten_" + name, "amount", 90));
            saveConditionalRecipe(cache, casting, "compat/tconstruct/materials/casting/" + name, "tconstruct");
        }
    }

    private void tinkerMaterialIngredient(HashCache cache, String material, String family,
            int value, int needed) throws IOException {
        String normalized = family.equals("ingot") || family.equals("nugget") ? family + "s" : family;
        JsonObject root = object("type", "tconstruct:material", "value", value, "needed", needed,
                "material", "basemetals:" + material);
        root.add("ingredient", ingredient("#forge:" + normalized + "/" + material));
        saveConditionalRecipe(cache, root, "compat/tconstruct/materials/" + material + "/" + family, "tconstruct");
    }

    private static String harvestTier(int level) {
        return switch (level) {
            case 0 -> "minecraft:wood";
            case 1 -> "minecraft:stone";
            case 2 -> "minecraft:iron";
            case 3 -> "minecraft:diamond";
            default -> "minecraft:netherite";
        };
    }

    private void saveConditionalRecipe(HashCache cache, JsonObject recipe, String id,
            String modId) throws IOException {
        JsonArray conditions = new JsonArray();
        conditions.add(object("type", "forge:mod_loaded", "modid", modId));
        recipe.add("conditions", conditions);
        save(cache, recipe, "data/basemetals/recipes/" + id + ".json");
    }

    private static AlloyComponent component(String material, int count) {
        return new AlloyComponent("#forge:dusts/" + material, count);
    }

    private record AlloyComponent(String ingredient, int count) {}

    private void writeAdvancements(HashCache cache) throws IOException {
        for (String id : ADVANCEMENTS) {
            JsonObject root = new JsonObject();
            String parent = advancementParent(id);
            if (parent != null) root.addProperty("parent", "basemetals:" + parent);
            JsonObject display = new JsonObject();
            display.add("icon", object("item", advancementIcon(id)));
            display.add("title", object("translate", "advancements.basemetals." + id + ".title"));
            display.add("description", object("translate", "advancements.basemetals." + id + ".description"));
            display.addProperty("frame", parent == null ? "task" : "goal");
            display.addProperty("show_toast", true); display.addProperty("announce_to_chat", true);
            root.add("display", display);
            JsonObject criterion = new JsonObject();
            criterion.addProperty("trigger", "minecraft:inventory_changed");
            JsonObject conditions = new JsonObject();
            JsonArray items = new JsonArray();
            advancementItems(id).forEach(item -> items.add(object("items", array(item))));
            conditions.add("items", items); criterion.add("conditions", conditions);
            JsonObject criteria = new JsonObject(); criteria.add("has_item", criterion); root.add("criteria", criteria);
            save(cache, root, "data/basemetals/advancements/" + id + ".json");
        }
    }

    private void writeLanguages(HashCache cache) throws IOException {
        Path legacyRoot = findLegacyLanguageRoot();
        for (String locale : LOCALES) {
            JsonObject language = readLegacyLanguage(legacyRoot.resolve(locale + ".lang"));
            ModContent.blocksById().keySet().forEach(id -> addIfAbsent(language, "block.basemetals." + id,
                    ModContent.fluids().containsKey(id) ? "Molten " + title(id) : title(id)));
            ModContent.itemsById().keySet().forEach(id -> addIfAbsent(language, "item.basemetals." + id,
                    id.endsWith("_bucket") ? title(id.substring(0, id.length() - 7)) + " Bucket" : title(id)));
            addIfAbsent(language, "itemGroup.basemetals.blocks", "Base Metals Blocks");
            addIfAbsent(language, "itemGroup.basemetals.items", "Base Metals Items");
            addIfAbsent(language, "itemGroup.basemetals.tools", "Base Metals Tools");
            addIfAbsent(language, "itemGroup.basemetals.combat", "Base Metals Combat");
            for (MaterialDefinition material : MaterialCatalogue.ALL) {
                addIfAbsent(language, "material.basemetals." + material.name(), title(material.name()));
            }
            for (String material : PROCESSING_MATERIALS) {
                addIfAbsent(language, "slurry.basemetals.dirty_" + material,
                        "Dirty " + title(material) + " Slurry");
                addIfAbsent(language, "slurry.basemetals.clean_" + material,
                        "Clean " + title(material) + " Slurry");
            }
            for (String advancement : ADVANCEMENTS) {
                addIfAbsent(language, "advancements.basemetals." + advancement + ".title", title(advancement));
                addIfAbsent(language, "advancements.basemetals." + advancement + ".description",
                        "Progress through Base Metals: " + title(advancement) + ".");
            }
            save(cache, language, "assets/basemetals/lang/" + locale + ".json");
        }
    }

    private static Path findLegacyLanguageRoot() throws IOException {
        Path current = Path.of("").toAbsolutePath();
        for (int depth = 0; depth < 5 && current != null; depth++, current = current.getParent()) {
            Path candidate = current.resolve("reference/1.12/lang");
            if (Files.isDirectory(candidate)) return candidate;
        }
        throw new IOException("Could not locate retained 1.12 Base Metals language files");
    }

    private static JsonObject readLegacyLanguage(Path path) throws IOException {
        JsonObject language = new JsonObject();
        for (String rawLine : Files.readAllLines(path, StandardCharsets.UTF_8)) {
            String line = rawLine.strip();
            if (line.isEmpty() || line.startsWith("#")) continue;
            int separator = line.indexOf('=');
            if (separator < 1) continue;
            String key = modernLanguageKey(line.substring(0, separator).strip());
            String value = line.substring(separator + 1);
            language.addProperty(key, value);
        }
        return language;
    }

    private static String modernLanguageKey(String key) {
        if (key.startsWith("item.basemetals.") && key.endsWith(".name")) {
            return key.substring(0, key.length() - ".name".length());
        }
        if (key.startsWith("tile.basemetals.") && key.endsWith(".name")) {
            return "block.basemetals." + key.substring("tile.basemetals.".length(),
                    key.length() - ".name".length());
        }
        if (key.startsWith("achievement.")) {
            String suffix = key.substring("achievement.".length());
            if (suffix.endsWith(".desc")) {
                return "advancements.basemetals." + suffix.substring(0, suffix.length() - 5) + ".description";
            }
            return "advancements.basemetals." + suffix + ".title";
        }
        return key;
    }

    private static void addIfAbsent(JsonObject language, String key, String value) {
        if (!language.has(key)) language.addProperty(key, value);
    }

    private void writeChestLoot(HashCache cache) throws IOException {
        Map<String, String> targets = new LinkedHashMap<>();
        targets.put("abandoned_mineshaft", "minecraft:chests/abandoned_mineshaft");
        targets.put("desert_pyramid", "minecraft:chests/desert_pyramid");
        targets.put("end_city_treasure", "minecraft:chests/end_city_treasure");
        targets.put("jungle_temple", "minecraft:chests/jungle_temple");
        targets.put("nether_bridge", "minecraft:chests/nether_bridge");
        targets.put("simple_dungeon", "minecraft:chests/simple_dungeon");
        targets.put("spawn_bonus_chest", "minecraft:chests/spawn_bonus_chest");
        targets.put("stronghold_corridor", "minecraft:chests/stronghold_corridor");
        targets.put("stronghold_crossing", "minecraft:chests/stronghold_crossing");
        targets.put("village_armorer", "minecraft:chests/village/village_armorer");
        targets.put("village_toolsmith", "minecraft:chests/village/village_toolsmith");
        targets.put("village_weaponsmith", "minecraft:chests/village/village_weaponsmith");
        Map<String, String> templates = new LinkedHashMap<>();
        targets.keySet().forEach(id -> templates.put(id, id));
        templates.put("village_armorer", "village_blacksmith");
        templates.put("village_toolsmith", "village_blacksmith");
        templates.put("village_weaponsmith", "village_blacksmith");
        JsonArray global = new JsonArray();
        for (Map.Entry<String, String> target : targets.entrySet()) {
            String id = "basemetals:" + target.getKey();
            global.add(id);
            JsonObject modifier = new JsonObject();
            modifier.addProperty("type", "basemetals:add_table");
            JsonArray conditions = new JsonArray();
            conditions.add(object("condition", "forge:loot_table_id", "loot_table_id", target.getValue()));
            modifier.add("conditions", conditions);
            modifier.addProperty("table", "basemetals:chests/inject/" + target.getKey());
            save(cache, modifier, "data/basemetals/loot_modifiers/" + target.getKey() + ".json");
            save(cache, legacyChestTable(templates.get(target.getKey())),
                    "data/basemetals/loot_tables/chests/inject/" + target.getKey() + ".json");
        }
        JsonObject root = new JsonObject(); root.addProperty("replace", false); root.add("entries", global);
        save(cache, root, "data/forge/loot_modifiers/global_loot_modifiers.json");
    }

    private JsonObject legacyChestTable(String template) throws IOException {
        Path source = legacyReference.resolve("alt/chests/" + template + ".json");
        if (!Files.isRegularFile(source)) {
            throw new IOException("Missing retained 1.12 chest-loot reference: " + source);
        }
        JsonObject table = JsonParser.parseString(Files.readString(source, StandardCharsets.UTF_8)).getAsJsonObject();
        modernizeLootJson(table);
        table.addProperty("type", "minecraft:chest");
        return table;
    }

    private static void modernizeLootJson(JsonElement element) {
        if (element.isJsonArray()) {
            element.getAsJsonArray().forEach(GeneratedData::modernizeLootJson);
            return;
        }
        if (!element.isJsonObject()) return;
        JsonObject object = element.getAsJsonObject();
        new ArrayList<>(object.keySet()).stream()
                .filter(key -> key.startsWith("__comment"))
                .forEach(object::remove);
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            if ((entry.getKey().equals("type") || entry.getKey().equals("function")
                    || entry.getKey().equals("condition"))
                    && entry.getValue().isJsonPrimitive()
                    && entry.getValue().getAsJsonPrimitive().isString()
                    && !entry.getValue().getAsString().contains(":")) {
                entry.setValue(new com.google.gson.JsonPrimitive("minecraft:" + entry.getValue().getAsString()));
            } else {
                modernizeLootJson(entry.getValue());
            }
        }
    }

    private void writeManifest(HashCache cache) throws IOException {
        List<String> blocks = ModContent.blocksById().keySet().stream().map(id -> "basemetals:" + id).toList();
        List<String> items = ModContent.itemsById().keySet().stream().map(id -> "basemetals:" + id).toList();
        List<String> buckets = ModContent.fluids().keySet().stream()
                .map(id -> "basemetals:" + id + "_bucket").toList();
        List<String> sourceFluids = ModContent.fluids().keySet().stream()
                .map(id -> "basemetals:" + id).toList();
        List<String> flowingFluids = ModContent.fluids().keySet().stream()
                .map(id -> "basemetals:flowing_" + id).toList();

        Path runtimeManifest = legacyReference.resolve("runtime/registry_manifest.json");
        JsonObject legacy = JsonParser.parseString(Files.readString(runtimeManifest, StandardCharsets.UTF_8))
                .getAsJsonObject();
        save(cache, legacy, "data/basemetals/registry_manifest_1_12.json");

        List<String> restoredBlocks = difference(blocks, strings(legacy.getAsJsonArray("blocks")), List.of());
        List<String> restoredItems = difference(items, strings(legacy.getAsJsonArray("items")), buckets);
        List<String> restoredFluids = difference(sourceFluids, strings(legacy.getAsJsonArray("fluids")), List.of());

        JsonObject root = new JsonObject();
        root.addProperty("format", 1);
        root.addProperty("source", "Base Metals 1.18 catalogue");
        root.add("blocks", array(blocks));
        root.add("items", array(items));
        root.add("fluids", array(ModContent.fluids().keySet().stream().flatMap(id ->
                java.util.stream.Stream.of("basemetals:" + id, "basemetals:flowing_" + id)).toList()));
        root.add("entities", array("basemetals:custom_arrow", "basemetals:custom_bolt"));
        root.add("recipe_serializers", array("basemetals:crushing"));
        root.add("loot_modifier_serializers", array("basemetals:add_table"));
        root.add("restored_blocks", array(restoredBlocks));
        root.add("restored_items", array(restoredItems));
        root.add("restored_fluids", array(restoredFluids));
        root.add("new_items", array(buckets));
        root.add("new_fluids", array(flowingFluids));
        root.add("new_entities", array("basemetals:custom_arrow", "basemetals:custom_bolt"));
        root.add("new_recipe_serializers", array("basemetals:crushing"));
        root.add("new_loot_modifier_serializers", array("basemetals:add_table"));
        save(cache, root, "data/basemetals/registry_manifest.json");
    }

    private static List<String> difference(List<String> current, List<String> baseline, List<String> exclusions) {
        LinkedHashSet<String> remaining = new LinkedHashSet<>(current);
        remaining.removeAll(baseline);
        remaining.removeAll(exclusions);
        return List.copyOf(remaining);
    }

    private static List<String> strings(JsonArray values) {
        List<String> result = new ArrayList<>();
        values.forEach(value -> result.add(value.getAsString()));
        return result;
    }

    private void itemTag(HashCache cache, String family, String material, List<String> values) throws IOException {
        tag(cache, "forge", "items/" + family + "/" + material, values);
    }
    private void blockTag(HashCache cache, String family, String material, List<String> values) throws IOException {
        tag(cache, "forge", "blocks/" + family + "/" + material, values);
    }
    private void tag(HashCache cache, String namespace, String path, List<String> values) throws IOException {
        JsonObject root = new JsonObject(); root.addProperty("replace", false); root.add("values", array(values));
        save(cache, root, "data/" + namespace + "/tags/" + path + ".json");
    }
    private void uncheckedTag(HashCache cache, String namespace, String path, List<String> values) {
        try { tag(cache, namespace, path, values); } catch (IOException e) { throw new RuntimeException(e); }
    }
    private static void addAggregate(Map<String, List<String>> aggregate, String path, String value) {
        aggregate.computeIfAbsent(path, ignored -> new ArrayList<>()).add(value);
    }

    private void shaped(HashCache cache, String id, List<String> pattern, Map<Character, String> keys,
            String result, int count) throws IOException {
        JsonObject root = object("type", "minecraft:crafting_shaped");
        root.add("pattern", array(pattern));
        JsonObject key = new JsonObject();
        keys.entrySet().stream().sorted(Map.Entry.comparingByKey())
                .forEach(entry -> key.add(entry.getKey().toString(), ingredient(entry.getValue())));
        root.add("key", key); root.add("result", result(result, count));
        save(cache, root, "data/basemetals/recipes/" + id + ".json");
    }
    private void shapeless(HashCache cache, String id, List<String> ingredients, String result, int count) throws IOException {
        JsonObject root = object("type", "minecraft:crafting_shapeless");
        JsonArray values = new JsonArray(); ingredients.forEach(value -> values.add(ingredient(value)));
        root.add("ingredients", values); root.add("result", result(result, count));
        save(cache, root, "data/basemetals/recipes/" + id + ".json");
    }
    private void smelting(HashCache cache, String id, String input, String result, float xp, int time,
            String type) throws IOException {
        JsonObject root = object("type", type, "ingredient", ingredient(input), "result", result,
                "experience", xp, "cookingtime", time);
        save(cache, root, "data/basemetals/recipes/" + id + ".json");
    }
    private void crushing(HashCache cache, String id, String input, String result, int count) throws IOException {
        JsonObject root = object("type", "basemetals:crushing", "ingredient", ingredient(input),
                "result", result(result, count));
        save(cache, root, "data/basemetals/recipes/" + id + ".json");
    }
    private static JsonObject ingredient(String value) {
        return value.startsWith("#") ? object("tag", value.substring(1)) : object("item", value);
    }
    private static JsonObject result(String item, int count) {
        JsonObject result = object("item", item); if (count != 1) result.addProperty("count", count); return result;
    }

    private static List<String> entries(String material, String suffix, String extra) {
        List<String> result = new ArrayList<>(); result.add("basemetals:" + material + "_" + suffix);
        if (extra != null) result.add(extra); return result;
    }
    private static List<String> entries(String material, String suffix) {
        return entries(material, suffix, (String) null);
    }
    private static List<String> entries(String material, String suffix, List<String> extras) {
        List<String> result = new ArrayList<>(); result.add("basemetals:" + material + "_" + suffix);
        result.addAll(extras); return result;
    }
    private static String advancementIcon(String id) {
        return switch (id) {
            case "blocktastic" -> "basemetals:copper_block";
            case "geologist" -> "basemetals:iron_crackhammer";
            case "metallurgy" -> "basemetals:bronze_blend";
            case "angel_of_death" -> "basemetals:mithril_sword";
            case "scuba_diver" -> "basemetals:aquarium_helmet";
            case "demon_slayer" -> "basemetals:coldiron_sword";
            case "juggernaut" -> "basemetals:adamantine_chestplate";
            case "moon_boots" -> "basemetals:starsteel_boots";
            default -> id.endsWith("_maker") ? "basemetals:" + id.substring(0, id.length() - 6) + "_blend"
                    : "basemetals:copper_ingot";
        };
    }

    private static String advancementParent(String id) {
        return switch (id) {
            case "this_is_new" -> null;
            case "blocktastic", "geologist", "metallurgy", "demon_slayer", "juggernaut", "moon_boots" -> "this_is_new";
            case "angel_of_death" -> "mithril_maker";
            case "scuba_diver" -> "aquarium_maker";
            default -> "metallurgy";
        };
    }

    private static List<String> advancementItems(String id) {
        return switch (id) {
            case "angel_of_death" -> equipmentSet("mithril", true);
            case "demon_slayer" -> equipmentSet("coldiron", true);
            case "scuba_diver" -> equipmentSet("aquarium", false);
            case "juggernaut" -> equipmentSet("adamantine", false);
            default -> List.of(advancementIcon(id));
        };
    }

    private static List<String> equipmentSet(String material, boolean sword) {
        List<String> items = new ArrayList<>(List.of(
                "basemetals:" + material + "_helmet",
                "basemetals:" + material + "_chestplate",
                "basemetals:" + material + "_leggings",
                "basemetals:" + material + "_boots"));
        if (sword) items.add("basemetals:" + material + "_sword");
        return items;
    }
    private static String title(String id) {
        StringBuilder result = new StringBuilder();
        for (String word : id.replace("coldiron", "cold_iron").replace("starsteel", "starsteel").split("_")) {
            if (result.length() > 0) result.append(' ');
            result.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1).toLowerCase(Locale.ROOT));
        }
        return result.toString();
    }
    private void save(HashCache cache, JsonObject value, String relative) throws IOException {
        DataProvider.save(GSON, cache, value, output.resolve(relative));
    }
    private static JsonArray array(String... values) { return array(List.of(values)); }
    private static JsonArray array(Iterable<String> values) {
        JsonArray array = new JsonArray(); values.forEach(array::add); return array;
    }
    private static JsonObject object(Object... values) {
        JsonObject result = new JsonObject();
        for (int i = 0; i < values.length; i += 2) {
            String key = (String) values[i]; Object value = values[i + 1];
            if (value instanceof String text) result.addProperty(key, text);
            else if (value instanceof Number number) result.addProperty(key, number);
            else if (value instanceof Boolean bool) result.addProperty(key, bool);
            else if (value instanceof com.google.gson.JsonElement json) result.add(key, json);
            else throw new IllegalArgumentException("Unsupported JSON value " + value);
        }
        return result;
    }

    @Override public String getName() { return "Base Metals catalogue data"; }
}
