import { ReportPointRDto, ReportRDto } from './report-models';

/** Chiffre d'affaires d'un jour de démonstration : creux en début de semaine, pic le week-end. */
function dayRevenue(day: number, seed: number): number {
  const wave = 0.72 + 0.28 * Math.sin(day * 0.9 + seed);
  return Math.round((1_050_000 * wave) / 1000) * 1000;
}

function points(count: number, todayIndex: number): readonly ReportPointRDto[] {
  return Array.from({ length: count }, (_, index) => ({
    label: String(index + 1),
    current: dayRevenue(index + 1, 0),
    previous: dayRevenue(index + 1, 1.6) * 0.92,
    today: index === todayIndex,
  }));
}

export const REPORT_FIXTURE: ReportRDto = {
  kpis: {
    revenue: { value: 24_600_000, previous: 21_540_000 },
    sales: { value: 912, previous: 844 },
    basket: { value: 26_970, previous: 25_540 },
    delivery: { value: 91, previous: 93 },
  },
  points: points(22, 21),
  ranges: [
    { range: 'BOTANIKA', amount: 11_300_000 },
    { range: 'ZEZIKA', amount: 7_900_000 },
    { range: 'SOLIKA', amount: 5_400_000 },
  ],
  top: [
    { name: 'Savon noir 100 g', units: 214, revenue: 1_820_000 },
    { name: 'Huile de coco vierge', units: 168, revenue: 2_100_000 },
    { name: 'Gommage café 200 g', units: 131, revenue: 1_640_000 },
    { name: 'Beurre de karité 150 g', units: 97, revenue: 1_460_000 },
    { name: 'Baume lèvres miel 15 g', units: 88, revenue: 400_000 },
  ],
};

export const REPORT_SELLERS: readonly string[] = ['Miora Andriamasy', 'Lova Rakoto', 'Fara Razafy'];
