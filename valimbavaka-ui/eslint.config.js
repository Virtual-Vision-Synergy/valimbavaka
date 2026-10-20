// @ts-check
const eslint = require('@eslint/js');
const { defineConfig } = require('eslint/config');
const tseslint = require('typescript-eslint');
const angular = require('angular-eslint');

module.exports = defineConfig([
  {
    files: ['**/*.ts'],
    extends: [
      eslint.configs.recommended,
      tseslint.configs.recommended,
      tseslint.configs.stylistic,
      angular.configs.tsRecommended,
    ],
    processor: angular.processInlineTemplates,
    rules: {
      '@angular-eslint/directive-selector': [
        'error',
        {
          type: 'attribute',
          prefix: 'vvs',
          style: 'camelCase',
        },
      ],
      '@angular-eslint/component-selector': [
        'error',
        {
          type: 'element',
          prefix: 'vvs',
          style: 'kebab-case',
        },
      ],
      // Convention VVS V2.1. Sévérité selon la grille §15 : 'error' = P0 (bloque la PR), 'warn' = P1/P2.
      // §1.4 : any interdit sauf eslint-disable-next-line justifié.
      '@typescript-eslint/no-explicit-any': 'error',
      // §9 : jeton d'accès en mémoire uniquement. Le stockage reste permis pour une préférence (langue),
      // d'où un avertissement à vérifier en revue plutôt qu'un blocage.
      'no-restricted-globals': [
        'warn',
        { name: 'localStorage', message: 'Jamais pour un jeton ou une donnée sensible (§9).' },
        { name: 'sessionStorage', message: 'Jamais pour un jeton ou une donnée sensible (§9).' },
      ],
      'no-restricted-properties': [
        'error',
        {
          property: 'innerHTML',
          message: 'Interdit (§9) : risque XSS, utiliser le binding Angular.',
        },
      ],
      'no-console': ['error', { allow: ['warn', 'error'] }],
      // §10 : composants standalone, OnPush, signals.
      '@angular-eslint/prefer-on-push-component-change-detection': 'warn',
      '@angular-eslint/prefer-standalone': 'error',
      '@angular-eslint/component-max-inline-declarations': ['warn', { template: 15, styles: 15 }],
      // §2.2 : limites de taille.
      'max-lines': ['warn', { max: 300, skipBlankLines: true, skipComments: true }],
      'max-lines-per-function': ['warn', { max: 30, skipBlankLines: true, skipComments: true }],
      'max-params': ['warn', 4],
      'max-depth': ['warn', 3],
      complexity: ['warn', 10],
      'max-len': [
        'warn',
        { code: 120, ignoreUrls: true, ignoreStrings: true, ignoreTemplateLiterals: true },
      ],
    },
  },
  {
    // Les tests décrivent des scénarios : les blocs describe/it dépassent naturellement 30 lignes.
    files: ['**/*.spec.ts'],
    rules: {
      'max-lines-per-function': 'off',
      // Les tests de la préférence de langue lisent et vident le stockage du navigateur.
      'no-restricted-globals': 'off',
    },
  },
  {
    files: ['**/*.html'],
    extends: [angular.configs.templateRecommended, angular.configs.templateAccessibility],
    rules: {
      // §10 : @if / @for (avec track) plutôt que *ngIf / *ngFor.
      '@angular-eslint/template/prefer-control-flow': 'error',
    },
  },
]);
