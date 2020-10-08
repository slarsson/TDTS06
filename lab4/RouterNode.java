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

	// ? -> i
	private int[] prevVertex = new int[RouterSimulator.NUM_NODES];



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

			this.prevVertex[i] = (costs[i] == RouterSimulator.INFINITY) ? -1 : i; 
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
						if (this.prevVertex[j] == i) {
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
		if (Arrays.equals(this.table[pkt.sourceid], pkt.mincost)) {
			return;
		}
		System.arraycopy(pkt.mincost, 0, this.table[pkt.sourceid], 0, RouterSimulator.NUM_NODES);
		this.bellman();
		System.out.println(this.myID + ": " + Arrays.toString(this.table[this.myID]) + " => " + Arrays.deepToString(this.table));
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
	private void bellman() {
		boolean update = false;
		for (int i = 0; i < RouterSimulator.NUM_NODES; i++) {
			if (i == this.myID) continue;
			
			// (innan i -> i) + (myid -> innan i)
			// total kostnad: myID -> i
			int newcost = this.table[this.prevVertex[i]][i] + this.table[this.myID][this.prevVertex[i]];

			// kostnaden har ändrats
			if (this.table[this.myID][i] != newcost) {
				this.table[this.myID][i] = newcost;
				update = true;
			}
			
			// om (myID -> ? -> i) dyrare än (myID -> i), fel väg.
			if (this.table[this.myID][i] > this.costs[i]) {
				this.table[this.myID][i] = this.costs[i];
				this.prevVertex[i] = i;
				update = true;
			}

			// cost = myID -> i + (i -> j)
			for (int j = 0; j < RouterSimulator.NUM_NODES; j++) {
				if (j == this.myID) continue;
				
				int cost = this.table[this.myID][i] + this.table[i][j];
				
				// om billigare att gå till j via i, myID -> i -> j
				if (this.table[this.myID][j] > cost) {
					this.table[this.myID][j] = cost;
					
					// before: ? -> j
					// after: innan i -> j
					this.prevVertex[j] = this.prevVertex[i];
					update = true;
				}
			}

		}

		if (update) {
			this.broadcast();
		}
	}

	public void updateLinkCost(int dest, int newcost) {
		this.costs[dest] = newcost;
		
		// om i -> i, uppdatera kostnaden
		if (this.prevVertex[dest] == dest) {
			this.table[this.myID][dest] = newcost;
		}
		
		// kolla om nya kostnaden gör det billigare
		// om (myID -> ? -> i) dyrare än (myID -> i), byt!
		if (this.table[this.myID][dest] > this.costs[dest]) {
			this.table[this.myID][dest] = this.costs[dest];
			this.prevVertex[dest] = dest;
		}

		// bellman
		// cost = myID -> i + (i -> j)
		for (int j = 0; j < RouterSimulator.NUM_NODES; j++) {
			if (j == this.myID) continue;			
			int cost = this.table[this.myID][dest] + this.table[dest][j];
			if (this.table[this.myID][j] > cost) {
				this.table[this.myID][j] = cost;
				this.prevVertex[j] = this.prevVertex[dest];
			}
		}

		this.broadcast();
		System.out.println("*" + this.myID + ": " + Arrays.toString(this.table[this.myID]) + " => " + Arrays.deepToString(this.table));
	}
}