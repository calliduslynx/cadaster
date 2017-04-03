package de.mabe.cadaster.expression

// ***********************************************************************************************
// ***********************************************************************************************
// ***********************************************************************************************
fun main(args: Array<String>) {
  val list = listOf(
      MinusExpression(
          PlusExpression(
              ValueExpression(1.0),
              ValueExpression(2.0)
          ),
          ValueExpression(1.5)
      ),
      MinusExpression(
          PlusExpression(
              ValueExpression(1.0),
              ValueExpression(2.0)
          ),
          VariableExpression("x")
      ),
      MinusExpression(
          PlusExpression(
              VariableExpression("x"),
              VariableExpression("y")
          ),
          PlusExpression(
              PlusExpression(
                  VariableExpression("x"),
                  VariableExpression("z")
              ),
              PlusExpression(
                  VariableExpression("x"),
                  VariableExpression("y")
              )
          )
      ),
      WurzelExpression(
          PlusExpression(
              QuadratExpression(
                  MinusExpression(
                      VariableExpression("X2"),
                      VariableExpression("X1")
                  )
              ),
              QuadratExpression(
                  MinusExpression(
                      VariableExpression("Y2"),
                      VariableExpression("Y1")
                  )
              )
          )
      ),
      Wurzel(
          Hoch2(Var("X2") - Var("X1")) +
              Hoch2(Var("Y2") - Var("Y1"))
      )
  )

  list.forEach {
    println("-------------------------------------")
    println("-------------------------------------")
    println("-------------------------------------")
    println(it)
    println()
    println(it.toGraph())
    println()
    println("contains 'x': ${it.containsVariable("x")}")
    println("value: ${it.getValue()}")
    println("simplifing ...")
    val simple = it.simplify()
    println(simple.toGraph())
    println(it.toGraph())
  }

  // TODO build query
  // TODO make sliding from one side to another
  // TODO vielleicht String-Reader

}
