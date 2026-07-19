import type { App, DirectiveBinding } from 'vue'
import { usePermission } from '@/shared/auth/composables/usePermission'

type CanBindingValue = [resource: string, action: string]

export function registerPermissionDirective(app: App) {
  app.directive('can', {
    mounted(el, binding: DirectiveBinding<CanBindingValue>) {
      const { can } = usePermission()

      const [resource, action] = binding.value

      if (!can(resource, action)) {
        el.remove()
      }
    },
  })
}