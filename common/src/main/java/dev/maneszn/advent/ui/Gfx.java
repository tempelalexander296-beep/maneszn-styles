package dev.maneszn.advent.ui;

import dev.maneszn.advent.mesh.Tex;

/** Minimal 2D drawing surface implemented per MC version (GuiGraphics / GuiGraphicsExtractor). */
public interface Gfx {
	int width();
	int height();
	void fill(int x0, int y0, int x1, int y1, int argb);
	void gradient(int x0, int y0, int x1, int y1, int top, int bottom);
	/** Draws a region of an atlas tile (local uv 0..1) stretched to the rectangle. */
	void tile(Tex tex, int x, int y, int w, int h, float u0, float v0, float u1, float v1, int argb);
	default void tile(Tex tex, int x, int y, int w, int h, int argb) { tile(tex, x, y, w, h, 0, 0, 1, 1, argb); }
	void text(String s, int x, int y, int argb, boolean shadow);
	int textWidth(String s);
	int lineHeight();
	void push();
	void pop();
	void translate(float x, float y);
	void scale(float s);
	void rotate(float rad);
	/** Local player preview (3D, with cosmetics), spinning by yawDeg. */
	void player(int x0, int y0, int x1, int y1, int size, float yawDeg, float offsetY);
}
