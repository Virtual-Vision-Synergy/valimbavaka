import { StockRDto } from '../../stocks/data-access/stock-models';
import { SalonServiceRDto } from './salon-catalog-models';
import { SALON_LOCATION_CODE, ServiceRecordRDto } from './service-record-models';
import { SalonProductRDto, ServiceRecordRowRDto, TreatmentRDto } from './service-models';

/** Unité courte d'un article (`ArticleRDto.unit`) : les quantités consommées sont dans cette unité. */
const SHORT_UNITS: Readonly<Record<string, string>> = { PIECE: 'u.', KILOGRAM: 'kg', LITER: 'L' };

/** Article actif proposé à la consommation, avec sa quantité dans l'emplacement salon (0 s'il n'y en a pas). */
export function toSalonProduct(stock: StockRDto): SalonProductRDto {
  const salon = stock.locations.find((location) => location.code === SALON_LOCATION_CODE);
  return {
    id: stock.articleId,
    name: stock.name,
    unit: SHORT_UNITS[stock.unit] ?? stock.unit,
    stock: salon?.quantity ?? 0,
  };
}

/**
 * Soin proposé à l'enregistrement. La recette ne garde que les articles proposés (`known`) : un article désactivé
 * depuis la création du service n'est plus pré-rempli.
 */
export function toTreatment(service: SalonServiceRDto, known: ReadonlySet<string>): TreatmentRDto {
  return {
    id: service.id,
    name: service.name,
    minutes: service.durationMinutes,
    price: service.price,
    recipe: service.products
      .filter((product) => known.has(product.articleId))
      .map((product) => ({ productId: product.articleId, quantity: product.quantity })),
  };
}

/**
 * Ligne « Mes prestations du jour » (SRV-03) : heure locale `HH:MM`, soin, cliente, produits consommés et prix copiés
 * à l'enregistrement. `units` donne l'unité courte de chaque article proposé ; un article absent n'en a pas.
 */
export function toServiceRow(
  record: ServiceRecordRDto,
  units: ReadonlyMap<string, string>,
): ServiceRecordRowRDto {
  return {
    id: record.id,
    number: record.number,
    treatmentName: record.serviceName,
    time: new Date(record.performedAt).toTimeString().slice(0, 5),
    customer: record.customerName,
    products: record.products.map((product) => ({
      name: product.articleName,
      quantity: product.quantity,
      unit: units.get(product.articleId) ?? '',
    })),
    price: record.price,
  };
}
