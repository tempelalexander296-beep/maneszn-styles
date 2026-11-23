import dev.maneszn.advent.core.Reward;
import dev.maneszn.advent.core.Rewards;
import dev.maneszn.advent.mesh.Mesh;
import dev.maneszn.advent.mesh.Tex;
import dev.maneszn.advent.render.*;
import dev.maneszn.advent.render.cosmetics.Cosmetics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javax.imageio.ImageIO;

/** Offline software rasterizer: renders every cosmetic to a contact sheet for QA (not part of the mod). */
public final class PreviewRenderer {
	record Tri(float[] a, float[] b, float[] c, Tex tex, Layer layer, int argb) {}
	static BufferedImage atlas;

	static final class Capture implements Sink {
		final List<Tri> tris = new ArrayList<>(); float[] m = ident(); final List<float[]> stack = new ArrayList<>();
		public void push() { stack.add(m.clone()); }
		public void pop() { m = stack.remove(stack.size() - 1); }
		public void translate(float x, float y, float z) { m = mul(m, new float[] { 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, x, y, z, 1 }); }
		public void rotateX(float r) { float c = (float) Math.cos(r), s = (float) Math.sin(r); m = mul(m, new float[] { 1, 0, 0, 0, 0, c, s, 0, 0, -s, c, 0, 0, 0, 0, 1 }); }
		public void rotateY(float r) { float c = (float) Math.cos(r), s = (float) Math.sin(r); m = mul(m, new float[] { c, 0, -s, 0, 0, 1, 0, 0, s, 0, c, 0, 0, 0, 0, 1 }); }
		public void rotateZ(float r) { float c = (float) Math.cos(r), s = (float) Math.sin(r); m = mul(m, new float[] { c, s, 0, 0, -s, c, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1 }); }
		public void scale(float x, float y, float z) { m = mul(m, new float[] { x, 0, 0, 0, 0, y, 0, 0, 0, 0, z, 0, 0, 0, 0, 1 }); }
		public void mesh(Mesh mesh, Tex tex, Layer layer, int argb) {
			float[] d = mesh.data;
			for (int q = 0; q < mesh.quads; q++) {
				float[][] v = new float[4][];
				for (int k = 0; k < 4; k++) {
					int o = (q * 4 + k) * Mesh.STRIDE; float x = d[o], y = d[o + 1], z = d[o + 2], nx = d[o + 3], ny = d[o + 4], nz = d[o + 5];
					for (int j = 0; j < 8; j++) if (Float.isNaN(d[o + j]) || Float.isInfinite(d[o + j])) throw new IllegalStateException("NaN in mesh");
					v[k] = new float[] { m[0] * x + m[4] * y + m[8] * z + m[12], m[1] * x + m[5] * y + m[9] * z + m[13], m[2] * x + m[6] * y + m[10] * z + m[14],
						m[0] * nx + m[4] * ny + m[8] * nz, m[1] * nx + m[5] * ny + m[9] * nz, m[2] * nx + m[6] * ny + m[10] * nz, tex.u(d[o + 6]), tex.v(d[o + 7]) };
				}
				tris.add(new Tri(v[0], v[1], v[2], tex, layer, argb)); tris.add(new Tri(v[0], v[2], v[3], tex, layer, argb));
			}
		}
	}

	static float[] ident() { return new float[] { 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1 }; }
	static float[] mul(float[] a, float[] b) { float[] r = new float[16]; for (int c = 0; c < 4; c++) for (int rr = 0; rr < 4; rr++) { float s = 0; for (int k = 0; k < 4; k++) s += a[k * 4 + rr] * b[c * 4 + k]; r[c * 4 + rr] = s; } return r; }

	static void render(List<Tri> tris, BufferedImage img, int ox, int oy, int S, float yaw) {
		float minX = 1e9f, minY = 1e9f, minZ = 1e9f, maxX = -1e9f, maxY = -1e9f, maxZ = -1e9f;
		for (Tri t : tris) for (float[] v : new float[][] { t.a, t.b, t.c }) { minX = Math.min(minX, v[0]); maxX = Math.max(maxX, v[0]); minY = Math.min(minY, v[1]); maxY = Math.max(maxY, v[1]); minZ = Math.min(minZ, v[2]); maxZ = Math.max(maxZ, v[2]); }
		float cx = (minX + maxX) / 2, cy = (minY + maxY) / 2, cz = (minZ + maxZ) / 2, ext = Math.max(maxX - minX, Math.max(maxY - minY, maxZ - minZ)) * 0.62f + 1e-4f;
		float[] zb = new float[S * S]; java.util.Arrays.fill(zb, -1e9f);
		int[] col = new int[S * S];
		for (int i = 0; i < S * S; i++) { int yy = i / S; float t = yy / (float) S; col[i] = rgb(0.05f + .08f * t, 0.07f + .05f * t, 0.14f - .05f * t); }
		float cyw = (float) Math.cos(yaw), syw = (float) Math.sin(yaw), pitch = 0.32f, cp = (float) Math.cos(pitch), sp = (float) Math.sin(pitch);
		float[] L = norm(new float[] { 0.5f, 0.8f, 0.6f });
		List<Tri> trans = new ArrayList<>();
		for (int pass = 0; pass < 2; pass++) for (Tri t : (pass == 0 ? tris : trans)) {
			if (pass == 0 && t.layer != Layer.SOLID) { trans.add(t); continue; }
			float[][] p = new float[3][]; float[][] src = { t.a, t.b, t.c };
			for (int k = 0; k < 3; k++) {
				float x = src[k][0] - cx, y = src[k][1] - cy, z = src[k][2] - cz;
				float rx = x * cyw + z * syw, rz = -x * syw + z * cyw;
				float ry = y * cp - rz * sp, rz2 = y * sp + rz * cp;
				p[k] = new float[] { S / 2f + rx / ext * S / 2f, S / 2f - ry / ext * S / 2f, rz2 };
			}
			float[] nrm = t.a; float nx = (t.a[3] + t.b[3] + t.c[3]) / 3, ny = (t.a[4] + t.b[4] + t.c[4]) / 3, nz = (t.a[5] + t.b[5] + t.c[5]) / 3;
			float[] n = norm(new float[] { nx * cyw + nz * syw, ny, -nx * syw + nz * cyw }); void_(nrm);
			float lam = t.layer == Layer.GLOW ? 1.4f : 0.35f + 0.65f * Math.abs(n[0] * L[0] + n[1] * L[1] + n[2] * L[2]);
			int x0 = (int) Math.max(0, Math.floor(Math.min(p[0][0], Math.min(p[1][0], p[2][0])))), x1 = (int) Math.min(S - 1, Math.ceil(Math.max(p[0][0], Math.max(p[1][0], p[2][0]))));
			int y0 = (int) Math.max(0, Math.floor(Math.min(p[0][1], Math.min(p[1][1], p[2][1])))), y1 = (int) Math.min(S - 1, Math.ceil(Math.max(p[0][1], Math.max(p[1][1], p[2][1]))));
			float area = (p[1][0] - p[0][0]) * (p[2][1] - p[0][1]) - (p[2][0] - p[0][0]) * (p[1][1] - p[0][1]);
			if (Math.abs(area) < 1e-6f) continue;
			for (int y = y0; y <= y1; y++) for (int x = x0; x <= x1; x++) {
				float w0 = ((p[1][0] - x) * (p[2][1] - y) - (p[2][0] - x) * (p[1][1] - y)) / area, w1 = ((p[2][0] - x) * (p[0][1] - y) - (p[0][0] - x) * (p[2][1] - y)) / area, w2 = 1 - w0 - w1;
				if (w0 < 0 || w1 < 0 || w2 < 0) continue;
				float z = w0 * p[0][2] + w1 * p[1][2] + w2 * p[2][2];
				int i = y * S + x;
				if (t.layer == Layer.SOLID && z <= zb[i]) continue;
				if (t.layer != Layer.SOLID && z <= zb[i]) continue;
				float u = w0 * t.a[6] + w1 * t.b[6] + w2 * t.c[6], v = w0 * t.a[7] + w1 * t.b[7] + w2 * t.c[7];
				int tx = Math.min(atlas.getWidth() - 1, Math.max(0, (int) (u * atlas.getWidth()))), ty = Math.min(atlas.getHeight() - 1, Math.max(0, (int) (v * atlas.getHeight())));
				int a = atlas.getRGB(tx, ty); float ta = ((a >>> 24) & 255) / 255f * (((t.argb >>> 24) & 255) / 255f);
				if (t.layer == Layer.SOLID && ta < 0.1f) continue;
				float r = ((a >> 16) & 255) / 255f * (((t.argb >> 16) & 255) / 255f) * lam, g = ((a >> 8) & 255) / 255f * (((t.argb >> 8) & 255) / 255f) * lam, b = (a & 255) / 255f * ((t.argb & 255) / 255f) * lam;
				if (t.layer == Layer.SOLID) { col[i] = rgb(r, g, b); zb[i] = z; }
				else { int c0 = col[i]; float k = t.layer == Layer.GLOW ? ta : Math.max(.35f, ta);
					float br = ((c0 >> 16) & 255) / 255f, bg = ((c0 >> 8) & 255) / 255f, bb = (c0 & 255) / 255f;
					col[i] = t.layer == Layer.GLOW ? rgb(br + r * k, bg + g * k, bb + b * k) : rgb(br * (1 - k) + r * k, bg * (1 - k) + g * k, bb * (1 - k) + b * k); }
			}
		}
		for (int i = 0; i < S * S; i++) img.setRGB(ox + i % S, oy + i / S, col[i]);
	}
	static void void_(Object o) {}
	static float[] norm(float[] v) { float l = (float) Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]); return l < 1e-8 ? new float[] { 0, 1, 0 } : new float[] { v[0] / l, v[1] / l, v[2] / l }; }
	static int rgb(float r, float g, float b) { return 0xFF000000 | (clamp(r) << 16) | (clamp(g) << 8) | clamp(b); }
	static int clamp(float x) { return (int) (Math.max(0, Math.min(1, x)) * 255); }

	public static void main(String[] args) throws Exception {
		atlas = ImageIO.read(new File(args[0]));
		int S = Integer.parseInt(args[2]), cols = 6;
		BufferedImage sheet = new BufferedImage(cols * S, 4 * S, BufferedImage.TYPE_INT_RGB);
		Random rnd = new Random(1);
		for (Reward r : Rewards.ALL) {
			CosmeticModel m = Cosmetics.get(r.id());
			Capture cap = new Capture(); Wearer w = new Wearer(); w.time = 2.3f; w.preview = true;
			long t0 = System.nanoTime(); m.render(cap, w); long ms = (System.nanoTime() - t0) / 1_000_000;
			int fx[] = { 0 }; for (int i = 0; i < 40; i++) m.tickFx((type, x, y, z, vx, vy, vz) -> fx[0]++, w, rnd);
			render(cap.tris, sheet, ((r.day() - 1) % cols) * S, ((r.day() - 1) / cols) * S, S, r.slot().name().equals("BACK") ? 2.6f : 0.6f);
			System.out.printf("%2d %-26s tris=%6d  fx/2s=%4d  build+render=%dms%n", r.day(), r.name(), cap.tris.size(), fx[0], ms);
		}
		ImageIO.write(sheet, "png", new File(args[1]));
	}
}
