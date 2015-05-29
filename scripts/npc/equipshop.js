/* 
 * Universal Equip Shop
 * JourneyMS 
 * Equip Shop - Not Done yet
 */

var status, job, sel0, sel1;
var categories = [[["1H Swords", "2H Swords", "1H Axes", "2H Axes", "1H Blunt Weapons", "2H Blunt Weapons", "Spears", "Polearms"], ["Overalls", "Tops", "Bottoms", "Shields"], ["Helmets"], ["Boots"]],
                    [["Nextjobwep.."]], [[]], [[]], 
                    [["Guns", "Knuckles"], ["Overalls"], ["Caps"], ["Shoes"]]];
var prices = [[250000], [120000, 60000, 60000, 120000, 60000, 60000], [40000], [40000]];
var equips = [/*Warrior*/[/*Weapons*/[[], [], [], [], [], [], [], []],
                /*Armor*/[/*Male*/[], [], [],
                    /*Female*/ [1051010, 1051015, 1051079], [1041084, 1041088, 1041093, 1041098, 1041120, 1041124], [1061083, 1061087, 1061092, 1061097, 1061119, 1061123]],
                /*Helmets*/[[]],
                /*Shoes*/[[1072040, 1072127, 1072135, 1072149, 1072155, 1072211, 1072198, 1072221]]], [[[]]], [[[]]], [[[]]], 
            /*Pirates*/[/*Weapons*/[/*Guns*/[1492004, 1492006, 1492007, 1492008, 1492009, 1492010, 1492011, 1492012]]]];

function start() {
    status = -1;
    job = Math.floor(cm.getJobId()/100);
    if (job == 0) {
        cm.sendOk("I'm sorry but I don't have any equips for beginners.");
        cm.dispose();
    } else {
        job -= 1;
        action(1, 0, 0);
    }
}

function action(mode, type, selection) {
    if (mode == 1) {
        status ++;
        if (status == 0)
            cm.sendSimple("What kind of equips are you interested in?\r\n#b#L0#Weapons#l\r\n#L1#Armor#l\r\n#L2#Headgear#l\r\n#L3#Shoes#l#k");
        else if (status == 1) {
            sel0 = selection;
            var cattext = "For "+jobName(job)+" I have the following available: \r\n#b";
            if (categories[job][sel0].length > 1) {
                for (var i = 0; i < categories[job][sel0].length; i++) {
                    cattext += "#L"+i+"#"+categories[job][sel0][i]+"#l\r\n";
                }
            } else {
                sel1 = 0;
                cattext = listItems(job, sel0, sel1);
                status ++;
            }
            cm.sendSimple(cattext);
        } else if (status == 2) {
            sel1 = selection;
            cattext = listItems(job, sel0, sel1);
            cm.sendSimple(cattext);
        } else if (status == 3)  {
            var item = equips[job][sel0][sel1][selection];
            var price;
            if (prices[sel0].length > 1)
                price = prices[sel0][sel1]*(selection+1);
            else
                price = prices[sel0][0]*(selection+1);
            if (cm.getMeso() >= price && cm.canHold(item)) {
                cm.gainMeso(-price);
                cm.gainItem(item, 1, true);
                cm.sendOk("Enjoy your new equip!");
            } else {
                cm.sendOk("You do not have enough Mesos or your inventory is full.");
            }
            cm.dispose();
        }
    } else {
        cm.openScript("admin");
    }
}

function jobName(ord) {
    switch (ord) {
        case 0: return "Warriors";
        case 4: return "Pirates";
    }
}

function listItems(ord, cat, type) {
    var ret = "Which item do you need?\r\n\r\n";
    if (cm.getPlayer().getGender() == 1) {
        if (ord == 0 && cat == 1) //Distinguish between male/female versions
            sel1 += 3;
        type = sel1;
    }
    var items = equips[ord][cat][type];
    var price;
    for (var i = 0; i < items.length; i++) {
        if (prices[cat].length > 1)
            price = prices[cat][type]*(i+1);
        else
            price = prices[cat][0]*(i+1);
        ret += "#L"+i+"##i"+items[i]+"# #b #z"+items[i]+"# #k("+price+" mesos)#l\r\n";
    }
    return ret;
}