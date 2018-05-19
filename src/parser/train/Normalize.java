package parser.train;

import parser.grammar.Grammar;
import parser.grammar.Rule;
import parser.tree.Node;
import parser.tree.Tree;
import parser.utils.CountMap;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by aymann on 18/05/2018.
 */
public class Normalize {

    /**
     * recieve original tree as input and preformes:
     * step 1: Short rules (eg. A->a)
     * step2: normalized Long rules (eg. A->BCD)
     *
     * @param tree
     * @return
     */
    public static Tree normalize(Tree tree) {
        //step 2-normalized non binary Nodes
        Node binaryTreeRoot=binarizeTreeNodes(tree.getRoot());
        return new Tree(binaryTreeRoot);

    }

    public static Node binarizeTreeNodes2(Node root) {
        if (root.isLeaf()) {
            return root.clone();
        }

        Node newRoot = binarizeNode(root);
        List<Node> binaryDaughters = new ArrayList<Node>(2);
        for (Node child : newRoot.getDaughters()) {
            binaryDaughters.add(binarizeTreeNodes(child));
        }

        newRoot.removeAllDaughters();
        newRoot.addAllDaughters(binaryDaughters);

        return newRoot;
    }


    public static Node binarizeTreeNodes(Node root) {
        if (root.isLeaf()) {
            return root.clone();
        }

        List<Node> binaryDaughters = new ArrayList<Node>();
        for (Node child : root.getDaughters()) {
            binaryDaughters.add(binarizeTreeNodes(child));
        }
        Node preBinzrizationRoot = root.clone();
        preBinzrizationRoot.removeAllDaughters();
        preBinzrizationRoot.addAllDaughters(binaryDaughters);
        Node newRoot = binarizeNode(preBinzrizationRoot);
        return newRoot;
    }

    /**
     * binarize a single node in the tree in case it is with more than 2 daughters
     *
     * @param treeRoot
     * @return
     */
    public static Node binarizeNode(Node treeRoot) {
        Node newRootNode = treeRoot.clone();

        if (newRootNode.getDaughters().size() > 2) {
            int numOfDaughters = newRootNode.getDaughters().size();
            List<Node> origRootDaughters = newRootNode.getDaughters();
            Node curentParent = newRootNode;

            for (int counter = 0; counter < numOfDaughters - 1; counter++) {
                Node leftSideNode = origRootDaughters.get(counter).clone();
                Node rightSideNode;

                //prepare label of new right side dummy node
                if (counter < numOfDaughters - 2) {
                    String label = getNewLabel(origRootDaughters, counter, numOfDaughters);
                    rightSideNode = new Node(label);
                } else {//counter==n-2
                    rightSideNode = origRootDaughters.get(counter + 1).clone();
                }

                //update right and left as daughters of parent
                leftSideNode.setParent(curentParent);
                rightSideNode.setParent(curentParent);

                curentParent.addDaughter(leftSideNode);
                curentParent.addDaughter(rightSideNode);

                curentParent = rightSideNode;
            }
        }
        return newRootNode;
    }


    private static String getNewLabel(List<Node> nodes, int cuurentIndexCounter, int numOfDaughters) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < numOfDaughters; i++) {
            sb.append(nodes.get(i).getLabel());
            if (i == cuurentIndexCounter) {
                sb.append("@");
            } else {
                if (i < numOfDaughters - 1) {
                    sb.append("-");
                }
            }
        }
        return sb.toString();
    }


    public static void collapseTreeNodes(Node root) {
        List<Node> daughters = root.getDaughters();

        if (daughters == null) {
            return;
        }
        collapseRootNode(root);
        for (Node daughter : root.getDaughters()) {
            collapseTreeNodes(daughter); //recursive
        }
    }

    public static void collapseRootNode(Node treeRoot) {

        if (treeRoot.getDaughters().size() == 1) {
            String label = treeRoot.getLabel() + '@' + treeRoot.getDaughters().get(0).getLabel();
            List<Node> newRootDaughters = treeRoot.getDaughters().get(0).getDaughters();

            if (newRootDaughters.size() > 0) {
                treeRoot.setIdentifier(label);
                treeRoot.removeDaughter(treeRoot.getDaughters().get(0));

                //add all new daughers to root node and update root as their parant
                for (Node daughter : newRootDaughters) {
                    treeRoot.addDaughter(daughter);
                    daughter.setParent(treeRoot);
                }
            }


        }
        return;
    }

}
