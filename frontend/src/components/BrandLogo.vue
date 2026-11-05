<script>
export default {
  props: {
    size: { type: [Number, String], default: 34 },
    dark: { type: Boolean, default: false }
  }
};
</script>

<template>
  <svg class="brand-logo" :width="size" :height="size" viewBox="0 0 64 64" aria-hidden="true">
    <rect width="64" height="64" rx="14" :fill="dark ? '#003b95' : '#fff'" />
    <path d="M17 19 L32 46 L47 19" fill="none" :stroke="dark ? '#fff' : '#003b95'" stroke-width="7.5" stroke-linecap="round" stroke-linejoin="round" />
    <circle cx="48" cy="46" r="5" fill="#febb02" />
  </svg>
</template>
