package st.evening.mc.exalembico.block.heater

import gnu.trove.list.TIntList
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiButton
import net.minecraft.client.gui.inventory.GuiContainer
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.resources.I18n
import net.minecraft.entity.player.InventoryPlayer
import net.minecraft.util.ResourceLocation
import net.minecraft.util.math.MathHelper
import st.evening.mc.exalembico.ExAlembico
import st.evening.mc.exalembico.ExAlembicoConsts
import st.evening.mc.exalembico.network.C2SEnableDisable
import st.evening.mc.exalembico.network.C2SSetHeatLevel
import kotlin.math.roundToInt

class GuiHeater(private val heaterCont: ContainerHeater) : GuiContainer(heaterCont) {
    companion object {
        val GUI_TEXTURES_HEATER: ResourceLocation = ExAlembico.resource("textures/gui/heater.png")
    }

    private lateinit var heatLevelSlider: HeatLevelSlider

    init {
        ySize = 185
    }

    constructor(playerInv: InventoryPlayer, heater: TileEntityHeater) : this(ContainerHeater(playerInv, heater))

    override fun initGui() {
        super.initGui()
        addButton(PowerButton(guiLeft + 57, guiTop + 73))
        heatLevelSlider = HeatLevelSlider(guiLeft + 78, guiTop + 73)
        addButton(heatLevelSlider)
    }

    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        drawDefaultBackground()
        super.drawScreen(mouseX, mouseY, partialTicks)
        renderHoveredToolTip(mouseX, mouseY)
    }

    override fun drawGuiContainerBackgroundLayer(partialTicks: Float, mouseX: Int, mouseY: Int) {
        GlStateManager.pushMatrix()
        GlStateManager.translate(guiLeft.toFloat(), guiTop.toFloat(), 0F)
        GlStateManager.color(1F, 1F, 1F, 1F)

        mc.textureManager.bindTexture(GUI_TEXTURES_HEATER)
        drawTexturedModalRect(0, 0, 0, 0, xSize, ySize)

        val barWidth = MathHelper.ceil(63 * heaterCont.durationFraction)
        if (barWidth > 0) {
            drawTexturedModalRect(57 + 63 - barWidth, 65, 176 + 63 - barWidth, 62, barWidth, 7)
        }

        val heatLevel = heaterCont.heatLevel
        if (heatLevel > 0) {
            val i = heatLevel - 1
            drawTexturedModalRect(83, 34, 201 + 11 * (i % 5), 69 + 13 * (i / 5), 11, 13)
        }
        GlStateManager.popMatrix()
    }

    override fun drawGuiContainerForegroundLayer(mouseX: Int, mouseY: Int) {
        fontRenderer.drawString(
            I18n.format("container.${ExAlembicoConsts.MOD_ID}.heater"),
            8, ySize - 96 + 2, 0x404040
        )
    }

    override fun renderHoveredToolTip(mouseX: Int, mouseY: Int) {
        if (isPointInRegion(83, 34, 11, 13, mouseX, mouseY)) {
            drawHoveringText(
                I18n.format("${ExAlembicoConsts.MOD_ID}.tooltip.heat_level.${heaterCont.heatLevel}"),
                mouseX, mouseY
            )
        } else if (heatLevelSlider.isMouseOver) {
            heatLevelSlider.getSliderHeatLevel(mouseX)?.let {
                drawHoveringText(
                    I18n.format("${ExAlembicoConsts.MOD_ID}.tooltip.heat_level.$it"),
                    mouseX, mouseY
                )
            }
        } else {
            super.renderHoveredToolTip(mouseX, mouseY)
        }
    }

    override fun actionPerformed(button: GuiButton) {
        if (button.id == 999) {
            ExAlembico.network.sendToServer(C2SEnableDisable(!heaterCont.enabled))
        }
    }

    private inner class PowerButton(x: Int, y: Int) : GuiButton(999, x, y, 10, 9, "") {
        override fun drawButton(mc: Minecraft, mouseX: Int, mouseY: Int, partialTicks: Float) {
            if (!visible) return
            hovered = mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height
            mc.textureManager.bindTexture(GUI_TEXTURES_HEATER)
            GlStateManager.color(1F, 1F, 1F, 1F)
            drawTexturedModalRect(x, y, if (heaterCont.enabled) 186 else 176, if (hovered) 99 else 90, 10, 9)
        }
    }

    private inner class HeatLevelSlider(x: Int, y: Int) : GuiButton(1000, x, y, 41, 9, "") {
        private var dragging: Boolean = false
        private var currentIndex: Int = 0

        override fun drawButton(mc: Minecraft, mouseX: Int, mouseY: Int, partialTicks: Float) {
            if (!visible) return
            hovered = mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height
            heaterCont.getConfigurableRange()?.let { range ->
                if (range.size() <= 1) return@let
                val index = getVisibleIndex(range)
                if (index >= range.size()) return@let
                drawTexturedModalRect(x + 39 * index / (range.size() - 1), y, 176, 77, 2, 9)
            }
            mouseDragged(mc, mouseX, mouseY)
        }

        override fun mousePressed(mc: Minecraft, mouseX: Int, mouseY: Int): Boolean {
            if (!super.mousePressed(mc, mouseX, mouseY)) return false
            val index = getSliderIndex(mouseX) ?: return false
            dragging = true
            currentIndex = index
            return true
        }

        override fun mouseDragged(mc: Minecraft, mouseX: Int, mouseY: Int) {
            if (dragging) {
                getSliderIndex(mouseX)?.let { currentIndex = it }
            }
        }

        override fun mouseReleased(mouseX: Int, mouseY: Int) {
            if (dragging) {
                dragging = false
                heaterCont.getConfigurableRange()?.let { heatLevels ->
                    val heatLevel = heatLevels[currentIndex]
                    heaterCont.configuredHeatLevel = heatLevel
                    ExAlembico.network.sendToServer(C2SSetHeatLevel(heatLevel))
                }
            }
        }

        fun getSliderHeatLevel(mouseX: Int): Int? {
            val heatLevels = heaterCont.getConfigurableRange() ?: return null
            return heatLevels[getSliderIndex(mouseX, heatLevels.size())]
        }

        private fun getSliderIndex(mouseX: Int): Int? =
            heaterCont.getConfigurableRange()?.let { getSliderIndex(mouseX, it.size()) }

        private fun getSliderIndex(mouseX: Int, range: Int): Int {
            val frac = (mouseX - x - 1) / (width - 2).toFloat()
            return when {
                frac <= 0 -> 0
                frac >= 1 -> range - 1
                else -> (frac * (range - 1)).roundToInt()
            }
        }

        private fun getVisibleIndex(range: TIntList): Int {
            if (dragging) return currentIndex
            val heatLevel = heaterCont.configuredHeatLevel
            if (range[currentIndex] != heatLevel) {
                val index = range.binarySearch(heatLevel)
                if (index >= 0) {
                    currentIndex = index
                    return index
                }
            }
            return currentIndex
        }
    }
}
