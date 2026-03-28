package st.evening.mc.exalembico.recipe

import net.minecraft.item.ItemStack
import net.minecraft.item.crafting.Ingredient
import net.minecraftforge.fluids.FluidStack
import net.minecraftforge.fluids.IFluidTank
import net.minecraftforge.items.IItemHandler

fun ItemStack.canFitAsOutput(inventory: IItemHandler, slotIndex: Int): Boolean =
    inventory.insertItem(slotIndex, this, true).isEmpty

interface ItemIngredient {
    fun matches(stack: ItemStack): Boolean
    fun canFitOutputs(inventory: IItemHandler, slotIndex: Int): Boolean
    fun runIngredient(inventory: IItemHandler, slotIndex: Int)
}

sealed interface ItemIn : ItemIngredient {
    class Stack(val input: Ingredient) : ItemIn {
        override fun matches(stack: ItemStack): Boolean = input.apply(stack)

        override fun canFitOutputs(inventory: IItemHandler, slotIndex: Int): Boolean = true

        override fun runIngredient(inventory: IItemHandler, slotIndex: Int) {
            val out = inventory.extractItem(slotIndex, 1, false)
            if (out.item.hasContainerItem(out)) {
                inventory.insertItem(slotIndex, out.item.getContainerItem(out), false)
            }
        }
    }

    class Transform(val input: Ingredient, val output: ItemStack) : ItemIn {
        override fun matches(stack: ItemStack): Boolean = input.apply(stack)

        override fun canFitOutputs(inventory: IItemHandler, slotIndex: Int): Boolean = true

        override fun runIngredient(inventory: IItemHandler, slotIndex: Int) {
            inventory.extractItem(slotIndex, Integer.MAX_VALUE, false)
            inventory.insertItem(slotIndex, output.copy(), false)
        }
    }
}

sealed interface ItemOut : ItemIngredient {
    class Stack(val output: ItemStack) : ItemOut {
        override fun matches(stack: ItemStack): Boolean = true

        override fun canFitOutputs(inventory: IItemHandler, slotIndex: Int): Boolean =
            output.canFitAsOutput(inventory, slotIndex)

        override fun runIngredient(inventory: IItemHandler, slotIndex: Int) {
            inventory.insertItem(slotIndex, output.copy(), false)
        }
    }

    class Transform(val input: Ingredient, val output: ItemStack) : ItemOut {
        override fun matches(stack: ItemStack): Boolean = input.apply(stack)

        override fun canFitOutputs(inventory: IItemHandler, slotIndex: Int): Boolean = true

        override fun runIngredient(inventory: IItemHandler, slotIndex: Int) {
            inventory.extractItem(slotIndex, Integer.MAX_VALUE, false)
            inventory.insertItem(slotIndex, output.copy(), false)
        }
    }
}

fun FluidStack.matchesAsInput(input: FluidStack?): Boolean = isFluidEqual(input) && input!!.amount >= amount

fun FluidStack.canFitAsOutput(tank: IFluidTank): Boolean = tank.fill(this, false) >= amount

interface FluidIngredient {
    fun matches(stack: FluidStack?): Boolean
    fun canFitOutputs(tank: IFluidTank): Boolean
    fun runIngredient(tank: IFluidTank)
}

sealed interface FluidIn : FluidIngredient {
    class Stack(val input: FluidStack) : FluidIn {
        override fun matches(stack: FluidStack?): Boolean = input.matchesAsInput(stack)

        override fun canFitOutputs(tank: IFluidTank): Boolean = true

        override fun runIngredient(tank: IFluidTank) {
            tank.drain(input.amount, true)
        }
    }

    class Transform(val input: FluidStack, val output: FluidStack) : FluidIn {
        override fun matches(stack: FluidStack?): Boolean = input.matchesAsInput(stack)

        override fun canFitOutputs(tank: IFluidTank): Boolean = true

        override fun runIngredient(tank: IFluidTank) {
            tank.drain(Integer.MAX_VALUE, true)
            tank.fill(output, true)
        }
    }
}
