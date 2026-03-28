package st.evening.mc.exalembico.util

import net.minecraft.nbt.NBTBase
import net.minecraft.util.EnumFacing
import net.minecraftforge.common.capabilities.Capability

object NoopCapabilityStorage : Capability.IStorage<Any> {
    @Suppress("UNCHECKED_CAST")
    operator fun <T> invoke(): Capability.IStorage<T> = this as Capability.IStorage<T>

    override fun writeNBT(capability: Capability<in Any>, instance: Any, side: EnumFacing?): NBTBase? = null

    override fun readNBT(capability: Capability<in Any>, instance: Any, side: EnumFacing?, nbt: NBTBase) {}
}
