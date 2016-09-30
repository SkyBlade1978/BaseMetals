package cyano.basemetals.init;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import cyano.basemetals.material.MetalMaterial;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item.ToolMaterial;
import net.minecraft.item.ItemArmor.ArmorMaterial;
import net.minecraftforge.common.util.EnumHelper;
import net.minecraftforge.fml.common.FMLLog;

/**
 * This class initializes all of the metal materials in Base Metals. It also
 * contains utility methods for looking up materials by name and finding the
 * tool and armor material equivalents for a given metal.
 *
 * @author DrCyano
 *
 */
public abstract class Materials {

	private static Map<String, MetalMaterial> allMaterials = new HashMap<>();
	private static Map<MetalMaterial, ArmorMaterial> armorMaterialMap = new HashMap<>();
	private static Map<MetalMaterial, ToolMaterial> toolMaterialMap = new HashMap<>();
	protected static List<MetalMaterial> materials = new LinkedList<MetalMaterial>();

	public static MetalMaterial adamantine;
	public static MetalMaterial antimony;
	public static MetalMaterial aquarium;
	public static MetalMaterial bismuth;
	public static MetalMaterial brass;
	public static MetalMaterial bronze;
	public static MetalMaterial coldiron;
	public static MetalMaterial copper;
	public static MetalMaterial cupronickel;
	public static MetalMaterial electrum;
	public static MetalMaterial invar;
	public static MetalMaterial lead;
	public static MetalMaterial mithril;
	public static MetalMaterial nickel;
	public static MetalMaterial pewter;
	public static MetalMaterial platinum;
	public static MetalMaterial silver;
	public static MetalMaterial starsteel;
	public static MetalMaterial steel;
	public static MetalMaterial tin;
	public static MetalMaterial zinc;

	// vanilla imports
	public static MetalMaterial vanilla_wood;
	public static MetalMaterial vanilla_stone;
	public static MetalMaterial vanilla_iron;
	public static MetalMaterial vanilla_gold;
	public static MetalMaterial vanilla_diamond;

	private static boolean initDone = false;

	/**
	 *
	 */
	public static void init() {
		if (initDone)
			return;

		// Vanilla Materials
		vanilla_wood = addMaterial("wood", 2, 2, 6, 0xFF000000);
		vanilla_stone = addMaterial("stone", 5, 4, 2, 0xFF000000);
		vanilla_iron = addMaterial("iron", 8, 8, 4.5, 0xFF000000);
		vanilla_gold = addMaterial("gold", 1, 1, 10, 0xFF000000);
		vanilla_diamond = addMaterial("diamond", 10, 15, 4, 0xFF000000);

		// Mod Metals
		adamantine = addMaterial("adamantine", 12, 100, 0, 0xFF53393F).setBlastResistance(2000f);
		antimony = addMaterial("antimony", 1, 1, 1, 0xFFD8E3DE);
		aquarium = addMaterial("aquarium", 4, 10, 15, 0xFF000000);
		bismuth = addMaterial("bismuth", 1, 1, 1, 0xFFDDD7CB);
		brass = addMaterial("brass", 3.5, 3, 9, 0xFFFFE374);
		bronze = addMaterial("bronze", 8, 4, 4.5, 0xFFF7A54F);
		coldiron = addMaterial("coldiron", 7, 7, 7, 0xFFC7CEF0);
		copper = addMaterial("copper", 4, 4, 5, 0xFFFF9F78);
		cupronickel = addMaterial("cupronickel", 6, 6, 6, 0xFFC8AB6F);
		electrum = addMaterial("electrum", 5, 4, 10, 0xFFFFF2B3);
		invar = addMaterial("invar", 9, 10, 3, 0xFFD2CDB8);
		lead = addMaterial("lead", 1, 1, 1, 0xFF7B7B7B).setBaseDamage(4f);
		mithril = addMaterial("mithril", 9, 9, 9, 0xFFF4FFFF);
		nickel = addMaterial("nickel", 4, 4, 7, 0xFFEEFFEB);
		pewter = addMaterial("pewter", 1, 1, 1, 0xFF92969F);
		platinum = addRareMaterial("platinum", 3, 5, 15, 0xFFF2FFFF);
		silver = addMaterial("silver", 5, 4, 6, 0xFFFFFFFF);
		starsteel = addMaterial("starsteel", 10, 25, 12, 0xFF53393F).setBlastResistance(2000f);
		steel = addMaterial("steel", 8, 15, 2, 0xFFD5E3E5);
		tin = addMaterial("tin", 3, 1, 2, 0xFFFFF7EE);
		zinc = addMaterial("zinc", 1, 1, 1, 0xFFBCBCBC);

		initDone = true;
	}

	protected static MetalMaterial addMaterial(String name, double hardness, double strength, double magic, int tintColor) {
		final MetalMaterial m = new MetalMaterial(name, (float) hardness, (float) strength, (float) magic, tintColor);
		registerMaterial(name, m);

		materials.add(m);
		return m;
	}

	protected static MetalMaterial addRareMaterial(String name, double hardness, double strength, double magic, int tintColor) {
		final MetalMaterial m = new MetalMaterial(name, (float) hardness, (float) strength, (float) magic, tintColor, true);
		registerMaterial(name, m);

		materials.add(m);
		return m;
	}

	protected static void registerMaterial(String name, MetalMaterial m) {

		allMaterials.put(name, m);

		final String enumName = m.getEnumName();
		final String texName = m.getName();
		final int[] protection = m.getDamageReductionArray();
		final int durability = m.getArmorMaxDamageFactor();
		final ArmorMaterial am = EnumHelper.addArmorMaterial(enumName, texName, durability, protection,
				m.getEnchantability(), SoundEvents.ITEM_ARMOR_EQUIP_IRON,
				(m.hardness > 10 ? (int) (m.hardness / 5) : 0));
		if (am == null)
			// uh-oh
			FMLLog.severe("Failed to create armor material enum for " + m);
		armorMaterialMap.put(m, am);
		FMLLog.info("Created armor material enum " + am);

		final ToolMaterial tm = EnumHelper.addToolMaterial(enumName, m.getToolHarvestLevel(), m.getToolDurability(),
				m.getToolEfficiency(), m.getBaseAttackDamage(), m.getEnchantability());
		if (tm == null)
			// uh-oh
			FMLLog.severe("Failed to create tool material enum for " + m);
		toolMaterialMap.put(m, tm);
		FMLLog.info("Created tool material enum " + tm);
	}

	/**
	 * Gets the armor material for a given metal
	 * 
	 * @param m
	 *            The metal of interest
	 * @return The armor material for this metal, or null if there isn't one
	 */
	public static ArmorMaterial getArmorMaterialFor(MetalMaterial m) {
		return armorMaterialMap.get(m);
	}

	/**
	 * Gets the tool material for a given metal
	 * 
	 * @param m
	 *            The metal of interest
	 * @return The tool material for this metal, or null if there isn't one
	 */
	public static ToolMaterial getToolMaterialFor(MetalMaterial m) {
		return toolMaterialMap.get(m);
	}

	/**
	 * Returns a list of all metal materials in Base Metals. All of the metals
	 * in this list are also available as static public members of this class.
	 * 
	 * @return A Collection of MetalMaterial instances.
	 */
	public static Collection<MetalMaterial> getAllMetals() {
		return allMaterials.values();
	}

	/**
	 * Gets a metal material by its name (e.g. "copper").
	 * 
	 * @param metalName
	 *            The name of a metal
	 * @return The material representing the named metal, or null if no metals
	 *         have been registered under that name.
	 */
	public static MetalMaterial getMetalByName(String metalName) {
		return allMaterials.get(metalName);
	}
}
