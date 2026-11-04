<script>
export default {
  props: { size: { type: [Number, String], default: 34 } }
};
</script>

<template>
  <svg class="brand-logo" :width="size" :height="size" viewBox="0 0 64 64" aria-hidden="true"><defs><linearGradient id="vs-logo-g" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stop-color="#2b8cff"/><stop offset="1" stop-color="#00224f"/></linearGradient></defs><rect width="64" height="64" rx="16" fill="url(#vs-logo-g)" stroke="rgba(255,255,255,.35)" stroke-width="2"/><circle cx="46" cy="18" r="6.5" fill="#febb02"/><path d="M13 33 L30 18 L47 33" fill="none" stroke="#fff" stroke-width="5" stroke-linecap="round" stroke-linejoin="round"/><path d="M19 31 V44 H41 V31" fill="none" stroke="#fff" stroke-width="5" stroke-linejoin="round"/><path d="M27 44 V36 H33 V44" fill="#febb02"/><path d="M10 53 q5.5 -4.5 11 0 t11 0 t11 0 t11 0" fill="none" stroke="#7fd3ff" stroke-width="3.5" stroke-linecap="round"/></svg>
</template>
