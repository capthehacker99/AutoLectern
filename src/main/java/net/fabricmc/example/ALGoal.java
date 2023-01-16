package net.fabricmc.example;

import net.minecraft.enchantment.Enchantment;

public class ALGoal {
    public byte type;
    public Integer min;
    public Integer max;
    public Enchantment enchant;

    public ALGoal(byte typeA, Enchantment enchantA) {
        type = typeA;
        enchant = enchantA;
    }

    public ALGoal(Enchantment enchantA, Integer minA, Integer maxA) {
        type = (byte) 3;
        enchant = enchantA;
        min = minA;
        max = maxA;
    }
}
