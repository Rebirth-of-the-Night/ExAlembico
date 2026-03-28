package st.evening.mc.exalembico.recipe

import st.evening.mc.exalembico.ExAConfig

object ExARecipeManager {
    val alembicRecipes: RecipeRegistry<AlembicRecipe> = SortedRecipeRegistry(naturalOrder())
    val netherAlembicRecipes: RecipeRegistry<AlembicRecipe> = SortedRecipeRegistry(naturalOrder())
    val heaterRecipes: RecipeRegistry<HeaterRecipe> = RecipeRegistry()

    internal fun finalize() {
        if (ExAConfig.netherAlembicInherit) {
            alembicRecipes.forEach { netherAlembicRecipes.addRecipe(it.withBonusModifier(1.2F)) }
        }

        alembicRecipes.freeze()
        netherAlembicRecipes.freeze()
        heaterRecipes.freeze()
    }

    private fun AlembicRecipe.withBonusModifier(modifier: Float): AlembicRecipe = bonusOutput?.let { (stack, odds) ->
        AlembicRecipe(heatLevels, duration, inputItems, inputFluid, output, outputFluid, stack to (odds * modifier))
    } ?: this
}

open class RecipeRegistry<T> : MutableIterable<T> {
    protected val recipes: MutableList<T> = mutableListOf()
    private var frozen: Boolean = false

    fun addRecipe(recipe: T) {
        checkFrozen()
        recipes += recipe
    }

    internal open fun freeze() {
        frozen = true
    }

    private fun checkFrozen() {
        check(!frozen) { "Recipe list is frozen!" }
    }

    override fun iterator(): MutableIterator<T> = IteratorWrapper(recipes.iterator())

    private inner class IteratorWrapper(private val backing: MutableIterator<T>) : MutableIterator<T> by backing {
        override fun remove() {
            checkFrozen()
            backing.remove()
        }
    }
}

class SortedRecipeRegistry<T>(private val comparator: Comparator<T>) : RecipeRegistry<T>() {
    override fun freeze() {
        super.freeze()
        recipes.sortWith(comparator)
    }
}
