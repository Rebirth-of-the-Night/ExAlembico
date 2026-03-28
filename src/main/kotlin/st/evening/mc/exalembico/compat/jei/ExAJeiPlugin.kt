package st.evening.mc.exalembico.compat.jei

import mezz.jei.api.IModPlugin
import mezz.jei.api.IModRegistry
import mezz.jei.api.JEIPlugin
import mezz.jei.api.ingredients.IModIngredientRegistration
import mezz.jei.api.recipe.IIngredientType
import mezz.jei.api.recipe.IRecipeCategoryRegistration
import net.minecraft.item.ItemStack
import net.minecraft.util.ResourceLocation
import st.evening.mc.exalembico.ExAlembico
import st.evening.mc.exalembico.ExAlembicoConsts
import st.evening.mc.exalembico.block.alembic.AlembicType
import st.evening.mc.exalembico.block.alembic.GuiAlembic
import st.evening.mc.exalembico.block.heater.HeaterType
import st.evening.mc.exalembico.init.ExABlocks
import st.evening.mc.exalembico.recipe.ExARecipeManager

@JEIPlugin
class ExAJeiPlugin : IModPlugin {
    companion object {
        val JEI_TEXTURE: ResourceLocation = ExAlembico.resource("textures/gui/jei.png")
        val ING_TYPE_HEAT: IIngredientType<Int> = { Int::class.javaObjectType }
    }

    override fun registerIngredients(registry: IModIngredientRegistration) {
        registry.register(ING_TYPE_HEAT, emptyList(), HeatJeiIngredientHelper, HeatJeiIngredientRenderer.ALEMBIC)
    }

    override fun registerCategories(registry: IRecipeCategoryRegistration) {
        val guiHelper = registry.jeiHelpers.guiHelper
        registry.addRecipeCategories(AlembicJeiRecipeCategory(AlembicType.ALEMBIC, guiHelper))
        registry.addRecipeCategories(AlembicJeiRecipeCategory(AlembicType.ALEMBIC_NETHER, guiHelper))
        registry.addRecipeCategories(HeaterJeiRecipeCategory(HeaterType.HEATER, guiHelper))
    }

    override fun register(registry: IModRegistry) {
        val stackHelper = registry.jeiHelpers.stackHelper
        val guiHelper = registry.jeiHelpers.guiHelper

        val alembicUid = "${ExAlembicoConsts.MOD_ID}.alembic"
        val alembicFactory = AlembicJeiRecipeWrapper.Factory(AlembicType.ALEMBIC, stackHelper, guiHelper)
        registry.addRecipes(
            ExARecipeManager.alembicRecipes.mapTo(mutableListOf()) { alembicFactory.wrap(it) },
            alembicUid
        )
        registry.addRecipeCatalyst(ItemStack(ExABlocks.alembic, 1, 0), alembicUid)
        registry.addRecipeClickArea(GuiAlembic.Alembic::class.java, 54, 14, 20, 20, alembicUid)

        val netherAlembicUid = "${ExAlembicoConsts.MOD_ID}.alembic_nether"
        val netherAlembicFactory = AlembicJeiRecipeWrapper.Factory(AlembicType.ALEMBIC_NETHER, stackHelper, guiHelper)
        registry.addRecipes(
            ExARecipeManager.netherAlembicRecipes.mapTo(mutableListOf()) { netherAlembicFactory.wrap(it) },
            netherAlembicUid
        )
        registry.addRecipeCatalyst(ItemStack(ExABlocks.alembic, 1, 1), netherAlembicUid)
        registry.addRecipeClickArea(GuiAlembic.AlembicNether::class.java, 54, 14, 20, 20, netherAlembicUid)

        val heaterUid = "${ExAlembicoConsts.MOD_ID}.heater"
        registry.addRecipes(
            ExARecipeManager.heaterRecipes.mapTo(mutableListOf()) { HeaterJeiRecipeWrapper(it, stackHelper) },
            heaterUid
        )
        registry.addRecipeCatalyst(ItemStack(ExABlocks.heater), heaterUid)
    }
}
