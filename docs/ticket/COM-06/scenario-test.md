# Scénarios de test — COM-06 · Ajout d'une règle de commission

Statut : ☐ tests automatiques à lancer (CI), recette 🧪 à passer · Ticket : [#90](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/90) · [description](description.md) · [guide](guide.md)

Convention (VVS V2.1) : tests backend `methode_scenario_resultatAttendu`, SUT nommé `sut`, Arrange / Act / Assert, Testcontainers PostgreSQL ; tests frontend Vitest sur le comportement visible.

**Périmètre.** Le formulaire, l'API d'écriture et le résolveur de taux sont livrés par [COM-01](../COM-01/guide.md) (PR #185). COM-06 n'ajoute **aucun code de production** : il démontre, critère par critère, qu'une règle saisie dans le formulaire est enregistrée **et** prise par le calcul (`CommissionRateResolver`, utilisé par COM-02), y compris après modification, désactivation et saisie hors ligne. Les cas déjà couverts par COM-01 sont cités, pas re-testés.

**Décision PO du 07/11/2026** : la commission la plus précise gagne, sans cumul : taux du service salon, sinon taux du canal (règle active), sinon taux de l'utilisateur.

## Pré-requis

| Élément | Valeur |
|---|---|
| Rôle(s) utilisé(s) | `ADMIN` (`admin.test@valimbavaka.mg`) pour les écritures ; vendeuse `SELLER` (Miora Andriamasy) comme bénéficiaire du calcul |
| Données de départ | en test d'intégration, un canal neuf par test (`IT-xxxxxxxx`, inséré par `CommissionRuleCalculationIT.createChannel`, la base est partagée et un canal n'a qu'une règle active) ; vendeuse à **5,00 %** rattachée à ce canal |
| Tickets à avoir déployés avant | COM-01 (migration `2026_11_08-90`), JNL-01 |

## Scénarios issus des critères d'acceptation

Critère : « Le formulaire enregistre la règle et la rend disponible pour le calcul. »

| # | Cas | Exemple | Résultat attendu | Test | Statut |
|---|---|---|---|---|---|
| 1 | Le formulaire enregistre la règle | « Facebook live », Facebook, `5.25`, Marge brute | POST `/api/v1/commission-rules`, retour à la liste (COM-01) | `commission-rule-form-page.spec.ts` › should create the rule with POST then go back to the rules | ✅ auto (CI COM-01) |
| 1b | La règle enregistrée est relue et prise par le calcul, avec son taux **et** sa base | POST « Regle-… », canal neuf, `4.75`, `MARGIN` ; vendeuse à 5 % | GET `/{id}` renvoie la règle active ; `resolve` → `4.75`, `MARGIN`, source `SALES_CHANNEL`, `ruleId` = id créé (le taux de la vendeuse n'est plus appliqué) | `CommissionRuleCalculationIT.createRule_byAdmin_isStoredAndAppliedWithItsRateAndBase` | ☐ |
| 1c | Avant la règle, le calcul retombe sur la vendeuse | même canal, aucune règle | `5.00`, `REVENUE`, source `SELLER` | même test (assertion « avant ») ; `CommissionRuleSecurityIT.resolve_channelRuleThenSellerRate_appliesTheMostPreciseLevel` | ☐ / ✅ auto (CI COM-01) |
| 1d | Une modification est prise par le calcul immédiatement | règle à `3` → PUT à `6.5`, base `REVENUE` | `resolve` → `6.50` | `CommissionRuleCalculationIT.updateRule_newRate_isAppliedByTheNextCalculation` | ☐ |
| 1e | Une règle désactivée n'est plus prise, puis l'est de nouveau après réactivation | PATCH `/status` `{"active":false}` puis `{"active":true}` | désactivée : `5.00` (`SELLER`) ; réactivée : `3.00` (`SALES_CHANNEL`) | `CommissionRuleCalculationIT.setActive_deactivateThenReactivate_switchesTheCalculationBackAndForth` | ☐ |
| 1f | Une règle ne vaut que pour son canal | règle à `3` sur le canal B ; vente de la vendeuse sur le canal A | canal A : `5.00` (`SELLER`) ; canal B : `3.00` | `CommissionRuleCalculationIT.createRule_onAnotherChannel_leavesThisChannelOnTheSellerRate` | ☐ |
| 1g | Une règle refusée n'est jamais prise par le calcul | POST à `100.01` → 422 | `resolve` → `5.00` (`SELLER`) | `CommissionRuleCalculationIT.createRule_refusedWith422_leavesTheCalculationUnchanged` | ☐ |
| 1h | Le formulaire n'affiche aucun message « en attente » quand la règle est enregistrée en ligne | en ligne, création | POST avec `Idempotency-Key`, retour à la liste, aucun toast | `commission-rule-form-page-offline.spec.ts` › should send the rule with an idempotency key when online and show no waiting message | ☐ |

## Sécurité et erreurs

Contrôles de l'endpoint d'écriture déjà prouvés par COM-01 (aucun endpoint nouveau) :

| Cas | Exemple | Résultat attendu | Test | Statut |
|---|---|---|---|---|
| Sans jeton | GET `/api/v1/commission-rules` | 401 `UNAUTHENTICATED` | `CommissionRuleSecurityIT.getRules_withoutToken_returns401` | ✅ auto (CI COM-01) |
| Mauvais rôle | jeton vendeuse, POST | 403 `FORBIDDEN` | `CommissionRuleSecurityIT.createRule_withSellerRole_returns403` | ✅ auto (CI COM-01) |
| Mass assignment | `"id"` forcé et `"active":false` | `id` ignoré, règle créée active | `CommissionRuleSecurityIT.createRule_withIdInBody_ignoresIt` | ✅ auto (CI COM-01) |
| Données invalides | taux `100.01` | 422 `VALIDATION_FAILED`, et rien n'entre dans le calcul | `CommissionRuleSecurityIT.createRule_rateAboveHundred_returns422` ; `CommissionRuleCalculationIT.createRule_refusedWith422_leavesTheCalculationUnchanged` | ✅ auto (CI COM-01) / ☐ |
| Ressource introuvable | GET `/{uuid aléatoire}` | 404 `NOT_FOUND` | `CommissionRuleSecurityIT.getRuleById_unknownId_returns404` | ✅ auto (CI COM-01) |
| Doublon | 2ᵉ règle active sur un canal | 409 `CONFLICT` | `CommissionRuleSecurityIT.createRule_secondActiveRuleOnSameChannel_returns409` | ✅ auto (CI COM-01) |
| Journal | création | ligne `CREATE`, `resourceType = COMMISSION_RULE` | `CommissionRuleSecurityIT.createRule_byAdmin_returns201AndTracesInJournal` | ✅ auto (CI COM-01) |

## Cas limites et hors ligne

| Cas | Exemple | Résultat attendu | Test | Statut |
|---|---|---|---|---|
| Saisie hors ligne | réseau coupé, « Facebook live », `5.25`, Marge brute | rien n'est envoyé, 1 saisie en file, toast `common.queuedSaved`, retour à la liste. La règle n'est **pas encore** disponible pour le calcul | `commission-rule-form-page-offline.spec.ts` › should keep a rule entered offline in the queue, say so and go back to the rules | ☐ |
| Retour du réseau | même saisie, réseau rétabli | un seul POST, même corps, `Idempotency-Key` UUID ; file vide après la réponse : la règle devient disponible pour le calcul (1b) | `commission-rule-form-page-offline.spec.ts` › should send the queued rule once with its idempotency key when the network is back | ☐ |
| Formulaire invalide hors ligne | nom `"   "` | rien en file, pas de navigation | `commission-rule-form-page-offline.spec.ts` › should queue nothing when the form is invalid, even offline | ☐ |
| Bornes du taux | `-1`, `3.555`, `100.01` refusés ; `100` accepté | voir COM-01 | `commission-rule-form-page.spec.ts` (`it.each`) ; should accept a rate of exactly 100 | ✅ auto (CI COM-01) |
| Taux de la règle inférieur à celui de la vendeuse | règle `4.75` %, vendeuse `5.00` % | la règle gagne quand même (pas de « meilleur des deux ») | `CommissionRuleCalculationIT.createRule_byAdmin_isStoredAndAppliedWithItsRateAndBase` | ☐ |
| Deux administrateurs en même temps sur le même canal | 2 POST simultanés | l'index unique partiel refuse le second (409) : une seule règle entre dans le calcul | 🔍 code `uq_commission_rules_sales_channel_id_active` (COM-01) | 🔍 code |

## Tests existants

- Backend (nouveau) : `valimbavaka-core/src/test/java/org/vvs/valimbavaka/feature/commission/controller/CommissionRuleCalculationIT.java` (5 tests).
- Frontend (nouveau) : `valimbavaka-ui/src/app/features/commissions/pages/commission-rule-form-page/commission-rule-form-page-offline.spec.ts` (4 tests).
- Repris de COM-01 : `CommissionRuleSecurityIT`, `CommissionRuleServiceImplTest`, `CommissionRateResolverImplTest`, `commission-rule-form-page.spec.ts`, `commission-rule-list-page.spec.ts`, `commission-rule-service.spec.ts`.

## Recette manuelle (🧪)

| Étape | Résultat attendu | Statut |
|---|---|---|
| Administrateur : Administration › Règles de commission › Nouvelle règle, « Boutique standard », Boutique, 3 %, Chiffre d'affaires | règle listée, active ; ligne `CREATE` au journal | 🧪 |
| Couper le réseau (DevTools hors ligne), créer une règle sur Facebook, rétablir le réseau | toast « enregistré, en attente du réseau », puis la règle apparaît dans la liste après synchronisation | 🧪 |
| Après COM-02 : une vente Boutique d'une vendeuse à 5 % | commission calculée à 3 % du CA | 🧪 (quand COM-02 sera livré) |

## Résultat de l'exécution

- Date : à lancer (CI ou local).
- Environnement : à lancer (CI ou local).
- Anomalies ouvertes : aucune à ce stade.
- Étapes 🧪 restantes : 3.
