<script setup>
import { ref, reactive, computed, watch, nextTick } from "vue";
import { Client } from "@stomp/stompjs";
import {
  API_BASE,
  store,
  toISO,
  addDays,
  todayISO,
  nightsBetween,
  fmtDate,
  money,
  plural,
  initials,
  imgUrl,
  ROLE_LABEL,
  jwtExp,
  classify,
  blank,
} from "@/lib/utils";
import { R, useForm, focusFirstError } from "@/lib/forms";
import {
  api,
  ApiError,
  pending,
  auth,
  role,
  isCustomer,
  isManager,
  isAdmin,
  setExpireHandler,
} from "@/lib/api";
import { toasts, toast, dismiss, dlg, ask, answer } from "@/lib/ui";

/* ---------------- routers ---------------- */
const route = reactive({ name: "home", params: {}, query: {} });
const page = reactive({ loading: true, error: "" });
const menuOpen = ref(false),
  feedOpen = ref(false);
let navSeq = 0,
  afterLogin = "";
let readyResolve;
const authReady = new Promise((r) => {
  readyResolve = r;
});
const TITLES = {
  home: "Find your next stay",
  search: "Search results",
  explore: "Explore hotels",
  hotel: "Hotel",
  book: "Complete your booking",
  login: "Sign in",
  register: "Create account",
  forgot: "Forgot password",
  "reset-password": "Reset password",
  "verify-email": "Verify email",
  "check-email": "Check your email",
  profile: "My profile",
  bookings: "My bookings",
  manage: "Manager portal",
  "manage-new": "Add a hotel",
  "manage-hotel": "Manage hotel",
  admin: "Admin panel",
};

function parseHash() {
  const h = location.hash.replace(/^#/, "") || "/";
  const [pathPart, qs = ""] = h.split("?");
  const query = Object.fromEntries(new URLSearchParams(qs));
  const segs = pathPart.split("/").filter(Boolean);
  const params = {};
  let name = "home";
  switch (segs[0]) {
    case undefined:
      break;
    case "search":
    case "explore":
    case "login":
    case "register":
    case "forgot":
    case "reset-password":
    case "verify-email":
    case "check-email":
    case "profile":
    case "bookings":
    case "admin":
      name = segs[0];
      break;
    case "hotel":
      name = "hotel";
      params.id = segs[1];
      break;
    case "book":
      name = "book";
      params.id = segs[1];
      break;
    case "manage":
      if (segs[1] === "new") name = "manage-new";
      else if (segs[1] === "hotel") {
        name = "manage-hotel";
        params.id = segs[2];
      } else name = "manage";
      break;
    default:
      name = "notfound";
  }
  return { name, params, query };
}
function go(path) {
  const h = "#" + path;
  if (location.hash === h) applyRoute();
  else location.hash = h;
}
function silentUrl(path) {
  try {
    history.replaceState(null, "", "#" + path);
  } catch {}
}
const href = (p) => "#" + p;

function guard(r) {
  const n = r.name;
  if (
    [
      "profile",
      "bookings",
      "book",
      "manage",
      "manage-new",
      "manage-hotel",
      "admin",
    ].includes(n) &&
    !auth.user
  )
    return { to: "/login", login: true, msg: "Please sign in to continue." };
  if (["login", "register", "forgot"].includes(n) && auth.user)
    return { to: "/" };
  if (n === "bookings" && !isCustomer.value)
    return {
      to: "/",
      msg: "“My bookings” is for guest accounts.",
      type: "warn",
    };
  if (n === "book" && !isCustomer.value)
    return {
      to: `/hotel/${r.params.id}`,
      msg: "Only guest accounts can make bookings. Sign in with a guest account to continue.",
      type: "warn",
    };
  if (
    (n === "manage" || n === "manage-new" || n === "manage-hotel") &&
    !(isManager.value || isAdmin.value)
  )
    return {
      to: "/",
      msg: "You don't have access to the manager portal.",
      type: "warn",
    };
  if (n === "manage" && isAdmin.value) return { to: "/admin?tab=hotels" };
  if (n === "admin" && !isAdmin.value)
    return {
      to: "/",
      msg: "The admin panel is for administrators only.",
      type: "warn",
    };
  return null;
}
async function applyRoute() {
  const seq = ++navSeq;
  await authReady;
  if (seq !== navSeq) return;
  const r = parseHash();
  const g = guard(r);
  if (g) {
    if (g.login) afterLogin = location.hash;
    if (g.msg) toast(g.msg, g.type || "info");
    go(g.to);
    return;
  }
  route.name = r.name;
  route.params = r.params;
  route.query = r.query;
  menuOpen.value = false;
  feedOpen.value = false;
  document.title = (TITLES[r.name] || "VibeStay") + " – VibeStay";
  window.scrollTo(0, 0);
  page.error = "";
  page.loading = true;
  try {
    await loadRoute(r, seq);
  } catch (e) {
    if (seq === navSeq) page.error = e.message || "Something went wrong.";
  } finally {
    if (seq === navSeq) page.loading = false;
  }
}
async function loadRoute(r, seq) {
  lk.data = null;
  lk.err = "";
  lk.id = "";
  switch (r.name) {
    case "home":
      return loadHome();
    case "search":
      return loadSearch(r.query);
    case "explore":
      return loadExplore(0);
    case "hotel":
      return loadHotel(r.params.id, r.query);
    case "book":
      return loadBook(r.params.id, r.query);
    case "bookings":
      return loadBookings(0);
    case "profile":
      return loadProfilePage();
    case "manage":
      return loadManagerHotels(0);
    case "manage-new":
      return initHotelForm(null);
    case "manage-hotel":
      return loadManageHotel(r.params.id, r.query.tab || "overview");
    case "admin":
      return loadAdmin(r.query.tab || "overview");
    case "verify-email":
      return runVerify(r.query.token);
    case "reset-password":
      resetF.reset({ token: r.query.token || "" });
      return;
    case "login":
      loginF.reset();
      return;
    case "register":
      regF.reset();
      regImage.file = null;
      regImage.preview = "";
      regImage.error = "";
      return;
    case "forgot":
      forgotF.reset();
      forgotSent.value = false;
      return;
    case "check-email":
      return;
  }
}

/* ---------------- session ---------------- */
const profileImage = computed(() =>
  auth.user ? imgUrl(auth.user.profileImageUrl) : "",
);
const userInitials = computed(() =>
  auth.user
    ? initials(
        (auth.user.firstName || "") + " " + (auth.user.lastName || "") ||
          auth.user.username,
      )
    : "",
);
async function loadProfile() {
  const u = await api("GET", "/auth/users/profile");
  auth.user = u;
  return u;
}
function clearSession() {
  auth.token = "";
  auth.user = null;
  store.del("vs_token");
  disconnectWs();
}
function expireSession() {
  if (!auth.token) return;
  clearSession();
  toast("Your session has expired. Please sign in again.", "warn");
  afterLogin = location.hash;
  go("/login");
}
function logout() {
  clearSession();
  known.clear();
  feed.value = [];
  unread.value = 0;
  toast("You have been signed out. See you soon!", "info");
  go("/");
}
setExpireHandler(expireSession);

/* ---------------- forms: auth ---------------- */
const loginF = useForm(
  { email: "", password: "" },
  { email: [R.req("Email"), R.email()], password: [R.req("Password")] },
);
async function doLogin() {
  loginF.banner = "";
  if (!loginF.validateAll()) return;
  loginF.busy = true;
  try {
    const data = await api("POST", "/auth/users/login", {
      auth: false,
      body: {
        email: loginF.values.email.trim(),
        password: loginF.values.password,
      },
    });
    const token =
      data && typeof data === "object"
        ? data.message || data.token || data.jwt
        : data;
    if (typeof token !== "string" || token.split(".").length !== 3)
      throw new ApiError(
        200,
        "Signed in, but the server did not return a valid token.",
      );
    auth.token = token;
    store.set("vs_token", token);
    await loadProfile();
    connectWs();
    toast(`Welcome back, ${auth.user.firstName || auth.user.username}!`);
    loginF.reset();
    const dest = afterLogin;
    afterLogin = "";
    if (dest && !/^#\/(login|register)/.test(dest)) location.hash = dest;
    else go(isManager.value ? "/manage" : isAdmin.value ? "/admin" : "/");
  } catch (e) {
    clearSession();
    loginF.banner = e.message;
  } finally {
    loginF.busy = false;
  }
}

const regF = useForm(
  {
    firstName: "",
    lastName: "",
    username: "",
    email: "",
    phone: "",
    password: "",
    confirmedPassword: "",
  },
  {
    firstName: [R.req("First name")],
    lastName: [R.req("Last name")],
    username: [R.req("Username")],
    email: [R.req("Email"), R.email()],
    phone: [R.req("Phone number")],
    password: [R.req("Password"), R.min(8, "Password")],
    confirmedPassword: [
      R.req("Confirm password"),
      R.same("password", "Passwords do not match"),
    ],
  },
);
const regImage = reactive({ file: null, preview: "", error: "" });
const pwStrength = computed(() => {
  const p = regF.values.password || "";
  let s = 0;
  if (p.length >= 8) s++;
  if (p.length >= 12) s++;
  if (/[A-Z]/.test(p) && /[a-z]/.test(p)) s++;
  if (/\d/.test(p)) s++;
  if (/[^A-Za-z0-9]/.test(p)) s++;
  const lvl = [
    ["Too short", "#c5192d", 10],
    ["Weak", "#e8590c", 30],
    ["Okay", "#e8a900", 55],
    ["Good", "#4c9a2a", 80],
    ["Strong", "#008234", 100],
    ["Strong", "#008234", 100],
  ][p ? s : 0];
  return { label: p ? lvl[0] : "", color: lvl[1], pct: p ? lvl[2] : 0 };
});
function pickImage(e, target) {
  const f = e.target.files && e.target.files[0];
  target.error = "";
  if (!f) return;
  if (!/^image\/(jpeg|jpg|png|webp)$/.test(f.type)) {
    target.error = "Please choose an image file (JPG, PNG or WebP).";
    e.target.value = "";
    return;
  }
  if (f.size > 1024 * 1024) {
    target.error =
      "That photo is " +
      (f.size / 1048576).toFixed(1) +
      " MB – the server accepts up to 1 MB. Please choose a smaller one.";
    e.target.value = "";
    return;
  }
  target.file = f;
  if (target.preview) URL.revokeObjectURL(target.preview);
  target.preview = URL.createObjectURL(f);
}
function mapServerErr(form, msg, pairs) {
  for (const [re, field] of pairs) {
    if (re.test(msg)) {
      form.errors[field] = msg;
      return true;
    }
  }
  return false;
}
async function doRegister() {
  regF.banner = "";
  let ok = regF.validateAll();
  if (!ok) return;
  const fd = new FormData();
  const v = regF.values;
  for (const k of [
    "username",
    "email",
    "password",
    "confirmedPassword",
    "firstName",
    "lastName",
    "phone",
  ])
    fd.append(
      k,
      typeof v[k] === "string" && k !== "password" && k !== "confirmedPassword"
        ? v[k].trim()
        : v[k],
    );
  if (regImage.file) fd.append("image", regImage.file); // the photo is optional
  regF.busy = true;
  try {
    await api("POST", "/auth/users/register", { auth: false, form: fd });
    const em = v.email.trim();
    regF.reset();
    regImage.file = null;
    regImage.preview = "";
    go("/check-email?email=" + encodeURIComponent(em));
  } catch (e) {
    if (e.status === 409) {
      regF.errors.email =
        "An account with this email already exists. Try signing in instead.";
      regF.banner = "Please fix the highlighted field and try again.";
    } else if (
      !mapServerErr(regF, e.message, [
        [/email/i, "email"],
        [/do not match|mismatch/i, "confirmedPassword"],
        [/password/i, "password"],
        [/username/i, "username"],
      ])
    )
      regF.banner = e.message;
    else regF.banner = "Please fix the highlighted field and try again.";
  } finally {
    regF.busy = false;
  }
}
const verify = reactive({ state: "idle", msg: "", token: "" });
const extractToken = (s) => {
  const m = String(s || "").match(/token=([A-Za-z0-9-]+)/);
  return m ? m[1] : String(s || "").trim();
};
async function runVerify(token) {
  verify.token = token || "";
  verify.msg = "";
  verify.state = token ? "loading" : "idle";
  if (!token) return;
  try {
    const t = await api("GET", "/auth/users/verify-email", {
      auth: false,
      query: { token },
    });
    verify.state = "ok";
    verify.msg = typeof t === "string" ? t : "Email verified";
  } catch (e) {
    verify.state = "error";
    verify.msg = e.message;
  }
}
function submitVerifyToken() {
  const t = extractToken(verify.token);
  if (!t) {
    verify.msg = "Paste the link or token from your email.";
    verify.state = "error";
    return;
  }
  go("/verify-email?token=" + encodeURIComponent(t));
}

const forgotF = useForm({ email: "" }, { email: [R.req("Email"), R.email()] });
const forgotSent = ref(false);
async function doForgot() {
  forgotF.banner = "";
  if (!forgotF.validateAll()) return;
  forgotF.busy = true;
  try {
    await api("POST", "/auth/users/forgot-password", {
      auth: false,
      body: { email: forgotF.values.email.trim() },
    });
    forgotSent.value = true;
  } catch (e) {
    forgotF.banner = e.message;
  } finally {
    forgotF.busy = false;
  }
}
const resetF = useForm(
  { token: "", password: "", confirmedPassword: "" },
  {
    token: [R.req("Reset token")],
    password: [R.req("New password"), R.min(8, "Password")],
    confirmedPassword: [
      R.req("Confirm password"),
      R.same("password", "Passwords do not match"),
    ],
  },
);
async function doReset() {
  resetF.banner = "";
  if (!resetF.validateAll()) return;
  resetF.busy = true;
  try {
    const r = await api("POST", "/auth/users/reset-link", {
      auth: false,
      query: { token: extractToken(resetF.values.token) },
      body: {
        password: resetF.values.password,
        confirmedPassword: resetF.values.confirmedPassword,
      },
    });
    toast((r && r.message) || "Password reset. You can sign in now.");
    resetF.reset();
    go("/login");
  } catch (e) {
    if (
      !mapServerErr(resetF, e.message, [
        [/token/i, "token"],
        [/do not match/i, "confirmedPassword"],
      ])
    )
      resetF.banner = e.message;
  } finally {
    resetF.busy = false;
  }
}

/* ---------------- profile ---------------- */
const profileF = useForm(
  { username: "", firstName: "", lastName: "", phone: "" },
  {
    username: [R.req("Username")],
    firstName: [R.req("First name")],
    lastName: [R.req("Last name")],
    phone: [R.req("Phone number")],
  },
);
const pwF = useForm(
  { currentPassword: "", newPassword: "", confirmedPassword: "" },
  {
    currentPassword: [R.req("Current password")],
    newPassword: [R.req("New password"), R.min(8, "Password")],
    confirmedPassword: [
      R.req("Confirm password"),
      R.same("newPassword", "Passwords do not match"),
    ],
  },
);
const avatarUp = reactive({ file: null, preview: "", error: "", busy: false });
async function loadProfilePage() {
  const u = await loadProfile();
  profileF.reset({
    username: u.username || "",
    firstName: u.firstName || "",
    lastName: u.lastName || "",
    phone: u.phone || "",
  });
  pwF.reset();
  avatarUp.file = null;
  avatarUp.preview = "";
  avatarUp.error = "";
}
async function saveProfile() {
  profileF.banner = "";
  if (!profileF.validateAll()) return;
  profileF.busy = true;
  try {
    const v = profileF.values;
    const r = await api("PUT", "/auth/users/profile", {
      body: {
        username: v.username.trim(),
        firstName: v.firstName.trim(),
        lastName: v.lastName.trim(),
        phone: v.phone.trim(),
      },
    });
    await loadProfile();
    toast((r && r.message) || "Profile updated");
  } catch (e) {
    profileF.banner = e.message;
  } finally {
    profileF.busy = false;
  }
}
async function saveAvatar() {
  if (!avatarUp.file) {
    avatarUp.error = "Choose a photo first.";
    return;
  }
  avatarUp.busy = true;
  avatarUp.error = "";
  try {
    const fd = new FormData();
    fd.append("image", avatarUp.file);
    const r = await api("PUT", "/auth/users/profile/image", { form: fd });
    await loadProfile();
    avatarUp.file = null;
    avatarUp.preview = "";
    toast((r && r.message) || "Photo updated");
  } catch (e) {
    avatarUp.error = e.message;
  } finally {
    avatarUp.busy = false;
  }
}
async function changePw() {
  pwF.banner = "";
  if (!pwF.validateAll()) return;
  pwF.busy = true;
  try {
    const r = await api("POST", "/auth/users/change-password", {
      body: { ...pwF.values },
    });
    pwF.reset();
    toast((r && r.message) || "Password changed");
  } catch (e) {
    if (
      !mapServerErr(pwF, e.message, [
        [/current/i, "currentPassword"],
        [/do not match/i, "confirmedPassword"],
      ])
    )
      pwF.banner = e.message;
  } finally {
    pwF.busy = false;
  }
}

/* ---------------- home / explore ---------------- */
const home = reactive({ hotels: [] });
let homeP = null,
  homeAt = 0;
function loadHome() {
  if (homeP && Date.now() - homeAt < 60000) return homeP;
  homeAt = Date.now();
  homeP = api("GET", "/api/hotels", {
    auth: false,
    query: { size: 60, sort: "name,asc" },
  })
    .then((list) => {
      home.hotels = Array.isArray(list) ? list : [];
      home.hotels.forEach((h) => {
        hc[h.id] = h;
      });
      return list;
    })
    .catch((e) => {
      homeP = null;
      throw e;
    });
  return homeP;
}
const cities = computed(() =>
  [...new Set(home.hotels.map((h) => h.city).filter(Boolean))].sort(),
);
const destinations = computed(() => {
  const m = {};
  home.hotels.forEach((h) => {
    const k = h.city;
    if (!k) return;
    (m[k] = m[k] || { city: k, country: h.country, count: 0, img: "" }).count++;
    if (!m[k].img && h.images && h.images[0]) m[k].img = h.images[0].imageUrl;
  });
  return Object.values(m)
    .sort((a, b) => b.count - a.count)
    .slice(0, 6);
});
const featured = computed(() => home.hotels.slice(0, 6));
const defaultQS = (city) =>
  qsOf({
    city: city || "",
    checkIn: toISO(addDays(new Date(), 1)),
    checkOut: toISO(addDays(new Date(), 3)),
    adults: 2,
    children: 0,
  });

const viewMode = reactive({ ex: "list", sr: "list" });
const exPins = computed(() =>
  ex.items.map((h) => ({
    id: h.id,
    name: h.name,
    lat: h.latitude,
    lng: h.longitude,
    sub: [h.address, h.city].filter(Boolean).join(", "),
    href: "#/hotel/" + h.id,
  })),
);
const ex = reactive({
  items: [],
  page: 0,
  size: 9,
  hasNext: false,
  loading: false,
  error: "",
  q: { search: "", city: "", country: "" },
  sort: "name,asc",
});
async function loadExplore(p = 0) {
  ex.loading = true;
  ex.error = "";
  try {
    const list = await api("GET", "/api/hotels", {
      auth: false,
      query: {
        search: ex.q.search.trim(),
        city: ex.q.city.trim(),
        country: ex.q.country.trim(),
        page: p,
        size: ex.size,
        sort: ex.sort,
      },
    });
    ex.items = Array.isArray(list) ? list : [];
    ex.page = p;
    ex.hasNext = ex.items.length === ex.size;
    ex.items.forEach((h) => {
      hc[h.id] = h;
    });
  } catch (e) {
    ex.error = e.message;
    ex.items = [];
  } finally {
    ex.loading = false;
  }
}
function clearExplore() {
  ex.q.search = "";
  ex.q.city = "";
  ex.q.country = "";
  loadExplore(0);
}

/* ---------------- search ---------------- */
const minIn = computed(() => toISO(addDays(new Date(), 1)));
const searchF = useForm(
  {
    city: "",
    checkIn: toISO(addDays(new Date(), 1)),
    checkOut: toISO(addDays(new Date(), 3)),
    adults: 2,
    children: 0,
  },
  {
    city: [R.max(100, "City")],
    checkIn: [
      R.req("Check-in date"),
      (v) =>
        v && v <= todayISO()
          ? "Check-in must be a future date (tomorrow or later)"
          : "",
    ],
    checkOut: [
      R.req("Check-out date"),
      (v, d) =>
        v && v <= todayISO()
          ? "Check-out must be a future date"
          : v && d.checkIn && v <= d.checkIn
            ? "Check-out must be after check-in"
            : "",
    ],
    adults: [(v) => (!(Number(v) >= 1) ? "At least 1 adult is required" : "")],
    children: [(v) => (Number(v) < 0 ? "Children cannot be negative" : "")],
  },
);
function qsOf(v) {
  const p = new URLSearchParams();
  if (v.city && v.city.trim()) p.set("city", v.city.trim());
  p.set("in", v.checkIn);
  p.set("out", v.checkOut);
  p.set("a", v.adults);
  p.set("c", v.children);
  return p.toString();
}
function submitSearch() {
  if (!searchF.validateAll()) return;
  go("/search?" + qsOf(searchF.values));
}
const hc = reactive({});
const imgOf = (id) =>
  hc[id] && hc[id].images && hc[id].images[0] ? hc[id].images[0].imageUrl : "";
async function enrichHotels(ids) {
  const todo = [...new Set(ids)].filter((id) => !hc[id]).slice(0, 40);
  await Promise.all(
    todo.map(async (id) => {
      try {
        hc[id] = await api("GET", `/api/hotels/${id}`, { auth: false });
      } catch {
        hc[id] = { _failed: true };
      }
    }),
  );
}
const sr = reactive({
  items: null,
  loading: false,
  error: "",
  q: null,
  priceMax: null,
  priceCap: 0,
  types: [],
  fit: false,
  sort: "price",
});
const byPrice = (a, b) => Number(a.pricePerNight) - Number(b.pricePerNight);
async function loadSearch(q) {
  searchF.reset({
    city: q.city || "",
    ...(q.in ? { checkIn: q.in } : {}),
    ...(q.out ? { checkOut: q.out } : {}),
    adults: Number(q.a) || 2,
    children: Math.max(0, Number(q.c) || 0),
  });
  sr.items = null;
  sr.error = "";
  if (!searchF.validateAll()) return;
  const v = searchF.values;
  sr.loading = true;
  try {
    const data = await api("POST", "/api/availability/search", {
      auth: false,
      body: {
        checkIn: v.checkIn,
        checkOut: v.checkOut,
        adults: Number(v.adults),
        children: Number(v.children),
        city: (v.city || "").trim() || null,
      },
    });
    const items = (Array.isArray(data) ? data : []).map((h) => {
      const rooms = (h.rooms || []).slice().sort(byPrice);
      return { ...h, rooms };
    });
    sr.q = {
      city: (v.city || "").trim(),
      checkIn: v.checkIn,
      checkOut: v.checkOut,
      adults: Number(v.adults),
      children: Number(v.children),
    };
    sr.items = items;
    const prices = items.flatMap((h) =>
      h.rooms.map((r) => Number(r.pricePerNight)),
    );
    sr.priceCap = prices.length ? Math.ceil(Math.max(...prices)) : 0;
    sr.priceMax = sr.priceCap;
    sr.types = [];
    sr.fit = false;
    sr.sort = "price";
    enrichHotels(items.map((i) => i.hotelId));
  } catch (e) {
    sr.error = e.message;
  } finally {
    sr.loading = false;
  }
}
const roomFits = (r, a, c) =>
  r.maxAdults >= a && r.maxChildren >= c && r.maxOccupancy >= a + c;
const typeOptions = computed(() => {
  const s = new Set();
  (sr.items || []).forEach((h) =>
    h.rooms.forEach((r) => s.add(r.roomTypeName)),
  );
  return [...s].filter(Boolean).sort();
});
const results = computed(() => {
  const a = sr.q ? sr.q.adults : 1,
    c = sr.q ? sr.q.children : 0;
  let list = (sr.items || [])
    .map((h) => {
      const shown = h.rooms.filter(
        (r) =>
          (!sr.types.length || sr.types.includes(r.roomTypeName)) &&
          (sr.priceMax == null || Number(r.pricePerNight) <= sr.priceMax),
      );
      return {
        ...h,
        shown,
        fitCount: shown.filter((r) => roomFits(r, a, c)).length,
        from: shown.length ? Number(shown[0].pricePerNight) : 0,
      };
    })
    .filter((h) => h.shown.length);
  if (sr.fit) list = list.filter((h) => h.fitCount > 0);
  const cmp = {
    price: (x, y) => x.from - y.from,
    "price-desc": (x, y) => y.from - x.from,
    name: (x, y) => x.hotelName.localeCompare(y.hotelName),
    rooms: (x, y) => y.shown.length - x.shown.length,
  }[sr.sort];
  return list.sort(cmp);
});
const srPins = computed(() =>
  results.value.map((h) => {
    const d = hc[h.hotelId] || {};
    return {
      id: h.hotelId,
      name: h.hotelName,
      lat: d.latitude,
      lng: d.longitude,
      label: money(h.from),
      sub:
        [h.address, h.city].filter(Boolean).join(", ") +
        " · from " +
        money(h.from) +
        "/night",
      href: hotelHref(h.hotelId),
    };
  }),
);
const srNights = computed(() =>
  sr.q ? nightsBetween(sr.q.checkIn, sr.q.checkOut) : 0,
);
const hotelHref = (id) =>
  "#/hotel/" +
  id +
  (sr.q
    ? "?" +
      new URLSearchParams({
        in: sr.q.checkIn,
        out: sr.q.checkOut,
        a: sr.q.adults,
        c: sr.q.children,
      }).toString()
    : "");
function resetFilters() {
  sr.priceMax = sr.priceCap;
  sr.types = [];
  sr.fit = false;
}
function toggleType(t) {
  const i = sr.types.indexOf(t);
  if (i >= 0) sr.types.splice(i, 1);
  else sr.types.push(t);
}

/* ---------------- hotel page ---------------- */
const hp = reactive({
  hotel: null,
  policy: null,
  policyMissing: false,
  images: [],
  rooms: null,
  roomsLoading: false,
  roomsError: "",
  sel: [],
  q: null,
  lightbox: -1,
});
async function loadHotel(id, q) {
  Object.assign(hp, {
    hotel: null,
    policy: null,
    policyMissing: false,
    images: [],
    rooms: null,
    roomsError: "",
    sel: [],
    q: null,
    lightbox: -1,
  });
  const [h, pol, imgs] = await Promise.all([
    api("GET", `/api/hotels/${id}`, { auth: false }),
    api("GET", `/api/hotels/${id}/child-policy`, { auth: false }).catch((e) =>
      e.status === 404 ? "missing" : null,
    ),
    api("GET", `/api/hotels/${id}/images`, { auth: false }).catch(() => null),
  ]);
  hp.hotel = h;
  hc[h.id] = h;
  hp.policy = pol && pol !== "missing" ? pol : null;
  hp.policyMissing = pol === "missing";
  hp.images = Array.isArray(imgs) && imgs.length ? imgs : h.images || [];
  searchF.reset({
    city: h.city || "",
    ...(q.in ? { checkIn: q.in } : {}),
    ...(q.out ? { checkOut: q.out } : {}),
    adults: Number(q.a) || 2,
    children: Math.max(0, Number(q.c) || 0),
  });
  if (q.in && q.out && searchF.validateAll())
    await loadHotelRooms(searchF.values);
}
async function loadHotelRooms(v) {
  hp.roomsLoading = true;
  hp.roomsError = "";
  hp.sel = [];
  hp.q = {
    checkIn: v.checkIn,
    checkOut: v.checkOut,
    adults: Number(v.adults),
    children: Number(v.children),
  };
  try {
    const data = await api("POST", "/api/availability/search", {
      auth: false,
      body: {
        checkIn: v.checkIn,
        checkOut: v.checkOut,
        adults: Number(v.adults),
        children: Number(v.children),
        city: hp.hotel.city,
      },
    });
    const mine = (Array.isArray(data) ? data : []).find(
      (x) => Number(x.hotelId) === Number(hp.hotel.id),
    );
    hp.rooms = mine ? mine.rooms.slice().sort(byPrice) : [];
  } catch (e) {
    hp.roomsError = e.message;
    hp.rooms = null;
  } finally {
    hp.roomsLoading = false;
  }
}
async function checkHotelDates() {
  if (!searchF.validateAll()) return;
  const v = searchF.values;
  silentUrl(
    `/hotel/${hp.hotel.id}?` +
      new URLSearchParams({
        in: v.checkIn,
        out: v.checkOut,
        a: v.adults,
        c: v.children,
      }).toString(),
  );
  await loadHotelRooms(v);
}
const hpNights = computed(() =>
  hp.q ? nightsBetween(hp.q.checkIn, hp.q.checkOut) : 0,
);
const hpSelected = computed(() =>
  (hp.rooms || []).filter((r) => hp.sel.includes(r.id)),
);
const hpTotal = computed(
  () =>
    hpSelected.value.reduce((s, r) => s + Number(r.pricePerNight), 0) *
    hpNights.value,
);
function toggleRoom(id) {
  const i = hp.sel.indexOf(id);
  if (i >= 0) hp.sel.splice(i, 1);
  else hp.sel.push(id);
}
function reserve() {
  if (!hp.sel.length) {
    toast("Select at least one room first.", "warn");
    return;
  }
  const url =
    `/book/${hp.hotel.id}?` +
    new URLSearchParams({
      in: hp.q.checkIn,
      out: hp.q.checkOut,
      a: hp.q.adults,
      c: hp.q.children,
      rooms: hp.sel.join(","),
    }).toString();
  if (!auth.user) {
    afterLogin = "#" + url;
    toast("Sign in or create an account to complete your booking.", "info");
    go("/login");
    return;
  }
  go(url);
}
const mapSrc = computed(() => {
  const h = hp.hotel;
  if (!h || h.latitude == null || h.longitude == null) return "";
  const la = Number(h.latitude),
    lo = Number(h.longitude);
  if (Number.isNaN(la) || Number.isNaN(lo)) return "";
  return `https://www.openstreetmap.org/export/embed.html?bbox=${lo - 0.012}%2C${la - 0.007}%2C${lo + 0.012}%2C${la + 0.007}&layer=mapnik&marker=${la}%2C${lo}`;
});
const mapsLink = computed(() => {
  const h = hp.hotel;
  if (!h) return "#";
  const q =
    h.latitude != null && h.longitude != null
      ? `${h.latitude},${h.longitude}`
      : `${h.name} ${h.address} ${h.city}`;
  return (
    "https://www.google.com/maps/search/?api=1&query=" + encodeURIComponent(q)
  );
});
const policyText = (pol) =>
  pol
    ? {
        infant: `Infants: 0–${pol.infantMaxAge} years`,
        child: `Children: ${pol.infantMaxAge + 1}–${pol.childMaxAge} years`,
        adult: `Adults: ${pol.childMaxAge + 1} years and over`,
      }
    : null;
const hotelPhotos = computed(() => hp.images.map((i) => i.imageUrl));

/* ---------------- booking ---------------- */
const bk = reactive({
  hotel: null,
  policy: null,
  noPolicy: false,
  rooms: [],
  q: null,
  missing: [],
  text: "",
  busy: false,
  banner: "",
  done: null,
  errs: {},
  touched: false,
});
async function loadBook(id, q) {
  Object.assign(bk, {
    hotel: null,
    policy: null,
    noPolicy: false,
    rooms: [],
    q: null,
    missing: [],
    text: "",
    busy: false,
    banner: "",
    done: null,
    errs: {},
    touched: false,
  });
  const ids = String(q.rooms || "")
    .split(",")
    .map(Number)
    .filter(Boolean);
  if (!q.in || !q.out || !ids.length) {
    toast("Pick your dates and rooms first.", "info");
    go(`/hotel/${id}`);
    return;
  }
  const v = {
    city: "",
    checkIn: q.in,
    checkOut: q.out,
    adults: Number(q.a) || 1,
    children: Math.max(0, Number(q.c) || 0),
  };
  searchF.reset(v);
  if (!searchF.validateAll()) {
    toast("Those dates are no longer valid – please choose new ones.", "warn");
    go(`/hotel/${id}`);
    return;
  }
  const [hotel, pol] = await Promise.all([
    api("GET", `/api/hotels/${id}`, { auth: false }),
    api("GET", `/api/hotels/${id}/child-policy`, { auth: false }).catch((e) =>
      e.status === 404 ? "missing" : Promise.reject(e),
    ),
  ]);
  const data = await api("POST", "/api/availability/search", {
    auth: false,
    body: {
      checkIn: v.checkIn,
      checkOut: v.checkOut,
      adults: v.adults,
      children: v.children,
      city: hotel.city,
    },
  });
  const mine = (Array.isArray(data) ? data : []).find(
    (x) => Number(x.hotelId) === Number(id),
  );
  const avail = mine ? mine.rooms : [];
  const found = ids.map((i) => avail.find((r) => r.id === i)).filter(Boolean);
  bk.hotel = hotel;
  bk.policy = pol === "missing" ? null : pol;
  bk.noPolicy = pol === "missing";
  bk.q = {
    checkIn: v.checkIn,
    checkOut: v.checkOut,
    adults: v.adults,
    children: v.children,
  };
  bk.missing = ids.filter((i) => !found.find((r) => r.id === i));
  let remaining = Math.max(1, v.adults + v.children);
  bk.rooms = found.map((r, idx) => {
    const n =
      idx === found.length - 1
        ? Math.max(1, remaining)
        : Math.max(1, Math.min(remaining, r.maxOccupancy));
    remaining -= n;
    const guests = Array.from({ length: n }, () => ({ name: "", age: "" }));
    if (idx === 0 && auth.user)
      guests[0].name = (
        (auth.user.firstName || "") +
        " " +
        (auth.user.lastName || "")
      ).trim();
    return { ...r, guests };
  });
}
const bkNights = computed(() =>
  bk.q ? nightsBetween(bk.q.checkIn, bk.q.checkOut) : 0,
);
const bkTotal = computed(
  () =>
    bk.rooms.reduce((s, r) => s + Number(r.pricePerNight), 0) * bkNights.value,
);
function roomStats(room) {
  const pol = bk.policy;
  let adults = 0,
    children = 0,
    infants = 0;
  room.guests.forEach((g) => {
    if (blank(g.age) || Number.isNaN(Number(g.age))) return;
    const t = classify(Number(g.age), pol);
    if (t === "INFANT") infants++;
    else if (t === "CHILD") children++;
    else adults++;
  });
  const total =
    adults + children + (pol && pol.infantsCountTowardOccupancy ? infants : 0);
  const problems = [];
  if (adults > room.maxAdults)
    problems.push(
      `This room fits at most ${plural(room.maxAdults, "adult")} (you have ${adults}).`,
    );
  if (children > room.maxChildren)
    problems.push(
      `This room fits at most ${plural(room.maxChildren, "child", "children")} (you have ${children}).`,
    );
  if (total > room.maxOccupancy)
    problems.push(
      `This room sleeps at most ${room.maxOccupancy} guests (you have ${total}).`,
    );
  return {
    adults,
    children,
    infants,
    total,
    problems,
    pct: Math.min(
      100,
      Math.round((total / Math.max(1, room.maxOccupancy)) * 100),
    ),
  };
}
const guestKind = (g) =>
  blank(g.age) || Number.isNaN(Number(g.age))
    ? ""
    : { ADULT: "Adult", CHILD: "Child", INFANT: "Infant" }[
        classify(Number(g.age), bk.policy)
      ];
function addGuest(r) {
  r.guests.push({ name: "", age: "" });
}
function removeGuest(r, i) {
  r.guests.splice(i, 1);
}
function validateBooking() {
  const errs = {};
  bk.rooms.forEach((r, ri) => {
    if (!r.guests.length)
      errs["r" + ri] = "Add at least one guest to this room.";
    r.guests.forEach((g, gi) => {
      if (blank(g.name)) errs[`n${ri}_${gi}`] = "Guest name is required";
      if (blank(g.age)) errs[`a${ri}_${gi}`] = "Age is required";
      else if (!Number.isInteger(Number(g.age)) || Number(g.age) < 0)
        errs[`a${ri}_${gi}`] = "Age must be 0 or more";
      else if (Number(g.age) > 120)
        errs[`a${ri}_${gi}`] = "Enter a realistic age";
    });
    const st = roomStats(r);
    if (st.problems.length) errs["c" + ri] = st.problems[0];
  });
  if (bk.text.length > 500)
    errs.text = "Special request cannot exceed 500 characters";
  bk.errs = errs;
  return !Object.keys(errs).length;
}
watch(
  () => [bk.rooms, bk.text],
  () => {
    if (bk.touched) validateBooking();
  },
  { deep: true },
);
function bookingHelp(e) {
  const m = e.message || "";
  if (/overlap/i.test(m))
    return "Sorry – one of these rooms was just booked for your dates. Please go back and choose another room or different dates.";
  if (/accommodate|number of guests/i.test(m))
    return m + " Please adjust the guests in each room.";
  if (/only active customer/i.test(m))
    return "Your account needs to be active (email verified) before you can book.";
  if (e.status >= 500)
    return "The hotel can’t take this booking right now. It may not be fully set up for online booking yet (for example, it has no child policy). Please try another hotel.";
  return m;
}
async function submitBooking() {
  bk.touched = true;
  bk.banner = "";
  if (!validateBooking()) {
    toast("Please fix the highlighted fields.", "error");
    nextTick(focusFirstError);
    nextTick(() => {
      const el = document.querySelector(".bk-err");
      if (el) el.scrollIntoView({ block: "center", behavior: "smooth" });
    });
    return;
  }
  bk.busy = true;
  try {
    await api("POST", "/api/bookings", {
      body: {
        hotelId: bk.hotel.id,
        checkIn: bk.q.checkIn,
        checkOut: bk.q.checkOut,
        specialRequest: bk.text.trim() || null,
        rooms: bk.rooms.map((r) => ({
          roomId: r.id,
          guests: r.guests.map((g) => ({
            name: g.name.trim(),
            age: Number(g.age),
          })),
        })),
      },
    });
    let latest = null;
    try {
      const list = await api("GET", "/api/bookings/my-bookings", {
        query: { page: 0, size: 1, sort: "id,desc" },
      });
      latest = (Array.isArray(list) && list[0]) || null;
    } catch {}
    bk.done = latest || {
      bookingReference: "",
      hotelName: bk.hotel.name,
      checkIn: bk.q.checkIn,
      checkOut: bk.q.checkOut,
      totalAmount: bkTotal.value,
      status: "CONFIRMED",
      rooms: [],
      adults: 0,
      children: 0,
    };
    if (latest) quiet(latest.bookingReference);
    toast("Booking confirmed! A confirmation email is on its way.");
    window.scrollTo(0, 0);
  } catch (e) {
    bk.banner = bookingHelp(e);
    window.scrollTo({ top: 0, behavior: "smooth" });
  } finally {
    bk.busy = false;
  }
}

/* ---------------- my bookings ---------------- */
const known = new Set();
const quietRefs = new Set();
function quiet(ref) {
  if (!ref) return;
  known.add(ref);
  quietRefs.add(ref);
  setTimeout(() => quietRefs.delete(ref), 5000);
}
const mb = reactive({
  items: [],
  page: 0,
  size: 6,
  hasNext: false,
  loading: false,
  filter: "ALL",
  error: "",
});
async function loadBookings(p = 0) {
  mb.loading = true;
  mb.error = "";
  try {
    const list = await api("GET", "/api/bookings/my-bookings", {
      query: { page: p, size: mb.size, sort: "id,desc" },
    });
    mb.items = Array.isArray(list) ? list : [];
    mb.page = p;
    mb.hasNext = mb.items.length === mb.size;
    mb.items.forEach((b) => known.add(b.bookingReference));
  } catch (e) {
    mb.error = e.message;
    mb.items = [];
  } finally {
    mb.loading = false;
  }
}
const mbShown = computed(() =>
  mb.filter === "ALL"
    ? mb.items
    : mb.items.filter((b) => b.status === mb.filter),
);
const bd = reactive({ open: false, loading: false, data: null, err: "" });
async function openBooking(id) {
  Object.assign(bd, { open: true, loading: true, data: null, err: "" });
  try {
    bd.data = await api("GET", `/api/bookings/${id}`);
  } catch (e) {
    bd.err = e.message;
  } finally {
    bd.loading = false;
  }
}
async function cancelBooking(b, after) {
  const ok = await ask({
    title: "Cancel this booking?",
    danger: true,
    confirmText: "Yes, cancel booking",
    cancelText: "Keep booking",
    message: `Booking ${b.bookingReference} at ${b.hotelName} (${fmtDate(b.checkIn)} → ${fmtDate(b.checkOut)}) will be cancelled and the rooms released. This can’t be undone.`,
  });
  if (!ok) return;
  try {
    const r = await api("PATCH", `/api/bookings/${b.id}/cancel`);
    quiet(b.bookingReference);
    toast((r && r.message) || "Booking cancelled.");
    bd.open = false;
    if (after) await after();
  } catch (e) {
    toast(e.message, "error");
  }
}

/* ---------------- booking lookup (manager/admin) ---------------- */
const lk = reactive({ id: "", err: "", loading: false, data: null });
async function lookupBooking() {
  lk.err = "";
  lk.data = null;
  const n = Number(lk.id);
  if (blank(lk.id) || !Number.isInteger(n) || n < 1) {
    lk.err = "Enter a valid booking ID (a whole number).";
    return;
  }
  lk.loading = true;
  try {
    lk.data = await api("GET", `/api/bookings/${n}`);
  } catch (e) {
    lk.err =
      e.status === 404 ? `No booking with ID ${n} was found.` : e.message;
  } finally {
    lk.loading = false;
  }
}
async function completeBooking(b) {
  const ok = await ask({
    title: "Mark booking as completed?",
    message: `Booking ${b.bookingReference} will be marked as completed (the guest has stayed). This can’t be undone.`,
    confirmText: "Mark completed",
  });
  if (!ok) return;
  try {
    const r = await api("PATCH", `/api/bookings/${b.id}/status`, {
      body: { status: "COMPLETED" },
    });
    quiet(b.bookingReference);
    toast((r && r.message) || "Booking completed.");
    await lookupBooking();
  } catch (e) {
    toast(e.message, "error");
  }
}
const cancelFromLookup = (b) => cancelBooking(b, lookupBooking);

/* ---------------- hotel form (create / edit) ---------------- */
const hotelEditId = ref(null);
const hotelF = useForm(
  {
    name: "",
    description: "",
    address: "",
    city: "",
    country: "Bahrain",
    phone: "",
    latitude: "",
    longitude: "",
    managerId: "",
  },
  {
    name: [R.req("Hotel name"), R.len(2, 100, "Hotel name")],
    description: [R.req("Description"), R.len(10, 500, "Description")],
    address: [R.req("Address"), R.len(5, 255, "Address")],
    city: [R.req("City"), R.len(2, 100, "City")],
    country: [R.req("Country"), R.len(2, 100, "Country")],
    phone: [R.req("Phone"), R.len(8, 12, "Phone")],
    latitude: [R.num("Latitude", { min: -90, max: 90, optional: true })],
    longitude: [R.num("Longitude", { min: -180, max: 180, optional: true })],
    managerId: [
      (v) =>
        isAdmin.value && !hotelEditId.value && blank(v)
          ? "Manager user ID is required when an admin creates a hotel"
          : "",
      R.num("Manager ID", { min: 1, int: true, optional: true }),
    ],
  },
);
function initHotelForm(h) {
  hotelEditId.value = h ? h.id : null;
  hotelF.reset(
    h
      ? {
          name: h.name || "",
          description: h.description || "",
          address: h.address || "",
          city: h.city || "",
          country: h.country || "",
          phone: h.phone || "",
          latitude: h.latitude ?? "",
          longitude: h.longitude ?? "",
          managerId: "",
        }
      : {},
  );
}
async function saveHotel() {
  hotelF.banner = "";
  if (!hotelF.validateAll()) return;
  hotelF.busy = true;
  const v = hotelF.values,
    num = (x) => (blank(x) ? null : Number(x));
  const body = {
    name: v.name.trim(),
    description: v.description.trim(),
    address: v.address.trim(),
    city: v.city.trim(),
    country: v.country.trim(),
    phone: v.phone.trim(),
    latitude: num(v.latitude),
    longitude: num(v.longitude),
    managerId: num(v.managerId),
  };
  try {
    const editing = hotelEditId.value;
    const r = await api(
      editing ? "PUT" : "POST",
      editing ? `/api/hotels/${editing}` : "/api/hotels",
      { body },
    );
    toast((r && r.message) || "Hotel saved.");
    if (editing) {
      mp.hotel = await api("GET", `/api/hotels/${editing}`, { auth: false });
    } else go(isAdmin.value ? "/admin?tab=hotels" : "/manage");
  } catch (e) {
    if (e.status === 409) {
      hotelF.errors.name = "A hotel with this name already exists";
      hotelF.banner = "Please fix the highlighted field.";
    } else if (!mapServerErr(hotelF, e.message, [[/manager/i, "managerId"]]))
      hotelF.banner = e.message;
    else hotelF.banner = "Please fix the highlighted field.";
  } finally {
    hotelF.busy = false;
  }
}
function useMyLocation() {
  if (!navigator.geolocation) {
    toast("Location is not available in this browser.", "warn");
    return;
  }
  navigator.geolocation.getCurrentPosition(
    (p) => {
      hotelF.values.latitude = p.coords.latitude.toFixed(6);
      hotelF.values.longitude = p.coords.longitude.toFixed(6);
    },
    () => toast("Could not get your location.", "warn"),
  );
}

/* ---------------- manager portal ---------------- */
const mh = reactive({
  items: [],
  page: 0,
  size: 9,
  hasNext: false,
  loading: false,
  error: "",
  live: {},
});
async function probeLive(id) {
  try {
    await api("GET", `/api/hotels/${id}/rooms`, {
      query: { page: 0, size: 1 },
    });
    return true;
  } catch (e) {
    return e.status === 400 ? false : null;
  }
}
async function loadManagerHotels(p = 0) {
  mh.loading = true;
  mh.error = "";
  try {
    const list = await api("GET", "/api/hotels/my-hotels", {
      query: { page: p, size: mh.size, sort: "id,desc" },
    });
    mh.items = Array.isArray(list) ? list : [];
    mh.page = p;
    mh.hasNext = mh.items.length === mh.size;
    mh.items.forEach((h) => {
      hc[h.id] = h;
    });
    mh.live = {};
    mh.items.forEach(async (h) => {
      mh.live[h.id] = await probeLive(h.id);
    });
  } catch (e) {
    mh.error = e.message;
    mh.items = [];
  } finally {
    mh.loading = false;
  }
}
const mp = reactive({
  hotel: null,
  live: null,
  tab: "overview",
  rooms: [],
  roomsMsg: "",
  roomsPage: 0,
  roomsNext: false,
  loading: false,
  images: [],
  policy: null,
  policyMissing: false,
  imgFails: 0,
});
const roomTypes = ref([]);
async function loadRoomTypes() {
  try {
    const l = await api("GET", "/api/rooms-types");
    roomTypes.value = Array.isArray(l) ? l : [];
  } catch (e) {
    toast(e.message, "error");
  }
}
async function loadManageHotel(id, tab) {
  const same = mp.hotel && String(mp.hotel.id) === String(id);
  if (!same)
    Object.assign(mp, {
      hotel: null,
      live: null,
      rooms: [],
      roomsMsg: "",
      images: [],
      policy: null,
      policyMissing: false,
      imgFails: 0,
    });
  mp.hotel = await api("GET", `/api/hotels/${id}`, { auth: false });
  mp.tab = [
    "overview",
    "rooms",
    "amenities",
    "photos",
    "policy",
    "bookings",
  ].includes(tab)
    ? tab
    : "overview";
  if (mp.tab === "overview") initHotelForm(mp.hotel);
  mp.live = await probeLive(id);
  if (mp.tab === "rooms") await loadMRooms(0);
  if (mp.tab === "amenities") await loadAmenityCatalog();
  if (mp.tab === "photos") await loadMImages();
  if (mp.tab === "policy") await loadMPolicy();
}
async function loadMRooms(p = 0) {
  mp.roomsMsg = "";
  try {
    const list = await api("GET", `/api/hotels/${mp.hotel.id}/rooms`, {
      query: { page: p, size: 10, sort: "roomNumber,asc" },
    });
    mp.rooms = Array.isArray(list) ? list : [];
    mp.roomsPage = p;
    mp.roomsNext = mp.rooms.length === 10;
  } catch (e) {
    mp.rooms = [];
    mp.roomsMsg =
      e.status === 400
        ? "Rooms can be added and viewed once this hotel is approved by an admin and active."
        : e.message;
  }
}
async function loadMImages() {
  try {
    const l = await api("GET", `/api/hotels/${mp.hotel.id}/images`, {
      auth: false,
    });
    mp.images = Array.isArray(l) ? l : [];
    mp.imgFails = 0;
  } catch (e) {
    toast(e.message, "error");
  }
}
async function loadMPolicy() {
  try {
    mp.policy = await api("GET", `/api/hotels/${mp.hotel.id}/child-policy`, {
      auth: false,
    });
    mp.policyMissing = false;
    policyF.reset({
      infantMaxAge: mp.policy.infantMaxAge,
      childMaxAge: mp.policy.childMaxAge,
    });
  } catch (e) {
    if (e.status === 404) {
      mp.policy = null;
      mp.policyMissing = true;
      policyF.reset();
    } else toast(e.message, "error");
  }
}
function manageTab(t) {
  go(`/manage/hotel/${mp.hotel.id}?tab=${t}`);
}

const roomF = useForm(
  {
    id: null,
    roomTypeId: "",
    roomNumber: "",
    floorNumber: "",
    pricePerNight: "",
    maxAdults: 2,
    maxChildren: 1,
    maxOccupancy: 3,
  },
  {
    roomTypeId: [R.req("Room type")],
    roomNumber: [R.req("Room number"), R.max(20, "Room number")],
    floorNumber: [R.req("Floor number"), R.max(10, "Floor number")],
    pricePerNight: [R.num("Price per night", { min: 0.01 })],
    maxAdults: [R.num("Max adults", { min: 1, int: true })],
    maxChildren: [R.num("Max children", { min: 1, int: true })],
    maxOccupancy: [R.num("Max occupancy", { min: 1, int: true })],
  },
);
const roomModal = reactive({ open: false });
async function openRoomModal(r) {
  roomF.reset(
    r && r.id
      ? {
          id: r.id,
          roomTypeId: r.roomTypeId,
          roomNumber: r.roomNumber,
          floorNumber: r.floorNumber,
          pricePerNight: r.pricePerNight,
          maxAdults: r.maxAdults,
          maxChildren: r.maxChildren,
          maxOccupancy: r.maxOccupancy,
        }
      : {},
  );
  roomModal.open = true;
  if (!roomTypes.value.length) await loadRoomTypes();
}
async function saveRoom() {
  roomF.banner = "";
  if (!roomF.validateAll()) return;
  roomF.busy = true;
  const v = roomF.values;
  const body = {
    roomTypeId: Number(v.roomTypeId),
    roomNumber: String(v.roomNumber).trim(),
    floorNumber: String(v.floorNumber).trim(),
    pricePerNight: Number(v.pricePerNight),
    maxAdults: Number(v.maxAdults),
    maxChildren: Number(v.maxChildren),
    maxOccupancy: Number(v.maxOccupancy),
  };
  try {
    const r = v.id
      ? await api("PUT", `/api/hotels/${mp.hotel.id}/rooms/${v.id}`, { body })
      : await api("POST", `/api/hotels/${mp.hotel.id}/rooms`, { body });
    toast((r && r.message) || (v.id ? "Room updated." : "Room added."));
    roomModal.open = false;
    await loadMRooms(v.id ? mp.roomsPage : 0);
  } catch (e) {
    if (e.status === 409) {
      roomF.errors.roomNumber =
        "This hotel already has a room with this number";
      roomF.banner = e.message && e.message !== "Conflict" ? e.message : "";
    } else roomF.banner = e.message;
  } finally {
    roomF.busy = false;
  }
}
const ROOM_STATUS = {
  ACTIVE: "Active",
  INACTIVE: "Inactive",
  UNDER_MAINTENANCE: "Under maintenance",
};
async function setRoomStatus(r, status) {
  if (status === r.status) return;
  if (
    status !== "ACTIVE" &&
    !(await ask({
      title: `Set room ${r.roomNumber} to “${ROOM_STATUS[status]}”?`,
      message: "Guests won’t be able to book this room while it is not active.",
      confirmText: "Change status",
      danger: true,
    }))
  ) {
    await loadMRooms(mp.roomsPage);
    return;
  }
  try {
    await api("PATCH", `/api/hotels/${mp.hotel.id}/rooms/${r.id}/status`, {
      body: { status },
    });
    toast(`Room ${r.roomNumber} is now ${ROOM_STATUS[status].toLowerCase()}.`);
  } catch (e) {
    toast(e.message, "error");
  }
  await loadMRooms(mp.roomsPage);
}

/* add many rooms at once: 101, 102, 103 ... */
const bulkF = useForm(
  {
    roomTypeId: "",
    prefix: "",
    start: 101,
    count: 5,
    floorNumber: "1",
    pricePerNight: "",
    maxAdults: 2,
    maxChildren: 1,
    maxOccupancy: 3,
  },
  {
    roomTypeId: [R.req("Room type")],
    prefix: [R.max(10, "Prefix")],
    start: [R.num("First room number", { min: 1, int: true })],
    count: [R.num("Number of rooms", { min: 1, max: 100, int: true })],
    floorNumber: [R.req("Floor number"), R.max(10, "Floor number")],
    pricePerNight: [R.num("Price per night", { min: 0.01 })],
    maxAdults: [R.num("Max adults", { min: 1, int: true })],
    maxChildren: [R.num("Max children", { min: 1, int: true })],
    maxOccupancy: [R.num("Max occupancy", { min: 1, int: true })],
  },
);
const bulkModal = reactive({ open: false });
async function openBulk() {
  bulkF.reset();
  bulkModal.open = true;
  if (!roomTypes.value.length) await loadRoomTypes();
}
const bulkNumbers = computed(() => {
  const v = bulkF.values,
    n = Number(v.count),
    s = Number(v.start);
  if (!(n >= 1 && n <= 100) || !Number.isInteger(s)) return [];
  return Array.from(
    { length: n },
    (_, i) => String(v.prefix || "").trim() + (s + i),
  );
});
async function saveBulk() {
  bulkF.banner = "";
  if (!bulkF.validateAll()) return;
  bulkF.busy = true;
  const v = bulkF.values;
  try {
    const rooms = bulkNumbers.value.map((n) => ({
      roomTypeId: Number(v.roomTypeId),
      roomNumber: n,
      floorNumber: String(v.floorNumber).trim(),
      pricePerNight: Number(v.pricePerNight),
      maxAdults: Number(v.maxAdults),
      maxChildren: Number(v.maxChildren),
      maxOccupancy: Number(v.maxOccupancy),
    }));
    const r = await api("POST", `/api/hotels/${mp.hotel.id}/rooms/bulk`, {
      body: { rooms },
    });
    toast((r && r.message) || "Rooms added.");
    bulkModal.open = false;
    await loadMRooms(0);
  } catch (e) {
    bulkF.banner =
      e.status === 409
        ? e.message && e.message !== "Conflict"
          ? e.message
          : "One of these room numbers already exists. Nothing was added."
        : e.message;
  } finally {
    bulkF.busy = false;
  }
}

/* photos of one room */
const rimg = reactive({
  open: false,
  room: null,
  images: [],
  items: [],
  busy: false,
  err: "",
  loading: false,
  fails: 0,
});
async function loadRoomImages() {
  rimg.loading = true;
  try {
    const l = await api(
      "GET",
      `/api/hotels/${mp.hotel.id}/rooms/${rimg.room.id}/images`,
      { auth: false },
    );
    rimg.images = Array.isArray(l) ? l : [];
    rimg.fails = 0;
  } catch (e) {
    rimg.err = e.message;
  } finally {
    rimg.loading = false;
  }
}
async function openRoomImages(r) {
  Object.assign(rimg, {
    open: true,
    room: r,
    images: [],
    items: [],
    err: "",
    busy: false,
  });
  await loadRoomImages();
}
function closeRoomImages() {
  rimg.items.forEach((it) => URL.revokeObjectURL(it.preview));
  rimg.items = [];
  rimg.open = false;
}
function pickRoomImages(e) {
  rimg.err = "";
  for (const f of Array.from(e.target.files || [])) {
    if (!/^image\/(jpeg|jpg|png|webp)$/.test(f.type)) {
      rimg.err = `“${f.name}” must be a JPG, PNG or WebP image.`;
      continue;
    }
    if (f.size > 1024 * 1024) {
      rimg.err = `“${f.name}” is ${(f.size / 1048576).toFixed(1)} MB – the server accepts up to 1 MB per file.`;
      continue;
    }
    if (rimg.images.length + rimg.items.length >= 20) {
      rimg.err = "A room can have at most 20 photos.";
      break;
    }
    if (rimg.items.length >= 10) {
      rimg.err = "You can upload 10 photos at a time.";
      break;
    }
    rimg.items.push({ file: f, preview: URL.createObjectURL(f) });
  }
  e.target.value = "";
}
function dropRoomImage(i) {
  URL.revokeObjectURL(rimg.items[i].preview);
  rimg.items.splice(i, 1);
}
async function uploadRoomImages() {
  if (!rimg.items.length) {
    rimg.err = "Choose at least one photo.";
    return;
  }
  rimg.busy = true;
  rimg.err = "";
  try {
    const fd = new FormData();
    rimg.items.forEach((it) => fd.append("images", it.file));
    const r = await api(
      "POST",
      `/api/hotels/${mp.hotel.id}/rooms/${rimg.room.id}/images`,
      { form: fd, timeout: 60000 },
    );
    rimg.items.forEach((it) => URL.revokeObjectURL(it.preview));
    rimg.items = [];
    toast((r && r.message) || "Photos uploaded.");
    await loadRoomImages();
  } catch (e) {
    rimg.err = e.message;
  } finally {
    rimg.busy = false;
  }
}
async function deleteRoomImage(img) {
  if (
    !(await ask({
      title: "Delete this photo?",
      message: "The photo will be removed from this room.",
      confirmText: "Delete photo",
      danger: true,
    }))
  )
    return;
  try {
    const r = await api(
      "DELETE",
      `/api/hotels/${mp.hotel.id}/rooms/${rimg.room.id}/images/${img.id}`,
    );
    toast((r && r.message) || "Photo deleted.");
    await loadRoomImages();
  } catch (e) {
    toast(e.message, "error");
  }
}

/* ---------------- amenities ---------------- */
const amenityCatalog = ref([]);
async function loadAmenityCatalog() {
  try {
    const l = await api("GET", "/api/amenities", { auth: false });
    amenityCatalog.value = Array.isArray(l) ? l : [];
  } catch (e) {
    toast(e.message, "error");
  }
}
async function reloadMpHotel() {
  mp.hotel = await api("GET", `/api/hotels/${mp.hotel.id}`, { auth: false });
}
const availableAmenities = computed(() => {
  const have = new Set(
    ((mp.hotel && mp.hotel.amenities) || []).map((a) => a.id),
  );
  return amenityCatalog.value.filter((a) => !have.has(a.id));
});
const amenF = useForm(
  { amenityId: "", newName: "", description: "", additionalCost: "" },
  {
    amenityId: [
      (v, d) =>
        blank(v) && blank(d.newName)
          ? "Choose an amenity or type a new one"
          : "",
    ],
    newName: [R.len(3, 60, "Amenity name")],
    description: [R.max(255, "Description")],
    additionalCost: [
      (v) =>
        !blank(v) && (isNaN(Number(v)) || Number(v) < 0)
          ? "Extra cost must be 0 or more"
          : "",
    ],
  },
);
const amenModal = reactive({ open: false, editing: null });
async function openAmenity(a) {
  amenModal.editing = a || null;
  amenF.reset(
    a
      ? {
          amenityId: a.id,
          description: a.description || "",
          additionalCost: a.additionalCost == null ? "" : a.additionalCost,
        }
      : {},
  );
  amenModal.open = true;
  if (!amenityCatalog.value.length) await loadAmenityCatalog();
}
async function saveAmenity() {
  amenF.banner = "";
  if (!amenF.validateAll()) return;
  amenF.busy = true;
  const v = amenF.values;
  const body = {
    description: (v.description || "").trim() || null,
    additionalCost: blank(v.additionalCost) ? null : Number(v.additionalCost),
  };
  try {
    if (amenModal.editing) {
      await api(
        "PUT",
        `/api/hotels/${mp.hotel.id}/amenities/${amenModal.editing.id}`,
        { body: { amenityId: amenModal.editing.id, ...body } },
      );
    } else {
      let id = Number(v.amenityId);
      if (!blank(v.newName)) {
        const c = await api("POST", "/api/amenities", {
          body: { name: v.newName.trim() },
        });
        id = c.id;
        await loadAmenityCatalog();
      }
      await api("POST", `/api/hotels/${mp.hotel.id}/amenities`, {
        body: { amenityId: id, ...body },
      });
    }
    toast("Amenity saved.");
    amenModal.open = false;
    await reloadMpHotel();
  } catch (e) {
    if (e.status === 409)
      amenF.banner =
        e.message && e.message !== "Conflict"
          ? e.message
          : "That amenity already exists.";
    else amenF.banner = e.message;
  } finally {
    amenF.busy = false;
  }
}
async function removeAmenity(a) {
  if (
    !(await ask({
      title: `Remove “${a.name}”?`,
      message:
        "It will no longer be shown on this hotel’s page. It stays in the amenity list for other hotels.",
      confirmText: "Remove",
      danger: true,
    }))
  )
    return;
  try {
    const r = await api(
      "DELETE",
      `/api/hotels/${mp.hotel.id}/amenities/${a.id}`,
    );
    toast((r && r.message) || "Amenity removed.");
    await reloadMpHotel();
  } catch (e) {
    toast(e.message, "error");
  }
}

/* admin: the amenity catalog */
const catF = useForm(
  { id: null, name: "", description: "" },
  {
    name: [R.req("Amenity name"), R.max(60, "Amenity name")],
    description: [R.max(255, "Description")],
  },
);
const catModal = reactive({ open: false });
function openCatalog(a) {
  catF.reset(
    a ? { id: a.id, name: a.name, description: a.description || "" } : {},
  );
  catModal.open = true;
}
async function saveCatalog() {
  catF.banner = "";
  if (!catF.validateAll()) return;
  catF.busy = true;
  const v = catF.values;
  try {
    await api(
      v.id ? "PUT" : "POST",
      v.id ? `/api/amenities/${v.id}` : "/api/amenities",
      {
        body: {
          name: v.name.trim(),
          description: (v.description || "").trim() || null,
        },
      },
    );
    toast("Amenity saved.");
    catModal.open = false;
    await loadAmenityCatalog();
  } catch (e) {
    if (e.status === 409)
      catF.errors.name = "An amenity with this name already exists";
    else catF.banner = e.message;
  } finally {
    catF.busy = false;
  }
}
async function deleteCatalog(a) {
  if (
    !(await ask({
      title: `Delete “${a.name}”?`,
      message: "This only works if no hotel is using it.",
      confirmText: "Delete",
      danger: true,
    }))
  )
    return;
  try {
    const r = await api("DELETE", `/api/amenities/${a.id}`);
    toast((r && r.message) || "Amenity deleted.");
    await loadAmenityCatalog();
  } catch (e) {
    toast(e.message, "error");
  }
}

const imgUp = reactive({ items: [], busy: false, err: "" });
function pickHotelImages(e) {
  imgUp.err = "";
  for (const f of Array.from(e.target.files || [])) {
    if (!/^image\/(jpeg|jpg|png|webp)$/.test(f.type)) {
      imgUp.err = `“${f.name}” must be a JPG, PNG or WebP image.`;
      continue;
    }
    if (f.size > 1024 * 1024) {
      imgUp.err = `“${f.name}” is ${(f.size / 1048576).toFixed(1)} MB – the server accepts up to 1 MB per file.`;
      continue;
    }
    imgUp.items.push({
      file: f,
      alt: "",
      err: "",
      preview: URL.createObjectURL(f),
    });
  }
  e.target.value = "";
}
function dropImage(i) {
  URL.revokeObjectURL(imgUp.items[i].preview);
  imgUp.items.splice(i, 1);
}
async function uploadHotelImages() {
  let ok = true;
  imgUp.items.forEach((it) => {
    it.alt = it.alt.replace(/,/g, " ").trim();
    it.err = it.alt ? "" : "Describe this photo (alt text) – required";
    if (it.err) ok = false;
  });
  if (!imgUp.items.length) {
    imgUp.err = "Choose at least one photo.";
    return;
  }
  if (!ok) return;
  imgUp.busy = true;
  imgUp.err = "";
  try {
    const fd = new FormData();
    imgUp.items.forEach((it) => {
      fd.append("images", it.file);
      fd.append("altTexts", it.alt);
    });
    const r = await api("POST", `/api/hotels/${mp.hotel.id}/images`, {
      form: fd,
      timeout: 60000,
    });
    imgUp.items.forEach((it) => URL.revokeObjectURL(it.preview));
    imgUp.items = [];
    toast((r && r.message) || "Photos uploaded.");
    await loadMImages();
  } catch (e) {
    imgUp.err = e.message;
  } finally {
    imgUp.busy = false;
  }
}
async function deleteHotelImage(img) {
  if (
    !(await ask({
      title: "Delete this photo?",
      message: "The photo will be removed from the hotel page.",
      confirmText: "Delete photo",
      danger: true,
    }))
  )
    return;
  try {
    const r = await api(
      "DELETE",
      `/api/hotels/${mp.hotel.id}/images/${img.id}`,
    );
    toast((r && r.message) || "Photo deleted.");
    await loadMImages();
  } catch (e) {
    toast(e.message, "error");
  }
}

const policyF = useForm(
  { infantMaxAge: 2, childMaxAge: 12, infantsCountTowardOccupancy: false },
  {
    infantMaxAge: [R.num("Infant maximum age", { min: 0, max: 5, int: true })],
    childMaxAge: [
      R.num("Child maximum age", { min: 1, max: 17, int: true }),
      (v, d) =>
        !blank(v) &&
        !blank(d.infantMaxAge) &&
        Number(d.infantMaxAge) >= Number(v)
          ? "Infant maximum age must be less than child maximum age"
          : "",
    ],
  },
);
async function savePolicy() {
  policyF.banner = "";
  if (!policyF.validateAll()) return;
  policyF.busy = true;
  try {
    const v = policyF.values;
    const r = await api(
      mp.policyMissing ? "POST" : "PUT",
      `/api/hotels/${mp.hotel.id}/child-policy`,
      {
        body: {
          infantMaxAge: Number(v.infantMaxAge),
          childMaxAge: Number(v.childMaxAge),
          infantsCountTowardOccupancy: !!v.infantsCountTowardOccupancy,
        },
      },
    );
    toast((r && r.message) || "Child policy saved.");
    await loadMPolicy();
  } catch (e) {
    if (e.status === 409) {
      policyF.banner =
        "This hotel already has a child policy – loaded it so you can edit it.";
      await loadMPolicy();
    } else policyF.banner = e.message;
  } finally {
    policyF.busy = false;
  }
}

/* ---------------- admin ---------------- */
const ad = reactive({
  tab: "overview",
  users: [],
  uPage: 0,
  uPages: 0,
  uTotal: 0,
  uBusy: null,
  uq: { search: "", status: "", role: "", sort: "createdAt,desc" },
  pending: [],
  pPage: 0,
  pNext: false,
  hotels: [],
  hPage: 0,
  hNext: false,
  q: { search: "", city: "", country: "" },
  counts: { pending: null, hotels: null, types: null, users: null },
  loading: false,
  error: "",
});
const cnt = (n, max) => (n >= max ? max + "+" : String(n));
async function loadAdmin(tab) {
  ad.tab = [
    "overview",
    "pending",
    "hotels",
    "users",
    "status",
    "types",
    "amenities",
    "bookings",
  ].includes(tab)
    ? tab
    : "overview";
  ad.error = "";
  try {
    if (ad.tab === "overview") {
      const [p, h, t, u] = await Promise.all([
        api("GET", "/api/hotels/pending", { query: { size: 50 } }),
        api("GET", "/api/hotels", { auth: false, query: { size: 50 } }),
        api("GET", "/api/rooms-types"),
        api("GET", "/api/admin/users", { query: { size: 1 } }).catch(
          () => null,
        ),
      ]);
      ad.counts.users =
        u && u.totalElements != null ? String(u.totalElements) : null;
      ad.counts.pending = cnt(p.length, 50);
      ad.counts.hotels = cnt(h.length, 50);
      ad.counts.types = cnt(t.length, 1000);
      roomTypes.value = t;
    }
    if (ad.tab === "pending") await loadPending(0);
    if (ad.tab === "hotels") await loadAdminHotels(0);
    if (ad.tab === "users") await loadAdminUsers(0);
    if (ad.tab === "types") await loadRoomTypes();
    if (ad.tab === "amenities") await loadAmenityCatalog();
  } catch (e) {
    ad.error = e.message;
  }
}
/** Loads one page of users using the server-side filters (search, status, role) and sorting. */
async function loadAdminUsers(p = 0) {
  ad.loading = true;
  ad.error = "";
  try {
    const r = await api("GET", "/api/admin/users", {
      query: {
        page: p,
        size: 10,
        sort: ad.uq.sort,
        search: ad.uq.search.trim(),
        status: ad.uq.status,
        role: ad.uq.role,
      },
    });
    ad.users = r.content || [];
    ad.uPage = r.page;
    ad.uPages = r.totalPages;
    ad.uTotal = r.totalElements;
  } catch (e) {
    ad.users = [];
    ad.error =
      e.status === 404
        ? "User management needs the updated backend (GET /api/admin/users)."
        : e.message;
  } finally {
    ad.loading = false;
  }
}
function resetUserFilters() {
  Object.assign(ad.uq, {
    search: "",
    status: "",
    role: "",
    sort: "createdAt,desc",
  });
  loadAdminUsers(0);
}
const isSelf = (u) => !!auth.user && auth.user.email === u.email;
/** Deactivate (soft delete) or reactivate a user after the admin confirms. */
async function setUserStatus(u, status) {
  const off = status === "INACTIVE";
  const yes = await ask({
    title: `${off ? "Deactivate" : "Reactivate"} “${u.username}”?`,
    danger: off,
    confirmText: off ? "Deactivate" : "Reactivate",
    message: off
      ? "They won’t be able to sign in and any session they have open stops working. Their data is kept, and you can reactivate them later."
      : "They will be able to sign in again.",
  });
  if (!yes) return;
  ad.uBusy = u.id;
  try {
    const r = await api("PATCH", `/api/admin/users/${u.id}/status`, {
      body: { status },
    });
    toast((r && r.message) || "Done.");
    await loadAdminUsers(ad.uPage);
  } catch (e) {
    toast(e.message, "error");
  } finally {
    ad.uBusy = null;
  }
}
function adminTab(t) {
  go("/admin?tab=" + t);
}
async function loadPending(p = 0) {
  ad.loading = true;
  try {
    const l = await api("GET", "/api/hotels/pending", {
      query: { page: p, size: 8, sort: "id,asc" },
    });
    ad.pending = Array.isArray(l) ? l : [];
    ad.pPage = p;
    ad.pNext = ad.pending.length === 8;
  } catch (e) {
    ad.error = e.message;
  } finally {
    ad.loading = false;
  }
}
async function loadAdminHotels(p = 0) {
  ad.loading = true;
  try {
    const l = await api("GET", "/api/hotels", {
      auth: false,
      query: {
        search: ad.q.search.trim(),
        city: ad.q.city.trim(),
        country: ad.q.country.trim(),
        page: p,
        size: 8,
        sort: "name,asc",
      },
    });
    ad.hotels = Array.isArray(l) ? l : [];
    ad.hPage = p;
    ad.hNext = ad.hotels.length === 8;
  } catch (e) {
    ad.error = e.message;
  } finally {
    ad.loading = false;
  }
}
async function setHotelStatus(h, status, after) {
  const label = {
    ACTIVE: "approve / activate",
    REJECTED: "reject",
    INACTIVE: "deactivate",
  }[status];
  const ok = await ask({
    title: `${label[0].toUpperCase() + label.slice(1)} “${h.name || "hotel #" + h.id}”?`,
    danger: status !== "ACTIVE",
    confirmText: label[0].toUpperCase() + label.slice(1),
    message:
      status === "ACTIVE"
        ? "The hotel will go live and guests will be able to find and book it."
        : status === "REJECTED"
          ? "The manager will see that the hotel was not approved."
          : "The hotel will be hidden from search and can no longer be booked.",
  });
  if (!ok) return false;
  try {
    const r = await api("PATCH", `/api/hotels/${h.id}/status`, {
      body: { status },
    });
    toast(
      {
        ACTIVE: "Hotel approved and live.",
        REJECTED: "Hotel rejected.",
        INACTIVE: "Hotel deactivated.",
      }[status] ||
        (r && r.message) ||
        "Hotel status updated.",
      "success",
    );
    if (after) await after();
    return true;
  } catch (e) {
    toast(e.message, "error");
    return false;
  }
}
const statusF = useForm(
  { hotelId: "", status: "ACTIVE" },
  {
    hotelId: [R.num("Hotel ID", { min: 1, int: true })],
    status: [R.req("Status")],
  },
);
async function runStatusTool() {
  statusF.banner = "";
  if (!statusF.validateAll()) return;
  statusF.busy = true;
  try {
    const r = await api(
      "PATCH",
      `/api/hotels/${Number(statusF.values.hotelId)}/status`,
      { body: { status: statusF.values.status } },
    );
    toast((r && r.message) || "Hotel status updated.");
  } catch (e) {
    statusF.banner = e.message;
  } finally {
    statusF.busy = false;
  }
}
const assignF = useForm(
  { hotelId: null, hotelName: "", managerId: "" },
  { managerId: [R.num("Manager user ID", { min: 1, int: true })] },
);
const assignModal = reactive({ open: false });
function openAssign(h) {
  assignF.reset({ hotelId: h.id, hotelName: h.name, managerId: "" });
  assignModal.open = true;
}
async function doAssign() {
  assignF.banner = "";
  if (!assignF.validateAll()) return;
  assignF.busy = true;
  try {
    const r = await api(
      "POST",
      `/api/hotels/${assignF.values.hotelId}/managers/${Number(assignF.values.managerId)}`,
    );
    toast((r && r.message) || "Manager assigned.");
    assignModal.open = false;
  } catch (e) {
    assignF.banner = e.message;
  } finally {
    assignF.busy = false;
  }
}
const typeF = useForm(
  { id: null, name: "", description: "" },
  {
    name: [R.req("Room type name"), R.len(2, 100, "Room type name")],
    description: [R.req("Description"), R.len(10, 500, "Description")],
  },
);
const typeModal = reactive({ open: false });
function openType(t) {
  typeF.reset(t ? { id: t.id, name: t.name, description: t.description } : {});
  typeModal.open = true;
}
async function saveType() {
  typeF.banner = "";
  if (!typeF.validateAll()) return;
  typeF.busy = true;
  const v = typeF.values;
  try {
    const r = await api(
      v.id ? "PUT" : "POST",
      v.id ? `/api/rooms-types/${v.id}` : "/api/rooms-types",
      { body: { name: v.name.trim(), description: v.description.trim() } },
    );
    toast((r && r.message) || "Room type saved.");
    typeModal.open = false;
    await loadRoomTypes();
  } catch (e) {
    if (e.status === 409) {
      typeF.errors.name = "A room type with this name already exists";
    } else typeF.banner = e.message;
  } finally {
    typeF.busy = false;
  }
}
async function setTypeStatus(t, status) {
  if (
    status === "INACTIVE" &&
    !(await ask({
      title: `Deactivate “${t.name}”?`,
      message:
        "Managers will no longer be able to use this room type for new rooms.",
      confirmText: "Deactivate",
      danger: true,
    }))
  )
    return;
  try {
    const r = await api("PATCH", `/api/rooms-types/${t.id}/status`, {
      body: { status },
    });
    toast((r && r.message) || "Room type updated.");
    await loadRoomTypes();
  } catch (e) {
    toast(e.message, "error");
  }
}
const typeActivate = reactive({ id: "", err: "" });
async function activateTypeById() {
  typeActivate.err = "";
  const n = Number(typeActivate.id);
  if (blank(typeActivate.id) || !Number.isInteger(n) || n < 1) {
    typeActivate.err = "Enter a valid room type ID.";
    return;
  }
  try {
    const r = await api("PATCH", `/api/rooms-types/${n}/status`, {
      body: { status: "ACTIVE" },
    });
    toast((r && r.message) || "Room type activated.");
    typeActivate.id = "";
    await loadRoomTypes();
  } catch (e) {
    typeActivate.err = e.message;
  }
}

/* ---------------- live notifications (STOMP over WebSocket) ---------------- */
const feed = ref([]),
  unread = ref(0),
  wsState = reactive({ on: false });
let stomp = null;
function disconnectWs() {
  try {
    if (stomp) stomp.deactivate();
  } catch {}
  stomp = null;
  wsState.on = false;
}
function connectWs() {
  if (stomp || !auth.user) return;
  let base;
  try {
    base = new URL(API_BASE || location.origin);
  } catch {
    return;
  }
  if (!/^https?:$/.test(base.protocol)) return;
  stomp = new Client({
    brokerURL:
      (base.protocol === "https:" ? "wss:" : "ws:") + "//" + base.host + "/ws",
    reconnectDelay: 10000,
    // the server checks this token when the socket connects (browsers can't send headers on the handshake)
    beforeConnect() {
      stomp.connectHeaders = { Authorization: "Bearer " + auth.token };
    },
    async onConnect() {
      wsState.on = true;
      const handle = (m) => {
        try {
          onBookingEvent(JSON.parse(m.body));
        } catch {}
      };
      if (auth.user && auth.user.email)
        stomp.subscribe(`/topic/users/${auth.user.email}/bookings`, handle); // my own bookings
      try {
        if (isAdmin.value)
          stomp.subscribe("/topic/admin/bookings", handle); // admins: everything
        else if (isManager.value) {
          // managers: their hotels only
          const list = await api("GET", "/api/hotels/my-hotels", {
            query: { size: 100 },
          });
          (Array.isArray(list) ? list : []).forEach(
            (h) =>
              stomp &&
              stomp.connected &&
              stomp.subscribe(`/topic/hotels/${h.id}/bookings`, handle),
          );
        }
      } catch {}
    },
    onStompError() {
      wsState.on = false;
    },
    onWebSocketClose() {
      wsState.on = false;
    },
  });
  stomp.activate();
}
const recentEvents = new Map();
function onBookingEvent(n) {
  // a manager can receive the same event on two channels (own booking + hotel topic): show it once
  const key = n.bookingReference + "|" + n.status,
    now = Date.now();
  if (recentEvents.has(key) && now - recentEvents.get(key) < 3000) return;
  recentEvents.set(key, now);
  const mine = known.has(n.bookingReference);
  feed.value.unshift({ ...n, at: new Date() });
  if (feed.value.length > 30) feed.value.pop();
  unread.value++;
  if (!quietRefs.has(n.bookingReference))
    toast(
      `Booking ${n.bookingReference}${n.hotelName ? " (" + n.hotelName + ")" : ""}: ${n.message}`,
      n.status === "CANCELLED" ? "warn" : "info",
    );
  if (route.name === "bookings") loadBookings(mb.page);
}
function toggleFeed() {
  feedOpen.value = !feedOpen.value;
  menuOpen.value = false;
  if (feedOpen.value) unread.value = 0;
}

function scrollToRooms() {
  const el = document.getElementById("rooms");
  if (el) el.scrollIntoView({ behavior: "smooth", block: "start" });
}

/* ---------------- boot ---------------- */
document.addEventListener("click", () => {
  menuOpen.value = false;
  feedOpen.value = false;
});
window.addEventListener("hashchange", applyRoute);
(async () => {
  if (auth.token) {
    const exp = jwtExp(auth.token);
    if (exp && exp * 1000 < Date.now()) clearSession();
    else {
      try {
        await loadProfile();
        connectWs();
      } catch (e) {
        clearSession();
      }
    }
  }
  readyResolve();
  applyRoute();
  loadHome().catch(() => {});
})();

const navActive = (n) =>
  ({
    home: ["home", "search"],
    explore: ["explore"],
    bookings: ["bookings"],
    manage: ["manage", "manage-new", "manage-hotel"],
    admin: ["admin"],
  })[n].includes(route.name);
const tomorrowISO = () => toISO(addDays(new Date(), 1));
const printPage = () => window.print();
</script>

<template>
  <div>
    <div class="loadbar no-print" :class="{ on: pending > 0 }"></div>
    <datalist id="city-list">
      <option v-for="c in cities" :key="c" :value="c"></option>
    </datalist>

    <!-- ================= HEADER ================= -->
    <header class="topbar no-print">
      <div class="wrap topbar-in">
        <a class="brand" href="#/"><span class="logo-mark">V</span>VibeStay</a>
        <div class="spacer"></div>
        <template v-if="auth.user">
          <div class="rel" @click.stop>
            <button
              class="icon-btn"
              @click="toggleFeed"
              aria-label="Notifications"
              title="Live booking notifications"
            >
              <icon name="bell" :size="20"></icon
              ><span v-if="unread" class="dot">{{ unread }}</span>
            </button>
            <div v-if="feedOpen" class="feed">
              <header>
                <span>Live notifications</span
                ><span class="xs muted">{{
                  wsState.on ? "● connected" : "○ offline"
                }}</span>
              </header>
              <ul>
                <li v-if="!feed.length" class="muted">
                  Nothing yet. Booking updates appear here in real time.
                </li>
                <li v-for="(n, i) in feed" :key="i">
                  <b>{{ n.bookingReference }}</b>
                  <span class="badge" :class="statusClass(n.status)">{{
                    statusLabel(n.status)
                  }}</span>
                  <div class="small">{{ n.message }}</div>
                </li>
              </ul>
            </div>
          </div>
          <div class="rel" @click.stop>
            <button
              class="user-btn"
              @click="
                menuOpen = !menuOpen;
                feedOpen = false;
              "
              :aria-expanded="menuOpen"
            >
              <span class="avatar"
                ><img
                  v-if="profileImage"
                  :src="profileImage"
                  alt=""
                  @error="$event.target.remove()"
                /><template v-else>{{ userInitials }}</template></span
              >
              <span
                ><b>{{ auth.user.firstName || auth.user.username }}</b
                ><small>{{ roleLabel(role) }}</small></span
              ><icon name="down" :size="16"></icon>
            </button>
            <div v-if="menuOpen" class="menu" role="menu">
              <a href="#/profile"><icon name="user"></icon>My profile</a>
              <a v-if="isCustomer" href="#/bookings"
                ><icon name="list"></icon>My bookings</a
              >
              <a v-if="isManager" href="#/manage"
                ><icon name="brief"></icon>Manager portal</a
              >
              <a v-if="isAdmin" href="#/admin"
                ><icon name="shield"></icon>Admin panel</a
              >
              <hr />
              <button @click="logout">
                <icon name="logout"></icon>Sign out
              </button>
            </div>
          </div>
        </template>
        <template v-else>
          <a class="btn btn-outline-light btn-sm" href="#/register">Register</a>
          <a class="btn btn-light btn-sm" href="#/login">Sign in</a>
        </template>
      </div>
      <nav class="wrap navtabs" aria-label="Main">
        <a class="nav-pill" :class="{ on: navActive('home') }" href="#/"
          ><icon name="bed"></icon>Stays</a
        >
        <a
          class="nav-pill"
          :class="{ on: navActive('explore') }"
          href="#/explore"
          ><icon name="building"></icon>Explore hotels</a
        >
        <a
          v-if="isCustomer"
          class="nav-pill"
          :class="{ on: navActive('bookings') }"
          href="#/bookings"
          ><icon name="calendar"></icon>My bookings</a
        >
        <a
          v-if="isManager"
          class="nav-pill"
          :class="{ on: navActive('manage') }"
          href="#/manage"
          ><icon name="brief"></icon>Manager portal</a
        >
        <a
          v-if="isAdmin"
          class="nav-pill"
          :class="{ on: navActive('admin') }"
          href="#/admin"
          ><icon name="shield"></icon>Admin panel</a
        >
      </nav>
    </header>

    <main>
      <!-- ================= HOME ================= -->
      <section v-if="route.name === 'home'">
        <div class="hero">
          <div class="wrap">
            <h1>Find your next stay</h1>
            <p>
              Search hotels, see real-time room availability and book in minutes
              – with instant confirmation by email.
            </p>
          </div>
        </div>
        <div class="wrap searchwrap">
          <search-bar
            :f="searchF"
            :min-in="minIn"
            @submit="submitSearch"
          ></search-bar>
        </div>

        <div class="wrap section" v-if="page.error">
          <alert-box type="error" :msg="page.error"></alert-box>
        </div>

        <div class="wrap section" v-if="destinations.length">
          <h2>Popular destinations</h2>
          <p class="muted">Cities with hotels you can book right now</p>
          <div class="grid g3">
            <a
              v-for="d in destinations"
              :key="d.city"
              class="dest"
              :href="
                '#/search?city=' +
                encodeURIComponent(d.city) +
                '&' +
                defaultQS('')
              "
            >
              <hotel-img :src="d.img" :name="d.city"></hotel-img>
              <div class="meta">
                <h3>{{ d.city }}</h3>
                <span>{{ d.country }} · {{ plural(d.count, "hotel") }}</span>
              </div>
            </a>
          </div>
        </div>

        <div class="wrap section" v-if="featured.length">
          <div class="row between">
            <div>
              <h2>Featured stays</h2>
              <p class="muted">Hand-picked places guests love</p>
            </div>
            <a class="btn btn-outline" href="#/explore"
              >See all hotels<icon name="right" :size="16"></icon
            ></a>
          </div>
          <div class="grid g3">
            <a
              v-for="h in featured"
              :key="h.id"
              class="gcard"
              :href="'#/hotel/' + h.id"
              style="color: inherit; text-decoration: none"
            >
              <div class="img">
                <hotel-img
                  :src="h.images && h.images[0] && h.images[0].imageUrl"
                  :name="h.name"
                ></hotel-img>
              </div>
              <div class="bd">
                <h3 style="margin: 0; color: var(--blue)">{{ h.name }}</h3>
                <div class="loc">
                  <icon name="pin" :size="15"></icon>{{ h.city }},
                  {{ h.country }}
                </div>
                <div class="muted small" style="flex: 1">
                  {{ (h.description || "").slice(0, 90)
                  }}{{ (h.description || "").length > 90 ? "…" : "" }}
                </div>
              </div>
            </a>
          </div>
        </div>

        <div
          class="wrap section"
          v-if="!page.loading && !page.error && !home.hotels.length"
        >
          <div class="card empty">
            <div class="big"><icon name="building" :size="32"></icon></div>
            <h3>No hotels are live yet</h3>
            <p>
              Once an admin approves a hotel it will appear here. You can still
              try a search above.
            </p>
          </div>
        </div>

        <div class="wrap section">
          <div class="grid g3">
            <div class="usp">
              <div class="ico"><icon name="check" :size="22"></icon></div>
              <div>
                <h4>Real-time availability</h4>
                <p class="muted small">
                  We only show rooms that are free for your exact dates – no
                  surprises.
                </p>
              </div>
            </div>
            <div class="usp">
              <div class="ico"><icon name="mail" :size="22"></icon></div>
              <div>
                <h4>Instant confirmation</h4>
                <p class="muted small">
                  Your booking is confirmed straight away and sent to your
                  inbox.
                </p>
              </div>
            </div>
            <div class="usp">
              <div class="ico"><icon name="x" :size="22"></icon></div>
              <div>
                <h4>Easy cancellations</h4>
                <p class="muted small">
                  Change of plans? Cancel from “My bookings” in two clicks.
                </p>
              </div>
            </div>
          </div>
        </div>
      </section>

      <!-- ================= SEARCH RESULTS ================= -->
      <section v-if="route.name === 'search'" class="page">
        <div class="wrap">
          <search-bar
            :f="searchF"
            :min-in="minIn"
            compact
            :busy="sr.loading"
            @submit="submitSearch"
          ></search-bar>
          <div class="layout mt2">
            <aside
              class="card pad filter sticky no-print"
              v-if="sr.items && sr.items.length"
            >
              <div class="row between">
                <h3 style="margin: 0">Filter by</h3>
                <button class="btn btn-ghost btn-sm" @click="resetFilters">
                  Reset
                </button>
              </div>
              <h4>
                Max price per night:
                <span class="blue">{{ money(sr.priceMax) }}</span>
              </h4>
              <input
                type="range"
                min="0"
                :max="sr.priceCap"
                step="1"
                v-model.number="sr.priceMax"
                aria-label="Maximum price per night"
              />
              <h4>Room type</h4>
              <label
                v-for="t in typeOptions"
                :key="t"
                class="check"
                style="margin-bottom: 8px"
                ><input
                  type="checkbox"
                  :checked="sr.types.includes(t)"
                  @change="toggleType(t)"
                />{{ t }}</label
              >
              <h4>Your group</h4>
              <label class="check" style="align-items: flex-start"
                ><input
                  type="checkbox"
                  v-model="sr.fit"
                  style="margin-top: 3px"
                /><span
                  >Only hotels with a room that fits
                  {{ sr.q ? plural(sr.q.adults, "adult") : "1 adult"
                  }}<template v-if="sr.q && sr.q.children"
                    >,
                    {{ plural(sr.q.children, "child", "children") }}</template
                  ></span
                ></label
              >
            </aside>
            <div :style="sr.items && sr.items.length ? '' : 'grid-column:1/-1'">
              <alert-box
                v-if="sr.error"
                type="error"
                :msg="sr.error"
              ></alert-box>
              <div v-if="sr.loading">
                <div
                  v-for="i in 3"
                  :key="i"
                  class="skel"
                  style="height: 200px; margin-bottom: 16px"
                ></div>
              </div>
              <template v-else-if="sr.items">
                <div class="row between mb">
                  <div>
                    <h2 style="margin: 0">
                      {{ sr.q.city || "All destinations" }}:
                      {{ plural(results.length, "hotel") }} available
                    </h2>
                    <div class="muted small">
                      {{ fmtDate(sr.q.checkIn) }} →
                      {{ fmtDate(sr.q.checkOut) }} ·
                      {{ plural(srNights, "night") }} ·
                      {{ plural(sr.q.adults, "adult") }},
                      {{ plural(sr.q.children, "child", "children") }}
                    </div>
                  </div>
                  <div class="row no-print" style="gap: 12px; flex-wrap: wrap">
                    <div
                      class="viewtoggle no-print"
                      role="group"
                      aria-label="View"
                    >
                      <button
                        type="button"
                        :class="{ on: viewMode.sr === 'list' }"
                        @click="viewMode.sr = 'list'"
                      >
                        <icon name="list" :size="16"></icon>List</button
                      ><button
                        type="button"
                        :class="{ on: viewMode.sr === 'map' }"
                        @click="viewMode.sr = 'map'"
                      >
                        <icon name="map" :size="16"></icon>Map
                      </button>
                    </div>
                    <label class="small row" style="gap: 6px"
                      >Sort by
                      <select
                        v-model="sr.sort"
                        style="
                          padding: 8px 10px;
                          border: 1px solid #b5bcc5;
                          border-radius: 8px;
                        "
                      >
                        <option value="price">Price (lowest first)</option>
                        <option value="price-desc">
                          Price (highest first)
                        </option>
                        <option value="name">Name (A–Z)</option>
                        <option value="rooms">Most rooms available</option>
                      </select></label
                    >
                  </div>
                </div>
                <hotel-map
                  v-if="viewMode.sr === 'map' && results.length"
                  :pins="srPins"
                  :height="520"
                  style="display: block; margin-bottom: 16px"
                ></hotel-map>
                <article
                  v-show="viewMode.sr === 'list'"
                  v-for="h in results"
                  :key="h.hotelId"
                  class="hcard"
                >
                  <a class="hc-img" :href="hotelHref(h.hotelId)"
                    ><hotel-img
                      :src="imgOf(h.hotelId)"
                      :name="h.hotelName"
                    ></hotel-img
                  ></a>
                  <div class="hc-body">
                    <h3>
                      <a :href="hotelHref(h.hotelId)">{{ h.hotelName }}</a>
                    </h3>
                    <div class="loc">
                      <icon name="pin" :size="15"></icon>{{ h.address }},
                      {{ h.city }}, {{ h.country }}
                    </div>
                    <div
                      v-if="
                        hc[h.hotelId] &&
                        hc[h.hotelId].amenities &&
                        hc[h.hotelId].amenities.length
                      "
                      style="margin-bottom: 8px"
                    >
                      <span
                        v-for="a in hc[h.hotelId].amenities.slice(0, 5)"
                        :key="a.id"
                        class="chip"
                        >{{ amenityIcon(a.name) }} {{ a.name }}</span
                      >
                    </div>
                    <div
                      v-for="r in h.shown.slice(0, 2)"
                      :key="r.id"
                      class="small"
                      style="margin-bottom: 3px"
                    >
                      <b>{{ r.roomTypeName }}</b> · sleeps
                      {{ r.maxOccupancy }} · {{ money(r.pricePerNight) }}/night
                      <span
                        v-if="roomFits(r, sr.q.adults, sr.q.children)"
                        class="badge b-green"
                        ><icon name="check" :size="12"></icon>fits your
                        group</span
                      >
                    </div>
                    <div
                      class="small"
                      style="
                        color: var(--green);
                        font-weight: 600;
                        margin-top: 6px;
                      "
                    >
                      {{ plural(h.shown.length, "room") }} available for your
                      dates<span
                        v-if="!h.fitCount"
                        class="muted"
                        style="font-weight: 400"
                      >
                        – you may need more than one room</span
                      >
                    </div>
                  </div>
                  <div class="hc-price">
                    <span
                      v-if="hc[h.hotelId] && hc[h.hotelId].averageRating"
                      class="score"
                      >{{
                        Number(hc[h.hotelId].averageRating).toFixed(1)
                      }}</span
                    ><span v-else></span>
                    <div>
                      <div class="muted xs">
                        {{ plural(srNights, "night") }}, from
                      </div>
                      <strong>{{ money(h.from * srNights) }}</strong>
                      <div class="muted xs">{{ money(h.from) }} per night</div>
                    </div>
                    <a class="btn btn-primary" :href="hotelHref(h.hotelId)"
                      >See availability<icon name="right" :size="16"></icon
                    ></a>
                  </div>
                </article>
                <div v-if="!results.length" class="card empty">
                  <div class="big"><icon name="search" :size="30"></icon></div>
                  <h3>
                    {{
                      sr.items.length
                        ? "No hotels match your filters"
                        : "No hotels available for these dates"
                    }}
                  </h3>
                  <p v-if="sr.items.length">
                    Try raising the maximum price or clearing the room type
                    filter.
                  </p>
                  <p v-else>
                    Try different dates{{
                      sr.q.city ? ", or search another city" : ""
                    }}.
                  </p>
                  <button
                    v-if="sr.items.length"
                    class="btn btn-outline"
                    @click="resetFilters"
                  >
                    Clear filters
                  </button>
                  <a
                    v-else-if="sr.q.city"
                    class="btn btn-outline"
                    :href="'#/search?' + defaultQS('')"
                    >Search all destinations</a
                  >
                </div>
              </template>
              <div v-else-if="!sr.error" class="card empty">
                <div class="big"><icon name="calendar" :size="30"></icon></div>
                <h3>Check your search</h3>
                <p>Please fix the highlighted dates above and search again.</p>
              </div>
            </div>
          </div>
        </div>
      </section>

      <!-- ================= EXPLORE ================= -->
      <section v-if="route.name === 'explore'" class="page">
        <div class="wrap">
          <div class="row between" style="flex-wrap: wrap; gap: 10px">
            <div>
              <h2 style="margin-bottom: 4px">Explore hotels</h2>
              <p class="muted" style="margin-top: 0">
                Browse every hotel on VibeStay. Pick dates on a hotel page to
                see its available rooms.
              </p>
            </div>
            <div class="viewtoggle no-print" role="group" aria-label="View">
              <button
                type="button"
                :class="{ on: viewMode.ex === 'list' }"
                @click="viewMode.ex = 'list'"
              >
                <icon name="list" :size="16"></icon>List</button
              ><button
                type="button"
                :class="{ on: viewMode.ex === 'map' }"
                @click="viewMode.ex = 'map'"
              >
                <icon name="map" :size="16"></icon>Map
              </button>
            </div>
          </div>
          <form class="card pad mb" @submit.prevent="loadExplore(0)">
            <div class="grid g4" style="align-items: end">
              <f-input
                v-model="ex.q.search"
                label="Hotel name"
                placeholder="e.g. Grand"
              ></f-input>
              <f-input
                v-model="ex.q.city"
                label="City"
                placeholder="e.g. Manama"
                list="city-list"
              ></f-input>
              <f-input
                v-model="ex.q.country"
                label="Country"
                placeholder="e.g. Bahrain"
              ></f-input>
              <div class="field row" style="gap: 8px">
                <button
                  class="btn btn-primary"
                  :class="{ 'is-loading': ex.loading }"
                >
                  <icon name="search"></icon>Filter</button
                ><button
                  type="button"
                  class="btn btn-ghost"
                  @click="clearExplore"
                >
                  Clear
                </button>
              </div>
            </div>
            <label class="small row" style="gap: 8px"
              >Sort by
              <select
                v-model="ex.sort"
                @change="loadExplore(0)"
                style="
                  padding: 7px 10px;
                  border: 1px solid #b5bcc5;
                  border-radius: 8px;
                "
              >
                <option value="name,asc">Name (A–Z)</option>
                <option value="name,desc">Name (Z–A)</option>
                <option value="city,asc">City (A–Z)</option>
              </select></label
            >
          </form>
          <alert-box v-if="ex.error" type="error" :msg="ex.error"></alert-box>
          <div v-if="ex.loading" class="grid g3">
            <div
              v-for="i in 6"
              :key="i"
              class="skel"
              style="height: 290px"
            ></div>
          </div>
          <hotel-map
            v-else-if="ex.items.length && viewMode.ex === 'map'"
            :pins="exPins"
            :height="560"
          ></hotel-map>
          <div v-else-if="ex.items.length" class="grid g3">
            <a
              v-for="h in ex.items"
              :key="h.id"
              class="gcard"
              :href="'#/hotel/' + h.id"
              style="color: inherit; text-decoration: none"
            >
              <div class="img">
                <hotel-img
                  :src="h.images && h.images[0] && h.images[0].imageUrl"
                  :name="h.name"
                ></hotel-img>
              </div>
              <div class="bd">
                <h3 style="margin: 0; color: var(--blue)">{{ h.name }}</h3>
                <div class="loc">
                  <icon name="pin" :size="15"></icon>{{ h.address }},
                  {{ h.city }}
                </div>
                <div class="muted small" style="flex: 1">
                  {{ (h.description || "").slice(0, 110)
                  }}{{ (h.description || "").length > 110 ? "…" : "" }}
                </div>
                <div>
                  <span
                    v-for="a in (h.amenities || []).slice(0, 4)"
                    :key="a.id"
                    class="chip"
                    >{{ amenityIcon(a.name) }} {{ a.name }}</span
                  >
                </div>
              </div>
            </a>
          </div>
          <div v-else-if="!ex.error" class="card empty">
            <div class="big"><icon name="building" :size="30"></icon></div>
            <h3>No hotels found</h3>
            <p>Try a different search or clear your filters.</p>
            <button class="btn btn-outline" @click="clearExplore">
              Clear filters
            </button>
          </div>
          <div class="pager" v-if="ex.page > 0 || ex.hasNext">
            <button
              class="btn btn-outline btn-sm"
              :disabled="ex.page === 0"
              @click="loadExplore(ex.page - 1)"
            >
              <icon name="left" :size="16"></icon>Previous</button
            ><span class="muted small">Page {{ ex.page + 1 }}</span
            ><button
              class="btn btn-outline btn-sm"
              :disabled="!ex.hasNext"
              @click="loadExplore(ex.page + 1)"
            >
              Next<icon name="right" :size="16"></icon>
            </button>
          </div>
        </div>
      </section>

      <!-- ================= HOTEL DETAILS ================= -->
      <section v-if="route.name === 'hotel'" class="page">
        <div class="wrap">
          <div v-if="page.loading">
            <div class="skel" style="height: 340px; margin-bottom: 16px"></div>
            <div class="skel" style="height: 140px"></div>
          </div>
          <div v-else-if="page.error" class="card empty">
            <div class="big"><icon name="alert" :size="30"></icon></div>
            <h3>We couldn’t load this hotel</h3>
            <p>{{ page.error }}</p>
            <a class="btn btn-primary" href="#/">Back to search</a>
          </div>
          <template v-else-if="hp.hotel">
            <div class="crumbs">
              <a href="#/">Stays</a> › <a href="#/explore">Hotels</a> ›
              {{ hp.hotel.name }}
            </div>
            <div
              class="row between"
              style="align-items: flex-start; margin-bottom: 14px"
            >
              <div style="min-width: 0">
                <h1 style="margin-bottom: 6px">{{ hp.hotel.name }}</h1>
                <div class="loc" style="font-size: 0.95rem">
                  <icon name="pin" :size="16"></icon>{{ hp.hotel.address }},
                  {{ hp.hotel.city }}, {{ hp.hotel.country }} ·
                  <a :href="mapsLink" target="_blank" rel="noopener"
                    >Show on map<icon name="ext" :size="13"></icon
                  ></a>
                </div>
              </div>
              <div class="row no-print">
                <div class="row" v-if="hp.hotel.averageRating" style="gap: 8px">
                  <span class="score">{{
                    Number(hp.hotel.averageRating).toFixed(1)
                  }}</span
                  ><span class="small muted" v-if="hp.hotel.reviewCount">{{
                    plural(hp.hotel.reviewCount, "review")
                  }}</span>
                </div>
                <button class="btn btn-primary" @click="scrollToRooms">
                  See availability
                </button>
              </div>
            </div>

            <div
              class="gallery"
              :class="{
                none: !hotelPhotos.length,
                one: hotelPhotos.length === 1,
                two: hotelPhotos.length === 2,
                three: hotelPhotos.length === 3,
              }"
            >
              <template v-if="hotelPhotos.length">
                <div
                  v-for="(u, i) in hotelPhotos.slice(
                    0,
                    hotelPhotos.length < 2 ? 1 : 3,
                  )"
                  :key="i"
                  @click="hp.lightbox = i"
                >
                  <hotel-img
                    :src="u"
                    :name="hp.hotel.name"
                    :alt="
                      (hp.images[i] && hp.images[i].altText) || hp.hotel.name
                    "
                  ></hotel-img>
                  <div
                    v-if="i === 2 && hotelPhotos.length > 3"
                    class="more-pics"
                  >
                    +{{ hotelPhotos.length - 3 }} photos
                  </div>
                </div>
              </template>
              <div
                v-else
                style="grid-column: 1/-1; grid-row: 1/-1; cursor: default"
              >
                <hotel-img :src="''" :name="hp.hotel.name"></hotel-img>
              </div>
            </div>
            <div
              v-if="hp.lightbox >= 0"
              class="lightbox"
              @click="hp.lightbox = -1"
            >
              <button
                class="btn btn-light btn-sm"
                @click.stop="hp.lightbox = -1"
              >
                <icon name="x" :size="16"></icon>Close
              </button>
              <img
                :src="imgUrl(hotelPhotos[hp.lightbox])"
                :alt="
                  (hp.images[hp.lightbox] && hp.images[hp.lightbox].altText) ||
                  hp.hotel.name
                "
                @click.stop
              />
            </div>

            <div class="layout-b mt2">
              <div class="stack">
                <div class="card pad">
                  <h3>About this hotel</h3>
                  <p style="white-space: pre-line; margin: 0">
                    {{ hp.hotel.description }}
                  </p>
                </div>

                <div
                  class="card pad"
                  v-if="hp.hotel.amenities && hp.hotel.amenities.length"
                >
                  <h3>Facilities</h3>
                  <div class="grid g3" style="gap: 10px">
                    <div
                      v-for="a in hp.hotel.amenities"
                      :key="a.id"
                      class="fact"
                    >
                      <span style="font-size: 1.3rem">{{
                        amenityIcon(a.name)
                      }}</span>
                      <div>
                        <b>{{ a.name }}</b>
                        <div class="xs muted" v-if="a.description">
                          {{ a.description }}
                        </div>
                        <div
                          class="xs"
                          v-if="a.additionalCost > 0"
                          style="color: var(--amber)"
                        >
                          + {{ money(a.additionalCost) }}
                        </div>
                        <div class="xs" style="color: var(--green)" v-else>
                          Free
                        </div>
                      </div>
                    </div>
                  </div>
                </div>

                <div class="card pad" id="rooms">
                  <h3>Availability</h3>
                  <p class="muted small">
                    Choose your dates to see which rooms are free. Select one or
                    more rooms, then reserve.
                  </p>
                  <search-bar
                    :f="searchF"
                    hide-city
                    compact
                    :min-in="minIn"
                    :busy="hp.roomsLoading"
                    @submit="checkHotelDates"
                  ></search-bar>
                  <alert-box
                    v-if="hp.roomsError"
                    type="error"
                    :msg="hp.roomsError"
                  ></alert-box>
                  <div
                    v-if="hp.roomsLoading"
                    class="skel mt"
                    style="height: 120px"
                  ></div>
                  <div
                    v-else-if="hp.rooms === null && !hp.roomsError"
                    class="empty"
                  >
                    <div class="big">
                      <icon name="calendar" :size="30"></icon>
                    </div>
                    <p style="margin: 0">
                      Pick your check-in and check-out dates above and press
                      <b>Search</b>.
                    </p>
                  </div>
                  <div v-else-if="hp.rooms && !hp.rooms.length" class="empty">
                    <div class="big"><icon name="bed" :size="30"></icon></div>
                    <h3>No rooms available for these dates</h3>
                    <p>
                      Try different dates – this hotel is fully booked for
                      {{ fmtDate(hp.q.checkIn) }} →
                      {{ fmtDate(hp.q.checkOut) }}.
                    </p>
                  </div>
                  <div v-else-if="hp.rooms" class="scroll-x mt">
                    <table class="t">
                      <thead>
                        <tr>
                          <th>Room type</th>
                          <th>Sleeps</th>
                          <th>Price per night</th>
                          <th>Total ({{ plural(hpNights, "night") }})</th>
                          <th style="width: 130px"></th>
                        </tr>
                      </thead>
                      <tbody>
                        <tr
                          v-for="r in hp.rooms"
                          :key="r.id"
                          :class="{ sel: hp.sel.includes(r.id) }"
                        >
                          <td>
                            <b style="color: var(--blue)">{{
                              r.roomTypeName
                            }}</b>
                            <div class="xs muted">
                              Room {{ r.roomNumber }} · Floor
                              {{ r.floorNumber }}
                            </div>
                            <span
                              v-if="roomFits(r, hp.q.adults, hp.q.children)"
                              class="badge b-green"
                              ><icon name="check" :size="12"></icon>Fits your
                              group</span
                            >
                            <span v-else class="badge b-yellow"
                              >Smaller than your group ({{ hp.q.adults }}+{{
                                hp.q.children
                              }})</span
                            >
                          </td>
                          <td class="small">
                            <div>
                              <icon name="user" :size="14"></icon> up to
                              {{ plural(r.maxAdults, "adult") }}
                            </div>
                            <div>
                              <icon name="baby" :size="14"></icon> up to
                              {{ plural(r.maxChildren, "child", "children") }}
                            </div>
                            <div class="muted">
                              max {{ r.maxOccupancy }} guests
                            </div>
                          </td>
                          <td>
                            <b>{{ money(r.pricePerNight) }}</b>
                          </td>
                          <td>
                            <b>{{ money(r.pricePerNight * hpNights) }}</b>
                            <div class="xs muted">taxes may apply</div>
                          </td>
                          <td>
                            <button
                              class="btn btn-sm"
                              :class="
                                hp.sel.includes(r.id)
                                  ? 'btn-green'
                                  : 'btn-outline'
                              "
                              @click="toggleRoom(r.id)"
                            >
                              <icon
                                :name="hp.sel.includes(r.id) ? 'check' : 'plus'"
                                :size="15"
                              ></icon
                              >{{
                                hp.sel.includes(r.id) ? "Selected" : "Select"
                              }}
                            </button>
                          </td>
                        </tr>
                      </tbody>
                    </table>
                  </div>
                </div>
              </div>

              <aside class="stack sticky">
                <div class="card pad" v-if="hp.rooms && hp.rooms.length">
                  <h3>Your selection</h3>
                  <div v-if="!hp.sel.length" class="muted small">
                    No rooms selected yet. Pick one or more rooms from the list.
                  </div>
                  <template v-else>
                    <div
                      v-for="r in hpSelected"
                      :key="r.id"
                      class="row between small"
                      style="margin-bottom: 6px"
                    >
                      <span
                        >{{ r.roomTypeName }}
                        <span class="muted"
                          >· Room {{ r.roomNumber }}</span
                        ></span
                      ><b>{{ money(r.pricePerNight) }}</b>
                    </div>
                    <hr style="border: 0; border-top: 1px solid var(--line)" />
                    <div class="small muted">
                      {{ fmtDate(hp.q.checkIn) }} →
                      {{ fmtDate(hp.q.checkOut) }} ·
                      {{ plural(hpNights, "night") }}
                    </div>
                    <div class="row between" style="margin: 8px 0 14px">
                      <span>Total</span
                      ><strong style="font-size: 1.4rem">{{
                        money(hpTotal)
                      }}</strong>
                    </div>
                  </template>
                  <button
                    class="btn btn-primary btn-block btn-lg mt"
                    :disabled="!hp.sel.length"
                    @click="reserve"
                  >
                    Reserve<icon name="right" :size="18"></icon>
                  </button>
                  <p class="xs muted center mt" style="margin-bottom: 0">
                    You won’t be charged yet – you’ll review everything first.
                  </p>
                </div>
                <div class="card pad">
                  <h4>Hotel policy</h4>
                  <template v-if="hp.policy"
                    ><div class="small" style="line-height: 1.9">
                      <div>
                        <icon name="baby" :size="15"></icon>
                        {{ policyText(hp.policy).infant }}
                      </div>
                      <div>
                        <icon name="users" :size="15"></icon>
                        {{ policyText(hp.policy).child }}
                      </div>
                      <div>
                        <icon name="user" :size="15"></icon>
                        {{ policyText(hp.policy).adult }}
                      </div>
                    </div></template
                  >
                  <p v-else class="small muted" style="margin: 0">
                    This hotel hasn’t published its child policy yet.<span
                      v-if="hp.policyMissing"
                    >
                      Online booking becomes available once it does.</span
                    >
                  </p>
                </div>
                <div class="card pad">
                  <h4>Location &amp; contact</h4>
                  <iframe
                    v-if="mapSrc"
                    class="mapframe"
                    :src="mapSrc"
                    loading="lazy"
                    title="Hotel location map"
                  ></iframe>
                  <div class="small" style="margin-top: 10px; line-height: 1.9">
                    <div>
                      <icon name="pin" :size="15"></icon>
                      {{ hp.hotel.address }}, {{ hp.hotel.city }}
                    </div>
                    <div v-if="hp.hotel.phone">
                      <icon name="phone" :size="15"></icon> {{ hp.hotel.phone }}
                    </div>
                    <div>
                      <a :href="mapsLink" target="_blank" rel="noopener"
                        >Open in Google Maps<icon name="ext" :size="13"></icon
                      ></a>
                    </div>
                  </div>
                </div>
              </aside>
            </div>
          </template>
        </div>
      </section>

      <!-- ================= BOOKING ================= -->
      <section v-if="route.name === 'book'" class="page">
        <div class="wrap">
          <div v-if="page.loading">
            <div class="skel" style="height: 80px; margin-bottom: 16px"></div>
            <div class="skel" style="height: 360px"></div>
          </div>
          <div v-else-if="page.error" class="card empty">
            <div class="big"><icon name="alert" :size="30"></icon></div>
            <h3>We couldn’t start your booking</h3>
            <p>{{ page.error }}</p>
            <a class="btn btn-primary" :href="'#/hotel/' + route.params.id"
              >Back to the hotel</a
            >
          </div>

          <!-- confirmation -->
          <template v-else-if="bk.done">
            <div class="steps">
              <div class="step done"><i>✓</i>Your selection</div>
              <div class="step done"><i>✓</i>Guest details</div>
              <div class="step on"><i>3</i>Confirmed</div>
            </div>
            <div
              class="card pad center"
              style="max-width: 720px; margin: 0 auto"
            >
              <div
                style="
                  width: 72px;
                  height: 72px;
                  border-radius: 50%;
                  background: var(--green-s);
                  color: var(--green);
                  display: grid;
                  place-items: center;
                  margin: 0 auto 12px;
                "
              >
                <icon name="check" :size="40"></icon>
              </div>
              <h1>Your booking is confirmed!</h1>
              <p class="muted">
                We’ve emailed your confirmation. Show this reference at
                check-in.
              </p>
              <div v-if="bk.done.bookingReference" style="margin: 16px 0">
                <span class="ref big-ref">{{ bk.done.bookingReference }}</span>
              </div>
              <div class="card pad-s" style="text-align: left; margin: 18px 0">
                <booking-detail
                  v-if="bk.done.rooms && bk.done.rooms.length"
                  :b="bk.done"
                ></booking-detail>
                <dl v-else class="kv">
                  <dt>Hotel</dt>
                  <dd>{{ bk.done.hotelName }}</dd>
                  <dt>Dates</dt>
                  <dd>
                    {{ fmtDate(bk.done.checkIn) }} →
                    {{ fmtDate(bk.done.checkOut) }}
                  </dd>
                  <dt>Total</dt>
                  <dd>{{ money(bk.done.totalAmount) }}</dd>
                </dl>
              </div>
              <div class="row" style="justify-content: center">
                <a class="btn btn-primary" href="#/bookings"
                  ><icon name="list"></icon>View my bookings</a
                ><button class="btn btn-outline" @click="printPage">
                  <icon name="print"></icon>Print</button
                ><a class="btn btn-ghost" href="#/">Book another stay</a>
              </div>
            </div>
          </template>

          <template v-else-if="bk.hotel">
            <div class="steps no-print">
              <div class="step done"><i>✓</i>Your selection</div>
              <div class="step on"><i>2</i>Guest details</div>
              <div class="step"><i>3</i>Confirmed</div>
            </div>
            <h1>Complete your booking</h1>
            <alert-box v-if="bk.banner" type="error"
              ><div>
                {{ bk.banner }}
                <a :href="'#/hotel/' + bk.hotel.id">Back to rooms</a>
              </div></alert-box
            >
            <alert-box v-if="bk.noPolicy" type="warn"
              ><b>This hotel can’t take online bookings yet.</b> It hasn’t set
              up its child policy, which we need to check room capacity. Please
              choose another hotel or contact the hotel directly.
              <a :href="'#/hotel/' + bk.hotel.id">Back</a></alert-box
            >
            <alert-box v-if="bk.missing.length" type="warn"
              >{{ plural(bk.missing.length, "room was", "rooms were") }} just
              booked by someone else and
              {{ bk.missing.length === 1 ? "was" : "were" }} removed from your
              selection.
              <a
                :href="
                  '#/hotel/' +
                  bk.hotel.id +
                  '?in=' +
                  bk.q.checkIn +
                  '&out=' +
                  bk.q.checkOut +
                  '&a=' +
                  bk.q.adults +
                  '&c=' +
                  bk.q.children
                "
                >Choose another room</a
              ></alert-box
            >
            <div v-if="!bk.rooms.length && !bk.noPolicy" class="card empty">
              <div class="big"><icon name="bed" :size="30"></icon></div>
              <h3>Those rooms are no longer available</h3>
              <p>Please go back and choose different rooms or dates.</p>
              <a class="btn btn-primary" :href="'#/hotel/' + bk.hotel.id"
                >Choose rooms</a
              >
            </div>

            <form
              v-if="bk.rooms.length && !bk.noPolicy"
              class="layout-b"
              @submit.prevent="submitBooking"
              novalidate
            >
              <div class="stack">
                <div v-for="(r, ri) in bk.rooms" :key="r.id" class="card">
                  <div class="card-h">
                    <div>
                      <h3>Room {{ ri + 1 }}: {{ r.roomTypeName }}</h3>
                      <div class="xs muted">
                        Room {{ r.roomNumber }} · up to
                        {{ r.maxAdults }} adults, {{ r.maxChildren }} children,
                        {{ r.maxOccupancy }} guests in total
                      </div>
                    </div>
                    <b
                      >{{ money(r.pricePerNight)
                      }}<span class="xs muted"> / night</span></b
                    >
                  </div>
                  <div class="pad">
                    <div
                      v-for="(g, gi) in r.guests"
                      :key="gi"
                      class="guest-row"
                      style="margin-bottom: 6px"
                    >
                      <f-input
                        v-model="g.name"
                        :label="gi === 0 ? 'Guest name' : ''"
                        placeholder="Full name"
                        autocomplete="off"
                        :error="bk.errs['n' + ri + '_' + gi]"
                        class="full-m"
                      ></f-input>
                      <f-input
                        v-model="g.age"
                        :label="gi === 0 ? 'Age' : ''"
                        type="number"
                        min="0"
                        max="120"
                        placeholder="Age"
                        inputmode="numeric"
                        :error="bk.errs['a' + ri + '_' + gi]"
                      ></f-input>
                      <div class="field">
                        <label v-if="gi === 0">Guest type</label>
                        <div style="padding: 10px 0">
                          <span
                            v-if="guestKind(g)"
                            class="badge"
                            :class="
                              guestKind(g) === 'Adult'
                                ? 'b-blue'
                                : guestKind(g) === 'Child'
                                  ? 'b-yellow'
                                  : 'b-green'
                            "
                            >{{ guestKind(g) }}</span
                          ><span v-else class="xs muted">Enter age</span>
                        </div>
                      </div>
                      <div class="field">
                        <label v-if="gi === 0">&nbsp;</label
                        ><button
                          type="button"
                          class="btn btn-ghost btn-sm"
                          style="margin-top: 2px"
                          :disabled="r.guests.length <= 1"
                          @click="removeGuest(r, gi)"
                          aria-label="Remove guest"
                        >
                          <icon name="trash" :size="16"></icon>
                        </button>
                      </div>
                    </div>
                    <button
                      type="button"
                      class="btn btn-outline btn-sm"
                      @click="addGuest(r)"
                    >
                      <icon name="plus" :size="15"></icon>Add guest
                    </button>
                    <div style="margin-top: 14px">
                      <div class="row between xs muted">
                        <span
                          >Occupancy: {{ roomStats(r).total }} of
                          {{ r.maxOccupancy }} ·
                          {{ roomStats(r).adults }} adult(s),
                          {{ roomStats(r).children }} child(ren)<span
                            v-if="roomStats(r).infants"
                            >, {{ roomStats(r).infants }} infant(s)</span
                          ></span
                        >
                      </div>
                      <div
                        class="cap"
                        :class="{ over: roomStats(r).problems.length }"
                        style="margin-top: 5px"
                      >
                        <i :style="{ width: roomStats(r).pct + '%' }"></i>
                      </div>
                      <div
                        v-for="p in roomStats(r).problems"
                        :key="p"
                        class="err bk-err"
                        style="margin-top: 6px"
                      >
                        <icon name="alert" :size="14"></icon>{{ p }}
                      </div>
                      <div
                        v-if="bk.errs['r' + ri]"
                        class="err bk-err"
                        style="margin-top: 6px"
                      >
                        <icon name="alert" :size="14"></icon
                        >{{ bk.errs["r" + ri] }}
                      </div>
                      <div
                        v-if="roomStats(r).total > 0 && !roomStats(r).adults"
                        class="hint"
                        style="margin-top: 6px"
                      >
                        Tip: there’s no adult in this room – children usually
                        need to be accompanied by an adult.
                      </div>
                    </div>
                  </div>
                </div>
                <div class="card pad">
                  <f-input
                    v-model="bk.text"
                    textarea
                    :rows="3"
                    label="Special requests (optional)"
                    :maxlength="500"
                    counter
                    placeholder="e.g. late check-in, high floor, extra pillows"
                    :error="bk.errs.text"
                    hint="Requests can’t be guaranteed, but the hotel will do its best."
                  ></f-input>
                </div>
              </div>
              <aside class="sticky stack">
                <div class="card pad">
                  <h3>{{ bk.hotel.name }}</h3>
                  <div class="loc">
                    <icon name="pin" :size="15"></icon>{{ bk.hotel.city }},
                    {{ bk.hotel.country }}
                  </div>
                  <dl class="kv" style="grid-template-columns: 90px 1fr">
                    <dt>Check-in</dt>
                    <dd>{{ fmtDate(bk.q.checkIn) }}</dd>
                    <dt>Check-out</dt>
                    <dd>{{ fmtDate(bk.q.checkOut) }}</dd>
                    <dt>Length</dt>
                    <dd>{{ plural(bkNights, "night") }}</dd>
                  </dl>
                  <hr
                    style="
                      border: 0;
                      border-top: 1px solid var(--line);
                      margin: 14px 0;
                    "
                  />
                  <div
                    v-for="r in bk.rooms"
                    :key="r.id"
                    class="row between small"
                    style="margin-bottom: 6px"
                  >
                    <span
                      >{{ r.roomTypeName }} ×
                      {{ plural(bkNights, "night") }}</span
                    ><span>{{ money(r.pricePerNight * bkNights) }}</span>
                  </div>
                  <div class="row between" style="margin-top: 12px">
                    <b>Total</b
                    ><strong style="font-size: 1.6rem">{{
                      money(bkTotal)
                    }}</strong>
                  </div>
                  <div class="xs muted">
                    Pay at the hotel. Free cancellation from “My bookings” while
                    your booking is confirmed.
                  </div>
                  <button
                    class="btn btn-yellow btn-block btn-lg mt"
                    :class="{ 'is-loading': bk.busy }"
                    style="font-weight: 800"
                  >
                    Confirm booking
                  </button>
                  <a
                    class="btn btn-ghost btn-block mt"
                    :href="
                      '#/hotel/' +
                      bk.hotel.id +
                      '?in=' +
                      bk.q.checkIn +
                      '&out=' +
                      bk.q.checkOut +
                      '&a=' +
                      bk.q.adults +
                      '&c=' +
                      bk.q.children
                    "
                    >← Change rooms</a
                  >
                </div>
              </aside>
            </form>
          </template>
        </div>
      </section>

      <!-- ================= AUTH: LOGIN ================= -->
      <section v-if="route.name === 'login'" class="page">
        <div class="wrap">
          <div class="card pad authbox">
            <h2>Sign in</h2>
            <p class="muted">Welcome back! Sign in to manage your bookings.</p>
            <alert-box v-if="loginF.banner" type="error"
              ><div>
                {{ loginF.banner }}
                <div
                  v-if="/verify your email/i.test(loginF.banner)"
                  class="small mt"
                >
                  Check your inbox for the verification link, or
                  <a href="#/verify-email">paste your token here</a>.
                </div>
              </div></alert-box
            >
            <form @submit.prevent="doLogin" novalidate>
              <f-input
                v-model="loginF.values.email"
                label="Email address"
                type="email"
                autocomplete="email"
                placeholder="you@example.com"
                required
                :error="loginF.errors.email"
                @blur="loginF.check('email')"
              ></f-input>
              <f-input
                v-model="loginF.values.password"
                label="Password"
                type="password"
                autocomplete="current-password"
                required
                :error="loginF.errors.password"
                @blur="loginF.check('password')"
              ></f-input>
              <div class="row between" style="margin: -4px 0 14px">
                <span></span
                ><a href="#/forgot" class="small">Forgot your password?</a>
              </div>
              <button
                class="btn btn-primary btn-block btn-lg"
                :class="{ 'is-loading': loginF.busy }"
              >
                Sign in
              </button>
            </form>
            <p class="center small mt2" style="margin-bottom: 0">
              New to VibeStay? <a href="#/register"><b>Create an account</b></a>
            </p>
          </div>
        </div>
      </section>

      <!-- ================= AUTH: REGISTER ================= -->
      <section v-if="route.name === 'register'" class="page">
        <div class="wrap">
          <div class="card pad authbox wide">
            <h2>Create your account</h2>
            <p class="muted">
              Join VibeStay to book hotels and manage your trips. We’ll email
              you a link to verify your address.
            </p>
            <alert-box
              v-if="regF.banner"
              type="error"
              :msg="regF.banner"
            ></alert-box>
            <form @submit.prevent="doRegister" novalidate>
              <div
                class="row"
                style="gap: 18px; margin-bottom: 14px; align-items: center"
              >
                <span
                  class="avatar lg"
                  :style="regImage.error ? 'outline:3px solid var(--red)' : ''"
                  ><img
                    v-if="regImage.preview"
                    :src="regImage.preview"
                    alt="Profile preview" /><icon
                    v-else
                    name="user"
                    :size="38"
                  ></icon
                ></span>
                <div>
                  <label class="btn btn-outline btn-sm" style="cursor: pointer"
                    ><icon name="upload" :size="15"></icon
                    >{{ regImage.file ? "Change photo" : "Choose profile photo"
                    }}<input
                      type="file"
                      accept="image/jpeg,image/png,image/webp"
                      class="hide"
                      @change="pickImage($event, regImage)"
                  /></label>
                  <div class="hint" style="margin-top: 4px">
                    Optional · JPG, PNG or WebP, up to 1 MB. You can add or
                    change it later in your profile.
                  </div>
                  <div v-if="regImage.error" class="err" role="alert">
                    <icon name="alert" :size="14"></icon>{{ regImage.error }}
                  </div>
                </div>
              </div>
              <div class="form-grid">
                <f-input
                  v-model="regF.values.firstName"
                  label="First name"
                  required
                  autocomplete="given-name"
                  :error="regF.errors.firstName"
                  @blur="regF.check('firstName')"
                ></f-input>
                <f-input
                  v-model="regF.values.lastName"
                  label="Last name"
                  required
                  autocomplete="family-name"
                  :error="regF.errors.lastName"
                  @blur="regF.check('lastName')"
                ></f-input>
                <f-input
                  v-model="regF.values.username"
                  label="Username"
                  required
                  autocomplete="username"
                  :error="regF.errors.username"
                  @blur="regF.check('username')"
                ></f-input>
                <f-input
                  v-model="regF.values.phone"
                  label="Phone number"
                  type="tel"
                  required
                  autocomplete="tel"
                  placeholder="+973 3xxx xxxx"
                  :error="regF.errors.phone"
                  @blur="regF.check('phone')"
                ></f-input>
                <f-input
                  class="full"
                  v-model="regF.values.email"
                  label="Email address"
                  type="email"
                  required
                  autocomplete="email"
                  placeholder="you@example.com"
                  :error="regF.errors.email"
                  @blur="regF.check('email')"
                ></f-input>
                <div>
                  <f-input
                    v-model="regF.values.password"
                    label="Password"
                    type="password"
                    required
                    autocomplete="new-password"
                    hint="At least 8 characters"
                    :error="regF.errors.password"
                    @blur="regF.check('password')"
                  ></f-input>
                  <div
                    v-if="regF.values.password"
                    class="strength"
                    style="margin: -8px 0 12px"
                  >
                    <i
                      :style="{
                        width: pwStrength.pct + '%',
                        background: pwStrength.color,
                      }"
                    ></i>
                  </div>
                  <div
                    v-if="regF.values.password"
                    class="xs"
                    style="margin: -6px 0 10px"
                    :style="{ color: pwStrength.color }"
                  >
                    {{ pwStrength.label }}
                  </div>
                </div>
                <f-input
                  v-model="regF.values.confirmedPassword"
                  label="Confirm password"
                  type="password"
                  required
                  autocomplete="new-password"
                  :error="regF.errors.confirmedPassword"
                  @blur="regF.check('confirmedPassword')"
                ></f-input>
              </div>
              <button
                class="btn btn-primary btn-block btn-lg"
                :class="{ 'is-loading': regF.busy }"
              >
                Create account
              </button>
            </form>
            <p class="center small mt2" style="margin-bottom: 0">
              Already have an account? <a href="#/login"><b>Sign in</b></a>
            </p>
          </div>
        </div>
      </section>

      <section v-if="route.name === 'check-email'" class="page">
        <div class="wrap">
          <div class="card pad authbox center">
            <div
              style="
                width: 72px;
                height: 72px;
                border-radius: 50%;
                background: var(--blue-s);
                color: var(--blue);
                display: grid;
                place-items: center;
                margin: 0 auto 12px;
              "
            >
              <icon name="mail" :size="36"></icon>
            </div>
            <h2>Check your email</h2>
            <p class="muted">
              We’ve sent a verification link<span v-if="route.query.email">
                to <b>{{ route.query.email }}</b></span
              >. Click it to activate your account, then sign in. The link is
              valid for 24 hours.
            </p>
            <a class="btn btn-primary btn-block" href="#/login"
              >Go to sign in</a
            >
            <p class="small muted mt">
              Didn’t get it? Check your spam folder. Already have the token?
              <a href="#/verify-email">Verify manually</a>
            </p>
          </div>
        </div>
      </section>

      <section v-if="route.name === 'verify-email'" class="page">
        <div class="wrap">
          <div class="card pad authbox center">
            <template v-if="verify.state === 'loading'"
              ><div class="skel" style="height: 90px"></div>
              <p class="muted mt">Verifying your email…</p></template
            >
            <template v-else-if="verify.state === 'ok'">
              <div
                style="
                  width: 72px;
                  height: 72px;
                  border-radius: 50%;
                  background: var(--green-s);
                  color: var(--green);
                  display: grid;
                  place-items: center;
                  margin: 0 auto 12px;
                "
              >
                <icon name="check" :size="40"></icon>
              </div>
              <h2>Email verified</h2>
              <p class="muted">Your account is active. You can sign in now.</p>
              <a class="btn btn-primary btn-block" href="#/login">Sign in</a>
            </template>
            <template v-else>
              <h2>Verify your email</h2>
              <alert-box
                v-if="verify.state === 'error'"
                type="error"
                :msg="verify.msg"
              ></alert-box>
              <p class="muted">
                Open the link from your verification email, or paste the link /
                token below.
              </p>
              <form
                @submit.prevent="submitVerifyToken"
                style="text-align: left"
              >
                <f-input
                  v-model="verify.token"
                  label="Verification link or token"
                  placeholder="Paste here"
                ></f-input
                ><button class="btn btn-primary btn-block">Verify email</button>
              </form>
            </template>
          </div>
        </div>
      </section>

      <section v-if="route.name === 'forgot'" class="page">
        <div class="wrap">
          <div class="card pad authbox">
            <template v-if="!forgotSent">
              <h2>Forgot your password?</h2>
              <p class="muted">
                Enter your email and we’ll send you a link to reset it.
              </p>
              <alert-box
                v-if="forgotF.banner"
                type="error"
                :msg="forgotF.banner"
              ></alert-box>
              <form @submit.prevent="doForgot" novalidate>
                <f-input
                  v-model="forgotF.values.email"
                  label="Email address"
                  type="email"
                  required
                  autocomplete="email"
                  :error="forgotF.errors.email"
                  @blur="forgotF.check('email')"
                ></f-input>
                <button
                  class="btn btn-primary btn-block btn-lg"
                  :class="{ 'is-loading': forgotF.busy }"
                >
                  Send reset link
                </button>
              </form>
              <p class="center small mt2" style="margin-bottom: 0">
                <a href="#/login">← Back to sign in</a>
              </p>
            </template>
            <template v-else>
              <div class="center">
                <div
                  style="
                    width: 72px;
                    height: 72px;
                    border-radius: 50%;
                    background: var(--green-s);
                    color: var(--green);
                    display: grid;
                    place-items: center;
                    margin: 0 auto 12px;
                  "
                >
                  <icon name="mail" :size="36"></icon>
                </div>
                <h2>Check your inbox</h2>
                <p class="muted">
                  If an account exists for that email, a reset link is on its
                  way. It expires in 15 minutes.
                </p>
                <a class="btn btn-primary btn-block" href="#/reset-password"
                  >I have my reset token</a
                >
                <p class="small mt"><a href="#/login">Back to sign in</a></p>
              </div>
            </template>
          </div>
        </div>
      </section>

      <section v-if="route.name === 'reset-password'" class="page">
        <div class="wrap">
          <div class="card pad authbox">
            <h2>Choose a new password</h2>
            <p class="muted">
              Paste the reset link or token from your email, then pick a new
              password.
            </p>
            <alert-box
              v-if="resetF.banner"
              type="error"
              :msg="resetF.banner"
            ></alert-box>
            <form @submit.prevent="doReset" novalidate>
              <f-input
                v-model="resetF.values.token"
                label="Reset link or token"
                required
                :error="resetF.errors.token"
                @blur="resetF.check('token')"
              ></f-input>
              <f-input
                v-model="resetF.values.password"
                label="New password"
                type="password"
                required
                autocomplete="new-password"
                hint="At least 8 characters"
                :error="resetF.errors.password"
                @blur="resetF.check('password')"
              ></f-input>
              <f-input
                v-model="resetF.values.confirmedPassword"
                label="Confirm new password"
                type="password"
                required
                autocomplete="new-password"
                :error="resetF.errors.confirmedPassword"
                @blur="resetF.check('confirmedPassword')"
              ></f-input>
              <button
                class="btn btn-primary btn-block btn-lg"
                :class="{ 'is-loading': resetF.busy }"
              >
                Reset password
              </button>
            </form>
          </div>
        </div>
      </section>

      <!-- ================= PROFILE ================= -->
      <section v-if="route.name === 'profile' && auth.user" class="page">
        <div class="wrap" style="max-width: 860px">
          <div v-if="page.error" class="alert error">
            <icon name="alert"></icon>{{ page.error }}
          </div>
          <div class="card pad row" style="gap: 20px; margin-bottom: 18px">
            <span class="avatar lg"
              ><img
                v-if="avatarUp.preview || profileImage"
                :src="avatarUp.preview || profileImage"
                alt="Your profile photo"
                @error="$event.target.remove()"
              /><template v-else>{{ userInitials }}</template></span
            >
            <div>
              <h2 style="margin-bottom: 2px">
                {{ auth.user.firstName }} {{ auth.user.lastName }}
              </h2>
              <div class="muted">
                @{{ auth.user.username }} · {{ auth.user.email }}
              </div>
              <span class="badge b-blue" style="margin-top: 6px">{{
                roleLabel(role)
              }}</span>
            </div>
          </div>
          <div class="grid g2" style="align-items: start">
            <div class="card pad">
              <h3>Personal details</h3>
              <alert-box
                v-if="profileF.banner"
                type="error"
                :msg="profileF.banner"
              ></alert-box>
              <form @submit.prevent="saveProfile" novalidate>
                <f-input
                  :model-value="auth.user.email"
                  label="Email"
                  disabled
                  hint="Your email is your sign-in and can’t be changed"
                ></f-input>
                <f-input
                  v-model="profileF.values.username"
                  label="Username"
                  required
                  :error="profileF.errors.username"
                  @blur="profileF.check('username')"
                ></f-input>
                <div class="form-grid">
                  <f-input
                    v-model="profileF.values.firstName"
                    label="First name"
                    required
                    :error="profileF.errors.firstName"
                    @blur="profileF.check('firstName')"
                  ></f-input
                  ><f-input
                    v-model="profileF.values.lastName"
                    label="Last name"
                    required
                    :error="profileF.errors.lastName"
                    @blur="profileF.check('lastName')"
                  ></f-input>
                </div>
                <f-input
                  v-model="profileF.values.phone"
                  label="Phone number"
                  type="tel"
                  required
                  :error="profileF.errors.phone"
                  @blur="profileF.check('phone')"
                ></f-input>
                <button
                  class="btn btn-primary"
                  :class="{ 'is-loading': profileF.busy }"
                >
                  Save changes
                </button>
              </form>
            </div>
            <div class="stack">
              <div class="card pad">
                <h3>Profile photo</h3>
                <alert-box
                  v-if="avatarUp.error"
                  type="error"
                  :msg="avatarUp.error"
                ></alert-box>
                <label class="dz" style="display: block"
                  ><icon name="upload" :size="26"></icon>
                  <div class="small"><b>Click to choose a new photo</b></div>
                  <div class="xs">JPG, PNG or WebP · up to 1 MB</div>
                  <input
                    type="file"
                    accept="image/jpeg,image/png,image/webp"
                    class="hide"
                    @change="pickImage($event, avatarUp)"
                /></label>
                <button
                  class="btn btn-primary mt"
                  :disabled="!avatarUp.file"
                  :class="{ 'is-loading': avatarUp.busy }"
                  @click="saveAvatar"
                >
                  Upload photo
                </button>
              </div>
              <div class="card pad">
                <h3>Change password</h3>
                <alert-box
                  v-if="pwF.banner"
                  type="error"
                  :msg="pwF.banner"
                ></alert-box>
                <form @submit.prevent="changePw" novalidate>
                  <f-input
                    v-model="pwF.values.currentPassword"
                    label="Current password"
                    type="password"
                    required
                    autocomplete="current-password"
                    :error="pwF.errors.currentPassword"
                    @blur="pwF.check('currentPassword')"
                  ></f-input>
                  <f-input
                    v-model="pwF.values.newPassword"
                    label="New password"
                    type="password"
                    required
                    autocomplete="new-password"
                    hint="At least 8 characters"
                    :error="pwF.errors.newPassword"
                    @blur="pwF.check('newPassword')"
                  ></f-input>
                  <f-input
                    v-model="pwF.values.confirmedPassword"
                    label="Confirm new password"
                    type="password"
                    required
                    autocomplete="new-password"
                    :error="pwF.errors.confirmedPassword"
                    @blur="pwF.check('confirmedPassword')"
                  ></f-input>
                  <button
                    class="btn btn-outline"
                    :class="{ 'is-loading': pwF.busy }"
                  >
                    Update password
                  </button>
                </form>
              </div>
            </div>
          </div>
        </div>
      </section>

      <!-- ================= MY BOOKINGS ================= -->
      <section v-if="route.name === 'bookings'" class="page">
        <div class="wrap" style="max-width: 900px">
          <div class="row between">
            <div>
              <h2>My bookings</h2>
              <p class="muted">Your reservations, most recent first.</p>
            </div>
            <button
              class="btn btn-outline btn-sm"
              @click="loadBookings(mb.page)"
            >
              <icon name="refresh" :size="15"></icon>Refresh
            </button>
          </div>
          <div class="tabs">
            <button
              v-for="f in [
                ['ALL', 'All'],
                ['CONFIRMED', 'Confirmed'],
                ['COMPLETED', 'Completed'],
                ['CANCELLED', 'Cancelled'],
              ]"
              :key="f[0]"
              class="tab"
              :class="{ on: mb.filter === f[0] }"
              @click="mb.filter = f[0]"
            >
              {{ f[1] }}
            </button>
          </div>
          <alert-box v-if="mb.error" type="error" :msg="mb.error"></alert-box>
          <div v-if="mb.loading">
            <div
              v-for="i in 3"
              :key="i"
              class="skel"
              style="height: 130px; margin-bottom: 14px"
            ></div>
          </div>
          <template v-else>
            <div v-if="!mbShown.length && !mb.error" class="card empty">
              <div class="big"><icon name="calendar" :size="30"></icon></div>
              <h3>
                {{
                  mb.items.length
                    ? "No " + mb.filter.toLowerCase() + " bookings on this page"
                    : "No bookings yet"
                }}
              </h3>
              <p>
                {{
                  mb.items.length
                    ? "Try another filter."
                    : "When you book a stay it will show up here."
                }}
              </p>
              <a v-if="!mb.items.length" class="btn btn-primary" href="#/"
                >Find a hotel</a
              >
            </div>
            <article
              v-for="b in mbShown"
              :key="b.id"
              class="card pad"
              style="margin-bottom: 14px"
            >
              <div class="row between" style="align-items: flex-start">
                <div style="min-width: 0">
                  <div class="row" style="gap: 10px">
                    <h3 style="margin: 0">{{ b.hotelName }}</h3>
                    <span class="badge" :class="statusClass(b.status)">{{
                      statusLabel(b.status)
                    }}</span>
                  </div>
                  <div class="small muted" style="margin-top: 4px">
                    Reference <span class="ref">{{ b.bookingReference }}</span>
                  </div>
                </div>
                <div class="right">
                  <div class="xs muted">Total</div>
                  <strong style="font-size: 1.3rem">{{
                    money(b.totalAmount)
                  }}</strong>
                </div>
              </div>
              <div class="row small" style="gap: 22px; margin: 12px 0">
                <span
                  ><icon name="calendar" :size="15"></icon>
                  {{ fmtDate(b.checkIn) }} → {{ fmtDate(b.checkOut) }} ({{
                    plural(nightsBetween(b.checkIn, b.checkOut), "night")
                  }})</span
                ><span
                  ><icon name="users" :size="15"></icon>
                  {{ plural(b.adults, "adult") }},
                  {{ plural(b.children, "child", "children") }}</span
                ><span
                  ><icon name="bed" :size="15"></icon>
                  {{ plural((b.rooms || []).length, "room") }}</span
                >
              </div>
              <div class="row">
                <button
                  class="btn btn-outline btn-sm"
                  @click="openBooking(b.id)"
                >
                  <icon name="list" :size="15"></icon>View details
                </button>
                <a class="btn btn-ghost btn-sm" :href="'#/hotel/' + b.hotelId"
                  >View hotel</a
                >
                <button
                  v-if="b.status === 'CONFIRMED' || b.status === 'PENDING'"
                  class="btn btn-danger-o btn-sm"
                  @click="cancelBooking(b, () => loadBookings(mb.page))"
                >
                  <icon name="x" :size="15"></icon>Cancel booking
                </button>
              </div>
            </article>
            <div class="pager" v-if="mb.page > 0 || mb.hasNext">
              <button
                class="btn btn-outline btn-sm"
                :disabled="mb.page === 0"
                @click="loadBookings(mb.page - 1)"
              >
                <icon name="left" :size="16"></icon>Newer</button
              ><span class="muted small">Page {{ mb.page + 1 }}</span
              ><button
                class="btn btn-outline btn-sm"
                :disabled="!mb.hasNext"
                @click="loadBookings(mb.page + 1)"
              >
                Older<icon name="right" :size="16"></icon>
              </button>
            </div>
          </template>
        </div>
      </section>

      <!-- ================= MANAGER PORTAL ================= -->
      <section v-if="route.name === 'manage'" class="page">
        <div class="wrap">
          <div class="row between">
            <div>
              <h2>Manager portal</h2>
              <p class="muted">
                The hotels you manage. New hotels are reviewed by an admin
                before they go live.
              </p>
            </div>
            <a class="btn btn-primary" href="#/manage/new"
              ><icon name="plus" :size="16"></icon>Add a hotel</a
            >
          </div>
          <alert-box v-if="mh.error" type="error" :msg="mh.error"></alert-box>
          <div v-if="mh.loading" class="grid g3">
            <div
              v-for="i in 3"
              :key="i"
              class="skel"
              style="height: 260px"
            ></div>
          </div>
          <div v-else-if="!mh.items.length && !mh.error" class="card empty">
            <div class="big"><icon name="building" :size="30"></icon></div>
            <h3>You don’t manage any hotels yet</h3>
            <p>Add your first hotel – an admin will review and approve it.</p>
            <a class="btn btn-primary" href="#/manage/new">Add a hotel</a>
          </div>
          <div v-else class="grid g3">
            <article v-for="h in mh.items" :key="h.id" class="gcard">
              <div class="img">
                <hotel-img
                  :src="h.images && h.images[0] && h.images[0].imageUrl"
                  :name="h.name"
                ></hotel-img>
              </div>
              <div class="bd">
                <div class="row between">
                  <h3 style="margin: 0">{{ h.name }}</h3>
                  <span v-if="mh.live[h.id] === true" class="badge b-green"
                    >Live</span
                  ><span
                    v-else-if="mh.live[h.id] === false"
                    class="badge b-yellow"
                    title="Pending approval, rejected or inactive"
                    >Not live</span
                  >
                </div>
                <div class="loc">
                  <icon name="pin" :size="15"></icon>{{ h.city }},
                  {{ h.country }}
                </div>
                <p
                  v-if="mh.live[h.id] === false"
                  class="xs muted"
                  style="margin: 0"
                >
                  Waiting for admin approval (or inactive). Rooms can be added
                  once it’s live.
                </p>
                <div class="row" style="margin-top: auto">
                  <a
                    class="btn btn-primary btn-sm"
                    :href="'#/manage/hotel/' + h.id"
                    >Manage</a
                  ><a class="btn btn-ghost btn-sm" :href="'#/hotel/' + h.id"
                    >Preview</a
                  >
                </div>
              </div>
            </article>
          </div>
          <div class="pager" v-if="mh.page > 0 || mh.hasNext">
            <button
              class="btn btn-outline btn-sm"
              :disabled="mh.page === 0"
              @click="loadManagerHotels(mh.page - 1)"
            >
              <icon name="left" :size="16"></icon>Previous</button
            ><span class="muted small">Page {{ mh.page + 1 }}</span
            ><button
              class="btn btn-outline btn-sm"
              :disabled="!mh.hasNext"
              @click="loadManagerHotels(mh.page + 1)"
            >
              Next<icon name="right" :size="16"></icon>
            </button>
          </div>
        </div>
      </section>

      <!-- ================= HOTEL FORM (new) ================= -->
      <section v-if="route.name === 'manage-new'" class="page">
        <div class="wrap" style="max-width: 780px">
          <div class="crumbs">
            <a :href="isAdmin ? '#/admin?tab=hotels' : '#/manage'">← Back</a>
          </div>
          <div class="card pad">
            <h2>Add a hotel</h2>
            <alert-box v-if="isManager" type="info"
              >New hotels are submitted for admin approval. You’ll be able to
              add rooms once it’s approved.</alert-box
            >
            <alert-box v-else type="info"
              >As an admin you must choose the manager (their <b>user ID</b>)
              who will run this hotel. The hotel goes live
              immediately.</alert-box
            >
            <alert-box
              v-if="hotelF.banner"
              type="error"
              :msg="hotelF.banner"
            ></alert-box>
            <form @submit.prevent="saveHotel" novalidate>
              <f-input
                v-model="hotelF.values.name"
                label="Hotel name"
                required
                :error="hotelF.errors.name"
                @blur="hotelF.check('name')"
              ></f-input>
              <f-input
                v-model="hotelF.values.description"
                textarea
                :rows="4"
                label="Description"
                required
                :maxlength="500"
                counter
                hint="10–500 characters"
                :error="hotelF.errors.description"
                @blur="hotelF.check('description')"
              ></f-input>
              <f-input
                v-model="hotelF.values.address"
                label="Street address"
                required
                :error="hotelF.errors.address"
                @blur="hotelF.check('address')"
              ></f-input>
              <div class="form-grid">
                <f-input
                  v-model="hotelF.values.city"
                  label="City"
                  required
                  list="city-list"
                  :error="hotelF.errors.city"
                  @blur="hotelF.check('city')"
                ></f-input
                ><f-input
                  v-model="hotelF.values.country"
                  label="Country"
                  required
                  :error="hotelF.errors.country"
                  @blur="hotelF.check('country')"
                ></f-input>
              </div>
              <f-input
                v-model="hotelF.values.phone"
                label="Phone"
                type="tel"
                required
                hint="8–12 characters"
                :error="hotelF.errors.phone"
                @blur="hotelF.check('phone')"
              ></f-input>
              <div class="form-grid">
                <f-input
                  v-model="hotelF.values.latitude"
                  label="Latitude (optional)"
                  type="number"
                  step="any"
                  placeholder="26.2285"
                  :error="hotelF.errors.latitude"
                  @blur="hotelF.check('latitude')"
                ></f-input
                ><f-input
                  v-model="hotelF.values.longitude"
                  label="Longitude (optional)"
                  type="number"
                  step="any"
                  placeholder="50.5860"
                  :error="hotelF.errors.longitude"
                  @blur="hotelF.check('longitude')"
                ></f-input>
              </div>
              <button
                type="button"
                class="btn btn-ghost btn-sm"
                style="margin: -6px 0 14px"
                @click="useMyLocation"
              >
                <icon name="pin" :size="15"></icon>Use my current location
              </button>
              <f-input
                v-if="isAdmin"
                v-model="hotelF.values.managerId"
                label="Manager user ID"
                type="number"
                min="1"
                required
                hint="The numeric ID of a user with the Hotel manager role"
                :error="hotelF.errors.managerId"
                @blur="hotelF.check('managerId')"
              ></f-input>
              <div class="row">
                <button
                  class="btn btn-primary btn-lg"
                  :class="{ 'is-loading': hotelF.busy }"
                >
                  {{ isAdmin ? "Create hotel" : "Submit for approval" }}</button
                ><a
                  class="btn btn-ghost"
                  :href="isAdmin ? '#/admin?tab=hotels' : '#/manage'"
                  >Cancel</a
                >
              </div>
            </form>
          </div>
        </div>
      </section>

      <!-- ================= MANAGE ONE HOTEL ================= -->
      <section v-if="route.name === 'manage-hotel'" class="page">
        <div class="wrap">
          <div v-if="page.loading && !mp.hotel">
            <div class="skel" style="height: 60px; margin-bottom: 14px"></div>
            <div class="skel" style="height: 300px"></div>
          </div>
          <div v-else-if="page.error" class="card empty">
            <div class="big"><icon name="alert" :size="30"></icon></div>
            <h3>Couldn’t load this hotel</h3>
            <p>{{ page.error }}</p>
            <a
              class="btn btn-primary"
              :href="isAdmin ? '#/admin?tab=hotels' : '#/manage'"
              >Back</a
            >
          </div>
          <template v-else-if="mp.hotel">
            <div class="crumbs">
              <a :href="isAdmin ? '#/admin?tab=hotels' : '#/manage'"
                >← {{ isAdmin ? "Admin panel" : "My hotels" }}</a
              >
            </div>
            <div class="row between">
              <div>
                <h2 style="margin-bottom: 2px">{{ mp.hotel.name }}</h2>
                <div class="muted small">
                  <icon name="pin" :size="14"></icon> {{ mp.hotel.city }},
                  {{ mp.hotel.country }} · ID {{ mp.hotel.id }}
                  <span
                    v-if="mp.live === true"
                    class="badge b-green"
                    style="margin-left: 6px"
                    >Live</span
                  ><span
                    v-else-if="mp.live === false"
                    class="badge b-yellow"
                    style="margin-left: 6px"
                    >Not live yet</span
                  >
                </div>
              </div>
              <a
                class="btn btn-outline btn-sm"
                :href="'#/hotel/' + mp.hotel.id"
                target="_blank"
                ><icon name="ext" :size="14"></icon>View public page</a
              >
            </div>
            <div class="tabs mt">
              <button
                class="tab"
                :class="{ on: mp.tab === 'overview' }"
                @click="manageTab('overview')"
              >
                <icon name="edit" :size="16"></icon>Details
              </button>
              <button
                class="tab"
                :class="{ on: mp.tab === 'rooms' }"
                @click="manageTab('rooms')"
              >
                <icon name="bed" :size="16"></icon>Rooms
              </button>
              <button
                class="tab"
                :class="{ on: mp.tab === 'amenities' }"
                @click="manageTab('amenities')"
              >
                <icon name="check" :size="16"></icon>Amenities
              </button>
              <button
                class="tab"
                :class="{ on: mp.tab === 'photos' }"
                @click="manageTab('photos')"
              >
                <icon name="image" :size="16"></icon>Photos
              </button>
              <button
                class="tab"
                :class="{ on: mp.tab === 'policy' }"
                @click="manageTab('policy')"
              >
                <icon name="baby" :size="16"></icon>Child policy
              </button>
              <button
                class="tab"
                :class="{ on: mp.tab === 'bookings' }"
                @click="manageTab('bookings')"
              >
                <icon name="calendar" :size="16"></icon>Bookings
              </button>
            </div>

            <!-- details -->
            <div
              v-if="mp.tab === 'overview'"
              class="card pad"
              style="max-width: 780px"
            >
              <alert-box v-if="mp.live === false" type="warn"
                >This hotel isn’t live yet. It needs admin approval (or has been
                deactivated), so guests can’t find or book it.</alert-box
              >
              <alert-box
                v-if="hotelF.banner"
                type="error"
                :msg="hotelF.banner"
              ></alert-box>
              <form @submit.prevent="saveHotel" novalidate>
                <f-input
                  v-model="hotelF.values.name"
                  label="Hotel name"
                  required
                  :error="hotelF.errors.name"
                  @blur="hotelF.check('name')"
                ></f-input>
                <f-input
                  v-model="hotelF.values.description"
                  textarea
                  :rows="4"
                  label="Description"
                  required
                  :maxlength="500"
                  counter
                  :error="hotelF.errors.description"
                  @blur="hotelF.check('description')"
                ></f-input>
                <f-input
                  v-model="hotelF.values.address"
                  label="Street address"
                  required
                  :error="hotelF.errors.address"
                  @blur="hotelF.check('address')"
                ></f-input>
                <div class="form-grid">
                  <f-input
                    v-model="hotelF.values.city"
                    label="City"
                    required
                    :error="hotelF.errors.city"
                    @blur="hotelF.check('city')"
                  ></f-input
                  ><f-input
                    v-model="hotelF.values.country"
                    label="Country"
                    required
                    :error="hotelF.errors.country"
                    @blur="hotelF.check('country')"
                  ></f-input>
                </div>
                <f-input
                  v-model="hotelF.values.phone"
                  label="Phone"
                  type="tel"
                  required
                  hint="8–12 characters"
                  :error="hotelF.errors.phone"
                  @blur="hotelF.check('phone')"
                ></f-input>
                <div class="form-grid">
                  <f-input
                    v-model="hotelF.values.latitude"
                    label="Latitude"
                    type="number"
                    step="any"
                    :error="hotelF.errors.latitude"
                    @blur="hotelF.check('latitude')"
                  ></f-input
                  ><f-input
                    v-model="hotelF.values.longitude"
                    label="Longitude"
                    type="number"
                    step="any"
                    :error="hotelF.errors.longitude"
                    @blur="hotelF.check('longitude')"
                  ></f-input>
                </div>
                <f-input
                  v-if="isAdmin"
                  v-model="hotelF.values.managerId"
                  label="Add another manager (user ID, optional)"
                  type="number"
                  min="1"
                  :error="hotelF.errors.managerId"
                ></f-input>
                <button
                  class="btn btn-primary"
                  :class="{ 'is-loading': hotelF.busy }"
                >
                  Save changes
                </button>
              </form>
            </div>

            <!-- rooms -->
            <div v-if="mp.tab === 'rooms'">
              <div class="row between mb">
                <div>
                  <h3 style="margin: 0">Rooms</h3>
                  <div class="small muted">
                    Rooms guests can book at this hotel.
                  </div>
                </div>
                <div class="row" style="gap: 8px">
                  <button
                    class="btn btn-outline"
                    :disabled="mp.live === false"
                    @click="openBulk"
                  >
                    Add several</button
                  ><button
                    class="btn btn-primary"
                    :disabled="mp.live === false"
                    @click="openRoomModal()"
                  >
                    <icon name="plus" :size="16"></icon>Add room
                  </button>
                </div>
              </div>
              <alert-box
                v-if="mp.roomsMsg"
                type="warn"
                :msg="mp.roomsMsg"
              ></alert-box>
              <div v-else-if="!mp.rooms.length" class="card empty">
                <div class="big"><icon name="bed" :size="30"></icon></div>
                <h3>No rooms yet</h3>
                <p>Add your first room so guests can start booking.</p>
                <button class="btn btn-primary" @click="openRoomModal()">
                  Add a room
                </button>
              </div>
              <div v-else class="card scroll-x">
                <table class="t plain">
                  <thead>
                    <tr>
                      <th>Room</th>
                      <th>Floor</th>
                      <th>Type</th>
                      <th>Adults</th>
                      <th>Children</th>
                      <th>Max guests</th>
                      <th>Price / night</th>
                      <th>Status</th>
                      <th>Actions</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="r in mp.rooms" :key="r.id">
                      <td>
                        <b>{{ r.roomNumber }}</b>
                      </td>
                      <td>{{ r.floorNumber }}</td>
                      <td>{{ r.roomTypeName }}</td>
                      <td>{{ r.maxAdults }}</td>
                      <td>{{ r.maxChildren }}</td>
                      <td>{{ r.maxOccupancy }}</td>
                      <td>{{ money(r.pricePerNight) }}</td>
                      <td>
                        <select
                          :value="r.status"
                          @change="setRoomStatus(r, $event.target.value)"
                          :aria-label="'Status of room ' + r.roomNumber"
                          style="
                            padding: 6px 8px;
                            border: 1px solid #b5bcc5;
                            border-radius: 8px;
                          "
                        >
                          <option
                            v-for="(label, key) in ROOM_STATUS"
                            :key="key"
                            :value="key"
                          >
                            {{ label }}
                          </option>
                        </select>
                      </td>
                      <td>
                        <div class="row" style="gap: 6px; flex-wrap: nowrap">
                          <button
                            class="btn btn-outline btn-sm"
                            @click="openRoomModal(r)"
                          >
                            <icon name="edit" :size="14"></icon>Edit</button
                          ><button
                            class="btn btn-outline btn-sm"
                            @click="openRoomImages(r)"
                          >
                            <icon name="image" :size="14"></icon>Photos
                          </button>
                        </div>
                      </td>
                    </tr>
                  </tbody>
                </table>
              </div>
              <div class="pager" v-if="mp.roomsPage > 0 || mp.roomsNext">
                <button
                  class="btn btn-outline btn-sm"
                  :disabled="mp.roomsPage === 0"
                  @click="loadMRooms(mp.roomsPage - 1)"
                >
                  Previous</button
                ><span class="muted small">Page {{ mp.roomsPage + 1 }}</span
                ><button
                  class="btn btn-outline btn-sm"
                  :disabled="!mp.roomsNext"
                  @click="loadMRooms(mp.roomsPage + 1)"
                >
                  Next
                </button>
              </div>
            </div>

            <!-- amenities -->
            <div v-if="mp.tab === 'amenities'">
              <div class="row between mb">
                <div>
                  <h3 style="margin: 0">Amenities</h3>
                  <div class="small muted">
                    What guests get at this hotel. Some amenities can have an
                    extra cost.
                  </div>
                </div>
                <button class="btn btn-primary" @click="openAmenity(null)">
                  <icon name="plus" :size="16"></icon>Add amenity
                </button>
              </div>
              <div
                v-if="!mp.hotel.amenities || !mp.hotel.amenities.length"
                class="card empty"
              >
                <div class="big"><icon name="check" :size="30"></icon></div>
                <h3>No amenities yet</h3>
                <p>
                  Add Wi-Fi, a pool, breakfast … so guests know what to expect.
                </p>
                <button class="btn btn-primary" @click="openAmenity(null)">
                  Add an amenity
                </button>
              </div>
              <div v-else class="card scroll-x">
                <table class="t plain">
                  <thead>
                    <tr>
                      <th>Amenity</th>
                      <th>Description</th>
                      <th>Extra cost</th>
                      <th>Actions</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="a in mp.hotel.amenities" :key="a.id">
                      <td>
                        <b>{{ amenityIcon(a.name) }} {{ a.name }}</b>
                      </td>
                      <td class="small">{{ a.description || "–" }}</td>
                      <td>
                        {{
                          a.additionalCost ? money(a.additionalCost) : "Free"
                        }}
                      </td>
                      <td>
                        <div class="row" style="gap: 6px; flex-wrap: nowrap">
                          <button
                            class="btn btn-outline btn-sm"
                            @click="openAmenity(a)"
                          >
                            <icon name="edit" :size="14"></icon>Edit</button
                          ><button
                            class="btn btn-danger-o btn-sm"
                            @click="removeAmenity(a)"
                          >
                            Remove
                          </button>
                        </div>
                      </td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </div>

            <!-- photos -->
            <div v-if="mp.tab === 'photos'" class="stack">
              <div class="card pad">
                <h3>Upload photos</h3>
                <p class="small muted">
                  Add clear, bright photos. Each needs a short description
                  (shown to screen-reader users). Up to 1 MB per photo, 10
                  photos at a time.
                </p>
                <alert-box
                  v-if="imgUp.err"
                  type="error"
                  :msg="imgUp.err"
                ></alert-box>
                <label class="dz" style="display: block"
                  ><icon name="image" :size="28"></icon>
                  <div><b>Click to choose photos</b></div>
                  <div class="xs">You can select several at once</div>
                  <input
                    type="file"
                    accept="image/jpeg,image/png,image/webp"
                    multiple
                    class="hide"
                    @change="pickHotelImages"
                /></label>
                <div
                  v-for="(it, i) in imgUp.items"
                  :key="i"
                  class="row mt"
                  style="align-items: flex-start; flex-wrap: nowrap"
                >
                  <img class="thumb" :src="it.preview" alt="" />
                  <div style="flex: 1">
                    <f-input
                      v-model="it.alt"
                      label="Describe this photo"
                      placeholder="e.g. Sea-view deluxe room"
                      :error="it.err"
                      hint="Commas are removed automatically"
                    ></f-input>
                  </div>
                  <button
                    class="btn btn-ghost btn-sm"
                    style="margin-top: 26px"
                    @click="dropImage(i)"
                    aria-label="Remove"
                  >
                    <icon name="trash" :size="16"></icon>
                  </button>
                </div>
                <button
                  v-if="imgUp.items.length"
                  class="btn btn-primary mt"
                  :class="{ 'is-loading': imgUp.busy }"
                  @click="uploadHotelImages"
                >
                  <icon name="upload" :size="16"></icon>Upload
                  {{ plural(imgUp.items.length, "photo") }}
                </button>
              </div>
              <div class="card pad">
                <h3>Current photos</h3>
                <alert-box
                  v-if="mp.images.length && mp.imgFails >= mp.images.length"
                  type="warn"
                  ><b>Photos saved, but the server isn’t serving them.</b>
                  Spring Boot needs a resource handler for the
                  <code>/uploads/**</code> folder – see the backend
                  notes.</alert-box
                >
                <div v-if="!mp.images.length" class="muted">No photos yet.</div>
                <div class="grid g4">
                  <div v-for="img in mp.images" :key="img.id">
                    <div class="pimg">
                      <hotel-img
                        :src="img.imageUrl"
                        :name="mp.hotel.name"
                        :alt="img.altText"
                        @fail="mp.imgFails++"
                      ></hotel-img
                      ><button
                        class="btn btn-danger btn-sm del"
                        @click="deleteHotelImage(img)"
                        aria-label="Delete photo"
                      >
                        <icon name="trash" :size="14"></icon>
                      </button>
                    </div>
                    <div class="xs muted" style="margin-top: 4px">
                      {{ img.altText }}
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <!-- child policy -->
            <div
              v-if="mp.tab === 'policy'"
              class="card pad"
              style="max-width: 640px"
            >
              <h3>Child policy</h3>
              <p class="small muted">
                We use guests’ ages to tell infants, children and adults apart
                and to check room capacity.
                <b>Bookings can’t be made until a policy exists.</b>
              </p>
              <alert-box v-if="mp.policyMissing" type="warn"
                >This hotel has no child policy yet – guests can’t book until
                you create one.</alert-box
              >
              <alert-box
                v-if="policyF.banner"
                type="error"
                :msg="policyF.banner"
              ></alert-box>
              <form @submit.prevent="savePolicy" novalidate>
                <div class="form-grid">
                  <f-input
                    v-model="policyF.values.infantMaxAge"
                    label="Infant: up to age"
                    type="number"
                    min="0"
                    max="5"
                    required
                    hint="0–5 years"
                    :error="policyF.errors.infantMaxAge"
                    @blur="policyF.check('infantMaxAge')"
                  ></f-input>
                  <f-input
                    v-model="policyF.values.childMaxAge"
                    label="Child: up to age"
                    type="number"
                    min="1"
                    max="17"
                    required
                    hint="1–17 years; must be above infant age"
                    :error="policyF.errors.childMaxAge"
                    @blur="policyF.check('childMaxAge')"
                  ></f-input>
                </div>
                <div
                  class="alert info"
                  v-if="
                    policyF.values.infantMaxAge !== '' &&
                    policyF.values.childMaxAge !== ''
                  "
                >
                  <icon name="info"></icon>
                  <div>
                    Infants: 0–{{ policyF.values.infantMaxAge }} · Children:
                    {{ Number(policyF.values.infantMaxAge) + 1 }}–{{
                      policyF.values.childMaxAge
                    }}
                    · Adults: {{ Number(policyF.values.childMaxAge) + 1 }}+
                  </div>
                </div>
                <label class="check mb"
                  ><input
                    type="checkbox"
                    v-model="policyF.values.infantsCountTowardOccupancy"
                  />Infants count towards room occupancy</label
                >
                <button
                  class="btn btn-primary"
                  :class="{ 'is-loading': policyF.busy }"
                >
                  {{ mp.policyMissing ? "Create policy" : "Save policy" }}
                </button>
              </form>
            </div>

            <!-- bookings -->
            <div v-if="mp.tab === 'bookings'" class="card pad">
              <lookup-panel
                :lk="lk"
                @find="lookupBooking"
                @complete="completeBooking"
                @cancel="cancelFromLookup"
              ></lookup-panel>
            </div>
          </template>
        </div>
      </section>

      <!-- ================= ADMIN ================= -->
      <section v-if="route.name === 'admin'" class="page">
        <div class="wrap">
          <h2>Admin panel</h2>
          <div class="tabs">
            <button
              class="tab"
              :class="{ on: ad.tab === 'overview' }"
              @click="adminTab('overview')"
            >
              <icon name="home" :size="16"></icon>Overview
            </button>
            <button
              class="tab"
              :class="{ on: ad.tab === 'pending' }"
              @click="adminTab('pending')"
            >
              <icon name="clock" :size="16"></icon>Pending approvals
            </button>
            <button
              class="tab"
              :class="{ on: ad.tab === 'hotels' }"
              @click="adminTab('hotels')"
            >
              <icon name="building" :size="16"></icon>Hotels
            </button>
            <button
              class="tab"
              :class="{ on: ad.tab === 'users' }"
              @click="adminTab('users')"
            >
              <icon name="users" :size="16"></icon>Users
            </button>
            <button
              class="tab"
              :class="{ on: ad.tab === 'types' }"
              @click="adminTab('types')"
            >
              <icon name="bed" :size="16"></icon>Room types
            </button>
            <button
              class="tab"
              :class="{ on: ad.tab === 'amenities' }"
              @click="adminTab('amenities')"
            >
              <icon name="check" :size="16"></icon>Amenities
            </button>
            <button
              class="tab"
              :class="{ on: ad.tab === 'status' }"
              @click="adminTab('status')"
            >
              <icon name="settings" :size="16"></icon>Hotel status
            </button>
            <button
              class="tab"
              :class="{ on: ad.tab === 'bookings' }"
              @click="adminTab('bookings')"
            >
              <icon name="calendar" :size="16"></icon>Bookings
            </button>
          </div>
          <alert-box v-if="ad.error" type="error" :msg="ad.error"></alert-box>

          <div v-if="ad.tab === 'overview'">
            <div class="row mb2 mt2">
              <a class="btn btn-primary" href="#/manage/new"
                ><icon name="plus" :size="16"></icon>Create a hotel</a
              ><a class="btn btn-outline" href="#/admin?tab=bookings"
                >Find a booking</a
              >
            </div>

            <div class="grid g4">
                <!--- if pending requests is more than 0 card should be in yellow color --->
              <div class="card stat" :class="{ 'needs-review': ad.counts.pending > 0 }">
                <b>{{ ad.counts.pending ?? "–" }}</b
                ><span>Hotels awaiting approval</span>
                  
                <div class="mt">
                  <a class="btn btn-outline btn-sm" href="#/admin?tab=pending"
                    >Review</a
                  >
                </div>
              </div>
              <div class="card stat">
                <b>{{ ad.counts.hotels ?? "–" }}</b
                ><span>Live hotels</span>
                <div class="mt">
                  <a class="btn btn-outline btn-sm" href="#/admin?tab=hotels"
                    >View hotels</a
                  >
                </div>
              </div>
              <div class="card stat">
                <b>{{ ad.counts.users ?? "–" }}</b
                ><span>Registered users</span>
                <div class="mt">
                  <a class="btn btn-outline btn-sm" href="#/admin?tab=users"
                    >View users</a
                  >
                </div>
              </div>
              <div class="card stat">
                <b>{{ ad.counts.types ?? "–" }}</b
                ><span>Active room types</span>
                <div class="mt">
                  <a class="btn btn-outline btn-sm" href="#/admin?tab=types"
                    >Edit room types</a
                  >
                </div>
              </div>
            </div>
          </div>

          <div v-if="ad.tab === 'pending'">
            <div v-if="ad.loading" class="skel" style="height: 120px"></div>
            <div v-else-if="!ad.pending.length" class="card empty">
              <div class="big"><icon name="check" :size="30"></icon></div>
              <h3>All caught up</h3>
              <p>No hotels are waiting for approval.</p>
            </div>
            <article
              v-for="h in ad.pending"
              :key="h.id"
              class="card pad"
              style="margin-bottom: 12px"
            >
              <div class="row between" style="align-items: flex-start">
                <div>
                  <h3 style="margin-bottom: 2px">
                    {{ h.name }} <span class="badge b-yellow">Pending</span>
                  </h3>
                  <div class="small muted">
                    {{ h.address }}, {{ h.city }}, {{ h.country }} ·
                    {{ h.phone }} · ID {{ h.id }}
                  </div>
                  <p class="small" style="margin: 8px 0 0; max-width: 680px">
                    {{ h.description }}
                  </p>
                </div>
                <div class="row">
                  <button
                    class="btn btn-green btn-sm"
                    @click="
                      setHotelStatus(h, 'ACTIVE', () => loadPending(ad.pPage))
                    "
                  >
                    <icon name="check" :size="15"></icon>Approve</button
                  ><button
                    class="btn btn-danger-o btn-sm"
                    @click="
                      setHotelStatus(h, 'REJECTED', () => loadPending(ad.pPage))
                    "
                  >
                    <icon name="x" :size="15"></icon>Reject
                  </button>
                </div>
              </div>
            </article>
            <div class="pager" v-if="ad.pPage > 0 || ad.pNext">
              <button
                class="btn btn-outline btn-sm"
                :disabled="ad.pPage === 0"
                @click="loadPending(ad.pPage - 1)"
              >
                Previous</button
              ><span class="muted small">Page {{ ad.pPage + 1 }}</span
              ><button
                class="btn btn-outline btn-sm"
                :disabled="!ad.pNext"
                @click="loadPending(ad.pPage + 1)"
              >
                Next
              </button>
            </div>
          </div>

          <div v-if="ad.tab === 'hotels'">
            <div class="row between mb">
              <form class="row" @submit.prevent="loadAdminHotels(0)">
                <input
                  v-model="ad.q.search"
                  placeholder="Search by name"
                  style="
                    padding: 9px 12px;
                    border: 1px solid #b5bcc5;
                    border-radius: 8px;
                  "
                /><input
                  v-model="ad.q.city"
                  placeholder="City"
                  list="city-list"
                  style="
                    padding: 9px 12px;
                    border: 1px solid #b5bcc5;
                    border-radius: 8px;
                    width: 150px;
                  "
                /><button class="btn btn-primary btn-sm">Filter</button>
              </form>
              <a class="btn btn-primary btn-sm" href="#/manage/new"
                ><icon name="plus" :size="15"></icon>Create hotel</a
              >
            </div>
            <alert-box type="info"
              >This list shows <b>live</b> hotels only (that’s all the API
              returns). To bring back a deactivated hotel use the
              <a href="#/admin?tab=status">Hotel status</a> tab.</alert-box
            >
            <div v-if="ad.loading" class="skel" style="height: 140px"></div>
            <div v-else-if="!ad.hotels.length" class="card empty">
              <p>No live hotels match.</p>
            </div>
            <div v-else class="card scroll-x">
              <table class="t plain">
                <thead>
                  <tr>
                    <th>ID</th>
                    <th>Hotel</th>
                    <th>City</th>
                    <th>Phone</th>
                    <th style="min-width: 300px">Actions</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="h in ad.hotels" :key="h.id">
                    <td>{{ h.id }}</td>
                    <td>
                      <b>{{ h.name }}</b>
                    </td>
                    <td>{{ h.city }}, {{ h.country }}</td>
                    <td>{{ h.phone }}</td>
                    <td>
                      <div class="row" style="gap: 6px">
                        <a
                          class="btn btn-outline btn-sm"
                          :href="'#/manage/hotel/' + h.id"
                          >Open</a
                        ><button
                          class="btn btn-outline btn-sm"
                          @click="openAssign(h)"
                        >
                          Assign manager</button
                        ><button
                          class="btn btn-danger-o btn-sm"
                          @click="
                            setHotelStatus(h, 'INACTIVE', () =>
                              loadAdminHotels(ad.hPage),
                            )
                          "
                        >
                          Deactivate
                        </button>
                      </div>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
            <div class="pager" v-if="ad.hPage > 0 || ad.hNext">
              <button
                class="btn btn-outline btn-sm"
                :disabled="ad.hPage === 0"
                @click="loadAdminHotels(ad.hPage - 1)"
              >
                Previous</button
              ><span class="muted small">Page {{ ad.hPage + 1 }}</span
              ><button
                class="btn btn-outline btn-sm"
                :disabled="!ad.hNext"
                @click="loadAdminHotels(ad.hPage + 1)"
              >
                Next
              </button>
            </div>
          </div>

          <div v-if="ad.tab === 'types'">
            <div class="row between mb">
              <div>
                <h3 style="margin: 0">Room types</h3>
                <div class="small muted">
                  Managers pick from these when adding rooms.
                </div>
              </div>
              <button class="btn btn-primary btn-sm" @click="openType(null)">
                <icon name="plus" :size="15"></icon>New room type
              </button>
            </div>
            <div v-if="!roomTypes.length" class="card empty">
              <div class="big"><icon name="bed" :size="30"></icon></div>
              <h3>No room types yet</h3>
              <p>Create types like “Deluxe King” or “Family Suite”.</p>
            </div>
            <div v-else class="card scroll-x">
              <table class="t plain">
                <thead>
                  <tr>
                    <th>ID</th>
                    <th>Name</th>
                    <th>Description</th>
                    <th>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="t in roomTypes" :key="t.id">
                    <td>{{ t.id }}</td>
                    <td>
                      <b>{{ t.name }}</b>
                    </td>
                    <td class="small">{{ t.description }}</td>
                    <td>
                      <div class="row" style="gap: 6px; flex-wrap: nowrap">
                        <button
                          class="btn btn-outline btn-sm"
                          @click="openType(t)"
                        >
                          <icon name="edit" :size="14"></icon>Edit</button
                        ><button
                          class="btn btn-danger-o btn-sm"
                          @click="setTypeStatus(t, 'INACTIVE')"
                        >
                          Deactivate
                        </button>
                      </div>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
            <div class="card pad mt">
              <h4>Re-activate a room type</h4>
              <p class="small muted">
                Deactivated types disappear from the list above. Enter the
                type’s ID to bring it back.
              </p>
              <form
                class="row"
                style="align-items: flex-start"
                @submit.prevent="activateTypeById"
              >
                <div style="width: 200px">
                  <f-input
                    v-model="typeActivate.id"
                    type="number"
                    min="1"
                    placeholder="Room type ID"
                    :error="typeActivate.err"
                  ></f-input>
                </div>
                <button class="btn btn-outline">Activate</button>
              </form>
            </div>
          </div>

          <div v-if="ad.tab === 'amenities'">
            <div class="row between mb">
              <div>
                <h3 style="margin: 0">Amenities</h3>
                <div class="small muted">
                  The list hotels choose from. An amenity that a hotel uses
                  can’t be deleted.
                </div>
              </div>
              <button class="btn btn-primary btn-sm" @click="openCatalog(null)">
                <icon name="plus" :size="15"></icon>New amenity
              </button>
            </div>
            <div v-if="!amenityCatalog.length" class="card empty">
              <div class="big"><icon name="check" :size="30"></icon></div>
              <h3>No amenities yet</h3>
              <p>Create amenities like “Wi-Fi” or “Swimming pool”.</p>
            </div>
            <div v-else class="card scroll-x">
              <table class="t plain">
                <thead>
                  <tr>
                    <th>ID</th>
                    <th>Name</th>
                    <th>Description</th>
                    <th>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="a in amenityCatalog" :key="a.id">
                    <td>{{ a.id }}</td>
                    <td>
                      <b>{{ amenityIcon(a.name) }} {{ a.name }}</b>
                    </td>
                    <td class="small">{{ a.description || "–" }}</td>
                    <td>
                      <div class="row" style="gap: 6px; flex-wrap: nowrap">
                        <button
                          class="btn btn-outline btn-sm"
                          @click="openCatalog(a)"
                        >
                          <icon name="edit" :size="14"></icon>Edit</button
                        ><button
                          class="btn btn-danger-o btn-sm"
                          @click="deleteCatalog(a)"
                        >
                          Delete
                        </button>
                      </div>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>

          <div v-if="ad.tab === 'users'">
            <div class="row between mb">
              <div>
                <h3 style="margin: 0">Users</h3>
                <div class="small muted">
                  Search, filter and activate or deactivate accounts. Users are
                  never deleted – deactivating keeps their data.
                </div>
              </div>
            </div>
            <form
              class="row mb"
              @submit.prevent="loadAdminUsers(0)"
              style="flex-wrap: wrap"
            >
              <input
                v-model="ad.uq.search"
                placeholder="Search username or email"
                aria-label="Search users"
                style="
                  padding: 9px 12px;
                  border: 1px solid #b5bcc5;
                  border-radius: 8px;
                  min-width: 230px;
                "
              />
              <select
                v-model="ad.uq.status"
                aria-label="Status"
                style="
                  padding: 9px 12px;
                  border: 1px solid #b5bcc5;
                  border-radius: 8px;
                "
              >
                <option value="">Any status</option>
                <option value="ACTIVE">Active</option>
                <option value="INACTIVE">Inactive</option>
                <option value="UNVERIFIED">Unverified</option>
              </select>
              <select
                v-model="ad.uq.role"
                aria-label="Role"
                style="
                  padding: 9px 12px;
                  border: 1px solid #b5bcc5;
                  border-radius: 8px;
                "
              >
                <option value="">Any role</option>
                <option value="CUSTOMER">Guest</option>
                <option value="HOTEL_MANAGER">Hotel manager</option>
                <option value="ADMIN">Administrator</option>
                <option value="STAFF">Staff</option>
              </select>
              <select
                v-model="ad.uq.sort"
                @change="loadAdminUsers(0)"
                aria-label="Sort"
                style="
                  padding: 9px 12px;
                  border: 1px solid #b5bcc5;
                  border-radius: 8px;
                "
              >
                <option value="createdAt,desc">Newest first</option>
                <option value="createdAt,asc">Oldest first</option>
                <option value="username,asc">Username A–Z</option>
                <option value="username,desc">Username Z–A</option>
              </select>
              <button class="btn btn-primary btn-sm">Search</button
              ><button
                type="button"
                class="btn btn-ghost btn-sm"
                @click="resetUserFilters"
              >
                Clear
              </button>
            </form>
            <div v-if="ad.loading" class="skel" style="height: 160px"></div>
            <div v-else-if="!ad.users.length && !ad.error" class="card empty">
              <div class="big"><icon name="users" :size="30"></icon></div>
              <h3>No users found</h3>
              <p>Try a different search or clear the filters.</p>
            </div>
            <div v-else-if="ad.users.length" class="card scroll-x">
              <table class="t plain">
                <thead>
                  <tr>
                    <th>ID</th>
                    <th>User</th>
                    <th>Role</th>
                    <th>Status</th>
                    <th>Joined</th>
                    <th style="min-width: 150px">Actions</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="u in ad.users" :key="u.id">
                    <td>{{ u.id }}</td>
                    <td>
                      <b>{{ u.username }}</b>
                      <div class="xs muted">
                        {{ [u.firstName, u.lastName].filter(Boolean).join(" ")
                        }}<template v-if="u.firstName || u.lastName">
                          · </template
                        >{{ u.email }}
                      </div>
                    </td>
                    <td>{{ ROLE_LABEL[u.role] || u.role }}</td>
                    <td>
                      <span
                        class="badge"
                        :class="
                          u.status === 'ACTIVE'
                            ? 'b-green'
                            : u.status === 'INACTIVE'
                              ? 'b-red'
                              : 'b-yellow'
                        "
                        >{{
                          u.status === "ACTIVE"
                            ? "Active"
                            : u.status === "INACTIVE"
                              ? "Inactive"
                              : "Unverified"
                        }}</span
                      >
                    </td>
                    <td class="small">{{ fmtDateTime(u.createdAt) }}</td>
                    <td>
                      <span v-if="isSelf(u)" class="xs muted">This is you</span>
                      <span
                        v-else-if="u.status === 'UNVERIFIED'"
                        class="xs muted"
                        >Waiting for email verification</span
                      >
                      <button
                        v-else-if="u.status === 'ACTIVE'"
                        class="btn btn-danger-o btn-sm"
                        :disabled="ad.uBusy === u.id"
                        @click="setUserStatus(u, 'INACTIVE')"
                      >
                        Deactivate
                      </button>
                      <button
                        v-else
                        class="btn btn-green btn-sm"
                        :disabled="ad.uBusy === u.id"
                        @click="setUserStatus(u, 'ACTIVE')"
                      >
                        Reactivate
                      </button>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
            <div class="pager" v-if="ad.uPages > 1">
              <button
                class="btn btn-outline btn-sm"
                :disabled="ad.uPage === 0"
                @click="loadAdminUsers(ad.uPage - 1)"
              >
                <icon name="left" :size="16"></icon>Previous</button
              ><span class="muted small"
                >Page {{ ad.uPage + 1 }} of {{ ad.uPages }} ·
                {{ ad.uTotal }} users</span
              ><button
                class="btn btn-outline btn-sm"
                :disabled="ad.uPage >= ad.uPages - 1"
                @click="loadAdminUsers(ad.uPage + 1)"
              >
                Next
              </button>
            </div>
            <div v-else-if="ad.users.length" class="muted small mt">
              {{ ad.uTotal }} {{ ad.uTotal === 1 ? "user" : "users" }}
            </div>
          </div>

          <div
            v-if="ad.tab === 'status'"
            class="card pad"
            style="max-width: 640px"
          >
            <h3>Change a hotel’s status</h3>
            <p class="small muted">
              Allowed changes: <b>Pending → Active / Rejected</b>,
              <b>Active → Inactive</b>, <b>Inactive → Active</b>. The server
              rejects anything else and tells you the current status.
            </p>
            <alert-box
              v-if="statusF.banner"
              type="error"
              :msg="statusF.banner"
            ></alert-box>
            <form @submit.prevent="runStatusTool" novalidate>
              <f-input
                v-model="statusF.values.hotelId"
                label="Hotel ID"
                type="number"
                min="1"
                required
                :error="statusF.errors.hotelId"
                @blur="statusF.check('hotelId')"
              ></f-input>
              <f-select
                v-model="statusF.values.status"
                label="New status"
                required
                :options="[
                  { value: 'ACTIVE', label: 'Active (live)' },
                  { value: 'INACTIVE', label: 'Inactive (hidden)' },
                  { value: 'REJECTED', label: 'Rejected' },
                ]"
              ></f-select>
              <button
                class="btn btn-primary"
                :class="{ 'is-loading': statusF.busy }"
              >
                Update status
              </button>
            </form>
          </div>

          <div v-if="ad.tab === 'bookings'" class="card pad">
            <lookup-panel
              :lk="lk"
              @find="lookupBooking"
              @complete="completeBooking"
              @cancel="cancelFromLookup"
            ></lookup-panel>
          </div>
        </div>
      </section>

      <section v-if="route.name === 'notfound'" class="page">
        <div class="wrap">
          <div class="card empty">
            <div class="big"><icon name="search" :size="30"></icon></div>
            <h2>Page not found</h2>
            <p>That page doesn’t exist.</p>
            <a class="btn btn-primary" href="#/">Go home</a>
          </div>
        </div>
      </section>
    </main>

    <footer class="site no-print">
      <div class="wrap row between">
        <div><b style="color: #fff">VibeStay</b> – your stay starts here.</div>
        <div class="row">
          <a href="#/explore">Explore hotels</a
          ><a href="#/register">Create account</a>
        </div>
      </div>
    </footer>

    <!-- ================= MODALS ================= -->
    <div v-if="bd.open" class="modal-bg" @click.self="bd.open = false">
      <div class="modal lg" role="dialog" aria-modal="true">
        <div class="modal-h">
          <h3>Booking details</h3>
          <button class="x-btn" @click="bd.open = false" aria-label="Close">
            <icon name="x"></icon>
          </button>
        </div>
        <div class="modal-b">
          <div v-if="bd.loading" class="skel" style="height: 200px"></div>
          <alert-box v-else-if="bd.err" type="error" :msg="bd.err"></alert-box
          ><booking-detail v-else-if="bd.data" :b="bd.data"></booking-detail>
        </div>
        <div class="modal-f" v-if="bd.data">
          <button
            v-if="bd.data.status === 'CONFIRMED'"
            class="btn btn-danger-o"
            @click="cancelBooking(bd.data, () => loadBookings(mb.page))"
          >
            Cancel booking</button
          ><button class="btn btn-primary" @click="bd.open = false">
            Close
          </button>
        </div>
      </div>
    </div>

    <div
      v-if="roomModal.open"
      class="modal-bg"
      @click.self="roomModal.open = false"
    >
      <div class="modal" role="dialog" aria-modal="true">
        <div class="modal-h">
          <h3>{{ roomF.values.id ? "Edit room" : "Add a room" }}</h3>
          <button
            class="x-btn"
            @click="roomModal.open = false"
            aria-label="Close"
          >
            <icon name="x"></icon>
          </button>
        </div>
        <form @submit.prevent="saveRoom" novalidate>
          <div class="modal-b">
            <alert-box
              v-if="roomF.banner"
              type="error"
              :msg="roomF.banner"
            ></alert-box>
            <f-select
              v-model="roomF.values.roomTypeId"
              label="Room type"
              required
              placeholder="Select a room type"
              :options="roomTypes.map((t) => ({ value: t.id, label: t.name }))"
              :error="roomF.errors.roomTypeId"
              @blur="roomF.check('roomTypeId')"
              :hint="
                roomTypes.length
                  ? ''
                  : 'No room types yet – an admin needs to create some first'
              "
            ></f-select>
            <div class="form-grid">
              <f-input
                v-model="roomF.values.roomNumber"
                label="Room number"
                required
                :error="roomF.errors.roomNumber"
                @blur="roomF.check('roomNumber')"
              ></f-input
              ><f-input
                v-model="roomF.values.floorNumber"
                label="Floor"
                required
                :error="roomF.errors.floorNumber"
                @blur="roomF.check('floorNumber')"
              ></f-input>
            </div>
            <f-input
              v-model="roomF.values.pricePerNight"
              label="Price per night (BHD)"
              type="number"
              min="0.01"
              step="0.01"
              required
              :error="roomF.errors.pricePerNight"
              @blur="roomF.check('pricePerNight')"
            ></f-input>
            <div
              class="form-grid"
              style="grid-template-columns: repeat(3, 1fr)"
            >
              <f-input
                v-model="roomF.values.maxAdults"
                label="Max adults"
                type="number"
                min="1"
                required
                :error="roomF.errors.maxAdults"
                @blur="roomF.check('maxAdults')"
              ></f-input
              ><f-input
                v-model="roomF.values.maxChildren"
                label="Max children"
                type="number"
                min="1"
                required
                :error="roomF.errors.maxChildren"
                @blur="roomF.check('maxChildren')"
              ></f-input
              ><f-input
                v-model="roomF.values.maxOccupancy"
                label="Max guests"
                type="number"
                min="1"
                required
                :error="roomF.errors.maxOccupancy"
                @blur="roomF.check('maxOccupancy')"
              ></f-input>
            </div>
          </div>
          <div class="modal-f">
            <button
              type="button"
              class="btn btn-ghost"
              @click="roomModal.open = false"
            >
              Cancel</button
            ><button
              class="btn btn-primary"
              :class="{ 'is-loading': roomF.busy }"
            >
              {{ roomF.values.id ? "Save room" : "Add room" }}
            </button>
          </div>
        </form>
      </div>
    </div>

    <div
      v-if="bulkModal.open"
      class="modal-bg"
      @click.self="bulkModal.open = false"
    >
      <div class="modal" role="dialog" aria-modal="true">
        <div class="modal-h">
          <h3>Add several rooms</h3>
          <button
            class="x-btn"
            @click="bulkModal.open = false"
            aria-label="Close"
          >
            <icon name="x"></icon>
          </button>
        </div>
        <form @submit.prevent="saveBulk" novalidate>
          <div class="modal-b">
            <p class="small muted">
              Creates rooms with consecutive numbers and the same settings. If
              one number is already used, nothing is added.
            </p>
            <alert-box
              v-if="bulkF.banner"
              type="error"
              :msg="bulkF.banner"
            ></alert-box>
            <f-select
              v-model="bulkF.values.roomTypeId"
              label="Room type"
              required
              placeholder="Select a room type"
              :options="roomTypes.map((t) => ({ value: t.id, label: t.name }))"
              :error="bulkF.errors.roomTypeId"
              @blur="bulkF.check('roomTypeId')"
            ></f-select>
            <div
              class="form-grid"
              style="grid-template-columns: repeat(3, 1fr)"
            >
              <f-input
                v-model="bulkF.values.prefix"
                label="Prefix (optional)"
                placeholder="e.g. A-"
                :error="bulkF.errors.prefix"
              ></f-input
              ><f-input
                v-model="bulkF.values.start"
                label="First number"
                type="number"
                min="1"
                required
                :error="bulkF.errors.start"
                @blur="bulkF.check('start')"
              ></f-input
              ><f-input
                v-model="bulkF.values.count"
                label="How many"
                type="number"
                min="1"
                max="100"
                required
                :error="bulkF.errors.count"
                @blur="bulkF.check('count')"
              ></f-input>
            </div>
            <div
              v-if="bulkNumbers.length"
              class="small muted"
              style="margin: -4px 0 10px"
            >
              Will create: <b>{{ bulkNumbers.slice(0, 6).join(", ") }}</b
              ><span v-if="bulkNumbers.length > 6">
                … {{ bulkNumbers[bulkNumbers.length - 1] }}</span
              >
              ({{ plural(bulkNumbers.length, "room") }})
            </div>
            <div class="form-grid">
              <f-input
                v-model="bulkF.values.floorNumber"
                label="Floor"
                required
                :error="bulkF.errors.floorNumber"
                @blur="bulkF.check('floorNumber')"
              ></f-input
              ><f-input
                v-model="bulkF.values.pricePerNight"
                label="Price per night (BHD)"
                type="number"
                min="0.01"
                step="0.01"
                required
                :error="bulkF.errors.pricePerNight"
                @blur="bulkF.check('pricePerNight')"
              ></f-input>
            </div>
            <div
              class="form-grid"
              style="grid-template-columns: repeat(3, 1fr)"
            >
              <f-input
                v-model="bulkF.values.maxAdults"
                label="Max adults"
                type="number"
                min="1"
                required
                :error="bulkF.errors.maxAdults"
                @blur="bulkF.check('maxAdults')"
              ></f-input
              ><f-input
                v-model="bulkF.values.maxChildren"
                label="Max children"
                type="number"
                min="1"
                required
                :error="bulkF.errors.maxChildren"
                @blur="bulkF.check('maxChildren')"
              ></f-input
              ><f-input
                v-model="bulkF.values.maxOccupancy"
                label="Max guests"
                type="number"
                min="1"
                required
                :error="bulkF.errors.maxOccupancy"
                @blur="bulkF.check('maxOccupancy')"
              ></f-input>
            </div>
          </div>
          <div class="modal-f">
            <button
              type="button"
              class="btn btn-ghost"
              @click="bulkModal.open = false"
            >
              Cancel</button
            ><button
              class="btn btn-primary"
              :class="{ 'is-loading': bulkF.busy }"
            >
              Add {{ plural(bulkNumbers.length, "room") }}
            </button>
          </div>
        </form>
      </div>
    </div>

    <div v-if="rimg.open" class="modal-bg" @click.self="closeRoomImages">
      <div
        class="modal"
        role="dialog"
        aria-modal="true"
        style="max-width: 640px"
      >
        <div class="modal-h">
          <h3>Photos – room {{ rimg.room && rimg.room.roomNumber }}</h3>
          <button class="x-btn" @click="closeRoomImages" aria-label="Close">
            <icon name="x"></icon>
          </button>
        </div>
        <div class="modal-b">
          <alert-box v-if="rimg.err" type="error" :msg="rimg.err"></alert-box>
          <label class="dz" style="display: block"
            ><icon name="image" :size="28"></icon>
            <div><b>Click to choose photos</b></div>
            <div class="xs">
              JPG, PNG or WebP · up to 1 MB each · 10 at a time · 20 per room
            </div>
            <input
              type="file"
              accept="image/jpeg,image/png,image/webp"
              multiple
              class="hide"
              @change="pickRoomImages"
          /></label>
          <div v-if="rimg.items.length" class="row mt" style="gap: 10px">
            <div
              v-for="(it, i) in rimg.items"
              :key="i"
              style="position: relative"
            >
              <img class="thumb" :src="it.preview" alt="" /><button
                type="button"
                class="btn btn-ghost btn-sm"
                style="
                  position: absolute;
                  top: -8px;
                  right: -8px;
                  background: #fff;
                "
                @click="dropRoomImage(i)"
                aria-label="Remove"
              >
                <icon name="x" :size="12"></icon>
              </button>
            </div>
          </div>
          <button
            v-if="rimg.items.length"
            class="btn btn-primary mt"
            :class="{ 'is-loading': rimg.busy }"
            @click="uploadRoomImages"
          >
            <icon name="upload" :size="16"></icon>Upload
            {{ plural(rimg.items.length, "photo") }}
          </button>
          <h4 class="mt2">Current photos</h4>
          <div v-if="rimg.loading" class="skel" style="height: 80px"></div>
          <div v-else-if="!rimg.images.length" class="muted small">
            No photos for this room yet.
          </div>
          <div class="grid g3">
            <div v-for="img in rimg.images" :key="img.id">
              <div class="pimg">
                <hotel-img
                  :src="img.imageUrl"
                  :name="'Room ' + rimg.room.roomNumber"
                  alt="Room photo"
                  @fail="rimg.fails++"
                ></hotel-img
                ><button
                  class="btn btn-danger btn-sm del"
                  @click="deleteRoomImage(img)"
                  aria-label="Delete photo"
                >
                  <icon name="trash" :size="14"></icon>
                </button>
              </div>
            </div>
          </div>
        </div>
        <div class="modal-f">
          <button class="btn btn-primary" @click="closeRoomImages">Done</button>
        </div>
      </div>
    </div>

    <div
      v-if="amenModal.open"
      class="modal-bg"
      @click.self="amenModal.open = false"
    >
      <div class="modal" role="dialog" aria-modal="true">
        <div class="modal-h">
          <h3>
            {{
              amenModal.editing
                ? "Edit " + amenModal.editing.name
                : "Add an amenity"
            }}
          </h3>
          <button
            class="x-btn"
            @click="amenModal.open = false"
            aria-label="Close"
          >
            <icon name="x"></icon>
          </button>
        </div>
        <form @submit.prevent="saveAmenity" novalidate>
          <div class="modal-b">
            <alert-box
              v-if="amenF.banner"
              type="error"
              :msg="amenF.banner"
            ></alert-box>
            <template v-if="!amenModal.editing">
              <f-select
                v-model="amenF.values.amenityId"
                label="Choose from the list"
                placeholder="Select an amenity"
                :options="
                  availableAmenities.map((a) => ({
                    value: a.id,
                    label: a.name,
                  }))
                "
                :error="amenF.errors.amenityId"
                @blur="amenF.check('amenityId')"
              ></f-select>
              <f-input
                v-model="amenF.values.newName"
                label="…or create a new one"
                placeholder="e.g. Airport shuttle"
                :error="amenF.errors.newName"
                hint="If you type a name here it is used instead of the list"
              ></f-input>
            </template>
            <f-input
              v-model="amenF.values.description"
              label="Description (optional)"
              placeholder="e.g. Rooftop pool, open 7am–10pm"
              :error="amenF.errors.description"
            ></f-input>
            <f-input
              v-model="amenF.values.additionalCost"
              label="Extra cost (BHD, optional)"
              type="number"
              min="0"
              step="0.01"
              :error="amenF.errors.additionalCost"
              hint="Leave empty if it is free"
            ></f-input>
          </div>
          <div class="modal-f">
            <button
              type="button"
              class="btn btn-ghost"
              @click="amenModal.open = false"
            >
              Cancel</button
            ><button
              class="btn btn-primary"
              :class="{ 'is-loading': amenF.busy }"
            >
              Save
            </button>
          </div>
        </form>
      </div>
    </div>

    <div
      v-if="catModal.open"
      class="modal-bg"
      @click.self="catModal.open = false"
    >
      <div class="modal" role="dialog" aria-modal="true">
        <div class="modal-h">
          <h3>{{ catF.values.id ? "Edit amenity" : "New amenity" }}</h3>
          <button
            class="x-btn"
            @click="catModal.open = false"
            aria-label="Close"
          >
            <icon name="x"></icon>
          </button>
        </div>
        <form @submit.prevent="saveCatalog" novalidate>
          <div class="modal-b">
            <alert-box
              v-if="catF.banner"
              type="error"
              :msg="catF.banner"
            ></alert-box>
            <f-input
              v-model="catF.values.name"
              label="Name"
              required
              :error="catF.errors.name"
              @blur="catF.check('name')"
            ></f-input>
            <f-input
              v-model="catF.values.description"
              label="Description (optional)"
              :error="catF.errors.description"
            ></f-input>
          </div>
          <div class="modal-f">
            <button
              type="button"
              class="btn btn-ghost"
              @click="catModal.open = false"
            >
              Cancel</button
            ><button
              class="btn btn-primary"
              :class="{ 'is-loading': catF.busy }"
            >
              Save
            </button>
          </div>
        </form>
      </div>
    </div>

    <div
      v-if="typeModal.open"
      class="modal-bg"
      @click.self="typeModal.open = false"
    >
      <div class="modal" role="dialog" aria-modal="true">
        <div class="modal-h">
          <h3>{{ typeF.values.id ? "Edit room type" : "New room type" }}</h3>
          <button
            class="x-btn"
            @click="typeModal.open = false"
            aria-label="Close"
          >
            <icon name="x"></icon>
          </button>
        </div>
        <form @submit.prevent="saveType" novalidate>
          <div class="modal-b">
            <alert-box
              v-if="typeF.banner"
              type="error"
              :msg="typeF.banner"
            ></alert-box>
            <f-input
              v-model="typeF.values.name"
              label="Name"
              required
              placeholder="e.g. Deluxe King"
              :error="typeF.errors.name"
              @blur="typeF.check('name')"
            ></f-input>
            <f-input
              v-model="typeF.values.description"
              textarea
              :rows="3"
              label="Description"
              required
              :maxlength="500"
              counter
              hint="10–500 characters"
              :error="typeF.errors.description"
              @blur="typeF.check('description')"
            ></f-input>
          </div>
          <div class="modal-f">
            <button
              type="button"
              class="btn btn-ghost"
              @click="typeModal.open = false"
            >
              Cancel</button
            ><button
              class="btn btn-primary"
              :class="{ 'is-loading': typeF.busy }"
            >
              Save
            </button>
          </div>
        </form>
      </div>
    </div>

    <div
      v-if="assignModal.open"
      class="modal-bg"
      @click.self="assignModal.open = false"
    >
      <div class="modal" role="dialog" aria-modal="true">
        <div class="modal-h">
          <h3>Assign a manager</h3>
          <button
            class="x-btn"
            @click="assignModal.open = false"
            aria-label="Close"
          >
            <icon name="x"></icon>
          </button>
        </div>
        <form @submit.prevent="doAssign" novalidate>
          <div class="modal-b">
            <p class="small muted">
              Add another manager to <b>{{ assignF.values.hotelName }}</b
              >.
            </p>
            <alert-box
              v-if="assignF.banner"
              type="error"
              :msg="assignF.banner"
            ></alert-box>
            <f-input
              v-model="assignF.values.managerId"
              label="Manager user ID"
              type="number"
              min="1"
              required
              :error="assignF.errors.managerId"
              @blur="assignF.check('managerId')"
            ></f-input>
          </div>
          <div class="modal-f">
            <button
              type="button"
              class="btn btn-ghost"
              @click="assignModal.open = false"
            >
              Cancel</button
            ><button
              class="btn btn-primary"
              :class="{ 'is-loading': assignF.busy }"
            >
              Assign
            </button>
          </div>
        </form>
      </div>
    </div>

    <div v-if="dlg.open" class="modal-bg" @click.self="answer(false)">
      <div
        class="modal"
        role="alertdialog"
        aria-modal="true"
        style="max-width: 440px"
      >
        <div class="modal-b">
          <h3>{{ dlg.title }}</h3>
          <p class="muted" style="margin: 0">{{ dlg.message }}</p>
        </div>
        <div class="modal-f">
          <button class="btn btn-ghost" @click="answer(false)">
            {{ dlg.cancelText }}</button
          ><button
            class="btn"
            :class="dlg.danger ? 'btn-danger' : 'btn-primary'"
            @click="answer(true)"
          >
            {{ dlg.confirmText }}
          </button>
        </div>
      </div>
    </div>

    <div class="toasts no-print" aria-live="polite">
      <div
        v-for="t in toasts"
        :key="t.id"
        class="toast"
        :class="t.type"
        role="status"
      >
        <icon
          :name="
            t.type === 'success'
              ? 'ok'
              : t.type === 'error'
                ? 'alert'
                : t.type === 'warn'
                  ? 'alert'
                  : 'info'
          "
          :size="20"
        ></icon>
        <div>{{ t.msg }}</div>
        <button @click="dismiss(t.id)" aria-label="Dismiss">
          <icon name="x" :size="16"></icon>
        </button>
      </div>
    </div>
  </div>
</template>
