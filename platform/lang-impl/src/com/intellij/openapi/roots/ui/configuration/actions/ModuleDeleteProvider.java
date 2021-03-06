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

package com.intellij.openapi.roots.ui.configuration.actions;

import com.intellij.ide.DeleteProvider;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.module.ModifiableModuleModel;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectBundle;
import com.intellij.openapi.roots.*;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.ArrayUtil;
import com.intellij.util.Function;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class ModuleDeleteProvider  implements DeleteProvider  {
  public boolean canDeleteElement(DataContext dataContext) {
    return LangDataKeys.MODULE_CONTEXT_ARRAY.getData(dataContext) != null;
  }

  public void deleteElement(DataContext dataContext) {
    final Module[] modules = LangDataKeys.MODULE_CONTEXT_ARRAY.getData(dataContext);
    assert modules != null;
    final Project project = PlatformDataKeys.PROJECT.getData(dataContext);
    String names = StringUtil.join(Arrays.asList(modules), new Function<Module, String>() {
      public String fun(final Module module) {
        return "\'" + module.getName() + "\'";
      }
    }, ", ");
    int ret = Messages.showOkCancelDialog(ProjectBundle.message("module.remove.confirmation.prompt", names, modules.length),
                                          ProjectBundle.message("module.remove.confirmation.title"), Messages.getQuestionIcon());
    if (ret != 0) return;
    CommandProcessor.getInstance().executeCommand(project, new Runnable() {
      public void run() {
        final Runnable action = new Runnable() {
          public void run() {
            final ModuleManager moduleManager = ModuleManager.getInstance(project);
            final Module[] currentModules = moduleManager.getModules();
            final ModifiableModuleModel modifiableModuleModel = moduleManager.getModifiableModel();
            final Map<Module, ModifiableRootModel> otherModuleRootModels = new HashMap<Module, ModifiableRootModel>();
            for (final Module module : modules) {
              final ModifiableRootModel modifiableModel = ModuleRootManager.getInstance(module).getModifiableModel();
              for (final Module otherModule : currentModules) {
                if (otherModule == module || ArrayUtil.find(modules, otherModule) != -1) continue;
                if (!otherModuleRootModels.containsKey(otherModule)) {
                  otherModuleRootModels.put(otherModule, ModuleRootManager.getInstance(otherModule).getModifiableModel());
                }
              }
              removeModule(module, modifiableModel, otherModuleRootModels.values(), modifiableModuleModel);
            }
            final ModifiableRootModel[] modifiableRootModels = otherModuleRootModels.values().toArray(new ModifiableRootModel[otherModuleRootModels.size()]);
            ProjectRootManager.getInstance(project).multiCommit(modifiableModuleModel, modifiableRootModels);
          }
        };
        ApplicationManager.getApplication().runWriteAction(action);
      }
    }, ProjectBundle.message("module.remove.command"), null);
  }

  public static void removeModule(@NotNull final Module moduleToRemove,
                                   @Nullable ModifiableRootModel modifiableRootModelToRemove,
                                   @NotNull Collection<ModifiableRootModel> otherModuleRootModels,
                                   @NotNull final ModifiableModuleModel moduleModel) {
    // remove all dependencies on the module that is about to be removed
    for (final ModifiableRootModel modifiableRootModel : otherModuleRootModels) {
      final OrderEntry[] orderEntries = modifiableRootModel.getOrderEntries();
      for (final OrderEntry orderEntry : orderEntries) {
        if (orderEntry instanceof ModuleOrderEntry && orderEntry.isValid()) {
          final Module orderEntryModule = ((ModuleOrderEntry)orderEntry).getModule();
          if (orderEntryModule != null && orderEntryModule.equals(moduleToRemove)) {
            modifiableRootModel.removeOrderEntry(orderEntry);
          }
        }
      }
    }
    // destroyProcess editor
    if (modifiableRootModelToRemove != null) {
      modifiableRootModelToRemove.dispose();
    }
    // destroyProcess module
    moduleModel.disposeModule(moduleToRemove);
  }
}
