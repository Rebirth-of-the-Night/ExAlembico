package st.evening.mc.exalembico.init

import net.minecraft.block.Block
import net.minecraft.client.renderer.block.model.ModelResourceLocation
import net.minecraft.item.Item
import net.minecraft.item.ItemBlock
import net.minecraftforge.client.event.ModelRegistryEvent
import net.minecraftforge.client.model.ModelLoader
import net.minecraftforge.event.RegistryEvent
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.registry.GameRegistry
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly
import net.minecraftforge.registries.IForgeRegistry
import st.evening.mc.exalembico.ExAlembico
import st.evening.mc.exalembico.ExAlembicoConsts
import st.evening.mc.exalembico.block.alembic.AlembicType
import st.evening.mc.exalembico.block.alembic.BlockAlembic
import st.evening.mc.exalembico.block.alembic.ItemBlockAlembic
import st.evening.mc.exalembico.block.heater.BlockHeater
import st.evening.mc.exalembico.block.heater.HeaterType

@Mod.EventBusSubscriber(modid = ExAlembicoConsts.MOD_ID)
object ExABlocks {
    @field:GameRegistry.ObjectHolder("${ExAlembicoConsts.MOD_ID}:alembic")
    lateinit var alembic: BlockAlembic private set

    @field:GameRegistry.ObjectHolder("${ExAlembicoConsts.MOD_ID}:heater")
    lateinit var heater: BlockHeater private set

    @SubscribeEvent
    fun onRegisterBlocks(event: RegistryEvent.Register<Block>) {
        event.registry.run {
            register("alembic", BlockAlembic())
            register("heater", BlockHeater(HeaterType.HEATER))
        }
    }

    private fun IForgeRegistry<Block>.register(name: String, block: Block) {
        block.registryName = ExAlembico.resource(name)
        block.translationKey = "${ExAlembicoConsts.MOD_ID}.$name"
        block.creativeTab = ExAlembico.creativeTab
        register(block)
    }

    @SubscribeEvent
    fun onRegisterItems(event: RegistryEvent.Register<Item>) {
        event.registry.run {
            register("alembic", ItemBlockAlembic(alembic))
            register("heater", ItemBlock(heater))
        }
    }

    private fun IForgeRegistry<Item>.register(name: String, item: Item) {
        item.registryName = ExAlembico.resource(name)
        item.translationKey = "${ExAlembicoConsts.MOD_ID}.$name"
        item.creativeTab = ExAlembico.creativeTab
        register(item)
    }

    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    fun onRegisterModels(event: ModelRegistryEvent) {
        Item.getItemFromBlock(alembic).let { item ->
            AlembicType.entries.forEach {
                item.registerItemModel(it.ordinal, it.getName())
            }
        }
        Item.getItemFromBlock(heater).registerItemModel(0, "heater")
    }

    @SideOnly(Side.CLIENT)
    private fun Item.registerItemModel(meta: Int, path: String) {
        ModelLoader.setCustomModelResourceLocation(
            this, meta, ModelResourceLocation(ExAlembico.resource(path), "inventory")
        )
    }
}
