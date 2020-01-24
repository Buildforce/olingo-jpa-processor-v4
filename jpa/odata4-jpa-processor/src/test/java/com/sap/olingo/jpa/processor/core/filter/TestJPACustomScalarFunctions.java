package com.sap.olingo.jpa.processor.core.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Query;
import javax.sql.DataSource;

import org.apache.olingo.commons.api.ex.ODataException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.sap.olingo.jpa.metadata.api.JPAEntityManagerFactory;
import com.sap.olingo.jpa.metadata.core.edm.mapper.impl.JPADefaultEdmNameBuilder;
import com.sap.olingo.jpa.processor.core.testmodel.DataSourceHelper;
import com.sap.olingo.jpa.processor.core.util.IntegrationTestHelper;
import com.sap.olingo.jpa.processor.core.util.TestHelper;

public class TestJPACustomScalarFunctions {

  protected static final String PUNIT_NAME = "com.sap.olingo.jpa";
  protected static EntityManagerFactory emf;
  protected TestHelper helper;
  protected Map<String, List<String>> headers;
  protected static JPADefaultEdmNameBuilder nameBuilder;
  protected static DataSource ds;

  @BeforeAll
  public static void setupClass() {
    ds = DataSourceHelper.createDataSource(DataSourceHelper.DB_HSQLDB);
    emf = JPAEntityManagerFactory.getEntityManagerFactory(PUNIT_NAME, ds);
    nameBuilder = new JPADefaultEdmNameBuilder(PUNIT_NAME);
    CreateDenfityFunction();
  }

  @AfterAll
  public static void tearDownClass() {
    DropDenfityFunction();
  }

  @Test
  public void testFilterOnFunction() throws IOException, ODataException {

    IntegrationTestHelper helper = new IntegrationTestHelper(emf,
        "AdministrativeDivisions?$filter=com.sap.olingo.jpa.PopulationDensity(Area=$it/Area,Population=$it/Population) gt 1");
    helper.assertStatus(200);
  }

  @Test
  public void testFilterOnFunctionAndProperty() throws IOException, ODataException {

    IntegrationTestHelper helper = new IntegrationTestHelper(emf,
        "AdministrativeDivisions?$filter=com.sap.olingo.jpa.PopulationDensity(Area=$it/Area,Population=$it/Population)  mul 1000000 gt 1000 and ParentDivisionCode eq 'BE255'&orderBy=DivisionCode)");
    helper.assertStatus(200);

    ArrayNode orgs = helper.getValues();
    assertEquals(2, orgs.size());
    assertEquals("35002", orgs.get(0).get("DivisionCode").asText());
  }

  @Test
  public void testFilterOnFunctionAndMultiply() throws IOException, ODataException {

    IntegrationTestHelper helper = new IntegrationTestHelper(emf,
        "AdministrativeDivisions?$filter=com.sap.olingo.jpa.PopulationDensity(Area=Area,Population=Population)  mul 1000000 gt 100");
    helper.assertStatus(200);

    ArrayNode orgs = helper.getValues();
    assertEquals(59, orgs.size());
  }

  @Test
  public void testFilterOnFunctionWithFixedValue() throws IOException, ODataException {

    IntegrationTestHelper helper = new IntegrationTestHelper(emf,
        "AdministrativeDivisions?$filter=com.sap.olingo.jpa.PopulationDensity(Area=13079087,Population=$it/Population)  mul 1000000 gt 1000");
    helper.assertStatus(200);

    ArrayNode orgs = helper.getValues();
    assertEquals(29, orgs.size());
  }

  @Test
  public void testFilterOnFunctionCommuteValue() throws IOException, ODataException {

    IntegrationTestHelper helper = new IntegrationTestHelper(emf,
        "AdministrativeDivisions?$filter=com.sap.olingo.jpa.PopulationDensity(Area=Area div 1000000,Population=Population) gt 1000");
    helper.assertStatus(200);

    ArrayNode orgs = helper.getValues();
    assertEquals(7, orgs.size());
  }

  @Test
  public void testFilterOnFunctionMixParamOrder() throws IOException, ODataException {

    IntegrationTestHelper helper = new IntegrationTestHelper(emf,
        "AdministrativeDivisions?$filter=com.sap.olingo.jpa.PopulationDensity(Population=Population,Area=Area) mul 1000000 gt 1000");
    helper.assertStatus(200);

    ArrayNode orgs = helper.getValues();
    assertEquals(7, orgs.size());
  }

  private static void CreateDenfityFunction() {
    EntityManager em = emf.createEntityManager();
    EntityTransaction t = em.getTransaction();

    t.begin();
    String sqlString = "CREATE FUNCTION  \"OLINGO\".\"PopulationDensity\" (UnitArea  INT, Population BIGINT ) " +
            "RETURNS DOUBLE " +
            "BEGIN ATOMIC  " + //
            "  DECLARE aDouble DOUBLE; " + //
            "  DECLARE pDouble DOUBLE; " +
            "  SET aDouble = UnitArea; " +
            "  SET pDouble = Population; " +
            "  IF UnitArea <= 0 THEN RETURN 0; " +
            "  ELSE RETURN pDouble  / aDouble; " + // * 1000000
            "  END IF;  " + //
            "END";
    Query q = em.createNativeQuery(sqlString);
    q.executeUpdate();
    t.commit();
  }

  private static void DropDenfityFunction() {
    EntityManager em = emf.createEntityManager();
    EntityTransaction t = em.getTransaction();

    t.begin();
    Query q = em.createNativeQuery("DROP FUNCTION  \"OLINGO\".\"PopulationDensity\"");
    q.executeUpdate();
    t.commit();
  }
}