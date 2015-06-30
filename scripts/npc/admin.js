/* 
 * Maple Administrator - Main NPC - JourneyMS
*/

var status;
var itemlist;
var sel;
var item;
var trophyshop = [[5490002, 1], [5590000, 2], [5520000, 3], [1112414, 4], [5041000, 5], [5010068, 2], [5010069, 2], [3010063, 2], [3010017, 2]];

function start() {
    status = -1;
    sel = 0;
    action(1, 0, 0);
}

function action(mode, type, selection) {
    if (mode == 1) {
        status++;
    } else {
        if ((status > 1 && status < 24) || status == 0) {
            cm.dispose();
        }
        if (status == 24)
            status = 0;
        else
            status--;
    }
    if (status == 0) {
        cm.teachSkill(1007, 3, 3, -1);
        cm.sendSimple("Hello #e#r#h0##k#n, how may I help you?\r\n#b#L0#Special Maps#l\r\n#L1#Trade Vote Points#l\r\n#L2#Maple Coin Shop#l\r\n#L5#Universal Equip Shop#l\r\n#L3#View Achievements#l\r\n#L4#Starter Guide#l");
    } else if (status == 1) {
        sel = selection;
        if (selection == 0) {
            cm.sendSimple("Where do you want to go?\r\n#b#L0#Boss Party Quest#l\r\n#L1#Monster Park#l\r\n#L2#Dimensional Mirror#l\r\n#L3#Fishing Map#l\r\n#L4#Ardentmill#l");
        } else if (selection == 1) {
            if (cm.getPlayer().getLevel() >= 20) {
                var s = sel-1;
                itemlist = "";
                if (s == 0) {
                    itemlist += "\r\n#b#L0##fItem/Special/0900.img/09000002/iconRaw/1#  2,500,000 Meso#k#l\r\n";
                    itemlist += "\r\n#b#L1##fItem/Special/MaplePoint.img/5000/iconRaw#  5000 Cash#k#l";
                    itemlist += "\r\n#b#L2##v4001129#   10x Maple Coin#k#l";
                }
                cm.sendSimple("You can trade your #eVote Points#n for these items:\r\n"+itemlist);
            } else {
                cm.sendOk("You need to be at least level 20 to redeem your vote points.");
                cm.dispose();
            }
        } else if (selection == 2) {
            cm.dispose();
            cm.openNpc(9209001);
        } else if (selection == 3) {
            var text = "Your current achievements in order of completion: \r\n\r\n#e";
            text += cm.getPlayer().getAchievements().getNpcText(false);
            cm.sendSimple(text+"\r\n#n#b#L0#Show Achievements in Progress#l\r\n#L1#Claim Trophies#l\r\n#L2#Exchange Trophies for Items#l");
        } else if (selection == 4) {
            status = 24;
            cm.sendSimple("What would you like to know about?#b\r\n#L0#Custom Features#l\r\n#L1#Potential and Upgrade System#l\r\n#L2#Available Content#l\r\n#L3#Terms of Service (Game Rules)#l")
	} else if (selection == 5) {
            cm.openScript("equipshop");
        }
    } else if (status == 2) {
        if (sel == 0) {
            if (selection == 0) {
                if (cm.getPlayer().getLevel() > 69) {
                    cm.getPlayer().saveLocation("BOSSPQ");
                    cm.warp(689010000);
                    cm.dispose();
                } else {
                    cm.sendOk("You need to be at least Level 70 to enter Boss PQ.");
                    cm.dispose();
                }
            } else if (selection == 1) {
                if (cm.getPlayer().getLevel() > 49) {
                    cm.dispose();
                    cm.getPlayer().saveLocation("MONSTER_PARK");
                    cm.warp(951000000,1);
                } else {
                    cm.sendOk("You need to be at least Level 50 to enter Monster Park");
                    cm.dispose();
                }
            } else if (selection == 2) {
                cm.dispose();
                cm.openNpc(9010022);
            } else if (selection == 3) {
                cm.getPlayer().saveLocation("SUNDAY_MARKET");
                cm.warp(970020000, 0);
                cm.dispose();
            } else if (selection == 4) {
                cm.getPlayer().saveLocation("ARDENTMILL");
                cm.warp(910001000, 0);
                cm.dispose();
            }
        } else if (sel == 1) {
            item = selection;
            cm.sendGetNumber("How many points do you want to spend?",1,1,10);
        } else if (sel == 3) {
            if (selection == 0) {
                var progress = cm.getPlayer().getAchievements().getNpcText(true);
                if (progress.equals(""))
                    cm.sendOk("You currently do not have any achievements in progress.");
                else
                    cm.sendOk("Your achievements in progress:\r\n\r\n#b"+progress);
                cm.dispose();
            } else if (selection == 1) {
                var trophies = cm.getPlayer().getAchievements().getTrophies();
                if (trophies > 0) {
                    if (cm.canHold(3995001)) {
                        cm.gainItem(3995001, trophies);
                        cm.getPlayer().clearTrophies();
                        cm.sendOk("Enjoy your rewards!\r\n\r\n#fUI/UIWindow.img/QuestIcon/4/0#\r\n#v3995001# #b"+trophies+" #z3995001#");
                    } else {
                        cm.sendOk("Please make some room in your inventory.");
                    }
                } else {
                    cm.sendOk("I'm afraid that there are no trophies you could claim.");
                }
                cm.dispose();
            } else if (selection == 2) {
                itemlist = "Current Trophies:#r "+cm.getPlayer().getItemQuantity(3995001, false)+"#k\r\n\r\n";
                for (var i = 0; i < trophyshop.length; i++) {
                    itemlist += "#L"+i+"##v"+trophyshop[i][0]+"#  #b#z"+trophyshop[i][0]+"# #kfor "+trophyshop[i][1]+" "+((trophyshop[i][1] > 1)?"Trophies":"Trophy")+"#l\r\n";
                }
                itemlist += "\r\n\r\n#b(Note: Be careful with spending your trophies. The amount of trophies you can obtain per account is limited.)";
                cm.sendSimple(itemlist);
            }
        }
    } else if (status == 3) {
        if (sel == 1) {
            if (cm.getVPoints() >= selection) {
                if (item < 2 || cm.canHold(4001129)) {
                    cm.getClient().setVPoints(cm.getVPoints() - selection);
                    if (item == 0) {
                        cm.gainMeso(2500000*selection);
                        cm.sendOk("Enjoy your rewards!\r\n\r\n#fUI/UIWindow.img/QuestIcon/4/0#\r\n#fItem/Special/0900.img/09000002/iconRaw/1# #b"+(2500000*selection)+" Mesos");
                    } else if (item == 1) {
                        cm.getPlayer().getCashShop().gainCash(1,selection*5000);
                        cm.sendOk("Enjoy your rewards!\r\n\r\n#fUI/UIWindow.img/QuestIcon/4/0#\r\n#fItem/Special/MaplePoint.img/"+(5000*selection)+"/iconRaw# #b"+(5000*selection)+" Cash");
                    } else {    
                        cm.gainItem(4001129, selection*10);
                        cm.sendOk("Enjoy your reward!\r\n\r\n#fUI/UIWindow.img/QuestIcon/4/0#\r\n#v4001129# #b"+(10*selection)+" #z4001129#");
                    }
                } else {
                    cm.sendOk("Please make some room in your inventory.");
                }
            } else {
                cm.sendOk("You do not have enough Vote Points for the purchase.");
            }
            cm.dispose();
        } else if (sel == 3) {
            if (cm.getPlayer().getItemQuantity(3995001, false)+5 >= trophyshop[selection][1] && cm.canHold(trophyshop[selection][0])) {
                cm.gainItem(3995001, -1, false);
                cm.gainItem(trophyshop[selection][0], 1, false);
                cm.sendOk("Enjoy your reward!\r\n\r\n#fUI/UIWindow.img/QuestIcon/4/0#\r\n#v"+trophyshop[selection][0]+"# #b1 #z"+trophyshop[selection][0]+"#");
            } else {
                cm.sendOk("You do not have enough trophies or your inventory is full.");
            }
            cm.dispose();
        }
    } else if (status == 24) {
        cm.sendSimple("What would you like to know about?#b\r\n#L0#Custom Features#l\r\n#L1#Potential and Upgrade System#l\r\n#L2#Available Content#l\r\n#L3#Terms of Service (Game Rules)#l");
    } else if (status == 25) {
            if (selection == 0) {
                status = 29;
                cm.sendNext("There is a variety of #bcustom features#k in #eJourneyMS#n. Most of them can be accessed through the ~admin command.");
            } else if (selection == 1) {
                status = 39;
                cm.sendNext("The #bpotential system#k in #eJourneyMS#n is quite different from the official servers.");
            } else if (selection == 2) {
                status = 23;
                cm.sendNext("Available Content (Latest releases in #bblue#k)#e\r\nBoss PQ#l\r\nMonster Park#l#b\r\nNeo Tokyo#l\r\nUlu City#l\r\nGold Beach#l\r\nRiena Strait#l\r\nLionheart Castle#l#n#k");
            }  else if (selection == 3) {
                status = 23;
                cm.sendNext("#eTerms of Service - 19.03.2015:#n\r\nThis is only an abbreviated version of the ToS. Please read the ToS on the forums if you are unsure about one of these rules.\r\n\r\nThe following offences will lead to a ban:\r\n\r\n#e1.Hacking, Botting, Use of 3rd Party Programs#n\r\nIncludes all WZ Edits and Cheat Programs. Botting includes all attempts to play while away from keyboard.\r\n#bIP Ban#k\r\n\r\n#e2.Kill Stealing and Harassment#n\r\nIf you want to report someone for ksing or harassment, please ensure that:\r\nI. You have told them their behaviour will lead to a ban.\r\nII. KS: They have attacked in your map for at least 1 minute. Harrasment: The harassment been going on for a longer period.\r\nIII. You include an unedited SS in your report.\r\n#bAccount Ban#k\r\n\r\n#e3.Bug/Glitch Exploits#n\r\nLimited to bugs/glitches which will give a significant gameplay advanatge. Visual bugs/glitches are not considered exploitable. If you find any exploitable bugs/glitches please report them to a gm.\r\n#bIP Ban#k\r\n\r\n#e4.Account Sharing#n\r\nInludes different people playing on one account on the same computer, also includes sharing your password/id.\r\n#bAccount Ban#k\r\n\r\n#e5.Multi Clienting/Accounting: Includes having two clients open on one computer AND using two seperate computers to play on two accounts. Creating two accounts is allowed, but they may not be played at the same time.\r\n#IP Ban#k\r\n\r\n#n\r\n#e6.Innapropriate and Non-English Smegas#n\r\nRacist, homophobic or sexist slur is not to be used in smegas. Vulgarity is not considered innapropriate unless another player/group is directly addressed. English is the official language in this server, so please keep all smegas in english.\r\n#b7-Day Ban or Mute#k\r\n\r\n#e7.Spam or Advertisement#n\r\nSpam (repeating the same message again and again) is only allowed in fm. Advertising other websites or products is not allowed.\r\n\r\n#b7-Day Ban or Mute#k");
            }
    } else if (status == 29) {
        cm.sendNext("There is a variety of #bcustom features#k in #eJourneyMS#n. Most of them can be accessed through the ~admin command.");
    } else if (status == 30) {
        cm.sendNextPrev("#bLucia#k, the owner of the Maple Coin Shop, exchanges game items for #rmaple coins#k. Most of her selection is endgame oriented and will become important in the later stages of the game.");
    } else if (status == 31) {
        cm.sendNextPrev("Maple coins are obtained from hunting normal mobs. #bLucia#k will also exchange coins for #rmaple leaves#k, which are obtained from party quests and events.");
    } else if (status == 32) {
        cm.sendNextPrev("#bAchievements#k can be unlocked by completing a variety of challenges. Some of them are trivial, like defeating a major boss or finishing a party quest and others are very difficult or hard to find.");
    } else if (status == 33) {
        cm.sendNextPrev("All achievements reward a trophy and some can even get you a medal. The trophies can be exchanged for very rare items, but you should think carefully before buying anything since you can only get so many trophies.");
    } else if (status == 34) {
        cm.sendNextPrev("Besides these custom mechanics, we also have lots of content from other versions, such as the Lionheart Castle or Monster Park. And there are also custom features like our Boss PQ.");
        status = 23;
    } else if (status == 39) {
        cm.sendNext("The #bpotential system#k in #eJourneyMS#n is very different from the official servers.");
    } else if (status == 40) {
        cm.sendNextPrev("There is only one type of #rmiracle cube#k and it upgrades both the number of lines and the potential rank.");
    } else if (status == 41) {
        cm.sendNextPrev("All items start at rare rank and can go up to godly rank.\r\n The #ehighest percentage#n for any stat are 1/2 for rare, 2/4 for epic, 3/6 for unique, 4/8 for legendary and 5/10 for godly regardless of item level.");
    } else if (status == 42) {
        cm.sendNextPrev("Potentials can be transferred between items of the same type through the #epotential merge#n.\r\nTo access this feature, talk to #bMs. Bo#k who can be found in Ellinia, Orbis, Ludibirium and Leafre.");
        status = 23;
    }
}


