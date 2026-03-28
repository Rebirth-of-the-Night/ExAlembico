package st.evening.mc.exalembico.util

import net.minecraftforge.fluids.FluidStack
import net.minecraftforge.fluids.IFluidTank

interface ModifiableFluidTank : IFluidTank {
    fun setFluidInTank(fluid: FluidStack?)
}
