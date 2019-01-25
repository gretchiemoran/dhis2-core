package org.hisp.dhis.sms.listener;

import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.dataset.DataSetService;
import org.hisp.dhis.datavalue.DataValueService;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.sms.incoming.IncomingSms;
import org.hisp.dhis.sms.incoming.IncomingSmsService;
import org.hisp.dhis.sms.parse.SMSParserException;
import org.hisp.dhis.sms.parser.SmsMessage;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 
 * @author gmoran
 * 
 * Integrated test requires SL sample data set loaded into database; test exercises the "Mortality < 5 years" dataset.
 */
public class DatasetSmsListenerTest extends DhisSpringTest {
	
	private String correctFormatMessage = "#fe9f mobile DiszpKrYNg8 DS pBOMPrpg1QX AOC_UID 2018-10 |FTRrcoaog83-HllvX50cXC0=12|LjNlMTl9Nq9-HllvX50cXC0=0|eY5ehpbEsB7-HllvX50cXC0=35|Ix2HsbDMLea-HllvX50cXC0=0|NpJtsQkMTm3-HllvX50cXC0=0|r6nrJANOqMw-HllvX50cXC0=0|f7n9E0hX8qk-HllvX50cXC0=55|MSZuQ1mTsia-HllvX50cXC0=0|lXolhoWewYH-HllvX50cXC0=0|jVDAvs6kIAP-HllvX50cXC0=0|Vp12ncSU1Av-HllvX50cXC0=10|hM4ya5T2AqX-HllvX50cXC0=0|Yy9NtNfwYZJ-HllvX50cXC0=0|USBq0VHSkZq-HllvX50cXC0=0|";
	
	IncomingSms sms;
			
	private DatasetSmsListener listener = new DatasetSmsListener();
	
	@Autowired
    private DataSetService dataSetService;
	
	@Autowired
    private DataValueService dataValueService;
	
	@Autowired
    private DataElementService dataElementService;
	
    @Autowired
    protected OrganisationUnitService organizationUnitService;
    
    @Autowired
    protected CategoryService dataElementCategoryService;

    @Autowired
    private IncomingSmsService incomingSmsService;
    
	
	@Before
	public void setupTest() {
		
		listener.setOrganizationUnitService(organizationUnitService);
		listener.setDataElementCategoryService(dataElementCategoryService);
		listener.setDataElementService(dataElementService);
		listener.setDataSetService(dataSetService);
		listener.setDataValueService(dataValueService);
		listener.setIncomingSmsService(incomingSmsService);
		
		sms = new IncomingSms();
		sms.setText(correctFormatMessage);
		
	}

	
	@Test
	public void testIsAcceptable() throws SMSParserException {
		
		SmsMessage message = new SmsMessage(sms);
		Assert.assertTrue("PASS", listener.isAcceptable(message));
	}

	@Test
	public void testPostProcess() throws SMSParserException {
		
		try {
			
			listener.receive(sms);
		
		} catch (Exception e) {
		
			Assert.fail();
		
		}
		
		Assert.assertTrue(true);
	}

	
}
