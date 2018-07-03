package parser.train;

import parser.tree.Node;
import parser.tree.Tree;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by aymann on 18/05/2018.
 * implementation of trees annotation operations:
 * Binarization and de-binarization
 * Parent annotation and removal of parent annotation
 */
public class Normalize {

    /**
     * receive an original tree as input and preformes:
     * normalized Long rules (eg. A->BCD) to binary rules
     * returned tree is a Binary tree
     *
     * @param tree
     * @return
     */
    public static Tree normalizeToBinaryTree(Tree tree, int h) {
        //normalized non binary Nodes in the tree
        Node binaryTreeRoot = binarizeTreeNodes(tree.getRoot(), h);
        return new Tree(binaryTreeRoot);
    }

    /**
     * Main metod for binarization
     * Recursive manner
     *
     * @param root
     * @param h
     * @return
     */
    public static Node binarizeTreeNodes(Node root, int h) {
        if (root.isLeaf()) {
            return root.clone();
        }

        List<Node> binaryDaughters = new ArrayList<Node>();
        for (Node child : root.getDaughters()) {
            binaryDaughters.add(binarizeTreeNodes(child, h));
        }
        Node preBinzrizationRoot = root.clone();

        preBinzrizationRoot.removeAllDaughters();
        preBinzrizationRoot.addAllDaughters(binaryDaughters);
        Node newRoot = binarizeNode(preBinzrizationRoot, h);
        return newRoot;
    }

    /**
     * binarize a single node in the tree in case it is with more than 2 daughters
     *assign labels to new node according to h value
     * @param treeRoot
     * @return
     */
    public static Node binarizeNode(Node treeRoot, int h) {
        Node newRootNode = treeRoot.clone();
        String origeRootLabel = treeRoot.getLabel();

        if (newRootNode.getDaughters().size() > 2) {
            int numOfDaughters = newRootNode.getDaughters().size();
            List<Node> origRootDaughters = treeRoot.getDaughters();
            Node curentParent = newRootNode;

            for (int counter = 0; counter < numOfDaughters - 1; counter++) {
                Node leftSideNode = origRootDaughters.get(counter).clone();
                Node rightSideNode;

                //prepare label of new right side dummy node
                if (counter < numOfDaughters - 2) {
                    String label = getNewLabel(origeRootLabel, origRootDaughters, counter, numOfDaughters, h);
                    rightSideNode = new Node(label);
                } else {//counter==n-2
                    rightSideNode = origRootDaughters.get(counter + 1).clone();
                }

                //update right and left as daughters of parent
                leftSideNode.setParent(curentParent);
                rightSideNode.setParent(curentParent);

                curentParent.removeAllDaughters();
                curentParent.addDaughter(leftSideNode);
                curentParent.addDaughter(rightSideNode);

                curentParent = rightSideNode;
            }
        }
        return newRootNode;
    }


    private static String getNewLabel(String origeRootLabel, List<Node> nodes, int cuurentIndexCounter, int numOfDaughters, int h) {
        StringBuffer sb = new StringBuffer();

        if (h < 0) {
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
        } else {
            sb.append(origeRootLabel).append("@/");
            for (int i = Math.max(0, cuurentIndexCounter - h + 1); i < numOfDaughters && i < cuurentIndexCounter + 1; i++) {
                sb.append(nodes.get(i).getLabel());
                if (i < cuurentIndexCounter && i < numOfDaughters - 1) {
                    sb.append("-");
                }
            }
            sb.append("/");

        }
        return sb.toString();
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


    /**
     * get original tree from a binarized tree
     *
     * @param tree
     * @return
     */
    public static Tree unBinarizeTree(Tree tree) {
        //normalized non binary Nodes in the tree
        List<Node> denormchildrens = unBinarizeTreeNodes(tree.getRoot());
        Node newRoot = denormchildrens.get(0);
        return new Tree(newRoot);
    }

    private static List<Node> unBinarizeTreeNodes(Node root) {
        List<Node> unBinarizeNodes = new ArrayList<Node>();
        if (root.isLeaf()) {
            unBinarizeNodes.add(root.clone());
            return unBinarizeNodes;
        }

        List<Node> nonBinaryDaughters = new ArrayList<Node>();
        for (Node child : root.getDaughters()) {
            nonBinaryDaughters.addAll(unBinarizeTreeNodes(child));
        }
        Node binarizedRoot = root.clone();
        binarizedRoot.removeAllDaughters();
        binarizedRoot.addAllDaughters(nonBinaryDaughters);
        nonBinaryDaughters = unBinarizeNode(binarizedRoot); //if not a dummy root, this will be the original root. otherwise, only his daughetrs
        return nonBinaryDaughters;
    }

    private static List<Node> unBinarizeNode(Node binarizedRoot) {
        //if the node is not a dummy node than return original node without any transformation
        List<Node> unBinarizeNodes = new ArrayList<Node>();
        if (!binarizedRoot.getLabel().contains("@")) {
            unBinarizeNodes.add(binarizedRoot.clone());
            return unBinarizeNodes;
        }
        //clone root daughters list
        for (Node child : binarizedRoot.getDaughters()) {
            unBinarizeNodes.add(child.clone());
        }
        return unBinarizeNodes;
    }

    /**
     * return a tree with parent annotation
     * each Node label A, with parent B, is transformed to Node with identifier A^B
     * @param tree
     * @return
     */
    public static Tree getParentAnnotationTree(Tree tree) {
        Node parentAnnotatedRoot = changeTreeLabel(tree.getRoot(),0);
        return new Tree(parentAnnotatedRoot);
    }

    public static Tree removeParentAnnotationTree(Tree tree) {
        Node removedParentAnnotationRoot = changeTreeLabel(tree.getRoot(),1);
        return new Tree(removedParentAnnotationRoot);
    }

    /**
     * recursively change all nodes labels
     * mode 0 for parent annotation
     * mode 1 for parent annotation removal
     * @param treeRoot
     * @param mode
     * @return
     */
    private static Node changeTreeLabel(Node treeRoot, int mode){
        Node newRoot = treeRoot.clone();

        if (treeRoot.isLeaf()) {
            return newRoot;
        }
        List<Node> newDaughers = new ArrayList<Node>();
        for (int i = 0; i < treeRoot.getDaughters().size(); i++) {
            Node daughter = treeRoot.getDaughters().get(i);
            Node newDaugher = changeTreeLabel(daughter,mode);
            newDaughers.add(newDaugher);
        }
        //set new label
        if(!treeRoot.isRoot()){
            if(mode==0 && !treeRoot.getParent().getLabel().equals("TOP")){
                newRoot.setIdentifier(treeRoot.getIdentifier()+"^"+treeRoot.getParent().getIdentifier());
            }
            if(mode==1 && !treeRoot.getParent().getLabel().equals("TOP")){
                //mode==1
                String[] newId=treeRoot.getIdentifier().split("\\^",-1);
                newRoot.setIdentifier(newId[0]);
            }
        }

        newRoot.removeAllDaughters();
        newRoot.addAllDaughters(newDaughers);

        return newRoot;
    }
}
