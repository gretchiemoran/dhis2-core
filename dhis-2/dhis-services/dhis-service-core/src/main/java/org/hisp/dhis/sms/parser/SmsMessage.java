package org.hisp.dhis.sms.parser;

import org.hisp.dhis.sms.incoming.IncomingSms;
import org.hisp.dhis.sms.parse.Parse;
import org.hisp.dhis.sms.parse.ParserType;
import org.hisp.dhis.sms.parse.SMSParserException;

public class SmsMessage {
	
	// Regex: match all characters as words between whitespace, including reserved characters in format (#,|,-,=)
	private static final String splitAtOneOrMoreWhitespace = "\\s+";
	
	private String message;
	
	protected String [] tokens; 
	
	public SmsMessage (IncomingSms sms) throws SMSParserException {
		
		tokens = sms.getText().split(splitAtOneOrMoreWhitespace);
	
	}

	public String getCheckSum() {
		return tokens[Parse.CHECKSUM];
	}

	public String getUserName() {
		return tokens[Parse.USERNAME];
	}

	public String getOrgUnit() {
		return tokens[Parse.ORGUNIT];
	}

	public ParserType getParserType() {
		return ParserType.valueOf(tokens[Parse.SUBTYPE]);
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}
	
	public boolean validated() {
		return true;
	}
	
}
