package zone.moddev.mc.basemetals.material;

import static zone.moddev.mc.basemetals.material.MaterialDefinition.Kind.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Ordered catalogue used by registration, data generation, and validation. */
public final class MaterialCatalogue {
    public static final List<MaterialDefinition> ALL = List.of(
            material("adamantine", RARE_ORE, 12, 100, 0, 0x53393F, true),
            material("antimony", ORE, 1, 1, 1, 0xD8E3DE, true),
            material("aquarium", RARE_ALLOY, 4, 10, 15, 0x0000FF, false),
            material("bismuth", ORE, 1, 1, 1, 0xDDD7CB, true),
            material("brass", ALLOY, 3.5, 3, 9, 0xFFE374, false),
            material("bronze", ALLOY, 8, 4, 4.5, 0xF7A54F, false),
            material("coldiron", RARE_ORE, 7, 7, 7, 0xC7CEF0, true),
            material("copper", ORE, 4, 4, 5, 0xFF9F78, false),
            material("cupronickel", ALLOY, 6, 6, 6, 0xC8AB6F, false),
            material("electrum", ALLOY, 5, 4, 10, 0xFFF2B3, false),
            material("invar", ALLOY, 9, 10, 3, 0xD2CDB8, false),
            material("lead", ORE, 1, 1, 1, 0x7B7B7B, false),
            material("mercury", MERCURY, 1, 1, 1, 0xE2E2E2, false),
            material("mithril", RARE_ALLOY, 9, 9, 9, 0xF4FFFF, false),
            material("nickel", ORE, 4, 4, 7, 0xEEFFEB, true),
            material("pewter", ALLOY, 1, 1, 1, 0x92969F, false),
            material("platinum", RARE_ORE, 3, 5, 15, 0xF2FFFF, true),
            material("silver", ORE, 5, 4, 6, 0xFFFFFF, false),
            material("starsteel", RARE_ORE, 10, 25, 12, 0x53393F, true),
            material("steel", ALLOY, 8, 15, 2, 0xD5E3E5, false),
            material("tin", ORE, 3, 1, 2, 0xFFF7EE, false),
            material("zinc", ORE, 1, 1, 1, 0xBCBCBC, true));

    public static final Map<String, MaterialDefinition> BY_NAME;

    static {
        Map<String, MaterialDefinition> byName = new LinkedHashMap<>();
        ALL.forEach(material -> {
            if (byName.put(material.name(), material) != null) {
                throw new IllegalStateException("Duplicate material " + material.name());
            }
        });
        BY_NAME = Map.copyOf(byName);
    }

    private MaterialCatalogue() {}

    public static MaterialDefinition get(String name) {
        MaterialDefinition material = BY_NAME.get(name);
        if (material == null) {
            throw new IllegalArgumentException("Unknown Base Metals material: " + name);
        }
        return material;
    }

    private static MaterialDefinition material(String name, MaterialDefinition.Kind kind,
            double hardness, double strength, double magic, int colour, boolean processing) {
        return new MaterialDefinition(name, kind, hardness, strength, magic, colour, processing);
    }
}
