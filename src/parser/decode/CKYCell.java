package parser.decode;

import parser.utils.Triplet;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Created by aymann on 22/05/2018.
 */
public class CKYCell {
    private static Map<String,Double> m_possibleSymbolsScores;
    private static Map<String,Triplet<Integer, String, String>> m_possibleRulesTriplets;

    public CKYCell() {
        m_possibleSymbolsScores =new HashMap<String, Double>();
        m_possibleRulesTriplets=new HashMap<String, Triplet<Integer, String, String>>();
    }

    public double addScore(String symbol, double logProb) {
        return m_possibleSymbolsScores.put(symbol,logProb);
    }

    public Triplet<Integer, String, String> addTriplet(String symbol, Triplet<Integer, String, String> triplet) {
        return m_possibleRulesTriplets.put(symbol,triplet);
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
