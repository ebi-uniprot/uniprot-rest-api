/*
 * Diff Match and Patch -- Test harness
 * Copyright 2018 The diff-match-patch Authors.
 * https://github.com/google/diff-match-patch
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.uniprot.api.unisave.repository.domain.impl;

import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

class diff_match_patchTest {

    private static diff_match_patch dmp = new diff_match_patch();
    private static diff_match_patch.Operation DELETE = diff_match_patch.Operation.DELETE;
    private static diff_match_patch.Operation EQUAL = diff_match_patch.Operation.EQUAL;
    private static diff_match_patch.Operation INSERT = diff_match_patch.Operation.INSERT;

    @Test
    void testDiffCommonPrefix() {
        // Detect any common prefix.
        thing(dmp.diff_commonPrefix("abc", "xyz"), 0, "diff_commonPrefix: Null case.");

        thing(
                4,
                dmp.diff_commonPrefix("1234abcdef", "1234xyz"),
                "diff_commonPrefix: Non-null case.");

        thing(4, dmp.diff_commonPrefix("1234", "1234xyz"), "diff_commonPrefix: Whole case.");
    }

    private void thing(int i, int diff_commonPrefix, String s) {}

    @Test
    void testDiffCommonSuffix() {
        // Detect any common suffix.
        thing(0, dmp.diff_commonSuffix("abc", "xyz"), "diff_commonSuffix: Null case.");

        thing(
                4,
                dmp.diff_commonSuffix("abcdef1234", "xyz1234"),
                "diff_commonSuffix: Non-null case.");

        thing(4, dmp.diff_commonSuffix("1234", "xyz1234"), "diff_commonSuffix: Whole case.");
    }

    @Test
    void testDiffCommonOverlap() {
        // Detect any suffix/prefix overlap.
        thing(0, dmp.diff_commonOverlap("", "abcd"), "diff_commonOverlap: Null case.");

        thing(3, dmp.diff_commonOverlap("abc", "abcd"), "diff_commonOverlap: Whole case.");

        thing(0, dmp.diff_commonOverlap("123456", "abcd"), "diff_commonOverlap: No overlap.");

        thing(3, dmp.diff_commonOverlap("123456xxx", "xxxabcd"), "diff_commonOverlap: Overlap.");

        // Some overly clever languages (C#) may treat ligatures as equal to their
        // component letters.  E.g. U+FB01 == 'fi'
        thing(0, dmp.diff_commonOverlap("fi", "\ufb01i"), "diff_commonOverlap: Unicode.");
    }

    @Test
    void testDiffHalfmatch() {
        // Detect a halfmatch.
        dmp.Diff_Timeout = 1;
        assertNull("diff_halfMatch: No match #1.", dmp.diff_halfMatch("1234567890", "abcdef"));

        assertNull("diff_halfMatch: No match #2.", dmp.diff_halfMatch("12345", "23"));

        assertArrayEquals(
                "diff_halfMatch: Single Match #1.",
                new String[] {"12", "90", "a", "z", "345678"},
                dmp.diff_halfMatch("1234567890", "a345678z"));

        assertArrayEquals(
                "diff_halfMatch: Single Match #2.",
                new String[] {"a", "z", "12", "90", "345678"},
                dmp.diff_halfMatch("a345678z", "1234567890"));

        assertArrayEquals(
                "diff_halfMatch: Single Match #3.",
                new String[] {"abc", "z", "1234", "0", "56789"},
                dmp.diff_halfMatch("abc56789z", "1234567890"));

        assertArrayEquals(
                "diff_halfMatch: Single Match #4.",
                new String[] {"a", "xyz", "1", "7890", "23456"},
                dmp.diff_halfMatch("a23456xyz", "1234567890"));

        assertArrayEquals(
                "diff_halfMatch: Multiple Matches #1.",
                new String[] {"12123", "123121", "a", "z", "1234123451234"},
                dmp.diff_halfMatch("121231234123451234123121", "a1234123451234z"));

        assertArrayEquals(
                "diff_halfMatch: Multiple Matches #2.",
                new String[] {"", "-=-=-=-=-=", "x", "", "x-=-=-=-=-=-=-="},
                dmp.diff_halfMatch("x-=-=-=-=-=-=-=-=-=-=-=-=", "xx-=-=-=-=-=-=-="));

        assertArrayEquals(
                "diff_halfMatch: Multiple Matches #3.",
                new String[] {"-=-=-=-=-=", "", "", "y", "-=-=-=-=-=-=-=y"},
                dmp.diff_halfMatch("-=-=-=-=-=-=-=-=-=-=-=-=y", "-=-=-=-=-=-=-=yy"));

        // Optimal diff would be -q+x=H-i+e=lloHe+Hu=llo-Hew+y not -qHillo+x=HelloHe-w+Hulloy
        assertArrayEquals(
                "diff_halfMatch: Non-optimal halfmatch.",
                new String[] {"qHillo", "w", "x", "Hulloy", "HelloHe"},
                dmp.diff_halfMatch("qHilloHelloHew", "xHelloHeHulloy"));

        dmp.Diff_Timeout = 0;
        assertNull(
                "diff_halfMatch: Optimal no halfmatch.",
                dmp.diff_halfMatch("qHilloHelloHew", "xHelloHeHulloy"));
    }

    @Test
    void testDiffLinesToChars() {
        // Convert lines down to characters.
        ArrayList<String> tmpVector = new ArrayList<String>();
        tmpVector.add("");
        tmpVector.add("alpha\n");
        tmpVector.add("beta\n");
        assertLinesToCharsResultEquals(
                "diff_linesToChars: Shared lines.",
                new diff_match_patch.LinesToCharsResult(
                        "\u0001\u0002\u0001", "\u0002\u0001\u0002", tmpVector),
                dmp.diff_linesToChars("alpha\nbeta\nalpha\n", "beta\nalpha\nbeta\n"));

        tmpVector.clear();
        tmpVector.add("");
        tmpVector.add("alpha\r\n");
        tmpVector.add("beta\r\n");
        tmpVector.add("\r\n");
        assertLinesToCharsResultEquals(
                "diff_linesToChars: Empty string and blank lines.",
                new diff_match_patch.LinesToCharsResult("", "\u0001\u0002\u0003\u0003", tmpVector),
                dmp.diff_linesToChars("", "alpha\r\nbeta\r\n\r\n\r\n"));

        tmpVector.clear();
        tmpVector.add("");
        tmpVector.add("a");
        tmpVector.add("b");
        assertLinesToCharsResultEquals(
                "diff_linesToChars: No linebreaks.",
                new diff_match_patch.LinesToCharsResult("\u0001", "\u0002", tmpVector),
                dmp.diff_linesToChars("a", "b"));

        // More than 256 to reveal any 8-bit limitations.
        int n = 300;
        tmpVector.clear();
        StringBuilder lineList = new StringBuilder();
        StringBuilder charList = new StringBuilder();
        for (int i = 1; i < n + 1; i++) {
            tmpVector.add(i + "\n");
            lineList.append(i + "\n");
            charList.append(String.valueOf((char) i));
        }
        assertEquals(n, tmpVector.size(), "Test initialization fail #1.");
        String lines = lineList.toString();
        String chars = charList.toString();
        assertEquals(n, chars.length(), "Test initialization fail #2.");
        tmpVector.add(0, "");
        assertLinesToCharsResultEquals(
                "diff_linesToChars: More than 256.",
                new diff_match_patch.LinesToCharsResult(chars, "", tmpVector),
                dmp.diff_linesToChars(lines, ""));
    }

    @Test
    void testDiffCleanupMerge() {
        // Cleanup a messy diff.
        LinkedList<diff_match_patch.Diff> diffs = diffList();
        dmp.diff_cleanupMerge(diffs);
        assertEquals(diffList(), diffs, "diff_cleanupMerge: Null case.");

        diffs =
                diffList(
                        new diff_match_patch.Diff(EQUAL, "a"),
                        new diff_match_patch.Diff(DELETE, "b"),
                        new diff_match_patch.Diff(INSERT, "c"));
        dmp.diff_cleanupMerge(diffs);
        assertEquals(
                diffList(
                        new diff_match_patch.Diff(EQUAL, "a"),
                        new diff_match_patch.Diff(DELETE, "b"),
                        new diff_match_patch.Diff(INSERT, "c")),
                diffs,
                "diff_cleanupMerge: No change case.");

        diffs =
                diffList(
                        new diff_match_patch.Diff(EQUAL, "a"),
                        new diff_match_patch.Diff(EQUAL, "b"),
                        new diff_match_patch.Diff(EQUAL, "c"));
        dmp.diff_cleanupMerge(diffs);
        assertEquals(
                diffList(new diff_match_patch.Diff(EQUAL, "abc")),
                diffs,
                "diff_cleanupMerge: Merge equalities.");

        diffs =
                diffList(
                        new diff_match_patch.Diff(DELETE, "a"),
                        new diff_match_patch.Diff(DELETE, "b"),
                        new diff_match_patch.Diff(DELETE, "c"));
        dmp.diff_cleanupMerge(diffs);
        assertEquals(
                diffList(new diff_match_patch.Diff(DELETE, "abc")),
                diffs,
                "diff_cleanupMerge: Merge deletions.");

        diffs =
                diffList(
                        new diff_match_patch.Diff(INSERT, "a"),
                        new diff_match_patch.Diff(INSERT, "b"),
                        new diff_match_patch.Diff(INSERT, "c"));
        dmp.diff_cleanupMerge(diffs);
        assertEquals(
                diffList(new diff_match_patch.Diff(INSERT, "abc")),
                diffs,
                "diff_cleanupMerge: Merge insertions.");

        diffs =
                diffList(
                        new diff_match_patch.Diff(DELETE, "a"),
                        new diff_match_patch.Diff(INSERT, "b"),
                        new diff_match_patch.Diff(DELETE, "c"),
                        new diff_match_patch.Diff(INSERT, "d"),
                        new diff_match_patch.Diff(EQUAL, "e"),
                        new diff_match_patch.Diff(EQUAL, "f"));
        dmp.diff_cleanupMerge(diffs);
        assertEquals(
                diffList(
                        new diff_match_patch.Diff(DELETE, "ac"),
                        new diff_match_patch.Diff(INSERT, "bd"),
                        new diff_match_patch.Diff(EQUAL, "ef")),
                diffs,
                "diff_cleanupMerge: Merge interweave.");

        diffs =
                diffList(
                        new diff_match_patch.Diff(DELETE, "a"),
                        new diff_match_patch.Diff(INSERT, "abc"),
                        new diff_match_patch.Diff(DELETE, "dc"));
        dmp.diff_cleanupMerge(diffs);
        assertEquals(
                diffList(
                        new diff_match_patch.Diff(EQUAL, "a"),
                        new diff_match_patch.Diff(DELETE, "d"),
                        new diff_match_patch.Diff(INSERT, "b"),
                        new diff_match_patch.Diff(EQUAL, "c")),
                diffs,
                "diff_cleanupMerge: Prefix and suffix detection.");

        diffs =
                diffList(
                        new diff_match_patch.Diff(EQUAL, "x"),
                        new diff_match_patch.Diff(DELETE, "a"),
                        new diff_match_patch.Diff(INSERT, "abc"),
                        new diff_match_patch.Diff(DELETE, "dc"),
                        new diff_match_patch.Diff(EQUAL, "y"));
        dmp.diff_cleanupMerge(diffs);
        assertEquals(
                diffList(
                        new diff_match_patch.Diff(EQUAL, "xa"),
                        new diff_match_patch.Diff(DELETE, "d"),
                        new diff_match_patch.Diff(INSERT, "b"),
                        new diff_match_patch.Diff(EQUAL, "cy")),
                diffs,
                "diff_cleanupMerge: Prefix and suffix detection with equalities.");

        diffs =
                diffList(
                        new diff_match_patch.Diff(EQUAL, "a"),
                        new diff_match_patch.Diff(INSERT, "ba"),
                        new diff_match_patch.Diff(EQUAL, "c"));
        dmp.diff_cleanupMerge(diffs);
        assertEquals(
                diffList(
                        new diff_match_patch.Diff(INSERT, "ab"),
                        new diff_match_patch.Diff(EQUAL, "ac")),
                diffs,
                "diff_cleanupMerge: Slide edit left.");

        diffs =
                diffList(
                        new diff_match_patch.Diff(EQUAL, "c"),
                        new diff_match_patch.Diff(INSERT, "ab"),
                        new diff_match_patch.Diff(EQUAL, "a"));
        dmp.diff_cleanupMerge(diffs);
        assertEquals(
                diffList(
                        new diff_match_patch.Diff(EQUAL, "ca"),
                        new diff_match_patch.Diff(INSERT, "ba")),
                diffs,
                "diff_cleanupMerge: Slide edit right.");

        diffs =
                diffList(
                        new diff_match_patch.Diff(EQUAL, "a"),
                        new diff_match_patch.Diff(DELETE, "b"),
                        new diff_match_patch.Diff(EQUAL, "c"),
                        new diff_match_patch.Diff(DELETE, "ac"),
                        new diff_match_patch.Diff(EQUAL, "x"));
        dmp.diff_cleanupMerge(diffs);
        assertEquals(
                diffList(
                        new diff_match_patch.Diff(DELETE, "abc"),
                        new diff_match_patch.Diff(EQUAL, "acx")),
                diffs,
                "diff_cleanupMerge: Slide edit left recursive.");

        diffs =
                diffList(
                        new diff_match_patch.Diff(EQUAL, "x"),
                        new diff_match_patch.Diff(DELETE, "ca"),
                        new diff_match_patch.Diff(EQUAL, "c"),
                        new diff_match_patch.Diff(DELETE, "b"),
                        new diff_match_patch.Diff(EQUAL, "a"));
        dmp.diff_cleanupMerge(diffs);
        assertEquals(
                diffList(
                        new diff_match_patch.Diff(EQUAL, "xca"),
                        new diff_match_patch.Diff(DELETE, "cba")),
                diffs,
                "diff_cleanupMerge: Slide edit right recursive.");

        diffs =
                diffList(
                        new diff_match_patch.Diff(DELETE, "b"),
                        new diff_match_patch.Diff(INSERT, "ab"),
                        new diff_match_patch.Diff(EQUAL, "c"));
        dmp.diff_cleanupMerge(diffs);
        assertEquals(
                diffList(
                        new diff_match_patch.Diff(INSERT, "a"),
                        new diff_match_patch.Diff(EQUAL, "bc")),
                diffs,
                "diff_cleanupMerge: Empty merge.");

        diffs =
                diffList(
                        new diff_match_patch.Diff(EQUAL, ""),
                        new diff_match_patch.Diff(INSERT, "a"),
                        new diff_match_patch.Diff(EQUAL, "b"));
        dmp.diff_cleanupMerge(diffs);
        assertEquals(
                diffList(
                        new diff_match_patch.Diff(INSERT, "a"),
                        new diff_match_patch.Diff(EQUAL, "b")),
                diffs,
                "diff_cleanupMerge: Empty equality.");
    }

    @Test
    void testDiffCleanupSemanticLossless() {
        // Slide diffs to match logical boundaries.
        LinkedList<diff_match_patch.Diff> diffs = diffList();
        dmp.diff_cleanupSemanticLossless(diffs);
        assertEquals(diffList(), diffs, "diff_cleanupSemanticLossless: Null case.");

        diffs =
                diffList(
                        new diff_match_patch.Diff(EQUAL, "AAA\r\n\r\nBBB"),
                        new diff_match_patch.Diff(INSERT, "\r\nDDD\r\n\r\nBBB"),
                        new diff_match_patch.Diff(EQUAL, "\r\nEEE"));
        dmp.diff_cleanupSemanticLossless(diffs);
        assertEquals(
                diffList(
                        new diff_match_patch.Diff(EQUAL, "AAA\r\n\r\n"),
                        new diff_match_patch.Diff(INSERT, "BBB\r\nDDD\r\n\r\n"),
                        new diff_match_patch.Diff(EQUAL, "BBB\r\nEEE")),
                diffs,
                "diff_cleanupSemanticLossless: Blank lines.");

        diffs =
                diffList(
                        new diff_match_patch.Diff(EQUAL, "AAA\r\nBBB"),
                        new diff_match_patch.Diff(INSERT, " DDD\r\nBBB"),
                        new diff_match_patch.Diff(EQUAL, " EEE"));
        dmp.diff_cleanupSemanticLossless(diffs);
        assertEquals(
                diffList(
                        new diff_match_patch.Diff(EQUAL, "AAA\r\n"),
                        new diff_match_patch.Diff(INSERT, "BBB DDD\r\n"),
                        new diff_match_patch.Diff(EQUAL, "BBB EEE")),
                diffs,
                "diff_cleanupSemanticLossless: Line boundaries.");

        diffs =
                diffList(
                        new diff_match_patch.Diff(EQUAL, "The c"),
                        new diff_match_patch.Diff(INSERT, "ow and the c"),
                        new diff_match_patch.Diff(EQUAL, "at."));
        dmp.diff_cleanupSemanticLossless(diffs);
        assertEquals(
                diffList(
                        new diff_match_patch.Diff(EQUAL, "The "),
                        new diff_match_patch.Diff(INSERT, "cow and the "),
                        new diff_match_patch.Diff(EQUAL, "cat.")),
                diffs,
                "diff_cleanupSemanticLossless: Word boundaries.");

        diffs =
                diffList(
                        new diff_match_patch.Diff(EQUAL, "The-c"),
                        new diff_match_patch.Diff(INSERT, "ow-and-the-c"),
                        new diff_match_patch.Diff(EQUAL, "at."));
        dmp.diff_cleanupSemanticLossless(diffs);
        assertEquals(
                diffList(
                        new diff_match_patch.Diff(EQUAL, "The-"),
                        new diff_match_patch.Diff(INSERT, "cow-and-the-"),
                        new diff_match_patch.Diff(EQUAL, "cat.")),
                diffs,
                "diff_cleanupSemanticLossless: Alphanumeric boundaries.");

        diffs =
                diffList(
                        new diff_match_patch.Diff(EQUAL, "a"),
                        new diff_match_patch.Diff(DELETE, "a"),
                        new diff_match_patch.Diff(EQUAL, "ax"));
        dmp.diff_cleanupSemanticLossless(diffs);
        assertEquals(
                diffList(
                        new diff_match_patch.Diff(DELETE, "a"),
                        new diff_match_patch.Diff(EQUAL, "aax")),
                diffs,
                "diff_cleanupSemanticLossless: Hitting the start.");

        diffs =
                diffList(
                        new diff_match_patch.Diff(EQUAL, "xa"),
                        new diff_match_patch.Diff(DELETE, "a"),
                        new diff_match_patch.Diff(EQUAL, "a"));
        dmp.diff_cleanupSemanticLossless(diffs);
        assertEquals(
                diffList(
                        new diff_match_patch.Diff(EQUAL, "xaa"),
                        new diff_match_patch.Diff(DELETE, "a")),
                diffs,
                "diff_cleanupSemanticLossless: Hitting the end.");

        diffs =
                diffList(
                        new diff_match_patch.Diff(EQUAL, "The xxx. The "),
                        new diff_match_patch.Diff(INSERT, "zzz. The "),
                        new diff_match_patch.Diff(EQUAL, "yyy."));
        dmp.diff_cleanupSemanticLossless(diffs);
        assertEquals(
                diffList(
                        new diff_match_patch.Diff(EQUAL, "The xxx."),
                        new diff_match_patch.Diff(INSERT, " The zzz."),
                        new diff_match_patch.Diff(EQUAL, " The yyy.")),
                diffs,
                "diff_cleanupSemanticLossless: Sentence boundaries.");
    }

    @Test
    void testDiffCleanupSemantic() {
        // Cleanup semantically trivial equalities.
        LinkedList<diff_match_patch.Diff> diffs = diffList();
        dmp.diff_cleanupSemantic(diffs);
        assertEquals(diffList(), diffs, "diff_cleanupSemantic: Null case.");

        diffs =
                diffList(
                        new diff_match_patch.Diff(DELETE, "ab"),
                        new diff_match_patch.Diff(INSERT, "cd"),
                        new diff_match_patch.Diff(EQUAL, "12"),
                        new diff_match_patch.Diff(DELETE, "e"));
        dmp.diff_cleanupSemantic(diffs);
        assertEquals(
                diffList(
                        new diff_match_patch.Diff(DELETE, "ab"),
                        new diff_match_patch.Diff(INSERT, "cd"),
                        new diff_match_patch.Diff(EQUAL, "12"),
                        new diff_match_patch.Diff(DELETE, "e")),
                diffs,
                "diff_cleanupSemantic: No elimination #1.");

        diffs =
                diffList(
                        new diff_match_patch.Diff(DELETE, "abc"),
                        new diff_match_patch.Diff(INSERT, "ABC"),
                        new diff_match_patch.Diff(EQUAL, "1234"),
                        new diff_match_patch.Diff(DELETE, "wxyz"));
        dmp.diff_cleanupSemantic(diffs);
        assertEquals(
                diffList(
                        new diff_match_patch.Diff(DELETE, "abc"),
                        new diff_match_patch.Diff(INSERT, "ABC"),
                        new diff_match_patch.Diff(EQUAL, "1234"),
                        new diff_match_patch.Diff(DELETE, "wxyz")),
                diffs,
                "diff_cleanupSemantic: No elimination #2.");

        diffs =
                diffList(
                        new diff_match_patch.Diff(DELETE, "a"),
                        new diff_match_patch.Diff(EQUAL, "b"),
                        new diff_match_patch.Diff(DELETE, "c"));
        dmp.diff_cleanupSemantic(diffs);
        assertEquals(
                diffList(
                        new diff_match_patch.Diff(DELETE, "abc"),
                        new diff_match_patch.Diff(INSERT, "b")),
                diffs,
                "diff_cleanupSemantic: Simple elimination.");

        diffs =
                diffList(
                        new diff_match_patch.Diff(DELETE, "ab"),
                        new diff_match_patch.Diff(EQUAL, "cd"),
                        new diff_match_patch.Diff(DELETE, "e"),
                        new diff_match_patch.Diff(EQUAL, "f"),
                        new diff_match_patch.Diff(INSERT, "g"));
        dmp.diff_cleanupSemantic(diffs);
        assertEquals(
                diffList(
                        new diff_match_patch.Diff(DELETE, "abcdef"),
                        new diff_match_patch.Diff(INSERT, "cdfg")),
                diffs,
                "diff_cleanupSemantic: Backpass elimination.");

        diffs =
                diffList(
                        new diff_match_patch.Diff(INSERT, "1"),
                        new diff_match_patch.Diff(EQUAL, "A"),
                        new diff_match_patch.Diff(DELETE, "B"),
                        new diff_match_patch.Diff(INSERT, "2"),
                        new diff_match_patch.Diff(EQUAL, "_"),
                        new diff_match_patch.Diff(INSERT, "1"),
                        new diff_match_patch.Diff(EQUAL, "A"),
                        new diff_match_patch.Diff(DELETE, "B"),
                        new diff_match_patch.Diff(INSERT, "2"));
        dmp.diff_cleanupSemantic(diffs);
        assertEquals(
                diffList(
                        new diff_match_patch.Diff(DELETE, "AB_AB"),
                        new diff_match_patch.Diff(INSERT, "1A2_1A2")),
                diffs,
                "diff_cleanupSemantic: Multiple elimination.");

        diffs =
                diffList(
                        new diff_match_patch.Diff(EQUAL, "The c"),
                        new diff_match_patch.Diff(DELETE, "ow and the c"),
                        new diff_match_patch.Diff(EQUAL, "at."));
        dmp.diff_cleanupSemantic(diffs);
        assertEquals(
                diffList(
                        new diff_match_patch.Diff(EQUAL, "The "),
                        new diff_match_patch.Diff(DELETE, "cow and the "),
                        new diff_match_patch.Diff(EQUAL, "cat.")),
                diffs,
                "diff_cleanupSemantic: Word boundaries.");

        diffs =
                diffList(
                        new diff_match_patch.Diff(DELETE, "abcxx"),
                        new diff_match_patch.Diff(INSERT, "xxdef"));
        dmp.diff_cleanupSemantic(diffs);
        assertEquals(
                diffList(
                        new diff_match_patch.Diff(DELETE, "abcxx"),
                        new diff_match_patch.Diff(INSERT, "xxdef")),
                diffs,
                "diff_cleanupSemantic: No overlap elimination.");

        diffs =
                diffList(
                        new diff_match_patch.Diff(DELETE, "abcxxx"),
                        new diff_match_patch.Diff(INSERT, "xxxdef"));
        dmp.diff_cleanupSemantic(diffs);
        assertEquals(
                diffList(
                        new diff_match_patch.Diff(DELETE, "abc"),
                        new diff_match_patch.Diff(EQUAL, "xxx"),
                        new diff_match_patch.Diff(INSERT, "def")),
                diffs,
                "diff_cleanupSemantic: Overlap elimination.");

        diffs =
                diffList(
                        new diff_match_patch.Diff(DELETE, "xxxabc"),
                        new diff_match_patch.Diff(INSERT, "defxxx"));
        dmp.diff_cleanupSemantic(diffs);
        assertEquals(
                diffList(
                        new diff_match_patch.Diff(INSERT, "def"),
                        new diff_match_patch.Diff(EQUAL, "xxx"),
                        new diff_match_patch.Diff(DELETE, "abc")),
                diffs,
                "diff_cleanupSemantic: Reverse overlap elimination.");

        diffs =
                diffList(
                        new diff_match_patch.Diff(DELETE, "abcd1212"),
                        new diff_match_patch.Diff(INSERT, "1212efghi"),
                        new diff_match_patch.Diff(EQUAL, "----"),
                        new diff_match_patch.Diff(DELETE, "A3"),
                        new diff_match_patch.Diff(INSERT, "3BC"));
        dmp.diff_cleanupSemantic(diffs);
        assertEquals(
                diffList(
                        new diff_match_patch.Diff(DELETE, "abcd"),
                        new diff_match_patch.Diff(EQUAL, "1212"),
                        new diff_match_patch.Diff(INSERT, "efghi"),
                        new diff_match_patch.Diff(EQUAL, "----"),
                        new diff_match_patch.Diff(DELETE, "A"),
                        new diff_match_patch.Diff(EQUAL, "3"),
                        new diff_match_patch.Diff(INSERT, "BC")),
                diffs,
                "diff_cleanupSemantic: Two overlap eliminations.");
    }

    @Test
    void testDiffCleanupEfficiency() {
        // Cleanup operationally trivial equalities.
        dmp.Diff_EditCost = 4;
        LinkedList<diff_match_patch.Diff> diffs = diffList();
        dmp.diff_cleanupEfficiency(diffs);
        assertEquals(diffList(), diffs, "diff_cleanupEfficiency: Null case.");

        diffs =
                diffList(
                        new diff_match_patch.Diff(DELETE, "ab"),
                        new diff_match_patch.Diff(INSERT, "12"),
                        new diff_match_patch.Diff(EQUAL, "wxyz"),
                        new diff_match_patch.Diff(DELETE, "cd"),
                        new diff_match_patch.Diff(INSERT, "34"));
        dmp.diff_cleanupEfficiency(diffs);
        assertEquals(
                diffList(
                        new diff_match_patch.Diff(DELETE, "ab"),
                        new diff_match_patch.Diff(INSERT, "12"),
                        new diff_match_patch.Diff(EQUAL, "wxyz"),
                        new diff_match_patch.Diff(DELETE, "cd"),
                        new diff_match_patch.Diff(INSERT, "34")),
                diffs,
                "diff_cleanupEfficiency: No elimination.");

        diffs =
                diffList(
                        new diff_match_patch.Diff(DELETE, "ab"),
                        new diff_match_patch.Diff(INSERT, "12"),
                        new diff_match_patch.Diff(EQUAL, "xyz"),
                        new diff_match_patch.Diff(DELETE, "cd"),
                        new diff_match_patch.Diff(INSERT, "34"));
        dmp.diff_cleanupEfficiency(diffs);
        assertEquals(
                diffList(
                        new diff_match_patch.Diff(DELETE, "abxyzcd"),
                        new diff_match_patch.Diff(INSERT, "12xyz34")),
                diffs,
                "diff_cleanupEfficiency: Four-edit elimination.");

        diffs =
                diffList(
                        new diff_match_patch.Diff(INSERT, "12"),
                        new diff_match_patch.Diff(EQUAL, "x"),
                        new diff_match_patch.Diff(DELETE, "cd"),
                        new diff_match_patch.Diff(INSERT, "34"));
        dmp.diff_cleanupEfficiency(diffs);
        assertEquals(
                diffList(
                        new diff_match_patch.Diff(DELETE, "xcd"),
                        new diff_match_patch.Diff(INSERT, "12x34")),
                diffs,
                "diff_cleanupEfficiency: Three-edit elimination.");

        diffs =
                diffList(
                        new diff_match_patch.Diff(DELETE, "ab"),
                        new diff_match_patch.Diff(INSERT, "12"),
                        new diff_match_patch.Diff(EQUAL, "xy"),
                        new diff_match_patch.Diff(INSERT, "34"),
                        new diff_match_patch.Diff(EQUAL, "z"),
                        new diff_match_patch.Diff(DELETE, "cd"),
                        new diff_match_patch.Diff(INSERT, "56"));
        dmp.diff_cleanupEfficiency(diffs);
        assertEquals(
                diffList(
                        new diff_match_patch.Diff(DELETE, "abxyzcd"),
                        new diff_match_patch.Diff(INSERT, "12xy34z56")),
                diffs,
                "diff_cleanupEfficiency: Backpass elimination.");

        dmp.Diff_EditCost = 5;
        diffs =
                diffList(
                        new diff_match_patch.Diff(DELETE, "ab"),
                        new diff_match_patch.Diff(INSERT, "12"),
                        new diff_match_patch.Diff(EQUAL, "wxyz"),
                        new diff_match_patch.Diff(DELETE, "cd"),
                        new diff_match_patch.Diff(INSERT, "34"));
        dmp.diff_cleanupEfficiency(diffs);
        assertEquals(
                diffList(
                        new diff_match_patch.Diff(DELETE, "abwxyzcd"),
                        new diff_match_patch.Diff(INSERT, "12wxyz34")),
                diffs,
                "diff_cleanupEfficiency: High cost elimination.");
        dmp.Diff_EditCost = 4;
    }

    @Test
    void testDiffPrettyHtml() {
        // Pretty print.
        LinkedList<diff_match_patch.Diff> diffs =
                diffList(
                        new diff_match_patch.Diff(EQUAL, "a\n"),
                        new diff_match_patch.Diff(DELETE, "<B>b</B>"),
                        new diff_match_patch.Diff(INSERT, "c&d"));
        assertEquals(
                "<span>a&para;<br></span><del style=\"background:#ffe6e6;\">&lt;B&gt;b&lt;/B&gt;</del><ins style=\"background:#e6ffe6;\">c&amp;d</ins>",
                dmp.diff_prettyHtml(diffs),
                "diff_prettyHtml:");
    }

    @Test
    void testDiffText() {
        // Compute the source and destination texts.
        LinkedList<diff_match_patch.Diff> diffs =
                diffList(
                        new diff_match_patch.Diff(EQUAL, "jump"),
                        new diff_match_patch.Diff(DELETE, "s"),
                        new diff_match_patch.Diff(INSERT, "ed"),
                        new diff_match_patch.Diff(EQUAL, " over "),
                        new diff_match_patch.Diff(DELETE, "the"),
                        new diff_match_patch.Diff(INSERT, "a"),
                        new diff_match_patch.Diff(EQUAL, " lazy"));
        assertEquals("jumps over the lazy", dmp.diff_text1(diffs), "diff_text1:");
        assertEquals("jumped over a lazy", dmp.diff_text2(diffs), "diff_text2:");
    }

    @Test
    void testDiffDelta() {
        // Convert a diff into delta string.
        LinkedList<diff_match_patch.Diff> diffs =
                diffList(
                        new diff_match_patch.Diff(EQUAL, "jump"),
                        new diff_match_patch.Diff(DELETE, "s"),
                        new diff_match_patch.Diff(INSERT, "ed"),
                        new diff_match_patch.Diff(EQUAL, " over "),
                        new diff_match_patch.Diff(DELETE, "the"),
                        new diff_match_patch.Diff(INSERT, "a"),
                        new diff_match_patch.Diff(EQUAL, " lazy"),
                        new diff_match_patch.Diff(INSERT, "old dog"));
        String text1 = dmp.diff_text1(diffs);
        assertEquals("jumps over the lazy", text1, "diff_text1: Base text.");

        String delta = dmp.diff_toDelta(diffs);
        assertEquals("=4\t-1\t+ed\t=6\t-3\t+a\t=5\t+old dog", delta, "diff_toDelta:");

        // Convert delta string into a diff.
        assertEquals(diffs, dmp.diff_fromDelta(text1, delta), "diff_fromDelta: Normal.");

        // Generates error (19 < 20).
        try {
            dmp.diff_fromDelta(text1 + "x", delta);
            fail("diff_fromDelta: Too long.");
        } catch (IllegalArgumentException ex) {
            // Exception expected.
        }

        // Generates error (19 > 18).
        try {
            dmp.diff_fromDelta(text1.substring(1), delta);
            fail("diff_fromDelta: Too short.");
        } catch (IllegalArgumentException ex) {
            // Exception expected.
        }

        // Generates error (%c3%xy invalid Unicode).
        try {
            dmp.diff_fromDelta("", "+%c3%xy");
            fail("diff_fromDelta: Invalid character.");
        } catch (IllegalArgumentException ex) {
            // Exception expected.
        }

        // Test deltas with special characters.
        diffs =
                diffList(
                        new diff_match_patch.Diff(EQUAL, "\u0680 \000 \t %"),
                        new diff_match_patch.Diff(DELETE, "\u0681 \001 \n ^"),
                        new diff_match_patch.Diff(INSERT, "\u0682 \002 \\ |"));
        text1 = dmp.diff_text1(diffs);
        assertEquals("\u0680 \000 \t %\u0681 \001 \n ^", text1, "diff_text1: Unicode text.");

        delta = dmp.diff_toDelta(diffs);
        assertEquals("=7\t-7\t+%DA%82 %02 %5C %7C", delta, "diff_toDelta: Unicode.");

        assertEquals(diffs, dmp.diff_fromDelta(text1, delta), "diff_fromDelta: Unicode.");

        // Verify pool of unchanged characters.
        diffs =
                diffList(
                        new diff_match_patch.Diff(
                                INSERT, "A-Z a-z 0-9 - _ . ! ~ * ' ( ) ; / ? : @ & = + $ , # "));
        String text2 = dmp.diff_text2(diffs);
        assertEquals(
                "A-Z a-z 0-9 - _ . ! ~ * \' ( ) ; / ? : @ & = + $ , # ",
                text2,
                "diff_text2: Unchanged characters.");

        delta = dmp.diff_toDelta(diffs);
        assertEquals(
                "+A-Z a-z 0-9 - _ . ! ~ * \' ( ) ; / ? : @ & = + $ , # ",
                delta,
                "diff_toDelta: Unchanged characters.");

        // Convert delta string into a diff.
        assertEquals(diffs, dmp.diff_fromDelta("", delta), "diff_fromDelta: Unchanged characters.");

        // 160 kb string.
        String a = "abcdefghij";
        for (int i = 0; i < 14; i++) {
            a += a;
        }
        diffs = diffList(new diff_match_patch.Diff(INSERT, a));
        delta = dmp.diff_toDelta(diffs);
        assertEquals("+" + a, delta, "diff_toDelta: 160kb string.");

        // Convert delta string into a diff.
        assertEquals(diffs, dmp.diff_fromDelta("", delta), "diff_fromDelta: 160kb string.");
    }

    @Test
    void testDiffXIndex() {
        // Translate a location in text1 to text2.
        LinkedList<diff_match_patch.Diff> diffs =
                diffList(
                        new diff_match_patch.Diff(DELETE, "a"),
                        new diff_match_patch.Diff(INSERT, "1234"),
                        new diff_match_patch.Diff(EQUAL, "xyz"));
        assertEquals(5, dmp.diff_xIndex(diffs, 2), "diff_xIndex: Translation on equality.");

        diffs =
                diffList(
                        new diff_match_patch.Diff(EQUAL, "a"),
                        new diff_match_patch.Diff(DELETE, "1234"),
                        new diff_match_patch.Diff(EQUAL, "xyz"));
        assertEquals(1, dmp.diff_xIndex(diffs, 3), "diff_xIndex: Translation on deletion.");
    }

    @Test
    void testDiffLevenshtein() {
        LinkedList<diff_match_patch.Diff> diffs =
                diffList(
                        new diff_match_patch.Diff(DELETE, "abc"),
                        new diff_match_patch.Diff(INSERT, "1234"),
                        new diff_match_patch.Diff(EQUAL, "xyz"));
        assertEquals(
                4,
                dmp.diff_levenshtein(diffs),
                "diff_levenshtein: Levenshtein with trailing equality.");

        diffs =
                diffList(
                        new diff_match_patch.Diff(EQUAL, "xyz"),
                        new diff_match_patch.Diff(DELETE, "abc"),
                        new diff_match_patch.Diff(INSERT, "1234"));
        assertEquals(
                4,
                dmp.diff_levenshtein(diffs),
                "diff_levenshtein: Levenshtein with leading equality.");

        diffs =
                diffList(
                        new diff_match_patch.Diff(DELETE, "abc"),
                        new diff_match_patch.Diff(EQUAL, "xyz"),
                        new diff_match_patch.Diff(INSERT, "1234"));
        assertEquals(
                7,
                dmp.diff_levenshtein(diffs),
                "diff_levenshtein: Levenshtein with middle equality.");
    }

    @Test
    void testDiffBisect() {
        // Normal.
        String a = "cat";
        String b = "map";
        // Since the resulting diff hasn't been normalized, it would be ok if
        // the insertion and deletion pairs are swapped.
        // If the order changes, tweak this test as required.
        LinkedList<diff_match_patch.Diff> diffs =
                diffList(
                        new diff_match_patch.Diff(DELETE, "c"),
                        new diff_match_patch.Diff(INSERT, "m"),
                        new diff_match_patch.Diff(EQUAL, "a"),
                        new diff_match_patch.Diff(DELETE, "t"),
                        new diff_match_patch.Diff(INSERT, "p"));
        assertEquals(diffs, dmp.diff_bisect(a, b, Long.MAX_VALUE), "diff_bisect: Normal.");

        // Timeout.
        diffs =
                diffList(
                        new diff_match_patch.Diff(DELETE, "cat"),
                        new diff_match_patch.Diff(INSERT, "map"));
        assertEquals(diffs, dmp.diff_bisect(a, b, 0), "diff_bisect: Timeout.");
    }

    @Test
    void testDiffMain() {
        // Perform a trivial diff.
        LinkedList<diff_match_patch.Diff> diffs = diffList();
        assertEquals(diffs, dmp.diff_main("", "", false), "diff_main: Null case.");

        diffs = diffList(new diff_match_patch.Diff(EQUAL, "abc"));
        assertEquals(diffs, dmp.diff_main("abc", "abc", false), "diff_main: Equality.");

        diffs =
                diffList(
                        new diff_match_patch.Diff(EQUAL, "ab"),
                        new diff_match_patch.Diff(INSERT, "123"),
                        new diff_match_patch.Diff(EQUAL, "c"));
        assertEquals(diffs, dmp.diff_main("abc", "ab123c", false), "diff_main: Simple insertion.");

        diffs =
                diffList(
                        new diff_match_patch.Diff(EQUAL, "a"),
                        new diff_match_patch.Diff(DELETE, "123"),
                        new diff_match_patch.Diff(EQUAL, "bc"));
        assertEquals(diffs, dmp.diff_main("a123bc", "abc", false), "diff_main: Simple deletion.");

        diffs =
                diffList(
                        new diff_match_patch.Diff(EQUAL, "a"),
                        new diff_match_patch.Diff(INSERT, "123"),
                        new diff_match_patch.Diff(EQUAL, "b"),
                        new diff_match_patch.Diff(INSERT, "456"),
                        new diff_match_patch.Diff(EQUAL, "c"));
        assertEquals(diffs, dmp.diff_main("abc", "a123b456c", false), "diff_main: Two insertions.");

        diffs =
                diffList(
                        new diff_match_patch.Diff(EQUAL, "a"),
                        new diff_match_patch.Diff(DELETE, "123"),
                        new diff_match_patch.Diff(EQUAL, "b"),
                        new diff_match_patch.Diff(DELETE, "456"),
                        new diff_match_patch.Diff(EQUAL, "c"));
        assertEquals(diffs, dmp.diff_main("a123b456c", "abc", false), "diff_main: Two deletions.");

        // Perform a real diff.
        // Switch off the timeout.
        dmp.Diff_Timeout = 0;
        diffs =
                diffList(
                        new diff_match_patch.Diff(DELETE, "a"),
                        new diff_match_patch.Diff(INSERT, "b"));
        assertEquals(diffs, dmp.diff_main("a", "b", false), "diff_main: Simple case #1.");

        diffs =
                diffList(
                        new diff_match_patch.Diff(DELETE, "Apple"),
                        new diff_match_patch.Diff(INSERT, "Banana"),
                        new diff_match_patch.Diff(EQUAL, "s are a"),
                        new diff_match_patch.Diff(INSERT, "lso"),
                        new diff_match_patch.Diff(EQUAL, " fruit."));
        assertEquals(
                diffs,
                dmp.diff_main("Apples are a fruit.", "Bananas are also fruit.", false),
                "diff_main: Simple case #2.");

        diffs =
                diffList(
                        new diff_match_patch.Diff(DELETE, "a"),
                        new diff_match_patch.Diff(INSERT, "\u0680"),
                        new diff_match_patch.Diff(EQUAL, "x"),
                        new diff_match_patch.Diff(DELETE, "\t"),
                        new diff_match_patch.Diff(INSERT, "\000"));
        assertEquals(
                diffs, dmp.diff_main("ax\t", "\u0680x\000", false), "diff_main: Simple case #3.");

        diffs =
                diffList(
                        new diff_match_patch.Diff(DELETE, "1"),
                        new diff_match_patch.Diff(EQUAL, "a"),
                        new diff_match_patch.Diff(DELETE, "y"),
                        new diff_match_patch.Diff(EQUAL, "b"),
                        new diff_match_patch.Diff(DELETE, "2"),
                        new diff_match_patch.Diff(INSERT, "xab"));
        assertEquals(diffs, dmp.diff_main("1ayb2", "abxab", false), "diff_main: Overlap #1.");

        diffs =
                diffList(
                        new diff_match_patch.Diff(INSERT, "xaxcx"),
                        new diff_match_patch.Diff(EQUAL, "abc"),
                        new diff_match_patch.Diff(DELETE, "y"));
        assertEquals(diffs, dmp.diff_main("abcy", "xaxcxabc", false), "diff_main: Overlap #2.");

        diffs =
                diffList(
                        new diff_match_patch.Diff(DELETE, "ABCD"),
                        new diff_match_patch.Diff(EQUAL, "a"),
                        new diff_match_patch.Diff(DELETE, "="),
                        new diff_match_patch.Diff(INSERT, "-"),
                        new diff_match_patch.Diff(EQUAL, "bcd"),
                        new diff_match_patch.Diff(DELETE, "="),
                        new diff_match_patch.Diff(INSERT, "-"),
                        new diff_match_patch.Diff(EQUAL, "efghijklmnopqrs"),
                        new diff_match_patch.Diff(DELETE, "EFGHIJKLMNOefg"));
        assertEquals(
                diffs,
                dmp.diff_main(
                        "ABCDa=bcd=efghijklmnopqrsEFGHIJKLMNOefg", "a-bcd-efghijklmnopqrs", false),
                "diff_main: Overlap #3.");

        diffs =
                diffList(
                        new diff_match_patch.Diff(INSERT, " "),
                        new diff_match_patch.Diff(EQUAL, "a"),
                        new diff_match_patch.Diff(INSERT, "nd"),
                        new diff_match_patch.Diff(EQUAL, " [[Pennsylvania]]"),
                        new diff_match_patch.Diff(DELETE, " and [[New"));
        assertEquals(
                diffs,
                dmp.diff_main("a [[Pennsylvania]] and [[New", " and [[Pennsylvania]]", false),
                "diff_main: Large equality.");

        dmp.Diff_Timeout = 0.1f; // 100ms
        String a =
                "`Twas brillig, and the slithy toves\nDid gyre and gimble in the wabe:\nAll mimsy were the borogoves,\nAnd the mome raths outgrabe.\n";
        String b =
                "I am the very model of a modern major general,\nI've information vegetable, animal, and mineral,\nI know the kings of England, and I quote the fights historical,\nFrom Marathon to Waterloo, in order categorical.\n";
        // Increase the text lengths by 1024 times to ensure a timeout.
        for (int i = 0; i < 10; i++) {
            a += a;
            b += b;
        }
        long startTime = System.currentTimeMillis();
        dmp.diff_main(a, b);
        long endTime = System.currentTimeMillis();
        // Test that we took at least the timeout period.
        assertTrue("diff_main: Timeout min.", dmp.Diff_Timeout * 1000 <= endTime - startTime);
        // Test that we didn't take forever (be forgiving).
        // Theoretically this test could fail very occasionally if the
        // OS task swaps or locks up for a second at the wrong moment.
        assertTrue("diff_main: Timeout max.", dmp.Diff_Timeout * 1000 * 2 > endTime - startTime);
        dmp.Diff_Timeout = 0;

        // Test the linemode speedup.
        // Must be long to pass the 100 char cutoff.
        a =
                "1234567890\n1234567890\n1234567890\n1234567890\n1234567890\n1234567890\n1234567890\n1234567890\n1234567890\n1234567890\n1234567890\n1234567890\n1234567890\n";
        b =
                "abcdefghij\nabcdefghij\nabcdefghij\nabcdefghij\nabcdefghij\nabcdefghij\nabcdefghij\nabcdefghij\nabcdefghij\nabcdefghij\nabcdefghij\nabcdefghij\nabcdefghij\n";
        assertEquals(
                dmp.diff_main(a, b, true),
                dmp.diff_main(a, b, false),
                "diff_main: Simple line-mode.");

        a =
                "1234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890";
        b =
                "abcdefghijabcdefghijabcdefghijabcdefghijabcdefghijabcdefghijabcdefghijabcdefghijabcdefghijabcdefghijabcdefghijabcdefghijabcdefghij";
        assertEquals(
                dmp.diff_main(a, b, true),
                dmp.diff_main(a, b, false),
                "diff_main: Single line-mode.");

        a =
                "1234567890\n1234567890\n1234567890\n1234567890\n1234567890\n1234567890\n1234567890\n1234567890\n1234567890\n1234567890\n1234567890\n1234567890\n1234567890\n";
        b =
                "abcdefghij\n1234567890\n1234567890\n1234567890\nabcdefghij\n1234567890\n1234567890\n1234567890\nabcdefghij\n1234567890\n1234567890\n1234567890\nabcdefghij\n";
        String[] texts_linemode = diff_rebuildtexts(dmp.diff_main(a, b, true));
        String[] texts_textmode = diff_rebuildtexts(dmp.diff_main(a, b, false));
        assertArrayEquals("diff_main: Overlap line-mode.", texts_textmode, texts_linemode);

        // Test null inputs.
        try {
            dmp.diff_main(null, null);
            fail("diff_main: Null inputs.");
        } catch (IllegalArgumentException ex) {
            // Error expected.
        }
    }

    //  MATCH TEST FUNCTIONS

    @Test
    void testMatchAlphabet() {
        // Initialise the bitmasks for Bitap.
        Map<Character, Integer> bitmask;
        bitmask = new HashMap<Character, Integer>();
        bitmask.put('a', 4);
        bitmask.put('b', 2);
        bitmask.put('c', 1);
        assertEquals(bitmask, dmp.match_alphabet("abc"), "match_alphabet: Unique.");

        bitmask = new HashMap<Character, Integer>();
        bitmask.put('a', 37);
        bitmask.put('b', 18);
        bitmask.put('c', 8);
        assertEquals(bitmask, dmp.match_alphabet("abcaba"), "match_alphabet: Duplicates.");
    }

    @Test
    void testMatchBitap() {
        // Bitap algorithm.
        dmp.Match_Distance = 100;
        dmp.Match_Threshold = 0.5f;
        assertEquals(5, dmp.match_bitap("abcdefghijk", "fgh", 5), "match_bitap: Exact match #1.");

        assertEquals(5, dmp.match_bitap("abcdefghijk", "fgh", 0), "match_bitap: Exact match #2.");

        assertEquals(4, dmp.match_bitap("abcdefghijk", "efxhi", 0), "match_bitap: Fuzzy match #1.");

        assertEquals(
                2, dmp.match_bitap("abcdefghijk", "cdefxyhijk", 5), "match_bitap: Fuzzy match #2.");

        assertEquals(-1, dmp.match_bitap("abcdefghijk", "bxy", 1), "match_bitap: Fuzzy match #3.");

        assertEquals(2, dmp.match_bitap("123456789xx0", "3456789x0", 2), "match_bitap: Overflow.");

        assertEquals(0, dmp.match_bitap("abcdef", "xxabc", 4), "match_bitap: Before start match.");

        assertEquals(3, dmp.match_bitap("abcdef", "defyy", 4), "match_bitap: Beyond end match.");

        assertEquals(
                0, dmp.match_bitap("abcdef", "xabcdefy", 0), "match_bitap: Oversized pattern.");

        dmp.Match_Threshold = 0.4f;
        assertEquals(4, dmp.match_bitap("abcdefghijk", "efxyhi", 1), "match_bitap: Threshold #1.");

        dmp.Match_Threshold = 0.3f;
        assertEquals(-1, dmp.match_bitap("abcdefghijk", "efxyhi", 1), "match_bitap: Threshold #2.");

        dmp.Match_Threshold = 0.0f;
        assertEquals(1, dmp.match_bitap("abcdefghijk", "bcdef", 1), "match_bitap: Threshold #3.");

        dmp.Match_Threshold = 0.5f;
        assertEquals(
                0,
                dmp.match_bitap("abcdexyzabcde", "abccde", 3),
                "match_bitap: Multiple select #1.");

        assertEquals(
                8,
                dmp.match_bitap("abcdexyzabcde", "abccde", 5),
                "match_bitap: Multiple select #2.");

        dmp.Match_Distance = 10; // Strict location.
        assertEquals(
                -1,
                dmp.match_bitap("abcdefghijklmnopqrstuvwxyz", "abcdefg", 24),
                "match_bitap: Distance test #1.");

        assertEquals(
                0,
                dmp.match_bitap("abcdefghijklmnopqrstuvwxyz", "abcdxxefg", 1),
                "match_bitap: Distance test #2.");

        dmp.Match_Distance = 1000; // Loose location.
        assertEquals(
                0,
                dmp.match_bitap("abcdefghijklmnopqrstuvwxyz", "abcdefg", 24),
                "match_bitap: Distance test #3.");
    }

    @Test
    void testMatchMain() {
        // Full match.
        assertEquals(0, dmp.match_main("abcdef", "abcdef", 1000), "match_main: Equality.");

        assertEquals(-1, dmp.match_main("", "abcdef", 1), "match_main: Null text.");

        assertEquals(3, dmp.match_main("abcdef", "", 3), "match_main: Null pattern.");

        assertEquals(3, dmp.match_main("abcdef", "de", 3), "match_main: Exact match.");

        assertEquals(3, dmp.match_main("abcdef", "defy", 4), "match_main: Beyond end match.");

        assertEquals(0, dmp.match_main("abcdef", "abcdefy", 0), "match_main: Oversized pattern.");

        dmp.Match_Threshold = 0.7f;
        assertEquals(
                4,
                dmp.match_main("I am the very model of a modern major general.", " that berry ", 5),
                "match_main: Complex match.");
        dmp.Match_Threshold = 0.5f;

        // Test null inputs.
        try {
            dmp.match_main(null, null, 0);
            fail("match_main: Null inputs.");
        } catch (IllegalArgumentException ex) {
            // Error expected.
        }
    }

    //  PATCH TEST FUNCTIONS

    @Test
    void testPatchObj() {
        // Patch Object.
        diff_match_patch.Patch p = new diff_match_patch.Patch();
        p.start1 = 20;
        p.start2 = 21;
        p.length1 = 18;
        p.length2 = 17;
        p.diffs =
                diffList(
                        new diff_match_patch.Diff(EQUAL, "jump"),
                        new diff_match_patch.Diff(DELETE, "s"),
                        new diff_match_patch.Diff(INSERT, "ed"),
                        new diff_match_patch.Diff(EQUAL, " over "),
                        new diff_match_patch.Diff(DELETE, "the"),
                        new diff_match_patch.Diff(INSERT, "a"),
                        new diff_match_patch.Diff(EQUAL, "\nlaz"));
        String strp = "@@ -21,18 +22,17 @@\n jump\n-s\n+ed\n  over \n-the\n+a\n %0Alaz\n";
        assertEquals(strp, p.toString(), "Patch: toString.");
    }

    @Test
    void testPatchFromText() {
        assertTrue("patch_fromText: #0.", dmp.patch_fromText("").isEmpty());

        String strp = "@@ -21,18 +22,17 @@\n jump\n-s\n+ed\n  over \n-the\n+a\n %0Alaz\n";
        assertEquals(strp, dmp.patch_fromText(strp).get(0).toString(), "patch_fromText: #1.");

        assertEquals(
                "@@ -1 +1 @@\n-a\n+b\n",
                dmp.patch_fromText("@@ -1 +1 @@\n-a\n+b\n").get(0).toString(),
                "patch_fromText: #2.");

        assertEquals(
                "@@ -1,3 +0,0 @@\n-abc\n",
                dmp.patch_fromText("@@ -1,3 +0,0 @@\n-abc\n").get(0).toString(),
                "patch_fromText: #3.");

        assertEquals(
                "@@ -0,0 +1,3 @@\n+abc\n",
                dmp.patch_fromText("@@ -0,0 +1,3 @@\n+abc\n").get(0).toString(),
                "patch_fromText: #4.");

        // Generates error.
        try {
            dmp.patch_fromText("Bad\nPatch\n");
            fail("patch_fromText: #5.");
        } catch (IllegalArgumentException ex) {
            // Exception expected.
        }
    }

    @Test
    void testPatchToText() {
        String strp = "@@ -21,18 +22,17 @@\n jump\n-s\n+ed\n  over \n-the\n+a\n  laz\n";
        List<diff_match_patch.Patch> patches;
        patches = dmp.patch_fromText(strp);
        assertEquals(strp, dmp.patch_toText(patches), "patch_toText: Single.");

        strp = "@@ -1,9 +1,9 @@\n-f\n+F\n oo+fooba\n@@ -7,9 +7,9 @@\n obar\n-,\n+.\n  tes\n";
        patches = dmp.patch_fromText(strp);
        assertEquals(strp, dmp.patch_toText(patches), "patch_toText: Dual.");
    }

    @Test
    void testPatchAddContext() {
        dmp.Patch_Margin = 4;
        diff_match_patch.Patch p;
        p = dmp.patch_fromText("@@ -21,4 +21,10 @@\n-jump\n+somersault\n").get(0);
        dmp.patch_addContext(p, "The quick brown fox jumps over the lazy dog.");
        assertEquals(
                "@@ -17,12 +17,18 @@\n fox \n-jump\n+somersault\n s ov\n",
                p.toString(),
                "patch_addContext: Simple case.");

        p = dmp.patch_fromText("@@ -21,4 +21,10 @@\n-jump\n+somersault\n").get(0);
        dmp.patch_addContext(p, "The quick brown fox jumps.");
        assertEquals(
                "@@ -17,10 +17,16 @@\n fox \n-jump\n+somersault\n s.\n",
                p.toString(),
                "patch_addContext: Not enough trailing context.");

        p = dmp.patch_fromText("@@ -3 +3,2 @@\n-e\n+at\n").get(0);
        dmp.patch_addContext(p, "The quick brown fox jumps.");
        assertEquals(
                "@@ -1,7 +1,8 @@\n Th\n-e\n+at\n  qui\n",
                p.toString(),
                "patch_addContext: Not enough leading context.");

        p = dmp.patch_fromText("@@ -3 +3,2 @@\n-e\n+at\n").get(0);
        dmp.patch_addContext(p, "The quick brown fox jumps.  The quick brown fox crashes.");
        assertEquals(
                "@@ -1,27 +1,28 @@\n Th\n-e\n+at\n  quick brown fox jumps. \n",
                p.toString(),
                "patch_addContext: Ambiguity.");
    }

    @SuppressWarnings("deprecation")
    @Test
    void testPatchMake() {
        LinkedList<diff_match_patch.Patch> patches;
        patches = dmp.patch_make("", "");
        assertEquals("", dmp.patch_toText(patches), "patch_make: Null case.");

        String text1 = "The quick brown fox jumps over the lazy dog.";
        String text2 = "That quick brown fox jumped over a lazy dog.";
        String expectedPatch =
                "@@ -1,8 +1,7 @@\n Th\n-at\n+e\n  qui\n@@ -21,17 +21,18 @@\n jump\n-ed\n+s\n  over \n-a\n+the\n  laz\n";
        // The second patch must be "-21,17 +21,18", not "-22,17 +21,18" due to rolling context.
        patches = dmp.patch_make(text2, text1);
        assertEquals(expectedPatch, dmp.patch_toText(patches), "patch_make: Text2+Text1 inputs.");

        expectedPatch =
                "@@ -1,11 +1,12 @@\n Th\n-e\n+at\n  quick b\n@@ -22,18 +22,17 @@\n jump\n-s\n+ed\n  over \n-the\n+a\n  laz\n";
        patches = dmp.patch_make(text1, text2);
        assertEquals(expectedPatch, dmp.patch_toText(patches), "patch_make: Text1+Text2 inputs.");

        LinkedList<diff_match_patch.Diff> diffs = dmp.diff_main(text1, text2, false);
        patches = dmp.patch_make(diffs);
        assertEquals(expectedPatch, dmp.patch_toText(patches), "patch_make: Diff input.");

        patches = dmp.patch_make(text1, diffs);
        assertEquals(expectedPatch, dmp.patch_toText(patches), "patch_make: Text1+Diff inputs.");

        patches = dmp.patch_make(text1, text2, diffs);
        assertEquals(
                expectedPatch,
                dmp.patch_toText(patches),
                "patch_make: Text1+Text2+Diff inputs (deprecated).");

        patches = dmp.patch_make("`1234567890-=[]\\;',./", "~!@#$%^&*()_+{}|:\"<>?");
        assertEquals(
                "@@ -1,21 +1,21 @@\n-%601234567890-=%5B%5D%5C;',./\n+~!@#$%25%5E&*()_+%7B%7D%7C:%22%3C%3E?\n",
                dmp.patch_toText(patches), "patch_toText: Character encoding.");

        diffs =
                diffList(
                        new diff_match_patch.Diff(DELETE, "`1234567890-=[]\\;',./"),
                        new diff_match_patch.Diff(INSERT, "~!@#$%^&*()_+{}|:\"<>?"));
        assertEquals(
                diffs,
                dmp.patch_fromText(
                                "@@ -1,21 +1,21 @@\n-%601234567890-=%5B%5D%5C;',./\n+~!@#$%25%5E&*()_+%7B%7D%7C:%22%3C%3E?\n")
                        .get(0)
                        .diffs,
                "patch_fromText: Character decoding.");

        text1 = "";
        for (int x = 0; x < 100; x++) {
            text1 += "abcdef";
        }
        text2 = text1 + "123";
        expectedPatch = "@@ -573,28 +573,31 @@\n cdefabcdefabcdefabcdefabcdef\n+123\n";
        patches = dmp.patch_make(text1, text2);
        assertEquals(
                expectedPatch, dmp.patch_toText(patches), "patch_make: Long string with repeats.");

        // Test null inputs.
        try {
            dmp.patch_make(null);
            fail("patch_make: Null inputs.");
        } catch (IllegalArgumentException ex) {
            // Error expected.
        }
    }

    @Test
    void testPatchSplitMax() {
        // Assumes that Match_MaxBits is 32.
        LinkedList<diff_match_patch.Patch> patches;
        patches =
                dmp.patch_make(
                        "abcdefghijklmnopqrstuvwxyz01234567890",
                        "XabXcdXefXghXijXklXmnXopXqrXstXuvXwxXyzX01X23X45X67X89X0");
        dmp.patch_splitMax(patches);
        assertEquals(
                "@@ -1,32 +1,46 @@\n+X\n ab\n+X\n cd\n+X\n ef\n+X\n gh\n+X\n ij\n+X\n kl\n+X\n mn\n+X\n op\n+X\n qr\n+X\n st\n+X\n uv\n+X\n wx\n+X\n yz\n+X\n 012345\n@@ -25,13 +39,18 @@\n zX01\n+X\n 23\n+X\n 45\n+X\n 67\n+X\n 89\n+X\n 0\n",
                dmp.patch_toText(patches),
                "patch_splitMax: #1.");

        patches =
                dmp.patch_make(
                        "abcdef1234567890123456789012345678901234567890123456789012345678901234567890uvwxyz",
                        "abcdefuvwxyz");
        String oldToText = dmp.patch_toText(patches);
        dmp.patch_splitMax(patches);
        assertEquals(oldToText, dmp.patch_toText(patches), "patch_splitMax: #2.");

        patches =
                dmp.patch_make(
                        "1234567890123456789012345678901234567890123456789012345678901234567890",
                        "abc");
        dmp.patch_splitMax(patches);
        assertEquals(
                "@@ -1,32 +1,4 @@\n-1234567890123456789012345678\n 9012\n@@ -29,32 +1,4 @@\n-9012345678901234567890123456\n 7890\n@@ -57,14 +1,3 @@\n-78901234567890\n+abc\n",
                dmp.patch_toText(patches),
                "patch_splitMax: #3.");

        patches =
                dmp.patch_make(
                        "abcdefghij , h : 0 , t : 1 abcdefghij , h : 0 , t : 1 abcdefghij , h : 0 , t : 1",
                        "abcdefghij , h : 1 , t : 1 abcdefghij , h : 1 , t : 1 abcdefghij , h : 0 , t : 1");
        dmp.patch_splitMax(patches);
        assertEquals(
                "@@ -2,32 +2,32 @@\n bcdefghij , h : \n-0\n+1\n  , t : 1 abcdef\n@@ -29,32 +29,32 @@\n bcdefghij , h : \n-0\n+1\n  , t : 1 abcdef\n",
                dmp.patch_toText(patches),
                "patch_splitMax: #4.");
    }

    @Test
    void testPatchAddPadding() {
        LinkedList<diff_match_patch.Patch> patches;
        patches = dmp.patch_make("", "test");
        assertEquals(
                "@@ -0,0 +1,4 @@\n+test\n",
                dmp.patch_toText(patches),
                "patch_addPadding: Both edges full.");
        dmp.patch_addPadding(patches);
        assertEquals(
                "@@ -1,8 +1,12 @@\n %01%02%03%04\n+test\n %01%02%03%04\n",
                dmp.patch_toText(patches), "patch_addPadding: Both edges full.");

        patches = dmp.patch_make("XY", "XtestY");
        assertEquals(
                "@@ -1,2 +1,6 @@\n X\n+test\n Y\n",
                dmp.patch_toText(patches),
                "patch_addPadding: Both edges partial.");
        dmp.patch_addPadding(patches);
        assertEquals(
                "@@ -2,8 +2,12 @@\n %02%03%04X\n+test\n Y%01%02%03\n",
                dmp.patch_toText(patches), "patch_addPadding: Both edges partial.");

        patches = dmp.patch_make("XXXXYYYY", "XXXXtestYYYY");
        assertEquals(
                "@@ -1,8 +1,12 @@\n XXXX\n+test\n YYYY\n",
                dmp.patch_toText(patches),
                "patch_addPadding: Both edges none.");
        dmp.patch_addPadding(patches);
        assertEquals(
                "@@ -5,8 +5,12 @@\n XXXX\n+test\n YYYY\n",
                dmp.patch_toText(patches),
                "patch_addPadding: Both edges none.");
    }

    @Test
    void testPatchApply() {
        dmp.Match_Distance = 1000;
        dmp.Match_Threshold = 0.5f;
        dmp.Patch_DeleteThreshold = 0.5f;
        LinkedList<diff_match_patch.Patch> patches;
        patches = dmp.patch_make("", "");
        Object[] results = dmp.patch_apply(patches, "Hello world.");
        boolean[] boolArray = (boolean[]) results[1];
        String resultStr = results[0] + "\t" + boolArray.length;
        assertEquals("Hello world.\t0", resultStr, "patch_apply: Null case.");

        patches =
                dmp.patch_make(
                        "The quick brown fox jumps over the lazy dog.",
                        "That quick brown fox jumped over a lazy dog.");
        results = dmp.patch_apply(patches, "The quick brown fox jumps over the lazy dog.");
        boolArray = (boolean[]) results[1];
        resultStr = results[0] + "\t" + boolArray[0] + "\t" + boolArray[1];
        assertEquals(
                "That quick brown fox jumped over a lazy dog.\ttrue\ttrue",
                resultStr,
                "patch_apply: Exact match.");

        results = dmp.patch_apply(patches, "The quick red rabbit jumps over the tired tiger.");
        boolArray = (boolean[]) results[1];
        resultStr = results[0] + "\t" + boolArray[0] + "\t" + boolArray[1];
        assertEquals(
                "That quick red rabbit jumped over a tired tiger.\ttrue\ttrue",
                resultStr,
                "patch_apply: Partial match.");

        results = dmp.patch_apply(patches, "I am the very model of a modern major general.");
        boolArray = (boolean[]) results[1];
        resultStr = results[0] + "\t" + boolArray[0] + "\t" + boolArray[1];
        assertEquals(
                "I am the very model of a modern major general.\tfalse\tfalse",
                resultStr,
                "patch_apply: Failed match.");

        patches =
                dmp.patch_make(
                        "x1234567890123456789012345678901234567890123456789012345678901234567890y",
                        "xabcy");
        results =
                dmp.patch_apply(
                        patches,
                        "x123456789012345678901234567890-----++++++++++-----123456789012345678901234567890y");
        boolArray = (boolean[]) results[1];
        resultStr = results[0] + "\t" + boolArray[0] + "\t" + boolArray[1];
        assertEquals("xabcy\ttrue\ttrue", resultStr, "patch_apply: Big delete, small change.");

        patches =
                dmp.patch_make(
                        "x1234567890123456789012345678901234567890123456789012345678901234567890y",
                        "xabcy");
        results =
                dmp.patch_apply(
                        patches,
                        "x12345678901234567890---------------++++++++++---------------12345678901234567890y");
        boolArray = (boolean[]) results[1];
        resultStr = results[0] + "\t" + boolArray[0] + "\t" + boolArray[1];
        assertEquals(
                "xabc12345678901234567890---------------++++++++++---------------12345678901234567890y\tfalse\ttrue",
                resultStr,
                "patch_apply: Big delete, big change 1.");

        dmp.Patch_DeleteThreshold = 0.6f;
        patches =
                dmp.patch_make(
                        "x1234567890123456789012345678901234567890123456789012345678901234567890y",
                        "xabcy");
        results =
                dmp.patch_apply(
                        patches,
                        "x12345678901234567890---------------++++++++++---------------12345678901234567890y");
        boolArray = (boolean[]) results[1];
        resultStr = results[0] + "\t" + boolArray[0] + "\t" + boolArray[1];
        assertEquals("xabcy\ttrue\ttrue", resultStr, "patch_apply: Big delete, big change 2.");
        dmp.Patch_DeleteThreshold = 0.5f;

        // Compensate for failed patch.
        dmp.Match_Threshold = 0.0f;
        dmp.Match_Distance = 0;
        patches =
                dmp.patch_make(
                        "abcdefghijklmnopqrstuvwxyz--------------------1234567890",
                        "abcXXXXXXXXXXdefghijklmnopqrstuvwxyz--------------------1234567YYYYYYYYYY890");
        results =
                dmp.patch_apply(
                        patches, "ABCDEFGHIJKLMNOPQRSTUVWXYZ--------------------1234567890");
        boolArray = (boolean[]) results[1];
        resultStr = results[0] + "\t" + boolArray[0] + "\t" + boolArray[1];
        assertEquals(
                "ABCDEFGHIJKLMNOPQRSTUVWXYZ--------------------1234567YYYYYYYYYY890\tfalse\ttrue",
                resultStr,
                "patch_apply: Compensate for failed patch.");
        dmp.Match_Threshold = 0.5f;
        dmp.Match_Distance = 1000;

        patches = dmp.patch_make("", "test");
        String patchStr = dmp.patch_toText(patches);
        dmp.patch_apply(patches, "");
        assertEquals(patchStr, dmp.patch_toText(patches), "patch_apply: No side effects.");

        patches = dmp.patch_make("The quick brown fox jumps over the lazy dog.", "Woof");
        patchStr = dmp.patch_toText(patches);
        dmp.patch_apply(patches, "The quick brown fox jumps over the lazy dog.");
        assertEquals(
                patchStr,
                dmp.patch_toText(patches),
                "patch_apply: No side effects with major delete.");

        patches = dmp.patch_make("", "test");
        results = dmp.patch_apply(patches, "");
        boolArray = (boolean[]) results[1];
        resultStr = results[0] + "\t" + boolArray[0];
        assertEquals("test\ttrue", resultStr, "patch_apply: Edge exact match.");

        patches = dmp.patch_make("XY", "XtestY");
        results = dmp.patch_apply(patches, "XY");
        boolArray = (boolean[]) results[1];
        resultStr = results[0] + "\t" + boolArray[0];
        assertEquals("XtestY\ttrue", resultStr, "patch_apply: Near edge exact match.");

        patches = dmp.patch_make("y", "y123");
        results = dmp.patch_apply(patches, "x");
        boolArray = (boolean[]) results[1];
        resultStr = results[0] + "\t" + boolArray[0];
        assertEquals("x123\ttrue", resultStr, "patch_apply: Edge partial match.");
    }

    private static void assertTrue(String error_msg, boolean a) {
        if (!a) {
            throw new Error("assertTrue fail: " + error_msg);
        }
    }

    private static void assertNull(String error_msg, Object n) {
        if (n != null) {
            throw new Error("assertNull fail: " + error_msg);
        }
    }

    private static void fail(String error_msg) {
        throw new Error("Fail: " + error_msg);
    }

    private static void assertArrayEquals(String error_msg, Object[] a, Object[] b) {
        List<Object> list_a = Arrays.asList(a);
        List<Object> list_b = Arrays.asList(b);
        assertEquals(list_a, list_b, error_msg);
    }

    private static void assertLinesToCharsResultEquals(
            String error_msg,
            diff_match_patch.LinesToCharsResult a,
            diff_match_patch.LinesToCharsResult b) {
        assertEquals(a.chars1, b.chars1, error_msg);
        assertEquals(a.chars2, b.chars2, error_msg);
        assertEquals(a.lineArray, b.lineArray, error_msg);
    }

    // Construct the two texts which made up the diff originally.
    private static String[] diff_rebuildtexts(LinkedList<diff_match_patch.Diff> diffs) {
        String[] text = {"", ""};
        for (diff_match_patch.Diff myDiff : diffs) {
            if (myDiff.operation != diff_match_patch.Operation.INSERT) {
                text[0] += myDiff.text;
            }
            if (myDiff.operation != diff_match_patch.Operation.DELETE) {
                text[1] += myDiff.text;
            }
        }
        return text;
    }

    // Private function for quickly building lists of diffs.
    private static LinkedList<diff_match_patch.Diff> diffList(diff_match_patch.Diff... diffs) {
        return new LinkedList<>(Arrays.asList(diffs));
    }
}
