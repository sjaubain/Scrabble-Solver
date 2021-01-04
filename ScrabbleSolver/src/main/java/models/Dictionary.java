package models;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Dictionary represented as a ternary search trie
 */
public class Dictionary {

    final static Logger LOG = Logger.getLogger(Dictionary.class.getName());

    private class Node {
        char value;
        boolean isTerminal;
        Node lo, mi, hi;
        Node(char value) {
            this.value = value;
            this.isTerminal = false;
            lo = mi = hi = null;
        }
    }

    private Node root;

    public Dictionary() {
        this.root = null;
    }

    public void add(String wrd) {
        this.root = add(wrd, 0, this.root);
    }

    private Node add(String wrd, int pos, Node root) {
        if(pos < wrd.length()) {
            char curChar = wrd.charAt(pos);
            if (root == null) {
                root = new Node(curChar);

                // reaching end of world
                if(pos == wrd.length() - 1) {
                    root.isTerminal = true;
                }

                root.mi = add(wrd, pos + 1, root.mi);
            } else {
                if(curChar < root.value) {
                    root.lo = add(wrd, pos, root.lo);
                } else if(curChar > root.value) {
                    root.hi = add(wrd, pos, root.hi);
                } else {
                    root.mi = add(wrd, pos + 1, root.mi);
                }
            }
            return root;
        }
        return null;
    }

    public void load(String filename) {

        LOG.log(Level.INFO, "Starting parsing dictionary");
        int nbWorldAdded = 0;
        try {
            BufferedReader reader = new BufferedReader(new FileReader(filename));
            String line;
            while((line = reader.readLine()) != null) {
                add(line);
                nbWorldAdded++;
            }
            LOG.log(Level.INFO, "Finished parsing " + nbWorldAdded + " worlds");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean isValidWorld(String wrd, boolean onlyPrefix) {

        if(wrd.isEmpty())
            return false;

        int pos = 0; wrd = wrd.toLowerCase();
        Node curNode = root; Node prevNode;
        char curChar;
        do {
            curChar = wrd.charAt(pos);
            prevNode = curNode;
            if(curChar == curNode.value) {
                pos++;
                curNode = curNode.mi;
            } else if(curChar > curNode.value) {
                curNode = curNode.hi;
            } else {
                curNode = curNode.lo;
            }
        } while(pos < wrd.length() && curNode != null);

        return pos == wrd.length() && (prevNode.isTerminal || onlyPrefix);
    }
}
