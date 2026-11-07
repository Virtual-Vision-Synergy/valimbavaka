import { StockLocation } from '../../../shared/data-access/demo-catalog';
import { ProductRange } from '../../../shared/models/tone';

export type { StockLocation } from '../../../shared/data-access/demo-catalog';

/** Quantité d'un article dans un emplacement ; `code` est le code du référentiel (ex. `STORE`). */
export interface StockLocationQuantityRDto {
  readonly locationId: string;
  readonly code: string;
  readonly label: string;
  readonly quantity: number;
}

/** `GET /api/v1/stocks` : stock d'un article, somme des mouvements par emplacement actif et total (STK-01). */
export interface StockRDto {
  readonly articleId: string;
  readonly code: string;
  readonly name: string;
  readonly rangeId: string | null;
  readonly unit: string;
  readonly threshold: number;
  readonly cost: number | null;
  readonly total: number;
  readonly locations: readonly StockLocationQuantityRDto[];
}

/**
 * `GET /api/v1/stocks/alerts` : article actif sous son seuil minimum (STK-04). `quantity` est le total des
 * emplacements actifs, `shortage` = seuil − stock, `suggestedQuantity` = manque arrondi à la dizaine supérieure.
 * Fournisseur, délai et date de livraison prévue (commande du jour + délai) viennent du fournisseur de l'article
 * (FRS-04) ; tous `null` pour un article sans fournisseur (fabrication interne).
 */
export interface StockAlertRDto {
  readonly articleId: string;
  readonly code: string;
  readonly name: string;
  readonly unit: string;
  readonly quantity: number;
  readonly threshold: number;
  readonly shortage: number;
  readonly suggestedQuantity: number;
  readonly supplierId: string | null;
  readonly supplierName: string | null;
  readonly leadTimeDays: number | null;
  /** Date ISO `yyyy-MM-dd`, au fuseau de Madagascar. */
  readonly expectedDeliveryDate: string | null;
}

/** `POST /api/v1/stocks/restock-orders` (STK-05) : articles à commander ; vide = tous les articles sous le seuil. */
export interface RestockOrderWDto {
  readonly articleIds: readonly string[];
}

/** Ligne d'un bon de réapprovisionnement, copiée de l'alerte de seuil au moment de la génération (STK-05). */
export interface RestockOrderLineRDto {
  readonly lineNumber: number;
  readonly articleId: string;
  readonly articleCode: string;
  readonly articleName: string;
  readonly unit: string;
  readonly stockQuantity: number;
  readonly threshold: number;
  /** Quantité à commander : la quantité suggérée de l'alerte (STK-04). */
  readonly quantity: number;
  readonly supplierId: string | null;
  readonly supplierName: string | null;
  readonly leadTimeDays: number | null;
  readonly expectedDeliveryDate: string | null;
}

/** Bon de réapprovisionnement généré par le serveur (`BR-000001`), `RECEIVED` une fois réceptionné (STK-06). */
export interface RestockOrderRDto {
  readonly id: string;
  readonly reference: string;
  readonly status: 'PENDING' | 'RECEIVED';
  readonly createdAt: string;
  readonly lines: readonly RestockOrderLineRDto[];
}

/** Ligne reçue d'une livraison fournisseur (STK-06) : quantité à 3 décimales, coût unitaire d'achat en ariary. */
export interface SupplierReceiptLineWDto {
  readonly articleId: string;
  readonly quantity: number;
  readonly unitCost: number;
}

/**
 * `POST /api/v1/stocks/receipts` (STK-06) : sans emplacement, l'entrepôt ; `restockOrderId` reprend un bon
 * de réapprovisionnement (STK-05), qui passe alors à `RECEIVED`.
 */
export interface SupplierReceiptWDto {
  readonly supplierId: string;
  readonly locationId: string | null;
  readonly restockOrderId: string | null;
  readonly lines: readonly SupplierReceiptLineWDto[];
  readonly note: string | null;
}

export interface SupplierReceiptLineRDto {
  readonly articleId: string;
  readonly articleCode: string;
  readonly articleName: string;
  readonly quantity: number;
  readonly unitCost: number;
  /** Quantité × coût unitaire, arrondi au centime. */
  readonly lineCost: number;
}

/** Réception enregistrée (`REC-000001`) ; `REVERSED` une fois contre-passée (mouvements inverses). */
export interface SupplierReceiptRDto {
  readonly id: string;
  readonly number: string;
  readonly status: 'VALIDATED' | 'REVERSED';
  readonly supplierId: string;
  readonly supplierName: string;
  readonly locationId: string;
  readonly restockOrderId: string | null;
  readonly note: string | null;
  readonly createdAt: string;
  readonly totalCost: number;
  readonly lines: readonly SupplierReceiptLineRDto[];
}

/** Stock d'un emplacement, tous articles confondus : valeur au coût d'achat en ariary (STK-03). */
export interface StockLocationValueRDto {
  readonly locationId: string;
  readonly code: string;
  readonly label: string;
  readonly quantity: number;
  readonly value: number;
  /** Part de la quantité non valorisée : ses articles n'ont pas de coût d'achat. */
  readonly unvaluedQuantity: number;
}

/** `GET /api/v1/stocks/valuation` (permission `stocks.value`) : chaque emplacement actif et les totaux (STK-03). */
export interface StockValuationRDto {
  readonly quantity: number;
  readonly value: number;
  readonly unvaluedQuantity: number;
  readonly locations: readonly StockLocationValueRDto[];
}

/**
 * Ligne affichée par la page des stocks, déduite d'un `StockRDto` (voir `stock-mapping.ts`).
 * Fournisseur, délai et ventes journalières sont provisoires jusqu'à STK-05.
 */
export interface StockRowRDto {
  readonly articleId: string;
  readonly reference: string;
  readonly name: string;
  readonly range: ProductRange | null;
  readonly quantities: Readonly<Record<StockLocation, number>>;
  readonly threshold: number;
  readonly unitCost: number;
  /** Fournisseur principal ; `null` = fabrication interne (atelier). */
  readonly supplierName: string | null;
  readonly leadTimeDays: number | null;
  readonly dailySales: number;
}
