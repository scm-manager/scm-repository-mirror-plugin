---
title: Mirror erstellen
---

Im SCM-Manager gibt es neben dem Erstellen eines neuen Repositories 
und dem Importieren eines bestehenden Repositories noch einen dritten Modus `Mirroring Repository`.

Über das Formular können eine URL, mögliche Zugangsdaten und ein Synchronisierungsintervall definiert werden. 
Zusätzlich werden die SCM-Manager üblichen Informationen zum neuen Repository abgefragt.
Auf Grundlage dieser Daten wird beim Erstellen im SCM-Manager ein neues Repository angelegt 
und der Inhalt von der externen Quelle gespiegelt.

Gespiegelte Repository werden über den `Mirror`-Tag kenntlich gemacht. 
Ein grauer Tag weist auf eine laufende Synchronisation mit der externen Quelle hin.
Wurden die Daten erfolgreich synchronisiert, ist der Tag grün, im Fehlerfall rot.

[!Create_Mirror]("screenshot")

Die Mirror Einstellungen wie Quelle, Zugangsdaten und Synchronisationsintervall können auch nachträglich
über das entsprechende Einstellungsmenü im Repository angepasst werden.

