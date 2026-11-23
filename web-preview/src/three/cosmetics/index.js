import { snowflakeWings, candyCaneWings, frozenWings, goldenGiftWings } from './wings.js';
import { christmasTrail, iceCrystalTrail, santasSleighTrail } from './trails.js';
import { snowfallAura, winterStormAura } from './auras.js';
import { frozenCrown, reindeerHorns, christmasHalo, santaHat, legendaryIceCrown } from './headwear.js';
import { candyCaneSword, presentHammer } from './handheld.js';
import { snowGlobe, goldenBells, christmasLantern } from './floating.js';
import { reindeerCompanion, snowmanCompanion, christmasDragon } from './companions.js';
import { ultimateChristmasAura } from './ultimate.js';
import { giftBackpack } from './backbling.js';

/** reward id → builder. Every builder returns a fresh, self-animating cosmetic instance. */
export const COSMETIC_BUILDERS = Object.freeze({
  'snowflake-wings': snowflakeWings,
  'christmas-trail': christmasTrail,
  'candy-cane-sword': candyCaneSword,
  'snowfall-aura': snowfallAura,
  'gift-backpack': giftBackpack,
  'frozen-crown': frozenCrown,
  'reindeer-horns': reindeerHorns,
  'candy-cane-wings': candyCaneWings,
  'snow-globe': snowGlobe,
  'christmas-halo': christmasHalo,
  'santa-hat': santaHat,
  'ice-crystal-trail': iceCrystalTrail,
  'golden-bell': goldenBells,
  'present-hammer': presentHammer,
  'reindeer-companion': reindeerCompanion,
  'frozen-wings': frozenWings,
  'christmas-lantern': christmasLantern,
  'snowman-companion': snowmanCompanion,
  'golden-gift-wings': goldenGiftWings,
  'winter-storm-aura': winterStormAura,
  'santas-sleigh-trail': santasSleighTrail,
  'christmas-dragon': christmasDragon,
  'legendary-ice-crown': legendaryIceCrown,
  'ultimate-christmas-aura': ultimateChristmasAura,
});
