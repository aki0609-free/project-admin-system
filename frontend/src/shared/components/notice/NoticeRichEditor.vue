<script setup lang="ts">
import { onBeforeUnmount, ref } from 'vue'

import { Color } from '@tiptap/extension-color'
import { Highlight } from '@tiptap/extension-highlight'
import { Link } from '@tiptap/extension-link'
import { TextStyle } from '@tiptap/extension-text-style'
import StarterKit from '@tiptap/starter-kit'
import { EditorContent, useEditor } from '@tiptap/vue-3'

const modelValue = defineModel<string>({
  required: true,
})

const textColor = ref('#2563eb')
const highlightColor = ref('#fef08a')

const isExternalUrl = (url: string) =>
  /^https?:\/\//i.test(url) || /^mailto:/i.test(url)

const editor = useEditor({
  content: modelValue.value,
  extensions: [
    StarterKit,
    TextStyle,
    Color.configure({
      types: ['textStyle'],
    }),
    Highlight.configure({
      multicolor: true,
    }),
    Link.configure({
      openOnClick: false,
      autolink: true,
      linkOnPaste: true,
    }),
  ],
  onUpdate: ({ editor }) => {
    modelValue.value = editor.getHTML()
  },
})

const setLink = () => {
  const previousUrl = editor.value?.getAttributes('link').href as string | undefined
  const url = window.prompt('リンクURLを入力してください', previousUrl ?? '')

  if (url === null) return

  const trimmed = url.trim()

  if (!trimmed) {
    editor.value?.chain().focus().extendMarkRange('link').unsetLink().run()
    return
  }

  const attrs = isExternalUrl(trimmed)
    ? {
        href: trimmed,
        target: '_blank',
        rel: 'noopener noreferrer',
      }
    : {
        href: trimmed,
        target: '_self',
        rel: null,
      }

  editor.value
    ?.chain()
    .focus()
    .extendMarkRange('link')
    .setLink(attrs)
    .run()
}

const unsetLink = () => {
  editor.value?.chain().focus().extendMarkRange('link').unsetLink().run()
}

const setTextColor = () => {
  editor.value?.chain().focus().setColor(textColor.value).run()
}

const unsetTextColor = () => {
  editor.value?.chain().focus().unsetColor().run()
}

const setHighlight = () => {
  editor.value
    ?.chain()
    .focus()
    .toggleHighlight({ color: highlightColor.value })
    .run()
}

const unsetHighlight = () => {
  editor.value?.chain().focus().unsetHighlight().run()
}

onBeforeUnmount(() => {
  editor.value?.destroy()
})
</script>

<template>
  <div class="editor-wrapper">
    <div class="toolbar">
      <div class="toolbar-group">
        <v-btn size="small" variant="text" @click="editor?.chain().focus().toggleBold().run()">
          太字
        </v-btn>

        <v-btn size="small" variant="text" @click="editor?.chain().focus().toggleItalic().run()">
          斜体
        </v-btn>

        <v-btn size="small" variant="text" @click="editor?.chain().focus().toggleHeading({ level: 2 }).run()">
          見出し
        </v-btn>
      </div>

      <div class="toolbar-divider" />

      <div class="toolbar-group">
        <v-btn size="small" variant="text" @click="editor?.chain().focus().toggleBulletList().run()">
          箇条書き
        </v-btn>

        <v-btn size="small" variant="text" @click="editor?.chain().focus().toggleOrderedList().run()">
          番号リスト
        </v-btn>

        <v-btn size="small" variant="text" @click="editor?.chain().focus().toggleBlockquote().run()">
          引用
        </v-btn>

        <v-btn size="small" variant="text" @click="editor?.chain().focus().setHorizontalRule().run()">
          区切り線
        </v-btn>
      </div>

      <div class="toolbar-divider" />

      <div class="toolbar-group">
        <v-btn size="small" variant="text" prepend-icon="mdi-link" @click="setLink">
          リンク
        </v-btn>

        <v-btn size="small" variant="text" prepend-icon="mdi-link-off" @click="unsetLink">
          解除
        </v-btn>
      </div>

      <div class="toolbar-divider" />

      <div class="toolbar-group color-tools">
        <label class="color-tool">
          <span>文字</span>
          <input v-model="textColor" type="color" @input="setTextColor">
        </label>

        <v-btn size="small" variant="text" @click="unsetTextColor">
          文字色解除
        </v-btn>

        <label class="color-tool">
          <span>背景</span>
          <input v-model="highlightColor" type="color" @input="setHighlight">
        </label>

        <v-btn size="small" variant="text" @click="unsetHighlight">
          背景解除
        </v-btn>
      </div>

      <div class="toolbar-divider" />

      <div class="toolbar-group">
        <v-btn size="small" variant="text" @click="editor?.chain().focus().undo().run()">
          戻す
        </v-btn>

        <v-btn size="small" variant="text" @click="editor?.chain().focus().redo().run()">
          進む
        </v-btn>
      </div>
    </div>

    <EditorContent :editor="editor" class="editor-content" />
  </div>
</template>

<style scoped>
.editor-wrapper {
  border: 1px solid #d0d7de;
  border-radius: 8px;
  overflow: hidden;
}

.toolbar {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 12px;
  border-bottom: 1px solid #e5e7eb;
  background: #f8fafc;
  overflow-x: auto;
}

.toolbar-group {
  display: flex;
  align-items: center;
  gap: 4px;
  flex-shrink: 0;
}

.toolbar-divider {
  width: 1px;
  height: 24px;
  background: #dbeafe;
  flex-shrink: 0;
}

.color-tools {
  gap: 6px;
}

.color-tool {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #475569;
  white-space: nowrap;
}

.color-tool input[type='color'] {
  width: 30px;
  height: 30px;
  border: none;
  background: transparent;
  cursor: pointer;
}

.editor-content {
  min-height: 240px;
  padding: 12px 16px;
}

.editor-content :deep(.ProseMirror) {
  min-height: 220px;
  outline: none;
}

.editor-content :deep(a) {
  color: #2563eb;
  text-decoration: underline;
}

.editor-content :deep(p) {
  margin: 0 0 12px;
}

.editor-content :deep(ul),
.editor-content :deep(ol) {
  padding-left: 1.5rem;
  margin: 8px 0 12px;
}

.editor-content :deep(li) {
  margin: 4px 0;
}

.editor-content :deep(blockquote) {
  margin: 8px 0 12px;
  padding-left: 12px;
  border-left: 4px solid #cbd5e1;
  color: #64748b;
}

.editor-content :deep(hr) {
  border: none;
  border-top: 1px solid #e5e7eb;
  margin: 16px 0;
}
</style>