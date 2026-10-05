package st.evening.mc.exalembico.recipe

import gnu.trove.set.TIntSet
import net.minecraft.item.ItemStack
import net.minecraft.item.crafting.Ingredient
import net.minecraft.world.World
import net.minecraftforge.fluids.FluidStack
import net.minecraftforge.fluids.IFluidTank
import net.minecraftforge.items.IItemHandler
import net.minecraftforge.items.ItemHandlerHelper
import st.evening.mc.exalembico.block.alembic.TileEntityAlembic

class AlembicRecipe(
    val heatLevels: TIntSet,
    val duration: Int,
    val inputItems: InputItems?,
    val inputItem3: Ingredient?,
    val inputFluid1: FluidStack?,
    val inputFluid2: FluidStack?,
    val outputItems: OutputItems?,
    val outputItem3: ItemStack?,
    val outputFluid1: FluidStack?,
    val outputFluid2: FluidStack?,
    val bonusOutput: Pair<ItemStack, Float>?
) : Comparable<AlembicRecipe> {
    private val minHeatLevel: Int

    init {
        if (heatLevels.isEmpty) throw IllegalArgumentException("No heat levels defined!")
        if (duration < 0) throw IllegalArgumentException("Negative duration!")
        if (inputItems == null && inputFluid1 == null && inputFluid2 == null) {
            throw IllegalArgumentException("No inputs defined!")
        }
        if (outputItems == null && outputItem3 == null && outputFluid1 == null && outputFluid2 == null) {
            throw IllegalArgumentException("No outputs defined!")
        }
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

    fun matches(inventory: IItemHandler, fluidIn1: IFluidTank, fluidIn2: IFluidTank): Boolean =
        inputItems?.matchesAsInput(inventory) != false &&
            inputItem3?.matchesAsInput(inventory, TileEntityAlembic.SLOT_IN3) != false &&
            inputFluid1?.matchesAsInput(fluidIn1) != false &&
            inputFluid2?.matchesAsInput(fluidIn2) != false

    fun canFitOutputs(inventory: IItemHandler, fluidOut1: IFluidTank, fluidOut2: IFluidTank): Boolean =
        outputItems?.canFitAsOutput(inventory) != false &&
            outputItem3?.canFitAsOutput(inventory, TileEntityAlembic.SLOT_OUT3) != false &&
            outputFluid1?.canFitAsOutput(fluidOut1) != false &&
            outputFluid2?.canFitAsOutput(fluidOut2) != false &&
            bonusOutput?.first?.canFitAsOutput(inventory, TileEntityAlembic.SLOT_OUT_BONUS) != false

    fun runRecipe(
        world: World,
        inventory: IItemHandler,
        fluidIn1: IFluidTank,
        fluidIn2: IFluidTank,
        fluidOut1: IFluidTank,
        fluidOut2: IFluidTank
    ) {
        inputItems?.consumeAsInput(inventory)
        inputItem3?.consumeAsInput(inventory, TileEntityAlembic.SLOT_IN3)
        inputFluid1?.consumeAsInput(fluidIn1)
        inputFluid2?.consumeAsInput(fluidIn2)
        outputItems?.produceAsOutput(inventory)
        outputItem3?.produceAsOutput(inventory, TileEntityAlembic.SLOT_OUT3)
        outputFluid1?.produceAsOutput(fluidOut1)
        outputFluid2?.produceAsOutput(fluidOut2)
        bonusOutput?.let { (stack, odds) ->
            if (world.rand.nextFloat() < odds) {
                inventory.insertItem(TileEntityAlembic.SLOT_OUT_BONUS, stack.copy(), false)
            }
        }
    }

    sealed interface InputItems {
        fun matchesAsInput(inventory: IItemHandler): Boolean

        fun consumeAsInput(inventory: IItemHandler)

        class One(val input: Ingredient) : InputItems {
            override fun matchesAsInput(inventory: IItemHandler): Boolean =
                input.apply(inventory.getStackInSlot(TileEntityAlembic.SLOT_IN1)) ||
                    input.apply(inventory.getStackInSlot(TileEntityAlembic.SLOT_IN2))

            override fun consumeAsInput(inventory: IItemHandler) {
                if (input.apply(inventory.getStackInSlot(TileEntityAlembic.SLOT_IN1))) {
                    input.consumeAsInput(inventory, TileEntityAlembic.SLOT_IN1)
                } else {
                    input.consumeAsInput(inventory, TileEntityAlembic.SLOT_IN2)
                }
            }
        }

        class Two(val input1: Ingredient, val input2: Ingredient) : InputItems {
            override fun matchesAsInput(inventory: IItemHandler): Boolean =
                rightSideUpMatch(inventory) || upsideDownMatch(inventory)

            override fun consumeAsInput(inventory: IItemHandler) {
                if (rightSideUpMatch(inventory)) {
                    input1.consumeAsInput(inventory, TileEntityAlembic.SLOT_IN1)
                    input2.consumeAsInput(inventory, TileEntityAlembic.SLOT_IN2)
                } else {
                    input2.consumeAsInput(inventory, TileEntityAlembic.SLOT_IN1)
                    input1.consumeAsInput(inventory, TileEntityAlembic.SLOT_IN2)
                }
            }

            private fun rightSideUpMatch(inventory: IItemHandler): Boolean =
                input1.apply(inventory.getStackInSlot(TileEntityAlembic.SLOT_IN1)) &&
                    input2.apply(inventory.getStackInSlot(TileEntityAlembic.SLOT_IN2))

            private fun upsideDownMatch(inventory: IItemHandler): Boolean =
                input1.apply(inventory.getStackInSlot(TileEntityAlembic.SLOT_IN2)) &&
                    input2.apply(inventory.getStackInSlot(TileEntityAlembic.SLOT_IN1))
        }
    }

    sealed interface OutputItems {
        fun canFitAsOutput(inventory: IItemHandler): Boolean

        fun produceAsOutput(inventory: IItemHandler)

        class One(val output: ItemStack) : OutputItems {
            override fun canFitAsOutput(inventory: IItemHandler): Boolean {
                val rem = inventory.insertItem(TileEntityAlembic.SLOT_OUT1, output, true)
                return rem.isEmpty || inventory.insertItem(TileEntityAlembic.SLOT_OUT2, output, true).isEmpty
            }

            override fun produceAsOutput(inventory: IItemHandler) {
                if (
                    ItemHandlerHelper.canItemStacksStack(output, inventory.getStackInSlot(TileEntityAlembic.SLOT_OUT2))
                ) {
                    val rem = inventory.insertItem(TileEntityAlembic.SLOT_OUT2, output.copy(), false)
                    if (!rem.isEmpty) {
                        inventory.insertItem(TileEntityAlembic.SLOT_OUT1, rem, false)
                    }
                } else {
                    val rem = inventory.insertItem(TileEntityAlembic.SLOT_OUT1, output.copy(), false)
                    if (!rem.isEmpty) {
                        inventory.insertItem(TileEntityAlembic.SLOT_OUT2, rem, false)
                    }
                }
            }
        }

        class Two(val output1: ItemStack, val output2: ItemStack) : OutputItems {
            // might behave weirdly if the two stacks are of the same item
            override fun canFitAsOutput(inventory: IItemHandler): Boolean =
                rightSideUpMatch(inventory) || upsideDownMatch(inventory)

            override fun produceAsOutput(inventory: IItemHandler) {
                if (rightSideUpMatch(inventory)) {
                    output1.produceAsOutput(inventory, TileEntityAlembic.SLOT_OUT1)
                    output2.produceAsOutput(inventory, TileEntityAlembic.SLOT_OUT2)
                } else {
                    output2.produceAsOutput(inventory, TileEntityAlembic.SLOT_OUT1)
                    output1.produceAsOutput(inventory, TileEntityAlembic.SLOT_OUT2)
                }
            }

            private fun rightSideUpMatch(inventory: IItemHandler): Boolean =
                inventory.insertItem(TileEntityAlembic.SLOT_OUT1, output1, true).isEmpty &&
                    inventory.insertItem(TileEntityAlembic.SLOT_OUT2, output2, true).isEmpty

            private fun upsideDownMatch(inventory: IItemHandler): Boolean =
                inventory.insertItem(TileEntityAlembic.SLOT_OUT2, output1, true).isEmpty &&
                    inventory.insertItem(TileEntityAlembic.SLOT_OUT1, output2, true).isEmpty
        }
    }
}
