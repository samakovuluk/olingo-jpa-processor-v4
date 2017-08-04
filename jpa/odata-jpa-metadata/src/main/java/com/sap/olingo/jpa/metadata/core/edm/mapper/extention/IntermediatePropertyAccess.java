package com.sap.olingo.jpa.metadata.core.edm.mapper.extention;

import java.util.List;

import org.apache.olingo.commons.api.edm.provider.CsdlAnnotation;

public interface IntermediatePropertyAccess extends IntermediateModelItemAccess {
  public boolean isEtag();

  /**
   * Enables to add annotations to a property, e.g. because the type of annotation is not enabled via
   * {@link com.sap.olingo.jpa.metadata.core.edm.annotation.EdmAnnotation EdmAnnotation} or should be during runtime
   * @param annotations
   */
  public void addAnnotations(final List<CsdlAnnotation> annotations);
}
