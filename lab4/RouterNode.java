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

	private boolean poision = true;

	// first hop, where the router should go first
	private int[] route = new int[RouterSimulator.NUM_NODES];

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
			this.route[i] = (costs[i] == RouterSimulator.INFINITY) ? -1 : i; 
			this.neighbors[i] = !(costs[i] == RouterSimulator.INFINITY);
		}
		this.broadcast();
	}

	private void broadcast() {
		for (int i = 0; i < RouterSimulator.NUM_NODES; i++) {
			if (i != this.myID && this.neighbors[i]) {
			
				int[] arr = new int[RouterSimulator.NUM_NODES];
				System.arraycopy(this.table[myID], 0, arr, 0, RouterSimulator.NUM_NODES);

				if (this.poision) {
					for (int j = 0; j < RouterSimulator.NUM_NODES; j++) {						
						if (this.route[j] == i && i != j) {
							System.out.println("poision");

							arr[j] = RouterSimulator.INFINITY;
						}
					}
				}
				System.out.println("from: " + myID + ", to: " + i + ", arr: " + Arrays.toString(arr));
				this.sendUpdate(new RouterPacket(myID, i, arr));
			}
		}
	}

	//--------------------------------------------------
	public void recvUpdate(RouterPacket pkt) {
		if (Arrays.equals(this.table[pkt.sourceid], pkt.mincost)) return;
		System.arraycopy(pkt.mincost, 0, this.table[pkt.sourceid], 0, RouterSimulator.NUM_NODES);
		
		boolean update = false;
		for (int i = 0; i < RouterSimulator.NUM_NODES; i++) {
			if (i == this.myID || this.route[i] == -1) continue;
			
			int newcost = this.table[this.route[i]][i] + this.table[this.myID][this.route[i]];
			if (this.table[this.myID][i] != newcost) {
				this.table[this.myID][i] = newcost;
				update = true;
			}
			
			// if current route is more expensive then origin cost, reset!
			if (this.table[this.myID][i] > this.costs[i]) {
				this.table[this.myID][i] = this.costs[i];
				this.route[i] = i;
				update = true;
			}

			// Bellman-Ford
			for (int j = 0; j < RouterSimulator.NUM_NODES; j++) {
				if (j == this.myID) continue;
	
				// cost: (myID -> i) + (i -> j)
				int cost = this.table[this.myID][i] + this.table[i][j];
				
				// check if route through i to j is cheaper then direct route to j
				if (this.table[this.myID][j] > cost) {
					this.table[this.myID][j] = cost;
					
					// update where the router should go first
					this.route[j] = this.route[i];
					update = true;
				}
			}
		}

		if (update) {
			this.broadcast();
		}
		System.out.println(this.myID + ": " + Arrays.toString(this.table[this.myID]) + " => " + Arrays.deepToString(this.table));
		System.out.println(this.myID + ": " + Arrays.toString(this.route));
	}

	//--------------------------------------------------
	private void sendUpdate(RouterPacket pkt) {
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
	public void updateLinkCost(int dest, int newcost) {
		this.costs[dest] = newcost;
		
		// check if dest -> dest, not routing through anything else
		if (this.route[dest] == dest) {
			this.table[this.myID][dest] = newcost;
		} else if (this.table[this.myID][dest] > this.costs[dest]) {
			// if current route is more expensive then origin cost, reset!
			this.table[this.myID][dest] = this.costs[dest];
			this.route[dest] = dest;
		}

		// Bellman-Ford - neighbours
		for (int j = 0; j < RouterSimulator.NUM_NODES; j++) {
			if (j == this.myID) continue;			
			int cost = this.table[this.myID][dest] + this.table[dest][j];
			if (this.table[this.myID][j] > cost) {
				this.table[this.myID][j] = cost;
				this.route[j] = this.route[dest];
			}
		}

		this.broadcast();
		System.out.println("*" + this.myID + ": " + Arrays.toString(this.table[this.myID]) + " => " + Arrays.deepToString(this.table));
		System.out.println(this.myID + ": " + Arrays.toString(this.route));
	}
}