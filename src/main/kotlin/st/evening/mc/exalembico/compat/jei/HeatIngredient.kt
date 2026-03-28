package st.evening.mc.exalembico.compat.jei

import mezz.jei.api.ingredients.IIngredientHelper
import mezz.jei.api.ingredients.IIngredientRenderer
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Gui
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.resources.I18n
import net.minecraft.client.util.ITooltipFlag
import net.minecraft.util.ResourceLocation
import st.evening.mc.exalembico.ExAlembicoConsts
import st.evening.mc.exalembico.block.alembic.GuiAlembic
import st.evening.mc.exalembico.block.heater.GuiHeater

object HeatJeiIngredientHelper : IIngredientHelper<Int> {
    override fun getMatch(ingredients: Iterable<Int>, ingredientToMatch: Int): Int? =
        if (ingredientToMatch in ingredients) ingredientToMatch else null

    override fun getDisplayName(ingredient: Int): String =
        I18n.format("${ExAlembicoConsts.MOD_ID}.tooltip.heat_level.$ingredient")

    override fun getUniqueId(ingredient: Int): String = "${ExAlembicoConsts.MOD_ID}.heat:$ingredient"

    override fun getHash(ingredient: Int): Int = Integer.hashCode(ingredient)

    override fun getWildcardId(ingredient: Int): String = getUniqueId(ingredient)

    override fun getModId(ingredient: Int): String = ExAlembicoConsts.MOD_ID

    override fun getResourceId(ingredient: Int): String = ingredient.toString()

    override fun copyIngredient(ingredient: Int): Int = ingredient

    override fun getErrorInfo(ingredient: Int?): String = ingredient?.let { "heat $it" } ?: "null"
}

class HeatJeiIngredientRenderer(
    private val atlasTexture: ResourceLocation,
    private val offsetX: Int,
    private val offsetY: Int,
    private val iconGridU: Int,
    private val iconGridV: Int,
    private val iconWidth: Int,
    private val iconHeight: Int,
    private val iconGridRowSize: Int,
    private val heatLevelOffset: Int
) : IIngredientRenderer<Int> {
    companion object {
        val HEATER: HeatJeiIngredientRenderer =
            HeatJeiIngredientRenderer(GuiHeater.GUI_TEXTURES_HEATER, 1, 1, 201, 69, 11, 13, 5, 1)
        val ALEMBIC: HeatJeiIngredientRenderer =
            HeatJeiIngredientRenderer(GuiAlembic.GUI_TEXTURES_ALEMBIC, 2, 1, 176, 0, 10, 9, 8, 1)
        val ALEMBIC_NETHER: HeatJeiIngredientRenderer =
            HeatJeiIngredientRenderer(GuiAlembic.GUI_TEXTURES_ALEMBIC_NETHER, 2, 1, 176, 0, 10, 9, 8, 1)
    }

    override fun render(mc: Minecraft, xPosition: Int, yPosition: Int, heatLevel: Int?) {
        if (heatLevel == null || heatLevel < heatLevelOffset) return
        val k = heatLevel - heatLevelOffset
        if (k < 0) return
        GlStateManager.enableBlend()
        mc.textureManager.bindTexture(atlasTexture)
        val u = (iconGridU + iconWidth * (k % iconGridRowSize)).toFloat()
        val v = (iconGridV + iconHeight * (k / iconGridRowSize)).toFloat()
        Gui.drawModalRectWithCustomSizedTexture(
            xPosition + offsetX, yPosition + offsetY, u, v, iconWidth, iconHeight, 256F, 256F
        )
    }

    override fun getTooltip(mc: Minecraft, ingredient: Int, tooltipFlag: ITooltipFlag): List<String> =
        listOf(HeatJeiIngredientHelper.getDisplayName(ingredient))
}
