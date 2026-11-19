<script>
import { nightsBetween } from '@/lib/utils';

const digits = (s) => String(s || '').slice(0, 10).replace(/\D/g, '');
const esc = (s) => String(s || '').replace(/([,;\\])/g, '\\$1').replace(/\r?\n/g, '\\n');

export default {
  props: { b: Object },
  computed: {
    nights() {
      return nightsBetween(this.b.checkIn, this.b.checkOut);
    },
    roomLine() {
      const rooms = this.b.rooms || [];
      if (!rooms.length) return '';
      return rooms.map((r) => `${r.roomTypeName} · Room ${r.roomNumber}`).join('  |  ');
    },
    bars() {
      const out = [];
      let x = 0;
      const text = String(this.b.bookingReference || 'VIBESTAY');
      for (let i = 0; i < text.length; i++) {
        const c = text.charCodeAt(i);
        for (let k = 0; k < 4; k++) {
          const w = ((c >> (k * 2)) & 3) + 1;
          if (k % 2 === 0) out.push({ x, w });
          x += w;
        }
        x += 2;
      }
      return { list: out, width: x };
    },
  },
  methods: {
    addToCalendar() {
      const b = this.b;
      const lines = [
        'BEGIN:VCALENDAR',
        'VERSION:2.0',
        'PRODID:-//VibeStay//Booking//EN',
        'CALSCALE:GREGORIAN',
        'BEGIN:VEVENT',
        `UID:${esc(b.bookingReference)}@vibestay`,
        `DTSTAMP:${new Date().toISOString().replace(/[-:]/g, '').slice(0, 15)}Z`,
        `DTSTART;VALUE=DATE:${digits(b.checkIn)}`,
        `DTEND;VALUE=DATE:${digits(b.checkOut)}`,
        `SUMMARY:${esc('Stay at ' + b.hotelName)}`,
        `DESCRIPTION:${esc('VibeStay booking ' + b.bookingReference)}`,
        'END:VEVENT',
        'END:VCALENDAR',
      ];
      const blob = new Blob([lines.join('\r\n')], { type: 'text/calendar;charset=utf-8' });
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `vibestay-${b.bookingReference}.ics`;
      document.body.appendChild(a);
      a.click();
      a.remove();
      setTimeout(() => URL.revokeObjectURL(url), 1000);
    },
  },
};
</script>

<template>
  <div class="ticket-wrap">
    <div class="ticket">
      <div class="ticket-main">
        <div class="ticket-head">
          <brand-logo :size="26"></brand-logo>
          <span class="ticket-tag">Booking confirmed</span>
        </div>
        <div class="ticket-hotel">{{ b.hotelName }}</div>
        <div class="ticket-dates">
          <div>
            <div class="ticket-label">Check-in</div>
            <div class="ticket-date">{{ fmtDate(b.checkIn) }}</div>
          </div>
          <div class="ticket-nights">
            <span>{{ plural(nights, 'night') }}</span>
          </div>
          <div>
            <div class="ticket-label">Check-out</div>
            <div class="ticket-date">{{ fmtDate(b.checkOut) }}</div>
          </div>
        </div>
        <div class="ticket-meta">
          <div>
            <div class="ticket-label">Guests</div>
            <div>{{ plural(b.adults, 'adult') }}, {{ plural(b.children, 'child', 'children') }}</div>
          </div>
          <div v-if="roomLine">
            <div class="ticket-label">Rooms</div>
            <div>{{ roomLine }}</div>
          </div>
        </div>
      </div>
      <div class="ticket-stub">
        <div class="ticket-label">Reference</div>
        <div class="ticket-ref">{{ b.bookingReference }}</div>
        <svg class="ticket-bars" :viewBox="`0 0 ${bars.width} 40`" preserveAspectRatio="none" aria-hidden="true">
          <rect v-for="(r, i) in bars.list" :key="i" :x="r.x" y="0" :width="r.w" height="40" fill="currentColor"></rect>
        </svg>
        <div class="ticket-label mt">Total paid</div>
        <div class="ticket-total">{{ money(b.totalAmount) }}</div>
      </div>
    </div>
    <div class="row ticket-actions no-print" style="justify-content: center">
      <button type="button" class="btn btn-yellow" @click="addToCalendar">
        <icon name="calendar" :size="16"></icon>Add to calendar
      </button>
    </div>
  </div>
</template>
