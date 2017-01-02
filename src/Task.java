import java.util.*;

public class Task {
	//LinkedList of all instrutions
	Queue<Instruction> instructions = new LinkedList<Instruction>();
	//see Status.java
	private Status status;
	//array containing all resource claims
	private int[] claims = new int[6];
	//array containing all resource requests
	private int[] requests = new int[6];
	//array containing all resources currently held
	private int[] holds = new int[6];
	// max of 5 resource types (covers all the inputs)
	private int waitingTime;
	private int totalTime;
	private int computingTime;
	private int blockedTime;
	
	
	public Task() {
		status = Status.READY;
		waitingTime = 0;
		totalTime = 0;
		blockedTime = 0;
		computingTime = 0;
	}
	
	// Getters and setters
	
	public void setTotalTime(int totalTime) {
		this.totalTime = totalTime;
	}
	
	public int getTotalTime() {
		return totalTime;
	}
	
	public void incWaitingTime() {
		waitingTime ++;
	}
	
	public int getWaitingTime() {
		return waitingTime;
	}
	
	public void add(Instruction i) {
		instructions.add(i);
	}
	
	public Instruction peek() {
		return instructions.element();
	}
	
	public void remove() {
		instructions.remove();
	}
	
	public void setStatus(Status s) {
		status = s;
	}
	
	public Status getStatus() {
		return status;
	}
	
	public int getBlockedTime() {
		return blockedTime;
	}
	
	/*
	 * initiate: params: resource type, intial claim, array containing available resources, boolean to tell
	 * if it is using the banker's algorithm or optimistic
	 * returns true if it can fulfill the claim with the initial number of resources(used only for banker's)
	 * returns false if it cannot
	 */
	public boolean initiate(int resourceType, int initialClaim, int[] available, boolean isBankers) {
		if (isBankers) {
			claims[resourceType] = initialClaim;
			if (claims[resourceType] > available[resourceType]) {
				abort(available);
				return false;
			}
		}
		status = Status.READY;
		return true;
	}
	
	/*
	 * request: params: resource type, number of that resource requested, true if optimistic false if banker's, array of available resources
	 * if optimistic, does the request if it can
	 * if banker's, aborts the task if it asks for more than its initial claim, otherwise checks for safety
	 * if it is deemed safe, allocates resources
	 * 
	 */
	public void request(int resourceType, int request, boolean isOptimistic, int[] available) {
		requests[resourceType] = request;
		if (isOptimistic) {
			if (available[resourceType] >= requests[resourceType]) {
				holds[resourceType] += request;
				requests[resourceType] = 0;
				available[resourceType] = available[resourceType] - requests[resourceType];
				blockedTime = 0;
				status = Status.READY;
			} else {
				blockedTime ++;
				status = Status.BLOCKED;
			}
			return;
		} else {
			if (holds[resourceType] + request > claims[resourceType]) {
				abort(available);
				return;
			}
			
			for (int i = 1; i < 6; i ++) {
				if (claims[i] - holds[i] > available[i]) {
					blockedTime ++;
					status = Status.BLOCKED;
					return;
				}
			}
			
			holds[resourceType] += request;
			requests[resourceType] = 0;
			available[resourceType] = available[resourceType] - requests[resourceType];
			blockedTime = 0;
			status = Status.READY;
		}
		
	}
	
	
	/*
	 * canRequest: used for optimistic only in case of deadlock
	 * given an array of the available resources, checks if the given task's request can be fulfilled
	 * returns true if it can, false if it cannot
	 * 
	 */
	public boolean canRequest(int[] available) {
		for (int i = 1; i < 6; i ++) {
			if (available[i] < requests[i]) {
				return false;
			}
		}
		return true;
	}
	
	/*
	 * release: params: resource type, number of that resource released, array to add the released resources to
	 * 
	 */
	
	public void release(int resourceType, int numReleased, int[] released) {
		holds[resourceType] = holds[resourceType] - numReleased;
		released[resourceType] = released[resourceType] + numReleased;
		status = Status.READY;
	}
	
	public int getComputeTime() {
		return computingTime;
	}
	
	public void setComputeTime(int computingTime) {
		this.computingTime = computingTime;
	}
	
	public void terminate() {
		status = Status.TERMINATED;
	}
	
	/*
	 * abort: param: array of resources to release into
	 * releases all resources currently held by the task into the array and sets status to aborted
	 * 
	 */
	public void abort(int[] released) {
		for (int i = 1; i < 6; i ++) {
			if (holds[i] != 0) {
				released[i] += holds[i];
				holds[i] = 0;
			}
		}
		status = Status.ABORTED;
	}
}
