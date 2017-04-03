package de.mabe.cadaster.expression

// ***********************************************************************************************
// ***********************************************************************************************
// ***********************************************************************************************
sealed class Result

class MissingVariableResult : Result {
  val missingVariables: Set<String>

  constructor(name: String) {
    missingVariables = setOf(name)
  }

  constructor(mvr1: MissingVariableResult, mvr2: MissingVariableResult) {
    missingVariables = mvr1.missingVariables + mvr2.missingVariables
  }

  override fun toString() = "Missing Variables: $missingVariables"
}

class ConcreteResult(val value: Double) : Result() {
  override fun toString() = "Res: [$value]"
  fun asExpression() = ValueExpression(value)
}
