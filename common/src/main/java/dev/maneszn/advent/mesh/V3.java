package dev.maneszn.advent.mesh;

/** Tiny mutable 3D vector (no external deps → shared by every MC version). */
public final class V3 {
	public float x, y, z;
	public V3() {}
	public V3(float x, float y, float z) { this.x = x; this.y = y; this.z = z; }
	public static V3 of(double x, double y, double z) { return new V3((float) x, (float) y, (float) z); }
	public V3 copy() { return new V3(x, y, z); }
	public V3 set(float x, float y, float z) { this.x = x; this.y = y; this.z = z; return this; }
	public V3 add(V3 o) { return new V3(x + o.x, y + o.y, z + o.z); }
	public V3 sub(V3 o) { return new V3(x - o.x, y - o.y, z - o.z); }
	public V3 mul(float s) { return new V3(x * s, y * s, z * s); }
	public float dot(V3 o) { return x * o.x + y * o.y + z * o.z; }
	public V3 cross(V3 o) { return new V3(y * o.z - z * o.y, z * o.x - x * o.z, x * o.y - y * o.x); }
	public float len() { return (float) Math.sqrt(x * x + y * y + z * z); }
	public V3 norm() { float l = len(); return l < 1e-8f ? new V3(0, 1, 0) : mul(1f / l); }
	public V3 lerp(V3 o, float t) { return new V3(x + (o.x - x) * t, y + (o.y - y) * t, z + (o.z - z) * t); }
}
