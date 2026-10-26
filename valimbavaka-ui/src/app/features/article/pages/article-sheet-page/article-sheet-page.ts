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
import { FormBuilder, FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { catchError, finalize, of } from 'rxjs';
import { errorMessage } from '../../../../core/http/problem-detail';
import { ReferentialService } from '../../../referential/data-access/referential-service';
import { SupplierService } from '../../../supplier/data-access/supplier-service';
import {
  ArticleRDto,
  ArticleType,
  ArticleUnit,
  ArticleWDto,
} from '../../data-access/article-models';
import { ArticleService } from '../../data-access/article-service';

const MAX_NAME_LENGTH = 120;
const BARCODE_PATTERN = /^\d{13}$/;
/** Une seule page de 100 fournisseurs suffit pour la liste de choix (comme les référentiels). */
const SUPPLIER_OPTIONS = { page: 1, size: 100, sortBy: 'name' } as const;

/** Clé de traduction et paramètres du message d'erreur d'un champ. */
interface FieldError {
  key: string;
  params?: { max: number };
}

type SheetField = 'name' | 'barcode' | 'cost' | 'price' | 'threshold' | 'rangeId';

/**
 * Fiche article complète (ART-01) : code, nom, type, gamme, unité, coût, prix, seuil, fournisseur.
 * Sans `id` le formulaire crée l'article ; avec `id` il pré-remplit la fiche et la modifie (ART-02).
 */
@Component({
  selector: 'vvs-article-sheet-page',
  imports: [ReactiveFormsModule, RouterLink, TranslatePipe],
  templateUrl: './article-sheet-page.html',
  styleUrl: './article-sheet-page.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ArticleSheetPage implements OnInit {
  private readonly articleService = inject(ArticleService);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  /** Identifiant lié depuis la route (:id/edit) ; absent en création. */
  readonly id = input<string>();

  protected readonly isEdit = computed(() => !!this.id());
  private readonly sheetRead = signal(false);
  /** En modification, rien ne s'enregistre tant que la fiche n'est pas lue : un envoi l'écraserait. */
  protected readonly canSave = computed(
    () => !this.isSaving() && (!this.isEdit() || this.sheetRead()),
  );
  protected readonly types: readonly ArticleType[] = ['RAW_MATERIAL', 'SEMI_FINISHED', 'FINISHED'];
  protected readonly units: readonly ArticleUnit[] = ['PIECE', 'KILOGRAM', 'LITER'];
  protected readonly maxNameLength = MAX_NAME_LENGTH;
  /** Listes de choix : en cas d'erreur la liste reste vide, le reste de la fiche reste utilisable. */
  protected readonly ranges = toSignal(
    inject(ReferentialService)
      .listProductRanges()
      .pipe(catchError(() => of([]))),
    { initialValue: [] },
  );
  protected readonly suppliers = toSignal(
    inject(SupplierService)
      .list(SUPPLIER_OPTIONS)
      .pipe(catchError(() => of(null))),
    { initialValue: null },
  );
  protected readonly isSaving = signal(false);
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly code = signal<string | null>(null);
  /** Version de la fiche ouverte : le serveur refuse (409) si elle a changé entre-temps. */
  private version: number | null = null;
  /** Fournisseur déjà lié à la fiche ouverte : reste proposé même s'il a été désactivé depuis. */
  private readonly linkedSupplierId = signal<string | null>(null);

  protected readonly form = inject(FormBuilder).nonNullable.group({
    // \S : un nom fait uniquement d'espaces est refusé ici comme par le serveur (@NotBlank).
    name: [
      '',
      [Validators.required, Validators.pattern(/\S/), Validators.maxLength(MAX_NAME_LENGTH)],
    ],
    barcode: ['', Validators.pattern(BARCODE_PATTERN)],
    type: ['FINISHED' as ArticleType, Validators.required],
    // '' = aucune gamme : envoyé comme null au serveur.
    rangeId: ['', Validators.required],
    unit: ['PIECE' as ArticleUnit, Validators.required],
    cost: new FormControl<number | null>(null, Validators.min(0)),
    price: new FormControl<number | null>(null, [Validators.required, Validators.min(0)]),
    threshold: new FormControl<number | null>(0, [Validators.required, Validators.min(0)]),
    supplierId: [''],
  });

  /** Fournisseurs actifs, plus celui déjà lié à la fiche. */
  protected readonly supplierOptions = computed(() => {
    const linked = this.linkedSupplierId();
    return (this.suppliers()?.items ?? []).filter((s) => s.active || s.id === linked);
  });

  protected readonly isSold = signal(true);

  constructor() {
    this.applyType('FINISHED');
    this.form.controls.type.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((type) => this.applyType(type));
  }

  ngOnInit(): void {
    const id = this.id();
    if (!id) {
      return;
    }
    this.articleService
      .getById(id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (article) => this.fill(article),
        error: (error: unknown) => this.errorMessage.set(errorMessage(error)),
      });
  }

  /** Message à afficher sous un champ touché et invalide, ou null : mêmes règles que le serveur. */
  protected fieldError(field: SheetField): FieldError | null {
    const control = this.form.controls[field];
    if (!control.invalid || !control.touched) {
      return null;
    }
    const maxLength = control.getError('maxlength') as { requiredLength: number } | null;
    if (maxLength) {
      return { key: 'article.form.tooLong', params: { max: maxLength.requiredLength } };
    }
    if (control.hasError('min')) {
      return { key: 'article.form.negative' };
    }
    const isBarcode = field === 'barcode' && control.hasError('pattern');
    return { key: isBarcode ? 'article.form.barcodeInvalid' : 'article.form.required' };
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
    const body = this.toDto();
    const request = id ? this.articleService.update(id, body) : this.articleService.create(body);
    request
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.isSaving.set(false)),
      )
      .subscribe({
        next: () => void this.router.navigate(['/articles']),
        error: (error: unknown) => this.errorMessage.set(errorMessage(error)),
      });
  }

  /** Gamme et prix n'ont de sens que pour un produit fini (vendu en caisse). */
  private applyType(type: ArticleType): void {
    const { rangeId, price } = this.form.controls;
    const sold = type === 'FINISHED';
    this.isSold.set(sold);
    for (const control of [rangeId, price]) {
      if (sold) {
        control.enable({ emitEvent: false });
      } else {
        control.disable({ emitEvent: false });
      }
    }
  }

  private toDto(): ArticleWDto {
    const raw = this.form.getRawValue();
    const sold = raw.type === 'FINISHED';
    return {
      name: raw.name,
      barcode: raw.barcode.trim() || null,
      type: raw.type,
      rangeId: sold ? raw.rangeId || null : null,
      unit: raw.unit,
      cost: raw.cost,
      price: sold ? raw.price : null,
      threshold: raw.threshold ?? 0,
      supplierId: raw.supplierId || null,
      version: this.version,
    };
  }

  private fill(article: ArticleRDto): void {
    this.version = article.version;
    this.sheetRead.set(true);
    this.code.set(article.code);
    this.linkedSupplierId.set(article.supplierId);
    this.applyType(article.type);
    this.form.setValue({
      name: article.name,
      barcode: article.barcode ?? '',
      type: article.type,
      rangeId: article.rangeId ?? '',
      unit: article.unit,
      cost: article.cost,
      price: article.price,
      threshold: article.threshold,
      supplierId: article.supplierId ?? '',
    });
  }
}
