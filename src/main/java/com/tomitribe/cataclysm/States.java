/*
 * Tomitribe Confidential
 *
 * Copyright Tomitribe Corporation. 2015
 *
 * The source code for this program is not published or otherwise divested 
 * of its trade secrets, irrespective of what has been deposited with the 
 * U.S. Copyright Office.
 */
package com.tomitribe.cataclysm;

import org.tomitribe.util.Join;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;

public class States {
    private States() {
    }

    public static <T> Map<String, State> count(final List<T> states, final Function<T, String> function) {
        final Map<String, State> map = new TreeMap<>();

        for (final T state : states) {
            final String id = function.apply(state).toLowerCase();

            final State count = map.get(id);

            if (count == null) {

                map.put(id, new State(id, 1));

            } else {
                count.incrementAndGet();
            }
        }
        return map;
    }

    public static String printStates(final Map<String, State> states) {
        return printStates(states.values());
    }

    public static String printStates(final Collection<State> states) {
        return Join.join(", ", states);
    }

    public static int get(String name, Map<String, State> states) {
        final State running = states.get(name);
        return running != null ? running.getCount() : 0;
    }
}
