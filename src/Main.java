import java.util.*;
import java.io.*;

public class Main {
	public static void main(String args[]) {
		try {
			File input = new File(args[0]);
			Scanner sc = new Scanner(input);
			int numTasks = sc.nextInt();
			int resourceTypes = sc.nextInt();
			//(See README) can have max of 5 resource types
			if (resourceTypes > 5) {
				System.err.println("Error: more than 5 resource types");
				sc.close();
				return;
			}
			//array of resource types-each element 1-5 contains the number of resources left for each type
			int[] resources = new int[6];
			for (int i = 0; i < resourceTypes; i++) {
				//resources[0] is not used for simplicity-resource[1] corresponds to resource 1
				resources[i + 1] = sc.nextInt();
			}
			
			//ArrayList of tasks-each task holds instructions for what to do and many variables (see task.java)
			ArrayList<Task> tasks = new ArrayList<Task>();
			// offsetting list by 1 so that task 1 is stored in tasks.get(1)
			tasks.add(new Task());
			for (int i = 0; i < numTasks; i++) {
				tasks.add(new Task());
			}
			
			while (sc.hasNext()) {
				//add instructions to correct tasks
				String instruction = sc.next();
				tasks.get(sc.nextInt()).add(new Instruction(instruction, sc.nextInt(), sc.nextInt()));
			}
			sc.close();

			// OPTIMISTIC MANAGER
			boolean deadlocked;
			boolean done = false;
			int cycle = 0;
			
			while (!done) {
				done = true;
				deadlocked = true;
				
				//t is an ArrayList used to rearrange the tasks in FIFO order
				ArrayList<Task> t = new ArrayList<Task>();
				t.add(new Task());
				
				//checks for deadlock-if any task is READY or COMPUTING, it is NOT deadlocked
				for (int i = 1; i < tasks.size(); i ++) {
					if (tasks.get(i).getStatus() == Status.READY || tasks.get(i).getStatus() == Status.COMPUTING) {
						deadlocked = false;
						break;
					}
				}
				
				if (deadlocked) {
					while (deadlocked) {
						for (int i = 1; i < tasks.size(); i ++) { //finds first blocked task and aborts it
							if (tasks.get(i).getStatus() == Status.BLOCKED) {
								tasks.get(i).abort(resources);
								done = false;
								break;
							}
						}
						for (int i = 1; i < tasks.size(); i ++) { //after aborting, checks if still deadlocked
							if (tasks.get(i).getStatus() == Status.BLOCKED) {
								if (tasks.get(i).canRequest(resources)) {
									deadlocked = false;
									tasks.get(i).setStatus(Status.READY);
								}
							}
						}
						if (!deadlocked) { //if no longer deadlocked, sets previously blocked tasks to READY
							for (int i = 1; i < tasks.size(); i ++) {
								if (tasks.get(i).getStatus() == Status.BLOCKED) {
									tasks.get(i).setStatus(Status.READY);
								}
							}
						}
					}
				} else { //if NOT deadlocked
					//released is an array corresponding to resources released during the cycle,
					//since resources released during the cycle cannot be used until the next cycle
					int[] released = new int[6];
					for (int i = 0; i < 6; i ++) {
						released[i] = 0;
					}
					
					//copy tasks over to t for sorting
					for (int i = 1; i < tasks.size(); i ++) {
						t.add(tasks.get(i));
					}
					
					boolean sorted = false;
					while (!sorted) { //bubble sorts t by BlockedTime
						sorted = true;
						for (int i = 1; i < t.size() - 1; i ++) {
							if (t.get(i).getBlockedTime() < t.get(i+1).getBlockedTime()) {
								Task temp = t.get(i);
								t.set(i, t.get(i+1));
								t.set(i + 1, temp);
								sorted = false;
							}
						}
					}
					
					for (int i = 1; i < t.size(); i ++) { //go through sorted tasks list instructions one task at a time
						if (t.get(i).getStatus() == Status.ABORTED || t.get(i).getStatus() == Status.TERMINATED) {
							//do nothing
						} else {
							
							// each "if" statement here corresponds to each instruction and handles the appropriately
							
							if (t.get(i).peek().getInstruction().equals("initiate")) {
								t.get(i).initiate(t.get(i).peek().getValue1(), t.get(i).peek().getValue2(), resources, false);
								t.get(i).remove();
								
							} else if (t.get(i).peek().getInstruction().equals("request")) {
								t.get(i).request(t.get(i).peek().getValue1(), t.get(i).peek().getValue2(), true, resources);
								if (t.get(i).getStatus() != Status.BLOCKED) { //BLOCKED after a request means it did not go through
									//if it is NOT BLOCKED, do this, otherwise keep the request instruction on top
									resources[t.get(i).peek().getValue1()] -= t.get(i).peek().getValue2();
									t.get(i).remove();
								}
								
							} else if (t.get(i).peek().getInstruction().equals("release")) {
								t.get(i).release(t.get(i).peek().getValue1(), t.get(i).peek().getValue2(), released);
								t.get(i).remove();
								
							} else if (t.get(i).peek().getInstruction().equals("compute")) {
								t.get(i).setStatus(Status.COMPUTING);
								t.get(i).setComputeTime(t.get(i).getComputeTime() + 1);
								if (t.get(i).getComputeTime() == t.get(i).peek().getValue1()) {
									//if its done computing
									t.get(i).remove();
									t.get(i).setComputeTime(0);
									t.get(i).setStatus(Status.READY);
								}
								
							} else if (t.get(i).peek().getInstruction().equals("terminate")) {
								t.get(i).terminate();
								t.get(i).setTotalTime(cycle);
								t.get(i).remove();
							}
							
							if (t.get(i).getStatus() == Status.READY || t.get(i).getStatus() == Status.BLOCKED
									|| t.get(i).getStatus() == Status.COMPUTING) { //if any task is READY, BLOCKED, or COMPUTING, its not done
								done = false;
							}

							if (t.get(i).getStatus() == Status.BLOCKED) { //keeps track of BLOCKED tasks
								t.get(i).incWaitingTime();
							}
						}
					}
					//adds resources released during the cycle at the end of the cycle
					for (int i = 1; i < 6; i ++) {
						resources[i] += released[i];
					}
					cycle++;
				}
			}
			int total, waiting;
			int totalTotal = 0;
			int totalWaiting = 0;
			System.out.println("FIFO:");
			for (int i = 1; i < tasks.size(); i ++) {
				if (tasks.get(i).getStatus() == Status.ABORTED) {
					System.out.println("Task " + i + ":\tABORTED");
				} else {
					total = tasks.get(i).getTotalTime();
					totalTotal += total;
					waiting = tasks.get(i).getWaitingTime();
					totalWaiting += waiting;
					System.out.println("Task " + i + ":\t" + total + "\t" + waiting + "\t" + (100 * waiting / total) + "%");
				}
			}
			System.out.println("Total:\t" + totalTotal + "\t" + totalWaiting + "\t" + (100 * totalWaiting / totalTotal) + "%");

		} catch (FileNotFoundException e) {
			System.err.println("File not found");
		}

		
		
			System.out.println("");
			
			
			//Banker's Algorithm
			
			try {
				File input = new File(args[0]);
				Scanner sc = new Scanner(input);
				int numTasks = sc.nextInt();
				int resourceTypes = sc.nextInt();
				if (resourceTypes > 5) {
					System.err.println("Error: more than 5 resource types");
				}
				int[] resources = new int[6];
				int[] newResources = new int[6];
				for (int i = 0; i < resourceTypes; i++) {
					resources[i + 1] = sc.nextInt();
					newResources[i + 1] = resources[i + 1];
				}
				
				ArrayList<Task> tasks = new ArrayList<Task>();
				tasks.add(new Task());
				for (int i = 0; i < numTasks; i++) {
					tasks.add(new Task());
				}
				while (sc.hasNext()) {
					String instruction = sc.next();
					tasks.get(sc.nextInt()).add(new Instruction(instruction, sc.nextInt(), sc.nextInt()));
				}
				sc.close();
			
			boolean done = false;
			int cycle = 0;
			
			/*
			 * DIFFERENCES BETWEEN BANKERS AND OPTIMISTIC
			 * 
			 * 1) The only times a task can be aborted are if it asks for more resources than its claim
			 * or its claim exceeds the initial number of available resources
			 * 2) Slightly different "request" methods (see Task.java)
			 * 
			 */
			
			while (!done) {
				done = true;
				
				ArrayList<Task> t = new ArrayList<Task>();
				t.add(new Task());
				
				
				int[] released = new int[6];
				for (int i = 0; i < 6; i ++) {
					released[i] = 0;
				}
				
				for (int i = 1; i < tasks.size(); i ++) {
					t.add(tasks.get(i));
				}
				
				boolean sorted = false;
				while (!sorted) {
					sorted = true;
					for (int i = 1; i < t.size() - 1; i ++) {
						if (t.get(i).getBlockedTime() < t.get(i+1).getBlockedTime()) {
							Task temp = t.get(i);
							t.set(i, t.get(i+1));
							t.set(i + 1, temp);
							sorted = false;
						}
					}
				}
				
				for (int i = 1; i < t.size(); i ++) {
					if (t.get(i).getStatus() == Status.ABORTED || t.get(i).getStatus() == Status.TERMINATED) {
						//do nothing
					} else {
						
						
						if (t.get(i).peek().getInstruction().equals("initiate")) {
							boolean canFulfill = t.get(i).initiate(t.get(i).peek().getValue1(), t.get(i).peek().getValue2(), resources, true);
							if (!canFulfill) { // See difference 1
								System.out.println("Task " + i + " aborted: not enough initial resources to fulfil claim");
							}
							t.get(i).remove();
							
							
						} else if (t.get(i).peek().getInstruction().equals("request")) {
							t.get(i).request(t.get(i).peek().getValue1(), t.get(i).peek().getValue2(), false, resources);
							if (t.get(i).getStatus() == Status.READY) {
								resources[t.get(i).peek().getValue1()] -= t.get(i).peek().getValue2();
								t.get(i).remove();
							} else if (t.get(i).getStatus() == Status.ABORTED) { //See difference 1
								System.out.println("Task " + (tasks.indexOf(t.get(i))) + "'s request exceeds its claim: aborted");
							}
							
							
						} else if (t.get(i).peek().getInstruction().equals("release")) {
							t.get(i).release(t.get(i).peek().getValue1(), t.get(i).peek().getValue2(), released);
							t.get(i).remove();
							
							
						} else if (t.get(i).peek().getInstruction().equals("compute")) {
							t.get(i).setStatus(Status.COMPUTING);
							t.get(i).setComputeTime(t.get(i).getComputeTime() + 1);
							if (t.get(i).getComputeTime() == t.get(i).peek().getValue1()) {
								t.get(i).remove();
								t.get(i).setComputeTime(0);
								t.get(i).setStatus(Status.READY);
							}
							
							
						} else if (t.get(i).peek().getInstruction().equals("terminate")) {
							t.get(i).terminate();
							t.get(i).setTotalTime(cycle);
							t.get(i).remove();
						}

						if (t.get(i).getStatus() == Status.READY || t.get(i).getStatus() == Status.BLOCKED
								|| t.get(i).getStatus() == Status.COMPUTING) {
							done = false;
						}

						if (t.get(i).getStatus() == Status.BLOCKED) {
							t.get(i).incWaitingTime();
						}
					}
				}
				for (int i = 1; i < 6; i ++) {
					resources[i] += released[i];
				}
				cycle++;
			}
			int total, waiting;
			int totalTotal = 0;
			int totalWaiting = 0;
			System.out.println("BANKER'S:");
			for (int i = 1; i < tasks.size(); i ++) {
				if (tasks.get(i).getStatus() == Status.ABORTED) {
					System.out.println("Task " + i + ":\tABORTED");
				} else {
					total = tasks.get(i).getTotalTime();
					totalTotal += total;
					waiting = tasks.get(i).getWaitingTime();
					totalWaiting += waiting;
					System.out.println("Task " + i + ":\t" + total + "\t" + waiting + "\t" + (100 * waiting / total) + "%");
				}
			}
			System.out.println("Total:\t" + totalTotal + "\t" + totalWaiting + "\t" + (100 * totalWaiting / totalTotal) + "%");

		} catch (FileNotFoundException e) {
			System.err.println("File not found");
		}

	}
}
