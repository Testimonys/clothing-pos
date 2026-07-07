<template>
  <div style="padding: 24px">
    <h2>{{ title }}</h2>
    <p style="color: #999">此页面正在开发中...</p>
  </div>
</template>

<script setup lang="ts">
defineProps<{ title: string }>()
</script>
