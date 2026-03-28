package st.evening.mc.exalembico.compat.crt

import crafttweaker.CraftTweakerAPI
import crafttweaker.IAction
import crafttweaker.annotations.ZenRegister
import crafttweaker.api.item.IIngredient
import crafttweaker.api.minecraft.CraftTweakerMC
import gnu.trove.list.TIntList
import gnu.trove.list.array.TIntArrayList
import st.evening.mc.exalembico.ExAlembicoConsts
import st.evening.mc.exalembico.recipe.ExARecipeManager
import st.evening.mc.exalembico.recipe.HeaterRecipe
import stanhebben.zenscript.annotations.ZenClass
import stanhebben.zenscript.annotations.ZenMethod

@ZenRegister
@ZenClass("mods.${ExAlembicoConsts.MOD_ID}.Heater")
object CrTHeater {
    @JvmStatic
    @ZenMethod
    fun addHeaterRangeRecipe(fuel: IIngredient, duration: Int, minHeatLevel: Int, maxHeatLevel: Int) {
        val heatLevels = TIntArrayList()
        for (heatLevel in minHeatLevel..maxHeatLevel) {
            heatLevels.add(heatLevel)
        }
        addHeaterRecipe(fuel, duration, heatLevels)
    }

    @JvmStatic
    @ZenMethod
    fun addHeaterRecipe(fuel: IIngredient, duration: Int, vararg heatLevels: Int) {
        addHeaterRecipe(fuel, duration, TIntArrayList(heatLevels).apply { sort() })
    }

    private fun addHeaterRecipe(fuel: IIngredient, duration: Int, heatLevels: TIntList) {
        val fuelMc = CraftTweakerMC.getIngredient(fuel)
        CraftTweakerAPI.apply(object : IAction {
            override fun apply() {
                ExARecipeManager.heaterRecipes.addRecipe(HeaterRecipe(fuelMc, duration, heatLevels))
            }

            override fun describe(): String = "Adding Ex Alembico heater recipe for $fuel"
        })
    }

    @JvmStatic
    @ZenMethod
    fun removeHeaterRecipe(fuel: IIngredient) {
        val fuelStacks = CraftTweakerMC.getItemStacks(fuel.items)
        CraftTweakerAPI.apply(object : IAction {
            override fun apply() {
                ExARecipeManager.heaterRecipes.removeAll { recipe ->
                    fuelStacks.any { recipe.matches(it) }
                }
            }

            override fun describe(): String = "Removing Ex Alembico heater recipe for $fuel"
        })
    }
}
