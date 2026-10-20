import { createApp } from 'vue'
import App from './App.vue'
import './assets/main.css'

import AppIcon from './components/AppIcon.vue'
import HotelMap from './components/HotelMap.vue'
import HotelImg from './components/HotelImg.vue'
import FInput from './components/FInput.vue'
import FSelect from './components/FSelect.vue'
import AlertBox from './components/AlertBox.vue'
import SearchBar from './components/SearchBar.vue'
import BookingDetail from './components/BookingDetail.vue'
import LookupPanel from './components/LookupPanel.vue'
import { fmtDate, fmtDateTime, money, imgUrl, nightsBetween, statusLabel, statusClass, plural, amenityIcon, initials, roleLabel } from './lib/utils'

const app = createApp(App)

// formatting helpers available in every template
Object.assign(app.config.globalProperties, { fmtDate, fmtDateTime, money, imgUrl, nightsBetween, statusLabel, statusClass, plural, amenityIcon, initials, roleLabel })

app.component('icon', AppIcon)
app.component('hotel-map', HotelMap)
app.component('hotel-img', HotelImg)
app.component('f-input', FInput)
app.component('f-select', FSelect)
app.component('alert-box', AlertBox)
app.component('search-bar', SearchBar)
app.component('booking-detail', BookingDetail)
app.component('lookup-panel', LookupPanel)

app.mount('#app')
