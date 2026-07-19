// For more info, see https://github.com/storybookjs/eslint-plugin-storybook#configuration-flat-config-format
import { globalIgnores } from 'eslint/config'
import { defineConfigWithVueTs, vueTsConfigs } from '@vue/eslint-config-typescript'
import pluginVue from 'eslint-plugin-vue'
import pluginVitest from '@vitest/eslint-plugin'
import pluginOxlint from 'eslint-plugin-oxlint'
import skipFormatting from 'eslint-config-prettier/flat'

export default defineConfigWithVueTs(
  {
    name: 'app/files-to-lint',
    files: ['**/*.{vue,ts,mts,tsx}'],
  },

  globalIgnores([
    '**/dist/**',
    '**/dist-ssr/**',
    '**/coverage/**',
    '**/node_modules/**',
  ]),

  // Vue強化（essential → recommended）
  ...pluginVue.configs['flat/recommended'],

  // TypeScript強化
  vueTsConfigs.strict,

  {
    rules: {
      'no-console': ['warn', { allow: ['warn', 'error'] }],
      'no-debugger': 'error',

      // TS強化
      '@typescript-eslint/no-explicit-any': 'error',
      '@typescript-eslint/consistent-type-imports': 'error',
      '@typescript-eslint/no-unused-vars': ['error', { argsIgnorePattern: '^_' }],

      // Vue強化
      'vue/multi-word-component-names': 'off', // 業務では柔軟に
      'vue/no-unused-components': 'error',
      'vue/require-default-prop': 'off',

      // import順制御（超重要）
      
      'import/order': 'off',
      //'import/order': [
      //  'error',
      //  {
      //    groups: [
      //      'builtin',
      //      'external',
      //      'internal',
      //      ['parent', 'sibling', 'index'],
      //    ],
      //    'newlines-between': 'always',
      //  },
      //  
      //],
    },
  },

  // テスト用緩和
  {
    ...pluginVitest.configs.recommended,
    files: ['src/**/__tests__/*'],
    rules: {
      '@typescript-eslint/no-explicit-any': 'off',
    },
  },

  ...pluginOxlint.buildFromOxlintConfigFile('.oxlintrc.json'),

  skipFormatting,
)