package dev.maneszn.advent.render;

/** Particle effects, coordinates in wearer feet space (x right, y up, z forward). */
public interface Fx {
	enum Type { SNOW, SPARKLE, GOLD, RED, GREEN, ICE, WHITE, END_ROD, GLOW, FIREWORK, ELECTRIC, CLOUD }
	void spawn(Type type, float x, float y, float z, float vx, float vy, float vz);
}
