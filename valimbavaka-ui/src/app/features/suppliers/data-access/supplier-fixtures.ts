import { DEMO_ARTICLES, DEMO_SUPPLIERS } from '../../../shared/data-access/demo-catalog';
import { SupplierDetailRDto, SupplierRDto, SupplierType } from './supplier-models';

/** [nom, type, ville, délai (j), dernière réception (MM-JJ), contact, actif]. */
type SupplierRow = readonly [string, SupplierType, string, number, string, string, boolean];

const ROWS: readonly SupplierRow[] = [
  ['Huilerie de Sambava', 'RAW_MATERIAL', 'Sambava', 12, '10-21', 'Rivo Andrianaivo', true],
  ['Ravintsara Mada', 'RAW_MATERIAL', 'Moramanga', 7, '10-18', 'Fara Rakotobe', true],
  ["Verrerie d'Antsirabe", 'PACKAGING', 'Antsirabe', 10, '10-08', 'Hery Rasolo', true],
  ['Karité Sud', 'IMPORT', 'Toliara', 15, '09-29', 'Mamy Razafy', true],
  ['Imprimerie Tana Print', 'PACKAGING', 'Antananarivo', 4, '10-20', 'Lanto Rabe', true],
  ['Embal Pack Tana', 'PACKAGING', 'Antananarivo', 5, '10-17', 'Tiana Rakoto', true],
  ['Miel Ambohimanga', 'RAW_MATERIAL', 'Ambohimanga', 4, '10-14', 'Naina Ravelo', true],
  ['Café Ankazobe', 'RAW_MATERIAL', 'Ankazobe', 6, '10-06', 'Solo Randria', true],
  ['Atelier Ravintsara', 'FINISHED', 'Moramanga', 7, '09-30', 'Vola Rakotomalala', true],
  ['Argiles du Sud', 'RAW_MATERIAL', 'Fianarantsoa', 10, '09-22', 'Haja Rafidy', true],
  ['Karité Import Sarl', 'IMPORT', 'Toamasina', 35, '09-29', 'Jean Rabarison', true],
  ["Cartonnerie d'Ankorondrano", 'PACKAGING', 'Antananarivo', 3, '10-19', 'Onja Raharison', true],
  ['Vanille Sava Export', 'RAW_MATERIAL', 'Antalaha', 14, '10-02', 'Mialy Rasoa', true],
  ['Coffrets Raphia Majunga', 'FINISHED', 'Mahajanga', 9, '10-09', 'Zo Ranaivo', true],
  ['Tubes Océan Indien', 'IMPORT', 'Toamasina', 40, '09-12', 'Patrick Ah-Thion', true],
  ["Cire d'abeille Itasy", 'RAW_MATERIAL', 'Arivonimamo', 5, '10-11', 'Fanja Rabe', true],
  ['Étiquettes Print Mada', 'PACKAGING', 'Antananarivo', 4, '07-03', 'Ando Rasolofo', false],
  ['Savonnerie Analamanga', 'FINISHED', 'Antananarivo', 6, '10-15', 'Nirina Rakoto', true],
];

const YEAR = new Date().getFullYear();

function toSupplier(row: SupplierRow, index: number): SupplierRDto {
  const [name, type, city, leadTimeDays, lastReception, , active] = row;
  const shared = DEMO_SUPPLIERS[index];
  const number = String(index + 1).padStart(3, '0');
  return {
    id: shared?.id ?? `sup-${String(index + 1).padStart(2, '0')}`,
    code: shared?.code ?? `FRS-${number}`,
    name,
    type,
    city,
    taxId: `40${String(index * 7919 + 1234)
      .padStart(8, '0')
      .replace(/(\d{2})(\d{3})(\d{3})$/, '$1 $2 $3')}`,
    leadTimeDays,
    lastReceptionOn: `${YEAR}-${lastReception}`,
    active,
  };
}

export const SUPPLIERS_FIXTURE: readonly SupplierRDto[] = ROWS.map(toSupplier);

const ARTICLE_TYPE = {
  RAW_MATERIAL: 'RAW_MATERIAL',
  PACKAGING: 'PACKAGING',
  FINISHED: 'FINISHED',
  IMPORT: 'FINISHED',
} as const;

function slug(name: string): string {
  return name
    .normalize('NFD')
    .replace(/[̀-ͯ]/g, '')
    .toLowerCase()
    .replace(/[^a-z]+/g, '-')
    .replace(/^-|-$/g, '');
}

function articlesOf(supplier: SupplierRDto, index: number): SupplierDetailRDto['articles'] {
  const candidates = DEMO_ARTICLES.filter(
    (article) => article.type === ARTICLE_TYPE[supplier.type],
  );
  return Array.from({ length: Math.min(3 + (index % 4), candidates.length) }, (_, offset) => {
    const article = candidates[(index + offset) % candidates.length];
    return { reference: article.reference, name: article.name, lastUnitCost: article.cost };
  });
}

function receptionsOf(supplier: SupplierRDto, index: number): SupplierDetailRDto['receptions'] {
  const last = new Date(`${supplier.lastReceptionOn}T12:00:00`);
  return [0, 1, 2].map((rank) => {
    const day = new Date(last);
    day.setDate(day.getDate() - rank * (supplier.leadTimeDays + 6));
    return {
      number: `BR-00${31 - index - rank * 4}`,
      label: rank === 1 ? 'Commande complémentaire' : 'Réapprovisionnement',
      receivedOn: day.toISOString().slice(0, 10),
    };
  });
}

export function supplierDetailFixture(id: string): SupplierDetailRDto | null {
  const index = SUPPLIERS_FIXTURE.findIndex((supplier) => supplier.id === id);
  if (index < 0) {
    return null;
  }
  const supplier = SUPPLIERS_FIXTURE[index];
  const site = slug(supplier.name);
  return {
    ...supplier,
    contactName: ROWS[index][5],
    phone: `+261 3${index % 3} ${String(10 + index).padStart(2, '0')} ${String(100 + index * 7).slice(0, 3)} ${String(40 + index)}`,
    email: `commandes@${site}.mg`,
    address: `Lot ${index + 2} ${String.fromCharCode(65 + (index % 6))}, ${supplier.city}`,
    transport:
      supplier.type === 'IMPORT' ? 'SEA' : supplier.city === 'Antananarivo' ? 'LOCAL' : 'ROAD',
    articles: articlesOf(supplier, index),
    receptions: receptionsOf(supplier, index),
  };
}
