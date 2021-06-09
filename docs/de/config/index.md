---
title: Spiegel Konfiguration
---

Wurde ein Repository als Spiegel erstellt, gibt es eine Konfiguration für diese Spiegelung. 
Neben dem sofortigen Starten eines Synchronisationslaufes [^1] können hier die Zugangsdaten,
das Synchronisationsintervall sowie die Filter angepasst werden.

Zusätzlich zum Zeitpunkt der Erstellung findet sich hier eine Liste von "Notfallkontakten". Diese
Benutzer werden benachrichtigt, wenn sich der Status einer Spiegelung ändert.

Die Details zu den anderen Einstellungen finden sich in der [Dokumentation zur Erstelung](../create).

![Konfiguration der Spiegelung](assets/mirror-configuration.png)

[^1]: Es laufen grundsätzlich höchstens vier Synchronisationen gleichzeitig.

## Globale Konfiguration

In den Administrationseinstellungen können zusätzliche Einstellungen vorgenommen
und Standardwerte für Filter definiert werden. Außerdem kann die lokale Konfiguration
der Filter unterbunden werden.

![Konfiguration der Spiegelung](assets/global-mirror-configuration.png)
