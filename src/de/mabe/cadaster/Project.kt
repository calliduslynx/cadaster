package de.mabe.cadaster

import de.mabe.cadaster.ValueType.exactly
import de.mabe.cadaster.expression.Expression
import de.mabe.cadaster.expression.VariableExpression


class Project {
  private val elementList = ElementList()
  private val ruleList = RuleList()

  fun elements() = elementList
  fun rules() = ruleList

  override fun toString() = "Elements:\n$elementList\nRules:\n$ruleList"
}

enum class ValueType {
  maybe, exactly;

  operator fun rangeTo(i: Int) = VariableExpression("Y")
  operator fun rangeTo(d: Double) = VariableExpression("X")
}


// *******************************************************

open class Rule

data class PointDistanceRule(
    val idPoint1: Int,
    val idPoint2: Int,
    val distance: Double
) : Rule()

class RuleList {
  private class NumberedRule(val id: Int, val rule: Rule)

  private val list = mutableListOf<NumberedRule>()

  infix fun Int.isA(rule: Rule) = list.add(NumberedRule(this, rule))

  override fun toString() = list.sortedBy { it.id }.map { " - #${it.id} ==> ${it.rule}" }.joinToString("\n")
}

// *******************************************************

open class Element

class Point(val x: Expression, val y: Expression) : Element() {
  constructor(x: Double, y: Double) : this(exactly..x, exactly..y)
  constructor(x: Int, y: Double) : this(exactly..x, exactly..y)
  constructor(x: Double, y: Int) : this(exactly..x, exactly..y)
  constructor(x: Int, y: Int) : this(exactly..x, exactly..y)

  override fun toString() = "Point($x, $y)"

}


class ElementList {
  private class NumberedElement(val id: Int, val element: Element)

  private val list = mutableListOf<NumberedElement>()

  infix fun Int.isA(element: Element) = list.add(NumberedElement(this, element))
  override fun toString() = list.sortedBy { it.id }.map { " - #${it.id} ==> ${it.element}" }.joinToString("\n")
}


