/*
 * Copyright (c) 2015-2015 Vladimir Schneider <vladimir.schneider@gmail.com>, all rights reserved.
 *
 * This code is private property of the copyright holder and cannot be used without
 * having obtained a license or prior written permission of the of the copyright holder.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 */
package com.vladsch.idea.multimarkdown.util

import com.vladsch.idea.multimarkdown.TestUtils.*
import org.junit.Test

class TestLinkResolver_Compl_wiki_normal_file_LinkRef {
    val projectResolver: LinkResolver.ProjectResolver = MarkdownTestData
    val containingFileRef = FileRef("/Users/vlad/src/MarkdownTest/MarkdownTest.wiki/normal-file.md")
    val resolver = GitHubLinkResolver(projectResolver, containingFileRef)

    /**
     * REMOTE or LOCAL URI
     */
    @Test
    fun test_LocalRemoteUri_With_png() {
        val linkRef = LinkRef(containingFileRef, ".png", null, null)
        val list = resolver.multiResolve(linkRef, LinkResolver.PREFER_LOCAL or LinkResolver.ONLY_REMOTE or LinkResolver.ONLY_URI or LinkResolver.LOOSE_MATCH)
        val matchText = resolver.getLastMatcher()?.linkLooseMatch

        compareOrderedLists("$matchText does not match\n", imageFiles.asLocalURI(), list.asFilePaths())
    }

    @Test
    fun test_LocalRemoteUri_With_kt() {
        val linkRef = LinkRef(containingFileRef, ".kt", null, null)
        val list = resolver.multiResolve(linkRef, LinkResolver.PREFER_LOCAL or LinkResolver.ONLY_REMOTE or LinkResolver.ONLY_URI or LinkResolver.LOOSE_MATCH)
        val matchText = resolver.getLastMatcher()?.linkLooseMatch

        compareOrderedLists("$matchText does not match\n", kotlinFiles.asLocalURI(), list.asFilePaths())
    }

    @Test
    fun test_LocalRemoteUri_With_md() {
        val linkRef = LinkRef(containingFileRef, ".md", null, null)
        val list = resolver.multiResolve(linkRef, LinkResolver.PREFER_LOCAL or LinkResolver.ONLY_REMOTE or LinkResolver.ONLY_URI or LinkResolver.LOOSE_MATCH)
        val matchText = resolver.getLastMatcher()?.linkLooseMatch

        compareOrderedLists("$matchText does not match\n", markdownFiles.asLocalURI(), list.asFilePaths())
    }

    @Test
    fun test_LocalRemoteUri_NoExt() {
        val linkRef = LinkRef(containingFileRef, "", null, null)
        val list = resolver.multiResolve(linkRef, LinkResolver.PREFER_LOCAL or LinkResolver.ONLY_REMOTE or LinkResolver.ONLY_URI or LinkResolver.LOOSE_MATCH)
        val matchText = resolver.getLastMatcher()?.linkLooseMatch

        compareOrderedLists("$matchText does not match\n", markdownFiles.asLocalURI().with(gitHubLinks), list.asFilePaths())
    }

    /**
     * REMOTE URI
     */
    @Test
    fun test_RemoteUri_With_png() {
        val linkRef = LinkRef(containingFileRef, ".png", null, null)
        val list = resolver.multiResolve(linkRef, LinkResolver.ONLY_REMOTE or LinkResolver.ONLY_URI or LinkResolver.LOOSE_MATCH)
        val matchText = resolver.getLastMatcher()?.linkLooseMatch

        compareOrderedLists("$matchText does not match\n", imageRemoteFiles.asRemoteURI(), list.asFilePaths())
    }

    @Test
    fun test_RemoteUri_With_kt() {
        val linkRef = LinkRef(containingFileRef, ".kt", null, null)
        val list = resolver.multiResolve(linkRef, LinkResolver.ONLY_REMOTE or LinkResolver.ONLY_URI or LinkResolver.LOOSE_MATCH)
        val matchText = resolver.getLastMatcher()?.linkLooseMatch

        compareOrderedLists("$matchText does not match\n", kotlinFiles.asRemoteURI(), list.asFilePaths())
    }

    @Test
    fun test_RemoteUri_With_md() {
        val linkRef = LinkRef(containingFileRef, ".md", null, null)
        val list = resolver.multiResolve(linkRef, LinkResolver.ONLY_REMOTE or LinkResolver.ONLY_URI or LinkResolver.LOOSE_MATCH)
        val matchText = resolver.getLastMatcher()?.linkLooseMatch

        compareOrderedLists("$matchText does not match\n", markdownRemoteFiles.asRemoteURI(), list.asFilePaths())
    }

    @Test
    fun test_RemoteUri_NoExt() {
        val linkRef = LinkRef(containingFileRef, "", null, null)
        val list = resolver.multiResolve(linkRef, LinkResolver.ONLY_REMOTE or LinkResolver.ONLY_URI or LinkResolver.LOOSE_MATCH)
        val matchText = resolver.getLastMatcher()?.linkLooseMatch

        compareOrderedLists("$matchText does not match\n", markdownRemoteFiles.asRemoteURI().with(gitHubLinks), list.asFilePaths())
    }

    /**
     * URI
     */
    @Test
    fun test_Uri_With_png() {
        val linkRef = LinkRef(containingFileRef, ".png", null, null)
        val list = resolver.multiResolve(linkRef, LinkResolver.ONLY_URI or LinkResolver.LOOSE_MATCH)
        val matchText = resolver.getLastMatcher()?.linkLooseMatch

        compareOrderedLists("$matchText does not match\n", imageFiles.asURI(), list.asFilePaths())
    }

    @Test
    fun test_Uri_With_kt() {
        val linkRef = LinkRef(containingFileRef, ".kt", null, null)
        val list = resolver.multiResolve(linkRef, LinkResolver.ONLY_URI or LinkResolver.LOOSE_MATCH)
        val matchText = resolver.getLastMatcher()?.linkLooseMatch

        compareOrderedLists("$matchText does not match\n", kotlinFiles.asURI(), list.asFilePaths())
    }

    @Test
    fun test_Uri_With_md() {
        val linkRef = LinkRef(containingFileRef, ".md", null, null)
        val list = resolver.multiResolve(linkRef, LinkResolver.ONLY_URI or LinkResolver.LOOSE_MATCH)
        val matchText = resolver.getLastMatcher()?.linkLooseMatch

        compareOrderedLists("$matchText does not match\n", markdownFiles.asURI(), list.asFilePaths())
    }

    @Test
    fun test_Uri_NoExt() {
        val linkRef = LinkRef(containingFileRef, "", null, null)
        val list = resolver.multiResolve(linkRef, LinkResolver.ONLY_URI or LinkResolver.LOOSE_MATCH)
        val matchText = resolver.getLastMatcher()?.linkLooseMatch

        compareOrderedLists("$matchText does not match\n", markdownFiles.asURI().with(gitHubLinks), list.asFilePaths())
    }

    /**
     * REMOTE
     *
     */

    @Test
    fun test_Remote_With_png() {
        val linkRef = LinkRef(containingFileRef, ".png", null, null)
        val list = resolver.multiResolve(linkRef, LinkResolver.ONLY_REMOTE or LinkResolver.LOOSE_MATCH)
        val matchText = resolver.getLastMatcher()?.linkLooseMatch

        compareOrderedLists("$matchText does not match\n", imageRemoteFiles, list.asFilePaths())
    }

    @Test
    fun test_Remote_With_kt() {
        val linkRef = LinkRef(containingFileRef, ".kt", null, null)
        val list = resolver.multiResolve(linkRef, LinkResolver.ONLY_REMOTE or LinkResolver.LOOSE_MATCH)
        val matchText = resolver.getLastMatcher()?.linkLooseMatch

        compareOrderedLists("$matchText does not match\n", kotlinFiles, list.asFilePaths())
    }

    @Test
    fun test_Remote_With_md() {
        val linkRef = LinkRef(containingFileRef, ".md", null, null)
        val list = resolver.multiResolve(linkRef, LinkResolver.ONLY_REMOTE or LinkResolver.LOOSE_MATCH)
        val matchText = resolver.getLastMatcher()?.linkLooseMatch

        compareOrderedLists("$matchText does not match\n", markdownRemoteFiles, list.asFilePaths())
    }

    @Test
    fun test_Remote_NoExt() {
        val linkRef = LinkRef(containingFileRef, "", null, null)
        val list = resolver.multiResolve(linkRef, LinkResolver.ONLY_REMOTE or LinkResolver.LOOSE_MATCH)
        val matchText = resolver.getLastMatcher()?.linkLooseMatch

        compareOrderedLists("$matchText does not match\n", markdownRemoteFiles, list.asFilePaths())
    }

    /**
     *
     * BASIC
     */
    @Test fun test_Basic_WithExt_png() {
        val linkRef = LinkRef(containingFileRef, ".png", null, null)
        val list = resolver.multiResolve(linkRef, LinkResolver.LOOSE_MATCH)
        val matchText = resolver.getLastMatcher()?.linkLooseMatch

        compareOrderedLists("$matchText does not match\n", imageFiles, list.asFilePaths())
    }

    @Test fun test_Basic_WithExt_iml() {
        val linkRef = LinkRef(containingFileRef, ".iml", null, null)
        val list = resolver.multiResolve(linkRef, LinkResolver.LOOSE_MATCH)
        val matchText = resolver.getLastMatcher()?.linkLooseMatch

        compareOrderedLists("$matchText does not match\n", arrayListOf<String>(
                "/Users/vlad/src/MarkdownTest/MarkdownTest.iml",
                "/Users/vlad/src/MarkdownTest/untitled/untitled.iml"
        ), list.asFilePaths())
    }

    @Test fun test_Basic_WithExt_kt() {
        val linkRef = LinkRef(containingFileRef, ".kt", null, null)
        val list = resolver.multiResolve(linkRef, LinkResolver.LOOSE_MATCH)
        val matchText = resolver.getLastMatcher()?.linkLooseMatch

        compareOrderedLists("$matchText does not match\n", kotlinFiles, list.asFilePaths())
    }

    @Test fun test_Basic_WithExt_md() {
        val linkRef = LinkRef(containingFileRef, ".md", null, null)
        val list = resolver.multiResolve(linkRef, LinkResolver.LOOSE_MATCH)
        val matchText = resolver.getLastMatcher()?.linkLooseMatch

        compareOrderedLists("$matchText does not match\n", markdownFiles, list.asFilePaths())
    }

    @Test fun test_Basic_NoExt() {
        val linkRef = LinkRef(containingFileRef, "", null, null)
        val list = resolver.multiResolve(linkRef, LinkResolver.LOOSE_MATCH)
        val matchText = resolver.getLastMatcher()?.linkLooseMatch

        compareOrderedLists("$matchText does not match\n", markdownFiles.with(gitHubLinks), list.asFilePaths())
    }

}
