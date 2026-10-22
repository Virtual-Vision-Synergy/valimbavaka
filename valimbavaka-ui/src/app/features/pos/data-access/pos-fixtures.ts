import { DEMO_ARTICLES, demoArticle } from '../../../shared/data-access/demo-catalog';
import { PosArticleRDto, PosContextRDto } from './pos-models';

/** Articles vendables en boutique : produits finis, stock de l'emplacement du poste. */
const ARTICLES: readonly PosArticleRDto[] = DEMO_ARTICLES.flatMap((article) =>
  article.type === 'FINISHED' && article.range
    ? [
        {
          id: article.id,
          reference: article.reference,
          barcode: article.barcode,
          name: article.name,
          price: article.price,
          range: article.range,
          stock: article.stock.SHOP,
          favorite: article.favorite,
          box: article.box,
        },
      ]
    : [],
);

const SAVON_NOIR = demoArticle('ART-0031').id;
const GOMMAGE = demoArticle('ART-0087').id;

/** Ticket VTE-2452 de la maquette : 64 000 Ar, remise Octobre rose 5 700 Ar, net 58 300 Ar. */
export const POS_FIXTURE: PosContextRDto = {
  location: 'Boutique Analakely',
  cashierName: 'Miora A.',
  ticketNumber: 2452,
  openedAt: '10:52',
  heldTickets: 2,
  articles: ARTICLES,
  promotion: { code: 'PRO-02', name: 'Octobre rose', rate: 15, articleIds: [SAVON_NOIR, GOMMAGE] },
  openTicket: [
    { articleId: SAVON_NOIR, quantity: 3 },
    { articleId: demoArticle('ART-0154').id, quantity: 1 },
    { articleId: demoArticle('ART-0190').id, quantity: 1 },
    { articleId: GOMMAGE, quantity: 1 },
  ],
  openTicketReceived: 60000,
};
