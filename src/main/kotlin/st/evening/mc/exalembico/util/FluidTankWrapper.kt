package st.evening.mc.exalembico.util

import net.minecraftforge.fluids.FluidStack
import net.minecraftforge.fluids.IFluidTank
import net.minecraftforge.fluids.capability.IFluidHandler
import net.minecraftforge.fluids.capability.IFluidTankProperties

class FluidTankWrapper(private val tank: IFluidTank) : IFluidTank by tank, IFluidHandler, IFluidTankProperties {
    private val tankProps: Array<out IFluidTankProperties> = arrayOf(this)

    override fun drain(resource: FluidStack?, doDrain: Boolean): FluidStack? =
        if (resource != null && resource.isFluidEqual(tank.fluid)) tank.drain(resource.amount, doDrain) else null

    override fun getTankProperties(): Array<out IFluidTankProperties?> = tankProps
    override fun getContents(): FluidStack? = tank.fluid

    override fun canFill(): Boolean = true
    override fun canDrain(): Boolean = true
    override fun canFillFluidType(fluidStack: FluidStack?): Boolean = true
    override fun canDrainFluidType(fluidStack: FluidStack?): Boolean = true
}
