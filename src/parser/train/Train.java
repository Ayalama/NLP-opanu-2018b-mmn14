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
     *
     * @param myTreebank
     * @return
     */
    public Grammar train(Treebank myTreebank) {
        Grammar myGrammar = new Grammar();
        Map<String, CountMap<String>> nonTerminalBiGram = new HashMap<String, CountMap<String>>();
        Map<String, String> nonTerminalMaxBiGram = new HashMap<String, String>();

        //for each tree in the tree bank
        for (int i = 0; i < myTreebank.size(); i++) {
            Tree myTree = myTreebank.getAnalyses().get(i);//get tree in index i from treebank
            List<Rule> theRules = getRules(myTree); //get tree rules. rules are in the order of sentence words from left to right
            myGrammar.addAll(theRules);

            nonTerminalBiGram = getBiGramForTree(theRules, nonTerminalBiGram);
        }
        updateRulesLogProb(myGrammar);

        // pushing through- smoothing related part
        myGrammar.setBiGramMap(nonTerminalBiGram);
        Map<String, Double> nonTerminalBiGramMaxLogprob=new HashMap<String, Double>();

        for (String nonTerminalSymbol : nonTerminalBiGram.keySet()) {
            String maxRightTerm=nonTerminalBiGram.get(nonTerminalSymbol).maxKey();
            nonTerminalMaxBiGram.put(nonTerminalSymbol, maxRightTerm);

            //calc log-prob
            int totalCountForSynbol=nonTerminalBiGram.get(nonTerminalSymbol).allCounts();
            Double logprob=-Math.log(nonTerminalBiGram.get(nonTerminalSymbol).get(maxRightTerm))+Math.log(totalCountForSynbol);
            nonTerminalBiGramMaxLogprob.put(nonTerminalSymbol,logprob);
        }
        myGrammar.setM_maxBiGramMap(nonTerminalMaxBiGram);
        myGrammar.setM_maxBiGramMapLogprob(nonTerminalBiGramMaxLogprob);

        myGrammar.setM_minusLogProbForTag(getMinusLogProbForTag(myGrammar));
        myGrammar.setM_setNNSymbols(getNNSymbols(myGrammar));

//        //calc log prob to NN tag
//        int totalLexRulesInstances=0;
//        for(Rule rule: myGrammar.getLexicalRules()){
//            int count=myGrammar.getRuleCounts().get(rule);
//            totalLexRulesInstances=totalLexRulesInstances+count;
//        }
//
//        int NNcount=getNNLexicalCount(myGrammar);
//        myGrammar.setNNLogprob(-Math.log(NNcount)+Math.log(totalLexRulesInstances));
//
//        // pushing through- smoothing related part

        return myGrammar;
    }

    /**
     * map of symbol and their minuse log prob to lexical words
     * @param grammar
     * @return
     */
    private Map<String, Double> getMinusLogProbForTag(Grammar grammar){
        CountMap<String> lexCountForSynbol=new CountMap<String>();
        Map<String, Double> lexLogProb=new HashMap<String, Double>();

        //get denominator
        int totalLexRulesInstances=0;
        for(Rule rule: grammar.getLexicalRules()){
            int count=grammar.getRuleCounts().get(rule);
            totalLexRulesInstances=totalLexRulesInstances+count;

            String lhsSymbol = rule.getLHS().getSymbols().get(0);
            lexCountForSynbol.add(lhsSymbol,count);
        }

        for(String symbol:lexCountForSynbol.keySet()){
            double minusLogProb= -Math.log(lexCountForSynbol.get(symbol))+Math.log(totalLexRulesInstances);
            lexLogProb.put(symbol,minusLogProb);
        }
        return lexLogProb;
    }

    /**
     * get all posible symbols outputed as NN
     * @param grammar
     * @return
     */
    private Set<String> getNNSymbols(Grammar grammar){
        Set<String> NNSymbols=new HashSet<String>();

        for(Rule rule: grammar.getLexicalRules()){
            String lhsSymbol = rule.getLHS().getSymbols().get(0);
            if (lhsSymbol.equals("NN") || lhsSymbol.contains("NN^")){
                NNSymbols.add(lhsSymbol);
            }
        }
        return NNSymbols;
    }

    private int getNNLexicalCount(Grammar grammar) {
        Set<Rule> NNRules = new HashSet<Rule>();
        int NNCount=0;
        for (Rule rule : grammar.getLexicalRules()) {
            String lhsSymbol = rule.getLHS().getSymbols().get(0);
            if(lhsSymbol.equals("NN")){
                NNCount=NNCount+grammar.getRuleCounts().get(rule);
            }
        }
        return NNCount;
    }
    /**
     * Map<String,CountMap<String>>- contains tag i as a key and tag i+1 as a key to countmap to hold bi-gram counts for unknown words. used for smoothing
     *
     * @param treeRules
     * @param nonTerminalBiGram
     * @return
     */
    private Map<String, CountMap<String>> getBiGramForTree(List<Rule> treeRules, Map<String, CountMap<String>> nonTerminalBiGram) {
        String leftTag = "";
        String rightTag = "";
        int count = 0;

        for (Rule rule : treeRules) {
            if (rule.isLexical()) {
                if (count == 0) {
                    leftTag = rule.getLHS().getSymbols().get(0);
                    count++;
                } else {
                    rightTag = rule.getLHS().getSymbols().get(0);
                    if (!rightTag.equals(rule.getRHS().getSymbols().get(0))) {
                        if (nonTerminalBiGram.containsKey(leftTag)) {
                            nonTerminalBiGram.get(leftTag).increment(rightTag);
                        } else {
                            CountMap<String> countMap = new CountMap<String>();
                            countMap.increment(rightTag);
                            nonTerminalBiGram.put(leftTag, countMap);
                        }
                    }
                    leftTag = rightTag;
                }
            }
        }

        return nonTerminalBiGram;
    }


    /**
     * get the grammer as input, adn review all it's rules
     * each rules is updates with logprob in accordance to grammer rule counts and the count of instances of the rule precursor
     * update the log probabilities inline for each rule
     *
     * @param myGrammar
     */
    private void updateRulesLogProb(Grammar myGrammar) {
        CountMap theRulesCounts = myGrammar.getRuleCounts();
        Map<String, Integer> nonTerminals = getNonTerminalSymbRulesCount(theRulesCounts);
        for (Rule r : (Set<Rule>) theRulesCounts.keySet()) {
            int ruleCount = theRulesCounts.get(r);
            int topSymbolCount = nonTerminals.get(r.getLHS().getSymbols().get(0));
            double estimatedRuleProb = 0;
            estimatedRuleProb = -1 * Math.log(((double) ruleCount) / topSymbolCount);
            r.setMinusLogProb(estimatedRuleProb);
        }

    }

    private Map<String, Integer> getNonTerminalSymbRulesCount(CountMap myRules) {
        Map<String, Integer> nonTerminalSymbRulesCount = new HashMap<String, Integer>();
        for (Rule r : (Set<Rule>) myRules.keySet()) {
            String topSymbol = r.getLHS().getSymbols().get(0);
            Integer value = nonTerminalSymbRulesCount.get(topSymbol);
            nonTerminalSymbRulesCount.put(topSymbol, value == null ? myRules.get(r) : value + myRules.get(r));
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
                if (myNode.getParent().getLabel().equals("TOP")) {
                    theRule.setTop(Boolean.TRUE);
                }
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
