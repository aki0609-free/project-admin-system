/* eslint-disable @typescript-eslint/no-explicit-any */
import type { MenuItem } from "./routes/menuItem"

export const generateRoutesFromMenu = (menuItems: MenuItem[]): any[] => {
  const routes: any[] = []

  const traverse = (items: MenuItem[]) => {
    for (const item of items) {
      if (item.to && item.component) {
        routes.push({
          path: item.to,
          component: item.component,
          meta: {
            resource: item.resource,
            action: item.action,
            requiresAuth: true,
          },
        })
      }
      if (item.children) {
        traverse(item.children)
      }
    }
  }

  traverse(menuItems)
  return routes
}