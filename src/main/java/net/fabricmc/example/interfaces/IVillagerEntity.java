package net.fabricmc.example.interfaces;

import net.minecraft.village.VillagerProfession;

public interface IVillagerEntity {
    VillagerProfession getprevProfession();

    void setprevProfession(VillagerProfession pp);
}
