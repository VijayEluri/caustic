package net.microscraper.util;

public class IntUUID implements UUID {

	private final int id;
	public IntUUID(int id) {
		this.id = id;
	}
	
	public int asInt() {
		return id;
	}

	public String asString() {
		return Integer.toString(id);
	}

}