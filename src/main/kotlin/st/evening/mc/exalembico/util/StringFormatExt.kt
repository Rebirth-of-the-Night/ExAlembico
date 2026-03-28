package st.evening.mc.exalembico.util

import kotlin.math.roundToInt

fun Float.formatPercentage(): String {
    if (this <= 0F) return "0%"
    if (this >= 1F) return "100%"
    val dec = (this * 1000).roundToInt()
    if (dec == 0) return "%.3f".format(this * 100F) // smaller than 0.05%!
    val frac = dec % 10
    return if (frac == 0) "${dec / 10}%" else "${dec / 10}.$frac%"
}

fun Int.formatTicksAsSeconds(): String = if (this % 20 == 0) "${this / 20}s" else "%.1fs".format(this / 20F)
