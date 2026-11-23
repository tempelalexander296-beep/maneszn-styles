package dev.maneszn.advent.mesh;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/** Procedural high-detail geometry: lathes, sweeps, tori, spheres, extrusions, stars, snowflakes … */
public final class MeshBuilder {
	public static final float TAU = (float) (Math.PI * 2);
	private float[] buf = new float[4096];
	private int n; // floats written
	private int quads;

	public static MeshBuilder create() { return new MeshBuilder(); }

	private void v(float x, float y, float z, float nx, float ny, float nz, float u, float vv) {
		if (n + Mesh.STRIDE > buf.length) buf = java.util.Arrays.copyOf(buf, buf.length * 2);
		buf[n++] = x; buf[n++] = y; buf[n++] = z; buf[n++] = nx; buf[n++] = ny; buf[n++] = nz; buf[n++] = u; buf[n++] = vv;
	}
	private void v(V3 p, V3 nrm, float u, float vv) { v(p.x, p.y, p.z, nrm.x, nrm.y, nrm.z, u, vv); }

	/** Quad a-b-c-d counter-clockwise seen from outside. */
	public MeshBuilder quad(V3 a, V3 b, V3 c, V3 d, V3 na, V3 nb, V3 nc, V3 nd, float[] uv) {
		v(a, na, uv[0], uv[1]); v(b, nb, uv[2], uv[3]); v(c, nc, uv[4], uv[5]); v(d, nd, uv[6], uv[7]); quads++; return this;
	}
	public MeshBuilder tri(V3 a, V3 b, V3 c, V3 nrm, float ua, float va, float ub, float vb, float uc, float vc) {
		v(a, nrm, ua, va); v(b, nrm, ub, vb); v(c, nrm, uc, vc); v(c, nrm, uc, vc); quads++; return this;
	}
	public MeshBuilder triFlat(V3 a, V3 b, V3 c, float ua, float va, float ub, float vb, float uc, float vc) {
		V3 nrm = b.sub(a).cross(c.sub(a)).norm(); return tri(a, b, c, nrm, ua, va, ub, vb, uc, vc);
	}
	public Mesh build() { return new Mesh(java.util.Arrays.copyOf(buf, n), quads); }

	/** Grid of (rows+1)*(cols+1) points → quads with smooth normals provided by caller. */
	private void grid(V3[][] p, V3[][] nr, int rows, int cols, float uScale, float vScale) {
		for (int i = 0; i < rows; i++) for (int j = 0; j < cols; j++) {
			float u0 = (float) i / rows * uScale, u1 = (float) (i + 1) / rows * uScale, v0 = (float) j / cols * vScale, v1 = (float) (j + 1) / cols * vScale;
			quad(p[i][j], p[i + 1][j], p[i + 1][j + 1], p[i][j + 1], nr[i][j], nr[i + 1][j], nr[i + 1][j + 1], nr[i][j + 1], new float[] { u0, v0, u1, v0, u1, v1, u0, v1 });
		}
	}

	/* ------------------------------------------------------------ primitives */
	public static Mesh box(float w, float h, float d) {
		MeshBuilder b = create(); float x = w / 2, y = h / 2, z = d / 2;
		float[] uv = { 0, 1, 1, 1, 1, 0, 0, 0 };
		V3[][] faces = {
			{ V3.of(-x, -y, z), V3.of(x, -y, z), V3.of(x, y, z), V3.of(-x, y, z), V3.of(0, 0, 1) },
			{ V3.of(x, -y, -z), V3.of(-x, -y, -z), V3.of(-x, y, -z), V3.of(x, y, -z), V3.of(0, 0, -1) },
			{ V3.of(x, -y, z), V3.of(x, -y, -z), V3.of(x, y, -z), V3.of(x, y, z), V3.of(1, 0, 0) },
			{ V3.of(-x, -y, -z), V3.of(-x, -y, z), V3.of(-x, y, z), V3.of(-x, y, -z), V3.of(-1, 0, 0) },
			{ V3.of(-x, y, z), V3.of(x, y, z), V3.of(x, y, -z), V3.of(-x, y, -z), V3.of(0, 1, 0) },
			{ V3.of(-x, -y, -z), V3.of(x, -y, -z), V3.of(x, -y, z), V3.of(-x, -y, z), V3.of(0, -1, 0) },
		};
		for (V3[] f : faces) b.quad(f[0], f[1], f[2], f[3], f[4], f[4], f[4], f[4], uv);
		return b.build();
	}

	/** Rounded box (bevelled edges) – soft highlights instead of hard voxel edges. */
	public static Mesh roundedBox(float w, float h, float d, float r, int seg) {
		// superellipsoid-like: sphere-projected cube
		MeshBuilder b = create();
		int s = Math.max(2, seg) * 2 + 2;
		float hx = w / 2 - r, hy = h / 2 - r, hz = d / 2 - r;
		V3[][] dirs = { { V3.of(0, 0, 1), V3.of(1, 0, 0), V3.of(0, 1, 0) }, { V3.of(0, 0, -1), V3.of(-1, 0, 0), V3.of(0, 1, 0) }, { V3.of(1, 0, 0), V3.of(0, 0, -1), V3.of(0, 1, 0) },
			{ V3.of(-1, 0, 0), V3.of(0, 0, 1), V3.of(0, 1, 0) }, { V3.of(0, 1, 0), V3.of(1, 0, 0), V3.of(0, 0, -1) }, { V3.of(0, -1, 0), V3.of(1, 0, 0), V3.of(0, 0, 1) } };
		for (V3[] f : dirs) {
			V3[][] p = new V3[s + 1][s + 1], nr = new V3[s + 1][s + 1];
			for (int i = 0; i <= s; i++) for (int j = 0; j <= s; j++) {
				float a = (float) i / s * 2 - 1, c = (float) j / s * 2 - 1;
				V3 q = f[0].add(f[1].mul(a)).add(f[2].mul(c)); // on cube surface
				V3 core = new V3(Math.max(-hx, Math.min(hx, q.x * (w / 2))), Math.max(-hy, Math.min(hy, q.y * (h / 2))), Math.max(-hz, Math.min(hz, q.z * (d / 2))));
				V3 surf = new V3(q.x * w / 2, q.y * h / 2, q.z * d / 2);
				V3 nrm = surf.sub(core).norm();
				p[i][j] = core.add(nrm.mul(r)); nr[i][j] = nrm;
			}
			b.grid(p, nr, s, s, 1, 1);
		}
		return b.build();
	}

	public static Mesh cylinder(float rTop, float rBottom, float h, int seg, boolean caps) {
		MeshBuilder b = create();
		float slope = (rBottom - rTop) / h;
		for (int i = 0; i < seg; i++) {
			float a0 = (float) i / seg * TAU, a1 = (float) (i + 1) / seg * TAU;
			V3 n0 = new V3((float) Math.sin(a0), slope, (float) Math.cos(a0)).norm(), n1 = new V3((float) Math.sin(a1), slope, (float) Math.cos(a1)).norm();
			V3 b0 = V3.of(Math.sin(a0) * rBottom, -h / 2, Math.cos(a0) * rBottom), b1 = V3.of(Math.sin(a1) * rBottom, -h / 2, Math.cos(a1) * rBottom);
			V3 t0 = V3.of(Math.sin(a0) * rTop, h / 2, Math.cos(a0) * rTop), t1 = V3.of(Math.sin(a1) * rTop, h / 2, Math.cos(a1) * rTop);
			float u0 = (float) i / seg, u1 = (float) (i + 1) / seg;
			b.quad(b0, b1, t1, t0, n0, n1, n1, n0, new float[] { u0, 1, u1, 1, u1, 0, u0, 0 });
			if (caps) {
				if (rTop > 0) b.tri(V3.of(0, h / 2, 0), t0, t1, V3.of(0, 1, 0), 0.5f, 0.5f, 0.5f + (float) Math.sin(a0) * .5f, 0.5f + (float) Math.cos(a0) * .5f, 0.5f + (float) Math.sin(a1) * .5f, 0.5f + (float) Math.cos(a1) * .5f);
				if (rBottom > 0) b.tri(V3.of(0, -h / 2, 0), b1, b0, V3.of(0, -1, 0), 0.5f, 0.5f, 0.5f + (float) Math.sin(a1) * .5f, 0.5f + (float) Math.cos(a1) * .5f, 0.5f + (float) Math.sin(a0) * .5f, 0.5f + (float) Math.cos(a0) * .5f);
			}
		}
		return b.build();
	}

	public static Mesh cone(float r, float h, int seg) { return cylinder(0.0001f, r, h, seg, true); }

	public static Mesh sphere(float r, int seg, int rings) { return ellipsoid(r, r, r, seg, rings); }

	public static Mesh ellipsoid(float rx, float ry, float rz, int seg, int rings) {
		MeshBuilder b = create();
		V3[][] p = new V3[seg + 1][rings + 1], nr = new V3[seg + 1][rings + 1];
		for (int i = 0; i <= seg; i++) for (int j = 0; j <= rings; j++) {
			float th = (float) i / seg * TAU, ph = (float) j / rings * (float) Math.PI;
			V3 u = V3.of(Math.sin(ph) * Math.sin(th), Math.cos(ph), Math.sin(ph) * Math.cos(th));
			p[i][j] = new V3(u.x * rx, u.y * ry, u.z * rz); nr[i][j] = new V3(u.x / rx, u.y / ry, u.z / rz).norm();
		}
		// grid winding: i around (+), j downward → reverse to face outward
		for (int i = 0; i < seg; i++) for (int j = 0; j < rings; j++) {
			float u0 = (float) i / seg, u1 = (float) (i + 1) / seg, v0 = (float) j / rings, v1 = (float) (j + 1) / rings;
			b.quad(p[i][j], p[i][j + 1], p[i + 1][j + 1], p[i + 1][j], nr[i][j], nr[i][j + 1], nr[i + 1][j + 1], nr[i + 1][j], new float[] { u0, v0, u0, v1, u1, v1, u1, v0 });
		}
		return b.build();
	}

	/** Displaced sphere (fur pompoms, snowballs). */
	public static Mesh lumpySphere(float r, int seg, int rings, float amount, long seed) {
		Mesh s = sphere(r, seg, rings);
		java.util.Random rnd = new java.util.Random(seed);
		float[] d = s.data.clone();
		java.util.Map<Long, Float> cache = new java.util.HashMap<>();
		for (int i = 0; i < s.quads * 4; i++) {
			int o = i * Mesh.STRIDE;
			long key = (Math.round(d[o] * 1e4) * 73856093L) ^ (Math.round(d[o + 1] * 1e4) * 19349663L) ^ (Math.round(d[o + 2] * 1e4) * 83492791L);
			float k = cache.computeIfAbsent(key, kk -> 1 + (rnd.nextFloat() - 0.5f) * amount);
			k += (float) (Math.sin(d[o] * 60) * Math.sin(d[o + 1] * 55) * Math.sin(d[o + 2] * 50)) * amount * 0.5f;
			d[o] *= k; d[o + 1] *= k; d[o + 2] *= k;
		}
		return new Mesh(d, s.quads);
	}

	public static Mesh torus(float R, float r, int seg, int tube, float arc) {
		MeshBuilder b = create();
		V3[][] p = new V3[seg + 1][tube + 1], nr = new V3[seg + 1][tube + 1];
		for (int i = 0; i <= seg; i++) for (int j = 0; j <= tube; j++) {
			float u = (float) i / seg * arc, v = (float) j / tube * TAU;
			V3 c = V3.of(Math.cos(u) * R, Math.sin(u) * R, 0);
			V3 nrm = V3.of(Math.cos(u) * Math.cos(v), Math.sin(u) * Math.cos(v), Math.sin(v));
			p[i][j] = c.add(nrm.mul(r)); nr[i][j] = nrm;
		}
		b.grid(p, nr, seg, tube, Math.max(1, Math.round(arc * R * 4)), 1);
		return b.build();
	}

	/** Revolve profile (x = radius, y = height) around Y. */
	public static Mesh lathe(float[][] profile, int seg) {
		MeshBuilder b = create(); int m = profile.length;
		V3[][] p = new V3[seg + 1][m], nr = new V3[seg + 1][m];
		for (int j = 0; j < m; j++) {
			float dx, dy;
			int a = Math.max(0, j - 1), c = Math.min(m - 1, j + 1);
			dx = profile[c][0] - profile[a][0]; dy = profile[c][1] - profile[a][1];
			float nx = dy, ny = -dx; float l = (float) Math.hypot(nx, ny); if (l < 1e-6f) { nx = 0; ny = 1; l = 1; }
			for (int i = 0; i <= seg; i++) {
				float th = (float) i / seg * TAU, s = (float) Math.sin(th), co = (float) Math.cos(th);
				p[i][j] = new V3(profile[j][0] * s, profile[j][1], profile[j][0] * co);
				nr[i][j] = new V3(nx / l * s, ny / l, nx / l * co);
			}
		}
		for (int i = 0; i < seg; i++) for (int j = 0; j < m - 1; j++) {
			float u0 = (float) i / seg, u1 = (float) (i + 1) / seg, v0 = (float) j / (m - 1), v1 = (float) (j + 1) / (m - 1);
			b.quad(p[i][j], p[i + 1][j], p[i + 1][j + 1], p[i][j + 1], nr[i][j], nr[i + 1][j], nr[i + 1][j + 1], nr[i][j + 1], new float[] { u0, v0, u1, v0, u1, v1, u0, v1 });
		}
		return b.build();
	}

	/** Tapered sweep along a Catmull-Rom curve with parallel-transport frames. radius(t) may differ in x/y via ry/rx scale. */
	public static Mesh sweep(List<V3> ctrl, Function<Float, Float> radius, int tubular, int radial, boolean caps) { return sweep(ctrl, radius, 1f, tubular, radial, caps, 1f); }

	public static Mesh sweep(List<V3> ctrl, Function<Float, Float> radius, float flatten, int tubular, int radial, boolean caps, float uRepeat) {
		Curve curve = new Curve(ctrl);
		V3[] pts = new V3[tubular + 1], tan = new V3[tubular + 1], nrm = new V3[tubular + 1], bin = new V3[tubular + 1];
		for (int i = 0; i <= tubular; i++) { float t = (float) i / tubular; pts[i] = curve.point(t); tan[i] = curve.tangent(t); }
		V3 ref = Math.abs(tan[0].y) < 0.9f ? new V3(0, 1, 0) : new V3(1, 0, 0);
		nrm[0] = tan[0].cross(ref).cross(tan[0]).norm(); bin[0] = tan[0].cross(nrm[0]).norm();
		for (int i = 1; i <= tubular; i++) {
			V3 nn = nrm[i - 1].sub(tan[i].mul(nrm[i - 1].dot(tan[i]))).norm();
			nrm[i] = nn; bin[i] = tan[i].cross(nn).norm();
		}
		MeshBuilder b = create();
		V3[][] p = new V3[tubular + 1][radial + 1], nr = new V3[tubular + 1][radial + 1];
		for (int i = 0; i <= tubular; i++) {
			float r = radius.apply((float) i / tubular);
			for (int j = 0; j <= radial; j++) {
				float a = (float) j / radial * TAU, s = (float) Math.sin(a), c = (float) Math.cos(a);
				V3 dir = nrm[i].mul(c).add(bin[i].mul(s));
				p[i][j] = pts[i].add(nrm[i].mul(c * r * flatten)).add(bin[i].mul(s * r));
				nr[i][j] = nrm[i].mul(c / Math.max(flatten, 0.05f)).add(bin[i].mul(s)).norm();
				if (flatten == 1f) nr[i][j] = dir;
			}
		}
		for (int i = 0; i < tubular; i++) for (int j = 0; j < radial; j++) {
			float u0 = (float) i / tubular * uRepeat, u1 = (float) (i + 1) / tubular * uRepeat, v0 = (float) j / radial, v1 = (float) (j + 1) / radial;
			b.quad(p[i][j], p[i][j + 1], p[i + 1][j + 1], p[i + 1][j], nr[i][j], nr[i][j + 1], nr[i + 1][j + 1], nr[i + 1][j], new float[] { u0, v0, u0, v1, u1, v1, u1, v0 });
		}
		if (caps) {
			for (int end = 0; end < 2; end++) {
				int i = end == 0 ? 0 : tubular; V3 nn = end == 0 ? tan[0].mul(-1) : tan[tubular];
				for (int j = 0; j < radial; j++) {
					if (end == 0) b.tri(pts[i], p[i][j + 1], p[i][j], nn, .5f, .5f, 0, 0, 1, 0);
					else b.tri(pts[i], p[i][j], p[i][j + 1], nn, .5f, .5f, 0, 0, 1, 0);
				}
			}
		}
		return b.build();
	}

	/** Extrude a star-shaped polygon (XY, CCW) along Z with bevel-like side normals. */
	public static Mesh extrude(float[][] poly, float depth) {
		MeshBuilder b = create(); int m = poly.length;
		float cx = 0, cy = 0, minX = 1e9f, minY = 1e9f, maxX = -1e9f, maxY = -1e9f;
		for (float[] q : poly) { cx += q[0]; cy += q[1]; minX = Math.min(minX, q[0]); maxX = Math.max(maxX, q[0]); minY = Math.min(minY, q[1]); maxY = Math.max(maxY, q[1]); }
		cx /= m; cy /= m; float sx = Math.max(1e-4f, maxX - minX), sy = Math.max(1e-4f, maxY - minY);
		float z = depth / 2;
		for (int i = 0; i < m; i++) {
			float[] a = poly[i], c = poly[(i + 1) % m];
			b.tri(V3.of(cx, cy, z), V3.of(a[0], a[1], z), V3.of(c[0], c[1], z), V3.of(0, 0, 1), (cx - minX) / sx, (cy - minY) / sy, (a[0] - minX) / sx, (a[1] - minY) / sy, (c[0] - minX) / sx, (c[1] - minY) / sy);
			b.tri(V3.of(cx, cy, -z), V3.of(c[0], c[1], -z), V3.of(a[0], a[1], -z), V3.of(0, 0, -1), (cx - minX) / sx, (cy - minY) / sy, (c[0] - minX) / sx, (c[1] - minY) / sy, (a[0] - minX) / sx, (a[1] - minY) / sy);
			V3 e = V3.of(c[0] - a[0], c[1] - a[1], 0); V3 n = V3.of(e.y, -e.x, 0).norm();
			b.quad(V3.of(a[0], a[1], z), V3.of(a[0], a[1], -z), V3.of(c[0], c[1], -z), V3.of(c[0], c[1], z), n, n, n, n, new float[] { 0, 0, 0, 1, 1, 1, 1, 0 });
		}
		return b.build();
	}

	/** Faceted 3D star (double pyramid). */
	public static Mesh star(int points, float outer, float inner, float depth) {
		MeshBuilder b = create(); V3 f = V3.of(0, 0, depth), k = V3.of(0, 0, -depth);
		V3[] ring = new V3[points * 2];
		for (int i = 0; i < ring.length; i++) { float r = i % 2 == 1 ? inner : outer; double a = (double) i / ring.length * TAU + Math.PI / 2; ring[i] = V3.of(Math.cos(a) * r, Math.sin(a) * r, 0); }
		for (int i = 0; i < ring.length; i++) {
			V3 a = ring[i], c = ring[(i + 1) % ring.length];
			b.triFlat(f, a, c, .5f, .5f, 0, 1, 1, 1);
			b.triFlat(k, c, a, .5f, .5f, 0, 1, 1, 1);
		}
		return b.build();
	}

	public static Mesh octahedron(float r) {
		MeshBuilder b = create();
		V3[] ax = { V3.of(r, 0, 0), V3.of(0, 0, r), V3.of(-r, 0, 0), V3.of(0, 0, -r) }; V3 top = V3.of(0, r, 0), bot = V3.of(0, -r, 0);
		for (int i = 0; i < 4; i++) { V3 a = ax[i], c = ax[(i + 1) % 4]; b.triFlat(top, c, a, .5f, 0, 1, 1, 0, 1); b.triFlat(bot, a, c, .5f, 1, 0, 0, 1, 0); }
		return b.build();
	}

	/** Hexagonal crystal (lathe with few sides). */
	public static Mesh crystal(int sides, float h, float r, float tip) {
		return lathe(new float[][] { { 0, -tip * .4f }, { r * .8f, 0 }, { r, h * .1f }, { r * .9f, h }, { 0, h + tip } }, sides);
	}

	/** Six-armed snowflake with branches, extruded. Unit radius ≈ 1. */
	public static Mesh snowflake(int variant, float depth) {
		List<Mesh> parts = new ArrayList<>();
		float[][] arm = { { -0.055f, 0 }, { 0.055f, 0 }, { 0.03f, 1 }, { 0, 1.09f }, { -0.03f, 1 } };
		float[][] br = { { -0.032f, 0 }, { 0.032f, 0 }, { 0.02f, 1 }, { 0, 1.15f }, { -0.02f, 1 } };
		float[][] sets = { { 0.38f, 0.32f }, { 0.62f, variant == 2 ? 0.31f : 0.24f }, { 0.82f, 0.14f } };
		Mesh armM = extrude(arm, depth), brM = extrude(br, depth * 0.9f);
		for (int i = 0; i < 6; i++) {
			float a = i / 6f * TAU;
			parts.add(armM.transform(Mat4.rotZ(a)));
			for (float[] s : sets) for (int sg = -1; sg <= 1; sg += 2) {
				float L = s[1] * (variant == 1 ? 1.15f : 1f);
				parts.add(brM.transform(Mat4.rotZ(a).mul(Mat4.translate(0, s[0], 0)).mul(Mat4.rotZ(sg * (variant == 1 ? 0.9f : 0.78f))).mul(Mat4.scale(1, L, 1))));
			}
		}
		float[][] hex = new float[6][]; for (int i = 0; i < 6; i++) hex[i] = new float[] { (float) Math.cos(i / 6f * TAU) * 0.17f, (float) Math.sin(i / 6f * TAU) * 0.17f };
		parts.add(extrude(hex, depth * 1.2f));
		return Mesh.merge(parts.toArray(new Mesh[0]));
	}

	/** Catmull-Rom (centripetal-ish uniform) curve with arc-length-agnostic sampling. */
	public static final class Curve {
		private final List<V3> p;
		public Curve(List<V3> p) { this.p = p; }
		public V3 point(float t) {
			int n = p.size() - 1; float f = Math.max(0, Math.min(1, t)) * n; int i = Math.min(n - 1, (int) Math.floor(f)); float u = f - i;
			V3 p0 = p.get(Math.max(0, i - 1)), p1 = p.get(i), p2 = p.get(i + 1), p3 = p.get(Math.min(n, i + 2));
			float u2 = u * u, u3 = u2 * u;
			return new V3(cr(p0.x, p1.x, p2.x, p3.x, u, u2, u3), cr(p0.y, p1.y, p2.y, p3.y, u, u2, u3), cr(p0.z, p1.z, p2.z, p3.z, u, u2, u3));
		}
		public V3 tangent(float t) { float e = 0.001f; return point(Math.min(1, t + e)).sub(point(Math.max(0, t - e))).norm(); }
		private static float cr(float a, float b, float c, float d, float u, float u2, float u3) {
			return 0.5f * ((2 * b) + (-a + c) * u + (2 * a - 5 * b + 4 * c - d) * u2 + (-a + 3 * b - 3 * c + d) * u3);
		}
	}

	public static List<V3> pts(float... xyz) {
		List<V3> l = new ArrayList<>(); for (int i = 0; i + 2 < xyz.length; i += 3) l.add(new V3(xyz[i], xyz[i + 1], xyz[i + 2])); return l;
	}
}
