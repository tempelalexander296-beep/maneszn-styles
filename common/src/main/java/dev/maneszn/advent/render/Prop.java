package dev.maneszn.advent.render;

import dev.maneszn.advent.mesh.Mat4;
import dev.maneszn.advent.mesh.Mesh;
import dev.maneszn.advent.mesh.Tex;
import java.util.ArrayList;
import java.util.List;

/** A static multi-material model built once (meshes are pre-baked with their local transforms). */
public final class Prop {
	private record Piece(Mesh mesh, Tex tex, Layer layer, int argb) {}
	private final List<Piece> pieces = new ArrayList<>();

	public Prop add(Mesh m, Tex t, Layer l, int argb) { pieces.add(new Piece(m, t, l, argb)); return this; }
	public Prop add(Mesh m, Tex t, Layer l) { return add(m, t, l, 0xFFFFFFFF); }
	public Prop add(Mesh m, Tex t) { return add(m, t, Layer.SOLID); }
	public Prop add(Mesh m, Mat4 xf, Tex t, Layer l) { return add(m.transform(xf), t, l); }
	public Prop add(Mesh m, Mat4 xf, Tex t) { return add(m.transform(xf), t, Layer.SOLID); }
	public Prop add(Prop other, Mat4 xf) { for (Piece p : other.pieces) pieces.add(new Piece(p.mesh.transform(xf), p.tex, p.layer, p.argb)); return this; }
	public Prop add(Prop other) { pieces.addAll(other.pieces); return this; }

	public void render(Sink s) { for (Piece p : pieces) s.mesh(p.mesh, p.tex, p.layer, p.argb); }
	public void render(Sink s, Layer only) { for (Piece p : pieces) if (p.layer == only) s.mesh(p.mesh, p.tex, p.layer, p.argb); }
}
