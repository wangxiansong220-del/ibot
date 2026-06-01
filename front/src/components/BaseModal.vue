<script setup lang="ts">
import { X } from 'lucide-vue-next';

defineProps<{
  show: boolean;
  title?: string;
}>();

defineEmits(['close']);
</script>

<template>
  <Transition
    enter-active-class="transition duration-300 ease-out"
    enter-from-class="opacity-0"
    enter-to-class="opacity-100"
    leave-active-class="transition duration-200 ease-in"
    leave-from-class="opacity-100"
    leave-to-class="opacity-0"
  >
    <div v-if="show" class="fixed inset-0 z-[100] flex items-center justify-center p-4 bg-black/60 backdrop-blur-sm">
      <Transition
        enter-active-class="transition duration-300 ease-out delay-75"
        enter-from-class="opacity-0 scale-95 translate-y-4"
        enter-to-class="opacity-100 scale-100 translate-y-0"
        leave-active-class="transition duration-200 ease-in"
        leave-from-class="opacity-100 scale-100 translate-y-0"
        leave-to-class="opacity-0 scale-95 translate-y-4"
      >
        <div class="bg-surface-container-highest w-full max-w-md rounded-3xl shadow-2xl border border-outline-variant/10 overflow-hidden">
          <div v-if="title" class="px-6 py-4 flex items-center justify-between border-b border-outline-variant/5">
            <h3 class="text-lg font-medium text-on-surface">{{ title }}</h3>
            <button @click="$emit('close')" class="p-2 hover:bg-on-surface/5 rounded-full transition-colors">
              <X class="w-5 h-5 text-on-surface-variant" />
            </button>
          </div>
          <div class="p-6">
            <slot />
          </div>
        </div>
      </Transition>
    </div>
  </Transition>
</template>
