package de.mabe.cadaster.expression

import org.junit.Assert.*
import org.junit.Test

private val t = true
private val f = false

class Tests {
  // ***** Equality
  @Test fun exp_eq_001() = exp_eq(t, Val(5), Val(5))

  @Test fun exp_eq_002() = exp_eq(t, x, x)
  @Test fun exp_eq_003() = exp_eq(t, y, y)
  @Test fun exp_eq_004() = exp_eq(t, x + y, y + x)
  @Test fun exp_eq_005() = exp_eq(t, x + 1, Val(1) + x)
  @Test fun exp_eq_006() = exp_eq(t, Val(1), Val(1.0))
  @Test fun exp_eq_007() = exp_eq(f, Val(1), Val(1.1))
  @Test fun exp_eq_008() = exp_eq(f, x + 1, x + 2)
  @Test fun exp_eq_009() = exp_eq(f, x, y)
  @Test fun exp_eq_010() = exp_eq(f, -Val(10), Val(-10))
  @Test fun exp_eq_011() = exp_eq(t, Val(14), Val(13.999999999999998))
  @Test fun exp_eq_012() = exp_eq(t, Val(13), Val(13.0000000000001))

  @Test fun gl_eq_001() = gl_eq(t, G(x, "=", x), G(x, "=", x))
  @Test fun gl_eq_002() = gl_eq(t, G(x, "=", y), G(x, "=", y))
  @Test fun gl_eq_003() = gl_eq(t, G(x, "=", y), G(y, "=", x))
  @Test fun gl_eq_004() = gl_eq(f, G(x, "=", y), G(x, "=", x))

  @Test fun simp_001() = simp(Val(12), Val(14) - 2)
  @Test fun simp_002() = simp(Val(0), x - (Val(1) - (Val(1) - x)))
  @Test fun simp_003() = simp(x * 2, x + x)
  @Test fun simp_004() = simp(x * 7, (x * 3) + (x * 4))
  @Test fun simp_005() = simp(x, (x * 2) + -x)
  @Test fun simp_006() = simp(x * 10 + 10, x + x + x + x + x + x + x + x + x + x + 10)
  @Test fun simp_007() = simp(x, Neg(Neg(x)))
  @Test fun simp_008() = simp(x + 1, Neg(Neg(x)) + 1)
  @Test fun simp_009() = simp(x + 1, Kehrwert(Kehrwert(x)) + 1)
  @Test fun simp_010() = simp(x + 1, Quadrat(Wurzel(x)) + 1)
  @Test fun simp_011() = simp(x + 1, Wurzel(Quadrat(x)) + 1)
  @Test fun simp_012() = simp(x * 6, (Val(3) * x) + (Val(3) * x))
  @Test fun simp_013() = simp(x, (x * -3) + (x * 4))
  @Test fun simp_014() = simp(-x + -y, -(x + y))
  @Test fun simp_015() = simp(-x * y, -(x * y))
  @Test fun simp_016_f() = simp(Val(16), Val(16) / y * y)
  @Test fun simp_017_f() = simp(Val(4), Wurzel(Val(16) / y * y))
  @Test fun simp_018() = simp(y * 0.5 + Val(-0.5), (y - 1) * 0.5)
  @Test fun simp_019() = simp(Quadrat(x + 1), (x + 1) * (x + 1))

  @Test fun exp_withVal_001() = withExpVal(Val(10), Val(2), x + 8)

  @Test fun umst_001() = umst(Val(12), G(x, "=", Val(12)))
  @Test fun umst_002() = umst(Val(9), G(x, "=", Val(12) - 3))
  @Test fun umst_003() = umst(Val(1), G(Val(0), "=", x + x + x + x + x + x + x + x + x + x - 10))
  @Test fun umst_004() = umst(Val(-1), G(Val(0), "=", x + x + x + x + x + x + x + x + x + x + 10))
  @Test fun umst_005() = umst(G(x, "=", Val(-1)), G(Val(0), "=", x + x + x + x + x + x + x + x + x + x + 10))
  @Test fun umst_006() = umst(G(x, ">", Val(10)), G(Val(0), "<", x - 10))
  @Test fun umst_007() = umst(G(x, "<", Val(10)), G(Val(0), "<", Val(10) - x))
  @Test fun umst_008() = umst(G(x, "<", Val(0)), G(Val(0), "<", -x))
  @Test fun umst_009() = umst(G(x, "<=", Val(0)), G(Val(0), "<=", -x))
  @Test fun umst_010() = umst(G(x, ">", Val(0)), G(Val(0), ">", -x))
  @Test fun umst_011() = umst(G(x, ">=", Val(0)), G(Val(0), ">=", -x))
  @Test fun umst_012() = umst(G(x, "<", Val(0)), G(-x, ">", Val(0)))
  @Test fun umst_013() = umst(G(x, "<=", Val(0)), G(-x, ">=", Val(0)))
  @Test fun umst_014() = umst(G(x, ">", Val(0)), G(-x, "<", Val(0)))
  @Test fun umst_015() = umst(G(x, ">=", Val(0)), G(-x, "<=", -Val(0)))
  @Test fun umst_016() = umstNR(G(x, "=", x + 14 + y))

  @Test fun gleichheit() = Gleichheit.values().forEach { println(" - ${it.name} ${it.look} flip:${it.flip.name}") }

  @Test fun wertebereich1() {
    val exp = Wurzel(x + y)
    val wb = exp.wertebereiche()[0]
    assertEquals(G(Val(0), "<=", x + y), wb)

    assertEquals(G(x, ">=", -y), (wb.loese_auf_nach("x") as ErfolgreicheUmstellung).gleichung)
    assertEquals(G(y, ">=", -x), (wb.loese_auf_nach("y") as ErfolgreicheUmstellung).gleichung)
  }

  @Test fun wertebereich2() {
    val exp = Wurzel(x * y)
    val wb = exp.wertebereiche()[0]
    assertEquals(G(Val(0), "<=", x * y), wb)

    // TODO das hier unten ist nicht ganz korrekt ... 
    // entweder x >= 0 && y >= 0     ||    x <= 0 && y <= 0    
    assertEquals(G(x, ">=", Val(0)), (wb.loese_auf_nach("x") as ErfolgreicheUmstellung).gleichung)
    assertEquals(G(y, ">=", Val(0)), (wb.loese_auf_nach("y") as ErfolgreicheUmstellung).gleichung)
  }

  @Test fun wertebereich3() {
    val exp = Wurzel(x + y + z)
    val wb = exp.wertebereiche()[0]
    assertEquals(G(Val(0), "<=", x + y + z), wb)
  }
  // ******************************************************************************************************************

  private fun withExpVal(expected: Expression, subVal: Expression, exp: Expression) {
    println("EXPRESSION '$exp' mit Wert x = '$subVal' soll vereinfacht = '$expected' sein")
    assertEquals(expected, exp.withValue("x", subVal).simplify())
  }

  private fun exp_eq(expected: Boolean, exp1: Expression, exp2: Expression) {
    println("EXPRESSION soll " + (if (expected) "GLEICH" else "UNGLEICH") + " sein: '$exp1' ... '$exp2'")
    if (expected)
      assertEquals(exp1, exp2)
    else
      assertNotEquals(exp1, exp2)
  }

  private fun gl_eq(expected: Boolean, gl1: Gleichung, gl2: Gleichung) {
    println("GLEICHUNG soll " + (if (expected) "GLEICH" else "UNGLEICH") + " sein: '$gl1' ... '$gl2'")
    if (expected)
      assertEquals(gl1, gl2)
    else
      assertNotEquals(gl1, gl2)
  }

  private fun umst(expected: Expression, gleichung: Gleichung) {
    umst(G(x, "=", expected), gleichung)
  }

  private fun umst(expected: Gleichung, gleichung: Gleichung) {
    println("GLEICHUNG '$gleichung' soll nach x umgestellt sein: '$expected'")
    val umstellungsErgebnis = gleichung.loese_auf_nach("x")
    assertTrue("Ergebnis: $umstellungsErgebnis", umstellungsErgebnis is ErfolgreicheUmstellung)
    umstellungsErgebnis as ErfolgreicheUmstellung
    assertEquals(expected.left, umstellungsErgebnis.gleichung.left)
    assertEquals(expected.right, umstellungsErgebnis.gleichung.right)
    assertEquals(expected.gleichheit, umstellungsErgebnis.gleichung.gleichheit)
  }

  private fun umstNR(gleichung: Gleichung) {
    println("GLEICHUNG '$gleichung' soll nach x ergeben, dass x nicht relevant ist")
    val umstellungsErgebnis = gleichung.loese_auf_nach("x")
    assertTrue("Ergebnis: $umstellungsErgebnis", umstellungsErgebnis is VariableNichtRelevant)
  }

  private fun simp(expSimple: Expression, exp: Expression) {
    println("EXPRESSION '$exp' soll simple aussehen: '$expSimple'")

    if (DEBUG) println("--- original")
    if (DEBUG) println(exp.toGraph())
    val simple = exp.simplify()
    if (DEBUG) println("--- simple")
    if (DEBUG) println(simple.toGraph())

    assertEquals(expSimple, simple)
  }
}
