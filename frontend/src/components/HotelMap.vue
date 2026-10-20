<script>
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';

export default {
  props:{ pins:{ type:Array, default:()=>[] }, height:{ type:[Number,String], default:460 }, hi:{ default:null } },
  data(){ return { ok:true, missing:0 }; },
  mounted(){
    try{
      this.map = L.map(this.$refs.el,{ scrollWheelZoom:false }).setView([26.2,50.55],10);
    }catch{ this.ok=false; return; }
    L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png',{ maxZoom:19, attribution:'&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>' }).addTo(this.map);
    this.layer = L.layerGroup().addTo(this.map); this.draw(true);
  },
  beforeUnmount(){ if(this.map){ this.map.remove(); this.map=null; } },
  watch:{ pins:{ deep:true, handler(){ this.draw(true); } }, hi(){ this.draw(false); } },
  methods:{
    esc(t){ return String(t==null?'':t).replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c])); },
    draw(fit){
      if(!this.map) return; this.layer.clearLayers(); const pts=[]; let miss=0;
      for(const p of this.pins){
        const la=Number(p.lat), lo=Number(p.lng);
        if(p.lat==null||p.lng==null||Number.isNaN(la)||Number.isNaN(lo)){ miss++; continue; }
        const icon = L.divIcon({ className:'', html:`<div class="vs-pin${this.hi===p.id?' hi':''}">${this.esc(p.label||p.name)}</div>`, iconSize:[0,0] });
        const m = L.marker([la,lo],{ icon, title:p.name }).addTo(this.layer);
        m.bindPopup(`<div class="pop"><h4>${this.esc(p.name)}</h4><div class="s">${this.esc(p.sub||'')}</div><a class="go" href="${this.esc(p.href)}">View hotel</a></div>`);
        pts.push([la,lo]);
      }
      this.missing=miss;
      if(fit && pts.length){ pts.length===1 ? this.map.setView(pts[0],14) : this.map.fitBounds(pts,{ padding:[50,50], maxZoom:15 }); }
      setTimeout(()=>this.map&&this.map.invalidateSize(),50);
    }
  }
};
</script>

<template>
  <div>
    <div v-if="!ok" class="hmap hmap-note">The map could not be loaded. Check your internet connection and reload the page.</div>
    <div v-else ref="el" class="hmap" :style="{height:(typeof height==='number'?height+'px':height)}" role="application" aria-label="Map of hotels"></div>
    <div v-if="ok && missing" class="muted xs" style="margin-top:6px">{{missing}} hotel(s) are not shown because they have no map location yet.</div>
  </div>
</template>
