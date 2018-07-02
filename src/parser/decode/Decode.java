package parser.decode;

import parser.grammar.Grammar;
import parser.grammar.Rule;
import parser.tree.Node;
import parser.tree.Terminal;
import parser.tree.Tree;
import parser.utils.Triplet;

import java.util.*;

public class Decode {

    public static Set<Rule> m_setGrammarRules = null;
    public static Map<String, Set<Rule>> m_mapLexicalRules = null;
    public static Grammar grammar;

    /**
     * Implementation of a singleton pattern
     * Avoids redundant instances in memory
     */
    public static Decode m_singDecoder = null;

    public static Decode getInstance(Grammar g) {
        if (m_singDecoder == null) {
            m_singDecoder = new Decode();
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
    public Tree decode(List<String> input, int type) {

        // Done: Baseline Decoder
        //       Returns a flat tree with NN labels on all leaves

        Tree baselineTree = new Tree(new Node("TOP"));
        Iterator<String> theInput = input.iterator();
        while (theInput.hasNext()) {
            String theWord = (String) theInput.next();
            Node preTerminal = new Node("NN");
            Terminal terminal = new Terminal(theWord);
            preTerminal.addDaughter(terminal);
            baselineTree.getRoot().addDaughter(preTerminal);
        }


        // CKY decoder - type 1 for Q3-4 decoder. type 2 for decoder with smoothing. type 3 for decoder with...
        Tree ckyTree = null;
        if (type == 1) {
            ckyTree = CKYDecode.getInstance(grammar).decode(input);
        } else if (type == 2) { //include unknown words smoothing
            ckyTree = CKYDecodeExtended.getInstance(grammar).decode(input);
        }
        if (ckyTree == null) {
            return baselineTree;
        } else {
            return ckyTree;
        }

//        // CYK decoder
//        Tree ckyTree = ckyDecode(input);
//        if (ckyTree == null) {
//            return baselineTree;
//        } else {
//            return ckyTree;
//        }
    }

    /**
     * return unary rules that their child is labelSearched, out of given set of rules
     *
     * @param rules-         given set of rules to search in
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
     * inialize back pointers array(CKY chart)
     *
     * @param inputSize-    number of words in input sentence
     * @param symbolesSize- number of non-terminal symbols in grammar
     * @return
     */
    private Triplet[][][] initBacks(int inputSize, int symbolesSize) {
        Triplet[][][] back = new Triplet[inputSize + 1][inputSize + 1][symbolesSize];
        for (int i = 0; i < inputSize; i++) {
            for (int j = 0; j < inputSize; j++) {
                for (int z = 0; z < symbolesSize; z++) {
                    back[i][j][z] = new Triplet<Integer, Integer, Integer>(-1, -1, -1);
                }
            }
        }
        return back;

    }

    /**
     * inialize scores array (CKY chart)
     *
     * @param inputSize-    number of words in input sentence
     * @param symbolesSize- number of non-terminal symbols in grammar
     * @return
     */
    private double[][][] initScores(int inputSize, int symbolesSize) {
        double[][][] score = new double[inputSize + 1][inputSize + 1][symbolesSize];
        for (int i = 0; i < inputSize; i++) {
            for (int j = 0; j < inputSize; j++) {
                for (int z = 0; z < symbolesSize; z++) {
                    score[i][j][z] = Double.POSITIVE_INFINITY;
                }
            }
        }
        return score;
    }

    /**
     * returne the base parsed tree, based on trained grammar according to
     *
     * @param input
     * @return
     */
    private Tree ckyDecode(List<String> input) {
        List<String> nonTerminalSymbols = new ArrayList<String>(grammar.getNonTerminalSymbols());
        double[][][] score = initScores(input.size(), nonTerminalSymbols.size());
        Triplet[][][] back = initBacks(input.size(), nonTerminalSymbols.size());

        // Fill in the table's diagonal, the words' tags
        for (int i = 0; i < input.size(); i++) {
            int begin = i;
            int end = i + 1;
            String word = input.get(begin);
            boolean foundRule = Boolean.FALSE;

            for (int symbolIdx = 0; symbolIdx < nonTerminalSymbols.size(); symbolIdx++) {
                String nonTerminal = nonTerminalSymbols.get(symbolIdx);
                List<Rule> rules = getUnaryRulesForWord(m_mapLexicalRules.get(nonTerminal), word);
                if (rules.size() > 0) {
                    Rule lexRule = rules.get(0);
                    score[begin][end][symbolIdx] = lexRule.getMinusLogProb();
                    foundRule = Boolean.TRUE;
                }
            }
            if (!foundRule) {
                //if score array not updated for word (all scored are -1) tag word as NN with a score of 0
                score[begin][end][nonTerminalSymbols.indexOf("NN")] = 0.0;
            }
            // Add all unary rules that match.
            addUnary(score, back, begin, end, nonTerminalSymbols);
        }

        // Fill in the rest of the table
        for (int span = 2; span < input.size(); span++) {
            for (int begin = 0; begin < input.size() - span; begin++) {
                int end = begin + span;
                for (int split = begin + 1; split < end; split++) {
                    //for each rule A->BC in grammar
                    double[] scoresleft = score[begin][split];
                    double[] scoresright = score[split][end];

                    for (Rule rule : m_setGrammarRules) {

                        if (rule.getRHS().getSymbols().size() == 2) {
                            String labelA = rule.getLHS().getSymbols().get(0);
                            String labelB = rule.getRHS().getSymbols().get(0);
                            String labelC = rule.getRHS().getSymbols().get(1);
                            //// check if B is in left side of scores and C in in right side of scores
                            if (score[begin][split][nonTerminalSymbols.indexOf(labelB)] != Double.POSITIVE_INFINITY && score[split][end][nonTerminalSymbols.indexOf(labelC)] != Double.POSITIVE_INFINITY) {
                                double prob = score[begin][split][nonTerminalSymbols.indexOf(labelB)] + score[split][end][nonTerminalSymbols.indexOf(labelC)] + rule.getMinusLogProb();
                                double scoreA = score[begin][end][nonTerminalSymbols.indexOf(labelA)];
                                if (prob < scoreA || scoreA == Double.POSITIVE_INFINITY) {
                                    score[begin][end][nonTerminalSymbols.indexOf(labelA)] = prob;
                                    back[begin][end][nonTerminalSymbols.indexOf(labelA)] = new Triplet(split, nonTerminalSymbols.indexOf(labelB), nonTerminalSymbols.indexOf(labelC));
                                }

                            }
                        }
                    }
                }
                addUnary(score, back, begin, end, nonTerminalSymbols);
            }
        }
        //print top of scores
//        System.out.println("Top of chart symbols and score: ");
//        double[] symbleScores = score[0][input.size() - 1];
//        for (int i = 0; i < nonTerminalSymbols.size(); i++) {
//            if (symbleScores[i] !=Double.POSITIVE_INFINITY) {
//                String symbol = nonTerminalSymbols.get(i);
//                System.out.println(symbol + " score:" + symbleScores[i]);
//            }
//        }

        Node parsedTreeRoot = buildTree(score, back, nonTerminalSymbols, input, 0, input.size() - 1, "S");
        if (parsedTreeRoot != null) {
            Node top = new Node("TOP");
            top.addDaughter(parsedTreeRoot);
            return new Tree(top);
        } else {
            return null;
        }

    }

    /**
     * get the CKY chart and build the tree that yield minimun minus log prob and starts with S
     *
     * @param score-              array of left side index, right side index and chart score for each symbol in the grammar
     * @param back-               array of left side index, right side index and a Triplet for each symbol in the grammar mentioning the split index, left child and right child
     * @param nonTerminalSymbols- grammar non-terminal symbols
     * @param input-              input sentence
     * @param begin-              index in cky chart to begin with
     * @param end-                index in cky chart to end with
     * @param rootLabel-          expected label of current sub-tree
     * @return
     */
    private Node buildTree(double[][][] score, Triplet[][][] back, List<String> nonTerminalSymbols, List<String> input, int begin, int end, String rootLabel) {
        if (score[begin][end][nonTerminalSymbols.indexOf(rootLabel)] == Double.POSITIVE_INFINITY) {
            return null; //we don't have root label at the top of the chart
        }

        Node rootNode = new Node(rootLabel);

        Triplet<Integer, Integer, Integer> startTriplet = back[begin][end][nonTerminalSymbols.indexOf(rootLabel)];
        if (startTriplet.getFirst() == -1 && startTriplet.getSecond() == -1 && startTriplet.getThird() == -1) { //Terminal
            // No back pointer. Terminal
            String terminalLabel = input.get(begin);
            rootNode.addDaughter(new Node(terminalLabel));
        } else {
            if (startTriplet.getFirst() == -1) {//Unary rule
                int idxB = startTriplet.getSecond();
                Node nodeB = buildTree(score, back, nonTerminalSymbols, input, begin, end, nonTerminalSymbols.get(idxB));
                rootNode.addDaughter(nodeB);
            } else {// Binary rule
                int split = startTriplet.getFirst();
                int idxB = startTriplet.getSecond();
                int idxC = startTriplet.getThird();

                Node nodeB = buildTree(score, back, nonTerminalSymbols, input, begin, split, nonTerminalSymbols.get(idxB));
                Node nodeC = buildTree(score, back, nonTerminalSymbols, input, split, end, nonTerminalSymbols.get(idxC));
                rootNode.addDaughter(nodeB);
                rootNode.addDaughter(nodeC);
            }

        }

        return rootNode;
    }

    private void addUnary(double[][][] score, Triplet[][][] back, int begin, int end, List<String> nonTerminalSymbols) {
        boolean added = Boolean.TRUE;
        while (added) {
            added = Boolean.FALSE;
            for (int i = 0; i < nonTerminalSymbols.size(); i++) {
                String labelB = nonTerminalSymbols.get(i);
                double scoreB = score[begin][end][i];
                if (!(scoreB == Double.POSITIVE_INFINITY)) {//Label B is a key in the diagonal of the matrix
                    List<Rule> unaryRulesB = getUnaryRulesForWord(m_setGrammarRules, labelB);//all rules from the form A->B
                    if (unaryRulesB.size() > 0) {
                        for (Rule rule : unaryRulesB) {
                            double unaryScore = rule.getMinusLogProb() + scoreB;
                            String labelA = rule.getLHS().getSymbols().get(0);
                            double scoreA = score[begin][end][nonTerminalSymbols.indexOf(labelA)];
                            if (unaryScore < scoreA || scoreA == Double.POSITIVE_INFINITY) {
                                score[begin][end][nonTerminalSymbols.indexOf(labelA)] = unaryScore;
                                back[begin][end][nonTerminalSymbols.indexOf(labelA)] = new Triplet(-1, i, null);  // i for B's index.
                                added = true;
                            }

                        }
                    }
                }

            }
        }
    }
}
