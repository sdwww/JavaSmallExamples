/**
 * @(#)TopSortDemo.java, 9月 28, 2021.
 * <p>
 * Copyright 2021 fenbi.com. All rights reserved.
 * FENBI.COM PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * 拓扑排序
 *
 * @author wangweiwei
 */
public class TopSortDemo {

    private static class Node {
        int value;
        Set<Node> pre;
        Set<Node> next;

        public Node(int value) {
            this.value = value;
            pre = new HashSet<>();
            next = new HashSet<>();
        }

        public void setNext(Node node) {
            this.next.add(node);
        }

        public void removeNext(Node node) {
            this.next.remove(node);
        }

        public void setPre(Node node) {
            this.pre.add(node);
        }

        public void removePre(Node node) {
            this.pre.remove(node);
        }
    }

    private static List<Integer> topSort(List<Node> nodes) {
        LinkedList<Integer> queue = new LinkedList<>();
        LinkedList<Node> stack = new LinkedList<>();
        for (Node node : nodes) {
            if (node.pre.isEmpty()) {
                stack.push(node);
            }
        }
        while (!stack.isEmpty()) {
            Node pop = stack.pop();
            queue.add(pop.value);
            for (Node node : pop.next) {
                node.pre.remove(pop);
                if (node.pre.isEmpty()) {
                    stack.push(node);
                }
            }
        }
        return queue;
    }

    public static void main(String[] args) {
        Node node1 = new Node(1);
        Node node2 = new Node(2);
        Node node3 = new Node(3);
        Node node4 = new Node(4);
        node1.setNext(node2);
        node2.setPre(node1);
        node1.setNext(node3);
        node3.setPre(node1);
        node2.setNext(node4);
        node4.setPre(node2);
        List<Node> list = new ArrayList<>();
        list.add(node1);
        list.add(node2);
        list.add(node3);
        list.add(node4);
        List<Integer> values = topSort(list);
        System.out.println(values);
    }
}