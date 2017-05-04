package de.mabe.cadaster.expression


private infix fun List<*>.equalsIgnoreOrder(other: List<*>): Boolean {
  val otherList = other.toMutableList()
  this.forEach { if (!otherList.remove(it)) return false }
  return otherList.isEmpty()
}

class Ergebnis {
  val variable: String
  val list: List<TeilErgebnis>

  constructor(variable: String, list: List<TeilErgebnis>) {
    this.variable = variable
    this.list = simplifyList(list)
  }

  constructor(variable: String, string: String) {
    this.variable = variable
    list = simplifyList(parseString(string))
  }

  private fun parseString(ergebnisAlsString: String): List<TeilErgebnis> =
      ergebnisAlsString.split(";").map { part ->
        if (part.contains(",")) {
          Grenzwert(part.trim())
        } else {
          KonkretesErgebnis(part.trim().toDouble())
        }
      }

  private fun simplifyList(list: List<TeilErgebnis>): List<TeilErgebnis> {
    val listSize = list.size

    if (listSize == 1)
      return list

    for (i in 0..listSize - 1) {
      for (j in i + 1..listSize - 1) {
        val joinResult = list[i] outerJoin list[j]
        if (joinResult != null) {
          val preparedList = list.toMutableList()
          preparedList.removeAt(j)
          preparedList.removeAt(i)
          preparedList.add(joinResult)
          return simplifyList(preparedList)
        }
      }
    }

    return list
  }

  override fun equals(other: Any?) = (other as? Ergebnis)?.list!! equalsIgnoreOrder list

  override fun toString() = "$variable: " + list.map { it.toString() }.joinToString(";")
}

// ******************************************************************************************************************
// ******************************************************************************************************************
// ******************************************************************************************************************

sealed class TeilErgebnis {
  /** ein neues TeilErgebnis, welches die beiden zusmmenfässt oder null, wenn es sich nicht zusammenfassen lässt */
  infix fun outerJoin(other: TeilErgebnis): TeilErgebnis? {
    return when {
    /******** Beide Konkret */
      this is KonkretesErgebnis && other is KonkretesErgebnis -> {
        if (this.value == other.value) this else null
      }
    /******** Beide Grenzwert */
      this is Grenzwert && other is Grenzwert -> {
        this.grenzwertOuterJoin(other)
      }
    /******** 1x Grenzwert, 1x Konkret */
      else -> {
        val (grenzwert, konkret) = when {
          this is Grenzwert -> Pair(this as Grenzwert, other as KonkretesErgebnis)
          else -> Pair(other as Grenzwert, this as KonkretesErgebnis)
        }
        if (grenzwert.inRange(konkret.value)) {
          grenzwert
        } else {
          grenzwert.isOn(konkret.value)
        }
      }
    }
  }

  abstract fun inRange(value: Double): Boolean
}

// ******************************************************************************************************************

class KonkretesErgebnis(val value: Double) : TeilErgebnis() {
  override fun inRange(value: Double) = value == this.value

  override fun toString() = value.toString()
  override fun equals(other: Any?) = (other as? KonkretesErgebnis)?.value == value
}

// ******************************************************************************************************************

class Grenzwert : TeilErgebnis {
  private val lower: Grenze
  private val upper: Grenze

  constructor(str: String) : super() {
    val lowerStr = str.split(",")[0]
    val upperStr = str.split(",")[1]

    lower = when {
      lowerStr in listOf("[-I", "]-I", "[I", "]I") -> INFINITE
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

  private constructor(lower: Grenze, upper: Grenze) {
    this.lower = lower
    this.upper = upper
  }

  /** ein Neuer Grenzwert, der den value inkludiert oder null */
  fun isOn(value: Double) = when {
    lower is VAL_EXKL && lower.value == value -> Grenzwert(VAL_INKL(value), upper)
    upper is VAL_EXKL && upper.value == value -> Grenzwert(lower, VAL_INKL(value))
    else -> null
  }

  /** true or false if given number is in Range */
  override fun inRange(value: Double) = when (lower) {
    is INFINITE -> true
    is VAL_INKL -> value >= lower.value
    is VAL_EXKL -> value > lower.value
    else -> throw IllegalStateException("Boo")
  } && when (upper) {
    is INFINITE -> true
    is VAL_INKL -> value <= upper.value
    is VAL_EXKL -> value < upper.value
    else -> throw IllegalStateException("Boo")
  }

  /** liefert einen neuen Grenzwert zurück, der beide Grenzwerte vereint oder null, wenn sie sich nicht vereinen lassen */
  fun grenzwertOuterJoin(other: Grenzwert): Grenzwert? {
    val (kleiner, groesser) = when {
      lower is INFINITE -> Pair(this, other)
      other.lower is INFINITE -> Pair(other, this)
      else -> {
        val thisLowerValue = if (this.lower is VAL_INKL) this.lower.value else (this.lower as VAL_EXKL).value
        val otherLowerValue = if (other.lower is VAL_INKL) other.lower.value else (other.lower as VAL_EXKL).value
        if (thisLowerValue < otherLowerValue) Pair(this, other) else Pair(other, this)
      }
    }

    val touching = kleiner.upper is INFINITE ||
        groesser.lower is INFINITE ||
        kleiner.upper.value > groesser.lower.value ||
        (kleiner.upper.value == groesser.lower.value) && !(kleiner.upper is VAL_EXKL && groesser.lower is VAL_EXKL)

    if (!touching)
      return null

    val newLower = when {
      kleiner.lower is INFINITE -> INFINITE
      groesser.lower is INFINITE -> INFINITE
      kleiner.lower.value < groesser.lower.value -> kleiner.lower
      groesser.lower.value < kleiner.lower.value -> groesser.lower
      kleiner.lower is VAL_INKL -> kleiner.lower
      groesser.lower is VAL_INKL -> groesser.lower
      else -> kleiner.lower
    }
    val newUpper = when {
      kleiner.upper is INFINITE -> INFINITE
      groesser.upper is INFINITE -> INFINITE
      kleiner.upper.value > groesser.upper.value -> kleiner.upper
      groesser.upper.value > kleiner.upper.value -> groesser.upper
      kleiner.upper is VAL_INKL -> kleiner.upper
      groesser.upper is VAL_INKL -> groesser.upper
      else -> kleiner.upper
    }

    return Grenzwert(newLower, newUpper)
  }

  override fun toString() = "" +
      when (lower) {
        is INFINITE -> "[I"
        is VAL_INKL -> "[" + lower.value
        is VAL_EXKL -> "]" + lower.value
        else -> "?"
      } + ";" +
      when (upper) {
        is INFINITE -> "I]"
        is VAL_INKL -> "" + upper.value + "]"
        is VAL_EXKL -> "" + upper.value + "["
        else -> "?"
      }

  override fun equals(other: Any?) = (other is Grenzwert) && other.lower == lower && other.upper == upper
}

sealed class Grenze {
  abstract val value: Double
}

object INFINITE : Grenze() {
  override val value: Double get() = throw IllegalStateException("FALSCH")
}

class VAL_INKL(override val value: Double) : Grenze() {
  override fun equals(other: Any?) = (other as? VAL_INKL)?.value == value
}

class VAL_EXKL(override val value: Double) : Grenze() {
  override fun equals(other: Any?) = (other as? VAL_EXKL)?.value == value
}
