package com.sap.olingo.jpa.metadata.core.edm.mapper.testobjects;

import javax.persistence.EntityManager;

import com.sap.olingo.jpa.metadata.core.edm.annotation.EdmAction;
import com.sap.olingo.jpa.metadata.core.edm.annotation.EdmFunction;
import com.sap.olingo.jpa.metadata.core.edm.annotation.EdmFunction.ReturnType;
import com.sap.olingo.jpa.metadata.core.edm.annotation.EdmParameter;
import com.sap.olingo.jpa.metadata.core.edm.mapper.extension.ODataAction;
import com.sap.olingo.jpa.metadata.core.edm.mapper.extension.ODataFunction;

public class ExampleJavaPrivateConstructor implements ODataFunction, ODataAction {

  private ExampleJavaPrivateConstructor(EntityManager em) {
    super();
  }

  @EdmFunction(returnType = @ReturnType)
  public Integer sum(
      @EdmParameter(name = "A") short a, @EdmParameter(name = "B") int b) {
    return a + b;
  }

  @EdmAction()
  public void mul(
      @EdmParameter(name = "A") short a, @EdmParameter(name = "B") int b) {}
}