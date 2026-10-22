import { isoDay } from '../../../shared/data-access/demo-dates';
import {
  SalonProductRDto,
  ServicesContextRDto,
  ShootingRDto,
  TreatmentRDto,
} from './service-models';

const PRODUCTS: readonly SalonProductRDto[] = [
  { id: 'coco', name: 'Huile de coco vierge', unit: 'ml', stock: 220 },
  { id: 'argile', name: 'Masque argile rose 100 g', unit: 'u.', stock: 9 },
  { id: 'gommage', name: 'Gommage café 200 g', unit: 'g', stock: 600 },
  { id: 'karite', name: 'Beurre de karité 150 g', unit: 'g', stock: 400 },
  { id: 'ravintsara', name: 'Huile essentielle ravintsara', unit: 'ml', stock: 45 },
];

/** [id, nom, minutes, prix, [produit, quantité]…]. */
const TREATMENTS: readonly TreatmentRDto[] = [
  {
    id: 'face',
    name: 'Soin visage éclat',
    minutes: 45,
    price: 35000,
    recipe: [
      { productId: 'coco', quantity: 15 },
      { productId: 'argile', quantity: 1 },
    ],
  },
  {
    id: 'scrub',
    name: 'Gommage corps café',
    minutes: 30,
    price: 25000,
    recipe: [
      { productId: 'gommage', quantity: 80 },
      { productId: 'coco', quantity: 10 },
    ],
  },
  {
    id: 'massage',
    name: 'Massage huile coco',
    minutes: 60,
    price: 40000,
    recipe: [{ productId: 'coco', quantity: 40 }],
  },
  {
    id: 'hair',
    name: 'Soin cheveux ravintsara',
    minutes: 40,
    price: 30000,
    recipe: [
      { productId: 'ravintsara', quantity: 15 },
      { productId: 'karite', quantity: 20 },
    ],
  },
  { id: 'brows', name: 'Épilation sourcils', minutes: 15, price: 8000, recipe: [] },
  {
    id: 'nails',
    name: 'Manucure naturelle',
    minutes: 30,
    price: 15000,
    recipe: [{ productId: 'karite', quantity: 10 }],
  },
];

const SHOOTINGS: readonly ShootingRDto[] = [
  {
    number: 'SHO-0042',
    items: [
      { name: 'Gommage café 200 g', quantity: 1 },
      { name: 'Baume lèvres miel 15 g', quantity: 1 },
    ],
    takenBy: 'Miora A.',
    expectedReturn: `${isoDay(0)}T16:00`,
    status: 'OUT',
  },
  {
    number: 'SHO-0041',
    items: [{ name: 'Lait corporel vanille', quantity: 2 }],
    takenBy: 'Lova R.',
    expectedReturn: `${isoDay(-1)}T18:00`,
    status: 'LATE',
  },
  {
    number: 'SHO-0040',
    items: [{ name: 'Coffret Botanika', quantity: 1 }],
    takenBy: 'Miora A.',
    expectedReturn: `${isoDay(2)}T12:00`,
    status: 'OUT',
  },
  {
    number: 'SHO-0039',
    items: [{ name: 'Savon noir 100 g', quantity: 2 }],
    takenBy: 'Lova R.',
    expectedReturn: `${isoDay(-3)}T17:00`,
    status: 'RETURNED',
  },
];

export const SERVICES_FIXTURE: ServicesContextRDto = {
  treatments: TREATMENTS,
  products: PRODUCTS,
  staff: ['Fara Razafy', 'Lova Rakoto'],
  today: [
    { id: 's4', treatmentName: 'Soin visage éclat', time: '10:22', staff: 'Fara R.', price: 35000 },
    {
      id: 's3',
      treatmentName: 'Massage huile coco',
      time: '09:10',
      staff: 'Fara R.',
      price: 40000,
    },
    {
      id: 's2',
      treatmentName: 'Gommage corps café',
      time: '08:45',
      staff: 'Lova R.',
      price: 25000,
    },
    {
      id: 's1',
      treatmentName: 'Manucure naturelle',
      time: '08:15',
      staff: 'Lova R.',
      price: 12000,
    },
  ],
  shootings: SHOOTINGS,
};
