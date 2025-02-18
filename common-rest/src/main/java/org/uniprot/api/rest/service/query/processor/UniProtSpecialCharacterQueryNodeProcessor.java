package org.uniprot.api.rest.service.query.processor;

import java.io.Reader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.CharSequenceReader;
import org.apache.lucene.analysis.charfilter.MappingCharFilter;
import org.apache.lucene.analysis.charfilter.NormalizeCharMap;
import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import org.apache.lucene.queryparser.flexible.core.nodes.FieldQueryNode;
import org.apache.lucene.queryparser.flexible.core.nodes.QueryNode;
import org.apache.lucene.queryparser.flexible.core.processors.QueryNodeProcessorImpl;

public class UniProtSpecialCharacterQueryNodeProcessor extends QueryNodeProcessorImpl {
    private static final Map<Character, String> SPECIAL_CHAR_MAP = createCharMap();

    private static Map<Character, String> createCharMap() {
        Map<Character, String> map = new HashMap<>();
        map.put('\u00C0', "A"); // À
        map.put('\u00C1', "A"); // Á
        map.put('\u00C2', "A"); // Â
        map.put('\u00C3', "A"); // Ã

        map.put('\u00C5', "AA"); // Å

        map.put('\u00C4', "AE"); // Ä
        map.put('\u00C6', "AE"); // Æ

        map.put('\u00C7', "C"); // Ç

        map.put('\u00C8', "E"); // È
        map.put('\u00C9', "E"); // É
        map.put('\u00CA', "E"); // Ê
        map.put('\u00CB', "E"); // Ë

        map.put('\u00CC', "I"); // Ì
        map.put('\u00CD', "I"); // Í
        map.put('\u00CE', "I"); // Î
        map.put('\u00CF', "I"); // Ï

        map.put('\u00D0', "D"); // Ð
        map.put('\u00D1', "N"); // Ñ
        map.put('\u00D2', "O"); // Ò
        map.put('\u00D3', "O"); // Ó
        map.put('\u00D4', "O"); // Ô
        map.put('\u00D5', "O"); // Õ
        map.put('\u00D8', "OE"); // Ø
        map.put('\u00D6', "OE"); // Ö
        map.put('\u0152', "OE"); // Œ
        map.put('\u00DE', "P"); // Þ
        map.put('\u00D9', "U"); // Ù
        map.put('\u00DA', "U"); // Ú
        map.put('\u00DB', "U"); // Û
        map.put('\u00DC', "UE"); // Ü
        map.put('\u00DD', "Y"); // Ý
        map.put('\u0178', "Y"); // Ÿ
        map.put('\u00E0', "a"); // à
        map.put('\u00E1', "a"); // á
        map.put('\u00E2', "a"); // â
        map.put('\u00E3', "a"); // ã
        map.put('\u00E5', "aa"); // å
        map.put('\u00E4', "ae"); // ä
        map.put('\u00E6', "ae"); // æ
        map.put('\u00E7', "c"); // ç
        map.put('\u00E8', "e"); // è
        map.put('\u00E9', "e"); // é
        map.put('\u00EA', "e"); // ê
        map.put('\u00EB', "e"); // ë
        map.put('\u00EC', "i"); // ì
        map.put('\u00ED', "i"); // í
        map.put('\u00EE', "i"); // î
        map.put('\u00EF', "i"); // ï
        map.put('\u00F0', "d"); // ð
        map.put('\u00F1', "n"); // ñ
        map.put('\u00F2', "o"); // ò
        map.put('\u00F3', "o"); // ó
        map.put('\u00F4', "o"); // ô
        map.put('\u00F5', "o"); // õ
        map.put('\u00F8', "oe"); // ø
        map.put('\u00F6', "oe"); // ö
        map.put('\u0153', "oe"); // œ
        map.put('\u00DF', "ss"); // ß
        map.put('\u00FE', "th"); // þ
        map.put('\u00F9', "u"); // ù
        map.put('\u00FA', "u"); // ú
        map.put('\u00FB', "u"); // û
        map.put('\u00FC', "ue"); // ü
        map.put('\u00FD', "y"); // ý
        map.put('\u00FF', "y"); // ÿ
        /* Greek */
        map.put('\u03B1', "alpha"); // alpha
        map.put('\u03B2', "beta"); // beta
        map.put('\u03D0', "beta"); // beta symbol
        map.put('\u03B3', "gamma"); // gamma
        map.put('\u03B4', "delta"); // delta
        map.put('\u03B5', "epsilon"); // epsilon
        map.put('\u03B6', "zeta"); // zeta
        map.put('\u03B7', "eta"); // eta
        map.put('\u03B8', "theta"); // theta
        map.put('\u03B9', "iota"); // iota
        map.put('\u03BA', "kappa"); // kappa
        map.put('\u03BB', "lamda"); // lambda
        map.put('\u03BC', "u"); // mu
        map.put('\u03BD', "nu"); // nu
        map.put('\u03BE', "xi"); // xi
        map.put('\u03BF', "omicron"); // omicron
        map.put('\u03C0', "pi"); // pi
        map.put('\u03C1', "rho"); // rho
        map.put('\u03C2', "sigma"); // final sigma
        map.put('\u03C3', "sigma"); // sigma
        map.put('\u03C4', "tau"); // tau
        map.put('\u03C5', "upsilon"); // upsilon
        map.put('\u03C6', "phi"); // phi
        map.put('\u03C7', "chi"); // chi
        map.put('\u03C8', "psi"); // psi
        map.put('\u03C9', "omega"); // omega
        map.put('\u2122', "(tm)"); // trademark
        return map;
    }

    @Override
    protected QueryNode preProcessNode(QueryNode node) {
        return node;
    }

    @Override
    protected QueryNode postProcessNode(QueryNode node) throws QueryNodeException {
        if (node.isLeaf() && node instanceof FieldQueryNode fieldQueryNode) {
            CharSequence text = fieldQueryNode.getText();
            if (text.chars().anyMatch(i -> SPECIAL_CHAR_MAP.containsKey((char) i))) {
                try (UniProtSpecialCharacterFilter uniProtSpecialCharacterFilter =
                        new UniProtSpecialCharacterFilter(
                                new CharSequenceReader(text), SPECIAL_CHAR_MAP)) {
                    uniProtSpecialCharacterFilter.read();
                    uniProtSpecialCharacterFilter.reset();
                    fieldQueryNode.setText(IOUtils.toString(uniProtSpecialCharacterFilter));
                } catch (Exception e) {
                    throw new QueryNodeException(e);
                }
            }
        }
        return node;
    }

    @Override
    protected List<QueryNode> setChildrenOrder(List<QueryNode> children) {
        return children;
    }

    private static class UniProtSpecialCharacterFilter extends MappingCharFilter {
        /**
         * Default constructor that takes a {@link Reader}.
         *
         * @param in
         */
        public UniProtSpecialCharacterFilter(Reader in, Map<Character, String> map) {
            super(normMap(map), in);
        }

        private static NormalizeCharMap normMap(Map<Character, String> map) {
            NormalizeCharMap.Builder b = new NormalizeCharMap.Builder();
            for (Map.Entry<Character, String> entry : map.entrySet()) {
                b.add("" + entry.getKey(), entry.getValue());
            }
            return b.build();
        }
    }
}
