import js from '@eslint/js'
import tseslint from '@typescript-eslint/eslint-plugin'
import tsParser from '@typescript-eslint/parser'
import eslintReact from '@eslint-react/eslint-plugin'
import prettierConfig from 'eslint-config-prettier'
import prettierPlugin from 'eslint-plugin-prettier'
import globals from 'globals'

export default [
  js.configs.recommended,
  {
    // Build / tooling scripts that run under Node directly (no bundler).
    files: ['scripts/**/*.js', 'scripts/**/*.mjs', 'scripts/**/*.cjs'],
    languageOptions: {
      ecmaVersion: 'latest',
      sourceType: 'module',
      globals: {
        ...globals.node,
      },
    },
  },
  {
    files: [
      'src/**/*.js',
      'src/**/*.jsx',
      'src/**/*.ts',
      'src/**/*.tsx',
      'test/**/*.ts',
      'test/**/*.tsx',
    ],
    plugins: {
      '@typescript-eslint': tseslint,
      '@eslint-react': eslintReact,
      prettier: prettierPlugin,
    },
    languageOptions: {
      parser: tsParser,
      ecmaVersion: 'latest',
      sourceType: 'module',
      parserOptions: {
        ecmaFeatures: { jsx: true },
        project: ['tsconfig.json'],
      },
      globals: {
        ...globals.browser,
        ...globals.jest,
      },
    },
    settings: {
      // @eslint-react reads React-version metadata under the `react-x` namespace
      // (its underlying rule package is eslint-plugin-react-x), not under `react`
      // like the legacy eslint-plugin-react. Pinning explicitly avoids the
      // 'detect' codepath that uses APIs removed in ESLint 10.
      'react-x': {
        version: '19.2',
      },
    },
    rules: {
      ...tseslint.configs.recommended.rules,
      ...eslintReact.configs['recommended-typescript'].rules,
      ...prettierConfig.rules,

      // Core JS rules that produce false positives in TS — TypeScript already enforces
      // these and `no-redeclare` rejects legitimate function overloads.
      'no-undef': 'off',
      'no-redeclare': 'off',
      'no-shadow': 'off',
      '@typescript-eslint/no-shadow': 'error',

      // High-value type-safety rules (require type-aware linting via parserOptions.project).
      // These catch entire classes of bugs that the type system alone can't detect.
      // `await-thenable` is `error` because awaiting a non-Promise is always wrong.
      // `no-floating-promises` and `no-misused-promises` are `warn` for now: the legacy
      // codebase has many fire-and-forget calls (`doRequest` with callback) that need
      // case-by-case review. Promote to `error` once the warning backlog is cleared.
      '@typescript-eslint/await-thenable': 'error',
      '@typescript-eslint/no-floating-promises': 'warn',
      '@typescript-eslint/no-misused-promises': 'warn',
      '@typescript-eslint/no-unnecessary-type-assertion': 'warn',
      '@typescript-eslint/prefer-nullish-coalescing': 'warn',
      '@typescript-eslint/prefer-optional-chain': 'warn',

      // Code-quality rules (no type info needed)
      '@typescript-eslint/consistent-type-imports': 'warn',
      '@typescript-eslint/no-non-null-assertion': 'warn',
      '@typescript-eslint/no-explicit-any': 'warn',
      eqeqeq: ['error', 'smart'],
      'no-throw-literal': 'error',
      // Allow `!!x` since it is idiomatic and preserves TypeScript narrowing in
      // `&&` chains (Boolean(x) does not narrow). Still flags `+x`, `~x.indexOf()`, `'' + x`.
      'no-implicit-coercion': ['warn', { boolean: false }],
      'no-console': ['warn', { allow: ['warn', 'error', 'info'] }],

      // Intentionally disabled: too noisy or stylistic-only.
      '@typescript-eslint/strict-boolean-expressions': 'off',
      '@typescript-eslint/return-await': 'off',
      // `set-state-in-effect` is overly aggressive: the project legitimately sets
      // state from async data fetches inside useEffect, which is the canonical
      // React pattern (see react.dev "You Might Not Need an Effect" — the carve-out
      // for fetching/subscriptions still applies).
      '@eslint-react/set-state-in-effect': 'off',
      // `purity` is a React Compiler hint (flags `new Date()` and `localStorage.getItem()`
      // during render). The project doesn't use React Compiler; these reads are
      // intentional and safe in our context.
      '@eslint-react/purity': 'off',
      // `naming-convention-ref-name` is purely stylistic (requires refs to end in `Ref`).
      '@eslint-react/naming-convention-ref-name': 'off',

      'max-len': [
        'warn',
        {
          code: 160,
          ignoreComments: true,
          ignoreUrls: true,
        },
      ],
      'prettier/prettier': [
        'error',
        {
          endOfLine: 'auto',
        },
      ],

      // Forbid `useMantineColorScheme` outside of the dedicated toggle component.
      // The hook returns the *stored preference* — which can be 'auto' — so code
      // like `colorScheme === 'dark' ? darkColor : lightColor` silently picks the
      // light branch when the user is on 'auto' with a dark system theme. See #1016.
      // For reading the resolved scheme, use `useComputedColorScheme` instead; it
      // returns the actually rendered 'light' or 'dark'. The only legitimate uses
      // of `useMantineColorScheme` are `setColorScheme` / `toggleColorScheme` /
      // `clearColorScheme`, which the override below permits in the toggle UI.
      'no-restricted-imports': [
        'error',
        {
          paths: [
            {
              name: '@mantine/core',
              importNames: ['useMantineColorScheme'],
              message:
                "Use `useComputedColorScheme` instead — it resolves 'auto' to the actually rendered 'light' or 'dark'. `useMantineColorScheme` returns the stored preference ('light' | 'dark' | 'auto') and silently breaks branching in auto mode (see #1016). If you need to set/toggle the scheme, add an override for your file in eslint.config.mjs.",
            },
          ],
        },
      ],
    },
  },
  {
    // ColorSchemeToggleButton is the only legitimate consumer of `useMantineColorScheme`:
    // it needs `toggleColorScheme` / `clearColorScheme`, and it handles the 'auto' value
    // explicitly via the `prefers-color-scheme` media query.
    files: ['src/components/ColorSchemeToggleButton/ColorSchemeToggleButton.tsx'],
    rules: {
      'no-restricted-imports': 'off',
    },
  },
]
