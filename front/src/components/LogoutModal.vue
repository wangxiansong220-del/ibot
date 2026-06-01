<script setup lang="ts">
import BaseModal from "./BaseModal.vue";

const props = defineProps<{
  show: boolean;
  email: string;
}>();

const emit = defineEmits(["close", "logout"]);
</script>

<template>
  <BaseModal :show="show" title="退出登录" @close="emit('close')">
    <div class="space-y-5 text-sm">
      <p class="text-on-surface-variant">
        你即将退出 <span class="text-on-surface">{{ email || '当前账号' }}</span>。
      </p>
      <div class="flex items-center justify-end gap-3">
        <button class="rounded-full px-5 py-2.5 text-on-surface-variant transition hover:bg-surface-container-low hover:text-on-surface" @click="emit('close')">
          取消
        </button>
        <button class="rounded-full bg-on-surface px-5 py-2.5 text-surface transition hover:opacity-90" @click="emit('logout')">
          退出登录
        </button>
      </div>
    </div>
  </BaseModal>
</template>
