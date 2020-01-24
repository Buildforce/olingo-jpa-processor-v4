package com.sap.olingo.jpa.processor.core.testmodel;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;

@Embeddable
public class CollectionNestedComplex {

  @Column(name = "\"Number\"")
  private Long number;

  @Embedded
  private CollectionInnerComplex inner;

  public Long getNumber() {
    return number;
  }

  public void setNumber(Long number) {
    this.number = number;
  }

  public CollectionInnerComplex getInner() {
    return inner;
  }

  public void setInner(CollectionInnerComplex inner) {
    this.inner = inner;
  }
}