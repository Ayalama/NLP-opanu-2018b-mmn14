package parser.train;

import parser.grammar.Event;
import parser.grammar.Grammar;
import parser.grammar.Rule;
import parser.tree.Node;
import parser.tree.Tree;
import parser.treebank.Treebank;
import parser.utils.CountMap;

import java.util.*;

/**
 * @author Reut Tsarfaty
 *         <p>
 *         CLASS: Train
 *         <p>
 *         Definition: a learning component
 *         Role: reads off a grammar from a treebank
 *         Responsibility: keeps track of rule counts
 */

public class Train {


    /**
     * Implementation of a singleton pattern
     * Avoids redundant instances in memory
     */
    public static Train m_singTrainer = null;

    public static Train getInstance() {
        if (m_singTrainer == null) {
            m_singTrainer = new Train();
        }
        return m_singTrainer;
    }

    public static void main(String[] args) {

    }

    /**
     * procedure reads off grammar from the tree bank and keeps track of rule-counts + rules log probabilities
     * @param myTreebank
     * @return
     */
    public Grammar train(Treebank myTreebank) {
        Grammar myGrammar = new Grammar();
        //for each tree in the tree bank
        for (int i = 0; i < myTreebank.size(); i++) {
            Tree myTree = myTreebank.getAnalyses().get(i);//get tree in index i from treebank
            List<Rule> theRules = getRules(myTree); //get tree rules
            myGrammar.addAll(theRules);
        }
        updateRulesLogProb(myGrammar);

        return myGrammar;
    }

    /**
     * get the grammer as input, adn review all it's rules
     * each rules is updates with logprob in accordance to grammer rule counts and the count of instances of the rule precursor
     * update the log probabilities inline for each rule
     * @param myGrammar
     */
    private void updateRulesLogProb(Grammar myGrammar) {
        CountMap theRulesCounts = myGrammar.getRuleCounts();
        Map<String,Integer> nonTerminals= getNonTerminalSymbRulesCount(theRulesCounts);
        for(Rule r: (Set<Rule>)theRulesCounts.keySet()){
            int ruleCount=theRulesCounts.get(r);
            int topSymbolCount=nonTerminals.get(r.getLHS().getSymbols().get(0));
            double estimatedRuleProb=0;
            if(!r.getLHS().getSymbols().get(0).contains("@")){
                estimatedRuleProb=-1*Math.log(((double)ruleCount)/topSymbolCount);
            }
            r.setMinusLogProb(estimatedRuleProb);
        }

    }

    private Map<String,Integer> getNonTerminalSymbRulesCount(CountMap myRules) {
        Map<String,Integer> nonTerminalSymbRulesCount=new HashMap<String,Integer>();
        for(Rule r : (Set<Rule>)myRules.keySet()){
            String topSymbol=r.getLHS().getSymbols().get(0); //TODO- validate that the first symbol on the lhs list is the rule node in the tree
            Integer value=nonTerminalSymbRulesCount.get(topSymbol);
            nonTerminalSymbRulesCount.put(topSymbol, value == null ? myRules.get(r) : value+myRules.get(r));
        }
        return nonTerminalSymbRulesCount;
    }

    /**
     * returns the rules of a given tree
     *
     * @param myTree
     * @return
     */
    public List<Rule> getRules(Tree myTree) {
        List<Rule> theRules = new ArrayList<Rule>();

        List<Node> myNodes = myTree.getNodes();
        //for each node in the tree
        for (int j = 0; j < myNodes.size(); j++) {
            Node myNode = myNodes.get(j);
            if (myNode.isInternal()) {
                Event eLHS = new Event(myNode.getIdentifier());
                Iterator<Node> theDaughters = myNode.getDaughters().iterator();
                StringBuffer sb = new StringBuffer();
                while (theDaughters.hasNext()) {
                    Node n = (Node) theDaughters.next();
                    sb.append(n.getIdentifier());
                    if (theDaughters.hasNext())
                        sb.append(" ");
                }
                Event eRHS = new Event(sb.toString());
                Rule theRule = new Rule(eLHS, eRHS); //binary rule
                if (myNode.isPreTerminal())
                    theRule.setLexical(true);
                if (myNode.isRoot())
                    theRule.setTop(true);
                theRules.add(theRule);
            }
        }
        return theRules;
    }

}
