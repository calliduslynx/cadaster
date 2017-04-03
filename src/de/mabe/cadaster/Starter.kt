package de.mabe.cadaster

import de.mabe.cadaster.ValueType.exactly
import de.mabe.cadaster.ValueType.maybe

fun main(args: Array<String>) {
}

val p1 = Project().apply {
  elements().apply {
    1 isA Point(maybe..3, exactly..3.4)
    2 isA Point(3, 3.4)
    3 isA Point(3.4, 3)
    4 isA Point(3, 3)
    5 isA Point(3.0, 3.0)
  }

  rules().apply {
    1 isA PointDistanceRule(1, 2, 2.0)
    2 isA PointDistanceRule(1, 4, 2.0)
  }
}


val p2 = Project().apply {
  elements().apply {
    1 isA Point(1, 1)
    2 isA Point(maybe..3, maybe..1)
  }

  rules().apply {
    1 isA PointDistanceRule(1, 2, 1.5)
  }
}
