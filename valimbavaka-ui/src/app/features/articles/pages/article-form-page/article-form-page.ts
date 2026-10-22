import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  ElementRef,
  inject,
  signal,
} from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { FormBuilder, FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { debounceTime } from 'rxjs';
import { Check } from 'lucide';
import { TranslatePipe } from '@ngx-translate/core';
import { Icon } from '../../../../shared/components/icon/icon';
import { PageHeader } from '../../../../shared/components/page-header/page-header';
import { ToastService } from '../../../../shared/components/toast/toast-service';
import { ProductRange } from '../../../../shared/models/tone';
import { ArticleDraftStore } from '../../data-access/article-draft-store';
import { ArticleType, ArticleUnit, ArticleWDto } from '../../data-access/article-models';
import { ArticleService } from '../../data-access/article-service';
import { ean13Validator } from '../../data-access/ean13';
import { ArticlePreview } from '../../components/article-preview/article-preview';
import { PhotoDrop } from '../../components/photo-drop/photo-drop';

const MAX_NAME_LENGTH = 120;
const DRAFT_DEBOUNCE_MS = 800;
const RANGES: readonly ProductRange[] = ['ZEZIKA', 'BOTANIKA', 'SOLIKA'];
const TYPES: readonly ArticleType[] = ['FINISHED', 'RAW_MATERIAL', 'PACKAGING'];
const UNITS: readonly ArticleUnit[] = ['PIECE', 'KILOGRAM', 'LITER'];

/** Champs dont l'erreur est affichée sous le contrôle. */
type CheckedField = 'name' | 'barcode' | 'price' | 'threshold';

/** Création d'article (ART-02 à ART-04) : sections guidées, aperçu caisse en direct, brouillon. */
@Component({
  selector: 'vvs-article-form-page',
  imports: [
    ReactiveFormsModule,
    RouterLink,
    TranslatePipe,
    Icon,
    PageHeader,
    ArticlePreview,
    PhotoDrop,
  ],
  templateUrl: './article-form-page.html',
  styleUrl: './article-form-page.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ArticleFormPage {
  private readonly articleService = inject(ArticleService);
  private readonly draftStore = inject(ArticleDraftStore);
  private readonly toast = inject(ToastService);
  private readonly router = inject(Router);
  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef);

  protected readonly icons = { Check };
  protected readonly ranges = RANGES;
  protected readonly types = TYPES;
  protected readonly units = UNITS;
  protected readonly maxNameLength = MAX_NAME_LENGTH;
  protected readonly reference = toSignal(this.articleService.nextReference(), {
    initialValue: '',
  });
  protected readonly suppliers = toSignal(this.articleService.suppliers(), { initialValue: [] });
  protected readonly savedAt = signal(this.draftStore.draft()?.savedAt ?? null);
  protected readonly isSaving = signal(false);

  protected readonly form = inject(FormBuilder).nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(MAX_NAME_LENGTH)]],
    range: new FormControl<ProductRange | null>('BOTANIKA', Validators.required),
    barcode: ['', ean13Validator],
    type: new FormControl<ArticleType>('FINISHED', { nonNullable: true }),
    manufactured: [true],
    salonUsable: [false],
    price: new FormControl<number | null>(null, [Validators.required, Validators.min(1)]),
    unit: new FormControl<ArticleUnit>('PIECE', { nonNullable: true }),
    threshold: new FormControl<number | null>(30, [Validators.required, Validators.min(0)]),
    supplierId: new FormControl<string | null>(null),
  });

  protected readonly value = toSignal(this.form.valueChanges, {
    initialValue: this.form.getRawValue(),
  });

  constructor() {
    const draft = this.draftStore.draft();
    if (draft) {
      this.form.patchValue(draft.value);
    }
    this.form.controls.type.valueChanges
      .pipe(takeUntilDestroyed(inject(DestroyRef)))
      .subscribe((type) => this.applyType(type));
    this.applyType(this.form.controls.type.value);
    this.form.valueChanges
      .pipe(debounceTime(DRAFT_DEBOUNCE_MS), takeUntilDestroyed(inject(DestroyRef)))
      .subscribe(() => this.saveDraft());
  }

  protected hasError(name: CheckedField): boolean {
    const control = this.form.controls[name];
    return control.invalid && (control.touched || control.dirty);
  }

  protected save(another: boolean): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.host.nativeElement.querySelector<HTMLElement>('.ng-invalid:not(form)')?.focus();
      return;
    }
    this.isSaving.set(true);
    this.articleService.create(this.toDto()).subscribe((reference) => {
      this.isSaving.set(false);
      this.draftStore.clear();
      this.toast.show('articles.form.created', { reference, name: this.form.getRawValue().name });
      if (another) {
        this.form.reset();
        this.savedAt.set(null);
      } else {
        void this.router.navigate(['/articles']);
      }
    });
  }

  protected cancel(): void {
    this.draftStore.clear();
    void this.router.navigate(['/articles']);
  }

  protected demoAction(): void {
    this.toast.show('common.demoAction', {}, 'info');
  }

  /** Gamme et prix n'ont de sens que pour un produit fini (vendu en caisse). */
  private applyType(type: ArticleType): void {
    const { range, price, manufactured } = this.form.controls;
    const sold = type === 'FINISHED';
    for (const control of [range, price]) {
      if (sold) {
        control.enable({ emitEvent: false });
      } else {
        control.disable({ emitEvent: false });
      }
    }
    if (!sold) {
      manufactured.setValue(false, { emitEvent: false });
    }
  }

  private saveDraft(): void {
    if (!this.form.dirty) {
      return;
    }
    const time = new Intl.DateTimeFormat('fr-FR', { timeStyle: 'short' }).format(new Date());
    this.draftStore.save(this.form.getRawValue(), time);
    this.savedAt.set(time);
  }

  private toDto(): ArticleWDto {
    const raw = this.form.getRawValue();
    const sold = raw.type === 'FINISHED';
    return {
      name: raw.name.trim(),
      type: raw.type,
      range: sold ? raw.range : null,
      barcode: raw.barcode.replace(/\s/g, '') || null,
      manufactured: raw.manufactured,
      salonUsable: raw.salonUsable,
      price: sold ? raw.price : null,
      unit: raw.unit,
      threshold: raw.threshold ?? 0,
      supplierId: raw.supplierId,
    };
  }
}
