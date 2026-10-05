package st.evening.mc.exalembico.block.alembic

import net.minecraft.entity.player.EntityPlayer
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.entity.player.InventoryPlayer
import net.minecraft.inventory.Slot
import net.minecraft.item.ItemStack
import net.minecraftforge.items.SlotItemHandler
import st.evening.mc.exalembico.gui.FluidContainer
import st.evening.mc.exalembico.gui.FluidSlot

class ContainerAlembic(playerInv: InventoryPlayer, private val alembic: TileEntityAlembic) :
    FluidContainer() {
    override val fluidSlots: List<FluidSlot> = listOf(
        FluidSlot(alembic.inputTank1, 81, 43, 5, 16),
        FluidSlot(alembic.inputTank2, 128, 43, 5, 16),
        FluidSlot(alembic.outputTank1, 81, 92, 5, 16),
        FluidSlot(alembic.outputTank2, 128, 92, 5, 16)
    )
    private var stateClock: Int = -1

    var heatLevel: Int = 0

    val progressFraction: Float
        get() = alembic.progressFraction

    init {
        addSlotToContainer(SlotItemHandler(alembic.inventory, TileEntityAlembic.SLOT_IN1, 42, 43))
        addSlotToContainer(SlotItemHandler(alembic.inventory, TileEntityAlembic.SLOT_IN2, 61, 43))
        addSlotToContainer(SlotItemHandler(alembic.inventory, TileEntityAlembic.SLOT_IN3, 108, 43))
        addSlotToContainer(SlotItemHandler(alembic.inventory, TileEntityAlembic.SLOT_OUT_BONUS, 136, 50))
        addSlotToContainer(SlotItemHandler(alembic.inventory, TileEntityAlembic.SLOT_OUT1, 42, 92))
        addSlotToContainer(SlotItemHandler(alembic.inventory, TileEntityAlembic.SLOT_OUT2, 61, 92))
        addSlotToContainer(SlotItemHandler(alembic.inventory, TileEntityAlembic.SLOT_OUT3, 108, 92))

        for (i in 0..<3) {
            for (j in 0..<9) {
                addSlotToContainer(Slot(playerInv, j + i * 9 + 9, 8 + j * 18, 114 + i * 18))
            }
        }
        for (k in 0..<9) {
            addSlotToContainer(Slot(playerInv, k, 8 + k * 18, 172))
        }
    }

    override fun canInteractWith(player: EntityPlayer): Boolean = player.getDistanceSq(alembic.pos) <= 64

    override fun transferStackInSlot(player: EntityPlayer, index: Int): ItemStack {
        val slot = inventorySlots[index]
        if (!slot.hasStack) return ItemStack.EMPTY

        val stack = slot.stack
        val orig = stack.copy()
        if (index < TileEntityAlembic.NUM_SLOTS) {
            if (!mergeItemStack(stack, TileEntityAlembic.NUM_SLOTS, 36 + TileEntityAlembic.NUM_SLOTS, true)) {
                return ItemStack.EMPTY
            }
        } else if (!mergeItemStack(stack, 0, TileEntityAlembic.SLOT_OUT_BONUS, true)) {
            return ItemStack.EMPTY
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
        if (stateClock < alembic.stateClock) {
            val packet = alembic.updatePacket
            listeners.forEach {
                if (it is EntityPlayerMP) {
                    it.connection.sendPacket(packet)
                }
            }
            stateClock = alembic.stateClock
        }

        val heatLevel = alembic.getHeatLevel()
        if (this.heatLevel != heatLevel) {
            listeners.forEach { it.sendWindowProperty(this, 0, heatLevel) }
            this.heatLevel = heatLevel
        }
    }

    override fun updateProgressBar(id: Int, data: Int) {
        heatLevel = data
    }
}
