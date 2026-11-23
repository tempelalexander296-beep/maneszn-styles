package dev.maneszn.advent.core;

/** One daily reward of the calendar. */
public record Reward(int day, String id, String name, String description, Rarity rarity, CosmeticSlot slot, String type, int doorColorA, int doorColorB, int ribbonColor) {}
