import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { timeout } from 'rxjs/operators';

interface UserInfo {
  authenticated: boolean;
  username?: string;
  email?: string;
}

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div style="font-family: sans-serif; max-width: 860px; margin: 48px auto; padding: 0 24px;">
      <h1 style="border-bottom: 2px solid #333; padding-bottom: 12px;">
        Microservices BFF Test
      </h1>

      <p *ngIf="loading" style="color: #666;">Yükleniyor...</p>

      <ng-container *ngIf="!loading">

        <!-- Giriş yapılmamış -->
        <div *ngIf="!user?.authenticated"
             style="padding: 24px; border: 1px solid #ddd; border-radius: 8px; max-width: 360px;">
          <p *ngIf="initError" style="color:#c5221f; margin: 0 0 12px; font-size:13px;">
            Bağlantı hatası: {{ initError }}
          </p>
          <p style="margin: 0 0 16px;">Devam etmek için Keycloak ile giriş yapın.</p>
          <button (click)="login()"
                  style="padding: 10px 20px; background: #1a73e8; color: #fff;
                         border: none; border-radius: 4px; cursor: pointer; font-size: 14px;">
            Keycloak ile Giriş Yap
          </button>
        </div>

        <!-- Giriş yapılmış -->
        <div *ngIf="user?.authenticated">
          <p>Hoşgeldiniz, <strong>{{ user?.username }}</strong>
            <span style="color:#666; font-size:13px;"> ({{ user?.email }})</span>
          </p>
          <button (click)="logout()"
                  style="padding: 8px 16px; background: #d93025; color: #fff;
                         border: none; border-radius: 4px; cursor: pointer; font-size: 13px;">
            Çıkış Yap
          </button>

          <hr style="margin: 28px 0;">

          <h2 style="margin-top: 0;">GET /api/v1/products/hello</h2>
          <button (click)="fetchProducts()" [disabled]="productsLoading"
                  style="padding: 10px 20px; background: #188038; color: #fff;
                         border: none; border-radius: 4px; cursor: pointer; font-size: 14px;">
            {{ productsLoading ? 'Yükleniyor…' : 'İsteği Gönder' }}
          </button>

          <div *ngIf="productsError"
               style="margin-top: 16px; padding: 12px; background: #fce8e6;
                      border-radius: 4px; color: #c5221f;">
            Hata: {{ productsError }}
          </div>

          <pre *ngIf="productsData !== null"
               style="margin-top: 16px; padding: 16px; background: #f8f9fa;
                      border: 1px solid #dadce0; border-radius: 4px;
                      white-space: pre-wrap; word-break: break-all;">{{ productsData }}</pre>
        </div>

      </ng-container>
    </div>
  `,
})
export class AppComponent implements OnInit {
  loading = true;
  user: UserInfo | null = null;
  initError: string | null = null;
  productsLoading = false;
  productsData: string | null = null;
  productsError: string | null = null;

  constructor(private http: HttpClient) {}

  ngOnInit(): void {
    this.http
      .get<UserInfo>('/me', { headers: { Accept: 'application/json' } })
      .pipe(timeout(8000))
      .subscribe({
        next: (user) => {
          this.user = user;
          this.loading = false;
        },
        error: (err) => {
          // 401 → giriş yapılmamış (beklenen durum)
          // diğer hatalar → BFF ulaşılamıyor veya proxy sorunu
          this.initError = err.name === 'TimeoutError'
            ? 'BFF yanıt vermedi (8 s). BFF sunucusu çalışıyor mu?'
            : err.status
              ? `HTTP ${err.status}`
              : 'BFF\'e bağlanılamadı — proxy veya BFF çalışıyor mu?';
          this.user = { authenticated: false };
          this.loading = false;
        },
      });
  }

  login(): void {
    window.location.href = '/oauth2/authorization/keycloak';
  }

  logout(): void {
    this.http
      .post('/logout', {}, {
        observe: 'response',
        headers: { Accept: 'application/json' },
      })
      .subscribe({
        next: (response) => {
          const location = response.headers.get('Location');
          window.location.href = location ?? '/';
        },
        error: () => {
          window.location.href = '/';
        },
      });
  }

  fetchProducts(): void {
    this.productsLoading = true;
    this.productsError = null;
    this.productsData = null;

    this.http
      .get('/api/v1/products/hello', { responseType: 'text' })
      .subscribe({
        next: (data) => {
          this.productsData = data;
          this.productsLoading = false;
        },
        error: (err) => {
          this.productsError = `HTTP ${err.status} — ${err.message}`;
          this.productsLoading = false;
        },
      });
  }
}
