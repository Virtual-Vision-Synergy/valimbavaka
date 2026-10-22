import { MaterialRDto, ProducibleArticleRDto } from './production-models';

export interface DraftLine {
  readonly materialId: string;
  readonly quantity: number;
}

export interface DraftRow {
  readonly line: DraftLine;
  readonly material: MaterialRDto;
  readonly short: boolean;
  readonly cost: number;
}

export interface LotCosts {
  readonly materials: number;
  readonly perUnit: number;
  readonly margin: number | null;
}

/** Lignes proposées par la recette du produit pour la quantité demandée (arrondies au millième). */
export function recipeLines(
  product: ProducibleArticleRDto | undefined,
  quantity: number,
): readonly DraftLine[] {
  return (product?.recipe ?? []).map(({ materialId, perUnit }) => ({
    materialId,
    quantity: Math.round(perUnit * quantity * 1000) / 1000,
  }));
}

export function toRows(
  lines: readonly DraftLine[],
  materials: readonly MaterialRDto[],
): readonly DraftRow[] {
  return lines.flatMap((line) => {
    const material = materials.find((item) => item.id === line.materialId);
    return material
      ? [
          {
            line,
            material,
            short: line.quantity > material.available,
            cost: line.quantity * material.unitCost,
          },
        ]
      : [];
  });
}

/** Plus grande quantité réalisable avec le stock, d'après la recette (null si aucune contrainte). */
export function maxProducible(
  rows: readonly DraftRow[],
  product: ProducibleArticleRDto | undefined,
): number | null {
  const limits = rows.flatMap((row) => {
    const perUnit = product?.recipe.find((item) => item.materialId === row.material.id)?.perUnit;
    return perUnit ? [Math.floor(row.material.available / perUnit)] : [];
  });
  return limits.length ? Math.min(...limits) : null;
}

export function costsOf(
  rows: readonly DraftRow[],
  quantity: number,
  price: number | undefined,
): LotCosts {
  const materials = Math.round(rows.reduce((sum, row) => sum + row.cost, 0));
  const perUnit = quantity > 0 ? Math.round(materials / quantity) : 0;
  return {
    materials,
    perUnit,
    margin: price && perUnit ? Math.round(((price - perUnit) / price) * 100) : null,
  };
}
