/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package exercise2;

import java.util.Comparator;

/**
 *
 * @author Erwin
 */
public class RequestComparator implements Comparator<Request> {

    @Override
    public int compare(Request m1, Request m2) {
        if (m1 == m2) {
            return 0;
        }
        if (m1.timestamp < m2.timestamp) {
            return -1;
        } else if (m1.timestamp > m2.timestamp) {
            return 1;
        } else if (m1.srcID > m2.srcID) {
            return 1;
        } else if (m1.srcID < m2.srcID) {
            return -1;
        } else {
            return 0;
        }
    }
}
