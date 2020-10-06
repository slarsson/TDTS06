import javax.swing.*;        

import java.util.Arrays;

public class RouterNode {
	private int myID;
	private GuiTextArea myGUI;
	private RouterSimulator sim;


	// cost of link, myID -> x
	private int[] costs = new int[RouterSimulator.NUM_NODES];
	
	// is neighbor to current router
	private boolean[] neighbors = new boolean[RouterSimulator.NUM_NODES];
	
	// the forwarding table
	private int[][] table = new int[RouterSimulator.NUM_NODES][RouterSimulator.NUM_NODES];

	// first fkn hop
	private int[] firstHop = new int[RouterSimulator.NUM_NODES];

	private boolean poisoned = true;



	//--------------------------------------------------
	public RouterNode(int ID, RouterSimulator sim, int[] costs) {
		this.myID = ID;
		this.sim = sim;
		this.myGUI = new GuiTextArea("  Output window for Router #"+ ID + "  ");

		System.arraycopy(costs, 0, this.costs, 0, RouterSimulator.NUM_NODES);

		for (int i = 0; i < RouterSimulator.NUM_NODES; i++) {
			if (i == myID) {
				System.arraycopy(costs, 0, this.table[i], 0, RouterSimulator.NUM_NODES);
			} else {
				for (int j = 0; j < RouterSimulator.NUM_NODES; j++) {
					this.table[i][j] = RouterSimulator.INFINITY;
				}
			}

			this.firstHop[i] = (costs[i] == RouterSimulator.INFINITY) ? RouterSimulator.INFINITY : i; 
			this.neighbors[i] = !(costs[i] == RouterSimulator.INFINITY);
		}

		// System.out.println(this.myID);
		// System.out.println(Arrays.toString(this.neighbors));
		
		System.out.println("HOP:" + Arrays.toString(this.firstHop));

		this.broadcast();
	}

	private void broadcast() {
		for (int i = 0; i < RouterSimulator.NUM_NODES; i++) {
			if (i != this.myID && this.neighbors[i]) {
				System.out.println("from: " + myID + ", to: " + i + ", arr: " + Arrays.toString(this.table[myID]));
				this.sendUpdate(new RouterPacket(myID, i, this.table[myID]));
			}
		}
	}

	//--------------------------------------------------
	public void recvUpdate(RouterPacket pkt) {
		// nothing have changed ..
		if (Arrays.equals(this.table[pkt.sourceid], pkt.mincost)) {
			return;
		}
		
		// copy the incoming packet to 'table'
		System.arraycopy(pkt.mincost, 0, this.table[pkt.sourceid], 0, RouterSimulator.NUM_NODES);

		// check if 'table' has changed
		if (this.bellman()) {
			this.broadcast();
		}
	}

	//--------------------------------------------------
	private void sendUpdate(RouterPacket pkt) {
		if (this.poisoned) {
			for (int i = 0; i < RouterSimulator.NUM_NODES; i++) {
				if (this.firstHop[i] == pkt.destid && i != pkt.destid) {
					pkt.mincost[i] = RouterSimulator.INFINITY;
				}
			}
		}
		
		sim.toLayer2(pkt);
	}

	//--------------------------------------------------
	public void printDistanceTable() {
		myGUI.println("Current table for " + myID + "  at time " + sim.getClocktime());
		myGUI.println("\n Distancetable: \n");

		String line = "-----------------------------------------------------------------";
		String dst = F.format("dst ", 10) + " |";
		String temp;

		for(int i = 0; i < RouterSimulator.NUM_NODES; i++) {
			dst += F.format(i, 10);
		}

		myGUI.println(dst);
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

		temp = F.format("cost ", 9) + " |";
		String route = F.format("route ", 8) + " |";
		for(int i = 0; i < RouterSimulator.NUM_NODES; i++) {
			temp += F.format(this.table[this.myID][i], 10);
			if (this.table[this.myID][i] == RouterSimulator.INFINITY) {
				route += F.format("-", 10);
			} else {
				route += F.format(i, 10);
			}
		}

		myGUI.println(dst);
		myGUI.println(line);
		myGUI.println(temp);
		myGUI.println(route);  
		myGUI.println("\n\n\n");
	}

	//--------------------------------------------------

	private boolean bellman() {
		boolean state = false;

		for (int i = 0; i < RouterSimulator.NUM_NODES; i++) {
			if (i == this.myID) continue;
			for (int j = 0; j < RouterSimulator.NUM_NODES; j++) {
				if (j == this.myID) continue;
				
				// myID -> j -> i
				int newcost = this.costs[j] + this.table[j][i];
				
				// myID -> i
				int currentcost = this.costs[i];
				
				if (newcost < currentcost) {
					this.table[myID][i] = newcost;
					this.firstHop[i] = j;
					state = true;
				}
			}
		}

		return state;
	}

	public void updateLinkCost(int dest, int newcost) {
		System.out.println("update: " + myID + " -> " + dest + " : " + this.costs[dest] + " -> " + newcost);
		
		// set the new cost
		this.costs[dest] = newcost;
		
		// check if cost have changed 'table'
		if (this.bellman()) {
			this.broadcast();
		}
	}
}

// vad händer ens här!?!??!!?
//https://github.com/Awolize/TDTS04-04/blob/master/RouterNode.java
//https://github.com/jmlasnier/tdts04-labs/blob/master/Lab4/src/lab4/RouterNode.java
//https://github.com/diblaze/TDTS04/blob/master/Lab%204/RouterNode.java