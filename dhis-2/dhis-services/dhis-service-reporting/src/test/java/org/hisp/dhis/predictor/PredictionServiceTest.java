package org.hisp.dhis.predictor;

/*
 * Copyright (c) 2004-2018, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.hisp.dhis.IntegrationTest;
import org.hisp.dhis.IntegrationTestBase;
import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.category.Category;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.*;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.DataSetService;
import org.hisp.dhis.datavalue.DataExportParams;
import org.hisp.dhis.datavalue.DataValue;
import org.hisp.dhis.datavalue.DataValueService;
import org.hisp.dhis.datavalue.DeflatedDataValue;
import org.hisp.dhis.expression.Expression;
import org.hisp.dhis.expression.ExpressionService;
import org.hisp.dhis.expression.MissingValueStrategy;
import org.hisp.dhis.jdbc.batchhandler.DataValueBatchHandler;
import org.hisp.dhis.mock.MockCurrentUserService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitLevel;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.quick.BatchHandler;
import org.hisp.quick.BatchHandlerFactory;
import org.joda.time.DateTime;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.google.common.collect.Sets.newHashSet;
import static org.hisp.dhis.expression.ExpressionService.SYMBOL_DAYS;
import static org.junit.Assert.assertEquals;

/**
 * @author Lars Helge Overland
 * @author Jim Grace
 */
@org.junit.experimental.categories.Category( IntegrationTest.class )
public class PredictionServiceTest
    extends IntegrationTestBase
{
    @Autowired
    private PredictionService predictionService;

    @Autowired
    private PredictorService predictorService;

    @Autowired
    private DataElementService dataElementService;

    @Autowired
    private OrganisationUnitService organisationUnitService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private ExpressionService expressionService;

    @Autowired
    private DataValueService dataValueService;

    @Autowired
    private PeriodService periodService;

    @Autowired
    private DataSetService dataSetService;

    @Autowired
    private CurrentUserService currentUserService;

    @Autowired
    private BatchHandlerFactory batchHandlerFactory;

    private OrganisationUnitLevel orgUnitLevel1;

    private DataElement dataElementA;
    private DataElement dataElementB;
    private DataElement dataElementC;
    private DataElement dataElementD;
    private DataElement dataElementX;
    private DataElement dataElementY;

    private CategoryOptionCombo defaultCombo;

    private CategoryOptionCombo altCombo;

    CategoryOption altCategoryOption;
    Category altCategory;
    CategoryCombo altCategoryCombo;

    private Set<DataElement> dataElements;

    private OrganisationUnit sourceA, sourceB, sourceC, sourceD, sourceE, sourceF, sourceG;

    private Set<CategoryOptionCombo> optionCombos;

    private Expression expressionA;
    private Expression expressionB;
    private Expression expressionC;
    private Expression expressionD;
    private Expression expressionE;
    private Expression expressionF;
    private Expression expressionG;

    private PeriodType periodTypeMonthly;

    private DataSet dataSetMonthly;

    private BatchHandler<DataValue> dataValueBatchHandler;

    private PredictionSummary summary;

    // -------------------------------------------------------------------------
    // Fixture
    // -------------------------------------------------------------------------

    @Override
    public void setUpTest()
        throws Exception
    {
        orgUnitLevel1 = new OrganisationUnitLevel( 1, "Level1" );

        organisationUnitService.addOrganisationUnitLevel( orgUnitLevel1 );

        dataElementA = createDataElement( 'A' );
        dataElementB = createDataElement( 'B' );
        dataElementC = createDataElement( 'C' );
        dataElementD = createDataElement( 'D' );
        dataElementX = createDataElement( 'X', ValueType.NUMBER, AggregationType.NONE );
        dataElementY = createDataElement( 'Y', ValueType.INTEGER, AggregationType.NONE );

        dataElementService.addDataElement( dataElementA );
        dataElementService.addDataElement( dataElementB );
        dataElementService.addDataElement( dataElementC );
        dataElementService.addDataElement( dataElementD );
        dataElementService.addDataElement( dataElementX );
        dataElementService.addDataElement( dataElementY );

        dataElements = new HashSet<>();

        dataElements.add( dataElementA );
        dataElements.add( dataElementB );
        dataElements.add( dataElementC );
        dataElements.add( dataElementD );

        sourceA = createOrganisationUnit( 'A' );
        sourceB = createOrganisationUnit( 'B' );
        sourceC = createOrganisationUnit( 'C', sourceB );
        sourceD = createOrganisationUnit( 'D', sourceB );
        sourceE = createOrganisationUnit( 'E', sourceD );
        sourceF = createOrganisationUnit( 'F', sourceD );
        sourceG = createOrganisationUnit( 'G' );

        organisationUnitService.addOrganisationUnit( sourceA );
        organisationUnitService.addOrganisationUnit( sourceB );
        organisationUnitService.addOrganisationUnit( sourceC );
        organisationUnitService.addOrganisationUnit( sourceD );
        organisationUnitService.addOrganisationUnit( sourceE );
        organisationUnitService.addOrganisationUnit( sourceF );
        organisationUnitService.addOrganisationUnit( sourceG );

        periodTypeMonthly = PeriodType.getPeriodTypeByName( "Monthly" );
        dataSetMonthly = createDataSet( 'M', periodTypeMonthly );

        dataSetMonthly.addDataSetElement( dataElementA );
        dataSetMonthly.addDataSetElement( dataElementB );
        dataSetMonthly.addDataSetElement( dataElementC );
        dataSetMonthly.addDataSetElement( dataElementD );
        dataSetMonthly.addDataSetElement( dataElementX );
        dataSetMonthly.addDataSetElement( dataElementY );

        dataSetMonthly.addOrganisationUnit( sourceA );
        dataSetMonthly.addOrganisationUnit( sourceB );
        dataSetMonthly.addOrganisationUnit( sourceC );
        dataSetMonthly.addOrganisationUnit( sourceD );
        dataSetMonthly.addOrganisationUnit( sourceE );
        dataSetMonthly.addOrganisationUnit( sourceG );

        dataSetService.addDataSet( dataSetMonthly );

        CategoryOptionCombo categoryOptionCombo = categoryService.getDefaultCategoryOptionCombo();

        defaultCombo = categoryService.getDefaultCategoryOptionCombo();

        altCategoryOption = new CategoryOption( "AltCategoryOption" );
        categoryService.addCategoryOption( altCategoryOption );
        altCategory = createCategory( 'A', altCategoryOption );
        categoryService.addCategory( altCategory );

        altCategoryCombo = createCategoryCombo( 'Y', altCategory );
        categoryService.addCategoryCombo( altCategoryCombo );

        altCombo = createCategoryOptionCombo( 'Z', altCategoryCombo, altCategoryOption );

        optionCombos = new HashSet<>();
        optionCombos.add( categoryOptionCombo );
        optionCombos.add( altCombo );

        categoryService.addCategoryOptionCombo( altCombo );

        expressionA = new Expression(
            "AVG(#{" + dataElementA.getUid() + "})+1.5*StdDev(#{" + dataElementA.getUid() + "})", "descriptionA" );
        expressionB = new Expression( "avg(#{" + dataElementB.getUid() + "." + defaultCombo.getUid() + "})", "descriptionB" );
        expressionC = new Expression( "135.79", "descriptionC" );
        expressionD = new Expression( SYMBOL_DAYS, "descriptionD" );
        expressionE = new Expression( "SUM(#{" + dataElementA.getUid() + "})+#{" + dataElementB.getUid() + "}", "descriptionE" );
        expressionF = new Expression( "#{" + dataElementB.getUid() + "}", "descriptionF" );
        expressionG = new Expression( "SUM(#{" + dataElementA.getUid() + "}+#{" + dataElementB.getUid() + "})", "descriptionG" );

        expressionService.addExpression( expressionA );
        expressionService.addExpression( expressionB );
        expressionService.addExpression( expressionC );
        expressionService.addExpression( expressionD );

        summary = new PredictionSummary();

        dataValueBatchHandler = batchHandlerFactory.createBatchHandler( DataValueBatchHandler.class ).init();

        Set<OrganisationUnit> units = newHashSet( sourceA, sourceB, sourceG );
        CurrentUserService mockCurrentUserService = new MockCurrentUserService( true, units, units );
        setDependency( predictionService, "currentUserService", mockCurrentUserService, CurrentUserService.class );
    }

    @Override
    public boolean emptyDatabaseAfterTest()
    {
        return true;
    }

    @Override
    public void tearDownTest()
    {
        setDependency( predictionService, "currentUserService", currentUserService, CurrentUserService.class );
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    private Period makeMonth( int year, int month )
    {
        Date start = getDate( year, month, 1 );
        Period period = periodTypeMonthly.createPeriod( start );
        Date end = getDate( year, month, period.getDaysInPeriod() );
        return createPeriod( periodTypeMonthly, start, end );
    }

    private Date monthStart( int year, int month )
    {
        DateTime starting = new DateTime( year, month, 1, 0, 0 );

        return starting.toDate();
    }

    private void useDataValue( DataElement e, Period p, OrganisationUnit s, Number value )
    {
        useDataValue(  e, p, s, defaultCombo, value );
    }

    private void useDataValue( DataElement e, Period p, OrganisationUnit s, CategoryOptionCombo attributeOptionCombo, Number value )
    {
        dataValueBatchHandler.addObject( createDataValue( e, periodService.reloadPeriod( p ), s, value.toString(), defaultCombo, attributeOptionCombo ) );
    }

    private String getDataValue( DataElement dataElement, CategoryOptionCombo combo, OrganisationUnit source, Period period )
    {
        return getDataValue( dataElement, combo, defaultCombo, source, period );
    }

    private String getDataValue( DataElement dataElement, CategoryOptionCombo combo,
        CategoryOptionCombo attributeOptionCombo, OrganisationUnit source, Period period )
    {
        DataExportParams params = new DataExportParams()
            .setDataElementOperands( Sets.newHashSet( new DataElementOperand( dataElement, combo ) ) )
            .setAttributeOptionCombos( Sets.newHashSet( attributeOptionCombo ) )
            .setOrganisationUnits( Sets.newHashSet( source ) )
            .setPeriods( Sets.newHashSet( periodService.reloadPeriod( period ) ) );

        List<DeflatedDataValue> values = dataValueService.getDeflatedDataValues( params );

        if ( values != null && values.size() > 0 )
        {
            return values.get( 0 ).getValue();
        }

        return null;
    }

    private String shortSummary( PredictionSummary summary )
    {
        return "Pred " + summary.getPredictors()
            + " Ins " + summary.getInserted()
            + " Upd " + summary.getUpdated()
            + " Del " + summary.getDeleted()
            + " Unch " + summary.getUnchanged();
    }

    private void setupTestData()
    {
        // dataElementA - 2001

        useDataValue( dataElementA, makeMonth( 2001, 6 ), sourceA, 5 );
        useDataValue( dataElementA, makeMonth( 2001, 7 ), sourceA, 3 );
        useDataValue( dataElementA, makeMonth( 2001, 8 ), sourceA, 8 );
        useDataValue( dataElementA, makeMonth( 2001, 9 ), sourceA, 4 );
        useDataValue( dataElementA, makeMonth( 2001, 10 ), sourceA, 7 );

        useDataValue( dataElementA, makeMonth( 2001, 6 ), sourceC, 6 );
        useDataValue( dataElementA, makeMonth( 2001, 7 ), sourceC, 4 );
        useDataValue( dataElementA, makeMonth( 2001, 8 ), sourceC, 7 );
        useDataValue( dataElementA, makeMonth( 2001, 9 ), sourceC, 4 );
        useDataValue( dataElementA, makeMonth( 2001, 10 ), sourceC, 7 );

        useDataValue( dataElementA, makeMonth( 2001, 6 ), sourceE, 2 );
        useDataValue( dataElementA, makeMonth( 2001, 7 ), sourceE, 1 );
        useDataValue( dataElementA, makeMonth( 2001, 8 ), sourceE, 3 );
        useDataValue( dataElementA, makeMonth( 2001, 9 ), sourceE, 2 );
        useDataValue( dataElementA, makeMonth( 2001, 10 ), sourceE, 1 );

        useDataValue( dataElementA, makeMonth( 2001, 6 ), sourceF, 3 );
        useDataValue( dataElementA, makeMonth( 2001, 7 ), sourceF, 2 );
        useDataValue( dataElementA, makeMonth( 2001, 8 ), sourceF, 4 );
        useDataValue( dataElementA, makeMonth( 2001, 9 ), sourceF, 3 );
        useDataValue( dataElementA, makeMonth( 2001, 10 ), sourceF, 2 );

        // dataElementA - 2002

        useDataValue( dataElementA, makeMonth( 2002, 6 ), sourceA, 8 );
        useDataValue( dataElementA, makeMonth( 2002, 7 ), sourceA, 4 );
        useDataValue( dataElementA, makeMonth( 2002, 8 ), sourceA, 10 );
        useDataValue( dataElementA, makeMonth( 2002, 9 ), sourceA, 5 );
        useDataValue( dataElementA, makeMonth( 2002, 10 ), sourceA, 7 );

        useDataValue( dataElementA, makeMonth( 2002, 6 ), sourceC, 7 );
        useDataValue( dataElementA, makeMonth( 2002, 7 ), sourceC, 4 );
        useDataValue( dataElementA, makeMonth( 2002, 8 ), sourceC, 11 );
        useDataValue( dataElementA, makeMonth( 2002, 9 ), sourceC, 5 );
        useDataValue( dataElementA, makeMonth( 2002, 10 ), sourceC, 6 );

        useDataValue( dataElementA, makeMonth( 2002, 6 ), sourceE, 3 );
        useDataValue( dataElementA, makeMonth( 2002, 7 ), sourceE, 2 );
        useDataValue( dataElementA, makeMonth( 2002, 8 ), sourceE, 1 );
        useDataValue( dataElementA, makeMonth( 2002, 9 ), sourceE, 2 );
        useDataValue( dataElementA, makeMonth( 2002, 10 ), sourceE, 2 );

        useDataValue( dataElementA, makeMonth( 2002, 6 ), sourceF, 4 );
        useDataValue( dataElementA, makeMonth( 2002, 7 ), sourceF, 3 );
        useDataValue( dataElementA, makeMonth( 2002, 8 ), sourceF, 2 );
        useDataValue( dataElementA, makeMonth( 2002, 9 ), sourceF, 3 );
        useDataValue( dataElementA, makeMonth( 2002, 10 ), sourceF, 3 );

        // dataElementA - 2003

        useDataValue( dataElementA, makeMonth( 2003, 5 ), sourceA, 9 );
        useDataValue( dataElementA, makeMonth( 2003, 6 ), sourceA, 11 );
        useDataValue( dataElementA, makeMonth( 2003, 7 ), sourceA, 6 );
        useDataValue( dataElementA, makeMonth( 2003, 8 ), sourceA, 7 );
        useDataValue( dataElementA, makeMonth( 2003, 9 ), sourceA, 9 );
        useDataValue( dataElementA, makeMonth( 2003, 10 ), sourceA, 10 );

        useDataValue( dataElementA, makeMonth( 2003, 5 ), sourceC, 10 );
        useDataValue( dataElementA, makeMonth( 2003, 6 ), sourceC, 10 );
        useDataValue( dataElementA, makeMonth( 2003, 7 ), sourceC, 7 );
        useDataValue( dataElementA, makeMonth( 2003, 8 ), sourceC, 7 );
        useDataValue( dataElementA, makeMonth( 2003, 9 ), sourceC, 8 );
        useDataValue( dataElementA, makeMonth( 2003, 10 ), sourceC, 9 );

        useDataValue( dataElementA, makeMonth( 2003, 5 ), sourceE, 4 );
        useDataValue( dataElementA, makeMonth( 2003, 6 ), sourceE, 4 );
        useDataValue( dataElementA, makeMonth( 2003, 7 ), sourceE, 3 );
        useDataValue( dataElementA, makeMonth( 2003, 8 ), sourceE, 2 );
        useDataValue( dataElementA, makeMonth( 2003, 9 ), sourceE, 2 );
        useDataValue( dataElementA, makeMonth( 2003, 10 ), sourceE, 1 );

        useDataValue( dataElementA, makeMonth( 2003, 5 ), sourceF, 5 );
        useDataValue( dataElementA, makeMonth( 2003, 6 ), sourceF, 5 );
        useDataValue( dataElementA, makeMonth( 2003, 7 ), sourceF, 4 );
        useDataValue( dataElementA, makeMonth( 2003, 8 ), sourceF, 3 );
        useDataValue( dataElementA, makeMonth( 2003, 9 ), sourceF, 3 );
        useDataValue( dataElementA, makeMonth( 2003, 10 ), sourceF, 2 );

        // dataElementA - 2004

        useDataValue( dataElementA, makeMonth( 2004, 5 ), sourceA, 4 );
        useDataValue( dataElementA, makeMonth( 2004, 6 ), sourceA, 8 );
        useDataValue( dataElementA, makeMonth( 2004, 7 ), sourceA, 4 );
        useDataValue( dataElementA, makeMonth( 2004, 8 ), sourceA, 7 );
        useDataValue( dataElementA, makeMonth( 2004, 9 ), sourceA, 5 );
        useDataValue( dataElementA, makeMonth( 2004, 10 ), sourceA, 6 );

        useDataValue( dataElementA, makeMonth( 2004, 5 ), sourceC, 5 );
        useDataValue( dataElementA, makeMonth( 2004, 6 ), sourceC, 9 );
        useDataValue( dataElementA, makeMonth( 2004, 7 ), sourceC, 6 );
        useDataValue( dataElementA, makeMonth( 2004, 8 ), sourceC, 7 );
        useDataValue( dataElementA, makeMonth( 2004, 9 ), sourceC, 6 );
        useDataValue( dataElementA, makeMonth( 2004, 10 ), sourceC, 5 );

        useDataValue( dataElementA, makeMonth( 2004, 5 ), sourceE, 5 );
        useDataValue( dataElementA, makeMonth( 2004, 6 ), sourceE, 7 );
        useDataValue( dataElementA, makeMonth( 2004, 7 ), sourceE, 5 );
        useDataValue( dataElementA, makeMonth( 2004, 8 ), sourceE, 4 );
        useDataValue( dataElementA, makeMonth( 2004, 9 ), sourceE, 4 );
        useDataValue( dataElementA, makeMonth( 2004, 10 ), sourceE, 3 );

        useDataValue( dataElementA, makeMonth( 2004, 5 ), sourceF, 6 );
        useDataValue( dataElementA, makeMonth( 2004, 6 ), sourceF, 8 );
        useDataValue( dataElementA, makeMonth( 2004, 7 ), sourceF, 6 );
        useDataValue( dataElementA, makeMonth( 2004, 8 ), sourceF, 5 );
        useDataValue( dataElementA, makeMonth( 2004, 9 ), sourceF, 5 );
        useDataValue( dataElementA, makeMonth( 2004, 10 ), sourceF, 4 );

        // dataElementB - 2003

        useDataValue( dataElementB, makeMonth( 2003, 6 ), sourceA, 1 );
        useDataValue( dataElementB, makeMonth( 2003, 7 ), sourceA, 1 );
        useDataValue( dataElementB, makeMonth( 2003, 8 ), sourceA, 1 );
        useDataValue( dataElementB, makeMonth( 2003, 9 ), sourceA, 1 );
        useDataValue( dataElementB, makeMonth( 2003, 10 ), sourceA, 1 );

        useDataValue( dataElementB, makeMonth( 2003, 5 ), sourceF, 1 );
        useDataValue( dataElementB, makeMonth( 2003, 6 ), sourceF, 1 );
        useDataValue( dataElementB, makeMonth( 2003, 7 ), sourceF, 1 );
        useDataValue( dataElementB, makeMonth( 2003, 9 ), sourceF, 1 );
        useDataValue( dataElementB, makeMonth( 2003, 10 ), sourceF, 1 );

        dataValueBatchHandler.flush();
    }

    // -------------------------------------------------------------------------
    // Prediction tests
    // -------------------------------------------------------------------------

    @Test
    public void testPredictWithCategoryOptionCombo()
    {
        useDataValue( dataElementB, makeMonth( 2001, 6 ), sourceA, 5 );

        dataValueBatchHandler.flush();

        Predictor p = createPredictor( dataElementX, defaultCombo, "A", expressionB, null,
            periodTypeMonthly, orgUnitLevel1, 1, 0, 0 );

        predictionService.predict( p, monthStart( 2001, 7 ), monthStart( 2001, 8 ), summary );

        assertEquals( "Pred 1 Ins 1 Upd 0 Del 0 Unch 0", shortSummary( summary ) );

        assertEquals( "5.0", getDataValue( dataElementX, defaultCombo, sourceA, makeMonth( 2001, 7 ) ) );
    }

    @Test
    public void testPredictSequential()
    {
        setupTestData();

        Predictor p = createPredictor( dataElementX, defaultCombo, "PredictSequential",
            expressionA, null, periodTypeMonthly, orgUnitLevel1, 3, 1, 0 );

        predictionService.predict( p, monthStart( 2001, 7 ), monthStart( 2001, 12 ), summary );

        assertEquals( "Pred 1 Ins 8 Upd 0 Del 0 Unch 0", shortSummary( summary ) );

        assertEquals( "5.0", getDataValue( dataElementX, defaultCombo, sourceA, makeMonth( 2001, 8 ) ) );
        assertEquals( "5.5", getDataValue( dataElementX, defaultCombo, sourceA, makeMonth( 2001, 9 ) ) );
        assertEquals( "9.25", getDataValue( dataElementX, defaultCombo, sourceA, makeMonth( 2001, 10 ) ) );
        assertEquals( "9.0", getDataValue( dataElementX, defaultCombo, sourceA, makeMonth( 2001, 11 ) ) );

        assertEquals( "11.0", getDataValue( dataElementX, defaultCombo, sourceB, makeMonth( 2001, 8 ) ) );
        assertEquals( "12.0", getDataValue( dataElementX, defaultCombo, sourceB, makeMonth( 2001, 9 ) ) );
        assertEquals( "15.75", getDataValue( dataElementX, defaultCombo, sourceB, makeMonth( 2001, 10 ) ) );
        assertEquals( "15.25", getDataValue( dataElementX, defaultCombo, sourceB, makeMonth( 2001, 11 ) ) );

        // Make sure we can do it again.

        summary = new PredictionSummary();

        predictionService.predict( p, monthStart( 2001, 7 ), monthStart( 2001, 12 ), summary );

        assertEquals( "Pred 1 Ins 0 Upd 0 Del 0 Unch 8", shortSummary( summary ) );
    }

    @Test
    public void testPredictSeasonal()
    {
        setupTestData();

        Predictor p = createPredictor( dataElementX, altCombo, "GetPredictionsSeasonal",
            expressionA, null, periodTypeMonthly, orgUnitLevel1, 3, 1, 2 );

        predictionService.predict( p, monthStart( 2001, 7 ), monthStart( 2005, 12 ), summary );

        assertEquals( "Pred 1 Ins 100 Upd 0 Del 0 Unch 0", shortSummary( summary ) );

        assertEquals( "5.0", getDataValue( dataElementX, altCombo, sourceA, makeMonth( 2001, 8 ) ) );
        assertEquals( "5.5", getDataValue( dataElementX, altCombo, sourceA, makeMonth( 2001, 9 ) ) );
        assertEquals( "7.0", getDataValue( dataElementX, altCombo, sourceA, makeMonth( 2003, 1 ) ) );
        assertEquals( "8.75", getDataValue( dataElementX, altCombo, sourceA, makeMonth( 2003, 3 ) ) );
        assertEquals( "10.94", getDataValue( dataElementX, altCombo, sourceA, makeMonth( 2004, 7 ) ) );
        assertEquals( "10.85", getDataValue( dataElementX, altCombo, sourceA, makeMonth( 2004, 8 ) ) );

        // This value is derived from organisation units beneath the actual *sourceB*.
        assertEquals( "18.14", getDataValue( dataElementX, altCombo, sourceB, makeMonth( 2004, 7 ) ) );
    }

    @Test
    public void testGetPredictionsSeasonalWithOutbreak()
    {
        setupTestData();

        String auid = dataElementA.getUid();
        Predictor p = createPredictor( dataElementX, altCombo, "GetPredictionsSeasonalWithOutbreak",
            new Expression( "AVG(#{" + auid + "})+1.5*STDDEV(#{" + auid + "})", "descriptionA" ),
            new Expression( "#{" + dataElementB.getUid() + "}", "outbreak" ),
            periodTypeMonthly, orgUnitLevel1, 3, 1, 2 );

        predictionService.predict( p, monthStart( 2001, 7 ), monthStart( 2005, 12 ), summary );

        assertEquals( "Pred 1 Ins 99 Upd 0 Del 0 Unch 0", shortSummary( summary ) );

        assertEquals( "5.0", getDataValue( dataElementX, altCombo, sourceA, makeMonth( 2001, 8 ) ) );
        assertEquals( "5.5", getDataValue( dataElementX, altCombo, sourceA, makeMonth( 2001, 9 ) ) );
        assertEquals( "7.0", getDataValue( dataElementX, altCombo, sourceA, makeMonth( 2003, 1 ) ) );
        assertEquals( "8.75", getDataValue( dataElementX, altCombo, sourceA, makeMonth( 2003, 3 ) ) );
        assertEquals( "10.09", getDataValue( dataElementX, altCombo, sourceA, makeMonth( 2004, 7 ) ) );
        assertEquals( "10.1", getDataValue( dataElementX, altCombo, sourceA, makeMonth( 2004, 8 ) ) );

        // This value is derived from organisation units beneath the actual *sourceB*.
        assertEquals( "15.75", getDataValue( dataElementX, altCombo, sourceB, makeMonth( 2004, 7 ) ) );
    }

    @Test
    public void testPredictConstant()
    {
        setupTestData();

        Predictor p = createPredictor( dataElementX, defaultCombo, "PredictConstant",
            expressionC, null, periodTypeMonthly, orgUnitLevel1, 0, 0, 0 );

        predictionService.predict( p, monthStart( 2001, 7 ), monthStart( 2001, 9 ), summary );

        assertEquals( "Pred 1 Ins 6 Upd 0 Del 0 Unch 0", shortSummary( summary ) );

        assertEquals( "135.8", getDataValue( dataElementX, defaultCombo, sourceA, makeMonth( 2001, 7 ) ) );
        assertEquals( "135.8", getDataValue( dataElementX, defaultCombo, sourceA, makeMonth( 2001, 8 ) ) );

        assertEquals( "135.8", getDataValue( dataElementX, defaultCombo, sourceB, makeMonth( 2001, 7 ) ) );
        assertEquals( "135.8", getDataValue( dataElementX, defaultCombo, sourceB, makeMonth( 2001, 8 ) ) );

        assertEquals( "135.8", getDataValue( dataElementX, defaultCombo, sourceG, makeMonth( 2001, 7 ) ) );
        assertEquals( "135.8", getDataValue( dataElementX, defaultCombo, sourceG, makeMonth( 2001, 8 ) ) );
    }

    @Test
    public void testPredictInteger()
    {
        setupTestData();

        Predictor p = createPredictor( dataElementY, defaultCombo, "PredictInteger",
            expressionC, null, periodTypeMonthly, orgUnitLevel1, 0, 0, 0 );

        predictionService.predict( p, monthStart( 2001, 7 ), monthStart( 2001, 8 ), summary );

        assertEquals( "Pred 1 Ins 3 Upd 0 Del 0 Unch 0", shortSummary( summary ) );

        assertEquals( "136", getDataValue( dataElementY, defaultCombo, sourceA, makeMonth( 2001, 7 ) ) );
        assertEquals( "136", getDataValue( dataElementY, defaultCombo, sourceB, makeMonth( 2001, 7 ) ) );
        assertEquals( "136", getDataValue( dataElementY, defaultCombo, sourceG, makeMonth( 2001, 7 ) ) );
    }

    @Test
    public void testPredictDays()
    {
        setupTestData();

        Predictor p = createPredictor( dataElementX, altCombo, "PredictDays",
            expressionD, null, periodTypeMonthly, orgUnitLevel1, 0, 0, 0 );

        predictionService.predict( p, monthStart( 2001, 8 ), monthStart( 2001, 10 ), summary );

        assertEquals( "Pred 1 Ins 6 Upd 0 Del 0 Unch 0", shortSummary( summary ) );

        assertEquals( "31.0", getDataValue( dataElementX, altCombo, sourceA, makeMonth( 2001, 8 ) ) );
        assertEquals( "30.0", getDataValue( dataElementX, altCombo, sourceA, makeMonth( 2001, 9 ) ) );

        assertEquals( "31.0", getDataValue( dataElementX, altCombo, sourceB, makeMonth( 2001, 8 ) ) );
        assertEquals( "30.0", getDataValue( dataElementX, altCombo, sourceB, makeMonth( 2001, 9 ) ) );

        assertEquals( "31.0", getDataValue( dataElementX, altCombo, sourceG, makeMonth( 2001, 8 ) ) );
        assertEquals( "30.0", getDataValue( dataElementX, altCombo, sourceG, makeMonth( 2001, 9 ) ) );
    }

    @Test
    public void testPredictNoPeriods()
    {
        setupTestData();

        Predictor p = createPredictor( dataElementX, altCombo, "PredictDays",
            expressionD, null, periodTypeMonthly, orgUnitLevel1, 3, 1, 2 );

        predictionService.predict( p, monthStart( 2001, 8 ), monthStart( 2001, 8 ), summary );

        assertEquals( "Pred 1 Ins 0 Upd 0 Del 0 Unch 0", shortSummary( summary ) );
    }

    @Test
    public void testPredictWithCurrentPeriodData()
    {
        useDataValue( dataElementA, makeMonth( 2001, 6 ), sourceA, 10 );
        useDataValue( dataElementA, makeMonth( 2001, 7 ), sourceA, 20 );
        useDataValue( dataElementA, makeMonth( 2001, 8 ), sourceA, 30 );
        useDataValue( dataElementA, makeMonth( 2001, 9 ), sourceA, 40 );

        useDataValue( dataElementB, makeMonth( 2001, 7 ), sourceA, 1 );
        useDataValue( dataElementB, makeMonth( 2001, 8 ), sourceA, 2 );
        useDataValue( dataElementB, makeMonth( 2001, 9 ), sourceA, 3 );
        useDataValue( dataElementB, makeMonth( 2001, 10 ), sourceA, 4 );

        dataValueBatchHandler.flush();

        try { Thread.sleep( 1000 ); } catch ( Exception e ) {} //TODO: Why is this needed here (and not everywhere)?

        Predictor p = createPredictor( dataElementX, defaultCombo, "PredictWithCurrentPeriodData",
            expressionE, null, periodTypeMonthly, orgUnitLevel1, 1, 0, 0 );

        predictionService.predict( p, monthStart( 2001, 7 ), monthStart( 2001, 11 ), summary );

        assertEquals( "Pred 1 Ins 4 Upd 0 Del 0 Unch 0", shortSummary( summary ) );

        assertEquals( "11.0", getDataValue( dataElementX, defaultCombo, sourceA, makeMonth( 2001, 7 ) ) );
        assertEquals( "22.0", getDataValue( dataElementX, defaultCombo, sourceA, makeMonth( 2001, 8 ) ) );
        assertEquals( "33.0", getDataValue( dataElementX, defaultCombo, sourceA, makeMonth( 2001, 9 ) ) );
        assertEquals( "44.0", getDataValue( dataElementX, defaultCombo, sourceA, makeMonth( 2001, 10 ) ) );
    }

    @Test
    public void testPredictWithOnlyCurrentPeriodData()
    {
        useDataValue( dataElementA, makeMonth( 2001, 6 ), sourceA, 10 );
        useDataValue( dataElementA, makeMonth( 2001, 7 ), sourceA, 20 );
        useDataValue( dataElementA, makeMonth( 2001, 8 ), sourceA, 30 );
        useDataValue( dataElementA, makeMonth( 2001, 9 ), sourceA, 40 );

        useDataValue( dataElementB, makeMonth( 2001, 7 ), sourceA, 1 );
        useDataValue( dataElementB, makeMonth( 2001, 8 ), sourceA, 2 );
        useDataValue( dataElementB, makeMonth( 2001, 9 ), sourceA, 3 );
        useDataValue( dataElementB, makeMonth( 2001, 10 ), sourceA, 4 );

        dataValueBatchHandler.flush();

        try { Thread.sleep( 1000 ); } catch ( Exception e ) {} //TODO: Why is this needed here (and not everywhere)?

        Predictor p = createPredictor( dataElementX, defaultCombo, "PredictWithOnlyCurrentPeriodData",
            expressionF, null, periodTypeMonthly, orgUnitLevel1, 1, 0, 0 );

        predictionService.predict( p, monthStart( 2001, 7 ), monthStart( 2001, 11 ), summary );

        assertEquals( "Pred 1 Ins 4 Upd 0 Del 0 Unch 0", shortSummary( summary ) );

        assertEquals( "1.0", getDataValue( dataElementX, defaultCombo, sourceA, makeMonth( 2001, 7 ) ) );
        assertEquals( "2.0", getDataValue( dataElementX, defaultCombo, sourceA, makeMonth( 2001, 8 ) ) );
        assertEquals( "3.0", getDataValue( dataElementX, defaultCombo, sourceA, makeMonth( 2001, 9 ) ) );
        assertEquals( "4.0", getDataValue( dataElementX, defaultCombo, sourceA, makeMonth( 2001, 10 ) ) );
    }

    @Test
    public void testPredictMultipleDataElements()
    {
        useDataValue( dataElementA, makeMonth( 2010, 6 ), sourceA, 3 );
        useDataValue( dataElementB, makeMonth( 2010, 6 ), sourceA, 5 );

        dataValueBatchHandler.flush();

        Predictor p = createPredictor( dataElementX, defaultCombo, "A", expressionG, null,
            periodTypeMonthly, orgUnitLevel1, 1, 0, 0 );

        predictionService.predict( p, monthStart( 2010, 7 ), monthStart( 2010, 8 ), summary );

        assertEquals( "Pred 1 Ins 1 Upd 0 Del 0 Unch 0", shortSummary( summary ) );

        assertEquals( "8.0", getDataValue( dataElementX, defaultCombo, sourceA, makeMonth( 2010, 7 ) ) );
    }

    @Test
    public void testPredictMultipleAttributeOptionCombos()
    {
        CategoryOption optionJ = new CategoryOption( "CategoryOptionJ" );
        CategoryOption optionK = new CategoryOption( "CategoryOptionK" );
        CategoryOption optionL = new CategoryOption( "CategoryOptionL" );

        categoryService.addCategoryOption( optionJ );
        categoryService.addCategoryOption( optionK );
        categoryService.addCategoryOption( optionL );

        Category categoryJ = createCategory( 'J', optionJ, optionK );
        Category categoryL = createCategory( 'L', optionL );
        categoryJ.setDataDimension( true );
        categoryL.setDataDimension( true );

        categoryService.addCategory( categoryJ );
        categoryService.addCategory( categoryL );

        CategoryCombo categoryComboJL = createCategoryCombo( 'A', categoryJ, categoryL );

        categoryService.addCategoryCombo( categoryComboJL );

        CategoryOptionCombo optionComboJL = createCategoryOptionCombo( 'A',
            categoryComboJL, optionJ, optionK );
        CategoryOptionCombo optionComboKL = createCategoryOptionCombo( 'A',
            categoryComboJL, optionK, optionL );

        categoryService.addCategoryOptionCombo( optionComboJL );
        categoryService.addCategoryOptionCombo( optionComboKL );

        useDataValue( dataElementA, makeMonth( 2011, 6 ), sourceA, optionComboJL, 1 );
        useDataValue( dataElementB, makeMonth( 2011, 6 ), sourceA, optionComboJL, 2 );

        useDataValue( dataElementA, makeMonth( 2011, 6 ), sourceA, optionComboKL, 3 );
        useDataValue( dataElementB, makeMonth( 2011, 6 ), sourceA, optionComboKL, 4 );

        dataValueBatchHandler.flush();

        Predictor p = createPredictor( dataElementX, defaultCombo, "A", expressionG, null,
            periodTypeMonthly, orgUnitLevel1, 1, 0, 0 );

        predictionService.predict( p, monthStart( 2011, 7 ), monthStart( 2011, 8 ), summary );

        assertEquals( "Pred 1 Ins 2 Upd 0 Del 0 Unch 0", shortSummary( summary ) );

        assertEquals( "3.0", getDataValue( dataElementX, defaultCombo, optionComboJL, sourceA, makeMonth( 2011, 7 ) ) );
        assertEquals( "7.0", getDataValue( dataElementX, defaultCombo, optionComboKL, sourceA, makeMonth( 2011, 7 ) ) );
    }

    @Test
    public void testPredictIf()
    {
        useDataValue( dataElementA, makeMonth( 2001, 6 ), sourceA, 10 );
        useDataValue( dataElementB, makeMonth( 2001, 6 ), sourceA, 10 );

        useDataValue( dataElementA, makeMonth( 2001, 7 ), sourceA, 20 );
        useDataValue( dataElementB, makeMonth( 2001, 7 ), sourceA, 40 );

        dataValueBatchHandler.flush();

        Predictor p = createPredictor( dataElementX, defaultCombo, "PredictIf_A",
            new Expression( "If(#{" + dataElementB.getUid() + "} == #{" + dataElementA.getUid() + "},1,2)", "ExpressionIf_A" ),
            null, periodTypeMonthly, orgUnitLevel1, 1, 0, 0 );

        predictionService.predict( p, monthStart( 2001, 6 ), monthStart( 2001, 8 ), summary );

        assertEquals( "Pred 1 Ins 2 Upd 0 Del 0 Unch 0", shortSummary( summary ) );

        assertEquals( "1.0", getDataValue( dataElementX, defaultCombo, sourceA, makeMonth( 2001, 6 ) ) );
        assertEquals( "2.0", getDataValue( dataElementX, defaultCombo, sourceA, makeMonth( 2001, 7 ) ) );

        p = createPredictor( dataElementX, defaultCombo, "PredictIf_B",
            new Expression( "SUM(if(#{" + dataElementB.getUid() + "} < 2 * #{" + dataElementA.getUid() + "},3,4))", "ExpressionIf_B" ),
            null, periodTypeMonthly, orgUnitLevel1, 1, 0, 0 );

        summary = new PredictionSummary();

        predictionService.predict( p, monthStart( 2001, 7 ), monthStart( 2001, 9 ), summary );

        assertEquals( "Pred 1 Ins 1 Upd 1 Del 0 Unch 0", shortSummary( summary ) );

        assertEquals( "3.0", getDataValue( dataElementX, defaultCombo, sourceA, makeMonth( 2001, 7 ) ) );
        assertEquals( "4.0", getDataValue( dataElementX, defaultCombo, sourceA, makeMonth( 2001, 8 ) ) );

        p = createPredictor( dataElementX, defaultCombo, "PredictIf_C",
            new Expression( "IF(SUM(#{" + dataElementB.getUid() + "}) != SUM(2 * #{" + dataElementA.getUid() + "}),5,6)", "ExpressionIf_C" ),
            null, periodTypeMonthly, orgUnitLevel1, 1, 0, 0 );

        summary = new PredictionSummary();

        predictionService.predict( p, monthStart( 2001, 7 ), monthStart( 2001, 9 ), summary );

        assertEquals( "Pred 1 Ins 0 Upd 2 Del 0 Unch 0", shortSummary( summary ) );

        assertEquals( "5.0", getDataValue( dataElementX, defaultCombo, sourceA, makeMonth( 2001, 7 ) ) );
        assertEquals( "6.0", getDataValue( dataElementX, defaultCombo, sourceA, makeMonth( 2001, 8 ) ) );
    }

    @Test
    public void testPredictIsNull()
    {
        useDataValue( dataElementA, makeMonth( 2001, 6 ), sourceA, 1 );
        useDataValue( dataElementB, makeMonth( 2001, 6 ), sourceA, 2 );

        useDataValue( dataElementA, makeMonth( 2001, 7 ), sourceA, 3 );

        dataValueBatchHandler.flush();

        Predictor p = createPredictor( dataElementX, defaultCombo, "PredictIsNull",
            new Expression( "#{" + dataElementA.getUid() + "} + If(IsNull(#{" + dataElementB.getUid() + "}),5,#{" + dataElementB.getUid() + "})", "ExpressionIsNull" ),
            null, periodTypeMonthly, orgUnitLevel1, 1, 0, 0 );

        predictionService.predict( p, monthStart( 2001, 6 ), monthStart( 2001, 8 ), summary );

        assertEquals( "Pred 1 Ins 2 Upd 0 Del 0 Unch 0", shortSummary( summary ) );

        assertEquals( "3.0", getDataValue( dataElementX, defaultCombo, sourceA, makeMonth( 2001, 6 ) ) );
        assertEquals( "8.0", getDataValue( dataElementX, defaultCombo, sourceA, makeMonth( 2001, 7 ) ) );
    }

    @Test
    public void testPredictStrategyNeverSkip()
    {
        useDataValue( dataElementA, makeMonth( 2001, 6 ), sourceA, 1 );
        useDataValue( dataElementB, makeMonth( 2001, 6 ), sourceA, 2 );

        useDataValue( dataElementA, makeMonth( 2001, 7 ), sourceA, 4 );

        dataValueBatchHandler.flush();

        Expression expressionX = new Expression( "10 + #{" + dataElementA.getUid() + "} + #{" + dataElementB.getUid() + "}", "descriptionY", MissingValueStrategy.NEVER_SKIP );
        Expression expressionY = new Expression( "10 + SUM( #{" + dataElementA.getUid() + "} + #{" + dataElementB.getUid() + "} )", "descriptionX", MissingValueStrategy.NEVER_SKIP );

        expressionService.addExpression( expressionX );
        expressionService.addExpression( expressionY );

        Predictor predictorX = createPredictor( dataElementX, defaultCombo, "PredictNeverSkipX",
            expressionX, null, periodTypeMonthly, orgUnitLevel1, 0, 0, 0 );

        Predictor predictorY = createPredictor( dataElementY, defaultCombo, "PredictNeverSkipY",
            expressionY, null, periodTypeMonthly, orgUnitLevel1, 1, 0, 0 );

        predictionService.predict( predictorX, monthStart( 2001, 6 ), monthStart( 2001, 9 ), summary );

        assertEquals( "Pred 1 Ins 9 Upd 0 Del 0 Unch 0", shortSummary( summary ) );

        assertEquals( "13.0", getDataValue( dataElementX, defaultCombo, sourceA, makeMonth( 2001, 6 ) ) );
        assertEquals( "14.0", getDataValue( dataElementX, defaultCombo, sourceA, makeMonth( 2001, 7 ) ) );
        assertEquals( "10.0", getDataValue( dataElementX, defaultCombo, sourceA, makeMonth( 2001, 8 ) ) );

        assertEquals( "10.0", getDataValue( dataElementX, defaultCombo, sourceB, makeMonth( 2001, 6 ) ) );
        assertEquals( "10.0", getDataValue( dataElementX, defaultCombo, sourceB, makeMonth( 2001, 7 ) ) );
        assertEquals( "10.0", getDataValue( dataElementX, defaultCombo, sourceB, makeMonth( 2001, 8 ) ) );

        assertEquals( "10.0", getDataValue( dataElementX, defaultCombo, sourceG, makeMonth( 2001, 6 ) ) );
        assertEquals( "10.0", getDataValue( dataElementX, defaultCombo, sourceG, makeMonth( 2001, 7 ) ) );
        assertEquals( "10.0", getDataValue( dataElementX, defaultCombo, sourceG, makeMonth( 2001, 8 ) ) );

        summary = new PredictionSummary();

        predictionService.predict( predictorY, monthStart( 2001, 7 ), monthStart( 2001, 10 ), summary );

        assertEquals( "Pred 1 Ins 2 Upd 0 Del 0 Unch 0", shortSummary( summary ) );

        assertEquals( "13", getDataValue( dataElementY, defaultCombo, sourceA, makeMonth( 2001, 7 ) ) );
        assertEquals( "14", getDataValue( dataElementY, defaultCombo, sourceA, makeMonth( 2001, 8 ) ) );
    }

    @Test
    public void testPredictStrategySkipIfAllValuesMissing()
    {
        useDataValue( dataElementA, makeMonth( 2001, 6 ), sourceG, 1 );
        useDataValue( dataElementB, makeMonth( 2001, 6 ), sourceG, 2 );

        useDataValue( dataElementA, makeMonth( 2001, 7 ), sourceG, 4 );

        dataValueBatchHandler.flush();

        Expression expressionX = new Expression( "10 + #{" + dataElementA.getUid() + "} + #{" + dataElementB.getUid() + "}", "descriptionY", MissingValueStrategy.SKIP_IF_ALL_VALUES_MISSING );
        Expression expressionY = new Expression( "10 + SUM( #{" + dataElementA.getUid() + "} + #{" + dataElementB.getUid() + "} )", "descriptionX", MissingValueStrategy.SKIP_IF_ALL_VALUES_MISSING );

        expressionService.addExpression( expressionX );
        expressionService.addExpression( expressionY );

        Predictor predictorX = createPredictor( dataElementX, defaultCombo, "PredictNeverSkipX",
            expressionX, null, periodTypeMonthly, orgUnitLevel1, 0, 0, 0 );

        Predictor predictorY = createPredictor( dataElementY, defaultCombo, "PredictNeverSkipY",
            expressionY, null, periodTypeMonthly, orgUnitLevel1, 1, 0, 0 );

        predictionService.predict( predictorX, monthStart( 2001, 6 ), monthStart( 2001, 9 ), summary );

        assertEquals( "Pred 1 Ins 2 Upd 0 Del 0 Unch 0", shortSummary( summary ) );

        assertEquals( "13.0", getDataValue( dataElementX, defaultCombo, sourceG, makeMonth( 2001, 6 ) ) );
        assertEquals( "14.0", getDataValue( dataElementX, defaultCombo, sourceG, makeMonth( 2001, 7 ) ) );

        summary = new PredictionSummary();

        predictionService.predict( predictorY, monthStart( 2001, 7 ), monthStart( 2001, 10 ), summary );

        assertEquals( "Pred 1 Ins 2 Upd 0 Del 0 Unch 0", shortSummary( summary ) );

        assertEquals( "13", getDataValue( dataElementY, defaultCombo, sourceG, makeMonth( 2001, 7 ) ) );
        assertEquals( "14", getDataValue( dataElementY, defaultCombo, sourceG, makeMonth( 2001, 8 ) ) );
    }

    @Test
    public void testPredictStrategySkipIfAnyValueMissing()
    {
        useDataValue( dataElementA, makeMonth( 2001, 6 ), sourceG, 1 );
        useDataValue( dataElementB, makeMonth( 2001, 6 ), sourceG, 2 );

        useDataValue( dataElementA, makeMonth( 2001, 7 ), sourceG, 4 );

        dataValueBatchHandler.flush();

        Expression expressionX = new Expression( "10 + #{" + dataElementA.getUid() + "} + #{" + dataElementB.getUid() + "}", "descriptionY", MissingValueStrategy.SKIP_IF_ANY_VALUE_MISSING );
        Expression expressionY = new Expression( "10 + SUM( #{" + dataElementA.getUid() + "} + #{" + dataElementB.getUid() + "} )", "descriptionX", MissingValueStrategy.SKIP_IF_ANY_VALUE_MISSING );

        expressionService.addExpression( expressionX );
        expressionService.addExpression( expressionY );

        Predictor predictorX = createPredictor( dataElementX, defaultCombo, "PredictSkipIfAnyValueMissingX",
            expressionX, null, periodTypeMonthly, orgUnitLevel1, 0, 0, 0 );

        Predictor predictorY = createPredictor( dataElementY, defaultCombo, "PredictSkipIfAnyValueMissingY",
            expressionY, null, periodTypeMonthly, orgUnitLevel1, 1, 0, 0 );

        predictionService.predict( predictorX, monthStart( 2001, 6 ), monthStart( 2001, 9 ), summary );

        assertEquals( "Pred 1 Ins 1 Upd 0 Del 0 Unch 0", shortSummary( summary ) );

        assertEquals( "13.0", getDataValue( dataElementX, defaultCombo, sourceG, makeMonth( 2001, 6 ) ) );

        summary = new PredictionSummary();

        predictionService.predict( predictorY, monthStart( 2001, 7 ), monthStart( 2001, 10 ), summary );

        assertEquals( "Pred 1 Ins 1 Upd 0 Del 0 Unch 0", shortSummary( summary ) );

        assertEquals( "13", getDataValue( dataElementY, defaultCombo, sourceG, makeMonth( 2001, 7 ) ) );
    }

    @Test
    public void testPredictTaskPredictors()
    {
        useDataValue( dataElementA, makeMonth( 2001, 6 ), sourceA, 10 );
        useDataValue( dataElementB, makeMonth( 2001, 6 ), sourceA, 20 );

        dataValueBatchHandler.flush();

        Predictor predictorA = createPredictor( dataElementX, defaultCombo, "A", expressionA, null,
            periodTypeMonthly, orgUnitLevel1, 1, 0, 0 );

        Predictor predictorB = createPredictor( dataElementY, defaultCombo, "B", expressionB, null,
            periodTypeMonthly, orgUnitLevel1, 1, 0, 0 );

        predictorService.addPredictor( predictorA );
        predictorService.addPredictor( predictorB );

        List<String> predictors = Lists.newArrayList( predictorA.getUid() );

        summary = predictionService.predictTask( monthStart( 2001, 7 ), monthStart( 2001, 8 ), predictors, null, null );

        assertEquals( "Pred 1 Ins 1 Upd 0 Del 0 Unch 0", shortSummary( summary ) );

        assertEquals( "10.0", getDataValue( dataElementX, defaultCombo, sourceA, makeMonth( 2001, 7 ) ) );

        predictors = Lists.newArrayList( predictorA.getUid(), predictorB.getUid() );

        summary = predictionService.predictTask( monthStart( 2001, 7 ), monthStart( 2001, 8 ), predictors, null, null );

        assertEquals( "Pred 2 Ins 1 Upd 0 Del 0 Unch 1", shortSummary( summary ) );

        assertEquals( "20", getDataValue( dataElementY, defaultCombo, sourceA, makeMonth( 2001, 7 ) ) );

        summary = predictionService.predictTask( monthStart( 2001, 7 ), monthStart( 2001, 8 ), predictors, null, null );

        assertEquals( "Pred 2 Ins 0 Upd 0 Del 0 Unch 2", shortSummary( summary ) );
    }

    @Test
    public void testPredictTaskPredictorGroups()
    {
        useDataValue( dataElementA, makeMonth( 2001, 6 ), sourceA, 10 );
        useDataValue( dataElementB, makeMonth( 2001, 6 ), sourceA, 20 );

        dataValueBatchHandler.flush();

        Predictor predictorA = createPredictor( dataElementX, defaultCombo, "A", expressionA, null,
            periodTypeMonthly, orgUnitLevel1, 1, 0, 0 );

        Predictor predictorB = createPredictor( dataElementY, defaultCombo, "B", expressionB, null,
            periodTypeMonthly, orgUnitLevel1, 1, 0, 0 );

        predictorService.addPredictor( predictorA );
        predictorService.addPredictor( predictorB );

        PredictorGroup predictorGroupA = createPredictorGroup( 'A' );

        predictorGroupA.addPredictor( predictorA );

        predictorService.addPredictorGroup( predictorGroupA );

        List<String> predictorGroups = Lists.newArrayList( predictorGroupA.getUid() );

        summary = predictionService.predictTask( monthStart( 2001, 7 ), monthStart( 2001, 8 ), null, predictorGroups, null );

        assertEquals( "Pred 1 Ins 1 Upd 0 Del 0 Unch 0", shortSummary( summary ) );

        assertEquals( "10.0", getDataValue( dataElementX, defaultCombo, sourceA, makeMonth( 2001, 7 ) ) );

        predictorGroupA.addPredictor( predictorB );

        predictorService.updatePredictorGroup( predictorGroupA );

        summary = predictionService.predictTask( monthStart( 2001, 7 ), monthStart( 2001, 8 ), null, predictorGroups, null );

        assertEquals( "Pred 2 Ins 1 Upd 0 Del 0 Unch 1", shortSummary( summary ) );

        assertEquals( "20", getDataValue( dataElementY, defaultCombo, sourceA, makeMonth( 2001, 7 ) ) );
    }

    @Test
    public void testPredictMedian()
    {
        useDataValue( dataElementA, makeMonth( 2001, 1 ), sourceA, 50 );
        useDataValue( dataElementA, makeMonth( 2001, 2 ), sourceA, 10 );
        useDataValue( dataElementA, makeMonth( 2001, 3 ), sourceA, 40 );
        useDataValue( dataElementA, makeMonth( 2001, 4 ), sourceA, 30 );
        useDataValue( dataElementA, makeMonth( 2001, 5 ), sourceA, 20 );

        dataValueBatchHandler.flush();

        Expression expressionM = new Expression("median(#{" + dataElementA.getUid() + "})", "median" );
        expressionService.addExpression( expressionM );

        Predictor predictorM = createPredictor( dataElementY, defaultCombo, "M", expressionM, null,
            periodTypeMonthly, orgUnitLevel1, 5, 0, 0 );

        predictorService.addPredictor( predictorM );

        predictionService.predict( predictorM, monthStart( 2001, 6 ), monthStart( 2001, 11 ), summary );

        assertEquals( "Pred 1 Ins 5 Upd 0 Del 0 Unch 0", shortSummary( summary ) );

        assertEquals( "30", getDataValue( dataElementY, defaultCombo, sourceA, makeMonth( 2001, 6 ) ) ); // Values 10, 20, 30, 40, 50
        assertEquals( "25", getDataValue( dataElementY, defaultCombo, sourceA, makeMonth( 2001, 7 ) ) ); // Values 10, 20, 30, 40
        assertEquals( "30", getDataValue( dataElementY, defaultCombo, sourceA, makeMonth( 2001, 8 ) ) ); // Values 20, 30, 40
        assertEquals( "25", getDataValue( dataElementY, defaultCombo, sourceA, makeMonth( 2001, 9 ) ) ); // Values 20, 30
        assertEquals( "20", getDataValue( dataElementY, defaultCombo, sourceA, makeMonth( 2001, 10 ) ) ); // Value 20
    }
}
