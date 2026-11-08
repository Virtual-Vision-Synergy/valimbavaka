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
import {
  AbstractControl,
  FormArray,
  FormBuilder,
  FormControl,
  FormGroup,
  NonNullableFormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  Validators,
} from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { Plus, Trash2 } from 'lucide';
import { TranslatePipe } from '@ngx-translate/core';
import { finalize, map } from 'rxjs';
import { errorMessage } from '../../../../core/http/problem-detail';
import { Icon } from '../../../../shared/components/icon/icon';
import { ToastService } from '../../../../shared/components/toast/toast-service';
import { ArticleRDto } from '../../../article/data-access/article-models';
import { ArticleService } from '../../../article/data-access/article-service';
import {
  MAX_COMMISSION_RATE,
  MAX_CONSUMED_PRODUCTS,
  MAX_DURATION_MINUTES,
  MAX_SALON_SERVICE_NAME_LENGTH,
  MIN_DURATION_MINUTES,
  SalonServiceRDto,
  SalonServiceWDto,
} from '../../data-access/salon-catalog-models';
import { SalonCatalogService } from '../../data-access/salon-catalog-service';

const CATALOG_PATH = '/services/catalog';
/** Articles proposés : actifs, triés par nom ; le serveur plafonne une page à 100. */
const ARTICLE_QUERY = {
  page: 1,
  size: 100,
  active: true,
  sortBy: 'name',
  sortDirection: 'ASC',
} as const;

/** Clé de traduction et paramètres du message d'erreur d'un champ. */
interface FieldError {
  key: string;
  params?: { max: number };
}

type ScalarField = 'name' | 'durationMinutes' | 'price' | 'commissionRate';

/** Ligne « produit consommé » : article (UUID) et quantité dans son unité, 3 décimales au plus. */
type ProductLine = FormGroup<{
  articleId: FormControl<string>;
  quantity: FormControl<number | null>;
}>;

function productLine(
  fb: NonNullableFormBuilder,
  articleId: string,
  quantity: number | null,
): ProductLine {
  return fb.group({
    articleId: [articleId, Validators.required],
    quantity: [
      quantity,
      [Validators.required, Validators.min(0.001), Validators.pattern(/^\d{1,9}(\.\d{1,3})?$/)],
    ],
  });
}

/** Un même article ne peut figurer qu'une fois dans la liste des produits consommés (422 côté serveur). */
function distinctArticles(control: AbstractControl): ValidationErrors | null {
  const ids = (control.value as { articleId: string }[]).map((line) => line.articleId);
  return new Set(ids).size === ids.length ? null : { duplicateArticle: true };
}

/**
 * Fiche d'un service salon (SRV-01) : nom, durée, prix, taux de commission facultatif (prioritaire sur le canal et
 * la vendeuse, sans cumul) et produits habituellement consommés, repris par l'enregistrement d'une prestation.
 */
@Component({
  selector: 'vvs-salon-catalog-form-page',
  imports: [ReactiveFormsModule, RouterLink, TranslatePipe, Icon],
  templateUrl: './salon-catalog-form-page.html',
  styleUrl: './salon-catalog-form-page.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SalonCatalogFormPage implements OnInit {
  private readonly catalogService = inject(SalonCatalogService);
  private readonly router = inject(Router);
  private readonly toast = inject(ToastService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly fb = inject(FormBuilder).nonNullable;

  /** Identifiant lié depuis la route (:id/edit) ; absent en création. */
  readonly id = input<string>();

  protected readonly icons = { Plus, Trash2 };
  protected readonly catalogPath = CATALOG_PATH;
  protected readonly maxProducts = MAX_CONSUMED_PRODUCTS;
  protected readonly articles = toSignal(
    inject(ArticleService)
      .list(ARTICLE_QUERY)
      .pipe(map((page) => page.items)),
    { initialValue: [] as ArticleRDto[] },
  );
  private readonly articlesById = computed(
    () => new Map(this.articles().map((article) => [article.id, article])),
  );
  protected readonly isEdit = computed(() => !!this.id());
  protected readonly isSaving = signal(false);
  protected readonly errorMessage = signal<string | null>(null);
  /** Version du service ouvert : le serveur refuse (409) s'il a changé entre-temps. */
  private version: number | null = null;
  private readonly serviceRead = signal(false);
  /** En modification, rien ne s'enregistre tant que le service n'est pas lu : un envoi l'écraserait. */
  protected readonly canSave = computed(
    () => !this.isSaving() && (!this.isEdit() || this.serviceRead()),
  );

  protected readonly form = this.fb.group({
    // \S : un nom fait uniquement d'espaces est refusé ici comme par le serveur (@NotBlank).
    name: [
      '',
      [
        Validators.required,
        Validators.pattern(/\S/),
        Validators.maxLength(MAX_SALON_SERVICE_NAME_LENGTH),
      ],
    ],
    durationMinutes: [
      null as number | null,
      [
        Validators.required,
        Validators.min(MIN_DURATION_MINUTES),
        Validators.max(MAX_DURATION_MINUTES),
        Validators.pattern(/^\d+$/),
      ],
    ],
    price: [
      null as number | null,
      [Validators.required, Validators.min(0), Validators.pattern(/^\d{1,12}(\.\d{1,2})?$/)],
    ],
    // Facultatif : vide = le taux du canal, sinon celui de la vendeuse, s'applique.
    commissionRate: [
      null as number | null,
      [
        Validators.min(0),
        Validators.max(MAX_COMMISSION_RATE),
        Validators.pattern(/^\d{1,3}(\.\d{1,2})?$/),
      ],
    ],
    products: this.fb.array<ProductLine>([], distinctArticles),
  });

  ngOnInit(): void {
    const id = this.id();
    if (!id) {
      return;
    }
    this.catalogService
      .getById(id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (service) => this.fill(service),
        error: (error: unknown) => this.errorMessage.set(errorMessage(error)),
      });
  }

  protected get products(): FormArray<ProductLine> {
    return this.form.controls.products;
  }

  /** Unité courte de l'article choisi (`u.`, `kg`, `L`), ou rien tant qu'aucun article n'est choisi. */
  protected unitKey(articleId: string): string | null {
    const unit = this.articlesById().get(articleId)?.unit;
    return unit ? `salonCatalog.unit.${unit}` : null;
  }

  /** Article déjà listé mais absent des articles actifs (désactivé depuis) : gardé, signalé comme inactif. */
  protected isUnlisted(articleId: string): boolean {
    return !!articleId && !this.articlesById().has(articleId);
  }

  protected addProduct(): void {
    if (this.products.length < MAX_CONSUMED_PRODUCTS) {
      this.products.push(productLine(this.fb, '', null));
    }
  }

  protected removeProduct(index: number): void {
    this.products.removeAt(index);
  }

  /** Message à afficher sous un champ touché et invalide, ou null : mêmes règles que le serveur. */
  protected fieldError(field: ScalarField): FieldError | null {
    const control = this.form.controls[field];
    if (!control.invalid || !control.touched) {
      return null;
    }
    if (control.hasError('maxlength')) {
      return { key: 'salonCatalog.form.tooLong', params: { max: MAX_SALON_SERVICE_NAME_LENGTH } };
    }
    const keys: Record<ScalarField, string> = {
      name: 'salonCatalog.form.nameRequired',
      durationMinutes: 'salonCatalog.form.durationInvalid',
      price: 'salonCatalog.form.priceInvalid',
      commissionRate: 'salonCatalog.form.rateInvalid',
    };
    return { key: keys[field] };
  }

  /** Vrai si une ligne de produit touchée est incomplète, ou si un article est listé deux fois. */
  protected productsInvalid(): boolean {
    return this.products.invalid && this.products.controls.some((line) => line.touched);
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
    const body = this.toBody();
    const request = id ? this.catalogService.update(id, body) : this.catalogService.create(body);
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
          void this.router.navigate([CATALOG_PATH]);
        },
        error: (error: unknown) => this.errorMessage.set(errorMessage(error)),
      });
  }

  private toBody(): SalonServiceWDto {
    const { name, durationMinutes, price, commissionRate, products } = this.form.getRawValue();
    const rate =
      commissionRate === null || `${commissionRate}` === '' ? null : Number(commissionRate);
    return {
      name,
      durationMinutes: Number(durationMinutes),
      price: Number(price),
      commissionRate: rate,
      products: products.map((line) => ({
        articleId: line.articleId,
        quantity: Number(line.quantity),
      })),
      version: this.version,
    };
  }

  private fill(service: SalonServiceRDto): void {
    this.version = service.version;
    this.serviceRead.set(true);
    this.products.clear();
    service.products.forEach((line) =>
      this.products.push(productLine(this.fb, line.articleId, line.quantity)),
    );
    this.form.patchValue({
      name: service.name,
      durationMinutes: service.durationMinutes,
      price: service.price,
      commissionRate: service.commissionRate,
    });
  }
}
