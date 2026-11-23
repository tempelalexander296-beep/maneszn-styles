package dev.maneszn.advent.render;

/** A cosmetic: renders in its mount space and may emit particles every client tick. */
public interface CosmeticModel {
	void render(Sink sink, Wearer w);
	default void tickFx(Fx fx, Wearer w, java.util.Random rnd) {}
}
