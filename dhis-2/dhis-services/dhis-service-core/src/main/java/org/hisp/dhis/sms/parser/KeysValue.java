package org.hisp.dhis.sms.parser;

public class KeysValue {
	
	private String dataElementKey;
	
	private String CategoryComboKey;
	
	private String value;

	public String getDataElementKey() {
		return dataElementKey;
	}

	public void setDataElementKey(String dataElementKey) {
		this.dataElementKey = dataElementKey;
	}

	public String getCategoryComboKey() {
		return CategoryComboKey;
	}

	public void setCategoryComboKey(String categoryComboKey) {
		CategoryComboKey = categoryComboKey;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	} 

}
