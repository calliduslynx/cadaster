package de.mabe.cadaster

import de.mabe.cadaster.PValueType.exactly
import de.mabe.cadaster.PValueType.maybe

fun main(args: Array<String>) {
  Cadaster().solve(p2)
}

val p1 = Project().apply {
  elements().apply {
    "P1" isA Point(maybe..3, exactly..3.4)
    "P2" isA Point(3, 3.4)
    "P3" isA Point(3.4, 3)
    "P4" isA Point(3, 3)
    "P5" isA Point(3.0, 3.0)
  }

  rules().apply {
    1 isA PointDistanceRule("P1", "P2", 2.0)
    2 isA PointDistanceRule("P1", "P4", 2.0)
  }
}

val p2 = Project().apply {
  elements().apply {
    "P1" isA Point(0, 0)
    "P2" isA Point(maybe..1, maybe..1)
  }

  rules().apply {
    1 isA PointDistanceRule("P2", "P3", 2.0)
  }
}

val p3 = Project().apply {
  elements().apply {
    "P1" isA Point(0, 0)
    "P2" isA Point(maybe..1, maybe..1)
    "P3" isA Point(maybe..4, maybe..1)
  }

  rules().apply {
    1 isA PointDistanceRule("P2", "P3", 2.0)
    2 isA PointDistanceRule("P1", "P2", 2.0)
  }
}
