package com.manuelmaly.hn.parser;

import com.manuelmaly.hn.model.HNComment;
import com.manuelmaly.hn.model.HNCommentTreeNode;
import com.manuelmaly.hn.model.HNPostComments;

import org.junit.Before;
import org.junit.Test;

import java.io.InputStream;
import java.util.List;
import java.util.Scanner;

import static org.junit.Assert.*;

public class HNCommentsParserTest {

    private HNCommentsParser parser;
    private String sampleHtml;

    @Before
    public void setUp() throws Exception {
        parser = new HNCommentsParser("testUser");
        sampleHtml = loadResource("sample_comments.html");
    }

    @Test
    public void parsesCommentsSuccessfully() throws Exception {
        HNPostComments comments = parser.parse(sampleHtml);
        assertNotNull("Comments should not be null", comments);
    }

    @Test
    public void parsesCorrectNumberOfComments() throws Exception {
        HNPostComments comments = parser.parse(sampleHtml);
        List<HNComment> flatComments = comments.getComments();
        assertNotNull(flatComments);
        assertTrue("Should parse at least 1 comment", flatComments.size() >= 1);
    }

    @Test
    public void parsesCommentAuthor() throws Exception {
        HNPostComments comments = parser.parse(sampleHtml);
        List<HNComment> flatComments = comments.getComments();
        if (!flatComments.isEmpty()) {
            HNComment first = flatComments.get(0);
            assertNotNull("Comment author should not be null", first.getAuthor());
            assertFalse("Comment author should not be empty", first.getAuthor().isEmpty());
        }
    }

    @Test
    public void parsesCommentText() throws Exception {
        HNPostComments comments = parser.parse(sampleHtml);
        List<HNComment> flatComments = comments.getComments();
        if (!flatComments.isEmpty()) {
            HNComment first = flatComments.get(0);
            assertNotNull("Comment text should not be null", first.getText());
            assertFalse("Comment text should not be empty", first.getText().isEmpty());
        }
    }

    @Test
    public void parsesCommentColor() throws Exception {
        HNPostComments comments = parser.parse(sampleHtml);
        List<HNComment> flatComments = comments.getComments();
        if (!flatComments.isEmpty()) {
            HNComment first = flatComments.get(0);
            // Color should be parsed from class attribute (c5a = rgb(0x5A, 0x5A, 0x5A))
            assertNotNull("Color should not be null", first.getColor());
        }
    }

    @Test
    public void parsesCommentLevels() throws Exception {
        HNPostComments comments = parser.parse(sampleHtml);
        List<HNComment> flatComments = comments.getComments();
        if (flatComments.size() >= 2) {
            // First comment should be level 0
            assertEquals("First comment should be level 0", 0, flatComments.get(0).getCommentLevel());
        }
    }

    @Test
    public void parsesUserAcquiredFor() throws Exception {
        HNPostComments comments = parser.parse(sampleHtml);
        assertEquals("testUser", comments.getUserAcquiredFor());
    }

    @Test
    public void handlesNullInput() throws Exception {
        HNPostComments comments = parser.parse(null);
        assertNotNull(comments);
    }

    @Test
    public void handlesEmptyHtml() throws Exception {
        HNPostComments comments = parser.parse("<html><body></body></html>");
        assertNotNull(comments);
    }

    @Test
    public void treeNodeStructureIsBuilt() throws Exception {
        HNPostComments comments = parser.parse(sampleHtml);
        List<HNCommentTreeNode> treeNodes = comments.getTreeNodes();
        assertNotNull("Tree nodes should not be null", treeNodes);
    }

    @Test
    public void toggleCommentExpandedWorks() throws Exception {
        HNPostComments comments = parser.parse(sampleHtml);
        List<HNComment> flatComments = comments.getComments();
        if (!flatComments.isEmpty()) {
            HNComment first = flatComments.get(0);
            boolean wasExpanded = first.getTreeNode().isExpanded();
            comments.toggleCommentExpanded(first);
            assertEquals("Expanded state should toggle", !wasExpanded, first.getTreeNode().isExpanded());
        }
    }

    private String loadResource(String name) throws Exception {
        InputStream is = getClass().getClassLoader().getResourceAsStream(name);
        assertNotNull("Resource " + name + " should exist", is);
        Scanner scanner = new Scanner(is, "UTF-8").useDelimiter("\\A");
        String content = scanner.hasNext() ? scanner.next() : "";
        scanner.close();
        return content;
    }
}
