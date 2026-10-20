<script>
export default {
  props:{ type:{default:'error'}, msg:String },
  computed:{ ic(){ return {error:'alert',warn:'alert',info:'info',ok:'ok'}[this.type]; } }
};
</script>

<template>
  <div v-if="msg || $slots.default" class="alert" :class="type" role="alert"><icon :name="ic" :size="18"></icon><div><slot>{{msg}}</slot></div></div>
</template>
