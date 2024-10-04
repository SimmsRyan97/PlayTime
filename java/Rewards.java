package com.whiteiverson.minecraft.playtime_plugin;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Represents a reward based on play time.
 */
public class Rewards implements Comparable<Rewards> {
    private final String name;
    private final double time;
    private final List<String> commands;

    /**
     * Constructs a Rewards object.
     *
     * @param name     the name of the reward
     * @param time     the time required to earn the reward
     * @param commands a list of commands associated with the reward
     */
    public Rewards(String name, double time, List<String> commands) {
        if (time < 0) {
            throw new IllegalArgumentException("Time cannot be negative");
        }
        this.name = name;
        this.time = time;
        this.commands = Collections.unmodifiableList(commands); // Make commands immutable
    }

    public String getName() {
        return name;
    }

    public double getTime() {
        return time;
    }

    public List<String> getCommands() {
        return commands;
    }

    @Override
    public int compareTo(Rewards other) {
        return Double.compare(this.time, other.time); // Ascending order
        // If you want descending order, use:
        // return Double.compare(other.time, this.time);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Rewards)) return false;
        Rewards rewards = (Rewards) o;
        return Double.compare(rewards.time, time) == 0 && name.equals(rewards.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, time);
    }
}