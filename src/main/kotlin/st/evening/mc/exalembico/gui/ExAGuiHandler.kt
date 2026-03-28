package st.evening.mc.exalembico.gui

import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import net.minecraftforge.fml.common.network.IGuiHandler
import st.evening.mc.exalembico.block.alembic.ContainerAlembic
import st.evening.mc.exalembico.block.alembic.GuiAlembic
import st.evening.mc.exalembico.block.alembic.TileEntityAlembic
import st.evening.mc.exalembico.block.heater.ContainerHeater
import st.evening.mc.exalembico.block.heater.GuiHeater
import st.evening.mc.exalembico.block.heater.TileEntityHeater

object ExAGuiHandler : IGuiHandler {
    const val GUI_ALEMBIC: Int = 0
    const val GUI_HEATER: Int = 1

    private inline fun <reified T> World.useTileEntity(x: Int, y: Int, z: Int, f: (T) -> Any): Any? =
        (getTileEntity(BlockPos(x, y, z)) as? T)?.let(f)

    override fun getServerGuiElement(id: Int, player: EntityPlayer, world: World, x: Int, y: Int, z: Int): Any? =
        when (id) {
            GUI_ALEMBIC -> world.useTileEntity<TileEntityAlembic>(x, y, z) { ContainerAlembic(player.inventory, it) }
            GUI_HEATER -> world.useTileEntity<TileEntityHeater>(x, y, z) { ContainerHeater(player.inventory, it) }
            else -> null
        }

    override fun getClientGuiElement(id: Int, player: EntityPlayer, world: World, x: Int, y: Int, z: Int): Any? =
        when (id) {
            GUI_ALEMBIC -> world.useTileEntity<TileEntityAlembic>(x, y, z) { GuiAlembic.create(player.inventory, it) }
            GUI_HEATER -> world.useTileEntity<TileEntityHeater>(x, y, z) { GuiHeater(player.inventory, it) }
            else -> null
        }
}
