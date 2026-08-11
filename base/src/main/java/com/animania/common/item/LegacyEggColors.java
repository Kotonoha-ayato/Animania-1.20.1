package com.animania.common.item;

import java.util.Map;

/** Exact 1.12 {@code ISpawnable} egg colours, keyed by legacy breed/entity ID. */
public final class LegacyEggColors {
    public record Colors(int primary, int secondary) { }

    private static final Map<String, Colors> BREEDS = Map.ofEntries(
            // Farm cows
            e("angus", 3028024, 2304560), e("friesian_cow", 15987699, 3944229),
            e("hereford", 4461056, 15987699), e("highland", 8340777, 2760475),
            e("holstein", 15987699, 2236962), e("jersey", 12089918, 16775643),
            e("longhorn", 16763795, 11227168), e("mooshroom", 12325394, 12627887),
            // Farm chickens
            e("leghorn", 15724527, 14869218), e("orpington", 15980429, 13270563),
            e("plymouth_rock", 13683925, 9735826), e("rhode_island_red", 13668724, 12480342),
            e("wyandotte", 8219743, 5129532),
            // Farm goats
            e("alpine", 14867928, 8281676), e("angora", 16776179, 13814191),
            e("fainting", 1250067, 14803425), e("kiko", 8802872, 3549475),
            e("kinder", 9263679, 13811120), e("nigerian_dwarf", 2697513, 8343350),
            e("pygmy", 9475221, 4145731),
            // Farm pigs
            e("duroc", 9399147, 6896443), e("hampshire", 5327691, 13684944),
            e("large_black", 8417906, 5326149), e("large_white", 15061714, 13876669),
            e("old_spot", 15845576, 9859698), e("yorkshire", 15845576, 15117998),
            // Farm sheep and draft horse
            e("dorper", 15987699, 13552319), e("dorper_child", 15987699, 1776411),
            e("dorset", 4863280, 15790320), e("friesian_sheep", 2039583, 4013373),
            e("jacob", 15921647, 2368548), e("merino", 15526109, 11904114),
            e("suffolk", 4336416, 2757652), e("draft", 8600606, 12829635),
            // Extra rabbits
            e("chinchilla", 13750737, 8289918), e("cottontail", 11310726, 7559493),
            e("dutch", 0, 16777215), e("havana", 4079166, 0), e("jack", 12692381, 6640455),
            e("lop", 16513763, 12883817), e("new_zealand", 16513529, 14211031),
            e("rex", 13419709, 5389358),
            // Extra peafowl
            e("blue", 2446225, 4361491), e("charcoal", 3815994, 3092271),
            e("opal", 5265772, 7174504), e("peach", 12419159, 6111535),
            e("purple", 2373476, 3569227), e("taupe", 12427148, 7102038),
            e("white", 15658734, 13421772),
            // Cats
            e("american_shorthair", 7434609, 0), e("asiatic", 8152144, 3684408),
            e("exotic", 11426596, 14129778), e("norwegian", 3878181, 9992290),
            e("ocelot", 11633487, 4995106), e("ragdoll", 13948116, 8741209),
            e("siamese", 12489844, 3615264), e("tabby", 4272939, 4075560),
            // Dogs
            e("blood_hound", 10838580, 3087372), e("chihuahua", 16183788, 394500),
            e("collie", 4206629, 16579836), e("corgi", 16514043, 13790014),
            e("dachshund", 16579836, 788743), e("fox", 11361596, 2830613),
            e("german_shepherd", 8476992, 2298895), e("great_dane", 8476992, 2364431),
            e("greyhound", 9198644, 789508), e("husky", 2170912, 15658734),
            e("labrador", 12623223, 4270368), e("pomeranian", 16579836, 2892836),
            e("poodle", 16118509, 11240027), e("pug", 15262687, 3750978),
            e("wolf", 12367536, 3288364)
    );

    private static final Map<String, Colors> EXACT = Map.ofEntries(
            e("ferret_grey", 13948116, 8741209), e("ferret_white", 15395298, 16447993),
            e("frog", 1860371, 1793554), e("hamster", 14603464, 14317391),
            e("hedgehog", 10451558, 14337943), e("hedgehog_albino", 12369084, 16777215),
            e("toad", 13868916, 5650205)
    );

    public static Colors forEntity(String id) {
        if (id == null || id.endsWith("_random") || id.equals("dart_frog") || id.equals("dartfrog")) return null;
        Colors exact = EXACT.get(id);
        if (exact != null) return exact;
        String breed = stripRole(id);
        if (id.equals("lamb_dorper")) breed = "dorper_child";
        if (breed.equals("friesian")) {
            breed = startsWithAny(id, "bull_", "cow_", "calf_") ? "friesian_cow" : "friesian_sheep";
        }
        return BREEDS.get(breed);
    }

    private static String stripRole(String id) {
        String[] roles = {"rooster_", "stallion_", "peachick_", "peacock_", "peahen_", "piglet_",
                "female_", "kitten_", "puppy_", "queen_", "bull_", "calf_", "chick_", "cow_",
                "buck_", "doe_", "ewe_", "foal_", "hen_", "hog_", "kid_", "kit_", "lamb_",
                "male_", "mare_", "ram_", "sow_", "tom_"};
        for (String role : roles) if (id.startsWith(role)) return id.substring(role.length());
        return id;
    }

    private static boolean startsWithAny(String value, String... prefixes) {
        for (String prefix : prefixes) if (value.startsWith(prefix)) return true;
        return false;
    }

    private static Map.Entry<String, Colors> e(String id, int primary, int secondary) {
        return Map.entry(id, new Colors(primary, secondary));
    }

    private LegacyEggColors() { }
}
