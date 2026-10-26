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
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { finalize } from 'rxjs';
import { errorMessage } from '../../../../core/http/problem-detail';
import { SupplierRDto, SupplierType } from '../../data-access/supplier-models';
import { SupplierService } from '../../data-access/supplier-service';

/** Fiche fournisseur (FRS-01) : création et modification de tous les champs (FRS-02). */
@Component({
  selector: 'vvs-supplier-form-page',
  imports: [ReactiveFormsModule, RouterLink, TranslatePipe],
  templateUrl: './supplier-form-page.html',
  styleUrl: './supplier-form-page.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SupplierFormPage implements OnInit {
  private readonly supplierService = inject(SupplierService);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  /** Identifiant lié depuis la route (:id/edit) ; absent en création. */
  readonly id = input<string>();

  protected readonly isEdit = computed(() => !!this.id());
  protected readonly types: readonly SupplierType[] = ['LOCAL', 'FOREIGN'];
  protected readonly isSaving = signal(false);
  protected readonly errorMessage = signal<string | null>(null);
  /** Version de la fiche ouverte : le serveur refuse (409) si elle a changé entre-temps. */
  private version: number | null = null;

  protected readonly form = inject(FormBuilder).nonNullable.group({
    // \S : un nom fait uniquement d'espaces est refusé ici comme par le serveur (@NotBlank).
    name: ['', [Validators.required, Validators.pattern(/\S/), Validators.maxLength(150)]],
    type: ['LOCAL' as SupplierType, Validators.required],
    contactName: ['', Validators.maxLength(150)],
    phone: ['', Validators.maxLength(50)],
    email: ['', [Validators.email, Validators.maxLength(255)]],
    address: ['', Validators.maxLength(500)],
    notes: ['', Validators.maxLength(2000)],
  });

  ngOnInit(): void {
    const id = this.id();
    if (!id) {
      return;
    }
    this.supplierService
      .getById(id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (supplier) => this.fill(supplier),
        error: (error: unknown) => this.errorMessage.set(errorMessage(error)),
      });
  }

  protected isInvalid(field: keyof typeof this.form.controls): boolean {
    const control = this.form.controls[field];
    return control.invalid && control.touched;
  }

  protected save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.isSaving.set(true);
    this.errorMessage.set(null);
    const id = this.id();
    const body = { ...this.form.getRawValue(), version: this.version };
    const request = id ? this.supplierService.update(id, body) : this.supplierService.create(body);
    request
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.isSaving.set(false)),
      )
      .subscribe({
        next: () => void this.router.navigate(['/suppliers']),
        error: (error: unknown) => this.errorMessage.set(errorMessage(error)),
      });
  }

  private fill(supplier: SupplierRDto): void {
    this.version = supplier.version;
    this.form.setValue({
      name: supplier.name,
      type: supplier.type,
      contactName: supplier.contactName ?? '',
      phone: supplier.phone ?? '',
      email: supplier.email ?? '',
      address: supplier.address ?? '',
      notes: supplier.notes ?? '',
    });
  }
}
