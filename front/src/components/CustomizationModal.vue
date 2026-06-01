<script setup lang="ts">
import { ref } from 'vue';

const props = defineProps<{
  show: boolean;
}>();

const emit = defineEmits(['close', 'save']);

const customInstructions = ref('');

const handleSave = () => {
  emit('save', customInstructions.value);
  emit('close');
};
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
        <div class="bg-[#1e1e1e] w-full max-w-sm rounded-[2rem] shadow-2xl border border-white/5 overflow-hidden p-8">
          <div class="flex flex-col gap-6">
            <h3 class="text-xl font-medium text-white">个性化</h3>
            
            <div class="space-y-4">
              <div class="space-y-2">
                <label class="text-xs font-medium text-white/50 px-4 uppercase tracking-wider">自定义指令</label>
                <textarea 
                  v-model="customInstructions"
                  placeholder="你希望 Ibot 如何回应？"
                  class="w-full h-48 bg-[#2a2a2a] border border-white/5 rounded-2xl px-5 py-4 text-white focus:outline-none focus:border-white/20 transition-colors resize-none"
                ></textarea>
              </div>
              <p class="text-[11px] text-white/40 leading-relaxed px-4">
                你可以提供背景信息或偏好，以便 Ibot 在每次对话中都能更好地为你服务。
              </p>
            </div>

            <!-- Buttons -->
            <div class="flex items-center justify-end gap-3 w-full mt-4">
              <button 
                @click="$emit('close')"
                class="px-6 py-2.5 rounded-full text-sm font-medium text-white hover:bg-white/5 transition-colors"
              >
                取消
              </button>
              <button 
                @click="handleSave"
                class="px-6 py-2.5 rounded-full text-sm font-medium bg-white text-black hover:bg-white/90 transition-colors"
              >
                保存
              </button>
            </div>
          </div>
        </div>
      </Transition>
    </div>
  </Transition>
</template>
