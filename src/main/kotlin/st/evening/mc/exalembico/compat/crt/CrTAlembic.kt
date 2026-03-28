package st.evening.mc.exalembico.compat.crt

import crafttweaker.CraftTweakerAPI
import crafttweaker.IAction
import crafttweaker.annotations.ZenRegister
import crafttweaker.api.item.IIngredient
import crafttweaker.api.item.IItemStack
import crafttweaker.api.liquid.ILiquidStack
import crafttweaker.api.minecraft.CraftTweakerMC
import gnu.trove.set.TIntSet
import gnu.trove.set.hash.TIntHashSet
import net.minecraft.item.ItemStack
import net.minecraftforge.fluids.FluidStack
import st.evening.mc.exalembico.ExAlembicoConsts
import st.evening.mc.exalembico.recipe.AlembicRecipe
import st.evening.mc.exalembico.recipe.ExARecipeManager
import st.evening.mc.exalembico.recipe.FluidIn
import st.evening.mc.exalembico.recipe.ItemIn
import st.evening.mc.exalembico.recipe.ItemOut
import stanhebben.zenscript.annotations.Optional
import stanhebben.zenscript.annotations.ZenClass
import stanhebben.zenscript.annotations.ZenMethod

@ZenRegister
@ZenClass("mods.${ExAlembicoConsts.MOD_ID}.Alembic")
object CrTAlembic {
    @JvmStatic
    @ZenMethod
    fun beginAlembicRecipe(duration: Int): CrTAlembicRecipeBuilder =
        CrTAlembicRecipeBuilder(duration)
}

@ZenRegister
@ZenClass("mods.${ExAlembicoConsts.MOD_ID}.AlembicRecipeBuilder")
class CrTAlembicRecipeBuilder(private val duration: Int) {
    private val heatLevels: TIntSet = TIntHashSet()
    private var inputItems: AlembicRecipe.InputItems? = null
    private var inputFluid: FluidIn? = null
    private var output: ItemOut? = null
    private var outputFluid: FluidStack? = null
    private var bonusOutput: Pair<ItemStack, Float>? = null

    @ZenMethod
    fun setHeatLevels(vararg heatLevels: Int): CrTAlembicRecipeBuilder {
        this.heatLevels.addAll(heatLevels)
        return this
    }

    @ZenMethod
    fun setHeatLevelRange(minHeatLevel: Int, maxHeatLevel: Int): CrTAlembicRecipeBuilder {
        for (heatLevel in minHeatLevel..maxHeatLevel) {
            heatLevels.add(heatLevel)
        }
        return this
    }

    @ZenMethod
    fun setInputItem(ingredient: IIngredient, @Optional transformInto: IItemStack?): CrTAlembicRecipeBuilder {
        val newInput = if (transformInto == null) {
            ItemIn.Stack(CraftTweakerMC.getIngredient(ingredient))
        } else {
            ItemIn.Transform(CraftTweakerMC.getIngredient(ingredient), CraftTweakerMC.getItemStack(transformInto))
        }
        inputItems = when (val old = inputItems) {
            null -> AlembicRecipe.InputItems.One(newInput)
            is AlembicRecipe.InputItems.One -> AlembicRecipe.InputItems.Two(old.input, newInput)
            is AlembicRecipe.InputItems.Two -> throw IllegalStateException("Too many input items!")
        }
        return this
    }

    @ZenMethod
    fun setInputFluid(fluid: ILiquidStack, @Optional transformInto: ILiquidStack?): CrTAlembicRecipeBuilder {
        check(inputFluid == null) { "Too many input fluids!" }
        inputFluid = if (transformInto == null) {
            FluidIn.Stack(CraftTweakerMC.getLiquidStack(fluid))
        } else {
            FluidIn.Transform(CraftTweakerMC.getLiquidStack(fluid), CraftTweakerMC.getLiquidStack(transformInto))
        }
        return this
    }

    @ZenMethod
    fun setOutputItem(stack: IItemStack, @Optional transformFrom: IIngredient?): CrTAlembicRecipeBuilder {
        check(output == null) { "Too many output items!" }
        output = if (transformFrom == null) {
            ItemOut.Stack(CraftTweakerMC.getItemStack(stack))
        } else {
            ItemOut.Transform(CraftTweakerMC.getIngredient(transformFrom), CraftTweakerMC.getItemStack(stack))
        }
        return this
    }

    @ZenMethod
    fun setOutputFluid(fluid: ILiquidStack): CrTAlembicRecipeBuilder {
        check(outputFluid == null) { "Too many output fluids!" }
        outputFluid = CraftTweakerMC.getLiquidStack(fluid)
        return this
    }

    @ZenMethod
    fun setBonusOutputItem(stack: IItemStack, @Optional(valueDouble = 1.0) odds: Float): CrTAlembicRecipeBuilder {
        check(bonusOutput == null) { "Too many bonus output items!" }
        bonusOutput = CraftTweakerMC.getItemStack(stack) to odds
        return this
    }

    @ZenMethod
    fun addToAlembic() {
        CraftTweakerAPI.apply(object : IAction {
            override fun apply() {
                ExARecipeManager.alembicRecipes.addRecipe(
                    AlembicRecipe(heatLevels, duration, inputItems, inputFluid, output, outputFluid, bonusOutput)
                )
            }

            override fun describe(): String = "Adding Ex Alembico alembic recipe"
        })
    }

    @ZenMethod
    fun addToNetherAlembic() {
        CraftTweakerAPI.apply(object : IAction {
            override fun apply() {
                ExARecipeManager.netherAlembicRecipes.addRecipe(
                    AlembicRecipe(heatLevels, duration, inputItems, inputFluid, output, outputFluid, bonusOutput)
                )
            }

            override fun describe(): String = "Adding Ex Alembico nether alembic recipe"
        })
    }
}
