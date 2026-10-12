package com.trackmyfit.app.data

/** Works out which plates to load on each side of a bar. */
object Plates {
    val kgPlates = listOf(25.0, 20.0, 15.0, 10.0, 5.0, 2.5, 1.25)
    val lbPlates = listOf(45.0, 35.0, 25.0, 10.0, 5.0, 2.5)
    val kgBars = listOf(20.0, 15.0, 10.0, 0.0)
    val lbBars = listOf(45.0, 35.0, 25.0, 0.0)

    data class Result(val perSide: List<Double>, val loaded: Double, val remainder: Double)

    /** All values in the display unit. */
    fun calculate(target: Double, bar: Double, plates: List<Double>): Result {
        var side = ((target - bar) / 2.0).coerceAtLeast(0.0)
        val used = mutableListOf<Double>()
        for (p in plates.sortedDescending()) {
            while (side + 1e-9 >= p) { used += p; side -= p }
        }
        val loaded = bar + used.sum() * 2
        return Result(used, loaded, (target - loaded).coerceAtLeast(0.0))
    }
}
