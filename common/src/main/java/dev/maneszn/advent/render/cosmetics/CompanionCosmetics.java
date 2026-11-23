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

/** Companions float beside the player (origin = companion anchor next to the left shoulder). */
final class CompanionCosmetics {
	private CompanionCosmetics() {}

	static Prop eye(float r) {
		return new Prop().add(sphere(r, 16, 12), Tex.BLACK).add(sphere(r * .3f, 8, 6), translate(r * .35f, r * .4f, r * .75f), Tex.GLOW_WHITE, Layer.GLOW);
	}

	/* Day 15 */
	static final class Reindeer implements CosmeticModel {
		final Prop body = new Prop(), head = new Prop(), leg = new Prop(); final Mesh nose = sphere(.022f, 20, 14), noseGlow = sphere(.05f, 14, 10);
		Reindeer() {
			body.add(ellipsoid(.145f, .095f, .09f, 32, 20), Tex.FUR_BROWN);
			body.add(ellipsoid(.12f, .064f, .068f, 24, 14), translate(0, -.03f, 0), Tex.CREAM);
			body.add(sweep(pts(.1f, .03f, 0, .14f, .1f, 0, .16f, .16f, 0), t -> lerp(.05f, .04f, t), 12, 16, false), Tex.FUR_BROWN);
			body.add(torus(.048f, .016f, 32, 12, TAU), trs(.14f, .12f, 0, PI / 2, .5f, 0, 1, 1, 1), Tex.KNIT_RED);
			body.add(Parts.bell(Tex.GOLD), translate(.175f, .09f, 0).mul(rotZ(PI)).mul(scale(.025f, .025f, .025f)));
			body.add(sphere(.025f, 12, 8), translate(-.15f, .04f, 0), Tex.CREAM);
			head.add(ellipsoid(.072f, .065f, .062f, 28, 18), Tex.FUR_BROWN);
			head.add(ellipsoid(.058f, .036f, .04f, 20, 12), translate(.06f, -.025f, 0), Tex.CREAM);
			for (int z = -1; z <= 1; z += 2) {
				head.add(eye(.012f), translate(.035f, .02f, z * .045f).mul(rotY(z * .4f)));
				head.add(cone(.02f, .06f, 12), trs(-.02f, .05f, z * .055f, z * .9f, 0, -.4f, 1, 1, .4f), Tex.FUR_BROWN);
				head.add(Parts.antler(.12f, z > 0 ? 3 : 8, z, Tex.ANTLER), translate(-.02f, .05f, z * .03f).mul(rotY(-PI / 2)));
			}
			leg.add(sweep(pts(0, 0, 0, 0, -.06f, .01f, 0, -.12f, 0), t -> lerp(.022f, .012f, t), 12, 10, true), Tex.FUR_BROWN);
			leg.add(cylinder(.012f, .015f, .02f, 12, true), translate(0, -.125f, 0), Tex.BLACK);
		}
		public void render(Sink s, Wearer w) {
			float t = w.time, a = t * .7f;
			s.push(); s.translate(cos(a) * .12f, sin(t * 2.4f) * .05f, sin(a) * .12f); s.rotateY(-a); s.rotateZ(sin(t * 4.8f) * .06f);
			body.render(s);
			float[][] legs = { { .08f, .06f, 0 }, { .08f, -.06f, 0 }, { -.08f, .06f, PI }, { -.08f, -.06f, PI } };
			for (float[] l : legs) { s.push(); s.translate(l[0], -.04f, l[1]); s.rotateZ(sin(t * 9.6f + l[2]) * .6f); leg.render(s); s.pop(); }
			s.push(); s.translate(.17f, .19f, 0); s.rotateZ(sin(t * 4.8f + 1) * .08f); head.render(s);
			s.translate(.115f, -.02f, 0); s.mesh(nose, Tex.GLOW_RED, Layer.GLOW); s.mesh(noseGlow, Tex.GLOW_RED, Layer.GLOW, alpha(0xFFFFFF, .25f + .1f * sin(t * 3))); s.pop();
			s.pop();
		}
		public void tickFx(Fx fx, Wearer w, Random r) { if (r.nextInt(3) == 0) fx.spawn(r.nextBoolean() ? Fx.Type.GOLD : Fx.Type.SPARKLE, -.72f + (r.nextFloat() - .5f) * .3f, 1.15f, .22f, 0, -.02f, 0); }
	}

	/* Day 18 */
	static final class Snowman implements CosmeticModel {
		final Prop lower = new Prop(), upper = new Prop(), head = new Prop(), arm = new Prop();
		Snowman() {
			lower.add(lumpySphere(.11f, 40, 28, .03f, 1), translate(0, .1f, 0), Tex.SNOW);
			upper.add(lumpySphere(.08f, 36, 24, .03f, 2), translate(0, .06f, 0), Tex.SNOW);
			for (int i = 0; i < 3; i++) upper.add(sphere(.01f, 10, 8), translate(0, .08f - i * .035f, .077f), Tex.COAL);
			upper.add(torus(.058f, .016f, 40, 12, TAU), translate(0, .13f, 0).mul(rotX(PI / 2)), Tex.KNIT_GREEN);
			upper.add(box(.03f, .09f, .012f), translate(.035f, .085f, .055f).mul(rotZ(.2f)), Tex.KNIT_GREEN);
			head.add(lumpySphere(.06f, 32, 22, .03f, 3), Tex.SNOW);
			for (int x = -1; x <= 1; x += 2) head.add(eye(.009f), translate(x * .022f, .015f, .053f));
			head.add(cone(.012f, .07f, 16), translate(0, 0, .088f).mul(rotX(PI / 2)), Tex.CARROT);
			for (int i = 0; i < 5; i++) { float a = -.6f + i * .3f; head.add(sphere(.006f, 8, 6), translate(sin(a) * .04f, -.025f - cos(a) * .008f, .048f), Tex.COAL); }
			var hat = translate(0, .05f, 0).mul(rotZ(.15f));
			head.add(cylinder(.06f, .06f, .008f, 40, true), hat, Tex.SATIN_BLACK);
			head.add(cylinder(.038f, .04f, .065f, 40, true), hat.mul(translate(0, .035f, 0)), Tex.SATIN_BLACK);
			head.add(cylinder(.0405f, .0405f, .014f, 40, false), hat.mul(translate(0, .012f, 0)), Tex.SATIN_RED);
			head.add(Parts.holly(.06f), hat.mul(translate(.03f, .02f, .03f)).mul(rotY(.8f)));
			List<V3> c = pts(0, 0, 0, .06f, .03f, 0, .12f, .05f, .01f);
			arm.add(sweep(c, t -> lerp(.006f, .003f, t), 12, 6, true), Tex.BARK);
			for (float k : new float[] { .6f, .85f }) { V3 p = new dev.maneszn.advent.mesh.MeshBuilder.Curve(c).point(k); arm.add(sweep(List.of(p, p.add(V3.of(.025f, .03f, 0))), t -> .0025f, 4, 5, true), Tex.BARK); }
		}
		public void render(Sink s, Wearer w) {
			float t = w.time, cyc = (t * 1.4f) % 1, hop = sin(cyc * PI), sq = cyc < .08f || cyc > .92f ? .9f : 1.03f;
			s.push(); s.translate(0, -.2f + hop * .12f, 0); s.rotateY(sin(t * .5f) * .5f + .3f); s.scale(1 / (float) Math.sqrt(sq), sq, 1 / (float) Math.sqrt(sq));
			lower.render(s); s.translate(0, .19f, 0); upper.render(s);
			for (int sd = -1; sd <= 1; sd += 2) { s.push(); s.translate(sd * .07f, .08f, 0); s.rotateZ(sd > 0 ? .5f + sin(t * 7) * .45f : -(.2f - sin(t * 1.4f) * .1f)); s.scale(sd, 1, 1); arm.render(s); s.pop(); }
			s.translate(0, .18f, 0); s.rotateZ(sin(t * 1.4f) * .12f); s.rotateY(sin(t * .7f) * .4f); head.render(s);
			s.pop();
		}
		public void tickFx(Fx fx, Wearer w, Random r) { if (((w.time * 1.4f) % 1) < .05f) for (int i = 0; i < 4; i++) fx.spawn(Fx.Type.SNOW, -.72f + (r.nextFloat() - .5f) * .2f, 1.1f, .22f + (r.nextFloat() - .5f) * .2f, (r.nextFloat() - .5f) * .1f, .05f, (r.nextFloat() - .5f) * .1f); }
	}

	/* Day 22 */
	static final class Dragon implements CosmeticModel {
		static final int SEG = 22;
		final Mesh seg = sphere(1, 20, 14), spike = cone(.4f, 1.1f, 8), tip = cone(.018f, .07f, 14), membrane;
		final Prop head = new Prop(), bones = new Prop();
		Dragon() {
			head.add(ellipsoid(.06f, .05f, .052f, 28, 20), Tex.SCALES);
			head.add(ellipsoid(.06f, .028f, .034f, 20, 12), translate(.055f, -.012f, 0), Tex.SCALES);
			head.add(ellipsoid(.05f, .016f, .028f, 20, 10), translate(.05f, -.03f, 0), Tex.BELLY_GOLD);
			for (int z = -1; z <= 1; z += 2) {
				head.add(sphere(.012f, 12, 8), translate(.03f, .02f, z * .038f), Tex.GLOW_ICE, Layer.GLOW);
				head.add(sweep(pts(0, 0, 0, -.03f, .03f, z * .01f, -.08f, .04f, z * .02f), t -> lerp(.011f, .002f, t), 16, 8, true), translate(-.01f, .04f, z * .025f), Tex.GOLD);
				head.add(sweep(pts(0, 0, 0, .02f, -.02f, z * .04f, -.02f, -.05f, z * .08f), t -> lerp(.003f, .001f, t), 16, 5, true), translate(.09f, -.01f, z * .02f), Tex.GOLD);
			}
			var hat = translate(-.005f, .048f, 0).mul(rotZ(.3f));
			head.add(sweep(pts(0, 0, 0, 0, .04f, 0, -.03f, .06f, 0), t -> lerp(.032f, .004f, t), 24, 16, true), hat, Tex.VELVET_RED);
			head.add(torus(.032f, .009f, 32, 10, TAU), hat.mul(rotX(PI / 2)), Tex.FUR);
			head.add(sphere(.011f, 12, 8), hat.mul(translate(-.032f, .06f, 0)), Tex.FUR);
			float[][] w = { { 0, 0 }, { .12f, .04f }, { .09f, .12f }, { .04f, .17f }, { -.03f, .2f }, { -.04f, .1f } };
			membrane = extrude(w, .004f);
			for (float[] b : new float[][] { { .12f, .04f }, { .04f, .17f }, { -.03f, .2f } }) bones.add(sweep(List.of(V3.of(0, 0, 0), V3.of(b[0], b[1], 0)), t -> lerp(.005f, .0015f, t), 8, 6, true), Tex.GOLD);
		}
		static V3 path(float t) { return V3.of(sin(t * .6f) * .28f, sin(t * 1.2f) * .1f + .05f, sin(t * 1.2f) * .16f); }
		static void look(Sink s, V3 from, V3 to) {
			V3 d = to.sub(from); float yaw = (float) Math.atan2(-d.z, d.x), pitch = (float) Math.atan2(d.y, Math.hypot(d.x, d.z));
			s.rotateY(yaw); s.rotateZ(pitch);
		}
		public void render(Sink s, Wearer w) {
			float t = w.time;
			for (int i = 0; i < SEG; i++) {
				float lag = i * .075f, k = i / (SEG - 1f), r = lerp(.045f, .008f, (float) Math.pow(k, .9f)) * (i < 3 ? 1 - (3 - i) * .08f : 1);
				V3 p = path(t - lag); p.y += sin(t * 5 - i * .55f) * .018f; V3 q = path(t - lag + .02f);
				s.push(); s.translate(p.x, p.y, p.z); look(s, p, q);
				s.push(); s.scale(r * 1.3f, r, r); s.mesh(seg, Tex.SCALES, Layer.SOLID); s.pop();
				s.push(); s.translate(0, -r * .4f, 0); s.scale(r * 1.2f, r * .6f, r * .9f); s.mesh(seg, Tex.BELLY_GOLD, Layer.SOLID); s.pop();
				if (i % 2 == 0 && i < SEG - 2) { s.push(); s.translate(0, r * 1.05f, 0); s.rotateZ(-.35f); s.scale(r); s.mesh(spike, Tex.GOLD, Layer.SOLID); s.pop(); }
				if (i == 3) for (int z = -1; z <= 1; z += 2) {
					s.push(); s.translate(0, r * .7f, 0); s.rotateX(z * (.5f + sin(t * 8) * .6f) + z * PI / 2 * 0); s.scale(1, 1, z);
					s.rotateX(-PI / 2); s.mesh(membrane, Tex.MEMBRANE_RED, Layer.TRANSLUCENT, 0xE0FFFFFF); bones.render(s); s.pop();
				}
				s.pop();
			}
			V3 e = path(t - SEG * .075f), e2 = path(t - SEG * .075f + .05f);
			s.push(); s.translate(e.x, e.y, e.z); look(s, e, e2); s.rotateZ(PI / 2); s.mesh(tip, Tex.CANDY_RED, Layer.SOLID); s.pop();
			V3 h = path(t + .06f), h2 = path(t + .12f); h.y += .02f;
			s.push(); s.translate(h.x, h.y, h.z); look(s, h, h2); head.render(s); s.pop();
		}
		public void tickFx(Fx fx, Wearer w, Random r) {
			float cyc = w.time % 3.5f;
			if (cyc < .9f) { V3 h = path(w.time + .06f), d = path(w.time + .12f).sub(h).norm().mul(.25f); for (int i = 0; i < 3; i++) fx.spawn(r.nextBoolean() ? Fx.Type.SNOW : Fx.Type.ICE, -.72f + h.x, 1.3f + h.y, .22f + h.z, d.x + (r.nextFloat() - .5f) * .05f, d.y, d.z); }
			if (r.nextInt(3) == 0) fx.spawn(r.nextBoolean() ? Fx.Type.GOLD : Fx.Type.GREEN, -.72f + (r.nextFloat() - .5f) * .5f, 1.3f, .22f, 0, -.02f, 0);
		}
	}
}
