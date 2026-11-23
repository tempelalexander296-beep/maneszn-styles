import { RARITIES } from '../data/rarities.js';
import { wait, toast } from './fx.js';

/** Controls the fullscreen 3D showcase (lazy-loads the WebGL stage on first use). */
export class ShowcaseController {
  constructor({ background }) {
    this.el = document.getElementById('showcase');
    this.card = document.getElementById('reveal-card');
    this.background = background;
    this.stage = null;
    this.avatarOn = true; this.rotateOn = true;
    this.onClose = null;
    document.getElementById('sc-close').addEventListener('click', () => this.close());
    document.getElementById('sc-avatar').addEventListener('click', (e) => { this.avatarOn = !this.avatarOn; this.stage?.setAvatarVisible(this.avatarOn); e.currentTarget.textContent = this.avatarOn ? 'Avatar ausblenden' : 'Avatar einblenden'; });
    document.getElementById('sc-rotate').addEventListener('click', (e) => { this.rotateOn = !this.rotateOn; if (this.stage) this.stage.controls.autoRotate = this.rotateOn; e.currentTarget.textContent = this.rotateOn ? 'Rotation pausieren' : 'Rotation starten'; });
    document.getElementById('sc-4k').addEventListener('click', () => this.capture());
    addEventListener('keydown', (e) => { if (e.key === 'Escape' && !this.el.hidden) this.close(); });
  }

  async ensureStage() {
    if (this.stage) return this.stage;
    const { ShowcaseStage } = await import('../three/stage.js');
    this.stage = new ShowcaseStage(document.getElementById('stage'));
    return this.stage;
  }

  fillCard(reward) {
    const r = RARITIES[reward.rarity];
    this.card.style.setProperty('--rc', r.color);
    document.getElementById('rc-day').textContent = `Türchen ${reward.day} · ${reward.day}. Dezember`;
    document.getElementById('rc-rarity').textContent = r.label;
    document.getElementById('rc-name').textContent = reward.name;
    document.getElementById('rc-desc').textContent = reward.desc;
    document.getElementById('rc-type').textContent = reward.type;
  }

  async open(reward, { reveal = false } = {}) {
    this.fillCard(reward);
    this.card.classList.remove('show');
    this.el.classList.toggle('mythic', reward.rarity === 'mythic');
    document.getElementById('sc-close').textContent = reveal ? 'Einsammeln & schließen' : 'Schließen';
    this.el.hidden = false;
    this.background.paused = true;
    const stage = await this.ensureStage();
    stage.resize();
    stage.setAvatarVisible(this.avatarOn);
    const done = stage.show(reward, { reveal });
    stage.controls.autoRotate = this.rotateOn && !reveal;
    stage.start();
    await done;
    if (this.rotateOn) stage.controls.autoRotate = true;
    await wait(reveal ? 150 : 0);
    this.card.classList.add('show'); // step 6: name + rarity
    return new Promise((res) => { this.onClose = res; });
  }

  close() {
    if (this.el.hidden) return;
    this.el.hidden = true;
    this.stage?.stop(); this.stage?.clear();
    this.background.paused = false;
    this.onClose?.(); this.onClose = null;
  }

  async capture() {
    if (!this.stage) return;
    toast('Rendere 4K-Screenshot (3840 × 2160) …', 2000);
    await wait(30);
    const blob = await this.stage.capture4K();
    if (!blob) { toast('Screenshot fehlgeschlagen'); return; }
    const a = document.createElement('a');
    a.href = URL.createObjectURL(blob); a.download = `advent-cosmetic-${Date.now()}-4k.png`; a.click();
    setTimeout(() => URL.revokeObjectURL(a.href), 4000);
    toast('4K-Screenshot gespeichert ✦');
  }
}
