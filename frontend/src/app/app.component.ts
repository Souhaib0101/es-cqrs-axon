import { CommonModule, CurrencyPipe, DatePipe } from '@angular/common';
import { Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { timer } from 'rxjs';
import { Account, AccountApiService, AccountOperation } from './account-api.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, FormsModule, CurrencyPipe, DatePipe],
  templateUrl: './app.component.html'
})
export class AppComponent implements OnInit {
  private readonly api = inject(AccountApiService);
  private readonly destroyRef = inject(DestroyRef);
  private watchConnection?: EventSource;

  readonly accounts = signal<Account[]>([]);
  readonly selectedId = signal('');
  readonly selectedAccount = signal<Account | null>(null);
  readonly operations = signal<AccountOperation[]>([]);
  readonly loading = signal(true);
  readonly serverReady = signal(false);
  readonly saving = signal(false);
  readonly error = signal('');
  readonly notice = signal('');
  readonly live = signal(false);
  readonly summaryCurrency = computed(() => this.selectedAccount()?.currency ?? this.accounts()[0]?.currency ?? 'MAD');
  readonly totalBalance = computed(() => this.accounts()
    .filter((account) => account.currency === this.summaryCurrency())
    .reduce((sum, account) => sum + Number(account.balance), 0));

  newBalance = 500;
  newCurrency = 'MAD';
  transactionAmount = 100;

  ngOnInit(): void {
    this.refreshAccounts();
    timer(2500, 4000).pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => this.refreshAccounts(false));
    this.destroyRef.onDestroy(() => this.stopWatching());
  }

  isActive(account: Account): boolean {
    return account.status === 'ACTIVATED';
  }

  refreshAccounts(showLoading = true): void {
    if (showLoading) this.loading.set(true);
    this.api.accounts().subscribe({
      next: (accounts) => {
        this.serverReady.set(true);
        if (showLoading) this.clearMessages();
        this.accounts.set(accounts);
        const currentId = this.selectedId();
        if (currentId) {
          const refreshed = accounts.find((account) => account.id === currentId);
          if (refreshed) this.selectedAccount.set(refreshed);
        } else if (accounts.length) {
          this.selectAccount(accounts[0]);
        }
        this.loading.set(false);
      },
      error: (error: unknown) => {
        this.serverReady.set(false);
        this.loading.set(false);
        if (showLoading) this.showError(error, 'Could not connect to the account service. Start the Spring Boot backend and refresh.');
      }
    });
  }

  selectAccount(account: Account): void {
    this.selectedId.set(account.id);
    this.selectedAccount.set(account);
    this.loadOperations(account.id);
    this.startWatching(account.id);
  }

  createAccount(): void {
    this.clearMessages();
    if (!Number.isFinite(this.newBalance) || this.newBalance < 0 || !this.newCurrency.trim()) {
      this.error.set('Enter a non-negative opening balance and a currency code.');
      return;
    }
    this.saving.set(true);
    this.api.create(this.newBalance, this.newCurrency.trim().toUpperCase()).subscribe({
      next: (id) => {
        this.saving.set(false);
        this.notice.set('Account created. Activate it to start making transactions.');
        this.refreshAccounts();
        // The query projection is updated asynchronously after the command is accepted.
        window.setTimeout(() => this.api.account(id).subscribe({
          next: (account) => { this.accounts.update((items) => [account, ...items.filter((item) => item.id !== id)]); this.selectAccount(account); },
          error: () => this.refreshAccounts(false)
        }), 500);
      },
      error: (error: unknown) => { this.saving.set(false); this.showError(error, 'Account could not be created.'); }
    });
  }

  activateAccount(): void {
    const account = this.selectedAccount();
    if (!account) return;
    this.runCommand(this.api.activate(account.id), 'Account activated.');
  }

  transact(type: 'credit' | 'debit'): void {
    const account = this.selectedAccount();
    if (!account) return;
    if (!Number.isFinite(this.transactionAmount) || this.transactionAmount <= 0) {
      this.error.set('Enter an amount greater than zero.');
      return;
    }
    const command = type === 'credit'
      ? this.api.credit(account.id, this.transactionAmount, account.currency)
      : this.api.debit(account.id, this.transactionAmount, account.currency);
    this.runCommand(command, type === 'credit' ? 'Credit submitted.' : 'Debit submitted.');
  }

  private runCommand(command: import('rxjs').Observable<void>, success: string): void {
    this.clearMessages();
    this.saving.set(true);
    command.subscribe({
      next: () => { this.saving.set(false); this.notice.set(success); this.loadSelectedAccount(); },
      error: (error: unknown) => { this.saving.set(false); this.showError(error, 'The command was rejected.'); }
    });
  }

  private loadSelectedAccount(): void {
    const id = this.selectedId();
    if (!id) return;
    this.api.account(id).subscribe({
      next: (account) => this.selectedAccount.set(account),
      error: () => undefined
    });
    this.loadOperations(id);
  }

  private loadOperations(id: string): void {
    this.api.operations(id).subscribe({ next: (items) => this.operations.set(items), error: () => this.operations.set([]) });
  }

  private startWatching(id: string): void {
    this.stopWatching();
    const source = this.api.watch(id);
    this.watchConnection = source;
    source.addEventListener('account', (event: MessageEvent<string>) => {
      if (!event.data || event.data === 'null') return;
      const account = JSON.parse(event.data) as Account;
      this.selectedAccount.set(account);
      this.live.set(true);
    });
    source.addEventListener('operation', (event: MessageEvent<string>) => {
      if (!event.data) return;
      const operation = JSON.parse(event.data) as AccountOperation;
      if (operation.accountId !== this.selectedId()) return;
      this.operations.update((items) => [operation, ...items.filter((item) => item.id !== operation.id)]);
      this.loadSelectedAccount();
      this.live.set(true);
    });
    source.onerror = () => this.live.set(false);
    this.live.set(false);
  }

  private stopWatching(): void {
    this.watchConnection?.close();
    this.watchConnection = undefined;
    this.live.set(false);
  }

  private clearMessages(): void { this.error.set(''); this.notice.set(''); }

  private showError(error: unknown, fallback: string): void {
    const detail = error as { error?: unknown; message?: string };
    const response = typeof detail?.error === 'string' ? detail.error :
      (detail?.error && typeof detail.error === 'object' && 'error' in detail.error
        ? String((detail.error as { error: unknown }).error)
        : detail?.message);
    this.error.set(response || fallback);
  }
}
