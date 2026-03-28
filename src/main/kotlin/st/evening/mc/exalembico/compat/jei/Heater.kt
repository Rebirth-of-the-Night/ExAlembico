package st.evening.mc.exalembico.compat.jei

import mezz.jei.api.IGuiHelper
import mezz.jei.api.gui.IDrawable
import mezz.jei.api.gui.IRecipeLayout
import mezz.jei.api.ingredients.IIngredientRenderer
import mezz.jei.api.ingredients.IIngredients
import mezz.jei.api.ingredients.VanillaTypes
import mezz.jei.api.recipe.IRecipeCategory
import mezz.jei.api.recipe.IRecipeWrapper
import mezz.jei.api.recipe.IStackHelper
import net.minecraft.client.resources.I18n
import st.evening.mc.exalembico.ExAlembicoConsts
import st.evening.mc.exalembico.block.heater.HeaterType
import st.evening.mc.exalembico.recipe.HeaterRecipe
import st.evening.mc.exalembico.util.formatTicksAsSeconds

class HeaterJeiRecipeWrapper(
    private val recipe: HeaterRecipe,
    private val stackHelper: IStackHelper
) : IRecipeWrapper {
    override fun getIngredients(ingredients: IIngredients) {
        ingredients.setInputLists(VanillaTypes.ITEM, listOf(stackHelper.toItemStackList(recipe.fuel)))
        ingredients.setOutputLists(
            ExAJeiPlugin.ING_TYPE_HEAT, listOf(recipe.heatLevels.toArray().apply { sort() }.asList())
        )
    }

    override fun getTooltipStrings(mouseX: Int, mouseY: Int): List<String> = when (mouseX) {
        in 7..<70 if mouseY in 40..<45 -> listOf(recipe.duration.formatTicksAsSeconds())
        else -> emptyList()
    }
}

class HeaterJeiRecipeCategory(type: HeaterType, guiHelper: IGuiHelper) : IRecipeCategory<HeaterJeiRecipeWrapper> {
    private val _uid: String
    private val unlocalizedTitle: String
    private val _background: IDrawable = guiHelper.createDrawable(ExAJeiPlugin.JEI_TEXTURE, 0, 112, 78, 50)

    private val heatRenderer: IIngredientRenderer<Int> = when (type) {
        HeaterType.HEATER -> HeatJeiIngredientRenderer.HEATER
    }

    init {
        val name = type.getName()
        _uid = "${ExAlembicoConsts.MOD_ID}.$name"
        unlocalizedTitle = "${ExAlembicoConsts.MOD_ID}.jei.category.$name"
    }

    override fun getUid(): String = _uid
    override fun getTitle(): String = I18n.format(unlocalizedTitle)
    override fun getModName(): String = ExAlembicoConsts.MOD_ID
    override fun getBackground(): IDrawable = _background

    override fun setRecipe(layout: IRecipeLayout, recipe: HeaterJeiRecipeWrapper, ingredients: IIngredients) {
        layout.itemStacks.run {
            init(0, true, 29, 21)
            set(ingredients)
        }
        layout.getIngredientsGroup(ExAJeiPlugin.ING_TYPE_HEAT).run {
            init(0, false, heatRenderer, 32, 7, 13, 14, 0, 0)
            set(ingredients)
        }
    }
}
