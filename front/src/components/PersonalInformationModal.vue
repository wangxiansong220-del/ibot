<script setup lang="ts">
import BaseModal from "./BaseModal.vue";
import type { UserProfile } from "../services/auth";

const props = defineProps<{
  show: boolean;
  user: UserProfile | null;
}>();

const emit = defineEmits(["close"]);
</script>

<template>
  <BaseModal :show="show" title="个人信息" @close="emit('close')">
    <div class="space-y-4 text-sm">
      <div class="rounded-3xl bg-surface-container-low px-4 py-4">
        <p class="text-xs uppercase tracking-[0.24em] text-on-surface-variant">显示名称</p>
        <p class="mt-2 text-on-surface">{{ user?.displayName || '--' }}</p>
      </div>
      <div class="rounded-3xl bg-surface-container-low px-4 py-4">
        <p class="text-xs uppercase tracking-[0.24em] text-on-surface-variant">邮箱</p>
        <p class="mt-2 text-on-surface">{{ user?.email || '--' }}</p>
      </div>
      <div class="rounded-3xl bg-surface-container-low px-4 py-4">
        <p class="text-xs uppercase tracking-[0.24em] text-on-surface-variant">角色</p>
        <p class="mt-2 text-on-surface">{{ user?.role === 'ADMIN' ? '管理员' : user?.role === 'USER' ? '用户' : '--' }}</p>
      </div>
      <p class="text-xs text-on-surface-variant">当前版本还没有接入资料编辑，这里暂时是只读展示。</p>
    </div>
  </BaseModal>
</template>
