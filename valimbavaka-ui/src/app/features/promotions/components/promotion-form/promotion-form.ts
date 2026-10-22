import {
  ChangeDetectionStrategy,
  Component,
  computed,
  inject,
  input,
  output,
  signal,
} from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import {
  AbstractControl,
  FormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  Validators,
} from '@angular/forms';
import { TranslatePipe } from '@ngx-translate/core';
import { isoDay } from '../../../../shared/data-access/demo-dates';
import {
  SegmentOption,
  SegmentedControl,
} from '../../../../shared/components/segmented-control/segmented-control';
import { SALE_CHANNELS } from '../../../../shared/models/commerce';
import {
  PROMOTION_KINDS,
  PromotionKind,
  PromotionScopeRDto,
  PromotionWDto,
} from '../../data-access/promotion-models';

/** La fin ne précède pas le début ; au moins un canal est coché. */
function validPeriod(group: AbstractControl): ValidationErrors | null {
  const { startsOn, endsOn } = group.value as { startsOn: string; endsOn: string };
  return startsOn && endsOn && endsOn < startsOn ? { period: true } : null;
}

function someChannel(group: AbstractControl): ValidationErrors | null {
  return Object.values(group.value as Record<string, boolean>).some(Boolean)
    ? null
    : { channels: true };
}

/** Création d'une promotion (PRO-02) : un seul formulaire, la prévisualisation du statut vient des dates. */
@Component({
  selector: 'vvs-promotion-form',
  imports: [ReactiveFormsModule, TranslatePipe, SegmentedControl],
  templateUrl: './promotion-form.html',
  styleUrl: './promotion-form.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PromotionForm {
  private readonly formBuilder = inject(FormBuilder).nonNullable;

  readonly scopes = input.required<readonly PromotionScopeRDto[]>();
  readonly submitted = output<PromotionWDto>();
  readonly cancelled = output<void>();

  protected readonly channels = SALE_CHANNELS;
  protected readonly kind = signal<PromotionKind>('PERCENT');
  protected readonly kindOptions: readonly SegmentOption<PromotionKind>[] = PROMOTION_KINDS.map(
    (value) => ({
      value,
      labelKey: `promotions.kind.${value}`,
    }),
  );
  protected readonly attempted = signal(false);

  protected readonly form = this.formBuilder.group(
    {
      name: ['', [Validators.required, Validators.maxLength(60)]],
      value: [20, [Validators.required, Validators.min(0)]],
      code: ['', [Validators.pattern(/^[A-Z0-9]{3,12}$/)]],
      scope: ['ALL' as PromotionWDto['scope']],
      startsOn: [isoDay(1), Validators.required],
      endsOn: [isoDay(31), Validators.required],
      channels: this.formBuilder.group(
        { SHOP: true, FACEBOOK: true, PHONE: false },
        { validators: someChannel },
      ),
      limit: [null as number | null, Validators.min(1)],
    },
    { validators: validPeriod },
  );

  private readonly formValue = toSignal(this.form.valueChanges, {
    initialValue: this.form.getRawValue(),
  });
  protected readonly valueMax = computed(() => (this.kind() === 'PERCENT' ? 100 : null));
  protected readonly valueUnit = computed(() => `promotions.valueUnit.${this.kind()}`);
  protected readonly invalidPeriod = computed(() => {
    this.formValue();
    return this.form.hasError('period');
  });

  protected submit(): void {
    this.attempted.set(true);
    if (this.form.invalid || (this.kind() === 'PERCENT' && this.form.controls.value.value > 100)) {
      this.form.markAllAsTouched();
      return;
    }
    const raw = this.form.getRawValue();
    this.submitted.emit({
      name: raw.name.trim(),
      kind: this.kind(),
      value: raw.value,
      code: raw.code || null,
      scope: raw.scope,
      startsOn: raw.startsOn,
      endsOn: raw.endsOn,
      channels: this.channels.filter((channel) => raw.channels[channel]),
      limit: raw.limit,
    });
  }

  protected reset(): void {
    this.form.reset();
    this.kind.set('PERCENT');
    this.attempted.set(false);
    this.cancelled.emit();
  }
}
