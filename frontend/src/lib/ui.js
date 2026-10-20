import { ref, reactive } from 'vue';

/* UI state for the app including toasts &&  confirmation dialog */
export const toasts = ref([]); let toastId = 0;
export function toast(msg, type='success', ms=5500){ if(toasts.value.some(t=>t.msg===msg && t.type===type)) return; const id=++toastId; toasts.value.push({id,msg,type}); setTimeout(()=>dismiss(id), ms); }
export function dismiss(id){ toasts.value = toasts.value.filter(t=>t.id!==id); }

/* confirmation dialog */
export const dlg = reactive({open:false,title:'',message:'',confirmText:'Confirm',cancelText:'Cancel',danger:false});
let dlgResolve = null;
export function ask(opts){ Object.assign(dlg,{open:true,title:'',message:'',confirmText:'Confirm',cancelText:'Cancel',danger:false,...opts}); return new Promise(r=>{dlgResolve=r;}); }
export function answer(v){ dlg.open=false; if(dlgResolve) dlgResolve(v); dlgResolve=null; }
