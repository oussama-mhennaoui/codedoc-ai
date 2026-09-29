import { Routes } from '@angular/router';
import { authGuard } from './core/auth/auth.guard';

export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () => import('./features/auth/login/login.component').then(m => m.LoginComponent)
  },
  {
    path: 'register',
    loadComponent: () => import('./features/auth/register/register.component').then(m => m.RegisterComponent)
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
    path: 'docs/:id',
    loadComponent: () => import('./features/doc-editor/doc-editor.component').then(m => m.DocEditorComponent),
    canActivate: [authGuard]
  },
  {
    path: '',
    redirectTo: 'dashboard',
    pathMatch: 'full'
  }
];
