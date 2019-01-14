package org.hisp.dhis.sms.parser;

import java.util.ArrayList;
import java.util.List;

import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.sms.incoming.IncomingSms;
import org.hisp.dhis.sms.parse.Parse;
import org.hisp.dhis.sms.parse.SMSParserException;

public class DatasetSmsMessage extends SmsMessage {
	

	public DatasetSmsMessage(IncomingSms sms) throws SMSParserException {
		
		super(sms);
	
	}
	
	public Period getPeriod() {
		return PeriodType.getPeriodFromIsoString(tokens[Parse.DS_PERIOD]);
	}
	 
	public String getDatasetId() {
		return tokens[Parse.DS_DATASET_UID];
	}
	
	public String getDataAttributeComboId() {
		return tokens[Parse.DS_ATT_OPTION_COMBO_UID];
	}
	
	public List <KeysValue> getKeysValues() throws SMSParserException {
		
		String dataset = tokens[Parse.DS_KEY_VALUES];
		
		String[] keyValues = dataset.split("\\".concat(Parse.PIPE));
		
		ArrayList <KeysValue> keysValues = new ArrayList<KeysValue>();
		
		for (String keyValue : keyValues) {
			
			if (keyValue.isEmpty()) {
				continue;
			}
			
			try {
				
				// FTRrcoaog83-HllvX50cXC0=12
				
				String[] split = keyValue.split(Parse.EQUALS);
				KeysValue kv = new KeysValue();
				kv.setCategoryComboKey(split[0].split(Parse.DASH)[1].trim());
				kv.setDataElementKey(split[0].split(Parse.DASH)[0].trim());
				kv.setValue(split[1]);
				
				keysValues.add(kv);
				
			} catch (Exception e) {

				throw new SMSParserException("Error in data value format:" + keyValue);
			
			}
			
		}
		
		return keysValues;
	}

}
