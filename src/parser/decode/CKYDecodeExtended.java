package parser.decode;

import parser.grammar.Grammar;
import parser.grammar.Rule;

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
            m_syntacticEnteries = getSyntacticEnteries(g);
            grammar = g;
        }
        return m_singDecoder;
    }

    /**
     * implementation of unknown words tagging, in decoding
     * used structures;
     * biGramMap- holds the list of tag(i), tag(i+1) where tag(i+1) is the most probable to show after tag(i)
     * biGramLogPMap - holds the list of tag(i) and the -logprog value for tag(i+1) from biGramMap
     *
     * smoothing is looping over all possible symbols from previous word in sentence, as reflected in CKY table
     * pull the most probable tag to follow each and update -logprob of their bigram in CKY table, for current word cell
     *
     * @param word
     * @param ckyTable
     * @param currentWordIdx
     * @return
     */
    protected Set<Rule> setUnknownWordTag(String word, CKYCell[][] ckyTable, int currentWordIdx) {
        Map<String, String> biGramMap = grammar.getMaxBiGramMap();
        Map<String, Double> biGramLogPMap = grammar.getMaxBiGramMapLogprob();

        Set<Rule> rules = new HashSet<Rule>();
        boolean NNflg = Boolean.FALSE;

        Set<Rule> NNRules = getNNRules(word);

        if (currentWordIdx == 0) {
            return NNRules;
        }

        CKYCell prevWordCell = ckyTable[currentWordIdx - 1][currentWordIdx];
        Set<String> prevSymbols = prevWordCell.getPossibleSymbols();

        //loop over all possible symbols from previous word in sentence, as showd in CKY table
        for (String prevSymbol : prevSymbols) {
            if (biGramMap.containsKey(prevSymbol) && prevWordCell.getTriplet(prevSymbol) == null) {
                String candidateSymbol = biGramMap.get(prevSymbol); //most probable symbol where previous tag is prevSymbol
                double logProb = biGramLogPMap.get(prevSymbol);

                if (grammar.getNNSymbolsSet().contains(candidateSymbol)) {
                    NNflg = Boolean.TRUE;
                    logProb = Math.min(logProb, grammar.getMinusLogProbForTag().get(candidateSymbol));
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
        for (String nnSymbol : grammar.getNNSymbolsSet()) {
            Rule ruleNN = new Rule(nnSymbol, word);
            double minusLogProb = grammar.getMaxBiGramMapLogprob().get(nnSymbol);
            ruleNN.setMinusLogProb(minusLogProb);
            rulesNN.add(ruleNN);
        }
        return rulesNN;
    }

}
