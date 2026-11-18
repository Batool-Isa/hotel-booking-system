const COLORS = ['#febb02', '#006ce4', '#003b95', '#008234', '#ffffff', '#ff7a59']

function progressBar() {
  const bar = document.createElement('div')
  bar.className = 'fx-progress'
  document.body.appendChild(bar)
  const top = document.createElement('button')
  top.className = 'fx-top'
  top.type = 'button'
  top.setAttribute('aria-label', 'Back to top')
  top.textContent = '↑'
  top.addEventListener('click', () => window.scrollTo({ top: 0, behavior: 'smooth' }))
  document.body.appendChild(top)
  let ticking = false
  const update = () => {
    const max = document.documentElement.scrollHeight - window.innerHeight
    const y = window.scrollY
    bar.style.transform = 'scaleX(' + (max > 0 ? Math.min(1, y / max) : 0) + ')'
    top.classList.toggle('show', y > 600)
    ticking = false
  }
  window.addEventListener('scroll', () => {
    if (!ticking) {
      ticking = true
      requestAnimationFrame(update)
    }
  }, { passive: true })
  window.addEventListener('resize', update)
  update()
}

export function confetti() {
  if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) return
  const canvas = document.createElement('canvas')
  canvas.className = 'fx-confetti'
  canvas.width = window.innerWidth
  canvas.height = window.innerHeight
  document.body.appendChild(canvas)
  const ctx = canvas.getContext('2d')
  const pieces = Array.from({ length: 140 }, () => ({
    x: canvas.width / 2 + (Math.random() - 0.5) * 160,
    y: canvas.height * 0.35,
    vx: (Math.random() - 0.5) * 16,
    vy: -Math.random() * 14 - 4,
    size: Math.random() * 7 + 4,
    rot: Math.random() * Math.PI,
    vr: (Math.random() - 0.5) * 0.3,
    color: COLORS[Math.floor(Math.random() * COLORS.length)]
  }))
  const started = performance.now()
  const frame = (now) => {
    const t = now - started
    ctx.clearRect(0, 0, canvas.width, canvas.height)
    pieces.forEach((p) => {
      p.vy += 0.38
      p.vx *= 0.99
      p.x += p.vx
      p.y += p.vy
      p.rot += p.vr
      ctx.save()
      ctx.translate(p.x, p.y)
      ctx.rotate(p.rot)
      ctx.globalAlpha = Math.max(0, 1 - t / 3200)
      ctx.fillStyle = p.color
      ctx.fillRect(-p.size / 2, -p.size / 4, p.size, p.size / 2)
      ctx.restore()
    })
    if (t < 3200) requestAnimationFrame(frame)
    else canvas.remove()
  }
  requestAnimationFrame(frame)
}

export function initEffects() {
  progressBar()
  let last = ''
  const check = () => {
    const h = location.hash
    if (/\/payment\/success/.test(h) && h !== last) setTimeout(confetti, 700)
    last = h
  }
  window.addEventListener('hashchange', check)
  check()
}
