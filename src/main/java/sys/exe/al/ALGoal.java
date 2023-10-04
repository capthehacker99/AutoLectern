package sys.exe.al;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.StringTokenizer;

public record ALGoal(Enchantment enchant, int lvlMin, int lvlMax, int priceMin, int priceMax) {
    public static ALGoal convertToField(final String value) {
        try {
            if(value.length() < 11)
                return null;
            final var tokenizer = new StringTokenizer(value.substring(1, value.length()-1), ";");
            if(!tokenizer.hasMoreTokens())
                return null;
            final var enchantment = Registries.ENCHANTMENT.get(new Identifier(tokenizer.nextToken()));
            if(enchantment == null)
                return null;
            if(!tokenizer.hasMoreTokens())
                return null;
            final var lvlMin = Integer.parseInt(tokenizer.nextToken());
            if(!tokenizer.hasMoreTokens())
                return null;
            final var lvlMax = Integer.parseInt(tokenizer.nextToken());
            if(!tokenizer.hasMoreTokens())
                return null;
            final var priceMin = Integer.parseInt(tokenizer.nextToken());
            if(!tokenizer.hasMoreTokens())
                return null;
            final var priceMax = Integer.parseInt(tokenizer.nextToken());
            return new ALGoal(enchantment, lvlMin, lvlMax, priceMin, priceMax);
        }catch (final Exception ignored) {
            return null;
        }
    }
    public String convertFromField() {
        final var id = Registries.ENCHANTMENT.getId(this.enchant());
        return "{" + (id == null ? "minecraft:power" : id.toString()) + ";" + this.lvlMin() + ";" + this.lvlMax() + ";" + this.priceMin() + ";" + this.priceMax() + "}";
    }
}