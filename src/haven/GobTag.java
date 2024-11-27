package haven;

import me.ender.ContainerInfo;
import me.ender.ResName;
import me.ender.gob.GobTimerData;
import me.ender.gob.KinInfo;

import java.util.*;

public enum GobTag {
    TREE, BUSH, LOG, STUMP, HERB,
    ANIMAL, AGGRESSIVE, CRITTER,
    
    MIDGES, RABBIT, SPEED,
    
    DOMESTIC, YOUNG, ADULT,
    CATTLE, COW, BULL, CALF,
    GOAT, NANNY, BILLY, KID,
    HORSE, MARE, STALLION, FOAL,
    PIG, SOW, HOG, PIGLET,
    SHEEP, EWE, RAM, LAMB,
    
    GEM, ARROW,
    VEHICLE, PUSHED, //vehicle that is pushed (wheelbarrow, plow)
    
    CONTAINER, PROGRESSING, GATE, SMELTER,
    
    HAS_WATER, DRINKING,
    
    PLAYER, ME, FRIEND, FOE, PARTY, LEADER, IN_COMBAT, COMBAT_TARGET, AGGRO_TARGET,
    KO, DEAD, EMPTY, READY, FULL, LIT, COLD,
    
    MENU, PICKUP, HIDDEN;
    
    private static final String[] AGGRO = {
	"/adder",
	"/badger",
	//"/bat", //bats are handled separately to account for wearing cape
	"/bear",
	"/boar",
	"/boreworm",
	"/caveangler",
	"/cavelouse",
	"/caverat",
	"/eagleowl",
	"/goat/wildgoat",
	"/goldeneagle",
	"/greenooze",
	"/lynx",
	"/mammoth",
	"/moose",
	"/orca",
	"/spermwhale",
	"/troll",
	"/walrus",
	"/wolf",
	"/wolverine",
    };
    
    private static final String[] BIG_PARTS = {
        "/orca/orcabeef", "/spermwhale/spermwhaleskull", "/spermwhale/spermwhalesteak", "/spermwhale/spermwhaleheart", "/spermwhale/spermwhaleskeleton"
    };
    private static final String[] ANIMALS = {
        "/fox", "/swan", "/bat", "/beaver", "/reddeer"
    };
    
    //behave like herbs - r-click and select Pick from menu
    private static final String[] LIKE_HERB = {
        "/precioussnowflake"
    };
    
    //behave like critters - can be picked up by r-clicking 
    private static final String[] LIKE_CRITTER = {
        "/terobjs/items/hoppedcow",
        "/terobjs/items/mandrakespirited",
        "/terobjs/items/grub"
    };
    
    //these can be picked up by r-clicking
    private static final String[] CRITTERS = {
	"/bayshrimp",
	"/bogturtle",
	"/brimstonebutterfly",
        "/bullfinch",
	"/cavecentipede",
	"/cavemoth",
	"/chicken",
	"/crab",
	"/dragonfly",
	"/earthworm",
	"/firefly",
	"/forestlizard",
	"/forestsnail",
	"/frog",
	"/grasshopper",
	"/hedgehog",
	"/irrbloss",
	"/items/grub",
        "/items/hoppedcow",
        "/items/mandrakespirited",
	"/jellyfish",
        "/kritter/tick/tick",
	"/ladybug",
	"/lobster",
        "/magpie",
	"/mallard",
	"/mole",
	"/monarchbutterfly",
	"/moonmoth",
        "/opiumdragon",
        "/ptarmigan",
	"/quail",
	"/rabbit",
	"/rat/rat",
	"/rockdove",
	"/sandflea",
        "/seagull",
	"/silkmoth",
	"/springbumblebee",
	"/squirrel",
	"/stagbeetle",
        "/stalagoomba",
	"/toad",
	"/waterstrider",
	"/whirlingsnowflake",
        "/woodgrouse-f",
        "/woodworm",
    };
    
    //List of animals that player can aggro
    private static final String[] CAN_AGGRO = {
        "gfx/kritter/adder/",
        "gfx/kritter/ants/", //all ant types
        "gfx/kritter/aurochs/",
        "gfx/kritter/badger/",
        "gfx/kritter/bat/", //all bat types
        "gfx/kritter/bear/",
        "gfx/kritter/beaver/", //all beaver types
        "gfx/kritter/bees/", //all bee types
        "gfx/kritter/boar/",
        "gfx/kritter/boreworm/",
        "gfx/kritter/caveangler/",
        "gfx/kritter/cavelouse/",
        "gfx/kritter/chasmconch/",
        "gfx/kritter/eagleowl/",
        "gfx/kritter/fox/",
        "gfx/kritter/goat/wildgoat",
        "gfx/kritter/goldeneagle",
        "gfx/kritter/greyseal",
        "gfx/kritter/horse/", //all horse types
        "gfx/kritter/lynx/",
        "gfx/kritter/mammoth/",
        "gfx/kritter/moose/",
        "gfx/kritter/mouflon/",
        "gfx/kritter/nidbane/",
        "gfx/kritter/ooze/",
        "gfx/kritter/orca/",
        "gfx/kritter/otter/",
        "gfx/kritter/pelican/",
        "gfx/kritter/rat/caverat",
        "gfx/kritter/reddeer/",
        "gfx/kritter/reindeer/",
        "gfx/kritter/roedeer/",
        "gfx/kritter/spermwhale/",
        "gfx/kritter/stoat/",
        "gfx/kritter/swan/",
        "gfx/kritter/troll/",
        "gfx/kritter/walrus/",
        "gfx/kritter/wolf/",
        "gfx/kritter/wolverine/",
        "gfx/kritter/woodgrouse/woodgrouse-m",
        "gfx/kritter/wildbees/beeswarm",
    };
    
    private static final String[] VEHICLES = {"/wheelbarrow", "/plow", "/cart", "/dugout", "/rowboat", "/vehicle/snekkja", "/vehicle/knarr", "/vehicle/wagon", "/vehicle/coracle", "/horse/mare", "/horse/stallion", "/vehicle/spark"};
    
    private static final boolean DBG = false;
    private static final Set<String> UNKNOWN = new HashSet<>();
    
    public static Set<GobTag> tags(Gob gob) {
        Set<GobTag> tags = new HashSet<>();
        GameUI gui = gob.context(GameUI.class);
        Glob glob = gob.context(Glob.class);
        Equipory equipory = gui != null ? gui.equipory : null;
        Fightview fight = gui != null ? gui.fv : null;
        
        String name = gob.resid();
        int sdt = gob.sdt();
        if(name != null) {
            List<String> ols = Collections.emptyList();
            synchronized (gob.ols) {
                try {
                    List<String> list = new ArrayList<>();
                    for (Gob.Overlay overlay : gob.ols) {
                        if(overlay != null && overlay.spr != null && overlay.spr.res != null) {
                            list.add(overlay.spr.res.name);
                        }
                    }
                    ols = list;
                } catch (Loading e) {
                    gob.tagsUpdated();
                }
            }
    
            if(name.startsWith("gfx/terobjs/trees")) {
                if(name.endsWith("log") || name.endsWith("oldtrunk") || name.contains("/driftwood")) {
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
                if(name.contains("/rabbit")) {
                    tags.add(RABBIT);
                }
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
                    if(equipory == null || !equipory.hasBatCape()) {
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
                boolean empty = (sdt & 0b1100) == 0; //has no hide nor leather
                boolean done = (sdt & 0b1000) != 0; //has leather
                if(empty) { tags.add(EMPTY); }
                if(done) { tags.add(READY); }
            } else if(name.equals(ResName.STACK_FURNACE)) {
                tags.add(PROGRESSING);
                tags.add(SMELTER);
                //sdt bits: 0 - lit, 1 - ore, 2 - bars, 3 - partial heat or pumping, 4 - full heat
                boolean lit = (sdt & 0b0001) != 0;
                boolean ore = (sdt & 0b0000_0010) != 0;
                boolean bars = (sdt & 0b0000_0100) != 0;
                //boolean cold = (sdt & 0b0001_1000) == 0;
                boolean hot = (sdt & 0b0001_0000) != 0;
                if(bars) {tags.add(READY);}
                if(lit) {
                    tags.add(LIT);
                    if(!hot && ore) {tags.add(COLD);}
                }
            } else if(name.equals(ResName.ORE_SMELTER)) {
                tags.add(PROGRESSING);
                tags.add(SMELTER);
                //sdt bits: 0 - open; 1 - lit; 2 - melting ore; 3,4,5 - bars; 6 - closed
                boolean lit = (sdt & 0b0010) != 0;
                boolean bars = (sdt & 0b0011_1000) != 0;
                if(bars) {tags.add(READY);}
                if(lit) {tags.add(LIT);}
            } else if(name.equals(ResName.FINERY_FORGE)) {
                tags.add(PROGRESSING);
                tags.add(SMELTER);
                //TODO: read sdt flags
                boolean lit = true;
                if(lit) {tags.add(LIT);}
            } else if(name.endsWith("/beehive")) {
                tags.add(PROGRESSING);
                //sdt bits: 0 - honey, 1 - bees?, 2 - wax
                //boolean noHoney = (sdt & 1) == 0; //has no honey
                boolean hasWax = (sdt & 4) != 0; //has wax
                if(hasWax) {tags.add(READY);}
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
            if(name.equals("gfx/terobjs/boostspeed")) {
                tags.add(SPEED);
            }
            
            if(ItemData.WATER.equalsIgnoreCase(gob.contents())) {
                tags.add(HAS_WATER);
            }
            
            if(anyOf(tags, HERB, CRITTER, GEM, ARROW)) {
                tags.add(PICKUP);
            }
            
            if(anyOf(tags, DOMESTIC, HERB, TREE, BUSH)) {
                tags.add(MENU);
            }
            
            Party.Member member = glob.party.memb.get(gob.id);
            if(member != null) {
                tags.add(PARTY);
            }
            
            Party.Member leader = glob.party.leader;
            if(leader != null && leader.gobid == gob.id) {
                tags.add(LEADER);
            }
            
            if(fight != null) {
                for (Fightview.Relation relation : fight.lsrel) {
                    if(relation.gobid == gob.id) {
                        tags.add(IN_COMBAT);
                        break;
                    }
                }
                Fightview.Relation current = fight.current;
                if(current != null && current.gobid == gob.id) {
                    tags.add(COMBAT_TARGET);
                }
            }
            
            if((anyOf(tags, PLAYER) || ofType(name, CAN_AGGRO)) && !anyOf(tags, ME, PARTY, IN_COMBAT, KO, DEAD)) {
                tags.add(AGGRO_TARGET);
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
                if(d.hasPose("drinkan")) {
                    tags.add(DRINKING);
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
