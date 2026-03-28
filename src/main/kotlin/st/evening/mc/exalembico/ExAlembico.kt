package st.evening.mc.exalembico

import net.minecraft.creativetab.CreativeTabs
import net.minecraft.item.ItemStack
import net.minecraft.util.ResourceLocation
import net.minecraftforge.common.capabilities.CapabilityManager
import net.minecraftforge.common.config.Config
import net.minecraftforge.common.config.ConfigManager
import net.minecraftforge.fml.client.event.ConfigChangedEvent
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.common.event.FMLLoadCompleteEvent
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.network.NetworkRegistry
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper
import net.minecraftforge.fml.common.registry.GameRegistry
import net.minecraftforge.fml.relauncher.Side
import org.apache.logging.log4j.Logger
import st.evening.mc.exalembico.block.alembic.TileEntityAlembic
import st.evening.mc.exalembico.block.heater.TileEntityHeater
import st.evening.mc.exalembico.gui.ExAGuiHandler
import st.evening.mc.exalembico.heat.HeatSource
import st.evening.mc.exalembico.init.ExABlocks
import st.evening.mc.exalembico.network.C2SClickFluidSlot
import st.evening.mc.exalembico.network.C2SEnableDisable
import st.evening.mc.exalembico.network.C2SSetHeatLevel
import st.evening.mc.exalembico.network.S2CSyncFluidSlot
import st.evening.mc.exalembico.recipe.ExARecipeManager
import st.evening.mc.exalembico.util.NoopCapabilityStorage

@Mod(
    modid = ExAlembicoConsts.MOD_ID,
    version = ExAlembicoConsts.VERSION,
    useMetadata = true,
    modLanguageAdapter = "net.shadowfacts.forgelin.KotlinAdapter"
)
@Mod.EventBusSubscriber(modid = ExAlembicoConsts.MOD_ID)
object ExAlembico {
    lateinit var logger: Logger private set
    internal val network: SimpleNetworkWrapper = NetworkRegistry.INSTANCE.newSimpleChannel(ExAlembicoConsts.MOD_ID)

    val creativeTab: CreativeTabs = object : CreativeTabs(ExAlembicoConsts.MOD_ID) {
        override fun createIcon(): ItemStack = ItemStack(ExABlocks.alembic)
    }

    init {
        network.registerMessage(C2SClickFluidSlot.Handler, C2SClickFluidSlot::class.java, 0, Side.SERVER)
        network.registerMessage(C2SEnableDisable.Handler, C2SEnableDisable::class.java, 1, Side.SERVER)
        network.registerMessage(C2SSetHeatLevel.Handler, C2SSetHeatLevel::class.java, 2, Side.SERVER)
        network.registerMessage(S2CSyncFluidSlot.Handler, S2CSyncFluidSlot::class.java, 3, Side.CLIENT)
    }

    fun resource(path: String): ResourceLocation = ResourceLocation(ExAlembicoConsts.MOD_ID, path)

    @Mod.EventHandler
    fun onPreInit(event: FMLPreInitializationEvent) {
        logger = event.modLog
        CapabilityManager.INSTANCE.register(HeatSource::class.java, NoopCapabilityStorage()) { HeatSource.Trivial }
        GameRegistry.registerTileEntity(TileEntityAlembic::class.java, resource("alembic"))
        GameRegistry.registerTileEntity(TileEntityHeater::class.java, resource("heater"))
        NetworkRegistry.INSTANCE.registerGuiHandler(this, ExAGuiHandler)
    }

    @Mod.EventHandler
    fun onLoadComplete(event: FMLLoadCompleteEvent) {
        ExARecipeManager.finalize()
    }

    @SubscribeEvent
    fun onConfigChanged(event: ConfigChangedEvent.OnConfigChangedEvent) {
        if (event.modID == ExAlembicoConsts.MOD_ID) {
            ConfigManager.sync(ExAlembicoConsts.MOD_ID, Config.Type.INSTANCE)
        }
    }
}
