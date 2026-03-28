package st.evening.mc.exalembico.compat.jei

import mezz.jei.api.gui.IDrawable
import mezz.jei.api.ingredients.IIngredientRenderer
import mezz.jei.plugins.vanilla.ingredients.fluid.FluidStackRenderer
import mezz.jei.plugins.vanilla.ingredients.item.ItemStackRenderer
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Gui
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.util.ITooltipFlag
import net.minecraft.item.ItemStack
import net.minecraftforge.fluids.FluidStack

abstract class JeiTransformItemRenderer : IIngredientRenderer<ItemStack> {
    companion object {
        private val itemRenderer: ItemStackRenderer = ItemStackRenderer()
    }

    override fun render(mc: Minecraft, posX: Int, posY: Int, stack: ItemStack?) {
        if (stack == null) return
        GlStateManager.pushMatrix()
        GlStateManager.scale(0.5F, 0.5F, 0.5F)
        itemRenderer.render(mc, posX * 2, posY * 2, stack)
        GlStateManager.popMatrix()

        GlStateManager.disableDepth()
        GlStateManager.enableBlend()
        mc.textureManager.bindTexture(ExAJeiPlugin.JEI_TEXTURE)
        drawArrow(posX, posY)
        GlStateManager.enableDepth()
    }

    protected abstract fun drawArrow(x: Int, y: Int)

    override fun getTooltip(mc: Minecraft, ingredient: ItemStack, tooltipFlag: ITooltipFlag): List<String> =
        itemRenderer.getTooltip(mc, ingredient, tooltipFlag)
}

object JeiTransformInputItemRenderer : JeiTransformItemRenderer() {
    override fun drawArrow(x: Int, y: Int) {
        Gui.drawModalRectWithCustomSizedTexture(x + 6, y + 6, 251F, 0F, 5, 5, 256F, 256F)
    }
}

object JeiTransformOutputItemRenderer : JeiTransformItemRenderer() {
    override fun drawArrow(x: Int, y: Int) {
        Gui.drawModalRectWithCustomSizedTexture(x - 3, y - 3, 251F, 5F, 5, 5, 256F, 256F)
    }
}

class JeiTransformOutputFluidRenderer(
    capacity: Int,
    showCapacity: Boolean,
    width: Int,
    height: Int,
    overlay: IDrawable?
) : IIngredientRenderer<FluidStack> {
    private val fluidRenderer: FluidStackRenderer = FluidStackRenderer(capacity, showCapacity, width, height, overlay)

    override fun render(mc: Minecraft, posX: Int, posY: Int, stack: FluidStack?) {
        if (stack == null) return
        fluidRenderer.render(mc, posX, posY, stack)

        GlStateManager.disableDepth()
        GlStateManager.enableBlend()
        GlStateManager.color(1F, 1F, 1F, 1F)
        mc.textureManager.bindTexture(ExAJeiPlugin.JEI_TEXTURE)
        Gui.drawModalRectWithCustomSizedTexture(posX - 3, posY + 4, 251F, 5F, 5, 5, 256F, 256F)
        GlStateManager.enableDepth()
    }

    override fun getTooltip(mc: Minecraft, ingredient: FluidStack, tooltipFlag: ITooltipFlag): List<String> =
        fluidRenderer.getTooltip(mc, ingredient, tooltipFlag)
}
