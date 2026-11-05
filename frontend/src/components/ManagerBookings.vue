<script>
import { api } from "@/lib/api";
import { todayISO, initials, parseISO } from "@/lib/utils";

// sort that makes sense for each time window
const SORT = {
  upcoming: "checkIn,asc",
  current: "checkOut,asc",
  past: "checkOut,desc",
  all: "id,desc",
};

export default {
  props: {
    // hotels the manager runs, for the hotel filter: [{ id, name }]
    hotels: { type: Array, default: () => [] },
    // when set, the list is locked to this hotel (used inside a hotel's page)
    hotelId: { type: [Number, String], default: null },
    // managers use their own list; admins pass "/api/bookings/all"
    endpoint: { type: String, default: "/api/bookings/managed" },
    onDetails: Function,
    onComplete: Function,
    onCancel: Function,
  },
  data() {
    return {
      when: "upcoming",
      status: "",
      hotel: "",
      q: "",
      items: [],
      page: 0,
      size: 10,
      total: 0,
      totalPages: 0,
      loading: false,
      error: "",
      today: todayISO(),
      timer: null,
    };
  },
  computed: {
    lockedHotel() {
      return this.hotelId != null && this.hotelId !== "";
    },
    showHotel() {
      return !this.lockedHotel && this.hotels.length > 1;
    },
    windows() {
      return [
        ["upcoming", "Upcoming"],
        ["current", "Staying now"],
        ["past", "Past"],
        ["all", "All"],
      ];
    },
    emptyText() {
      if (this.q || this.status || this.hotel) return "No bookings match these filters.";
      return {
        upcoming: "No upcoming bookings yet.",
        current: "No guests are staying right now.",
        past: "No past stays yet.",
        all: "No bookings yet.",
      }[this.when];
    },
  },
  watch: {
    hotelId() {
      this.load(0);
    },
  },
  mounted() {
    this.load(0);
  },
  beforeUnmount() {
    clearTimeout(this.timer);
  },
  methods: {
    initials,
    async load(p = this.page) {
      this.loading = true;
      this.error = "";
      this.today = todayISO();
      try {
        const hotelId = this.lockedHotel ? this.hotelId : this.hotel || undefined;
        const r = await api("GET", this.endpoint, {
          query: {
            page: p,
            size: this.size,
            sort: SORT[this.when],
            when: this.when === "all" ? undefined : this.when,
            status: this.status || undefined,
            hotelId,
            q: this.q.trim() || undefined,
          },
        });
        this.items = (r && r.content) || [];
        this.total = (r && r.totalElements) || 0;
        this.totalPages = (r && r.totalPages) || 0;
        this.page = p;
      } catch (e) {
        this.items = [];
        this.error = e.message;
      } finally {
        this.loading = false;
      }
    },
    reload() {
      return this.load(this.page);
    },
    setWhen(w) {
      this.when = w;
      this.load(0);
    },
    searchSoon() {
      clearTimeout(this.timer);
      this.timer = setTimeout(() => this.load(0), 350);
    },
    clear() {
      this.q = "";
      this.status = "";
      this.hotel = "";
      this.load(0);
    },
    // a short hint next to the dates, e.g. "Arrives today"
    hint(b) {
      if (b.status === "CANCELLED") return "";
      if (b.checkIn === this.today) return "Arrives today";
      if (b.checkOut === this.today) return "Leaves today";
      if (b.checkIn < this.today && b.checkOut > this.today) return "Staying now";
      return "";
    },
    // "Thu 14 Jan", plus the year when it isn't this year
    shortDate(d) {
      const x = parseISO(d);
      const opts = { weekday: "short", day: "numeric", month: "short" };
      if (x.getFullYear() !== new Date().getFullYear()) opts.year = "numeric";
      return x.toLocaleDateString("en-GB", opts);
    },
    roomsText(b) {
      return (b.rooms || [])
        .map((r) => (r.roomTypeName ? r.roomTypeName + " " : "") + "#" + r.roomNumber)
        .join(", ");
    },
    done(fn, b) {
      if (fn) fn(b, () => this.reload());
    },
  },
};
</script>

<template>
  <div class="mgr-bk">
    <div class="row between mb" style="gap: 10px; flex-wrap: wrap">
      <div class="seg" role="tablist" aria-label="When">
        <button
          v-for="[k, label] in windows"
          :key="k"
          :class="{ on: when === k }"
          role="tab"
          :aria-selected="when === k"
          @click="setWhen(k)"
        >
          {{ label }}
        </button>
      </div>
      <button class="btn btn-outline btn-sm" :class="{ 'is-loading': loading }" @click="reload">
        <icon name="refresh" :size="15"></icon>Refresh
      </button>
    </div>

    <div class="card pad mb mgr-filters">
      <label class="mgr-search">
        <icon name="search" :size="16"></icon>
        <input
          v-model="q"
          type="search"
          placeholder="Search by guest name, email or booking reference"
          style="min-width: 0"
          aria-label="Search bookings"
          @input="searchSoon"
        />
      </label>
      <select v-if="showHotel" v-model="hotel" aria-label="Hotel" @change="load(0)">
        <option value="">All hotels</option>
        <option v-for="h in hotels" :key="h.id" :value="h.id">{{ h.name }}</option>
      </select>
      <select v-model="status" aria-label="Status" @change="load(0)">
        <option value="">Any status</option>
        <option value="PENDING">{{ statusLabel("PENDING") }}</option>
        <option value="CONFIRMED">{{ statusLabel("CONFIRMED") }}</option>
        <option value="COMPLETED">{{ statusLabel("COMPLETED") }}</option>
        <option value="CANCELLED">{{ statusLabel("CANCELLED") }}</option>
      </select>
      <button v-if="q || status || hotel" class="btn btn-ghost btn-sm" @click="clear">Clear</button>
    </div>

    <alert-box v-if="error" type="error" :msg="error"></alert-box>

    <div v-if="loading && !items.length" class="skel" style="height: 180px"></div>
    <div v-else-if="!items.length && !error" class="card empty">
      <div class="big"><icon name="calendar" :size="30"></icon></div>
      <p>{{ emptyText }}</p>
    </div>

    <template v-else-if="items.length">
      <p class="small muted" style="margin: 0 0 8px">
        {{ plural(total, "booking") }} · click a booking to see all its details
      </p>
      <div class="card bk-list">
        <div class="bk-row bk-head" aria-hidden="true">
          <span>Guest</span>
          <span>Stay</span>
          <span>{{ lockedHotel ? "Room" : "Hotel & room" }}</span>
          <span>Total</span>
          <span></span>
        </div>
        <div
          v-for="b in items"
          :key="b.id"
          class="bk-row"
          role="button"
          tabindex="0"
          :aria-label="'Open booking ' + b.bookingReference"
          @click="onDetails && onDetails(b.id)"
          @keydown.enter="onDetails && onDetails(b.id)"
        >
          <div class="bk-guest">
            <span class="avatar sm">{{ initials(b.guestName) }}</span>
            <div class="bk-min">
              <b class="ell">{{ b.guestName || "Guest" }}</b>
              <div class="xs muted ell" :title="b.guestEmail">{{ b.guestEmail }}</div>
            </div>
          </div>

          <div class="bk-stay bk-min">
            <div class="ell">{{ shortDate(b.checkIn) }} → {{ shortDate(b.checkOut) }}</div>
            <div class="xs muted ell">
              {{ plural(nightsBetween(b.checkIn, b.checkOut), "night") }} ·
              {{ plural(b.adults, "adult") }}<span v-if="b.children"
                >, {{ plural(b.children, "child", "children") }}</span
              >
            </div>
            <span v-if="hint(b)" class="badge b-blue bk-hint">{{ hint(b) }}</span>
          </div>

          <div class="bk-place bk-min">
            <div v-if="!lockedHotel" class="ell" :title="b.hotelName">{{ b.hotelName }}</div>
            <div class="xs muted ell" :class="{ small: lockedHotel }">{{ roomsText(b) || "–" }}</div>
            <span class="bk-ref">{{ b.bookingReference }}</span>
          </div>

          <div class="bk-money">
            <b>{{ money(b.totalAmount) }}</b>
            <span class="badge" :class="statusClass(b.status)">{{ statusLabel(b.status) }}</span>
          </div>

          <div class="bk-actions" @click.stop @keydown.enter.stop>
            <button
              v-if="b.status === 'CONFIRMED' && b.checkIn <= today"
              class="btn btn-green btn-sm"
              title="The guest has stayed"
              @click="done(onComplete, b)"
            >
              <icon name="check" :size="14"></icon>Complete
            </button>
            <button
              v-if="b.status === 'CONFIRMED' || b.status === 'PENDING'"
              class="btn btn-danger-o btn-sm"
              @click="done(onCancel, b)"
            >
              Cancel
            </button>
            <icon class="bk-chev" name="right" :size="18"></icon>
          </div>
        </div>
      </div>
      <div class="pager" v-if="totalPages > 1">
        <button class="btn btn-outline btn-sm" :disabled="page === 0" @click="load(page - 1)">
          <icon name="left" :size="16"></icon>Previous</button
        ><span class="muted small">Page {{ page + 1 }} of {{ totalPages }}</span
        ><button class="btn btn-outline btn-sm" :disabled="page + 1 >= totalPages" @click="load(page + 1)">
          Next<icon name="right" :size="16"></icon>
        </button>
      </div>
    </template>
  </div>
</template>

<style scoped>
.bk-list {
  overflow: hidden;
}
.bk-row {
  display: grid;
  /* fixed-width actions column, so rows with and without buttons line up */
  grid-template-columns: minmax(0, 1.5fr) minmax(0, 1.3fr) minmax(0, 1.4fr) minmax(0, 0.9fr) 220px;
  gap: 16px;
  align-items: center;
  padding: 14px 18px;
  border-top: 1px solid var(--line);
  cursor: pointer;
  transition: background 0.12s;
}
.bk-row:hover,
.bk-row:focus-visible {
  background: #f5f8ff;
  outline: 0;
}
.bk-head {
  border-top: 0;
  padding-top: 10px;
  padding-bottom: 10px;
  background: var(--bg);
  cursor: default;
  font-size: 0.75rem;
  font-weight: 700;
  letter-spacing: 0.04em;
  text-transform: uppercase;
  color: var(--muted);
}
.bk-head:hover {
  background: var(--bg);
}
.bk-min {
  min-width: 0;
}
.ell {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.bk-guest {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}
.bk-hint {
  margin-top: 4px;
}
.bk-ref {
  display: inline-block;
  margin-top: 4px;
  font-family: ui-monospace, Consolas, monospace;
  font-size: 0.72rem;
  color: var(--muted);
  letter-spacing: 0.03em;
}
.bk-money {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 4px;
  white-space: nowrap;
}
.bk-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 6px;
  cursor: default;
}
.bk-chev {
  color: var(--muted);
  cursor: pointer;
}
.mgr-filters {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  align-items: center;
}
.mgr-filters select {
  min-width: 150px;
  padding: 9px 10px;
  border: 1px solid var(--line);
  border-radius: 8px;
  font: inherit;
  background: #fff;
}
.mgr-search {
  flex: 1 1 280px;
  display: flex;
  align-items: center;
  gap: 8px;
  border: 1px solid var(--line);
  border-radius: 8px;
  padding: 0 10px;
  color: var(--muted);
  background: #fff;
}
.mgr-search input {
  border: 0;
  flex: 1;
  padding: 9px 0;
  font: inherit;
  background: none;
  box-shadow: none !important;
}
.avatar.sm {
  width: 34px;
  height: 34px;
  font-size: 0.8rem;
  flex: none;
}

/* narrow screens: each booking becomes a small card */
@media (max-width: 860px) {
  .bk-head {
    display: none;
  }
  .bk-row {
    grid-template-columns: minmax(0, 1fr) auto;
    grid-template-areas:
      "guest money"
      "stay place"
      "actions actions";
    gap: 10px 14px;
  }
  .bk-row:first-of-type {
    border-top: 0;
  }
  .bk-guest {
    grid-area: guest;
  }
  .bk-money {
    grid-area: money;
    align-items: flex-end;
  }
  .bk-stay {
    grid-area: stay;
  }
  .bk-place {
    grid-area: place;
    text-align: right;
  }
  .bk-actions {
    grid-area: actions;
  }
}
</style>
