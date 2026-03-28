package st.evening.mc.exalembico.network

import io.netty.buffer.ByteBuf
import net.minecraftforge.fml.common.network.simpleimpl.IMessage
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext
import st.evening.mc.exalembico.block.heater.ContainerHeater
import st.evening.mc.exalembico.gui.EnableDisableContainer
import st.evening.mc.exalembico.gui.FluidContainer

class C2SClickFluidSlot(private var slotIndex: Int) : IMessage {
    constructor() : this(0)

    override fun toBytes(buf: ByteBuf) {
        buf.writeByte(slotIndex)
    }

    override fun fromBytes(buf: ByteBuf) {
        slotIndex = buf.readByte().toInt()
    }

    object Handler : IMessageHandler<C2SClickFluidSlot, IMessage> {
        override fun onMessage(message: C2SClickFluidSlot, ctx: MessageContext): IMessage? {
            val player = ctx.serverHandler.player
            player.serverWorld.addScheduledTask {
                (player.openContainer as? FluidContainer)?.handleFluidSlotClick(player, message.slotIndex)
            }
            return null
        }
    }
}

class C2SEnableDisable(private var enabled: Boolean) : IMessage {
    constructor() : this(true)

    override fun toBytes(buf: ByteBuf) {
        buf.writeBoolean(enabled)
    }

    override fun fromBytes(buf: ByteBuf) {
        enabled = buf.readBoolean()
    }

    object Handler : IMessageHandler<C2SEnableDisable, IMessage> {
        override fun onMessage(message: C2SEnableDisable, ctx: MessageContext): IMessage? {
            val player = ctx.serverHandler.player
            player.serverWorld.addScheduledTask {
                (player.openContainer as? EnableDisableContainer)?.handleSetEnabled(message.enabled)
            }
            return null
        }
    }
}

class C2SSetHeatLevel(private var heatLevel: Int) : IMessage {
    constructor() : this(0)

    override fun toBytes(buf: ByteBuf) {
        buf.writeShort(heatLevel)
    }

    override fun fromBytes(buf: ByteBuf) {
        heatLevel = buf.readShort().toInt()
    }

    object Handler : IMessageHandler<C2SSetHeatLevel, IMessage> {
        override fun onMessage(message: C2SSetHeatLevel, ctx: MessageContext): IMessage? {
            val player = ctx.serverHandler.player
            player.serverWorld.addScheduledTask {
                (player.openContainer as? ContainerHeater)?.handleSetHeatLevel(message.heatLevel)
            }
            return null
        }
    }
}
