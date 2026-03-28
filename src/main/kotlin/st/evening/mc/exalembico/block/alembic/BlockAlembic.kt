package st.evening.mc.exalembico.block.alembic

import net.minecraft.block.Block
import net.minecraft.block.BlockHorizontal
import net.minecraft.block.SoundType
import net.minecraft.block.material.Material
import net.minecraft.block.properties.PropertyEnum
import net.minecraft.block.state.BlockFaceShape
import net.minecraft.block.state.BlockStateContainer
import net.minecraft.block.state.IBlockState
import net.minecraft.creativetab.CreativeTabs
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemBlock
import net.minecraft.item.ItemStack
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.EnumFacing
import net.minecraft.util.EnumHand
import net.minecraft.util.Mirror
import net.minecraft.util.NonNullList
import net.minecraft.util.Rotation
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.BlockPos
import net.minecraft.world.IBlockAccess
import net.minecraft.world.World
import net.minecraftforge.fluids.FluidUtil
import net.minecraftforge.items.CapabilityItemHandler
import st.evening.mc.exalembico.ExAlembico
import st.evening.mc.exalembico.gui.ExAGuiHandler
import st.evening.mc.exalembico.heat.HeatManager
import st.evening.mc.exalembico.util.FluidTankWrapper

@Suppress("OVERRIDE_DEPRECATION")
class BlockAlembic : BlockHorizontal(Material.IRON) {
    companion object {
        val TYPE: PropertyEnum<AlembicType> = PropertyEnum.create("type", AlembicType::class.java)
        private val BOUNDING_BOX_NS: AxisAlignedBB = AxisAlignedBB(1 / 8.0, 0.0, 5 / 16.0, 7 / 8.0, 9 / 16.0, 11 / 16.0)
        private val BOUNDING_BOX_WE: AxisAlignedBB = AxisAlignedBB(
            BOUNDING_BOX_NS.minZ, BOUNDING_BOX_NS.minY, BOUNDING_BOX_NS.minX,
            BOUNDING_BOX_NS.maxZ, BOUNDING_BOX_NS.maxY, BOUNDING_BOX_NS.maxX
        )

        private fun tryFluidInteraction(world: World, pos: BlockPos, player: EntityPlayer, hand: EnumHand): Boolean {
            val held = player.getHeldItem(hand)
            if (held.isEmpty) return false
            val inv = player.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null) ?: return false
            val te = world.getTileEntity(pos) as? TileEntityAlembic ?: return false
            val insertResult = FluidUtil.tryEmptyContainerAndStow(
                held, FluidTankWrapper(te.inputTank), inv, 1000, player, true
            )
            if (insertResult.isSuccess) {
                player.setHeldItem(hand, insertResult.result)
                return true
            }
            val extractResult = FluidUtil.tryFillContainerAndStow(
                held, FluidTankWrapper(te.outputTank), inv, 1000, player, true
            )
            if (extractResult.isSuccess) {
                player.setHeldItem(hand, extractResult.result)
                return true
            }
            return false
        }
    }

    init {
        setHardness(4F)
        setSoundType(SoundType.METAL)
        setHarvestLevel("pickaxe", 0)
        defaultState = blockState.baseState
            .withProperty(TYPE, AlembicType.ALEMBIC)
            .withProperty(FACING, EnumFacing.NORTH)
        hasTileEntity = true
    }

    override fun createBlockState(): BlockStateContainer = BlockStateContainer(this, TYPE, FACING)

    override fun getStateFromMeta(meta: Int): IBlockState = defaultState
        .withProperty(FACING, EnumFacing.byHorizontalIndex(meta and 0x3))
        .withProperty(TYPE, AlembicType.forMeta((meta and 0x4) ushr 2))

    override fun getMetaFromState(state: IBlockState): Int =
        state.getValue(FACING).horizontalIndex or (state.getValue(TYPE).ordinal shl 2)

    override fun damageDropped(state: IBlockState): Int = state.getValue(TYPE).ordinal

    override fun withMirror(state: IBlockState, mirror: Mirror): IBlockState =
        state.withProperty(FACING, mirror.mirror(state.getValue(FACING)))

    override fun withRotation(state: IBlockState, rot: Rotation): IBlockState =
        state.withProperty(FACING, rot.rotate(state.getValue(FACING)))

    override fun getStateForPlacement(
        world: World, pos: BlockPos, facing: EnumFacing, hitX: Float, hitY: Float, hitZ: Float,
        meta: Int, placer: EntityLivingBase, hand: EnumHand
    ): IBlockState = defaultState
        .withProperty(FACING, placer.horizontalFacing.opposite)
        .withProperty(TYPE, AlembicType.forMeta(meta))

    override fun canPlaceBlockAt(world: World, pos: BlockPos): Boolean {
        return super.canPlaceBlockAt(world, pos) && isValidAlembicPosition(world, pos)
    }

    override fun hasTileEntity(state: IBlockState): Boolean = true

    override fun createTileEntity(world: World, state: IBlockState): TileEntity =
        TileEntityAlembic(state.getValue(TYPE))

    override fun onBlockActivated(
        world: World, pos: BlockPos, state: IBlockState, player: EntityPlayer, hand: EnumHand,
        facing: EnumFacing, hitX: Float, hitY: Float, hitZ: Float
    ): Boolean {
        if (world.isRemote || tryFluidInteraction(world, pos, player, hand)) return true
        player.openGui(ExAlembico, ExAGuiHandler.GUI_ALEMBIC, world, pos.x, pos.y, pos.z)
        return true
    }

    override fun neighborChanged(state: IBlockState, world: World, pos: BlockPos, blockIn: Block, fromPos: BlockPos) {
        if (!isValidAlembicPosition(world, pos)) {
            dropBlockAsItem(world, pos, state, 0)
            world.setBlockToAir(pos)
        }
    }

    override fun breakBlock(world: World, pos: BlockPos, state: IBlockState) {
        (world.getTileEntity(pos) as? TileEntityAlembic)?.dropInventory()
        super.breakBlock(world, pos, state)
    }

    private fun isValidAlembicPosition(world: World, pos: BlockPos): Boolean =
        HeatManager.isHeatSource(world, pos.down(), EnumFacing.UP)

    override fun isOpaqueCube(state: IBlockState): Boolean = false

    override fun isFullCube(state: IBlockState): Boolean = false

    override fun getBlockFaceShape(
        worldIn: IBlockAccess, state: IBlockState, pos: BlockPos, face: EnumFacing
    ): BlockFaceShape = BlockFaceShape.UNDEFINED

    override fun getBoundingBox(state: IBlockState, world: IBlockAccess, pos: BlockPos): AxisAlignedBB =
        when (state.getValue(FACING)) {
            EnumFacing.NORTH, EnumFacing.SOUTH -> BOUNDING_BOX_NS
            EnumFacing.WEST, EnumFacing.EAST -> BOUNDING_BOX_WE
            else -> FULL_BLOCK_AABB // shouldn't happen
        }

    override fun getSubBlocks(tab: CreativeTabs, items: NonNullList<ItemStack>) {
        for (i in 0..<AlembicType.entries.size) {
            items += ItemStack(this, 1, i)
        }
    }
}

class ItemBlockAlembic(block: BlockAlembic) : ItemBlock(block) {
    init {
        hasSubtypes = true
    }

    override fun getMetadata(damage: Int): Int = damage

    override fun getTranslationKey(stack: ItemStack): String =
        "${super.getTranslationKey(stack)}.${AlembicType.forMeta(stack.metadata).getName()}"
}
