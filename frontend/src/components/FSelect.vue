<script>
export default {
  props:{ modelValue:{default:''}, label:String, error:String, hint:String, options:{type:Array,default:()=>[]}, placeholder:String, required:Boolean, disabled:Boolean },
  emits:['update:modelValue','blur'],
  data(){ return { uid:'s'+Math.random().toString(36).slice(2,9) }; }
};
</script>

<template>
  <div class="field" :class="{'has-error':!!error}">
    <label v-if="label" :for="uid">{{label}}<span v-if="required" class="req">*</span></label>
    <select :id="uid" :value="modelValue" :disabled="disabled" @change="$emit('update:modelValue',$event.target.value)" @blur="$emit('blur')">
      <option v-if="placeholder" value="">{{placeholder}}</option>
      <option v-for="o in options" :key="o.value" :value="o.value">{{o.label}}</option>
    </select>
    <div class="field-foot"><span v-if="error" class="err" role="alert"><icon name="alert" :size="14"></icon>{{error}}</span><span v-else-if="hint" class="hint">{{hint}}</span></div>
  </div>
</template>
