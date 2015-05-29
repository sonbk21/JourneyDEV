/*
 * Boss PQ Host - Map: BPQ Lobby
 * JourneyMS
 */


var status;
var modes = [["easy", 70, ""], ["normal", 120, "EASY"], ["hard", 140, "MED"], ["hell", 170, "HARD"]];

function start() {
    status = -1;
    action(1, 0, 0);
}

function action(mode, type, selection) {
    if (mode == 1) {
        status ++;
        if (status == 0) {
            cm.sendSimple("Welcome to the Bossing Reasearch HQ! How can I help you?\r\n#L0##bChallenge Boss Rush PQ#l\r\n#L4##bView Rankings#l\r\n#L1#Item Shop#l\r\n#L2#Exchange Points for Goods#l\r\n#L3#I want to leave#l");
        } else if (status == 1) {
            if (selection == 0)
                cm.sendSimple("Please select your difficulty setting:\r\n#b#L0##i3994115##l (Lv. 70+)  #L1##i3994116##l (Lv. 120+)\r\n#L2##i3994117##l (Lv. 140+)#L3##i3994118##l (Lv. 170+)");
            else if (selection == 1) {
                cm.openNpcShop(9201060);
                cm.dispose();
            } else if (selection == 2) {
                //Todo, item exchange
            } else if (selection == 3) {
                cm.warp(cm.getPlayer().getSavedLocation("BOSSPQ"));
                cm.dispose();
            } else if (selection == 4) {
                status = 3;
                cm.sendSimple("Which rankings would you like to see?#b\r\n#L0#Easy Mode#l\r\n#L1#Normal Mode#l\r\n#L2#Hard Mode#l\r\n#L3#Hell Mode#l");
            }
        } else if (status == 2) {
            if (selection < 4) {
                var party = cm.getPlayer().getParty();
                if (party == null) {
                    cm.sendOk("Please make sure that you are in a party before entering.");
                    cm.dispose();
                    return;
                }
                
                var reqlevel = modes[selection][1];
                var reqAvm = modes[selection][2];
                var difficulty = modes[selection][0];
                
                var iter = party.getMembers().iterator();
                while (iter.hasNext()) {
                    var member = iter.next();
                    /*if (!(member.getMapId() == cm.getMapId()) || member.getLevel() < reqlevel || (selection > 0 && !member.hasAchievement(reqAvm))) {
                        cm.sendOk("One of your party members does not meet the level requirement, has not beaten the previous difficulty or is not in the same map.");
                        cm.dispose();
                        return;
                    }*/
                }
                cm.getEventManager("BossPQ"+difficulty).startInstance(party, cm.getPlayer().getMap());
                cm.dispose();
            }
        } else if (status == 4) {
            var difficulty = modes[selection][0];
            var rankings = cm.getEventManager("BossPQ"+difficulty).getNpcTextRankings();
            if (rankings.equals(""))
                cm.sendOk("It seems that noone has beaten "+difficulty+" mode yet, so there are no rankings available.");
            else
                cm.sendOk("The rankings for "+difficulty+" mode are:#b\r\n"+rankings);
            cm.dispose();
        }
    } else {
        cm.dispose();
    }
}