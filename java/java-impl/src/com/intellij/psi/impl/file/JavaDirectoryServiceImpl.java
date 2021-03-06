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

/*
 * @author max
 */
package com.intellij.psi.impl.file;

import com.intellij.core.CoreJavaDirectoryService;
import com.intellij.ide.fileTemplates.FileTemplate;
import com.intellij.ide.fileTemplates.FileTemplateManager;
import com.intellij.ide.fileTemplates.FileTemplateUtil;
import com.intellij.ide.fileTemplates.JavaTemplateUtil;
import com.intellij.ide.fileTemplates.ui.CreateFromTemplateDialog;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileTypes.StdFileTypes;
import com.intellij.openapi.module.LanguageLevelUtil;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.LanguageLevelProjectExtension;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.pom.java.LanguageLevel;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiUtil;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Map;
import java.util.Properties;

public class JavaDirectoryServiceImpl extends CoreJavaDirectoryService {
  private static final Logger LOG = Logger.getInstance("#com.intellij.psi.impl.file.JavaDirectoryServiceImpl");

  @Override
  public PsiPackage getPackage(@NotNull PsiDirectory dir) {
    ProjectFileIndex projectFileIndex = ProjectRootManager.getInstance(dir.getProject()).getFileIndex();
    String packageName = projectFileIndex.getPackageNameByDirectory(dir.getVirtualFile());
    if (packageName == null) return null;
    return JavaPsiFacade.getInstance(dir.getProject()).findPackage(packageName);
  }

  @Override
  @NotNull
  public PsiClass createClass(@NotNull PsiDirectory dir, @NotNull String name) throws IncorrectOperationException {
    return createClassFromTemplate(dir, name, JavaTemplateUtil.INTERNAL_CLASS_TEMPLATE_NAME);
  }

  @Override
  @NotNull
  public PsiClass createClass(@NotNull PsiDirectory dir, @NotNull String name, @NotNull String templateName) throws IncorrectOperationException {
    return createClassFromTemplate(dir, name, templateName);
  }

  @Override
  public PsiClass createClass(@NotNull PsiDirectory dir,
                              @NotNull String name,
                              @NotNull String templateName,
                              boolean askForUndefinedVariables) throws IncorrectOperationException {
    return createClass(dir, name, templateName, askForUndefinedVariables, Collections.<String, String>emptyMap());
  }

  @Override
  public PsiClass createClass(@NotNull PsiDirectory dir,
                              @NotNull String name,
                              @NotNull String templateName,
                              boolean askForUndefinedVariables, @NotNull final Map<String, String> additionalProperties) throws IncorrectOperationException {
    return createClassFromTemplate(dir, name, templateName, askForUndefinedVariables, additionalProperties);
  }

  @Override
  @NotNull
  public PsiClass createInterface(@NotNull PsiDirectory dir, @NotNull String name) throws IncorrectOperationException {
    String templateName = JavaTemplateUtil.INTERNAL_INTERFACE_TEMPLATE_NAME;
    PsiClass someClass = createClassFromTemplate(dir, name, templateName);
    if (!someClass.isInterface()) {
      throw new IncorrectOperationException(getIncorrectTemplateMessage(templateName));
    }
    return someClass;
  }

  @Override
  @NotNull
  public PsiClass createEnum(@NotNull PsiDirectory dir, @NotNull String name) throws IncorrectOperationException {
    String templateName = JavaTemplateUtil.INTERNAL_ENUM_TEMPLATE_NAME;
    PsiClass someClass = createClassFromTemplate(dir, name, templateName);
    if (!someClass.isEnum()) {
      throw new IncorrectOperationException(getIncorrectTemplateMessage(templateName));
    }
    return someClass;
  }

  @Override
  @NotNull
  public PsiClass createAnnotationType(@NotNull PsiDirectory dir, @NotNull String name) throws IncorrectOperationException {
    String templateName = JavaTemplateUtil.INTERNAL_ANNOTATION_TYPE_TEMPLATE_NAME;
    PsiClass someClass = createClassFromTemplate(dir, name, templateName);
    if (!someClass.isAnnotationType()) {
      throw new IncorrectOperationException(getIncorrectTemplateMessage(templateName));
    }
    return someClass;
  }

  private static PsiClass createClassFromTemplate(@NotNull PsiDirectory dir, String name, String templateName) throws IncorrectOperationException {
    return createClassFromTemplate(dir, name, templateName, false, Collections.<String, String>emptyMap());
  }

  private static PsiClass createClassFromTemplate(@NotNull PsiDirectory dir,
                                                  String name,
                                                  String templateName,
                                                  boolean askToDefineVariables, @NotNull Map<String, String> additionalProperties) throws IncorrectOperationException {
    //checkCreateClassOrInterface(dir, name);

    FileTemplate template = FileTemplateManager.getInstance().getInternalTemplate(templateName);

    Properties defaultProperties = FileTemplateManager.getInstance().getDefaultProperties(dir.getProject());
    Properties properties = new Properties(defaultProperties);
    properties.setProperty(FileTemplate.ATTRIBUTE_NAME, name);
    for (Map.Entry<String, String> entry : additionalProperties.entrySet()) {
      properties.setProperty(entry.getKey(), entry.getValue());
    }

    String ext = StdFileTypes.JAVA.getDefaultExtension();
    String fileName = name + "." + ext;

    PsiElement element;
    try {
      element = askToDefineVariables ? new CreateFromTemplateDialog(dir.getProject(), dir, template, null, properties).create()
                                     : FileTemplateUtil.createFromTemplate(template, fileName, properties, dir);
    }
    catch (IncorrectOperationException e) {
      throw e;
    }
    catch (Exception e) {
      LOG.error(e);
      return null;
    }
    if (element == null) return null;
    final PsiJavaFile file = (PsiJavaFile)element.getContainingFile();
    PsiClass[] classes = file.getClasses();
    if (classes.length < 1) {
      throw new IncorrectOperationException(getIncorrectTemplateMessage(templateName));
    }
    return classes[0];
  }

  private static String getIncorrectTemplateMessage(String templateName) {
    return PsiBundle.message("psi.error.incorroect.class.template.message",
                             FileTemplateManager.getInstance().internalTemplateToSubject(templateName), templateName);
  }

  @Override
  public void checkCreateClass(@NotNull PsiDirectory dir, @NotNull String name) throws IncorrectOperationException {
    checkCreateClassOrInterface(dir, name);
  }

  public static void checkCreateClassOrInterface(@NotNull PsiDirectory directory, String name) throws IncorrectOperationException {
    PsiUtil.checkIsIdentifier(directory.getManager(), name);

    String fileName = name + "." + StdFileTypes.JAVA.getDefaultExtension();
    directory.checkCreateFile(fileName);

    PsiNameHelper helper = JavaPsiFacade.getInstance(directory.getProject()).getNameHelper();
    PsiPackage aPackage = JavaDirectoryService.getInstance().getPackage(directory);
    String qualifiedName = aPackage == null ? null : aPackage.getQualifiedName();
    if (!StringUtil.isEmpty(qualifiedName) && !helper.isQualifiedName(qualifiedName)) {
      throw new IncorrectOperationException("Cannot create class in invalid package: '"+qualifiedName+"'");
    }
  }

  @Override
  public boolean isSourceRoot(@NotNull PsiDirectory dir) {
    final VirtualFile file = dir.getVirtualFile();
    final VirtualFile sourceRoot = ProjectRootManager.getInstance(dir.getProject()).getFileIndex().getSourceRootForFile(file);
    return file.equals(sourceRoot);
  }

  private static final Key<LanguageLevel> LANG_LEVEL_IN_DIRECTORY = new Key<LanguageLevel>("LANG_LEVEL_IN_DIRECTORY");
  @Override
  public LanguageLevel getLanguageLevel(@NotNull PsiDirectory dir) {
    synchronized (PsiLock.LOCK) {
      LanguageLevel level = dir.getUserData(LANG_LEVEL_IN_DIRECTORY);
      if (level == null) {
        level = getLanguageLevelInner(dir);
        dir.putUserData(LANG_LEVEL_IN_DIRECTORY, level);
      }
      return level;
    }
  }

  private static LanguageLevel getLanguageLevelInner(@NotNull PsiDirectory dir) {
    final VirtualFile virtualFile = dir.getVirtualFile();
    final Project project = dir.getProject();
    final Module module = ProjectRootManager.getInstance(project).getFileIndex().getModuleForFile(virtualFile);
    if (module != null) {
      return LanguageLevelUtil.getEffectiveLanguageLevel(module);
    }

    return LanguageLevelProjectExtension.getInstance(project).getLanguageLevel();
  }

}
