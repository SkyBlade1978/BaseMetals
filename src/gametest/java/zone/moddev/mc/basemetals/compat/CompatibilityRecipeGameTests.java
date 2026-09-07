package zone.moddev.mc.basemetals.compat;

import zone.moddev.mc.basemetals.BaseMetals;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.fml.ModList;

/** Proves optional-mod serializers accept every packaged compatibility recipe. */
@GameTestHolder(BaseMetals.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CompatibilityRecipeGameTests {
    private static final String EMPTY = "empty";

    private CompatibilityRecipeGameTests() {}

    @GameTest(template = EMPTY)
    public static void thermalRecipesLoadWhenInstalled(GameTestHelper helper) {
        requireRecipeCount(helper, "thermal_expansion", "thermal", 130L);
    }

    @GameTest(template = EMPTY)
    public static void tinkersConstructRecipesLoadWhenInstalled(GameTestHelper helper) {
        requireRecipeCount(helper, "tconstruct", "tconstruct", 472L);
    }

    @GameTest(template = EMPTY)
    public static void enderIoRecipesLoadWhenInstalled(GameTestHelper helper) {
        requireRecipeCount(helper, "enderio", "enderio", 44L);
    }

    private static void requireRecipeCount(GameTestHelper helper, String modId,
            String recipeFolder, long expected) {
        if (!ModList.get().isLoaded(modId)) {
            helper.succeed();
            return;
        }

        String pathPrefix = "compat/" + recipeFolder + "/";
        long actual = helper.getLevel().getRecipeManager().getRecipes().stream()
                .filter(recipe -> recipe.getId().getNamespace().equals(BaseMetals.MOD_ID))
                .filter(recipe -> recipe.getId().getPath().startsWith(pathPrefix))
                .count();
        if (actual != expected) {
            helper.fail("Expected " + expected + " live " + modId
                    + " compatibility recipes, found " + actual);
            return;
        }
        helper.succeed();
    }
}
