package org.apache.olingo.jpa.processor.core.query;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.From;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Path;

import org.apache.olingo.commons.api.edm.EdmPrimitiveType;
import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeException;
import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeKind;
import org.apache.olingo.commons.api.edm.provider.CsdlProperty;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAttribute;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPADescriptionAttribute;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAElement;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAEntityType;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAFunctionParamaterFacet;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAPath;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;
import org.apache.olingo.jpa.metadata.core.edm.mapper.impl.JPATypeConvertor;
import org.apache.olingo.jpa.processor.core.exception.ODataJPAFilterException;
import org.apache.olingo.server.api.OData;
import org.apache.olingo.server.api.uri.UriParameter;

public class ExpressionUtil {
  public static final int CONTAINY_ONLY_LANGU = 1;
  public static final int CONTAINS_LANGU_COUNTRY = 2;
  public static final String SELECT_ITEM_SEPERATOR = ",";

  public static Expression<Boolean> createEQExpression(final OData odata, CriteriaBuilder cb, From<?, ?> root,
      JPAEntityType jpaEntity, UriParameter keyPredicate) throws ODataJPAFilterException, ODataJPAModelException {

    JPAPath path = jpaEntity.getPath(keyPredicate.getName());
    JPAAttribute attribute = path.getLeaf();

    return cb.equal(convertToCriteriaPath(root, path), convertValueOnAttribute(odata, attribute, keyPredicate
        .getText()));

  }

  /**
   * Converts the jpaPath into a Criteria Path.
   * @param joinTables
   * @param root
   * @param jpaPath
   * @return
   */
  public static Path<?> convertToCriteriaPath(final Map<String, From<?, ?>> joinTables, From<?, ?> root,
      final JPAPath jpaPath) {
    Path<?> p = root;
    for (final JPAElement jpaPathElement : jpaPath.getPath())
      if (jpaPathElement instanceof JPADescriptionAttribute) {
        final Join<?, ?> join = (Join<?, ?>) joinTables.get(jpaPathElement.getInternalName());
        p = join.get(((JPADescriptionAttribute) jpaPathElement).getDescriptionAttribute().getInternalName());
      } else
        p = p.get(jpaPathElement.getInternalName());
    return p;
  }

  public static Path<?> convertToCriteriaPath(From<?, ?> root, final JPAPath jpaPath) {
    Path<?> p = root;
    for (final JPAElement jpaPathElement : jpaPath.getPath())
      p = p.get(jpaPathElement.getInternalName());
    return p;
  }

  public static Object convertValueOnAttribute(final OData odata, final JPAAttribute attribute, final String value)
      throws ODataJPAFilterException {
    return convertValueOnAttribute(odata, attribute, value, true);
  }

  public static Object convertValueOnAttribute(final OData odata, final JPAAttribute attribute, final String value,
      final Boolean isUri) throws ODataJPAFilterException {

    try {
      final CsdlProperty edmProperty = (CsdlProperty) attribute.getProperty();
      final EdmPrimitiveTypeKind edmTypeKind = JPATypeConvertor.convertToEdmSimpleType(attribute);

      // TODO literal does not convert decimals without scale properly
      String targetValue = null;
      final EdmPrimitiveType edmType = odata.createPrimitiveTypeInstance(edmTypeKind);
      if (isUri) {
        targetValue = edmType.fromUriLiteral(value);
      } else {
        targetValue = value;
      }
      return edmType.valueOfString(targetValue, edmProperty.isNullable(), edmProperty.getMaxLength(),
          edmProperty.getPrecision(), edmProperty.getScale(), true, attribute.getType());

    } catch (EdmPrimitiveTypeException e) {
      throw new ODataJPAFilterException(e, HttpStatusCode.INTERNAL_SERVER_ERROR);
    } catch (ODataJPAModelException e) {
      throw new ODataJPAFilterException(e, HttpStatusCode.INTERNAL_SERVER_ERROR);
    }
  }

  public static Object convertValueOnFacet(final OData odata, JPAFunctionParamaterFacet returnType, final String value)
      throws ODataJPAFilterException {
    try {
      final EdmPrimitiveTypeKind edmTypeKind = EdmPrimitiveTypeKind.valueOfFQN(returnType.getTypeFQN());
      final EdmPrimitiveType edmType = odata.createPrimitiveTypeInstance(edmTypeKind);
      String targetValue;

      targetValue = edmType.fromUriLiteral(value);
      return edmType.valueOfString(targetValue, true, returnType.getMaxLength(), returnType.getPrecision(), returnType
          .getScale(), true, returnType.getType());
    } catch (EdmPrimitiveTypeException e) {
      throw new ODataJPAFilterException(e, HttpStatusCode.INTERNAL_SERVER_ERROR);
    } catch (ODataJPAModelException e) {
      throw new ODataJPAFilterException(e, HttpStatusCode.INTERNAL_SERVER_ERROR);
    }
  }

  public static Locale determineLocale(final Map<String, List<String>> headers) {
    // TODO Make this replaceable so the default can be overwritten
    // http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html (14.4 accept language header
    // example: Accept-Language: da, en-gb;q=0.8, en;q=0.7)
    final List<String> languageHeaders = headers.get("accept-language");
    if (languageHeaders != null) {
      final String languageHeader = languageHeaders.get(0);
      if (languageHeader != null) {
        final String[] localeList = languageHeader.split(SELECT_ITEM_SEPERATOR);
        final String locale = localeList[0];
        final String[] languCountry = locale.split("-");
        if (languCountry.length == CONTAINS_LANGU_COUNTRY)
          return new Locale(languCountry[0], languCountry[1]);
        else if (languCountry.length == CONTAINY_ONLY_LANGU)
          return new Locale(languCountry[0]);
        else
          return Locale.ENGLISH;
      }
    }
    return Locale.ENGLISH;
  }
}