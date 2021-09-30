---
title: Spiegel Konfiguration
---

Wurde ein Repository als Spiegel erstellt, gibt es eine Konfiguration für diese Spiegelung. 
Neben dem sofortigen Starten eines Synchronisationslaufes [^1] können hier die Zugangsdaten,
das Synchronisationsintervall sowie die Filter angepasst werden.

Zusätzlich zum Zeitpunkt der Erstellung findet sich hier eine Liste von "Notfallkontakten". Diese
Benutzer werden benachrichtigt, wenn sich der Status einer Spiegelung ändert.

Die Details zu den anderen Einstellungen finden sich in der [Dokumentation zur Erstellung](../create).

![Konfiguration der Spiegelung](assets/mirror-configuration.png)

[^1]: Es laufen grundsätzlich höchstens vier Synchronisationen gleichzeitig.

In dem rot-umrandeten Bereich unten kann über die Aktion `Repository entspiegeln` die Spiegelung endgültig beendet werden. 
Als Folgen daraus wird das Repository nicht mehr von der externen Quelle aktualisiert und das Repository kann direkt bearbeitet werden.
Sobald eine Spiegelung beendet wurde, kann dieses Repository nicht mehr in einen Spiegel umgewandelt werden!
![Spiegelung beenden](assets/unmirror.png)

## Globale Konfiguration

In den Administrationseinstellungen können zusätzliche Einstellungen vorgenommen
und Standardwerte für Filter definiert werden. Außerdem kann die lokale Konfiguration
der Filter unterbunden werden.

![Konfiguration der Spiegelung](assets/global-mirror-configuration.png)
