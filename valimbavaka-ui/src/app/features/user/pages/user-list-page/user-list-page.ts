import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { finalize } from 'rxjs';
import { HasPermissionDirective } from '../../../../core/auth/has-permission-directive';
import { errorMessage } from '../../../../core/http/problem-detail';
import { UserService } from '../../data-access/user-service';
import { UserRDto, roleLabel } from '../../data-access/user-models';

const PAGE_SIZE = 10;

@Component({
  selector: 'vvs-user-list-page',
  imports: [RouterLink, HasPermissionDirective, TranslatePipe],
  templateUrl: './user-list-page.html',
  styleUrl: './user-list-page.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class UserListPage {
  private readonly userService = inject(UserService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly translate = inject(TranslateService);

  protected readonly roleLabel = roleLabel;
  protected readonly users = signal<UserRDto[]>([]);
  /** Page courante en base 1, comme le contrat de l'API. */
  protected readonly page = signal(1);
  protected readonly totalPages = signal(0);
  protected readonly totalCount = signal(0);
  protected readonly search = signal('');
  protected readonly isLoading = signal(false);
  protected readonly errorMessage = signal<string | null>(null);

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

  protected toggleActive(user: UserRDto): void {
    const key = user.active ? 'user.status.confirmDeactivate' : 'user.status.confirmActivate';
    const name = `${user.firstName} ${user.lastName}`;
    if (!confirm(this.translate.instant(key, { name }))) {
      return;
    }
    this.userService
      .setActive(user.id, !user.active)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => this.loadUsers(),
        error: (error: unknown) => this.errorMessage.set(errorMessage(error)),
      });
  }

  protected deleteUser(user: UserRDto): void {
    if (!confirm(`Supprimer l'utilisateur ${user.firstName} ${user.lastName} ?`)) {
      return;
    }
    this.userService
      .delete(user.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => this.reloadAfterDelete(),
        error: (error: unknown) => this.errorMessage.set(errorMessage(error)),
      });
  }

  private reloadAfterDelete(): void {
    const isLastItemOfPage = this.users().length === 1 && this.page() > 1;
    if (isLastItemOfPage) {
      this.page.update((page) => page - 1);
    }
    this.loadUsers();
  }

  private loadUsers(): void {
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
