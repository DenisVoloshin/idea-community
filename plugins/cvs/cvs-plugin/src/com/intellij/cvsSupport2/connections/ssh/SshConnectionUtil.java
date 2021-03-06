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
package com.intellij.cvsSupport2.connections.ssh;

import com.intellij.CvsBundle;
import com.intellij.cvsSupport2.config.SshSettings;
import com.intellij.cvsSupport2.connections.ssh.ui.SshPasswordDialog;

public class SshConnectionUtil {
  private SshConnectionUtil() {
  }

  public static boolean promptForPassword(final SshSettings settings, final String cvsRoot) {
    if (! settings.USE_PPK) {
      SSHPasswordProviderImpl sshPasswordProvider = SSHPasswordProviderImpl.getInstance();
      String password = sshPasswordProvider.getPasswordForCvsRoot(cvsRoot);

      if (password == null) {
        SshPasswordDialog sshPasswordDialog = new SshPasswordDialog(CvsBundle.message("propmt.text.enter.password.for", cvsRoot));
        sshPasswordDialog.show();
        if (!sshPasswordDialog.isOK()) return false;
        password = sshPasswordDialog.getPassword();
        sshPasswordProvider.storePasswordForCvsRoot(cvsRoot, password, sshPasswordDialog.saveThisPassword());
      }

      if (password == null) return false;
    } else {
      SSHPasswordProviderImpl sshPasswordProvider = SSHPasswordProviderImpl.getInstance();
      String password = sshPasswordProvider.getPPKPasswordForCvsRoot(cvsRoot);

      if (password == null) {
        SshPasswordDialog sshPasswordDialog = new SshPasswordDialog(CvsBundle.message("propmt.text.enter.private.key.password.for", cvsRoot));
        sshPasswordDialog.setAdditionalText(CvsBundle.message("prompt.path.to.private.key", settings.PATH_TO_PPK));
        sshPasswordDialog.show();
        if (!sshPasswordDialog.isOK()) return false;
        password = sshPasswordDialog.getPassword();
        sshPasswordProvider.storePPKPasswordForCvsRoot(cvsRoot, password, sshPasswordDialog.saveThisPassword());
      }

      if (password == null) return false;
    }
    return true;
  }
}
