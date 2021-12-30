package net.fabricmc.example.mixin;

import net.fabricmc.example.interfaces.IVillagerEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.village.VillagerProfession;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(VillagerEntity.class)
public class VillagerEntityMixin implements IVillagerEntity {
    public VillagerProfession prevProfession;

    public VillagerProfession getprevProfession() {
        return prevProfession;
    }

    public void setprevProfession(VillagerProfession pp) {
        prevProfession = pp;
    }
}
