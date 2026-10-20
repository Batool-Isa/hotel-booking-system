<script>
import { toISO, addDays, parseISO } from '@/lib/utils';

export default {
  props:{ f:Object, hideCity:Boolean, compact:Boolean, minIn:String, busy:Boolean },
  emits:['submit'],
  data(){ return { open:false }; },
  mounted(){ this._h = e => { if(!this.$el.contains(e.target)) this.open=false; }; document.addEventListener('click', this._h); },
  unmounted(){ document.removeEventListener('click', this._h); },
  computed:{
    minOut(){ return this.f.values.checkIn ? toISO(addDays(parseISO(this.f.values.checkIn),1)) : this.minIn; },
    firstErr(){ const e=this.f.errors; return e.checkIn||e.checkOut||e.adults||e.children||e.city||''; }
  },
  methods:{
    step(k,d){ const v=Number(this.f.values[k]||0)+d; const lo = k==='adults'?1:0; this.f.values[k]=Math.max(lo,Math.min(20,v)); },
    onIn(){ const v=this.f.values; if(v.checkIn && (!v.checkOut || v.checkOut<=v.checkIn)) v.checkOut = toISO(addDays(parseISO(v.checkIn),1)); }
  }
};
</script>

<template>
  <form class="searchbar" :class="{compact,nocity:hideCity}" @submit.prevent="$emit('submit')" novalidate>
    <div v-if="!hideCity" class="sb-field" :class="{bad:f.errors.city}"><icon name="bed"></icon><div><label for="sb-city">Where to?</label>
      <input id="sb-city" v-model="f.values.city" list="city-list" placeholder="City – e.g. Manama" maxlength="100" autocomplete="off"></div></div>
    <div class="sb-field" :class="{bad:f.errors.checkIn}"><icon name="calendar"></icon><div><label for="sb-in">Check-in</label>
      <input id="sb-in" type="date" v-model="f.values.checkIn" :min="minIn" @change="onIn();f.check('checkIn')"></div></div>
    <div class="sb-field" :class="{bad:f.errors.checkOut}"><icon name="calendar"></icon><div><label for="sb-out">Check-out</label>
      <input id="sb-out" type="date" v-model="f.values.checkOut" :min="minOut" @change="f.check('checkOut')"></div></div>
    <div class="sb-field" :class="{bad:f.errors.adults||f.errors.children}">
      <button type="button" class="guest-btn" @click="open=!open" :aria-expanded="open"><icon name="users"></icon>
        <div><label>Guests</label><span>{{plural(f.values.adults,'adult')}} · {{plural(f.values.children,'child','children')}}</span></div></button>
      <div v-if="open" class="guest-pop" @click.stop>
        <div class="row between mb"><div><b>Adults</b><div class="xs muted">Aged 18+ (hotel policy may vary)</div></div>
          <div class="stepper"><button type="button" :disabled="f.values.adults<=1" @click="step('adults',-1)" aria-label="Fewer adults"><icon name="minus" :size="16"></icon></button><b>{{f.values.adults}}</b><button type="button" @click="step('adults',1)" aria-label="More adults"><icon name="plus" :size="16"></icon></button></div></div>
        <div class="row between mb"><div><b>Children</b><div class="xs muted">Under 18</div></div>
          <div class="stepper"><button type="button" :disabled="f.values.children<=0" @click="step('children',-1)" aria-label="Fewer children"><icon name="minus" :size="16"></icon></button><b>{{f.values.children}}</b><button type="button" @click="step('children',1)" aria-label="More children"><icon name="plus" :size="16"></icon></button></div></div>
        <button type="button" class="btn btn-primary btn-block" @click="open=false">Done</button>
      </div>
    </div>
    <button class="btn btn-primary sb-btn" type="submit" :class="{'is-loading':busy}"><icon name="search"></icon>Search</button>
    <div v-if="firstErr" class="sb-errors" role="alert"><icon name="alert" :size="16"></icon>{{firstErr}}</div>
  </form>
</template>
