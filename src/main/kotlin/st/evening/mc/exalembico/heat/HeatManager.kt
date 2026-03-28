package st.evening.mc.exalembico.heat

import net.minecraft.block.Block
import net.minecraft.block.state.IBlockState
import net.minecraft.util.EnumFacing
import net.minecraft.util.math.BlockPos
import net.minecraft.world.IBlockAccess
import net.minecraftforge.common.capabilities.Capability
import net.minecraftforge.common.capabilities.CapabilityInject
import net.minecraftforge.common.capabilities.ICapabilityProvider
import java.util.*

object HeatManager {
    @field:CapabilityInject(HeatSource::class)
    lateinit var CAP_HEAT_SOURCE: Capability<HeatSource> private set

    private val blockHeatRegistry: MutableMap<Block, BlockHeatEntry> = IdentityHashMap()
    private val adapterRegistry: MutableList<HeatSourceAdapter> = mutableListOf()

    fun registerHeatSourceBlock(block: Block, heatLevel: Int) {
        blockHeatRegistry[block] = BlockHeatEntry.Wildcard(heatLevel)
    }

    fun registerHeatSourceBlock(state: IBlockState, heatLevel: Int) {
        val entry = blockHeatRegistry[state.block]?.let { it as? BlockHeatEntry.ByState ?: return }
            ?: BlockHeatEntry.ByState().also { blockHeatRegistry[state.block] = it }
        entry.states[state] = heatLevel
    }

    fun registerHeatSourceAdapter(adapter: HeatSourceAdapter) {
        adapterRegistry += adapter
    }

    fun isHeatSource(world: IBlockAccess, pos: BlockPos, face: EnumFacing?): Boolean {
        val state = world.getBlockState(pos)
        blockHeatRegistry[state.block]?.let { entry ->
            when (entry) {
                is BlockHeatEntry.ByState -> {
                    if (state in entry.states) return true
                }

                is BlockHeatEntry.Wildcard -> return true
            }
        }

        world.getTileEntity(pos)?.let { te ->
            if (te.hasCapability(CAP_HEAT_SOURCE, face) || adapterRegistry.any { it.isHeatSource(te, face) }) {
                return true
            }
        }

        return false
    }

    fun getHeatLevel(world: IBlockAccess, pos: BlockPos, face: EnumFacing?): Int {
        val state = world.getBlockState(pos)
        blockHeatRegistry[state.block]?.let { entry ->
            when (entry) {
                is BlockHeatEntry.ByState -> entry.states[state]?.let { return it }
                is BlockHeatEntry.Wildcard -> return entry.heat
            }
        }

        world.getTileEntity(pos)?.let { te ->
            if (te.hasCapability(CAP_HEAT_SOURCE, face)) {
                te.getCapability(CAP_HEAT_SOURCE, face)?.let { return it.heatLevel }
            }
            adapterRegistry.forEach { adapter ->
                if (adapter.isHeatSource(te, face)) {
                    adapter.wrapHeatSource(te, face)?.let { return it.heatLevel }
                }
            }
        }

        return 0
    }

    private sealed interface BlockHeatEntry {
        class ByState(val states: MutableMap<IBlockState, Int> = IdentityHashMap()) : BlockHeatEntry
        class Wildcard(val heat: Int) : BlockHeatEntry
    }
}

interface HeatSourceAdapter {
    fun isHeatSource(target: ICapabilityProvider, face: EnumFacing?): Boolean
    fun wrapHeatSource(target: ICapabilityProvider, face: EnumFacing?): HeatSource?
}

