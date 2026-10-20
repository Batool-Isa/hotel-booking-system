import { reactive, ref, watch, nextTick } from 'vue';
import { blank } from './utils';

/* validation for DTO coming from backend and then used in forms */
export const R = {
  req:  label => v => blank(v) ? `${label} is required` : '',
  email:() => v => !blank(v) && !/^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/.test(String(v).trim()) ? 'Please enter a valid email address' : '',
  min:  (n,label) => v => !blank(v) && String(v).length < n ? `${label} must be at least ${n} characters` : '',
  max:  (n,label) => v => !blank(v) && String(v).length > n ? `${label} cannot exceed ${n} characters` : '',
  len:  (a,b,label) => v => !blank(v) && (String(v).trim().length<a || String(v).trim().length>b) ? `${label} must be between ${a} and ${b} characters` : '',
  num:  (label,{min,max,int,optional}={}) => v => {
    if(blank(v)) return optional ? '' : `${label} is required`;
    const n = Number(v);
    if(Number.isNaN(n)) return `${label} must be a number`;
    if(int && !Number.isInteger(n)) return `${label} must be a whole number`;
    if(min!==undefined && n<min) return `${label} must be at least ${min}`;
    if(max!==undefined && n>max) return `${label} cannot exceed ${max}`;
    return '';
  },
  same: (other,msg) => (v,d) => v !== d[other] ? msg : '',
  custom: fn => fn,
};

export function useForm(initial, rules){
  const values = reactive({...initial});
  const errors = reactive({});
  const banner = ref('');
  const busy = ref(false);
  const check = f => { let m=''; for(const fn of (rules[f]||[])){ m = fn(values[f], values); if(m) break; } errors[f]=m; return !m; };
  const validateAll = () => { let ok=true, first=null; for(const f of Object.keys(rules)){ if(!check(f)){ ok=false; first=first||f; } } if(!ok) nextTick(()=>focusFirstError()); return ok; };
  const reset = (data) => { Object.assign(values, {...initial, ...(data||{})}); for(const k of Object.keys(errors)) delete errors[k]; banner.value=''; };
  watch(values, () => { for(const f of Object.keys(errors)) if(errors[f]) check(f); }, {deep:true});
  return reactive({ values, errors, banner, busy, check, validateAll, reset });
}

export function focusFirstError(){
  const el = document.querySelector('.has-error input, .has-error select, .has-error textarea, .sb-field.bad input');
  if(el){ el.scrollIntoView({block:'center',behavior:'smooth'}); el.focus({preventScroll:true}); }
}
