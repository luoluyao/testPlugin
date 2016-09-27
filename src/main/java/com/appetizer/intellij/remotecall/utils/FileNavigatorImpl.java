package com.appetizer.intellij.remotecall.utils;

import com.google.common.base.Joiner;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.markup.HighlighterLayer;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.ui.JBColor;

import java.awt.*;
import java.io.File;
import java.util.*;

public class FileNavigatorImpl implements FileNavigator {

  private static final Logger log = Logger.getInstance(FileNavigatorImpl.class);
  private static final Joiner pathJoiner = Joiner.on("/");

  @Override
  public void findAndNavigate(final String fileName, final int line, final int column, final int offsetline) {
    ApplicationManager.getApplication().invokeLater(new Runnable() {
      public void run() {
        Map<Project, Collection<VirtualFile>> foundFilesInAllProjects = new HashMap<Project, Collection<VirtualFile>>();
        Project[] projects = ProjectManager.getInstance().getOpenProjects();

        for (Project project : projects) {
          foundFilesInAllProjects
            .put(project, FilenameIndex.getVirtualFilesByName(project, new File(fileName).getName(), GlobalSearchScope.allScope(project)));
        }
        Deque<String> pathElements = splitPath(fileName);
        String variableFileName = pathJoiner.join(pathElements);

        while (pathElements.size() > 0) {
          for (Project project : foundFilesInAllProjects.keySet()) {
            for (VirtualFile directFile : foundFilesInAllProjects.get(project)) {
              if (directFile.getPath().endsWith(variableFileName)) {
                log.info("Found file " + directFile.getName());
                navigate(project, directFile, line, column, offsetline);
                return;
              }
            }
          }
          pathElements.pop();
          variableFileName = pathJoiner.join(pathElements);
        }
      }
    });
  }

  private static Deque<String> splitPath(String filePath) {
    File file = new File(filePath);
    Deque<String> pathParts = new ArrayDeque<String>();
    pathParts.push(file.getName());
    while ((file = file.getParentFile()) != null && !file.getName().isEmpty()) {
      pathParts.push(file.getName());
    }
    return pathParts;
  }

  private static void navigate(Project project, VirtualFile file, int line, int column, int offsetline) {
    final OpenFileDescriptor openFileDescriptor = new OpenFileDescriptor(project, file, line, column);
    if (openFileDescriptor.canNavigate()) {
      log.info("Trying to navigate to " + file.getPath() + ":" + line);
      openFileDescriptor.navigate(true);
      addLinesHighlighter(project, line, offsetline);
      Window parentWindow = WindowManager.getInstance().suggestParentWindow(project);
      if (parentWindow != null) {
        parentWindow.toFront();
      }
    }
    else {
      log.info("Cannot navigate");
    }
  }

  private static void addLinesHighlighter(Project project, int line, int offsetline) {
    Editor editor = FileEditorManager.getInstance(project).getSelectedTextEditor();
    editor.getMarkupModel().removeAllHighlighters();
    final TextAttributes attr = new TextAttributes();
    attr.setBackgroundColor(JBColor.LIGHT_GRAY);
    //attr.setForegroundColor(JBColor.LIGHT_GRAY);
    editor.getMarkupModel().addLineHighlighter(line, HighlighterLayer.LAST, attr);
    while( offsetline > 0 ) {
      editor.getMarkupModel().addLineHighlighter(line + offsetline, HighlighterLayer.LAST, attr);
      offsetline--;
    }
  }
}
