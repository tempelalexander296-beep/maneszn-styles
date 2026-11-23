package dev.maneszn.advent.render.cosmetics;

import static dev.maneszn.advent.mesh.Mat4.*;
import static dev.maneszn.advent.mesh.MeshBuilder.*;
import static dev.maneszn.advent.render.cosmetics.M.*;

import dev.maneszn.advent.mesh.Mat4;
import dev.maneszn.advent.mesh.Mesh;
import dev.maneszn.advent.mesh.MeshBuilder;
import dev.maneszn.advent.mesh.Tex;
import dev.maneszn.advent.mesh.V3;
import dev.maneszn.advent.render.*;
import java.util.List;
import java.util.Random;

/** Back-slot cosmetics. Space: origin on the upper back, x right, y up, -z away from the body. */
final class BackCosmetics {
	private BackCosmetics() {}

	static MeshBuilder.Curve arm(float span) {
		return new MeshBuilder.Curve(pts(0, 0, 0, .35f * span, .32f * span, -.04f, .8f * span, .55f * span, -.1f, 1.25f * span, .42f * span, -.14f));
	}
	static List<V3> armPts(float span) { return pts(0, 0, 0, .35f * span, .32f * span, -.04f, .8f * span, .55f * span, -.1f, 1.25f * span, .42f * span, -.14f); }

	/** Draws a mirrored pair of wings with a shared flap. */
	static void wings(Sink s, Wearer w, Prop wing, float flap, float speed, float sweep, float spread, Runnable perWing) {
		float f = sin(w.time * speed) * flap;
		for (int side = -1; side <= 1; side += 2) {
			s.push(); s.translate(side * spread, 0, 0);
			s.rotateY(side * (sweep + f)); s.rotateZ(side * cos(w.time * speed) * 0.04f);
			s.scale(side, 1, 1);
			wing.render(s); if (perWing != null) perWing.run();
			s.pop();
		}
	}

	/* Day 1 */
	static final class SnowflakeWings implements CosmeticModel {
		final Prop wing = new Prop(); final Mesh core = sphere(0.018f, 10, 8); final List<V3> cores = new java.util.ArrayList<>();
		SnowflakeWings() {
			wing.add(sweep(armPts(1), t -> lerp(0.04f, 0.012f, t), 48, 10, true), Tex.ICE, Layer.TRANSLUCENT);
			MeshBuilder.Curve arm = arm(1);
			float[][] fingers = { { 1f, -1.05f, 1f }, { .72f, -1.25f, .85f }, { .45f, -1.15f, .7f } };
			int seed = 3;
			for (float[] fg : fingers) {
				V3 root = arm.point(fg[0]), dir = V3.of(Math.cos(fg[1]), Math.sin(fg[1]), 0), end = root.add(dir.mul(fg[2]));
				V3 mid = root.lerp(end, .5f).add(V3.of(.08f, .05f, -.05f));
				List<V3> f = List.of(root, mid, end); MeshBuilder.Curve fc = new MeshBuilder.Curve(f);
				wing.add(sweep(f, t -> lerp(0.018f, 0.006f, t), 24, 8, true), Tex.ICE, Layer.TRANSLUCENT);
				for (int i = 0; i < 5; i++) {
					float t = 0.2f + i / 4f * 0.8f; V3 p = fc.point(t); float sc = lerp(0.2f, 0.11f, t) * (fg[0] > .9f ? 1.2f : 1f);
					wing.add(snowflake((seed++) % 3, 0.12f), trs(p.x, p.y, p.z, 0.1f, 0.2f, t * 2 + fg[0], sc, sc, sc), Tex.ICE, Layer.TRANSLUCENT);
					cores.add(p);
				}
			}
			for (int i = 0; i < 4; i++) { V3 p = arm.point(.25f + i * .22f).add(V3.of(0, -.12f, .02f)); float sc = .16f + i * .01f; wing.add(snowflake(i % 3, .12f), trs(p.x, p.y, p.z, 0, 0, i, sc, sc, sc), Tex.SILVER, Layer.SOLID); }
		}
		public void render(Sink s, Wearer w) {
			wings(s, w, wing, 0.14f, 1.3f, 0.38f, 0.07f, () -> { for (int i = 0; i < cores.size(); i++) { V3 p = cores.get(i); s.push(); s.translate(p.x, p.y, p.z); s.mesh(core, Tex.GLOW_ICE, Layer.GLOW, pulse(0xFFFFFF, .7f + .3f * sin(w.time * 3 + i))); s.pop(); } });
		}
		public void tickFx(Fx fx, Wearer w, Random r) { if (r.nextInt(2) == 0) { float sd = r.nextBoolean() ? 1 : -1; fx.spawn(Fx.Type.SNOW, sd * (.2f + r.nextFloat() * 1.1f), 1.0f + r.nextFloat() * 0.9f, -.35f, 0, -0.02f, 0); } }
	}

	/* Day 8 */
	static final class CandyCaneWings implements CosmeticModel {
		final Prop wing = new Prop(); final Mesh disc = cylinder(0.1f, 0.1f, 0.04f, 48, true); final V3[] discPos = new V3[3];
		CandyCaneWings() {
			wing.add(sweep(armPts(1.05f), t -> lerp(0.045f, 0.02f, t), 1f, 48, 12, true, 8), Tex.CANDY_RED);
			MeshBuilder.Curve arm = arm(1.05f);
			float[][] rows = { { 9, 1.05f, .55f, .03f, 0 }, { 7, .55f, .32f, .026f, .05f } };
			for (float[] row : rows) for (int i = 0; i < (int) row[0]; i++) {
				float t = i / (row[0] - 1); V3 p = arm.point(lerp(.15f, 1, t)).add(V3.of(0, 0, row[4]));
				wing.add(Parts.candyCane(lerp(row[2], row[1], t), row[3], row[4] == 0 ? Tex.CANDY_RED : Tex.CANDY_GREEN), translate(p.x, p.y, p.z).mul(rotZ(lerp(-2.95f, -1.85f, t))).mul(rotY(row[4] * 3)));
			}
			for (int i = 0; i < 3; i++) discPos[i] = arm.point(.12f + i * .4f).add(V3.of(0, 0, .06f));
		}
		public void render(Sink s, Wearer w) {
			wings(s, w, wing, 0.2f, 1.8f, 0.38f, 0.07f, () -> { for (int i = 0; i < 3; i++) { V3 p = discPos[i]; s.push(); s.translate(p.x, p.y, p.z); s.rotateX(PI / 2); s.rotateY(w.time * (0.8f + i * 0.3f)); s.scale(1 - i * 0.15f); s.mesh(disc, Tex.PEPPERMINT, Layer.SOLID); s.pop(); } });
		}
		public void tickFx(Fx fx, Wearer w, Random r) { if (r.nextInt(3) == 0) { float sd = r.nextBoolean() ? 1 : -1; fx.spawn(r.nextBoolean() ? Fx.Type.RED : Fx.Type.WHITE, sd * (.3f + r.nextFloat()), 0.8f + r.nextFloat() * 1.2f, -.35f, 0, -.02f, 0); } }
	}

	/* Day 16 */
	static float[][] shard(float len, float w) {
		return new float[][] { { 0, 0 }, { w * .7f, len * .15f }, { w, len * .45f }, { w * .65f, len * .72f }, { w * .2f, len * .95f }, { 0, len }, { -w * .15f, len * .9f }, { -w * .45f, len * .6f }, { -w * .5f, len * .3f }, { -w * .3f, len * .1f } };
	}

	static final class FrozenWings implements CosmeticModel {
		final Prop wing = new Prop(), veins = new Prop();
		FrozenWings() {
			float[][] sh = { { 1.55f, .32f, -.75f, .05f, .05f }, { 1.35f, .28f, -1.15f, .04f, 0 }, { 1.1f, .26f, -1.6f, .03f, -.05f }, { .95f, .22f, -2.05f, .02f, -.1f }, { .8f, .2f, -2.45f, .02f, -.15f }, { .75f, .16f, -.35f, .06f, .08f } };
			for (int i = 0; i < sh.length; i++) {
				float[] s = sh[i];
				Mat4 xf = translate(s[3], s[4], -i * .012f).mul(rotZ(s[2])).mul(rotY(.12f * i));
				wing.add(extrude(shard(s[0], s[1]), 0.03f), xf, Tex.ICE, Layer.TRANSLUCENT);
				List<V3> v = pts(0, .05f, .02f, s[1] * .15f, s[0] * .45f, .02f, .02f, s[0] * .9f, .02f);
				veins.add(sweep(v, t -> lerp(.008f, .002f, t), 30, 6, true), xf, Tex.GLOW_ICE, Layer.GLOW);
				MeshBuilder.Curve vc = new MeshBuilder.Curve(v);
				for (int k = 1; k <= 3; k++) { V3 p0 = vc.point(k * .25f); veins.add(sweep(List.of(p0, p0.add(V3.of(s[1] * .45f, s[0] * .12f, 0))), t -> lerp(.004f, .001f, t), 8, 5, true), xf, Tex.GLOW_ICE, Layer.GLOW); }
			}
			for (int i = 0; i < 5; i++) wing.add(octahedron(.06f + i * .012f), translate(.05f + i * .05f, .08f + i * .03f, .04f).mul(rotZ(-.4f - i * .2f)).mul(scale(1, 2.4f, 1)), Tex.ICE, Layer.TRANSLUCENT);
		}
		public void render(Sink s, Wearer w) {
			wings(s, w, wing, 0.1f, 1.0f, 0.3f, 0.07f, () -> veins.render(s));
		}
		public void tickFx(Fx fx, Wearer w, Random r) { float sd = r.nextBoolean() ? 1 : -1; float k = r.nextFloat(); fx.spawn(r.nextInt(3) == 0 ? Fx.Type.SNOW : Fx.Type.ICE, sd * (.15f + k * 1.2f), 1.6f - k * 1.2f, -.3f - k * .4f, 0, -.01f, 0); }
	}

	/* Day 19 */
	static final class GoldenGiftWings implements CosmeticModel {
		final Prop wing = new Prop(); final Prop[] gifts = new Prop[3]; final V3[] anchors = new V3[3]; final float[] lens = new float[3];
		final Mesh ribbon = cylinder(0.004f, 0.004f, 1, 6, false);
		GoldenGiftWings() {
			Random r = new Random(19);
			wing.add(sweep(armPts(1.1f), t -> lerp(.05f, .022f, t), 48, 12, true), Tex.GOLD);
			MeshBuilder.Curve arm = arm(1.1f);
			Object[][] rows = { { 12, 1.25f, .62f, .13f, Tex.FEATHER_GOLD, 0f, .12f }, { 10, .62f, .36f, .11f, Tex.ROSE_GOLD, .035f, .1f }, { 8, .32f, .2f, .09f, Tex.GOLD, .07f, .08f } };
			for (Object[] row : rows) {
				int n = (int) row[0];
				for (int i = 0; i < n; i++) {
					float t = i / (n - 1f), len = lerp((float) row[2], (float) row[1], t);
					V3 p = arm.point(lerp((float) row[6], 1, t)).add(V3.of(0, 0, (float) row[5]));
					Mat4 xf = translate(p.x, p.y, p.z).mul(rotZ(lerp(-2.9f, -1.75f, t))).mul(rotY((r.nextFloat() - .5f) * .08f));
					wing.add(Parts.feather(len, (float) row[3]), xf, (Tex) row[4]);
					if (row[4] == Tex.FEATHER_GOLD) wing.add(cylinder(.004f, .008f, len * .92f, 6, false), xf.mul(translate(0, len * .46f, .02f)), Tex.GLOW_GOLD, Layer.GLOW);
				}
			}
			Tex[][] looks = { { Tex.PAPER_RED_STARS, Tex.GOLD }, { Tex.PAPER_GREEN_DOTS, Tex.GOLD }, { Tex.PAPER_WHITE_FLAKES, Tex.SATIN_RED } };
			float[] at = { .55f, .78f, 1f };
			for (int i = 0; i < 3; i++) { anchors[i] = arm.point(at[i]).add(V3.of(0, 0, .12f)); lens[i] = .35f + i * .12f; gifts[i] = Parts.gift(.13f, .13f, .13f, looks[i][0], looks[i][1], true); }
			wing.add(Parts.bow(.2f, Tex.SATIN_RED), translate(.05f, .05f, .12f));
		}
		public void render(Sink s, Wearer w) {
			wings(s, w, wing, .16f, 1.4f, .35f, .07f, () -> {
				for (int i = 0; i < 3; i++) {
					s.push(); s.translate(anchors[i].x, anchors[i].y, anchors[i].z); s.rotateZ(sin(w.time * 2 + i * 1.3f) * .18f); s.rotateX(cos(w.time * 1.7f + i * 1.3f) * .1f);
					s.push(); s.translate(0, -lens[i] / 2, 0); s.scale(1, lens[i], 1); s.mesh(ribbon, Tex.SATIN_RED, Layer.SOLID); s.pop();
					s.translate(0, -lens[i] - .12f, 0); gifts[i].render(s); s.pop();
				}
			});
		}
		public void tickFx(Fx fx, Wearer w, Random r) { for (int i = 0; i < 2; i++) { float sd = r.nextBoolean() ? 1 : -1, k = r.nextFloat(); fx.spawn(Fx.Type.GOLD, sd * (.2f + k * 1.3f), 1.7f - k * 1.5f, -.3f - k * .5f, 0, -.03f, 0); } }
	}

	/* Day 5 */
	static final class GiftBackpack implements CosmeticModel {
		final Prop pack = new Prop(); final Prop charm = Parts.ornament(Tex.ORNAMENT_GOLD, 0.03f);
		GiftBackpack() {
			Mat4 base = translate(0, -0.2f, -0.07f);
			pack.add(Parts.gift(.34f, .4f, .2f, Tex.PAPER_RED_PLAID, Tex.GOLD, false), base.mul(translate(0, 0, -.1f)));
			pack.add(Parts.gift(.22f, .14f, .07f, Tex.PAPER_GREEN_DOTS, Tex.WHITE, false), base.mul(translate(0, .04f, -.235f)));
			pack.add(Parts.bow(.2f, Tex.GOLD), base.mul(translate(0, .42f, -.1f)));
			pack.add(Parts.holly(.12f), base.mul(trs(.12f, .4f, -.02f, -.3f, .4f, -.5f, 1, 1, 1)));
			for (int sd = -1; sd <= 1; sd += 2) {
				List<V3> c = pts(sd * .1f, .36f, -.02f, sd * .12f, .47f, .05f, sd * .13f, .4f, .2f, sd * .13f, .15f, .24f, sd * .12f, .02f, .12f, sd * .12f, .03f, -.02f);
				pack.add(sweep(c, t -> .022f, .28f, 64, 8, false, 6), base, Tex.SATIN_RED, Layer.SOLID);
				V3 b = new MeshBuilder.Curve(c).point(.55f);
				pack.add(roundedBox(.05f, .035f, .012f, .004f, 2), base.mul(translate(b.x, b.y, b.z + .01f)), Tex.GOLD);
			}
			pack.add(roundedBox(.07f, .045f, .004f, .002f, 2), base.mul(trs(-.13f, .27f, -.215f, 0, 0, .25f, 1, 1, 1)), Tex.PAPER_WHITE_FLAKES);
		}
		public void render(Sink s, Wearer w) {
			s.push(); s.translate(0, Math.abs(sin(w.time * 2)) * 0.008f, 0); pack.render(s);
			s.translate(.15f, 0, -.285f); s.rotateZ(sin(w.time * 2.4f) * .3f); charm.render(s); s.pop();
		}
		public void tickFx(Fx fx, Wearer w, Random r) { if (r.nextInt(8) == 0) fx.spawn(Fx.Type.GOLD, (r.nextFloat() - .5f) * .36f, 1.2f + r.nextFloat() * .45f, -.4f, 0, .01f, 0); }
	}
}
