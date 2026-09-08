package zone.moddev.mc.basemetals.compat;

import zone.moddev.mc.basemetals.BaseMetals;

import mekanism.api.MekanismAPI;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.fml.ModList;

/** Live registry and recipe checks for the optional Mekanism runtime. */
@GameTestHolder(BaseMetals.MOD_ID)
@PrefixGameTestTemplate(false)
public final class MekanismGameTests {
    private static final String EMPTY = "empty";

    private MekanismGameTests() {}

    @GameTest(template = EMPTY)
    public static void mekanismProcessingChainLoadsWhenInstalled(GameTestHelper helper) {
        if (!ModList.get().isLoaded("mekanism")) {
            helper.succeed();
            return;
        }

        for (String material : MekanismCompat.PROCESSING_MATERIALS) {
            require(helper, MekanismAPI.slurryRegistry().containsKey(
                    new ResourceLocation(BaseMetals.MOD_ID, "dirty_" + material)),
                    "Missing dirty Mekanism slurry for " + material);
            require(helper, MekanismAPI.slurryRegistry().containsKey(
                    new ResourceLocation(BaseMetals.MOD_ID, "clean_" + material)),
                    "Missing clean Mekanism slurry for " + material);
        }

        long recipes = helper.getLevel().getRecipeManager().getRecipes().stream()
                .filter(recipe -> recipe.getId().getNamespace().equals(BaseMetals.MOD_ID))
                .filter(recipe -> recipe.getId().getPath().startsWith("compat/mekanism/"))
                .count();
        require(helper, recipes == 88L,
                "Expected 88 live Mekanism compatibility recipes, found " + recipes);
        helper.succeed();
    }

    private static void require(GameTestHelper helper, boolean condition, String message) {
        if (!condition) helper.fail(message);
    }
}
