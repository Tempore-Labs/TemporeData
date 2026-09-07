<template>
  <el-breadcrumb separator="/" class="spark-breadcrumb">
    <el-breadcrumb-item :to="{ path: '/dashboard' }">首页</el-breadcrumb-item>
    <el-breadcrumb-item v-for="(item, index) in items" :key="index">
      {{ item.name }}
    </el-breadcrumb-item>
  </el-breadcrumb>
</template>

<script setup>
defineProps({
  items: {
    type: Array,
    default: () => []
  }
})
</script>

<style scoped>
.spark-breadcrumb {
  margin-bottom: 16px;
  font-size: 13px;
}
</style>