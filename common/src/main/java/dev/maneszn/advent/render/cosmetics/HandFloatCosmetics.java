package dev.maneszn.advent.render.cosmetics;

import static dev.maneszn.advent.mesh.Mat4.*;
import static dev.maneszn.advent.mesh.MeshBuilder.*;
import static dev.maneszn.advent.render.cosmetics.M.*;

import dev.maneszn.advent.mesh.Mesh;
import dev.maneszn.advent.mesh.Tex;
import dev.maneszn.advent.render.*;
import java.util.Random;

/** Hand (origin = grip, +Y along the weapon) and floating (origin beside the shoulder) cosmetics. */
final class HandFloatCosmetics {
	private HandFloatCosmetics() {}

	/* Day 3 */
	static final class CandyCaneSword implements CosmeticModel {
		final Prop sword = new Prop();
		CandyCaneSword() {
			float[][] blade = { { -.045f, 0 }, { .045f, 0 }, { .05f, .6f }, { .038f, .72f }, { 0, .84f }, { -.038f, .72f }, { -.05f, .6f } };
			Mesh b = extrude(blade, .03f);
			float[] d = b.data.clone(); // pulled-sugar twist
			for (int i = 0; i < b.quads * 4; i++) { int o = i * Mesh.STRIDE; float a = d[o + 1] * 1.4f, x = d[o], z = d[o + 2]; d[o] = x * cos(a) - z * sin(a); d[o + 2] = x * sin(a) + z * cos(a); d[o + 6] = d[o] * 4 + d[o + 1] * 4; d[o + 7] = d[o + 1] * 4; }
			sword.add(Mesh.of(d, b.quads), translate(0, .1f, 0), Tex.CANDY_RED);
			sword.add(cylinder(.003f, .003f, .8f, 6, false), translate(0, .48f, 0), Tex.GLOW_WHITE, Layer.GLOW);
			for (int sd = -1; sd <= 1; sd += 2) sword.add(Parts.candyCane(.12f, .016f, Tex.CANDY_RED), translate(0, .09f, 0).mul(rotZ(sd * -PI / 2)).mul(scale(sd, 1, 1)));
			sword.add(ellipsoid(.042f, .028f, .025f, 20, 12), translate(0, .09f, 0), Tex.GOLD);
			sword.add(octahedron(.018f), translate(0, .09f, .024f), Tex.GEM_GREEN, Layer.GLOW);
			sword.add(Parts.holly(.09f), translate(.04f, .1f, .02f).mul(rotZ(-.6f)));
			sword.add(cylinder(.018f, .02f, .16f, 24, true), Tex.CANDY_GREEN);
			sword.add(Parts.ornament(Tex.ORNAMENT_RED, .03f), translate(0, -.1f, 0).mul(rotX(PI)));
		}
		public void render(Sink s, Wearer w) { s.push(); s.translate(0, sin(w.time * 1.4f) * 0.01f, 0); sword.render(s); s.pop(); }
		public void tickFx(Fx fx, Wearer w, Random r) {}
	}

	/* Day 14 */
	static final class PresentHammer implements CosmeticModel {
		final Prop hammer = new Prop(); final Mesh ring = torus(.3f, .006f, 120, 6, TAU);
		PresentHammer() {
			var head = translate(0, .62f, 0);
			hammer.add(Parts.gift(.36f, .22f, .22f, Tex.PAPER_GREEN_DOTS, Tex.SATIN_RED, false), head.mul(translate(0, -.11f, 0)));
			for (int sd = -1; sd <= 1; sd += 2) {
				hammer.add(roundedBox(.03f, .2f, .2f, .01f, 2), head.mul(translate(sd * .195f, 0, 0)), Tex.GOLD);
				hammer.add(ellipsoid(.015f, .03f, .03f, 16, 10), head.mul(translate(sd * .21f, 0, 0)), Tex.GOLD);
			}
			hammer.add(Parts.bow(.16f, Tex.SATIN_RED), head.mul(translate(0, .13f, 0)));
			hammer.add(cylinder(.022f, .026f, .62f, 24, true), translate(0, .2f, 0), Tex.CANDY_GOLD);
			for (float y : new float[] { -.11f, .49f }) hammer.add(cylinder(.032f, .032f, .03f, 24, true), translate(0, y, 0), Tex.GOLD);
			hammer.add(Parts.ornament(Tex.ORNAMENT_GOLD, .035f), translate(0, -.15f, 0).mul(rotX(PI)));
		}
		public void render(Sink s, Wearer w) {
			hammer.render(s);
			s.push(); s.translate(0, .62f, 0); s.rotateX(PI / 2.3f); s.rotateZ(w.time * 2); s.mesh(ring, Tex.GLOW_GOLD, Layer.GLOW); s.pop();
		}
	}

	/* Day 9 */
	static final class SnowGlobe implements CosmeticModel {
		final Prop globe = new Prop(); final Mesh flake = octahedron(0.006f); final float[][] snow = new float[60][4];
		SnowGlobe() {
			float R = .17f, gy = R * .85f, baseY = -.1f + gy - R * .75f;
			globe.add(lathe(new float[][] { { 0, 0 }, { .16f, 0 }, { .17f, .015f }, { .155f, .03f }, { .14f, .06f }, { .13f, .09f }, { .145f, .1f }, { 0, .1f } }, 64), translate(0, baseY, 0), Tex.WOOD);
			globe.add(torus(.163f, .006f, 96, 8, TAU), translate(0, baseY + .002f, 0).mul(rotX(PI / 2)), Tex.GOLD);
			globe.add(torus(.143f, .006f, 96, 8, TAU), translate(0, baseY + .098f, 0).mul(rotX(PI / 2)), Tex.GOLD);
			globe.add(box(.08f, .03f, .004f), translate(0, baseY + .05f, .14f).mul(rotX(-.25f)), Tex.GOLD);
			float sy = baseY + .1f;
			globe.add(cylinder(.13f, .13f, .02f, 48, true), translate(0, sy, 0), Tex.SNOW_GROUND);
			globe.add(Parts.tree(.16f, 9, true), translate(-.035f, sy + .01f, -.02f));
			globe.add(Parts.tree(.09f, 4, false), translate(.075f, sy + .01f, -.04f));
			var house = translate(.045f, sy + .01f, .045f).mul(rotY(-.5f));
			globe.add(box(.05f, .035f, .04f), house.mul(translate(0, .0175f, 0)), Tex.WOOD);
			globe.add(cylinder(0.0001f, .04f, .03f, 4, true), house.mul(translate(0, .05f, 0)).mul(rotY(PI / 4)), Tex.SNOW);
			globe.add(box(.012f, .012f, .001f), house.mul(translate(0, .018f, .0205f)), Tex.FLAME, Layer.GLOW);
			globe.add(sphere(R, 48, 32), translate(0, gy, 0), Tex.GLASS, Layer.TRANSLUCENT);
			Random r = new Random(9); for (float[] f : snow) { f[0] = r.nextFloat() * TAU; f[1] = r.nextFloat() * R * .8f; f[2] = r.nextFloat(); f[3] = .3f + r.nextFloat(); }
		}
		public void render(Sink s, Wearer w) {
			float t = w.time; s.push(); s.translate(0, sin(t * 1.3f) * .03f, 0); s.rotateY(sin(t * .4f) * .6f);
			globe.render(s, Layer.SOLID); globe.render(s, Layer.GLOW);
			float gy = .17f * .85f;
			for (float[] f : snow) { float a = f[0] + t * f[3], y = ((f[2] - t * .06f * f[3]) % 1 + 1) % 1; s.push(); s.translate(cos(a) * f[1], gy - .12f + y * .26f, sin(a) * f[1]); s.mesh(flake, Tex.WHITE, Layer.GLOW); s.pop(); }
			globe.render(s, Layer.TRANSLUCENT); s.pop();
		}
	}

	/* Day 13 */
	static final class GoldenBells implements CosmeticModel {
		final Prop top = new Prop(), bell = Parts.bell(Tex.GOLD); final Mesh rib = box(.012f, 1, .003f);
		final float[][] bells = { { -.07f, .2f, .09f, 0 }, { .07f, .26f, .1f, 1.3f }, { 0, .32f, .12f, 2.1f } };
		GoldenBells() { top.add(Parts.bow(.12f, Tex.SATIN_RED)).add(Parts.holly(.12f), translate(0, -.02f, -.02f)); }
		public void render(Sink s, Wearer w) {
			float t = w.time; s.push(); s.translate(0, .15f + sin(t * 1.2f) * .025f, 0); s.rotateY(sin(t * .5f) * .5f); top.render(s);
			for (float[] b : bells) {
				s.push(); s.translate(b[0], -.01f, 0); s.rotateZ(sin(t * 2.4f + b[3]) * .28f); s.rotateX(cos(t * 1.9f + b[3]) * .08f);
				s.push(); s.translate(0, -b[1] / 2, 0); s.scale(1, b[1], 1); s.mesh(rib, Tex.SATIN_RED, Layer.SOLID); s.pop();
				s.translate(0, -b[1] - b[2], 0); s.scale(b[2]); bell.render(s); s.pop();
			}
			s.pop();
		}
		public void tickFx(Fx fx, Wearer w, Random r) { if (r.nextInt(4) == 0) fx.spawn(Fx.Type.GOLD, .58f + (r.nextFloat() - .5f) * .2f, 1.45f + r.nextFloat() * .2f, .12f, 0, -.01f, 0); }
	}

	/* Day 17 */
	static final class ChristmasLantern implements CosmeticModel {
		final Prop lantern = new Prop(); final Mesh flame = ellipsoid(.012f, .03f, .012f, 16, 10), halo = sphere(.05f, 16, 10);
		ChristmasLantern() {
			float W = .12f, H = .2f;
			lantern.add(cylinder(W * .85f, W * .95f, .025f, 4, true), rotY(PI / 4), Tex.DARK_METAL);
			lantern.add(cylinder(W * .85f, W * .95f, .025f, 4, true), translate(0, H, 0).mul(rotY(PI / 4)), Tex.DARK_METAL);
			for (int i = 0; i < 4; i++) {
				float a = i / 4f * TAU + PI / 4, pa = i / 4f * TAU;
				lantern.add(cylinder(.006f, .006f, H, 8, false), translate(cos(a) * W * .58f, H / 2, sin(a) * W * .58f), Tex.GOLD);
				lantern.add(box(W * .78f, H * .92f, .002f), translate(cos(pa) * W * .41f, H / 2, sin(pa) * W * .41f).mul(rotY(-pa + PI / 2)), Tex.GLASS, Layer.TRANSLUCENT);
				lantern.add(box(W * .78f, .004f, .004f), translate(cos(pa) * W * .41f, H * .55f, sin(pa) * W * .41f).mul(rotY(-pa + PI / 2)), Tex.DARK_METAL);
			}
			lantern.add(cone(W * .95f, .09f, 4), translate(0, H + .055f, 0).mul(rotY(PI / 4)), Tex.DARK_METAL);
			lantern.add(cone(W * .82f, .05f, 4), translate(0, H + .08f, 0).mul(rotY(PI / 4)), Tex.SNOW);
			lantern.add(torus(.025f, .005f, 32, 8, TAU), translate(0, H + .13f, 0), Tex.GOLD);
			lantern.add(Parts.holly(.1f), translate(0, H + .04f, W * .4f).mul(rotX(-.6f)));
			lantern.add(Parts.bow(.06f, Tex.SATIN_RED), translate(0, H + .02f, W * .48f));
			lantern.add(cylinder(.02f, .022f, .08f, 24, true), translate(0, .055f, 0), Tex.CANDLE);
		}
		public void render(Sink s, Wearer w) {
			float t = w.time, fl = .85f + sin(t * 17) * .07f + sin(t * 31) * .05f;
			s.push(); s.translate(0, -.1f + sin(t * 1.3f) * .025f, 0); s.rotateZ(sin(t * 1.1f) * .06f); s.rotateY(t * .2f);
			lantern.render(s, Layer.SOLID);
			s.push(); s.translate(0, .115f, 0); s.scale(1, fl, 1); s.mesh(flame, Tex.FLAME, Layer.GLOW); s.mesh(halo, Tex.GLOW_GOLD, Layer.GLOW, alpha(0xFFFFFF, .35f * fl)); s.pop();
			lantern.render(s, Layer.TRANSLUCENT); s.pop();
		}
		public void tickFx(Fx fx, Wearer w, Random r) { if (r.nextInt(10) == 0) fx.spawn(Fx.Type.GLOW, .58f, 1.85f, .12f, 0, .02f, 0); }
	}
}
