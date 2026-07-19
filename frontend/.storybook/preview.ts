import type { Preview } from '@storybook/vue3-vite'
import 'vuetify/styles';
import { vuetify } from '../src/app/plugins/vuetify'
import { setup } from '@storybook/vue3-vite';
import { createPinia } from 'pinia';

setup((app) => {
  app.use(vuetify)
     .use(createPinia())
});

const preview: Preview = {
  parameters: {
    controls: {
      matchers: {
        color: /(background|color)$/i,
        date: /Date$/i,
      },
    },

    a11y: {
      test: 'todo',
    },
  },
}

export default preview
