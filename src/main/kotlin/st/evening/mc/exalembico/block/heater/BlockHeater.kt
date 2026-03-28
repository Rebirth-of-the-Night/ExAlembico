package st.evening.mc.exalembico.block.heater

import net.minecraft.block.Block
import net.minecraft.block.SoundType
import net.minecraft.block.material.Material
import net.minecraft.block.state.BlockStateContainer
import net.minecraft.block.state.IBlockState
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.BlockRenderLayer
import net.minecraft.util.EnumFacing
import net.minecraft.util.EnumHand
import net.minecraft.util.math.BlockPos
import net.minecraft.world.IBlockAccess
import net.minecraft.world.World
import st.evening.mc.exalembico.ExAConfig
import st.evening.mc.exalembico.ExAlembico
import st.evening.mc.exalembico.block.PropertyHeat
import st.evening.mc.exalembico.gui.ExAGuiHandler

@Suppress("OVERRIDE_DEPRECATION")
class BlockHeater private constructor(val type: HeaterType, unused: Unit) : Block(Material.IRON) {
    companion object {
        // createBlockState is called before our fields get initialized; this dumb hack lets us work around it
        private var tempPropHeat: PropertyHeat? = null
    }

    @Suppress("UNNECESSARY_LATEINIT")
    lateinit var propHeat: PropertyHeat private set

    constructor(type: HeaterType) : this(type, Unit.run { tempPropHeat = PropertyHeat("heat", type.heatLevels) })

    init {
        propHeat = tempPropHeat!!
        setHardness(4F)
        setSoundType(SoundType.METAL)
        setHarvestLevel("pickaxe", 0)
        defaultState = blockState.baseState.withProperty(propHeat, 0)
        hasTileEntity = true
    }

    override fun createBlockState(): BlockStateContainer = BlockStateContainer(this, tempPropHeat)

    override fun getStateFromMeta(meta: Int): IBlockState =
        defaultState.withProperty(propHeat, propHeat.getValueByIndex(meta))

    override fun getMetaFromState(state: IBlockState): Int = propHeat.getIndexForValue(state.getValue(propHeat))!!

    override fun hasTileEntity(state: IBlockState): Boolean = true

    override fun createTileEntity(world: World, state: IBlockState): TileEntity = TileEntityHeater(type)

    override fun getLightValue(state: IBlockState, world: IBlockAccess, pos: BlockPos): Int {
        val levels = ExAConfig.heaterLightLevels
        return levels[getMetaFromState(state).coerceAtMost(levels.size - 1)]
    }

    override fun onBlockActivated(
        world: World, pos: BlockPos, state: IBlockState, player: EntityPlayer, hand: EnumHand,
        facing: EnumFacing, hitX: Float, hitY: Float, hitZ: Float
    ): Boolean {
        if (!world.isRemote) {
            player.openGui(ExAlembico, ExAGuiHandler.GUI_HEATER, world, pos.x, pos.y, pos.z)
        }
        return true
    }

    override fun breakBlock(world: World, pos: BlockPos, state: IBlockState) {
        if (!world.isRemote) {
            (world.getTileEntity(pos) as? TileEntityHeater)?.dropInventory()
        }
        super.breakBlock(world, pos, state)
    }

    override fun isOpaqueCube(state: IBlockState): Boolean = false

    override fun getRenderLayer(): BlockRenderLayer = BlockRenderLayer.CUTOUT
}
