<script setup>
import { ref, onMounted, onBeforeUnmount, watch } from "vue";

const props = defineProps({
  items: { type: Array, required: true },
  start: { type: Number, default: 0 },
});
const emit = defineEmits(["close"]);

const root = ref(null);
const canvas = ref(null);
const idx = ref(Math.min(props.start, props.items.length - 1));
const loading = ref(true);
const failed = ref(false);
const auto = ref(true);
const full = ref(false);
const touched = ref(false);

const VERT = `
attribute vec2 a;
varying vec2 uv;
void main() {
  uv = a * 0.5 + 0.5;
  gl_Position = vec4(a, 0.0, 1.0);
}`;

const FRAG = `
precision highp float;
uniform sampler2D tex;
uniform vec2 res;
uniform float yaw;
uniform float pitch;
uniform float fov;
uniform float planet;
varying vec2 uv;
const float PI = 3.14159265;
void main() {
  vec2 p = (uv * 2.0 - 1.0) * vec2(res.x / res.y, 1.0);
  vec3 rect = normalize(vec3(p * tan(fov * 0.5), 1.0));
  vec2 q = p * 1.5;
  float r2 = dot(q, q);
  vec3 st = vec3(2.0 * q, 1.0 - r2) / (1.0 + r2);
  vec3 d = normalize(mix(rect, st, planet));
  float cp = cos(pitch);
  float sp = sin(pitch);
  d = vec3(d.x, d.y * cp + d.z * sp, -d.y * sp + d.z * cp);
  float cy = cos(yaw);
  float sy = sin(yaw);
  d = vec3(d.x * cy + d.z * sy, d.y, -d.x * sy + d.z * cy);
  float lon = atan(d.x, d.z);
  float lat = asin(clamp(d.y, -1.0, 1.0));
  gl_FragColor = texture2D(tex, vec2(lon / (2.0 * PI) + 0.5, 0.5 - lat / PI));
}`;

const view = { yaw: 0, pitch: 0, fov: 1.25, planet: 0, vy: 0, vp: 0 };
const FOV_MIN = 0.45;
const FOV_MAX = 1.7;
const PITCH_MAX = Math.PI / 2 - 0.02;

let gl = null;
let prog = null;
let tex = null;
let loc = {};
let raf = 0;
let ro = null;
let intro = 0;
let loadId = 0;
const pointers = new Map();
let pinch = 0;

function compile(type, src) {
  const s = gl.createShader(type);
  gl.shaderSource(s, src);
  gl.compileShader(s);
  if (!gl.getShaderParameter(s, gl.COMPILE_STATUS)) throw new Error(gl.getShaderInfoLog(s));
  return s;
}

function setup() {
  gl = canvas.value.getContext("webgl", { antialias: false });
  if (!gl) throw new Error("no webgl");
  prog = gl.createProgram();
  gl.attachShader(prog, compile(gl.VERTEX_SHADER, VERT));
  gl.attachShader(prog, compile(gl.FRAGMENT_SHADER, FRAG));
  gl.linkProgram(prog);
  if (!gl.getProgramParameter(prog, gl.LINK_STATUS)) throw new Error("link");
  gl.useProgram(prog);
  const buf = gl.createBuffer();
  gl.bindBuffer(gl.ARRAY_BUFFER, buf);
  gl.bufferData(gl.ARRAY_BUFFER, new Float32Array([-1, -1, 3, -1, -1, 3]), gl.STATIC_DRAW);
  const a = gl.getAttribLocation(prog, "a");
  gl.enableVertexAttribArray(a);
  gl.vertexAttribPointer(a, 2, gl.FLOAT, false, 0, 0);
  ["tex", "res", "yaw", "pitch", "fov", "planet"].forEach((n) => (loc[n] = gl.getUniformLocation(prog, n)));
  tex = gl.createTexture();
}

function load() {
  const id = ++loadId;
  loading.value = true;
  const img = new Image();
  img.onload = () => {
    if (id !== loadId || !gl) return;
    gl.bindTexture(gl.TEXTURE_2D, tex);
    gl.pixelStorei(gl.UNPACK_FLIP_Y_WEBGL, false);
    gl.texImage2D(gl.TEXTURE_2D, 0, gl.RGBA, gl.RGBA, gl.UNSIGNED_BYTE, img);
    gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_MIN_FILTER, gl.LINEAR);
    gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_MAG_FILTER, gl.LINEAR);
    gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_S, gl.CLAMP_TO_EDGE);
    gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_T, gl.CLAMP_TO_EDGE);
    loading.value = false;
    startIntro();
  };
  img.onerror = () => {
    if (id !== loadId) return;
    loading.value = false;
    failed.value = true;
  };
  img.src = props.items[idx.value].src;
}

function startIntro() {
  view.yaw = 0;
  view.vy = 0;
  view.vp = 0;
  view.fov = 1.25;
  intro = performance.now();
  view.planet = 1;
  view.pitch = -PITCH_MAX;
}

function ease(t) {
  return 1 - Math.pow(1 - t, 3);
}

function resize() {
  const c = canvas.value;
  if (!c) return;
  const dpr = Math.min(window.devicePixelRatio || 1, 2);
  const w = Math.max(1, Math.round(c.clientWidth * dpr));
  const h = Math.max(1, Math.round(c.clientHeight * dpr));
  if (c.width !== w || c.height !== h) {
    c.width = w;
    c.height = h;
  }
}

function frame(now) {
  raf = requestAnimationFrame(frame);
  if (!gl || loading.value) return;
  if (intro) {
    const t = Math.min((now - intro) / 2600, 1);
    const e = ease(t);
    view.planet = 1 - e;
    view.pitch = -PITCH_MAX * (1 - e);
    view.yaw = (1 - e) * 2.4;
    if (t >= 1) {
      intro = 0;
      view.planet = 0;
      view.pitch = 0;
      view.yaw = 0;
    }
  } else if (pointers.size === 0) {
    if (auto.value) view.yaw += 0.0022;
    view.yaw += view.vy;
    view.pitch = Math.max(-PITCH_MAX, Math.min(PITCH_MAX, view.pitch + view.vp));
    view.vy *= 0.94;
    view.vp *= 0.94;
  }
  resize();
  gl.viewport(0, 0, canvas.value.width, canvas.value.height);
  gl.uniform1i(loc.tex, 0);
  gl.uniform2f(loc.res, canvas.value.width, canvas.value.height);
  gl.uniform1f(loc.yaw, view.yaw);
  gl.uniform1f(loc.pitch, view.pitch);
  gl.uniform1f(loc.fov, view.fov);
  gl.uniform1f(loc.planet, view.planet);
  gl.drawArrays(gl.TRIANGLES, 0, 3);
}

function interact() {
  if (!touched.value) touched.value = true;
  auto.value = false;
  intro = 0;
  view.planet = 0;
}

function down(e) {
  interact();
  canvas.value.setPointerCapture(e.pointerId);
  pointers.set(e.pointerId, { x: e.clientX, y: e.clientY });
  view.vy = 0;
  view.vp = 0;
  if (pointers.size === 2) {
    const [a, b] = [...pointers.values()];
    pinch = Math.hypot(a.x - b.x, a.y - b.y);
  }
}

function move(e) {
  const p = pointers.get(e.pointerId);
  if (!p) return;
  const dx = e.clientX - p.x;
  const dy = e.clientY - p.y;
  p.x = e.clientX;
  p.y = e.clientY;
  if (pointers.size === 2) {
    const [a, b] = [...pointers.values()];
    const d = Math.hypot(a.x - b.x, a.y - b.y);
    if (pinch) zoom((pinch - d) * 0.004);
    pinch = d;
    return;
  }
  const k = view.fov / canvas.value.clientHeight;
  view.vy = -dx * k;
  view.vp = dy * k;
  view.yaw += view.vy;
  view.pitch = Math.max(-PITCH_MAX, Math.min(PITCH_MAX, view.pitch + view.vp));
}

function up(e) {
  pointers.delete(e.pointerId);
  pinch = 0;
}

function zoom(d) {
  view.fov = Math.max(FOV_MIN, Math.min(FOV_MAX, view.fov + d));
}

function wheel(e) {
  interact();
  zoom(e.deltaY * 0.0012);
}

function toggleAuto() {
  intro = 0;
  view.planet = 0;
  auto.value = !auto.value;
}

function pick(i) {
  if (i === idx.value) return;
  idx.value = i;
  failed.value = false;
  load();
}

function step(d) {
  const n = props.items.length;
  pick((idx.value + d + n) % n);
}

function toggleFull() {
  const el = root.value;
  if (!document.fullscreenElement) {
    if (el.requestFullscreen) el.requestFullscreen().catch(() => {});
  } else {
    document.exitFullscreen();
  }
}

function onFull() {
  full.value = !!document.fullscreenElement;
}

function key(e) {
  if (e.key === "Escape") emit("close");
  else if (e.key === "ArrowLeft") {
    interact();
    view.vy = -0.02;
  } else if (e.key === "ArrowRight") {
    interact();
    view.vy = 0.02;
  } else if (e.key === "ArrowUp") {
    interact();
    view.vp = 0.015;
  } else if (e.key === "ArrowDown") {
    interact();
    view.vp = -0.015;
  } else if (e.key === "+" || e.key === "=") zoom(-0.12);
  else if (e.key === "-") zoom(0.12);
  else if (e.key === "f" || e.key === "F") toggleFull();
}

onMounted(() => {
  try {
    setup();
  } catch (err) {
    failed.value = true;
    loading.value = false;
    return;
  }
  load();
  raf = requestAnimationFrame(frame);
  ro = new ResizeObserver(resize);
  ro.observe(canvas.value);
  window.addEventListener("keydown", key);
  document.addEventListener("fullscreenchange", onFull);
  document.body.style.overflow = "hidden";
});

onBeforeUnmount(() => {
  cancelAnimationFrame(raf);
  if (ro) ro.disconnect();
  window.removeEventListener("keydown", key);
  document.removeEventListener("fullscreenchange", onFull);
  document.body.style.overflow = "";
  if (document.fullscreenElement) document.exitFullscreen();
  if (gl) {
    gl.deleteTexture(tex);
    const ext = gl.getExtension("WEBGL_lose_context");
    if (ext) ext.loseContext();
  }
  gl = null;
});

watch(
  () => props.items,
  () => {
    idx.value = 0;
    failed.value = false;
    if (gl) load();
  },
);
</script>

<template>
  <div ref="root" class="pano" role="dialog" aria-label="360 degree view" @click.self="emit('close')">
    <canvas
      ref="canvas"
      class="pano-canvas"
      :class="{ ready: !loading && !failed }"
      @pointerdown="down"
      @pointermove="move"
      @pointerup="up"
      @pointercancel="up"
      @wheel.prevent="wheel"
    ></canvas>

    <div v-if="loading" class="pano-load"><span class="pano-spin"></span>Loading 360° view</div>
    <div v-if="failed" class="pano-load">
      This 360° view could not be loaded on this device.
    </div>

    <div class="pano-top">
      <div class="pano-title">
        <span class="pano-badge">360°</span>
        <span>{{ items[idx].title }}</span>
        <span v-if="items[idx].sample" class="pano-sample">Sample image</span>
      </div>
      <div class="pano-tools">
        <button class="pano-btn" :class="{ on: auto }" :aria-pressed="auto" title="Auto rotate" @click="toggleAuto">
          <icon name="refresh" :size="18"></icon>
        </button>
        <button class="pano-btn" :title="full ? 'Exit full screen' : 'Full screen'" @click="toggleFull">
          <icon name="expand" :size="18"></icon>
        </button>
        <button class="pano-btn" title="Close" @click="emit('close')">
          <icon name="x" :size="18"></icon>
        </button>
      </div>
    </div>

    <button v-if="items.length > 1" class="pano-nav prev" aria-label="Previous view" @click="step(-1)">‹</button>
    <button v-if="items.length > 1" class="pano-nav next" aria-label="Next view" @click="step(1)">›</button>

    <div class="pano-bottom">
      <div v-if="items.length > 1" class="pano-tabs">
        <button
          v-for="(it, i) in items"
          :key="it.src"
          class="pano-tab"
          :class="{ on: i === idx }"
          @click="pick(i)"
        >
          {{ it.title }}
        </button>
      </div>
      <p class="pano-hint" :class="{ gone: touched }">Drag to look around · scroll or pinch to zoom · Esc to close</p>
    </div>
  </div>
</template>
