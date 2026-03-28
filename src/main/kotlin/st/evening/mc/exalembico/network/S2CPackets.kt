package st.evening.mc.exalembico.network

import io.netty.buffer.ByteBuf
import net.minecraft.client.Minecraft
import net.minecraft.network.PacketBuffer
import net.minecraftforge.fluids.FluidRegistry
import net.minecraftforge.fluids.FluidStack
import net.minecraftforge.fml.common.network.simpleimpl.IMessage
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext
import st.evening.mc.exalembico.ExAlembico
import st.evening.mc.exalembico.gui.FluidContainer

class S2CSyncFluidSlot(private var slotIndex: Int, private var stack: FluidStack?) : IMessage {
    constructor() : this(0, null)

    override fun toBytes(buf: ByteBuf) {
        val buf = PacketBuffer(buf)
        buf.writeByte(slotIndex)
        stack?.let {
            buf.writeString(it.fluid.name)
            buf.writeInt(it.amount)
            buf.writeCompoundTag(it.tag)
        } ?: run {
            buf.writeString("")
        }
    }

    override fun fromBytes(buf: ByteBuf) {
        val buf = PacketBuffer(buf)
        slotIndex = buf.readByte().toInt()
        val fluidName = buf.readString(32767)
        run {
            if (fluidName.isEmpty()) {
                return@run
            } else {
                val fluid = FluidRegistry.getFluid(fluidName)
                if (fluid == null) {
                    ExAlembico.logger.warn("Unknown fluid: {}", fluidName)
                    return@run
                }
                val amount = buf.readInt()
                if (amount <= 0) {
                    return@run
                }
                stack = FluidStack(fluid, amount, buf.readCompoundTag())
                return
            }
        }
        stack = null
    }

    object Handler : IMessageHandler<S2CSyncFluidSlot, IMessage> {
        override fun onMessage(message: S2CSyncFluidSlot, ctx: MessageContext): IMessage? {
            val mc = Minecraft.getMinecraft()
            mc.addScheduledTask {
                (mc.player.openContainer as? FluidContainer)?.handleSyncFluidSlot(message.slotIndex, message.stack)
            }
            return null
        }
    }
}
