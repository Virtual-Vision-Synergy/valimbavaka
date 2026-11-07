# Scénarios de test — STK-02 · Jauge visuelle du stock

Statut : ✅ rédigé le 2026-11-08 — **tests non exécutés** · Ticket : [#31](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/31) · [description](description.md) · [guide](guide.md)

Convention (VVS V2.1) : tests backend `methode_scenario_resultatAttendu`, SUT nommé `sut`, Arrange / Act / Assert, Testcontainers PostgreSQL ; tests frontend Vitest sur le comportement visible.

## Pré-requis

| Élément | Valeur |
|---|---|
| Rôle(s) utilisé(s) | `ADMIN` (user story) ; `SELLER` voit aussi la page (permission `stocks.read`) |
| Données de départ | Article `ART-0031 · Savon noir 100 g`, seuil 40 : 30 à l'entrepôt, 10 en boutique, 0 en livraison, 0 au salon (mouvements STK-01) |
| Tickets à avoir déployés avant | STK-01 (lecture du stock par emplacement) ; STK-03 et STK-04 sont sur la même page |

Ticket **frontend seul** : la répartition par emplacement est déjà renvoyée par `GET /api/v1/stocks` (STK-01).
Pas d'endpoint, pas de migration, pas d'écriture.

## Scénarios issus des critères d'acceptation

| # | Critère d'acceptation | Étant donné / Quand / Alors | Résultat attendu | Test auto | OK |
|---|---|---|---|---|---|
| 1 | Le survol de la jauge affiche le détail chiffré par emplacement. | Étant donné la page Stocks sur « Tous », quand le pointeur survole la jauge de `ART-0031`, alors une bulle s'ouvre | Bulle : Entrepôt 30 (75 %), Boutique 10 (25 %), Livraison 0 (0 %), Salon 0 (0 %), Total 40 | `StockGauge` › « affiche au survol le détail chiffré de chaque emplacement et le total » ; `StockPage` › « affiche au survol de la jauge le détail chiffré par emplacement (STK-02) » | ☐ |
| 2 | (visuel) Jauge de répartition | Même article, échelle 50 (seuil × 2,5 = 100 dans la page) | Une part colorée par emplacement, largeur proportionnelle (30/50 = 60 %, 10/50 = 20 %), même couleur que la pastille de la bulle | `StockGauge` › « dessine une part par emplacement, proportionnelle à la pleine échelle » | ☐ |
| 3 | (visuel) Fermeture | Bulle ouverte, le pointeur quitte la jauge | La bulle se ferme | `StockGauge` › « referme la bulle quand le pointeur quitte la jauge » | ☐ |

## Accessibilité (tient lieu de « Sécurité et erreurs » : aucun appel serveur nouveau)

| Cas | Résultat attendu | Test auto | OK |
|---|---|---|---|
| Navigation au clavier | La jauge reçoit le focus (Tab) ; le focus ouvre la bulle ; Échap la ferme | `StockGauge` › « ouvre la bulle au focus clavier et la ferme avec Échap » | ☐ |
| Lecteur d'écran | Jauge `role="img"`, libellé « Répartition du stock par emplacement », décrite par la bulle (`aria-describedby`) | `StockGauge` › « est atteignable au clavier et décrite par le détail pour les lecteurs d'écran » | ☐ |
| Jauge sans répartition (autres usages) | Reste décorative (`aria-hidden="true"`), pas de bulle, pas de focus | `StockGauge` › « reste décorative et sans bulle quand aucune répartition n'est fournie » | ☐ |
| 401 / 403 / 404 / 409 / 422, mass assignment | Sans objet : aucun endpoint nouveau ; la lecture reste couverte par `StockSecurityIT` (STK-01) | — | — |

## Cas limites et hors ligne

| Cas | Résultat attendu | Test auto | OK |
|---|---|---|---|
| Stock vide partout | Jauge vide encadrée en rouge (rupture) ; bulle : 0 et 0 % partout, Total 0 (pas de division par zéro) | `StockGauge` › « affiche 0 % partout et signale la rupture quand le stock est vide » | ☐ |
| Stock au-delà de la pleine échelle (200 pour une échelle de 50) | Les parts sont réduites pour tenir : 150 / 50 → 75 % / 25 % | `StockGauge` › « réduit les parts pour tenir dans la jauge quand le stock dépasse la pleine échelle » | ☐ |
| Un seul emplacement choisi (carte « Entrepôt ») | La jauge redevient la jauge de niveau de cet emplacement (seuil / 4), sans bulle | `StockPage` › « revient à la jauge simple quand un seul emplacement est choisi (STK-02) » | ☐ |
| Sous le seuil sur « Tous » | La jauge garde ses couleurs d'emplacement et s'entoure d'un liseré ocre ; le ratio « total / seuil » reste affiché | Vérification visuelle (recette) | ☐ |
| Tableau défilé horizontalement, dernière ligne | La bulle (position fixe) n'est pas coupée par le cadre du tableau ; elle se ferme au défilement de la fenêtre | Vérification visuelle (recette) | ☐ |
| Hors ligne | Rien de nouveau : la page affiche la dernière liste chargée ou l'état d'erreur de STK-01 | — | — |

## Résultat de l'exécution

- Date : à lancer (CI ou local)
- Environnement : à lancer (CI ou local) — `ng test` non lancé ; `tsc` (app et spec), ESLint et Prettier passés sur les fichiers touchés
- Anomalies ouvertes : aucune connue
