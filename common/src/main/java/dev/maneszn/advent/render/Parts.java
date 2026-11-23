package dev.maneszn.advent.render;

import static dev.maneszn.advent.mesh.Mat4.*;
import static dev.maneszn.advent.mesh.MeshBuilder.*;

import dev.maneszn.advent.mesh.Mat4;
import dev.maneszn.advent.mesh.Mesh;
import dev.maneszn.advent.mesh.MeshBuilder;
import dev.maneszn.advent.mesh.Tex;
import dev.maneszn.advent.mesh.V3;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Reusable high-detail Christmas props (meters, y-up). */
public final class Parts {
	private Parts() {}

	public static Mesh candyCaneMesh(float length, float radius) {
		List<V3> pts = new ArrayList<>();
		for (int i = 0; i <= 8; i++) pts.add(new V3(0, i / 8f * length, 0));
		float hook = length * 0.22f;
		for (int i = 1; i <= 12; i++) { double a = i / 12.0 * Math.PI * 1.08; pts.add(V3.of(hook - Math.cos(a) * hook, length + Math.sin(a) * hook, 0)); }
		return sweep(pts, t -> radius, 1f, 44, 10, true, length * 7);
	}

	public static Prop candyCane(float length, float radius, Tex stripes) { return new Prop().add(candyCaneMesh(length, radius), stripes); }

	public static Prop bow(float size, Tex ribbon) {
		Prop p = new Prop();
		Mesh loop = torus(0.5f, 0.16f, 22, 8, TAU).transform(scale(1, 0.55f, 0.35f));
		for (int s = -1; s <= 1; s += 2) {
			p.add(loop, trs(s * 0.45f, 0.22f, 0, 0, s * 0.25f, s * 0.45f, 1, 1, 1), ribbon, Layer.SOLID);
			p.add(box(0.22f, 0.75f, 0.05f), trs(s * 0.28f, -0.1f, 0.15f, 0.6f, 0, s * 0.5f, 1, 1, 1), ribbon, Layer.SOLID);
		}
		p.add(ellipsoid(0.2f, 0.17f, 0.16f, 14, 8), translate(0, 0.2f, 0), ribbon, Layer.SOLID);
		Prop out = new Prop(); out.add(p, scale(size, size, size)); return out;
	}

	/** Gift box (origin at bottom centre). */
	public static Prop gift(float w, float h, float d, Tex paper, Tex ribbon, boolean withBow) {
		Prop p = new Prop(); float r = Math.min(w, Math.min(h, d)) * 0.06f;
		p.add(roundedBox(w, h * 0.82f, d, r, 1), translate(0, h * 0.41f, 0), paper);
		p.add(roundedBox(w * 1.06f, h * 0.2f, d * 1.06f, r, 1), translate(0, h * 0.86f, 0), paper);
		float bw = Math.min(w, d) * 0.16f;
		p.add(box(bw, h * 1.0f, d * 1.085f), translate(0, h * 0.48f, 0), ribbon);
		p.add(box(w * 1.085f, h * 1.0f, bw), translate(0, h * 0.48f, 0), ribbon);
		if (withBow) p.add(bow(Math.min(w, d) * 0.32f, ribbon), translate(0, h * 0.97f, 0));
		return p;
	}

	public static Prop ornament(Tex ball, float r) {
		return new Prop().add(sphere(r, 18, 12), ball).add(cylinder(r * .25f, r * .28f, r * .25f, 14, true), translate(0, r, 0), Tex.GOLD)
			.add(torus(r * .12f, r * .03f, 16, 6, TAU), translate(0, r * 1.2f, 0), Tex.GOLD);
	}

	public static Mesh bellMesh() {
		return lathe(new float[][] { { 0, 1 }, { .12f, .99f }, { .22f, .94f }, { .28f, .82f }, { .31f, .62f }, { .36f, .4f }, { .46f, .2f }, { .58f, .06f }, { .62f, 0 }, { .6f, -.03f }, { .54f, 0 }, { .42f, .12f }, { .32f, .36f }, { .27f, .6f }, { .24f, .8f }, { .17f, .9f }, { 0, .92f } }, 32);
	}

	public static Prop bell(Tex metal) {
		return new Prop().add(bellMesh(), metal).add(torus(.09f, .03f, 20, 8, TAU), translate(0, 1.04f, 0), metal)
			.add(sphere(.1f, 16, 10), translate(0, .06f, 0), metal).add(torus(.6f, .025f, 48, 8, TAU), rotX((float) Math.PI / 2), metal);
	}

	public static Mesh hollyLeaf() {
		float[][] pts = { { 0, 0 }, { .12f, .1f }, { .2f, .08f }, { .18f, .22f }, { .26f, .28f }, { .17f, .38f }, { .2f, .52f }, { .08f, .5f }, { 0, .66f }, { -.08f, .5f }, { -.2f, .52f }, { -.17f, .38f }, { -.26f, .28f }, { -.18f, .22f }, { -.2f, .08f }, { -.12f, .1f } };
		return extrude(pts, 0.02f);
	}

	public static Prop holly(float s) {
		Prop p = new Prop(); Mesh leaf = hollyLeaf();
		p.add(leaf, rotZ(-0.7f).mul(rotX(-0.3f)), Tex.LEAF); p.add(leaf, rotZ(0.7f).mul(rotX(-0.3f)), Tex.LEAF); p.add(leaf, rotZ((float) Math.PI).mul(rotX(0.3f)), Tex.HOLLY_DARK);
		Mesh berry = sphere(0.05f, 14, 10);
		p.add(berry, translate(0, .02f, .05f), Tex.BERRY); p.add(berry, translate(.055f, -.02f, .04f), Tex.BERRY); p.add(berry, translate(-.055f, -.02f, .04f), Tex.BERRY);
		Prop out = new Prop(); out.add(p, scale(s, s, s)); return out;
	}

	/** Decorated Christmas tree (origin at trunk base). */
	public static Prop tree(float height, long seed, boolean ornaments) {
		Prop p = new Prop(); Random r = new Random(seed);
		for (int i = 0; i < 5; i++) {
			float t = i / 5f, radius = height * (0.42f - t * 0.065f) * (1 - t * 0.35f), h = height * 0.34f;
			Mesh cone = jaggedCone(radius, h, 24, i);
			float y = height * (0.22f + t * 0.15f) + h * 0.5f;
			p.add(cone, translate(0, y, 0), Tex.PINE);
			p.add(cylinder(0.0001f, radius * 0.72f, h * 0.35f, 24, false), translate(0, y + h * 0.33f, 0), Tex.SNOW);
		}
		p.add(cylinder(height * .05f, height * .065f, height * .25f, 12, true), translate(0, height * .12f, 0), Tex.BARK);
		p.add(star(5, height * .09f, height * .04f, height * .028f), translate(0, height * 1.06f, 0), Tex.GLOW_GOLD, Layer.GLOW);
		if (ornaments) {
			Tex[] cols = { Tex.ORNAMENT_RED, Tex.ORNAMENT_GOLD, Tex.ORNAMENT_SILVER, Tex.ORNAMENT_GREEN };
			Mesh ball = sphere(height * 0.032f, 12, 8);
			for (int i = 0; i < 16; i++) {
				float t = 0.12f + r.nextFloat() * 0.75f, a = r.nextFloat() * TAU, rad = height * 0.4f * (1 - t) * 0.95f;
				p.add(ball, translate((float) Math.cos(a) * rad, height * (0.28f + t * 0.7f), (float) Math.sin(a) * rad), cols[i % 4], Layer.SOLID);
			}
		}
		return p;
	}

	/** Fairy-light bulbs along a spiral (rendered separately so they can blink). */
	public static List<V3> spiral(float height, float r0, float r1, float turns, int count, float y0) {
		List<V3> l = new ArrayList<>();
		for (int i = 0; i < count; i++) { float t = (float) i / count, a = t * TAU * turns, r = r0 + (r1 - r0) * t; l.add(V3.of(Math.cos(a) * r, y0 + t * height, Math.sin(a) * r)); }
		return l;
	}

	private static Mesh jaggedCone(float radius, float h, int seg, int seed) {
		MeshBuilder b = MeshBuilder.create(); Random r = new Random(seed * 31L + 7);
		V3 top = V3.of(0, h / 2, 0);
		V3[] ring = new V3[seg];
		for (int i = 0; i < seg; i++) {
			float a = (float) i / seg * TAU, j = 1 + 0.18f * (float) Math.sin(a * 11 + seed) + (r.nextFloat() - 0.5f) * 0.12f;
			ring[i] = V3.of(Math.sin(a) * radius * j, -h / 2 - r.nextFloat() * 0.03f, Math.cos(a) * radius * j);
		}
		for (int i = 0; i < seg; i++) {
			V3 a = ring[i], c = ring[(i + 1) % seg];
			V3 mid = a.lerp(c, .5f).lerp(top, 0.18f).add(V3.of(0, -0.02f, 0));
			b.triFlat(top, a, c, .5f, 0, 0, 1, 1, 1);
			b.triFlat(a, mid, c, 0, 1, .5f, .8f, 1, 1);
		}
		return b.build();
	}

	/** Branching antler; returns the prop (origin at base). */
	public static Prop antler(float s, long seed, int mirror, Tex tex) {
		Prop p = new Prop(); Random r = new Random(seed);
		branch(p, r, V3.of(0, 0, 0), V3.of(0.35f * mirror, 1, -0.1f).norm(), 0.9f, 0.07f, 2, mirror, tex);
		Prop out = new Prop(); out.add(p, scale(s, s, s)); return out;
	}

	private static void branch(Prop p, Random r, V3 start, V3 dir, float len, float rad, int depth, int mirror, Tex tex) {
		List<V3> pts = new ArrayList<>(); pts.add(start); V3 d = dir.copy(), q = start;
		for (int i = 1; i <= 5; i++) {
			Mat4 rz = rotZ(-0.12f * mirror); d = rz.dir(d.x, d.y, d.z).norm(); d.y += 0.05f; q = q.add(d.norm().mul(len / 5)); pts.add(q);
		}
		p.add(sweep(pts, t -> rad + (rad * 0.35f - rad) * t, 14, 8, true), tex);
		if (depth <= 0) return;
		MeshBuilder.Curve c = new MeshBuilder.Curve(pts);
		int count = depth == 2 ? 3 : 2;
		for (int i = 0; i < count; i++) {
			float t = 0.35f + (float) i / count * 0.55f; V3 at = c.point(t); V3 nd = c.tangent(t);
			nd = rotZ((0.7f + r.nextFloat() * 0.3f) * mirror).dir(nd.x, nd.y, nd.z); nd.z += (r.nextFloat() - 0.5f) * 0.6f;
			branch(p, r, at, nd.norm(), len * (0.45f + r.nextFloat() * 0.15f), rad * 0.6f, depth - 1, mirror, tex);
		}
	}

	/** Feather blade (extruded, slightly curved), pointing +Y. */
	public static Mesh feather(float len, float w) {
		int n = 14; float[][] poly = new float[n * 2][];
		for (int i = 0; i < n; i++) {
			float t = (float) i / (n - 1), width = (float) Math.sin(Math.min(1, t * 1.15f) * Math.PI) * w * (1 - t * 0.25f) + 0.004f;
			poly[i] = new float[] { width * 0.85f, t * len }; poly[n * 2 - 1 - i] = new float[] { -width, t * len };
		}
		Mesh m = extrude(poly, 0.012f);
		float[] d = m.data.clone();
		for (int i = 0; i < m.quads * 4; i++) { int o = i * Mesh.STRIDE; float y = d[o + 1] / len; d[o + 2] -= y * y * 0.08f; }
		return Mesh.of(d, m.quads);
	}
}
