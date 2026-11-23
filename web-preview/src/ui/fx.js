/** DOM snowflake burst around an element (step 2 of the opening animation). */
export function snowBurst(el, count = 26, color = '#ffffff') {
  if (matchMedia('(prefers-reduced-motion: reduce)').matches) return;
  const r = el.getBoundingClientRect();
  const cx = r.left + r.width / 2, cy = r.top + r.height / 2;
  const glyphs = ['❄', '❅', '❆', '✦'];
  for (let i = 0; i < count; i++) {
    const f = document.createElement('span');
    f.className = 'fx-flake'; f.textContent = glyphs[i % glyphs.length]; f.style.color = i % 3 ? '#ffffff' : color;
    f.style.left = `${cx}px`; f.style.top = `${cy}px`;
    f.style.fontSize = `${10 + Math.random() * 16}px`;
    document.body.appendChild(f);
    const a = Math.random() * Math.PI * 2, d = r.width * (0.5 + Math.random() * 0.9);
    f.animate([
      { transform: 'translate(-50%,-50%) scale(0.2) rotate(0deg)', opacity: 0 },
      { transform: `translate(calc(-50% + ${Math.cos(a) * d * 0.6}px), calc(-50% + ${Math.sin(a) * d * 0.6}px)) scale(1) rotate(120deg)`, opacity: 1, offset: 0.4 },
      { transform: `translate(calc(-50% + ${Math.cos(a) * d}px), calc(-50% + ${Math.sin(a) * d + 40}px)) scale(0.6) rotate(240deg)`, opacity: 0 },
    ], { duration: 1200 + Math.random() * 600, easing: 'cubic-bezier(.2,.8,.2,1)' }).onfinish = () => f.remove();
  }
}

export const wait = (ms) => new Promise((r) => setTimeout(r, ms));

let toastTimer;
export function toast(msg, ms = 3200) {
  const el = document.getElementById('toast');
  el.textContent = msg; el.classList.add('show');
  clearTimeout(toastTimer); toastTimer = setTimeout(() => el.classList.remove('show'), ms);
}
