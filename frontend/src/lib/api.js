import { ref, reactive, computed } from 'vue';
import { API_BASE, store, jwtExp } from './utils';

export class ApiError extends Error { constructor(status, message, data){ super(message); this.status=status; this.data=data; } }

export const pending = ref(0);
export const auth = reactive({ token: store.get('vs_token')||'', user: null });
export const role = computed(() => auth.user ? (auth.user.role && auth.user.role.name) || auth.user.role || '' : '');
export const isCustomer = computed(() => role.value==='CUSTOMER');
export const isManager = computed(() => role.value==='HOTEL_MANAGER');
export const isAdmin = computed(() => role.value==='ADMIN');

/* App.vue registers what to do when the token turns out to be expired */
let expireSession = () => {};
export function setExpireHandler(fn){ expireSession = fn; }

function friendly(status, msg){
  const m = String(msg||'').trim();
  if(/Required part 'image'|part 'image' is not present/i.test(m)) return 'Please choose a profile photo.';
  if(/Required part 'images'|part 'images' is not present/i.test(m)) return 'Please choose at least one photo.';
  if(/maximum upload size|MaxUploadSize|too large|exceeded/i.test(m) && /upload|size|large/i.test(m)) return 'That file is too large. Please choose an image under 1 MB.';
  if(/JSON parse error|not readable|Cannot deserialize|HttpMessageNotReadable/i.test(m)) return 'Some details were in the wrong format. Please check the form and try again.';
  if(m && !/^<(!doctype|html)/i.test(m) && m.length<400) return m;
  switch(status){
    case 0: return "We couldn't reach the server. Check your connection and make sure the backend is running.";
    case 400: return 'Some details look wrong. Please check the form and try again.';
    case 401: return 'Please sign in to continue.';
    case 403: return "You don't have permission to do that.";
    case 404: return "We couldn't find what you were looking for.";
    case 405: return 'That action is not supported by the server.';
    case 409: return 'That already exists.';
    case 413: return 'That file is too large. Please choose an image under 1 MB.';
    case 429: return 'Too many attempts. Please wait a minute and try again.';
    default: return status>=500 ? 'Something went wrong on our side. Please try again in a moment.' : 'Something went wrong. Please try again.';
  }
}

export async function api(method, path, opts={}){
  const { body, form, query, auth: useAuth=true, timeout=20000, headers: extraHeaders } = opts;
  const headers = { ...extraHeaders };
  if(useAuth && auth.token) headers.Authorization = 'Bearer '+auth.token;
  let payload;
  if(form) payload = form;
  else if(body!==undefined){ headers['Content-Type']='application/json'; payload=JSON.stringify(body); }
  let url = API_BASE + path;
  if(query){ const p = new URLSearchParams(); for(const [k,v] of Object.entries(query)) if(v!==undefined && v!==null && v!=='') p.append(k,v); const s=p.toString(); if(s) url += '?'+s; }
  pending.value++;
  const ctrl = new AbortController(); const timer = setTimeout(()=>ctrl.abort(), timeout);
  try{
    let res;
    try{ res = await fetch(url,{ method, headers, body:payload, signal:ctrl.signal }); }
    catch(e){ throw new ApiError(0, e.name==='AbortError' ? 'The server took too long to respond. Please try again.' : friendly(0), null); }
    const text = await res.text();
    let data = null;
    if(text){ try{ data = JSON.parse(text); }catch{ data = text; } }
    if(res.ok) return data;
    let msg = data && typeof data==='object' ? (data.message||'') : (typeof data==='string' ? data : '');
    if(res.status===401 && useAuth && auth.token && !(data && data.error)){   // token problem (business 401s such as a wrong password carry an `error` code)
      expireSession(); throw new ApiError(401,'Your session has expired. Please sign in again.',data); }
    if(res.status===429){ const ra = Number(res.headers.get('Retry-After')||(data&&data.retryAfterSeconds)||0); if(ra>0) throw new ApiError(429, `Too many attempts. Please try again in ${ra>=60?Math.ceil(ra/60)+' minute'+(Math.ceil(ra/60)>1?'s':''):ra+' seconds'}.`, data); }
    if(res.status===403 && !msg){
      const exp = jwtExp(auth.token);
      if(auth.token && exp && exp*1000 < Date.now()){ expireSession(); throw new ApiError(403,'Your session has expired. Please sign in again.',data); }
      if(!auth.token) throw new ApiError(403,'Please sign in to continue.',data);
    }
    throw new ApiError(res.status, friendly(res.status,msg), data);
  } finally { clearTimeout(timer); pending.value--; }
}
