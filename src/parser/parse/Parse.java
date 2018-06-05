package parser.parse;

import parser.bracketimport.TreebankReader;
import parser.decode.Decode;
import parser.grammar.Grammar;
import parser.grammar.Rule;
import parser.train.Normalize;
import parser.train.Train;
import parser.tree.Tree;
import parser.treebank.Treebank;
import parser.utils.LineWriter;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class Parse {

    /**
     * @author Reut Tsarfaty
     * @date 27 April 2013
     * <p>
     * param train-set
     * param test-set
     * param exp-name
     */

    public static void main(String[] args) {

        //**************************//
        //*      NLP@IDC PA2       *//
        //*   Statistical Parsing  *//
        //*     Point-of-Entry     *//
        //**************************//

        if (args.length < 3) {
            System.out.println("Usage: Parse <goldset> <trainset> <experiment-identifier-string>");
            return;
        }

        // 1. read input
        System.out.println("Read input...");
        Treebank myGoldTreebank = TreebankReader.getInstance().read(true, args[0]);
        Treebank myTrainTreebank = TreebankReader.getInstance().read(true, args[1]);

        int h = -1;
        if (args.length > 3) {
            h = Integer.parseInt(args[3]);
        }

        // 2. transform trees- myTrainTreebank
        System.out.println("Transform train trees...");
        Treebank myBinaryTrainTreebank = new Treebank();
        for (int i = 0; i < myTrainTreebank.size(); i++) {
            Tree myTree = myTrainTreebank.getAnalyses().get(i);//get tree in index i from treebank
            Tree myBinaryTree = Normalize.normalize(myTree, h);
            myBinaryTrainTreebank.add(myBinaryTree);
        }

//        // 2.2 transform trees- myGoldTreebank
//        System.out.println("start transform golden trees...");
//        Treebank myBinaryGoldTreebank = new Treebank();
//        for (int i = 0; i < myGoldTreebank.size(); i++) {
//            Tree myTree = myGoldTreebank.getAnalyses().get(i);//get tree in index i from treebank
//            Tree myBinaryTree = Normalize.normalize(myTree, h);
//            myBinaryGoldTreebank.add(myBinaryTree);
//        }

        // 3. train
        System.out.println("Train grammar...");
        Grammar myGrammar = Train.getInstance().train(myBinaryTrainTreebank);

        // 4. decode
        System.out.println("Decoding golden set (CKY)...");
        List<Tree> myParseTrees = new ArrayList<Tree>();
        for (int i = 0; i < myGoldTreebank.size(); i++) {
            System.out.println("decode: parsing sentence # "+i);
            List<String> mySentence = myGoldTreebank.getAnalyses().get(i).getYield();
            Tree myParseTree = Decode.getInstance(myGrammar).decode(mySentence);
            myParseTrees.add(myParseTree);
        }


        // 5. de-transform ParseTree trees to non binaries form to be comparable with gold set on evaluation phase
//        Tree myBinTree=myBinaryTrainTreebank.getAnalyses().get(0);
//        Tree myBinTreeUnnorm=Normalize.de-normalize(myBinTree);
        System.out.println("De-transform best parsed trees...");
        List<Tree> myParseTreesDetransdormed = new ArrayList<Tree>();
        for (int i = 0; i < myParseTrees.size(); i++) {
            Tree myTree = myParseTrees.get(i);//get tree in index i from treebank
            Tree myParseTreeUnnorm = Normalize.unnormalize(myTree);
            myParseTreesDetransdormed.add(myParseTreeUnnorm);
        }
//// TODO: 23/05/2018 add vertical markovization (Q4) 
//// TODO: 23/05/2018 - change calculation of logprobs under different h=0,1,2... 
        // 6. write output
        writeOutput(args[2], myGrammar, myParseTreesDetransdormed);
    }


    /**
     * Writes output to files:
     * = the trees are written into a .parsed file
     * = the grammar rules are written into a .gram file
     * = the lexicon entries are written into a .lex file
     */
    private static void writeOutput(
            String sExperimentName,
            Grammar myGrammar,
            List<Tree> myTrees) {

        writeParseTrees(sExperimentName, myTrees);
        writeGrammarRules(sExperimentName, myGrammar);
        writeLexicalEntries(sExperimentName, myGrammar);
    }

    /**
     * Writes the parsed trees into a file.
     */
    private static void writeParseTrees(String sExperimentName,
                                        List<Tree> myTrees) {
        LineWriter writer = new LineWriter(sExperimentName + ".parsed");
        for (int i = 0; i < myTrees.size(); i++) {
            writer.writeLine(myTrees.get(i).toString());
        }
        writer.close();
    }

    /**
     * Writes the grammar rules into a file.
     */
    private static void writeGrammarRules(String sExperimentName,
                                          Grammar myGrammar) {
        LineWriter writer;
        writer = new LineWriter(sExperimentName + ".gram");
        Set<Rule> myRules = myGrammar.getSyntacticRules();
        Iterator<Rule> myItrRules = myRules.iterator();
        while (myItrRules.hasNext()) {
            Rule r = (Rule) myItrRules.next();
            writer.writeLine(r.getMinusLogProb() + "\t" + r.getLHS() + "\t" + r.getRHS());
        }
        writer.close();
    }

    /**
     * Writes the lexical entries into a file.
     */
    private static void writeLexicalEntries(String sExperimentName, Grammar myGrammar) {
        LineWriter writer;
        Iterator<Rule> myItrRules;
        writer = new LineWriter(sExperimentName + ".lex");
        Set<String> myEntries = myGrammar.getLexicalEntries().keySet();
        Iterator<String> myItrEntries = myEntries.iterator();
        while (myItrEntries.hasNext()) {
            String myLexEntry = myItrEntries.next();
            StringBuffer sb = new StringBuffer();
            sb.append(myLexEntry);
            sb.append("\t");
            Set<Rule> myLexRules = myGrammar.getLexicalEntries().get(myLexEntry);
            myItrRules = myLexRules.iterator();
            while (myItrRules.hasNext()) {
                Rule r = (Rule) myItrRules.next();
                sb.append(r.getLHS().toString());
                sb.append(" ");
                sb.append(r.getMinusLogProb());
                sb.append(" ");
            }
            writer.writeLine(sb.toString());
        }
    }


}
