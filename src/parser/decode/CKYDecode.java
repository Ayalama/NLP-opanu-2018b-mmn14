package parser.decode;

import parser.grammar.Grammar;
import parser.grammar.Rule;
import parser.tree.Node;
import parser.tree.Tree;
import parser.utils.Triplet;

import java.util.*;

/**
 * Created by aymann on 05/06/2018.
 * Implementation of CKY decoder, given grammer object
 * m_syntacticEnteries- index of grammer rules according to their lhs symbols. key in the map is symbol in grammer, where the value is a set of all rules this sybol apears as one of the rhs symbols.
 */
public class CKYDecode {

    public static Map<String, Set<Rule>> m_syntacticEnteries = null;
    public static Grammar grammar;

    /**
     * Implementation of a singleton pattern
     * Avoids redundant instances in memory
     */
    public static CKYDecode m_singDecoder = null;

    public static CKYDecode getInstance(Grammar g) {
        if (m_singDecoder == null) {
            m_singDecoder = new CKYDecode();
            m_syntacticEnteries = getSyntacticEnteries(g);
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
     *
     * @param labelSearched- expected RHS of the rule
     * @return
     */
    protected List<Rule> getUnaryRulesForWord(String labelSearched) {
        if (!m_syntacticEnteries.containsKey(labelSearched)) return new ArrayList<Rule>();
        Set<Rule> unaryRules = new HashSet<Rule>();
        for (Rule rule : m_syntacticEnteries.get(labelSearched)) {
            String rhsSymbol = rule.getRHS().getSymbols().get(0);
            if (rule.getRHS().getSymbols().size() == 1) {
                unaryRules.add(rule);
            }
        }
        return new ArrayList<Rule>(unaryRules);
    }

    /**
     * inialize scores array (CKY chart)
     *
     * @param inputSize-    number of words in input sentence
     * @param symbolesSize- number of non-terminal symbols in grammar
     * @return
     */
    protected CKYCell[][] initCKYTable(int inputSize, int symbolesSize) {
        CKYCell[][] cells = new CKYCell[inputSize + 1][inputSize + 1];
        for (int i = 0; i < inputSize + 1; i++) {
            for (int j = 0; j < inputSize + 1; j++) {
                cells[i][j] = new CKYCell();
            }
        }
        return cells;
    }

    /**
     * returne the best parsed tree, based on trained grammar according to CKY algorithm
     *
     * @param input
     * @return
     */
    protected Tree ckyDecode(List<String> input) {
        List<String> nonTerminalSymbols = new ArrayList<String>(grammar.getNonTerminalSymbols());
        Map<String, Set<Rule>> lexicalEntries = grammar.getLexicalEntries(); //map of lexical word and all related rules
        CKYCell[][] ckyTable = initCKYTable(input.size(), nonTerminalSymbols.size());

        // Fill in the table's diagonal, the words' tags
        for (int i = 0; i < input.size(); i++) {
            Set<Rule> lexRulesForWord = new HashSet<Rule>();
            int begin = i;
            int end = i + 1;
            CKYCell cell = ckyTable[begin][end];
            String word = input.get(begin);

            if (lexicalEntries.containsKey(word)) {//word is known in training set
                lexRulesForWord.addAll(lexicalEntries.get(word));
            } else {
                lexRulesForWord.addAll(setUnknownWordTag(word, ckyTable, i));
            }
            for (Rule rule : lexRulesForWord) {
                cell.addScore(rule.getLHS().getSymbols().get(0), rule.getMinusLogProb());
            }
            // Add all unary rules that match.
            addUnary(cell);
        }

        // Fill in the rest of the table
        for (int span = 2; span < input.size() + 1; span++) {
            for (int begin = 0; begin < input.size() + 1 - span; begin++) {
                int end = begin + span;
                CKYCell cellHead = ckyTable[begin][end];

                for (int split = begin + 1; split < end; split++) {
                    //for each rule A->BC in grammar
                    CKYCell cellLeft = ckyTable[begin][split];
                    CKYCell cellRight = ckyTable[split][end];

                    //loop to get all rules (A->leftlabel rightlebal) where leftlabel and rightlebal are possible symbols in leftCell and rightCell in accordance
                    for (String labelLeft : cellLeft.getPossibleSymbols()) {
                        if (m_syntacticEnteries.containsKey(labelLeft)) {
                            Set<Rule> rulesLeft = m_syntacticEnteries.get(labelLeft);
                            for (Rule lRule : rulesLeft) {
                                if (lRule.getRHS().getSymbols().size() == 2 && lRule.getRHS().getSymbols().get(0).equals(labelLeft)) {
                                    String labelHead = lRule.getLHS().getSymbols().get(0);
                                    String labelRight = lRule.getRHS().getSymbols().get(1); //right side of rule

                                    if (cellRight.getPossibleSymbols().contains(labelRight)) {//possible symbol A is found for cellHead
                                        double prob = cellLeft.getScore(labelLeft) + cellRight.getScore(labelRight) + lRule.getMinusLogProb();
                                        if (!cellHead.getPossibleSymbols().contains(labelHead) || prob < cellHead.getScore(labelHead)) {
                                            cellHead.addScore(labelHead, prob);
                                            cellHead.addTriplet(labelHead, new Triplet(split, labelLeft, labelRight));
                                        }
                                    }

                                }
                            }
                        }
                    }
                }
                addUnary(cellHead);
            }
        }

        //Get the best tree with respect to minus Logprob
        Node topNode = new Node("TOP");
        Node parsedTreeRoot = buildTree(ckyTable, input, 0, input.size(), grammar.getStartSymbols(), topNode);
        if (parsedTreeRoot != null) {
            parsedTreeRoot.setRoot(Boolean.TRUE);
            topNode.addDaughter(parsedTreeRoot);
            return new Tree(topNode);
        } else {
            return null;
        }
    }

    /**
     * tag unknown words with "NN", with logprob of 0.
     * this is the basic solution as describe in Q4
     * @param word
     * @param ckyTable
     * @param currentWordIdx
     * @return
     */
    protected Set<Rule> setUnknownWordTag(String word, CKYCell[][] ckyTable, int currentWordIdx) {
        Set<Rule> rulesNN = new HashSet<Rule>();
        Rule ruleNN = new Rule("NN", word);
        ruleNN.setMinusLogProb(0.0);
        rulesNN.add(ruleNN);
        return rulesNN;
    }


    /**
     * return a map of syntactic symbols with a set of rule they appear as one of their rhs symbols
     * @param grammar
     * @return
     */
    protected static Map<String, Set<Rule>> getSyntacticEnteries(Grammar grammar) {
        Map<String, Set<Rule>> syntacticEnteries = new HashMap<String, Set<Rule>>();
        for (Rule rule : grammar.getSyntacticRules()) {
            List<String> rhsSymbols = rule.getRHS().getSymbols();
            for (String symbol : rhsSymbols) {
                if (syntacticEnteries.containsKey(symbol)) {
                    syntacticEnteries.get(symbol).add(rule);
                } else {
                    Set<Rule> symbolEntries = new HashSet<Rule>();
                    symbolEntries.add(rule);
                    syntacticEnteries.put(symbol, symbolEntries);
                }
            }
        }
        return syntacticEnteries;
    }

    /**
     * get the CKY chart and build the tree that yield minimun minus log prob. possible symbole to start with is any symbol listed as starting symbol in the grammar
     *
     * @param ckyTable-   array of left side index,s right ide index and chart score for each symbol in the grammar together with Triplet for each symbol in the grammar mentioning the split index, left child and right child
     * @param input-      input sentence
     * @param begin-      index in cky chart to begin with
     * @param end-        index in cky chart to end with
     * @param rootLabels- expected label of current sub-tree
     * @return
     */
    protected Node buildTree(CKYCell[][] ckyTable, List<String> input, int begin, int end, Set<String> rootLabels, Node parentNode) {
        String rootLabel = getBestRootLabel(ckyTable, begin, end, rootLabels);

        if (rootLabel == null) {
            return null; //we don't have root label at the top of the chart
        }

        Node rootNode = new Node(rootLabel);
        rootNode.setParent(parentNode);

        Triplet<Integer, String, String> startTriplet = ckyTable[begin][end].getTriplet(rootLabel);
        if (startTriplet == null) { //Terminal
            // No back pointer. Terminal
            String terminalLabel = input.get(begin);
            rootNode.addDaughter(new Node(terminalLabel));
        } else {
            if (startTriplet.getFirst() == -1) {//Unary rule
                String labelB = startTriplet.getSecond();
                Set<String> labelBset = new HashSet<String>();
                labelBset.add(labelB);
                Node nodeB = buildTree(ckyTable, input, begin, end, labelBset, rootNode);
                rootNode.addDaughter(nodeB);
            } else {// Binary rule
                int split = startTriplet.getFirst();
                String labelB = startTriplet.getSecond();
                Set<String> labelBset = new HashSet<String>();
                labelBset.add(labelB);

                String labelC = startTriplet.getThird();
                Set<String> labelCset = new HashSet<String>();
                labelCset.add(labelC);

                Node nodeB = buildTree(ckyTable, input, begin, split, labelBset, rootNode);
                Node nodeC = buildTree(ckyTable, input, split, end, labelCset, rootNode);
                rootNode.addDaughter(nodeB);
                rootNode.addDaughter(nodeC);
            }

        }

        return rootNode;
    }

    /**
     * locally select the best next tag to go with in the tree, by selecting the tag probiding th local min of minus logproob
     * @param ckyTable
     * @param begin
     * @param end
     * @param rootLabels
     * @return
     */
    protected String getBestRootLabel(CKYCell[][] ckyTable, int begin, int end, Set<String> rootLabels) {
        CKYCell rootCell = ckyTable[begin][end];
        Set<String> possibleSymbols = rootCell.getPossibleSymbols();
        double minScore = Double.POSITIVE_INFINITY;
        String bestRootLabel = null;

        for (String rootLabel : rootLabels) {
            if (possibleSymbols.contains(rootLabel)) {
                double score = rootCell.getScore(rootLabel);
                if (score < minScore) {
                    bestRootLabel = rootLabel;
                    minScore = score;
                }
            }
        }
        return bestRootLabel;
    }

    /**
     * having the input of CKY cell, with all posible binary rules, find inary rules, with respect to this symbols
     * @param cell
     */
    protected void addUnary(CKYCell cell) {
        boolean added = Boolean.TRUE;

        Set<String> nonTerminalSymbols = null;

        while (added) {
            added = Boolean.FALSE;
            //get all possible symboles as candidates for rhs of unary rule
            nonTerminalSymbols = new HashSet<String>(cell.getPossibleSymbols());

            for (String labelB : nonTerminalSymbols) {
                Double scoreB = cell.getScore(labelB);
                if (!(scoreB == null)) {//Label B is a key in the diagonal of the matrix
                    List<Rule> unaryRulesB = getUnaryRulesForWord(labelB);//all rules from the form A->B
                    if (unaryRulesB.size() > 0) {
                        for (Rule rule : unaryRulesB) {
                            double unaryScore = rule.getMinusLogProb() + scoreB;
                            String labelA = rule.getLHS().getSymbols().get(0);
                            Double scoreA = cell.getScore(labelA);
                            if (scoreA == null || unaryScore < scoreA.doubleValue()) {
                                cell.addScore(labelA, unaryScore);
                                cell.addTriplet(labelA, new Triplet(-1, labelB, null)); // i for B's index.
                                added = true;
                            }

                        }
                    }
                }

            }
        }
    }
}
