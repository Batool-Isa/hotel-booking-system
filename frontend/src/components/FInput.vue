<script>
export default {
  props:{ modelValue:{default:''}, label:String, type:{default:'text'}, error:String, hint:String, placeholder:String, required:Boolean,
    min:[Number,String], max:[Number,String], step:[Number,String], maxlength:[Number,String], disabled:Boolean, textarea:Boolean,
    rows:{default:3}, counter:Boolean, autocomplete:String, list:String, inputmode:String },
  emits:['update:modelValue','blur'],
  data(){ return { show:false, uid:'f'+Math.random().toString(36).slice(2,9) }; },
  computed:{ t(){ return this.type==='password' && this.show ? 'text' : this.type; } }
};
</script>

<template>
  <div class="field" :class="{'has-error':!!error}">
    <label v-if="label" :for="uid">{{label}}<span v-if="required" class="req">*</span></label>
    <div class="input-wrap">
      <textarea v-if="textarea" :id="uid" :value="modelValue" :rows="rows" :maxlength="maxlength" :placeholder="placeholder" :disabled="disabled" :aria-invalid="!!error"
        @input="$emit('update:modelValue',$event.target.value)" @blur="$emit('blur')"></textarea>
      <input v-else :id="uid" :type="t" :value="modelValue" :min="min" :max="max" :step="step" :maxlength="maxlength" :placeholder="placeholder" :disabled="disabled"
        :autocomplete="autocomplete" :list="list" :inputmode="inputmode" :aria-invalid="!!error" :style="type==='password'?'padding-right:44px':''"
        @input="$emit('update:modelValue',$event.target.value)" @blur="$emit('blur')">
      <button v-if="type==='password'" type="button" class="eye" tabindex="-1" :aria-label="show?'Hide password':'Show password'" @click="show=!show"><icon :name="show?'eye-off':'eye'"></icon></button>
    </div>
    <div class="field-foot"><span v-if="error" class="err" role="alert"><icon name="alert" :size="14"></icon>{{error}}</span><span v-else-if="hint" class="hint">{{hint}}</span>
      <span v-if="counter && maxlength" class="counter">{{String(modelValue||'').length}}/{{maxlength}}</span></div>
  </div>
</template>
