import type { Component } from 'vue'

export interface MenuItem {
  title: string
  icon?: string
  to?: string
  children?: MenuItem[]
  component?: Component
  resource?: string
  action?: string
}