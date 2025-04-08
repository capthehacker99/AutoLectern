package sys.exe.al.mixin;

import net.minecraft.registry.entry.RegistryEntry;
import org.spongepowered.asm.mixin.Unique;
import sys.exe.al.interfaces.ExtraVillagerData;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.village.VillagerProfession;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(VillagerEntity.class)
public class VillagerEntityMixin implements ExtraVillagerData {
    @Unique
    public RegistryEntry<VillagerProfession> prevProfession;

    public RegistryEntry<VillagerProfession> autolec$getPrevProfession() {
        return prevProfession;
    }

    public void autolec$setPrevProfession(final RegistryEntry<VillagerProfession> pp) {
        prevProfession = pp;
    }
}
