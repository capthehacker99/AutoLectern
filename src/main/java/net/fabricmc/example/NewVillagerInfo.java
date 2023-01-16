package net.fabricmc.example;

import net.minecraft.enchantment.Enchantment;

public class NewVillagerInfo {

    public Enchantment VE;
    public Integer price;

    public NewVillagerInfo(Enchantment VEa, Integer PEa) {
        price = PEa;
        VE = VEa;
    }
}
