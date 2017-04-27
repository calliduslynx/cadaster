package de.mabe.cadaster

import de.mabe.cadaster.PValueType.exactly
import de.mabe.cadaster.expression.*
import de.mabe.cadaster.expression.Gleichheit.IST_GLEICH


class Project {
  private val elementList = ElementList()
  private val ruleList = RuleList()

  fun elements() = elementList
  fun rules() = ruleList

  override fun toString() = "Elements:\n$elementList\nRules:\n$ruleList"
}


enum class PValueType {
  maybe, exactly, unknown;

  operator fun rangeTo(i: Int) = PValue(i.toDouble(), this)
  operator fun rangeTo(d: Double) = PValue(d, this)

  override fun toString() = when (this) {
    maybe -> "~"
    unknown -> "?"
    exactly -> ""
  }
}

class PValue(val value: Double, val type: PValueType) {
  override fun toString() = "$type$value"
}

class PVariable(val name: String, val pValue: PValue) {
  override fun toString() = "$name = $pValue"
}


// *******************************************************

sealed class Element {
  abstract fun getAllVariables(): List<Pair<String, PValue>>
}

class Point(val x: PValue, val y: PValue) : Element() {
  constructor(x: Double, y: Double) : this(exactly..x, exactly..y)
  constructor(x: Int, y: Double) : this(exactly..x, exactly..y)
  constructor(x: Double, y: Int) : this(exactly..x, exactly..y)
  constructor(x: Int, y: Int) : this(exactly..x, exactly..y)

  override fun toString() = "Point($x, $y)"

  override fun getAllVariables() = listOf(Pair("x", x), Pair("y", y))
}

class NumberedElement(val id: String, val element: Element) {
  override fun toString() = "$id -> $element"
}

class ElementList {
  private val list = mutableListOf<NumberedElement>()

  infix fun String.isA(element: Element) = list.add(NumberedElement(this, element))

  fun all() = listOf(*list.toTypedArray())
  fun allVariables(): List<PVariable> {
    val vars = mutableListOf<PVariable>()
    list.forEach { numElement ->
      numElement.element.getAllVariables().forEach { pVar ->
        vars.add(PVariable("${numElement.id}.${pVar.first}", pVar.second))
      }
    }
    return vars
  }
}

// *******************************************************

sealed class Rule {
  abstract fun gleichung(): Gleichung
}

data class PointDistanceRule(
    val idPoint1: String,
    val idPoint2: String,
    val distance: Double
) : Rule() {
  override fun gleichung() = Gleichung(
      Quadrat(Val(distance)),
      IST_GLEICH,
      Quadrat(Var("$idPoint2.x") - Var("$idPoint1.x")) + Quadrat(Var("$idPoint2.y") - Var("$idPoint1.y"))
  )

  override fun toString() = "PointDistanceRule: distance between $idPoint1 and $idPoint2 must be $distance"
}

class NumberedRule(val id: Int, val rule: Rule) {
  override fun toString() = "$id -> $rule"
}

class RuleList {
  private val list = mutableListOf<NumberedRule>()

  infix fun Int.isA(rule: Rule) = list.add(NumberedRule(this, rule))

  fun all() = listOf(*list.toTypedArray())
}
