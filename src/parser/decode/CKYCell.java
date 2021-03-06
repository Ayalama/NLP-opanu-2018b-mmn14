package parser.decode;

import parser.utils.Triplet;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Created by aymann on 22/05/2018.
 * Represent a single Cell in CKY table
 * Hold 2 structures:
 * m_possibleSymbolsScores for all non terminal symbols that are possible to continue with from this cell, together with the equivalent rule minus-logprob value
 * m_possibleRulesTriplets represents the backpointers of each symbol in the possible symbol map- each triplet is <split value, rhs(0), rhs(1)>
 *
 */
public class CKYCell {
    private Map<String, Double> m_possibleSymbolsScores;
    private Map<String, Triplet<Integer, String, String>> m_possibleRulesTriplets;

    public CKYCell() {
        m_possibleSymbolsScores = new HashMap<String, Double>();
        m_possibleRulesTriplets = new HashMap<String, Triplet<Integer, String, String>>();
    }

    public Double addScore(String symbol, double logProb) {
        return m_possibleSymbolsScores.put(symbol, new Double(logProb));
    }

    public Triplet<Integer, String, String> addTriplet(String symbol, Triplet<Integer, String, String> triplet) {
        return m_possibleRulesTriplets.put(symbol, triplet);
    }

    public Triplet<Integer, String, String> getTriplet(String symbol) {
        return m_possibleRulesTriplets.get(symbol);
    }

    public Double getScore(String symbol) {
        return m_possibleSymbolsScores.get(symbol);
    }


    public Set<String> getPossibleSymbols() {
        return m_possibleSymbolsScores.keySet();
    }
}
