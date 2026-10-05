package st.evening.mc.exalembico.heat

interface HeatSource {
    val heatLevel: Int

    val showLegs: Boolean

    object Trivial : HeatSource {
        override val heatLevel: Int
            get() = 0

        override val showLegs: Boolean
            get() = false
    }
}
