import { ReportDemoRDto } from './report-models';

export const REPORT_FIXTURE: ReportDemoRDto = {
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
