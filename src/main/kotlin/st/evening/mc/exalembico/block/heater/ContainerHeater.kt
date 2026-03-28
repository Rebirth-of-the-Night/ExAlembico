package st.evening.mc.exalembico.block.heater

import gnu.trove.list.TIntList
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.entity.player.InventoryPlayer
import net.minecraft.inventory.Container
import net.minecraft.inventory.Slot
import net.minecraft.item.ItemStack
import net.minecraftforge.items.SlotItemHandler
import st.evening.mc.exalembico.gui.EnableDisableContainer

class ContainerHeater(playerInv: InventoryPlayer, private val heater: TileEntityHeater) :
    Container(), EnableDisableContainer {
    private var stateClock: Int = -1

    var configuredHeatLevel: Int = 0

    val enabled: Boolean
        get() = heater.enabled
    val heatLevel: Int
        get() = heater.heatLevel
    val durationFraction: Float
        get() = heater.durationFraction

    init {
        addSlotToContainer(SlotItemHandler(heater.inventory, 0, 80, 48))

        for (i in 0..<3) {
            for (j in 0..<9) {
                addSlotToContainer(Slot(playerInv, j + i * 9 + 9, 8 + j * 18, 103 + i * 18))
            }
        }
        for (k in 0..<9) {
            addSlotToContainer(Slot(playerInv, k, 8 + k * 18, 161))
        }
    }

    fun getConfigurableRange(): TIntList? = heater.getConfigurableRange()

    override fun canInteractWith(player: EntityPlayer): Boolean = player.getDistanceSq(heater.pos) <= 64

    override fun handleSetEnabled(enabled: Boolean) {
        heater.setEnabled(enabled)
    }

    fun handleSetHeatLevel(heatLevel: Int) {
        heater.setHeatLevel(heatLevel)
    }

    override fun transferStackInSlot(player: EntityPlayer, index: Int): ItemStack {
        val slot = inventorySlots[index]
        if (!slot.hasStack) return ItemStack.EMPTY

        val stack = slot.stack
        val orig = stack.copy()
        if (index == 0) {
            if (!mergeItemStack(stack, 1, 37, true)) return ItemStack.EMPTY
        } else {
            if (!mergeItemStack(stack, 0, 1, false)) return ItemStack.EMPTY
        }

        if (stack.isEmpty) {
            slot.putStack(ItemStack.EMPTY)
        } else {
            slot.onSlotChanged()
        }
        if (orig.count == stack.count) return ItemStack.EMPTY
        slot.onTake(player, stack)
        return orig
    }

    override fun detectAndSendChanges() {
        super.detectAndSendChanges()
        if (stateClock < heater.stateClock) {
            val packet = heater.updatePacket
            listeners.forEach {
                if (it is EntityPlayerMP) {
                    it.connection.sendPacket(packet)
                }
            }
            stateClock = heater.stateClock
        }

        val configuredHeatLevel = heater.getConfiguredHeatLevel()
        if (this.configuredHeatLevel != configuredHeatLevel) {
            listeners.forEach { it.sendWindowProperty(this, 0, configuredHeatLevel) }
            this.configuredHeatLevel = configuredHeatLevel
        }
    }

    override fun updateProgressBar(id: Int, data: Int) {
        configuredHeatLevel = data
    }
}
