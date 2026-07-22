package com.mcmoddev.basemetals.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

class ResourceIntegrityTest {
    private static final Path MAIN = Path.of("src/main/resources");
    private static final Path GENERATED = Path.of("src/generated/resources");

    @Test
    void everyJsonResourceParses() throws Exception {
        int count = 0;
        for (Path root : List.of(MAIN, GENERATED)) {
            try (Stream<Path> paths = Files.walk(root)) {
                for (Path path : (Iterable<Path>) paths.filter(Files::isRegularFile)
                        .filter(file -> file.toString().endsWith(".json"))::iterator) {
                    assertNotNull(read(path), path.toString());
                    count++;
                }
            }
        }
        assertTrue(count > 2_000, "Expected the complete generated data set, found " + count);
    }

    @Test
    void manifestHasModelsLootAndTranslationsForEveryRegisteredId() throws Exception {
        JsonObject manifest = read(resource("data/basemetals/registry_manifest.json")).getAsJsonObject();
        Set<String> blocks = strings(manifest.getAsJsonArray("blocks"));
        Set<String> items = strings(manifest.getAsJsonArray("items"));
        Set<String> fluids = strings(manifest.getAsJsonArray("fluids"));
        assertEquals(360, blocks.size());
        assertEquals(1115, items.size());
        assertEquals(72, fluids.size());
        assertEquals(2, manifest.getAsJsonArray("entities").size());

        JsonObject language = read(resource("assets/basemetals/lang/en_us.json")).getAsJsonObject();
        for (String block : blocks) {
            String id = path(block);
            assertResource("assets/basemetals/blockstates/" + id + ".json");
            assertTrue(language.has("block.basemetals." + id), "Missing block translation for " + block);
            if (!fluids.contains(block)) {
                assertResource("data/basemetals/loot_tables/blocks/" + id + ".json");
            }
        }
        for (String item : items) {
            String id = path(item);
            assertResource("assets/basemetals/models/item/" + id + ".json");
            assertTrue(language.has("item.basemetals." + id), "Missing item translation for " + item);
        }
    }

    @Test
    void registryManifestMatchesTheAudited112BaselinePlusExplicitNewIds() throws Exception {
        JsonObject legacy = read(resource("data/basemetals/registry_manifest_1_12.json")).getAsJsonObject();
        JsonObject current = read(resource("data/basemetals/registry_manifest.json")).getAsJsonObject();

        assertEquals(2, legacy.get("format").getAsInt());
        assertEquals(6_851, legacy.get("block_state_count").getAsInt());
        assertEquals(354, legacy.getAsJsonArray("blocks").size());
        assertEquals(1_038, legacy.getAsJsonArray("items").size());
        assertEquals(33, legacy.getAsJsonArray("fluids").size());
        assertBaselinePlusAdditions(legacy, current, "blocks", "restored_blocks");
        assertBaselinePlusAdditions(legacy, current, "items", "restored_items", "new_items");
        assertBaselinePlusAdditions(legacy, current, "fluids", "restored_fluids", "new_fluids");
        assertEquals(strings(current.getAsJsonArray("entities")), strings(current.getAsJsonArray("new_entities")));
        assertEquals(strings(current.getAsJsonArray("recipe_serializers")),
                strings(current.getAsJsonArray("new_recipe_serializers")));
        assertEquals(strings(current.getAsJsonArray("loot_modifier_serializers")),
                strings(current.getAsJsonArray("new_loot_modifier_serializers")));
    }

    @Test
    void packaged112NumericMapCoversEveryCapturedLegacyBlock() throws Exception {
        JsonObject legacy = read(resource("data/basemetals/registry_manifest_1_12.json")).getAsJsonObject();
        JsonObject numericMap = read(MAIN.resolve("data/basemetals/migration/legacy_block_ids_1_12.json"))
                .getAsJsonObject();
        JsonObject ids = numericMap.getAsJsonObject("ids");
        assertEquals(608, ids.size());
        assertEquals("basemetals:adamantine", ids.get("460").getAsString());
        Set<String> mapped = new LinkedHashSet<>();
        ids.entrySet().forEach(entry -> mapped.add(entry.getValue().getAsString()));
        assertTrue(mapped.containsAll(strings(legacy.getAsJsonArray("blocks"))),
                "The pre-flattening map omits captured Base Metals block IDs");
    }

    @Test
    void localModelAndTextureReferencesResolve() throws Exception {
        JsonObject manifest = read(resource("data/basemetals/registry_manifest.json")).getAsJsonObject();
        Deque<String> pending = new ArrayDeque<>();
        strings(manifest.getAsJsonArray("items")).forEach(item -> pending.add("item/" + path(item)));
        for (String block : strings(manifest.getAsJsonArray("blocks"))) {
            JsonElement state = read(resource("assets/basemetals/blockstates/" + path(block) + ".json"));
            visit(state, (key, value) -> {
                if (key.equals("model") && value.isJsonPrimitive()
                        && value.getAsString().startsWith("basemetals:")) {
                    pending.add(value.getAsString().substring("basemetals:".length()));
                }
            });
        }
        Set<String> visited = new LinkedHashSet<>();
        while (!pending.isEmpty()) {
            String model = pending.removeFirst();
            if (!visited.add(model)) continue;
            Path modelPath = resource("assets/basemetals/models/" + model + ".json");
            assertTrue(Files.isRegularFile(modelPath), "Missing model basemetals:" + model);
            visit(read(modelPath), (key, value) -> {
                if (!(value.isJsonPrimitive() && value.getAsJsonPrimitive().isString())) return;
                String reference = value.getAsString();
                if (!reference.startsWith("basemetals:") || reference.contains("#")) return;
                String id = reference.substring("basemetals:".length());
                if (key.equals("parent") || key.equals("model")) {
                    pending.add(id);
                } else if (key.startsWith("texture:")) {
                    assertResource("assets/basemetals/textures/" + id + ".png");
                }
            });
        }
    }

    @Test
    void modelResourcesUseFlattened118Names() throws Exception {
        Path modelRoot = MAIN.resolve("assets/basemetals/models");
        try (Stream<Path> paths = Files.walk(modelRoot)) {
            for (Path model : (Iterable<Path>) paths.filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".json"))::iterator) {
                String json = Files.readString(model, StandardCharsets.UTF_8);
                assertTrue(!json.contains("\"blocks/") && !json.contains("\"items/"),
                        model + " uses a pre-flattening plural texture path");
                for (String obsolete : List.of("block/half_slab", "block/upper_slab", "block/wall_post",
                        "block/wall_side", "block/trapdoor_bottom", "block/trapdoor_top", "block/trapdoor_open")) {
                    assertTrue(!json.contains("\"parent\": \"" + obsolete + "\""),
                            model + " uses obsolete parent " + obsolete);
                }
            }
        }
        assertTrue(!Files.exists(MAIN.resolve("assets/minecraft/models/item/oak_door.json")),
                "Base Metals must not replace vanilla door models");
    }

    @Test
    void tagsRecipesLootAndProviderReferenceExistingBaseMetalsContent() throws Exception {
        JsonObject manifest = read(resource("data/basemetals/registry_manifest.json")).getAsJsonObject();
        Set<String> blocks = strings(manifest.getAsJsonArray("blocks"));
        Set<String> items = strings(manifest.getAsJsonArray("items"));
        Set<String> fluids = strings(manifest.getAsJsonArray("fluids"));

        validateTagTree(GENERATED.resolve("data"), "items", items);
        validateTagTree(GENERATED.resolve("data"), "blocks", blocks);
        validateTagTree(GENERATED.resolve("data"), "fluids", fluids);

        for (Path root : List.of(MAIN.resolve("data/basemetals"), GENERATED.resolve("data/basemetals"))) {
            if (!Files.isDirectory(root)) continue;
            try (Stream<Path> paths = Files.walk(root)) {
                for (Path file : (Iterable<Path>) paths.filter(Files::isRegularFile)
                        .filter(path -> path.toString().endsWith(".json"))::iterator) {
                    visit(read(file), (key, value) -> {
                        if (!(value.isJsonPrimitive() && value.getAsJsonPrimitive().isString())) return;
                        String reference = value.getAsString();
                        if (!reference.startsWith("basemetals:")) return;
                        if (key.equals("item") || key.equals("name")) {
                            assertTrue(items.contains(reference), file + " references missing item " + reference);
                        } else if (key.equals("fluid")) {
                            assertTrue(fluids.contains(reference), file + " references missing fluid " + reference);
                        } else if (key.equals("block")) {
                            assertTrue(blocks.contains(reference), file + " references missing block " + reference);
                        }
                    });
                }
            }
        }
    }

    @Test
    void crushingAliasesAndOptionalProcessingContractsAreGenerated() throws Exception {
        JsonObject crushing = read(resource("data/basemetals/recipes/copper_ore_crushing.json")).getAsJsonObject();
        assertEquals("basemetals:crushing", crushing.get("type").getAsString());
        assertEquals("forge:ores/copper", crushing.getAsJsonObject("ingredient").get("tag").getAsString());
        assertEquals(2, crushing.getAsJsonObject("result").get("count").getAsInt());

        JsonObject adamantAlias = read(resource("data/forge/tags/items/ingots/adamantium.json")).getAsJsonObject();
        assertTrue(strings(adamantAlias.getAsJsonArray("values")).contains("#forge:ingots/adamantine"));
        JsonObject sprocket = read(resource("data/forge/tags/items/sprockets/steel.json")).getAsJsonObject();
        assertTrue(strings(sprocket.getAsJsonArray("values")).contains("#forge:gears/steel"));

        assertConditionalRecipe("compat/mekanism/adamantine/ore_to_dirty_slurry", "mekanism");
        assertConditionalRecipe("compat/thermal/alloys/bronze", "thermal_expansion");
        assertConditionalRecipe("compat/tconstruct/alloys/bronze", "tconstruct");
        assertConditionalRecipe("compat/enderio/alloys/bronze", "enderio_machines");
    }

    @Test
    void chestInjectionTablesRetainEveryLegacyEntry() throws Exception {
        Map<String, String> templates = Map.ofEntries(
                Map.entry("abandoned_mineshaft", "abandoned_mineshaft"),
                Map.entry("desert_pyramid", "desert_pyramid"),
                Map.entry("end_city_treasure", "end_city_treasure"),
                Map.entry("jungle_temple", "jungle_temple"),
                Map.entry("nether_bridge", "nether_bridge"),
                Map.entry("simple_dungeon", "simple_dungeon"),
                Map.entry("spawn_bonus_chest", "spawn_bonus_chest"),
                Map.entry("stronghold_corridor", "stronghold_corridor"),
                Map.entry("stronghold_crossing", "stronghold_crossing"),
                Map.entry("village_armorer", "village_blacksmith"),
                Map.entry("village_toolsmith", "village_blacksmith"),
                Map.entry("village_weaponsmith", "village_blacksmith"));
        for (Map.Entry<String, String> target : templates.entrySet()) {
            JsonObject legacy = read(Path.of("reference/1.12/alt/chests/" + target.getValue() + ".json"))
                    .getAsJsonObject();
            JsonObject generated = read(resource("data/basemetals/loot_tables/chests/inject/"
                    + target.getKey() + ".json")).getAsJsonObject();
            assertEquals(legacy.getAsJsonArray("pools").size(), generated.getAsJsonArray("pools").size(), target.getKey());
            for (int pool = 0; pool < legacy.getAsJsonArray("pools").size(); pool++) {
                JsonArray legacyEntries = legacy.getAsJsonArray("pools").get(pool).getAsJsonObject()
                        .getAsJsonArray("entries");
                JsonArray generatedEntries = generated.getAsJsonArray("pools").get(pool).getAsJsonObject()
                        .getAsJsonArray("entries");
                assertEquals(legacyEntries.size(), generatedEntries.size(), target.getKey() + " pool " + pool);
                assertEquals(itemNames(legacyEntries), itemNames(generatedEntries),
                        target.getKey() + " changed legacy loot entries");
            }
        }
    }

    private static void assertBaselinePlusAdditions(JsonObject legacy, JsonObject current,
            String registry, String... additions) {
        Set<String> expected = new LinkedHashSet<>(strings(legacy.getAsJsonArray(registry)));
        for (String addition : additions) {
            expected.addAll(strings(current.getAsJsonArray(addition)));
        }
        assertEquals(expected, strings(current.getAsJsonArray(registry)), registry);
    }

    private static Set<String> itemNames(JsonArray entries) {
        Set<String> result = new LinkedHashSet<>();
        entries.forEach(value -> {
            JsonObject entry = value.getAsJsonObject();
            if (entry.has("name")) result.add(entry.get("name").getAsString());
        });
        return result;
    }

    private static void validateTagTree(Path dataRoot, String kind, Set<String> registry) throws Exception {
        if (!Files.isDirectory(dataRoot)) return;
        try (Stream<Path> paths = Files.walk(dataRoot)) {
            for (Path file : (Iterable<Path>) paths.filter(Files::isRegularFile)
                    .filter(path -> path.toString().replace('\\', '/').contains("/tags/" + kind + "/"))
                    .filter(path -> path.toString().endsWith(".json"))::iterator) {
                JsonArray values = read(file).getAsJsonObject().getAsJsonArray("values");
                for (JsonElement value : values) {
                    String id = value.isJsonObject() ? value.getAsJsonObject().get("id").getAsString() : value.getAsString();
                    if (id.startsWith("basemetals:")) {
                        assertTrue(registry.contains(id), file + " references missing " + kind + " ID " + id);
                    }
                }
            }
        }
    }

    private static void assertConditionalRecipe(String id, String modId) throws Exception {
        JsonObject recipe = read(resource("data/basemetals/recipes/" + id + ".json")).getAsJsonObject();
        JsonObject condition = recipe.getAsJsonArray("conditions").get(0).getAsJsonObject();
        assertEquals("forge:mod_loaded", condition.get("type").getAsString(), id);
        assertEquals(modId, condition.get("modid").getAsString(), id);
    }

    private static void visit(JsonElement value, KeyValueConsumer consumer) {
        if (value.isJsonObject()) {
            JsonObject object = value.getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
                if (entry.getKey().equals("textures") && entry.getValue().isJsonObject()) {
                    entry.getValue().getAsJsonObject().entrySet().forEach(texture ->
                            consumer.accept("texture:" + texture.getKey(), texture.getValue()));
                } else {
                    consumer.accept(entry.getKey(), entry.getValue());
                }
                visit(entry.getValue(), consumer);
            }
        } else if (value.isJsonArray()) {
            value.getAsJsonArray().forEach(child -> visit(child, consumer));
        }
    }

    private static Set<String> strings(JsonArray array) {
        Set<String> values = new LinkedHashSet<>();
        array.forEach(value -> values.add(value.getAsString()));
        return values;
    }

    private static String path(String id) {
        return id.substring(id.indexOf(':') + 1);
    }

    private static void assertResource(String relative) {
        assertTrue(Files.isRegularFile(resource(relative)), "Missing resource " + relative);
    }

    private static Path resource(String relative) {
        Path generated = GENERATED.resolve(relative);
        if (Files.isRegularFile(generated)) return generated;
        return MAIN.resolve(relative);
    }

    private static JsonElement read(Path path) throws IOException {
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader);
        }
    }

    @FunctionalInterface
    private interface KeyValueConsumer {
        void accept(String key, JsonElement value);
    }
}
