/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package com.intellij.remotesdk2;

import com.intellij.openapi.util.text.StringUtil;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

/**
* @author traff
*/
public class VagrantBasedCredentialsHolder {
  private static final String VAGRANT_FOLDER = "VAGRANT_FOLDER";
  private String myVagrantFolder;

  public VagrantBasedCredentialsHolder() {
  }

  public VagrantBasedCredentialsHolder(@NotNull String folder) {
    myVagrantFolder = folder;
  }

  public void setVagrantFolder(String vagrantFolder) {
    myVagrantFolder = vagrantFolder;
  }

  @NotNull
  public String getVagrantFolder() {
    return myVagrantFolder;
  }

  public void load(Element element) {
    setVagrantFolder(element.getAttributeValue(VAGRANT_FOLDER));
  }

  public void save(Element element) {
    element.setAttribute(VAGRANT_FOLDER, getVagrantFolder());
  }
}
