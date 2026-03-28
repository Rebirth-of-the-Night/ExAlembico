package st.evening.mc.exalembico.block.heater

import exter.foundry.api.heatable.IHeatProvider
import gnu.trove.list.TIntList
import net.minecraft.block.state.IBlockState
import net.minecraft.inventory.InventoryHelper
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.network.NetworkManager
import net.minecraft.network.play.server.SPacketUpdateTileEntity
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.EnumFacing
import net.minecraft.util.ITickable
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import net.minecraftforge.common.capabilities.Capability
import net.minecraftforge.common.util.Constants
import net.minecraftforge.fml.common.Optional
import net.minecraftforge.items.CapabilityItemHandler
import net.minecraftforge.items.IItemHandler
import net.minecraftforge.items.ItemStackHandler
import st.evening.mc.exalembico.ExAConfig
import st.evening.mc.exalembico.ExAlembico
import st.evening.mc.exalembico.compat.zenfoundry.ExAZenFoundryCompat
import st.evening.mc.exalembico.heat.HeatManager
import st.evening.mc.exalembico.heat.HeatSource
import st.evening.mc.exalembico.recipe.ExARecipeManager
import st.evening.mc.exalembico.recipe.HeaterRecipe
import st.evening.mc.exalembico.recipe.RecipeRegistry
import kotlin.math.absoluteValue

@Optional.Interface(modid = "foundry", iface = "exter.foundry.api.heatable.IHeatProvider")
class TileEntityHeater() : TileEntity(), ITickable, HeatSource, IHeatProvider {
    companion object {
        private fun roundHeatLevel(heatLevels: TIntList?, target: Int): Int {
            if (heatLevels == null) return target
            val k = heatLevels.binarySearch(target) // assume the list is sorted
            if (k >= 0) return target // target is a valid heat level

            val index = -k - 1
            if (index == 0) return heatLevels[0]
            if (index == heatLevels.size()) return heatLevels[index - 1]

            val left = heatLevels[index - 1]
            val right = heatLevels[index]
            return if ((left - target).absoluteValue <= (right - target).absoluteValue) left else right
        }
    }

    private val _inventory: HeaterInventory = HeaterInventory()
    private var recipeState: RecipeState? = null
    var enabled: Boolean = false
        private set
    private var configuredHeatLevel: Int = 0

    lateinit var heaterType: HeaterType private set
    private var inputDirty: Boolean = true
    private var updatingBlockState: Boolean? = null
    var stateClock: Int = 0
        private set

    val inventory: IItemHandler
        get() = _inventory

    override val heatLevel: Int
        get() = if (recipeState is RecipeState.Running) configuredHeatLevel else 0
    val durationFraction: Float
        get() = (recipeState as? RecipeState.Running)?.let {
            (it.duration / it.recipe.duration.toFloat()).coerceIn(0F, 1F)
        } ?: 0F

    constructor(heaterType: HeaterType) : this() {
        this.heaterType = heaterType
    }

    override fun setWorldCreate(world: World) {
        setWorld(world)
    }

    override fun hasCapability(capability: Capability<*>, facing: EnumFacing?): Boolean = when (capability) {
        CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, HeatManager.CAP_HEAT_SOURCE,
        ExAZenFoundryCompat.CAP_HEAT_PROVIDER -> true

        else -> super.hasCapability(capability, facing)
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T> getCapability(capability: Capability<T>, facing: EnumFacing?): T? = when (capability) {
        CapabilityItemHandler.ITEM_HANDLER_CAPABILITY -> _inventory as T
        HeatManager.CAP_HEAT_SOURCE -> this as T
        ExAZenFoundryCompat.CAP_HEAT_PROVIDER -> this as T
        else -> super.getCapability(capability, facing)
    }

    fun setEnabled(enabled: Boolean) {
        this.enabled = enabled
        markDirty()
    }

    fun setHeatLevel(heatLevel: Int) {
        if (heatLevel == configuredHeatLevel) return
        when (val state = recipeState) {
            is RecipeState.WaitingForEnable -> {
                if (heatLevel in state.recipe.heatLevels) {
                    configuredHeatLevel = heatLevel
                }
            }

            is RecipeState.Running -> {
                if (heatLevel in state.recipe.heatLevels) {
                    configuredHeatLevel = heatLevel
                    updateBlockState()
                }
            }

            null -> configuredHeatLevel = heatLevel
        }
        markDirty()
    }

    fun getConfigurableRange(): TIntList? = when (val state = recipeState) {
        is RecipeState.Running -> state.recipe.heatLevels
        is RecipeState.WaitingForEnable -> state.recipe.heatLevels
        null -> null
    }

    fun getConfiguredHeatLevel(): Int = roundHeatLevel(getConfigurableRange(), configuredHeatLevel)

    override fun update() {
        if (inputDirty) {
            if (!world.isRemote && recipeState == null) {
                findAndStartRecipe()
            }
            inputDirty = false
        }

        when (val state = recipeState) {
            null -> {}

            is RecipeState.WaitingForEnable -> {
                if (!world.isRemote && canWork()) {
                    startRecipe(state.recipe)
                }
            }

            is RecipeState.Running -> {
                if (--state.duration <= 0) {
                    if (!world.isRemote) {
                        recipeState = null
                        if (recipeMatches(state.recipe, _inventory.getStackInSlot(0))) {
                            tryStartRecipe(state.recipe)
                        } else {
                            findAndStartRecipe()
                        }
                        updateBlockState()
                    }
                }
                markDirty()
            }
        }

        if (updatingBlockState != null) {
            try {
                val oldState = world.getBlockState(pos)
                val newState = oldState.withProperty((oldState.block as BlockHeater).propHeat, heatLevel)
                if (oldState !== newState) {
                    updatingBlockState = true
                    world.setBlockState(pos, newState)
                }
            } finally {
                updatingBlockState = null
            }
        }
    }

    private fun findAndStartRecipe() {
        val fuel = _inventory.getStackInSlot(0)
        getRecipes().firstOrNull { recipeMatches(it, fuel) }?.let {
            tryStartRecipe(it)
        }
    }

    private fun tryStartRecipe(recipe: HeaterRecipe) {
        if (canWork()) {
            startRecipe(recipe)
        } else {
            recipeState = RecipeState.WaitingForEnable(recipe)
        }
    }

    private fun startRecipe(recipe: HeaterRecipe) {
        val fuel = _inventory.extractItem(0, 1, false)
        if (!fuel.isEmpty) { // probably shouldn't fail
            recipeState = RecipeState.Running(fuel, recipe, recipe.duration)
            configuredHeatLevel = roundHeatLevel(recipe.heatLevels, configuredHeatLevel)
            markDirty()
            updateBlockState()
        }
    }

    private fun canWork(): Boolean = enabled && world.getStrongPower(pos) <= 0

    private fun getRecipes(): RecipeRegistry<HeaterRecipe> = ExARecipeManager.heaterRecipes

    private fun recipeMatches(recipe: HeaterRecipe, fuelStack: ItemStack): Boolean = recipe.matches(fuelStack)

    fun dropInventory() {
        _inventory.dropInventory(world, pos.x + 0.5, pos.y + 0.5, pos.z + 0.5)
    }

    // zen foundry integration
    override fun provideHeat(maxHeat: Int, unused: Int): Int = if (heatLevel > 0) {
        ExAConfig.zenFoundryHeaterTemps[(heatLevel - 1).coerceAtMost(ExAConfig.zenFoundryHeaterTemps.size - 1)]
    } else {
        0
    }

    private fun updateBlockState() {
        updatingBlockState = false
    }

    override fun markDirty() {
        super.markDirty()
        stateClock++
    }

    override fun shouldRefresh(world: World, pos: BlockPos, oldState: IBlockState, newState: IBlockState): Boolean =
        updatingBlockState != true && oldState.block != newState.block

    override fun getUpdatePacket(): SPacketUpdateTileEntity {
        return SPacketUpdateTileEntity(pos, 0, NBTTagCompound().also { writeUpdateTag(it) })
    }

    override fun getUpdateTag(): NBTTagCompound = super.getUpdateTag().also { writeUpdateTag(it) }

    private fun writeUpdateTag(tag: NBTTagCompound) {
        recipeState?.writeToNBT(tag)
        tag.setByte("heat", configuredHeatLevel.toByte())
        tag.setBoolean("enabled", enabled)
    }

    override fun onDataPacket(net: NetworkManager, pkt: SPacketUpdateTileEntity) {
        handleUpdateTag(pkt.nbtCompound)
    }

    override fun handleUpdateTag(tag: NBTTagCompound) {
        deserializeRecipeState(tag)
        configuredHeatLevel = tag.getByte("heat").toInt()
        enabled = tag.getBoolean("enabled")
    }

    override fun writeToNBT(tag: NBTTagCompound): NBTTagCompound {
        super.writeToNBT(tag)
        tag.setByte("type", heaterType.ordinal.toByte())
        tag.setTag("inventory", _inventory.serializeNBT())
        writeUpdateTag(tag)
        return tag
    }

    override fun readFromNBT(tag: NBTTagCompound) {
        super.readFromNBT(tag)
        heaterType = HeaterType.entries[tag.getByte("type").toInt()]
        _inventory.deserializeNBT(tag.getCompoundTag("inventory"))
        handleUpdateTag(tag)
    }

    private fun deserializeRecipeState(tag: NBTTagCompound) {
        if (!tag.hasKey("state", Constants.NBT.TAG_BYTE)) {
            recipeState = null
            return
        }
        when (tag.getByte("state")) {
            0.toByte() -> {
                findRecipeForDeserialization(_inventory.getStackInSlot(0))?.let {
                    recipeState = RecipeState.WaitingForEnable(it)
                }
            }

            1.toByte() -> {
                val fuel = ItemStack(tag.getCompoundTag("fuel"))
                findRecipeForDeserialization(fuel)?.let {
                    recipeState = RecipeState.Running(fuel, it, tag.getInteger("duration"))
                }
            }

            else -> ExAlembico.logger.warn(
                "Bad state ${tag.getByte("state")} for heater in dimension ${world.provider.dimension} at $pos"
            )
        }
    }

    private fun findRecipeForDeserialization(fuel: ItemStack): HeaterRecipe? {
        if (fuel.isEmpty) {
            recipeState = null
            return null
        }
        val recipe = getRecipes().firstOrNull { recipeMatches(it, fuel) }
        if (recipe == null) {
            ExAlembico.logger.warn(
                "Could not find running recipe for fuel $fuel in heater in dimension ${world.provider.dimension} at $pos"
            )
            recipeState = null
        }
        return recipe
    }

    private sealed interface RecipeState {
        fun writeToNBT(tag: NBTTagCompound)

        class WaitingForEnable(val recipe: HeaterRecipe) : RecipeState {
            override fun writeToNBT(tag: NBTTagCompound) {
                tag.setByte("state", 0)
            }
        }

        class Running(
            val fuel: ItemStack,
            val recipe: HeaterRecipe,
            var duration: Int
        ) : RecipeState {
            override fun writeToNBT(tag: NBTTagCompound) {
                tag.setByte("state", 1)
                tag.setTag("fuel", fuel.serializeNBT())
                tag.setInteger("duration", duration)
            }
        }
    }

    private inner class HeaterInventory : ItemStackHandler(1) {
        override fun isItemValid(slot: Int, stack: ItemStack): Boolean = getRecipes().any { recipeMatches(it, stack) }

        fun dropInventory(world: World, x: Double, y: Double, z: Double) {
            stacks.forEach {
                if (!it.isEmpty) {
                    InventoryHelper.spawnItemStack(world, x, y, z, it)
                }
            }
        }

        override fun onContentsChanged(slot: Int) {
            inputDirty = true
            markDirty()
        }
    }
}
