<script setup lang="ts">
import { ref, onMounted } from 'vue'
import AppSidebar from './AppSidebar.vue'
import AppHeader from './AppHeader.vue'

const drawer = ref(true)
const mini = ref(false)
const isMobile = ref(false)

const toggleDrawer = () => {
  drawer.value = !drawer.value
}

const checkMobile = () => {
  isMobile.value = window.innerWidth < 960
}

onMounted(() => {
  checkMobile()
  window.addEventListener('resize', checkMobile)
})
</script>

<template>
  <v-app>
    <app-sidebar
      :drawer="drawer"
      :mini="mini"
      :isMobile="isMobile"
      @update:drawer="drawer = $event"
    />

    <app-header
      @toggle="toggleDrawer"
    />

    <v-main style="background: #f7f9fc;">
      <v-container fluid class="pa-6">
        <router-view />
      </v-container>
    </v-main>
  </v-app>
</template>