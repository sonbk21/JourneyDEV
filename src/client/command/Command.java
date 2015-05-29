/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package client.command;

import client.MapleClient;

/*
Journey MS
*/

public abstract class Command {
    
    protected MapleClient c;
    protected String[] args;
    
    public abstract void execute();
}
