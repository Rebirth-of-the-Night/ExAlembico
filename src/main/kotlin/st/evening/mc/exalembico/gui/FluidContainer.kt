package st.evening.mc.exalembico.gui

import net.minecraft.entity.player.EntityPlayer
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.inventory.Container
import net.minecraftforge.fluids.FluidStack
import net.minecraftforge.fluids.FluidUtil
import net.minecraftforge.fluids.IFluidTank
import net.minecraftforge.items.CapabilityItemHandler
import st.evening.mc.exalembico.ExAlembico
import st.evening.mc.exalembico.network.S2CSyncFluidSlot
import st.evening.mc.exalembico.util.FluidTankWrapper
import st.evening.mc.exalembico.util.ModifiableFluidTank

abstract class FluidContainer : Container() {
    abstract val fluidSlots: List<FluidSlot>

    private val lastSentFluidStacks: MutableList<FluidStack?> by lazy { MutableList(fluidSlots.size) { null } }

    fun handleSyncFluidSlot(slotIndex: Int, stack: FluidStack?) {
        (fluidSlots[slotIndex].tank as? ModifiableFluidTank)?.setFluidInTank(stack)
    }

    fun handleFluidSlotClick(player: EntityPlayer, slotIndex: Int) {
        if (slotIndex < 0 || slotIndex >= fluidSlots.size) return
        val held = player.inventory.itemStack
        if (held.isEmpty) return
        val playerInv = player.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null) ?: return
        val tank = FluidTankWrapper(fluidSlots[slotIndex].tank)
        val result = FluidUtil.tryFillContainerAndStow(held, tank, playerInv, Integer.MAX_VALUE, player, true)
        if (result.isSuccess) {
            player.inventory.itemStack = result.result
            (player as? EntityPlayerMP)?.updateHeldItem()
        } else {
            val result = FluidUtil.tryEmptyContainerAndStow(held, tank, playerInv, Integer.MAX_VALUE, player, true)
            if (result.isSuccess) {
                player.inventory.itemStack = result.result
                (player as? EntityPlayerMP)?.updateHeldItem()
            }
        }
    }

    override fun detectAndSendChanges() {
        super.detectAndSendChanges()
        fluidSlots.forEachIndexed { index, slot ->
            val fluid = slot.tank.fluid?.takeIf { it.amount > 0 }
            if (areFluidStacksEqual(fluid, lastSentFluidStacks[index])) return@forEachIndexed
            val message = S2CSyncFluidSlot(index, fluid)
            listeners.forEach { listener ->
                if (listener is EntityPlayerMP) {
                    ExAlembico.network.sendTo(message, listener)
                }
            }
            lastSentFluidStacks[index] = fluid
        }
    }

    companion object {
        private fun areFluidStacksEqual(a: FluidStack?, b: FluidStack?): Boolean =
            if (a == null) b == null else (b != null && a.isFluidStackIdentical(b))
    }
}

class FluidSlot(val tank: IFluidTank, val posX: Int, val posY: Int, val width: Int, val height: Int)
