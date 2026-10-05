package st.evening.mc.exalembico.compat.jei

import mezz.jei.api.IGuiHelper
import mezz.jei.api.gui.IDrawable
import mezz.jei.api.gui.IDrawableAnimated
import mezz.jei.api.gui.IDrawableStatic
import mezz.jei.api.gui.IRecipeLayout
import mezz.jei.api.ingredients.IIngredientRenderer
import mezz.jei.api.ingredients.IIngredients
import mezz.jei.api.ingredients.VanillaTypes
import mezz.jei.api.recipe.IRecipeCategory
import mezz.jei.api.recipe.IRecipeWrapper
import mezz.jei.api.recipe.IStackHelper
import net.minecraft.client.Minecraft
import net.minecraft.client.resources.I18n
import net.minecraft.item.ItemStack
import net.minecraftforge.fluids.FluidStack
import st.evening.mc.exalembico.ExAlembicoConsts
import st.evening.mc.exalembico.block.alembic.AlembicType
import st.evening.mc.exalembico.recipe.AlembicRecipe
import st.evening.mc.exalembico.util.formatPercentage
import st.evening.mc.exalembico.util.formatTicksAsSeconds

class AlembicJeiRecipeWrapper private constructor(
    private val recipe: AlembicRecipe,
    private val stackHelper: IStackHelper,
    guiHelper: IGuiHelper,
    progressBar: IDrawableStatic
) : IRecipeWrapper {
    private val progressBar: IDrawableAnimated = guiHelper.createAnimatedDrawable(
        progressBar, recipe.duration.coerceAtLeast(4), IDrawableAnimated.StartDirection.BOTTOM, false
    )

    private fun MutableList<List<ItemStack>>.setIngredient(index: Int, ingredient: Any) {
        this[index] = stackHelper.toItemStackList(ingredient)
    }

    override fun getIngredients(ingredients: IIngredients) {
        val inputItems = MutableList<List<ItemStack>>(3) { emptyList() }
        val inputFluids = MutableList<List<FluidStack>>(2) { emptyList() }
        val twoLeftInputs: Boolean
        when (val input = recipe.inputItems) {
            null -> twoLeftInputs = false
            is AlembicRecipe.InputItems.One -> {
                inputItems.setIngredient(0, input.input)
                twoLeftInputs = false
            }
            is AlembicRecipe.InputItems.Two -> {
                inputItems.setIngredient(0, input.input1)
                inputItems.setIngredient(1, input.input2)
                twoLeftInputs = true
            }
        }
        recipe.inputItem3?.let {
            inputItems.setIngredient(2, it)
        }
        recipe.inputFluid1?.let {
            inputFluids[0] = listOf(it)
        }
        recipe.inputFluid2?.let {
            inputFluids[1] = listOf(it)
        }
        ingredients.setInputLists(VanillaTypes.ITEM, inputItems)
        ingredients.setInputLists(VanillaTypes.FLUID, inputFluids)

        val outputItems = MutableList<List<ItemStack>>(4) { emptyList() }
        val outputFluids = MutableList<List<FluidStack>>(2) { emptyList() }
        when (val output = recipe.outputItems) {
            null -> {}
            is AlembicRecipe.OutputItems.One -> outputItems.setIngredient(if (twoLeftInputs) 1 else 0, output.output)
            is AlembicRecipe.OutputItems.Two -> {
                outputItems.setIngredient(0, output.output1)
                outputItems.setIngredient(1, output.output2)
            }
        }
        recipe.outputItem3?.let {
            outputItems.setIngredient(2, it)
        }
        recipe.outputFluid1?.let {
            outputFluids[0] = listOf(it)
        }
        recipe.outputFluid2?.let {
            outputFluids[1] = listOf(it)
        }
        recipe.bonusOutput?.let { output ->
            outputItems.setIngredient(3, output.first)
        }
        ingredients.setOutputLists(VanillaTypes.ITEM, outputItems)
        ingredients.setOutputLists(VanillaTypes.FLUID, outputFluids)

        ingredients.setInputLists(
            ExAJeiPlugin.ING_TYPE_HEAT, listOf(recipe.heatLevels.toArray().apply { sort() }.asList())
        )
    }

    override fun drawInfo(mc: Minecraft, recipeWidth: Int, recipeHeight: Int, mouseX: Int, mouseY: Int) {
        progressBar.draw(mc, 54, 34)
        recipe.bonusOutput?.let { mc.fontRenderer.drawString(it.second.formatPercentage(), 96, 42, 0x404040) }
    }

    override fun getTooltipStrings(mouseX: Int, mouseY: Int): List<String> = when (mouseX) {
        in 54..<74 if mouseY in 34..<54 -> listOf(recipe.duration.formatTicksAsSeconds())
        else -> emptyList()
    }

    class Factory(type: AlembicType, private val stackHelper: IStackHelper, private val guiHelper: IGuiHelper) {
        private val progressBar: IDrawableStatic = when (type) {
            AlembicType.ALEMBIC -> guiHelper.createDrawable(ExAJeiPlugin.JEI_TEXTURE, 111, 34, 20, 20)
            AlembicType.ALEMBIC_NETHER -> guiHelper.createDrawable(ExAJeiPlugin.JEI_TEXTURE, 111, 90, 20, 20)
        }

        fun wrap(recipe: AlembicRecipe): AlembicJeiRecipeWrapper =
            AlembicJeiRecipeWrapper(recipe, stackHelper, guiHelper, progressBar)
    }
}

class AlembicJeiRecipeCategory(type: AlembicType, guiHelper: IGuiHelper) : IRecipeCategory<AlembicJeiRecipeWrapper> {
    private val _uid: String
    private val unlocalizedTitle: String
    private val _background: IDrawable
    private val leftGaugeOverlay: IDrawable
    private val rightGaugeOverlay: IDrawable

    private val heatRenderer: IIngredientRenderer<Int> = when (type) {
        AlembicType.ALEMBIC -> HeatJeiIngredientRenderer.ALEMBIC
        AlembicType.ALEMBIC_NETHER -> HeatJeiIngredientRenderer.ALEMBIC_NETHER
    }

    init {
        val name = type.getName()
        _uid = "${ExAlembicoConsts.MOD_ID}.$name"
        unlocalizedTitle = "${ExAlembicoConsts.MOD_ID}.jei.category.$name"

        val yOffset = when (type) {
            AlembicType.ALEMBIC -> 56
            AlembicType.ALEMBIC_NETHER -> 0
        }
        _background = guiHelper.createDrawable(ExAJeiPlugin.JEI_TEXTURE, 0, yOffset, 111, 56)
        leftGaugeOverlay = guiHelper.createDrawable(ExAJeiPlugin.JEI_TEXTURE, 111, yOffset, 5, 16)
        rightGaugeOverlay = guiHelper.createDrawable(ExAJeiPlugin.JEI_TEXTURE, 116, yOffset, 5, 16)
    }

    override fun getUid(): String = _uid
    override fun getTitle(): String = I18n.format(unlocalizedTitle)
    override fun getModName(): String = ExAlembicoConsts.MOD_ID
    override fun getBackground(): IDrawable = _background

    override fun setRecipe(layout: IRecipeLayout, recipe: AlembicJeiRecipeWrapper, ingredients: IIngredients) {
        layout.itemStacks.run {
            init(0, true, 8, 5)
            init(1, true, 27, 5)
            init(2, true, 73, 5)
            init(3, false, JeiTransformOutputItemRenderer, 25, 22, 8, 8, 0, 0)
            init(4, false, JeiTransformOutputItemRenderer, 44, 22, 8, 8, 0, 0)
            init(5, false, JeiTransformOutputItemRenderer, 90, 22, 8, 8, 0, 0)
            init(6, false, 76, 36)
            set(ingredients)
        }
        layout.fluidStacks.run {
            init(0, true, 48, 6, 5, 16, 1000, false, leftGaugeOverlay)
            init(1, true, 94, 6, 5, 16, 1000, false, rightGaugeOverlay)
            init(2, false, JeiTransformOutputFluidRenderer(1000, false, 5, 16, null), 56, 9, 5, 16, 0, 0)
            init(3, false, JeiTransformOutputFluidRenderer(1000, false, 5, 16, null), 102, 9, 5, 16, 0, 0)
            set(ingredients)
        }
        layout.getIngredientsGroup(ExAJeiPlugin.ING_TYPE_HEAT).run {
            init(0, true, heatRenderer, 24, 39, 14, 11, 0, 0)
            set(ingredients)
        }
    }
}
