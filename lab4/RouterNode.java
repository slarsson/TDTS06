import javax.swing.*;        

public class RouterNode {
  private int myID;
  private GuiTextArea myGUI;
  private RouterSimulator sim;
  private int[] costs = new int[RouterSimulator.NUM_NODES];
  private int[][] table = new int[RouterSimulator.NUM_NODES][RouterSimulator.NUM_NODES];
 

  //--------------------------------------------------
  public RouterNode(int ID, RouterSimulator sim, int[] costs) {
    myID = ID;
    this.sim = sim;
    myGUI =new GuiTextArea("  Output window for Router #"+ ID + "  ");

    System.arraycopy(costs, 0, this.costs, 0, RouterSimulator.NUM_NODES);

    for(int i = 0; i < RouterSimulator.NUM_NODES; i++) {
      if(i == myID){
         this.table[myID] = costs;
         //this.table[i][myID] = costs[i];
      } else {
        for(int j = 0; j < RouterSimulator.NUM_NODES; j++) {
          this.table[i][j] = RouterSimulator.INFINITY;
          //this.table[j][i] = RouterSimulator.INFINITY;
        }
      }

      
    }
  }

  //--------------------------------------------------
  public void recvUpdate(RouterPacket pkt) {
    System.out.println(pkt.sourceid);
    if (pkt.destid != this.myID) {
      System.out.println("NEJ");
    }
    
    //this.costs[pkt.destid] = 


  }
  

  //--------------------------------------------------
  private void sendUpdate(RouterPacket pkt) {
    sim.toLayer2(pkt);

  }
  

  //--------------------------------------------------
  public void printDistanceTable() {
	  myGUI.println("Current table for " + myID + "  at time " + sim.getClocktime());
    myGUI.println("\n Distancetable: \n");
    String temp = F.format("dst ", 10) + " |";
    String line = F.format("----------", 10);
    for(int i = 0; i < RouterSimulator.NUM_NODES; i++) {
      temp += F.format(i, 10);
      line += F.format("----------", 10);
    }
    myGUI.println(temp);
    myGUI.println(line);
    for(int i = 0; i < RouterSimulator.NUM_NODES; i++) {
      if(this.myID != i && this.costs[i] != RouterSimulator.INFINITY) {
        temp = F.format("nbr " + String.valueOf(i) + " |", 10);
        
        for(int j = 0; j < RouterSimulator.NUM_NODES; j++) {
          temp += F.format(table[i][j], 10);
        }
        myGUI.println(temp);
      }
    }

    myGUI.println("\n");
    myGUI.println("Our distance vector and rotes: \n");
    String temp2 = F.format("dst ", 10) + " |";
    line = F.format("----------", 10);
    
    temp = F.format("cost ", 10) + " |";
    temp2 = F.format("dst ", 10) + " |";
    for(int i = 0; i < RouterSimulator.NUM_NODES; i++) {
      temp += F.format(this.table[this.myID][i], 10);
      temp2 += F.format(i, 10);
    }
    myGUI.println(temp);
    myGUI.println(temp2);  
  

    myGUI.println("\n\n");
    
  }

  //--------------------------------------------------
  public void updateLinkCost(int dest, int newcost) {
    System.out.println(newcost);
    this.costs[dest] = newcost;
    this.table[myID][dest] = newcost;
    this.table[dest][myID] = newcost;
  }

}
