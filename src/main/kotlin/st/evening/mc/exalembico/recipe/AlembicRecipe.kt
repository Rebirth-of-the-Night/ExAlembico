package st.evening.mc.exalembico.recipe

import gnu.trove.set.TIntSet
import net.minecraft.item.ItemStack
import net.minecraft.world.World
import net.minecraftforge.fluids.FluidStack
import net.minecraftforge.fluids.IFluidTank
import net.minecraftforge.items.IItemHandler
import st.evening.mc.exalembico.block.alembic.TileEntityAlembic.Companion.SLOT_IN1
import st.evening.mc.exalembico.block.alembic.TileEntityAlembic.Companion.SLOT_IN2
import st.evening.mc.exalembico.block.alembic.TileEntityAlembic.Companion.SLOT_OUT
import st.evening.mc.exalembico.block.alembic.TileEntityAlembic.Companion.SLOT_OUT_BONUS

class AlembicRecipe(
    val heatLevels: TIntSet,
    val duration: Int,
    val inputItems: InputItems?,
    val inputFluid: FluidIn?,
    val output: ItemOut?,
    val outputFluid: FluidStack?,
    val bonusOutput: Pair<ItemStack, Float>?
) : Comparable<AlembicRecipe> {
    private val minHeatLevel: Int

    init {
        if (heatLevels.isEmpty) throw IllegalArgumentException("No heat levels defined!")
        if (inputItems == null && inputFluid == null) throw IllegalArgumentException("No inputs defined!")
        if (output == null && outputFluid == null) throw IllegalArgumentException("No outputs defined!")
        var minHeatLevel = Integer.MAX_VALUE
        heatLevels.forEach {
            if (it < minHeatLevel) {
                minHeatLevel = it
            }
            return@forEach true
        }
        this.minHeatLevel = minHeatLevel
    }

    // hottest recipes first
    override fun compareTo(other: AlembicRecipe): Int = other.minHeatLevel.compareTo(minHeatLevel)

    fun matches(inventory: IItemHandler, fluidIn: FluidStack?): Boolean =
        inputItems?.matches(inventory) != false &&
            inputFluid?.matches(fluidIn) != false &&
            output?.matches(inventory.getStackInSlot(SLOT_OUT)) != false

    fun canFitOutputs(inventory: IItemHandler, fluidIn: IFluidTank, fluidOut: IFluidTank): Boolean =
        inputItems?.canFitOutputs(inventory) != false &&
            inputFluid?.canFitOutputs(fluidIn) != false &&
            output?.canFitOutputs(inventory, SLOT_OUT) != false &&
            outputFluid?.canFitAsOutput(fluidOut) != false &&
            bonusOutput?.first?.canFitAsOutput(inventory, SLOT_OUT_BONUS) != false

    fun runRecipe(
        world: World,
        inventory: IItemHandler,
        fluidIn: IFluidTank,
        fluidOut: IFluidTank
    ) {
        inputItems?.runIngredient(inventory)
        inputFluid?.runIngredient(fluidIn)
        output?.runIngredient(inventory, SLOT_OUT)
        outputFluid?.let { fluidOut.fill(it, true) }
        bonusOutput?.let { (stack, odds) ->
            if (world.rand.nextFloat() < odds) {
                inventory.insertItem(SLOT_OUT_BONUS, stack.copy(), false)
            }
        }
    }

    sealed interface InputItems {
        fun matches(inventory: IItemHandler): Boolean
        fun canFitOutputs(inventory: IItemHandler): Boolean
        fun runIngredient(inventory: IItemHandler)

        class One(val input: ItemIn) : InputItems {
            override fun matches(inventory: IItemHandler): Boolean =
                input.matches(inventory.getStackInSlot(SLOT_IN1)) || input.matches(inventory.getStackInSlot(SLOT_IN2))

            override fun canFitOutputs(inventory: IItemHandler): Boolean =
                input.canFitOutputs(inventory, SLOT_IN1) || input.canFitOutputs(inventory, SLOT_IN2)

            override fun runIngredient(inventory: IItemHandler) {
                if (input.matches(inventory.getStackInSlot(SLOT_IN1))) {
                    input.runIngredient(inventory, SLOT_IN1)
                } else {
                    input.runIngredient(inventory, SLOT_IN2)
                }
            }
        }

        class Two(val input1: ItemIn, val input2: ItemIn) : InputItems {
            override fun matches(inventory: IItemHandler): Boolean =
                rightSideUpMatch(inventory) ||
                    (input2.matches(inventory.getStackInSlot(SLOT_IN1)) &&
                        input1.matches(inventory.getStackInSlot(SLOT_IN2)))

            override fun canFitOutputs(inventory: IItemHandler): Boolean =
                (input1.canFitOutputs(inventory, SLOT_IN1) && input2.canFitOutputs(inventory, SLOT_IN2)) ||
                    (input2.canFitOutputs(inventory, SLOT_IN1) && input1.canFitOutputs(inventory, SLOT_IN2))

            override fun runIngredient(inventory: IItemHandler) {
                if (rightSideUpMatch(inventory)) {
                    input1.runIngredient(inventory, SLOT_IN1)
                    input2.runIngredient(inventory, SLOT_IN2)
                } else {
                    input2.runIngredient(inventory, SLOT_IN1)
                    input1.runIngredient(inventory, SLOT_IN2)
                }
            }

            private fun rightSideUpMatch(inventory: IItemHandler): Boolean =
                input1.matches(inventory.getStackInSlot(SLOT_IN1)) && input2.matches(inventory.getStackInSlot(SLOT_IN2))
        }
    }
}
