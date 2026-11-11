import { ClosingExtras } from './day-models';

/**
 * Caisse et liste de contrôle de la clôture, encore en données de démonstration : la réconciliation du stock
 * (JR-04) vient du serveur, la caisse et les points de contrôle viendront de JR-05 et JR-06.
 */
export function closingExtras(): ClosingExtras {
  return {
    cash: {
      SHOP: { expected: 486500, counted: 485000, otherMethods: 798500 },
      DELIVERY: { expected: 196000, counted: 196000, otherMethods: 53000 },
      SALON: { expected: 64000, counted: 64000, otherMethods: 25000 },
    },
    checklist: { deliveriesDone: 14, deliveriesTotal: 14, heldTickets: 0, lateShootings: 1 },
  };
}
