package st.evening.mc.exalembico.compat.crt

import crafttweaker.CraftTweakerAPI
import crafttweaker.IAction
import crafttweaker.annotations.ZenRegister
import crafttweaker.api.block.IBlock
import crafttweaker.api.block.IBlockState
import crafttweaker.api.minecraft.CraftTweakerMC
import st.evening.mc.exalembico.ExAlembicoConsts
import st.evening.mc.exalembico.heat.HeatManager
import stanhebben.zenscript.annotations.ZenClass
import stanhebben.zenscript.annotations.ZenMethod

@ZenRegister
@ZenClass("mods.${ExAlembicoConsts.MOD_ID}.ExAlembico")
object CrTExAlembico {
    @JvmStatic
    @ZenMethod
    fun registerHeatSourceBlock(block: IBlock, heatLevel: Int) {
        val blockMc = CraftTweakerMC.getBlock(block)
        CraftTweakerAPI.apply(object : IAction {
            override fun apply() {
                HeatManager.registerHeatSourceBlock(blockMc, heatLevel)
            }

            override fun describe(): String = "Registering Ex Alembico heat source for $block"
        })
    }

    @JvmStatic
    @ZenMethod
    fun registerHeatSourceBlock(state: IBlockState, heatLevel: Int) {
        val stateMc = CraftTweakerMC.getBlockState(state)
        CraftTweakerAPI.apply(object : IAction {
            override fun apply() {
                HeatManager.registerHeatSourceBlock(stateMc, heatLevel)
            }

            override fun describe(): String = "Registering Ex Alembico heat source for $state"
        })
    }
}
