package st.evening.mc.exalembico.block.alembic

import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.resources.I18n
import net.minecraft.entity.player.InventoryPlayer
import net.minecraft.util.ResourceLocation
import st.evening.mc.exalembico.ExAlembico
import st.evening.mc.exalembico.ExAlembicoConsts
import st.evening.mc.exalembico.gui.FluidSlot
import st.evening.mc.exalembico.gui.GuiFluidContainer
import kotlin.math.ceil
import kotlin.math.roundToInt

abstract class GuiAlembic(private val alembicCont: ContainerAlembic) : GuiFluidContainer(alembicCont) {
    companion object {
        val GUI_TEXTURES_ALEMBIC: ResourceLocation = ExAlembico.resource("textures/gui/alembic.png")
        val GUI_TEXTURES_ALEMBIC_NETHER: ResourceLocation =
            ExAlembico.resource("textures/gui/alembic_nether.png")

        fun create(playerInv: InventoryPlayer, alembic: TileEntityAlembic): GuiAlembic = when (alembic.alembicType) {
            AlembicType.ALEMBIC -> Alembic(playerInv, alembic)
            AlembicType.ALEMBIC_NETHER -> AlembicNether(playerInv, alembic)
        }
    }

    init {
        ySize = 180
    }

    constructor(playerInv: InventoryPlayer, alembic: TileEntityAlembic) : this(ContainerAlembic(playerInv, alembic))

    protected abstract val guiTextures: ResourceLocation

    override fun drawGuiContainerBackgroundLayer(partialTicks: Float, mouseX: Int, mouseY: Int) {
        GlStateManager.color(1F, 1F, 1F, 1F)
        bindGuiTexture()
        val offX = (width - xSize) / 2
        val offY = (height - ySize) / 2
        drawTexturedModalRect(offX, offY, 0, 0, xSize, ySize)

        GlStateManager.pushMatrix()
        GlStateManager.translate(guiLeft.toFloat(), guiTop.toFloat(), 0F)

        val heatLevel = alembicCont.heatLevel
        if (heatLevel > 0) {
            drawTexturedModalRect(59, 75, 176 + 10 * (heatLevel % 8), 9 * (heatLevel / 8), 10, 9)
        }

        val progHeight = ceil(20 * alembicCont.progressFraction).roundToInt()
        if (progHeight > 0) {
            drawTexturedModalRect(54, 14 + 20 - progHeight, 176, 59 + 20 - progHeight, 20, progHeight)
        }

        GlStateManager.popMatrix()
        super.drawGuiContainerBackgroundLayer(partialTicks, mouseX, mouseY)
    }

    override fun drawFluidSlot(slotIndex: Int, slot: FluidSlot) {
        super.drawFluidSlot(slotIndex, slot)
        bindGuiTexture()
        GlStateManager.color(1F, 1F, 1F, 1F)
        drawTexturedModalRect(slot.posX, slot.posY, 176 + 5 * slotIndex, 79, 5, 16)
    }

    override fun drawGuiContainerForegroundLayer(mouseX: Int, mouseY: Int) {
        super.drawGuiContainerForegroundLayer(mouseX, mouseY)
        fontRenderer.drawString(
            I18n.format("container.${ExAlembicoConsts.MOD_ID}.alembic"),
            8, ySize - 96 + 2, 0x404040
        )
    }

    override fun renderHoveredToolTip(mouseX: Int, mouseY: Int) {
        if (isPointInRegion(57, 74, 14, 12, mouseX, mouseY)) {
            drawHoveringText(
                I18n.format("${ExAlembicoConsts.MOD_ID}.tooltip.heat_level.${alembicCont.heatLevel}"),
                mouseX, mouseY
            )
        } else {
            super.renderHoveredToolTip(mouseX, mouseY)
        }
    }

    private fun bindGuiTexture() {
        mc.textureManager.bindTexture(guiTextures)
    }

    // these subclasses mostly exist because JEI recipe click areas are configured based on the GUI class
    class Alembic(playerInv: InventoryPlayer, alembic: TileEntityAlembic) : GuiAlembic(playerInv, alembic) {
        override val guiTextures: ResourceLocation
            get() = GUI_TEXTURES_ALEMBIC
    }

    class AlembicNether(playerInv: InventoryPlayer, alembic: TileEntityAlembic) : GuiAlembic(playerInv, alembic) {
        override val guiTextures: ResourceLocation
            get() = GUI_TEXTURES_ALEMBIC_NETHER
    }
}
