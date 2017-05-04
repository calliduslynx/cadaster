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

  //@formatter:off
  @Test fun simp_001()   = Val(14) - 2                          isSimple  Val(12)  
  @Test fun simp_002()   = x - (Val(1) - (Val(1) - x))          isSimple  Val(0)
  @Test fun simp_003()   = x + x                                isSimple  x * 2
  @Test fun simp_004()   = (x * 3) + (x * 4)                    isSimple  x * 7
  @Test fun simp_005()   = (x * 2) + -x                         isSimple  x
  @Test fun simp_006()   = x + x + x + x + x + x + x + x  + 10  isSimple  x * 8 + 10
  @Test fun simp_007()   = Neg(Neg(x))                          isSimple  x
  @Test fun simp_008()   = Neg(Neg(x)) + 1                      isSimple  x + 1
  @Test fun simp_009()   = Kehrwert(Kehrwert(x)) + 1            isSimple  x + 1
  @Test fun simp_010()   = Quadrat(Wurzel(x)) + 1               isSimple  x + 1
  @Test fun simp_011()   = Wurzel(Quadrat(x)) + 1               isSimple  x + 1
  @Test fun simp_012()   = (Val(3) * x) + (Val(3) * x)          isSimple  x * 6
  @Test fun simp_013()   = (x * -3) + (x * 4)                   isSimple  x
  @Test fun simp_014()   = -(x + y)                             isSimple  -x + -y
  @Test fun simp_015()   = -(x * y)                             isSimple  -x * y
  @Test fun simp_016()   = Val(16) / y * y                      isSimple  Val(16)
  @Test fun simp_017()   = Wurzel(Val(16) / y * y)              isSimple  Val(4)
  @Test fun simp_018()   = (y - 1) * 0.5                        isSimple  y * 0.5 + Val(-0.5) 
  @Test fun simp_019()   = (x + 1) * (x + 1)                    isSimple  Quadrat(x + 1)
  @Test fun simp_020()   = x * 2 * y * 3 * x * 4                isSimple  Val(24) * Quadrat(x) * y
  @Test fun simp_021_f() = (Quadrat(4) / y) - (Quadrat(4) / x)  isSimple  x
  @Test fun simp_022_f() = (Quadrat(4) / y) - (Quadrat(4) / y)  isSimple  x
  @Test fun simp_023()   = Quadrat(4) / y                       isSimple  Val(16) / y
  @Test fun simp_024_f() = (Val(16) / x) - (Val(16) / x)        isSimple  Val(0)
  @Test fun simp_024()   = (x + y) - ((x + y) + (x + y))        isSimple  -x - y
  
  @Test fun exp_withVal_001() = withExpVal(Val(10), Val(2), x + 8)

  @Test fun umst_001()   = G(x, "=", 12)                                          isSolvedByX  G(x, "=" , 12)
  @Test fun umst_002()   = G(x, "=", Val(12) - 3)                                 isSolvedByX  G(x, "=" ,  9)
  @Test fun umst_003()   = G(0, "=", x + x + x + x + x + x + x + x + x + x - 10)  isSolvedByX  G(x, "=",   1)
  @Test fun umst_004()   = G(0, "=", x + x + x + x + x + x + x + x + x + x + 10)  isSolvedByX  G(x, "=" , -1)
  @Test fun umst_005()   = G(0, "=", x + x + x + x + x + x + x + x + x + x + 10)  isSolvedByX  G(x, "=" , -1)
  @Test fun umst_006()   = G(0, "<", x - 10)                                      isSolvedByX  G(x, ">" , 10)
  @Test fun umst_007()   = G(0, "<", Val(10) - x)                                 isSolvedByX  G(x, "<" , 10)
  @Test fun umst_008()   = G(0, "<", -x)                                          isSolvedByX  G(x, "<" ,  0)
  @Test fun umst_009()   = G(0, "<=", -x)                                         isSolvedByX  G(x, "<=",  0)
  @Test fun umst_010()   = G(0, ">", -x)                                          isSolvedByX  G(x, ">" ,  0)
  @Test fun umst_011()   = G(0, ">=", -x)                                         isSolvedByX  G(x, ">=",  0)
  @Test fun umst_012()   = G(-x, ">", Val(0))                                     isSolvedByX  G(x, "<" ,  0)
  @Test fun umst_013()   = G(-x, ">=", Val(0))                                    isSolvedByX  G(x, "<=",  0)
  @Test fun umst_014()   = G(-x, "<", Val(0))                                     isSolvedByX  G(x, ">" ,  0)
  @Test fun umst_015()   = G(-x, "<=", -Val(0))                                   isSolvedByX  G(x, ">=",  0)
  @Test fun umst_016()   = G(x, "=", x + 14 + y)                                  isSolvedByX  x_nicht_relevant
  @Test fun umst_017()   = G(Val(16) / x, "=", Val(16) / x)                       isSolvedByX  x_nicht_relevant
  @Test fun umst_018()   = G(Quadrat(x), "=", 4)                                  isSolvedByX  listOf(G(x, "=", 2), G(x, "=", -2))
  @Test fun umst_019_f() = G(Quadrat(Quadrat(x)), "=", 16)                        isSolvedByX  listOf(G(x, "=", 2), G(x, "=", -2))
  @Test fun umst_020()   = G(Quadrat(x - 2), "=", y * 3)                          isSolvedByX  listOf(G(x, "=", Wurzel(y*3) + 2), G(x, "=", -Wurzel(y*3) + 2))

  @Test fun erg_001() = G(x, "=", 3)      hatErgebnis  "3"
  @Test fun erg_002() = G(x, "=", 23.3)   hatErgebnis  "23.3"
  
  @Test fun erg_simp_001() = "[0,1];[1,2]"            hatSimplesErgebnis  "[0,2]"
  @Test fun erg_simp_002() = "[1,2];[0,1]"            hatSimplesErgebnis  "[0,2]"
  @Test fun erg_simp_003()= "[0,3];[1,2]"            hatSimplesErgebnis  "[0,3]"
  @Test fun erg_simp_004() = "[0,1[;[1,2]"            hatSimplesErgebnis  "[0,2]"
  @Test fun erg_simp_005() = "[0,1];]1,2]"            hatSimplesErgebnis  "[0,2]"
  @Test fun erg_simp_006() = "[0,1[;]1,2]"            hatSimplesErgebnis  "[0,1[;]1,2]"
  @Test fun erg_simp_007() = "[0,1[;]1,2];1"          hatSimplesErgebnis  "[0,2]"
  @Test fun erg_simp_008() = "1;0;1"                  hatSimplesErgebnis  "0;1"
  @Test fun erg_simp_009() = "[0,2];1;2;3"            hatSimplesErgebnis  "[0,2];3"
  @Test fun erg_simp_010() = "[0,2[;2"                hatSimplesErgebnis  "[0,2]"
  @Test fun erg_simp_011() = "[0,2[;]2,3];2"          hatSimplesErgebnis  "[0,3]"
  @Test fun erg_simp_012() = "[0,1.5];[2.5,4];[1,3]"  hatSimplesErgebnis  "[0,4]"
  @Test fun erg_simp_013() = "[I,1];[0,I]"            hatSimplesErgebnis  "[I,I]"
  @Test fun erg_simp_014() = "[I,1];[0,2]"            hatSimplesErgebnis  "[I,2]"
  @Test fun erg_simp_015() = "[0,2];[I,1]"            hatSimplesErgebnis  "[I,2]"

  @Test fun gw_001() = grenzwert("]-I,5[" , listOf(-1000.0, -1.0, 4.99999)    , listOf(5.0, 100.0)                                 )
  @Test fun gw_002() = grenzwert("[-12,I[", listOf(-12.0, -10.0, 1000.0)      , listOf(-12.00001, -10000.0)                        )
  @Test fun gw_003() = grenzwert("[0,1]"  , listOf(0.0, 0.00001, 0.99999, 1.0), listOf(-0.00001, 1.000001, 100.0, -100.0)          )
  @Test fun gw_004() = grenzwert("]0,1]"  , listOf(0.00001, 0.99999, 1.0)     , listOf(0.0, -0.00001, 1.000001, 100.0, -100.0)     )
  @Test fun gw_005() = grenzwert("]0,1["  , listOf(0.00001, 0.99999)          , listOf(0.0, -0.00001, 1.000001, 100.0, -100.0, 1.0))
  //@formatter:on

  @Test fun gleichheit() = Gleichheit.values().forEach { println(" - ${it.name} ${it.look} flip:${it.flip.name}") }

  val x1 = Var("x1")
  val x2 = Var("x2")
  val y1 = Var("y1")
  val y2 = Var("y2")


  @Test fun test() {
    val g = G(Quadrat(2), "=", Quadrat(Val(10) - x1) + Quadrat((y2 - y1)))
    println(g.loese_auf_nach(x1))
    println(g.loese_auf_nach(y1))
    println(g.loese_auf_nach(y2))
  }

  // ******************************************************************************************************************
  private fun grenzwert(wbString: String, inRange: List<Double>, outRange: List<Double>) {
    val wb = Grenzwert(wbString)

    inRange.forEach { value -> assertTrue("$value should be in Range of $wb", wb.inRange(value)) }
  }

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


  private val x_nicht_relevant = G(x, "=", x)

  private infix fun Gleichung.isSolvedByX(expected: Gleichung) = this.isSolvedByX(listOf(expected))

  private infix fun Gleichung.isSolvedByX(expected: List<Gleichung>) {
    val umstellungsErgebnis = this.loese_auf_nach("x")
    if (expected[0] === x_nicht_relevant) {
      println("GLEICHUNG '$this' soll nach x ergeben, dass x nicht relevant ist")
      assertTrue("Ergebnis: $umstellungsErgebnis", umstellungsErgebnis is VariableNichtRelevant)
    } else {
      println("GLEICHUNG '$this' soll nach x umgestellt sein: '$expected'")
      assertTrue("Ergebnis: $umstellungsErgebnis", umstellungsErgebnis is ErfolgreicheUmstellung)
      umstellungsErgebnis as ErfolgreicheUmstellung

      val expectedGleichung = when (expected.size) {
        1 -> expected[0]
        else -> G(expected)
      }
      assertEquals(expectedGleichung, umstellungsErgebnis.gleichung)
    }
  }

  private infix fun Expression.isSimple(expected: Expression) {
    println("EXPRESSION '$this' soll simple aussehen: '$expected'")

    if (DEBUG) println("--- original")
    if (DEBUG) println(this.toGraph())
    val simple = this.simplify()
    if (DEBUG) println("--- simple")
    if (DEBUG) println(simple.toGraph())

    assertEquals(expected, simple)
  }

  private infix fun Gleichung.hatErgebnis(ergebnisExpectedString: String) {
    println("Gleichung '$this' soll als Ergebnis haben: " + Ergebnis("x", ergebnisExpectedString))
    assertEquals(Ergebnis("x", ergebnisExpectedString), this.getErgebnis())
  }

  private infix fun String.hatSimplesErgebnis(ergebnisExpectedString: String) {
    println("Ergebnis '$this' soll zusammengefasst sein: $ergebnisExpectedString")
    assertEquals(Ergebnis("x", ergebnisExpectedString), Ergebnis("x", this))
  }
}
