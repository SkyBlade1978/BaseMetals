package com.mcmoddev.basemetals;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.mcmoddev.basemetals.proxy.CommonProxy;
import com.mcmoddev.basemetals.util.Config;
import com.mcmoddev.lib.data.SharedStrings;
import com.mcmoddev.lib.integration.IntegrationManager;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLConstructionEvent;
import net.minecraftforge.fml.common.event.FMLFingerprintViolationEvent;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.versioning.InvalidVersionSpecificationException;

/**
 * This is the entry point for this Mod. If you are writing your own Mod that uses this Mod, the
 * classes of interest to you are the init classes (classes in package com.mcmoddev.basemetals.init)
 * and the CrusherRecipeRegistry class (in package com.mcmoddev.basemetals.registry). Note that you
 * should add 'dependencies = "required-after:basemetals"' to your &#64;Mod annotation (e.g. <br>
 * &#64;Mod(modid = "moremetals", name="More Metals!", version = "1.2.3", dependencies =
 * "required-after:basemetals") <br>
 * )
 *
 * @author Jasmine Iwanek
 *
 */
@Mod(
	modid = BaseMetals.MODID,
	name = BaseMetals.NAME,
	version = BaseMetals.VERSION,
	dependencies = "required-after:forge@[14.21.1.2387,);after:tconstruct;after:ic2;before:buildingbricks",
	acceptedMinecraftVersions = "[1.12,)",
	certificateFingerprint = "@FINGERPRINT@",
	updateJSON = BaseMetals.UPDATEJSON)
public final class BaseMetals {

	@Instance
	public static BaseMetals instance;

	/** ID of this Mod. */
	public static final String MODID = "basemetals";

	/** Display name of this Mod. */
	protected static final String NAME = "Base Metals";

	/**
	 * Version number, in Major.Minor.Patch format. The minor number is increased whenever a change
	 * is made that has the potential to break compatibility with other mods that depend on this
	 * one.
	 */
	protected static final String VERSION = "2.5.0-beta5";

	protected static final String UPDATEJSON = SharedStrings.UPDATE_JSON_URL
			+ "BaseMetals/master/update.json";

	private static final String PROXY_BASE = SharedStrings.MMD_PROXY_GROUP + MODID
			+ SharedStrings.DOT_PROXY_DOT;

	@SidedProxy(
				clientSide = PROXY_BASE + SharedStrings.CLIENTPROXY,
				serverSide = PROXY_BASE + SharedStrings.SERVERPROXY)
	public static CommonProxy proxy;

	public static final Logger logger = LogManager.getFormatterLogger(BaseMetals.MODID);

	static {
		// Forge says this needs to be statically initialized here.
		FluidRegistry.enableUniversalBucket();
	}

	public static String getVersion() {
		return VERSION;
	}

	/**
	 * 
	 * @param event The Event.
	 */
	@EventHandler
	public static void constructing(final FMLConstructionEvent event) {
		try {
			IntegrationManager.INSTANCE.setup(event);
		} catch (InvalidVersionSpecificationException e) {
			logger.error("Error loading version information for plugins: %s", e);
		}
		
		Config.init();
	}
	
	@EventHandler
	public void onFingerprintViolation(final FMLFingerprintViolationEvent event) {
		logger.warn(SharedStrings.INVALID_FINGERPRINT);
	}

	/**
	 *
	 * @param event The Event.
	 */
	@EventHandler
	public static void preInit(final FMLPreInitializationEvent event) {
		proxy.preInit(event);
	}

	@EventHandler
	public static void init(final FMLInitializationEvent event) {
		proxy.init(event);
	}

	/**
	 * 
	 * @param event The Event.
	 */
	@EventHandler
	public static void postInit(final FMLPostInitializationEvent event) {
		proxy.postInit(event);
		//com.mcmoddev.lib.init.Materials.dumpRegistry();
		//com.mcmoddev.lib.init.Recipes.dumpFurnaceRecipes();
		//com.mcmoddev.lib.init.ItemGroups.dumpTabs();
	}

	@SubscribeEvent
	public void onRemapBlock(final RegistryEvent.MissingMappings<Block> event) {
		proxy.onRemapBlock(event);
	}

	@SubscribeEvent
	public void onRemapItem(final RegistryEvent.MissingMappings<Item> event) {
		proxy.onRemapItem(event);
	}
}
