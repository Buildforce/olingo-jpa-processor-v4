package com.sap.olingo.jpa.processor.core.api;

import com.sap.olingo.jpa.metadata.api.JPAEdmProvider;
import com.sap.olingo.jpa.metadata.api.JPAEntityManagerFactory;
import com.sap.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAException;
import com.sap.olingo.jpa.processor.core.exception.ODataJPAFilterException;
import com.sap.olingo.jpa.processor.core.processor.JPAODataRequestContextImpl;
import org.apache.olingo.commons.api.ex.ODataException;
import org.apache.olingo.server.api.OData;
import org.apache.olingo.server.api.ODataHttpHandler;
import org.apache.olingo.server.api.debug.DebugSupport;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.metamodel.Metamodel;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import java.util.Optional;

// import org.apache.olingo.server.core.ODataHttpHandlerImpl.REQUESTMAPPING

public class JPAODataGetHandler {
  public static final String REQUESTMAPPING = "requestMapping";
  public final Optional<EntityManagerFactory> emf;
  private final JPAODataServiceContext serviceContext;
  private final JPAODataRequestContextImpl requestContext;
  final OData odata;
  @Deprecated
  final DataSource ds;
  @Deprecated
  final String namespace;
  @Deprecated
  final Metamodel jpaMetamodel;

  /**
   * @deprecated (Will be removed with 1.0.0, use service context builder, <code>JPAODataServiceContext.with()</code>
   * instead
   * @param pUnit
   * @throws ODataJPAException
   */
  @Deprecated
  public JPAODataGetHandler(final String pUnit) throws ODataJPAFilterException {// NOSONAR
    this(pUnit, null);
  }

  /**
   * @deprecated (Will be removed with 1.0.0, use service context builder, <code>JPAODataServiceContext.with()</code>
   * instead
   * @param pUnit
   * @param ds
   * @throws ODataJPAFilterException
   */
  @Deprecated
  public JPAODataGetHandler(final String pUnit, final DataSource ds) throws ODataJPAFilterException {
    namespace = pUnit;
    this.ds = ds;
    emf = ds == null ? Optional.empty() : Optional.ofNullable(JPAEntityManagerFactory.getEntityManagerFactory(pUnit, ds));
    jpaMetamodel = emf.map(EntityManagerFactory::getMetamodel).orElse(null);
    serviceContext = new JPAODataServiceContext(this);
    requestContext = new JPAODataRequestContextImpl();
    odata = OData.newInstance();
  }

  public JPAODataGetHandler(final JPAODataCRUDContextAccess serviceContext) {
    this(serviceContext, OData.newInstance());
  }

  /**
   * Give the option to inject the odata helper e.g. for testing
   * @param serviceContext
   * @param odata
   */
  JPAODataGetHandler(final JPAODataCRUDContextAccess serviceContext, final OData odata) {
    namespace = null;
    ds = null;
    emf = serviceContext.getEntityManagerFactory();
    jpaMetamodel = null;
    this.serviceContext = (JPAODataServiceContext) serviceContext;
    requestContext = new JPAODataRequestContextImpl();
    this.odata = odata;
  }

  public JPAODataGetContext getJPAODataContext() {
    return serviceContext;
  }

  public JPAODataRequestContext getJPAODataRequestContext() {
    return requestContext;
  }

  public void process(final HttpServletRequest request, final HttpServletResponse response) {
    try {
      if (emf.isPresent() && requestContext.getEntityManager() == null) {
        final EntityManager em = emf.get().createEntityManager();
        try {
          requestContext.setEntityManager(em);
          processInternal(request, response);
        } finally {
          em.close();
        }
      } else {
        processInternal(request, response);
      }
    } catch (RuntimeException | ODataException e ) {
      throw new RuntimeException( "ProcessInternal failed", e);
    }
  }

  /**
   * @deprecated (Will be removed with 1.0.0, parameter <code>em</code> not longer supported,
   * use Request Context (<code>getJPAODataRequestContext</code>) instead)
   * @param request
   * @param response
   * @param em
   */
  @Deprecated
  public void process(final HttpServletRequest request, final HttpServletResponse response, final EntityManager em) {
    requestContext.setEntityManager(em);
    process(request, response);
  }

  /**
   * @deprecated (Will be removed with 1.0.0, parameter <code>claims</code> and <code>em</code> not longer supported,
   * use Request Context (<code>getJPAODataRequestContext</code>) instead)
   * @param request
   * @param response
   * @param claims
   * @param em
   */
  @Deprecated
  public void process(final HttpServletRequest request, final HttpServletResponse response,
      final JPAODataClaimProvider claims, final EntityManager em) {
    requestContext.setClaimsProvider(claims);
    requestContext.setEntityManager(em);
    process(request, response);
  }

  private void processInternal(final HttpServletRequest request, final HttpServletResponse response)
      throws ODataJPAException {

    final JPAEdmProvider jpaEdm = serviceContext.getEdmProvider() == null
            ? serviceContext.getEdmProvider(requestContext.getEntityManager())
            : serviceContext.getEdmProvider();

    final ODataHttpHandler handler = odata.createHandler(odata.createServiceMetadata(jpaEdm, jpaEdm.getReferences()));
    serviceContext.getEdmProvider().setRequestLocales(request.getLocales());
    requestContext.setDebugFormat(request.getParameter(DebugSupport.ODATA_DEBUG_QUERY_PARAMETER));
    setCUDHandler();
    final HttpServletRequest mappedRequest = prepareRequestMapping(request, serviceContext.getMappingPath());
    handler.register(requestContext.getDebugSupport());
    handler.register(new JPAODataRequestProcessor(serviceContext, requestContext));
    handler.register(new JPAODataBatchProcessor(requestContext));
    handler.register(serviceContext.getEdmProvider().getServiceDocument());
    handler.register(serviceContext.getErrorProcessor());
    handler.process(mappedRequest, response);
  }

  private void setCUDHandler() {
    if (serviceContext.getCUDRequestHandler() != null && requestContext.getCUDRequestHandler() == null)
      requestContext.setCUDRequestHandler(serviceContext.getCUDRequestHandler());
  }

  private HttpServletRequest prepareRequestMapping(final HttpServletRequest req, final String requestPath) {
    if (requestPath != null && !requestPath.isEmpty()) {
      HttpServletRequestWrapper request = new HttpServletRequestWrapper(req);
      request.setAttribute(REQUESTMAPPING, requestPath);
      return request;
    } else {
      return req;
    }
  }
}