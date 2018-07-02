package parser.decode;

import parser.grammar.Grammar;
import parser.grammar.Rule;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * this class is an extension of CKYDecode, with addition of unknown word smoothing, according to their previous word tags.
 * Created by aymann on 30/06/2018.
 */
public class CKYDecodeExtended extends CKYDecode {

    public static CKYDecodeExtended m_singDecoder = null;

    public static CKYDecodeExtended getInstance(Grammar g) {
        if (m_singDecoder == null) {
            m_singDecoder = new CKYDecodeExtended();
            m_setGrammarRules = g.getSyntacticRules();
            m_mapLexicalRules = g.getLexicalEntries();
            m_syntaxticEnteries = getSyntaxticEnteries(g);
            grammar = g;
        }
        return m_singDecoder;
    }

    protected Set<Rule> setUnknownWordTag(String word, CKYCell[][] ckyTable, int currentWordIdx) {
        Map<String, String> biGramMap = grammar.getM_maxBiGramMap();
        Map<String, Double> biGramLogPMap = grammar.getM_maxBiGramMapLogprob();

        Set<Rule> rules = new HashSet<Rule>();
        boolean NNflg = Boolean.FALSE;

        Set<Rule> NNRules = getNNRules(word);

        if (currentWordIdx == 0) {
            return NNRules;
        }

        CKYCell prevWordCell = ckyTable[currentWordIdx - 1][currentWordIdx];
        Set<String> prevSymbols = prevWordCell.getPossibleSymbols();

        for (String prevSymbol : prevSymbols) {
            if (biGramMap.containsKey(prevSymbol) && prevWordCell.getTriplet(prevSymbol) == null) {
                String candidateSymbol = biGramMap.get(prevSymbol); //most probable symbol where previous tag is prevSymbol
                double logProb = biGramLogPMap.get(prevSymbol);

                if (grammar.getM_setNNSymbols().contains(candidateSymbol)) {
                    NNflg = Boolean.TRUE;
                    logProb = Math.min(logProb, grammar.getM_minusLogProbForTag().get(candidateSymbol));
                }

                Rule smoothRule = new Rule(candidateSymbol, word);
                smoothRule.setMinusLogProb(logProb);
                rules.add(smoothRule);
            }
        }

        if (!NNflg) {
            rules.addAll(NNRules);
        }

        return rules;
    }

    private Set<Rule> getNNRules(String word) {
        Set<Rule> rulesNN = new HashSet<Rule>();
        for (String nnSymbol : grammar.getM_setNNSymbols()) {
            Rule ruleNN = new Rule(nnSymbol, word);
            double minusLogProb = grammar.getM_maxBiGramMapLogprob().get(nnSymbol);
            ruleNN.setMinusLogProb(minusLogProb);
            rulesNN.add(ruleNN);
        }
        return rulesNN;
    }

}
