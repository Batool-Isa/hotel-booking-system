<script>
import { nightsBetween } from '@/lib/utils';

export default {
  props:{ b:Object },
  computed:{ nights(){ return nightsBetween(this.b.checkIn,this.b.checkOut); } },
  methods:{ gt(t){ return {ADULT:'Adult',CHILD:'Child',INFANT:'Infant'}[t]||t; } }
};
</script>

<template>
  <div>
    <div class="row between mb"><div><div class="xs muted">Booking reference</div><span class="ref">{{b.bookingReference}}</span></div><span class="badge" :class="statusClass(b.status)">{{statusLabel(b.status)}}</span></div>
    <dl class="kv mb">
      <dt>Hotel</dt><dd>{{b.hotelName}}</dd>
      <dt>Check-in</dt><dd>{{fmtDate(b.checkIn)}}</dd>
      <dt>Check-out</dt><dd>{{fmtDate(b.checkOut)}} <span class="muted small">({{plural(nights,'night')}})</span></dd>
      <dt>Guests</dt><dd>{{plural(b.adults,'adult')}}, {{plural(b.children,'child','children')}}</dd>
      <dt>Total</dt><dd>{{money(b.totalAmount)}}</dd>
      <dt v-if="b.specialRequest">Special request</dt><dd v-if="b.specialRequest">{{b.specialRequest}}</dd>
    </dl>
    <h4>Rooms &amp; guests</h4>
    <div v-for="r in (b.rooms||[])" :key="r.roomId" class="card pad-s mb">
      <div class="row between"><b>{{r.roomTypeName}} <span class="muted">· Room {{r.roomNumber}}</span></b><span class="small">{{money(r.pricePerNight)}} / night</span></div>
      <ul class="small" style="margin:8px 0 0;padding-left:18px"><li v-for="g in (r.guests||[])" :key="g.id">{{g.name}} – {{g.age}} yrs <span class="badge b-gray">{{gt(g.guestType)}}</span></li></ul>
    </div>
  </div>
</template>
