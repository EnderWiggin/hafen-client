package haven;

import me.ender.ContainerInfo;

import java.util.*;

public enum GobTag {
    TREE, BUSH, LOG, STUMP, HERB,
    ANIMAL, AGGRESSIVE, CRITTER,
    
    MIDGES,
    
    DOMESTIC, YOUNG, ADULT,
    CATTLE, COW, BULL, CALF,
    GOAT, NANNY, BILLY, KID,
    HORSE, MARE, STALLION, FOAL,
    PIG, SOW, HOG, PIGLET,
    SHEEP, EWE, RAM, LAMB,
    
    GEM, ARROW,
    VEHICLE, PUSHED, //vehicle that is pushed (wheelbarrow, plow)
    
    CONTAINER, PROGRESSING, GATE,
    
    PLAYER, ME, FRIEND, FOE,
    KO, DEAD, EMPTY, READY, FULL,
    
    MENU, PICKUP, HIDDEN;
    
    private static final String[] AGGRO = {
        "/bear", "/boar", "/troll", "/wolverine", "/badger", "/adder", "/wolf", "/walrus", "/lynx", "/rat/caverat", "/moose", 
        "/mammoth", "/goat/wildgoat", "/spermwhale", "/orca"
    };
    
    private static final String[] BIG_PARTS = {
        "/orca/orcabeef", "/spermwhale/spermwhaleskull", "/spermwhale/spermwhalesteak", "/spermwhale/spermwhaleheart", "/spermwhale/spermwhaleskeleton"
    };
    private static final String[] ANIMALS = {
        "/fox", "/swan", "/bat", "/beaver", "/reddeer"
    };
    
    private static final String[] LIKE_HERB = {
        "/precioussnowflake"
    };
    
    private static final String[] LIKE_CRITTER = {
        "/terobjs/items/grub"
    };
    
    private static final String[] CRITTERS = {
        "/rat/rat", "/swan", "/squirrel", "/silkmoth", "/frog", "/rockdove", "/quail", "/toad", "/grasshopper",
        "/ladybug", "/forestsnail", "/dragonfly", "/forestlizard", "/waterstrider", "/firefly", "/sandflea",
        "/rabbit", "/crab", "/cavemoth", "/hedgehog", "/stagbeetle", "jellyfish", "/mallard", "/chicken", "/irrbloss",
        "/cavecentipede", "/bogturtle", "/moonmoth", "/monarchbutterfly", "/items/grub", "/springbumblebee", "/bayshrimp",
        "/mole" , "/lobster",
    };
    
    private static final String[] VEHICLES = {"/wheelbarrow", "/plow", "/cart", "/dugout", "/rowboat", "/vehicle/snekkja", "/vehicle/knarr", "/vehicle/wagon", "/vehicle/coracle", "/horse/mare", "/horse/stallion", "/vehicle/spark"};
    
    private static final boolean DBG = false;
    private static final Set<String> UNKNOWN = new HashSet<>();
    
    public static Set<GobTag> tags(Gob gob) {
        Set<GobTag> tags = new HashSet<>();
        GameUI gui = gob.context(GameUI.class);
        Equipory equipory = gui != null ? gui.equipory : null;
        
        String name = gob.resid();
        int sdt = gob.sdt();
        if(name != null) {
            List<String> ols = Collections.emptyList();
            synchronized (gob.ols) {
                try {
                    List<String> list = new ArrayList<>();
                    for (Gob.Overlay overlay : gob.ols) {
                        if(overlay != null && overlay.res != null) {
                            list.add(overlay.res.get().name);
                        }
                    }
                    ols = list;
                } catch (Loading e) {
                    gob.tagsUpdated();
                }
            }
    
            if(name.startsWith("gfx/terobjs/trees")) {
                if(name.endsWith("log") || name.endsWith("oldtrunk")) {
                    tags.add(LOG);
                } else if(name.contains("stump")) {
                    tags.add(STUMP);
                } else {
                    tags.add(TREE);
                }
            } else if(name.startsWith("gfx/terobjs/bushes")) {
                tags.add(BUSH);
            } else if(name.startsWith("gfx/terobjs/herbs/") || ofType(name, LIKE_HERB)) {
                tags.add(HERB);
            } else if(name.startsWith("gfx/borka/body")) {
                tags.add(PLAYER);
                Boolean me = gob.isMe();
                if(me != null) {
                    if(me) {
                        tags.add(ME);
                    } else {
                        tags.add(KinInfo.isFoe(gob) ? FOE : FRIEND);
                    }
                }
            } else if(name.startsWith("gfx/kritter/") || ofType(name, LIKE_CRITTER)) {
                if(name.endsWith("/midgeswarm")) {
                    tags.add(MIDGES);
                } else if(ofType(name, CRITTERS)) {
                    tags.add(ANIMAL);
                    tags.add(CRITTER);
                } else if(ofType(name, BIG_PARTS)) {
                    //ignore big parts of animals like Orca
                } else if(ofType(name, AGGRO)) {
                    tags.add(ANIMAL);
                    tags.add(AGGRESSIVE);
                } else if(ofType(name, ANIMALS)) {
                    tags.add(ANIMAL);
                } else if(domesticated(gob, name, tags)) {
                    tags.add(ANIMAL);
                    tags.add(DOMESTIC);
                } else if(DBG && !UNKNOWN.contains(name)) {
                    UNKNOWN.add(name);
                    gob.glob.sess.ui.message(name, GameUI.MsgType.ERROR);
                    System.out.println(name);
                }
                if(name.contains("/bat")) {
                    if(equipory == null || !equipory.has("/batcape")) {
                        tags.add(AGGRESSIVE);
                    }
                }
            } else if(name.startsWith("gfx/terobjs/arch/") && name.endsWith("gate")) {
                tags.add(GATE);
            } else if(name.endsWith("/dframe")) {
                tags.add(CONTAINER);
                tags.add(PROGRESSING);
                boolean empty = ols.isEmpty();
                boolean done = !empty && ols.stream().noneMatch(GobTag::isDrying);
                if(empty) { tags.add(EMPTY); }
                if(done) { tags.add(READY); }
            } else if(name.endsWith("/ttub")) {
                tags.add(CONTAINER);
                tags.add(PROGRESSING);
                //sdt bits: 0 - water, 1 - tannin, 2 - hide, 3 - leather
                boolean empty = sdt < 4; //has no hide nor leather
                boolean done = sdt >= 8; //has leather
                if(empty) { tags.add(EMPTY); }
                if(done) { tags.add(READY); }
            } else if(name.endsWith("/gems/gemstone")) {
                tags.add(GEM);
            } else if(name.endsWith("/wheelbarrow") || name.endsWith("/plow")) {
                tags.add(PUSHED);
            }
            if(ofType(name, VEHICLES)) {
                tags.add(VEHICLE);
            }
            if(name.equals("gfx/terobjs/items/arrow")) {
                tags.add(ARROW);
            }
            
            if(anyOf(tags, HERB, CRITTER, GEM, ARROW)) {
                tags.add(PICKUP);
            }
            
            if(anyOf(tags, DOMESTIC, HERB, TREE, BUSH)) {
                tags.add(MENU);
            }
    
            ContainerInfo.get(name).ifPresent(container -> {
                tags.add(CONTAINER);
                if(container.isFull(sdt)) {
                    tags.add(FULL);
                } else if(container.isEmpty(sdt)) {
                    tags.add(EMPTY);
                }
            });
    
            Drawable d = gob.drawable;
            if(d != null) {
                if(d.hasPose("/knock")) {
                    tags.add(KO);
                }
                if(d.hasPose("/dead") || d.hasPose("/waterdead")) {
                    tags.add(DEAD);
                }
            }
        }
    
        return tags;
    }
    
    private static boolean isDrying(String ol) {
        return ol.endsWith("-blood") || ol.endsWith("-windweed") || ol.endsWith("-fishraw");
    }
    
    public static boolean ofType(String name, String[] patterns) {
        for (String pattern : patterns) {
            if(name.contains(pattern)) { return true; }
        }
        return false;
    }
    
    private static boolean domesticated(Gob gob, String name, Set<GobTag> tags) {
        if(name.contains("/cattle/")) {
            tags.add(CATTLE);
            //TODO: add distinction between cow and bull
            if(name.endsWith("/calf")) {
                tags.add(CALF);
            }
            return true;
        } else if(name.contains("/goat/")) {
            tags.add(GOAT);
            if(name.endsWith("/billy")) {
                tags.add(BILLY);
            } else if(name.endsWith("/nanny")) {
                tags.add(NANNY);
            } else if(name.endsWith("/kid")) {
                tags.add(KID);
            }
            return true;
        } else if(name.contains("/horse/")) {
            tags.add(HORSE);
            if(name.endsWith("/foal")) {
                tags.add(FOAL);
            } else if(name.endsWith("/mare")) {
                tags.add(MARE);
            } else if(name.endsWith("/stallion")) {
                tags.add(STALLION);
            }
            return true;
        } else if(name.contains("/pig/")) {
            tags.add(PIG);
            if(name.endsWith("/hog")) {
                tags.add(HOG);
            } else if(name.endsWith("/piglet")) {
                tags.add(PIGLET);
            } else if(name.endsWith("/sow")) {
                tags.add(SOW);
            }
            return true;
        } else if(name.contains("/sheep/")) {
            tags.add(SHEEP);
            //TODO: add distinction between ewe and ram
            if(name.endsWith("/lamb")) {
                tags.add(LAMB);
            }
            return true;
        }
        return false;
    }
    
    private static boolean anyOf(Set<GobTag> target, GobTag... tags) {
        for (GobTag tag : tags) {
            if(target.contains(tag)) {return true;}
        }
        return false;
    }
}
