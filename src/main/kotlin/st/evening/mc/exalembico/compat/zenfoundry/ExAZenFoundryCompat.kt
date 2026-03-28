package st.evening.mc.exalembico.compat.zenfoundry

import exter.foundry.api.heatable.IHeatProvider
import net.minecraftforge.common.capabilities.Capability
import net.minecraftforge.common.capabilities.CapabilityInject

object ExAZenFoundryCompat {
    @field:CapabilityInject(IHeatProvider::class)
    lateinit var CAP_HEAT_PROVIDER: Capability<IHeatProvider> private set
}
