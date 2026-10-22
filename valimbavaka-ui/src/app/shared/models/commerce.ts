/** Canal de vente (miroir de l'énumération backend SaleChannel). */
export type SaleChannel = 'SHOP' | 'FACEBOOK' | 'PHONE';

export const SALE_CHANNELS: readonly SaleChannel[] = ['SHOP', 'FACEBOOK', 'PHONE'];

/** Mode de paiement (miroir de l'énumération backend PaymentMethod). */
export type PaymentMethod = 'CASH' | 'MVOLA' | 'ORANGE_MONEY' | 'CARD' | 'ON_DELIVERY';
