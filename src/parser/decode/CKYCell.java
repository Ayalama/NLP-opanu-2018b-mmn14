package parser.decode;

import parser.grammar.Rule;
import parser.utils.Triplet;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by aymann on 22/05/2018.
 */
public class CKYCell {
    public static Map<Rule,Double> m_possibleRulesScores=null;//HashMap<Object,Double>()
    public static Map<Rule,Triplet<Integer, Object, Object>> m_possibleRulesTriplets=null;//HashMap<Rule,Triplet<Integer, Object, Object>>

    public CKYCell() {
        m_possibleRulesScores=new HashMap<Rule, Double>();
        m_possibleRulesTriplets=new HashMap<Rule, Triplet<Integer, Object, Object>>();
    }

    public double addPosRule(Rule rule) {
        return m_possibleRulesScores.put(rule,rule.getMinusLogProb());
    }


}
