package st.evening.mc.exalembico.block.alembic

import net.minecraft.inventory.InventoryHelper
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.network.NetworkManager
import net.minecraft.network.play.server.SPacketUpdateTileEntity
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.EnumFacing
import net.minecraft.util.ITickable
import net.minecraft.world.World
import net.minecraftforge.common.capabilities.Capability
import net.minecraftforge.common.util.Constants
import net.minecraftforge.fluids.FluidStack
import net.minecraftforge.fluids.FluidTank
import net.minecraftforge.fluids.IFluidTank
import net.minecraftforge.fluids.capability.CapabilityFluidHandler
import net.minecraftforge.fluids.capability.IFluidHandler
import net.minecraftforge.fluids.capability.templates.FluidHandlerConcatenate
import net.minecraftforge.items.CapabilityItemHandler
import net.minecraftforge.items.IItemHandler
import net.minecraftforge.items.ItemStackHandler
import st.evening.mc.exalembico.ExAlembico
import st.evening.mc.exalembico.heat.HeatManager
import st.evening.mc.exalembico.recipe.AlembicRecipe
import st.evening.mc.exalembico.recipe.ExARecipeManager
import st.evening.mc.exalembico.recipe.RecipeRegistry
import st.evening.mc.exalembico.util.ModifiableFluidTank

class TileEntityAlembic() : TileEntity(), ITickable {
    companion object {
        const val SLOT_IN1: Int = 0
        const val SLOT_IN2: Int = 1
        const val SLOT_OUT: Int = 2
        const val SLOT_OUT_BONUS: Int = 3
    }

    private val _inventory: AlembicInventory = AlembicInventory()
    private val _inputTank: AlembicFluidTank = AlembicFluidTank(true)
    private val _outputTank: AlembicFluidTank = AlembicFluidTank(false)
    private var recipeState: RecipeState? = null

    lateinit var alembicType: AlembicType private set

    // null => nothing; false => only outputs dirty; true => inputs and outputs dirty
    private var dirtyState: Boolean? = true
    var stateClock: Int = 0
        private set

    val inventory: IItemHandler
        get() = _inventory
    val inputTank: IFluidTank
        get() = _inputTank
    val outputTank: IFluidTank
        get() = _outputTank
    val fluidInventory: IFluidHandler = FluidHandlerConcatenate(_inputTank, _outputTank)

    val progressFraction: Float
        get() = when (val state = recipeState) {
            is RecipeState.Running -> (state.progress / state.recipe.duration.toFloat()).coerceIn(0F, 1F)
            is RecipeState.FinishedNoOutputSpace -> 1F
            else -> 0F
        }

    constructor(alembicType: AlembicType) : this() {
        this.alembicType = alembicType
    }

    override fun setWorldCreate(world: World) {
        setWorld(world)
    }

    override fun hasCapability(capability: Capability<*>, facing: EnumFacing?): Boolean = when (capability) {
        CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY -> true
        else -> super.hasCapability(capability, facing)
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T> getCapability(capability: Capability<T>, facing: EnumFacing?): T? = when (capability) {
        CapabilityItemHandler.ITEM_HANDLER_CAPABILITY -> _inventory as T
        CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY -> fluidInventory as T
        else -> super.getCapability(capability, facing)
    }

    override fun update() {
        dirtyState?.let { inputAndOutputDirty ->
            if (!world.isRemote) {
                if (inputAndOutputDirty) {
                    when (val state = recipeState) {
                        null -> findAndStartRecipe()
                        is RecipeState.NoOutputSpaceToStart -> {
                            if (recipeMatches(state.recipe) && recipeOutputsFit(state.recipe)) {
                                recipeState = RecipeState.Running(state.recipe, true)
                                markDirty()
                            }
                        }

                        is RecipeState.Running -> {
                            if (!recipeMatches(state.recipe)) {
                                recipeState = null
                                findAndStartRecipe()
                                markDirty()
                            } else {
                                state.outputsFit = recipeOutputsFit(state.recipe)
                                // marked dirty later
                            }
                        }

                        is RecipeState.FinishedNoOutputSpace -> {
                            if (!recipeMatches(state.recipe)) {
                                recipeState = null
                                findAndStartRecipe()
                                markDirty()
                            } else if (recipeOutputsFit(state.recipe)) {
                                runRecipe(state.recipe)
                                findAndStartRecipe()
                                markDirty()
                            }
                        }
                    }
                } else { // only the outputs are dirty
                    when (val state = recipeState) {
                        null -> {}

                        is RecipeState.NoOutputSpaceToStart -> {
                            if (recipeOutputsFit(state.recipe)) {
                                recipeState = RecipeState.Running(state.recipe, true)
                                markDirty()
                            }
                        }

                        is RecipeState.Running -> {
                            state.outputsFit = recipeOutputsFit(state.recipe)
                            // marked dirty later
                        }

                        is RecipeState.FinishedNoOutputSpace -> {
                            if (recipeOutputsFit(state.recipe)) {
                                runRecipe(state.recipe)
                                findAndStartRecipe()
                                markDirty()
                            }
                        }
                    }
                }
            }
            dirtyState = null
        }

        (recipeState as? RecipeState.Running)?.let { state ->
            if (canWork(state.recipe)) {
                if (++state.progress >= state.recipe.duration && !world.isRemote) {
                    if (state.outputsFit) {
                        runRecipe(state.recipe)
                    } else {
                        recipeState = RecipeState.FinishedNoOutputSpace(state.recipe)
                    }
                }
                markDirty()
            }
        }
    }

    private fun findAndStartRecipe() {
        getRecipes().firstOrNull { recipeMatches(it) }?.let {
            recipeState = if (recipeOutputsFit(it)) {
                RecipeState.Running(it, true)
            } else {
                RecipeState.NoOutputSpaceToStart(it)
            }
        }
    }

    private fun runRecipe(recipe: AlembicRecipe) {
        recipeState = null
        recipe.runRecipe(world, inventory, inputTank, outputTank)
    }

    private fun getRecipes(): RecipeRegistry<AlembicRecipe> = when (alembicType) {
        AlembicType.ALEMBIC -> ExARecipeManager.alembicRecipes
        AlembicType.ALEMBIC_NETHER -> ExARecipeManager.netherAlembicRecipes
    }

    fun getHeatLevel(): Int = HeatManager.getHeatLevel(world, pos.down(), EnumFacing.UP)

    private fun canWork(recipe: AlembicRecipe): Boolean {
        val heatLevel = getHeatLevel()
        return heatLevel > 0 && recipe.heatLevels.contains(heatLevel)
    }

    private fun recipeMatches(recipe: AlembicRecipe): Boolean = recipe.matches(inventory, inputTank.fluid)

    private fun recipeOutputsFit(recipe: AlembicRecipe): Boolean =
        recipe.canFitOutputs(inventory, inputTank, outputTank)

    fun dropInventory() {
        _inventory.dropInventory(world, pos.x + 0.5, pos.y + 0.5, pos.z + 0.5)
    }

    override fun markDirty() {
        super.markDirty()
        stateClock++
    }

    override fun getUpdatePacket(): SPacketUpdateTileEntity {
        return SPacketUpdateTileEntity(pos, 0, NBTTagCompound().also { writeUpdateTag(it) })
    }

    override fun getUpdateTag(): NBTTagCompound = super.getUpdateTag().also { writeUpdateTag(it) }

    private fun writeUpdateTag(tag: NBTTagCompound) {
        recipeState?.writeToNBT(tag)
    }

    override fun onDataPacket(net: NetworkManager, pkt: SPacketUpdateTileEntity) {
        handleUpdateTag(pkt.nbtCompound)
    }

    override fun handleUpdateTag(tag: NBTTagCompound) {
        deserializeRecipeState(tag)
    }

    override fun writeToNBT(tag: NBTTagCompound): NBTTagCompound {
        super.writeToNBT(tag)
        tag.setByte("type", alembicType.ordinal.toByte())
        tag.setTag("inventory", _inventory.serializeNBT())
        tag.setTag("inputTank", NBTTagCompound().also { _inputTank.writeToNBT(it) })
        tag.setTag("outputTank", NBTTagCompound().also { _outputTank.writeToNBT(it) })
        writeUpdateTag(tag)
        return tag
    }

    override fun readFromNBT(tag: NBTTagCompound) {
        super.readFromNBT(tag)
        alembicType = AlembicType.entries[tag.getByte("type").toInt()]
        _inventory.deserializeNBT(tag.getCompoundTag("inventory"))
        _inputTank.readFromNBT(tag.getCompoundTag("inputTank"))
        _outputTank.readFromNBT(tag.getCompoundTag("outputTank"))
        handleUpdateTag(tag)
    }

    fun deserializeRecipeState(tag: NBTTagCompound) {
        if (!tag.hasKey("state", Constants.NBT.TAG_BYTE)) {
            recipeState = null
            return
        }
        val recipe = getRecipes().firstOrNull { recipeMatches(it) }
        if (recipe == null) {
            ExAlembico.logger.warn(
                "Could not find running recipe for alembic in dimension ${world.provider.dimension} at $pos"
            )
            recipeState = null
            return
        }
        when (tag.getByte("state")) {
            0.toByte() -> {
                recipeState = RecipeState.NoOutputSpaceToStart(recipe)
                return
            }

            1.toByte() -> {
                recipeState = RecipeState.Running(recipe, recipeOutputsFit(recipe), tag.getInteger("progress"))
                return
            }

            2.toByte() -> {
                recipeState = RecipeState.FinishedNoOutputSpace(recipe)
                return
            }

            else -> ExAlembico.logger.warn(
                "Bad state ${tag.getByte("state")} for alembic in dimension ${world.provider.dimension} at $pos"
            )
        }
    }

    private sealed interface RecipeState {
        fun writeToNBT(tag: NBTTagCompound)

        class NoOutputSpaceToStart(val recipe: AlembicRecipe) : RecipeState {
            override fun writeToNBT(tag: NBTTagCompound) {
                tag.setByte("state", 0)
            }
        }

        class Running(val recipe: AlembicRecipe, var outputsFit: Boolean, var progress: Int = 0) : RecipeState {
            override fun writeToNBT(tag: NBTTagCompound) {
                tag.setByte("state", 1)
                tag.setInteger("progress", progress)
            }
        }

        class FinishedNoOutputSpace(val recipe: AlembicRecipe) : RecipeState {
            override fun writeToNBT(tag: NBTTagCompound) {
                tag.setByte("state", 2)
            }
        }
    }

    private inner class AlembicInventory : ItemStackHandler(4) {
        override fun isItemValid(slot: Int, stack: ItemStack): Boolean = slot != SLOT_OUT_BONUS

        fun dropInventory(world: World, x: Double, y: Double, z: Double) {
            stacks.forEach {
                if (!it.isEmpty) {
                    InventoryHelper.spawnItemStack(world, x, y, z, it)
                }
            }
        }

        override fun onContentsChanged(slot: Int) {
            when (slot) {
                SLOT_IN1, SLOT_IN2, SLOT_OUT -> dirtyState = true
                SLOT_OUT_BONUS -> dirtyState = dirtyState ?: false
            }
            markDirty()
        }
    }

    private inner class AlembicFluidTank(private val input: Boolean) : FluidTank(1000), ModifiableFluidTank {
        override fun setFluidInTank(fluid: FluidStack?) {
            this.fluid = fluid
        }

        override fun onContentsChanged() {
            dirtyState = input || (dirtyState ?: false)
            markDirty()
        }
    }
}
