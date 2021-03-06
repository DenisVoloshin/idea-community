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
package org.jetbrains.idea.svn.actions;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.changes.ChangeListManager;
import com.intellij.openapi.vcs.impl.ExcludedFileIndex;
import com.intellij.openapi.vfs.VirtualFile;
import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNException;

public class SvnExcludingIgnoredOperation {
  private final Operation myImportAction;
  private final SVNDepth myDepth;
  private final Filter myFilter;

  public SvnExcludingIgnoredOperation(final Project project, final Operation importAction, final SVNDepth depth) {
    myImportAction = importAction;
    myDepth = depth;

    myFilter = new Filter(project);
  }

  public static class Filter {
    private final Project myProject;
    private final ExcludedFileIndex myIndex;
    private final ChangeListManager myClManager;

    public Filter(final Project project) {
      myProject = project;

      if (! project.isDefault()) {
        myIndex = ExcludedFileIndex.getInstance(project);
        myClManager = ChangeListManager.getInstance(project);
      } else {
        myIndex = null;
        myClManager = null;
      }
    }

    public boolean accept(final VirtualFile file) {
      if (! myProject.isDefault()) {
        if (myIndex.isExcludedFile(file)) {
          return false;
        }
        if (myClManager.isIgnoredFile(file)) {
          return false;
        }
      }
      return true;
    }
  }

  private boolean operation(final VirtualFile file) throws SVNException {
    if (! myFilter.accept(file)) return false;

    myImportAction.doOperation(file);
    return true;
  }

  private void executeDown(final VirtualFile file) throws SVNException {
    if (! operation(file)) {
      return;
    }

    for (VirtualFile child : file.getChildren()) {
      executeDown(child);
    }
  }

  public void execute(final VirtualFile file) throws SVNException {
    if (SVNDepth.INFINITY.equals(myDepth)) {
      executeDown(file);
      return;
    }

    if (! operation(file)) {
      return;
    }

    if (SVNDepth.EMPTY.equals(myDepth)) {
      return;
    }

    for (VirtualFile child : file.getChildren()) {
      if (SVNDepth.FILES.equals(myDepth) && child.isDirectory()) {
        continue;
      }
      operation(child);
    }
  }

  public interface Operation {
    void doOperation(final VirtualFile file) throws SVNException;
  }
}
