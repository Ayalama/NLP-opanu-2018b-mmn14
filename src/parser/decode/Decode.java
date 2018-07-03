package parser.decode;

import parser.grammar.Grammar;
import parser.grammar.Rule;
import parser.tree.Node;
import parser.tree.Terminal;
import parser.tree.Tree;
import parser.utils.Triplet;

import java.util.*;

public class Decode {

    public static Grammar grammar;

    /**
     * Implementation of a singleton pattern
     * Avoids redundant instances in memory
     */
    public static Decode m_singDecoder = null;

    public static Decode getInstance(Grammar g) {
        if (m_singDecoder == null) {
            m_singDecoder = new Decode();
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


        // CKY decoder - type 1 for Q3-4 decoder. type 2 for decoder with smoothing. type 3 for decoder with parent annotation and smoothing
        Tree ckyTree = null;
        if (type == 1) {
            ckyTree = CKYDecode.getInstance(grammar).decode(input);
        } else if (type == 2 || type == 3) { //include unknown words smoothing
            ckyTree = CKYDecodeExtended.getInstance(grammar).decode(input);
        }
        if (ckyTree == null) {
            return baselineTree;
        } else {
            return ckyTree;
        }


    }
}
