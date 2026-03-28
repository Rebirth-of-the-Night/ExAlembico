package st.evening.mc.exalembico.gui

import net.minecraft.client.gui.inventory.GuiContainer
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.Tessellator
import net.minecraft.client.renderer.texture.TextureAtlasSprite
import net.minecraft.client.renderer.texture.TextureMap
import net.minecraft.client.renderer.vertex.DefaultVertexFormats
import net.minecraft.client.resources.I18n
import net.minecraft.util.text.TextFormatting
import net.minecraftforge.fluids.FluidStack
import org.lwjgl.opengl.GL11
import st.evening.mc.exalembico.ExAlembico
import st.evening.mc.exalembico.ExAlembicoConsts
import st.evening.mc.exalembico.network.C2SClickFluidSlot

abstract class GuiFluidContainer(protected val fluidCont: FluidContainer) : GuiContainer(fluidCont) {
    private var hoveredFluidSlot: Int = -1

    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        drawDefaultBackground()
        super.drawScreen(mouseX, mouseY, partialTicks)
        renderHoveredToolTip(mouseX, mouseY)
    }

    override fun drawGuiContainerBackgroundLayer(partialTicks: Float, mouseX: Int, mouseY: Int) {
        GlStateManager.pushMatrix()
        GlStateManager.translate(guiLeft.toFloat(), guiTop.toFloat(), 0F)
        fluidCont.fluidSlots.forEachIndexed { slotIndex, slot -> drawFluidSlot(slotIndex, slot) }
        GlStateManager.popMatrix()
    }

    override fun drawGuiContainerForegroundLayer(mouseX: Int, mouseY: Int) {
        fluidCont.fluidSlots.forEachIndexed { slotIndex, slot ->
            if (isPointInRegion(slot.posX, slot.posY, slot.width, slot.height, mouseX, mouseY)) {
                drawFluidSlotHighlight(slotIndex, slot)
                hoveredFluidSlot = slotIndex
                return
            }
        }
        hoveredFluidSlot = -1
    }

    open fun drawFluidSlot(slotIndex: Int, slot: FluidSlot) {
        val fluid = slot.tank.fluid
        if (fluid == null || fluid.amount <= 0) return
        val frac = fluid.amount / slot.tank.capacity.toDouble()

        val col = fluid.fluid.getColor(fluid)
        GlStateManager.color(
            ((col ushr 16) and 0xFF) / 255F,
            ((col ushr 8) and 0xFF) / 255F,
            (col and 0xFF) / 255F,
            1F
        )

        mc.textureManager.bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE)
        val sprite = fluid.getSprite()
        val xi = slot.posX.toDouble()
        val xf = xi + 5.0
        val yf = slot.posY.toDouble() + 16.0
        val yi = yf - 16.0 * frac
        val z = zLevel.toDouble()
        val ui = sprite.minU.toDouble()
        val uf = ui + 0.3125 * (sprite.maxU - ui)
        val vf = sprite.maxV.toDouble()
        val vi = vf - frac * (vf - sprite.minV)

        val tess = Tessellator.getInstance()
        val buf = tess.buffer
        buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX)
        buf.pos(xi, yf, z).tex(ui, vf).endVertex()
        buf.pos(xf, yf, z).tex(uf, vf).endVertex()
        buf.pos(xf, yi, z).tex(uf, vi).endVertex()
        buf.pos(xi, yi, z).tex(ui, vi).endVertex()
        tess.draw()
    }

    protected fun FluidStack.getSprite(): TextureAtlasSprite =
        fluid.getStill(this)?.let { mc.textureMapBlocks.getTextureExtry(it.toString()) }
            ?: mc.textureMapBlocks.missingSprite

    open fun drawFluidSlotHighlight(slotIndex: Int, slot: FluidSlot) {
        GlStateManager.disableDepth()
        GlStateManager.colorMask(true, true, true, false)
        val x = slot.posX
        val y = slot.posY
        drawGradientRect(x, y, x + slot.width, y + slot.height, 0x80FFFFFF.toInt(), 0x80FFFFFF.toInt())
        GlStateManager.colorMask(true, true, true, true)
        GlStateManager.enableDepth()
    }

    override fun renderHoveredToolTip(mouseX: Int, mouseY: Int) {
        if (hoveredFluidSlot == -1) {
            super.renderHoveredToolTip(mouseX, mouseY)
            return
        }

        if (!mc.player.inventory.itemStack.isEmpty) return
        val tank = fluidCont.fluidSlots[hoveredFluidSlot].tank
        val fluid = tank.fluid
        val text = if (fluid != null && fluid.amount > 0) {
            listOf(
                fluid.localizedName,
                "${TextFormatting.GRAY}${fluid.amount} / ${tank.capacity} mB"
            )
        } else {
            listOf(
                I18n.format("${ExAlembicoConsts.MOD_ID}.tooltip.empty_tank"),
                "${TextFormatting.GRAY}0 / ${tank.capacity} mB"
            )
        }
        drawHoveringText(text, mouseX, mouseY)
    }

    override fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int) {
        if (hoveredFluidSlot >= 0) {
            ExAlembico.network.sendToServer(C2SClickFluidSlot(hoveredFluidSlot))
        } else {
            super.mouseClicked(mouseX, mouseY, mouseButton)
        }
    }
}
