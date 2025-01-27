package com.sap.olingo.jpa.processor.core.filter;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.criteria.Subquery;

import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.uri.UriInfoResource;
import org.apache.olingo.server.api.uri.UriResource;
import org.apache.olingo.server.api.uri.UriResourceKind;
import org.apache.olingo.server.api.uri.UriResourceLambdaAll;
import org.apache.olingo.server.api.uri.UriResourceLambdaAny;
import org.apache.olingo.server.api.uri.UriResourceProperty;
import org.apache.olingo.server.api.uri.queryoption.expression.Expression;
import org.apache.olingo.server.api.uri.queryoption.expression.Member;

import com.sap.olingo.jpa.processor.core.query.JPAAbstractQuery;
import com.sap.olingo.jpa.processor.core.query.JPACollectionFilterQuery;
import com.sap.olingo.jpa.processor.core.query.JPANavigationFilterQuery;
import com.sap.olingo.jpa.processor.core.query.JPANavigationProptertyInfo;
import com.sap.olingo.jpa.processor.core.query.JPANavigationQuery;

abstract class JPALambdaOperation extends JPAExistsOperation {

  protected final UriInfoResource member;

  JPALambdaOperation(final JPAFilterComplierAccess jpaComplier, final UriInfoResource member) {
    super(jpaComplier);
    this.member = member;
  }

  public JPALambdaOperation(final JPAFilterComplierAccess jpaComplier, final Member member) {
    super(jpaComplier);
    this.member = member.getResourcePath();
  }

  @Override
  protected Subquery<?> getExistsQuery() throws ODataApplicationException {
    return getSubQuery(determineExpression());
  }

  protected final Subquery<?> getSubQuery(final Expression expression) throws ODataApplicationException {
    final List<UriResource> allUriResourceParts = new ArrayList<>(uriResourceParts);
    allUriResourceParts.addAll(member.getUriResourceParts());

    // 1. Determine all relevant associations
    final List<JPANavigationProptertyInfo> naviPathList = determineAssoziations(sd, allUriResourceParts);
    JPAAbstractQuery parent = root;
    final List<JPANavigationQuery> queryList = new ArrayList<>();

    // 2. Create the queries and roots
    for (int i = naviPathList.size() - 1; i >= 0; i--) {
      final JPANavigationProptertyInfo naviInfo = naviPathList.get(i);
      if (i == 0)
        if (naviInfo.getUriResiource() instanceof UriResourceProperty)
          queryList.add(new JPACollectionFilterQuery(odata, sd, em, parent, member.getUriResourceParts(), expression,
              from));
        else
          queryList.add(new JPANavigationFilterQuery(odata, sd, naviInfo.getUriResiource(), parent, em, naviInfo
              .getAssociationPath(), expression, from, claimsProvider));
      else
        queryList.add(new JPANavigationFilterQuery(odata, sd, naviInfo.getUriResiource(), parent, em, naviInfo
            .getAssociationPath(), from, claimsProvider));
      parent = queryList.get(queryList.size() - 1);
    }
    // 3. Create select statements
    Subquery<?> childQuery = null;
    for (int i = queryList.size() - 1; i >= 0; i--) {
      childQuery = queryList.get(i).getSubQueryExists(childQuery);
    }
    return childQuery;
  }

  Expression determineExpression() {
    for (final UriResource uriResource : member.getUriResourceParts()) {
      if (uriResource.getKind() == UriResourceKind.lambdaAny)
        return ((UriResourceLambdaAny) uriResource).getExpression();
      else if (uriResource.getKind() == UriResourceKind.lambdaAll)
        return ((UriResourceLambdaAll) uriResource).getExpression();
    }
    return null;
  }
}