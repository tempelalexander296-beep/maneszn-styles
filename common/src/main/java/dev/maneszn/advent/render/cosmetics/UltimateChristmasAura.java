package dev.maneszn.advent.render.cosmetics;

import static dev.maneszn.advent.mesh.Mat4.*;
import static dev.maneszn.advent.mesh.MeshBuilder.*;
import static dev.maneszn.advent.render.cosmetics.M.*;

import dev.maneszn.advent.mesh.Mesh;
import dev.maneszn.advent.mesh.Tex;
import dev.maneszn.advent.mesh.V3;
import dev.maneszn.advent.render.*;
import java.util.List;
import java.util.Random;

/**
 * Day 24 – ULTIMATE CHRISTMAS AURA (Mythic). Layered composition around the player:
 * ground runes · 6 orbiting decorated trees · 8 floating gifts · ornaments · rotating candy canes ·
 * swinging bells · snowflakes · blinking light-string helix · 12 twinkling stars · giant golden star with
 * god-rays above the head · snowfall, rising golden embers and sparkles.
 */
final class UltimateChristmasAura implements CosmeticModel {
	final Prop[] trees = new Prop[6]; final Prop[] gifts = new Prop[8]; final Prop[] ornaments = new Prop[10]; final Prop[] canes = new Prop[6];
	final Prop bell = Parts.bell(Tex.GOLD), bellHolly = Parts.holly(.07f), bigStar = new Prop();
	final Mesh flake = snowflake(1, .06f), smallStar = star(5, .045f, .02f, .015f), bulb = sphere(.018f, 8, 6), ray = box(.1f, 1.9f, .001f), glowBall = sphere(.42f, 24, 16), treeBulb = sphere(.012f, 6, 4);
	final float[][] giftD = new float[8][5], ornD = new float[10][4], flakeD = new float[16][5];
	final List<V3> helix = Parts.spiral(2.1f, .88f, .63f, 3.2f, 90, .15f);
	final List<V3> treeLights = Parts.spiral(.7f, .36f, .02f, 4, 22, .27f);

	UltimateChristmasAura() {
		Random R = new Random(2412);
		for (int i = 0; i < 6; i++) { boolean big = i % 2 == 0; float h = big ? .95f : .55f; trees[i] = Parts.tree(h, 30 + i, true).add(cylinder(big ? .2f : .13f, big ? .24f : .16f, .05f, 32, true), translate(0, .02f, 0), Tex.SNOW_GROUND, Layer.SOLID); }
		Tex[][] looks = { { Tex.PAPER_RED_STARS, Tex.GOLD }, { Tex.PAPER_GREEN_DOTS, Tex.GOLD }, { Tex.PAPER_WHITE_FLAKES, Tex.SATIN_RED }, { Tex.PAPER_GOLD_STRIPES, Tex.SATIN_RED },
			{ Tex.PAPER_NIGHT_STARS, Tex.SILVER }, { Tex.PAPER_ROYAL_PLAID, Tex.GOLD }, { Tex.PAPER_ICE_FLAKES, Tex.WHITE }, { Tex.PAPER_RED_PLAID, Tex.SATIN_GREEN } };
		for (int i = 0; i < 8; i++) {
			float s = .16f + R.nextFloat() * .12f; gifts[i] = new Prop().add(Parts.gift(s, s * (.8f + R.nextFloat() * .5f), s, looks[i][0], looks[i][1], true), translate(0, -s * .5f, 0));
			giftD[i] = new float[] { i / 8f * TAU + .3f, .95f + R.nextFloat() * .3f, .45f + (i % 4) * .42f + R.nextFloat() * .1f, R.nextFloat() * TAU, (R.nextFloat() - .5f) * 1.2f };
		}
		Tex[] oc = { Tex.ORNAMENT_RED, Tex.ORNAMENT_GOLD, Tex.ORNAMENT_SILVER, Tex.ORNAMENT_GREEN, Tex.ORNAMENT_SILVER };
		for (int i = 0; i < 10; i++) { ornaments[i] = Parts.ornament(oc[i % 5], .05f + R.nextFloat() * .03f); ornD[i] = new float[] { i / 10f * TAU, .75f + R.nextFloat() * .25f, .6f + R.nextFloat() * 1.6f, R.nextFloat() * TAU }; }
		for (int i = 0; i < 6; i++) canes[i] = new Prop().add(Parts.candyCane(.32f, .018f, i % 2 == 0 ? Tex.CANDY_GREEN : Tex.CANDY_RED), translate(0, -.16f, 0));
		for (float[] f : flakeD) { f[0] = R.nextFloat() * TAU; f[1] = .6f + R.nextFloat(); f[2] = .3f + R.nextFloat() * 2.2f; f[3] = R.nextFloat() * TAU; f[4] = .3f + R.nextFloat() * .4f; }
		bigStar.add(star(5, .3f, .13f, .09f), Tex.STAR_GOLD).add(star(5, .2f, .085f, .1f), Tex.GLOW_GOLD, Layer.GLOW).add(star(5, .22f, .1f, .06f), rotZ(PI / 5).mul(scale(.92f, .92f, .92f)), Tex.GOLD);
	}

	public void render(Sink s, Wearer w) {
		float t = w.time, k = w.intro;
		s.push(); s.scale(Math.max(.001f, k));
		AuraTrailCosmetics.groundRing(s, Tex.RUNE_GOLD, 3.6f, t * .1f, .8f + sin(t * 1.7f) * .2f);
		AuraTrailCosmetics.groundRing(s, Tex.RUNE_ICE, 2.4f, -t * .18f, .5f);
		// orbiting trees
		for (int i = 0; i < 6; i++) {
			float a = i / 6f * TAU + t * .12f, r = i % 2 == 0 ? 1.55f : 1.35f, h = i % 2 == 0 ? .95f : .55f;
			s.push(); s.translate(cos(a) * r, 0, sin(a) * r); s.rotateY(-t * .3f); trees[i].render(s);
			for (int b = 0; b < treeLights.size(); b++) { V3 p = treeLights.get(b); s.push(); s.translate(p.x * h, p.y * h + .0f, p.z * h); s.mesh(treeBulb, b % 3 == 0 ? Tex.GLOW_RED : b % 3 == 1 ? Tex.GLOW_GOLD : Tex.GLOW_ICE, Layer.GLOW, pulse(0xFFFFFF, .35f + .65f * (.5f + .5f * sin(t * 2.4f + b * 1.7f + i)))); s.pop(); }
			s.pop();
		}
		// gifts (counter-rotating)
		for (int i = 0; i < 8; i++) {
			float[] d = giftD[i]; float a = d[0] - t * .22f;
			s.push(); s.translate(cos(a) * d[1], d[2] + sin(t * 1.1f + d[3]) * .09f, sin(a) * d[1]); s.rotateY(t * d[4]); s.rotateX(sin(t * .8f + d[3]) * .25f); s.rotateZ(cos(t * .6f + d[3]) * .2f); gifts[i].render(s); s.pop();
		}
		float mid = t * .28f;
		for (int i = 0; i < 10; i++) { float[] d = ornD[i]; float a = d[0] + mid; s.push(); s.translate(cos(a) * d[1], d[2] + sin(t * 1.5f + d[3]) * .06f, sin(a) * d[1]); s.rotateY(t); ornaments[i].render(s); s.pop(); }
		for (int i = 0; i < 6; i++) { float a = i / 6f * TAU + .5f + mid; s.push(); s.translate(cos(a) * 1.15f, .9f + (i % 3) * .5f + sin(t + i) * .07f, sin(a) * 1.15f); s.rotateZ(sin(t * 1.3f + i) * .4f); s.rotateY(t * 1.6f + i); s.rotateX(.3f); canes[i].render(s); s.pop(); }
		for (int i = 0; i < 4; i++) {
			float a = i / 4f * TAU + 1.1f + mid; s.push(); s.translate(cos(a) * .68f, 1.75f + (i % 2) * .2f + sin(t * 1.2f + i * 1.7f) * .04f, sin(a) * .68f); s.rotateZ(sin(t * 2.6f + i * 1.7f) * .35f);
			s.push(); s.translate(0, -.03f, 0); bellHolly.render(s); s.pop(); s.translate(0, -.12f, 0); s.scale(.07f); s.translate(0, -1, 0); bell.render(s); s.pop();
		}
		for (float[] f : flakeD) { float a = f[0] + t * f[4] + mid; s.push(); s.translate(cos(a) * f[1], f[2] + sin(t + f[3]) * .1f, sin(a) * f[1]); s.rotateY(-a); s.rotateX(t * .6f + f[3]); s.rotateZ(t * .9f); s.scale(.05f + (f[3] % 1) * .05f); s.mesh(flake, Tex.SILVER, Layer.SOLID); s.pop(); }
		// light-string helix
		Tex[] bc = { Tex.GLOW_GOLD, Tex.GLOW_RED, Tex.GLOW_WHITE, Tex.GLOW_ICE, Tex.GLOW_GREEN };
		for (int i = 0; i < helix.size(); i++) { V3 p = helix.get(i); s.push(); s.translate(p.x, p.y, p.z); s.mesh(bulb, bc[i % 5], Layer.GLOW, pulse(0xFFFFFF, .35f + .65f * (.5f + .5f * sin(t * 2.4f + i * 1.7f)))); s.pop(); }
		// twinkling satellite stars
		for (int i = 0; i < 12; i++) {
			float a = i / 12f * TAU - t * .35f, r = 1.45f + (i % 2) * .25f, sc = .7f + (float) Math.pow(.5f + .5f * sin(t * 4.5f + i * 1.9f), 3) * .9f;
			s.push(); s.translate(cos(a) * r, .5f + (i % 4) * .55f + sin(t * 1.4f + i) * .08f, sin(a) * r); s.rotateY(t * 3); s.scale(sc); s.mesh(smallStar, Tex.GLOW_GOLD, Layer.GLOW); s.pop();
		}
		// giant star with god-rays
		s.push(); s.translate(0, 2.55f + sin(t * 1.2f) * .05f, 0);
		s.push(); s.rotateY(sin(t * .8f) * .5f); bigStar.render(s); s.pop();
		float tw = .85f + sin(t * 5) * .1f + sin(t * 11.3f) * .05f;
		s.push(); s.scale(tw); s.mesh(glowBall, Tex.GLOW_GOLD, Layer.GLOW, alpha(0xFFFFFF, .22f)); s.pop();
		for (int i = 0; i < 10; i++) { s.push(); s.rotateY(sin(t * .8f) * .5f); s.rotateZ(i / 10f * PI + t * .25f); s.translate(0, 0, -.05f); s.mesh(ray, Tex.BEAM, Layer.GLOW, alpha(0xFFD36B, .45f + sin(t * 2.2f) * .15f)); s.pop(); }
		s.pop();
		s.pop();
	}

	public void tickFx(Fx fx, Wearer w, Random r) {
		for (int i = 0; i < 3; i++) { float a = r.nextFloat() * TAU, rr = (float) Math.sqrt(r.nextFloat()) * 2.1f; fx.spawn(Fx.Type.SNOW, cos(a) * rr, 3.1f, sin(a) * rr, 0, -.06f, 0); }
		for (int i = 0; i < 3; i++) { float a = r.nextFloat() * TAU, rr = .45f + r.nextFloat() * 1.2f; fx.spawn(Fx.Type.GOLD, cos(a) * rr, r.nextFloat() * .3f, sin(a) * rr, -sin(a) * .05f, .08f, cos(a) * .05f); }
		if (r.nextInt(2) == 0) { float a = r.nextFloat() * TAU, rr = .5f + r.nextFloat() * 1.3f; fx.spawn(Fx.Type.END_ROD, cos(a) * rr, r.nextFloat() * 2.8f, sin(a) * rr, 0, .01f, 0); }
		if (r.nextInt(5) == 0) fx.spawn(Fx.Type.FIREWORK, (r.nextFloat() - .5f) * .6f, 2.55f, (r.nextFloat() - .5f) * .6f, 0, 0, 0);
	}
}
