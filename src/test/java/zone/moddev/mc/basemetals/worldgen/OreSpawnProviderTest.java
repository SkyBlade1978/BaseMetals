package zone.moddev.mc.basemetals.worldgen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.StreamSupport;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

class OreSpawnProviderTest {
    private record ExpectedRule(String selector, int minY, int maxY, double frequency,
            String distribution, List<String> hostTags) {}

    @Test
    void providerIsOreOnlyAndLeavesBaseMetalsCopperDisabled() throws IOException {
        Path path = Path.of("src/main/resources/data/basemetals/orespawn/provider.json");
        JsonObject root = JsonParser.parseString(Files.readString(path)).getAsJsonObject();
        assertEquals(3, root.get("schema_version").getAsInt());
        assertEquals(1, root.get("provider_revision").getAsInt());
        assertEquals(11, root.getAsJsonObject("ores").size());
        assertTrue(root.getAsJsonObject("rocks").entrySet().isEmpty());
        assertTrue(root.getAsJsonObject("terrain_dimensions").entrySet().isEmpty());
        assertFalse(root.getAsJsonObject("ores").has("basemetals:ore/antimony"));
        assertFalse(root.getAsJsonObject("ores").has("basemetals:ore/bismuth"));
        JsonObject copper = root.getAsJsonObject("ores").getAsJsonObject("basemetals:ore/copper")
                .getAsJsonObject("dimension_selectors").getAsJsonObject("orespawn:all_except_nether_end");
        assertFalse(copper.get("enabled").getAsBoolean());
        assertEquals(-16, copper.get("min_y").getAsInt());
        assertEquals(112, copper.get("max_y").getAsInt());
    }

    @Test
    void enabledRulesUseTheSharedVeinContract() throws IOException {
        JsonObject ores = JsonParser.parseString(Files.readString(
                Path.of("src/main/resources/data/basemetals/orespawn/provider.json")))
                .getAsJsonObject().getAsJsonObject("ores");
        ores.entrySet().stream().filter(entry -> !entry.getKey().endsWith("/copper")).forEach(entry -> {
            JsonObject ore = entry.getValue().getAsJsonObject();
            JsonObject rule = ore.has("dimensions")
                    ? ore.getAsJsonObject("dimensions").entrySet().iterator().next().getValue().getAsJsonObject()
                    : ore.getAsJsonObject("dimension_selectors").entrySet().iterator().next().getValue().getAsJsonObject();
            assertEquals("vein", rule.get("pattern").getAsString());
            assertEquals(4, rule.get("min_quantity").getAsInt());
            assertEquals(11, rule.get("max_quantity").getAsInt());
            assertEquals(8, rule.get("spread").getAsInt());
            assertEquals(4, rule.get("vertical_spread").getAsInt());
            assertEquals(4, rule.get("node_size").getAsInt());
            assertEquals(16, rule.get("length").getAsInt());
            assertEquals(0, rule.get("discard_chance_on_air_exposure").getAsDouble());
            assertFalse(ore.get("retrogen").getAsBoolean());
        });
    }

    @Test
    void everyRuleMatchesThePublishedDistribution() throws IOException {
        JsonObject ores = JsonParser.parseString(Files.readString(
                Path.of("src/main/resources/data/basemetals/orespawn/provider.json")))
                .getAsJsonObject().getAsJsonObject("ores");
        Map<String, ExpectedRule> expected = new LinkedHashMap<>();
        expected.put("coldiron", new ExpectedRule("minecraft:the_nether", 0, 127, 5, "uniform",
                List.of("forge:netherrack")));
        expected.put("adamantine", new ExpectedRule("minecraft:the_nether", 0, 127, 2, "uniform",
                List.of("forge:netherrack")));
        expected.put("starsteel", new ExpectedRule("minecraft:the_end", 0, 254, 5, "uniform",
                List.of("forge:end_stones")));
        expected.put("tin", ordinary(-64, 128, 10, "triangle"));
        expected.put("lead", ordinary(-64, 64, 5, "triangle"));
        expected.put("zinc", ordinary(-64, 96, 5, "triangle"));
        expected.put("silver", ordinary(-64, 32, 4, "bottom_triangle"));
        expected.put("mercury", ordinary(-64, 32, 3, "bottom_triangle"));
        expected.put("nickel", ordinary(-32, 96, 1, "triangle"));
        expected.put("platinum", ordinary(-64, 16, 0.125, "bottom_triangle"));
        expected.put("copper", ordinary(-16, 112, 10, "triangle"));

        assertEquals(expected.size(), ores.size());
        expected.forEach((name, contract) -> {
            JsonObject ore = ores.getAsJsonObject("basemetals:ore/" + name);
            assertEquals("basemetals:" + name + "_ore", ore.get("block").getAsString());
            boolean explicitDimension = contract.selector().startsWith("minecraft:");
            JsonObject rules = ore.getAsJsonObject(explicitDimension ? "dimensions" : "dimension_selectors");
            assertEquals(1, rules.size());
            JsonObject rule = rules.getAsJsonObject(contract.selector());
            assertEquals(contract.minY(), rule.get("min_y").getAsInt());
            assertEquals(contract.maxY(), rule.get("max_y").getAsInt());
            assertEquals(contract.frequency(), rule.get("frequency").getAsDouble(), 0.000001);
            assertEquals(contract.distribution(), rule.get("height_distribution").getAsString());
            assertEquals(contract.hostTags(), StreamSupport.stream(rule.getAsJsonArray("host_tags").spliterator(), false)
                    .map(element -> element.getAsString()).toList());
            if (!explicitDimension) {
                assertEquals(List.of("sedimentary", "metamorphic", "igneous_intrusive", "igneous_volcanic"),
                        StreamSupport.stream(rule.getAsJsonArray("host_families").spliterator(), false)
                                .map(element -> element.getAsString()).toList());
            }
            assertEquals(!name.equals("copper"), rule.get("enabled").getAsBoolean());
        });
    }

    private static ExpectedRule ordinary(int minY, int maxY, double frequency, String distribution) {
        return new ExpectedRule("orespawn:all_except_nether_end", minY, maxY, frequency, distribution,
                List.of("minecraft:stone_ore_replaceables", "minecraft:deepslate_ore_replaceables"));
    }
}
