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
        Map<String,Double> biGramLogPMap=grammar.getM_maxBiGramMapLogprob();
        Set<Rule> rules = new HashSet<Rule>();
        boolean NNflg=Boolean.FALSE;

        Rule NNRule= new ArrayList<Rule>(super.setUnknownWordTag(word, ckyTable, currentWordIdx)).get(0);
        NNRule.setMinusLogProb(grammar.getNNLogprob());

        if (currentWordIdx == 0) {
            return super.setUnknownWordTag(word, ckyTable, currentWordIdx);
        }

        CKYCell prevWordCell = ckyTable[currentWordIdx - 1][currentWordIdx];
        Set<String> prevSymbols = prevWordCell.getPossibleSymbols();

        for (String prevSymbol : prevSymbols) {
            if (biGramMap.containsKey(prevSymbol) && prevWordCell.getTriplet(prevSymbol) == null) {
                String candidateSymbols = biGramMap.get(prevSymbol); //most probable symbol where previous tag is prevSymbol
                double logProb= biGramLogPMap.get(prevSymbol);

                if (candidateSymbols.equals("NN")){
                    NNflg=Boolean.TRUE;
                    logProb=Math.min(logProb,grammar.getNNLogprob());
                }

                Rule smoothRule = new Rule(candidateSymbols, word);
                smoothRule.setMinusLogProb(logProb);
                rules.add(smoothRule);
            }
        }

        if (!NNflg) {
            rules.add(NNRule);
        }

        return rules;
    }

}
