<script>
export default {
  props:{ lk:Object, canComplete:{ default:true } },
  emits:['find','complete','cancel']
};
</script>

<template>
  <div>
    <alert-box type="info">The API has no “list all bookings” endpoint yet, so look a booking up by its numeric ID (the <code>id</code> field), then complete or cancel it.</alert-box>
    <form class="row" style="align-items:flex-start" @submit.prevent="$emit('find')" novalidate>
      <div style="width:240px"><f-input v-model="lk.id" label="Booking ID" type="number" min="1" placeholder="e.g. 12" :error="lk.err"></f-input></div>
      <button class="btn btn-primary" style="margin-top:26px" :class="{'is-loading':lk.loading}">Find booking</button>
    </form>
    <div v-if="lk.data" class="card pad mt">
      <booking-detail :b="lk.data"></booking-detail>
      <div class="row mt" v-if="lk.data.status==='CONFIRMED'">
        <button v-if="canComplete" class="btn btn-green" @click="$emit('complete',lk.data)"><icon name="check"></icon>Mark as completed</button>
        <button class="btn btn-danger-o" @click="$emit('cancel',lk.data)"><icon name="x"></icon>Cancel booking</button>
      </div>
      <p v-else class="muted small mt">This booking is {{statusLabel(lk.data.status).toLowerCase()}} and can’t be changed any more.</p>
    </div>
  </div>
</template>
