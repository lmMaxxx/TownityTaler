# TownityTaler

Netzwerkweite **Taler**-Währung für Townity (Velocity + Paper), eigene MySQL-Datenbank.

## Module

| Modul | Artefakt | Zweck |
|-------|----------|--------|
| `taler-api` | JAR | Öffentliche API für Unterserver-Plugins |
| `taler-common` | – | MySQL, Beträge, Plugin-Messages |
| `taler-velocity` | shaded JAR | Proxy: Admin + Shop-Gutschrift |
| `taler-paper` | shaded JAR | Backend: `TalerAPI` + Kauf-Notify |

## Velocity

JAR nach `plugins/` legen. Zugangsdaten liegen im eigenen Ordner:

`plugins/TalerAPI/config.yml` (eigene MySQL-DB `townity_taler`).

### Befehle

- `/velotaler add\|give <Spieler> <Betrag>` – gutschreiben (auch offline)
- `/velotaler remove <Spieler> <Betrag>` – abziehen
- `/velotaler set <Spieler> <Betrag>` – absolut setzen
- `/velotaler info <Spieler>` – Kontostand
- `/boughttaler <Spieler> <Anzahl>` – Shop-Kauf: gutschreiben + Ingame-Benachrichtigung

Rechte: `townity.taler.admin` · `townity.taler.bought`

Offline-Spieler werden über DB-Name bzw. Mojang-API aufgelöst. Ist der Spieler offline, wird die Kauf-Notify bis zum nächsten Join gespeichert.

## Paper (Unterserver)

JAR auf **jedem** Backend, das Taler braucht. Zugangsdaten:

`plugins/TalerAPI/config.yml` (dieselbe MySQL-DB wie Velocity).

### API für andere Plugins

```java
import de.townity.taler.api.TalerAPI;
import de.townity.taler.api.TalerProvider;

// Variante 1
TalerAPI taler = TalerProvider.get();

// Variante 2 (Bukkit Services)
TalerAPI taler = Bukkit.getServicesManager().load(TalerAPI.class);

taler.getBalance(uuid).thenAccept(balance -> { ... });
taler.add(uuid, name, amount);
taler.remove(uuid, amount); // Optional.empty = zu wenig
taler.set(uuid, name, amount);
taler.has(uuid, amount);
String pretty = taler.format(amount); // z. B. 1.000 Taler
```

Maven-Abhängigkeit (nach Publish / lokal install):

```xml
<dependency>
  <groupId>de.townity.taler</groupId>
  <artifactId>taler-api</artifactId>
  <version>1.0.0-SNAPSHOT</version>
  <scope>provided</scope>
</dependency>
```

Soft-Depend in `plugin.yml`: `softdepend: [TalerAPI]`

## Build

```bash
mvn -q -DskipTests package
```

Artefakte:

- `taler-velocity/target/taler-velocity-1.0.0-SNAPSHOT.jar`
- `taler-paper/target/taler-paper-1.0.0-SNAPSHOT.jar`
- `taler-api/target/taler-api-1.0.0-SNAPSHOT.jar`

## Datenbank

Tabellen-Prefix `tt_`:

- `tt_balances` – UUID, Name, Balance
- `tt_pending_notify` – Offline-Kauf-Benachrichtigungen
