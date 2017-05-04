package de.mabe.cadaster.expression


class Wertebereiche {

}


class Grenzwert(val str: String) {
  private val lower: Grenze
  private val upper: Grenze

  init {
    val lowerStr = str.split(",")[0]
    val upperStr = str.split(",")[1]

    lower = when {
      lowerStr in listOf("[-I", "]-I") -> INFINITE
      lowerStr[0] == ']' -> VAL_EXKL(lowerStr.substring(1).toDouble())
      lowerStr[0] == '[' -> VAL_INKL(lowerStr.substring(1).toDouble())
      else -> throw IllegalStateException("NOT VALID: $str")
    }

    upper = when {
      upperStr.endsWith("I[") || upperStr.endsWith("I]") -> INFINITE
      upperStr.endsWith("]") -> VAL_INKL(upperStr.dropLast(1).toDouble())
      upperStr.endsWith("[") -> VAL_EXKL(upperStr.dropLast(1).toDouble())
      else -> throw IllegalStateException("NOT VALID: $str")
    }
  }

  fun inRange(value: Double) = when (lower) {
    is INFINITE -> true
    is VAL_INKL -> value >= lower.value
    is VAL_EXKL -> value > lower.value
  } && when (upper) {
    is INFINITE -> true
    is VAL_INKL -> value <= upper.value
    is VAL_EXKL -> value < upper.value
  }

  override fun toString() = str
}

private sealed class Grenze

private object INFINITE : Grenze()
private class VAL_INKL(val value: Double) : Grenze()
private class VAL_EXKL(val value: Double) : Grenze()
