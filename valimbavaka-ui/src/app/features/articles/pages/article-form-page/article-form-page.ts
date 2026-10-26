import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  ElementRef,
  OnInit,
  computed,
  inject,
  input,
  signal,
} from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import {
  AbstractControl,
  FormBuilder,
  FormControl,
  ReactiveFormsModule,
  ValidationErrors,
  Validators,
} from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { catchError, debounceTime, finalize, of } from 'rxjs';
import { Check } from 'lucide';
import { TranslatePipe } from '@ngx-translate/core';
import { errorMessage } from '../../../../core/http/problem-detail';
import { Icon } from '../../../../shared/components/icon/icon';
import { PageHeader } from '../../../../shared/components/page-header/page-header';
import { ToastService } from '../../../../shared/components/toast/toast-service';
import { ProductRange } from '../../../../shared/models/tone';
import {
  ArticleRDto,
  ArticleType,
  ArticleUnit,
  ArticleWDto,
} from '../../../article/data-access/article-models';
import { ArticleService } from '../../../article/data-access/article-service';
import { ReferentialService } from '../../../referential/data-access/referential-service';
import { SupplierService } from '../../../supplier/data-access/supplier-service';
import { ArticlePreview } from '../../components/article-preview/article-preview';
import { PhotoDrop } from '../../components/photo-drop/photo-drop';
import { ArticleDraftStore } from '../../data-access/article-draft-store';
import { ean13Validator } from '../../data-access/ean13';

const MAX_NAME_LENGTH = 120;
const DRAFT_DEBOUNCE_MS = 800;
const RANGE_TONES: readonly string[] = ['ZEZIKA', 'BOTANIKA', 'SOLIKA'];
const TYPES: readonly ArticleType[] = ['FINISHED', 'RAW_MATERIAL', 'SEMI_FINISHED'];
const UNITS: readonly ArticleUnit[] = ['PIECE', 'KILOGRAM', 'LITER'];
/** Une seule page de 100 fournisseurs suffit pour la liste de choix (comme les référentiels). */
const SUPPLIER_OPTIONS = { page: 1, size: 100, sortBy: 'name' } as const;

/** Champs dont l'erreur est affichée sous le contrôle. */
type CheckedField = 'name' | 'barcode' | 'price' | 'cost' | 'threshold';

/**
 * Fiche article (ART-01, ART-02, ART-04) : sections guidées, aperçu caisse en direct.
 * Sans `id` elle crée l'article (avec brouillon) ; avec `id` elle pré-remplit la fiche et la modifie.
 */
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
export class ArticleFormPage implements OnInit {
  private readonly articleService = inject(ArticleService);
  private readonly draftStore = inject(ArticleDraftStore);
  private readonly toast = inject(ToastService);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);
  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef);

  /** Identifiant lié depuis la route (:id/edit) ; absent en création. */
  readonly id = input<string>();

  protected readonly icons = { Check };
  protected readonly types = TYPES;
  protected readonly units = UNITS;
  protected readonly maxNameLength = MAX_NAME_LENGTH;
  protected readonly isEdit = computed(() => !!this.id());
  /** Listes de choix : en cas d'erreur la liste reste vide, le reste de la fiche reste utilisable. */
  protected readonly ranges = toSignal(
    inject(ReferentialService)
      .listProductRanges()
      .pipe(catchError(() => of([]))),
    { initialValue: [] },
  );
  private readonly supplierPage = toSignal(
    inject(SupplierService)
      .list(SUPPLIER_OPTIONS)
      .pipe(catchError(() => of(null))),
    { initialValue: null },
  );
  protected readonly savedAt = signal(this.draftStore.draft()?.savedAt ?? null);
  protected readonly isSaving = signal(false);
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly reference = signal('');

  /** Version de la fiche ouverte : le serveur refuse (409) si elle a changé entre-temps. */
  private version: number | null = null;
  /** Code-barres déjà enregistré : conservé tel quel même si sa clé de contrôle est invalide. */
  private loadedBarcode = '';
  /** Fournisseur déjà lié à la fiche ouverte : reste proposé même s'il a été désactivé depuis. */
  private readonly linkedSupplierId = signal<string | null>(null);
  private readonly sheetRead = signal(false);
  /** En modification, rien ne s'enregistre tant que la fiche n'est pas lue : un envoi l'écraserait. */
  protected readonly canSave = computed(
    () => !this.isSaving() && (!this.isEdit() || this.sheetRead()),
  );

  protected readonly form = inject(FormBuilder).nonNullable.group({
    // \S : un nom fait uniquement d'espaces est refusé ici comme par le serveur (@NotBlank).
    name: [
      '',
      [Validators.required, Validators.pattern(/\S/), Validators.maxLength(MAX_NAME_LENGTH)],
    ],
    // '' = aucune gamme : envoyé comme null au serveur.
    rangeId: ['', Validators.required],
    barcode: ['', (control: AbstractControl) => this.checkBarcode(control)],
    type: new FormControl<ArticleType>('FINISHED', { nonNullable: true }),
    price: new FormControl<number | null>(null, [Validators.required, Validators.min(0)]),
    cost: new FormControl<number | null>(null, Validators.min(0)),
    unit: new FormControl<ArticleUnit>('PIECE', { nonNullable: true }),
    threshold: new FormControl<number | null>(30, [Validators.required, Validators.min(0)]),
    supplierId: new FormControl<string | null>(null),
  });

  protected readonly value = toSignal(this.form.valueChanges, {
    initialValue: this.form.getRawValue(),
  });

  /** Fournisseurs actifs, plus celui déjà lié à la fiche. */
  protected readonly suppliers = computed(() => {
    const linked = this.linkedSupplierId();
    return (this.supplierPage()?.items ?? []).filter((s) => s.active || s.id === linked);
  });

  /** Gamme choisie, pour l'aperçu (couleur de la gamme) ; null hors des trois gammes connues. */
  protected readonly previewRange = computed<ProductRange | null>(() => {
    const code = this.ranges().find((range) => range.id === this.value().rangeId)?.code ?? '';
    return RANGE_TONES.includes(code) ? (code as ProductRange) : null;
  });

  constructor() {
    this.form.controls.type.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((type) => this.applyType(type));
    this.applyType(this.form.controls.type.value);
  }

  ngOnInit(): void {
    const id = this.id();
    if (id) {
      this.articleService
        .getById(id)
        .pipe(takeUntilDestroyed(this.destroyRef))
        .subscribe({
          next: (article) => this.fill(article),
          error: (error: unknown) => this.errorMessage.set(errorMessage(error)),
        });
      return;
    }
    this.restoreDraft();
    this.form.valueChanges
      .pipe(debounceTime(DRAFT_DEBOUNCE_MS), takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.saveDraft());
  }

  protected hasError(name: CheckedField): boolean {
    const control = this.form.controls[name];
    return control.invalid && (control.touched || control.dirty);
  }

  protected save(another: boolean): void {
    if (!this.canSave()) {
      return;
    }
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.host.nativeElement.querySelector<HTMLElement>('.ng-invalid:not(form)')?.focus();
      return;
    }
    this.isSaving.set(true);
    this.errorMessage.set(null);
    const id = this.id();
    const body = this.toDto();
    const request = id ? this.articleService.update(id, body) : this.articleService.create(body);
    request
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.isSaving.set(false)),
      )
      .subscribe({
        next: (article) => this.afterSave(article, another),
        error: (error: unknown) => this.errorMessage.set(errorMessage(error)),
      });
  }

  protected cancel(): void {
    this.draftStore.clear();
    void this.router.navigate(['/articles']);
  }

  protected demoAction(): void {
    this.toast.show('common.demoAction', {}, 'info');
  }

  private afterSave(article: ArticleRDto, another: boolean): void {
    this.draftStore.clear();
    const key = this.isEdit() ? 'articles.form.updated' : 'articles.form.created';
    this.toast.show(key, { reference: article.code, name: article.name });
    if (another) {
      this.form.reset();
      this.savedAt.set(null);
    } else {
      void this.router.navigate(['/articles']);
    }
  }

  /** Clé de contrôle EAN-13, sauf pour le code-barres déjà enregistré sur la fiche ouverte. */
  private checkBarcode(control: AbstractControl): ValidationErrors | null {
    const value = control.value as string;
    return value === this.loadedBarcode ? null : ean13Validator(control);
  }

  /** Gamme et prix n'ont de sens que pour un produit fini (vendu en caisse). */
  private applyType(type: ArticleType): void {
    const { rangeId, price } = this.form.controls;
    const sold = type === 'FINISHED';
    for (const control of [rangeId, price]) {
      if (sold) {
        control.enable({ emitEvent: false });
      } else {
        control.disable({ emitEvent: false });
      }
    }
  }

  private restoreDraft(): void {
    const draft = this.draftStore.draft();
    if (draft) {
      this.form.patchValue(draft.value);
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
      barcode: raw.barcode.replace(/\s/g, '') || null,
      type: raw.type,
      rangeId: sold ? raw.rangeId || null : null,
      unit: raw.unit,
      cost: raw.cost,
      price: sold ? raw.price : null,
      threshold: raw.threshold ?? 0,
      supplierId: raw.supplierId,
      version: this.version,
    };
  }

  private fill(article: ArticleRDto): void {
    this.version = article.version;
    this.loadedBarcode = article.barcode ?? '';
    this.reference.set(article.code);
    this.linkedSupplierId.set(article.supplierId);
    this.sheetRead.set(true);
    this.applyType(article.type);
    this.form.setValue({
      name: article.name,
      rangeId: article.rangeId ?? '',
      barcode: article.barcode ?? '',
      type: article.type,
      price: article.price,
      cost: article.cost,
      unit: article.unit,
      threshold: article.threshold,
      supplierId: article.supplierId,
    });
  }
}
