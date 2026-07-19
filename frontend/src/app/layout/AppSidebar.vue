<script setup lang="ts">
import { useFilteredMenu } from '@/app/router/composables/useFilteredMenu'
import { computed } from 'vue'
import { menuItems } from '../menu/menuItems'

interface Props {
  drawer: boolean
  mini: boolean
  isMobile: boolean
}

const propsSidebar = defineProps<Props>()
const emit = defineEmits(['update:drawer'])

const drawerWidth = computed(() => (propsSidebar.mini ? 80 : 260))

const filteredMenu = useFilteredMenu(menuItems)
</script>

<template>
  <v-navigation-drawer
    :model-value="drawer"
    :permanent="!isMobile"
    :temporary="isMobile"
    :width="drawerWidth"
    elevation="1"
    class="border-e"
    @update:model-value="emit('update:drawer', $event)"
  >
    <div class="pa-4 text-center">
      <div class="text-h6 font-weight-bold text-primary">HR Analytics &</div>
      <div class="text-h6 font-weight-bold text-primary">Administration System</div>
    </div>

    <v-divider />

    <v-list nav density="comfortable">
      <template v-for="item in filteredMenu" :key="item.title">
        <v-list-group v-if="item.children">
          <template #activator="{ props }">
            <v-list-item v-bind="props" :title="item.title" :prepend-icon="item.icon" />
          </template>

          <v-list-item
            v-for="child in item.children"
            :key="child.title"
            :title="child.title"
            :to="child.to"
            link
          />
        </v-list-group>

        <v-list-item v-else :title="item.title" :prepend-icon="item.icon" :to="item.to" link />
      </template>
    </v-list>
  </v-navigation-drawer>
</template>
