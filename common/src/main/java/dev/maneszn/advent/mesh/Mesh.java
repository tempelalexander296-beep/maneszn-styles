package dev.maneszn.advent.mesh;

import java.util.Arrays;

/**
 * Immutable quad mesh. Vertex layout: x y z nx ny nz u v (u/v are tile-local 0..1).
 * Triangles are stored as degenerate quads (4th vertex = 3rd) so they render with QUADS render types.
 */
public final class Mesh {
	public static final int STRIDE = 8;
	public final float[] data; // quads * 4 * STRIDE
	public final int quads;

	Mesh(float[] data, int quads) { this.data = data; this.quads = quads; }

	public static Mesh of(float[] data, int quads) { return new Mesh(data, quads); }

	public Mesh transform(Mat4 t) {
		float[] d = Arrays.copyOf(data, quads * 4 * STRIDE);
		boolean flip = t.det3() < 0;
		for (int i = 0; i < quads * 4; i++) {
			int o = i * STRIDE;
			V3 p = t.point(d[o], d[o + 1], d[o + 2]); V3 n = t.dir(d[o + 3], d[o + 4], d[o + 5]).norm();
			d[o] = p.x; d[o + 1] = p.y; d[o + 2] = p.z; d[o + 3] = n.x; d[o + 4] = n.y; d[o + 5] = n.z;
		}
		if (flip) { // keep winding consistent after mirroring
			float[] tmp = new float[STRIDE];
			for (int q = 0; q < quads; q++) {
				int a = (q * 4 + 1) * STRIDE, b = (q * 4 + 3) * STRIDE;
				System.arraycopy(d, a, tmp, 0, STRIDE); System.arraycopy(d, b, d, a, STRIDE); System.arraycopy(tmp, 0, d, b, STRIDE);
			}
		}
		return new Mesh(d, quads);
	}

	public static Mesh merge(Mesh... meshes) {
		int q = 0; for (Mesh m : meshes) q += m.quads;
		float[] d = new float[q * 4 * STRIDE]; int o = 0;
		for (Mesh m : meshes) { System.arraycopy(m.data, 0, d, o, m.quads * 4 * STRIDE); o += m.quads * 4 * STRIDE; }
		return new Mesh(d, q);
	}
}
