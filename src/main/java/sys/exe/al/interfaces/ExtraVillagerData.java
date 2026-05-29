package sys.exe.al.interfaces;

import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.core.Holder;
import org.jetbrains.annotations.NotNull;

public interface ExtraVillagerData {
    Holder<@NotNull VillagerProfession> autolec$getPrevProfession();

    void autolec$setPrevProfession(final Holder<@NotNull VillagerProfession> prevProfession);
}
