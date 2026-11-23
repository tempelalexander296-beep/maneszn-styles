package dev.maneszn.advent.mesh;

/** Column-major 4x4 matrix used to bake transforms into meshes. */
public final class Mat4 {
	public final float[] m = new float[16];
	public Mat4() { m[0] = m[5] = m[10] = m[15] = 1; }

	public static Mat4 identity() { return new Mat4(); }
	public Mat4 mul(Mat4 o) { // this * o
		Mat4 r = new Mat4();
		for (int c = 0; c < 4; c++) for (int rr = 0; rr < 4; rr++) {
			float s = 0; for (int k = 0; k < 4; k++) s += m[k * 4 + rr] * o.m[c * 4 + k];
			r.m[c * 4 + rr] = s;
		}
		return r;
	}
	public static Mat4 translate(float x, float y, float z) { Mat4 r = new Mat4(); r.m[12] = x; r.m[13] = y; r.m[14] = z; return r; }
	public static Mat4 scale(float x, float y, float z) { Mat4 r = new Mat4(); r.m[0] = x; r.m[5] = y; r.m[10] = z; return r; }
	public static Mat4 rotX(float a) { Mat4 r = new Mat4(); float c = (float) Math.cos(a), s = (float) Math.sin(a); r.m[5] = c; r.m[6] = s; r.m[9] = -s; r.m[10] = c; return r; }
	public static Mat4 rotY(float a) { Mat4 r = new Mat4(); float c = (float) Math.cos(a), s = (float) Math.sin(a); r.m[0] = c; r.m[2] = -s; r.m[8] = s; r.m[10] = c; return r; }
	public static Mat4 rotZ(float a) { Mat4 r = new Mat4(); float c = (float) Math.cos(a), s = (float) Math.sin(a); r.m[0] = c; r.m[1] = s; r.m[4] = -s; r.m[5] = c; return r; }
	/** Builds T * Rz * Ry * Rx * S (Euler XYZ like three.js). */
	public static Mat4 trs(float tx, float ty, float tz, float rx, float ry, float rz, float sx, float sy, float sz) {
		return translate(tx, ty, tz).mul(rotZ(rz)).mul(rotY(ry)).mul(rotX(rx)).mul(scale(sx, sy, sz));
	}
	/** Rotation that maps +Y onto dir. */
	public static Mat4 alignY(V3 dir) {
		V3 y = dir.norm();
		V3 ref = Math.abs(y.y) > 0.95f ? new V3(1, 0, 0) : new V3(0, 1, 0);
		V3 x = ref.cross(y).norm(); V3 z = x.cross(y).norm();
		Mat4 r = new Mat4();
		r.m[0] = x.x; r.m[1] = x.y; r.m[2] = x.z; r.m[4] = y.x; r.m[5] = y.y; r.m[6] = y.z; r.m[8] = z.x; r.m[9] = z.y; r.m[10] = z.z;
		return r;
	}
	public V3 point(float x, float y, float z) { return new V3(m[0] * x + m[4] * y + m[8] * z + m[12], m[1] * x + m[5] * y + m[9] * z + m[13], m[2] * x + m[6] * y + m[10] * z + m[14]); }
	public V3 dir(float x, float y, float z) { return new V3(m[0] * x + m[4] * y + m[8] * z, m[1] * x + m[5] * y + m[9] * z, m[2] * x + m[6] * y + m[10] * z); }
	public float det3() { return m[0] * (m[5] * m[10] - m[9] * m[6]) - m[4] * (m[1] * m[10] - m[9] * m[2]) + m[8] * (m[1] * m[6] - m[5] * m[2]); }
}
