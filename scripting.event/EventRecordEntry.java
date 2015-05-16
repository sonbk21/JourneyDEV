/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package scripting.event;

/**
 * JourneyMS
 * 
 */
public class EventRecordEntry {
    private final String partynames;
    private final int time;
    
    public EventRecordEntry(String partynames, int time) {
        this.partynames = partynames;
        this.time = time;
    }
    
    public String getNames() {
        return partynames;
    }
    
    public int getTime() {
        return time;
    }
}
