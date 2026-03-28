package st.evening.mc.exalembico.util

import gnu.trove.TIntCollection

inline fun <T, C : MutableCollection<T>> TIntCollection.mapTo(dest: C, f: (Int) -> T): C {
    val iter = iterator()
    while (iter.hasNext()) {
        dest += f(iter.next())
    }
    return dest
}

inline fun <T> TIntCollection.map(f: (Int) -> T): List<T> = if (isEmpty) listOf() else mapTo(mutableListOf(), f)
