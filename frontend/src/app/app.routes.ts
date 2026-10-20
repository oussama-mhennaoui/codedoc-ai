import { Routes } from '@angular/router';
import { authGuard } from './core/auth/auth.guard';
import { guestGuard } from './core/auth/guest.guard';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () => import('./features/landing/landing.component').then(m => m.LandingComponent)
  },
  {
    path: 'login',
    loadComponent: () => import('./features/auth/login/login.component').then(m => m.LoginComponent),
    canActivate: [guestGuard]
  },
  {
    path: 'register',
    loadComponent: () => import('./features/auth/register/register.component').then(m => m.RegisterComponent),
    canActivate: [guestGuard]
  },
  {
    path: 'dashboard',
    loadComponent: () => import('./features/dashboard/dashboard.component').then(m => m.DashboardComponent),
    canActivate: [authGuard]
  },
  {
    path: 'projects/:id',
    loadComponent: () => import('./features/project-view/project-view.component').then(m => m.ProjectViewComponent),
    canActivate: [authGuard]
  },
  {
    path: 'projects/:id/files/:fileId',
    loadComponent: () => import('./features/file-view/file-view.component').then(m => m.FileViewComponent),
    canActivate: [authGuard]
  },
  {
    path: 'projects/:id/files/:fileId/audit',
    loadComponent: () => import('./features/security-audit/security-audit.component').then(m => m.SecurityAuditComponent),
    canActivate: [authGuard]
  },
  {
    path: 'docs/:id',
    loadComponent: () => import('./features/doc-editor/doc-editor.component').then(m => m.DocEditorComponent),
    canActivate: [authGuard]
  },
  {
    path: 'unit-test-generator',
    loadComponent: () => import('./features/test-generator/test-generator.component').then(m => m.TestGeneratorComponent),
    canActivate: [authGuard]
  },
  {
    path: '**',
    redirectTo: ''
  }
];
