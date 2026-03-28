package st.evening.mc.exalembico.recipe

import gnu.trove.list.TIntList
import net.minecraft.item.ItemStack
import net.minecraft.item.crafting.Ingredient

class HeaterRecipe(val fuel: Ingredient, val duration: Int, val heatLevels: TIntList) {
    fun matches(input: ItemStack): Boolean = fuel.apply(input)
}
