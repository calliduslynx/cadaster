 Cadaster
========== 

 Beschreibung
--------------

Man zeichnet Grundrisse von Grundstücken, Gebäuden oder Räumen
grob auf. Danach beginnt man die bekannten Maße einzutragen. Cadaster
berechnet mit den bekannten Werten möglichst alle anderen Werte des
Grundrisses. Wenn Werte nicht klar berechnet werden können, wird zumindest
eine gültige Variante aller Werte gezeigt.

 Definitionen
--------------

**Expression**

Ausdruck oder Term, der auf einer Seite einer Gleichung steht

*Kann:*
- Variable mit Wert besetzen
- sich selbst auflösen nach Variable

**Gleichung**

*Ist eine:*
- Gültige Verbindung zweier Expressions
- Kann auch mehrere Gleichungen beinhalten, die gleichzeitig gültig sind

*Besteht aus:*
- links: Expression
- Gleichheitszeichen: `=`, `<`, `<=`, `>`, `>=`
- rechts: Expression

*Kann:*
- umgestellt werden nach Variable
- kann auf Korrektheit überprüft werden
  - wenn keine Variablen mehr existieren
- Ergebnis erzeugen für eine Variable erzeugen
  - Wenn linke Seite der Gleichung eine Variable ist
  - Wenn rechte Seite der Gleichung eine Wert ist
  

**Ergebnis**

Das Ergebnis einer *Gleichung*. Eine Menge von konkreten
Werten oder Grenzwerten.

*Kann sein:*
- ein konkreter Wert: z.B. `4.32`
- eine Menge an konkreten Werten: z.B. `2, -2`
- ein Grenzwert: z.B. `]I,23]`
- eine Menge an Grenzwerten: `]I,23], [30, 31]`
- eine Menge an Grenzwerten und konkreten Werten: `2,-2,[-1,1]`


*Kann:*
- fässt automatisch Blöcke zusammen
  - `2,-2,2` --> `-2,2`
  - `[-2,2],1,2,3` --> `[-2,2],3`
  - `[-2,2],[0,3]` --> `[-2,3]`
  - `]0,1],[0,1[` --> `[0,1]`


**Gleichungssystem**

*Besteht aus:*
- einer Menge von Gleichungen

*Kann:*
- sich selbst Lösen und liefert alle Variablen zurück

 Lizenz
--------
Apache 2.0 - 
http://www.apache.org/licenses/LICENSE-2.0
