<template>
  <div class="rich-wrap">
    <Toolbar v-if="editorRef" class="toolbar" :editor="editorRef" :default-config="toolbarConfig" mode="default" />
    <Editor
      v-model="html"
      class="editor"
      :default-config="editorConfig"
      mode="default"
      @on-created="onCreated"
    />
  </div>
</template>

<script setup lang="ts">
import '@wangeditor/editor/dist/css/style.css';
import { onBeforeUnmount, ref, shallowRef, watch } from 'vue';
import { Editor, Toolbar } from '@wangeditor/editor-for-vue';
import type { IDomEditor, IEditorConfig, IToolbarConfig } from '@wangeditor/editor';

const props = withDefaults(
  defineProps<{
    modelValue: string;
    placeholder?: string;
  }>(),
  { placeholder: '支持图文、公式等富文本题干' }
);

const emit = defineEmits<{
  (e: 'update:modelValue', v: string): void;
}>();

const editorRef = shallowRef<IDomEditor>();
const html = ref(props.modelValue || '<p><br></p>');

watch(
  () => props.modelValue,
  (v) => {
    const next = v || '<p><br></p>';
    if (next !== html.value) html.value = next;
  }
);

watch(html, (v) => emit('update:modelValue', v));

const toolbarConfig: Partial<IToolbarConfig> = {
  excludeKeys: ['fullScreen', 'group-video']
};

const editorConfig: Partial<IEditorConfig> = {
  placeholder: props.placeholder,
  MENU_CONF: {}
};

function onCreated(editor: IDomEditor) {
  editorRef.value = editor;
}

onBeforeUnmount(() => {
  const e = editorRef.value;
  if (e) {
    e.destroy();
    editorRef.value = undefined;
  }
});
</script>

<style scoped>
.rich-wrap {
  border: 1px solid var(--el-border-color);
  border-radius: 4px;
  overflow: hidden;
}

.toolbar {
  border-bottom: 1px solid var(--el-border-color-light);
}

.editor {
  height: 280px;
  overflow-y: hidden;
}
</style>
