package st.evening.mc.exalembico.recipe

import net.minecraft.item.ItemStack
import net.minecraft.item.crafting.Ingredient
import net.minecraftforge.fluids.FluidStack
import net.minecraftforge.fluids.IFluidTank
import net.minecraftforge.items.IItemHandler

fun Ingredient.matchesAsInput(inventory: IItemHandler, slotIndex: Int): Boolean =
    apply(inventory.extractItem(slotIndex, 1, true))

fun Ingredient.consumeAsInput(inventory: IItemHandler, slotIndex: Int) {
    val out = inventory.extractItem(slotIndex, 1, false)
    if (out.item.hasContainerItem(out)) {
        inventory.insertItem(slotIndex, out.item.getContainerItem(out), false)
    }
}

fun ItemStack.canFitAsOutput(inventory: IItemHandler, slotIndex: Int): Boolean =
    inventory.insertItem(slotIndex, this, true).isEmpty

fun ItemStack.produceAsOutput(inventory: IItemHandler, slotIndex: Int) {
    inventory.insertItem(slotIndex, copy(), false)
}

fun FluidStack.matchesAsInput(tank: IFluidTank): Boolean {
    val fluid = tank.drain(amount, false)
    return fluid != null && fluid.amount >= amount && isFluidEqual(fluid)
}

fun FluidStack.consumeAsInput(tank: IFluidTank) {
    tank.drain(amount, true)
}

fun FluidStack.canFitAsOutput(tank: IFluidTank): Boolean = tank.fill(this, false) >= amount

fun FluidStack.produceAsOutput(tank: IFluidTank) {
    tank.fill(this, true)
}
