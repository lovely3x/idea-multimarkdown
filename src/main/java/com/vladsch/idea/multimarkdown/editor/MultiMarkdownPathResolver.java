/*
 * Copyright (c) 2011-2014 Julien Nicoulaud <julien.nicoulaud@gmail.com>
 * Copyright (c) 2015 Vladimir Schneider <vladimir.schneider@gmail.com>
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.vladsch.idea.multimarkdown.editor;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.VirtualFileSystem;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.GlobalSearchScope;
import com.vladsch.idea.multimarkdown.MultiMarkdownPlugin;
import com.vladsch.idea.multimarkdown.MultiMarkdownProjectComponent;
import com.vladsch.idea.multimarkdown.util.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.*;

/**
 * Static utilities for resolving resources paths.
 *
 * @author Roger Grantham (https://github.com/grantham)
 * @author Julien Nicoulaud <julien.nicoulaud@gmail.com>
 * @since 0.8
 */
public class MultiMarkdownPathResolver {
    private static final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(MultiMarkdownPathResolver.class);

    /**
     * Not to be instantiated.
     */
    private MultiMarkdownPathResolver() {
        // no op
    }

    /**
     * Makes a simple attempt to convert the URL into a VirtualFile.
     *
     * @param target url from which a VirtualFile is sought
     * @return VirtualFile or null
     */
    public static VirtualFile findVirtualFile(@NotNull URL target) {
        VirtualFileSystem virtualFileSystem = VirtualFileManager.getInstance().getFileSystem(target.getProtocol());
        return virtualFileSystem == null ? null : virtualFileSystem.findFileByPath(target.getFile());
    }

    /**
     * Interprets <var>target</var> as a path relative to the given document.
     *
     * @param document the document
     * @param target   relative path from which a VirtualFile is sought
     * @return VirtualFile or null
     */
    @Nullable
    public static String resolveExternalReference(@NotNull Project project, @NotNull Document document, @NotNull String target) {
        FileReference resolvedTarget = null;
        VirtualFile file = FileDocumentManager.getInstance().getFile(document);
        VirtualFile relativeFile = null;

        if (file != null) {
            FileReference documentFileReference = new FileReference(file.getPath(), project);
            resolvedTarget = documentFileReference.resolveExternalLinkRef(target, true, false);
        }
        return resolvedTarget == null ? null : resolvedTarget.getFilePathWithAnchor();
    }

    /**
     * Interprets <var>target</var> as a path relative to the given document.
     *
     * @param document the document
     * @param target   relative path from which a VirtualFile is sought
     * @return VirtualFile or null
     */
    public static VirtualFile resolveRelativePath(@Nullable Project project, @NotNull Document document, @NotNull String target) {
        VirtualFile relativeFile = null;
        VirtualFile file = FileDocumentManager.getInstance().getFile(document);
        if (file != null && !FilePathInfo.isExternalReference(target)) {
            FileReference documentFileReference = new FileReference(file.getPath(), project);
            boolean noLinks = false;

            if (project != null) {
                GitHubRepo gitHubRepo = documentFileReference.getGitHubRepo();
                FilePathInfo targetInfo = new FilePathInfo(target);
                FileReferenceList fileReferenceList = new FileReferenceListQuery(project)
                        .gitHubWikiRules()
                        .sameGitHubRepo()
                        .matchLinkRefNoExt(targetInfo.getFilePathWithAnchorNoExt(), file, project)
                        .wantMarkdownFiles()
                        .all()
                        .sorted();

                if (!canResolveRelativeLink(fileReferenceList, documentFileReference, gitHubRepo, target, true)) return null;

                PsiFile[] files = fileReferenceList.getPsiFiles();
                FilePathInfo newTargetInfo = resolveRelativeLink(files, documentFileReference, gitHubRepo, targetInfo);
                if (newTargetInfo != targetInfo) {
                    target = newTargetInfo.getFullFilePath();
                    noLinks = true;
                }
            }

            if (noLinks) {
                // should resolve as is
                FileReference resolvedTarget = documentFileReference.resolveLinkRef(target, false);
                if (resolvedTarget != null && !resolvedTarget.isExternalReference()) {
                    relativeFile = resolvedTarget.getVirtualFile();
                }
            } else {
                if (FilePathInfo.endsWith(target, FilePathInfo.GITHUB_LINKS)) {
                    FileReference resolvedTarget = documentFileReference.resolveLinkRef(target, false);
                    //if (resolvedTarget != null && (resolvedTarget.isExternalReference() || resolvedTarget.getVirtualFile() != null)) return true;
                    if (resolvedTarget == null) {
                        resolvedTarget = documentFileReference.resolveExternalLinkRef(target, true, false);
                    }

                    if (resolvedTarget != null && !resolvedTarget.isExternalReference()) {
                        relativeFile = resolvedTarget.getVirtualFile();
                    }
                }
            }
        }
        return relativeFile;
    }

    public static boolean canResolveRelativeLink(@NotNull FileReferenceList fileReferenceList, @NotNull FileReference documentFileReference, @Nullable GitHubRepo gitHubRepo, @NotNull String target, boolean resolveExternal) {
        // need to resolve using same code as links
        FilePathInfo targetInfo = new FilePathInfo(target);
        FileReferenceList fileList = fileReferenceList.query()
                .gitHubWikiRules()
                .wantMarkdownFiles()
                .inSource(documentFileReference)
                .matchLinkRefNoExt(targetInfo.getFilePathWithAnchorNoExt())
                .first();

        PsiFile[] files = fileList.getPsiFiles();

        // can just compare if it is the same instance, if not then it was resolved
        if (resolveRelativeLink(files, documentFileReference, gitHubRepo, targetInfo) != targetInfo) return true;

        target = targetInfo.getFullFilePath();

        if (FilePathInfo.endsWith(target, FilePathInfo.GITHUB_LINKS)) {
            FileReference resolvedTarget = documentFileReference.resolveLinkRef(target, false);
            //if (resolvedTarget != null && (resolvedTarget.isExternalReference() || resolvedTarget.getVirtualFile() != null)) return true;
            if (resolvedTarget != null) return true;
            resolvedTarget = documentFileReference.resolveExternalLinkRef(target, true, false);
            if (resolvedTarget != null) return true;
        }
        return false;
    }

    @NotNull
    public static FilePathInfo resolveRelativeLink(PsiFile[] files, @NotNull FileReference documentFileReference, @Nullable GitHubRepo gitHubRepo, @NotNull FilePathInfo targetInfo) {
        boolean wantExt = !documentFileReference.isWikiPage();
        String newTarget = null;
        if (files.length != 0) {
            FileReferenceLink fileReferenceLink;
            if (gitHubRepo != null) {
                fileReferenceLink = new FileReferenceLinkGitHubRules(documentFileReference, files[0]);
                if (!wantExt || fileReferenceLink.isWikiPage()) {
                    wantExt = false;
                    newTarget = fileReferenceLink.getLinkRefNoExt();
                } else {
                    newTarget = fileReferenceLink.getLinkRef();
                }
            } else {
                fileReferenceLink = new FileReferenceLink(documentFileReference, files[0]);
                newTarget = fileReferenceLink.getLinkRef();
            }

            // make sure both have matching extensions or no match
            if (wantExt) {
                if (!targetInfo.getExt().equalsIgnoreCase(fileReferenceLink.getExt())) return targetInfo;

                // now if the source and target are not wiki pages then comparison is case sensitive
                if (!documentFileReference.isWikiPage() && !fileReferenceLink.isWikiPage()) {
                    if (!targetInfo.getFilePath().equals(fileReferenceLink.getLinkRef())) return targetInfo;
                }
            } else {
                if (targetInfo.hasExt() && fileReferenceLink.isWikiPage()) return targetInfo;

                // now if the source and target are not wiki pages then comparison is case sensitive
                if (!documentFileReference.isWikiPage() && !fileReferenceLink.isWikiPage()) {
                    if (!targetInfo.getFileNameNoExt().equals(fileReferenceLink.getLinkRefNoExt())) return targetInfo;
                }
            }

            return new FilePathInfo(newTarget);
        }

        return targetInfo;
    }

    /**
     * Interprets <var>target</var> as a class reference.
     *
     * @param project the project to look for files in
     * @param target  from which a VirtualFile is sought
     * @return VirtualFile or null
     */
    public static VirtualFile resolveClassReference(@NotNull final Project project, @NotNull final String target) {
        try {
            if (!DumbService.isDumb(project)) {
                return ApplicationManager.getApplication().runReadAction(new Computable<VirtualFile>() {
                    @Override
                    public VirtualFile compute() {
                        try {
                            final PsiClass classpathResource = JavaPsiFacade.getInstance(project).findClass(target, GlobalSearchScope.projectScope(project));
                            if (classpathResource != null) {
                                return classpathResource.getContainingFile().getVirtualFile();
                            }
                        } catch (NoClassDefFoundError ignored) {
                            // API might not be available on all IntelliJ platform IDEs
                        }
                        return null;
                    }
                });
            }
        } catch (NoClassDefFoundError ignored) {

        }
        return null;
    }

    public static boolean isWikiDocument(@NotNull final Document document) {
        VirtualFile file = FileDocumentManager.getInstance().getFile(document);
        return file != null && new FilePathInfo(file).isWikiPage();
    }

    public static Object resolveImageLink(@Nullable final Project project, @NotNull final Document document, @NotNull String hrefEnc) {
        String hrefDec = hrefEnc;
        try {
            hrefDec = URLDecoder.decode(hrefEnc, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        int posHash;
        if ((posHash = hrefDec.indexOf('#')) > 0) {
            hrefDec = hrefDec.substring(0, posHash);
        }

        final String href = hrefDec;

        if (!FilePathInfo.isExternalReference(href)) {
            final Object[] foundFile = { null };
            final Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    VirtualFile virtualTarget = null;

                    if (href.startsWith("file:")) {
                        try {
                            URL target = new URL(href);
                            virtualTarget = findVirtualFile(target);
                        } catch (MalformedURLException e) {
                            //e.printStackTrace();
                        }
                    }

                    // relative path then we can open it.

                    if ((virtualTarget == null || !virtualTarget.exists())) {
                        virtualTarget = resolveRelativePath(project, document, href);
                    }

                    foundFile[0] = virtualTarget;
                }
            };

            Application application = ApplicationManager.getApplication();
            application.runReadAction(runnable);

            //if (foundFile[0] == null && project != null) {
            //    // try link remapping
            //    String newHref = resolveExternalReference(project, document, href);
            //    if (newHref == null) return null;
            //    hrefEnc = hrefDec = newHref;
            //} else {
            //    return foundFile[0];
            //}
            return foundFile[0];
        }

        if (FilePathInfo.isExternalReference(hrefDec)) {
            if (Desktop.isDesktopSupported()) {
                try {
                    return new URI(hrefEnc);
                } catch (URISyntaxException ex) {
                    // invalid URI, just log
                    logger.info("URISyntaxException on '" + hrefDec + "'" + ex.toString());
                }
            }
        }
        return null;
    }

    @Nullable
    public static Object openLink(@NotNull String href) {
        if (Desktop.isDesktopSupported()) {
            try {
                Object foundFile = new URI(href);
                Desktop.getDesktop().browse((URI) foundFile);
                return foundFile;
            } catch (URISyntaxException ex) {
                // invalid URI, just log
                logger.info("URISyntaxException on '" + href + "'" + ex.toString());
            } catch (IOException ex) {
                logger.info("IOException on '" + href + "'" + ex.toString());
            }
        }
        return null;
    }

    @Nullable
    public static String getGitHubDocumentURL(@NotNull Project project, @NotNull Document document, boolean noExtension) {
        MultiMarkdownProjectComponent projectComponent = MultiMarkdownPlugin.getProjectComponent(project);
        String githubhref = null;
        if (projectComponent != null) {
            VirtualFile virtualFile = FileDocumentManager.getInstance().getFile(document);

            if (virtualFile != null && projectComponent.isUnderVcs(virtualFile)) {
                GitHubRepo gitHubRepo = projectComponent.getGitHubRepo(virtualFile.getPath());
                if (gitHubRepo != null) {
                    FilePathInfo pathInfo = new FilePathInfo(virtualFile);
                    githubhref = gitHubRepo.repoUrlFor(gitHubRepo.getRelativePath(noExtension ? pathInfo.getFilePathNoExt() : pathInfo.getFilePath()));
                    if (githubhref != null && !FilePathInfo.isExternalReference(githubhref)) {
                        githubhref = null;
                    }
                }
            }
        }
        return githubhref;
    }

    public static void launchExternalLink(@NotNull Project project, @NotNull Document document, @NotNull String href) {
        Object resolved = resolveImageLink(project, document, href);
        if (resolved != null && resolved instanceof VirtualFile) {
            // can resolve, let see if we need to map it to github
            VirtualFile documentFile = FileDocumentManager.getInstance().getFile(document);
            if (documentFile != null) {
                FileReferenceLink fileReferenceLink = new FileReferenceLink(documentFile, (VirtualFile) resolved, project);
                // if it is under git source code control map it to remote
                MultiMarkdownProjectComponent projectComponent = MultiMarkdownPlugin.getProjectComponent(project);
                if (projectComponent != null) {
                    if (projectComponent.isUnderVcs((VirtualFile) resolved)) {
                        GitHubRepo gitHubRepo = projectComponent.getGitHubRepo(fileReferenceLink.getPath());
                        if (gitHubRepo != null) {
                            String githubhref = null;
                            FilePathInfo hrefInfo = new FilePathInfo(href);

                            if (fileReferenceLink.isMarkdownExt() && gitHubRepo.isWiki()) {
                                githubhref = gitHubRepo.repoUrlFor(fileReferenceLink.getFileNameNoExt()) + hrefInfo.getAnchor();
                            } else {
                                githubhref = gitHubRepo.repoUrlFor(fileReferenceLink.getLinkRef()) + hrefInfo.getAnchor();
                            }

                            if (FilePathInfo.isExternalReference(githubhref)) {
                                // remap it to external and launch browser
                                if (Desktop.isDesktopSupported()) {
                                    try {
                                        Desktop.getDesktop().browse((URI) new URI(githubhref));
                                        return;
                                    } catch (URISyntaxException ex) {
                                        // invalid URI, just log
                                        logger.info("URISyntaxException on '" + githubhref + "'" + ex.toString());
                                    } catch (IOException ex) {
                                        logger.info("IOException on '" + githubhref + "'" + ex.toString());
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        String targetRef = null;
        if (resolved != null && resolved instanceof String) {
            targetRef = (String) resolved;
        }

        if (targetRef == null) targetRef = resolveExternalReference(project, document, href);
        if (targetRef != null && FilePathInfo.isExternalReference(targetRef)) {
            launchExternalLink(project, document, targetRef);
        }
    }
}
