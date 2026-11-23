package dev.maneszn.advent.render;

import dev.maneszn.advent.mesh.Mesh;
import dev.maneszn.advent.mesh.Tex;

/** Version-neutral render target. Implemented per MC version on top of PoseStack + submit pipeline. */
public interface Sink {
	void push();
	void pop();
	void translate(float x, float y, float z);
	void rotateX(float rad);
	void rotateY(float rad);
	void rotateZ(float rad);
	void scale(float x, float y, float z);
	default void scale(float s) { scale(s, s, s); }
	void mesh(Mesh mesh, Tex tex, Layer layer, int argb);
	default void mesh(Mesh mesh, Tex tex, Layer layer) { mesh(mesh, tex, layer, 0xFFFFFFFF); }
}
