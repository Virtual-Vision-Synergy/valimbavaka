import { ProductRange } from '../models/tone';

/** Nature d'un article (miroir de l'énumération backend ArticleType). */
export type ArticleType = 'FINISHED' | 'RAW_MATERIAL' | 'PACKAGING';

/** Emplacements de stock (miroir de l'énumération backend StockLocation). */
export type StockLocation = 'WAREHOUSE' | 'SHOP' | 'DELIVERY' | 'SALON';

export const STOCK_LOCATIONS: readonly StockLocation[] = ['WAREHOUSE', 'SHOP', 'DELIVERY', 'SALON'];

/** Article du catalogue de démonstration, partagé par les écrans en attente de leur API. */
export interface DemoArticle {
  readonly id: string;
  readonly reference: string;
  readonly barcode: string;
  readonly name: string;
  readonly type: ArticleType;
  readonly range: ProductRange | null;
  readonly price: number;
  readonly cost: number;
  readonly stock: Readonly<Record<StockLocation, number>>;
  readonly threshold: number;
  readonly favorite: boolean;
  readonly box: boolean;
}

/** [référence, nom, type, gamme, prix, coût, stocks E/B/L/S, seuil, drapeaux F(avori) B(coffret)]. */
type CatalogRow = readonly [
  string,
  string,
  ArticleType,
  ProductRange | null,
  number,
  number,
  readonly [number, number, number, number],
  number,
  string,
];

const ROWS: readonly CatalogRow[] = [
  ['ART-0031', 'Savon noir 100 g', 'FINISHED', 'BOTANIKA', 8500, 3120, [86, 42, 18, 8], 40, 'F'],
  [
    'ART-0112',
    'Huile de coco vierge 250 ml',
    'FINISHED',
    'BOTANIKA',
    12500,
    4600,
    [8, 0, 4, 2],
    30,
    'F',
  ],
  ['ART-0087', 'Gommage café 200 g', 'FINISHED', 'ZEZIKA', 12500, 5100, [10, 3, 5, 0], 30, ''],
  ['ART-0154', 'Beurre de karité 150 g', 'FINISHED', 'SOLIKA', 15000, 6320, [6, 12, 8, 0], 30, 'F'],
  ['ART-0201', 'Baume lèvres miel 15 g', 'FINISHED', 'ZEZIKA', 4500, 1420, [40, 14, 6, 1], 30, ''],
  [
    'ART-0176',
    'Shampoing solide ravintsara',
    'FINISHED',
    'BOTANIKA',
    14000,
    5980,
    [24, 12, 6, 2],
    20,
    '',
  ],
  [
    'ART-0133',
    'Lait corporel vanille 200 ml',
    'FINISHED',
    'SOLIKA',
    18000,
    8000,
    [20, 9, 6, 2],
    20,
    'F',
  ],
  [
    'ART-0190',
    'Masque argile rose 100 g',
    'FINISHED',
    'ZEZIKA',
    11000,
    3900,
    [30, 11, 4, 9],
    20,
    '',
  ],
  ['ART-0045', 'Savon curcuma 100 g', 'FINISHED', 'SOLIKA', 9000, 3300, [50, 15, 10, 4], 30, ''],
  [
    'ART-0118',
    'Huile de ricin 100 ml',
    'FINISHED',
    'BOTANIKA',
    10000,
    4100,
    [28, 11, 6, 3],
    20,
    '',
  ],
  [
    'ART-0240',
    'Coffret découverte Botanika',
    'FINISHED',
    'BOTANIKA',
    35000,
    14800,
    [12, 4, 2, 0],
    5,
    'B',
  ],
  ['ART-0162', 'Brume corps ylang', 'FINISHED', 'SOLIKA', 16000, 6900, [18, 8, 4, 1], 15, ''],
  ['MP-0008', 'Base savonneuse', 'RAW_MATERIAL', null, 0, 9800, [12, 0, 0, 0], 10, ''],
  ['MP-0012', 'Beurre de karité brut', 'RAW_MATERIAL', null, 0, 21000, [9, 0, 0, 0], 5, ''],
  ['MP-0017', 'Argile rose', 'RAW_MATERIAL', null, 0, 14500, [6, 0, 0, 0], 4, ''],
  ['MP-0021', 'Huile essentielle ravintsara', 'RAW_MATERIAL', null, 0, 48000, [3, 0, 0, 0], 5, ''],
  ['MP-0030', 'Café moulu', 'RAW_MATERIAL', null, 0, 12000, [7, 0, 0, 0], 3, ''],
  ['EMB-0004', 'Pot verre ambré 150 ml', 'PACKAGING', null, 0, 1200, [240, 0, 0, 0], 100, ''],
  ['EMB-0011', 'Étiquette Botanika', 'PACKAGING', null, 0, 150, [820, 0, 0, 0], 300, ''],
  ['EMB-0020', 'Boîte coffret kraft', 'PACKAGING', null, 0, 2400, [36, 0, 0, 0], 20, ''],
];

function toArticle(row: CatalogRow): DemoArticle {
  const [reference, name, type, range, price, cost, stocks, threshold, flags] = row;
  const [warehouse, shop, delivery, salon] = stocks;
  return {
    id: `art-${reference.toLowerCase()}`,
    reference,
    barcode: `6201234${reference.replace(/\D/g, '').padStart(6, '0')}`,
    name,
    type,
    range,
    price,
    cost,
    stock: { WAREHOUSE: warehouse, SHOP: shop, DELIVERY: delivery, SALON: salon },
    threshold,
    favorite: flags.includes('F'),
    box: flags.includes('B'),
  };
}

/** Catalogue de démonstration (références de la maquette) ; remplacé par l'API articles à terme. */
export const DEMO_ARTICLES: readonly DemoArticle[] = ROWS.map(toArticle);

/** Stock total d'un article, tous emplacements confondus. */
export function totalStock(article: DemoArticle): number {
  return STOCK_LOCATIONS.reduce((sum, location) => sum + article.stock[location], 0);
}

/** Recherche d'un article de démonstration par référence (« ART-0031 »). */
export function demoArticle(reference: string): DemoArticle {
  const article = DEMO_ARTICLES.find((item) => item.reference === reference);
  if (!article) {
    throw new Error(`Article de démonstration inconnu : ${reference}`);
  }
  return article;
}

/** Fournisseur de démonstration (FRS-01). */
export interface DemoSupplier {
  readonly id: string;
  readonly code: string;
  readonly name: string;
  readonly city: string;
  readonly leadTimeDays: number;
}

export const DEMO_SUPPLIERS: readonly DemoSupplier[] = [
  { id: 'sup-01', code: 'FRS-001', name: 'Huilerie de Sambava', city: 'Sambava', leadTimeDays: 12 },
  { id: 'sup-02', code: 'FRS-002', name: 'Ravintsara Mada', city: 'Moramanga', leadTimeDays: 7 },
  {
    id: 'sup-03',
    code: 'FRS-003',
    name: "Verrerie d'Antsirabe",
    city: 'Antsirabe',
    leadTimeDays: 10,
  },
  { id: 'sup-04', code: 'FRS-004', name: 'Karité Sud', city: 'Toliara', leadTimeDays: 15 },
  {
    id: 'sup-05',
    code: 'FRS-005',
    name: 'Imprimerie Tana Print',
    city: 'Antananarivo',
    leadTimeDays: 4,
  },
];
