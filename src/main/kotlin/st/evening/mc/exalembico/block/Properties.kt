package st.evening.mc.exalembico.block

import com.google.common.base.Optional
import net.minecraft.block.properties.PropertyHelper

class PropertyHeat(name: String, val interval: IntRange) : PropertyHelper<Int>(name, Int::class.javaObjectType) {
    private val _allowedValues: AllowedValues = AllowedValues()

    override fun getName(value: Int): String = value.toString()

    override fun getAllowedValues(): Collection<Int> = _allowedValues

    fun getValueByIndex(index: Int): Int = if (index == 0) 0 else interval.first + index - 1

    fun getIndexForValue(value: Int): Int? = when (value) {
        0 -> 0
        in interval -> value - interval.first + 1
        else -> null
    }

    override fun parseValue(value: String): Optional<Int> {
        try {
            val intValue = value.toInt(10)
            return if (intValue in _allowedValues) Optional.of(intValue) else Optional.absent()
        } catch (_: NumberFormatException) {
            return Optional.absent()
        }
    }

    private inner class AllowedValues : AbstractSet<Int>() {
        override val size: Int
            get() = interval.last - interval.first + 2

        override fun contains(element: Int): Boolean = element == 0 || element in interval

        override fun iterator(): Iterator<Int> = IteratorImpl()
    }

    private inner class IteratorImpl : Iterator<Int> {
        private var next: Int? = null

        override fun hasNext(): Boolean = next?.let { it in interval } ?: true

        override fun next(): Int {
            next?.let {
                if (it !in interval) throw NoSuchElementException()
                next = it + 1
                return it
            }
            next = interval.first
            return 0
        }
    }
}
