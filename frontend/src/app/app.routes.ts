import { Routes } from '@angular/router';

import { adminGuard } from './core/guards/admin.guard';
import { loginRedirectGuard } from './core/guards/login-redirect.guard';
import { unsavedChangesGuard } from './core/guards/unsaved-changes.guard';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () => import('./pages/home/home.component').then((m) => m.HomeComponent),
    title: 'Technical Blog'
  },
  {
    path: 'category/:slug',
    loadComponent: () => import('./pages/category/category.component').then((m) => m.CategoryComponent),
    title: 'Category'
  },
  {
    path: 'article/:slug',
    loadComponent: () => import('./pages/article/article.component').then((m) => m.ArticleComponent),
    title: 'Article'
  },
  {
    path: 'search',
    loadComponent: () => import('./pages/search/search.component').then((m) => m.SearchComponent),
    title: 'Search'
  },
  {
    path: 'login',
    canActivate: [loginRedirectGuard],
    loadComponent: () => import('./pages/auth/login/login.component').then((m) => m.LoginComponent),
    title: 'Sign in'
  },
  {
    path: 'register',
    canActivate: [loginRedirectGuard],
    loadComponent: () => import('./pages/auth/register/register.component').then((m) => m.RegisterComponent),
    title: 'Create an account'
  },
  {
    path: 'admin/login',
    pathMatch: 'full',
    redirectTo: 'login'
  },
  {
    path: 'admin',
    canActivate: [adminGuard],
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
      {
        path: 'dashboard',
        loadComponent: () =>
          import('./pages/admin/dashboard/dashboard.component').then((m) => m.DashboardComponent),
        title: 'Admin dashboard'
      },
      {
        path: 'articles',
        pathMatch: 'full',
        redirectTo: 'dashboard'
      },
      {
        path: 'articles/create',
        canDeactivate: [unsavedChangesGuard],
        loadComponent: () =>
          import('./pages/admin/article-form/article-form.component').then((m) => m.ArticleFormComponent),
        title: 'Create article'
      },
      {
        path: 'articles/edit/:id',
        canDeactivate: [unsavedChangesGuard],
        loadComponent: () =>
          import('./pages/admin/article-form/article-form.component').then((m) => m.ArticleFormComponent),
        title: 'Edit article'
      },
      {
        path: 'categories',
        loadComponent: () =>
          import('./pages/admin/categories/categories.component').then((m) => m.CategoriesComponent),
        title: 'Manage categories'
      }
    ]
  },
  {
    path: '**',
    loadComponent: () => import('./pages/not-found/not-found.component').then((m) => m.NotFoundComponent),
    title: 'Page not found'
  }
];
