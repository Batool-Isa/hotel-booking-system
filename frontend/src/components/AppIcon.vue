<script>
import { ICONS } from '@/lib/icons';

export default {
  props:{ name:String, size:{ type:[Number,String], default:18 } },
  computed:{ p(){ return ICONS[this.name]||''; } }
};
</script>

<template>
  <svg class="ic" :width="size" :height="size" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true" v-html="p"></svg>
</template>
