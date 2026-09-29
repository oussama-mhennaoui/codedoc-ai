import { computed, inject, Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { AuthResponse, LoginRequest, RegisterRequest, User } from '../models/user.model';
import { tap } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private http = inject(HttpClient);
  private readonly TOKEN_KEY = 'token';

  private _user = signal<User | null>(null);
  user = this._user.asReadonly();
  isAuthenticated = computed(() => !!this._user());

  login(credentials: LoginRequest) {
    return this.http.post<AuthResponse>(`${environment.apiUrl}/api/auth/login`, credentials)
      .pipe(
        tap(res => this.handleAuth(res))
      );
  }

  register(data: RegisterRequest) {
    return this.http.post<AuthResponse>(`${environment.apiUrl}/api/auth/register`, data)
      .pipe(
        tap(res => this.handleAuth(res))
      );
  }

  logout() {
    localStorage.removeItem(this.TOKEN_KEY);
    this._user.set(null);
  }

  loadMe() {
    const token = localStorage.getItem(this.TOKEN_KEY);
    if (!token) return;

    this.http.get<User>(`${environment.apiUrl}/api/auth/me`)
      .subscribe({
        next: (user) => this._user.set(user),
        error: () => this.logout()
      });
  }

  private handleAuth(res: AuthResponse) {
    localStorage.setItem(this.TOKEN_KEY, res.token);
    this._user.set(res.user);
  }

  getToken(): string | null {
    return localStorage.getItem(this.TOKEN_KEY);
  }
}
