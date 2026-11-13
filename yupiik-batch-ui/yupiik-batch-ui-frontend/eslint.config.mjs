import js from '@eslint/js';
import globals from 'globals';

export default [
  { ignores: ['target/**', '.node/**', 'node_modules/**'] },
  js.configs.recommended,
  {
    files: ['**/*.{js,mjs}'],
    languageOptions: {
      globals: { ...globals.browser, ...globals.node },
      parserOptions: { ecmaFeatures: { jsx: true } },
    },
    rules: {
      // JSX identifiers are handled by the compiler, not ESLint's core scope analysis.
      'no-unused-vars': ['error', { varsIgnorePattern: '^[A-Z_]', argsIgnorePattern: '^_' }],
      'curly': 'error',
    },
  },
];
