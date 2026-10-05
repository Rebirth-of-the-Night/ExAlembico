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
import net.minecraft.item.crafting.Ingredient
import net.minecraftforge.fluids.FluidStack
import st.evening.mc.exalembico.ExAlembicoConsts
import st.evening.mc.exalembico.recipe.AlembicRecipe
import st.evening.mc.exalembico.recipe.ExARecipeManager
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
    private var inputItem3: Ingredient? = null
    private var inputFluid1: FluidStack? = null
    private var inputFluid2: FluidStack? = null
    private var outputItems: AlembicRecipe.OutputItems? = null
    private var outputItem3: ItemStack? = null
    private var outputFluid1: FluidStack? = null
    private var outputFluid2: FluidStack? = null
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
    fun setLeftInputItems(ing1: IIngredient, @Optional ing2: IIngredient?): CrTAlembicRecipeBuilder {
        inputItems = if (ing2 == null) {
            AlembicRecipe.InputItems.One(CraftTweakerMC.getIngredient(ing1))
        } else {
            AlembicRecipe.InputItems.Two(CraftTweakerMC.getIngredient(ing1), CraftTweakerMC.getIngredient(ing2))
        }
        return this
    }

    @ZenMethod
    fun setRightInputItem(ingredient: IIngredient): CrTAlembicRecipeBuilder {
        inputItem3 = CraftTweakerMC.getIngredient(ingredient)
        return this
    }

    @ZenMethod
    fun setLeftInputFluid(fluid: ILiquidStack): CrTAlembicRecipeBuilder {
        inputFluid1 = CraftTweakerMC.getLiquidStack(fluid)
        return this
    }

    @ZenMethod
    fun setRightInputFluid(fluid: ILiquidStack): CrTAlembicRecipeBuilder {
        inputFluid2 = CraftTweakerMC.getLiquidStack(fluid)
        return this
    }

    @ZenMethod
    fun setLeftOutputItems(stack1: IItemStack, @Optional stack2: IItemStack?): CrTAlembicRecipeBuilder {
        outputItems = if (stack2 == null) {
            AlembicRecipe.OutputItems.One(CraftTweakerMC.getItemStack(stack1))
        } else {
            AlembicRecipe.OutputItems.Two(CraftTweakerMC.getItemStack(stack1), CraftTweakerMC.getItemStack(stack2))
        }
        return this
    }

    @ZenMethod
    fun setRightOutputItem(stack: IItemStack): CrTAlembicRecipeBuilder {
        outputItem3 = CraftTweakerMC.getItemStack(stack)
        return this
    }

    @ZenMethod
    fun setLeftOutputFluid(fluid: ILiquidStack): CrTAlembicRecipeBuilder {
        outputFluid1 = CraftTweakerMC.getLiquidStack(fluid)
        return this
    }

    @ZenMethod
    fun setRightOutputFluid(fluid: ILiquidStack): CrTAlembicRecipeBuilder {
        outputFluid2 = CraftTweakerMC.getLiquidStack(fluid)
        return this
    }

    @ZenMethod
    fun setBonusOutputItem(stack: IItemStack, @Optional(valueDouble = 1.0) odds: Float): CrTAlembicRecipeBuilder {
        bonusOutput = CraftTweakerMC.getItemStack(stack) to odds
        return this
    }

    @ZenMethod
    fun addToAlembic() {
        CraftTweakerAPI.apply(object : IAction {
            override fun apply() {
                ExARecipeManager.alembicRecipes.addRecipe(
                    AlembicRecipe(
                        heatLevels, duration, inputItems, inputItem3, inputFluid1, inputFluid2,
                        outputItems, outputItem3, outputFluid1, outputFluid2, bonusOutput
                    )
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
                    AlembicRecipe(
                        heatLevels, duration, inputItems, inputItem3, inputFluid1, inputFluid2,
                        outputItems, outputItem3, outputFluid1, outputFluid2, bonusOutput
                    )
                )
            }

            override fun describe(): String = "Adding Ex Alembico nether alembic recipe"
        })
    }
}
