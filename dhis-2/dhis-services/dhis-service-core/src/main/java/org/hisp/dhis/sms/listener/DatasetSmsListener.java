package org.hisp.dhis.sms.listener;

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.DataSetService;
import org.hisp.dhis.datavalue.DataValue;
import org.hisp.dhis.datavalue.DataValueService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.sms.command.SMSCommand;
import org.hisp.dhis.sms.incoming.IncomingSms;
import org.hisp.dhis.sms.parse.ParserType;
import org.hisp.dhis.sms.parse.SMSParserException;
import org.hisp.dhis.sms.parser.DatasetSmsMessage;
import org.hisp.dhis.sms.parser.KeysValue;
import org.hisp.dhis.sms.parser.SmsMessage;
import org.springframework.beans.factory.annotation.Autowired;

public class DatasetSmsListener extends AbstractSMSListener {
	
	private static final String DATASET_LOCKED = "Dataset: %s is locked for period: %s";
	
	@Autowired
    private DataSetService dataSetService;
	
	@Autowired
    private DataValueService dataValueService;
	
	@Autowired
    private DataElementService dataElementService;
	

	@Override
	public boolean isAcceptable(SmsMessage message) {
		
		return message.getParserType().equals(ParserType.DS);
	}

	@Override
	protected void postProcess(IncomingSms sms) {
		
		DatasetSmsMessage message = new DatasetSmsMessage(sms);
		
		Period period = message.getPeriod();
		
		OrganisationUnit orgUnit = organizationUnitService.getOrganisationUnit(message.getOrgUnit());
		
		DataSet dataset = dataSetService.getDataSet(message.getDatasetId());
		
        if ( dataSetService.isLocked( null, dataset, period, orgUnit, dataElementCategoryService.getDefaultCategoryOptionCombo(), null ) ) {
        	
            sendFeedback( String.format( DATASET_LOCKED, dataset.getUid(), period.getName() ), sms.getOriginator(), ERROR );

            throw new SMSParserException( String.format( DATASET_LOCKED, dataset.getUid(), period.getName() ) );
        }

        boolean valueStored = false;
        
        for (KeysValue kv : message.getKeysValues()) {
        	
        		valueStored = storeDataValue( sms, orgUnit, message, kv );
        		
        }

        if ( !valueStored ) {
        	
        		throw new SMSParserException( SMSCommand.WRONG_FORMAT_MESSAGE );
        }
	}
	

    private boolean storeDataValue( IncomingSms sms, OrganisationUnit orgunit, DatasetSmsMessage message, KeysValue kv )
    {
        String storedBy = message.getUserName();

        CategoryOptionCombo optionCombo = dataElementCategoryService.getCategoryOptionCombo( kv.getCategoryComboKey());

        Period period = message.getPeriod();
        
        DataElement element = dataElementService.getDataElement(kv.getDataElementKey());

        DataValue dv = dataValueService.getDataValue( element, period, orgunit, optionCombo );

        String value = kv.getValue();

        if ( !StringUtils.isEmpty( value ) )
        {
            boolean newDataValue = false;

            if ( dv == null )
            {
                dv = new DataValue();
                dv.setCategoryOptionCombo( optionCombo );
                dv.setSource( orgunit );
                dv.setDataElement( element );
                dv.setPeriod( period );
                dv.setComment( "" );
                newDataValue = true;
            }

            if ( ValueType.BOOLEAN == dv.getDataElement().getValueType() )
            {
                if ( "Y".equals( value.toUpperCase() ) || "YES".equals( value.toUpperCase() ) )
                {
                    value = "true";
                }
                else if ( "N".equals( value.toUpperCase() ) || "NO".equals( value.toUpperCase() ) )
                {
                    value = "false";
                }
            }
            else if ( dv.getDataElement().getValueType().isInteger() )
            {
                try
                {
                    Integer.parseInt( value );
                }
                catch ( NumberFormatException e )
                {
                    return false;
                }
            }

            dv.setValue( value );
            dv.setLastUpdated( new java.util.Date() );
            dv.setStoredBy( storedBy );

            if ( newDataValue )
            {
                dataValueService.addDataValue( dv );
            }
            else
            {
                dataValueService.updateDataValue( dv );
            }
        }

        return true;
    }

	/**
	 * @param dataSetService the dataSetService to set
	 */
	public void setDataSetService(DataSetService dataSetService) {
		this.dataSetService = dataSetService;
	}

	/**
	 * @param dataValueService the dataValueService to set
	 */
	public void setDataValueService(DataValueService dataValueService) {
		this.dataValueService = dataValueService;
	}

	/**
	 * @param dataElementService the dataElementService to set
	 */
	public void setDataElementService(DataElementService dataElementService) {
		this.dataElementService = dataElementService;
	}

}
