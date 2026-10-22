import { ChangeDetectionStrategy, Component, inject, input, output } from '@angular/core';
import {
  Banknote,
  CreditCard,
  Minus,
  Plus,
  Smartphone,
  Tag,
  Trash2,
  UserRound,
  WalletCards,
} from 'lucide';
import { TranslatePipe } from '@ngx-translate/core';
import { Icon } from '../../../../shared/components/icon/icon';
import {
  SegmentOption,
  SegmentedControl,
} from '../../../../shared/components/segmented-control/segmented-control';
import { AriaryPipe } from '../../../../shared/pipes/ariary-pipe';
import { PosCart } from '../../data-access/pos-cart';
import { PaymentMethod, SaleChannel } from '../../data-access/pos-models';
import { KeypadKey, PosKeypad } from '../pos-keypad/pos-keypad';

const CHANNELS: readonly SegmentOption<SaleChannel>[] = [
  { value: 'SHOP', labelKey: 'pos.ticket.channel.SHOP' },
  { value: 'FACEBOOK', labelKey: 'pos.ticket.channel.FACEBOOK' },
  { value: 'PHONE', labelKey: 'pos.ticket.channel.PHONE' },
];

const PAYMENTS: readonly { value: PaymentMethod; icon: typeof Banknote }[] = [
  { value: 'CASH', icon: Banknote },
  { value: 'MVOLA', icon: Smartphone },
  { value: 'ORANGE_MONEY', icon: WalletCards },
  { value: 'CARD', icon: CreditCard },
];

/** Montant maximal saisissable au pavé (garde-fou contre une frappe en rafale). */
const MAX_RECEIVED = 99_999_999;

/** Ticket en cours (VTE-03 à VTE-06) : lignes, canal, remises, paiement et encaissement. */
@Component({
  selector: 'vvs-pos-ticket',
  imports: [TranslatePipe, AriaryPipe, Icon, SegmentedControl, PosKeypad],
  templateUrl: './pos-ticket.html',
  styleUrl: './pos-ticket.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PosTicket {
  protected readonly icons = { Minus, Plus, Tag, Trash2, UserRound };
  protected readonly cart = inject(PosCart);

  readonly ticketNumber = input.required<number>();
  readonly cashierName = input.required<string>();
  readonly openedAt = input.required<string>();
  readonly checkout = output();
  readonly hold = output();
  readonly promoCode = output();
  readonly customer = output();

  protected readonly channels = CHANNELS;
  protected readonly payments = PAYMENTS;
  protected readonly channel = this.cart.channel;

  protected selectPayment(method: PaymentMethod): void {
    this.cart.payment.set(method);
  }

  /** Saisie libre du montant reçu : seuls les chiffres comptent (« 60 000 » → 60000). */
  protected typeReceived(value: string): void {
    const digits = value.replace(/\D/g, '');
    this.cart.received.set(Math.min(MAX_RECEIVED, Number(digits || '0')));
  }

  protected pressKey(key: KeypadKey): void {
    const current = this.cart.received();
    if (key === 'BACK') {
      this.cart.received.set(Math.floor(current / 10));
      return;
    }
    const next = Number(`${current === 0 ? '' : current}${key}`);
    this.cart.received.set(Math.min(MAX_RECEIVED, next));
  }
}
