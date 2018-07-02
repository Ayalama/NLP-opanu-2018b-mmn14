package parser.train;

import parser.tree.Node;
import parser.tree.Tree;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by aymann on 18/05/2018.
 */
public class Normalize {

    /**
     * receive an original tree as input and preformes:
     * normalized Long rules (eg. A->BCD) to binary rules
     * returned tree is a Binary tree
     * TODO- add input parameter indicating the number of sisters encoded in the memory of added nodes (currently all sisters are encoded)
     *
     * @param tree
     * @return
     */
    public static Tree normalizeToBinaryTree(Tree tree, int h) {
        //normalized non binary Nodes in the tree
        Node binaryTreeRoot = binarizeTreeNodes(tree.getRoot(), h);
        return new Tree(binaryTreeRoot);
    }


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
     *
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


    /**
     * get original tree from a binarized tree
     * TODO- check what if returned list contains the actual root?!
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


    public static Tree getParentAnnotationTree(Tree tree) {
        Node parentAnnotatedRoot = getTreeNodesBottomUp(tree.getRoot());
        return new Tree(parentAnnotatedRoot);
    }

    public static Tree removeParentAnnotationTree(Tree tree) {
        Node removedParentAnnotationRoot = unAnnotateTree(tree.getRoot());
        return new Tree(removedParentAnnotationRoot);
    }

    private static Node getTreeNodesBottomUp(Node treeRoot) {
        return changeTreeLabel(treeRoot,0);
    }


    public static Node unAnnotateTree(Node annotatedTreeRoot) {
        return changeTreeLabel(annotatedTreeRoot,1);
    }

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
