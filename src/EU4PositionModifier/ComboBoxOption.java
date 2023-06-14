package EU4PositionModifier;

public class ComboBoxOption {

	private String displayName;
	private String value;
	
	public ComboBoxOption (String displayName, String value) {
		this.displayName = displayName;
		this.value = value;
	}
	
	public String getDisplayName() {
		return displayName;
	}
	
	public String getValue() {
		return value;
	}
	
	public String toString() {
		return getDisplayName();
	}
	
}
