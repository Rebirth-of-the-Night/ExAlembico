package st.evening.mc.exalembico.block.alembic

import net.minecraft.util.IStringSerializable

enum class AlembicType : IStringSerializable {
    ALEMBIC, ALEMBIC_NETHER;

    override fun getName(): String = name.lowercase()

    companion object {
        fun forMeta(meta: Int): AlembicType = entries.let { it[meta % it.size] }
    }
}
