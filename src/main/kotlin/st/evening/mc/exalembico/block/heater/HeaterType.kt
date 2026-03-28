package st.evening.mc.exalembico.block.heater

import net.minecraft.util.IStringSerializable

enum class HeaterType(val heatLevels: IntRange) : IStringSerializable {
    HEATER(1..15);

    override fun getName(): String = name.lowercase()
}
