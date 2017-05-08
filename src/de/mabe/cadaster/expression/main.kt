package de.mabe.cadaster.expression

import de.mabe.cadaster.expression.Gleichheit.IST_GLEICH
import de.mabe.cadaster.util.indentBy


// ***********************************************************************************************
// ***********************************************************************************************
// ***********************************************************************************************
fun main(args: Array<String>) {

  println("---------------------------------------------------------------------------------------------------------------")
  println("--------- GLEICHUNGSYSTEME ------------------------------------------------------------------------------------")
  gleichungssysteme.forEach {
    println("---------------------------------------------------------------------------------------------------------------")
    it.solve()
//    println("              simple: " + it.simplify())
//    println("      variable-count: " + it.variables())
//    println("         solve for x: " + it.loese_auf_nach("x"))
//    println("               graph: \n" + it.toGraph().indentBy(20))
//    println("               graph: \n" + it.simplify().toGraph().indentBy(20))
  }


  if (Math.random() < 2) return
  println("---------------------------------------------------------------------------------------------------------------")
  println("------------ GLEICHUNGEN --------------------------------------------------------------------------------------")
  gleichungen.forEach {
    println("---------------------------------------------------------------------------------------------------------------")
    println(" $it")
    println()
    println("              simple: " + it.simplify())
    println("           variables: " + it.variables)
    println("         solve for x: " + it.loese_auf_nach("x"))
    println("               graph: \n" + it.toGraph().indentBy(20))
    println("               graph: \n" + it.simplify().toGraph().indentBy(20))
  }

  println()
  println("---------------------------------------------------------------------------------------------------------------")
  println("------------ EXPRESSIONS --------------------------------------------------------------------------------------")
  list.forEach {
    println("---------------------------------------------------------------------------------------------------------------")
    println(" 0 = $it")
    println()
    println("              simple: " + it.simplify())
    println("      variable-count: " + it.variables)
    println("         solve for x: " + G(it, "=", 0).loese_auf_nach("x"))
    println("               graph: \n" + it.toGraph().indentBy(20))
    println("               graph: \n" + it.simplify().toGraph().indentBy(20))
  }
}

val x = Var("x")
val y = Var("y")
val z = Var("z")
val x2 = Var("x2")
val x1 = Var("x1")
val y2 = Var("y2")
val y1 = Var("y1")


val gleichungssysteme = listOf(
    Gleichungssystem(
        G(4, IST_GLEICH, x),
        G(12, IST_GLEICH, x + y)
    ),
    //    Gleichungssystem( FIXME: LOOP
//        EineGleichung(Val(4), IST_GLEICH, x),
//        EineGleichung(Val(12), IST_GLEICH, x + y),
//        EineGleichung(Val(24), IST_GLEICH, x + y + z)
//    ),
    Gleichungssystem(
        G(4, "=", x + y),
        G(12, "=", x - y)
    ),
    Gleichungssystem(
        G(4, "=", Val(1) / (x + y))
    ),
    Gleichungssystem(
        G(4, "=", Wurzel(x * y))
    ),
    Gleichungssystem(
        G(x, "=", x + 14 + y),
        G(y, "=", x * 3)
    )
    ,
    Gleichungssystem(// LOOP
        G(4, "=", Val(1) / (x + y)),
        G(4, "=", Val(1) / (z + y + x))
    )
)

val list = listOf(
    (Val(1.0) + Val(2.0)) - Val(1.5),
    (Val(1.0) + Val(2.0)) - x,
    Wurzel(Quadrat(x2 - x1) + Quadrat(y2 - y1)),
    (x * 13) - (Val(12) / 4),
    x - (Val(12) / 4),
    x + x,
    -x,
    Neg(Neg(x)),
    Neg(Neg(x)) + 1,
    Kehrwert(Kehrwert(x)) + 1,
    Quadrat(Wurzel(x)) + 1,
    Wurzel(Quadrat(x)) + 1,
    (Val(3) * x) + (Val(3) * x),
    x + x,
    x * (Val(3) + 5),
    x * ((x + x - 3) * 0),
    (x * y) - (x * y),
    (x * 2 + y) / (x * 2 + y),
    x + 2 + x + 4 + x,
    x + x + x + x + x + x + x + x + x + x - 10,
    x + x + x + x + x + x + x + x + x + x + 10,
    x * x,
    (x * 2) + x,
    (x * 3) + (x * 4),
    (x * 2) + -x,
    (x * 3) - (x * 4),
    (x * -3) + (x * 4),
    (x + y) - ((x + y) + (x + y))
)

val DEBUG = false

fun debug(string: Any) {
  if (!DEBUG) return
  string.toString().lines().forEach { println("DEBUG  $it") }
}

val gleichungen = listOf(
    G(x + 12, "=", x * 13),
    G((Val(12) + 23) * 12, "=", (x * 13) - (Val(12) / 4)),
    G((y + 23) * 12, "=", (x * 13) - (Val(12) / 4)),
    G(x + 4, "=", x * 0)
)


