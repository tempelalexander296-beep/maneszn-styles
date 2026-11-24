package com.maneszn.styles.emote;

import java.util.List;
import java.util.Map;
import java.util.Random;

public final class Emotes {
   public static final List<Emote> ALL = List.of(
      new Emote("winken", Map.of("de", "Winken", "en", "Wave"), "✋", 70, (p, t) -> {
         double k = env(t, 70.0);
         p.rot(2, -2.9 * k, 0.0, (0.15 + sin(t * 0.5) * 0.45) * k);
      }),
      new Emote("jubeln", Map.of("de", "Jubeln", "en", "Cheer"), "★", 0, (p, t) -> {
         double jump = Math.max(0.0, sin(t * 0.35)) * 2.2;
         p.rot(2, -0.2, 0.0, 2.7 + sin(t * 0.7) * 0.2);
         p.rot(3, -0.2, 0.0, -2.7 - sin(t * 0.7) * 0.2);
         p.offAll(0.0, -jump, 0.0);
      }),
      new Emote("klatschen", Map.of("de", "Klatschen", "en", "Clap"), "✦", 0, (p, t) -> {
         double c = 0.15 + Math.abs(sin(t * 0.45)) * 0.45;
         p.rot(2, -1.35, -c, 0.0);
         p.rot(3, -1.35, c, 0.0);
         p.rot(0, 0.15 + Math.abs(sin(t * 0.45)) * 0.1, 0.0, 0.0);
      }),
      new Emote("dab", Map.of("de", "Dab", "en", "Dab"), "⚡", 45, (p, t) -> {
         double k = env(t, 45.0);
         p.rot(2, -0.5 * k, 0.0, 2.3 * k);
         p.rot(3, -1.95 * k, 1.05 * k, 0.2 * k);
         p.rot(0, 0.55 * k, 0.5 * k, 0.2 * k);
      }),
      new Emote("disco", Map.of("de", "Disco", "en", "Disco"), "♫", 0, (p, t) -> {
         double ph = (sin(t * 0.3) + 1.0) / 2.0;
         p.rot(2, -0.3 - ph * 0.3, 0.0, 0.3 + ph * 2.2);
         p.rot(3, -0.2, 0.0, -0.25 - (1.0 - ph) * 0.4);
         p.rot(0, 0.1, (ph - 0.5) * 0.5, 0.0);
         p.offUpper((ph - 0.5) * 2.0, 0.0, 0.0);
         p.rot(4, 0.0, 0.0, 0.12 * ph);
         p.rot(5, 0.0, 0.0, -0.12 * (1.0 - ph));
      }),
      new Emote("floss", Map.of("de", "Floss", "en", "Floss"), "♪", 0, (p, t) -> {
         double s = sin(t * 0.5);
         double front = Math.signum(sin(t * 0.25)) * 0.35;
         p.rot(2, front, 0.0, 0.55 * s);
         p.rot(3, -front, 0.0, 0.55 * s);
         p.offUpper(-1.6 * s, 0.0, 0.0);
         p.rot(4, 0.0, 0.0, 0.1 * s);
         p.rot(5, 0.0, 0.0, 0.1 * s);
      }),
      new Emote("roboter", Map.of("de", "Roboter", "en", "Robot"), "⚙", 0, (p, t) -> {
         int step = (int)(t / 10.0) % 4;
         double[] head = new double[]{-0.6, 0.0, 0.6, 0.0};
         double up = step % 2 == 0 ? -1.57 : -1.2;
         p.rot(2, up, 0.0, 0.0);
         p.rot(3, step % 2 == 0 ? -1.2 : -1.57, 0.0, 0.0);
         p.rot(0, 0.0, head[step], 0.0);
         p.offAll(0.0, step % 2 == 0 ? 0.0 : -0.5, 0.0);
      }),
      new Emote("hampelmann", Map.of("de", "Hampelmann", "en", "Jumping Jack"), "✳", 0, (p, t) -> {
         double ph = (sin(t * 0.4 - (Math.PI / 2)) + 1.0) / 2.0;
         p.rot(2, 0.0, 0.0, 0.15 + 2.7 * ph);
         p.rot(3, 0.0, 0.0, -0.15 - 2.7 * ph);
         p.rot(4, 0.0, 0.0, 0.32 * ph);
         p.rot(5, 0.0, 0.0, -0.32 * ph);
         p.offAll(0.0, -1.8 * Math.sin(ph * Math.PI), 0.0);
      }),
      new Emote("verbeugen", Map.of("de", "Verbeugen", "en", "Bow"), "☺", 60, (p, t) -> {
         double a = 0.95 * env(t, 60.0);
         p.rot(1, a, 0.0, 0.0);
         p.rot(0, a, 0.0, 0.0);
         double dy = 2.0 * Math.cos(a) - 2.0;
         double dz = 2.0 * Math.sin(a);
         p.rot(2, a - 0.9 * env(t, 60.0), 0.4 * env(t, 60.0), 0.0);
         p.off(2, 0.0, dy, dz);
         p.rot(3, a, 0.0, -0.05);
         p.off(3, 0.0, dy, dz);
      }),
      new Emote("sitzen", Map.of("de", "Hinsetzen", "en", "Sit"), "☕", 0, (p, t) -> {
         p.rot(4, -1.45, 0.18, 0.0);
         p.rot(5, -1.45, -0.18, 0.0);
         p.rot(2, -0.6, 0.0, 0.1);
         p.rot(3, -0.6, 0.0, -0.1);
         p.offAll(0.0, 10.0, 0.0);
      }),
      new Emote("tpose", Map.of("de", "T-Pose", "en", "T-Pose"), "T", 0, (p, t) -> {
         p.rot(2, 0.0, 0.0, Math.PI / 2);
         p.rot(3, 0.0, 0.0, -Math.PI / 2);
         p.rot(0, 0.0, 0.0, 0.0);
      }),
      new Emote("salutieren", Map.of("de", "Salutieren", "en", "Salute"), "✪", 50, (p, t) -> {
         double k = env(t, 50.0);
         p.rot(2, -2.3 * k, -0.6 * k, 0.9 * k);
         p.rot(0, -0.1 * k, 0.0, 0.0);
      }),
      new Emote("schulterzucken", Map.of("de", "Schulterzucken", "en", "Shrug"), "?", 40, (p, t) -> {
         double k = env(t, 40.0);
         p.rot(2, -0.4 * k, 0.0, 0.9 * k);
         p.rot(3, -0.4 * k, 0.0, -0.9 * k);
         p.off(2, 0.0, -1.0 * k, 0.0);
         p.off(3, 0.0, -1.0 * k, 0.0);
         p.rot(0, 0.0, 0.0, 0.25 * k * sin(t * 0.3));
      }),
      new Emote("facepalm", Map.of("de", "Facepalm", "en", "Facepalm"), "✋", 50, (p, t) -> {
         double k = env(t, 50.0);
         p.rot(2, -2.0 * k, 0.75 * k, 0.0);
         p.rot(0, 0.45 * k, 0.1 * k * sin(t * 0.4), 0.0);
      }),
      new Emote("headbang", Map.of("de", "Headbang", "en", "Headbang"), "♬", 0, (p, t) -> {
         double b = Math.abs(sin(t * 0.55));
         p.rot(0, -0.3 + b * 0.9, 0.0, 0.0);
         p.rot(2, -2.6, 0.0, 0.3);
         p.rot(3, -0.3, 0.0, -0.1);
         p.offAll(0.0, -b * 0.4, 0.0);
      }),
      new Emote("flugzeug", Map.of("de", "Flugzeug", "en", "Airplane"), "✈", 0, (p, t) -> {
         double tilt = sin(t * 0.15) * 0.35;
         p.rot(2, 0.0, 0.0, (Math.PI / 2) + tilt);
         p.rot(3, 0.0, 0.0, (-Math.PI / 2) + tilt);
         p.off(2, 0.0, -tilt * 3.0, 0.0);
         p.off(3, 0.0, tilt * 3.0, 0.0);
         p.rot(0, 0.0, 0.0, tilt * 0.5);
      }),
      new Emote("herz", Map.of("de", "Herz", "en", "Heart"), "♥", 0, (p, t) -> {
         p.rot(2, -2.75, 0.0, -0.45);
         p.rot(3, -2.75, 0.0, 0.45);
         p.offAll(0.0, -Math.abs(sin(t * 0.2)) * 0.6, 0.0);
      }),
      new Emote("luftgitarre", Map.of("de", "Luftgitarre", "en", "Air Guitar"), "♪", 0, (p, t) -> {
         p.rot(3, -1.1, -0.6, 0.0);
         p.rot(2, -0.8 + sin(t * 1.1) * 0.35, 0.5, 0.0);
         p.rot(0, 0.25 + Math.abs(sin(t * 0.4)) * 0.3, 0.0, 0.0);
         p.rot(4, -0.25, 0.0, 0.0);
      }),
      new Emote("boxen", Map.of("de", "Schattenboxen", "en", "Shadow Boxing"), "✊", 0, (p, t) -> {
         double a = Math.max(0.0, sin(t * 0.7));
         double b = Math.max(0.0, -sin(t * 0.7));
         p.rot(2, -1.2 - a * 0.4, 0.3 - a * 0.3, 0.0);
         p.rot(3, -1.2 - b * 0.4, -0.3 + b * 0.3, 0.0);
         p.off(2, 0.0, 0.0, -a * 3.0);
         p.off(3, 0.0, 0.0, -b * 3.0);
         p.offAll(0.0, -Math.abs(sin(t * 0.35)) * 0.6, 0.0);
      }),
      new Emote("meditieren", Map.of("de", "Meditieren", "en", "Meditate"), "☯", 0, (p, t) -> {
         p.rot(4, -1.5, 0.9, 0.0);
         p.rot(5, -1.5, -0.9, 0.0);
         p.rot(2, -0.7, 0.0, 0.5);
         p.rot(3, -0.7, 0.0, -0.5);
         p.offAll(0.0, 10.0, 0.0);
         p.offUpper(0.0, sin(t * 0.08) * 0.5, 0.0);
      }),
      new Emote("kreiseln", Map.of("de", "Kreiseln", "en", "Spin"), "↻", 0, (p, t) -> spin(p, t * 0.35, false)),
      new Emote("zombie", Map.of("de", "Zombie", "en", "Zombie"), "☠", 0, (p, t) -> {
         p.rot(2, -1.55 + sin(t * 0.2) * 0.1, 0.0, 0.0);
         p.rot(3, -1.55 - sin(t * 0.2) * 0.1, 0.0, 0.0);
         p.rot(0, 0.2, 0.0, sin(t * 0.1) * 0.25);
         p.rot(4, sin(t * 0.2) * 0.3, 0.0, 0.0);
         p.rot(5, -sin(t * 0.2) * 0.3, 0.0, 0.0);
      }),
      new Emote("huhn", Map.of("de", "Hühnertanz", "en", "Chicken Dance"), "♣", 0, (p, t) -> {
         double f = Math.abs(sin(t * 0.6));
         p.rot(2, -0.3, 0.0, 0.3 + f * 0.9);
         p.rot(3, -0.3, 0.0, -0.3 - f * 0.9);
         p.rot(0, 0.0, 0.0, 0.0);
         p.off(0, 0.0, 0.0, -f * 1.2);
         p.offAll(0.0, f * 1.5, 0.0);
      }),
      new Emote("schwimmen", Map.of("de", "Trockenschwimmen", "en", "Dry Swim"), "~", 0, (p, t) -> {
         p.rot(2, -Math.PI + sin(t * 0.3) * 1.6, 0.0, 0.2);
         p.rot(3, -Math.PI - sin(t * 0.3) * 1.6, 0.0, -0.2);
         p.rot(0, 0.0, sin(t * 0.3) * 0.4, 0.0);
      }),
      new Emote("kniebeugen", Map.of("de", "Kniebeugen", "en", "Squats"), "↓", 0, (p, t) -> {
         double d = (1.0 - Math.cos(t * 0.25)) / 2.0;
         p.rot(4, -1.0 * d, 0.0, 0.0);
         p.rot(5, -1.0 * d, 0.0, 0.0);
         p.rot(2, -1.5 * d, 0.0, 0.0);
         p.rot(3, -1.5 * d, 0.0, 0.0);
         p.offAll(0.0, 5.0 * d, 0.0);
         p.off(4, 0.0, 0.0, -0.5 * d);
         p.off(5, 0.0, 0.0, -0.5 * d);
      }),
      new Emote("hula", Map.of("de", "Hula", "en", "Hula"), "❀", 0, (p, t) -> {
         double s = sin(t * 0.4);
         double c = Math.cos(t * 0.4);
         p.offUpper(s * 1.5, 0.0, c * 1.0);
         p.rot(2, -0.2, 0.0, 1.4 + s * 0.3);
         p.rot(3, -0.2, 0.0, -1.4 + s * 0.3);
         p.rot(0, 0.0, 0.0, -s * 0.15);
      }),
      new Emote("pinguin", Map.of("de", "Pinguin", "en", "Penguin"), "❄", 0, (p, t) -> {
         double s = sin(t * 0.5);
         p.rot(2, 0.0, 0.0, 0.35 + Math.max(0.0, s) * 0.4);
         p.rot(3, 0.0, 0.0, -0.35 - Math.max(0.0, -s) * 0.4);
         p.rot(4, 0.0, 0.0, Math.max(0.0, s) * 0.25);
         p.rot(5, 0.0, 0.0, -Math.max(0.0, -s) * 0.25);
         p.offAll(-s * 0.8, 0.0, 0.0);
         p.rot(0, 0.0, 0.0, s * 0.15);
      }),
      new Emote("wellen", Map.of("de", "Armwelle", "en", "Arm Wave"), "≈", 0, (p, t) -> {
         p.rot(2, 0.0, 0.0, (Math.PI / 2) + sin(t * 0.4) * 0.5);
         p.rot(3, 0.0, 0.0, (-Math.PI / 2) + sin(t * 0.4 - 1.5) * 0.5);
         p.offUpper(0.0, sin(t * 0.4 - 0.75) * 0.8, 0.0);
      }),
      new Emote("winken_beide", Map.of("de", "Beidhändig winken", "en", "Double Wave"), "✌", 0, (p, t) -> {
         p.rot(2, -2.9, 0.0, 0.15 + sin(t * 0.5) * 0.45);
         p.rot(3, -2.9, 0.0, -0.15 - sin(t * 0.5) * 0.45);
         p.offAll(0.0, -Math.max(0.0, sin(t * 0.5)) * 0.8, 0.0);
      }),
      new Emote("ohnmacht", Map.of("de", "Umfallen", "en", "Faint"), "✖", 0, (p, t) -> {
         double k = Math.min(1.0, t / 12.0);
         k *= k;
         double a = -1.45 * k;
         pivotTilt(p, a);
         p.rot(2, a, 0.0, 0.6 * k);
         p.rot(3, a, 0.0, -0.6 * k);
      }),
      new Emote("take_the_l", Map.of("de", "Take the L", "en", "Take the L"), "L", 0, (p, t) -> {
         double s = sin(t * 0.45);
         p.rot(2, -2.7, -0.45, 0.2);
         p.rot(3, -0.25, 0.0, -0.85 - 0.25 * s);
         p.rot(4, -1.0 * Math.max(0.0, s), 0.0, 0.12);
         p.rot(5, -1.0 * Math.max(0.0, -s), 0.0, -0.12);
         p.rot(0, 0.05, 0.25 * s, 0.0);
         p.offAll(0.0, -Math.abs(s) * 1.3, 0.0);
      }, "vip"),
      new Emote("griddy", Map.of("de", "Griddy", "en", "Griddy"), "♞", 0, (p, t) -> {
         double s = sin(t * 0.62);
         p.rot(1, 0.18, 0.0, 0.0);
         p.rot(0, 0.25, 0.0, 0.0);
         p.rot(4, 0.35 - 0.9 * Math.max(0.0, s), 0.0, 0.0);
         p.rot(5, 0.35 - 0.9 * Math.max(0.0, -s), 0.0, 0.0);
         p.rot(2, -0.5 + 1.25 * s, 0.0, 0.25);
         p.rot(3, -0.5 - 1.25 * s, 0.0, -0.25);
         p.offAll(0.0, -Math.abs(s) * 0.7, -0.6);
      }, "vip"),
      new Emote("moonwalk", Map.of("de", "Moonwalk", "en", "Moonwalk"), "☾", 0, (p, t) -> {
         double s = sin(t * 0.3);
         double slide = Math.asin(sin(t * 0.06)) / (Math.PI / 2);
         p.rot(4, 0.25 * Math.max(0.0, s), 0.0, 0.0);
         p.rot(5, 0.25 * Math.max(0.0, -s), 0.0, 0.0);
         p.off(4, 0.0, -0.8 * Math.max(0.0, s), 0.0);
         p.off(5, 0.0, -0.8 * Math.max(0.0, -s), 0.0);
         p.rot(2, 0.25 * s, 0.0, 0.1);
         p.rot(3, -0.25 * s, 0.0, -0.1);
         p.rot(0, -0.1, 0.0, 0.0);
         p.offAll(0.0, 0.0, slide * 4.0);
      }, "vip"),
      new Emote("karate", Map.of("de", "Karate", "en", "Karate"), "✊", 0, (p, t) -> {
         int step = (int)(t / 12.0) % 4;
         double k = Math.min(1.0, t % 12.0 / 4.0);
         p.rot(2, step == 1 ? -1.57 * k : -1.0, 0.0, 0.3);
         p.rot(3, step == 3 ? -1.57 * k : -1.0, 0.0, -0.3);
         p.rot(4, step == 0 ? -1.4 * k : 0.0, 0.0, step == 0 ? 0.2 * k : 0.0);
         p.rot(5, step == 2 ? -1.4 * k : 0.0, 0.0, step == 2 ? -0.2 * k : 0.0);
         p.rot(1, 0.0, (step % 2 == 0 ? 0.3 : -0.3) * k, 0.0);
         p.offAll(0.0, 0.8, 0.0);
      }, "vip"),
      new Emote("auslachen", Map.of("de", "Auslachen", "en", "Laugh At"), "☺", 0, (p, t) -> {
         double shake = sin(t * 1.6);
         p.rot(2, -1.55, -0.1, 0.0);
         p.rot(3, -0.9, 0.7, -0.1);
         p.rot(0, -0.35 + shake * 0.08, 0.0, 0.0);
         p.rot(1, -0.08 + shake * 0.04, 0.0, 0.0);
         p.offUpper(0.0, Math.abs(shake) * 0.4, 0.0);
      }, "vip"),
      new Emote("weinen", Map.of("de", "Weinen", "en", "Cry"), "☂", 0, (p, t) -> {
         double sob = sin(t * 0.9);
         p.rot(2, -2.2, -0.5, 0.0);
         p.rot(3, -2.2, 0.5, 0.0);
         p.rot(0, 0.45, 0.0, 0.0);
         p.offUpper(0.0, 0.3 + sob * 0.25, 0.0);
      }, "vip"),
      new Emote("party", Map.of("de", "Partytanz", "en", "Party Dance"), "♪", 0, (p, t) -> {
         double s = sin(t * 0.55);
         double b = Math.abs(sin(t * 0.55));
         p.rot(2, -1.2 + 0.9 * Math.max(0.0, s), 0.0, 0.35);
         p.rot(3, -1.2 + 0.9 * Math.max(0.0, -s), 0.0, -0.35);
         p.rot(0, 0.15 * b, 0.0, 0.0);
         p.rot(4, -0.25 * b, 0.0, 0.05);
         p.rot(5, -0.25 * b, 0.0, -0.05);
         p.offAll(0.0, b * 0.8, 0.0);
      }, "vip"),
      new Emote("muskeln", Map.of("de", "Muskeln zeigen", "en", "Flex"), "✊", 0, (p, t) -> {
         double s = sin(t * 0.25);
         p.rot(2, -0.3, 0.0, 1.45 + 0.2 * Math.max(0.0, s));
         p.rot(3, -0.3, 0.0, -1.45 - 0.2 * Math.max(0.0, -s));
         p.rot(0, -0.15, 0.35 * s, 0.0);
         p.rot(1, 0.0, 0.12 * s, 0.0);
         p.rot(4, 0.0, 0.0, 0.15);
         p.rot(5, 0.0, 0.0, -0.15);
      }, "vip"),
      new Emote("lasso", Map.of("de", "Lasso-Reiter", "en", "Lasso Rider"), "∞", 0, (p, t) -> {
         double s = sin(t * 0.5);
         double a = t * 0.5;
         p.rot(2, -2.6, a, 0.3);
         p.rot(3, -1.1, 0.0, -0.1);
         p.rot(4, -0.5 - 0.3 * s, 0.0, 0.35);
         p.rot(5, -0.5 - 0.3 * s, 0.0, -0.35);
         p.rot(1, 0.15, 0.0, 0.0);
         p.rot(0, -0.1 + 0.1 * s, 0.0, 0.0);
         p.offAll(0.0, 1.5 - Math.abs(s) * 2.0, 0.0);
      }, "vip"),
      new Emote("daumen", Map.of("de", "Daumen hoch", "en", "Thumbs Up"), "✔", 50, (p, t) -> {
         double k = env(t, 50.0);
         p.rot(2, -1.4 * k, -0.3 * k, 0.0);
         p.rot(3, -1.4 * k, 0.3 * k, 0.0);
         p.rot(0, (-0.15 + 0.08 * sin(t * 0.5)) * k, 0.0, 0.0);
      }, "vip"),
      new Emote("salto", Map.of("de", "Rückwärtssalto", "en", "Backflip"), "↺", 40, (p, t) -> {
         double k = Math.max(0.0, Math.min(1.0, (t - 6.0) / 22.0));
         double a = -k * Math.PI * 2.0;
         flip(p, a);
         double jump = Math.sin(Math.PI * k) * 9.0;
         p.offAll(0.0, -jump, 0.0);
         double tuck = Math.sin(Math.PI * k);
         p.rot(4, a - 1.4 * tuck, 0.0, 0.0);
         p.rot(5, a - 1.4 * tuck, 0.0, 0.0);
         p.rot(2, a - 2.6 * tuck, 0.0, 0.2);
         p.rot(3, a - 2.6 * tuck, 0.0, -0.2);
      }, "legend"),
      new Emote("schweben", Map.of("de", "Schweben", "en", "Levitate"), "☁", 0, (p, t) -> {
         double s = sin(t * 0.12);
         p.offAll(0.0, -5.0 - s * 1.2, 0.0);
         p.rot(2, -0.2, 0.0, 1.15 + s * 0.1);
         p.rot(3, -0.2, 0.0, -1.15 - s * 0.1);
         p.rot(4, 0.15, 0.0, 0.05);
         p.rot(5, 0.25, 0.0, -0.05);
         p.rot(0, -0.25, 0.0, 0.0);
      }, "legend"),
      new Emote("tornado", Map.of("de", "Tornado", "en", "Tornado"), "@", 0, (p, t) -> {
         spin(p, t * 0.9, true);
         p.offAll(0.0, -Math.abs(sin(t * 0.3)) * 1.5, 0.0);
      }, "legend"),
      new Emote("shuffle", Map.of("de", "Shuffle", "en", "Shuffle"), "⇆", 0, (p, t) -> {
         double s = sin(t * 0.7);
         p.rot(4, -0.5 * Math.max(0.0, s), 0.0, 0.45 * Math.max(0.0, s));
         p.rot(5, -0.5 * Math.max(0.0, -s), 0.0, -0.45 * Math.max(0.0, -s));
         p.rot(2, -0.9 - 0.4 * s, 0.0, 0.2);
         p.rot(3, -0.9 + 0.4 * s, 0.0, -0.2);
         p.rot(1, 0.0, s * 0.15, 0.0);
         p.offAll(s * 1.2, -Math.abs(s) * 1.0, 0.0);
      }, "legend"),
      new Emote("helikopter", Map.of("de", "Helikopter", "en", "Helicopter"), "✢", 0, (p, t) -> {
         double a = t * 0.8;
         p.rot(2, 0.0, a, 2.9);
         p.rot(3, -0.2, 0.0, -0.5);
         p.rot(0, -0.3, 0.0, 0.0);
         p.offAll(0.0, -Math.abs(sin(t * 0.2)) * 2.0, 0.0);
      }, "legend"),
      new Emote("ninja", Map.of("de", "Ninja-Lauf", "en", "Ninja Run"), "»", 0, (p, t) -> {
         double s = sin(t * 0.9);
         p.rot(1, 0.55, 0.0, 0.0);
         p.rot(0, 0.2, 0.0, 0.0);
         p.rot(2, 1.35, 0.0, 0.15);
         p.rot(3, 1.35, 0.0, -0.15);
         p.off(2, 0.0, 0.6, 2.2);
         p.off(3, 0.0, 0.6, 2.2);
         p.off(0, 0.0, 0.8, -2.5);
         p.rot(4, 1.0 * s, 0.0, 0.0);
         p.rot(5, -1.0 * s, 0.0, 0.0);
         p.offAll(0.0, -Math.abs(s) * 0.8, 0.0);
      }, "legend"),
      new Emote("matrix", Map.of("de", "Matrix-Ausweichen", "en", "Matrix Dodge"), "⟲", 0, (p, t) -> {
         double s = sin(t * 0.12);
         pivotTilt(p, -1.0 - 0.15 * s);
         p.rot(2, -0.3 + sin(t * 0.4) * 0.6, 0.0, 1.3);
         p.rot(3, -0.3 - sin(t * 0.4) * 0.6, 0.0, -1.3);
      }, "legend"),
      new Emote("aufladen", Map.of("de", "Kraft aufladen", "en", "Power Up"), "⚡", 0, (p, t) -> {
         double j = sin(t * 3.1) * 0.25;
         double k = sin(t * 0.08);
         p.rot(2, 0.25, 0.0, 0.45 + j * 0.2);
         p.rot(3, 0.25, 0.0, -0.45 - j * 0.2);
         p.rot(4, -0.2, 0.0, 0.3);
         p.rot(5, -0.2, 0.0, -0.3);
         p.rot(0, -0.35 - 0.1 * k, 0.0, 0.0);
         p.rot(1, -0.1, 0.0, 0.0);
         p.offAll(j, 1.0 - 0.6 * k, 0.0);
      }, "legend"),
      new Emote("windmuehle", Map.of("de", "Windmühle", "en", "Windmill"), "✣", 0, (p, t) -> {
         double a = t * 0.45;
         p.rot(2, a, 0.0, 0.3);
         p.rot(3, a + Math.PI, 0.0, -0.3);
         p.rot(1, 0.0, 0.15 * sin(a), 0.0);
         p.rot(0, 0.0, 0.15 * sin(a), 0.0);
         p.offAll(0.0, -Math.abs(sin(a)) * 0.6, 0.0);
      }, "legend"),
      new Emote("schnipsen", Map.of("de", "Fingerschnipsen", "en", "Finger Snap"), "✧", 60, (p, t) -> {
         double k = env(t, 60.0);
         double snap = t > 28.0 && t < 34.0 ? sin((t - 28.0) * 1.2) * 0.25 : 0.0;
         p.rot(2, (-1.9 + snap) * k, 0.2 * k, 0.0);
         p.rot(3, -0.2 * k, 0.0, -0.15 * k);
         p.rot(0, (t > 34.0 ? 0.2 : -0.1) * k, -0.2 * k, 0.0);
      }, "admin"),
      new Emote("machtwelle", Map.of("de", "Machtwelle", "en", "Force Push"), "≫", 0, (p, t) -> {
         double c = t % 30.0 / 30.0;
         double push = c < 0.25 ? c / 0.25 : Math.max(0.0, 1.0 - (c - 0.25) / 0.6);
         p.rot(2, -0.6 - 1.0 * push, -0.2, 0.0);
         p.rot(3, -0.6 - 1.0 * push, 0.2, 0.0);
         p.rot(1, 0.2 * push, 0.0, 0.0);
         p.rot(4, -0.4 * push, 0.0, 0.0);
         p.rot(5, 0.3 * push, 0.0, 0.0);
         p.offAll(0.0, 0.0, -1.5 * push);
      }, "admin"),
      new Emote("thron", Map.of("de", "Unsichtbarer Thron", "en", "Invisible Throne"), "♜", 0, (p, t) -> {
         double s = sin(t * 0.1);
         p.rot(4, -1.5, 0.15, 0.0);
         p.rot(5, -1.5, -0.15, 0.0);
         p.rot(2, -0.8, 0.0, 0.35);
         p.rot(3, -0.8, 0.0, -0.35);
         p.rot(0, -0.05, 0.3 * s, 0.0);
         p.offAll(0.0, -2.0 - s * 0.6, 0.0);
         p.off(4, 0.0, -1.0, -1.5);
         p.off(5, 0.0, -1.0, -1.5);
      }, "admin"),
      new Emote("bannhammer", Map.of("de", "Bann-Hammer", "en", "Ban Hammer"), "⚒", 0, (p, t) -> {
         double c = t % 30.0 / 30.0;
         double arm = c < 0.6 ? -3.0 * (c / 0.6) : -3.0 + 3.6 * Math.min(1.0, (c - 0.6) / 0.12);
         double hit = c > 0.6 && c < 0.8 ? 1.0 : 0.0;
         p.rot(2, arm, 0.0, 0.1);
         p.rot(3, -0.4, 0.0, -0.3);
         p.rot(1, 0.25 * hit - 0.1 * (c < 0.6 ? c / 0.6 : 0.0), 0.0, 0.0);
         p.rot(0, 0.3 * hit, 0.0, 0.0);
         p.offAll(0.0, hit * 0.8, 0.0);
      }, "admin"),
      new Emote("glitch", Map.of("de", "Glitch", "en", "Glitch"), "▒", 0, (p, t) -> {
         long f = (long)(t / 2.0);
         Random r = new Random(f * 31337L);

         for (int i = 0; i < 6; i++) {
            p.rot(i, (r.nextDouble() - 0.5) * 0.9, (r.nextDouble() - 0.5) * 0.9, (r.nextDouble() - 0.5) * 0.6);
         }

         if (r.nextInt(4) == 0) {
            p.offAll((r.nextDouble() - 0.5) * 6.0, -r.nextDouble() * 2.0, (r.nextDouble() - 0.5) * 6.0);
         }
      }, "admin"),
      new Emote("schwerelos", Map.of("de", "Schwerelos", "en", "Zero Gravity"), "☄", 0, (p, t) -> {
         flip(p, t * 0.04);
         p.offAll(0.0, -8.0 - sin(t * 0.1) * 1.5, 0.0);
      }, "admin"),
      new Emote("dirigent", Map.of("de", "Dirigent", "en", "Conductor"), "♩", 0, (p, t) -> {
         double s = sin(t * 0.35);
         double c = Math.cos(t * 0.35);
         p.rot(2, -1.7 + 0.5 * s, 0.4 * c, 0.2);
         p.rot(3, -1.4 - 0.3 * c, -0.3 * s, -0.2);
         p.rot(0, -0.15 + 0.1 * s, 0.2 * c, 0.0);
         p.rot(1, 0.05, 0.08 * c, 0.0);
      }, "admin"),
      new Emote("kroenung", Map.of("de", "Krönung", "en", "Coronation"), "♛", 90, (p, t) -> {
         double k = env(t, 90.0);
         double up = Math.min(1.0, t / 30.0);
         double down = Math.max(0.0, Math.min(1.0, (t - 40.0) / 25.0));
         double raise = up * (1.0 - down * 0.55);
         p.rot(2, -0.3 * raise * k, 0.0, 2.8 * raise * k);
         p.rot(3, -0.3 * raise * k, 0.0, -2.8 * raise * k);
         p.rot(0, -0.35 * raise * k, 0.0, 0.0);
         p.offAll(0.0, -1.5 * down * k, 0.0);
      }, "owner"),
      new Emote("koenig", Map.of("de", "Königsgruß", "en", "Royal Wave"), "♚", 0, (p, t) -> {
         p.rot(2, -2.4, 0.0, 0.25 + sin(t * 0.25) * 0.15);
         p.rot(3, -0.5, 0.9, -0.7);
         p.rot(0, -0.2, sin(t * 0.08) * 0.35, 0.0);
      }, "owner"),
      new Emote("weltherrscher", Map.of("de", "Weltherrscher", "en", "World Ruler"), "✺", 0, (p, t) -> {
         spin(p, t * 0.05, true);
         p.offAll(0.0, -6.0 - sin(t * 0.1) * 1.5, 0.0);
         p.rot(0, -0.4, t * 0.05, 0.0);
      }, "owner"),
      new Emote("geldregen", Map.of("de", "Geldregen", "en", "Make It Rain"), "$", 0, (p, t) -> {
         double s = sin(t * 0.8);
         p.rot(2, -1.3 - 0.5 * Math.max(0.0, s), -0.2, 0.1);
         p.rot(3, -1.3 - 0.5 * Math.max(0.0, -s), 0.2, -0.1);
         p.rot(0, -0.2, 0.15 * sin(t * 0.2), 0.0);
         p.offAll(0.0, Math.abs(sin(t * 0.4)) * 0.5, 0.0);
      }, "owner"),
      new Emote("engel", Map.of("de", "Engelsflug", "en", "Angel Flight"), "☼", 0, (p, t) -> {
         double s = sin(t * 0.3);
         p.offAll(0.0, -9.0 - s * 2.0, 0.0);
         p.rot(2, 0.3, 0.0, 1.2 + 0.6 * s);
         p.rot(3, 0.3, 0.0, -1.2 - 0.6 * s);
         p.rot(4, 0.2, 0.0, 0.0);
         p.rot(5, 0.3, 0.0, 0.0);
         p.rot(1, 0.1, 0.0, 0.0);
         p.rot(0, -0.2, 0.0, 0.0);
      }, "owner"),
      new Emote("gottmodus", Map.of("de", "Gott-Modus", "en", "God Mode"), "✹", 0, (p, t) -> {
         double up = Math.min(1.0, t / 40.0);
         spin(p, t * 0.03, true);
         p.offAll(0.0, -12.0 * up - sin(t * 0.1), 0.0);
         p.rot(0, -0.5 * up, t * 0.03, 0.0);
      }, "owner"),
      new Emote("sieg", Map.of("de", "Siegerpose", "en", "Victory"), "♔", 60, (p, t) -> {
         double k = env(t, 60.0);
         double jump = t < 20.0 ? Math.sin(Math.PI * t / 20.0) * 6.0 : 0.0;
         p.rot(2, -3.0 * k, 0.0, 0.1);
         p.rot(3, -0.4 * k, 0.6 * k, -0.6 * k);
         p.rot(0, -0.4 * k, 0.0, 0.0);
         p.rot(4, t < 20.0 ? -0.8 * Math.sin(Math.PI * t / 20.0) : 0.0, 0.0, 0.1 * k);
         p.rot(5, 0.0, 0.0, -0.1 * k);
         p.offAll(0.0, -jump, 0.0);
      }, "owner"),
      new Emote("schwertschlag", Map.of("de", "Schwertschlag", "en", "Sword Slash"), "⚔", 40, (p, t) -> {
         double k = env(t, 40.0);
         double sw = t < 16.0 ? t / 16.0 : Math.max(0.0, 1.0 - (t - 16.0) / 6.0);
         p.rot(2, (-2.9 * sw - 0.6 * (1.0 - sw)) * k, -0.3 * k, 0.2 * k);
         p.rot(3, -0.5 * k, 0.3 * k, -0.2 * k);
         p.rot(1, 0.0, (0.4 * sw - 0.3 * (1.0 - sw)) * k, 0.0);
         p.rot(0, 0.1 * k, (0.3 * sw - 0.2 * (1.0 - sw)) * k, 0.0);
      }),
      new Emote("schildblock", Map.of("de", "Schild hoch", "en", "Shield Up"), "⛨", 0, (p, t) -> {
         double b = sin(t * 0.25) * 0.05;
         p.rot(3, -1.45 + b, 0.55, 0.0);
         p.rot(2, -0.6, -0.2, 0.1);
         p.rot(0, 0.3, 0.0, 0.0);
         p.rot(1, 0.2, 0.0, 0.0);
         p.offAll(0.0, 1.0, 0.0);
         p.rot(4, -0.25, 0.0, 0.05);
         p.rot(5, 0.25, 0.0, -0.05);
      }),
      new Emote("angeln", Map.of("de", "Angeln", "en", "Fishing"), "≋", 0, (p, t) -> {
         double cyc = t % 60.0;
         double jerk = cyc > 48.0 ? Math.sin(Math.PI * (cyc - 48.0) / 12.0) * 0.9 : sin(t * 0.2) * 0.04;
         p.rot(2, -1.1 - jerk, -0.15, 0.0);
         p.rot(3, -0.9 - jerk * 0.7, 0.35, 0.0);
         p.rot(0, 0.25 - jerk * 0.2, 0.0, 0.0);
      }),
      new Emote("schlafen", Map.of("de", "Einschlafen", "en", "Doze Off"), "☾", 0, (p, t) -> {
         double nod = Math.max(0.0, sin(t * 0.08)) * 0.55;
         p.rot(0, 0.35 + nod, 0.0, 0.1);
         p.rot(2, -0.9, -0.7, 0.0);
         p.rot(3, -0.9, 0.7, 0.0);
         p.rot(1, 0.1 + nod * 0.15, 0.0, 0.0);
      }),
      new Emote("handy", Map.of("de", "Am Handy", "en", "On The Phone"), "▯", 0, (p, t) -> {
         double sc = sin(t * 0.6) * 0.06;
         p.rot(2, -1.25 + sc, -0.45, 0.0);
         p.rot(3, -1.1, 0.4, 0.0);
         p.rot(0, 0.65, 0.0, 0.0);
      }),
      new Emote("selfie", Map.of("de", "Selfie", "en", "Selfie"), "◉", 70, (p, t) -> {
         double k = env(t, 70.0);
         p.rot(2, -2.2 * k, 0.6 * k, 0.2 * k);
         p.rot(3, -2.6 * k, -0.2 * k, -0.4 * k);
         p.rot(0, -0.35 * k, -0.35 * k, 0.2 * k);
         p.offAll(0.0, t > 30.0 && t < 40.0 ? -0.6 : 0.0, 0.0);
      }),
      new Emote("kusshand", Map.of("de", "Kusshand", "en", "Blow A Kiss"), "♡", 50, (p, t) -> {
         double k = env(t, 50.0);
         double th = t < 20.0 ? 0.0 : Math.min(1.0, (t - 20.0) / 8.0);
         p.rot(2, (-2.0 + 0.6 * th) * k, (0.55 - 0.55 * th) * k, 0.15 * th * k);
         p.rot(0, (0.1 - 0.2 * th) * k, 0.0, -0.15 * k);
      }),
      new Emote("gg", Map.of("de", "GG", "en", "GG"), "✌", 60, (p, t) -> {
         double k = env(t, 60.0);
         double bow = t < 25.0 ? Math.sin(Math.PI * t / 25.0) : 0.0;
         p.rot(1, 0.5 * bow * k, 0.0, 0.0);
         p.rot(0, 0.6 * bow * k, 0.0, 0.0);
         p.offUpper(0.0, 0.0, -2.5 * bow * k);
         double hand = t >= 25.0 ? k : 0.0;
         p.rot(2, -1.4 * hand, 0.0, 0.05);
         p.rot(3, 0.2 * bow * k, 0.0, -0.05);
      }),
      new Emote("bogenschuss", Map.of("de", "Bogenschuss", "en", "Archer"), "➶", 0, (p, t) -> {
         double cyc = t % 40.0 / 40.0;
         double draw = cyc < 0.7 ? cyc / 0.7 : 0.0;
         p.rot(3, -1.57, 0.35, 0.0);
         p.rot(2, -1.57, -0.2 - 0.75 * draw, 0.0);
         p.rot(0, 0.0, 0.45, 0.0);
         p.rot(1, 0.0, 0.3, 0.0);
      }),
      new Emote("zaubern", Map.of("de", "Zauberspruch", "en", "Spellcast"), "✶", 0, (p, t) -> {
         double a = t * 0.18;
         p.rot(2, -2.4 + sin(a) * 0.35, Math.cos(a) * 0.4, 0.3);
         p.rot(3, -2.4 + sin(a + Math.PI) * 0.35, Math.cos(a + Math.PI) * 0.4, -0.3);
         p.rot(0, -0.35, 0.0, 0.0);
         p.offAll(0.0, -2.5 - sin(t * 0.1) * 1.0, 0.0);
         p.rot(4, 0.2, 0.0, 0.05);
         p.rot(5, 0.1, 0.0, -0.05);
      }, "legend"),
      new Emote("totem_pop", Map.of("de", "Totem-Pop", "en", "Totem Pop"), "✸", 50, (p, t) -> {
         double k = env(t, 50.0);
         double shake = t < 14.0 ? sin(t * 2.4) * 0.12 : 0.0;
         double jump = t >= 14.0 && t < 30.0 ? Math.sin(Math.PI * (t - 14.0) / 16.0) * 5.0 : 0.0;
         p.rot(2, -0.3 * k, 0.0, (2.4 + shake * 3.0) * k);
         p.rot(3, -0.3 * k, 0.0, (-2.4 + shake * 3.0) * k);
         p.rot(0, -0.5 * k, 0.0, shake);
         p.rot(1, 0.0, 0.0, shake);
         p.offAll(0.0, -jump, 0.0);
      }, "legend"),
      new Emote("kristall_spam", Map.of("de", "Kristall-Spam", "en", "Crystal Spam"), "◆", 0, (p, t) -> {
         double f = Math.abs(sin(t * 0.9));
         double g2 = Math.abs(sin(t * 0.9 + 1.3));
         p.rot(2, -0.7 - f * 0.9, -0.15, 0.0);
         p.rot(3, -0.6 - g2 * 0.6, 0.2, 0.0);
         p.rot(0, 0.5, sin(t * 0.3) * 0.15, 0.0);
         p.rot(1, 0.15, 0.0, 0.0);
         p.offAll(0.0, 0.8, 0.0);
      }, "admin"),
      new Emote("ht4_sieg", Map.of("de", "HT4-Sieg", "en", "HT4 Victory"), "✪", 90, (p, t) -> {
         double k = env(t, 90.0);
         if (t < 40.0) {
            p.rot(2, -0.95 * k, -0.75 * k, 0.0);
            p.rot(3, -0.95 * k, 0.75 * k, 0.0);
            p.rot(0, -0.15 * k, 0.25 * k, 0.0);
         } else {
            double u = Math.min(1.0, (t - 40.0) / 6.0) * k;
            double jump = t < 60.0 ? Math.sin(Math.PI * (t - 40.0) / 20.0) * 6.0 : 0.0;
            p.rot(2, -2.9 * u, 0.0, 0.35 * u);
            p.rot(3, -2.9 * u, 0.0, -0.35 * u);
            p.rot(0, -0.45 * u, 0.0, 0.0);
            p.offAll(0.0, -jump, 0.0);
         }
      }, "owner")
   );
   public static final int PAGE_SIZE = 10;

   private Emotes() {
   }

   private static double sin(double v) {
      return Math.sin(v);
   }

   private static double env(double t, double len) {
      double in = Math.min(1.0, t / 6.0);
      double out = Math.min(1.0, (len - t) / 6.0);
      double k = Math.max(0.0, Math.min(in, out));
      return k * k * (3.0 - 2.0 * k);
   }

   private static void spin(EmotePose p, double a, boolean armsOut) {
      double[][] piv = new double[][]{{0.0, 0.0, 0.0}, {0.0, 0.0, 0.0}, {-5.0, 2.0, 0.0}, {5.0, 2.0, 0.0}, {-1.9, 12.0, 0.0}, {1.9, 12.0, 0.0}};
      double c = Math.cos(a);
      double s = Math.sin(a);

      for (int i = 0; i < 6; i++) {
         double x = piv[i][0];
         double z = piv[i][2];
         double nx = x * c + z * s;
         double nz = -x * s + z * c;
         p.off(i, nx - x, 0.0, nz - z);
      }

      p.rot(0, 0.0, a, 0.0);
      p.rot(1, 0.0, a, 0.0);
      p.rot(4, 0.0, a, 0.0);
      p.rot(5, 0.0, a, 0.0);
      p.rot(2, 0.0, a, armsOut ? 1.3 : 0.0);
      p.rot(3, 0.0, a, armsOut ? -1.3 : 0.0);
   }

   private static void flip(EmotePose p, double a) {
      double[][] piv = new double[][]{{0.0, 0.0, 0.0}, {0.0, 0.0, 0.0}, {-5.0, 2.0, 0.0}, {5.0, 2.0, 0.0}, {-1.9, 12.0, 0.0}, {1.9, 12.0, 0.0}};
      double c = Math.cos(a);
      double s = Math.sin(a);

      for (int i = 0; i < 6; i++) {
         double y = piv[i][1] - 12.0;
         double z = piv[i][2];
         double ny = y * c - z * s;
         double nz = y * s + z * c;
         p.off(i, 0.0, ny - y, nz - z);
         p.rot(i, a, 0.0, 0.0);
      }
   }

   private static void pivotTilt(EmotePose p, double a) {
      double[][] piv = new double[][]{{0.0, 0.0, 0.0}, {0.0, 0.0, 0.0}, {-5.0, 2.0, 0.0}, {5.0, 2.0, 0.0}, {-1.9, 12.0, 0.0}, {1.9, 12.0, 0.0}};
      double c = Math.cos(a);
      double s = Math.sin(a);

      for (int i = 0; i < 6; i++) {
         double y = piv[i][1] - 24.0;
         double z = piv[i][2];
         double ny = y * c - z * s;
         double nz = y * s + z * c;
         p.off(i, 0.0, ny - y, nz - z);
         if (i != 2 && i != 3) {
            p.rot(i, a, 0.0, 0.0);
         }
      }
   }

   public static int pages() {
      return (ALL.size() + 10 - 1) / 10;
   }

   public static List<Emote> page(int page) {
      int from = Math.max(0, Math.min(ALL.size(), page * 10));
      return ALL.subList(from, Math.min(ALL.size(), from + 10));
   }

   public static List<Emote> all() {
      return ALL;
   }

   public static Emote get(String id) {
      if (id == null) {
         return null;
      } else {
         for (Emote e : ALL) {
            if (e.id().equals(id)) {
               return e;
            }
         }

         return null;
      }
   }
}
