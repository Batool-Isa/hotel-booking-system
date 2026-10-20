<script>
import { imgUrl, phStyle, initials } from '@/lib/utils';

export default {
  props:{ src:String, name:String, alt:String },
  emits:['fail'],
  data(){ return { failed:false }; },
  watch:{ src(){ this.failed=false; } },
  computed:{ url(){ return imgUrl(this.src); }, ph(){ return phStyle(this.name); }, ini(){ return initials(this.name); } },
  methods:{ onErr(){ this.failed=true; this.$emit('fail'); } }
};
</script>

<template>
  <img v-if="url && !failed" :src="url" :alt="alt||name||'Hotel photo'" loading="lazy" @error="onErr">
  <div v-else class="ph" :style="ph" role="img" :aria-label="name||'Hotel'"><span>{{ini}}</span></div>
</template>
