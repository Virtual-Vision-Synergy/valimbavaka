import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { UserRDto } from '../../../../models/user';
import { UserService } from '../../../../services/user-service';
import { extractErrorMessage } from '../../../../utils/api-utils';

const PAGE_SIZE = 10;

@Component({
  selector: 'app-user-list',
  imports: [RouterLink],
  templateUrl: './user-list.html',
  styleUrl: './user-list.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class UserList {
  private readonly userService = inject(UserService);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly users = signal<UserRDto[]>([]);
  protected readonly page = signal(0);
  protected readonly totalPages = signal(0);
  protected readonly totalElements = signal(0);
  protected readonly search = signal('');
  protected readonly isLoading = signal(false);
  protected readonly errorMessage = signal<string | null>(null);

  constructor() {
    this.loadUsers();
  }

  protected onSearch(term: string): void {
    this.search.set(term.trim());
    this.page.set(0);
    this.loadUsers();
  }

  protected goToPage(page: number): void {
    this.page.set(page);
    this.loadUsers();
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
        error: (error: unknown) => this.errorMessage.set(extractErrorMessage(error)),
      });
  }

  private reloadAfterDelete(): void {
    const isLastItemOfPage = this.users().length === 1 && this.page() > 0;
    if (isLastItemOfPage) {
      this.page.update((page) => page - 1);
    }
    this.loadUsers();
  }

  private loadUsers(): void {
    this.isLoading.set(true);
    this.errorMessage.set(null);
    this.userService
      .getAll({ search: this.search(), page: this.page(), size: PAGE_SIZE })
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.isLoading.set(false)),
      )
      .subscribe({
        next: (result) => {
          this.users.set(result.content);
          this.totalPages.set(result.totalPages);
          this.totalElements.set(result.totalElements);
        },
        error: (error: unknown) => this.errorMessage.set(extractErrorMessage(error)),
      });
  }
}
