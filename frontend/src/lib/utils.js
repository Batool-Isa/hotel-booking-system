/* utility functions for the app */
export const API_BASE = (import.meta.env.VITE_API_BASE || '').replace(/\/$/, '');

export const store = {
  get(k){ try { return localStorage.getItem(k); } catch { return null; } },
  set(k,v){ try { localStorage.setItem(k,v); } catch {} },
  del(k){ try { localStorage.removeItem(k); } catch {} }
};
export const pad2 = n => String(n).padStart(2,'0');
export const toISO = d => `${d.getFullYear()}-${pad2(d.getMonth()+1)}-${pad2(d.getDate())}`;
export const addDays = (d,n) => { const x = new Date(d); x.setDate(x.getDate()+n); return x; };
export const todayISO = () => toISO(new Date());
export const parseISO = s => { const [y,m,d] = String(s).split('-').map(Number); return new Date(y, m-1, d); };
export const nightsBetween = (a,b) => { if(!a||!b) return 0; const A=parseISO(a), B=parseISO(b); return Math.round((Date.UTC(B.getFullYear(),B.getMonth(),B.getDate())-Date.UTC(A.getFullYear(),A.getMonth(),A.getDate()))/86400000); };
export const fmtDate = s => { if(!s) return ''; const d = parseISO(String(s).slice(0,10)); return isNaN(d) ? s : d.toLocaleDateString('en-GB',{weekday:'short',day:'numeric',month:'short',year:'numeric'}); };
export const fmtDateTime = s => { if(!s) return ''; const d = new Date(s); return isNaN(d) ? s : d.toLocaleString('en-GB',{day:'numeric',month:'short',year:'numeric',hour:'2-digit',minute:'2-digit'}); };
export const money = n => 'BHD ' + Number(n||0).toLocaleString('en-US',{minimumFractionDigits:2,maximumFractionDigits:3});
export const plural = (n,w,p) => `${n} ${n===1?w:(p||w+'s')}`;
export const initials = s => String(s||'?').trim().split(/\s+/).slice(0,2).map(x=>x[0]).join('').toUpperCase() || '?';
export const imgUrl = u => {
  if(!u) return '';
  u = String(u).replace(/\\/g,'/');
  if(/^(https?:|data:|blob:)/i.test(u)) return u;
  u = u.replace(/^\.?\//,'');
  return API_BASE + '/' + u;
};
const GRADS = [['#0a3d91','#3b8cff'],['#0b5d57','#2bb5a0'],['#5b2a86','#a06bd6'],['#8a3b12','#e8913a'],['#1f4e79','#5aa9e6'],['#7a1f3d','#d6567e']];
export const phStyle = seed => { let h=0; for(const c of String(seed||'x')) h=(h*31+c.charCodeAt(0))>>>0; const g=GRADS[h%GRADS.length]; return `background:linear-gradient(135deg,${g[0]},${g[1]})`; };
const AMENITY_ICON = {'wi-fi':'📶','wifi':'📶','swimming pool':'🏊','pool':'🏊','gym':'🏋️','parking':'🅿️','restaurant':'🍽️','spa':'💆','breakfast':'🥐','beach access':'🏖️','room service':'🛎️','air conditioning':'❄️'};
export const amenityIcon = n => AMENITY_ICON[String(n||'').toLowerCase()] || '✔️';
const STATUS = {
  CONFIRMED:['Confirmed','b-green'], COMPLETED:['Completed','b-blue'], CANCELLED:['Cancelled','b-red'], PENDING:['Pending','b-yellow'],
  ACTIVE:['Active','b-green'], INACTIVE:['Inactive','b-gray'], PENDING_APPROVAL:['Waiting for approval','b-yellow'], REJECTED:['Rejected','b-red'], UNDER_MAINTENANCE:['Maintenance','b-yellow']
};
export const statusLabel = s => (STATUS[s]||[s])[0];
export const statusClass = s => (STATUS[s]||[0,'b-gray'])[1];
export const ROLE_LABEL = {CUSTOMER:'Guest',HOTEL_MANAGER:'Hotel manager',ADMIN:'Administrator',STAFF:'Staff'};
export const roleLabel = r => ROLE_LABEL[r]||r;
export const jwtExp = t => { try { const p = JSON.parse(atob(t.split('.')[1].replace(/-/g,'+').replace(/_/g,'/'))); return p.exp||0; } catch { return 0; } };
export const classify = (age, pol) => { if(!pol) return 'ADULT'; if(age<=pol.infantMaxAge) return 'INFANT'; if(age<=pol.childMaxAge) return 'CHILD'; return 'ADULT'; };
export const blank = v => v===null || v===undefined || String(v).trim()==='';
