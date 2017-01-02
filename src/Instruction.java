
public class Instruction {
	private String identity;
	private int value1;
	private int value2;
	
	// Value 1 is usually the resource number, and Value 2 is usually the amount of that resource
	// Value1 = second number in an instruction
	// Value2 = third number in an instruction
	
	public Instruction(String identity, int value1, int value2) {
		this.identity = identity;
		this.value1 = value1;
		this.value2 = value2;
	}
	
	public String getInstruction() {
		return identity;
	}
	
	public int getValue1() {
		return value1;
	}
	
	public int getValue2() {
		return value2;
	}
}
