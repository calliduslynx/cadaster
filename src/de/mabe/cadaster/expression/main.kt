package de.mabe.cadaster.expression

import de.mabe.cadaster.expression.Gleichheit.IST_GLEICH
import de.mabe.cadaster.util.indentBy


// ***********************************************************************************************
// ***********************************************************************************************
// ***********************************************************************************************
fun main(args: Array<String>) {

  println("-------------------------------------")
  println("--------- GLEICHUNGSYSTEME ----------")
  gleichungssysteme.forEach {
    println("-------------------------------------")
    it.solve()
//    println("              simple: " + it.simplify())
//    println("      variable-count: " + it.variableCount())
//    println("         solve for x: " + it.loese_auf_nach("x"))
//    println("               graph: \n" + it.toGraph().indentBy(20))
//    println("               graph: \n" + it.simplify().toGraph().indentBy(20))
  }


  if (Math.random() < 2) return
  println("-------------------------------------")
  println("------------ GLEICHUNGEN ------------")
  gleichungen.forEach {
    println("-------------------------------------")
    println(" $it")
    println()
    println("              simple: " + it.simplify())
    println("      variable-count: " + it.variableCount())
    println("         solve for x: " + it.loese_auf_nach("x"))
    println("               graph: \n" + it.toGraph().indentBy(20))
    println("               graph: \n" + it.simplify().toGraph().indentBy(20))
  }

  println()
  println("-------------------------------------")
  println("------------ EXPRESSIONS ------------")
  list.forEach {
    println("-------------------------------------")
    println(" 0 = $it")
    println()
    println("              simple: " + it.simplify())
    println("      variable-count: " + it.variableCount())
    println("         solve for x: " + Gleichung(it, IST_GLEICH, Val(0)).loese_auf_nach("x"))
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
        Gleichung(Val(4), IST_GLEICH, x),
        Gleichung(Val(12), IST_GLEICH, x + y)
    ),
    //    Gleichungssystem( FIXME: LOOP
//        Gleichung(Val(4), IST_GLEICH, x),
//        Gleichung(Val(12), IST_GLEICH, x + y),
//        Gleichung(Val(24), IST_GLEICH, x + y + z)
//    ),
    Gleichungssystem(
        Gleichung(Val(4), IST_GLEICH, x + y),
        Gleichung(Val(12), IST_GLEICH, x - y)
    ),
    Gleichungssystem(
        Gleichung(Val(4), IST_GLEICH, Val(1) / (x + y))
    ),
    Gleichungssystem(
        Gleichung(Val(4), IST_GLEICH, Wurzel(x * y))
    ),
    Gleichungssystem(
        Gleichung(x, IST_GLEICH, x + 14 + y),
        Gleichung(y, IST_GLEICH, x * 3)
    )
    ,
    Gleichungssystem(// LOOP
        Gleichung(Val(4), IST_GLEICH, Val(1) / (x + y)),
        Gleichung(Val(4), IST_GLEICH, Val(1) / (z + y + x))
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
    Gleichung(x + 12, IST_GLEICH, x * 13)
    ,
    Gleichung(
        (Val(12) + 23) * 12,
        IST_GLEICH,
        (x * 13) - (Val(12) / 4)
    ),
    Gleichung(
        (y + 23) * 12,
        IST_GLEICH,
        (x * 13) - (Val(12) / 4)
    ),
    Gleichung(
        x + 4,
        IST_GLEICH,
        x * 0
    )
)


