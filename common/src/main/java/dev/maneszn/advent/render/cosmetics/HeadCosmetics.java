package dev.maneszn.advent.render.cosmetics;

import static dev.maneszn.advent.mesh.Mat4.*;
import static dev.maneszn.advent.mesh.MeshBuilder.*;
import static dev.maneszn.advent.render.cosmetics.M.*;

import dev.maneszn.advent.mesh.Mesh;
import dev.maneszn.advent.mesh.Tex;
import dev.maneszn.advent.mesh.V3;
import dev.maneszn.advent.render.*;
import java.util.List;
import java.util.Random;

/** Head-slot cosmetics. Space: origin = head centre, head radius ≈ 0.125 (adapter scales to the MC head). */
final class HeadCosmetics {
	private HeadCosmetics() {}

	/* Day 6 */
	static final class FrozenCrown implements CosmeticModel {
		final Prop crown = new Prop(); final Mesh gem = octahedron(0.011f); final V3[] gemPos = new V3[11];
		FrozenCrown() {
			crown.add(cylinder(0.128f, 0.122f, 0.05f, 64, false), translate(0, 0.075f, 0), Tex.ICE, Layer.TRANSLUCENT);
			for (float y : new float[] { -0.026f, 0.026f }) crown.add(torus(0.127f, 0.007f, 96, 10, TAU), translate(0, 0.075f + y, 0).mul(rotX(PI / 2)), Tex.SILVER);
			for (int i = 0; i < 11; i++) {
				float a = i / 11f * TAU + PI / 2, h = 0.06f + (float) Math.pow(Math.max(0, Math.sin(a)), 3) * 0.09f + (i % 2) * 0.025f;
				crown.add(crystal(6, h, 0.017f, 0.035f), trs(cos(a) * 0.125f, 0.095f, sin(a) * 0.125f, sin(a) * 0.18f, 0, -cos(a) * 0.18f, 1, 1, 1), Tex.ICE, Layer.TRANSLUCENT);
				gemPos[i] = V3.of(cos(a) * 0.131f, 0.075f, sin(a) * 0.131f);
			}
			crown.add(octahedron(0.026f), translate(0, 0.08f, 0.135f).mul(scale(1, 1.4f, 1)), Tex.GEM_BLUE, Layer.GLOW);
		}
		public void render(Sink s, Wearer w) {
			crown.render(s);
			for (int i = 0; i < 11; i++) { s.push(); s.translate(gemPos[i].x, gemPos[i].y, gemPos[i].z); s.mesh(gem, Tex.GLOW_ICE, Layer.GLOW, pulse(0xFFFFFF, 0.6f + 0.4f * sin(w.time * 3 + i))); s.pop(); }
		}
		public void tickFx(Fx fx, Wearer w, Random r) { if (r.nextInt(6) == 0) fx.spawn(Fx.Type.SNOW, (r.nextFloat() - .5f) * .5f, 2.05f, (r.nextFloat() - .5f) * .5f, 0, 0.02f, 0); }
	}

	/* Day 7 */
	static final class ReindeerHorns implements CosmeticModel {
		final Prop base = new Prop(); @SuppressWarnings("unchecked") final List<V3>[] bulbs = new List[2]; final Mesh bulb = sphere(0.009f, 8, 6);
		ReindeerHorns() {
			List<V3> band = new java.util.ArrayList<>();
			for (int i = 0; i <= 12; i++) { double a = Math.PI * i / 12; band.add(V3.of(Math.cos(a) * 0.132, 0.02 + Math.sin(a) * 0.12, -0.01)); }
			base.add(sweep(band, t -> 0.011f, 0.5f, 48, 10, true, 4), Tex.SATIN_RED);
			for (int k = 0; k < 2; k++) {
				int side = k == 0 ? 1 : -1;
				base.add(Parts.antler(0.26f, side > 0 ? 7 : 11, side, Tex.ANTLER), translate(side * 0.1f, 0.1f, -0.01f).mul(rotZ(-side * 0.15f)));
				base.add(Parts.bell(Tex.GOLD), translate(side * 0.13f, 0.05f, 0.03f).mul(rotX(PI)).mul(scale(0.035f, 0.035f, 0.035f)));
				base.add(Parts.holly(0.08f), trs(side * 0.12f, 0.1f, 0.03f, 0.3f, side * 0.5f, side * -0.6f, 1, 1, 1));
				List<V3> l = new java.util.ArrayList<>();
				for (int i = 0; i < 14; i++) { float t = i / 14f, ang = t * TAU * 3; V3 p = V3.of(side * t * 0.11f + cos(ang) * 0.02f, t * 0.24f + sin(ang) * 0.02f, sin(ang) * 0.02f); l.add(rotZ(-side * 0.15f).point(p.x, p.y, p.z).add(V3.of(side * 0.1f, 0.1f, -0.01f))); }
				bulbs[k] = l;
			}
		}
		public void render(Sink s, Wearer w) {
			base.render(s);
			Tex[] cols = { Tex.GLOW_GOLD, Tex.GLOW_RED, Tex.GLOW_ICE, Tex.GLOW_GREEN };
			for (List<V3> l : bulbs) for (int i = 0; i < l.size(); i++) { V3 p = l.get(i); s.push(); s.translate(p.x, p.y, p.z); s.mesh(bulb, cols[i % 4], Layer.GLOW, pulse(0xFFFFFF, 0.35f + 0.65f * (0.5f + 0.5f * sin(w.time * 2.4f + i * 1.7f)))); s.pop(); }
		}
	}

	/* Day 10 */
	static final class ChristmasHalo implements CosmeticModel {
		final Prop ring = new Prop(); final Mesh star = star(5, 0.022f, 0.01f, 0.008f);
		ChristmasHalo() {
			ring.add(torus(0.19f, 0.016f, 128, 16, TAU), rotX(PI / 2), Tex.GOLD);
			ring.add(torus(0.19f, 0.006f, 128, 8, TAU), translate(0, -0.012f, 0).mul(rotX(PI / 2)), Tex.GLOW_GOLD, Layer.GLOW);
			for (int i = 0; i < 24; i++) { float a = i / 24f * TAU; ring.add(torus(0.012f, 0.0035f, 12, 6, TAU), translate(cos(a) * 0.19f, 0, sin(a) * 0.19f).mul(rotY(-a)), Tex.GOLD); }
			for (int i = 0; i < 4; i++) { float a = i / 4f * TAU; ring.add(Parts.holly(0.11f), trs(cos(a) * 0.19f, 0.01f, sin(a) * 0.19f, -PI / 2 + 0.3f, 0, -a + PI / 2, 1, 1, 1)); }
		}
		public void render(Sink s, Wearer w) {
			s.push(); s.translate(0, 0.25f + sin(w.time * 1.6f) * 0.012f, 0); s.rotateY(w.time * 0.4f);
			ring.render(s);
			for (int i = 0; i < 8; i++) {
				float a = i / 8f * TAU + w.time * 0.9f; s.push(); s.translate(cos(a) * 0.24f, sin(w.time * 2 + i) * 0.02f, sin(a) * 0.24f); s.rotateY(w.time * 3);
				s.scale(0.8f + sin(w.time * 5 + i * 2) * 0.35f); s.mesh(star, Tex.GLOW_GOLD, Layer.GLOW); s.pop();
			}
			s.pop();
		}
		public void tickFx(Fx fx, Wearer w, Random r) { if (r.nextInt(4) == 0) { float a = r.nextFloat() * TAU; fx.spawn(Fx.Type.GOLD, cos(a) * 0.38f, 2.32f, sin(a) * 0.38f, 0, -0.01f, 0); } }
	}

	/* Day 11 */
	static final class SantaHat implements CosmeticModel {
		final Prop hat = new Prop(); final Mesh pom; final V3 tip;
		SantaHat() {
			List<V3> c = pts(0, 0, 0, 0, .1f, -.01f, .015f, .19f, -.03f, .06f, .25f, -.07f, .13f, .24f, -.12f, .18f, .17f, -.15f, .2f, .1f, -.16f);
			var xf = translate(0, 0.04f, 0).mul(rotX(-0.12f));
			hat.add(sweep(c, t -> lerp(0.138f, 0.012f, (float) Math.pow(t, 0.85)) * (1 + sin(t * 40) * 0.015f * (1 - t)), 1f, 120, 40, true, 3), xf, Tex.VELVET_RED, Layer.SOLID);
			hat.add(torus(0.14f, 0.036f, 128, 24, TAU).transform(scale(1, 1, 0.8f)), xf.mul(rotX(PI / 2)), Tex.FUR, Layer.SOLID);
			hat.add(snowflake(1, 0.4f), xf.mul(translate(0, 0.075f, 0.122f)).mul(rotX(-0.15f)).mul(scale(0.035f, 0.035f, 0.035f)), Tex.GOLD, Layer.SOLID);
			pom = lumpySphere(0.045f, 32, 20, 0.16f, 4);
			tip = xf.point(0.2f, 0.1f, -0.16f);
		}
		public void render(Sink s, Wearer w) {
			hat.render(s);
			s.push(); s.translate(tip.x + sin(w.time * 2) * 0.006f, tip.y + sin(w.time * 2.6f) * 0.004f, tip.z); s.mesh(pom, Tex.FUR, Layer.SOLID); s.pop();
		}
	}

	/* Day 23 */
	static final class LegendaryIceCrown implements CosmeticModel {
		final Prop crown = new Prop(); final Mesh shard = crystal(4, 0.05f, 0.012f, 0.025f), beam = box(0.09f, 0.32f, 0.001f), jewel = octahedron(0.024f);
		LegendaryIceCrown() {
			var up = translate(0, 0.075f, 0);
			crown.add(cylinder(0.132f, 0.126f, 0.06f, 96, false), up, Tex.GOLD);
			for (float y : new float[] { -0.03f, 0.03f }) crown.add(torus(0.131f, 0.008f, 128, 10, TAU), up.mul(translate(0, y, 0)).mul(rotX(PI / 2)), Tex.GOLD);
			for (int i = 0; i < 32; i++) { float a = i / 32f * TAU; crown.add(torus(0.008f, 0.0025f, 10, 5, TAU), up.mul(translate(cos(a) * 0.134f, 0, sin(a) * 0.134f)).mul(rotY(-a)), Tex.GOLD); }
			for (int i = 0; i < 16; i++) { float a = i / 16f * TAU; crown.add(octahedron(0.011f), up.mul(translate(cos(a) * 0.137f, 0, sin(a) * 0.137f)), i % 2 == 0 ? Tex.GEM_BLUE : Tex.GEM_RED, Layer.GLOW); }
			for (int i = 0; i < 10; i++) {
				float a = i / 10f * TAU + PI / 2, fb = (float) Math.pow(Math.max(0, Math.sin(a)), 2);
				V3 b = V3.of(cos(a) * 0.132f, 0.03f, sin(a) * 0.132f);
				if (i % 2 == 0) {
					crown.add(crystal(6, 0.13f + fb * 0.08f, 0.02f, 0.05f), up.mul(trs(b.x, b.y, b.z, sin(a) * 0.2f, 0, -cos(a) * 0.2f, 1, 1, 1)), Tex.ICE, Layer.TRANSLUCENT);
					crown.add(cylinder(0.026f, 0.018f, 0.03f, 6, true), up.mul(translate(b.x, 0.04f, b.z)), Tex.GOLD);
				} else {
					V3 top = b.mul(1.12f); top.y = 0.11f + fb * 0.04f; V3 mid = b.mul(0.92f); mid.y = 0.07f;
					crown.add(sweep(List.of(b, mid, top), t -> lerp(0.009f, 0.004f, t), 18, 8, true), up, Tex.GOLD);
					crown.add(sphere(0.011f, 12, 8), up.mul(translate(top.x, top.y, top.z)), Tex.GOLD);
				}
			}
			crown.add(star(8, 0.045f, 0.02f, 0.014f), up.mul(translate(0, 0.1f, 0.15f)), Tex.STAR_GOLD);
		}
		public void render(Sink s, Wearer w) {
			float t = w.time;
			crown.render(s);
			s.push(); s.translate(0, 0.175f, 0.162f); s.rotateY(t * 1.5f); s.mesh(jewel, Tex.GEM_BLUE, Layer.GLOW, pulse(0xFFFFFF, 0.75f + 0.25f * sin(t * 3))); s.pop();
			for (int i = 0; i < 6; i++) { float a = i / 6f * TAU + t * 0.8f; s.push(); s.translate(cos(a) * 0.22f, 0.125f + (i % 2) * 0.05f, sin(a) * 0.22f); s.rotateY(t * 2 + i); s.mesh(shard, Tex.ICE, Layer.TRANSLUCENT); s.pop(); }
			for (int i = 0; i < 3; i++) { s.push(); s.translate(0, 0.3f, 0.162f); s.rotateY(t * 0.3f + i / 3f * PI); s.mesh(beam, Tex.BEAM, Layer.GLOW, alpha(0xBFE9FF, 0.25f + sin(t * 2) * 0.1f)); s.pop(); }
		}
		public void tickFx(Fx fx, Wearer w, Random r) { if (r.nextInt(2) == 0) { float a = r.nextFloat() * TAU; fx.spawn(r.nextBoolean() ? Fx.Type.GOLD : Fx.Type.ICE, cos(a) * .3f, 2.1f, sin(a) * .3f, 0, 0.03f, 0); } }
	}
}
