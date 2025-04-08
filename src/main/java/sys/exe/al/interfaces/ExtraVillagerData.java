package sys.exe.al.interfaces;

import net.minecraft.village.VillagerProfession;
import net.minecraft.registry.entry.RegistryEntry;

public interface ExtraVillagerData {
    RegistryEntry<VillagerProfession> autolec$getPrevProfession();

    void autolec$setPrevProfession(final RegistryEntry<VillagerProfession> prevProfession);
}
