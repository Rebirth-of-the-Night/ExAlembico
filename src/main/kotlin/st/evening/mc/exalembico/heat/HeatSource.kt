package st.evening.mc.exalembico.heat

interface HeatSource {
    val heatLevel: Int

    object Trivial : HeatSource {
        override val heatLevel: Int
            get() = 0
    }
}
