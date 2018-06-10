package parser.decode;

import parser.grammar.Grammar;
import parser.grammar.Rule;
import parser.tree.Node;
import parser.tree.Tree;
import parser.utils.Triplet;

import java.util.*;

/**
 * Created by aymann on 05/06/2018.
 */
public class CKYDecode {

    public static Set<Rule> m_setGrammarRules = null;
    public static Map<String, Set<Rule>> m_mapLexicalRules = null;
    public static Grammar grammar;

    /**
     * Implementation of a singleton pattern
     * Avoids redundant instances in memory
     */
    public static CKYDecode m_singDecoder = null;

    public static CKYDecode getInstance(Grammar g) {
        if (m_singDecoder == null) {
            m_singDecoder = new CKYDecode();
            m_setGrammarRules = g.getSyntacticRules();
            m_mapLexicalRules = g.getLexicalEntries();
            grammar = g;
        }
        return m_singDecoder;
    }

    /**
     * decoding according to CKY algorithem with binary and unary production, according to class grammar.
     * the decoding minimize the sum of minus log probability if the cell items in the chart
     *
     * @param input- sentence to decode
     * @return
     */
    public Tree decode(List<String> input) {

        // CYK decoder
        return ckyDecode(input);

    }

    /**
     * return unary rules that their child is labelSearched, out of given set of rules
     * @param rules- given set of rules to search in
     * @param labelSearched- expected RHS of the rule
     * @return
     */
    private List<Rule> getUnaryRulesForWord(Set<Rule> rules, String labelSearched) {
        if (rules == null) return new ArrayList<Rule>();
        Set<Rule> unaryRules = new HashSet<Rule>();
        for (Rule rule : rules) {
            String rhsSymbol = rule.getRHS().getSymbols().get(0);
            if (rhsSymbol.trim().equals(labelSearched.trim()) && rule.getRHS().getSymbols().size() == 1) {
                unaryRules.add(rule);
            }
        }
        return new ArrayList<Rule>(unaryRules);
    }

    /**
     * inialize scores array (CKY chart)
     * @param inputSize- number of words in input sentence
     * @param symbolesSize- number of non-terminal symbols in grammar
     * @return
     */
    private CKYCell[][] initCKYTable(int inputSize, int symbolesSize) {
        CKYCell[][] cells = new CKYCell[inputSize + 1][inputSize + 1];
        for (int i = 0; i < inputSize+1; i++) {
            for (int j = 0; j < inputSize+1; j++) {
                    cells[i][j] = new CKYCell();
            }
        }
        return cells;
    }

    /**
     * returne the base parsed tree, based on trained grammar according to
     * @param input
     * @return
     */
    private Tree ckyDecode(List<String> input) {
        List<String> nonTerminalSymbols = new ArrayList<String>(grammar.getNonTerminalSymbols());
        CKYCell[][] ckyTable = initCKYTable(input.size(), nonTerminalSymbols.size());

        // Fill in the table's diagonal, the words' tags
        for (int i = 0; i < input.size(); i++) {
            int begin = i;
            int end = i + 1;
            CKYCell cell=ckyTable[begin][end];
            String word = input.get(begin);
            boolean foundRule = Boolean.FALSE;

            for (int symbolIdx = 0; symbolIdx < nonTerminalSymbols.size(); symbolIdx++) {
                String nonTerminal = nonTerminalSymbols.get(symbolIdx);
                List<Rule> rules = getUnaryRulesForWord(m_mapLexicalRules.get(nonTerminal), word);
                if (rules.size() > 0) {
                    Rule lexRule = rules.get(0);
                    double logprob = lexRule.getMinusLogProb();
                    cell.addScore(nonTerminal,logprob);
                    foundRule = Boolean.TRUE;
                }
            }
            if (!foundRule) {
                //if score array not updated for word (all scored are -1) tag word as NN with a score of 0
                cell.addScore("NN",0.0);
            }
            // Add all unary rules that match.
            addUnary(cell);
        }

        // Fill in the rest of the table
        for (int span = 2; span < input.size()+1; span++) {
            for (int begin = 0; begin < input.size() - span+1; begin++) {
                int end = begin + span;
                CKYCell cellA=ckyTable[begin][end];

                for (int split = begin + 1; split < end; split++) {
                    //for each rule A->BC in grammar
                    CKYCell cellLeft=ckyTable[begin][split];
                    CKYCell cellRight=ckyTable[split][end];

                    for (Rule rule : m_setGrammarRules) {

                        if (rule.getRHS().getSymbols().size() == 2) {
                            String labelA = rule.getLHS().getSymbols().get(0);
                            String labelB = rule.getRHS().getSymbols().get(0);
                            String labelC = rule.getRHS().getSymbols().get(1);

                            //// check if B is in left side of scores and C in in right side of scores
                            if (cellLeft.getScore(labelB) != null && cellRight.getScore(labelC) != null) {
                                double prob = cellLeft.getScore(labelB) + cellRight.getScore(labelC) + rule.getMinusLogProb();
                                Double scoreA = cellA.getScore(labelA);
                                if ( scoreA == null || prob < scoreA ) {
                                    cellA.addScore(labelA,prob);
                                    cellA.addTriplet(labelA, new Triplet(split, labelB, labelC));
                                }

                            }
                        }
                    }
                }
                addUnary(cellA);
            }
        }

        Node parsedTreeRoot = buildTree(ckyTable, input, 0, input.size(), "S");

        if(parsedTreeRoot !=null){
            Node top=new Node("TOP");
            top.addDaughter(parsedTreeRoot);
            return new Tree(top);
        }else{
            return null;
        }
    }

    /**
     * get the CKY chart and build the tree that yield minimun minus log prob and starts with S
     * @param ckyTable- array of left side index,s right ide index and chart score for each symbol in the grammar together with Triplet for each symbol in the grammar mentioning the split index, left child and right child
     * @param input- input sentence
     * @param begin- index in cky chart to begin with
     * @param end- index in cky chart to end with
     * @param rootLabel- expected label of current sub-tree
     * @return
     */
    private Node buildTree(CKYCell[][] ckyTable, List<String> input, int begin, int end, String rootLabel) {

        if (ckyTable[begin][end].getScore(rootLabel)== null) {
            return null; //we don't have root label at the top of the chart
        }

        Node rootNode = new Node(rootLabel);

        Triplet<Integer, String, String> startTriplet = ckyTable[begin][end].getTriplet(rootLabel);
        if (startTriplet==null) { //Terminal
            // No back pointer. Terminal
            String terminalLabel = input.get(begin);
            rootNode.addDaughter(new Node(terminalLabel));
        } else {
            if (startTriplet.getFirst() == -1) {//Unary rule
                String labelB = startTriplet.getSecond();
                Node nodeB = buildTree(ckyTable, input, begin, end, labelB);
                rootNode.addDaughter(nodeB);
            } else {// Binary rule
                int split = startTriplet.getFirst();
                String labelB = startTriplet.getSecond();
                String labelC = startTriplet.getThird();

                Node nodeB = buildTree(ckyTable, input, begin, split, labelB);
                Node nodeC = buildTree(ckyTable, input, split, end, labelC);
                rootNode.addDaughter(nodeB);
                rootNode.addDaughter(nodeC);
            }

        }

        return rootNode;
    }

    private void addUnary(CKYCell cell) {
        boolean added = Boolean.TRUE;

        //get all possible symboles as candidates for rhs of unary rule
        Set<String> nonTerminalSymbols = new HashSet<String>(cell.getPossibleSymbols());

        while (added) {
            added = Boolean.FALSE;
            for (String labelB: nonTerminalSymbols) {
                Double scoreB = cell.getScore(labelB);
                if (!(scoreB == null)) {//Label B is a key in the diagonal of the matrix
                    List<Rule> unaryRulesB = getUnaryRulesForWord(m_setGrammarRules, labelB);//all rules from the form A->B
                    if (unaryRulesB.size() > 0) {
                        for (Rule rule : unaryRulesB) {
                            double unaryScore = rule.getMinusLogProb() + scoreB;
                            String labelA = rule.getLHS().getSymbols().get(0);
                            Double scoreA = cell.getScore(labelA);
                            if (scoreA == null || unaryScore < scoreA ) {
                                cell.addScore(labelA,unaryScore);
                                cell.addTriplet(labelA,new Triplet(-1, labelB, null)); // i for B's index.
                                added = true;
                            }

                        }
                    }
                }

            }
        }
    }
}
