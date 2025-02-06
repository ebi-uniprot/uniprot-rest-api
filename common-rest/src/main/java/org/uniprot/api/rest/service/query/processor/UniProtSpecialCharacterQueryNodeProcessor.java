package org.uniprot.api.rest.service.query.processor;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.CharSequenceReader;
import org.apache.lucene.analysis.charfilter.MappingCharFilter;
import org.apache.lucene.analysis.charfilter.NormalizeCharMap;
import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import org.apache.lucene.queryparser.flexible.core.nodes.FieldQueryNode;
import org.apache.lucene.queryparser.flexible.core.nodes.QueryNode;
import org.apache.lucene.queryparser.flexible.core.processors.QueryNodeProcessorImpl;

import java.io.Reader;
import java.util.List;

public class UniProtSpecialCharacterQueryNodeProcessor extends QueryNodeProcessorImpl {
    @Override
    protected QueryNode preProcessNode(QueryNode node) {
        return node;
    }

    @Override
    protected QueryNode postProcessNode(QueryNode node) throws QueryNodeException {
        if (node.isLeaf() && node instanceof FieldQueryNode fieldQueryNode) {
            try (UniProtSpecialCharacterFilter uniProtSpecialCharacterFilter =
                         new UniProtSpecialCharacterFilter(new CharSequenceReader(fieldQueryNode.getText()))) {
                uniProtSpecialCharacterFilter.read();
                uniProtSpecialCharacterFilter.reset();
                fieldQueryNode.setText(IOUtils.toString(uniProtSpecialCharacterFilter));
            } catch (Exception e) {
                throw new QueryNodeException(e);
            }
        }
        return node;
    }

    @Override
    protected List<QueryNode> setChildrenOrder(List<QueryNode> children) {
        return children;
    }

    private static class UniProtSpecialCharacterFilter extends MappingCharFilter {
        private static final NormalizeCharMap CHARMAP = normMap();

        /**
         * Default constructor that takes a {@link Reader}.
         *
         * @param in
         */
        public UniProtSpecialCharacterFilter(Reader in) {
            super(CHARMAP, in);
        }

        private static NormalizeCharMap normMap() {
            NormalizeCharMap.Builder b = new NormalizeCharMap.Builder();
            b.add("\u00C0", "A"); // À
            b.add("\u00C1", "A"); // Á
            b.add("\u00C2", "A"); // Â
            b.add("\u00C3", "A"); // Ã

            b.add("\u00C5", "AA"); // Å

            b.add("\u00C4", "AE"); // Ä
            b.add("\u00C6", "AE"); // Æ

            b.add("\u00C7", "C"); // Ç

            b.add("\u00C8", "E"); // È
            b.add("\u00C9", "E"); // É
            b.add("\u00CA", "E"); // Ê
            b.add("\u00CB", "E"); // Ë

            b.add("\u00CC", "I"); // Ì
            b.add("\u00CD", "I"); // Í
            b.add("\u00CE", "I"); // Î
            b.add("\u00CF", "I"); // Ï

            b.add("\u00D0", "D"); // Ð
            b.add("\u00D1", "N"); // Ñ
            b.add("\u00D2", "O"); // Ò
            b.add("\u00D3", "O"); // Ó
            b.add("\u00D4", "O"); // Ô
            b.add("\u00D5", "O"); // Õ
            b.add("\u00D8", "OE"); // Ø
            b.add("\u00D6", "OE"); // Ö
            b.add("\u0152", "OE"); // Œ
            b.add("\u00DE", "P"); // Þ
            b.add("\u00D9", "U"); // Ù
            b.add("\u00DA", "U"); // Ú
            b.add("\u00DB", "U"); // Û
            b.add("\u00DC", "UE"); // Ü
            b.add("\u00DD", "Y"); // Ý
            b.add("\u0178", "Y"); // Ÿ
            b.add("\u00E0", "a"); // à
            b.add("\u00E1", "a"); // á
            b.add("\u00E2", "a"); // â
            b.add("\u00E3", "a"); // ã
            b.add("\u00E5", "aa"); // å
            b.add("\u00E4", "ae"); // ä
            b.add("\u00E6", "ae"); // æ
            b.add("\u00E7", "c"); // ç
            b.add("\u00E8", "e"); // è
            b.add("\u00E9", "e"); // é
            b.add("\u00EA", "e"); // ê
            b.add("\u00EB", "e"); // ë
            b.add("\u00EC", "i"); // ì
            b.add("\u00ED", "i"); // í
            b.add("\u00EE", "i"); // î
            b.add("\u00EF", "i"); // ï
            b.add("\u00F0", "d"); // ð
            b.add("\u00F1", "n"); // ñ
            b.add("\u00F2", "o"); // ò
            b.add("\u00F3", "o"); // ó
            b.add("\u00F4", "o"); // ô
            b.add("\u00F5", "o"); // õ
            b.add("\u00F8", "oe"); // ø
            b.add("\u00F6", "oe"); // ö
            b.add("\u0153", "oe"); // œ
            b.add("\u00DF", "ss"); // ß
            b.add("\u00FE", "th"); // þ
            b.add("\u00F9", "u"); // ù
            b.add("\u00FA", "u"); // ú
            b.add("\u00FB", "u"); // û
            b.add("\u00FC", "ue"); // ü
            b.add("\u00FD", "y"); // ý
            b.add("\u00FF", "y"); // ÿ
            /* Greek */
            b.add("\u03B1", "alpha"); // alpha
            b.add("\u03B2", "beta"); // beta
            b.add("\u03D0", "beta"); // beta symbol
            b.add("\u03B3", "gamma"); // gamma
            b.add("\u03B4", "delta"); // delta
            b.add("\u03B5", "epsilon"); // epsilon
            b.add("\u03B6", "zeta"); // zeta
            b.add("\u03B7", "eta"); // eta
            b.add("\u03B8", "theta"); // theta
            b.add("\u03B9", "iota"); // iota
            b.add("\u03BA", "kappa"); // kappa
            b.add("\u03BB", "lamda"); // lambda
            b.add("\u03BC", "u"); // mu
            b.add("\u03BD", "nu"); // nu
            b.add("\u03BE", "xi"); // xi
            b.add("\u03BF", "omicron"); // omicron
            b.add("\u03C0", "pi"); // pi
            b.add("\u03C1", "rho"); // rho
            b.add("\u03C2", "sigma"); // final sigma
            b.add("\u03C3", "sigma"); // sigma
            b.add("\u03C4", "tau"); // tau
            b.add("\u03C5", "upsilon"); // upsilon
            b.add("\u03C6", "phi"); // phi
            b.add("\u03C7", "chi"); // chi
            b.add("\u03C8", "psi"); // psi
            b.add("\u03C9", "omega"); // omega
            b.add("\u2122", "(tm)"); // trademark
            return b.build();
        }
    }
}
