import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

export type AccountStatus = 'CREATED' | 'ACTIVATED' | 'SUSPENDED';

export interface Account {
  id: string;
  balance: number;
  currency: string;
  status: AccountStatus;
}

export interface AccountOperation {
  id: number;
  accountId: string;
  type: 'CREDIT' | 'DEBIT';
  amount: number;
  currency: string;
  occurredAt: string;
}

@Injectable({ providedIn: 'root' })
export class AccountApiService {
  private readonly http = inject(HttpClient);

  accounts(): Observable<Account[]> {
    return this.http.get<Account[]>('/queries/accounts');
  }

  account(id: string): Observable<Account> {
    return this.http.get<Account>(`/queries/accounts/${encodeURIComponent(id)}`);
  }

  operations(id: string): Observable<AccountOperation[]> {
    return this.http.get<AccountOperation[]>(`/queries/accounts/${encodeURIComponent(id)}/operations`);
  }

  create(initialBalance: number, currency: string): Observable<string> {
    return this.http.post('/commands/account/create', { initialBalance, currency }, { responseType: 'text' });
  }

  activate(id: string): Observable<void> {
    return this.http.post<void>(`/commands/account/${encodeURIComponent(id)}/activate`, {});
  }

  credit(id: string, amount: number, currency: string): Observable<void> {
    return this.http.post<void>(`/commands/account/${encodeURIComponent(id)}/credit`, { amount, currency });
  }

  debit(id: string, amount: number, currency: string): Observable<void> {
    return this.http.post<void>(`/commands/account/${encodeURIComponent(id)}/debit`, { amount, currency });
  }

  watch(id: string): EventSource {
    return new EventSource(`/queries/accounts/${encodeURIComponent(id)}/watch`);
  }
}
