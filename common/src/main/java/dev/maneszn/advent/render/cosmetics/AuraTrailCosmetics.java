package dev.maneszn.advent.render.cosmetics;

import static dev.maneszn.advent.mesh.Mat4.*;
import static dev.maneszn.advent.mesh.MeshBuilder.*;
import static dev.maneszn.advent.render.cosmetics.M.*;

import dev.maneszn.advent.mesh.Mesh;
import dev.maneszn.advent.mesh.Tex;
import dev.maneszn.advent.mesh.V3;
import dev.maneszn.advent.render.*;
import java.util.Random;

/** Auras and trails (origin = feet, y up, z forward). */
final class AuraTrailCosmetics {
	private AuraTrailCosmetics() {}

	static final Mesh RING = box(1, 0.001f, 1);

	static void groundRing(Sink s, Tex rune, float size, float rot, float a) {
		s.push(); s.translate(0, 0.02f, 0); s.rotateY(rot); s.scale(size, 1, size); s.mesh(RING, rune, Layer.GLOW, alpha(0xFFFFFF, a)); s.pop();
	}

	/** Showcase/trail path around the feet. */
	static V3 orbit(float t, float r) { float a = t * 1.25f; return V3.of(cos(a) * r, .45f + sin(t * 2.5f) * .22f, sin(a) * r); }

	/* Day 4 */
	static final class SnowfallAura implements CosmeticModel {
		final Mesh[] flakes = { snowflake(0, .06f), snowflake(1, .06f), snowflake(2, .06f) }; final float[][] d = new float[30][6];
		SnowfallAura() { Random r = new Random(4); for (float[] f : d) { f[0] = .55f + r.nextFloat() * .5f; f[1] = .15f + r.nextFloat() * 1.9f; f[2] = r.nextFloat() * TAU; f[3] = .25f + r.nextFloat() * .35f; f[4] = .04f + r.nextFloat() * .05f; f[5] = r.nextFloat() * TAU; } }
		public void render(Sink s, Wearer w) {
			float t = w.time;
			for (int i = 0; i < d.length; i++) {
				float[] f = d[i]; float a = f[2] + t * f[3];
				s.push(); s.translate(cos(a) * f[0], f[1] + sin(t * 1.2f + f[5]) * .08f, sin(a) * f[0]); s.rotateY(-a); s.rotateX(sin(t + f[5]) * .6f); s.rotateZ(t * .8f + f[5]); s.scale(f[4]);
				s.mesh(flakes[i % 3], Tex.SILVER, Layer.SOLID); s.pop();
			}
			groundRing(s, Tex.RUNE_ICE, 2.4f, t * .15f, .55f + sin(t * 1.5f) * .2f);
		}
		public void tickFx(Fx fx, Wearer w, Random r) { for (int i = 0; i < 2; i++) { float a = r.nextFloat() * TAU, rr = .3f + r.nextFloat(); fx.spawn(Fx.Type.SNOW, cos(a) * rr, 2.4f, sin(a) * rr, 0, -.05f, 0); } }
	}

	/* Day 20 */
	static final class WinterStormAura implements CosmeticModel {
		final Mesh shard = crystal(5, .16f, .03f, .06f), streak = box(.012f, .012f, .35f);
		public void render(Sink s, Wearer w) {
			float t = w.time;
			for (int i = 0; i < 14; i++) {
				float r = .75f + (i % 4) * .12f, h = .3f + i / 14f * 1.8f, a = i / 14f * TAU + t * (2 + (i % 3) * .6f);
				s.push(); s.translate(cos(a) * r, h + sin(t * 2 + i) * .1f, sin(a) * r); s.rotateY(-a); s.rotateZ(t * 3 + i); s.mesh(shard, Tex.ICE, Layer.TRANSLUCENT); s.pop();
			}
			for (int i = 0; i < 24; i++) {
				float a = i / 24f * TAU + t * 3.2f, r = .8f + (i % 3) * .15f, h = ((t * .6f + i * .37f) % 2.2f);
				s.push(); s.translate(cos(a) * r, .2f + h, sin(a) * r); s.rotateY(-a); s.mesh(streak, Tex.GLOW_WHITE, Layer.GLOW, alpha(0xE8F8FF, .55f)); s.pop();
			}
			groundRing(s, Tex.RUNE_ICE, 3f, -t * .4f, .7f);
		}
		public void tickFx(Fx fx, Wearer w, Random r) {
			for (int i = 0; i < 5; i++) { float a = r.nextFloat() * TAU, rr = .6f + r.nextFloat() * .7f, sp = .35f; fx.spawn(i % 3 == 0 ? Fx.Type.CLOUD : Fx.Type.SNOW, cos(a) * rr, r.nextFloat() * 2.2f, sin(a) * rr, -sin(a) * sp, .05f, cos(a) * sp); }
			if (r.nextInt(60) == 0) fx.spawn(Fx.Type.ELECTRIC, 0, 2.3f, 0, 0, 0, 0);
		}
	}

	/* Day 2 */
	static final class ChristmasTrail implements CosmeticModel {
		final Mesh star = star(5, .07f, .03f, .025f);
		public void render(Sink s, Wearer w) {
			V3 p = orbit(w.time, w.preview ? 1.15f : .55f);
			s.push(); s.translate(p.x, w.preview ? p.y : .12f, p.z); s.rotateY(w.time * 4); s.rotateZ(w.time * 2); s.mesh(star, Tex.GLOW_GOLD, Layer.GLOW); s.pop();
		}
		public void tickFx(Fx fx, Wearer w, Random r) {
			int n = w.walkSpeed > .05f ? 4 : 1;
			for (int i = 0; i < n; i++) { float x = (r.nextFloat() - .5f) * .4f; fx.spawn(i % 3 == 0 ? Fx.Type.RED : i % 3 == 1 ? Fx.Type.GREEN : Fx.Type.GOLD, x, .1f + r.nextFloat() * .2f, -.25f - r.nextFloat() * .2f, 0, .02f, 0); }
		}
	}

	/* Day 12 */
	static final class IceCrystalTrail implements CosmeticModel {
		final Mesh gem = octahedron(.07f), crystal = crystal(6, .22f, .04f, .07f);
		public void render(Sink s, Wearer w) {
			float t = w.time; V3 p = orbit(t, w.preview ? 1.15f : .5f);
			s.push(); s.translate(p.x, w.preview ? p.y : .15f, p.z); s.rotateY(t * 3); s.rotateX(t * 1.3f); s.mesh(gem, Tex.GEM_BLUE, Layer.GLOW); s.pop();
			for (int i = 0; i < 5; i++) { // crystals sprouting around the feet and melting again
				float ph = (t * .6f + i / 5f) % 1, k = ph < .2f ? ph / .2f : ph < .8f ? 1 : (1 - ph) / .2f, a = i / 5f * TAU + 1;
				s.push(); s.translate(cos(a) * .45f, 0, sin(a) * .45f); s.rotateZ(sin(a) * .4f); s.rotateX(cos(a) * .4f); s.scale(Math.max(.001f, k)); s.mesh(crystal, Tex.ICE, Layer.TRANSLUCENT); s.pop();
			}
		}
		public void tickFx(Fx fx, Wearer w, Random r) {
			int n = w.walkSpeed > .05f ? 4 : 1;
			for (int i = 0; i < n; i++) fx.spawn(i % 2 == 0 ? Fx.Type.ICE : Fx.Type.SNOW, (r.nextFloat() - .5f) * .4f, .05f + r.nextFloat() * .3f, -.25f - r.nextFloat() * .2f, 0, .01f, 0);
			if (n > 1 && r.nextInt(3) == 0) fx.spawn(Fx.Type.END_ROD, 0, .1f, -.3f, 0, .01f, 0);
		}
	}

	/* Day 21 */
	static final class SleighTrail implements CosmeticModel {
		final Prop sleigh = new Prop(); final Mesh guide = star(5, .06f, .025f, .02f);
		SleighTrail() {
			float[][] side = { { -.22f, .04f }, { .16f, .04f }, { .25f, .08f }, { .27f, .2f }, { .2f, .22f }, { .18f, .16f }, { .02f, .13f }, { -.14f, .2f }, { -.24f, .22f }, { -.26f, .15f } };
			sleigh.add(extrude(side, .18f), Tex.SATIN_RED);
			for (int z = -1; z <= 1; z += 2) {
				sleigh.add(sweep(pts(-.24f, 0, z * .08f, .18f, 0, z * .08f, .28f, .03f, z * .08f, .31f, .1f, z * .08f, .26f, .14f, z * .08f, .23f, .1f, z * .08f), t -> .008f, 60, 8, true), Tex.GOLD);
				for (float x : new float[] { -.14f, .08f }) sleigh.add(cylinder(.006f, .006f, .05f, 6, false), translate(x, .025f, z * .08f), Tex.GOLD);
				sleigh.add(torus(.115f, .007f, 48, 8, PI), translate(-.02f, .13f, z * .1f).mul(scale(1.6f, .5f, 1)), Tex.GOLD);
			}
			sleigh.add(lumpySphere(.09f, 24, 16, .08f, 3).transform(scale(1, 1.1f, .9f)), translate(-.13f, .2f, 0), Tex.FUR_BROWN);
			sleigh.add(Parts.gift(.07f, .07f, .07f, Tex.PAPER_GREEN_DOTS, Tex.GOLD, true), translate(-.04f, .13f, .03f).mul(rotY(.5f)));
			sleigh.add(Parts.gift(.06f, .06f, .06f, Tex.PAPER_WHITE_FLAKES, Tex.SATIN_RED, true), translate(-.08f, .27f, -.02f).mul(rotZ(.3f)));
			sleigh.add(sphere(.02f, 12, 8), translate(.27f, .22f, 0), Tex.GLOW_GOLD, Layer.GLOW);
		}
		public void render(Sink s, Wearer w) {
			float t = w.time, r = w.preview ? 1.15f : .9f; V3 p = orbit(t, r), q = orbit(t + .02f, r);
			s.push(); s.translate(p.x, w.preview ? p.y : .35f + sin(t * 2.5f) * .08f, p.z);
			V3 d = q.sub(p); s.rotateY((float) Math.atan2(-d.z, d.x)); s.rotateX(sin(t * 3) * .05f); s.scale(1.15f); sleigh.render(s); s.pop();
			V3 g = orbit(t + .45f, r);
			s.push(); s.translate(g.x, (w.preview ? g.y : .45f) + .12f, g.z); s.rotateY(t * 3); s.mesh(guide, Tex.GLOW_GOLD, Layer.GLOW); s.pop();
		}
		public void tickFx(Fx fx, Wearer w, Random r) {
			float rr = w.preview ? 1.15f : .9f; V3 p = orbit(w.time - .05f, rr);
			for (int i = 0; i < 3; i++) fx.spawn(i == 0 ? Fx.Type.FIREWORK : Fx.Type.GOLD, p.x, .35f, p.z, 0, .01f, 0);
		}
	}
}
