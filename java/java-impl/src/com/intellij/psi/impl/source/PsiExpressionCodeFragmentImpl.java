/*
 * Copyright 2000-2009 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.psi.impl.source;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiExpressionCodeFragment;
import com.intellij.psi.PsiType;
import com.intellij.psi.impl.source.tree.JavaElementType;
import org.jetbrains.annotations.NonNls;

public class PsiExpressionCodeFragmentImpl extends PsiCodeFragmentImpl implements PsiExpressionCodeFragment {
  private PsiType myExpectedType;

  public PsiExpressionCodeFragmentImpl(Project project,
                                       boolean isPhysical,
                                       @NonNls String name,
                                       CharSequence text,
                                       final PsiType expectedType) {
    super(project, JavaElementType.EXPRESSION_TEXT, isPhysical, name, text);
    myExpectedType = expectedType;
  }

  public PsiExpression getExpression() {
    ASTNode exprChild = calcTreeElement().findChildByType(Constants.EXPRESSION_BIT_SET);
    if (exprChild == null) return null;
    return (PsiExpression)SourceTreeToPsiMap.treeElementToPsi(exprChild);
  }

  public PsiType getExpectedType() {
    return myExpectedType;
  }

  public void setExpectedType(PsiType type) {
    myExpectedType = type;
  }
}
