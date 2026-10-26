import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { Plus } from 'lucide';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { finalize } from 'rxjs';
import { HasPermissionDirective } from '../../../../core/auth/has-permission-directive';
import { errorMessage } from '../../../../core/http/problem-detail';
import { AvatarInitials } from '../../../../shared/components/avatar-initials/avatar-initials';
import { ConfirmDialog } from '../../../../shared/components/confirm-dialog/confirm-dialog';
import { Icon } from '../../../../shared/components/icon/icon';
import { ListState } from '../../../../shared/components/list-state/list-state';
import { PageHeader } from '../../../../shared/components/page-header/page-header';
import { Pagination } from '../../../../shared/components/pagination/pagination';
import { SearchField } from '../../../../shared/components/search-field/search-field';
import { StatusBadge } from '../../../../shared/components/status-badge/status-badge';
import { ToastService } from '../../../../shared/components/toast/toast-service';
import { Tone } from '../../../../shared/models/tone';
import { UserService } from '../../data-access/user-service';
import { RoleName, UserRDto, roleLabel } from '../../data-access/user-models';

const PAGE_SIZE = 10;

const ROLE_TONES: Readonly<Record<RoleName, Tone>> = {
  ADMIN: 'secondary',
  SELLER: 'warning',
  DELIVERY_DRIVER: 'info',
};

/**
 * Liste des comptes (AUTH-03) : recherche, pagination serveur, activation et désactivation confirmées.
 * Un compte ne se supprime jamais (AUTH-03, JNL-02) : il se désactive.
 */
@Component({
  selector: 'vvs-user-list-page',
  imports: [
    RouterLink,
    HasPermissionDirective,
    TranslatePipe,
    AvatarInitials,
    ConfirmDialog,
    Icon,
    ListState,
    PageHeader,
    Pagination,
    SearchField,
    StatusBadge,
  ],
  templateUrl: './user-list-page.html',
  styleUrl: './user-list-page.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class UserListPage {
  private readonly userService = inject(UserService);
  private readonly toast = inject(ToastService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly translate = inject(TranslateService);

  protected readonly icons = { Plus };
  protected readonly roleLabel = roleLabel;
  protected readonly roleTones = ROLE_TONES;
  protected readonly pageSize = PAGE_SIZE;
  protected readonly users = signal<UserRDto[]>([]);
  /** Page courante en base 1, comme le contrat de l'API. */
  protected readonly page = signal(1);
  protected readonly totalPages = signal(0);
  protected readonly totalCount = signal(0);
  protected readonly search = signal('');
  protected readonly isLoading = signal(false);
  protected readonly errorMessage = signal<string | null>(null);
  /** Compte dont le changement de statut attend la confirmation. */
  protected readonly pending = signal<UserRDto | null>(null);

  constructor() {
    this.loadUsers();
  }

  protected onSearch(term: string): void {
    this.search.set(term.trim());
    this.page.set(1);
    this.loadUsers();
  }

  protected goToPage(page: number): void {
    this.page.set(page);
    this.loadUsers();
  }

  protected ask(user: UserRDto): void {
    this.pending.set(user);
  }

  protected dismiss(): void {
    this.pending.set(null);
  }

  protected fullName(user: UserRDto): string {
    return `${user.firstName} ${user.lastName}`;
  }

  /** Textes du dialogue de confirmation selon le statut actuel du compte. */
  protected dialogTexts(user: UserRDto): {
    heading: string;
    message: string;
    confirm: string;
  } {
    const name = this.fullName(user);
    const key = user.active ? 'deactivate' : 'activate';
    return {
      heading: this.translate.instant(`user.confirm.${key}Title`),
      message: this.translate.instant(
        `user.status.confirm${key === 'activate' ? 'Activate' : 'Deactivate'}`,
        { name },
      ),
      confirm: this.translate.instant(`user.status.${key}`),
    };
  }

  protected confirm(): void {
    const user = this.pending();
    this.pending.set(null);
    if (!user) {
      return;
    }
    this.userService
      .setActive(user.id, !user.active)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (result) => {
          if (result.queued) {
            this.toast.show('common.queuedSaved', {}, 'info');
          }
          this.loadUsers();
        },
        error: (error: unknown) => this.errorMessage.set(errorMessage(error)),
      });
  }

  protected loadUsers(): void {
    this.isLoading.set(true);
    this.errorMessage.set(null);
    this.userService
      .list({ search: this.search(), page: this.page(), size: PAGE_SIZE })
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.isLoading.set(false)),
      )
      .subscribe({
        next: (result) => {
          this.users.set(result.items);
          this.totalPages.set(result.totalPages);
          this.totalCount.set(result.totalCount);
        },
        error: (error: unknown) => this.errorMessage.set(errorMessage(error)),
      });
  }
}
