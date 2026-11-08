import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  OnInit,
  computed,
  inject,
  input,
  signal,
} from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { finalize } from 'rxjs';
import { errorMessage } from '../../../../core/http/problem-detail';
import { ToastService } from '../../../../shared/components/toast/toast-service';
import { ReferentialService } from '../../../referential/data-access/referential-service';
import {
  COMMISSION_BASES,
  CommissionBase,
  CommissionRuleRDto,
  MAX_COMMISSION_RATE,
  MAX_RULE_NAME_LENGTH,
  MIN_COMMISSION_RATE,
} from '../../data-access/commission-rule-models';
import { CommissionRuleService } from '../../data-access/commission-rule-service';

const RULES_PATH = '/commissions/rules';

/** Clé de traduction et paramètres du message d'erreur d'un champ. */
interface FieldError {
  key: string;
  params?: { max: number };
}

/**
 * Fiche d'une règle de commission (COM-01) : nom, canal, taux et base de calcul. Le serveur refuse (409) une
 * deuxième règle active sur le même canal et (422) un canal inconnu ou inactif.
 */
@Component({
  selector: 'vvs-commission-rule-form-page',
  imports: [ReactiveFormsModule, RouterLink, TranslatePipe],
  templateUrl: './commission-rule-form-page.html',
  styleUrl: './commission-rule-form-page.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CommissionRuleFormPage implements OnInit {
  private readonly ruleService = inject(CommissionRuleService);
  private readonly router = inject(Router);
  private readonly toast = inject(ToastService);
  private readonly destroyRef = inject(DestroyRef);

  /** Identifiant lié depuis la route (:id/edit) ; absent en création. */
  readonly id = input<string>();

  protected readonly rulesPath = RULES_PATH;
  protected readonly bases = COMMISSION_BASES;
  protected readonly salesChannels = toSignal(inject(ReferentialService).listSalesChannels(), {
    initialValue: [],
  });
  protected readonly isEdit = computed(() => !!this.id());
  protected readonly isSaving = signal(false);
  protected readonly errorMessage = signal<string | null>(null);
  /** Version de la règle ouverte : le serveur refuse (409) si elle a changé entre-temps. */
  private version: number | null = null;
  private readonly ruleRead = signal(false);
  /** En modification, rien ne s'enregistre tant que la règle n'est pas lue : un envoi l'écraserait. */
  protected readonly canSave = computed(
    () => !this.isSaving() && (!this.isEdit() || this.ruleRead()),
  );

  protected readonly form = inject(FormBuilder).nonNullable.group({
    // \S : un nom fait uniquement d'espaces est refusé ici comme par le serveur (@NotBlank).
    name: [
      '',
      [Validators.required, Validators.pattern(/\S/), Validators.maxLength(MAX_RULE_NAME_LENGTH)],
    ],
    salesChannelId: ['', Validators.required],
    // Mêmes bornes que le serveur : 0 à 100, deux décimales au plus.
    rate: [
      null as number | null,
      [
        Validators.required,
        Validators.min(MIN_COMMISSION_RATE),
        Validators.max(MAX_COMMISSION_RATE),
        Validators.pattern(/^\d{1,3}(\.\d{1,2})?$/),
      ],
    ],
    base: ['REVENUE' as CommissionBase, Validators.required],
  });

  ngOnInit(): void {
    const id = this.id();
    if (!id) {
      return;
    }
    this.ruleService
      .getById(id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (rule) => this.fill(rule),
        error: (error: unknown) => this.errorMessage.set(errorMessage(error)),
      });
  }

  /** Message à afficher sous un champ touché et invalide, ou null : mêmes règles que le serveur. */
  protected fieldError(field: keyof typeof this.form.controls): FieldError | null {
    const control = this.form.controls[field];
    if (!control.invalid || !control.touched) {
      return null;
    }
    if (control.hasError('maxlength')) {
      return { key: 'commissionRule.form.tooLong', params: { max: MAX_RULE_NAME_LENGTH } };
    }
    const keys: Record<typeof field, string> = {
      name: 'commissionRule.form.nameRequired',
      salesChannelId: 'commissionRule.form.channelRequired',
      rate: 'commissionRule.form.rateInvalid',
      base: 'commissionRule.form.baseRequired',
    };
    return { key: keys[field] };
  }

  protected save(): void {
    if (!this.canSave()) {
      return;
    }
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.isSaving.set(true);
    this.errorMessage.set(null);
    const id = this.id();
    const { name, salesChannelId, rate, base } = this.form.getRawValue();
    const body = { name, salesChannelId, rate: Number(rate), base, version: this.version };
    const request = id ? this.ruleService.update(id, body) : this.ruleService.create(body);
    request
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.isSaving.set(false)),
      )
      .subscribe({
        next: (result) => {
          if (result.queued) {
            this.toast.show('common.queuedSaved', {}, 'info');
          }
          void this.router.navigate([RULES_PATH]);
        },
        error: (error: unknown) => this.errorMessage.set(errorMessage(error)),
      });
  }

  private fill(rule: CommissionRuleRDto): void {
    this.version = rule.version;
    this.ruleRead.set(true);
    this.form.setValue({
      name: rule.name,
      salesChannelId: rule.salesChannelId,
      rate: rule.rate,
      base: rule.base,
    });
  }
}
